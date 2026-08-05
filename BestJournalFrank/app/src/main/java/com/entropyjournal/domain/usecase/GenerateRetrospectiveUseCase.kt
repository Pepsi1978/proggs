package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import android.util.Log
import com.entropyjournal.data.local.dao.JournalEntryDao
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity
import com.entropyjournal.data.prefs.CustomAnalysesStore
import com.entropyjournal.data.remote.ai.AiGateway
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.data.repository.RetrospectiveRepository
import com.entropyjournal.util.Constants
import com.entropyjournal.util.stripEmDashes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class GenerateRetrospectiveUseCase
@Inject
constructor(
    private val journalDao: JournalEntryDao,
    private val retroRepo: RetrospectiveRepository,
    private val geminiApi: AiGateway,
    private val encryptedPrefs: SharedPreferences,
) {
    companion object {
        /** Max concurrent AI API calls to avoid rate limiting */
        private const val MAX_PARALLEL = 3

        /** Das gewohnte Fenster fuer den automatischen Lauf beim Oeffnen der App. */
        private const val DEFAULT_WEEKS_BACK = 8
        private const val DEFAULT_MONTHS_BACK = 3

        /** Obergrenzen fuer den Lauf ueber die ganze Historie (Aktualisieren-Knopf). */
        private const val MAX_HISTORY_WEEKS = 520
        private const val MAX_HISTORY_MONTHS = 120
        private const val MAX_HISTORY_YEARS = 10
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Liefert die aktuelle "Generations-Signatur" — eine Kombination aus:
     *  - aktivem Profil-Index (PREF_DASHBOARD_SCENARIO)
     *  - Verbose-Schalter (PREF_VERBOSE_DASHBOARD)
     *  - Zeitpunkt der letzten Custom-Prompt-Speicherung (custom_prompt_saved_at)
     *
     * Wenn diese Signatur sich aendert, muessen ALLE bestehenden Rueckblicke
     * geloescht und neu generiert werden — sonst bleiben Eintraege aus einem
     * vorherigen Profil/Variant in der DB.
     */
    fun currentSignature(): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        val verbose = encryptedPrefs.getBoolean(Constants.PREF_VERBOSE_DASHBOARD, false)
        val customPromptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
        return "$scenario|$verbose|$customPromptSavedAt"
    }

    /**
     * Schreibt die aktuelle Signatur in die EncryptedPrefs — wird vom Aufrufer
     * NACH erfolgreichem regenerateAll()/generateMissing() aufgerufen, damit
     * der naechste ViewModel-Start keinen erneuten Regen-Lauf ausloest.
     */
    fun persistCurrentSignature() {
        encryptedPrefs
            .edit()
            .putString(Constants.PREF_RETRO_LAST_GENERATED_SIGNATURE, currentSignature())
            .apply()
    }

    /**
     * Liefert das vom Benutzer in den Einstellungen gewaehlte Gemini-Modell.
     * ALLE KI-Aufrufe in diesem UseCase nutzen ausschliesslich dieses Modell —
     * keine hartcodierten Modelle mehr fuer einzelne Aufgaben.
     */
    private fun selectedModel(): String =
        Constants.resolveValidModel(encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL))

    // ThreadLocal to ensure thread-safe date formatting during parallel generation
    private val dfLabel: SimpleDateFormat
        get() = SimpleDateFormat("dd.MM.", Locale.GERMANY)

    private val dfFull: SimpleDateFormat
        get() = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

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
        if (!geminiApi.isConfigured())
            return Result.failure(IllegalStateException(geminiApi.configurationError()))
        return try {
            val request =
                GeminiRequestBuilder.build(
                    userText = prompt,
                    temperature = temperature,
                    maxOutputTokens = maxOutputTokens,
                )
            val response =
                geminiApi.generateContent(model = modelName, apiKey = apiKey, request = request)
            val text = response.extractText()?.stripEmDashes()
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
    /** Number of AI failures in last generateMissing() run — thread-safe for parallel generation */
    private val _lastFailureCount = AtomicInteger(0)
    val lastFailureCount: Int
        get() = _lastFailureCount.get()

    /**
     * @param fullHistory bei true reicht die Suche bis zum aeltesten Tagebucheintrag zurueck statt
     *        nur ueber die letzten Wochen und Monate. Das braucht der Aktualisieren-Knopf im
     *        Rueckblick: nach dem Einspielen eines Backups sind die Eintraege aelter als das
     *        normale Fenster, und ohne diesen Weg entstuende zu ihnen nie ein Rueckblick.
     */
    suspend fun generateMissing(fullHistory: Boolean = false): Int {
        _lastFailureCount.set(0)
        // Cleanup: remove monthly reviews for months without actual diary entries
        cleanupOrphanedMonthlyReviews()
        val oldest = if (fullHistory) journalDao.getOldestEntryTimestamp() else null
        var generated = 0
        generated += generateMissingWeekly(weeksBackLimit(oldest))
        generated += generateMissingMonthly(monthsBackLimit(oldest))
        generated += generateMissingYearly(oldestYear(oldest))
        if (generated == 0 && lastFailureCount > 0) {
            throw Exception(
                "Alle $lastFailureCount KI-Anfragen fehlgeschlagen. Bitte Internetverbindung prüfen."
            )
        }
        return generated
    }

    /**
     * Wipes every existing weekly/monthly/yearly retrospective summary and regenerates
     * them from scratch using the currently active profile + verbose setting. Called
     * when the user switches dashboard profile or toggles "Längere Version" so every
     * existing retro reflects the new style — not only future ones.
     */
    suspend fun regenerateAll(): Int {
        retroRepo.deleteAll()
        val count = generateMissing()
        persistCurrentSignature()
        return count
    }

    // ── Weekly ──────────────────────────────────────────────────────────────

    private data class WeeklyTask(
        val weekStart: Calendar,
        val weekEnd: Calendar,
        val entriesText: String,
    )

    private suspend fun generateMissingWeekly(maxWeeksBack: Int): Int {
        val now = Calendar.getInstance()

        // Phase 1: Collect all weeks that need generation
        val tasks = mutableListOf<WeeklyTask>()
        for (weeksBack in 0..maxWeeksBack) {
            val (weekStart, weekEnd) = getWeekRange(weeksBack)
            val deadline =
                Calendar.getInstance().apply {
                    timeInMillis = weekEnd.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            if (deadline.timeInMillis > now.timeInMillis) continue
            if (retroRepo.existsForPeriod("WEEKLY", weekStart.timeInMillis)) continue
            val entries = journalDao.getEntriesBetween(weekStart.timeInMillis, weekEnd.timeInMillis)
            if (entries.size < 2) {
                Log.d("Retro", "Week ${weeksBack}w ago: only ${entries.size} entries, skipping")
                continue
            }
            val entriesText =
                entries.joinToString("\n\n---\n\n") { entry ->
                    val date = dfFull.format(entry.timestamp)
                    "[$date]\n${entry.displayText}"
                }
            tasks.add(WeeklyTask(weekStart, weekEnd, entriesText))
        }

        if (tasks.isEmpty()) return 0
        Log.d("Retro", "Generating ${tasks.size} weekly reviews in parallel...")

        // Phase 2: Generate all in parallel (max MAX_PARALLEL concurrent API calls)
        val semaphore = Semaphore(MAX_PARALLEL)
        val generated = AtomicInteger(0)
        val profileStyle = getProfileStyleInstruction()

        coroutineScope {
            tasks
                .map { task ->
                    async {
                        semaphore.withPermit {
                            generateSingleWeekly(task, profileStyle)?.let { entity ->
                                // Defense in depth: if the caller (ViewModel) has
                                // already cancelled this job (e.g. user toggled the
                                // verbose switch twice in quick succession), do NOT
                                // insert stale data into the fresh DB state.
                                currentCoroutineContext().ensureActive()
                                retroRepo.upsertForPeriod(entity)
                                generated.incrementAndGet()
                                Log.d(
                                    "Retro",
                                    "Weekly review saved: ${entity.periodLabel} — ${entity.title}",
                                )
                            }
                        }
                    }
                }
                .awaitAll()
        }

        return generated.get()
    }

    private suspend fun generateSingleWeekly(
        task: WeeklyTask,
        profileStyle: String,
    ): RetrospectiveSummaryEntity? {
        try {
            val verbose = encryptedPrefs.getBoolean(Constants.PREF_VERBOSE_DASHBOARD, false)
            val prompt =
                if (verbose) buildWeeklyPromptVerbose(task, profileStyle)
                else buildWeeklyPromptStandard(task, profileStyle)

            val result =
                generateContent(
                    prompt = prompt,
                    modelName = selectedModel(),
                    temperature = 0.7f,
                    maxOutputTokens = 8192,
                )

            if (result.isFailure) {
                Log.e("Retro", "Weekly AI failed: ${result.exceptionOrNull()?.message}")
                _lastFailureCount.incrementAndGet()
                return null
            }

            val summaryText = result.getOrThrow().trim()

            val titlePrompt =
                """Basierend auf diesem Wochenrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen. Keine Gedankenstriche (—).

Rückblick:
${summaryText.take(500)}"""

            val titleResult =
                generateContent(
                    prompt = titlePrompt,
                    modelName = selectedModel(),
                    temperature = 0.6f,
                    maxOutputTokens = 50,
                )
            val title = titleResult.getOrNull()?.trim()?.take(60) ?: "Wochenrückblick"

            // Use endDate to determine week number — consistent with month grouping by endDate
            val weekOfMonth =
                task.weekEnd.get(Calendar.DAY_OF_MONTH).let { day ->
                    when {
                        day <= 7 -> 1
                        day <= 14 -> 2
                        day <= 21 -> 3
                        else -> 4
                    }
                }

            val label =
                "Woche $weekOfMonth (${dfLabel.format(task.weekStart.time)} - ${dfLabel.format(task.weekEnd.time)})"

            return RetrospectiveSummaryEntity(
                type = "WEEKLY",
                periodLabel = label,
                startDate = task.weekStart.timeInMillis,
                endDate = task.weekEnd.timeInMillis,
                title = title,
                summaryText = summaryText,
                periodIndex = weekOfMonth,
            )
        } catch (e: Exception) {
            Log.e("Retro", "Weekly generation failed: ${e.message}")
            _lastFailureCount.incrementAndGet()
            return null
        }
    }

    // ── Monthly ─────────────────────────────────────────────────────────────

    private data class MonthlyTask(
        val monthStart: Calendar,
        val monthEnd: Calendar,
        val weeksText: String,
        // B1 — Original-Eintraege des Monats kompakt (Datum + Snippet pro Eintrag).
        // Werden zusaetzlich zum weeksText in den Monatsretro-Prompt eingebaut, damit
        // der Profil-Bezug nicht nur aus den (bereits zusammengefassten) Wochenretros
        // gezogen wird sondern direkt aus den Original-Eintraegen sichtbar bleibt.
        val monthEntriesCompact: String,
        val monthName: String,
        val month: Int,
        val year: Int,
    )

    private suspend fun generateMissingMonthly(maxMonthsBack: Int): Int {
        val now = Calendar.getInstance()

        // Phase 1: Collect all months that need generation
        val tasks = mutableListOf<MonthlyTask>()
        for (monthsBack in 0..maxMonthsBack) {
            val (monthStart, monthEnd) = getMonthRange(monthsBack)
            val deadline =
                Calendar.getInstance().apply {
                    timeInMillis = monthEnd.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            if (deadline.timeInMillis > now.timeInMillis) continue
            if (retroRepo.existsForPeriod("MONTHLY", monthStart.timeInMillis)) continue
            val monthEntries =
                journalDao.getEntriesBetween(monthStart.timeInMillis, monthEnd.timeInMillis)
            if (monthEntries.isEmpty()) {
                Log.d("Retro", "Month ${monthsBack}m ago: no diary entries in this month, skipping")
                continue
            }
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
            val weeksText =
                weeklyReviews.joinToString("\n\n---\n\n") { week ->
                    "[${week.periodLabel}] ${week.title}\n${week.summaryText}"
                }
            // B1 — Kompakte Liste der Original-Eintraege des Monats. Bei sehr vielen
            // Eintraegen (>40) werden die ersten 40 genommen, der Rest mit Hinweis
            // unten erwaehnt — verhindert Token-Eskalation.
            val compactDf = SimpleDateFormat("dd.MM.", Locale.GERMANY)
            val maxCompactEntries = 40
            val compactList =
                monthEntries.take(maxCompactEntries).joinToString("\n") { e ->
                    val date = compactDf.format(java.util.Date(e.timestamp))
                    val text = (e.displayText.ifBlank { e.rawText }).take(150)
                    "[$date] $text"
                }
            val monthEntriesCompact =
                if (monthEntries.size > maxCompactEntries)
                    compactList + "\n(+ ${monthEntries.size - maxCompactEntries} weitere Einträge)"
                else compactList
            val month = monthStart.get(Calendar.MONTH)
            val year = monthStart.get(Calendar.YEAR)
            tasks.add(
                MonthlyTask(
                    monthStart,
                    monthEnd,
                    weeksText,
                    monthEntriesCompact,
                    monthNames[month],
                    month,
                    year,
                )
            )
        }

        if (tasks.isEmpty()) return 0
        Log.d("Retro", "Generating ${tasks.size} monthly reviews in parallel...")

        // Phase 2: Generate all in parallel
        val semaphore = Semaphore(MAX_PARALLEL)
        val generated = AtomicInteger(0)
        val profileStyle = getProfileStyleInstruction()

        coroutineScope {
            tasks
                .map { task ->
                    async {
                        semaphore.withPermit {
                            generateSingleMonthly(task, profileStyle)?.let { entity ->
                                currentCoroutineContext().ensureActive()
                                retroRepo.upsertForPeriod(entity)
                                generated.incrementAndGet()
                                Log.d(
                                    "Retro",
                                    "Monthly review saved: ${entity.periodLabel} — ${entity.title}",
                                )
                            }
                        }
                    }
                }
                .awaitAll()
        }

        return generated.get()
    }

    private suspend fun generateSingleMonthly(
        task: MonthlyTask,
        profileStyle: String,
    ): RetrospectiveSummaryEntity? {
        try {
            val verbose = encryptedPrefs.getBoolean(Constants.PREF_VERBOSE_DASHBOARD, false)
            val prompt =
                if (verbose) buildMonthlyPromptVerbose(task, profileStyle)
                else buildMonthlyPromptStandard(task, profileStyle)

            val result =
                generateContent(
                    prompt = prompt,
                    modelName = selectedModel(),
                    temperature = 0.7f,
                    maxOutputTokens = 8192,
                )

            if (result.isFailure) {
                Log.e("Retro", "Monthly AI failed: ${result.exceptionOrNull()?.message}")
                _lastFailureCount.incrementAndGet()
                return null
            }

            val summaryText = result.getOrThrow().trim()

            val titlePrompt =
                """Basierend auf diesem Monatsrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen. Keine Gedankenstriche (—).

Rückblick:
${summaryText.take(500)}"""

            val titleResult =
                generateContent(
                    prompt = titlePrompt,
                    modelName = selectedModel(),
                    temperature = 0.6f,
                    maxOutputTokens = 50,
                )
            val title = titleResult.getOrNull()?.trim()?.take(60) ?: "Monatsrückblick"

            return RetrospectiveSummaryEntity(
                type = "MONTHLY",
                periodLabel = "${task.monthName} ${task.year}",
                startDate = task.monthStart.timeInMillis,
                endDate = task.monthEnd.timeInMillis,
                title = title,
                summaryText = summaryText,
                periodIndex = task.month + 1,
            )
        } catch (e: Exception) {
            Log.e("Retro", "Monthly generation failed: ${e.message}")
            _lastFailureCount.incrementAndGet()
            return null
        }
    }

    // ── Yearly ──────────────────────────────────────────────────────────────

    /**
     * Erzeugt alle noch fehlenden Jahresrueckblicke von [oldestYear] bis zum laufenden Jahr.
     * Voraussetzung sind mindestens 2 fertige Monatsrueckblicke pro Jahr; ein einzelner Monat
     * reicht nicht fuer einen aussagekraeftigen Jahresueberblick.
     */
    private suspend fun generateMissingYearly(oldestYear: Int): Int {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        var generated = 0
        for (candidate in currentYear downTo minOf(oldestYear, currentYear)) {
            val cStart =
                Calendar.getInstance().apply {
                    set(candidate, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            val cEnd =
                Calendar.getInstance().apply {
                    set(candidate, Calendar.DECEMBER, 31, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            val cDeadline =
                Calendar.getInstance().apply {
                    set(candidate, Calendar.DECEMBER, 31, 15, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            if (cDeadline.timeInMillis > now.timeInMillis) continue
            if (retroRepo.existsForPeriod("YEARLY", cStart.timeInMillis)) continue
            val cReviews =
                retroRepo.getByTypeAndRange("MONTHLY", cStart.timeInMillis, cEnd.timeInMillis)
            if (cReviews.size < 2) {
                Log.d("Retro", "Year $candidate: ${cReviews.size} monthly reviews, need >= 2, skipping")
                continue
            }
            generated += generateSingleYearly(candidate, cStart, cEnd, cReviews)
        }
        return generated
    }

    private suspend fun generateSingleYearly(
        year: Int,
        yearStart: Calendar,
        yearEnd: Calendar,
        monthlyReviews: List<RetrospectiveSummaryEntity>,
    ): Int {
        Log.d("Retro", "Generating yearly review from ${monthlyReviews.size} monthly reviews...")

        val monthsText =
            monthlyReviews.joinToString("\n\n---\n\n") { m ->
                "[${m.periodLabel}] ${m.title}\n${m.summaryText}"
            }

        val profileStyle = getProfileStyleInstruction()
        val verbose = encryptedPrefs.getBoolean(Constants.PREF_VERBOSE_DASHBOARD, false)
        val prompt =
            if (verbose) buildYearlyPromptVerbose(year, monthsText, profileStyle)
            else buildYearlyPromptStandard(year, monthsText, profileStyle)

        val result =
            generateContent(
                prompt = prompt,
                modelName = selectedModel(),
                temperature = 0.7f,
                maxOutputTokens = 16384,
            )

        if (result.isFailure) {
            Log.e("Retro", "Yearly AI failed: ${result.exceptionOrNull()?.message}")
            _lastFailureCount.incrementAndGet()
            return 0
        }

        val summaryText = result.getOrThrow().trim()

        val titlePrompt =
            """Basierend auf diesem Jahresrückblick, gib einen kurzen, emotionalen Titel (max 6 Wörter).
Nur den Titel ausgeben, nichts anderes. Keine Anführungszeichen. Keine Gedankenstriche (—).

Rückblick:
${summaryText.take(500)}"""

        val titleResult =
            generateContent(
                prompt = titlePrompt,
                modelName = selectedModel(),
                temperature = 0.6f,
                maxOutputTokens = 50,
            )
        val title = titleResult.getOrNull()?.trim()?.take(60) ?: "Jahresrückblick $year"

        currentCoroutineContext().ensureActive()
        retroRepo.upsertForPeriod(
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
        Log.d("Retro", "Yearly review saved: $year — $title")
        return 1
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

    // ── Retrospective prompt builders (6 total: 3 standard + 3 verbose) ─────
    //
    // Mirrors the dashboard's 10-prompt architecture: instead of a runtime
    // suffix that Gemini can ignore, each period (weekly/monthly/yearly) has
    // two fully separate prompt templates. `generateSingle{Weekly,Monthly,
    // Yearly}` picks one of the two based on PREF_VERBOSE_DASHBOARD.

    private fun buildWeeklyPromptStandard(task: WeeklyTask, profileStyle: String): String =
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

${Constants.NO_EM_DASH_RULE}

EINTRÄGE DER WOCHE:
${task.entriesText}"""

    private fun buildWeeklyPromptVerbose(task: WeeklyTask, profileStyle: String): String =
        """AUSFÜHRLICHE VERSION (PFLICHT BEACHTEN): Der Benutzer hat den Schalter "Längere Version" aktiviert. Liefere etwa DOPPELT so viel Text wie im Standard. Alle Zahlen- und Längenvorgaben unten sind bereits entsprechend gesetzt.

Du bist ein Erzähler, der aus Tagebucheinträgen einen ausführlichen, gut lesbaren Wochenrückblick schreibt.

AUFGABE: Fasse die folgenden Tagebucheinträge zu einem ausführlichen, strukturierten Wochenrückblick zusammen, mit mehr Kontext, konkreten Zitaten aus den Einträgen und tieferen Beobachtungen als üblich.

FORMAT (bitte genau einhalten):
1. Beginne mit einer Zusammenfassung als Stichpunkte (6-10 Punkte), jeweils mit "• " am Anfang
2. Dann eine Leerzeile
3. Dann der Fließtext, aufgeteilt in 4-7 thematische Abschnitte
4. Jeder Abschnitt beginnt mit einer Überschrift in der Form: [Thema] (z.B. [Neue Begegnungen] oder [Kleine Siege])
5. Nach der Überschrift folgt ein ausführlicher erzählender Text mit mehr Details, konkreten Zitaten/Formulierungen aus den Einträgen und persönlichen Beobachtungen
6. Die Abschnitte chronologisch vom Wochenanfang bis Wochenende ordnen

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst zurückblicken
- Keine Anrede, keine Grußformel — direkt in die Erzählung
- Fließender Stil mit guten Übergängen innerhalb der Abschnitte, jeder Abschnitt deutlich ausführlicher als im Standard
- Die Überschriften sollen kurz und thematisch passend sein (2-4 Wörter)
- Erwähne konkrete Ereignisse, Gefühle, Erkenntnisse — möglichst mit Zitaten oder Formulierungen aus den Einträgen
- Alltägliches darf ausführlicher erwähnt werden, solange es Kontext liefert
- Hebe Positives besonders hervor — verschweige Herausforderungen nicht, ordne sie ausführlich ein
- Schreibe warm, persönlich und detailreich
- MINDESTENS 400 Wörter insgesamt, lieber mehr
- Sprache: Deutsch$profileStyle

${Constants.NO_EM_DASH_RULE}

EINTRÄGE DER WOCHE:
${task.entriesText}"""

    private fun buildMonthlyPromptStandard(task: MonthlyTask, profileStyle: String): String =
        """Du bist ein Erzähler, der aus Wochenrückblicken einen natürlichen, gut lesbaren Monatsrückblick schreibt.

AUFGABE: Fasse die folgenden Wochenrückblicke zu einem strukturierten Monatsrückblick für ${task.monthName} ${task.year} zusammen.

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

${Constants.NO_EM_DASH_RULE}

WOCHENRÜCKBLICKE:
${task.weeksText}

ORIGINAL-TAGEBUCHEINTRÄGE DIESES MONATS (kompakt, als zweite Quelle für direkten Profil-Bezug):
${task.monthEntriesCompact}"""

    private fun buildMonthlyPromptVerbose(task: MonthlyTask, profileStyle: String): String =
        """AUSFÜHRLICHE VERSION (PFLICHT BEACHTEN): Der Benutzer hat den Schalter "Längere Version" aktiviert. Liefere etwa DOPPELT so viel Text wie im Standard. Alle Zahlen- und Längenvorgaben unten sind bereits entsprechend gesetzt.

Du bist ein Erzähler, der aus Wochenrückblicken einen ausführlichen, tiefen Monatsrückblick schreibt.

AUFGABE: Fasse die folgenden Wochenrückblicke zu einem ausführlichen, strukturierten Monatsrückblick für ${task.monthName} ${task.year} zusammen, mit mehr Details, thematischen Verbindungen und tieferen Einsichten als üblich.

FORMAT (bitte genau einhalten):
1. Beginne mit einer Zusammenfassung als Stichpunkte (8-12 Punkte), jeweils mit "• " am Anfang
2. Dann eine Leerzeile
3. Dann der Fließtext, aufgeteilt in 5-8 thematische Abschnitte
4. Jeder Abschnitt beginnt mit einer Überschrift in der Form: [Thema] (z.B. [Aufbruch und Neustart] oder [Stille Erkenntnisse])
5. Nach der Überschrift folgt ein ausführlicher erzählender Text mit mehr Kontext, Zitaten aus den Wochenrückblicken und persönlichen Entwicklungslinien
6. Die Abschnitte chronologisch vom Monatsanfang bis Monatsende ordnen

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst auf den Monat zurückblicken
- Keine Anrede, keine Grußformel — direkt in die Erzählung
- Fließender Stil mit tiefen Übergängen innerhalb der Abschnitte, jeder Abschnitt deutlich ausführlicher als im Standard
- Die Überschriften sollen kurz und thematisch passend sein (2-4 Wörter)
- Ziehe ausführliche Verbindungen zwischen den Wochen, zeige Entwicklungen und rote Fäden mit konkreten Beispielen
- Wiederhole nicht einfach die Rückblicke nacheinander, sondern verbinde sie thematisch und zeige das größere Bild
- Alltägliches darf ausführlicher erwähnt werden, solange es Kontext für Muster liefert
- Hebe Positives besonders hervor — ordne Herausforderungen ausführlich ein
- Schreibe warm, persönlich und detailreich
- MINDESTENS 600 Wörter insgesamt, lieber mehr
- Sprache: Deutsch$profileStyle

${Constants.NO_EM_DASH_RULE}

WOCHENRÜCKBLICKE:
${task.weeksText}

ORIGINAL-TAGEBUCHEINTRÄGE DIESES MONATS (kompakt, als zweite Quelle für direkten Profil-Bezug):
${task.monthEntriesCompact}"""

    private fun buildYearlyPromptStandard(year: Int, monthsText: String, profileStyle: String): String =
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

${Constants.NO_EM_DASH_RULE}

MONATSRÜCKBLICKE:
$monthsText"""

    private fun buildYearlyPromptVerbose(year: Int, monthsText: String, profileStyle: String): String =
        """AUSFÜHRLICHE VERSION (PFLICHT BEACHTEN): Der Benutzer hat den Schalter "Längere Version" aktiviert. Liefere etwa DOPPELT so viel Text wie im Standard. Alle Zahlen- und Längenvorgaben unten sind bereits entsprechend gesetzt.

Du bist ein Erzähler, der aus Monatsrückblicken einen ausführlichen, tiefen Jahresrückblick schreibt.

AUFGABE: Fasse die folgenden Monatsrückblicke zu einem ausführlichen, strukturierten Jahresrückblick für $year zusammen, mit tiefen thematischen Linien, konkreten Zitaten aus den Monatsrückblicken und einem umfassenden Blick auf das Jahr.

FORMAT (bitte genau einhalten):
1. Beginne mit einer Zusammenfassung als Stichpunkte (10-16 Punkte), jeweils mit "• " am Anfang
2. Dann eine Leerzeile
3. Dann der Fließtext, aufgeteilt in 7-12 thematische Abschnitte
4. Jeder Abschnitt beginnt mit einer Überschrift in der Form: [Thema] (z.B. [Der Frühling des Aufbruchs] oder [Ruhe finden])
5. Nach der Überschrift folgt ein ausführlicher erzählender Text mit konkreten Beispielen, Zitaten aus den Monatsrückblicken und persönlicher Einordnung
6. Die Abschnitte chronologisch vom Jahresanfang bis Jahresende ordnen

REGELN:
- Schreibe in der Du-Form, als würde das Tagebuch selbst auf das Jahr zurückblicken
- Keine Anrede, keine Grußformel — direkt in die Erzählung
- Fließender Stil mit tiefen Übergängen innerhalb der Abschnitte, jeder Abschnitt deutlich ausführlicher als im Standard
- Die Überschriften sollen kurz und thematisch passend sein (2-5 Wörter)
- Ziehe ausführliche Verbindungen zwischen den Monaten — zeige Entwicklungen, Wendepunkte und rote Fäden über das Jahr mit konkreten Beispielen
- Erkenne die großen Themen des Jahres und ordne Ereignisse ausführlich in diese Linien ein
- Alltägliches darf erwähnt werden, wenn es zu einem Muster gehört
- Hebe Positives besonders hervor — ordne Herausforderungen ausführlich ein
- Schließe mit einem ausführlichen Gedanken der nach vorne blickt
- Schreibe warm, persönlich und detailreich
- MINDESTENS 800 Wörter insgesamt, lieber mehr
- Sprache: Deutsch$profileStyle

${Constants.NO_EM_DASH_RULE}

MONATSRÜCKBLICKE:
$monthsText"""

    // ── Profile style ───────────────────────────────────────────────────────

    private fun getProfileStyleInstruction(): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return when {
            scenario == 0 ->
                "\n- Schreibe im Stil einer zusammenfassenden Erzählung — fasse die wichtigsten Ereignisse und Gefühle zusammen, wie ein persönlicher Chronist"
            scenario == 1 ->
                "\n- Schreibe im Stil eines Lebensberaters — hebe hervor was gut lief, was verbessert werden kann, und gib dem Leser das Gefühl von Klarheit und Ordnung"
            scenario == 2 ->
                "\n- Schreibe im Stil einer tiefgründigen Selbsterkenntnis-Erzählung — decke Denkmuster, wiederkehrende Gefühle und verborgene Stärken auf"
            scenario == 3 ->
                "\n- Schreibe im Stil eines motivierenden Ziel-Begleiters — zeige Fortschritte bei persönlichen Zielen auf und ermutige weiterzumachen"
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX -> {
                val custom =
                    CustomAnalysesStore.activePromptOrEmpty(encryptedPrefs, scenario)
                if (custom.isNotBlank())
                    """

PROFIL-FOKUS — KERN-REGEL (PFLICHT BEACHTEN, GLEICHWERTIG ZU FORMAT UND STIL):
Der Benutzer hat sich ein eigenes Profil gewählt. Sein Fokus ist:
"$custom"

Dieses Profil ist KEIN reiner Stil-Hinweis, sondern der inhaltliche Taktgeber für den ganzen Rückblick. Halte dich an diese vier Regeln:

1) MINDESTENS 60 PROZENT des Rückblicks (Zusammenfassung-Stichpunkte UND Fließtext) müssen sich klar erkennbar mit dem Profil-Fokus beschäftigen — als Inhalt, nicht nur als Erwähnung. Wenn nötig, ordne Ereignisse aus den Einträgen explizit dem Profil-Thema zu, statt sie nur chronologisch aufzulisten.

2) Mindestens EIN thematischer Abschnitt muss sich vollständig dem Profil-Fokus widmen, mit eigener Überschrift die zum Fokus passt.

3) Drücke den Profil-Bezug VARIANTENREICH aus. Nutze Synonyme, verwandte Begriffe, thematische Nachbarschaften und Umschreibungen — wiederhole NICHT mechanisch die exakten Wörter aus dem Fokus. Beispiel "Ziele": Vorhaben, Wünsche, Ambitionen, Kurs, Entwicklungs-Schritte. Beispiel "Sport": Bewegung, Training, körperliche Aktivität, Ausdauer, Fitness, Gesundheit. Der Bezug muss spürbar sein, nicht buchstabengetreu.

4) Beobachtungen aus den Einträgen, die KEINEN Bezug zum Fokus haben, dürfen erwähnt werden — aber kompakt, nicht ausschweifend. Lass lieber neutrale Alltagsbeobachtungen weg, wenn sie den Fokus verdrängen würden."""
                else ""
            }
            else -> ""
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Wie viele Wochen zurueck gesucht wird. Ohne [oldest] bleibt es beim gewohnten Fenster;
     * mit dem aeltesten Eintrag reicht die Suche bis zu dessen Woche, gedeckelt bei
     * [MAX_HISTORY_WEEKS], damit ein einzelner sehr alter Eintrag keinen endlosen Lauf ausloest.
     */
    private fun weeksBackLimit(oldest: Long?): Int {
        if (oldest == null) return DEFAULT_WEEKS_BACK
        val elapsed = System.currentTimeMillis() - oldest
        val weeks = (elapsed / WEEK_MS).toInt() + 1
        return weeks.coerceIn(DEFAULT_WEEKS_BACK, MAX_HISTORY_WEEKS)
    }

    private fun monthsBackLimit(oldest: Long?): Int {
        if (oldest == null) return DEFAULT_MONTHS_BACK
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = oldest }
        val months =
            (now.get(Calendar.YEAR) - then.get(Calendar.YEAR)) * 12 +
                (now.get(Calendar.MONTH) - then.get(Calendar.MONTH))
        return months.coerceIn(DEFAULT_MONTHS_BACK, MAX_HISTORY_MONTHS)
    }

    private fun oldestYear(oldest: Long?): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (oldest == null) return currentYear - 1
        val year = Calendar.getInstance().apply { timeInMillis = oldest }.get(Calendar.YEAR)
        return year.coerceAtLeast(currentYear - MAX_HISTORY_YEARS)
    }

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
