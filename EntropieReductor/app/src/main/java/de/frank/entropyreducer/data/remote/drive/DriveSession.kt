package de.frank.entropyreducer.data.remote.drive

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * User braucht Sign-In bevor Drive ueberhaupt erreichbar ist. Wir werfen
 * diese Ausnahme, damit die UI eine klare Meldung zeigt + zum Sign-In
 * weiterleitet.
 */
class DriveNotSignedInException : Exception("Kein Google-Konto für Drive-Backup verbunden.")

/**
 * Wenn Google die OAuth-Einwilligung verlangt (erstes Mal, oder Token abgelaufen),
 * liefert das System einen Intent — die UI muss ihn starten.
 */
class DriveConsentRequiredException(val consentIntent: Intent) :
    Exception("Drive-Einwilligung erforderlich.")

/**
 * Liefert auf Anfrage einen authentifizierten [Drive]-Client — gecacht innerhalb
 * einer "Session" (= Backup-/Restore-Operation), damit nicht jede einzelne
 * Drive-Operation einen neuen OAuth-Token-Roundtrip macht.
 */
@Singleton
class DriveSession @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secrets: EncryptedSecretsStore,
) {
    @Volatile private var current: Drive? = null
    @Volatile private var currentToken: String? = null
    private val mutex = Mutex()

    /** Liefert den aktuellen Drive-Client; baut ihn lazy beim ersten Aufruf. */
    suspend fun get(): Drive {
        current?.let { return it }
        return mutex.withLock {
            current ?: build().also { current = it }
        }
    }

    /** Beendet die Session. Naechster `get()`-Aufruf fordert frischen Token an. */
    fun end() {
        current = null
        currentToken = null
    }

    /**
     * Bei einem 401 vom Drive-Server: Token aus dem Google-Play-Services-Cache
     * loeschen, damit der naechste `get()`-Aufruf wirklich einen frischen Token
     * holt. Ohne `clearToken` wuerde `GoogleAuthUtil.getToken()` den toten
     * Token aus seinem internen Cache zurueckgeben — und der naechste Drive-
     * Aufruf wuerde wieder mit 401 scheitern.
     */
    suspend fun invalidateToken() = withContext(Dispatchers.IO) {
        val deadToken = currentToken
        current = null
        currentToken = null
        if (!deadToken.isNullOrEmpty()) {
            runCatching { GoogleAuthUtil.clearToken(context, deadToken) }
                .onFailure { Diag.w(DiagnosticArea.DRIVE_BACKUP, TAG, "clearToken fehlgeschlagen: ${it.message}") }
            Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "Token invalidiert, naechster get() holt frisch.")
        }
    }

    private suspend fun build(): Drive = withContext(Dispatchers.IO) {
        val email = secrets.driveAccountEmail ?: throw DriveNotSignedInException()
        Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "Token-Request für $email, Scope=DRIVE_APPDATA")
        val account = Account(email, "com.google")
        val scope = "oauth2:${DriveScopes.DRIVE_APPDATA}"
        val token = try {
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: UserRecoverableAuthException) {
            Diag.w(DiagnosticArea.DRIVE_BACKUP, TAG, "Consent erforderlich: ${e.message}")
            throw DriveConsentRequiredException(e.intent ?: Intent())
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.DRIVE_BACKUP, TAG, "Token-Hol fehlgeschlagen: ${t.javaClass.simpleName}: ${t.message}", t)
            throw t
        }
        currentToken = token
        Diag.d(DiagnosticArea.DRIVE_BACKUP, TAG, "Token erhalten (Länge=${token.length}), Drive-Client wird gebaut")
        Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance()) { request ->
            request.headers.authorization = "Bearer $token"
        }
            .setApplicationName("EntropieReductor")
            .build()
    }

    companion object {
        private const val TAG = "EREDriveSession"
    }
}
