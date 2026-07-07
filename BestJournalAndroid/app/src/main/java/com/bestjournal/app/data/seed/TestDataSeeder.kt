package com.bestjournal.app.data.seed

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bestjournal.app.R
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.data.repository.JournalRepository
import com.bestjournal.app.domain.model.JournalEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class TestDataSeeder
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val ai: FirebaseAiService,
    private val journalRepository: JournalRepository,
) {
    companion object {
        private const val TAG = "TestDataSeeder"
        private const val PREFS_NAME = "test_data_seeder_prefs"
        private const val KEY_SEEDED_IDS = "seeded_entry_ids"
        private const val TARGET_COUNT = 70
        private const val DAYS_BACK = 90L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isSeeded(): Boolean = getSeededIds().isNotEmpty()

    fun getSeededCount(): Int = getSeededIds().size

    private fun getSeededIds(): List<Long> {
        val raw = prefs.getString(KEY_SEEDED_IDS, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    private fun saveSeededIds(ids: List<Long>) {
        prefs.edit().putString(KEY_SEEDED_IDS, ids.joinToString(",")).apply()
    }

    private fun clearSeededIds() {
        prefs.edit().remove(KEY_SEEDED_IDS).apply()
    }

    /** Oeffentlich fuer "Alle loeschen": vergisst das Seed-ID-Tracking. */
    fun forgetSeededIds() = clearSeededIds()

    suspend fun seed(): Result<Int> {
        return try {
            val locale = context.resources.configuration.locales[0]
            val languageName = locale.getDisplayLanguage(java.util.Locale.ENGLISH)
            val languageTag = locale.toLanguageTag()
            Log.d(TAG, "seed start: locale=$languageTag, languageName=$languageName")

            val entries = generateEntriesViaAi(languageName, languageTag)
            if (entries.isEmpty()) {
                return Result.failure(Exception(context.getString(R.string.dev_seed_ai_no_entries)))
            }

            val timestamps = generateTimestamps(entries.size)
            val savedIds = mutableListOf<Long>()
            entries.zip(timestamps).forEach { (pair, ts) ->
                val (title, text) = pair
                val mood = pickMood(text)
                val entry =
                    JournalEntry(
                        timestamp = ts,
                        rawText = text,
                        improvedText = null,
                        isImproved = false,
                        displayText = text,
                        audioDurationSeconds = 0,
                        moodTag = mood,
                        title = title,
                        isSynced = true, // mark as synced so they won't be uploaded to Drive
                    )
                val id = journalRepository.saveEntry(entry)
                savedIds.add(id)
            }
            saveSeededIds(savedIds)
            Log.d(TAG, "seed done: ${savedIds.size} entries inserted")
            Result.success(savedIds.size)
        } catch (e: Exception) {
            Log.e(TAG, "seed failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSeeded(): Result<Int> {
        return try {
            val ids = getSeededIds()
            if (ids.isEmpty()) {
                return Result.success(0)
            }
            var deleted = 0
            ids.forEach { id ->
                val entry = journalRepository.getEntryById(id)
                if (entry != null) {
                    journalRepository.deleteEntry(entry)
                    deleted++
                }
            }
            clearSeededIds()
            Log.d(TAG, "deleteSeeded: removed $deleted of ${ids.size} entries")
            Result.success(deleted)
        } catch (e: Exception) {
            Log.e(TAG, "deleteSeeded failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun generateEntriesViaAi(
        languageName: String,
        languageTag: String,
    ): List<Pair<String, String>> {
        val systemPrompt =
            "You generate realistic, varied personal journal entries for app testing. " +
                "Always respond with strictly valid JSON (no markdown fences, no explanations)."
        val userPrompt = buildPrompt(languageName, languageTag)

        val result =
            ai.generateContent(
                prompt = userPrompt,
                modelName = FirebaseAiService.MODEL_FLASH,
                temperature = 1.0f,
                maxOutputTokens = 16000,
                systemPrompt = systemPrompt,
            )

        val raw = result.getOrThrow().trim()
        Log.d(TAG, "AI returned ${raw.length} chars")
        val cleaned = stripJsonFences(raw)
        return parseJsonEntries(cleaned)
    }

    private fun buildPrompt(languageName: String, languageTag: String): String =
        """
        Generate exactly $TARGET_COUNT realistic personal journal entries for app testing.
        All text must be written in $languageName (locale: $languageTag).
        The entries should sound like one consistent person reflecting on the last 3 months
        of their life — work, relationships, hobbies, small wins, frustrations, weather,
        food, weekends, weekday routine, occasional travel.

        Mix the entry lengths:
        - About 25 short entries (1-2 sentences, like a quick mood note)
        - About 25 medium entries (3-6 sentences)
        - About 20 long, reflective entries (8-15 sentences with detail)

        Each entry must have:
        - "title": a short headline (max 6 words) in $languageName
        - "text": the entry body in $languageName

        Output STRICTLY a valid JSON array with $TARGET_COUNT objects. Example shape:
        [{"title":"...","text":"..."},{"title":"...","text":"..."}]

        No markdown fences. No explanations. No leading or trailing text. Only the JSON array.
        """
            .trimIndent()

    private fun stripJsonFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter("\n", text).trim()
            if (text.endsWith("```")) {
                text = text.substringBeforeLast("```").trim()
            }
        }
        // Trim anything before the first '[' and after the last ']'
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1)
        }
        return text
    }

    private fun parseJsonEntries(json: String): List<Pair<String, String>> {
        val arr = JSONArray(json)
        val out = mutableListOf<Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val obj: JSONObject = arr.getJSONObject(i)
            val title = obj.optString("title", "").trim()
            val text = obj.optString("text", "").trim()
            if (text.isNotEmpty()) {
                out.add(title to text)
            }
        }
        return out
    }

    private fun generateTimestamps(count: Int): List<Long> {
        // Spread `count` timestamps roughly evenly over the last DAYS_BACK days,
        // with random jitter so they look natural. Sort ascending so older first.
        val now = System.currentTimeMillis()
        val msPerDay = 24L * 60L * 60L * 1000L
        val totalMs = DAYS_BACK * msPerDay
        val step = totalMs / count
        val list =
            (0 until count).map { i ->
                val base = now - totalMs + (step * i)
                // Jitter: random offset within +/- 40% of step
                val jitter = (Random.nextLong(step) - step / 2)
                // Random hour-of-day shift to make times look organic
                val hourShift = Random.nextLong(8 * 60 * 60 * 1000L)
                (base + jitter + hourShift).coerceAtMost(now - 60_000L)
            }
        return list.sorted()
    }

    private fun pickMood(text: String): String {
        // Lightweight keyword-based mood; falls back to random if no match
        val lower = text.lowercase()
        val positive =
            listOf("happy", "great", "good", "love", "win", "joy", "calm", "proud")
        val negative =
            listOf("sad", "tired", "stressed", "angry", "frustrated", "worried", "anxious")
        return when {
            positive.any { lower.contains(it) } -> "positiv"
            negative.any { lower.contains(it) } -> "belastend"
            else -> listOf("positiv", "neutral", "belastend").random()
        }
    }
}
