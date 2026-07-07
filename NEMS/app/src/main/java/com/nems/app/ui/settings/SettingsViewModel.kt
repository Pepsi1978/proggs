package com.nems.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nems.app.data.remote.NeedConsentException
import com.nems.app.data.repository.AuthRepository
import com.nems.app.domain.model.UserProfile
import com.nems.app.domain.usecase.SyncWithDriveUseCase
import com.nems.app.ui.theme.ThemeMode
import com.nems.app.ui.theme.ThemeStateHolder
import com.nems.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userProfile: UserProfile? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val lastSyncTimestamp: Long? = null,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val consentIntent: Intent? = null,
    val showLogoutDialog: Boolean = false,
    val showDeleteDataDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncUseCase: SyncWithDriveUseCase,
    private val encryptedPrefs: SharedPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        _uiState.update { it.copy(themeMode = ThemeStateHolder.themeMode.value) }
    }

    private fun loadProfile() {
        val profile = authRepository.getStoredProfile()
        _uiState.update {
            it.copy(
                userProfile = profile,
                lastSyncTimestamp = profile?.lastSyncTimestamp,
            )
        }
    }

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val result = authRepository.signIn(activityContext)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            userProfile = profile,
                            isSyncing = false,
                            syncMessage = "Angemeldet als ${profile.email}",
                        )
                    }
                    // Check if backup exists and offer restore
                    checkForExistingBackup()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncMessage = "Anmeldung fehlgeschlagen: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    private fun checkForExistingBackup() {
        viewModelScope.launch {
            try {
                if (syncUseCase.hasBackup()) {
                    _uiState.update { it.copy(syncMessage = "Backup gefunden! Tippe 'Wiederherstellen' um Daten zu laden.") }
                }
            } catch (_: Exception) { }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update {
            it.copy(
                userProfile = null,
                lastSyncTimestamp = null,
                syncMessage = null,
                showLogoutDialog = false,
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = "Wird gesichert...") }
            val result = syncUseCase.backup()
            result.fold(
                onSuccess = {
                    val timestamp = encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L)
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncMessage = "Backup erfolgreich!",
                            lastSyncTimestamp = timestamp,
                        )
                    }
                },
                onFailure = { error ->
                    if (error is NeedConsentException) {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                consentIntent = error.consentIntent,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncMessage = "Fehler: ${error.message}",
                            )
                        }
                    }
                },
            )
        }
    }

    fun restoreFromBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = "Wird wiederhergestellt...") }
            val result = syncUseCase.restore()
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = "Fehler: ${result.exceptionOrNull()?.message}",
                    )
                }
            }
            // On success, app restarts automatically via SyncWithDriveUseCase
        }
    }

    fun clearConsentIntent() {
        _uiState.update { it.copy(consentIntent = null) }
    }

    fun showLogoutDialog(show: Boolean) {
        _uiState.update { it.copy(showLogoutDialog = show) }
    }

    fun showDeleteDataDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDataDialog = show) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        ThemeStateHolder.setThemeMode(mode, encryptedPrefs)
    }

    fun dismissSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }
}
