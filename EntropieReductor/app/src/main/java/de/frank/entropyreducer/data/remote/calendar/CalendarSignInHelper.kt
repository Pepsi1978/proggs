package de.frank.entropyreducer.data.remote.calendar

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sign-In-Helper analog zum DriveSignInHelper, aber für Calendar-Read-Only.
 * Eigener Client, eigene Scopes — der Drive-Sign-In bleibt unberuehrt.
 *
 * SETUP-VORAUSSETZUNG: In Google Cloud Console muss ein OAuth 2.0 Client
 * vom Typ "Android" mit Package + SHA-1 angelegt sein (gleicher Client wie Drive
 * funktioniert — Android-Clients sind API-uebergreifend, solange die Calendar API
 * im Projekt aktiviert ist und der OAuth Consent Screen den Calendar-Scope listet).
 */
@Singleton
class CalendarSignInHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CALENDAR_READONLY_SCOPE))
            .build()

    private val client: GoogleSignInClient by lazy { GoogleSignIn.getClient(context, signInOptions) }

    fun signInIntent(): Intent = client.signInIntent

    sealed class SignInResult {
        data class Success(val account: GoogleSignInAccount) : SignInResult()
        data class Error(val statusCode: Int, val message: String) : SignInResult()
    }

    fun accountFromResult(data: Intent?): SignInResult {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            SignInResult.Success(account)
        } catch (e: ApiException) {
            val code = e.statusCode
            val codeName = GoogleSignInStatusCodes.getStatusCodeString(code)
            Diag.e(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Calendar Sign-In fehlgeschlagen: code=$code ($codeName), msg=${e.status.statusMessage}", e)
            SignInResult.Error(code, codeName)
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Calendar Sign-In: unerwartete Exception", t)
            SignInResult.Error(-1, t.message ?: "Unbekannter Fehler")
        }
    }

    suspend fun signOut() {
        runCatching { client.signOut().result }
        runCatching { client.revokeAccess().result }
    }

    companion object {
        private const val TAG = "CalendarSignInHelper"
        const val CALENDAR_READONLY_SCOPE = "https://www.googleapis.com/auth/calendar.readonly"
    }
}
