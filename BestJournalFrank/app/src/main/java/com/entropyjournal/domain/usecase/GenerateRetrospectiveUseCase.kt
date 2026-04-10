package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import android.util.Log
import com.entropyjournal.data.local.dao.JournalEntryDao
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.data.repository.RetrospectiveRepository
import com.entropyjournal.util.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GenerateRetrospectiveUseCase
@Inject
constructor(
    private val journalDao: JournalEntryDao,
    private val retroRepo: RetrospectiveRepository,
    private val geminiApi: GeminiApi,
    private val encryptedPrefs: SharedPreferences,
) {
    companion object {
        // Use a more capable model for the longer retrospective texts
        private const val MODEL_FLASH = "gemini-2.5-flash"
        private const val MODEL_FLASH_LITE = "gemini-2.5-flash-lite"
    }

    private val dfLabel = SimpleDateFormat("dd.MM.", Locale.GERMANY)
    private val dfFull = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    private val monthNames =
        listOf(
            "Januar",
            "Februar",
            "März",
            "April",
            "Mai",
            "Juni",
            "Juli",
            "August",
            "September",
            "Oktober",
            "November",
            "Dezember",
        )

    private fun getApiKey(): String =
        encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""

    private suspend fun generateContent(
        prompt: String,
        modelName: String,
        temperature: Float,
        maxOutputTokens: Int,
    ): Result<String> {
        val apiKey = getApiKey()
        if (apiKey.isBlank())
            return Result.failure(IllegalStateException("Gemini API-Key nicht konfiguriert"))
        return try {
            val request =
                GeminiRequestBuilder.build(
                    userText = prompt,
                    temperature = temperature,
                    maxOutputTokens = maxOutputTokens,
                )
            val response =
                geminiApi.generateContent(model = modelName, apiKey = apiKey, request = request)
            val text = response.extractText()
            if (text != null) Result.success(text)
            else Result.failure(Exception("No response text from Gemini"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Main entry point: generates all missing weekly, monthly and yearly reviews. Returns the
     * number of new reviews generated.
     */
    var lastFailureCount: Int = 0
        private set

    suspend fun generateMissing(): Int {
        lastFailureCount = 0
        // Cleanup: remove monthly reviews for months without actual diary entries
        cleanupOrphanedMonthlyReviews()
        var generated = 0
        generated += generateMissingWeekly()
        generated += generateMissingMonthly()
        generated += generateMissingYearly()
        if (generated == 0 && lastFailureCount > 0) {
            throw Exception(
                "Alle $lastFailureCount KI-Anfragen fehlgeschlagen. Bitte Internetverbindung prüfen."
            )
        }
        return generated
    }

    // ── Weekly ──────────────────────────────────────────────────────────────

    private suspend fun generateMissingWeekly(): Int {
        val now = Calendar.getInstance()
        var generated = 0

        // Go back up to 8 weeks to catch up missed weeks
        for (weeksBack in 1..8) {
            val (weekStart, weekEnd) = getWeekRange(weeksBack)

            // Time gate: Sunday 15:00 of that week must have passed
            val deadline =
                Calendar.getInstance().apply {
                    timeInMillis = weekEnd.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            if (deadline.timeInMillis > now.timeInMillis) continue

            // Skip if already generated
            if (retroRepo.existsForPeriod("WEEKLY", weekStart.timeInMillis)) continue

            // Get entries for this week — need at least 2
            val entries = journalDao.getEntriesBetween(weekStart.timeInMillis, weekEnd.timeInMillis)
            if (entries.size < 2) {
                Log.d("Retro", "Week ${weeksBack}w ago: only ${entries.size} entries, skipping")
                continue
            }

            Log.d("Retro", "Generating weekly review for ${dfFull.format(weekStart.time)}...")

            val entriesText =
                entries.joinToString("\n\n---\n\n") { entry ->
                    val date = dfFull.format(entry.timestamp)
                    val text = entry.displayText
                    "[$date]\n$text"
                }

            val profileStyle = getProfileStyleInstruction()
            val prompt =
                """Du bist ein Erzähler, der aus Tagebucheinträgen einen natürlichen, gut lesbaren Wochenrückblick schreibt.

AUFGABE: Fasse die folgenden Tagebucheinträge zu einem strukturierten Wochenrückblick zusammen.

FORMAT (bitte genau einhalten):
1. Beginne mit einer Zusammenfassung als kurze Stichpunkte (3-5 Punkte), jeweils mit "• " am Anfang
2. Dann eine Leerzeile
3. Dann der Fließtext, aufgeteilt in 2-4 thematische Abschnitte
4. Jeder Abschnitt beginnt mit einer Überschrift in der Form: [Thema] (z.B. [Neue Begegnungen] oder [Kleine Siege])
5. Nach der Überschrift folgt der erzählende Text des Abschnitts
6. Die Abschnitte chronologisch vom Wochenanfang bis Wochenende ordnen

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst zurückblicken
- Keine Anrede, keine Grußformel — direkt in die Erzählung
- Fließender Stil mit guten Übergängen innerhalb der Abschnitte
- Die Überschriften sollen kurz und thematisch passend sein (2-4 Wörter)
- Erwähne konkrete Ereignisse, Gefühle und Erkenntnisse
- Alltägliches nur erwähnen, wenn es bedeutsam war
- Hebe Positives besonders hervor — aber verschweige Herausforderungen nicht. Erkenntnisse aus schwierigen Momenten gehören dazu
- Schreibe warm und persönlich, aber nicht übertrieben
- Mindestens 200 Wörter
- Sprache: Deutsch$profileStyle

EINTRÄGE DER WOCHE:
$entriesText"""

            val titlePrompt =
                """Basierend auf diesem Wochenrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen.

Rückblick:
"""

            val result =
                generateContent(
                    prompt = prompt,
                    modelName = MODEL_FLASH,
                    temperature = 0.7f,
                    maxOutputTokens = 8192,
                )

            if (result.isFailure) {
                Log.e("Retro", "Weekly AI failed: ${result.exceptionOrNull()?.message}")
                lastFailureCount++
                continue
            }

            val summaryText = result.getOrThrow().trim()

            // Generate title
            val titleResult =
                generateContent(
                    prompt = titlePrompt + summaryText.take(500),
                    modelName = MODEL_FLASH_LITE,
                    temperature = 0.6f,
                    maxOutputTokens = 50,
                )
            val title = titleResult.getOrNull()?.trim()?.take(60) ?: "Wochenrückblick"

            val weekOfMonth =
                if (weekStart.get(Calendar.MONTH) != weekEnd.get(Calendar.MONTH)) {
                    // Week crosses month boundary → counts as Week 1 of the new month
                    1
                } else {
                    weekStart.get(Calendar.DAY_OF_MONTH).let { day ->
                        when {
                            day <= 7 -> 1
                            day <= 14 -> 2
                            day <= 21 -> 3
                            else -> 4
                        }
                    }
                }

            val label =
                "Woche $weekOfMonth (${dfLabel.format(weekStart.time)} - ${dfLabel.format(weekEnd.time)})"

            retroRepo.insert(
                RetrospectiveSummaryEntity(
                    type = "WEEKLY",
                    periodLabel = label,
                    startDate = weekStart.timeInMillis,
                    endDate = weekEnd.timeInMillis,
                    title = title,
                    summaryText = summaryText,
                    periodIndex = weekOfMonth,
                )
            )
            generated++
            Log.d("Retro", "Weekly review saved: $label — $title")
        }
        return generated
    }

    // ── Monthly ─────────────────────────────────────────────────────────────

    private suspend fun generateMissingMonthly(): Int {
        val now = Calendar.getInstance()
        var generated = 0

        // Go back up to 3 months to catch up
        for (monthsBack in 1..3) {
            val (monthStart, monthEnd) = getMonthRange(monthsBack)

            // Time gate: last day of month 15:00 must have passed
            val deadline =
                Calendar.getInstance().apply {
                    timeInMillis = monthEnd.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            if (deadline.timeInMillis > now.timeInMillis) continue

            // Skip if already generated
            if (retroRepo.existsForPeriod("MONTHLY", monthStart.timeInMillis)) continue

            // First check: actual diary entries must exist in this month
            val monthEntries =
                journalDao.getEntriesBetween(monthStart.timeInMillis, monthEnd.timeInMillis)
            if (monthEntries.isEmpty()) {
                Log.d("Retro", "Month ${monthsBack}m ago: no diary entries in this month, skipping")
                continue
            }

            // Get weekly reviews for this month
            val weeklyReviews =
                retroRepo.getByTypeAndRange(
                    "WEEKLY",
                    monthStart.timeInMillis,
                    monthEnd.timeInMillis,
                )
            if (weeklyReviews.isEmpty()) {
                Log.d("Retro", "Month ${monthsBack}m ago: no weekly reviews, skipping")
                continue
            }

            Log.d("Retro", "Generating monthly review from ${weeklyReviews.size} weekly reviews...")

            val weeksText =
                weeklyReviews.joinToString("\n\n---\n\n") { week ->
                    "[${week.periodLabel}] ${week.title}\n${week.summaryText}"
                }

            val month = monthStart.get(Calendar.MONTH)
            val year = monthStart.get(Calendar.YEAR)
            val monthName = monthNames[month]

            val profileStyle = getProfileStyleInstruction()
            val prompt =
                """Du bist ein Erzähler, der aus Wochenrückblicken einen natürlichen, gut lesbaren Monatsrückblick schreibt.

AUFGABE: Fasse die folgenden Wochenrückblicke zu einem strukturierten Monatsrückblick für $monthName $year zusammen.

FORMAT (bitte genau einhalten):
1. Beginne mit einer Zusammenfassung als kurze Stichpunkte (4-6 Punkte), jeweils mit "• " am Anfang
2. Dann eine Leerzeile
3. Dann der Fließtext, aufgeteilt in 3-5 thematische Abschnitte
4. Jeder Abschnitt beginnt mit einer Überschrift in der Form: [Thema] (z.B. [Aufbruch und Neustart] oder [Stille Erkenntnisse])
5. Nach der Überschrift folgt der erzählende Text des Abschnitts
6. Die Abschnitte chronologisch vom Monatsanfang bis Monatsende ordnen

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst auf den Monat zurückblicken
- Keine Anrede, keine Grußformel — direkt in die Erzählung
- Fließender Stil mit guten Übergängen innerhalb der Abschnitte
- Die Überschriften sollen kurz und thematisch passend sein (2-4 Wörter)
- Ziehe Verbindungen zwischen den Wochen — zeige Entwicklungen und rote Fäden
- Wiederhole nicht einfach die Rückblicke nacheinander, sondern verbinde sie thematisch
- Alltägliches nur erwähnen, wenn es bedeutsam war
- Hebe Positives besonders hervor — aber verschweige Herausforderungen nicht
- Schreibe warm und persönlich, aber nicht übertrieben
- Mindestens 300 Wörter
- Sprache: Deutsch$profileStyle

WOCHENRÜCKBLICKE:
$weeksText"""

            val titlePrompt =
                """Basierend auf diesem Monatsrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen.

Rückblick:
"""

            val result =
                generateContent(
                    prompt = prompt,
                    modelName = MODEL_FLASH,
                    temperature = 0.7f,
                    maxOutputTokens = 8192,
                )

            if (result.isFailure) {
                Log.e("Retro", "Monthly AI failed: ${result.exceptionOrNull()?.message}")
                lastFailureCount++
                continue
            }

            val summaryText = result.getOrThrow().trim()
            val titleResult =
                generateContent(
                    prompt = titlePrompt + summaryText.take(500),
                    modelName = MODEL_FLASH_LITE,
                    temperature = 0.6f,
                    maxOutputTokens = 50,
                )
            val title = titleResult.getOrNull()?.trim()?.take(60) ?: "Monatsrückblick"

            retroRepo.insert(
                RetrospectiveSummaryEntity(
                    type = "MONTHLY",
                    periodLabel = "$monthName $year",
                    startDate = monthStart.timeInMillis,
                    endDate = monthEnd.timeInMillis,
                    title = title,
                    summaryText = summaryText,
                    periodIndex = month + 1,
                )
            )
            generated++
            Log.d("Retro", "Monthly review saved: $monthName $year — $title")
        }
        return generated
    }

    // ── Yearly ──────────────────────────────────────────────────────────────

    private suspend fun generateMissingYearly(): Int {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        var generated = 0

        // Check only last year
        val year = currentYear - 1
        val yearStart =
            Calendar.getInstance().apply {
                set(year, Calendar.JANUARY, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val yearEnd =
            Calendar.getInstance().apply {
                set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }

        // Time gate: Dec 31 15:00 of that year must have passed
        val deadline =
            Calendar.getInstance().apply {
                set(year, Calendar.DECEMBER, 31, 15, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        if (deadline.timeInMillis > now.timeInMillis) return 0

        // Skip if already generated
        if (retroRepo.existsForPeriod("YEARLY", yearStart.timeInMillis)) return 0

        // Get monthly reviews for this year — need at least 1
        val monthlyReviews =
            retroRepo.getByTypeAndRange("MONTHLY", yearStart.timeInMillis, yearEnd.timeInMillis)
        if (monthlyReviews.isEmpty()) {
            Log.d("Retro", "Year $year: no monthly reviews, skipping")
            return 0
        }

        Log.d("Retro", "Generating yearly review from ${monthlyReviews.size} monthly reviews...")

        val monthsText =
            monthlyReviews.joinToString("\n\n---\n\n") { m ->
                "[${m.periodLabel}] ${m.title}\n${m.summaryText}"
            }

        val profileStyle = getProfileStyleInstruction()
        val prompt =
            """Du bist ein Erzähler, der aus Monatsrückblicken einen natürlichen, gut lesbaren Jahresrückblick schreibt.

AUFGABE: Fasse die folgenden Monatsrückblicke zu einem strukturierten Jahresrückblick für $year zusammen.

FORMAT (bitte genau einhalten):
1. Beginne mit einer Zusammenfassung als kurze Stichpunkte (5-8 Punkte), jeweils mit "• " am Anfang
2. Dann eine Leerzeile
3. Dann der Fließtext, aufgeteilt in 4-6 thematische Abschnitte
4. Jeder Abschnitt beginnt mit einer Überschrift in der Form: [Thema] (z.B. [Der Frühling des Aufbruchs] oder [Ruhe finden])
5. Nach der Überschrift folgt der erzählende Text des Abschnitts
6. Die Abschnitte chronologisch vom Jahresanfang bis Jahresende ordnen

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst auf das Jahr zurückblicken
- Keine Anrede, keine Grußformel — direkt in die Erzählung
- Fließender Stil mit guten Übergängen innerhalb der Abschnitte
- Die Überschriften sollen kurz und thematisch passend sein (2-5 Wörter)
- Ziehe Verbindungen zwischen den Monaten — zeige Entwicklungen über das Jahr
- Erkenne die großen Themen des Jahres und ordne Ereignisse in diese Linien ein
- Alltägliches nur erwähnen, wenn es bedeutsam war
- Hebe Positives besonders hervor — aber verschweige Herausforderungen nicht
- Schließe mit einem Gedanken der nach vorne blickt
- Schreibe warm und persönlich, aber nicht übertrieben
- Mindestens 400 Wörter
- Sprache: Deutsch$profileStyle

MONATSRÜCKBLICKE:
$monthsText"""

        val titlePrompt =
            """Basierend auf diesem Jahresrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen.

Rückblick:
"""

        val result =
            generateContent(
                prompt = prompt,
                modelName = MODEL_FLASH,
                temperature = 0.7f,
                maxOutputTokens = 16384,
            )

        if (result.isFailure) {
            Log.e("Retro", "Yearly AI failed: ${result.exceptionOrNull()?.message}")
            lastFailureCount++
            return 0
        }

        val summaryText = result.getOrThrow().trim()
        val titleResult =
            generateContent(
                prompt = titlePrompt + summaryText.take(500),
                modelName = MODEL_FLASH_LITE,
                temperature = 0.6f,
                maxOutputTokens = 50,
            )
        val title = titleResult.getOrNull()?.trim()?.take(60) ?: "Jahresrückblick $year"

        retroRepo.insert(
            RetrospectiveSummaryEntity(
                type = "YEARLY",
                periodLabel = "$year",
                startDate = yearStart.timeInMillis,
                endDate = yearEnd.timeInMillis,
                title = title,
                summaryText = summaryText,
                periodIndex = year,
            )
        )
        generated++
        Log.d("Retro", "Yearly review saved: $year — $title")
        return generated
    }

    // ── Cleanup ─────────────────────────────────────────────────────────────

    private suspend fun cleanupOrphanedMonthlyReviews() {
        try {
            for (monthsBack in 1..3) {
                val (monthStart, monthEnd) = getMonthRange(monthsBack)
                val entries =
                    journalDao.getEntriesBetween(monthStart.timeInMillis, monthEnd.timeInMillis)
                if (
                    entries.isEmpty() &&
                        retroRepo.existsForPeriod("MONTHLY", monthStart.timeInMillis)
                ) {
                    retroRepo.deleteByTypeAndRange(
                        "MONTHLY",
                        monthStart.timeInMillis,
                        monthEnd.timeInMillis,
                    )
                    Log.d("Retro", "Cleaned up orphaned monthly review (no diary entries in month)")
                }
            }
        } catch (e: Exception) {
            Log.e("Retro", "Cleanup failed: ${e.message}")
        }
    }

    // ── Profile style ───────────────────────────────────────────────────────

    private fun getProfileStyleInstruction(): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return when (scenario) {
            0 ->
                "\n- Schreibe im Stil einer zusammenfassenden Erzählung — fasse die wichtigsten Ereignisse und Gefühle zusammen, wie ein persönlicher Chronist"
            1 ->
                "\n- Schreibe im Stil eines Lebensberaters — hebe hervor was gut lief, was verbessert werden kann, und gib dem Leser das Gefühl von Klarheit und Ordnung"
            2 ->
                "\n- Schreibe im Stil einer tiefgründigen Selbsterkenntnis-Erzählung — decke Denkmuster, wiederkehrende Gefühle und verborgene Stärken auf"
            3 ->
                "\n- Schreibe im Stil eines motivierenden Ziel-Begleiters — zeige Fortschritte bei persönlichen Zielen auf und ermutige weiterzumachen"
            4 -> {
                val custom = encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                if (custom.isNotBlank())
                    "\n- Schreibe den Rückblick mit folgendem persönlichen Fokus des Benutzers: \"$custom\""
                else ""
            }
            else -> ""
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns (Monday 00:00, Sunday 23:59) for N weeks back. Forces firstDayOfWeek=MONDAY to avoid
     * Calendar bugs on devices with US locale (firstDayOfWeek=SUNDAY).
     */
    private fun getWeekRange(weeksBack: Int): Pair<Calendar, Calendar> {
        val start =
            Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                add(Calendar.WEEK_OF_YEAR, -weeksBack)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val end =
            Calendar.getInstance().apply {
                timeInMillis = start.timeInMillis
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        return start to end
    }

    /** Returns (1st of month 00:00, last day 23:59) for N months back. */
    private fun getMonthRange(monthsBack: Int): Pair<Calendar, Calendar> {
        val start =
            Calendar.getInstance().apply {
                add(Calendar.MONTH, -monthsBack)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val end =
            Calendar.getInstance().apply {
                timeInMillis = start.timeInMillis
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        return start to end
    }
}
