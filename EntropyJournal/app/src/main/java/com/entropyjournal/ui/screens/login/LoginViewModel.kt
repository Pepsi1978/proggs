package com.entropyjournal.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.domain.model.UserProfile
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val profile: UserProfile, val hasBackup: Boolean) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInWithGoogleUseCase,
    private val syncUseCase: SyncWithDriveUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            signInUseCase(activityContext)
                .onSuccess { profile ->
                    // Auto-restore backup on login
                    try {
                        val hasBackup = syncUseCase.hasBackup()
                        if (hasBackup) {
                            syncUseCase.restore()
                        }
                    } catch (_: Exception) { }
                    _uiState.value = LoginUiState.Success(profile, false)
                }
                .onFailure { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Anmeldung fehlgeschlagen"
                    )
                }
        }
    }
}
