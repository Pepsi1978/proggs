package de.frank.entropyreducer.data.remote.zepp

import android.util.Log
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet den 2-Stufen-Login bei der Zepp-Cloud und liefert frische Tokens fuer
 * Daten-Aufrufe.
 *
 * Login-Ablauf (aus argrento/huami-token):
 *   Stufe 1: POST an api-user-{region}.zepp.com/v2/registrations/tokens
 *            - Body: form-encoded (emailOrPhone, password, ...) → AES-CBC verschluesselt
 *            - Response: 303 Redirect, Location-Header enthaelt access+refresh in Query
 *   Stufe 2: POST an api-mifit-{region}.zepp.com/v2/client/login
 *            - Body: form-encoded (code=access_token, device_id, ...)
 *            - Response: JSON mit token_info { login_token, app_token, user_id }
 *
 * Bei Fehlschlag mit Region "de2" wird automatisch "us2" probiert (Account-Region
 * unklar, manche Konten sind in der US-Region registriert).
 */
@Singleton
class ZeppAuthService @Inject constructor(
    private val api: ZeppApi,
    private val secrets: EncryptedSecretsStore,
) {

    /** Versucht Login mit der gespeicherten oder uebergebenen E-Mail/Passwort. */
    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        // Geraete-ID stabil halten — beim ersten Login einmal generieren.
        val deviceId = secrets.zeppDeviceId ?: UUID.randomUUID().toString().also {
            secrets.zeppDeviceId = it
        }

        val regionsToTry = listOf(secrets.zeppRegion ?: "de2", "us2", "de2").distinct()
        var lastError: Throwable? = null
        for (region in regionsToTry) {
            try {
                Log.d(TAG, "Versuche Login in Region '$region'")
                doLogin(email, password, region, deviceId)
                secrets.zeppRegion = region
                secrets.zeppEmail = email
                secrets.zeppPassword = password
                Log.i(TAG, "Zepp-Login erfolgreich in Region '$region', userId=${secrets.zeppUserId}")
                return@runCatching
            } catch (t: Throwable) {
                Log.w(TAG, "Login-Fehler in Region '$region': ${t.message}")
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("Zepp-Login fehlgeschlagen")
    }

    /** Wiederhole den Login mit gespeicherten Credentials (bei abgelaufenem App-Token). */
    suspend fun reloginIfPossible(): Boolean {
        val email = secrets.zeppEmail ?: return false
        val password = secrets.zeppPassword ?: return false
        return login(email, password).isSuccess
    }

    /** Logout — Tokens lokal loeschen + Server informieren (best-effort). */
    suspend fun logout(): Result<Unit> = runCatching {
        val region = secrets.zeppRegion ?: "de2"
        val loginToken = secrets.zeppLoginToken
        if (loginToken != null) {
            try {
                val headers = mapOf(
                    "app_name" to ZeppEndpoints.APP_NAME,
                    "hm-privacy-ceip" to "false",
                    "accept-language" to "de-DE",
                    "appname" to ZeppEndpoints.APP_NAME,
                    "cv" to "${ZeppEndpoints.APP_BUILD}_${ZeppEndpoints.APP_VERSION}",
                    "v" to "2.0",
                    "appplatform" to "android_phone",
                    "vb" to "202509151347",
                    "vn" to ZeppEndpoints.APP_VERSION,
                    "user-agent" to ZeppEndpoints.USER_AGENT,
                    "content-type" to "application/x-www-form-urlencoded; charset=UTF-8",
                )
                val fields = mapOf(
                    "login_token" to loginToken,
                    "os_verison" to "vnull", // Tippfehler im Original-Protokoll, MUSS so bleiben
                )
                api.logout(ZeppEndpoints.logoutUrl(region), headers, fields)
            } catch (t: Throwable) {
                Log.w(TAG, "Server-Logout fehlgeschlagen (ignoriert): ${t.message}")
            }
        }
        clearTokens()
    }

    /** Loescht alle Zepp-Tokens und Credentials lokal. */
    fun clearTokens() {
        secrets.zeppEmail = null
        secrets.zeppPassword = null
        secrets.zeppRefreshToken = null
        secrets.zeppAccessToken = null
        secrets.zeppAppToken = null
        secrets.zeppLoginToken = null
        secrets.zeppUserId = null
        // zeppDeviceId behalten — soll stabil bleiben
        // zeppRegion behalten — User-Praeferenz
    }

    fun isAuthenticated(): Boolean =
        !secrets.zeppAppToken.isNullOrBlank() && !secrets.zeppUserId.isNullOrBlank()

    /** Hilfs-Methode fuer Repository: liefert App-Token oder versucht Re-Login. */
    suspend fun freshAppToken(): String? {
        val current = secrets.zeppAppToken
        if (!current.isNullOrBlank()) return current
        return if (reloginIfPossible()) secrets.zeppAppToken else null
    }

    /* =================================== privat =================================== */

    // Performance-Audit Loop 2 (2026-05-10): doLogin enthaelt AES-CBC-Verschluesselung
    // (ZeppCrypto.encryptLoginPayload — Cipher.getInstance + init + doFinal). JCA-Operationen
    // sind synchron CPU/Keystore. Wrapper mit Dispatchers.IO stellt sicher dass sie nicht auf
    // Main-Thread laufen wenn der Aufrufer von dort kommt.
    private suspend fun doLogin(
        email: String,
        password: String,
        region: String,
        deviceId: String,
    ): Unit = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // Stufe 1 — Token-Tausch
        val step1Body = ZeppCrypto.urlEncodeForm(
            listOf(
                "emailOrPhone" to email,
                "password" to password,
                "state" to "REDIRECTION",
                "client_id" to "HuaMi",
                "redirect_uri" to "https://s3-us-west-2.amazonaws.com/hm-registration/successsignin.html",
                "region" to "us-west-2",
                "token" to "access",
                "token" to "refresh",
                "country_code" to "DE",
            ),
        )
        val encrypted = ZeppCrypto.encryptLoginPayload(step1Body.toByteArray(Charsets.UTF_8))
        val requestBody = encrypted.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType())

        val step1 = api.loginStep1(
            url = ZeppEndpoints.loginStep1Url(region),
            headers = ZeppEndpoints.step1Headers(),
            encryptedBody = requestBody,
        )

        // Erwartet 303 mit Location-Header. OkHttp folgt Redirects standardmaessig —
        // wir setzen followRedirects=false in NetworkModule, dann landet die 303 hier.
        if (step1.code() != 303) {
            throw IllegalStateException(
                "Login-Stufe 1: erwartete 303, bekam ${step1.code()} (${step1.message()})",
            )
        }
        val location = step1.headers()["Location"]
            ?: throw IllegalStateException("Login-Stufe 1: kein Location-Header in der 303-Antwort")
        val (refresh, access) = parseTokensFromRedirect(location)
        secrets.zeppRefreshToken = refresh
        secrets.zeppAccessToken = access

        // Stufe 2 — Access-Token gegen App-Token tauschen
        val step2 = api.loginStep2(
            url = ZeppEndpoints.loginStep2Url(region),
            headers = ZeppEndpoints.step2Headers(),
            fields = mapOf(
                "code" to access,
                "device_id" to deviceId,
                "device_model" to "android_phone",
                "app_version" to ZeppEndpoints.APP_VERSION,
                "dn" to "api-mifit.zepp.com,api-user.zepp.com,api-mifit.zepp.com,api-watch.zepp.com,app-analytics.zepp.com,auth.zepp.com,api-analytics.zepp.com",
                "third_name" to "huami",
                "source" to "com.huami.watch.hmwatchmanager:${ZeppEndpoints.APP_VERSION}:${ZeppEndpoints.APP_BUILD}",
                "app_name" to ZeppEndpoints.APP_NAME,
                "country_code" to "DE",
                "grant_type" to "access_token",
                "allow_registration" to "false",
                "lang" to "de",
                "countryState" to "DE-NW",
            ),
        )
        val info = step2.tokenInfo
            ?: throw IllegalStateException("Login-Stufe 2: kein token_info in der Antwort (result=${step2.result})")

        secrets.zeppLoginToken = info.loginToken
            ?: throw IllegalStateException("Login-Stufe 2: kein login_token")
        secrets.zeppAppToken = info.appToken
            ?: throw IllegalStateException("Login-Stufe 2: kein app_token")
        secrets.zeppUserId = info.userId
            ?: throw IllegalStateException("Login-Stufe 2: keine user_id")
    }

    /** Extrahiert refresh und access aus dem Location-Header der 303-Antwort. */
    private fun parseTokensFromRedirect(location: String): Pair<String, String> {
        val uri = URI.create(location)
        val query = uri.rawQuery ?: throw IllegalStateException("Login-Stufe 1: Redirect-URL ohne Query")
        val params = query.split("&").associate {
            val (k, v) = it.split("=", limit = 2).let { parts ->
                parts[0] to (parts.getOrNull(1) ?: "")
            }
            k to java.net.URLDecoder.decode(v, "UTF-8")
        }
        val refresh = params["refresh"]
            ?: throw IllegalStateException("Login-Stufe 1: kein refresh-Token in Redirect-URL")
        val access = params["access"]
            ?: throw IllegalStateException("Login-Stufe 1: kein access-Token in Redirect-URL")
        return refresh to access
    }

    companion object {
        private const val TAG = "ZeppAuthService"
        internal val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
