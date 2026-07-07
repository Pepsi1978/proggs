package de.frank.entropyreducer.data.remote.calendar

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
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

class CalendarNotSignedInException : Exception("Kein Google-Konto für Calendar-Sync verbunden.")

class CalendarConsentRequiredException(val consentIntent: Intent) :
    Exception("Calendar-Einwilligung erforderlich.")

/**
 * Liefert auf Anfrage einen frischen OAuth-Access-Token für den Calendar-Read-Only-Scope.
 * Token werden automatisch von Play-Services refreshed — keine eigenen Refresh-Tokens noetig.
 */
@Singleton
class CalendarSession @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secrets: EncryptedSecretsStore,
) {
    @Volatile private var cachedToken: String? = null
    private val mutex = Mutex()

    /** Liefert ein gueltiges Bearer-Token. Cached intern für die aktuelle Operation. */
    suspend fun freshToken(): String {
        cachedToken?.let { return it }
        return mutex.withLock {
            cachedToken ?: requestToken().also { cachedToken = it }
        }
    }

    /** Beendet die Session — naechster `freshToken()`-Aufruf holt neuen Token. */
    fun end() { cachedToken = null }

    private suspend fun requestToken(): String = withContext(Dispatchers.IO) {
        val email = secrets.calendarAccountEmail ?: throw CalendarNotSignedInException()
        Diag.d(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Token-Request für $email, Scope=CALENDAR_READONLY")
        val account = Account(email, "com.google")
        val scope = "oauth2:${CalendarSignInHelper.CALENDAR_READONLY_SCOPE}"
        val token = try {
            GoogleAuthUtil.getToken(context, account, scope)
        } catch (e: UserRecoverableAuthException) {
            Diag.w(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Consent erforderlich: ${e.message}")
            throw CalendarConsentRequiredException(e.intent ?: Intent())
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Token-Hol fehlgeschlagen: ${t.javaClass.simpleName}: ${t.message}", t)
            throw t
        }
        Diag.d(DiagnosticArea.GOOGLE_CALENDAR, TAG, "Calendar-Token erhalten (Länge=${token.length})")
        token
    }

    companion object {
        private const val TAG = "CalendarSession"
    }
}
