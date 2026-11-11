package de.frank.jarvis.bio.presentation.einstellungen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import de.frank.jarvis.bio.BioAbruf
import de.frank.jarvis.bio.BioModul
import de.frank.jarvis.bio.data.diagnostics.Diag
import de.frank.jarvis.bio.data.diagnostics.DiagnosticArea
import de.frank.jarvis.bio.data.health.HealthConnectManager
import de.frank.jarvis.bio.data.local.entities.AmazfitWorkoutEntity
import de.frank.jarvis.bio.data.remote.oauth.OAuthService
import de.frank.jarvis.bio.data.repository.AmazfitRepository
import de.frank.jarvis.bio.data.repository.OuraRepository
import de.frank.jarvis.bio.data.settings.AppSettings
import de.frank.jarvis.bio.data.settings.EncryptedSecretsStore
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* ============================== Whoop (OAuth) + „Alles neu laden“ ============================== */

data class OAuthUiState(
    val whoopConnected: Boolean = false,
    val whoopClientId: String = "",
    val whoopClientSecret: String = "",
    val whoopRedirectUri: String = OAuthService.WHOOP_REDIRECT_URI_DEFAULT,
    val message: String? = null,
)

/** Whoop-Anmeldung über AppAuth (aus Entropie Reductor, ohne Google-Kalender und ohne WorkManager). */
class OAuthViewModel(
    private val app: Context,
    private val oauth: OAuthService,
    private val secrets: EncryptedSecretsStore,
    private val amazfitRepo: AmazfitRepository,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<OAuthUiState> = _state.asStateFlow()

    @Volatile private var reloadInProgress = false

    init { pruefeVerbindung() }

    /** Liest den gespeicherten Anmeldestand neu, ohne Netz (jeder Token-Refresh tauscht bei Whoop den Refresh-Token). */
    fun pruefeVerbindung() {
        val nowConnected = oauth.loadWhoopAuthState().isAuthorized && !oauth.whoopNeedsReauth() &&
            !secrets.whoopClientSecret.isNullOrBlank()
        if (nowConnected != _state.value.whoopConnected) _state.update { it.copy(whoopConnected = nowConnected) }
    }

    private fun loadInitial() = OAuthUiState(
        whoopConnected = oauth.loadWhoopAuthState().isAuthorized,
        whoopClientId = secrets.whoopClientId.orEmpty(),
        whoopClientSecret = secrets.whoopClientSecret.orEmpty(),
    )

    /**
     * „Alles neu laden“: setzt die Abruf-Zeitstempel zurück und startet dann den gemeinsamen Abruf ([BioAbruf.manuell]),
     * damit er die volle Historie holt statt nur das Neueste. Läuft nur auf Knopfdruck. Ob Whoop dabei gefragt wird,
     * entscheidet wie immer die Whoop-Steuerung (Schutz der Anmeldung).
     */
    fun reloadAllData() {
        if (reloadInProgress) return
        reloadInProgress = true
        _state.update { it.copy(message = "Lade alle Daten neu — das kann einen Moment dauern …") }
        BioModul.bereich.launch {
            try {
                if (!BioAbruf.bereit(app)) {
                    _state.update { it.copy(message = "Abruf nicht möglich: Die Übernahme der Biodaten nach Jarvis ist nicht abgeschlossen.") }
                    return@launch
                }
                // Den Whoop-Zeitstempel NICHT zurücksetzen: Die Whoop-Steuerung liest ihn (Mindestabstände, letzter Erfolg),
                // und Whoop lädt ohnehin immer 365 Tage.
                secrets.ouraLastSyncEpochMs = 0L
                runCatching {
                    appSettings.setLastOuraSync(0L)
                    appSettings.setLastZeppBodySync(0L)
                    appSettings.setLastAmazfitSync(0L)
                }
                val bericht = BioAbruf.manuell(app, "Alles neu laden").await()
                runCatching { amazfitRepo.mergeFromHealthConnect(days = 60) }.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                _state.update {
                    it.copy(message = when {
                        bericht == null -> "Abruf nicht möglich (Übernahme der Biodaten nicht abgeschlossen oder Fehler)."
                        bericht.probleme.isEmpty() -> "Daten neu geladen: " + bericht.kurzfassung()
                        else -> "Neu geladen mit Störungen:\n" + bericht.probleme.joinToString("\n")
                    })
                }
            } finally {
                reloadInProgress = false
            }
        }
    }

    fun setWhoopClientId(value: String) { _state.update { it.copy(whoopClientId = value) } }
    fun setWhoopClientSecret(value: String) { _state.update { it.copy(whoopClientSecret = value) } }

    fun saveWhoopCredentials() {
        val rawId = state.value.whoopClientId.trim()
        val rawSecret = state.value.whoopClientSecret.trim()
        if (rawId.isNotBlank() && !rawId.matches(WHOOP_CLIENT_ID_REGEX)) {
            _state.update {
                it.copy(message = "Whoop-Client-ID hat ein ungewöhnliches Format. Erwartet: 36-stellige UUID wie " +
                    "8aaa546c-da64-4cd7-abce-5d1a3cf19be8. Eingabe wurde NICHT gespeichert.")
            }
            return
        }
        if (rawSecret.length > 256) {
            _state.update {
                it.copy(message = "Whoop-Client-Secret ist ungewöhnlich lang (${rawSecret.length} Zeichen). Bitte nur den " +
                    "eigentlichen Secret-String aus dem Whoop-Dashboard einfügen. Eingabe wurde NICHT gespeichert.")
            }
            return
        }
        secrets.whoopClientId = rawId.ifBlank { null }
        secrets.whoopClientSecret = rawSecret.ifBlank { null }
        _state.update { it.copy(message = "Whoop-Zugangsdaten gespeichert.") }
    }

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
            oauth.handleWhoopAuthResult(intent, secrets.whoopClientSecret).onSuccess {
                _state.update { it.copy(whoopConnected = true, message = "Whoop verbunden. Das letzte Jahr wird geladen …") }
                // ER startete hier den WhoopSyncWorker (365 Tage). Jarvis hat kein WorkManager: im Bio-Bereich laden.
                BioModul.bereich.launch {
                    val r = BioModul.whoop.syncLastDays(365)
                    _state.update {
                        it.copy(message = r.fold({ n -> "Whoop verbunden, $n Einträge geladen." }, { e -> "Whoop verbunden, Laden fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}" }))
                    }
                }
            }.onFailure { ex ->
                _state.update { it.copy(message = "Whoop-Anmeldung fehlgeschlagen: ${ex.message}") }
            }
        }
    }

    fun disconnectWhoop() {
        oauth.clearWhoopAuthState()
        _state.update { it.copy(whoopConnected = false, message = "Whoop getrennt.") }
    }

    private companion object {
        val WHOOP_CLIENT_ID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}

