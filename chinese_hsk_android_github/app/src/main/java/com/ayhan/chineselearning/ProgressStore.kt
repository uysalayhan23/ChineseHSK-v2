package com.ayhan.chineselearning

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("chinese_learning_progress", Context.MODE_PRIVATE)

    companion object {
        const val VOCAB_PASS = 90
        const val SENTENCE_PASS = 85

        const val DISPLAY_AUTO = 0
        const val DISPLAY_ON = 1
        const val DISPLAY_OFF = 2

        const val THEME_PURPLE = 0
        const val THEME_DARK = 1
        const val THEME_LIGHT = 2
        const val THEME_BLUE = 3
        const val THEME_GREEN = 4
        const val THEME_ORANGE = 5
        const val THEME_PINK = 6
    }

    fun isOnboardingComplete(): Boolean = prefs.getBoolean("onboarding_complete", false)

    fun completeOnboarding(startLevel: Int) {
        prefs.edit()
            .putBoolean("onboarding_complete", true)
            .putInt("placement_start_level", startLevel.coerceIn(1, 6))
            .apply()
    }

    fun placementStartLevel(): Int = prefs.getInt("placement_start_level", 1).coerceIn(1, 6)

    fun savePlacementResult(level: Int, score: Int, total: Int) {
        prefs.edit()
            .putInt("placement_start_level", level.coerceIn(1, 6))
            .putInt("placement_score", score.coerceAtLeast(0))
            .putInt("placement_total", total.coerceAtLeast(1))
            .putBoolean("onboarding_complete", true)
            .apply()
    }

    fun placementScore(): Pair<Int, Int> =
        prefs.getInt("placement_score", 0) to prefs.getInt("placement_total", 0)

    fun favoriteIds(): Set<String> = prefs.getStringSet("favorite_word_ids", emptySet())?.toSet() ?: emptySet()

    fun isFavorite(wordId: String): Boolean = favoriteIds().contains(wordId)

    fun toggleFavorite(wordId: String): Boolean {
        val ids = favoriteIds().toMutableSet()
        val nowFavorite = if (ids.contains(wordId)) {
            ids.remove(wordId)
            false
        } else {
            ids.add(wordId)
            true
        }
        prefs.edit().putStringSet("favorite_word_ids", ids).apply()
        recordStudyActivity()
        return nowFavorite
    }

    fun vocabularyExamScore(sceneId: String): Int = prefs.getInt("exam_vocab_$sceneId", 0)
    fun sentenceExamScore(sceneId: String): Int = prefs.getInt("exam_sentence_$sceneId", 0)

    fun vocabularyExamPassed(sceneId: String, required: Int = VOCAB_PASS): Boolean =
        vocabularyExamScore(sceneId) >= required

    fun sentenceExamPassed(sceneId: String, required: Int = SENTENCE_PASS): Boolean =
        sentenceExamScore(sceneId) >= required

    fun saveVocabularyExam(sceneId: String, score: Int, required: Int = VOCAB_PASS) {
        val best = maxOf(score, vocabularyExamScore(sceneId))
        prefs.edit().putInt("exam_vocab_$sceneId", best).apply()
        if (best < required) prefs.edit().putBoolean("mastered_$sceneId", false).apply()
        recordStudyActivity()
    }

    fun saveSentenceExam(
        sceneId: String,
        score: Int,
        vocabRequired: Int = VOCAB_PASS,
        sentenceRequired: Int = SENTENCE_PASS
    ) {
        val best = maxOf(score, sentenceExamScore(sceneId))
        val mastered = vocabularyExamPassed(sceneId, vocabRequired) && best >= sentenceRequired
        prefs.edit()
            .putInt("exam_sentence_$sceneId", best)
            .putBoolean("mastered_$sceneId", mastered)
            .apply()
        recordStudyActivity()
    }

    fun pronunciationBest(itemId: String): Int = prefs.getInt("pronunciation_$itemId", 0)

    fun savePronunciationBest(itemId: String, score: Int) {
        val best = maxOf(score.coerceIn(0, 100), pronunciationBest(itemId))
        prefs.edit().putInt("pronunciation_$itemId", best).apply()
        recordStudyActivity()
    }

    fun dialoguePosition(sceneId: String): Int = prefs.getInt("dialogue_pos_$sceneId", 0)

    fun saveDialoguePosition(sceneId: String, position: Int) {
        prefs.edit().putInt("dialogue_pos_$sceneId", position.coerceAtLeast(0)).apply()
        prefs.edit().putString("last_scene_id", sceneId).apply()
        recordStudyActivity()
    }

    fun saveLastScene(sceneId: String) {
        prefs.edit().putString("last_scene_id", sceneId).apply()
    }

    fun lastSceneId(): String = prefs.getString("last_scene_id", "").orEmpty()

    fun playbackSpeed(defaultValue: Float = 1.0f): Float =
        prefs.getFloat("playback_speed", defaultValue)

    fun savePlaybackSpeed(speed: Float) {
        prefs.edit().putFloat("playback_speed", speed.coerceIn(0.75f, 1.25f)).apply()
    }

    fun pinyinDisplayMode(): Int = prefs.getInt("setting_pinyin_mode", DISPLAY_AUTO).coerceIn(DISPLAY_AUTO, DISPLAY_OFF)
    fun savePinyinDisplayMode(mode: Int) = prefs.edit().putInt("setting_pinyin_mode", mode.coerceIn(DISPLAY_AUTO, DISPLAY_OFF)).apply()

    fun turkishDisplayMode(): Int = prefs.getInt("setting_turkish_mode", DISPLAY_AUTO).coerceIn(DISPLAY_AUTO, DISPLAY_OFF)
    fun saveTurkishDisplayMode(mode: Int) = prefs.edit().putInt("setting_turkish_mode", mode.coerceIn(DISPLAY_AUTO, DISPLAY_OFF)).apply()

    fun autoPlayDefault(): Boolean = prefs.getBoolean("setting_autoplay", false)
    fun saveAutoPlayDefault(enabled: Boolean) = prefs.edit().putBoolean("setting_autoplay", enabled).apply()

    fun dailyReviewTarget(): Int = prefs.getInt("setting_daily_review_target", 12).coerceIn(5, 40)
    fun saveDailyReviewTarget(value: Int) = prefs.edit().putInt("setting_daily_review_target", value.coerceIn(5, 40)).apply()

    fun themeMode(): Int = prefs.getInt("setting_theme", THEME_PURPLE).coerceIn(THEME_PURPLE, THEME_PINK)
    fun saveThemeMode(mode: Int) = prefs.edit().putInt("setting_theme", mode.coerceIn(THEME_PURPLE, THEME_PINK)).apply()

    fun userName(): String = prefs.getString("setting_user_name", "").orEmpty()
    fun saveUserName(value: String) = prefs.edit().putString("setting_user_name", value.trim().take(40)).apply()

    fun isSceneMastered(sceneId: String): Boolean = prefs.getBoolean("mastered_$sceneId", false)

    fun masteredCount(levelNumber: Int? = null): Int {
        var count = 0
        val levels = if (levelNumber == null) 1..6 else levelNumber..levelNumber
        for (level in levels) {
            for (scene in 1..50) {
                if (isSceneMastered("ZH_HSK${level}_SC%03d".format(scene))) count++
            }
        }
        return count
    }

    fun averageVocabularyExamScore(): Pair<Int, Int> {
        var total = 0
        var count = 0
        for (level in 1..6) for (scene in 1..50) {
            val value = vocabularyExamScore("ZH_HSK${level}_SC%03d".format(scene))
            if (value > 0) { total += value; count++ }
        }
        return (if (count == 0) 0 else total / count) to count
    }

    fun averageSentenceExamScore(): Pair<Int, Int> {
        var total = 0
        var count = 0
        for (level in 1..6) for (scene in 1..50) {
            val value = sentenceExamScore("ZH_HSK${level}_SC%03d".format(scene))
            if (value > 0) { total += value; count++ }
        }
        return (if (count == 0) 0 else total / count) to count
    }

    fun isSceneUnlocked(levelId: String, sceneNumber: Int): Boolean {
        val levelNumber = levelId.removePrefix("HSK").toIntOrNull() ?: return false
        val startLevel = placementStartLevel()

        // Placement can open the first scene of the recommended level, while earlier levels
        // remain available from their own first scene for optional review.
        if (sceneNumber == 1 && levelNumber <= startLevel) return true

        val previousSceneId = if (sceneNumber > 1) {
            "ZH_HSK${levelNumber}_SC%03d".format(sceneNumber - 1)
        } else {
            if (levelNumber <= 1) return true
            "ZH_HSK${levelNumber - 1}_SC050"
        }
        return isSceneMastered(previousSceneId)
    }


    fun recordStudyActivity(date: LocalDate = LocalDate.now()) {
        val key = date.toString()
        val dates = studyDates().toMutableSet()
        dates.add(key)
        prefs.edit()
            .putStringSet("study_dates", dates)
            .putString("last_study_date", key)
            .apply()
    }

    fun studyDates(): Set<String> = prefs.getStringSet("study_dates", emptySet())?.toSet() ?: emptySet()

    fun studiedOn(date: LocalDate): Boolean = studyDates().contains(date.toString())

    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        val dates = studyDates()
        if (dates.isEmpty()) return 0
        var cursor = if (dates.contains(today.toString())) today else today.minusDays(1)
        var streak = 0
        while (dates.contains(cursor.toString())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun longestStreak(): Int {
        val dates = studyDates().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
        if (dates.isEmpty()) return 0
        var best = 1
        var current = 1
        for (i in 1 until dates.size) {
            current = if (ChronoUnit.DAYS.between(dates[i - 1], dates[i]) == 1L) current + 1 else 1
            best = maxOf(best, current)
        }
        return best
    }

    fun studyDaysInMonth(month: YearMonth = YearMonth.now()): Int =
        studyDates().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.count { YearMonth.from(it) == month }

    fun markWordDifficult(wordId: String, difficult: Boolean = true) {
        val ids = prefs.getStringSet("difficult_word_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (difficult) ids.add(wordId) else ids.remove(wordId)
        prefs.edit().putStringSet("difficult_word_ids", ids).apply()
        recordStudyActivity()
    }

    fun isWordDifficult(wordId: String): Boolean =
        prefs.getStringSet("difficult_word_ids", emptySet())?.contains(wordId) == true

    fun recordWordAnswer(wordId: String, correct: Boolean) {
        val attemptsKey = "word_attempts_$wordId"
        val wrongKey = "word_wrong_$wordId"
        val correctKey = "word_correct_$wordId"
        val editor = prefs.edit().putInt(attemptsKey, prefs.getInt(attemptsKey, 0) + 1)
        if (correct) editor.putInt(correctKey, prefs.getInt(correctKey, 0) + 1)
        else editor.putInt(wrongKey, prefs.getInt(wrongKey, 0) + 1)
        editor.apply()
        recordStudyActivity()
    }

    fun weakWordPriority(wordId: String): Int {
        val wrong = prefs.getInt("word_wrong_$wordId", 0)
        val correct = prefs.getInt("word_correct_$wordId", 0)
        val manual = if (isWordDifficult(wordId)) 4 else 0
        return (wrong * 3 + manual - correct).coerceAtLeast(0)
    }

    fun weakWordIds(): Set<String> {
        val manual = prefs.getStringSet("difficult_word_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        prefs.all.keys.filter { it.startsWith("word_wrong_") }.forEach { key ->
            val id = key.removePrefix("word_wrong_")
            if (weakWordPriority(id) > 0) manual.add(id)
        }
        return manual
    }

    fun studyReminderEnabled(): Boolean = prefs.getBoolean("setting_study_reminder_enabled", false)
    fun studyReminderHour(): Int = prefs.getInt("setting_study_reminder_hour", 20).coerceIn(0, 23)
    fun studyReminderMinute(): Int = prefs.getInt("setting_study_reminder_minute", 0).coerceIn(0, 59)

    fun saveStudyReminder(enabled: Boolean, hour: Int = studyReminderHour(), minute: Int = studyReminderMinute()) {
        prefs.edit()
            .putBoolean("setting_study_reminder_enabled", enabled)
            .putInt("setting_study_reminder_hour", hour.coerceIn(0, 23))
            .putInt("setting_study_reminder_minute", minute.coerceIn(0, 59))
            .apply()
    }

    fun exportBackupJson(): String {
        val values = JSONObject()
        prefs.all.toSortedMap().forEach { (key, value) ->
            val entry = JSONObject()
            when (value) {
                is Boolean -> { entry.put("type", "boolean"); entry.put("value", value) }
                is Int -> { entry.put("type", "int"); entry.put("value", value) }
                is Long -> { entry.put("type", "long"); entry.put("value", value) }
                is Float -> { entry.put("type", "float"); entry.put("value", value.toDouble()) }
                is String -> { entry.put("type", "string"); entry.put("value", value) }
                is Set<*> -> {
                    entry.put("type", "stringSet")
                    val array = JSONArray()
                    value.filterIsInstance<String>().sorted().forEach { array.put(it) }
                    entry.put("value", array)
                }
                else -> return@forEach
            }
            values.put(key, entry)
        }
        return JSONObject()
            .put("format", "chinese-hsk-offline-backup")
            .put("schemaVersion", 1)
            .put("values", values)
            .toString(2)
    }

    fun importBackupJson(json: String): Result<Int> = runCatching {
        val root = JSONObject(json)
        require(root.optString("format") == "chinese-hsk-offline-backup") { "Geçersiz yedek dosyası." }
        require(root.optInt("schemaVersion", 0) == 1) { "Desteklenmeyen yedek sürümü." }
        val values = root.getJSONObject("values")
        val editor = prefs.edit().clear()
        var restored = 0
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = values.getJSONObject(key)
            when (entry.getString("type")) {
                "boolean" -> editor.putBoolean(key, entry.getBoolean("value"))
                "int" -> editor.putInt(key, entry.getInt("value"))
                "long" -> editor.putLong(key, entry.getLong("value"))
                "float" -> editor.putFloat(key, entry.getDouble("value").toFloat())
                "string" -> editor.putString(key, entry.getString("value"))
                "stringSet" -> {
                    val array = entry.getJSONArray("value")
                    val set = linkedSetOf<String>()
                    for (i in 0 until array.length()) set.add(array.getString(i))
                    editor.putStringSet(key, set)
                }
            }
            restored++
        }
        check(editor.commit()) { "Yedek telefona yazılamadı." }
        restored
    }
}
