package de.frank.entropyreducer.data.remote.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * OAuth-2.0-Wrapper auf Basis von AppAuth fuer Google Calendar und Whoop.
 * Spec §15.4 und §15.5.
 *
 * SETUP:
 *  - Google Calendar: Authorization-Endpoint und Token-Endpoint sind festverdrahtet,
 *    Client-ID kommt aus den BuildConfig-Konstanten oder Settings.
 *  - Whoop: Endpoints festverdrahtet auf api.prod.whoop.com, Client-ID/Secret und
 *    Redirect-URI muss Frank im Whoop-Developer-Dashboard hinterlegen.
 *
 * Persistenz: Der gesamte AuthState wird als JSON in EncryptedSharedPreferences gespeichert.
 * Bei jedem `freshAccessToken()` aktualisiert AppAuth den State automatisch (via Refresh-Token).
 */
@Singleton
class OAuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secrets: EncryptedSecretsStore,
) {

    /** Konfiguration fuer Google Calendar. Endpoints von Google's OpenID-Connect-Discovery. */
    val googleConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token"),
    )

    /** Konfiguration fuer Whoop. Spec §15.4. */
    val whoopConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://api.prod.whoop.com/oauth/oauth2/auth"),
        Uri.parse("https://api.prod.whoop.com/oauth/oauth2/token"),
    )

    /** Gemeinsamer AuthorizationService — leichtgewichtig, kann pro Aufruf erzeugt werden. */
    fun newService(): AuthorizationService = AuthorizationService(context)

    /* ============================ Google Calendar ============================ */

    /**
     * Baut den Authorization-Intent fuer Google Calendar. Die UI startet ihn
     * via ActivityResultContract.
     */
    fun buildGoogleAuthIntent(clientId: String): Intent {
        val request = AuthorizationRequest.Builder(
            googleConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(GOOGLE_REDIRECT_URI),
        )
            .setScopes(GOOGLE_SCOPE_CALENDAR_READONLY, "email")
            .setPrompt("consent")
            // Fordert Refresh-Token an — Pflicht fuer Background-Sync.
            .setAdditionalParameters(mapOf("access_type" to "offline"))
            .build()
        return newService().getAuthorizationRequestIntent(request)
    }

    fun loadGoogleAuthState(): AuthState {
        val json = secrets.googleAuthStateJson
        return if (json != null) {
            try { AuthState.jsonDeserialize(json) } catch (e: JSONException) { AuthState() }
        } else AuthState()
    }

    fun saveGoogleAuthState(state: AuthState) {
        secrets.googleAuthStateJson = state.jsonSerializeString()
    }

    fun clearGoogleAuthState() {
        secrets.googleAuthStateJson = null
        secrets.googleAccessToken = null
        secrets.googleRefreshToken = null
        secrets.googleTokenExpiryEpochSec = 0L
    }

    /**
     * Verarbeitet das Ergebnis des Authorization-Intents. Tauscht den Code gegen
     * Tokens und speichert den AuthState.
     */
    suspend fun handleGoogleAuthResult(intent: Intent, clientId: String): Result<Unit> {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (resp == null) {
            return Result.failure(ex ?: IllegalStateException("Keine Authorization-Antwort"))
        }
        val state = AuthState(resp, ex)
        saveGoogleAuthState(state)

        val tokenResult = exchangeToken(resp.createTokenExchangeRequest())
        return tokenResult.onSuccess { tokenResp ->
            state.update(tokenResp, null)
            saveGoogleAuthState(state)
            secrets.googleAccessToken = tokenResp.accessToken
            secrets.googleRefreshToken = tokenResp.refreshToken
            secrets.googleTokenExpiryEpochSec = tokenResp.accessTokenExpirationTime?.div(1000L) ?: 0L
        }.map { Unit }
    }

    /**
     * Liefert ein gueltiges Access-Token fuer Google Calendar. Refreshed transparent
     * wenn das Token abgelaufen ist.
     */
    suspend fun freshGoogleAccessToken(): String? {
        val state = loadGoogleAuthState()
        if (!state.isAuthorized) return null
        val service = newService()
        return suspendCancellableCoroutine { cont ->
            state.performActionWithFreshTokens(service) { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, "Google-Token-Refresh fehlgeschlagen", ex)
                    cont.resume(null)
                } else {
                    saveGoogleAuthState(state)
                    secrets.googleAccessToken = accessToken
                    cont.resume(accessToken)
                }
                service.dispose()
            }
        }
    }

    /* =================================== Whoop =================================== */

    fun buildWhoopAuthIntent(clientId: String, redirectUri: String): Intent {
        val request = AuthorizationRequest.Builder(
            whoopConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri),
        )
            .setScopes(*WHOOP_SCOPES.toTypedArray())
            .build()
        return newService().getAuthorizationRequestIntent(request)
    }

    fun loadWhoopAuthState(): AuthState {
        val json = secrets.whoopAuthStateJson
        return if (json != null) {
            try { AuthState.jsonDeserialize(json) } catch (e: JSONException) { AuthState() }
        } else AuthState()
    }

    fun saveWhoopAuthState(state: AuthState) {
        secrets.whoopAuthStateJson = state.jsonSerializeString()
    }

    fun clearWhoopAuthState() {
        secrets.whoopAuthStateJson = null
        secrets.whoopAccessToken = null
        secrets.whoopRefreshToken = null
        secrets.whoopTokenExpiryEpochSec = 0L
    }

    suspend fun handleWhoopAuthResult(intent: Intent, clientSecret: String?): Result<Unit> {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (resp == null) {
            return Result.failure(ex ?: IllegalStateException("Keine Whoop-Authorization-Antwort"))
        }
        val state = AuthState(resp, ex)
        saveWhoopAuthState(state)

        // Whoop verlangt Client-Secret beim Token-Exchange (Confidential-Client).
        val tokenRequest = if (clientSecret != null) {
            resp.createTokenExchangeRequest(mapOf("client_secret" to clientSecret))
        } else {
            resp.createTokenExchangeRequest()
        }
        val tokenResult = exchangeToken(tokenRequest)
        return tokenResult.onSuccess { tokenResp ->
            state.update(tokenResp, null)
            saveWhoopAuthState(state)
            secrets.whoopAccessToken = tokenResp.accessToken
            secrets.whoopRefreshToken = tokenResp.refreshToken
            secrets.whoopTokenExpiryEpochSec = tokenResp.accessTokenExpirationTime?.div(1000L) ?: 0L
        }.map { Unit }
    }

    suspend fun freshWhoopAccessToken(): String? {
        val state = loadWhoopAuthState()
        if (!state.isAuthorized) return null
        val service = newService()
        return suspendCancellableCoroutine { cont ->
            state.performActionWithFreshTokens(service) { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, "Whoop-Token-Refresh fehlgeschlagen", ex)
                    cont.resume(null)
                } else {
                    saveWhoopAuthState(state)
                    secrets.whoopAccessToken = accessToken
                    cont.resume(accessToken)
                }
                service.dispose()
            }
        }
    }

    /* ================================== Helper ================================== */

    private suspend fun exchangeToken(
        request: net.openid.appauth.TokenRequest,
    ): Result<TokenResponse> = suspendCancellableCoroutine { cont ->
        val service = newService()
        service.performTokenRequest(request) { resp, ex ->
            if (resp != null) {
                cont.resume(Result.success(resp))
            } else {
                cont.resume(Result.failure(ex ?: IllegalStateException("Token-Exchange fehlgeschlagen")))
            }
            service.dispose()
        }
    }

    companion object {
        private const val TAG = "OAuthService"
        const val GOOGLE_REDIRECT_URI = "de.frank.entropyreducer:/oauth/google/callback"
        const val WHOOP_REDIRECT_URI_DEFAULT = "de.frank.entropyreducer:/oauth/whoop/callback"
        const val GOOGLE_SCOPE_CALENDAR_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
        val WHOOP_SCOPES = listOf(
            "read:recovery",
            "read:cycles",
            "read:sleep",
            "read:workout",
            "read:profile",
            "offline",
        )
    }
}
