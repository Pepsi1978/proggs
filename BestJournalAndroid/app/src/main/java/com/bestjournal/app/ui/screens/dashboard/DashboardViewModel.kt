package com.bestjournal.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.remote.ai.AiPhase
import com.bestjournal.app.data.remote.ai.AiRateLimiter
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.ai.TieredAccessResult
import com.bestjournal.app.data.repository.AdviceRepository
import com.bestjournal.app.domain.usecase.AnalyzeEntropyUseCase
import com.bestjournal.app.domain.usecase.GenerateAdviceUseCase
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import android.content.Context
import com.bestjournal.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
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
)

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val generateAdviceUseCase: GenerateAdviceUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val adviceRepository: AdviceRepository,
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

    val isFreemiumUser: StateFlow<Boolean> =
        billingManager.subscriptionState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionState.Free)
            .let { subState ->
                MutableStateFlow(false).also { flow ->
                    viewModelScope.launch {
                        subState.collect { state ->
                            flow.value = state is SubscriptionState.Free &&
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
        _uiState.update {
            it.copy(
                currentScenario = scenario,
                customHeaderTop5 = encryptedPrefs.getString("custom_header_top5", "") ?: "",
                customHeaderAnalyse = encryptedPrefs.getString("custom_header_analyse", "") ?: "",
                customHeaderErgebnisse =
                    encryptedPrefs.getString("custom_header_ergebnisse", "") ?: "",
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
        // Safety: reset stuck flag if update has been "running" for >2 minutes
        val updateStartKey = "dashboard_update_started_at"
        viewModelScope.launch {
            while (true) {
                val updating = encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
                if (updating) {
                    val startedAt = encryptedPrefs.getLong(updateStartKey, System.currentTimeMillis())
                    if (System.currentTimeMillis() - startedAt > 120_000L) {
                        encryptedPrefs.edit()
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
                    analyticsTracker.trackProfileSwitched(
                        _uiState.value.currentScenario,
                        currentScenario,
                    )
                    _uiState.update {
                        it.copy(currentScenario = currentScenario, isScenarioSwitch = true)
                    }
                    adviceRepository.clearDashboard()
                    if (currentScenario == 4) {
                        val customPrompt =
                            encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                        if (customPrompt.isNotBlank()) {
                            refreshDashboard()
                        }
                    } else {
                        refreshDashboard()
                    }
                }
                val promptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
                if (promptSavedAt > lastCustomPromptSavedAt && promptSavedAt > 0L) {
                    lastCustomPromptSavedAt = promptSavedAt
                    if (_uiState.value.currentScenario == 4) {
                        val customPrompt =
                            encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                        adviceRepository.clearDashboard()
                        if (customPrompt.isNotBlank()) {
                            _uiState.update { it.copy(isScenarioSwitch = true) }
                            refreshDashboard()
                        }
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private var lastCustomPromptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
    private var manualRefreshActive = false

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
                    _uiState.update {
                        it.copy(dashboardLimitMessage = context.getString(R.string.dashboard_limit_daily))
                    }
                    return@launch
                }
                is TieredAccessResult.Cooldown -> {
                    _uiState.update {
                        it.copy(dashboardLimitMessage = context.getString(R.string.dashboard_limit_cooldown, accessResult.minutesLeft))
                    }
                    return@launch
                }
                is TieredAccessResult.Allowed -> {
                    // Proceed with the allowed model
                }
            }

            val modelName = (accessResult as? TieredAccessResult.Allowed)?.modelName ?: return@launch

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
                    manualRefreshActive = false
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            isScenarioSwitch = false,
                            errorMessage = error.message ?: context.getString(R.string.dashboard_analysis_failed),
                        )
                }
        }
    }

    fun getCustomPrompt(): String {
        return encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
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
        val sdf = java.text.SimpleDateFormat(pattern, locale)
        return context.getString(R.string.dashboard_last_updated, sdf.format(java.util.Date(ts)))
    }
}
