package de.frank.experimente.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Alle Einstellungen aus 01-FUNKTIONS-SPEC.md §3, verschlüsselt auf dem Gerät
 * (`EncryptedSharedPreferences`, AES-256) — wie `SecureSettings.kt` in PerfectMoment.
 *
 * Kein Schlüssel steht im Quellcode.
 */
class Einstellungen(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                STORE,
                masterKey,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            null
        }
    }

    private val _erscheinung = MutableStateFlow(lies(Schluessel.THEME, Standard.THEME))
    val erscheinungFlow: StateFlow<String> = _erscheinung.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        if (key == Schluessel.THEME) {
            _erscheinung.value = p.getString(Schluessel.THEME, Standard.THEME) ?: Standard.THEME
        }
    }

    init {
        prefs?.registerOnSharedPreferenceChangeListener(listener)
    }

    // --- F-22: Modell und Effort, getrennt für Experimente und Logbuch ---
    var modellExperimente: String
        get() = lies(Schluessel.MODELL_EXPERIMENTE, Standard.MODELL_EXPERIMENTE)
        set(v) = schreib(Schluessel.MODELL_EXPERIMENTE, v)

    var effortExperimente: String
        get() = lies(Schluessel.EFFORT_EXPERIMENTE, Standard.EFFORT_EXPERIMENTE)
        set(v) = schreib(Schluessel.EFFORT_EXPERIMENTE, v)

    var modellLogbuch: String
        get() = lies(Schluessel.MODELL_LOGBUCH, Standard.MODELL_LOGBUCH)
        set(v) = schreib(Schluessel.MODELL_LOGBUCH, v)

    var effortLogbuch: String
        get() = lies(Schluessel.EFFORT_LOGBUCH, Standard.EFFORT_LOGBUCH)
        set(v) = schreib(Schluessel.EFFORT_LOGBUCH, v)

    // --- F-23: Stimme und Vorlesen ---
    var ttsAnbieter: String
        get() = lies(Schluessel.TTS_ANBIETER, Standard.TTS_ANBIETER)
        set(v) = schreib(Schluessel.TTS_ANBIETER, v)

    var stimmeGoogle: String
        get() = lies(Schluessel.STIMME_GOOGLE, Standard.STIMME_GOOGLE)
        set(v) = schreib(Schluessel.STIMME_GOOGLE, v)

    var stimmeEdge: String
        get() = lies(Schluessel.STIMME_EDGE, Standard.STIMME_EDGE)
        set(v) = schreib(Schluessel.STIMME_EDGE, v)

    var stimmeEigen: String
        get() = lies(Schluessel.STIMME_EIGEN, "")
        set(v) = schreib(Schluessel.STIMME_EIGEN, v)

    var sprechtempo: Float
        get() = prefs?.getFloat(Schluessel.SPRECHTEMPO, Standard.SPRECHTEMPO) ?: Standard.SPRECHTEMPO
        set(v) { prefs?.edit()?.putFloat(Schluessel.SPRECHTEMPO, v.coerceIn(0.7f, 1.3f))?.apply() }

    // --- F-24: Zugänge ---
    var groqSchluessel: String
        get() = lies(Schluessel.GROQ, "")
        set(v) = schreib(Schluessel.GROQ, v)

    var googleTtsSchluessel: String
        get() = lies(Schluessel.GOOGLE_TTS, "")
        set(v) = schreib(Schluessel.GOOGLE_TTS, v)

    var qwenSchluessel: String
        get() = lies(Schluessel.QWEN, "")
        set(v) = schreib(Schluessel.QWEN, v)

    // --- F-25: Erinnerungen ---
    var erinnerungMorgensAn: Boolean
        get() = prefs?.getBoolean(Schluessel.ERINNERUNG_MORGENS_AN, false) ?: false
        set(v) { prefs?.edit()?.putBoolean(Schluessel.ERINNERUNG_MORGENS_AN, v)?.apply() }

    var erinnerungMorgensZeit: String
        get() = lies(Schluessel.ERINNERUNG_MORGENS_ZEIT, Standard.ERINNERUNG_MORGENS_ZEIT)
        set(v) = schreib(Schluessel.ERINNERUNG_MORGENS_ZEIT, v)

    var erinnerungAbendsAn: Boolean
        get() = prefs?.getBoolean(Schluessel.ERINNERUNG_ABENDS_AN, false) ?: false
        set(v) { prefs?.edit()?.putBoolean(Schluessel.ERINNERUNG_ABENDS_AN, v)?.apply() }

    var erinnerungAbendsZeit: String
        get() = lies(Schluessel.ERINNERUNG_ABENDS_ZEIT, Standard.ERINNERUNG_ABENDS_ZEIT)
        set(v) = schreib(Schluessel.ERINNERUNG_ABENDS_ZEIT, v)

    // --- F-26: Erscheinung ---
    var erscheinung: String
        get() = lies(Schluessel.THEME, Standard.THEME)
        set(v) = schreib(Schluessel.THEME, v)

    /** Merkt den Kalendertag, an dem F-15 (Verdichtung) zuletzt gelaufen ist. */
    var letzteVerdichtung: String
        get() = lies(Schluessel.LETZTE_VERDICHTUNG, "")
        set(v) = schreib(Schluessel.LETZTE_VERDICHTUNG, v)

    private fun lies(key: String, standard: String): String =
        prefs?.getString(key, standard) ?: standard

    private fun schreib(key: String, wert: String) {
        prefs?.edit()?.putString(key, wert)?.apply()
    }

    private object Schluessel {
        const val MODELL_EXPERIMENTE = "model_experiments"
        const val EFFORT_EXPERIMENTE = "effort_experiments"
        const val MODELL_LOGBUCH = "model_logbook"
        const val EFFORT_LOGBUCH = "effort_logbook"
        const val TTS_ANBIETER = "tts_provider"
        const val STIMME_GOOGLE = "tts_voice_google"
        const val STIMME_EDGE = "tts_voice_edge"
        const val STIMME_EIGEN = "tts_voice_qwen"
        const val SPRECHTEMPO = "tts_rate"
        const val GROQ = "groq_api_key"
        const val GOOGLE_TTS = "google_tts_api_key"
        const val QWEN = "qwen_api_key"
        const val ERINNERUNG_MORGENS_AN = "reminder_morning_on"
        const val ERINNERUNG_MORGENS_ZEIT = "reminder_morning_time"
        const val ERINNERUNG_ABENDS_AN = "reminder_evening_on"
        const val ERINNERUNG_ABENDS_ZEIT = "reminder_evening_time"
        const val THEME = "theme"
        const val LETZTE_VERDICHTUNG = "last_compaction"
    }

    private object Standard {
        const val MODELL_EXPERIMENTE = "gpt-5.6-terra"
        const val EFFORT_EXPERIMENTE = "high"
        const val MODELL_LOGBUCH = "gpt-5.6-luna"
        const val EFFORT_LOGBUCH = "medium"
        const val TTS_ANBIETER = "google_cloud"
        const val STIMME_GOOGLE = "de-DE-Chirp3-HD-Kore"
        const val STIMME_EDGE = "de-DE-SeraphinaMultilingualNeural"
        const val SPRECHTEMPO = 1.0f
        const val ERINNERUNG_MORGENS_ZEIT = "08:00"
        const val ERINNERUNG_ABENDS_ZEIT = "20:30"
        const val THEME = "dark"
    }

    private companion object {
        const val STORE = "experimente_secure"
    }
}
