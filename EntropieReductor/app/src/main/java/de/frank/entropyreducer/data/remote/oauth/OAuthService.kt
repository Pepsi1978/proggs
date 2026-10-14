package de.frank.entropyreducer.data.remote.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import javax.inject.Singleton
import de.frank.entropyreducer.workers.SyncProblemNotifier
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.json.JSONException

/**
 * OAuth-2.0-Wrapper auf Basis von AppAuth für Google Calendar und Whoop. Spec §15.4 und §15.5.
 *
 * SETUP:
 * - Google Calendar: Authorization-Endpoint und Token-Endpoint sind festverdrahtet, Client-ID kommt
 *   aus den BuildConfig-Konstanten oder Settings.
 * - Whoop: Endpoints festverdrahtet auf api.prod.whoop.com, Client-ID/Secret und Redirect-URI muss
 *   Frank im Whoop-Developer-Dashboard hinterlegen.
 *
 * Persistenz: Der gesamte AuthState wird als JSON in EncryptedSharedPreferences gespeichert. Bei
 * jedem `freshAccessToken()` aktualisiert AppAuth den State automatisch (via Refresh-Token).
 */
@Singleton
class OAuthService
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val secrets: EncryptedSecretsStore,
    private val problemNotifier: SyncProblemNotifier,
) {

    /** Nur EIN Whoop-Refresh gleichzeitig: zwei parallele wuerden denselben Einmal-Token verbrauchen. */
    private val whoopRefreshMutex = Mutex()

    /**
     * Wann zuletzt geprueft wurde, ob Whoop die abgelaufene Anmeldung doch wieder annimmt. Liegt seit
     * 2026-10-14 im gespeicherten Stand: Im Speicher allein begann der 6-Stunden-Abstand bei jedem
     * frisch gestarteten Hintergrund-Prozess von vorn (neuer Versuch und neue Benachrichtigung).
     */
    private var lastWhoopReauthProbeMs: Long
        get() = secrets.whoopReauthProbeMs
        set(value) { secrets.whoopReauthProbeMs = value }

    /** Konfiguration für Google Calendar. Endpoints von Google's OpenID-Connect-Discovery. */
    val googleConfig =
        AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
            Uri.parse("https://oauth2.googleapis.com/token"),
        )

    /** Konfiguration für Whoop. Spec §15.4. */
    val whoopConfig =
        AuthorizationServiceConfiguration(
            Uri.parse("https://api.prod.whoop.com/oauth/oauth2/auth"),
            Uri.parse("https://api.prod.whoop.com/oauth/oauth2/token"),
        )

    // Polar AccessLink V3 + V4 OAuth-Configs entfernt 2026-05-17 (Frank-Wunsch:
    // Polar nur noch ueber ZIP-Bulk-Import, keine API-Anbindung mehr).

    /** Gemeinsamer AuthorizationService — leichtgewichtig, kann pro Aufruf erzeugt werden. */
    fun newService(): AuthorizationService = AuthorizationService(context)

    /* ============================ Google Calendar ============================ */

    /**
     * Baut den Authorization-Intent für Google Calendar. Die UI startet ihn via
     * ActivityResultContract.
     */
    fun buildGoogleAuthIntent(clientId: String): Intent {
        val request =
            AuthorizationRequest.Builder(
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
            try {
                AuthState.jsonDeserialize(json)
            } catch (e: JSONException) {
                AuthState()
            }
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
     * Verarbeitet das Ergebnis des Authorization-Intents. Tauscht den Code gegen Tokens und
     * speichert den AuthState.
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
        return tokenResult
            .onSuccess { tokenResp ->
                state.update(tokenResp, null)
                saveGoogleAuthState(state)
                secrets.googleAccessToken = tokenResp.accessToken
                secrets.googleRefreshToken = tokenResp.refreshToken
                secrets.googleTokenExpiryEpochSec =
                    tokenResp.accessTokenExpirationTime?.div(1000L) ?: 0L
            }
            .onFailure {
                // Bei Token-Fehler: vorhandenen State löschen, damit kein Halb-Zustand entsteht.
                clearGoogleAuthState()
            }
            .map { Unit }
    }

    /**
     * Liefert ein gueltiges Access-Token für Google Calendar. Refreshed transparent wenn das Token
     * abgelaufen ist.
     */
    suspend fun freshGoogleAccessToken(): String? {
        val state = loadGoogleAuthState()
        if (!state.isAuthorized) return null
        val service = newService()
        return suspendCancellableCoroutine { cont ->
            state.performActionWithFreshTokens(service) { accessToken, _, ex ->
                if (ex != null) {
                    Diag.e(DiagnosticArea.OAUTH, TAG, "Google-Token-Refresh fehlgeschlagen", ex)
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
        Diag.d(DiagnosticArea.OAUTH, 
            TAG,
            "Whoop: buildAuthIntent — clientId='$clientId' (Länge=${clientId.length}), redirect='$redirectUri', scopes=$WHOOP_SCOPES",
        )
        val request =
            AuthorizationRequest.Builder(
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
            try {
                AuthState.jsonDeserialize(json)
            } catch (e: JSONException) {
                AuthState()
            }
        } else AuthState()
    }

    fun saveWhoopAuthState(state: AuthState) {
        secrets.whoopAuthStateJson = state.jsonSerializeString()
    }

    fun clearWhoopAuthState() {
        secrets.whoopReauthSinceMs = 0L
        secrets.whoopAuthStateJson = null
        secrets.whoopAccessToken = null
        secrets.whoopRefreshToken = null
        secrets.whoopTokenExpiryEpochSec = 0L
    }

    suspend fun handleWhoopAuthResult(intent: Intent, clientSecret: String?): Result<Unit> {
        Diag.d(DiagnosticArea.OAUTH, TAG, "Whoop: handleAuthResult — clientSecret-vorhanden=${clientSecret != null}")
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (ex != null) {
            Diag.e(DiagnosticArea.OAUTH, 
                TAG,
                "Whoop: AuthorizationException type=${ex.type} code=${ex.code} error=${ex.error} desc=${ex.errorDescription} uri=${ex.errorUri}",
            )
        }
        if (resp == null) {
            return Result.failure(ex ?: IllegalStateException("Keine Whoop-Authorization-Antwort"))
        }
        Diag.d(DiagnosticArea.OAUTH, 
            TAG,
            "Whoop: Authorization OK — code-Länge=${resp.authorizationCode?.length ?: 0}, state=${resp.state}",
        )
        // Spiegel zur Google-Logik: AuthState erst nach erfolgreichem Token-Exchange.
        val state = AuthState(resp, ex)
        // Whoop verlangt Client-Secret beim Token-Exchange (Confidential-Client).
        val tokenRequest =
            if (clientSecret != null) {
                resp.createTokenExchangeRequest(mapOf("client_secret" to clientSecret))
            } else {
                resp.createTokenExchangeRequest()
            }
        Diag.d(DiagnosticArea.OAUTH, 
            TAG,
            "Whoop: Token-Exchange-Request gebaut — additionalParams-Keys=${tokenRequest.additionalParameters.keys}",
        )
        val tokenResult = exchangeToken(tokenRequest)
        return tokenResult
            .onSuccess { tokenResp ->
                Diag.i(DiagnosticArea.OAUTH, 
                    TAG,
                    "Whoop: Token-Exchange erfolgreich — access-Länge=${tokenResp.accessToken?.length ?: 0}, refresh-vorhanden=${tokenResp.refreshToken != null}, expires=${tokenResp.accessTokenExpirationTime}",
                )
                state.update(tokenResp, null)
                saveWhoopAuthState(state)
                secrets.whoopReauthSinceMs = 0L
                problemNotifier.whoopWiederOk()
                secrets.whoopAccessToken = tokenResp.accessToken
                secrets.whoopRefreshToken = tokenResp.refreshToken
                secrets.whoopTokenExpiryEpochSec =
                    tokenResp.accessTokenExpirationTime?.div(1000L) ?: 0L
            }
            .onFailure { failure ->
                Diag.e(DiagnosticArea.OAUTH, 
                    TAG,
                    "Whoop: Token-Exchange fehlgeschlagen — class=${failure.javaClass.simpleName}, message=${failure.message}",
                    failure,
                )
                if (failure is AuthorizationException) {
                    Diag.e(DiagnosticArea.OAUTH, 
                        TAG,
                        "Whoop: AuthException-Details — type=${failure.type} code=${failure.code} error=${failure.error} desc=${failure.errorDescription} uri=${failure.errorUri}",
                    )
                }
                clearWhoopAuthState()
            }
            .map { Unit }
    }

    /**
     * Liefert ein gueltiges Access-Token fuer Whoop. Refreshed transparent wenn das alte abgelaufen
     * ist.
     *
     * Whoop ist ein Confidential Client — beim Refresh-Request MUSS das Client-Secret mitgeschickt
     * werden. AppAuth's einfache performActionWithFreshTokens(service) Variante schickt es NICHT
     * mit, was zu einem stillen invalid_client-Fehler bei jedem Refresh fuehrt — und damit dazu
     * dass der Nutzer sich ca. jede Stunde neu anmelden muss.
     *
     * Direktive 3 — Resilient Bugfixing, vier Schichten: Schicht 1 (Praeventiv) — ClientSecretPost
     * beim Refresh mitsenden Schicht 2 (Reaktiv) — bei fehlendem Secret KLARE Exception werfen
     * statt silent null zurueckgeben Schicht 3 (Selbstheilend) — bei Refresh-Fehler AuthState
     * resetten, damit UI-Anzeige nicht widerspruechlich bleibt (Settings sagen "verbunden", Banner
     * sagt "anmelden") Schicht 4 (Diagnose) — detaillierte Logs damit naechster Vorfall in 30s
     * diagnostiziert werden kann
     */
    suspend fun freshWhoopAccessToken(): String? = whoopRefreshMutex.withLock {
        val state = loadWhoopAuthState()
        if (!state.isAuthorized) {
            Diag.d(DiagnosticArea.OAUTH, TAG, "Whoop-Refresh: kein AuthState — kein Token zurückgeben")
            return@withLock null
        }
        val clientSecret = secrets.whoopClientSecret
        if (clientSecret.isNullOrBlank()) {
            Diag.e(DiagnosticArea.OAUTH, TAG, "Whoop-Refresh: Client-Secret fehlt im EncryptedSecretsStore")
            throw IllegalStateException(
                "Whoop-Client-Secret fehlt — bitte in den API-Schlüssel-Settings neu eingeben."
            )
        }
        val nowMs = System.currentTimeMillis()
        val expiry = state.accessTokenExpirationTime
        if (de.frank.entropyreducer.BuildConfig.DEBUG) {
            Diag.d(DiagnosticArea.OAUTH, TAG, "Whoop-Refresh: needsRefresh=${state.needsTokenRefresh} expiryDeltaMs=${if (expiry != null) expiry - nowMs else Long.MIN_VALUE}")
        }
        if (state.refreshToken.isNullOrBlank()) {
            Diag.e(DiagnosticArea.OAUTH,
                TAG,
                "Whoop-Refresh: KEIN refreshToken im AuthState — Whoop hat beim Login keinen Refresh-Token zurückgegeben (offline-Scope nicht akzeptiert?). Kein Refresh möglich.",
            )
            // Frank-Wunsch 2026-05-09: alte Daten erhalten lassen wenn Refresh nicht moeglich.
            // NICHT clearWhoopAuthState() rufen — der State bleibt, die DB-Daten bleiben sichtbar.
            markWhoopReauthNeeded("Refresh-Token fehlt")
            throw WhoopReauthRequiredException()
        }
        // Bug-Fix 2026-10-13 (Whoop seit 09.10. ohne Daten): Whoop gibt bei jedem Refresh einen NEUEN
        // Refresh-Token aus und entwertet den alten. Reisst die Verbindung genau dabei ab, ist der alte
        // Token verbraucht und der neue nie angekommen — danach lehnt Whoop jeden Refresh endgueltig ab
        // (invalid_request). Das heilt nur eine neue Anmeldung. Deshalb:
        //  - Ist die Anmeldung als abgelaufen markiert, nicht bei jedem Abgleich erneut anklopfen, sondern
        //    nur alle paar Stunden pruefen (heilt sich selbst, falls Whoop doch wieder annimmt).
        //  - Netzfehler sofort wiederholen (Schleife unten).
        //  - Endgueltige OAuth-Fehler merken, Frank benachrichtigen und klar benennen.
        val reauthSince = secrets.whoopReauthSinceMs
        if (reauthSince > 0L && nowMs - lastWhoopReauthProbeMs < REAUTH_PROBE_INTERVAL_MS) {
            throw WhoopReauthRequiredException()
        }
        if (reauthSince > 0L) lastWhoopReauthProbeMs = nowMs

        var lastError: AuthorizationException? = null
        for (attempt in 0..NETWORK_RETRY_DELAYS_MS.size) {
            if (attempt > 0) delay(NETWORK_RETRY_DELAYS_MS[attempt - 1])
            val (token, error) = refreshWhoopOnce(state, ClientSecretPost(clientSecret))
            if (error == null) {
                if (reauthSince > 0L) {
                    Diag.i(DiagnosticArea.OAUTH, TAG, "Whoop-Anmeldung funktioniert wieder — Markierung entfernt")
                    secrets.whoopReauthSinceMs = 0L
                    problemNotifier.whoopWiederOk()
                }
                return@withLock token
            }
            lastError = error
            Diag.e(DiagnosticArea.OAUTH,
                TAG,
                "Whoop-Token-Refresh fehlgeschlagen (Versuch ${attempt + 1}) — type=${error.type} code=${error.code} error=${error.error} desc=${error.errorDescription} uri=${error.errorUri}",
                error,
            )
            // Nur Netzfehler lohnen eine Wiederholung; alles andere ist eine Antwort von Whoop.
            if (error.type != AuthorizationException.TYPE_GENERAL_ERROR) break
        }
        if (lastError?.type == AuthorizationException.TYPE_OAUTH_TOKEN_ERROR) {
            // Frank-Wunsch 2026-05-09 bleibt: AuthState NICHT loeschen, alte Daten bleiben sichtbar.
            markWhoopReauthNeeded("${lastError.error}: ${lastError.errorDescription}")
            throw WhoopReauthRequiredException()
        }
        throw java.io.IOException("Whoop ist gerade nicht erreichbar (${lastError?.errorDescription ?: "Netzwerkfehler"}).")
    }

    /** Ein einzelner Refresh-Versuch. Liefert (Token, null) oder (null, Fehler). */
    private suspend fun refreshWhoopOnce(
        state: AuthState,
        clientAuth: net.openid.appauth.ClientAuthentication,
    ): Pair<String?, AuthorizationException?> {
        val service = newService()
        return suspendCancellableCoroutine { cont ->
            // ClientSecretPost: Whoop ist ein Confidential Client und verlangt das Secret beim Refresh.
            state.performActionWithFreshTokens(service, clientAuth) { accessToken, _, ex ->
                if (ex != null || accessToken == null) {
                    cont.resume(null to (ex ?: AuthorizationException.GeneralErrors.NETWORK_ERROR))
                } else {
                    Diag.d(DiagnosticArea.OAUTH,
                        TAG,
                        "Whoop-Token-Refresh OK — neuer Access-Token erhalten (Länge=${accessToken.length})",
                    )
                    saveWhoopAuthState(state)
                    secrets.whoopAccessToken = accessToken
                    cont.resume(accessToken to null)
                }
                service.dispose()
            }
        }
    }

    /** True, solange Whoop die gespeicherte Anmeldung ablehnt und Frank sich neu anmelden muss. */
    fun whoopNeedsReauth(): Boolean = secrets.whoopReauthSinceMs > 0L

    private fun markWhoopReauthNeeded(grund: String) {
        if (secrets.whoopReauthSinceMs == 0L) {
            secrets.whoopReauthSinceMs = System.currentTimeMillis()
            lastWhoopReauthProbeMs = System.currentTimeMillis()
            Diag.e(DiagnosticArea.OAUTH, TAG, "Whoop-Anmeldung abgelaufen ($grund) — neue Anmeldung nötig, Frank wird benachrichtigt")
        }
        // Bei jeder Pruefung erneut zeigen (alle paar Stunden, ohne Ton): eine weggewischte Meldung
        // soll nicht dazu fuehren, dass Whoop wieder tagelang unbemerkt stillsteht.
        problemNotifier.whoopNeuAnmelden()
    }

    // Polar V3/V4/Flow-Web OAuth-Logik entfernt 2026-05-17 (Frank-Wunsch).
    // Polar-Historie wird ausschliesslich ueber Polar-ZIP-Bulk-Import eingelesen,
    // siehe PolarBulkImporter + PolarBulkImportWorker. Live-API-Anbindung entfaellt.

    /* ================================== Helper ================================== */

    private suspend fun exchangeToken(
        request: net.openid.appauth.TokenRequest
    ): Result<TokenResponse> = suspendCancellableCoroutine { cont ->
        val service = newService()
        Diag.d(DiagnosticArea.OAUTH, 
            TAG,
            "exchangeToken — endpoint=${request.configuration.tokenEndpoint}, grantType=${request.grantType}",
        )
        service.performTokenRequest(request) { resp, ex ->
            if (resp != null) {
                cont.resume(Result.success(resp))
            } else {
                Diag.e(DiagnosticArea.OAUTH, 
                    TAG,
                    "exchangeToken FAIL — type=${ex?.type} code=${ex?.code} error=${ex?.error} desc=${ex?.errorDescription} uri=${ex?.errorUri}",
                )
                cont.resume(
                    Result.failure(ex ?: IllegalStateException("Token-Exchange fehlgeschlagen"))
                )
            }
            service.dispose()
        }
    }

    /**
     * Wie exchangeToken, aber mit expliziter ClientAuthentication (HTTP Basic Auth fuer Polar —
     * Polar verlangt das beim Token-Endpoint).
     */
    private suspend fun exchangeTokenWithAuth(
        request: net.openid.appauth.TokenRequest,
        clientAuth: net.openid.appauth.ClientAuthentication,
    ): Result<TokenResponse> = suspendCancellableCoroutine { cont ->
        val service = newService()
        Diag.d(DiagnosticArea.OAUTH, 
            TAG,
            "exchangeTokenWithAuth — endpoint=${request.configuration.tokenEndpoint}, grantType=${request.grantType}",
        )
        service.performTokenRequest(request, clientAuth) { resp, ex ->
            if (resp != null) {
                cont.resume(Result.success(resp))
            } else {
                Diag.e(DiagnosticArea.OAUTH, 
                    TAG,
                    "exchangeTokenWithAuth FAIL — type=${ex?.type} code=${ex?.code} error=${ex?.error} desc=${ex?.errorDescription} uri=${ex?.errorUri}",
                )
                cont.resume(
                    Result.failure(ex ?: IllegalStateException("Token-Exchange fehlgeschlagen"))
                )
            }
            service.dispose()
        }
    }

    companion object {
        private const val TAG = "OAuthService"

        /** Wartezeiten vor den Wiederholungen eines Whoop-Refreshs nach einem Netzfehler. */
        private val NETWORK_RETRY_DELAYS_MS = longArrayOf(1_500L, 4_000L)

        /** So oft wird bei abgelaufener Anmeldung geprueft, ob Whoop wieder annimmt. */
        private const val REAUTH_PROBE_INTERVAL_MS = 6L * 60L * 60L * 1000L
        // Whoop's Developer-Dashboard-Validator akzeptiert nur Redirect-URIs mit
        // "://" (zwei Slashes nach dem Schema). RFC 8252 erlaubt zwar auch ":/",
        // Whoop's Parser aber nicht — daher beide URIs einheitlich auf "://".
        const val GOOGLE_REDIRECT_URI = "de.frank.entropyreducer://oauth/google/callback"
        const val WHOOP_REDIRECT_URI_DEFAULT = "de.frank.entropyreducer://oauth/whoop/callback"
        const val GOOGLE_SCOPE_CALENDAR_READONLY =
            "https://www.googleapis.com/auth/calendar.readonly"
        val WHOOP_SCOPES =
            listOf(
                "read:recovery",
                "read:cycles",
                "read:sleep",
                "read:workout",
                "read:profile",
                "offline",
            )

        // Polar V3/V4 Konstanten entfernt 2026-05-17 (Frank-Wunsch).

    }
}

/** Whoop lehnt die gespeicherte Anmeldung endgueltig ab — nur eine neue Anmeldung hilft. */
class WhoopReauthRequiredException :
    IllegalStateException("Whoop-Anmeldung abgelaufen — bitte unter Einstellungen → API-Schlüssel bei Whoop neu anmelden.")
