package de.frank.entropyreducer.presentation.settings.api

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.calendar.CalendarSignInHelper
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.workers.BackgroundScheduler
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Zustand der OAuth-Verbindungen für Whoop und Google Calendar. */
data class OAuthUiState(
    val calendarAccountEmail: String? = null,
    val whoopConnected: Boolean = false,
    val whoopClientId: String = "",
    val whoopClientSecret: String = "",
    val whoopRedirectUri: String = OAuthService.WHOOP_REDIRECT_URI_DEFAULT,
    // Polar-OAuth-State entfernt 2026-05-17 (Frank-Wunsch). Polar-Bulk-Import-
    // Status braucht keinen Persistent-State — der ZIP-Picker startet den
    // Worker direkt.
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
    private val amazfitRepo: de.frank.entropyreducer.data.repository.AmazfitRepository,
    private val workoutDao: de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao,
    val calendarSignIn: CalendarSignInHelper,
    // Frank-Wunsch 2026-05-23: fuer den "Alles neu laden"-Knopf.
    private val whoopRepo: de.frank.entropyreducer.data.repository.WhoopRepository,
    private val ouraRepo: de.frank.entropyreducer.data.repository.OuraRepository,
    private val healthConnectRepository:
        de.frank.entropyreducer.data.repository.HealthConnectRepository,
    private val appSettings: de.frank.entropyreducer.data.settings.AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<OAuthUiState> = _state.asStateFlow()

    /** Reentry-Schutz fuer den "Alles neu laden"-Knopf — verhindert parallele Voll-Syncs. */
    @Volatile
    private var reloadInProgress = false

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

    /**
     * Frank-Wunsch 2026-05-23: "Alles neu laden"-Knopf in den API-Einstellungen. Setzt alle
     * Sync-Zeitstempel zurueck, sodass der nachfolgende Sync NICHT inkrementell laeuft, sondern
     * die volle Historie neu holt. Laeuft NUR auf manuellen Knopf-Druck. Reihenfolge wie beim
     * App-Start (Health Connect -> Whoop -> Oura -> Training). Jeder Schritt defensiv (runCatching),
     * damit ein Fehler bei einer Quelle die anderen nicht stoppt.
     */
    fun reloadAllData() {
        // Reentry-Schutz: mehrfaches Knopf-Druecken darf keine parallelen Voll-Syncs starten.
        if (reloadInProgress) return
        reloadInProgress = true
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(message = "Lade alle Daten neu — das kann einen Moment dauern …")
                }
                // Zeitstempel zuruecksetzen -> die Sync-Methoden laden voll (lastSync == 0).
                secrets.ouraLastSyncEpochMs = 0L
                runCatching {
                    appSettings.setLastWhoopSync(0L)
                    appSettings.setLastOuraSync(0L)
                    appSettings.setLastHealthConnectSync(0L)
                    appSettings.setLastAmazfitSync(0L)
                }
                // Voll synchronisieren (gleiche Reihenfolge wie der App-Start).
                runCatching { healthConnectRepository.syncToCache() }
                runCatching {
                    if (oauth.loadWhoopAuthState().isAuthorized) whoopRepo.syncLastDays(365)
                }
                runCatching { if (ouraRepo.isTokenConfigured()) ouraRepo.syncLastDays(365) }
                runCatching { amazfitRepo.mergeFromHealthConnect(days = 60) }
                _state.update {
                    it.copy(message = "Alle Daten wurden neu geladen.")
                }
            } finally {
                reloadInProgress = false
            }
        }
    }

    private fun loadInitial(): OAuthUiState = OAuthUiState(
        calendarAccountEmail = secrets.calendarAccountEmail,
        whoopConnected = oauth.loadWhoopAuthState().isAuthorized,
        whoopClientId = secrets.whoopClientId.orEmpty(),
        whoopClientSecret = secrets.whoopClientSecret.orEmpty(),
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

    // Polar-V3/V4/FlowWeb OAuth-Methoden entfernt 2026-05-17 (Frank-Wunsch).
    // Polar-Historie kommt jetzt nur noch ueber Polar-ZIP-Bulk-Import
    // (siehe startPolarBulkImport unten).

    // Polar Flow Web Methoden (disconnectPolarFlowWeb, savePolarFlowWebWorkoutJson,
    // savePolarFlowWebCookies, reloadPolarFlowWebWorkout) entfernt 2026-05-17.

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
        Diag.i(DiagnosticArea.OAUTH, "OAuthViewModel", "Polar-Bulk: startPolarBulkImport Source-URI scheme=${zipUri.scheme} authority=${zipUri.authority}")
        // SCHICHT 1: Alle frueheren Worker-Versuche canceln. Ohne das laufen
        // gescheiterte Versuche (z.B. mit alter Drive-URI) im Hintergrund weiter
        // und ueberlagern den frischen Start.
        scheduler.cancelPolarBulkImport()
        _state.update { it.copy(message = "ZIP wird vorbereitet (Datei wird kopiert)…") }
        viewModelScope.launch {
            val cachedFile = runCatching { copyZipToCache(zipUri) }.getOrElse { t ->
                Diag.e(DiagnosticArea.OAUTH, "OAuthViewModel", "Polar-Bulk: Cache-Copy fehlgeschlagen", t)
                _state.update {
                    it.copy(message = "ZIP konnte nicht gelesen werden: ${t.message ?: t.javaClass.simpleName}")
                }
                return@launch
            }
            Diag.i(DiagnosticArea.OAUTH, "OAuthViewModel", "Polar-Bulk: Cache-Copy fertig — ${cachedFile.length() / 1024} KB unter ${cachedFile.absolutePath}")
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
