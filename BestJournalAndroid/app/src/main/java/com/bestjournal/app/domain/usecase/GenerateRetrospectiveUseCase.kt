package com.bestjournal.app.domain.usecase

import android.content.Context
import android.util.Log
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.data.repository.RetrospectiveRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GenerateRetrospectiveUseCase
@Inject
constructor(
    private val journalDao: JournalEntryDao,
    private val retroRepo: RetrospectiveRepository,
    private val aiService: FirebaseAiService,
    @ApplicationContext private val context: Context,
) {
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

    /**
     * Main entry point: generates all missing weekly, monthly and yearly reviews. Returns the
     * number of new reviews generated.
     */
    /** Number of AI failures in last generateMissing() run */
    var lastFailureCount: Int = 0
        private set

    suspend fun generateMissing(): Int {
        lastFailureCount = 0
        var generated = 0
        generated += generateMissingWeekly()
        generated += generateMissingMonthly()
        generated += generateMissingYearly()
        // If ALL attempts failed and nothing was generated, signal the error
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
                """Du bist ein begnadeter Erzähler, der aus Tagebucheinträgen einen mitreißenden, emotionalen Wochenrückblick schreibt — wie der Erzähler eines persönlichen Romans.

AUFGABE: Verwandle die folgenden Tagebucheinträge in einen ausführlichen, packenden Wochenrückblick, der sich liest wie ein Kapitel aus dem Leben des Lesers.

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst zurückblicken
- Keine Anrede, keine Grußformel, keine Höflichkeitsfloskeln — direkt rein in die Erzählung
- Erzählender, fließender Stil — kein Markdown, keine Aufzählungen, keine Überschriften
- Beginne nie mit "Diese Woche" — finde einen packenden, emotionalen Einstieg der sofort fesselt
- Erwähne konkrete Ereignisse, Gefühle, Gedanken und Erkenntnisse aus den Einträgen — mit lebendigen Details
- Beschreibe Emotionen nicht nur oberflächlich, sondern gehe in die Tiefe: Was hat sich angefühlt? Was hat es ausgelöst? Was hat es verändert?
- Baue Spannung auf — erzähle nicht chronologisch, sondern nach emotionalem Gewicht
- Alltägliches (Essen, Wetter, Routinen) nur erwähnen, wenn es emotional oder inhaltlich bedeutsam war
- Hebe Wendepunkte, Erkenntnisse und persönliches Wachstum besonders hervor — sie sind die Höhepunkte der Erzählung
- Schließe mit einem Satz, der nachklingt und zum Nachdenken anregt
- Betone besonders die positiven Dinge — Erfolge, schöne Momente, Fortschritte — aber verschweige weder Herausforderungen noch Rückschläge. Erkenntnisse aus schwierigen Momenten sind genauso wertvoll
- Mindestens 400 Wörter — lieber mehr, nimm dir Raum für die Geschichte
- Sprache: Deutsch$profileStyle

EINTRÄGE DER WOCHE:
$entriesText"""

            val titlePrompt =
                """Basierend auf diesem Wochenrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen.

Rückblick:
"""

            val result =
                aiService.generateContent(
                    prompt = prompt,
                    modelName = FirebaseAiService.MODEL_FLASH,
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
                aiService.generateContent(
                    prompt = titlePrompt + summaryText.take(500),
                    modelName = FirebaseAiService.MODEL_FLASH_LITE,
                    temperature = 0.6f,
                    maxOutputTokens = 50,
                )
            val title = titleResult.getOrNull()?.trim()?.take(60) ?: "Wochenrückblick"

            val weekOfMonth =
                weekStart.get(Calendar.DAY_OF_MONTH).let { day ->
                    when {
                        day <= 7 -> 1
                        day <= 14 -> 2
                        day <= 21 -> 3
                        else -> 4
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

            // Get weekly reviews for this month — need at least 1
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
                """Du bist ein begnadeter Erzähler, der aus Wochenrückblicken einen mitreißenden, emotionalen Monatsrückblick schreibt — wie ein Essay über einen prägenden Lebensabschnitt.

AUFGABE: Verwandle die folgenden Wochenrückblicke in einen ausführlichen, packenden Monatsrückblick für $monthName $year — eine zusammenhängende Geschichte, die den Leser emotional berührt.

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst auf den Monat zurückblicken
- Keine Anrede, keine Grußformel, keine Höflichkeitsfloskeln — direkt rein in die Erzählung
- Erzählender, fließender Stil — kein Markdown, keine Aufzählungen, keine Überschriften
- Beginne nie mit "Dieser Monat" — finde einen packenden Einstieg der den Leser sofort in den Monat hineinzieht
- Ziehe tiefe Verbindungen zwischen den Wochen — zeige Entwicklungsbögen, rote Fäden, innere Veränderungen und emotionale Reisen über den Monat hinweg
- Wiederhole nicht einfach die Rückblicke nacheinander, sondern verwebe sie kunstvoll zu einer Gesamterzählung mit Spannungsbogen
- Gehe in die emotionale Tiefe: Was hat der Monat mit dem Leser gemacht? Wie hat er sich verändert? Was hat er gelernt?
- Alltägliches nur erwähnen, wenn es emotional oder inhaltlich bedeutsam war
- Hebe Wendepunkte, tiefe Erkenntnisse, persönliches Wachstum und wiederkehrende Muster besonders hervor
- Schließe mit einem Gedanken, der nachklingt und den Monat auf den Punkt bringt
- Betone besonders die positiven Dinge — Erfolge, schöne Momente, Fortschritte — aber verschweige weder Herausforderungen noch Rückschläge. Erkenntnisse aus schwierigen Momenten sind genauso wertvoll
- Mindestens 500 Wörter — nimm dir Raum, dieser Monat verdient eine ausführliche Geschichte
- Sprache: Deutsch$profileStyle

WOCHENRÜCKBLICKE:
$weeksText"""

            val titlePrompt =
                """Basierend auf diesem Monatsrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen.

Rückblick:
"""

            val result =
                aiService.generateContent(
                    prompt = prompt,
                    modelName = FirebaseAiService.MODEL_FLASH,
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
                aiService.generateContent(
                    prompt = titlePrompt + summaryText.take(500),
                    modelName = FirebaseAiService.MODEL_FLASH_LITE,
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

        // Check only last year (not older)
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
            """Du bist ein begnadeter Erzähler, der aus Monatsrückblicken ein episches, emotionales Jahresresümee schreibt — wie eine Autobiografie in Kurzform, die den Leser zu Tränen rührt und gleichzeitig Kraft gibt.

AUFGABE: Verwandle die folgenden Monatsrückblicke in einen ausführlichen, zutiefst persönlichen Jahresrückblick für $year — eine Geschichte über Veränderung, Wachstum und die leisen und lauten Momente eines ganzen Lebensjahres.

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst auf das Jahr zurückblicken
- Keine Anrede, keine Grußformel, keine Höflichkeitsfloskeln — direkt rein in die Erzählung
- Erzählender, fließender Stil — kein Markdown, keine Aufzählungen, keine Überschriften
- Beginne nie mit "Dieses Jahr" — finde einen kraftvollen, emotionalen Einstieg der sofort unter die Haut geht
- Erzähle das Jahr wie einen Roman: mit Spannungsbogen, Wendepunkten, stillen Momenten und großen Durchbrüchen
- Ziehe tiefe Verbindungen zwischen den Monaten — zeige wie sich Themen entwickelt, verändert und manchmal aufgelöst haben
- Wiederhole nicht einfach die Rückblicke nacheinander, sondern verwebe sie kunstvoll zu einem großen Ganzen
- Erkenne die zwei oder drei großen Lebensthemen des Jahres und erzähle sie wie Handlungsstränge
- Gehe in die emotionale Tiefe: Was hat das Jahr mit dem Leser gemacht? Wer war er am Anfang, wer ist er jetzt?
- Hebe die entscheidenden Wendepunkte, die tiefsten Erkenntnisse und das prägendste Wachstum hervor
- Schließe mit einem Absatz der nachhallt — ein Blick nach vorne, getragen von allem was hinter einem liegt
- Betone besonders die positiven Dinge — Erfolge, schöne Momente, Fortschritte — aber verschweige weder Herausforderungen noch Rückschläge. Erkenntnisse aus schwierigen Momenten sind genauso wertvoll
- Mindestens 700 Wörter — dieses Jahr verdient eine ausführliche, würdige Geschichte
- Sprache: Deutsch$profileStyle

MONATSRÜCKBLICKE:
$monthsText"""

        val titlePrompt =
            """Basierend auf diesem Jahresrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen.

Rückblick:
"""

        val result =
            aiService.generateContent(
                prompt = prompt,
                modelName = FirebaseAiService.MODEL_FLASH,
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
            aiService.generateContent(
                prompt = titlePrompt + summaryText.take(500),
                modelName = FirebaseAiService.MODEL_FLASH_LITE,
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

    // ── Profile style ───────────────────────────────────────────────────────

    /** Cached prefs instance — avoids re-creating EncryptedSharedPreferences on every call */
    private val cachedPrefs: android.content.SharedPreferences? by lazy {
        try {
            val masterKey =
                androidx.security.crypto.MasterKeys.getOrCreate(
                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                )
            androidx.security.crypto.EncryptedSharedPreferences.create(
                com.bestjournal.app.util.Constants.ENCRYPTED_PREFS_NAME,
                masterKey,
                context,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
                    .AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
                    .AES256_GCM,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun getProfileStyleInstruction(): String {
        val prefs = cachedPrefs ?: return ""
        val scenario = prefs.getInt(com.bestjournal.app.util.Constants.PREF_DASHBOARD_SCENARIO, 0)
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
                val custom =
                    prefs.getString(com.bestjournal.app.util.Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                if (custom.isNotBlank())
                    "\n- Schreibe den Rückblick mit folgendem persönlichen Fokus des Benutzers: \"$custom\""
                else ""
            }
            else -> ""
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns (Monday 00:00, Sunday 23:59) for N weeks back from now. Uses ISO week definition
     * (Monday = first day) regardless of device locale to avoid Calendar.DAY_OF_WEEK bugs on
     * devices with firstDayOfWeek=SUNDAY.
     */
    private fun getWeekRange(weeksBack: Int): Pair<Calendar, Calendar> {
        val start =
            Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY // Force ISO week regardless of locale
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
