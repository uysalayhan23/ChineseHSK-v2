package com.ayhan.chineselearning

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONArray
import kotlin.math.abs

data class NativeMandarinVoiceOption(
    val name: String,
    val label: String,
    val kind: String,
    val offline: Boolean,
    val quality: Int
)

private data class BundledVoice(
    val sid: Int,
    val datasetId: String,
    val ageGroup: String,
    val gender: String,
    val accent: String
)

/**
 * Bundled offline AISHELL-3 voices, with Android Mandarin TTS as a fallback.
 * Dialogue audio is generated from current text; no pre-recorded foreign lines.
 */
class MandarinTtsPlayer(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(appContext, this)
    private val prefs = appContext.getSharedPreferences("offline_mandarin_voice_bindings", Context.MODE_PRIVATE)
    private val cacheDir = File(appContext.filesDir, "mandarin_tts_cache").apply { mkdirs() }
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private val synthErrors = ConcurrentHashMap<String, () -> Unit>()
    private val synthesisExecutor = Executors.newSingleThreadExecutor()
    private val synthesisGeneration = AtomicInteger(0)
    private var ready = false
    private var pending: (() -> Unit)? = null
    private var player: MediaPlayer? = null
    private var localVoices: List<Voice> = emptyList()
    private var nativeMandarinVoices: List<Voice> = emptyList()
    private var bundledTts: OfflineTts? = null
    private val bundledVoices: List<BundledVoice> by lazy { loadBundledVoices() }

    companion object {
        private const val MAX_CACHE_BYTES = 350L * 1024L * 1024L
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            pending?.invoke()
            pending = null
            return
        }
        val mandarinLocale = Locale.forLanguageTag("zh-CN")
        val result = tts.setLanguage(mandarinLocale)
        ready = result >= TextToSpeech.LANG_AVAILABLE
        nativeMandarinVoices = tts.voices.orEmpty()
            .filter { isSimplifiedMandarin(it.locale) }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.latency }.thenBy { it.name })
        localVoices = nativeMandarinVoices.filterNot { it.isNetworkConnectionRequired }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                if (utteranceId == null) return
                val callback = callbacks.remove(utteranceId)
                synthErrors.remove(utteranceId)
                if (callback != null) mainHandler.post(callback)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handleError(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                handleError(utteranceId)
            }
        })

        val action = pending
        pending = null
        action?.invoke()
        pruneCache()
    }

    private fun handleError(utteranceId: String?) {
        if (utteranceId == null) return
        val fallback = synthErrors.remove(utteranceId)
        val callback = callbacks.remove(utteranceId)
        if (fallback != null) mainHandler.post(fallback)
        else if (callback != null) mainHandler.post(callback)
    }

    private fun isSimplifiedMandarin(locale: Locale): Boolean =
        locale.language.equals("zh", ignoreCase = true) &&
            locale.country.equals("CN", ignoreCase = true) &&
            (locale.script.isBlank() || locale.script.equals("Hans", ignoreCase = true))

    private fun voiceKind(voice: Voice): String {
        val labels = (listOf(voice.name) + voice.features.orEmpty()).joinToString(" ").lowercase()
        val child = listOf("child", "kid", "teen", "童声", "儿童", "小孩", "女孩", "男孩").any { it in labels }
        val female = listOf("female", "woman", "girl", "女声", "女性", "女孩").any { it in labels }
        val male = listOf("male", "man", "boy", "男声", "男性", "男孩").any { it in labels }
        return when {
            child && female -> "girl"
            child && male -> "boy"
            child -> "child"
            female -> "female"
            male -> "male"
            else -> "neutral"
        }
    }

    private fun voiceCandidates(archetype: String): List<Voice> {
        if (nativeMandarinVoices.isEmpty()) return emptyList()
        val kinds = nativeMandarinVoices.associateWith(::voiceKind)
        val preferred = when (archetype) {
            "girl" -> listOf("girl", "female")
            "boy" -> listOf("boy", "male")
            "child", "young" -> listOf("child")
            "female" -> listOf("female")
            "male" -> listOf("male")
            else -> emptyList()
        }
        val match = preferred.firstNotNullOfOrNull { kind ->
            nativeMandarinVoices.filter { kinds[it] == kind }.takeIf { it.isNotEmpty() }
        }
        return match ?: if (archetype == "neutral") nativeMandarinVoices.take(1) else emptyList()
    }

    private fun configureNativeMandarin(archetype: String) {
        val selectedSlotName = prefs.getString(slotPreferenceKey(archetype), null)
        val selectedSlot = selectedSlotName?.let { name -> nativeMandarinVoices.firstOrNull { it.name == name } }
        // Android often exposes Mandarin voices without gender or age labels.
        // Distribute the installed voices across speaker roles instead of sending
        // every unclassified role to the same engine default.
        val selected = selectedSlot ?: automaticRoleVoices()[archetype]
        if (selected != null) {
            tts.voice = selected
        } else {
            tts.setLanguage(Locale.forLanguageTag("zh-CN"))
        }
        tts.setPitch(1.0f)
    }

    private fun automaticRoleVoices(): Map<String, Voice> {
        val roles = listOf("male", "female", "boy", "girl")
        val result = linkedMapOf<String, Voice>()
        val reserved = roles.mapNotNull { role ->
            prefs.getString(slotPreferenceKey(role), null)
                ?.let { name -> nativeMandarinVoices.firstOrNull { it.name == name } }
                ?.name
        }.toMutableSet()

        roles.forEach { role ->
            val manuallySelected = prefs.getString(slotPreferenceKey(role), null)
                ?.let { name -> nativeMandarinVoices.firstOrNull { it.name == name } }
            if (manuallySelected != null) {
                result[role] = manuallySelected
                return@forEach
            }

            val preferred = voiceCandidates(role)
            val selected = preferred.firstOrNull { it.name !in reserved }
                ?: nativeMandarinVoices.firstOrNull { it.name !in reserved }
                // Reuse a voice only after the installed inventory is exhausted.
                ?: preferred.firstOrNull()
                ?: nativeMandarinVoices.firstOrNull()
            if (selected != null) {
                result[role] = selected
                reserved += selected.name
            }
        }
        return result
    }

    private fun slotPreferenceKey(archetype: String): String =
        "native_zh_cn_slot_v3_${safeKey(archetype)}"

    fun nativeVoiceOptions(): List<NativeMandarinVoiceOption> = nativeMandarinVoices.map { voice ->
        NativeMandarinVoiceOption(voice.name, voice.name, voiceKind(voice), !voice.isNetworkConnectionRequired, voice.quality)
    } + bundledVoices.map { voice ->
        NativeMandarinVoiceOption(
            name = "aishell3:${voice.sid}",
            label = "AISHELL · ${genderLabel(voice.gender)} · ${ageLabel(voice.ageGroup)} · ${voice.accent}",
            kind = when {
                voice.ageGroup == "A" && voice.gender == "female" -> "girl"
                voice.ageGroup == "B" && voice.gender == "male" -> "boy"
                voice.gender == "female" -> "female"
                else -> "male"
            },
            offline = true,
            quality = 100
        )
    }

    fun bundledVoiceCount(): Int = bundledVoices.size

    private fun genderLabel(gender: String): String = if (gender == "female") "kadın" else "erkek"
    private fun ageLabel(group: String): String = when (group) {
        "A" -> "14 yaş altı"
        "B" -> "14–25 yaş"
        "C" -> "26–40 yaş"
        "D" -> "41 yaş üstü"
        else -> "yaş bilgisi yok"
    }

    private fun loadBundledVoices(): List<BundledVoice> = runCatching {
        val json = appContext.assets.open("mandarin_voice_inventory.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val rows = JSONArray(json)
        (0 until rows.length()).mapNotNull { i ->
            rows.optJSONObject(i)?.let { row ->
                BundledVoice(
                    sid = row.optInt("sid", -1),
                    datasetId = row.optString("datasetId", ""),
                    ageGroup = row.optString("ageGroup", ""),
                    gender = row.optString("gender", ""),
                    accent = row.optString("accent", "")
                )
            }
        }.filter { it.sid >= 0 && it.datasetId.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun bundledSpeaker(profileId: String, archetype: String): BundledVoice? {
        if (bundledVoices.isEmpty()) return null
        val normalized = when (archetype) {
            "boy", "child", "young" -> "boy"
            "girl" -> "girl"
            "male" -> "male"
            "female" -> "female"
            else -> "neutral"
        }
        val prefKey = "aishell3_profile_${safeKey(profileId)}"
        val saved = prefs.getString(prefKey, null)?.toIntOrNull()
        if (saved != null) bundledVoices.firstOrNull { it.sid == saved }?.let { return it }
        val candidates = when (normalized) {
            "boy" -> bundledVoices.filter { it.gender == "male" && it.ageGroup == "B" }
                .ifEmpty { bundledVoices.filter { it.gender == "male" } }
            "girl" -> bundledVoices.filter { it.gender == "female" && it.ageGroup == "A" }
                .ifEmpty { bundledVoices.filter { it.gender == "female" && it.ageGroup == "B" } }
            "male" -> bundledVoices.filter { it.gender == "male" && it.ageGroup in setOf("C", "D") }
                .ifEmpty { bundledVoices.filter { it.gender == "male" } }
            "female" -> bundledVoices.filter { it.gender == "female" && it.ageGroup in setOf("C", "D") }
                .ifEmpty { bundledVoices.filter { it.gender == "female" && it.ageGroup == "B" } }
            else -> bundledVoices
        }
        if (candidates.isEmpty()) return null
        val picked = candidates[Math.floorMod(profileId.hashCode(), candidates.size)]
        prefs.edit().putString(prefKey, picked.sid.toString()).apply()
        return picked
    }

    private fun selectedBundledSpeaker(archetype: String, profileId: String): BundledVoice? {
        val configured = prefs.getString(slotPreferenceKey(archetype), null)
        if (configured?.startsWith("aishell3:") == true) {
            val sid = configured.substringAfter(':').toIntOrNull()
            bundledVoices.firstOrNull { it.sid == sid }?.let { return it }
        }
        return bundledSpeaker(profileId, archetype)
    }

    private fun createBundledTts(): OfflineTts {
        bundledTts?.let { return it }
        val root = "mandarin_tts"
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "${root}/model.onnx",
                    lexicon = "${root}/lexicon.txt",
                    tokens = "${root}/tokens.txt"
                ),
                numThreads = 2,
                provider = "cpu",
                debug = false
            ),
            ruleFsts = listOf("phone.fst", "date.fst", "number.fst", "new_heteronym.fst")
                .joinToString(",") { "${root}/$it" },
            maxNumSentences = 1
        )
        return OfflineTts(appContext.assets, config).also { bundledTts = it }
    }

    private fun speakBundled(
        text: String,
        profileId: String,
        archetype: String,
        speed: Float,
        emotion: String,
        onDone: (() -> Unit)?
    ): Boolean {
        val speaker = selectedBundledSpeaker(archetype, profileId) ?: return false
        val emotionRate = when (emotion.lowercase()) {
            "excited", "enthusiastic", "happy", "surprised" -> 1.04f
            "sad", "tired", "worried" -> 0.96f
            else -> 1f
        }
        val modelSpeed = (speed * emotionRate).coerceIn(0.82f, 1.18f)
        val cacheKey = "MODEL_${speaker.sid}_${text.hashCode().toUInt().toString(16)}_${modelSpeed.toBits()}"
        val output = File(cacheDir, "${cacheKey}.wav")
        if (output.exists() && output.length() > 1024L) {
            playFile(output, 1.0f, onDone)
            return true
        }
        val generation = synthesisGeneration.incrementAndGet()
        synthesisExecutor.execute {
            runCatching {
                createBundledTts().generate(text, speaker.sid, modelSpeed).save(output.absolutePath)
            }.onSuccess {
                mainHandler.post {
                    if (generation == synthesisGeneration.get() && output.exists()) {
                        playFile(output, 1.0f, onDone)
                        pruneCache()
                    }
                }
            }.onFailure {
                mainHandler.post {
                    if (generation == synthesisGeneration.get()) {
                        speakAndroidFallback(text, archetype, speed, onDone)
                    }
                }
            }
        }
        return true
    }

    private fun speakAndroidFallback(text: String, archetype: String, speed: Float, onDone: (() -> Unit)?) {
        if (!ready) { onDone?.invoke(); return }
        configureNativeMandarin(archetype)
        tts.setSpeechRate(speed.coerceIn(0.65f, 1.35f))
        val utteranceId = "NATIVE_ZH_CN_${System.nanoTime()}"
        if (onDone != null) callbacks[utteranceId] = onDone
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId) == TextToSpeech.ERROR) {
            callbacks.remove(utteranceId)
            onDone?.invoke()
        }
    }

    fun selectedNativeVoiceName(archetype: String): String? =
        prefs.getString(slotPreferenceKey(archetype), null)

    fun assignNativeVoice(archetype: String, voiceName: String?) {
        val edit = prefs.edit()
        if (voiceName == null) edit.remove(slotPreferenceKey(archetype))
        else if (nativeMandarinVoices.any { it.name == voiceName }) edit.putString(slotPreferenceKey(archetype), voiceName)
        edit.apply()
    }

    fun previewNativeVoice(voiceName: String?, archetype: String) {
        stopPlaybackOnly()
        if (voiceName?.startsWith("aishell3:") == true) {
            speakBundled("你好，今天很高兴和你一起学习中文。", "preview_${voiceName}_${archetype}", archetype, 0.92f, "neutral", null)
            return
        }
        val action: () -> Unit = {
            val voice = voiceName?.let { name -> nativeMandarinVoices.firstOrNull { it.name == name } }
                ?: automaticRoleVoices()[archetype]
                ?: voiceCandidates(archetype).firstOrNull()
            if (voice != null) tts.voice = voice else tts.setLanguage(Locale.forLanguageTag("zh-CN"))
            tts.setPitch(1.0f)
            tts.setSpeechRate(0.92f)
            tts.speak("你好，今天很高兴和你一起学习中文。", TextToSpeech.QUEUE_FLUSH, Bundle(), "VOICE_PREVIEW_${System.nanoTime()}")
        }
        if (ready) action() else pending = action
    }

    fun refreshNativeVoiceInventory() {
        val result = tts.setLanguage(Locale.forLanguageTag("zh-CN"))
        ready = result >= TextToSpeech.LANG_AVAILABLE
        nativeMandarinVoices = tts.voices.orEmpty()
            .filter { isSimplifiedMandarin(it.locale) }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.latency }.thenBy { it.name })
        localVoices = nativeMandarinVoices.filterNot { it.isNetworkConnectionRequired }
    }

    fun speakNativeMandarin(
        text: String,
        profileId: String,
        archetype: String,
        speed: Float,
        emotion: String = "neutral",
        onDone: (() -> Unit)? = null
    ) {
        val action: () -> Unit = {
            stopPlaybackOnly()
            if (speakBundled(text, profileId, archetype, speed, emotion, onDone)) Unit
            else if (!ready) onDone?.invoke()
            else {
                configureNativeMandarin(archetype)
                val emotionRate = when (emotion.lowercase()) {
                    "excited", "enthusiastic", "happy", "surprised" -> 1.03f
                    "sad", "tired", "worried" -> 0.97f
                    else -> 1.0f
                }
                tts.setSpeechRate((speed * emotionRate).coerceIn(0.65f, 1.35f))
                val utteranceId = "NATIVE_ZH_CN_${System.nanoTime()}"
                if (onDone != null) callbacks[utteranceId] = onDone
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
                if (result == TextToSpeech.ERROR) {
                    callbacks.remove(utteranceId)
                    onDone?.invoke()
                }
            }
        }
        if (bundledVoices.isNotEmpty() || ready) action() else pending = action
    }

    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun cacheFile(cacheKey: String, voiceProfileId: String): File =
        File(cacheDir, "${safeKey(cacheKey)}__${safeKey(voiceProfileId)}.wav")

    private fun deterministicPitch(profileId: String): Float {
        val bucket = abs(profileId.hashCode()) % 19
        return (0.91f + bucket / 100f).coerceIn(0.91f, 1.09f)
    }

    private fun chooseVoice(profileId: String): Voice? {
        if (localVoices.isEmpty()) return null
        val prefKey = "voice_$profileId"
        val saved = prefs.getString(prefKey, null)
        val existing = saved?.let { name -> localVoices.firstOrNull { it.name == name } }
        if (existing != null) return existing

        val chosen = localVoices[abs(profileId.hashCode()) % localVoices.size]
        prefs.edit().putString(prefKey, chosen.name).apply()
        return chosen
    }

    private fun configure(profileId: String) {
        chooseVoice(profileId)?.let { tts.voice = it }
            ?: tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
        tts.setPitch(deterministicPitch(profileId))
        tts.setSpeechRate(1.0f)
    }

    fun playOrCache(
        cacheKey: String,
        text: String,
        voiceProfileId: String,
        speed: Float,
        onDone: (() -> Unit)? = null
    ) {
        val action = {
            val file = cacheFile(cacheKey, voiceProfileId)
            if (file.exists() && file.length() > 1024L) {
                playFile(file, speed, onDone)
            } else {
                configure(voiceProfileId)
                val utteranceId = "CACHE_${safeKey(cacheKey)}_${System.nanoTime()}"
                callbacks[utteranceId] = {
                    if (file.exists() && file.length() > 1024L) {
                        file.setLastModified(System.currentTimeMillis())
                        playFile(file, speed, onDone)
                        pruneCache()
                    } else {
                        speakDirect(text, voiceProfileId, speed, onDone)
                    }
                }
                synthErrors[utteranceId] = { speakDirect(text, voiceProfileId, speed, onDone) }
                val result = tts.synthesizeToFile(text, Bundle(), file, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    callbacks.remove(utteranceId)
                    synthErrors.remove(utteranceId)
                    speakDirect(text, voiceProfileId, speed, onDone)
                }
            }
        }
        if (ready) action() else pending = action
    }

    /**
     * Canonical convenience entry point for vocabulary, pronunciation, placement
     * and interactive-learning speech. It uses the exact same offline local
     * Mandarin voice binding + persistent cache pipeline as scene dialogue.
     */
    fun speak(
        text: String,
        voiceProfileId: String,
        speed: Float,
        onDone: (() -> Unit)? = null
    ) {
        val stableTextKey = "GEN_${safeKey(voiceProfileId)}_${text.hashCode().toUInt().toString(16).uppercase()}"
        playOrCache(stableTextKey, text, voiceProfileId, speed, onDone)
    }

    fun speakDirect(text: String, voiceProfileId: String, speed: Float, onDone: (() -> Unit)? = null) {
        val action = {
            stopPlaybackOnly()
            configure(voiceProfileId)
            tts.setSpeechRate(speed.coerceIn(0.65f, 1.35f))
            val utteranceId = "SPEAK_${System.nanoTime()}"
            if (onDone != null) callbacks[utteranceId] = onDone
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
            Unit
        }
        if (ready) action() else pending = action
    }

    private fun playFile(file: File, speed: Float, onDone: (() -> Unit)?) {
        stopPlaybackOnly()
        try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener {
                try {
                    it.playbackParams = PlaybackParams()
                        .setSpeed(speed.coerceIn(0.65f, 1.35f))
                        .setPitch(1.0f)
                } catch (_: Exception) { }
                file.setLastModified(System.currentTimeMillis())
                it.start()
            }
            mp.setOnCompletionListener {
                it.release()
                if (player === it) player = null
                onDone?.invoke()
            }
            mp.setOnErrorListener { failed, _, _ ->
                failed.release()
                if (player === failed) player = null
                onDone?.invoke()
                true
            }
            player = mp
            mp.prepareAsync()
        } catch (_: Exception) {
            player = null
            onDone?.invoke()
        }
    }

    private fun stopPlaybackOnly() {
        try { player?.stop() } catch (_: Exception) { }
        try { player?.release() } catch (_: Exception) { }
        player = null
    }

    fun cachedFileCount(): Int = cacheDir.listFiles()?.count { it.isFile && it.length() > 1024L } ?: 0
    fun cacheBytes(): Long = cacheDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    fun offlineVoiceCount(): Int = localVoices.size
    fun nativeMandarinVoiceCount(): Int = nativeMandarinVoices.size

    private fun pruneCache() {
        val files = cacheDir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        for (file in files) {
            if (total <= MAX_CACHE_BYTES) break
            val len = file.length()
            if (file.delete()) total -= len
        }
    }

    fun clearCache() {
        stopPlaybackOnly()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun stop() {
        pending = null
        callbacks.clear()
        synthErrors.clear()
        stopPlaybackOnly()
        tts.stop()
    }

    fun shutdown() {
        stop()
        synthesisExecutor.shutdownNow()
        bundledTts?.release()
        bundledTts = null
        tts.shutdown()
    }
}
