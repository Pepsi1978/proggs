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
        val sharedPrefs = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        // Sprint 3 (2026-05-22): Zepp-Cloud-API wurde entfernt — Plaintext-Credentials
        // (E-Mail + Passwort) waren ein Sicherheitsrisiko und die API liefert seit
        // Mai 2026 keine neuen Workouts mehr. Beim ersten App-Start nach Update werden
        // alle alten Zepp-Eintraege aus den EncryptedSharedPreferences entfernt.
        // Das Flag verhindert dass die Cleanup-Routine bei jedem Start laeuft.
        if (!sharedPrefs.getBoolean(KEY_ZEPP_CLEANUP_DONE, false)) {
            sharedPrefs.edit().apply {
                remove("zepp_email")
                remove("zepp_password")
                remove("zepp_region")
                remove("zepp_refresh_token")
                remove("zepp_access_token")
                remove("zepp_app_token")
                remove("zepp_login_token")
                remove("zepp_user_id")
                remove("zepp_device_id")
                remove("zepp_last_sync_ms")
                putBoolean(KEY_ZEPP_CLEANUP_DONE, true)
            }.apply()
        }
        sharedPrefs
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

    /** Bearer-Key fuer den privaten Second-Brain-Server (Ideen-Connector). */
    var secondBrainApiKey: String?
        get() = prefs.getString(KEY_SECOND_BRAIN, null)
        set(value) { prefs.edit().putString(KEY_SECOND_BRAIN, value).apply() }

    /** WireGuard-Konfiguration fuer den privaten Second-Brain-Tunnel. */
    var secondBrainWireGuardConfig: String?
        get() = prefs.getString(KEY_SECOND_BRAIN_WG, null)
        set(value) { prefs.edit().putString(KEY_SECOND_BRAIN_WG, value).apply() }

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

    // Zepp / Amazfit Cloud-API wurde entfernt (Sprint 3, 2026-05-22):
    // - API lieferte seit Mai 2026 keine neuen Workouts mehr (Zepp-App-Update)
    // - Plaintext-Credentials (E-Mail + Passwort) in EncryptedSharedPrefs waren ein Risiko
    // - Health Connect ist seit 2026-07-03 die primaere Quelle fuer Workouts
    // Historische Daten mit source="zepp" in amazfit_workouts bleiben erhalten.
    // Die Migration im prefs-Initializer entfernt alte Eintraege beim ersten Start.

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

    // Polar OAuth-Properties entfernt 2026-05-17 (Frank-Wunsch). Polar-Historie
    // wird nur noch ueber ZIP-Bulk-Import eingelesen, keine API-Anbindung.
    // Die alten DataStore-Eintraege bleiben physisch in den EncryptedShared-
    // Preferences erhalten (kein Cleanup-Pflicht), werden aber von keinem
    // Code mehr gelesen.

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

    /**
     * Zeitpunkt (epoch ms) des letzten automatischen Aufgaben-Refresh beim App-Start
     * (Frank-Wunsch 2026-05-22). Throttle: wir laufen hoechstens einmal pro 6 Stunden,
     * damit Cold-Starts kurz hintereinander keine Gemini-Quota verbrennen.
     */
    var lastStartupRefreshAtMs: Long
        get() = prefs.getLong(KEY_LAST_STARTUP_REFRESH, 0L)
        set(value) { prefs.edit().putLong(KEY_LAST_STARTUP_REFRESH, value).apply() }

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
        private const val KEY_SECOND_BRAIN = "second_brain_api_key"
        private const val KEY_SECOND_BRAIN_WG = "second_brain_wireguard_config"
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
        private const val KEY_LAST_STARTUP_REFRESH = "last_startup_refresh_at_ms"
        // Zepp / Amazfit Cloud-API Felder entfernt 2026-05-22 (Sprint 3).
        // Migration: KEY_ZEPP_CLEANUP_DONE markiert dass alte Eintraege bereits geleert wurden.
        private const val KEY_ZEPP_CLEANUP_DONE = "zepp_cleanup_done_v1"
        // Oura Ring
        private const val KEY_OURA_PAT = "oura_personal_access_token"
        private const val KEY_OURA_LAST_SYNC = "oura_last_sync_ms"
        // Polar-OAuth-Keys entfernt 2026-05-17 (Frank-Wunsch).
    }
}
