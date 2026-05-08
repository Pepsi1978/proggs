package de.frank.entropyreducer.presentation.settings.api

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.remote.calendar.CalendarSignInHelper
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Zustand der OAuth-Verbindungen für Whoop und Google Calendar. */
data class OAuthUiState(
    val calendarAccountEmail: String? = null,
    val whoopConnected: Boolean = false,
    val whoopClientId: String = "",
    val whoopClientSecret: String = "",
    val whoopRedirectUri: String = OAuthService.WHOOP_REDIRECT_URI_DEFAULT,
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
    private val oauth: OAuthService,
    private val secrets: EncryptedSecretsStore,
    private val scheduler: BackgroundScheduler,
    val calendarSignIn: CalendarSignInHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<OAuthUiState> = _state.asStateFlow()

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

    fun clearMessage() { _state.update { it.copy(message = null) } }

    private companion object {
        val WHOOP_CLIENT_ID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )
    }
}
