package de.frank.cortex.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class UserContextPrompt(
    val id: String,
    val text: String,
    val enabled: Boolean = true,
    val originalText: String? = null
)

// DataStore-Delegate muss top-level leben (eine Instanz pro Prozess).
private val Context.cortexSecretsDataStore: DataStore<Preferences> by preferencesDataStore(name = "cortex_secrets_ds")

object SettingsStore {

    private const val ENCRYPTED_PREFS = "cortex_secrets"          // Alt-Speicher (nur noch Migration)
    private const val PLAIN_PREFS = "cortex_prefs"
    private const val MIGRATED_FLAG = "_migrated_from_encrypted_prefs"

    // Schluessel-Speicher seit 2026-07-02: DataStore + Tink (BP client-anbindung Kurzcheck #4) —
    // EncryptedSharedPreferences ist deprecated und wird nicht mehr gepflegt. Werte liegen
    // Tink-AEAD-verschluesselt (Master-Key im Android Keystore) als Base64 im DataStore; eine
    // In-Memory-Kopie haelt die bisherige SYNCHRONE API unveraendert (alle Aufrufer bleiben 1:1).
    private lateinit var dataStore: DataStore<Preferences>
    private var aead: Aead? = null
    private val secretCache = ConcurrentHashMap<String, String>()
    // limitedParallelism(1): Writes desselben Keys muessen den DataStore in AUFRUF-ReihENFOLGE
    // erreichen — auf dem Multi-Thread-IO-Pool konnten zwei schnelle setSecret-Aufrufe
    // (SecretRow schreibt pro Tastendruck) vertauscht ankommen und ein aelterer Zwischenstand
    // blieb persistiert (fiel erst nach App-Neustart auf).
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    private val SECRET_KEYS = listOf(
        "sb_api_key", "groq_api_key", "gemini_api_key", "google_tts_api_key",
        "wg_config", "codex_access_token", "codex_refresh_token"
    )

    private lateinit var plain: SharedPreferences

    const val CONTEXT_MODE_AUTO = "auto"
    const val CONTEXT_MODE_SMALLTALK = "smalltalk"
    const val CONTEXT_MODE_SAVE = "save"
    const val CONTEXT_MODE_RULE = "rule"
    const val CONTEXT_MODE_SEARCH = "search"

    const val RESPONSE_SIZE_AUTO = "auto"
    const val RESPONSE_SIZE_SHORT = "s"
    const val RESPONSE_SIZE_MEDIUM = "m"
    const val RESPONSE_SIZE_XL = "xl"

