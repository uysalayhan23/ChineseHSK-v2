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
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Offline-first Mandarin speech engine.
 *
 * 1) Uses only locally installed zh voices when possible.
 * 2) Binds each stable Voice Bible profile to a deterministic local voice.
 * 3) Synthesizes a line once to a persistent local cache and reuses it afterwards.
 * 4) Playback speed is applied by MediaPlayer, so one cached file serves all speed settings.
 */
class MandarinTtsPlayer(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(appContext, this)
    private val prefs = appContext.getSharedPreferences("offline_mandarin_voice_bindings", Context.MODE_PRIVATE)
    private val cacheDir = File(appContext.filesDir, "mandarin_tts_cache").apply { mkdirs() }
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private val synthErrors = ConcurrentHashMap<String, () -> Unit>()
    private var ready = false
    private var pending: (() -> Unit)? = null
    private var player: MediaPlayer? = null
    private var localVoices: List<Voice> = emptyList()

    companion object {
        private const val MAX_CACHE_BYTES = 350L * 1024L * 1024L
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
        ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        localVoices = tts.voices.orEmpty()
            .filter { it.locale.language.equals("zh", ignoreCase = true) && !it.isNetworkConnectionRequired }
            .sortedBy { it.name }

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

        if (ready) {
            val action = pending
            pending = null
            action?.invoke()
        }
        pruneCache()
    }

    private fun handleError(utteranceId: String?) {
        if (utteranceId == null) return
        callbacks.remove(utteranceId)
        val fallback = synthErrors.remove(utteranceId)
        if (fallback != null) mainHandler.post(fallback)
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
        tts.shutdown()
    }
}
