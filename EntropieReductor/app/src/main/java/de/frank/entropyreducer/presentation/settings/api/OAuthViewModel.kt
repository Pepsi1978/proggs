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
        secrets.whoopClientId = state.value.whoopClientId.trim().ifBlank { null }
        secrets.whoopClientSecret = state.value.whoopClientSecret.trim().ifBlank { null }
        _state.update { it.copy(message = "Whoop-Credentials gespeichert.") }
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
