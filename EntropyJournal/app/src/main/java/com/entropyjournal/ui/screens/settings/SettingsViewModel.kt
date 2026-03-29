package com.entropyjournal.ui.screens.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.domain.model.UserProfile
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Intent
import com.entropyjournal.data.remote.googledrive.NeedConsentException

data class SettingsUiState(
    val userProfile: UserProfile? = null,
    val groqApiKey: String = "",
    val geminiApiKey: String = "",
    val selectedModel: String = Constants.DEFAULT_GEMINI_MODEL,
    val textImprovementDefault: Boolean = false,
    val maxRecordingDuration: Int = 5,
    val autoUpdateDashboard: Boolean = true,
    val isDarkTheme: Boolean = false,
    val followSystem: Boolean = false,
    val followSun: Boolean = false,
    val biometricLock: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val showLogoutDialog: Boolean = false,
    val consentIntent: Intent? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val signInUseCase: SignInWithGoogleUseCase,
    private val syncUseCase: SyncWithDriveUseCase,
    private val encryptedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    // Listen for external theme changes (e.g. quick toggle from Journal/Dashboard)
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Constants.PREF_DARK_THEME, Constants.PREF_THEME_FOLLOW_SYSTEM, Constants.PREF_THEME_FOLLOW_SUN -> {
                _uiState.value = _uiState.value.copy(
                    isDarkTheme = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false),
                    followSystem = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false),
                    followSun = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
                )
            }
        }
    }

    init {
        loadSettings()
        encryptedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onCleared() {
        super.onCleared()
        encryptedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
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
            isDarkTheme = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false),
            followSystem = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false),
            followSun = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false),
            biometricLock = encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false),
            lastSyncTimestamp = encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L).takeIf { it > 0 }
        )
    }

    fun updateDarkTheme(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(Constants.PREF_DARK_THEME, enabled)
            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
            .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
            .apply()
        _uiState.value = _uiState.value.copy(isDarkTheme = enabled, followSystem = false, followSun = false)
    }

    fun updateFollowSystem(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, enabled)
            .putBoolean(Constants.PREF_DARK_THEME, false)
            .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
            .apply()
        _uiState.value = _uiState.value.copy(followSystem = enabled, isDarkTheme = false, followSun = false)
    }

    fun updateFollowSun(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, enabled)
            .putBoolean(Constants.PREF_DARK_THEME, false)
            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
            .apply()
        _uiState.value = _uiState.value.copy(followSun = enabled, isDarkTheme = false, followSystem = false)
    }

    fun saveLocation(lat: Double, lon: Double) {
        encryptedPrefs.edit()
            .putFloat(Constants.PREF_LATITUDE, lat.toFloat())
            .putFloat(Constants.PREF_LONGITUDE, lon.toFloat())
            .apply()
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

    fun updateBiometricLock(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_BIOMETRIC_LOCK, enabled).apply()
        _uiState.value = _uiState.value.copy(biometricLock = enabled)
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            syncUseCase.backup()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncMessage = "Erfolgreich gesichert",
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                    // Auto-dismiss message after 3 seconds
                    delay(3000)
                    _uiState.value = _uiState.value.copy(syncMessage = null)
                }
                .onFailure { error ->
                    if (error is NeedConsentException) {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            consentIntent = error.consentIntent
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            syncMessage = "Fehler: ${error.message}"
                        )
                    }
                }
        }
    }

    fun restoreFromCloud(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            syncUseCase.restore()
                .onSuccess {
                    // Restart app so Room picks up the restored database
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }
                .onFailure { error ->
                    if (error is NeedConsentException) {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            consentIntent = error.consentIntent
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            syncMessage = "Fehler: ${error.message}"
                        )
                    }
                }
        }
    }

    fun showLogoutDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLogoutDialog = show)
    }

    fun clearConsentIntent() {
        _uiState.value = _uiState.value.copy(consentIntent = null)
    }

    fun signOut(context: android.content.Context) {
        try {
            // Save API keys before sign-out — they belong to the device, not the account
            val groqKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: ""
            val geminiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
            val selectedModel = encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL) ?: Constants.DEFAULT_GEMINI_MODEL
            val isDark = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, true)
            val biometricLock = encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false)

            signInUseCase.signOut()

            // Delete local database — data belongs to the account
            context.deleteDatabase("entropy_journal_db")
            // Also delete WAL/SHM files
            context.getDatabasePath("entropy_journal_db-wal")?.delete()
            context.getDatabasePath("entropy_journal_db-shm")?.delete()

            // Restore device-specific settings
            encryptedPrefs.edit()
                .putString(Constants.PREF_GROQ_API_KEY, groqKey)
                .putString(Constants.PREF_GEMINI_API_KEY, geminiKey)
                .putString(Constants.PREF_GEMINI_MODEL, selectedModel)
                .putBoolean(Constants.PREF_DARK_THEME, isDark)
                .putBoolean(Constants.PREF_BIOMETRIC_LOCK, biometricLock)
                .apply()
        } catch (_: Exception) { }

        // Restart the app process so Room clears its in-memory cache
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
