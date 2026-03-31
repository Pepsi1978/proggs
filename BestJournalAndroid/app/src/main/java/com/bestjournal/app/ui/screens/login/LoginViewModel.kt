package com.bestjournal.app.ui.screens.login

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.data.remote.googledrive.NeedConsentException
import com.bestjournal.app.domain.model.UserProfile
import com.bestjournal.app.domain.usecase.SignInWithGoogleUseCase
import com.bestjournal.app.domain.usecase.SyncWithDriveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val profile: UserProfile, val needsRestart: Boolean) : LoginUiState()
    data class NeedConsent(val intent: Intent, val profile: UserProfile) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInWithGoogleUseCase,
    private val syncUseCase: SyncWithDriveUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    private var pendingProfile: UserProfile? = null

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            signInUseCase(activityContext)
                .onSuccess { profile ->
                    pendingProfile = profile
                    tryRestore(profile)
                }
                .onFailure { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Anmeldung fehlgeschlagen"
                    )
                }
        }
    }

    fun onConsentResult() {
        val profile = pendingProfile ?: return
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            tryRestore(profile)
        }
    }

    private suspend fun tryRestore(profile: UserProfile) {
        try {
            val hasBackup = syncUseCase.hasBackup()
            if (hasBackup) {
                syncUseCase.restore()
                // Needs app restart so Room picks up restored database
                _uiState.value = LoginUiState.Success(profile, needsRestart = true)
            } else {
                _uiState.value = LoginUiState.Success(profile, needsRestart = false)
            }
        } catch (e: NeedConsentException) {
            _uiState.value = LoginUiState.NeedConsent(e.consentIntent, profile)
        } catch (_: Exception) {
            // Restore failed but login still succeeded
            _uiState.value = LoginUiState.Success(profile, needsRestart = false)
        }
    }
}
