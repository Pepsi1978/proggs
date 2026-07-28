package de.frank.perfectmoment.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Gets permission to write into the user's own Drive.
 *
 * Uses [AuthorizationRequest] rather than the old GoogleSignIn flow, which Google removed in May
 * 2026. The app is identified by its package name and signing fingerprint through the OAuth client
 * registered in the Cloud console — no client id ends up in the code.
 *
 * Access tokens live about an hour, so a token is never cached beyond a single operation: every
 * call asks again, which is cheap once permission has been granted.
 */
class DriveAuth(private val context: Context) {

    /** Permission is still missing and needs a screen the user can answer. */
    class NeedsConsent(val intent: Intent) : Exception("Zugriff auf Google Drive muss bestätigt werden")

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APPDATA)))
        .build()

    /**
     * @throws NeedsConsent when the user has to pick an account or approve access — hand the intent
     *   to an [android.app.Activity] and call again afterwards.
     */
    suspend fun accessToken(): String {
        val result = authorize()
        result.pendingIntent?.let { pending ->
            if (result.hasResolution()) {
                throw NeedsConsent(Intent().apply { putExtra(EXTRA_PENDING, pending) })
            }
        }
        return result.accessToken
            ?: throw IllegalStateException("Google hat keinen Zugang zurückgegeben")
    }

    /** True once Drive access has been granted, without showing anything to the user. */
    suspend fun isConnected(): Boolean = runCatching {
        val result = authorize()
        !result.hasResolution() && result.accessToken != null
    }.getOrDefault(false)

    /**
     * Removes the granted access entirely — used by "Verbindung trennen". Revoking takes back every
     * scope at once, so it is only offered as an explicit disconnect, never as cleanup.
     */
    suspend fun disconnect() {
        runCatching {
            suspendCancellableCoroutine { continuation ->
                Identity.getAuthorizationClient(context)
                    .revokeAccess(
                        RevokeAccessRequest.builder()
                            .setScopes(listOf(Scope(DRIVE_APPDATA)))
                            .build(),
                    )
                    .addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        }
    }

    private suspend fun authorize(): AuthorizationResult = suspendCancellableCoroutine { continuation ->
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    companion object {
        const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val EXTRA_PENDING = "pm_pending_consent"

        /** Result of the consent screen — the caller simply retries afterwards. */
        fun wasApproved(resultCode: Int): Boolean = resultCode == Activity.RESULT_OK
    }
}
