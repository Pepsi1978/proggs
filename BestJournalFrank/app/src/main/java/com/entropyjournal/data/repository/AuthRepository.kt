package com.entropyjournal.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.entropyjournal.domain.model.UserProfile
import com.entropyjournal.util.Constants
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val credentialManager: CredentialManager,
    private val encryptedPrefs: SharedPreferences,
    @ApplicationContext private val context: Context
) {
    suspend fun signIn(activityContext: Context): Result<UserProfile> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(Constants.GOOGLE_WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            val profile = UserProfile(
                displayName = googleIdTokenCredential.displayName ?: "",
                email = googleIdTokenCredential.id,
                avatarUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                isSignedIn = true
            )

            saveProfile(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getStoredProfile(): UserProfile? {
        val name = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_NAME, null) ?: return null
        val email = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null) ?: return null
        return UserProfile(
            displayName = name,
            email = email,
            avatarUrl = encryptedPrefs.getString(Constants.PREF_GOOGLE_AVATAR_URL, null),
            lastSyncTimestamp = encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L).takeIf { it > 0 },
            isSignedIn = true
        )
    }

    fun isSignedIn(): Boolean {
        return encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null) != null
    }

    fun signOut() {
        encryptedPrefs.edit()
            .remove(Constants.PREF_GOOGLE_ACCOUNT_NAME)
            .remove(Constants.PREF_GOOGLE_ACCOUNT_EMAIL)
            .remove(Constants.PREF_GOOGLE_AVATAR_URL)
            .remove(Constants.PREF_LAST_SYNC_TIMESTAMP)
            .apply()
    }

    private fun saveProfile(profile: UserProfile) {
        encryptedPrefs.edit()
            .putString(Constants.PREF_GOOGLE_ACCOUNT_NAME, profile.displayName)
            .putString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, profile.email)
            .putString(Constants.PREF_GOOGLE_AVATAR_URL, profile.avatarUrl)
            .apply()
    }
}
