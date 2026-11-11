package de.frank.jarvis.bio.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Verschlüsselte Zugangsdaten des Biomarker-Bereichs (aus Entropie Reductor übernommen): Whoop-Anmeldung (AppAuth-Zustand,
 * Client-ID und -Secret) und Oura-Token. AES-256, Schlüssel im Android Keystore. Schlüsselnamen wie in Entropie Reductor,
 * damit die Übernahme sie 1:1 einspielen kann. Nichts davon geht an den Server, ins Live-Gedächtnis oder an ein Modell.
 */
class EncryptedSecretsStore(context: Context) {
    private val app = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            app, FILE_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var whoopClientId: String?
        get() = prefs.getString(KEY_WHOOP_CLIENT_ID, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_CLIENT_ID, value).apply() }

    var whoopClientSecret: String?
        get() = prefs.getString(KEY_WHOOP_CLIENT_SECRET, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_CLIENT_SECRET, value).apply() }

    /** Seit wann Whoop die gespeicherte Anmeldung ablehnt (ms), 0 = alles in Ordnung. */
    var whoopReauthSinceMs: Long
        get() = prefs.getLong(KEY_WHOOP_REAUTH_SINCE, 0L)
        set(value) { prefs.edit().putLong(KEY_WHOOP_REAUTH_SINCE, value).apply() }

    var whoopAccessToken: String?
        get() = prefs.getString(KEY_WHOOP_ACCESS, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_ACCESS, value).apply() }

    var whoopRefreshToken: String?
        get() = prefs.getString(KEY_WHOOP_REFRESH, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_REFRESH, value).apply() }

    var whoopTokenExpiryEpochSec: Long
        get() = prefs.getLong(KEY_WHOOP_EXPIRY, 0L)
        set(value) { prefs.edit().putLong(KEY_WHOOP_EXPIRY, value).apply() }

    // Google (nur weil der übernommene OAuthService die Felder kennt; der Biomarker-Bereich nutzt keinen Google-Zugang).
    var googleAccessToken: String?
        get() = prefs.getString(KEY_GOOGLE_ACCESS, null)
        set(value) { prefs.edit().putString(KEY_GOOGLE_ACCESS, value).apply() }
    var googleRefreshToken: String?
        get() = prefs.getString(KEY_GOOGLE_REFRESH, null)
        set(value) { prefs.edit().putString(KEY_GOOGLE_REFRESH, value).apply() }
    var googleTokenExpiryEpochSec: Long
        get() = prefs.getLong(KEY_GOOGLE_EXPIRY, 0L)
        set(value) { prefs.edit().putLong(KEY_GOOGLE_EXPIRY, value).apply() }
    var googleAuthStateJson: String?
        get() = prefs.getString(KEY_GOOGLE_AUTH_STATE, null)
        set(value) { prefs.edit().putString(KEY_GOOGLE_AUTH_STATE, value).apply() }

    /** commit() statt apply(): Whoop entwertet bei jedem Refresh den alten Refresh-Token; der neue Stand darf nie verloren gehen. */
    var whoopAuthStateJson: String?
        get() = prefs.getString(KEY_WHOOP_AUTH_STATE, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_AUTH_STATE, value).commit() }

    /** Ein Whoop-Refresh ist abgeschickt, seine Antwort noch nicht gespeichert (ms), 0 = keiner offen. commit() wie oben. */
    var whoopRefreshUnterwegsSeitMs: Long
        get() = prefs.getLong(KEY_WHOOP_REFRESH_UNTERWEGS, 0L)
        set(value) { prefs.edit().putLong(KEY_WHOOP_REFRESH_UNTERWEGS, value).commit() }

    var whoopReauthProbeMs: Long
        get() = prefs.getLong(KEY_WHOOP_REAUTH_PROBE, 0L)
        set(value) { prefs.edit().putLong(KEY_WHOOP_REAUTH_PROBE, value).apply() }

    /** Oura Personal Access Token (kein OAuth, kein Ablauf). */
    var ouraPersonalAccessToken: String?
        get() = prefs.getString(KEY_OURA_PAT, null)
        set(value) { prefs.edit().putString(KEY_OURA_PAT, value).apply() }

    var ouraLastSyncEpochMs: Long
        get() = prefs.getLong(KEY_OURA_LAST_SYNC, 0L)
        set(value) { prefs.edit().putLong(KEY_OURA_LAST_SYNC, value).apply() }

    // In Jarvis ohne Drive-Sicherung und ohne Google-Kalender im Biomarker-Bereich.
    val driveAccountEmail: String? get() = null
    val calendarAccountEmail: String? get() = null
    val driveBackupEnabled: Boolean get() = false

    fun clearWhoopAuthState() {
        prefs.edit().remove(KEY_WHOOP_ACCESS).remove(KEY_WHOOP_REFRESH).remove(KEY_WHOOP_EXPIRY).remove(KEY_WHOOP_AUTH_STATE).commit()
    }

    fun clearGoogleAuthState() {
        prefs.edit().remove(KEY_GOOGLE_ACCESS).remove(KEY_GOOGLE_REFRESH).remove(KEY_GOOGLE_EXPIRY).remove(KEY_GOOGLE_AUTH_STATE).apply()
    }

    /** Ist schon ein Zugang da? Dann überschreibt eine erneute Übernahme nichts (der Whoop-Token könnte hier schon getauscht sein). */
    val hatZugang: Boolean get() = !whoopAuthStateJson.isNullOrBlank() || !ouraPersonalAccessToken.isNullOrBlank()

    /** Übernahme aus Entropie Reductor: genau die Biomarker-Schlüssel, in einem Schreibvorgang (commit). */
    fun uebernimm(werte: org.json.JSONObject) {
        val e = prefs.edit()
        TEXT.forEach { k -> if (werte.has(k) && !werte.isNull(k)) e.putString(k, werte.getString(k)) }
        LANG.forEach { k -> if (werte.has(k)) e.putLong(k, werte.optLong(k)) }
        e.commit()
    }

    companion object {
        private const val FILE_NAME = "bio_encrypted_secrets"
        const val KEY_WHOOP_CLIENT_ID = "whoop_client_id"
        const val KEY_WHOOP_CLIENT_SECRET = "whoop_client_secret"
        const val KEY_WHOOP_REAUTH_SINCE = "whoop_reauth_since_ms"
        const val KEY_WHOOP_REAUTH_PROBE = "whoop_reauth_probe_ms"
        const val KEY_WHOOP_REFRESH_UNTERWEGS = "whoop_refresh_unterwegs_seit_ms"
        const val KEY_WHOOP_ACCESS = "whoop_access_token"
        const val KEY_WHOOP_REFRESH = "whoop_refresh_token"
        const val KEY_WHOOP_EXPIRY = "whoop_expiry"
        const val KEY_WHOOP_AUTH_STATE = "whoop_auth_state_json"
        const val KEY_OURA_PAT = "oura_personal_access_token"
        const val KEY_OURA_LAST_SYNC = "oura_last_sync_ms"
        private const val KEY_GOOGLE_ACCESS = "google_access_token"
        private const val KEY_GOOGLE_REFRESH = "google_refresh_token"
        private const val KEY_GOOGLE_EXPIRY = "google_expiry"
        private const val KEY_GOOGLE_AUTH_STATE = "google_auth_state_json"
        /** Die Schlüssel, die die Übernahme aus Entropie Reductor mitbringt. */
        val TEXT = listOf(KEY_WHOOP_CLIENT_ID, KEY_WHOOP_CLIENT_SECRET, KEY_WHOOP_ACCESS, KEY_WHOOP_REFRESH, KEY_WHOOP_AUTH_STATE, KEY_OURA_PAT)
        val LANG = listOf(KEY_WHOOP_REAUTH_SINCE, KEY_WHOOP_REAUTH_PROBE, KEY_WHOOP_REFRESH_UNTERWEGS, KEY_WHOOP_EXPIRY, KEY_OURA_LAST_SYNC)
    }
}
