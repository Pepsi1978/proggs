package com.bestjournal.app.ui.screens.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.R
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.remote.ai.AiPhase
import com.bestjournal.app.data.remote.ai.AiRateLimiter
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.ai.TieredAccessResult
import com.bestjournal.app.data.repository.AdviceRepository
import com.bestjournal.app.domain.usecase.AnalyzeEntropyUseCase
import com.bestjournal.app.domain.usecase.GenerateAdviceUseCase
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isAutoUpdate: Boolean = false,
    val selectedCategoryIndex: Int = 0,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val showAiInfoBanner: Boolean = false,
    val dashboardLimitMessage: String? = null,
    val manualRefreshesLeft: Int = 3,
    val currentScenario: Int = 0,
    val isScenarioSwitch: Boolean = false,
    val customHeaderTop5: String = "",
    val customHeaderAnalyse: String = "",
    val customHeaderErgebnisse: String = "",
    val showAnalysisUpsellBanner: Boolean = false,
    val showWeeklyReviewBanner: Boolean = false,
    val isDeleteUpdate: Boolean = false,
    // B3 — true wenn das aktive Custom-Profil keinen Fokustext hat. UI zeigt
    // dann eine Hinweis-Karte statt des Dashboards, damit der Benutzer den
    // Schritt nicht uebersieht.
    val emptyCustomPromptWarning: Boolean = false,
    // C1 — sichtbarer Profil-Header (Profilname + Kurzfokus). Greift fuer alle
    // Profile (eingebaute und custom). Wird im Dashboard oben angezeigt.
    val activeProfileLabel: String = "",
    val activeProfileFocus: String = "",
    // A2 — fokus_kern und fokus_zitate aus der KI-Antwort fuer Custom-Profile.
    val customFokusKern: String = "",
    val customFokusZitateJson: String = "",
)

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val generateAdviceUseCase: GenerateAdviceUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val adviceRepository: AdviceRepository,
    private val journalEntryDao: JournalEntryDao,
    private val aiUsageTracker: AiUsageTracker,
    private val aiRateLimiter: AiRateLimiter,
    private val billingManager: BillingManager,
    private val encryptedPrefs: android.content.SharedPreferences,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    val adviceBlocks =
        generateAdviceUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private val _weeklyDashboardUsed = MutableStateFlow(0)
    val weeklyDashboardUsed: StateFlow<Int> = _weeklyDashboardUsed

    private val _weeklyDashboardMax = MutableStateFlow(Constants.FREE_WEEKLY_DASHBOARD_LIMIT)
    val weeklyDashboardMax: StateFlow<Int> = _weeklyDashboardMax

    // IMPORTANT: declare these BEFORE the init block. With Dispatchers.Main.immediate,
    // viewModelScope.launch { while(true) { ... } } begins executing synchronously until
    // the first suspension point (`delay(500)`). If `lastCustomPromptSavedAt` were
    // declared below the init block, its Kotlin default 0L would still be live during
    // that first inline pass — and any Pref that had ever been touched in the past
    // would look strictly greater than 0L and spuriously trigger clearDashboard +
    // refreshDashboard on every cold start. Declaring them here guarantees the initial
    // Pref read runs before the polling loop observes them.
    private var lastCustomPromptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
    private var manualRefreshActive = false

    val isFreemiumUser: StateFlow<Boolean> =
        billingManager.subscriptionState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionState.Free)
            .let { subState ->
                MutableStateFlow(false).also { flow ->
                    viewModelScope.launch {
                        subState.collect { state ->
                            flow.value =
                                state is SubscriptionState.Free &&
                                    aiUsageTracker.getCurrentPhase() == AiPhase.FREEMIUM
                        }
                    }
                }
            }

    init {
        _weeklyDashboardUsed.value = aiUsageTracker.getWeeklyDashboardCount()
        _weeklyDashboardMax.value = Constants.FREE_WEEKLY_DASHBOARD_LIMIT
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        aiRateLimiter.setCurrentScenario(scenario)
        analyticsTracker.trackDashboardViewed(scenario)
        // Bug-Fix 2026-05-15: customFokusKern stammt aus dem letzten Custom-Profil-Refresh
        // und ueberlebt in Prefs auch wenn der User auf ein eingebautes Profil wechselt.
        // Beim Init NUR lesen wenn das aktive Profil tatsaechlich ein Custom-Profil ist —
        // sonst sieht das eingebaute Profil den fremden Custom-Kern als Beschreibung.
        val isCurrentCustom = scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX
        _uiState.update {
            it.copy(
                currentScenario = scenario,
                customHeaderTop5 = encryptedPrefs.getString("custom_header_top5", "") ?: "",
                customHeaderAnalyse = encryptedPrefs.getString("custom_header_analyse", "") ?: "",
                customHeaderErgebnisse =
                    encryptedPrefs.getString("custom_header_ergebnisse", "") ?: "",
                customFokusKern =
                    if (isCurrentCustom)
                        encryptedPrefs.getString("custom_fokus_kern", "") ?: ""
                    else "",
                customFokusZitateJson =
                    if (isCurrentCustom)
                        encryptedPrefs.getString("custom_fokus_zitate_json", "") ?: ""
                    else "",
                activeProfileLabel = computeProfileLabel(scenario),
                activeProfileFocus = computeProfileFocus(scenario),
                emptyCustomPromptWarning = isEmptyCustomProfile(scenario),
            )
        }
        if (aiUsageTracker.shouldShowAiInfoBanner()) {
            _uiState.update { it.copy(showAiInfoBanner = true) }
        }
        // Show weekly review upsell banner for free users arriving from the notification
        viewModelScope.launch {
            val fromWeeklyReview =
                encryptedPrefs.getBoolean(Constants.PREF_FROM_WEEKLY_REVIEW, false)
            if (fromWeeklyReview) {
                encryptedPrefs.edit().putBoolean(Constants.PREF_FROM_WEEKLY_REVIEW, false).apply()
                kotlinx.coroutines.delay(600)
                val isFree = billingManager.subscriptionState.value is SubscriptionState.Free
                if (isFree) {
                    analyticsTracker.trackWeeklyReviewUpsellShown()
                    _uiState.update { it.copy(showWeeklyReviewBanner = true) }
                }
            }
        }
        // Check upsell banner when blocks load (handles auto-update that completed before
        // navigation)
        viewModelScope.launch {
            adviceBlocks.first { it.isNotEmpty() }
            // Small delay so BillingManager can resolve subscription status on cold starts
            kotlinx.coroutines.delay(500)
            if (!_uiState.value.showAnalysisUpsellBanner && shouldShowAnalysisUpsell()) {
                analyticsTracker.trackUpsellBannerShown("first_analysis")
                _uiState.update { it.copy(showAnalysisUpsellBanner = true) }
            }
        }
        // Continuously poll the auto-update flag so the loading indicator
        // appears even if the user navigates to the dashboard mid-update.
        //
        // PREF_DASHBOARD_UPDATING / PREF_DASHBOARD_UPDATE_IS_DELETE are runtime-only
        // flags used for cross-ViewModel communication during a live analysis. If the
        // app was killed mid-analysis (OOM, swipe-away, crash), the finally-blocks that
        // reset them never ran, leaving stale `true` values in persistent prefs. The
        // polling loop below would then render the "Dashboard wird aktualisiert" state
        // on every cold start even though nothing is actually running. Clear them here:
        // any previous process that set the flag is definitely dead by now.
        val updateStartKey = "dashboard_update_started_at"
        encryptedPrefs
            .edit()
            .putBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
            .putBoolean(Constants.PREF_DASHBOARD_UPDATE_IS_DELETE, false)
            .remove(updateStartKey)
            .apply()
        viewModelScope.launch {
            while (true) {
                val updating = encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
                if (updating) {
                    val startedAt = encryptedPrefs.getLong(updateStartKey, 0L)
                    if (startedAt == 0L || System.currentTimeMillis() - startedAt > 240_000L) {
                        encryptedPrefs
                            .edit()
                            .putBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
                            .remove(updateStartKey)
                            .apply()
                        _uiState.update { it.copy(isLoading = false, isAutoUpdate = false) }
                        continue
                    }
                }
                if (updating != _uiState.value.isLoading && !manualRefreshActive) {
                    if (!updating && _uiState.value.isAutoUpdate) {
                        // Auto-update just completed — check upsell banner
                        val showUpsell = shouldShowAnalysisUpsell()
                        if (showUpsell && !_uiState.value.showAnalysisUpsellBanner) {
                            analyticsTracker.trackUpsellBannerShown("first_analysis")
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAutoUpdate = false,
                                showAnalysisUpsellBanner = showUpsell || it.showAnalysisUpsellBanner,
                            )
                        }
                    } else {
                        val isDelete =
                            encryptedPrefs.getBoolean(
                                Constants.PREF_DASHBOARD_UPDATE_IS_DELETE,
                                false,
                            )
                        _uiState.update {
                            it.copy(
                                isLoading = updating,
                                isAutoUpdate = updating,
                                isDeleteUpdate = isDelete,
                            )
                        }
                    }
                }
                val currentScenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                if (currentScenario != _uiState.value.currentScenario) {
                    aiRateLimiter.setCurrentScenario(currentScenario)
                    // Weekly dashboard quota is tracked per profile (since 0.21.8): when
                    // the user switches to a different profile, refresh the counter so the
                    // limit indicator reflects the new profile's bucket (which may still
                    // have full 5/5 quota left even if the previous profile was exhausted).
                    _weeklyDashboardUsed.value = aiUsageTracker.getWeeklyDashboardCount()
                    analyticsTracker.trackProfileSwitched(
                        _uiState.value.currentScenario,
                        currentScenario,
                    )
                    val emptyWarn = isEmptyCustomProfile(currentScenario)
                    // Bug-Fix 2026-05-15: customFokusKern/Zitate stammen aus dem
                    // letzten Custom-Profil-Refresh. Beim Profil-Wechsel sind die
                    // Werte fuer das NEUE Profil noch nicht generiert — also leeren,
                    // sonst zeigt der Profil-Header den alten Fokus-Kern an. Bei
                    // Custom-Profilen werden die Felder mit der naechsten Analyse
                    // wieder gefuellt (AdviceRepository.parseAdviceJson).
                    _uiState.update {
                        it.copy(
                            currentScenario = currentScenario,
                            isScenarioSwitch = !emptyWarn,
                            activeProfileLabel = computeProfileLabel(currentScenario),
                            activeProfileFocus = computeProfileFocus(currentScenario),
                            emptyCustomPromptWarning = emptyWarn,
                            customFokusKern = "",
                            customFokusZitateJson = "",
                        )
                    }
                    adviceRepository.clearDashboard()
                    if (currentScenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                        val customPrompt =
                            com.bestjournal.app.data.prefs.CustomAnalysesStore
                                .activePromptOrEmpty(encryptedPrefs, currentScenario)
                        if (customPrompt.isNotBlank()) {
                            refreshDashboard()
                        }
                        // B3 — Bei leerem Custom-Prompt: KEIN refresh, aber
                        // emptyCustomPromptWarning ist oben gesetzt — UI zeigt Hinweis-Karte.
                    } else {
                        refreshDashboard()
                    }
                }
                val promptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
                if (promptSavedAt > lastCustomPromptSavedAt && promptSavedAt > 0L) {
                    lastCustomPromptSavedAt = promptSavedAt
                    val active = _uiState.value.currentScenario
                    if (active >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                        val customPrompt =
                            com.bestjournal.app.data.prefs.CustomAnalysesStore
                                .activePromptOrEmpty(encryptedPrefs, active)
                        adviceRepository.clearDashboard()
                        if (customPrompt.isNotBlank()) {
                            _uiState.update {
                                it.copy(
                                    isScenarioSwitch = true,
                                    activeProfileLabel = computeProfileLabel(active),
                                    activeProfileFocus = computeProfileFocus(active),
                                    emptyCustomPromptWarning = false,
                                )
                            }
                            refreshDashboard()
                        } else {
                            // B3 — Custom-Prompt wurde geleert -> Warn-Card statt stillem Skip
                            _uiState.update {
                                it.copy(
                                    activeProfileLabel = computeProfileLabel(active),
                                    activeProfileFocus = "",
                                    emptyCustomPromptWarning = true,
                                )
                            }
                        }
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }

        // Auto-generate dashboard ONLY on first load after an ACTUAL Google restore.
        // Previously this block ran on every cold start, so if blockCount happened to
        // be 0 (e.g. advice_blocks was cleared by a scenario switch whose follow-up
        // analysis never completed) it would fire a fresh AI refresh on every app
        // start even though the user had not added any new entries. We now only
        // trigger the refresh when PREF_RESTORE_PENDING was actually true when the VM
        // started — i.e. the user really came back from a Drive restore. Dashboard
        // refreshes must only happen on: profile switch, custom analysis saved, or
        // restored journal entries — never on a plain cold start.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hadRestore =
                    encryptedPrefs.getBoolean(Constants.PREF_RESTORE_PENDING, false)
                awaitRestoreComplete()
                if (!hadRestore) return@launch

                val blockCount = adviceRepository.getBlockCount()
                val entryCount = journalEntryDao.getEntryCount()
                if (blockCount == 0 && entryCount > 0) {
                    Log.d(
                        "DashboardVM",
                        "Dashboard empty after real restore, generating from $entryCount entries",
                    )
                    refreshDashboard()
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Auto-generate after restore failed: ${e.message}", e)
            }
        }
    }

    /** Waits for the Google Drive restore flag to clear (max 10 minutes). */
    private suspend fun awaitRestoreComplete() {
        val maxWaitMs = 10L * 60 * 1000
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < maxWaitMs) {
            val restorePending = encryptedPrefs.getBoolean(Constants.PREF_RESTORE_PENDING, false)
            if (!restorePending) break
            Log.d("DashboardVM", "Restore in progress, waiting...")
            kotlinx.coroutines.delay(3000)
        }
    }

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            // Check access BEFORE making the API call
            val subState = billingManager.subscriptionState.value
            val accessResult = aiRateLimiter.checkDashboardAccess(subState)

            when (accessResult) {
                is TieredAccessResult.HardLimitReached -> {
                    // Free users hit the per-profile WEEKLY limit (5 per profile per week,
                    // since 0.21.8). Trial/subscribed users hit the per-profile DAILY limit
                    // (101/151). Each case has its own message — the weekly one tells the
                    // user that switching the profile may free up a fresh bucket.
                    val isFreemiumFree =
                        subState is SubscriptionState.Free &&
                            aiUsageTracker.getCurrentPhase() == AiPhase.FREEMIUM
                    val limitMessageRes =
                        if (isFreemiumFree) R.string.dashboard_limit_weekly_profile
                        else R.string.dashboard_limit_daily
                    _uiState.update {
                        it.copy(dashboardLimitMessage = context.getString(limitMessageRes))
                    }
                    return@launch
                }
                is TieredAccessResult.Cooldown -> {
                    _uiState.update {
                        it.copy(
                            dashboardLimitMessage =
                                context.getString(
                                    R.string.dashboard_limit_cooldown,
                                    accessResult.minutesLeft,
                                )
                        )
                    }
                    return@launch
                }
                is TieredAccessResult.Allowed -> {
                    // Proceed with the allowed model
                }
            }

            val modelName =
                (accessResult as? TieredAccessResult.Allowed)?.modelName ?: return@launch

            manualRefreshActive = true
            analyticsTracker.trackDashboardRefreshed(_uiState.value.currentScenario)
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    isAutoUpdate = false,
                    errorMessage = null,
                    dashboardLimitMessage = null,
                )
            // Record attempt (daily + hourly counters) — errors are OK here
            aiRateLimiter.recordDashboardAttempt()
            analyzeEntropyUseCase(freshAnalysis = true, modelName = modelName)
                .onSuccess {
                    // Only count toward the free weekly limit on SUCCESS — errors don't count
                    aiRateLimiter.recordDashboardSuccess()
                    val scenarioKey = "dashboard_last_updated_${_uiState.value.currentScenario}"
                    encryptedPrefs.edit().putLong(scenarioKey, System.currentTimeMillis()).apply()
                    manualRefreshActive = false

                    // Track analysis count and check for first-analysis upsell
                    _weeklyDashboardUsed.value = aiUsageTracker.getWeeklyDashboardCount()

                    val analysisCount =
                        encryptedPrefs.getInt(Constants.PREF_DASHBOARD_ANALYSIS_COUNT, 0) + 1
                    encryptedPrefs
                        .edit()
                        .putInt(Constants.PREF_DASHBOARD_ANALYSIS_COUNT, analysisCount)
                        .apply()
                    val showUpsell = shouldShowAnalysisUpsell()
                    if (showUpsell) {
                        analyticsTracker.trackUpsellBannerShown("first_analysis")
                    }

                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            canUndo = adviceRepository.canUndo,
                            selectedCategoryIndex = 0,
                            isScenarioSwitch = false,
                            customHeaderTop5 =
                                encryptedPrefs.getString("custom_header_top5", "") ?: "",
                            customHeaderAnalyse =
                                encryptedPrefs.getString("custom_header_analyse", "") ?: "",
                            customHeaderErgebnisse =
                                encryptedPrefs.getString("custom_header_ergebnisse", "") ?: "",
                            showAnalysisUpsellBanner = showUpsell,
                        )
                    // Auto-hide undo button after 5 seconds
                    if (adviceRepository.canUndo) {
                        kotlinx.coroutines.delay(5_000)
                        _uiState.value = _uiState.value.copy(canUndo = false)
                    }
                }
                .onFailure { error ->
                    android.util.Log.e(
                        "DashboardVM",
                        "refreshDashboard FAILED: ${error.javaClass.simpleName}: ${error.message}",
                        error,
                    )
                    manualRefreshActive = false
                    val msg = if (error is com.bestjournal.app.domain.NoEntriesException)
                        context.getString(com.bestjournal.app.R.string.journal_no_entries)
                    else
                        error.message ?: context.getString(com.bestjournal.app.R.string.dashboard_gemini_unavailable)
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            isScenarioSwitch = false,
                            errorMessage = msg,
                        )
                }
        }
    }

    fun getCustomPrompt(): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return com.bestjournal.app.data.prefs.CustomAnalysesStore
            .activePromptOrEmpty(encryptedPrefs, scenario)
    }

    fun undoDashboard() {
        viewModelScope.launch {
            val success = adviceRepository.undoLastRefresh()
            _uiState.value = _uiState.value.copy(canUndo = false, selectedCategoryIndex = 0)
        }
    }

    fun dismissAiInfoBanner() {
        aiUsageTracker.markAiInfoBannerShown()
        _uiState.update { it.copy(showAiInfoBanner = false) }
    }

    fun dismissLimitMessage() {
        _uiState.update { it.copy(dashboardLimitMessage = null) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissAnalysisUpsellBanner() {
        encryptedPrefs.edit().putBoolean(Constants.PREF_FIRST_ANALYSIS_UPSELL_SHOWN, true).apply()
        _uiState.update { it.copy(showAnalysisUpsellBanner = false) }
    }

    fun onAnalysisUpsellClicked() {
        analyticsTracker.trackUpsellBannerClicked("first_analysis")
        dismissAnalysisUpsellBanner()
    }

    fun trackFreeLimitShown(remaining: Int) {
        analyticsTracker.trackFreeLimitIndicatorShown(remaining)
    }

    fun onFreeLimitUpgradeClicked() {
        analyticsTracker.trackFreeLimitUpgradeClicked()
    }

    fun dismissWeeklyReviewBanner() {
        _uiState.update { it.copy(showWeeklyReviewBanner = false) }
    }

    fun onWeeklyReviewUpsellClicked() {
        analyticsTracker.trackWeeklyReviewUpsellClicked()
        dismissWeeklyReviewBanner()
    }

    private fun shouldShowAnalysisUpsell(): Boolean {
        val analysisCount = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_ANALYSIS_COUNT, 0)
        val isFree = billingManager.subscriptionState.value is SubscriptionState.Free
        val alreadyShown =
            encryptedPrefs.getBoolean(Constants.PREF_FIRST_ANALYSIS_UPSELL_SHOWN, false)
        val onboardingDone = encryptedPrefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETED, false)
        return isFree && !alreadyShown && onboardingDone && analysisCount >= 1
    }

    fun getLastUpdatedText(): String? {
        val scenarioKey = "dashboard_last_updated_${_uiState.value.currentScenario}"
        val ts = encryptedPrefs.getLong(scenarioKey, 0L)
        if (ts == 0L) return null
        val locale = java.util.Locale.getDefault()
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMHHmm")
        // Thread-safe: DateTimeFormatter is immutable, unlike SimpleDateFormat
        val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern, locale)
        val formatted =
            java.time.ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(ts),
                    java.time.ZoneId.systemDefault(),
                )
                .format(formatter)
        return context.getString(R.string.dashboard_last_updated, formatted)
    }

    // ── C1 / B3 Helper ──────────────────────────────────────────────────────

    /**
     * C1 — Lesbare Bezeichnung des aktiven Profils. Wird oben im Dashboard angezeigt.
     */
    private fun computeProfileLabel(scenario: Int): String {
        return when {
            scenario == 0 -> context.getString(R.string.dashboard_profile_summary)
            scenario == 1 -> context.getString(R.string.dashboard_profile_entropy)
            scenario == 2 -> context.getString(R.string.dashboard_profile_insight)
            scenario == 3 -> context.getString(R.string.dashboard_profile_goals)
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX ->
                com.bestjournal.app.data.prefs.CustomAnalysesStore.activeNameOrDefault(
                    encryptedPrefs,
                    scenario,
                )
            else -> ""
        }
    }

    /**
     * C1 — Kurzer Fokus-Text fuer den Header (max 200 Zeichen).
     * Bei eingebauten Profilen: feste Beschreibung. Bei Custom: erste 200 Zeichen
     * des Profil-Prompts (oder leer wenn Prompt noch nicht definiert ist).
     */
    private fun computeProfileFocus(scenario: Int): String {
        return when {
            scenario == 0 -> context.getString(R.string.dashboard_profile_focus_summary)
            scenario == 1 -> context.getString(R.string.dashboard_profile_focus_entropy)
            scenario == 2 -> context.getString(R.string.dashboard_profile_focus_insight)
            scenario == 3 -> context.getString(R.string.dashboard_profile_focus_goals)
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX -> {
                val prompt =
                    com.bestjournal.app.data.prefs.CustomAnalysesStore.activePromptOrEmpty(
                        encryptedPrefs,
                        scenario,
                    )
                if (prompt.isBlank()) ""
                else if (prompt.length > 200) prompt.take(197) + "..."
                else prompt
            }
            else -> ""
        }
    }

    /**
     * B3 — Ist das aktive Profil ein Custom-Profil mit leerem Prompt?
     * Wenn ja, zeigt das Dashboard eine Hinweis-Karte statt des normalen Inhalts.
     */
    private fun isEmptyCustomProfile(scenario: Int): Boolean {
        if (scenario < Constants.FIRST_CUSTOM_SCENARIO_INDEX) return false
        return com.bestjournal.app.data.prefs.CustomAnalysesStore.activePromptOrEmpty(
                encryptedPrefs,
                scenario,
            )
            .isBlank()
    }
}
