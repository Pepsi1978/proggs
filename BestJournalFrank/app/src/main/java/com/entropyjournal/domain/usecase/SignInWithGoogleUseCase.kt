package com.entropyjournal.domain.usecase

import android.content.Context
import com.entropyjournal.data.repository.AuthRepository
import com.entropyjournal.domain.model.UserProfile
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(activityContext: Context): Result<UserProfile> {
        return authRepository.signIn(activityContext)
    }

    fun isSignedIn(): Boolean = authRepository.isSignedIn()

    fun getProfile(): UserProfile? = authRepository.getStoredProfile()

    fun signOut() = authRepository.signOut()
}
