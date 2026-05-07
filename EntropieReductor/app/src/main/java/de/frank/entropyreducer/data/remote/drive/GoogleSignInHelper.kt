package de.frank.entropyreducer.data.remote.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /** Aus einem ActivityResult-Daten-Intent das Konto extrahieren. */
    fun accountFromResult(data: Intent?): GoogleSignInAccount? {
        return runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(
                com.google.android.gms.common.api.ApiException::class.java
            )
        }.getOrNull()
    }

    fun lastSignedIn(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    /** Bei Logout: Token revoken + Account-Cache leeren. */
    suspend fun signOut() {
        runCatching { client.signOut().result }
        runCatching { client.revokeAccess().result }
    }
}
