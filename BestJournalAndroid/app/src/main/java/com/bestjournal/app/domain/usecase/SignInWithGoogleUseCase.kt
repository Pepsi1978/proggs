package com.bestjournal.app.domain.usecase

import android.content.Context
import com.bestjournal.app.data.repository.AuthRepository
import com.bestjournal.app.domain.model.UserProfile
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
