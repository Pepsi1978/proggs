package de.frank.entropyreducer.presentation.settings.api

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.remote.calendar.CalendarSignInHelper
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Zustand der OAuth-Verbindungen für Whoop, Google Calendar und Polar. */
data class OAuthUiState(
    val calendarAccountEmail: String? = null,
    val whoopConnected: Boolean = false,
    val whoopClientId: String = "",
    val whoopClientSecret: String = "",
    val whoopRedirectUri: String = OAuthService.WHOOP_REDIRECT_URI_DEFAULT,
    // Polar (Frank-Wunsch 2026-05-16): alleinige Workout-Quelle nach Strava-Revert.
    val polarConnected: Boolean = false,
    val polarClientId: String = "",
    val polarClientSecret: String = "",
    val polarRedirectUri: String = OAuthService.POLAR_REDIRECT_URI_DEFAULT,
    val polarUserId: Long = 0L,
    val polarLastSyncMs: Long = 0L,
    val message: String? = null,
)

/**
 * ViewModel für die OAuth-Cards im API-Keys-Bildschirm.
 *
 * Whoop laeuft über AppAuth + Custom-URI-Redirect (siehe OAuthService).
 * Google Calendar laeuft über GoogleSignIn + Play-Services-Token-Refresh
 * (siehe CalendarSignInHelper / CalendarSession), weil Google im Web-App-Client
 * keine Custom-URI-Schemes mehr akzeptiert.
 */
