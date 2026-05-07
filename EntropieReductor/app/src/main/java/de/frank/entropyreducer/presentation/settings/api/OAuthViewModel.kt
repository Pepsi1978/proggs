package de.frank.entropyreducer.presentation.settings.api

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Zustand der OAuth-Verbindungen fuer Whoop und Google Calendar. */
data class OAuthUiState(
    val googleCalendarConnected: Boolean = false,
    val whoopConnected: Boolean = false,
    val whoopClientId: String = "",
    val whoopClientSecret: String = "",
    val whoopRedirectUri: String = OAuthService.WHOOP_REDIRECT_URI_DEFAULT,
    val googleClientId: String = "",
    val message: String? = null,
)

/**
 * ViewModel fuer die OAuth-Cards im API-Keys-Bildschirm. Triggert die Authorization-Intents
 * und verarbeitet das Ergebnis.
 *
 * Spec §6.1, §15.4, §15.5.
 */
@HiltViewModel
class OAuthViewModel @Inject constructor(
    private val oauth: OAuthService,
    private val secrets: EncryptedSecretsStore,
    private val scheduler: BackgroundScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<OAuthUiState> = _state.asStateFlow()

    private fun loadInitial(): OAuthUiState = OAuthUiState(
        googleCalendarConnected = oauth.loadGoogleAuthState().isAuthorized,
        whoopConnected = oauth.loadWhoopAuthState().isAuthorized,
        whoopClientId = secrets.whoopClientId.orEmpty(),
        whoopClientSecret = secrets.whoopClientSecret.orEmpty(),
    )

    fun setWhoopClientId(value: String) { _state.update { it.copy(whoopClientId = value) } }
    fun setWhoopClientSecret(value: String) { _state.update { it.copy(whoopClientSecret = value) } }
    fun setGoogleClientId(value: String) { _state.update { it.copy(googleClientId = value) } }

    fun saveWhoopCredentials() {
        // Poka-Yoke: Client-ID muss UUID-aehnlich aussehen (Hex+Bindestriche, max 64 Zeichen).
        // Verhindert versehentliches Paste eines kompletten Briefes ins ID-Feld.
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

    private companion object {
        // UUID-Format: 8-4-4-4-12 hex digits, with dashes. 36 Zeichen total.
        val WHOOP_CLIENT_ID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )
    }

    /** Liefert den Authorization-Intent fuer Google Calendar — die UI startet ihn. */
    fun buildGoogleAuthIntent(clientId: String): Intent? {
        if (clientId.isBlank()) {
            _state.update { it.copy(message = "Bitte zuerst die Google-Client-ID eintragen.") }
            return null
        }
        return oauth.buildGoogleAuthIntent(clientId)
    }

    fun buildWhoopAuthIntent(): Intent? {
        val clientId = secrets.whoopClientId
        if (clientId.isNullOrBlank()) {
            _state.update { it.copy(message = "Bitte zuerst die Whoop-Client-ID speichern.") }
            return null
        }
        return oauth.buildWhoopAuthIntent(clientId, state.value.whoopRedirectUri)
    }

    fun onGoogleAuthResult(intent: Intent, clientId: String) {
        viewModelScope.launch {
            val result = oauth.handleGoogleAuthResult(intent, clientId)
            result.onSuccess {
                _state.update {
                    it.copy(googleCalendarConnected = true, message = "Google Calendar verbunden.")
                }
                scheduler.runCalendarSyncNow()
                scheduler.ensureNightlyJobs()
            }.onFailure { ex ->
                _state.update { it.copy(message = "Google-Auth fehlgeschlagen: ${ex.message}") }
            }
        }
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

    fun disconnectGoogleCalendar() {
        oauth.clearGoogleAuthState()
        scheduler.cancelCalendarSync()
        _state.update { it.copy(googleCalendarConnected = false, message = "Google Calendar getrennt.") }
    }

    fun disconnectWhoop() {
        oauth.clearWhoopAuthState()
        scheduler.cancelWhoopSync()
        _state.update { it.copy(whoopConnected = false, message = "Whoop getrennt.") }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }
}
