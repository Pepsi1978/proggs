package com.bestjournal.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.data.remote.ai.AiRateLimiter
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.ai.TieredAccessResult
import com.bestjournal.app.data.repository.AdviceRepository
import com.bestjournal.app.domain.usecase.AnalyzeEntropyUseCase
import com.bestjournal.app.domain.usecase.GenerateAdviceUseCase
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val currentScenario: Int = 1,
)

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    private val generateAdviceUseCase: GenerateAdviceUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val adviceRepository: AdviceRepository,
    private val aiUsageTracker: AiUsageTracker,
    private val aiRateLimiter: AiRateLimiter,
    private val billingManager: BillingManager,
    private val encryptedPrefs: android.content.SharedPreferences,
) : ViewModel() {

    val adviceBlocks =
        generateAdviceUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        _uiState.update { it.copy(currentScenario = scenario) }
        if (aiUsageTracker.shouldShowAiInfoBanner()) {
            _uiState.update { it.copy(showAiInfoBanner = true) }
        }
        // Continuously poll the auto-update flag so the loading indicator
        // appears even if the user navigates to the dashboard mid-update.
        viewModelScope.launch {
            while (true) {
                val updating = encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
                if (updating != _uiState.value.isLoading && !manualRefreshActive) {
                    _uiState.update { it.copy(isLoading = updating, isAutoUpdate = updating) }
                }
                val currentScenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                if (currentScenario != _uiState.value.currentScenario) {
                    _uiState.update { it.copy(currentScenario = currentScenario) }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private var manualRefreshActive = false

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            manualRefreshActive = true
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    isAutoUpdate = false,
                    errorMessage = null,
                    dashboardLimitMessage = null,
                )
            analyzeEntropyUseCase(freshAnalysis = true)
                .onSuccess {
                    encryptedPrefs
                        .edit()
                        .putLong(Constants.PREF_DASHBOARD_LAST_UPDATED, System.currentTimeMillis())
                        .apply()
                    manualRefreshActive = false
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            canUndo = adviceRepository.canUndo,
                            selectedCategoryIndex = 0,
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
                            errorMessage = error.message ?: "Analyse fehlgeschlagen",
                        )
                }
        }
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

    fun getLastUpdatedText(): String? {
        val ts = encryptedPrefs.getLong(Constants.PREF_DASHBOARD_LAST_UPDATED, 0L)
        if (ts == 0L) return null
        val sdf = java.text.SimpleDateFormat("dd.MM. 'um' HH:mm", java.util.Locale.GERMAN)
        return "Letzte Aktualisierung am ${sdf.format(java.util.Date(ts))}"
    }
}
