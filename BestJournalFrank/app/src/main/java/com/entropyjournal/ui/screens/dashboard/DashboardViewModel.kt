package com.entropyjournal.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.SharedPreferences
import com.entropyjournal.data.repository.AdviceRepository
import com.entropyjournal.domain.usecase.AnalyzeEntropyUseCase
import com.entropyjournal.domain.usecase.GenerateAdviceUseCase
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isAutoUpdate: Boolean = false,
    val selectedCategoryIndex: Int = 0,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val currentScenario: Int = 1,
    val isScenarioSwitch: Boolean = false,
    val customHeaderTop5: String = "",
    val customHeaderAnalyse: String = "",
    val customHeaderErgebnisse: String = "",
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val generateAdviceUseCase: GenerateAdviceUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val adviceRepository: AdviceRepository,
    private val encryptedPrefs: SharedPreferences
) : ViewModel() {

    val adviceBlocks = generateAdviceUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private var manualRefreshActive = false

    init {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        _uiState.value = _uiState.value.copy(
            currentScenario = scenario,
            customHeaderTop5 = encryptedPrefs.getString("custom_header_top5", "") ?: "",
            customHeaderAnalyse = encryptedPrefs.getString("custom_header_analyse", "") ?: "",
            customHeaderErgebnisse = encryptedPrefs.getString("custom_header_ergebnisse", "") ?: "",
        )
        // Poll for background dashboard updates triggered by JournalViewModel
        viewModelScope.launch {
            while (true) {
                val updating = encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
                if (updating != _uiState.value.isLoading && !manualRefreshActive) {
                    _uiState.value = _uiState.value.copy(isLoading = updating, isAutoUpdate = updating)
                }
                val currentScenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                if (currentScenario != _uiState.value.currentScenario) {
                    val hadData = adviceBlocks.value.isNotEmpty()
                    _uiState.value = _uiState.value.copy(currentScenario = currentScenario, isScenarioSwitch = hadData)
                    if (hadData) {
                        adviceRepository.clearDashboard()
                        if (currentScenario == 4) {
                            val customPrompt = encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                            if (customPrompt.isNotBlank()) {
                                refreshDashboard()
                            }
                        } else {
                            refreshDashboard()
                        }
                    }
                }
                val promptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
                if (promptSavedAt > lastCustomPromptSavedAt && promptSavedAt > 0L) {
                    lastCustomPromptSavedAt = promptSavedAt
                    if (_uiState.value.currentScenario == 4) {
                        val customPrompt = encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                        adviceRepository.clearDashboard()
                        if (customPrompt.isNotBlank()) {
                            _uiState.value = _uiState.value.copy(isScenarioSwitch = true)
                            refreshDashboard()
                        }
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private var lastCustomPromptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            manualRefreshActive = true
            _uiState.value = _uiState.value.copy(isLoading = true, isAutoUpdate = false, errorMessage = null)
            analyzeEntropyUseCase(freshAnalysis = true)
                .onSuccess {
                    encryptedPrefs.edit()
                        .putLong("dashboard_last_updated_${_uiState.value.currentScenario}", System.currentTimeMillis())
                        .apply()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        canUndo = adviceRepository.canUndo,
                        selectedCategoryIndex = 0,
                        isScenarioSwitch = false,
                        customHeaderTop5 = encryptedPrefs.getString("custom_header_top5", "") ?: "",
                        customHeaderAnalyse = encryptedPrefs.getString("custom_header_analyse", "") ?: "",
                        customHeaderErgebnisse = encryptedPrefs.getString("custom_header_ergebnisse", "") ?: "",
                    )
                    // Auto-hide undo button after 5 seconds
                    if (adviceRepository.canUndo) {
                        kotlinx.coroutines.delay(5_000)
                        _uiState.value = _uiState.value.copy(canUndo = false)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isScenarioSwitch = false,
                        errorMessage = error.message ?: "Analyse fehlgeschlagen"
                    )
                }
            manualRefreshActive = false
        }
    }

    fun getCustomPrompt(): String {
        return encryptedPrefs.getString(com.entropyjournal.util.Constants.PREF_CUSTOM_PROMPT, "") ?: ""
    }

    fun undoDashboard() {
        viewModelScope.launch {
            val success = adviceRepository.undoLastRefresh()
            _uiState.value = _uiState.value.copy(
                canUndo = false,
                selectedCategoryIndex = 0
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun getLastUpdatedText(): String? {
        val ts = encryptedPrefs.getLong("dashboard_last_updated_${_uiState.value.currentScenario}", 0L)
        if (ts == 0L) return null
        val sdf = java.text.SimpleDateFormat("dd.MM. 'um' HH:mm", java.util.Locale.GERMAN)
        return "Letzte Aktualisierung am ${sdf.format(java.util.Date(ts))}"
    }
}
