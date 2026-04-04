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
    val selectedCategoryIndex: Int = 0,
    val errorMessage: String? = null,
    val canUndo: Boolean = false
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

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            analyzeEntropyUseCase(freshAnalysis = true)
                .onSuccess {
                    encryptedPrefs.edit()
                        .putLong(Constants.PREF_DASHBOARD_LAST_UPDATED, System.currentTimeMillis())
                        .apply()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        canUndo = adviceRepository.canUndo,
                        selectedCategoryIndex = 0
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Analyse fehlgeschlagen"
                    )
                }
        }
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
        val ts = encryptedPrefs.getLong(Constants.PREF_DASHBOARD_LAST_UPDATED, 0L)
        if (ts == 0L) return null
        val sdf = java.text.SimpleDateFormat("dd.MM. 'um' HH:mm", java.util.Locale.GERMAN)
        return "Letzte Aktualisierung am ${sdf.format(java.util.Date(ts))}"
    }
}
