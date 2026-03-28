package com.entropyjournal.ui.screens.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.domain.model.UserProfile
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userProfile: UserProfile? = null,
    val groqApiKey: String = "",
    val geminiApiKey: String = "",
    val selectedModel: String = Constants.DEFAULT_GEMINI_MODEL,
    val textImprovementDefault: Boolean = false,
    val maxRecordingDuration: Int = 5,
    val autoUpdateDashboard: Boolean = true,
    val isDarkTheme: Boolean = true,
    val lastSyncTimestamp: Long? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val showLogoutDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val signInUseCase: SignInWithGoogleUseCase,
    private val syncUseCase: SyncWithDriveUseCase,
    private val encryptedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            userProfile = signInUseCase.getProfile(),
            groqApiKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: "",
            geminiApiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: "",
            selectedModel = encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL) ?: Constants.DEFAULT_GEMINI_MODEL,
            textImprovementDefault = encryptedPrefs.getBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, false),
            maxRecordingDuration = encryptedPrefs.getInt(Constants.PREF_MAX_RECORDING_DURATION, 5),
            autoUpdateDashboard = encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true),
            isDarkTheme = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, true),
            lastSyncTimestamp = encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L).takeIf { it > 0 }
        )
    }

    fun updateDarkTheme(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_DARK_THEME, enabled).apply()
        _uiState.value = _uiState.value.copy(isDarkTheme = enabled)
    }

    fun updateSelectedModel(modelId: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GEMINI_MODEL, modelId).apply()
        _uiState.value = _uiState.value.copy(selectedModel = modelId)
    }

    fun updateGroqApiKey(key: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GROQ_API_KEY, key).apply()
        _uiState.value = _uiState.value.copy(groqApiKey = key)
    }

    fun updateGeminiApiKey(key: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GEMINI_API_KEY, key).apply()
        _uiState.value = _uiState.value.copy(geminiApiKey = key)
    }

    fun updateTextImprovementDefault(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, enabled).apply()
        _uiState.value = _uiState.value.copy(textImprovementDefault = enabled)
    }

    fun updateMaxRecordingDuration(minutes: Int) {
        encryptedPrefs.edit().putInt(Constants.PREF_MAX_RECORDING_DURATION, minutes).apply()
        _uiState.value = _uiState.value.copy(maxRecordingDuration = minutes)
    }

    fun updateAutoUpdateDashboard(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, enabled).apply()
        _uiState.value = _uiState.value.copy(autoUpdateDashboard = enabled)
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            syncUseCase.backup()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncMessage = "Synchronisation erfolgreich",
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncMessage = "Fehler: ${error.message}"
                    )
                }
        }
    }

    fun showLogoutDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLogoutDialog = show)
    }

    fun signOut() {
        signInUseCase.signOut()
        _uiState.value = _uiState.value.copy(userProfile = null, showLogoutDialog = false)
    }
}
