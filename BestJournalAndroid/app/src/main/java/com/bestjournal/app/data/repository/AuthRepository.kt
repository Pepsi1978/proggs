package com.bestjournal.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.bestjournal.app.domain.model.UserProfile
import com.bestjournal.app.util.Constants
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository
@Inject
constructor(
    private val credentialManager: CredentialManager,
    private val encryptedPrefs: SharedPreferences,
    @ApplicationContext private val context: Context,
) {
    suspend fun signIn(activityContext: Context): Result<UserProfile> {
        return try {
            android.util.Log.e("AuthRepo", "=== SIGN-IN START ===")
            android.util.Log.e("AuthRepo", "Web Client ID: ${Constants.GOOGLE_WEB_CLIENT_ID}")
            android.util.Log.e("AuthRepo", "Activity Context class: ${activityContext.javaClass.name}")

            val signInOption =
                GetSignInWithGoogleOption.Builder(Constants.GOOGLE_WEB_CLIENT_ID)
                    .build()
            android.util.Log.e("AuthRepo", "GetSignInWithGoogleOption built (forces account picker)")

            val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
            android.util.Log.e("AuthRepo", "GetCredentialRequest built")

            android.util.Log.e("AuthRepo", "Calling credentialManager.getCredential() — this is where hangs typically happen")
            val result = credentialManager.getCredential(activityContext, request)
            android.util.Log.e("AuthRepo", "getCredential RETURNED — type: ${result.credential.type}")

            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            android.util.Log.e("AuthRepo", "Got Google ID: ${googleIdTokenCredential.id}")

            val profile =
                UserProfile(
                    displayName = googleIdTokenCredential.displayName ?: "",
                    email = googleIdTokenCredential.id,
                    avatarUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    isSignedIn = true,
                )

            saveProfile(profile)
            android.util.Log.e("AuthRepo", "=== SIGN-IN SUCCESS: ${profile.email} ===")
            Result.success(profile)
        } catch (e: Exception) {
            android.util.Log.e(
                "AuthRepo",
                "=== SIGN-IN FAILED: ${e.javaClass.simpleName}: ${e.message} ===",
                e,
            )
            Result.failure(e)
        }
    }

    fun getStoredProfile(): UserProfile? {
        val name = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_NAME, null) ?: return null
        val email =
            encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null) ?: return null
        return UserProfile(
            displayName = name,
            email = email,
            avatarUrl = encryptedPrefs.getString(Constants.PREF_GOOGLE_AVATAR_URL, null),
            lastSyncTimestamp =
                encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L).takeIf { it > 0 },
            isSignedIn = true,
        )
    }

    fun isSignedIn(): Boolean {
        return encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null) != null
    }

    fun signOut() {
        encryptedPrefs
            .edit()
            .remove(Constants.PREF_GOOGLE_ACCOUNT_NAME)
            .remove(Constants.PREF_GOOGLE_ACCOUNT_EMAIL)
            .remove(Constants.PREF_GOOGLE_AVATAR_URL)
            .remove(Constants.PREF_LAST_SYNC_TIMESTAMP)
            .apply()
    }

    @Suppress("ApplySharedPref")
    private fun saveProfile(profile: UserProfile) {
        // commit() (not apply()) so writes land on disk BEFORE the caller proceeds —
        // sign-in is immediately followed by Runtime.exit(0) for auto-restore restart
        // in SettingsViewModel, which would discard async apply() writes.
        encryptedPrefs
            .edit()
            .putString(Constants.PREF_GOOGLE_ACCOUNT_NAME, profile.displayName)
            .putString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, profile.email)
            .putString(Constants.PREF_GOOGLE_AVATAR_URL, profile.avatarUrl)
            .commit()
    }
}
