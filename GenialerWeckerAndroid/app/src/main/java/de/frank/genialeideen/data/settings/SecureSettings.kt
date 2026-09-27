package de.frank.genialeideen.data.settings

import android.content.Context
import android.content.SharedPreferences
import java.io.Closeable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Die Einstellungen der App: Darstellung und Vorlesen. Es gibt keine Schlüssel und keine Konten,
 * deshalb liegen sie in normalen SharedPreferences (Anmeldedaten-geschützter Speicher).
 */
class SecureSettings(context: Context) : Closeable {
    private val appContext = context.applicationContext

    private val preferences: SharedPreferences? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        // Vor dem ersten Entsperren nach einem Neustart ist dieser Speicher gesperrt; dann gelten die Vorgaben.
        runCatching { appContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE) }.getOrNull()
    }

    /**
     * Ob der Speicher wirklich geöffnet werden konnte. Ohne ihn liefert jeder Lesezugriff den Vorgabewert —
     * Aufrufer, für die ein falscher Vorgabewert teuer ist (z. B. ein hell startender Weckbildschirm), prüfen das.
     */
    val verfuegbar: Boolean get() = preferences != null

    private val _themeFlow = MutableStateFlow(readString(Keys.THEME, Defaults.THEME))
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _ausrichtungFlow = MutableStateFlow(ausrichtung)
    val ausrichtungFlow: StateFlow<String> = _ausrichtungFlow.asStateFlow()

    /** Ob die Statuszeile oben sichtbar ist. Vorgabe: ausgeblendet. */
    var statuszeileSichtbar: Boolean
        get() = readString("statuszeile", "aus") == "an"
        set(value) { writeString("statuszeile", if (value) "an" else "aus"); _statuszeileFlow.value = value }
    private val _statuszeileFlow = MutableStateFlow(readString("statuszeile", "aus") == "an")
    val statuszeileFlow: StateFlow<Boolean> = _statuszeileFlow.asStateFlow()

    private val _designFlow = MutableStateFlow(design)
    val designFlow: StateFlow<String> = _designFlow.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            Keys.AUSRICHTUNG -> _ausrichtungFlow.value = ausrichtung
            Keys.DESIGN -> _designFlow.value = design
            Keys.THEME -> _themeFlow.value = prefs.getString(Keys.THEME, Defaults.THEME) ?: Defaults.THEME
        }
    }

    init {
        preferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    // ---- Vorlesen: ausschließlich lokal installierte Android-Stimmen ----

    /** Name der gewählten Android-Stimme; leer = beste installierte deutsche Offline-Stimme. */
    var lokaleStimme: String
        get() = readString(Keys.LOKALE_STIMME, "")
        set(value) = writeString(Keys.LOKALE_STIMME, value)

    /**
     * Bevorzugte Stimme je Sprache: eine Premium-Stimme (Kennung wie „de-DE-KatjaNeural“) oder eine Gerätestimme.
     * Ohne eigene Wahl gilt die Premium-Vorgabe der Sprache, passend zur Region des Geräts.
     */
    fun stimmeFuer(sprache: String): String = readString("stimme2_$sprache", "").takeIf { de.frank.wecker.PremiumKatalog.istPremium(it) } ?: run {
        de.frank.wecker.PremiumKatalog.vorgabe(sprache, java.util.Locale.getDefault().country).orEmpty()
    }
    fun setzeStimme(sprache: String, name: String) { writeString("stimme2_$sprache", name) }

    /** Lieblingsstimmen (Stern); sie stehen in jeder Auswahl ganz oben. */
    var stimmFavoriten: Set<String>
        get() = readString("stimm_favoriten", "").split(',').filter(String::isNotBlank).toSet()
        set(value) = writeString("stimm_favoriten", value.joinToString(","))

    var ttsSpeechRate: Float
        get() = preferences?.getFloat(Keys.TTS_SPEECH_RATE, Defaults.TTS_SPEECH_RATE)
            ?.coerceIn(MIN_TTS_SPEECH_RATE, MAX_TTS_SPEECH_RATE) ?: Defaults.TTS_SPEECH_RATE
        set(value) {
            preferences?.edit()
                ?.putFloat(Keys.TTS_SPEECH_RATE, value.coerceIn(MIN_TTS_SPEECH_RATE, MAX_TTS_SPEECH_RATE))
                ?.apply()
        }

    // ---- Darstellung ----

    /** Gewähltes Erscheinungsbild; unabhängig von Hell/Dunkel und Ausrichtung. Vorgabe bleibt „schlicht". */
    var design: String
        get() = readString(Keys.DESIGN, Defaults.DESIGN).takeIf { it in ALLOWED_DESIGNS } ?: Defaults.DESIGN
        set(value) {
            val normalized = value.takeIf { it in ALLOWED_DESIGNS } ?: Defaults.DESIGN
            writeString(Keys.DESIGN, normalized)
            _designFlow.value = normalized
        }

    var ausrichtung: String
        get() = readString(Keys.AUSRICHTUNG, Defaults.AUSRICHTUNG)
            .takeIf { it in ALLOWED_AUSRICHTUNGEN } ?: Defaults.AUSRICHTUNG
        set(value) {
            val normalized = value.takeIf { it in ALLOWED_AUSRICHTUNGEN } ?: Defaults.AUSRICHTUNG
            writeString(Keys.AUSRICHTUNG, normalized)
            _ausrichtungFlow.value = normalized
        }

    var theme: String
        get() = readString(Keys.THEME, Defaults.THEME)
        set(value) {
            val normalized = value.takeIf(ALLOWED_THEMES::contains) ?: Defaults.THEME
            _themeFlow.value = normalized
            writeString(Keys.THEME, normalized)
        }

    override fun close() {
        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readString(key: String, default: String): String =
        preferences?.getString(key, default) ?: default

    private fun writeString(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }

    object Keys {
        const val LOKALE_STIMME = "lokale_stimme"
        const val TTS_SPEECH_RATE = "tts_speech_rate"
        const val THEME = "theme"
        const val AUSRICHTUNG = "ausrichtung"
        const val DESIGN = "design"
    }

    object Defaults {
        const val TTS_SPEECH_RATE = 1f
        const val THEME = "light"
        const val AUSRICHTUNG = "automatisch"
        const val DESIGN = "schlicht"
    }

    companion object {
        const val STORE_NAME = "wecker_einstellungen"
        val ALLOWED_THEMES = setOf("light", "dark", "system")
        val ALLOWED_AUSRICHTUNGEN = setOf("hochformat", "querformat", "automatisch")
        val ALLOWED_DESIGNS = setOf("schlicht", "morgenruhe", "traumraum", "orbit")
        const val MIN_TTS_SPEECH_RATE = 0.5f
        const val MAX_TTS_SPEECH_RATE = 2.0f
    }
}
