package de.frank.entropyreducer.presentation.settings.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class OuraApiUiState(
    /** Was im Eingabefeld steht (entweder eine Maske wenn Token gespeichert oder
     *  der frische Eintrag den Frank gerade tippt). */
    val tokenInput: String = "",
    /** True wenn ein Token im Encrypted-Store hinterlegt ist. */
    val tokenSaved: Boolean = false,
    /** Mailadresse aus /personal_info nach erfolgreichem Verbindungs-Test. */
    val accountEmail: String? = null,
    val lastSyncMs: Long = 0L,
    val busy: Boolean = false,
    val message: String? = null,
)

/**
 * State + Aktionen fuer die Oura-Ring-Karte unter Einstellungen → API.
 * Frank-Wunsch 2026-05-10: Personal Access Token statt OAuth-Flow, weil
 * Single-User-App. Eingabefeld → Speichern → Verbindung testen → Sync.
 */
@HiltViewModel
class OuraApiViewModel @Inject constructor(
    private val repo: OuraRepository,
    private val secrets: EncryptedSecretsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<OuraApiUiState> = _state.asStateFlow()

    private fun initialState(): OuraApiUiState {
        val saved = !secrets.ouraPersonalAccessToken.isNullOrBlank()
        return OuraApiUiState(
            tokenInput = if (saved) MASKED_TOKEN else "",
            tokenSaved = saved,
            lastSyncMs = secrets.ouraLastSyncEpochMs,
        )
    }

    fun setToken(value: String) {
        _state.value = _state.value.copy(tokenInput = value, message = null)
    }

    /**
     * Speichert den Token und testet ihn sofort gegen /personal_info. Bei Erfolg
     * wird automatisch ein erster Sync angestossen damit Frank gleich Daten sieht.
     */
    fun saveAndConnect() {
        val s = _state.value
        // Wenn das Feld noch die Maske enthaelt, behalten wir den gespeicherten Token.
        val effectiveToken = if (s.tokenInput == MASKED_TOKEN) {
            secrets.ouraPersonalAccessToken.orEmpty()
        } else {
            s.tokenInput.trim()
        }
        if (effectiveToken.isBlank()) {
            _state.value = s.copy(message = "Bitte zuerst einen Personal Access Token eintragen.")
            return
        }
        secrets.ouraPersonalAccessToken = effectiveToken
        viewModelScope.launch {
            _state.value = s.copy(busy = true, message = "Teste Verbindung zu Oura …")
            val testResult = runCatching { repo.testToken() }
            testResult.onSuccess { _ ->
                _state.value = _state.value.copy(
                    tokenInput = MASKED_TOKEN,
                    tokenSaved = true,
                    busy = false,
                    message = "Verbunden. Erster Sync laeuft …",
                )
                runSyncInternal()
            }.onFailure { ex ->
                // Fehlerhaftes Token wieder loeschen damit Frank nicht denkt es ist gespeichert.
                secrets.ouraPersonalAccessToken = null
                _state.value = _state.value.copy(
                    tokenInput = "",
                    tokenSaved = false,
                    busy = false,
                    message = "Verbindung fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}",
                )
            }
        }
    }

    fun runSync() {
        viewModelScope.launch { runSyncInternal() }
    }

    private suspend fun runSyncInternal() {
        if (!repo.isTokenConfigured()) {
            _state.value = _state.value.copy(message = "Bitte zuerst einen Token speichern.")
            return
        }
        _state.value = _state.value.copy(busy = true, message = "Oura-Sync laeuft …")
        val result = repo.syncLastDays(365)
        result.onSuccess { counts ->
            val total = counts.values.sum()
            _state.value = _state.value.copy(
                busy = false,
                lastSyncMs = secrets.ouraLastSyncEpochMs,
                message = "$total Eintraege geladen " +
                    "(Readiness: ${counts["readiness"] ?: 0}, " +
                    "Schlaf: ${counts["daily_sleep"] ?: 0}, " +
                    "Aktivitaet: ${counts["activity"] ?: 0}, " +
                    "Resilienz: ${counts["resilience"] ?: 0}, " +
                    "Schlaf-Detail: ${counts["sleep_detail"] ?: 0}).",
            )
        }.onFailure { ex ->
            _state.value = _state.value.copy(
                busy = false,
                message = "Sync fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}",
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = "Trenne Verbindung …")
            repo.clearAll()
            _state.value = OuraApiUiState(
                tokenInput = "",
                tokenSaved = false,
                message = "Verbindung getrennt. Token und Daten geloescht.",
            )
        }
    }

    companion object {
        private const val MASKED_TOKEN = "••••••••••••••••"
    }
}
