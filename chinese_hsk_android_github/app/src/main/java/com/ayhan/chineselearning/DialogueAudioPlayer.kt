package com.ayhan.chineselearning

import android.content.Context

/**
 * Speaks scene text through the phone's installed Simplified Mandarin TTS
 * voice. Dialogue playback intentionally does not use generated audio assets.
 */
class DialogueAudioPlayer(context: Context) {
    private val tts = MandarinTtsPlayer(context)
    private val voiceResolver = VoiceIdentityResolver(context)

    fun playDialogue(dialogue: DialogueLine, speed: Float, onDone: (() -> Unit)? = null) {
        val profile = voiceResolver.profileForSpeaker(dialogue.speaker)
        val archetype = voiceResolver.archetypeForSpeaker(dialogue.speaker)
        tts.speakNativeMandarin(dialogue.zh, profile, archetype, speed, dialogue.emotion, onDone)
    }

    fun playNarrator(sceneId: String, narrator: NarratorInfo, speed: Float = 0.9f, onDone: (() -> Unit)? = null) {
        if (narrator.zh.isBlank()) {
            onDone?.invoke()
            return
        }
        tts.speakNativeMandarin(narrator.zh, narrator.profileId, "neutral", speed, "neutral", onDone)
    }

    fun offlineVoiceCount(): Int = tts.offlineVoiceCount()
    fun nativeMandarinVoiceCount(): Int = tts.nativeMandarinVoiceCount()
    fun nativeVoiceOptions(): List<NativeMandarinVoiceOption> = tts.nativeVoiceOptions()
    fun selectedNativeVoiceName(archetype: String): String? = tts.selectedNativeVoiceName(archetype)
    fun assignNativeVoice(archetype: String, voiceName: String?) = tts.assignNativeVoice(archetype, voiceName)
    fun previewNativeVoice(voiceName: String?, archetype: String) = tts.previewNativeVoice(voiceName, archetype)
    fun refreshNativeVoiceInventory() = tts.refreshNativeVoiceInventory()
    fun cachedTtsCount(): Int = tts.cachedFileCount()
    fun cachedTtsBytes(): Long = tts.cacheBytes()
    fun clearTtsCache() = tts.clearCache()

    fun stop() = tts.stop()
    fun shutdown() = tts.shutdown()
}
