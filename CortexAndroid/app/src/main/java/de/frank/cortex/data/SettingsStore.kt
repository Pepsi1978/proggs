package de.frank.cortex.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.cortex.observability.CortexLog

object SettingsStore {

    private const val ENCRYPTED_PREFS = "cortex_secrets"
    private const val PLAIN_PREFS = "cortex_prefs"

    private lateinit var encrypted: SharedPreferences
    private lateinit var plain: SharedPreferences

    fun init(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encrypted = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            plain = context.getSharedPreferences(PLAIN_PREFS, Context.MODE_PRIVATE)
            CortexLog.info("SettingsStore", "init", "SettingsStore initialisiert")
        } catch (e: Exception) {
            CortexLog.error("SettingsStore", "init", "Fehler bei Initialisierung: ${e.message}")
            // Fallback: neue EncryptedPrefs anlegen
            context.getSharedPreferences(ENCRYPTED_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            encrypted = EncryptedSharedPreferences.create(
                context, ENCRYPTED_PREFS, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            plain = context.getSharedPreferences(PLAIN_PREFS, Context.MODE_PRIVATE)
        }
    }

    // --- Secrets (verschlüsselt) ---

    var sbApiKey: String
        get() = encrypted.getString("sb_api_key", "") ?: ""
        set(value) = encrypted.edit().putString("sb_api_key", value).apply()

    var groqApiKey: String
        get() = encrypted.getString("groq_api_key", "") ?: ""
        set(value) = encrypted.edit().putString("groq_api_key", value).apply()

    var geminiApiKey: String
        get() = encrypted.getString("gemini_api_key", "") ?: ""
        set(value) = encrypted.edit().putString("gemini_api_key", value).apply()

    var wgConfig: String
        get() = encrypted.getString("wg_config", "") ?: ""
        set(value) = encrypted.edit().putString("wg_config", value).apply()

    // --- Verbindung ---

    var serverHost: String
        get() = plain.getString("server_host", "10.8.0.1") ?: "10.8.0.1"
        set(value) = plain.edit().putString("server_host", value).apply()

    var agentPort: String
        get() = plain.getString("agent_port", "8002") ?: "8002"
        set(value) = plain.edit().putString("agent_port", value).apply()

    var brainPort: String
        get() = plain.getString("brain_port", "8000") ?: "8000"
        set(value) = plain.edit().putString("brain_port", value).apply()

    var dashboardPort: String
        get() = plain.getString("dashboard_port", "8003") ?: "8003"
        set(value) = plain.edit().putString("dashboard_port", value).apply()

    // --- UI-Präferenzen (klartext ok) ---

    var isDarkTheme: Boolean
        get() = plain.getBoolean("dark_theme", true)
        set(value) = plain.edit().putBoolean("dark_theme", value).apply()

    var ttsEnabled: Boolean
        get() = plain.getBoolean("tts_enabled", true)
        set(value) = plain.edit().putBoolean("tts_enabled", value).apply()

    var ttsVoice: String
        get() = plain.getString("tts_voice", "Aoede") ?: "Aoede"
        set(value) = plain.edit().putString("tts_voice", value).apply()

    // --- Hilfsmethoden ---

    fun agentUrl(): String = "http://${serverHost}:${agentPort}"
    fun brainUrl(): String = "http://${serverHost}:${brainPort}"
    fun dashboardUrl(): String = "http://${serverHost}:${dashboardPort}"
}
