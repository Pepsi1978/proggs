package de.frank.experimente.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Die Einstellungen aus `01-FUNKTIONS-SPEC.md` §3 — verschlüsselt auf dem Gerät
 * (`EncryptedSharedPreferences`, AES-256), wie `SecureSettings.kt` in PerfectMoment.
 *
 * Kein Schlüssel steht im Quellcode (F-24). Alles liegt verschlüsselt hier.
 */
class Einstellungen(ctx: Context) {

    private val p: SharedPreferences = run {
        val schluessel = MasterKey.Builder(ctx.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx.applicationContext,
            "experimente_einstellungen",
            schluessel,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // --- F-22: Modell und Effort, getrennt für Experimente und Logbuch ------------------
    var modellExperimente: String
        get() = p.getString(MODEL_EXPERIMENTS, "gpt-5.6-terra")!!
        set(v) { p.edit().putString(MODEL_EXPERIMENTS, v).commit() }

    var effortExperimente: String
        get() = p.getString(EFFORT_EXPERIMENTS, "high")!!
        set(v) { p.edit().putString(EFFORT_EXPERIMENTS, v).commit() }

    var modellLogbuch: String
        get() = p.getString(MODEL_LOGBOOK, "gpt-5.6-luna")!!
        set(v) { p.edit().putString(MODEL_LOGBOOK, v).commit() }

    var effortLogbuch: String
        get() = p.getString(EFFORT_LOGBOOK, "medium")!!
        set(v) { p.edit().putString(EFFORT_LOGBOOK, v).commit() }

    // --- F-23: Stimme und Vorlesen ------------------------------------------------------
    var ttsAnbieter: String
        get() = p.getString(TTS_PROVIDER, "google_cloud")!!
        set(v) { p.edit().putString(TTS_PROVIDER, v).commit() }

    var stimmeGoogle: String
        get() = p.getString(TTS_VOICE_GOOGLE, "de-DE-Chirp3-HD-Kore")!!
        set(v) { p.edit().putString(TTS_VOICE_GOOGLE, v).commit() }

    var stimmeEdge: String
        get() = p.getString(TTS_VOICE_EDGE, "de-DE-SeraphinaMultilingualNeural")!!
        set(v) { p.edit().putString(TTS_VOICE_EDGE, v).commit() }

    var stimmeQwen: String
        get() = p.getString(TTS_VOICE_QWEN, "")!!
        set(v) { p.edit().putString(TTS_VOICE_QWEN, v).commit() }

    var sprechtempo: Float
        get() = p.getFloat(TTS_RATE, 1.0f)
        set(v) { p.edit().putFloat(TTS_RATE, v).commit() }

    // --- F-24: Zugänge ------------------------------------------------------------------
    var groqSchluessel: String
        get() = p.getString(GROQ_API_KEY, "")!!
        set(v) { p.edit().putString(GROQ_API_KEY, v).commit() }

    var googleTtsSchluessel: String
        get() = p.getString(GOOGLE_TTS_API_KEY, "")!!
        set(v) { p.edit().putString(GOOGLE_TTS_API_KEY, v).commit() }

    var qwenSchluessel: String
        get() = p.getString(QWEN_API_KEY, "")!!
        set(v) { p.edit().putString(QWEN_API_KEY, v).commit() }

    // --- F-25: Erinnerungen -------------------------------------------------------------
    var erinnerungMorgensAn: Boolean
        get() = p.getBoolean(REMINDER_MORNING_ON, false)
        set(v) { p.edit().putBoolean(REMINDER_MORNING_ON, v).commit() }

    var erinnerungMorgensZeit: String
        get() = p.getString(REMINDER_MORNING_TIME, "08:00")!!
        set(v) { p.edit().putString(REMINDER_MORNING_TIME, v).commit() }

    var erinnerungAbendsAn: Boolean
        get() = p.getBoolean(REMINDER_EVENING_ON, false)
        set(v) { p.edit().putBoolean(REMINDER_EVENING_ON, v).commit() }

    var erinnerungAbendsZeit: String
        get() = p.getString(REMINDER_EVENING_TIME, "20:30")!!
        set(v) { p.edit().putString(REMINDER_EVENING_TIME, v).commit() }

    // --- F-26: Erscheinung --------------------------------------------------------------
    /** `light` · `dark` · `system`. Gespeichert wird der Modus, nicht die aufgelöste Farbe. */
    var erscheinung: String
        get() = p.getString(THEME, "dark")!!
        set(v) { p.edit().putString(THEME, v).commit() }

    // --- F-41: Effekt-Stärke -------------------------------------------------------------
    /**
     * `voll` · `gedaempft` · `aus`. Die Wahl wirkt sofort, ohne Neustart, auf allen
     * Bildschirmen. Auf *Aus* bleibt jede Funktion vollständig bedienbar.
     */
    var effektstufe: String
        get() = p.getString(EFFECT_LEVEL, "voll")!!
        set(v) { p.edit().putString(EFFECT_LEVEL, v).commit() }

    // --- Nachlauf offener Hintergrundschritte (§6) ---------------------------------------
    /**
     * F-14 / F-15 / F-17 merken sich, dass sie ausstehen, wenn kein Netz da war, und laufen
     * beim nächsten Start nach. Es geht nichts verloren.
     */
    var ausstehend: Set<String>
        get() = p.getStringSet(AUSSTEHEND, emptySet())!!
        set(v) { p.edit().putStringSet(AUSSTEHEND, v).commit() }

    fun merkeAusstehend(schritt: String) {
        ausstehend = ausstehend + schritt
    }

    fun erledigeAusstehend(schritt: String) {
        ausstehend = ausstehend - schritt
    }

    /** Der Tag, an dem F-15 zuletzt gelaufen ist — damit sie je Kalendertag einmal läuft. */
    var verdichtetAm: String
        get() = p.getString(VERDICHTET_AM, "")!!
        set(v) { p.edit().putString(VERDICHTET_AM, v).commit() }

    /** Einmaliger Hinweis, wenn `POST_NOTIFICATIONS` abgelehnt wurde (F-25). */
    var hinweisBenachrichtigungGezeigt: Boolean
        get() = p.getBoolean(HINWEIS_BENACHRICHTIGUNG, false)
        set(v) { p.edit().putBoolean(HINWEIS_BENACHRICHTIGUNG, v).commit() }

    private companion object {
        const val MODEL_EXPERIMENTS = "model_experiments"
        const val EFFORT_EXPERIMENTS = "effort_experiments"
        const val MODEL_LOGBOOK = "model_logbook"
        const val EFFORT_LOGBOOK = "effort_logbook"
        const val TTS_PROVIDER = "tts_provider"
        const val TTS_VOICE_GOOGLE = "tts_voice_google"
        const val TTS_VOICE_EDGE = "tts_voice_edge"
        const val TTS_VOICE_QWEN = "tts_voice_qwen"
        const val TTS_RATE = "tts_rate"
        const val GROQ_API_KEY = "groq_api_key"
        const val GOOGLE_TTS_API_KEY = "google_tts_api_key"
        const val QWEN_API_KEY = "qwen_api_key"
        const val REMINDER_MORNING_ON = "reminder_morning_on"
        const val REMINDER_MORNING_TIME = "reminder_morning_time"
        const val REMINDER_EVENING_ON = "reminder_evening_on"
        const val REMINDER_EVENING_TIME = "reminder_evening_time"
        const val THEME = "theme"
        const val EFFECT_LEVEL = "effect_level"
        const val AUSSTEHEND = "ausstehend"
        const val VERDICHTET_AM = "verdichtet_am"
        const val HINWEIS_BENACHRICHTIGUNG = "hinweis_benachrichtigung"
    }
}