@HiltViewModel
class OAuthViewModel @Inject constructor(
    private val app: Application,
    private val oauth: OAuthService,
    private val secrets: EncryptedSecretsStore,
    private val scheduler: BackgroundScheduler,
    val calendarSignIn: CalendarSignInHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<OAuthUiState> = _state.asStateFlow()

    init {
        // Direktive 3 — UI-Konsistenz: AuthState kann "isAuthorized=true" sagen
        // selbst wenn der Refresh seit Stunden invalid_client wirft. Ein einmaliger
        // Probe-Refresh beim ViewModel-Init synchronisiert die UI-Anzeige mit dem
        // tatsaechlichen Zustand. freshWhoopAccessToken() heilt den AuthState bei
        // Fehler selbst (siehe OAuthService — Schicht 3), wir muessen hier nur den
        // UI-Status nachziehen.
        viewModelScope.launch {
            runCatching { oauth.freshWhoopAccessToken() }
                .onSuccess { token ->
                    val nowConnected = token != null
                    if (nowConnected != _state.value.whoopConnected) {
                        _state.update { it.copy(whoopConnected = nowConnected) }
                    }
                }
                .onFailure {
                    // IllegalStateException("Whoop-Client-Secret fehlt …") landet hier.
                    // UI als nicht-verbunden markieren, damit Frank den Banner versteht.
                    _state.update { it.copy(whoopConnected = false) }
                }
        }
    }

    private fun loadInitial(): OAuthUiState = OAuthUiState(
        calendarAccountEmail = secrets.calendarAccountEmail,
        whoopConnected = oauth.loadWhoopAuthState().isAuthorized,
        whoopClientId = secrets.whoopClientId.orEmpty(),
        whoopClientSecret = secrets.whoopClientSecret.orEmpty(),
        polarConnected = oauth.loadPolarAuthState().isAuthorized && secrets.polarUserId > 0L,
        polarClientId = secrets.polarClientId.orEmpty(),
        polarClientSecret = secrets.polarClientSecret.orEmpty(),
        polarUserId = secrets.polarUserId,
        polarLastSyncMs = secrets.polarLastSyncEpochMs,
    )

    fun setWhoopClientId(value: String) { _state.update { it.copy(whoopClientId = value) } }
    fun setWhoopClientSecret(value: String) { _state.update { it.copy(whoopClientSecret = value) } }

    fun saveWhoopCredentials() {
        val rawId = state.value.whoopClientId.trim()
        val rawSecret = state.value.whoopClientSecret.trim()
        if (rawId.isNotBlank() && !rawId.matches(WHOOP_CLIENT_ID_REGEX)) {
            _state.update {
                it.copy(message = "Whoop-Client-ID hat ein ungewoehnliches Format. " +
                    "Erwartet: 36-stellige UUID wie 8aaa546c-da64-4cd7-abce-5d1a3cf19be8. " +
                    "Eingabe wurde NICHT gespeichert.")
            }
            return
        }
        if (rawSecret.length > 256) {
            _state.update {
                it.copy(message = "Whoop-Client-Secret ist ungewoehnlich lang " +
                    "(${rawSecret.length} Zeichen). Bitte nur den eigentlichen Secret-String " +
                    "aus dem Whoop-Dashboard einfuegen. Eingabe wurde NICHT gespeichert.")
            }
            return
        }
        secrets.whoopClientId = rawId.ifBlank { null }
        secrets.whoopClientSecret = rawSecret.ifBlank { null }
        _state.update { it.copy(message = "Whoop-Credentials gespeichert.") }
    }

    /* ------------------------------- Whoop ------------------------------- */

    fun buildWhoopAuthIntent(): Intent? {
        val clientId = secrets.whoopClientId
        if (clientId.isNullOrBlank()) {
            _state.update { it.copy(message = "Bitte zuerst die Whoop-Client-ID speichern.") }
            return null
        }
        return oauth.buildWhoopAuthIntent(clientId, state.value.whoopRedirectUri)
    }

    fun onWhoopAuthResult(intent: Intent) {
        viewModelScope.launch {
            val result = oauth.handleWhoopAuthResult(intent, secrets.whoopClientSecret)
            result.onSuccess {
                _state.update { it.copy(whoopConnected = true, message = "Whoop verbunden.") }
                scheduler.runWhoopSyncNow()
                scheduler.ensureNightlyJobs()
            }.onFailure { ex ->
                _state.update { it.copy(message = "Whoop-Auth fehlgeschlagen: ${ex.message}") }
            }
        }
    }

    fun disconnectWhoop() {
        oauth.clearWhoopAuthState()
        scheduler.cancelWhoopSync()
        _state.update { it.copy(whoopConnected = false, message = "Whoop getrennt.") }
    }

    /* ---------------------------- Google Calendar ---------------------------- */

    fun onCalendarSignInSuccess(account: GoogleSignInAccount) {
        val email = account.email ?: run {
            _state.update { it.copy(message = "Google-Konto ohne E-Mail — abgewiesen.") }
            return
        }
        secrets.calendarAccountEmail = email
        _state.update {
            it.copy(
                calendarAccountEmail = email,
                message = "Google Calendar verbunden mit $email — erster Sync laeuft.",
            )
        }
        scheduler.runCalendarSyncNow()
        scheduler.ensureNightlyJobs()
    }

    fun onCalendarSignInError(message: String) {
        _state.update { it.copy(message = "Calendar-Sign-In fehlgeschlagen: $message") }
    }

    fun disconnectGoogleCalendar() {
        viewModelScope.launch {
            calendarSignIn.signOut()
            secrets.calendarAccountEmail = null
            scheduler.cancelCalendarSync()
            _state.update { it.copy(calendarAccountEmail = null, message = "Google Calendar getrennt.") }
        }
    }

    /* ------------------------------- Polar ------------------------------- */

    fun setPolarClientId(value: String) { _state.update { it.copy(polarClientId = value) } }
    fun setPolarClientSecret(value: String) { _state.update { it.copy(polarClientSecret = value) } }

    fun savePolarCredentials() {
        val rawId = state.value.polarClientId.trim()
        val rawSecret = state.value.polarClientSecret.trim()
        if (rawSecret.length > 256) {
            _state.update {
                it.copy(message = "Polar-Client-Secret ist ungewoehnlich lang " +
                    "(${rawSecret.length} Zeichen). Bitte nur den Secret-String " +
                    "aus dem Polar-Developer-Dashboard einfuegen. NICHT gespeichert.")
            }
            return
        }
        secrets.polarClientId = rawId.ifBlank { null }
        secrets.polarClientSecret = rawSecret.ifBlank { null }
        _state.update { it.copy(message = "Polar-Credentials gespeichert.") }
    }

    fun buildPolarAuthIntent(): Intent? {
        val clientId = secrets.polarClientId
        if (clientId.isNullOrBlank()) {
            _state.update { it.copy(message = "Bitte zuerst die Polar-Client-ID speichern.") }
            return null
        }
        return oauth.buildPolarAuthIntent(clientId, state.value.polarRedirectUri)
    }

    fun onPolarAuthResult(intent: Intent) {
        viewModelScope.launch {
            val result = oauth.handlePolarAuthResult(intent, secrets.polarClientSecret)
            result.onSuccess {
                _state.update {
                    it.copy(
                        polarConnected = true,
                        polarUserId = secrets.polarUserId,
                        message = "Polar verbunden — Trainings werden geladen.",
                    )
                }
                // Sofort einen ersten Sync ausloesen, dann den Nightly-Job sicherstellen.
                scheduler.runPolarSyncNow()
                scheduler.ensureNightlyJobs()
            }.onFailure { ex ->
                _state.update { it.copy(message = "Polar-Auth fehlgeschlagen: ${ex.message}") }
            }
        }
    }

    fun disconnectPolar() {
        oauth.clearPolarAuthState()
        scheduler.cancelPolarSync()
        _state.update {
            it.copy(
                polarConnected = false,
                polarUserId = 0L,
                polarLastSyncMs = 0L,
                message = "Polar getrennt.",
            )
        }
    }

    /**
     * Startet den Polar-Bulk-Import (Frank-Wunsch 2026-05-16: 10-Jahre-Historie).
     *
     * Frank-Bugfix 2026-05-16 (Crash-Fix): Die vom File-Picker gelieferte URI
     * koennte ein Google-Drive-Content-Provider sein. Drive's Permission gilt
     * NUR fuer die Activity — sobald der Worker im Hintergrund laeuft, gibt
     * der Drive-Provider SecurityException. Loesung: ZIP synchron HIER (im
     * ViewModel mit Activity-lebensdauernder Permission) in den App-Cache
     * kopieren, dann dem Worker den lokalen file://-Pfad uebergeben.
     *
     * Die Kopie laeuft im viewModelScope auf Dispatchers.IO — bei 91 MB
     * dauert das 3-5 Sekunden. Solange zeigt die UI eine Status-Message.
     */
    fun startPolarBulkImport(zipUri: android.net.Uri) {
        _state.update { it.copy(message = "ZIP wird vorbereitet (Datei wird kopiert)…") }
        viewModelScope.launch {
            val cachedFile = runCatching { copyZipToCache(zipUri) }.getOrElse { t ->
                _state.update {
                    it.copy(message = "ZIP konnte nicht gelesen werden: ${t.message ?: t.javaClass.simpleName}")
                }
                return@launch
            }
            val cacheUri = android.net.Uri.fromFile(cachedFile)
            scheduler.runPolarBulkImport(cacheUri)
            _state.update {
                it.copy(message = "Polar-Historie wird importiert — beachte die Benachrichtigung")
            }
        }
    }

    /**
     * Kopiert die ZIP-Datei vom Source-URI (kann Drive, lokal oder sonstwo
     * sein) in den App-Cache. Wir arbeiten ueber den ContentResolver — der
     * App-Process hat hier noch die kurzlebige URI-Permission vom Picker.
     * 64 KB Buffer ist ein guter Kompromiss zwischen Speed und RAM.
     *
     * Bei 91 MB ZIP-Datei werden ~3-5 Sekunden gebraucht (abhaengig von der
     * Quelle: lokale Datei ~1s, Drive ~5s wegen Re-Download).
     */
    private suspend fun copyZipToCache(sourceUri: android.net.Uri): File = withContext(Dispatchers.IO) {
        val cacheDir = File(app.cacheDir, "polar-bulk-import")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cachedFile = File(cacheDir, "polar-export-${System.currentTimeMillis()}.zip")
        app.contentResolver.openInputStream(sourceUri)?.use { input ->
            cachedFile.outputStream().use { output ->
                input.copyTo(output, bufferSize = 64 * 1024)
            }
        } ?: throw java.io.IOException("openInputStream lieferte null fuer $sourceUri — gibt der Picker eine ungueltige Datei?")
        cachedFile
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }

    private companion object {
        val WHOOP_CLIENT_ID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )
    }
}
