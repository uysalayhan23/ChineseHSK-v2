package com.ayhan.chineselearning

import android.content.Context
import org.json.JSONObject
import kotlin.math.absoluteValue

class VoiceIdentityResolver(context: Context) {
    private val appContext = context.applicationContext

    private data class VoiceMaps(
        val bySpeaker: Map<String, String>,
        val allProfiles: List<String>,
        val archetypeBySpeaker: Map<String, String>
    )

    private val maps: VoiceMaps by lazy { loadVoiceMaps() }

    fun profileForSpeaker(speaker: String): String {
        val key = speaker.trim()
        maps.bySpeaker[key]?.let { if (it.isNotBlank()) return it }

        val pool = maps.allProfiles
        if (pool.isNotEmpty()) {
            val index = (key.hashCode() and Int.MAX_VALUE) % pool.size
            return pool[index]
        }

        // Last-resort identifier remains deterministic even if the voice manifest is unavailable.
        return "VOICE_STABLE_" + (key.hashCode() and Int.MAX_VALUE).toString(16)
    }

    /** Uses canonical age and gender metadata for each dialogue speaker. */
    fun archetypeForSpeaker(speaker: String): String =
        maps.archetypeBySpeaker[speaker.trim()] ?: "neutral"

    private fun loadVoiceMaps(): VoiceMaps {
        return try {
            val voicesText = appContext.assets
                .open("chinese_course/voices.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val voicesRoot = JSONObject(voicesText)
            val voices = voicesRoot.optJSONArray("voices")
            val byCharacterId = mutableMapOf<String, String>()
            val allProfiles = mutableListOf<String>()

            if (voices != null) {
                for (i in 0 until voices.length()) {
                    val item = voices.optJSONObject(i) ?: continue
                    val id = item.optString("id", "").trim()
                    val characterId = item.optString("characterId", "").trim()
                    if (id.isNotBlank()) {
                        allProfiles += id
                        if (characterId.isNotBlank() && characterId !in byCharacterId) {
                            byCharacterId[characterId] = id
                        }
                    }
                }
            }

            val characterText = appContext.assets
                .open("chinese_course/characters.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val characterRoot = JSONObject(characterText)
            val characters = characterRoot.optJSONArray("characters")
            val bySpeaker = mutableMapOf<String, String>()

            if (characters != null) {
                for (i in 0 until characters.length()) {
                    val item = characters.optJSONObject(i) ?: continue
                    val nameZh = item.optString("nameZh", "").trim()
                    val characterId = item.optString("id", "").trim()
                    val explicit = item.optString("voiceProfileId", "").trim()
                    val profile = explicit.ifBlank { byCharacterId[characterId].orEmpty() }
                    if (nameZh.isNotBlank() && profile.isNotBlank()) {
                        bySpeaker[nameZh] = profile
                    }
                }
            }

            val archetypeBySpeaker = mutableMapOf<String, String>()
            val castText = appContext.assets
                .open("chinese_course/voice_cast.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val cast = JSONObject(castText).optJSONArray("cast")
            if (cast != null) {
                for (i in 0 until cast.length()) {
                    val item = cast.optJSONObject(i) ?: continue
                    val name = item.optString("speaker", "").trim()
                    val gender = item.optString("genderPresentation", "").trim().lowercase()
                    val age = item.optString("ageClass", "").trim().lowercase()
                    if (name.isBlank()) continue
                    val type = when {
                        gender == "female" && age in setOf("child", "child_or_teen", "teen") -> "girl"
                        gender == "male" && age in setOf("child", "child_or_teen", "teen") -> "boy"
                        gender == "female" -> "female"
                        gender == "male" -> "male"
                        else -> "neutral"
                    }
                    archetypeBySpeaker[name] = type
                    val profile = item.optString("voiceProfileId", "").trim()
                    if (profile.isNotBlank()) {
                        bySpeaker[name] = profile
                        allProfiles += profile
                    }
                }
            }

            VoiceMaps(
                bySpeaker = bySpeaker.toMap(),
                allProfiles = allProfiles.distinct().sorted(),
                archetypeBySpeaker = archetypeBySpeaker.toMap()
            )
        } catch (_: Throwable) {
            VoiceMaps(emptyMap(), emptyList(), emptyMap())
        }
    }
}
