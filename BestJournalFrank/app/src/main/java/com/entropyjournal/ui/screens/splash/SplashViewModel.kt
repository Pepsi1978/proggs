package com.entropyjournal.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val signInUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    fun isUserSignedIn(): Boolean = signInUseCase.isSignedIn()
}
