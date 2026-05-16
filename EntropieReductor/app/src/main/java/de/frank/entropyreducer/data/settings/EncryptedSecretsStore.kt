package de.frank.entropyreducer.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verschluesselte Speicherung von API-Keys und OAuth-Tokens (Spec §6.1, §20).
 * AES-256 GCM, MasterKey via Android Keystore.
 */
@Singleton
class EncryptedSecretsStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    // Performance-Fix Loop 2.0: eager → lazy. EncryptedSharedPreferences.create
    // plus MasterKey-Hardware-Roundtrip kostet 100-300 ms beim ersten Aufruf.
    // Vorher: beim Hilt-Singleton-Konstruktor (transitive Abhaengigkeit aus
    // EntropyReducerApp.onCreate-Injection), also auf Main beim App-Start.
    // Jetzt: beim ersten Property-Access — der erfolgt in den fixierten
    // Aufruf-Pfaden ausschliesslich aus IO-Coroutinen (siehe EntropyReducerApp
    // ON_START-Wrap und StartupViewModel.init Dispatchers.IO).
    // lazy(SYNCHRONIZED) ist threadsafe — paralleler erster Access blockiert
    // sich gegenseitig sauber.
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var groqApiKey: String?
        get() = prefs.getString(KEY_GROQ, null)
        set(value) { prefs.edit().putString(KEY_GROQ, value).apply() }

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI, null)
        set(value) { prefs.edit().putString(KEY_GEMINI, value).apply() }

    var googleTtsApiKey: String?
        get() = prefs.getString(KEY_TTS, null)
        set(value) { prefs.edit().putString(KEY_TTS, value).apply() }

    // OAuth — Whoop
    var whoopClientId: String?
        get() = prefs.getString(KEY_WHOOP_CLIENT_ID, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_CLIENT_ID, value).apply() }

    var whoopClientSecret: String?
        get() = prefs.getString(KEY_WHOOP_CLIENT_SECRET, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_CLIENT_SECRET, value).apply() }

    var whoopAccessToken: String?
        get() = prefs.getString(KEY_WHOOP_ACCESS, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_ACCESS, value).apply() }

    var whoopRefreshToken: String?
        get() = prefs.getString(KEY_WHOOP_REFRESH, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_REFRESH, value).apply() }

    var whoopTokenExpiryEpochSec: Long
        get() = prefs.getLong(KEY_WHOOP_EXPIRY, 0L)
        set(value) { prefs.edit().putLong(KEY_WHOOP_EXPIRY, value).apply() }

    // OAuth — Google Calendar
    var googleAccessToken: String?
        get() = prefs.getString(KEY_GOOGLE_ACCESS, null)
        set(value) { prefs.edit().putString(KEY_GOOGLE_ACCESS, value).apply() }

    var googleRefreshToken: String?
        get() = prefs.getString(KEY_GOOGLE_REFRESH, null)
        set(value) { prefs.edit().putString(KEY_GOOGLE_REFRESH, value).apply() }

    var googleTokenExpiryEpochSec: Long
        get() = prefs.getLong(KEY_GOOGLE_EXPIRY, 0L)
        set(value) { prefs.edit().putLong(KEY_GOOGLE_EXPIRY, value).apply() }

    /** AppAuth speichert den serialisierten AuthState als JSON-Blob. */
    var googleAuthStateJson: String?
        get() = prefs.getString(KEY_GOOGLE_AUTH_STATE, null)
        set(value) { prefs.edit().putString(KEY_GOOGLE_AUTH_STATE, value).apply() }

    var whoopAuthStateJson: String?
        get() = prefs.getString(KEY_WHOOP_AUTH_STATE, null)
        set(value) { prefs.edit().putString(KEY_WHOOP_AUTH_STATE, value).apply() }

    // Zepp / Amazfit T-Rex 3 — inoffizielle Cloud-API mit E-Mail/Passwort-Login.
    // Frank-Wunsch 2026-05-09: Sport-Daten + PAI/BioCharge/Hauttemperatur von der
    // T-Rex 3 in den Biomarker-Bereich einlesen. Kein OAuth — 2-Stufen-Login mit
    // AES-CBC-verschluesseltem Body in Stufe 1, danach Token-Tausch in Stufe 2.
    // Refresh-Token-Lebensdauer bei Zepp ist begrenzt — bei Ablauf muessen wir
    // mit gespeicherter E-Mail + Passwort den Login wiederholen koennen.
    var zeppEmail: String?
        get() = prefs.getString(KEY_ZEPP_EMAIL, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_EMAIL, value).apply() }

    var zeppPassword: String?
        get() = prefs.getString(KEY_ZEPP_PASSWORD, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_PASSWORD, value).apply() }

    /** Region-Code fuer Zepp-Endpoints — "de2" fuer Europa, "us2" fuer USA.
     *  Default: "de2" weil Frank in Deutschland ist. Der AuthService schaltet
     *  bei Login-Fehlschlag automatisch auf "us2" um (Fallback). */
    var zeppRegion: String?
        get() = prefs.getString(KEY_ZEPP_REGION, "de2")
        set(value) { prefs.edit().putString(KEY_ZEPP_REGION, value).apply() }

    /** Refresh-Token aus Schritt 1 des Login-Flows (Redirect-URL-Query). */
    var zeppRefreshToken: String?
        get() = prefs.getString(KEY_ZEPP_REFRESH, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_REFRESH, value).apply() }

    /** Access-Token aus Schritt 1 — wird in Schritt 2 gegen den App-Token getauscht. */
    var zeppAccessToken: String?
        get() = prefs.getString(KEY_ZEPP_ACCESS, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_ACCESS, value).apply() }

    /** App-Token aus Schritt 2 — wird als `apptoken`-Header in allen Daten-Calls genutzt. */
    var zeppAppToken: String?
        get() = prefs.getString(KEY_ZEPP_APP_TOKEN, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_APP_TOKEN, value).apply() }

    /** Login-Token aus Schritt 2 — wird fuer Logout gebraucht. */
    var zeppLoginToken: String?
        get() = prefs.getString(KEY_ZEPP_LOGIN_TOKEN, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_LOGIN_TOKEN, value).apply() }

    /** User-ID — wird in URL-Pfaden und Query-Params der Daten-Endpoints gebraucht. */
    var zeppUserId: String?
        get() = prefs.getString(KEY_ZEPP_USER_ID, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_USER_ID, value).apply() }

    /** Stabile Geraete-ID fuer den Login (UUID) — beim ersten Login generiert,
     *  danach bestehend lassen damit der Server uns als "selbes Geraet" erkennt. */
    var zeppDeviceId: String?
        get() = prefs.getString(KEY_ZEPP_DEVICE_ID, null)
        set(value) { prefs.edit().putString(KEY_ZEPP_DEVICE_ID, value).apply() }

    /** Zeitstempel der letzten erfolgreichen Zepp-Synchronisation (ms). */
    var zeppLastSyncEpochMs: Long
        get() = prefs.getLong(KEY_ZEPP_LAST_SYNC, 0L)
        set(value) { prefs.edit().putLong(KEY_ZEPP_LAST_SYNC, value).apply() }

    // Oura Ring — Frank-Wunsch 2026-05-10. Personal Access Token aus
    // https://cloud.ouraring.com/personal-access-tokens. Single-User-Token,
    // kein OAuth-Flow noetig, kein Ablauf. Wird als Bearer-Header bei jedem
    // Daten-Request mitgesendet.
    var ouraPersonalAccessToken: String?
        get() = prefs.getString(KEY_OURA_PAT, null)
        set(value) { prefs.edit().putString(KEY_OURA_PAT, value).apply() }

    /** Zeitstempel der letzten erfolgreichen Oura-Synchronisation (ms). */
    var ouraLastSyncEpochMs: Long
        get() = prefs.getLong(KEY_OURA_LAST_SYNC, 0L)
        set(value) { prefs.edit().putLong(KEY_OURA_LAST_SYNC, value).apply() }

    // Polar AccessLink — Frank-Wunsch 2026-05-16. OAuth2 (Authorization Code +
    // HTTP Basic Auth beim Token-Endpoint, KEIN Refresh-Token). Polar wird die
    // alleinige Workout-Quelle nachdem Strava die externen Brustgurt-HR-Daten
    // verloren hat. Polar reicht die H10-Daten vom gekoppelten Brustgurt sauber
    // durch — genau das was fehlte. Tokens leben ~1 Jahr, bei Revocation muss
    // der User neu autorisieren (kein Refresh moeglich).
    var polarClientId: String?
        get() = prefs.getString(KEY_POLAR_CLIENT_ID, null)
        set(value) { prefs.edit().putString(KEY_POLAR_CLIENT_ID, value).apply() }

    var polarClientSecret: String?
        get() = prefs.getString(KEY_POLAR_CLIENT_SECRET, null)
        set(value) { prefs.edit().putString(KEY_POLAR_CLIENT_SECRET, value).apply() }

    /** Persistierter AppAuth-AuthState als JSON-Blob (analog Whoop/Google). */
    var polarAuthStateJson: String?
        get() = prefs.getString(KEY_POLAR_AUTH_STATE, null)
        set(value) { prefs.edit().putString(KEY_POLAR_AUTH_STATE, value).apply() }

    /**
     * Polar-User-ID aus dem Token-Response (`x_user_id`). Wird in JEDEM API-
     * Pfad als {user-id} verwendet — ohne sie geht nichts. 0 = noch nicht
     * gesetzt / nicht authentifiziert.
     */
    var polarUserId: Long
        get() = prefs.getLong(KEY_POLAR_USER_ID, 0L)
        set(value) { prefs.edit().putLong(KEY_POLAR_USER_ID, value).apply() }

    /** Zeitstempel der letzten erfolgreichen Polar-Synchronisation (ms). */
    var polarLastSyncEpochMs: Long
        get() = prefs.getLong(KEY_POLAR_LAST_SYNC, 0L)
        set(value) { prefs.edit().putLong(KEY_POLAR_LAST_SYNC, value).apply() }

    /**
     * Flag das anzeigt, ob der User bereits via POST /v3/users registriert
     * wurde. Polar gibt bei doppelter Registrierung 409 zurueck — wir cachen
     * den Erfolg damit jede Sync-Aktion nicht erneut versucht zu registrieren.
     */
    var polarUserRegistered: Boolean
        get() = prefs.getBoolean(KEY_POLAR_USER_REGISTERED, false)
        set(value) { prefs.edit().putBoolean(KEY_POLAR_USER_REGISTERED, value).apply() }

    /**
     * Persistierte member-id (UUID) die wir bei der ersten Registrierung an
     * Polar geschickt haben. Muss zwischen App-Neuinstallationen erhalten
     * bleiben — sonst wirft Polar 409 wenn der User schon mit einer anderen
     * member-id registriert ist.
     */
    var polarMemberId: String?
        get() = prefs.getString(KEY_POLAR_MEMBER_ID, null)
        set(value) { prefs.edit().putString(KEY_POLAR_MEMBER_ID, value).apply() }

    /**
     * Zaehler der aufeinanderfolgenden Polar-Sync-Versuche bei denen die
     * Transaction OFFEN gelassen wurde weil Polar noch keine Samples zu
     * frischen Workouts geliefert hat. Reset auf 0 bei erfolgreichem Commit.
     *
     * Sicherheitsnetz gegen ewig blockierte Transactions (Polar erlaubt nur
     * 1 offene pro User): nach 6 Versuchen (= ca. 3 Stunden bei 30-Min-Worker)
     * wird trotzdem committed, damit der Sync nicht ewig festsitzt.
     */
    var polarRefreshAttempts: Int
        get() = prefs.getInt(KEY_POLAR_REFRESH_ATTEMPTS, 0)
        set(value) { prefs.edit().putInt(KEY_POLAR_REFRESH_ATTEMPTS, value).apply() }

    fun clearPolarAuthState() {
        prefs.edit()
            .remove(KEY_POLAR_AUTH_STATE)
            .remove(KEY_POLAR_USER_ID)
            .remove(KEY_POLAR_USER_REGISTERED)
            .remove(KEY_POLAR_LAST_SYNC)
            .remove(KEY_POLAR_REFRESH_ATTEMPTS)
            .apply()
    }

    /** Account-Mailadresse für Drive-Backup. Wird beim Sign-In gesetzt. */
    var driveAccountEmail: String?
        get() = prefs.getString(KEY_DRIVE_ACCOUNT, null)
        set(value) { prefs.edit().putString(KEY_DRIVE_ACCOUNT, value).apply() }

    /** Account-Mailadresse für Google-Calendar-Sync. Separat von Drive damit beide
     *  Sign-Ins unabhaengig voneinander widerrufen werden können. */
    var calendarAccountEmail: String?
        get() = prefs.getString(KEY_CALENDAR_ACCOUNT, null)
        set(value) { prefs.edit().putString(KEY_CALENDAR_ACCOUNT, value).apply() }

    /** Toggle: Drive-Backup aktiv? Steuert ob Mutationen einen Sync-Trigger feuern. */
    var driveBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_DRIVE_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_DRIVE_ENABLED, value).apply() }

    /** Zeitstempel des letzten erfolgreichen Backup-Uploads. */
    var driveLastBackupEpochMs: Long
        get() = prefs.getLong(KEY_DRIVE_LAST_BACKUP, 0L)
        set(value) { prefs.edit().putLong(KEY_DRIVE_LAST_BACKUP, value).apply() }

    /**
     * versionCode der App bei dem zuletzt eine automatische Re-Bewertung aller
     * offenen Aufgaben mit der priorityScore-Doktrin gelaufen ist. Wenn dieser
     * Wert kleiner als der aktuelle versionCode ist und es eine neue Doktrin-
     * Version gibt, triggert TasksViewModel.init einen Auto-Re-Score.
     * 0 = noch nie gelaufen.
     */
    var lastRescoreVersionCode: Int
        get() = prefs.getInt(KEY_LAST_RESCORE_VERSION, 0)
        set(value) { prefs.edit().putInt(KEY_LAST_RESCORE_VERSION, value).apply() }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Performance-Audit Loop 4 (2026-05-10): Atomare Batch-Edits beim Logout.
     * Vorher fuehrten 4 separate Property-Setter zu 4× Keystore-Roundtrip + 4×
     * Disk-Write. Jetzt 1× Batch-Transaktion.
     */
    fun clearGoogleAuthState() {
        prefs.edit()
            .remove(KEY_GOOGLE_ACCESS)
            .remove(KEY_GOOGLE_REFRESH)
            .remove(KEY_GOOGLE_EXPIRY)
            .remove(KEY_GOOGLE_AUTH_STATE)
            .apply()
    }

    fun clearWhoopAuthState() {
        prefs.edit()
            .remove(KEY_WHOOP_ACCESS)
            .remove(KEY_WHOOP_REFRESH)
            .remove(KEY_WHOOP_EXPIRY)
            .remove(KEY_WHOOP_AUTH_STATE)
            .apply()
    }

    companion object {
        private const val FILE_NAME = "encrypted_secrets"
        private const val KEY_GROQ = "groq_api_key"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_TTS = "google_tts_api_key"
        private const val KEY_WHOOP_CLIENT_ID = "whoop_client_id"
        private const val KEY_WHOOP_CLIENT_SECRET = "whoop_client_secret"
        private const val KEY_WHOOP_ACCESS = "whoop_access_token"
        private const val KEY_WHOOP_REFRESH = "whoop_refresh_token"
        private const val KEY_WHOOP_EXPIRY = "whoop_expiry"
        private const val KEY_GOOGLE_ACCESS = "google_access_token"
        private const val KEY_GOOGLE_REFRESH = "google_refresh_token"
        private const val KEY_GOOGLE_EXPIRY = "google_expiry"
        private const val KEY_GOOGLE_AUTH_STATE = "google_auth_state_json"
        private const val KEY_WHOOP_AUTH_STATE = "whoop_auth_state_json"
        private const val KEY_DRIVE_ACCOUNT = "drive_account_email"
        private const val KEY_DRIVE_ENABLED = "drive_backup_enabled"
        private const val KEY_DRIVE_LAST_BACKUP = "drive_last_backup_ms"
        private const val KEY_CALENDAR_ACCOUNT = "calendar_account_email"
        private const val KEY_LAST_RESCORE_VERSION = "last_rescore_version_code"
        // Zepp / Amazfit T-Rex 3
        private const val KEY_ZEPP_EMAIL = "zepp_email"
        private const val KEY_ZEPP_PASSWORD = "zepp_password"
        private const val KEY_ZEPP_REGION = "zepp_region"
        private const val KEY_ZEPP_REFRESH = "zepp_refresh_token"
        private const val KEY_ZEPP_ACCESS = "zepp_access_token"
        private const val KEY_ZEPP_APP_TOKEN = "zepp_app_token"
        private const val KEY_ZEPP_LOGIN_TOKEN = "zepp_login_token"
        private const val KEY_ZEPP_USER_ID = "zepp_user_id"
        private const val KEY_ZEPP_DEVICE_ID = "zepp_device_id"
        private const val KEY_ZEPP_LAST_SYNC = "zepp_last_sync_ms"
        // Oura Ring
        private const val KEY_OURA_PAT = "oura_personal_access_token"
        private const val KEY_OURA_LAST_SYNC = "oura_last_sync_ms"
        // Polar AccessLink (Frank-Wunsch 2026-05-16)
        private const val KEY_POLAR_CLIENT_ID = "polar_client_id"
        private const val KEY_POLAR_CLIENT_SECRET = "polar_client_secret"
        private const val KEY_POLAR_AUTH_STATE = "polar_auth_state_json"
        private const val KEY_POLAR_USER_ID = "polar_user_id"
        private const val KEY_POLAR_LAST_SYNC = "polar_last_sync_ms"
        private const val KEY_POLAR_USER_REGISTERED = "polar_user_registered"
        private const val KEY_POLAR_MEMBER_ID = "polar_member_id"
        private const val KEY_POLAR_REFRESH_ATTEMPTS = "polar_refresh_attempts"
    }
}