/* ============================== Polar-Export-ZIP ============================== */

/**
 * Polar-Bulk-Import ohne WorkManager (ER: PolarBulkImportWorker). Läuft in [BioModul.bereich], überlebt also das Verlassen
 * der Seite; der Stand liegt hier statt im ViewModel. Gleiche Regeln wie der Worker: Liefert die ZIP nichts, bleiben die alten
 * Trainings; manuell bearbeitete Trainings werden nicht überschrieben; danach alte Nicht-Polar-Trainings weg, Polar-Trainings
 * schreiben, nur das letzte Jahr behalten – alles in einer Transaktion.
 */
object BioPolarImport {
    private val _stand = MutableStateFlow<String?>(null)
    val stand: StateFlow<String?> = _stand.asStateFlow()

    @Volatile var laeuft = false
        private set

    fun starte(context: Context, zipUri: Uri) {
        if (laeuft) return
        laeuft = true
        val app = context.applicationContext
        _stand.value = "ZIP wird vorbereitet (Datei wird kopiert) …"
        BioModul.bereich.launch {
            var kopie: File? = null
            try {
                kopie = kopiereInCache(app, zipUri)
                Diag.i(DiagnosticArea.POLAR, TAG, "Polar-Bulk: Kopie fertig, ${kopie.length() / 1024} KB")
                _stand.value = "Polar-Historie wird gelesen …"
                val alle = BioModul.polar.import(Uri.fromFile(kopie)) { p ->
                    _stand.value = "${p.entitiesParsed} Trainings gelesen (${p.skipped} übersprungen) …"
                }
                if (alle.isEmpty()) {
                    _stand.value = "Polar-ZIP konnte nicht gelesen werden (Format-Problem). Die bisherigen Trainings bleiben."
                    return@launch
                }
                val db = BioModul.datenbank
                val dao = db.amazfitWorkoutDao()
                val grenze = System.currentTimeMillis() - AmazfitRepository.TRAINING_RETENTION_DAYS * 86_400_000L
                var geschuetzt = 0
                var geschrieben: List<AmazfitWorkoutEntity> = emptyList()
                db.withTransaction {
                    geschrieben = alle.filter { neu ->
                        val alt = dao.getById(neu.trackId)
                        (alt?.manualOverridesMs == null).also { if (!it) geschuetzt++ }
                    }
                    dao.deleteNonPolarWorkouts()
                    dao.upsertAll(geschrieben)
                    dao.deleteOlderThan(grenze)
                }
                BioModul.abgleich.requestSync("Training: Polar-Bulk-Import abgeschlossen")
                _stand.value = "Fertig: ${geschrieben.size} Trainings importiert" +
                    (if (geschuetzt > 0) ", $geschuetzt manuell bearbeitete unverändert" else "") +
                    " (Trainings älter als ein Jahr werden nicht behalten)."
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Diag.e(DiagnosticArea.POLAR, TAG, "Polar-Bulk-Import fehlgeschlagen", t)
                _stand.value = "Import fehlgeschlagen: ${t.message ?: t.javaClass.simpleName}"
            } finally {
                kopie?.let { runCatching { it.delete() } }
                laeuft = false
            }
        }
    }

