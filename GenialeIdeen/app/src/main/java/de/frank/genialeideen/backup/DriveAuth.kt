package de.frank.genialeideen.backup

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ApiException
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

    /**
     * Permission is still missing and needs a screen the user can answer. The caller shows
     * [pendingIntent] and repeats the operation afterwards — approving alone does nothing.
     */
    class NeedsConsent(val pendingIntent: PendingIntent) :
        Exception("Zugriff auf Google Drive muss bestätigt werden")

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APPDATA)))
        .build()

    /**
     * @throws NeedsConsent when the user has to pick an account or approve access — hand the intent
     *   to an [android.app.Activity] and call again afterwards.
     */
    /** Zugang aus dem Freigabe-Bildschirm, gültig für die folgende Anfrage. */
    private var tokenFromConsent: String? = null

    suspend fun accessToken(): String {
        tokenFromConsent?.let { token ->
            tokenFromConsent = null
            return token
        }
        val result = runCatching { authorize() }
            .getOrElse { fehler -> throw IllegalStateException(explain(fehler), fehler) }
        if (result.hasResolution()) {
            val pending = result.pendingIntent
                ?: throw IllegalStateException("Google verlangt eine Freigabe, liefert aber keinen Dialog.")
            throw NeedsConsent(pending)
        }
        return result.accessToken
            ?: throw IllegalStateException("Google hat keinen Zugang zurückgegeben")
    }

    /**
     * Liest Googles Antwort auf den Freigabe-Bildschirm. Der Rückgabewert der Activity sagt nichts
     * darüber aus, ob der Zugriff erteilt wurde — erst dieses Ergebnis tut es, und im Fehlerfall
     * nennt es den Grund.
     */
    fun readConsentResult(data: Intent?): Result<Boolean> = runCatching {
        val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
        tokenFromConsent = result.accessToken
        result.accessToken != null
    }.recoverCatching { error ->
        throw IllegalStateException(explain(error), error)
    }

    /** Übersetzt Googles Fehlercodes in etwas, mit dem sich das Problem beheben lässt. */
    private fun explain(error: Throwable): String {
        val status = (error as? ApiException)?.statusCode
        return when (status) {
            CommonStatusCodes.DEVELOPER_ERROR ->
                "Google erkennt die App nicht. In der Cloud Console fehlt eine OAuth-Client-ID vom " +
                    "Typ Android für de.frank.genialeideen mit dem SHA-1 dieser App."
            CommonStatusCodes.SIGN_IN_REQUIRED ->
                "Die Anmeldung wurde nicht abgeschlossen."
            CommonStatusCodes.NETWORK_ERROR ->
                "Keine Verbindung zu Google."
            CommonStatusCodes.CANCELED ->
                "Die Freigabe wurde abgebrochen."
            null -> error.message ?: "Google hat die Anfrage ohne Grund abgelehnt."
            else -> "Google meldet Fehler $status: ${error.message}"
        }
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
    }
}