    // Vorlese-Motor (TTS-Provider): Google Chirp 3: HD (Standard) oder Microsoft Edge TTS.
    const val TTS_PROVIDER_CHIRP = "chirp"
    const val TTS_PROVIDER_EDGE = "edge"
    const val DEFAULT_EDGE_VOICE = "de-DE-SeraphinaMultilingualNeural"

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
        CONTEXT_MODE_RULE -> """
            Du bist jetzt im Regelmodus. Franks Eingabe ist ausschließlich eine neue dauerhafte Verhaltensregel
            für den Cortex-Hauptagenten. Erstelle keinen Gedächtniseintrag und starte keine Suche.
        """.trimIndent()
        CONTEXT_MODE_SEARCH -> """
            Du bist jetzt im Suchmodus. Frank möchte etwas aus seinem Gedächtnis wissen.
            Interpretiere seine Eingabe als Such- oder Erinnerungsfrage, formuliere klare Suchstichworte
            und antworte anschließend nur aus echten Gedächtnis-Treffern.
        """.trimIndent()
        else -> ""
    }

    fun defaultResponseSizePrompt(size: String): String = when (size) {
        RESPONSE_SIZE_AUTO -> """
            Antwortlänge A ist aktiv: Passe Länge, Detailgrad und Struktur automatisch an Franks Eingabe an.
            Kurze, klare Fragen beantwortest du kurz und direkt. Komplexe Fragen oder Arbeitsaufträge beantwortest du so ausführlich,
            strukturiert und gründlich, wie es für die bestmögliche Antwort sinnvoll ist. Keine künstliche Länge erzwingen.
        """.trimIndent()
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
        plain = context.getSharedPreferences(PLAIN_PREFS, Context.MODE_PRIVATE)
        dataStore = context.cortexSecretsDataStore
        aead = buildAead(context)

        // In-Memory-Kopie laden (einmalig blockierend beim App-Start — wie frueher
        // EncryptedSharedPreferences.create, das ebenfalls synchron initialisierte).
        val migrated = try {
            val prefs = runBlocking { dataStore.data.first() }
            prefs.asMap().forEach { (key, value) ->
                if (value is String) {
                    decryptSecret(key.name, value)?.let { secretCache[key.name] = it }
                }
            }
            prefs[booleanPreferencesKey(MIGRATED_FLAG)] ?: false
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.error("SettingsStore", "init", "DataStore nicht lesbar: ${e.message}")
            false
        }

        if (!migrated) migrateFromEncryptedPrefs(context)
        CortexLog.info("SettingsStore", "init", "SettingsStore initialisiert (DataStore+Tink)",
            mapOf("secrets_cached" to secretCache.size, "aead" to (aead != null)))
    }

    /** Tink-AEAD mit Keystore-Master-Key. Bei korruptem Keyset (z.B. Keystore-Reset) wird das
     *  Keyset verworfen und frisch aufgebaut — die App darf am Start nie crashen.
     *  WICHTIG: Der erste Fehlversuch loescht NICHTS — der Android-Keystore ist direkt nach
     *  Boot/Unlock gelegentlich kurz nicht verfuegbar (transient). Ein sofortiges clear()
     *  haette dann ALLE gespeicherten Secrets dauerhaft unentschluesselbar gemacht. */
    private fun buildAead(context: Context): Aead? {
        AeadConfig.register()
        repeat(3) { attempt ->
            try {
                return AndroidKeysetManager.Builder()
                    .withSharedPref(context, "cortex_tink_keyset", "cortex_tink_prefs")
                    .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                    .withMasterKeyUri("android-keystore://cortex_tink_master_key")
                    .build()
                    .keysetHandle
                    .getPrimitive(Aead::class.java)
            } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                CortexLog.error("SettingsStore", "buildAead", "Tink-Init fehlgeschlagen (Versuch ${attempt + 1}): ${e.message}")
                when (attempt) {
                    // Versuch 1 -> transient annehmen: kurz warten, OHNE clear() erneut versuchen.
                    0 -> Thread.sleep(150)
                    // Versuch 2 -> Keyset vermutlich wirklich korrupt: verwerfen + frisch aufbauen.
                    1 -> context.getSharedPreferences("cortex_tink_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                }
            }
        }
        return null   // Notlauf: Secrets nur in-memory fuer diese Sitzung (Fehler ist geloggt)
    }

    /** Einmalige Uebernahme der Alt-Secrets aus EncryptedSharedPreferences (deprecated).
     *  Erst nach erfolgreicher Uebernahme wird die alte Datei geloescht — kein Key geht verloren. */
    private fun migrateFromEncryptedPrefs(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val old = EncryptedSharedPreferences.create(
                context, ENCRYPTED_PREFS, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            var moved = 0
            SECRET_KEYS.forEach { key ->
                val value = old.getString(key, null)
                if (!value.isNullOrEmpty()) {
                    secretCache[key] = value
                    moved++
                }
            }
            // Synchron persistieren (Migration ist der eine Moment, wo Warten korrekt ist).
            // Erfolg PRO KEY pruefen: bei aead == null (Tink-Notlauf) wurde frueher NICHTS
            // geschrieben, die Altdatei aber trotzdem geloescht -> alle Keys nach dem
            // naechsten Neustart endgueltig weg. Jetzt: Altdatei nur loeschen + Flag nur
            // setzen, wenn wirklich ALLE Werte verschluesselt im DataStore gelandet sind.
            var persisted = 0
            runBlocking {
                dataStore.edit { store ->
                    secretCache.forEach { (key, value) ->
                        encryptSecret(key, value)?.let {
                            store[stringPreferencesKey(key)] = it
                            persisted++
                        }
                    }
                    if (persisted == secretCache.size) {
                        store[booleanPreferencesKey(MIGRATED_FLAG)] = true
                    }
                }
            }
            if (persisted == secretCache.size) {
                context.deleteSharedPreferences(ENCRYPTED_PREFS)
                CortexLog.info("SettingsStore", "migrate", "Secrets von EncryptedSharedPreferences uebernommen",
                    mapOf("moved" to moved))
            } else {
                CortexLog.error("SettingsStore", "migrate",
                    "Migration unvollstaendig ($persisted/${secretCache.size} persistiert) — Altdatei bleibt, naechster Start versucht es erneut")
            }
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            // Alte Datei fehlt/korrupt (frische Installation oder Keystore-Reset): nichts zu
            // uebernehmen — Flag setzen, damit nicht bei jedem Start erneut versucht wird.
            CortexLog.warn("SettingsStore", "migrate", "Keine Alt-Secrets uebernommen: ${e.message}")
            storeScope.launch {
                try {
                    dataStore.edit { it[booleanPreferencesKey(MIGRATED_FLAG)] = true }
                } catch (inner: CancellationException) {
                    throw inner
                } catch (inner: Exception) {
                    CortexLog.error("SettingsStore", "migrate", "Migrations-Flag nicht setzbar: ${inner.message}")
                }
            }
        }
    }

    private fun encryptSecret(key: String, value: String): String? = try {
        val cipher = aead?.encrypt(value.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        if (cipher != null) Base64.encodeToString(cipher, Base64.NO_WRAP) else null
    } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
        CortexLog.error("SettingsStore", "encrypt", "Verschluesseln fehlgeschlagen ($key): ${e.message}")
        null
    }

    private fun decryptSecret(key: String, encoded: String): String? = try {
        val cipher = Base64.decode(encoded, Base64.NO_WRAP)
        aead?.decrypt(cipher, key.toByteArray(Charsets.UTF_8))?.toString(Charsets.UTF_8)
    } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
        CortexLog.error("SettingsStore", "decrypt", "Entschluesseln fehlgeschlagen ($key): ${e.message}")
        null
    }

    private fun getSecret(key: String): String = secretCache[key] ?: ""

    private fun setSecret(key: String, value: String) {
        secretCache[key] = value
        // Persistenz asynchron (wie frueher .apply()); der Cache traegt die Session sofort.
        storeScope.launch {
            try {
                val encoded = encryptSecret(key, value)
                dataStore.edit { store ->
                    when {
                        encoded != null -> store[stringPreferencesKey(key)] = encoded
                        // Leeren Wert bewusst entfernen (Loeschen eines Keys).
                        value.isEmpty() -> store.remove(stringPreferencesKey(key))
                        // Encrypt-FEHLER (aead-Notlauf): den letzten guten persistierten Wert
                        // BEHALTEN statt ihn zu loeschen — sonst zerstoerte ein Schreibfehler
                        // auch noch den funktionierenden alten Stand.
                        else -> CortexLog.error("SettingsStore", "setSecret",
                            "Verschluesseln fehlgeschlagen ($key) — alter persistierter Wert bleibt erhalten")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("SettingsStore", "setSecret", "Persistieren fehlgeschlagen ($key): ${e.message}")
            }
        }
    }

    private fun removeSecrets(vararg keys: String) {
        keys.forEach { secretCache.remove(it) }
        storeScope.launch {
            try {
                dataStore.edit { store -> keys.forEach { store.remove(stringPreferencesKey(it)) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("SettingsStore", "removeSecrets", "Entfernen fehlgeschlagen: ${e.message}")
            }
        }
    }

    // --- Secrets (verschlüsselt via Tink, persistiert im DataStore) ---

    var sbApiKey: String
        get() = getSecret("sb_api_key")
        set(value) = setSecret("sb_api_key", value)

    var groqApiKey: String
        get() = getSecret("groq_api_key")
        set(value) = setSecret("groq_api_key", value)

    var geminiApiKey: String
        get() = getSecret("gemini_api_key")
        set(value) = setSecret("gemini_api_key", value)

    // Dedizierter Schluessel fuer die Google Cloud Text-to-Speech API (Chirp 3: HD).
    // Leer -> es wird der Gemini-Schluessel benutzt. Gefuellt -> dieser Schluessel wird fuer TTS
    // genutzt (wie in BestJournalFrank, dessen TTS-Schluessel-Projekt die Cloud-TTS-API aktiv hat).
    var googleTtsApiKey: String
        get() = getSecret("google_tts_api_key")
        set(value) = setSecret("google_tts_api_key", value)

    /** Der fuer TTS zu verwendende Schluessel: dedizierter TTS-Key falls gesetzt, sonst Gemini-Key. */
    val ttsApiKey: String
        get() = googleTtsApiKey.ifBlank { geminiApiKey }

    var wgConfig: String
        get() = getSecret("wg_config")
        set(value) = setSecret("wg_config", value)

    var codexAccessToken: String
        get() = getSecret("codex_access_token")
        set(value) = setSecret("codex_access_token", value)

    var codexRefreshToken: String
        get() = getSecret("codex_refresh_token")
        set(value) = setSecret("codex_refresh_token", value)

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

    // Letzte /config-Antwort als JSON (Performance 2026-07-12): die Einstellungen zeigen den
    // zwischengespeicherten Server-Stand SOFORT (keine Wartezeit) und aktualisieren still im
    // Hintergrund. Enthält keine Secrets (Modelle, Prompts, Limits) — Klartext ok.
    var cachedAgentConfigJson: String
        get() = plain.getString("cached_agent_config", "") ?: ""
        set(value) = plain.edit().putString("cached_agent_config", value).apply()

    // Letzte Dashboard-Übersicht als JSON (Performance 2026-07-13): totalEntries, Kategorie-Zähler
    // und Vitals werden beim Start SOFORT gezeigt (kein „0 Einträge" mehr, bis der Server antwortet).
    // Enthält keine Secrets — Klartext ok.
    var cachedDashboardJson: String
        get() = plain.getString("cached_dashboard", "") ?: ""
        set(value) = plain.edit().putString("cached_dashboard", value).apply()

    // --- Sync-Vormerkungen (Offline-Aenderungen an Prompts nachsenden) ---
    // true = lokale Prompt-Aenderung konnte nicht zum Server (kein VPN / PUT-Fehler): beim
    // naechsten Connect werden die LOKALEN Prompts hochgeschoben, statt sie vom alten
    // Server-Stand ueberschreiben zu lassen (sonst war jeder Offline-Edit still verloren).
    var pendingUserContextPromptSync: Boolean
        get() = plain.getBoolean("pending_user_context_prompt_sync", false)
        set(value) = plain.edit().putBoolean("pending_user_context_prompt_sync", value).apply()

    var pendingEditablePromptSync: Boolean
        get() = plain.getBoolean("pending_editable_prompt_sync", false)
        set(value) = plain.edit().putBoolean("pending_editable_prompt_sync", value).apply()

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

    // Welcher Vorlese-Motor genutzt wird (TTS_PROVIDER_CHIRP oder TTS_PROVIDER_EDGE).
    var ttsProvider: String
        get() = plain.getString("tts_provider", TTS_PROVIDER_CHIRP) ?: TTS_PROVIDER_CHIRP
        set(value) = plain.edit().putString("tts_provider", value).apply()

    // Gewaehlte Edge-TTS-Stimme (voller Name, z.B. de-DE-SeraphinaMultilingualNeural).
    var edgeTtsVoice: String
        get() = plain.getString("edge_tts_voice", DEFAULT_EDGE_VOICE) ?: DEFAULT_EDGE_VOICE
        set(value) = plain.edit().putString("edge_tts_voice", value).apply()

    // Sprechtempo (Design: 0.7–1.4, Schritt 0.05, Standard 1.0). Wird auf die Wiedergabe angewandt.
    var ttsRate: Float
        get() = plain.getFloat("tts_rate", 1.0f)
        set(value) = plain.edit().putFloat("tts_rate", value).apply()

    var responseSize: String
        get() = plain.getString("response_size", RESPONSE_SIZE_AUTO) ?: RESPONSE_SIZE_AUTO
        set(value) = plain.edit().putString(
            "response_size",
            when (value) {
                RESPONSE_SIZE_AUTO, RESPONSE_SIZE_SHORT, RESPONSE_SIZE_MEDIUM, RESPONSE_SIZE_XL -> value
                else -> RESPONSE_SIZE_AUTO
            }
        ).apply()

    val contextModeRevision: Int
        get() = plain.getInt("context_mode_revision", 0).coerceAtLeast(0)

    val contextMode: String
        get() = plain.getString("context_mode", CONTEXT_MODE_AUTO)
            ?.takeIf { it in setOf(CONTEXT_MODE_AUTO, CONTEXT_MODE_SMALLTALK, CONTEXT_MODE_SAVE, CONTEXT_MODE_RULE, CONTEXT_MODE_SEARCH) }
            ?: CONTEXT_MODE_AUTO

    fun advanceContextModeRevision(mode: String): Int {
        val next = if (contextModeRevision == Int.MAX_VALUE) 1 else contextModeRevision + 1
        plain.edit()
            .putInt("context_mode_revision", next)
            .putString("context_mode", mode)
            .apply()
        return next
    }

    var recordingToneEnabled: Boolean
        get() = plain.getBoolean("recording_tone_enabled", true)
        set(value) = plain.edit().putBoolean("recording_tone_enabled", value).apply()

    var recordingToneVolume: Float
        get() = plain.getFloat("recording_tone_volume", 0.45f)
        set(value) = plain.edit().putFloat("recording_tone_volume", value.coerceIn(0f, 1f)).apply()

    var biometricLockEnabled: Boolean
        get() = plain.getBoolean("biometric_lock_enabled", false)
        set(value) = plain.edit().putBoolean("biometric_lock_enabled", value).apply()

    val codexConnected: Boolean
        get() = codexAccessToken.isNotBlank()

    fun clearCodexAuth() {
        removeSecrets("codex_access_token", "codex_refresh_token")
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

    fun userContextPrompts(): List<UserContextPrompt> {
        val raw = plain.getString("user_context_prompts", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(
                        UserContextPrompt(
                            id = id,
                            text = item.optString("text"),
                            enabled = item.optBoolean("enabled", true),
                            originalText = item.optString("original_text").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.warn("SettingsStore", "userContextPrompts", "Kontext-Prompts nicht lesbar: ${e.message}")
            emptyList()
        }
    }

    fun setUserContextPrompts(prompts: List<UserContextPrompt>) {
        val array = JSONArray()
        prompts.forEach { prompt ->
            array.put(
                JSONObject()
                    .put("id", prompt.id)
                    .put("text", prompt.text)
                    .put("enabled", prompt.enabled)
                    .put("original_text", prompt.originalText ?: "")
            )
        }
        plain.edit().putString("user_context_prompts", array.toString()).apply()
    }

    // --- Hilfsmethoden ---

    fun agentUrl(): String = "http://${serverHost}:${agentPort}"
    fun brainUrl(): String = "http://${serverHost}:${brainPort}"
    fun dashboardUrl(): String = "http://${serverHost}:${dashboardPort}"
}
