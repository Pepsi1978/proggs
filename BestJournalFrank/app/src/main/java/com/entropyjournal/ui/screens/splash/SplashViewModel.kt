package com.entropyjournal.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val signInUseCase: SignInWithGoogleUseCase,
) : ViewModel() {
    var isSigningIn by mutableStateOf(false)
        private set

    var signInError by mutableStateOf<String?>(null)
        private set

    fun isSignedIn(): Boolean = signInUseCase.isSignedIn()

    fun signIn(context: android.content.Context, onSuccess: () -> Unit) {
        if (isSigningIn) return
        isSigningIn = true
        signInError = null
        viewModelScope.launch {
            signInUseCase(context)
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    signInError = error.message ?: "Google-Anmeldung fehlgeschlagen."
                }
            isSigningIn = false
        }
    }
}
