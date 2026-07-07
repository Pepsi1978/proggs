package de.frank.entropyreducer.data.remote.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrappt das Sign-In via Google. Wir fragen explizit den `drive.appdata`-Scope an,
 * damit der spaetere Drive-Roundtrip ohne weiteren Consent-Dialog laufen kann.
 *
 * SETUP-VORAUSSETZUNG: In der Google Cloud Console muss ein OAuth 2.0 Client
 * vom Typ "Android" mit dem Package `de.frank.entropyreducer.debug` (Debug)
 * bzw. `de.frank.entropyreducer` (Release) und der jeweiligen SHA-1 der
 * Signaturkeystore registriert sein. Ohne diesen Eintrag liefert Sign-In den
 * Fehler "DEVELOPER_ERROR" / Code 10. Siehe README.md.
 */
@Singleton
class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

    private val client: GoogleSignInClient by lazy { GoogleSignIn.getClient(context, signInOptions) }

    /** Liefert den Sign-In-Intent — die UI startet ihn via ActivityResultContract. */
    fun signInIntent(): Intent = client.signInIntent

    /**
     * Liefert das Konto bei Erfolg, sonst eine [SignInError] mit dem echten
     * Google-Status-Code — damit die UI den Fehler unterscheiden und dem Nutzer
     * eine spezifische Meldung zeigen kann.
     */
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
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "Sign-In fehlgeschlagen: code=$code ($codeName), statusMessage=${e.status.statusMessage}", e)
            SignInResult.Error(code, codeName)
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "Sign-In: unerwartete Exception", t)
            SignInResult.Error(-1, t.message ?: "Unbekannter Fehler")
        }
    }

    fun lastSignedIn(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    /** Bei Logout: Token revoken + Account-Cache leeren. */
    suspend fun signOut() {
        runCatching { client.signOut().result }
        runCatching { client.revokeAccess().result }
    }

    companion object {
        private const val TAG = "GoogleSignInHelper"
    }
}
