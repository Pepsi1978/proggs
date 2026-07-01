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

    const val CONTEXT_MODE_AUTO = "auto"
    const val CONTEXT_MODE_SMALLTALK = "smalltalk"
    const val CONTEXT_MODE_SAVE = "save"
    const val CONTEXT_MODE_SEARCH = "search"

    const val RESPONSE_SIZE_SHORT = "s"
    const val RESPONSE_SIZE_MEDIUM = "m"
    const val RESPONSE_SIZE_XL = "xl"

    fun defaultContextPrompt(mode: String): String = when (mode) {
        CONTEXT_MODE_SMALLTALK -> """
            Du bist jetzt im Smalltalk-Modus. Führe ein normales Gespräch mit Frank.
            Speichere nichts, suche nichts im Gedächtnis und starte keine Internet- oder Speicher-Aktion.
            Gehe freundlich, intelligent und direkt auf Franks Fragen, Gedanken und Wünsche ein.
        """.trimIndent()
        CONTEXT_MODE_SAVE -> """
            Du bist jetzt im Speichermodus. Frank möchte dir Informationen geben, die ins Gedächtnis sollen.
            Interpretiere seine Eingabe als zu speichernde Information, reagiere intelligent im Speicherkontext
            und frage wie gewohnt vor dem endgültigen Ablegen kurz nach.
        """.trimIndent()
        CONTEXT_MODE_SEARCH -> """
            Du bist jetzt im Suchmodus. Frank möchte etwas aus seinem Gedächtnis wissen.
            Interpretiere seine Eingabe als Such- oder Erinnerungsfrage, formuliere klare Suchstichworte
            und antworte anschließend nur aus echten Gedächtnis-Treffern.
        """.trimIndent()
        else -> ""
    }

    fun defaultResponseSizePrompt(size: String): String = when (size) {
        RESPONSE_SIZE_SHORT -> """
            Antwortlänge S ist aktiv: Antworte exakt auf den Punkt, kurz und eindeutig.
            Suche nur nach der wirklich passenden Antwort. Wenn ein perfekter Treffer gefunden ist, nutze ihn sofort
            und ziehe keine zusätzlichen Gedächtnis- oder Webtreffer künstlich hinzu.
        """.trimIndent()
        RESPONSE_SIZE_XL -> """
            Antwortlänge XL ist aktiv: Antworte maximal ausführlich, stark strukturiert und mit vielen Details.
            Bei Websuche: recherchiere breit und gründlich. Bei Gedächtnissuche: beziehe mehrere relevante Einträge ein,
            vergleiche sie, konsolidiere Widersprüche und erkläre den Gesamtzusammenhang global statt nur den besten Treffer zu nennen.
            Es gibt kein künstliches Kürzelimit; nutze die verfügbare Antwortlänge sinnvoll aus.
        """.trimIndent()
        else -> """
            Antwortlänge M ist aktiv: Antworte ausgewogen, gut erklärt und anschaulich.
            Suche bei Bedarf mehrere relevante Gedächtnis- oder Webtreffer, konsolidiere sie, aber bleibe weder zu kurz noch unnötig lang.
        """.trimIndent()
    }

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

    var codexAccessToken: String
        get() = encrypted.getString("codex_access_token", "") ?: ""
        set(value) = encrypted.edit().putString("codex_access_token", value).apply()

    var codexRefreshToken: String
        get() = encrypted.getString("codex_refresh_token", "") ?: ""
        set(value) = encrypted.edit().putString("codex_refresh_token", value).apply()

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

    var themeMode: String
        get() = plain.getString("theme_mode", "dark") ?: "dark"
        set(value) = plain.edit().putString("theme_mode", value).apply()

    var ttsEnabled: Boolean
        get() = plain.getBoolean("tts_enabled", true)
        set(value) = plain.edit().putBoolean("tts_enabled", value).apply()

    var ttsVoice: String
        get() = plain.getString("tts_voice", "Kore") ?: "Kore"
        set(value) = plain.edit().putString("tts_voice", value).apply()

    // Sprechtempo (Design: 0.7–1.4, Schritt 0.05, Standard 1.0). Wird auf die Wiedergabe angewandt.
    var ttsRate: Float
        get() = plain.getFloat("tts_rate", 1.0f)
        set(value) = plain.edit().putFloat("tts_rate", value).apply()

    var responseSize: String
        get() = plain.getString("response_size", RESPONSE_SIZE_MEDIUM) ?: RESPONSE_SIZE_MEDIUM
        set(value) = plain.edit().putString(
            "response_size",
            when (value) {
                RESPONSE_SIZE_SHORT, RESPONSE_SIZE_MEDIUM, RESPONSE_SIZE_XL -> value
                else -> RESPONSE_SIZE_MEDIUM
            }
        ).apply()

    var recordingToneEnabled: Boolean
        get() = plain.getBoolean("recording_tone_enabled", true)
        set(value) = plain.edit().putBoolean("recording_tone_enabled", value).apply()

    var recordingToneVolume: Float
        get() = plain.getFloat("recording_tone_volume", 0.45f)
        set(value) = plain.edit().putFloat("recording_tone_volume", value.coerceIn(0f, 1f)).apply()

    var biometricLockEnabled: Boolean
        get() = plain.getBoolean("biometric_lock_enabled", false)
        set(value) = plain.edit().putBoolean("biometric_lock_enabled", value).apply()

    var codexLocalEnabled: Boolean
        get() = plain.getBoolean("codex_local_enabled", false)
        set(value) = plain.edit().putBoolean("codex_local_enabled", value).apply()

    var codexModel: String
        get() = plain.getString("codex_model", "gpt-5.5") ?: "gpt-5.5"
        set(value) = plain.edit().putString("codex_model", value).apply()

    var codexReasoning: String
        get() = plain.getString("codex_reasoning", "medium") ?: "medium"
        set(value) = plain.edit().putString("codex_reasoning", value).apply()

    val codexConnected: Boolean
        get() = codexAccessToken.isNotBlank()

    fun clearCodexAuth() {
        encrypted.edit()
            .remove("codex_access_token")
            .remove("codex_refresh_token")
            .apply()
        codexLocalEnabled = false
    }

    fun contextPrompt(mode: String): String = plain.getString("context_prompt_$mode", null)
        ?: defaultContextPrompt(mode)

    fun setContextPrompt(mode: String, prompt: String) {
        plain.edit().putString("context_prompt_$mode", prompt).apply()
    }

    fun responseSizePrompt(size: String): String = plain.getString("response_size_prompt_$size", null)
        ?: defaultResponseSizePrompt(size)

    fun setResponseSizePrompt(size: String, prompt: String) {
        plain.edit().putString("response_size_prompt_$size", prompt).apply()
    }

    // --- Hilfsmethoden ---

    fun agentUrl(): String = "http://${serverHost}:${agentPort}"
    fun brainUrl(): String = "http://${serverHost}:${brainPort}"
    fun dashboardUrl(): String = "http://${serverHost}:${dashboardPort}"
}
