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
    val selectedCategoryIndex: Int = 0,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val showAiInfoBanner: Boolean = false,
    val dashboardLimitMessage: String? = null,
    val manualRefreshesLeft: Int = 3,
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
        if (aiUsageTracker.shouldShowAiInfoBanner()) {
            _uiState.update { it.copy(showAiInfoBanner = true) }
        }
        // Show loading if auto-update from JournalViewModel is still running
        if (encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATING, false)) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                // Poll until the flag clears (max 30s)
                repeat(30) {
                    kotlinx.coroutines.delay(1_000)
                    if (!encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATING, false)) {
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun refreshDashboard() {
        val remaining =
            encryptedPrefs.getInt(
                Constants.PREF_MANUAL_REFRESHES_LEFT,
                Constants.MAX_REFRESHES_PER_ENTRY,
            )
        if (remaining <= 0) {
            _uiState.update {
                it.copy(
                    dashboardLimitMessage =
                        "Aktualisierungslimit erreicht. Schreibe einen neuen Tagebucheintrag \u2014 das Dashboard wird dann automatisch aktualisiert."
                )
            }
            return
        }

        val subscriptionState = billingManager.subscriptionState.value
        when (val access = aiRateLimiter.checkDashboardAccess(subscriptionState)) {
            is TieredAccessResult.Allowed -> {
                performRefresh(access.modelName, remaining)
            }
            is TieredAccessResult.Cooldown -> {
                _uiState.update {
                    it.copy(
                        dashboardLimitMessage =
                            "Du hast heute schon ${access.totalToday} Aktualisierungen gemacht \u2014 nicht schlecht! Kurze Pause, in ${access.minutesLeft} Minuten geht\u2019s weiter."
                    )
                }
            }
            is TieredAccessResult.HardLimitReached -> {
                _uiState.update {
                    it.copy(
                        dashboardLimitMessage =
                            "Userlimit erreicht \u2014 neue Aktualisierungen morgen wieder verf\u00fcgbar."
                    )
                }
            }
        }
    }

    private fun performRefresh(modelName: String, remainingRefreshes: Int = -1) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    dashboardLimitMessage = null,
                )
            aiRateLimiter.recordDashboardRefresh()
            analyzeEntropyUseCase(freshAnalysis = true, modelName = modelName)
                .onSuccess {
                    // Only count successful refreshes against the limit
                    if (remainingRefreshes > 0) {
                        val newRemaining = remainingRefreshes - 1
                        encryptedPrefs
                            .edit()
                            .putInt(Constants.PREF_MANUAL_REFRESHES_LEFT, newRemaining)
                            .apply()
                        _uiState.update { it.copy(manualRefreshesLeft = newRemaining) }
                    }
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            canUndo = adviceRepository.canUndo,
                            selectedCategoryIndex = 0,
                        )
                }
                .onFailure { error ->
                    // Failed refresh does NOT count — user keeps their remaining refreshes
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
}
