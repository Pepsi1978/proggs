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
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * OAuth-2.0-Wrapper auf Basis von AppAuth für Google Calendar und Whoop.
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

    /** Konfiguration für Google Calendar. Endpoints von Google's OpenID-Connect-Discovery. */
    val googleConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token"),
    )

    /** Konfiguration für Whoop. Spec §15.4. */
    val whoopConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://api.prod.whoop.com/oauth/oauth2/auth"),
        Uri.parse("https://api.prod.whoop.com/oauth/oauth2/token"),
    )

    /**
     * Konfiguration fuer Polar AccessLink (Frank-Wunsch 2026-05-16).
     * Auth- und Token-Endpoint laufen auf VERSCHIEDENEN Domains:
     *  - Authorize: flow.polar.com (Polar-Flow-Login-Seite)
     *  - Token:     polarremote.com (separater OAuth-Server)
     * AppAuth kommt damit klar — die zwei URIs werden unabhaengig verwendet.
     *
     * KRITISCH: Polar verlangt HTTP-Basic-Auth fuer den Token-Endpoint
     * (`ClientSecretBasic`), NICHT Body-Auth (`ClientSecretPost` wie Whoop/Strava).
     */
    val polarConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://flow.polar.com/oauth2/authorization"),
        Uri.parse("https://polarremote.com/v2/oauth2/token"),
    )

    /**
     * Polar V4 (Dynamic API) OAuth-Config. Komplett separat von V3 weil:
     *  - Auth-Endpoint: auth.polar.com/oauth/authorize (V3: flow.polar.com)
     *  - Token-Endpoint: auth.polar.com/oauth/token (V3: polarremote.com)
     *  - Scopes: training_sessions:read, routes:read, training_targets:read
     *  - V4 liefert ALLE Workouts (auch alte) inkl. Streams — non-destruktiv
     */
    val polarV4Config = AuthorizationServiceConfiguration(
        Uri.parse("https://auth.polar.com/oauth/authorize"),
        Uri.parse("https://auth.polar.com/oauth/token"),
    )

    /** Gemeinsamer AuthorizationService — leichtgewichtig, kann pro Aufruf erzeugt werden. */
    fun newService(): AuthorizationService = AuthorizationService(context)

    /* ============================ Google Calendar ============================ */

    /**
     * Baut den Authorization-Intent für Google Calendar. Die UI startet ihn
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
            // Fordert Refresh-Token an — Pflicht für Background-Sync.
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
        // WICHTIG: AuthState erst NACH erfolgreichem Token-Exchange persistieren.
        // Sonst hätten wir einen State mit isAuthorized=true aber ohne Token —
        // freshGoogleAccessToken würde silent failen und der Nutzer denkt verbunden.
        val state = AuthState(resp, ex)
        val tokenResult = exchangeToken(resp.createTokenExchangeRequest())
        return tokenResult.onSuccess { tokenResp ->
            state.update(tokenResp, null)
            saveGoogleAuthState(state)
            secrets.googleAccessToken = tokenResp.accessToken
            secrets.googleRefreshToken = tokenResp.refreshToken
            secrets.googleTokenExpiryEpochSec = tokenResp.accessTokenExpirationTime?.div(1000L) ?: 0L
        }.onFailure {
            // Bei Token-Fehler: vorhandenen State löschen, damit kein Halb-Zustand entsteht.
            clearGoogleAuthState()
        }.map { Unit }
    }

    /**
     * Liefert ein gueltiges Access-Token für Google Calendar. Refreshed transparent
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
        // Diagnostik: volle Client-ID + Laenge — UUIDs sind nicht geheim, tauchen in der
        // OAuth-URL im Browser ohnehin auf. Hilft den "client_does_not_exist"-Bug einzugrenzen.
        Log.d(TAG, "Whoop: buildAuthIntent — clientId='$clientId' (Länge=${clientId.length}), redirect='$redirectUri', scopes=$WHOOP_SCOPES")
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
        Log.d(TAG, "Whoop: handleAuthResult — clientSecret-vorhanden=${clientSecret != null}")
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (ex != null) {
            Log.e(TAG, "Whoop: AuthorizationException type=${ex.type} code=${ex.code} error=${ex.error} desc=${ex.errorDescription} uri=${ex.errorUri}")
        }
        if (resp == null) {
            return Result.failure(ex ?: IllegalStateException("Keine Whoop-Authorization-Antwort"))
        }
        Log.d(TAG, "Whoop: Authorization OK — code-Länge=${resp.authorizationCode?.length ?: 0}, state=${resp.state}")
        // Spiegel zur Google-Logik: AuthState erst nach erfolgreichem Token-Exchange.
        val state = AuthState(resp, ex)
        // Whoop verlangt Client-Secret beim Token-Exchange (Confidential-Client).
        val tokenRequest = if (clientSecret != null) {
            resp.createTokenExchangeRequest(mapOf("client_secret" to clientSecret))
        } else {
            resp.createTokenExchangeRequest()
        }
        Log.d(TAG, "Whoop: Token-Exchange-Request gebaut — additionalParams-Keys=${tokenRequest.additionalParameters.keys}")
        val tokenResult = exchangeToken(tokenRequest)
        return tokenResult.onSuccess { tokenResp ->
            Log.i(TAG, "Whoop: Token-Exchange erfolgreich — access-Länge=${tokenResp.accessToken?.length ?: 0}, refresh-vorhanden=${tokenResp.refreshToken != null}, expires=${tokenResp.accessTokenExpirationTime}")
            state.update(tokenResp, null)
            saveWhoopAuthState(state)
            secrets.whoopAccessToken = tokenResp.accessToken
            secrets.whoopRefreshToken = tokenResp.refreshToken
            secrets.whoopTokenExpiryEpochSec = tokenResp.accessTokenExpirationTime?.div(1000L) ?: 0L
        }.onFailure { failure ->
            Log.e(TAG, "Whoop: Token-Exchange fehlgeschlagen — class=${failure.javaClass.simpleName}, message=${failure.message}", failure)
            if (failure is AuthorizationException) {
                Log.e(TAG, "Whoop: AuthException-Details — type=${failure.type} code=${failure.code} error=${failure.error} desc=${failure.errorDescription} uri=${failure.errorUri}")
            }
            clearWhoopAuthState()
        }.map { Unit }
    }

    /**
     * Liefert ein gueltiges Access-Token fuer Whoop. Refreshed transparent wenn das
     * alte abgelaufen ist.
     *
     * Whoop ist ein Confidential Client — beim Refresh-Request MUSS das
     * Client-Secret mitgeschickt werden. AppAuth's einfache
     * performActionWithFreshTokens(service) Variante schickt es NICHT mit, was zu
     * einem stillen invalid_client-Fehler bei jedem Refresh fuehrt — und damit
     * dazu dass der Nutzer sich ca. jede Stunde neu anmelden muss.
     *
     * Direktive 3 — Resilient Bugfixing, vier Schichten:
     *   Schicht 1 (Praeventiv) — ClientSecretPost beim Refresh mitsenden
     *   Schicht 2 (Reaktiv) — bei fehlendem Secret KLARE Exception werfen statt
     *                         silent null zurueckgeben
     *   Schicht 3 (Selbstheilend) — bei Refresh-Fehler AuthState resetten, damit
     *                               UI-Anzeige nicht widerspruechlich bleibt
     *                               (Settings sagen "verbunden", Banner sagt "anmelden")
     *   Schicht 4 (Diagnose) — detaillierte Logs damit naechster Vorfall in 30s
     *                          diagnostiziert werden kann
     */
    suspend fun freshWhoopAccessToken(): String? {
        val state = loadWhoopAuthState()
        if (!state.isAuthorized) {
            Log.d(TAG, "Whoop-Refresh: kein AuthState — kein Token zurückgeben")
            return null
        }
        val clientSecret = secrets.whoopClientSecret
        if (clientSecret.isNullOrBlank()) {
            Log.e(TAG, "Whoop-Refresh: Client-Secret fehlt im EncryptedSecretsStore")
            throw IllegalStateException(
                "Whoop-Client-Secret fehlt — bitte in den API-Schlüssel-Settings neu eingeben.",
            )
        }
        // Schicht 4 — DIAGNOSE-SONDEN (erweitert): Vor dem Refresh den AuthState
        // detailliert loggen damit man bei invalid_client genau sieht was fehlt.
        val rt = state.refreshToken
        val at = state.accessToken
        val expiry = state.accessTokenExpirationTime
        val nowMs = System.currentTimeMillis()
        val expiryDeltaMs = if (expiry != null) expiry - nowMs else Long.MIN_VALUE
        val needsRefresh = state.needsTokenRefresh
        val scope = state.scope
        val clientId = secrets.whoopClientId
        // Performance-Audit Loop 1 (2026-05-10): Eager 7-Feld-Interpolation pro
        // Token-Refresh entfernt. Token-Refresh laeuft 1x/Stunde — pro Aufruf wurde
        // ein 200-Zeichen-String allokiert nur fuer Logcat. Knappe Form unter Guard.
        if (de.frank.entropyreducer.BuildConfig.DEBUG) {
            Log.d(TAG, "Whoop-Refresh: needsRefresh=$needsRefresh expiryDeltaMs=$expiryDeltaMs")
        }
        if (rt.isNullOrBlank()) {
            Log.e(TAG, "Whoop-Refresh: KEIN refreshToken im AuthState — Whoop hat beim Login keinen Refresh-Token zurückgegeben (offline-Scope nicht akzeptiert?). Kein Refresh möglich.")
            // Frank's Wunsch 2026-05-09: alte Daten erhalten lassen wenn Refresh
            // nicht moeglich. NICHT clearWhoopAuthState() rufen — der State bleibt,
            // die Settings zeigen weiter "verbunden", die DB-Daten bleiben sichtbar.
            // UI zeigt im Banner ehrlich an: "Refresh-Token fehlt — neu anmelden".
            throw IllegalStateException(
                "Whoop-Refresh-Token fehlt — bitte unter API-Schlüssel neu anmelden.",
            )
        }
        val clientAuth = ClientSecretPost(clientSecret)
        val service = newService()
        return suspendCancellableCoroutine { cont ->
            // Schicht 1 — praeventiv: ClientSecretPost als ClientAuthentication
            // mitsenden. AppAuth haengt das Client-Secret an den Token-Refresh-
            // Request, Whoop akzeptiert, neuer Access-Token kommt zurueck.
            state.performActionWithFreshTokens(service, clientAuth) { accessToken, _, ex ->
                if (ex != null) {
                    // Schicht 4 — Diagnose: alles was Whoop geschickt hat in den Log
                    Log.e(TAG, "Whoop-Token-Refresh fehlgeschlagen — type=${ex.type} code=${ex.code} error=${ex.error} desc=${ex.errorDescription} uri=${ex.errorUri}", ex)
                    // Schicht 3 — Frank-Wunsch 2026-05-09: NICHT mehr clearen.
                    // Wenn der Refresh fehlschlaegt, bleibt der AuthState bestehen.
                    // Vorteile: alte DB-Daten bleiben sichtbar, Settings zeigen
                    // weiter "verbunden", Frank kann manuell neu anmelden wenn er
                    // will. Nachteil (akzeptiert): UI ist etwas widerspruechlich,
                    // aber besser als Datenverlust.
                    cont.resume(null)
                } else {
                    Log.d(TAG, "Whoop-Token-Refresh OK — neuer Access-Token erhalten (Länge=${accessToken?.length ?: 0})")
                    saveWhoopAuthState(state)
                    secrets.whoopAccessToken = accessToken
                    cont.resume(accessToken)
                }
                service.dispose()
            }
        }
    }

    /* =================================== Polar =================================== */

    /**
     * Frank-Wunsch 2026-05-16: Polar AccessLink wird die ALLEINIGE Workout-Quelle
     * nachdem Strava externe Brustgurt-HR-Daten verloren hat. Polar zieht die
     * H10-Daten vom gekoppelten Brustgurt sauber in seine Cloud, und liefert sie
     * uns ueber die API genauso wie wenn die Uhr selbst gemessen haette.
     *
     * UNTERSCHIEDE zu Whoop/Strava:
     *  - HTTP-Basic-Auth (ClientSecretBasic), nicht ClientSecretPost
     *  - KEIN Refresh-Token, dafuer Tokens mit ~1 Jahr Laufzeit
     *  - User-ID kommt im Token-Response als `x_user_id` zurueck — speichern wir
     *    in `secrets.polarUserId` weil sie in jedem nachfolgenden API-Pfad steht
     */
    fun buildPolarAuthIntent(clientId: String, redirectUri: String): Intent {
        Log.d(TAG, "Polar: buildAuthIntent — clientId='$clientId' redirect='$redirectUri'")
        val request = AuthorizationRequest.Builder(
            polarConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri),
        )
            .setScopes(*POLAR_SCOPES.toTypedArray())
            .build()
        return newService().getAuthorizationRequestIntent(request)
    }

    fun loadPolarAuthState(): AuthState {
        val json = secrets.polarAuthStateJson
        return if (json != null) {
            try { AuthState.jsonDeserialize(json) } catch (e: JSONException) { AuthState() }
        } else AuthState()
    }

    fun savePolarAuthState(state: AuthState) {
        secrets.polarAuthStateJson = state.jsonSerializeString()
    }

    fun clearPolarAuthState() {
        secrets.clearPolarAuthState()
    }

    /**
     * Verarbeitet das Result des Polar-OAuth-Intents.
     * Polar verlangt HTTP-Basic-Auth, also bauen wir den Token-Request ohne
     * client_secret im Body und uebergeben `ClientSecretBasic` an
     * `performTokenRequest`.
     *
     * Aus dem Token-Response extrahieren wir `x_user_id` (Polar-User-ID) und
     * speichern sie — sie wird in JEDEM nachfolgenden API-Pfad als {user-id}
     * gebraucht.
     */
    suspend fun handlePolarAuthResult(intent: Intent, clientSecret: String?): Result<Unit> {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (ex != null) {
            Log.e(TAG, "Polar: AuthException type=${ex.type} code=${ex.code} error=${ex.error} desc=${ex.errorDescription}")
        }
        if (resp == null) {
            return Result.failure(ex ?: IllegalStateException("Keine Polar-Authorization-Antwort"))
        }
        if (clientSecret.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Polar-Client-Secret fehlt — bitte in den API-Settings eintragen"))
        }
        val state = AuthState(resp, ex)
        // Polar's Token-Endpoint verlangt KEIN client_secret im Body — das kommt
        // ueber den Authorization-Header (Basic Auth) via ClientSecretBasic beim
        // performTokenRequest. Daher hier KEINE additionalParameters fuer
        // client_secret setzen.
        val tokenRequest = resp.createTokenExchangeRequest()
        val clientAuth = ClientSecretBasic(clientSecret)
        val tokenResult = exchangeTokenWithAuth(tokenRequest, clientAuth)
        return tokenResult.onSuccess { tokenResp ->
            Log.i(TAG, "Polar: Token-Exchange OK — access-Laenge=${tokenResp.accessToken?.length ?: 0}")
            state.update(tokenResp, null)
            savePolarAuthState(state)
            // x_user_id aus den additionalParameters fischen — AppAuth legt
            // unbekannte Token-Response-Felder dort ab.
            val xUserIdRaw = tokenResp.additionalParameters["x_user_id"]
            val parsedUserId = xUserIdRaw?.toLongOrNull()
            if (parsedUserId != null && parsedUserId > 0L) {
                secrets.polarUserId = parsedUserId
                Log.d(TAG, "Polar: x_user_id=$parsedUserId gespeichert")
            } else {
                Log.w(TAG, "Polar: x_user_id NICHT im Token-Response — additionalParams=${tokenResp.additionalParameters.keys}")
            }
        }.onFailure { failure ->
            Log.e(TAG, "Polar: Token-Exchange fehlgeschlagen — ${failure.message}", failure)
            clearPolarAuthState()
        }.map { Unit }
    }

    /**
     * Frischer Access-Token fuer Polar. Polar hat KEIN Refresh-Token —
     * solange der gespeicherte Token nicht widerrufen wurde, gibt AppAuth ihn
     * direkt zurueck. Bei Ablauf (~1 Jahr) oder Revocation kommt null und
     * der Aufrufer muss den User zur Neu-Autorisierung schicken.
     */
    suspend fun freshPolarAccessToken(): String? {
        val state = loadPolarAuthState()
        if (!state.isAuthorized) {
            Log.d(TAG, "Polar-Refresh: kein AuthState — kein Token")
            return null
        }
        // Da Polar keine Refresh-Tokens vergibt, gibt AppAuth den gespeicherten
        // Access-Token direkt zurueck. Wir setzen trotzdem ClientSecretBasic
        // damit der eventuelle Refresh-Versuch nicht in einen 401 laeuft.
        val clientSecret = secrets.polarClientSecret
        val clientAuth = if (!clientSecret.isNullOrBlank()) ClientSecretBasic(clientSecret) else null
        val service = newService()
        return suspendCancellableCoroutine { cont ->
            val callback = net.openid.appauth.AuthState.AuthStateAction { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, "Polar-Token-Refresh fehlgeschlagen — type=${ex.type} error=${ex.error} desc=${ex.errorDescription}", ex)
                    cont.resume(null)
                } else {
                    savePolarAuthState(state)
                    cont.resume(accessToken)
                }
                service.dispose()
            }
            if (clientAuth != null) {
                state.performActionWithFreshTokens(service, clientAuth, callback)
            } else {
                state.performActionWithFreshTokens(service, callback)
            }
        }
    }

    /* ================================== Polar V4 ================================== */

    fun buildPolarV4AuthIntent(clientId: String, redirectUri: String): Intent {
        Log.d(TAG, "PolarV4: buildAuthIntent — clientId='$clientId' redirect='$redirectUri'")
        val request = AuthorizationRequest.Builder(
            polarV4Config,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri),
        )
            .setScopes(*POLAR_V4_SCOPES.toTypedArray())
            .build()
        return newService().getAuthorizationRequestIntent(request)
    }

    fun loadPolarV4AuthState(): AuthState {
        val json = secrets.polarV4AuthStateJson
        return if (json != null) {
            try { AuthState.jsonDeserialize(json) } catch (e: JSONException) { AuthState() }
        } else AuthState()
    }

    fun savePolarV4AuthState(state: AuthState) {
        secrets.polarV4AuthStateJson = state.jsonSerializeString()
    }

    fun clearPolarV4AuthState() {
        secrets.polarV4AuthStateJson = null
    }

    suspend fun handlePolarV4AuthResult(intent: Intent, clientSecret: String?): Result<Unit> {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (ex != null) {
            Log.e(TAG, "PolarV4: AuthException type=${ex.type} code=${ex.code} error=${ex.error} desc=${ex.errorDescription}")
        }
        if (resp == null) {
            return Result.failure(ex ?: IllegalStateException("Keine PolarV4-Authorization-Antwort"))
        }
        if (clientSecret.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Polar-Client-Secret fehlt"))
        }
        val state = AuthState(resp, ex)
        val tokenRequest = resp.createTokenExchangeRequest()
        val clientAuth = ClientSecretBasic(clientSecret)
        val tokenResult = exchangeTokenWithAuth(tokenRequest, clientAuth)
        return tokenResult.onSuccess { tokenResp ->
            Log.i(TAG, "PolarV4: Token-Exchange OK — access-Laenge=${tokenResp.accessToken?.length ?: 0}")
            state.update(tokenResp, null)
            savePolarV4AuthState(state)
        }.onFailure { failure ->
            Log.e(TAG, "PolarV4: Token-Exchange fehlgeschlagen — ${failure.message}", failure)
            clearPolarV4AuthState()
        }.map { Unit }
    }

    suspend fun freshPolarV4AccessToken(): String? {
        val state = loadPolarV4AuthState()
        if (!state.isAuthorized) {
            Log.d(TAG, "PolarV4: kein AuthState — kein Token")
            return null
        }
        val clientSecret = secrets.polarClientSecret
        val clientAuth = if (!clientSecret.isNullOrBlank()) ClientSecretBasic(clientSecret) else null
        val service = newService()
        return suspendCancellableCoroutine { cont ->
            val callback = net.openid.appauth.AuthState.AuthStateAction { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, "PolarV4-Token-Refresh fehlgeschlagen — ${ex.message}", ex)
                    cont.resume(null)
                } else {
                    savePolarV4AuthState(state)
                    cont.resume(accessToken)
                }
                service.dispose()
            }
            if (clientAuth != null) {
                state.performActionWithFreshTokens(service, clientAuth, callback)
            } else {
                state.performActionWithFreshTokens(service, callback)
            }
        }
    }

    /* ================================== Helper ================================== */

    private suspend fun exchangeToken(
        request: net.openid.appauth.TokenRequest,
    ): Result<TokenResponse> = suspendCancellableCoroutine { cont ->
        val service = newService()
        Log.d(TAG, "exchangeToken — endpoint=${request.configuration.tokenEndpoint}, grantType=${request.grantType}")
        service.performTokenRequest(request) { resp, ex ->
            if (resp != null) {
                cont.resume(Result.success(resp))
            } else {
                Log.e(TAG, "exchangeToken FAIL — type=${ex?.type} code=${ex?.code} error=${ex?.error} desc=${ex?.errorDescription} uri=${ex?.errorUri}")
                cont.resume(Result.failure(ex ?: IllegalStateException("Token-Exchange fehlgeschlagen")))
            }
            service.dispose()
        }
    }

    /**
     * Wie exchangeToken, aber mit expliziter ClientAuthentication (HTTP Basic
     * Auth fuer Polar — Polar verlangt das beim Token-Endpoint).
     */
    private suspend fun exchangeTokenWithAuth(
        request: net.openid.appauth.TokenRequest,
        clientAuth: net.openid.appauth.ClientAuthentication,
    ): Result<TokenResponse> = suspendCancellableCoroutine { cont ->
        val service = newService()
        Log.d(TAG, "exchangeTokenWithAuth — endpoint=${request.configuration.tokenEndpoint}, grantType=${request.grantType}")
        service.performTokenRequest(request, clientAuth) { resp, ex ->
            if (resp != null) {
                cont.resume(Result.success(resp))
            } else {
                Log.e(TAG, "exchangeTokenWithAuth FAIL — type=${ex?.type} code=${ex?.code} error=${ex?.error} desc=${ex?.errorDescription} uri=${ex?.errorUri}")
                cont.resume(Result.failure(ex ?: IllegalStateException("Token-Exchange fehlgeschlagen")))
            }
            service.dispose()
        }
    }

    companion object {
        private const val TAG = "OAuthService"
        // Whoop's Developer-Dashboard-Validator akzeptiert nur Redirect-URIs mit
        // "://" (zwei Slashes nach dem Schema). RFC 8252 erlaubt zwar auch ":/",
        // Whoop's Parser aber nicht — daher beide URIs einheitlich auf "://".
        const val GOOGLE_REDIRECT_URI = "de.frank.entropyreducer://oauth/google/callback"
        const val WHOOP_REDIRECT_URI_DEFAULT = "de.frank.entropyreducer://oauth/whoop/callback"
        const val GOOGLE_SCOPE_CALENDAR_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
        val WHOOP_SCOPES = listOf(
            "read:recovery",
            "read:cycles",
            "read:sleep",
            "read:workout",
            "read:profile",
            "offline",
        )

        /**
         * Polar AccessLink (Frank-Wunsch 2026-05-16).
         *
         * Frank-Hinweis aus Strava-Erfahrung (Bug-Case 2026-05-16): Strava parst
         * die Redirect-URI streng und vergleicht den HOST-Teil mit der Callback-
         * Domain im Developer-Dashboard. Wir nehmen denselben localhost-Trick
         * fuer Polar — Frank kann "localhost" als Callback Domain im Polar-
         * Developer-Dashboard eintragen.
         *
         * Polar's Redirect-URI-Validator ist ab August 2024 ebenfalls auf reine
         * URI-Equality umgestellt — d.h. die URI muss EXAKT so eingetragen
         * sein wie hier definiert.
         */
        const val POLAR_REDIRECT_URI_DEFAULT = "de.frank.entropyreducer://localhost/oauth/polar/callback"

        /**
         * Polar's AccessLink hat genau einen relevanten Scope:
         * `accesslink.read_all` — lesender Zugriff auf alle Nutzerdaten
         * (Training, Recovery, Sleep, etc.). Mehrere Scopes-Strings sind in
         * der Doku nicht dokumentiert; wir bleiben bei diesem einen.
         */
        val POLAR_SCOPES = listOf("accesslink.read_all")
        /**
         * Polar V4 (Dynamic API) Scopes.
         *  - training_sessions:read — alle Trainings inkl. Samples + Streams
         *  - routes:read — GPS-Routen
         *  - training_targets:read — Trainings-Ziele
         */
        val POLAR_V4_SCOPES = listOf(
            "training_sessions:read",
            "routes:read",
            "training_targets:read",
        )
    }
}