    private suspend fun kopiereInCache(app: Context, quelle: Uri): File = withContext(Dispatchers.IO) {
        val ordner = File(app.cacheDir, "polar-bulk-import").apply { mkdirs() }
        val ziel = File(ordner, "polar-export-${System.currentTimeMillis()}.zip")
        app.contentResolver.openInputStream(quelle)?.use { input -> ziel.outputStream().use { input.copyTo(it, 64 * 1024) } }
            ?: throw java.io.IOException("Datei ließ sich nicht öffnen: $quelle")
        ziel
    }

    private const val TAG = "BioPolarImport"
}

/* ============================== Oura (Personal Access Token) ============================== */

@androidx.compose.runtime.Immutable
data class OuraApiUiState(
    val tokenInput: String = "",
    val tokenSaved: Boolean = false,
    val accountEmail: String? = null,
    val lastSyncMs: Long = 0L,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Oura-Karte: Token speichern, testen, Sync auf Knopfdruck, trennen (aus Entropie Reductor). */
class OuraApiViewModel(
    private val repo: OuraRepository,
    private val secrets: EncryptedSecretsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<OuraApiUiState> = _state.asStateFlow()

    private fun initialState(): OuraApiUiState {
        val saved = !secrets.ouraPersonalAccessToken.isNullOrBlank()
        return OuraApiUiState(tokenInput = if (saved) MASKED_TOKEN else "", tokenSaved = saved, lastSyncMs = secrets.ouraLastSyncEpochMs)
    }

    fun setToken(value: String) { _state.value = _state.value.copy(tokenInput = value, message = null) }

    fun saveAndConnect() {
        val s = _state.value
        val effectiveToken = if (s.tokenInput == MASKED_TOKEN) secrets.ouraPersonalAccessToken.orEmpty() else s.tokenInput.trim()
        if (effectiveToken.isBlank()) {
            _state.value = s.copy(message = "Bitte zuerst einen Personal Access Token eintragen.")
            return
        }
        secrets.ouraPersonalAccessToken = effectiveToken
        viewModelScope.launch {
            _state.value = s.copy(busy = true, message = "Teste Verbindung zu Oura …")
            runCatching { repo.testToken() }.onSuccess {
                _state.value = _state.value.copy(tokenInput = MASKED_TOKEN, tokenSaved = true, busy = false, message = "Verbunden. Erster Sync läuft …")
                runSyncInternal()
            }.onFailure { ex ->
                if (ex is CancellationException) throw ex
                secrets.ouraPersonalAccessToken = null
                _state.value = _state.value.copy(tokenInput = "", tokenSaved = false, busy = false,
                    message = "Verbindung fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}")
            }
        }
    }

    fun runSync() { viewModelScope.launch { runSyncInternal() } }

    private suspend fun runSyncInternal() {
        if (!repo.isTokenConfigured()) {
            _state.value = _state.value.copy(message = "Bitte zuerst einen Token speichern.")
            return
        }
        _state.value = _state.value.copy(busy = true, message = "Oura-Sync läuft …")
        repo.syncLastDays(365).onSuccess { counts ->
            _state.value = _state.value.copy(
                busy = false,
                lastSyncMs = secrets.ouraLastSyncEpochMs,
                message = "${counts.values.sum()} Einträge geladen (Readiness: ${counts["readiness"] ?: 0}, Schlaf: ${counts["daily_sleep"] ?: 0}, " +
                    "Aktivität: ${counts["activity"] ?: 0}, Resilienz: ${counts["resilience"] ?: 0}, Schlaf-Detail: ${counts["sleep_detail"] ?: 0}).",
            )
        }.onFailure { ex ->
            _state.value = _state.value.copy(busy = false, message = "Sync fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}")
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = "Trenne Verbindung …")
            repo.clearAll()
            _state.value = OuraApiUiState(message = "Verbindung getrennt. Token und Daten gelöscht.")
        }
    }

    companion object {
        private const val MASKED_TOKEN = "••••••••••••••••"
    }
}

/* ============================== Health Connect ============================== */

data class HealthConnectApiUiState(
    val available: Boolean = false,
    val allGranted: Boolean = false,
    val grantedCount: Int = 0,
    val totalCount: Int = 0,
    val message: String? = null,
)

/** Rechte-Status von Health Connect und Knopf zum (erneuten) Erteilen. Liest nur Rechte, keine Daten. */
class HealthConnectApiViewModel(private val healthConnect: HealthConnectManager) : ViewModel() {
    private val _state = MutableStateFlow(HealthConnectApiUiState())
    val state: StateFlow<HealthConnectApiUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            if (!healthConnect.isAvailable()) {
                _state.value = HealthConnectApiUiState(available = false, message = "Health Connect ist auf diesem Gerät nicht verfügbar.")
                return@launch
            }
            val all = healthConnect.requiredReadPermissions()
            val granted = healthConnect.allGrantedPermissions()
            _state.value = HealthConnectApiUiState(
                available = true,
                allGranted = granted.containsAll(all),
                grantedCount = granted.intersect(all).size,
                totalCount = all.size,
            )
        }
    }

    fun setMessage(text: String?) { _state.value = _state.value.copy(message = text) }

    fun requiredPermissions(): Set<String> = healthConnect.requiredReadPermissions()
}
