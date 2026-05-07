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

    private val prefs: SharedPreferences = run {
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

    /** Account-Mailadresse fuer Drive-Backup. Wird beim Sign-In gesetzt. */
    var driveAccountEmail: String?
        get() = prefs.getString(KEY_DRIVE_ACCOUNT, null)
        set(value) { prefs.edit().putString(KEY_DRIVE_ACCOUNT, value).apply() }

    /** Toggle: Drive-Backup aktiv? Steuert ob Mutationen einen Sync-Trigger feuern. */
    var driveBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_DRIVE_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_DRIVE_ENABLED, value).apply() }

    /** Zeitstempel des letzten erfolgreichen Backup-Uploads. */
    var driveLastBackupEpochMs: Long
        get() = prefs.getLong(KEY_DRIVE_LAST_BACKUP, 0L)
        set(value) { prefs.edit().putLong(KEY_DRIVE_LAST_BACKUP, value).apply() }

    fun clearAll() {
        prefs.edit().clear().apply()
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
    }
}
