package com.ayhan.chineselearning

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import org.json.JSONObject

/**
 * Authored-audio-first player.
 *
 * Priority:
 * 1) Packaged authored Opus/MP3/M4A asset
 * 2) Locally synthesized, persistent offline Mandarin TTS cache
 * 3) Direct local Mandarin TTS if synthesis-to-file fails
 */
class DialogueAudioPlayer(private val context: Context) {
    private val tts = MandarinTtsPlayer(context)
    private val voiceResolver = VoiceIdentityResolver(context)
    private var mediaPlayer: MediaPlayer? = null
    private val catalog: Map<String, String> by lazy { loadCatalog() }

    private fun loadCatalog(): Map<String, String> {
        return try {
            val text = context.assets.open("chinese_course/media/audio_manifest.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(text)
            val assets = root.optJSONObject("assets") ?: return emptyMap()
            buildMap {
                val keys = assets.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = assets.optString(key, "")
                    if (value.isNotBlank()) put(key, value)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun defaultDialoguePath(dialogueId: String): String =
        "chinese_course/media/audio/dialogues/$dialogueId.opus"

    private fun assetExists(path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            context.assets.open(path).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun generatedDialoguePath(dialogueId: String): String =
        catalog[dialogueId].orEmpty().takeIf {
            it.startsWith("chinese_course/media/audio/generated/")
        }.orEmpty()

    fun hasAuthoredAudio(dialogue: DialogueLine): Boolean {
        val path = generatedDialoguePath(dialogue.id)
        return path.isNotBlank() && assetExists(path)
    }

    fun playDialogue(dialogue: DialogueLine, speed: Float, onDone: (() -> Unit)? = null) {
        val path = generatedDialoguePath(dialogue.id)
        if (path.isNotBlank() && playAsset(path, speed, onDone)) return

        val profileId = voiceResolver.profileForSpeaker(dialogue.speaker)
        // Keep the existing local TTS pipeline, while ensuring edited dialogue
        // text cannot reuse a stale cache entry from the same dialogue ID.
        val textRevision = dialogue.zh.hashCode().toUInt().toString(16).uppercase()
        val stableCacheId = "VOICE_" + profileId + "_" + dialogue.id + "_" + textRevision
        tts.playOrCache(stableCacheId, dialogue.zh, profileId, speed, onDone)
    }

    fun playNarrator(sceneId: String, narrator: NarratorInfo, speed: Float = 0.9f, onDone: (() -> Unit)? = null) {
        val explicit = narrator.audioAsset
        val mapped = catalog["NARRATOR:$sceneId"].orEmpty()
        val convention = "chinese_course/media/audio/narrator/$sceneId.opus"
        val path = explicit.ifBlank { mapped.ifBlank { convention } }
        if (!playAsset(path, speed, onDone)) {
            if (narrator.zh.isNotBlank()) {
                tts.playOrCache("NARRATOR_$sceneId", narrator.zh, narrator.profileId, speed, onDone)
            } else onDone?.invoke()
        }
    }

    private fun playAsset(path: String, speed: Float, onDone: (() -> Unit)?): Boolean {
        if (!assetExists(path)) return false
        return try {
            stopAuthoredAudio()
            val afd = context.assets.openFd(path)
            val player = MediaPlayer()
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.setOnPreparedListener {
                try {
                    it.playbackParams = PlaybackParams()
                        .setSpeed(speed.coerceIn(0.65f, 1.35f))
                        .setPitch(1.0f)
                } catch (_: Exception) { }
                it.start()
            }
            player.setOnCompletionListener {
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
                onDone?.invoke()
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (mediaPlayer === mp) mediaPlayer = null
                true
            }
            mediaPlayer = player
            player.prepareAsync()
            true
        } catch (_: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            false
        }
    }

    private fun stopAuthoredAudio() {
        try { mediaPlayer?.stop() } catch (_: Exception) { }
        try { mediaPlayer?.release() } catch (_: Exception) { }
        mediaPlayer = null
    }

    fun offlineVoiceCount(): Int = tts.offlineVoiceCount()
    fun cachedTtsCount(): Int = tts.cachedFileCount()
    fun cachedTtsBytes(): Long = tts.cacheBytes()
    fun clearTtsCache() = tts.clearCache()

    fun stop() {
        stopAuthoredAudio()
        tts.stop()
    }

    fun shutdown() {
        stopAuthoredAudio()
        tts.shutdown()
    }
}
