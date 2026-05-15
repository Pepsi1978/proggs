package com.bestjournal.app.domain.usecase

import android.content.Context
import android.util.Log
import com.bestjournal.app.R
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.data.repository.RetrospectiveRepository
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.DeviceLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
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
    private val aiService: FirebaseAiService,
    @ApplicationContext private val context: Context,
) {
    // Thread-safe: get() creates a new instance per call, using the current device locale
    private val dfLabel: SimpleDateFormat
        get() {
            val locale = DeviceLocale.locale
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
            return SimpleDateFormat(pattern, locale)
        }

    private val dfFull: SimpleDateFormat
        get() {
            val locale = DeviceLocale.locale
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "yyyyMMMd")
            return SimpleDateFormat(pattern, locale)
        }

    // Localized month names from device locale (e.g., "January" for en, "1月" for ja)
    private val monthNames: Array<String>
        get() = DateFormatSymbols(DeviceLocale.locale).months

    companion object {
        /** Max concurrent AI API calls to avoid rate limiting */
        private const val MAX_PARALLEL = 3
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
     * Represents a week that qualifies for a review but is locked behind premium. Used to show
     * placeholder cards in the UI.
     */
    data class LockedWeekRange(
        val weekStart: Calendar,
        val weekEnd: Calendar,
        val periodLabel: String,
        val periodIndex: Int,
    )

    /**
     * Calculates the free review cutoff: first entry timestamp + FREE_REVIEW_PERIOD_DAYS. Returns
     * null if no entries exist yet (everything is free).
     */
    private suspend fun getFreeCutoffMillis(): Long? {
        val earliest = journalDao.getEarliestTimestamp() ?: return null
        return earliest + TimeUnit.DAYS.toMillis(Constants.FREE_REVIEW_PERIOD_DAYS.toLong())
    }

    /**
     * Checks whether a given week's review is within the free period. If cutoff is null (no entries
     * yet), everything is free.
     */
    private fun isWeekFree(weekEnd: Calendar, cutoffMillis: Long?): Boolean {
        if (cutoffMillis == null) return true
        return weekEnd.timeInMillis <= cutoffMillis
    }

    suspend fun generateMissing(isPremium: Boolean = false): Int {
        _lastFailureCount.set(0)
        // Cleanup: remove monthly reviews for months without actual diary entries
        cleanupOrphanedMonthlyReviews()
        val cutoff = if (isPremium) null else getFreeCutoffMillis()
        var generated = 0
        generated += generateMissingWeekly(isPremium, cutoff)
        if (isPremium) {
            generated += generateMissingMonthly()
            generated += generateMissingYearly()
        }
        // If ALL attempts failed and nothing was generated, signal the error
        if (generated == 0 && lastFailureCount > 0) {
            throw Exception(context.getString(R.string.error_retro_all_failed, lastFailureCount))
        }
        return generated
    }

    /**
     * Returns week ranges that have enough entries for a review but are locked behind premium
     * (outside the free 14-day window). Used for placeholder cards.
     */
    suspend fun getLockedWeekRanges(): List<LockedWeekRange> {
        val now = Calendar.getInstance()
        val locked = mutableListOf<LockedWeekRange>()

        for (weeksBack in 1..8) {
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
            // Skip weeks the free user gets for free (the N most recent)
            if (weeksBack <= Constants.FREE_WEEKLY_REVIEW_COUNT) continue
            // Skip if already generated (user may have upgraded and then downgraded)
            if (retroRepo.existsForPeriod("WEEKLY", weekStart.timeInMillis)) continue
            // Check if enough entries exist for this week
            val entries = journalDao.getEntriesBetween(weekStart.timeInMillis, weekEnd.timeInMillis)
            if (entries.size < 2) continue

            val weekOfMonth =
                weekEnd.get(Calendar.DAY_OF_MONTH).let { day ->
                    when {
                        day <= 7 -> 1
                        day <= 14 -> 2
                        day <= 21 -> 3
                        else -> 4
                    }
                }
            val label =
                context.getString(
                    R.string.retro_period_label_week,
                    weekOfMonth,
                    dfLabel.format(weekStart.time),
                    dfLabel.format(weekEnd.time),
                )
            locked.add(LockedWeekRange(weekStart, weekEnd, label, weekOfMonth))
        }
        return locked
    }

    // ── Weekly ──────────────────────────────────────────────────────────────

    private data class WeeklyTask(
        val weekStart: Calendar,
        val weekEnd: Calendar,
        val entriesText: String,
    )

    private suspend fun generateMissingWeekly(isPremium: Boolean, cutoffMillis: Long?): Int {
        val now = Calendar.getInstance()

        // Phase 1: Collect all weeks that need generation
        val tasks = mutableListOf<WeeklyTask>()
        for (weeksBack in 0..8) {
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
            // Free users get the FREE_WEEKLY_REVIEW_COUNT most recent weeks (default: 2),
            // independent of first-entry date or any other parameter.
            if (!isPremium && weeksBack > Constants.FREE_WEEKLY_REVIEW_COUNT) {
                Log.d(
                    "Retro",
                    "Week ${weeksBack}w ago: locked for free user (> ${Constants.FREE_WEEKLY_REVIEW_COUNT})",
                )
                continue
            }
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
                                retroRepo.insert(entity)
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
            val lang = DeviceLocale.promptLanguage
            val prompt =
                listOf(
                        context.getString(R.string.ai_retro_week_intro),
                        context.getString(R.string.ai_retro_week_task),
                        context.getString(R.string.ai_retro_week_format),
                        context.getString(R.string.ai_retro_week_rules),
                        profileStyle,
                        context.getString(R.string.ai_retro_week_entries_header) +
                            "\n" +
                            task.entriesText,
                    )
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n")

            val result =
                aiService.generateContent(
                    prompt = prompt,
                    modelName = FirebaseAiService.MODEL_FLASH,
                    temperature = 0.7f,
                    maxOutputTokens = 8192,
                )

            if (result.isFailure) {
                Log.e("Retro", "Weekly AI failed: ${result.exceptionOrNull()?.message}")
                _lastFailureCount.incrementAndGet()
                return null
            }

            val summaryText = result.getOrThrow().trim().replace("—", ", ")

            val titlePrompt =
                context.getString(R.string.ai_retro_week_title_prompt, lang, summaryText.take(500))

            val titleResult =
                aiService.generateContent(
                    prompt = titlePrompt,
                    modelName = FirebaseAiService.MODEL_FLASH_LITE,
                    temperature = 0.6f,
                    maxOutputTokens = 50,
                )
            val title =
                titleResult.getOrNull()?.trim()?.replace("—", ", ")?.take(60)
                    ?: context.getString(R.string.retro_fallback_week)

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
                context.getString(
                    R.string.retro_period_label_week,
                    weekOfMonth,
                    dfLabel.format(task.weekStart.time),
                    dfLabel.format(task.weekEnd.time),
                )

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

    private suspend fun generateMissingMonthly(): Int {
        val now = Calendar.getInstance()

        // Phase 1: Collect all months that need generation
        val tasks = mutableListOf<MonthlyTask>()
        for (monthsBack in 0..3) {
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
            val compactDf = SimpleDateFormat("dd.MM.", DeviceLocale.locale)
            val maxCompactEntries = 40
            val compactList =
                monthEntries.take(maxCompactEntries).joinToString("\n") { e ->
                    val date = compactDf.format(java.util.Date(e.timestamp))
                    val text = (e.displayText.ifBlank { e.rawText }).take(150)
                    "[$date] $text"
                }
            val monthEntriesCompact =
                if (monthEntries.size > maxCompactEntries)
                    compactList +
                        "\n" +
                        context.getString(
                            R.string.retro_month_more_entries,
                            monthEntries.size - maxCompactEntries,
                        )
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
                                retroRepo.insert(entity)
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
            val lang = DeviceLocale.promptLanguage
            // B1 — zweite Quelle fuer Profil-Bezug: Original-Tagebucheintraege des
            // Monats kompakt, damit der Profil-Bezug nicht erst durch die (bereits
            // zusammengefassten) Wochenrueckblicke filtern muss.
            val monthEntriesBlock =
                if (task.monthEntriesCompact.isNotBlank())
                    context.getString(R.string.retro_month_entries_header) +
                        "\n" +
                        task.monthEntriesCompact
                else ""
            val prompt =
                listOf(
                        context.getString(R.string.ai_retro_month_intro),
                        context.getString(R.string.ai_retro_month_task, task.monthName, task.year),
                        context.getString(R.string.ai_retro_month_format),
                        context.getString(R.string.ai_retro_month_rules),
                        profileStyle,
                        context.getString(R.string.ai_retro_month_entries_header) +
                            "\n" +
                            task.weeksText,
                        monthEntriesBlock,
                    )
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n")

            val result =
                aiService.generateContent(
                    prompt = prompt,
                    modelName = FirebaseAiService.MODEL_FLASH,
                    temperature = 0.7f,
                    maxOutputTokens = 8192,
                )

            if (result.isFailure) {
                Log.e("Retro", "Monthly AI failed: ${result.exceptionOrNull()?.message}")
                _lastFailureCount.incrementAndGet()
                return null
            }

            val summaryText = result.getOrThrow().trim().replace("—", ", ")

            val titlePrompt =
                context.getString(R.string.ai_retro_month_title_prompt, lang, summaryText.take(500))

            val titleResult =
                aiService.generateContent(
                    prompt = titlePrompt,
                    modelName = FirebaseAiService.MODEL_FLASH_LITE,
                    temperature = 0.6f,
                    maxOutputTokens = 50,
                )
            val title =
                titleResult.getOrNull()?.trim()?.replace("—", ", ")?.take(60)
                    ?: context.getString(R.string.retro_fallback_month)

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

    private suspend fun generateMissingYearly(): Int {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        var generated = 0

        // Pruefe sowohl das laufende Jahr (faellig ab 31.12. 15:00 dieses Jahres)
        // als auch das letzte Jahr — falls dort noch nichts existiert. Voraussetzung
        // sind mindestens 2 fertige Monatsrueckblicke pro Jahr; ein einzelner Monat
        // reicht nicht fuer einen aussagekraeftigen Jahresueberblick.
        var year = -1
        var yearStart: Calendar = Calendar.getInstance()
        var yearEnd: Calendar = Calendar.getInstance()
        var monthlyReviews: List<RetrospectiveSummaryEntity> = emptyList()
        var picked = false
        for (candidate in listOf(currentYear, currentYear - 1)) {
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
            year = candidate
            yearStart = cStart
            yearEnd = cEnd
            monthlyReviews = cReviews
            picked = true
            break
        }
        if (!picked) return 0

        Log.d("Retro", "Generating yearly review from ${monthlyReviews.size} monthly reviews...")

        val monthsText =
            monthlyReviews.joinToString("\n\n---\n\n") { m ->
                "[${m.periodLabel}] ${m.title}\n${m.summaryText}"
            }

        val profileStyle = getProfileStyleInstruction()
        val lang = DeviceLocale.promptLanguage
        val prompt =
            listOf(
                    context.getString(R.string.ai_retro_year_intro),
                    context.getString(R.string.ai_retro_year_task, year),
                    context.getString(R.string.ai_retro_year_format),
                    context.getString(R.string.ai_retro_year_rules),
                    profileStyle,
                    context.getString(R.string.ai_retro_year_entries_header) + "\n" + monthsText,
                )
                .filter { it.isNotBlank() }
                .joinToString("\n\n")

        val result =
            aiService.generateContent(
                prompt = prompt,
                modelName = FirebaseAiService.MODEL_FLASH,
                temperature = 0.7f,
                maxOutputTokens = 16384,
            )

        if (result.isFailure) {
            Log.e("Retro", "Yearly AI failed: ${result.exceptionOrNull()?.message}")
            _lastFailureCount.incrementAndGet()
            return 0
        }

        val summaryText = result.getOrThrow().trim().replace("—", ", ")
        val titlePrompt =
            context.getString(R.string.ai_retro_year_title_prompt, lang, summaryText.take(500))
        val titleResult =
            aiService.generateContent(
                prompt = titlePrompt,
                modelName = FirebaseAiService.MODEL_FLASH_LITE,
                temperature = 0.6f,
                maxOutputTokens = 50,
            )
        val title =
            titleResult.getOrNull()?.trim()?.replace("—", ", ")?.take(60)
                ?: context.getString(R.string.retro_fallback_year, year)

        currentCoroutineContext().ensureActive()
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

    /**
     * Remove monthly reviews for months that have no actual diary entries (cross-month week bug).
     */
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

    /**
     * Cached prefs instance — delegates to the shared provider so we pay the Keystore cost only
     * once per process, regardless of how many components ask for the prefs.
     */
    private val cachedPrefs: android.content.SharedPreferences? by lazy {
        try {
            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
        } catch (e: Exception) {
            android.util.Log.w("GenerateRetroUC", "Encrypted prefs unavailable", e)
            null
        }
    }

    private fun getProfileStyleInstruction(): String {
        val prefs = cachedPrefs ?: return ""
        val scenario = prefs.getInt(com.bestjournal.app.util.Constants.PREF_DASHBOARD_SCENARIO, 0)
        return when {
            scenario == 0 -> context.getString(R.string.profile_style_chronicler)
            scenario == 1 -> context.getString(R.string.profile_style_advisor)
            scenario == 2 -> context.getString(R.string.profile_style_insight)
            scenario == 3 -> context.getString(R.string.profile_style_coach)
            scenario >= com.bestjournal.app.util.Constants.FIRST_CUSTOM_SCENARIO_INDEX -> {
                val custom =
                    com.bestjournal.app.data.prefs.CustomAnalysesStore
                        .activePromptOrEmpty(prefs, scenario)
                if (custom.isNotBlank()) context.getString(R.string.profile_style_custom, custom)
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
