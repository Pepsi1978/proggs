package de.frank.entropyreducer.presentation.settings.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.remote.zepp.ZeppAuthService
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class ZeppApiUiState(
    val email: String = "",
    val password: String = "",
    val region: String = "de2",
    val connected: Boolean = false,
    val userId: String? = null,
    val lastSyncMs: Long = 0L,
    val busy: Boolean = false,
    val message: String? = null,
)

/**
 * State + Aktionen fuer die Amazfit/Zepp-Login-Karte unter Einstellungen → API.
 * Frank-Wunsch 2026-05-09: E-Mail + Passwort, Verbinden, Sync starten, Trennen.
 */
@HiltViewModel
class ZeppApiViewModel @Inject constructor(
    private val auth: ZeppAuthService,
    private val repo: AmazfitRepository,
    private val secrets: EncryptedSecretsStore,
    private val scheduler: BackgroundScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ZeppApiUiState> = _state.asStateFlow()

    private fun initialState(): ZeppApiUiState = ZeppApiUiState(
        email = secrets.zeppEmail.orEmpty(),
        password = if (secrets.zeppPassword.isNullOrEmpty()) "" else MASKED_PASSWORD,
        region = secrets.zeppRegion ?: "de2",
        connected = auth.isAuthenticated(),
        userId = secrets.zeppUserId,
        lastSyncMs = secrets.zeppLastSyncEpochMs,
    )

    fun setEmail(value: String) {
        _state.value = _state.value.copy(email = value, message = null)
    }

    fun setPassword(value: String) {
        _state.value = _state.value.copy(password = value, message = null)
    }

    fun setRegion(value: String) {
        secrets.zeppRegion = value
        _state.value = _state.value.copy(region = value)
    }

    fun login() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(message = "Bitte E-Mail und Passwort ausfuellen.")
            return
        }
        // Wenn der Nutzer das Maskenfeld nicht angefasst hat, nutze das gespeicherte Passwort.
        val effectivePassword = if (s.password == MASKED_PASSWORD) {
            secrets.zeppPassword.orEmpty()
        } else {
            s.password
        }
        viewModelScope.launch {
            _state.value = s.copy(busy = true, message = "Verbinde mit Zepp …")
            val result = auth.login(s.email, effectivePassword)
            result.onSuccess {
                _state.value = _state.value.copy(
                    busy = false,
                    connected = true,
                    userId = secrets.zeppUserId,
                    region = secrets.zeppRegion ?: "de2",
                    password = MASKED_PASSWORD,
                    message = "Erfolgreich verbunden. Sync laeuft im Hintergrund …",
                )
                // Direkt einen ersten Sync anstossen damit Frank Daten sieht.
                runSyncInternal()
            }.onFailure { ex ->
                _state.value = _state.value.copy(
                    busy = false,
                    message = "Login fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}",
                )
            }
        }
    }

    fun runSync() {
        viewModelScope.launch { runSyncInternal() }
    }

    private suspend fun runSyncInternal() {
        if (!auth.isAuthenticated()) {
            _state.value = _state.value.copy(message = "Bitte erst anmelden.")
            return
        }
        _state.value = _state.value.copy(busy = true, message = "Sync laeuft …")
        val result = repo.syncLastDays(365)
        result.onSuccess { count ->
            _state.value = _state.value.copy(
                busy = false,
                lastSyncMs = secrets.zeppLastSyncEpochMs,
                message = "$count Eintraege geladen.",
            )
        }.onFailure { ex ->
            _state.value = _state.value.copy(
                busy = false,
                message = "Sync fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}",
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = "Trenne Verbindung …")
            auth.logout()
            scheduler.cancelAmazfitSync()
            _state.value = ZeppApiUiState(
                email = "",
                password = "",
                region = secrets.zeppRegion ?: "de2",
                connected = false,
                message = "Verbindung getrennt.",
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    companion object {
        // Platzhalter im Passwort-Feld wenn ein gespeichertes Passwort existiert,
        // damit der Nutzer nicht sieht ob/wie lang sein Passwort ist.
        private const val MASKED_PASSWORD = "••••••••"
    }
}
