package com.nems.app.ui.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface AuthUiState {
    data object SignedOut : AuthUiState
    data object Loading : AuthUiState
    data class SignedIn(val email: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignedOut)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithGoogle() {
        // TODO: Implement with Firebase Auth + Credential Manager
        _uiState.value = AuthUiState.Error("Firebase noch nicht konfiguriert. Bitte google-services.json hinzufuegen.")
    }

    fun continueAsGuest() {
        // Nothing to do, app works offline
    }
}
