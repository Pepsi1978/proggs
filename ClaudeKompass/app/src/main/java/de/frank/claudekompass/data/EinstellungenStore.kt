package de.frank.claudekompass.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.claudekompass.data.model.Denktiefe
import de.frank.claudekompass.data.model.KiModell
import de.frank.claudekompass.data.model.TtsAnbieter
import de.frank.claudekompass.observability.KompassLog
import de.frank.claudekompass.tts.TtsCatalog
import de.frank.claudekompass.ui.theme.ThemeModus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Alle Einstellungen der App an einer Stelle.
 *
 * Zwei getrennte Ablagen mit Absicht:
 *  - [offen] für alles Harmlose (Erscheinungsbild, Sprechtempo, gewählte Stimme),
 *  - [geheim] ausschließlich für API-Schlüssel, verschlüsselt.
 *
 * Lässt sich die verschlüsselte Ablage auf einem Gerät nicht öffnen — das kommt bei
 * beschädigtem Schlüsselspeicher und auf manchen Emulator-Abbildern vor —, werden Schlüssel
 * NICHT ersatzweise im Klartext gespeichert. Stattdessen meldet [geheimVerfuegbar] das
 * Problem, und die Oberfläche sagt es dem Benutzer. Ein stiller Rückfall auf Klartext wäre
 * die Art Fehler, die man erst bemerkt, wenn der Schlüssel schon abgeflossen ist.
 */
class EinstellungenStore(context: Context) {

    private val appContext = context.applicationContext

    private val offen: SharedPreferences =
        appContext.getSharedPreferences(OFFENE_ABLAGE, Context.MODE_PRIVATE)

    private var geheimFehler: String? = null

    private val geheim: SharedPreferences? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        try {
            val hauptschluessel = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                GEHEIME_ABLAGE,
                hauptschluessel,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (fehler: Exception) {
            geheimFehler = fehler.message ?: fehler.javaClass.simpleName
            KompassLog.error(
                "EinstellungenStore",
                "geheim",
                "Verschlüsselte Ablage nicht verfügbar — Schlüssel können nicht gespeichert werden",
                mapOf("grund" to geheimFehler),
            )
            null
        }
    }

    /** true, wenn Schlüssel sicher abgelegt werden können. */
    val geheimVerfuegbar: Boolean get() = geheim != null

    /** Klartext-Grund, warum die verschlüsselte Ablage fehlt — für die Meldung in der App. */
    val geheimFehlerText: String? get() = geheimFehler

    // --- Erscheinungsbild -----------------------------------------------------------------

    private val _themeModus = MutableStateFlow(
        ThemeModus.fromId(offen.getString(SCHL_THEME, ThemeModus.SYSTEM.id) ?: ThemeModus.SYSTEM.id),
    )
    val themeModus: StateFlow<ThemeModus> = _themeModus.asStateFlow()

    fun setzeThemeModus(modus: ThemeModus) {
        offen.edit().putString(SCHL_THEME, modus.id).apply()
        _themeModus.value = modus
        KompassLog.info("EinstellungenStore", "setzeThemeModus", "Erscheinungsbild gewechselt", mapOf("modus" to modus.id))
    }

    // --- Vorlesen -------------------------------------------------------------------------

    var ttsAnbieter: TtsAnbieter
        get() = TtsAnbieter.fromId(offen.getString(SCHL_TTS_ANBIETER, TtsAnbieter.GOOGLE.id).orEmpty())
        set(wert) = offen.edit().putString(SCHL_TTS_ANBIETER, wert.id).apply()

    var googleStimme: String
        get() = offen.getString(SCHL_GOOGLE_STIMME, TtsCatalog.STANDARD_GOOGLE_STIMME) ?: TtsCatalog.STANDARD_GOOGLE_STIMME
        set(wert) = offen.edit().putString(SCHL_GOOGLE_STIMME, wert).apply()

    var edgeStimme: String
        get() = offen.getString(SCHL_EDGE_STIMME, TtsCatalog.STANDARD_EDGE_STIMME) ?: TtsCatalog.STANDARD_EDGE_STIMME
        set(wert) = offen.edit().putString(SCHL_EDGE_STIMME, wert).apply()

    var qwenStimmeId: String
        get() = offen.getString(SCHL_QWEN_STIMME, "").orEmpty()
        set(wert) = offen.edit().putString(SCHL_QWEN_STIMME, wert.filterNot(Char::isWhitespace)).apply()

    /** Sprechtempo laut Referenz zwischen 0,5 und 2,0. */
    var sprechtempo: Float
        get() = offen.getFloat(SCHL_TEMPO, 1f).coerceIn(MIN_TEMPO, MAX_TEMPO)
        set(wert) = offen.edit().putFloat(SCHL_TEMPO, wert.coerceIn(MIN_TEMPO, MAX_TEMPO)).apply()

    var lieblingsStimmen: Set<String>
        get() = offen.getStringSet(SCHL_LIEBLINGSSTIMMEN, emptySet()).orEmpty()
        set(wert) = offen.edit().putStringSet(SCHL_LIEBLINGSSTIMMEN, wert).apply()

    // --- Spracheingabe --------------------------------------------------------------------

    var groqModell: String
        get() = offen.getString(SCHL_GROQ_MODELL, STANDARD_GROQ_MODELL) ?: STANDARD_GROQ_MODELL
        set(wert) = offen.edit().putString(SCHL_GROQ_MODELL, wert).apply()

    /** Die vier Halluzinations-Schichten sind einzeln abschaltbar; Voreinstellung: alle an. */
    fun filterSchichtAktiv(schicht: Int): Boolean = offen.getBoolean(schlFilter(schicht), true)

    fun setzeFilterSchicht(schicht: Int, aktiv: Boolean) {
        offen.edit().putBoolean(schlFilter(schicht), aktiv).apply()
        KompassLog.info(
            "EinstellungenStore",
            "setzeFilterSchicht",
            "Halluzinations-Schicht umgeschaltet",
            mapOf("schicht" to schicht, "aktiv" to aktiv),
        )
    }

    // --- KI-Modell ------------------------------------------------------------------------

    var modellId: String
        get() = offen.getString(SCHL_MODELL, KiModell.standard.apiId) ?: KiModell.standard.apiId
        set(wert) = offen.edit().putString(SCHL_MODELL, wert).apply()

    var modellLabel: String
        get() = offen.getString(SCHL_MODELL_LABEL, KiModell.standard.label) ?: KiModell.standard.label
        set(wert) = offen.edit().putString(SCHL_MODELL_LABEL, wert).apply()

    var denktiefe: Denktiefe
        get() = Denktiefe.fromValue(offen.getString(SCHL_DENKTIEFE, Denktiefe.MEDIUM.apiValue).orEmpty())
        set(wert) = offen.edit().putString(SCHL_DENKTIEFE, wert.apiValue).apply()

    /** Zusätzlich eingetragene Modelle, damit spätere Modelle ohne App-Update nutzbar sind. */
    var eigeneModelle: Set<String>
        get() = offen.getStringSet(SCHL_EIGENE_MODELLE, emptySet()).orEmpty()
        set(wert) = offen.edit().putStringSet(SCHL_EIGENE_MODELLE, wert).apply()

    // --- Sicherheit -----------------------------------------------------------------------

    var appSperreAktiv: Boolean
        get() = offen.getBoolean(SCHL_APP_SPERRE, false)
        set(wert) = offen.edit().putBoolean(SCHL_APP_SPERRE, wert).apply()

    /** Wartezeit im Hintergrund, nach der wieder gesperrt wird. -1 bedeutet „nie". */
    var sperreNachSekunden: Int
        get() = offen.getInt(SCHL_SPERRE_NACH, 60)
        set(wert) = offen.edit().putInt(SCHL_SPERRE_NACH, wert).apply()

    // --- Schlüssel (nur verschlüsselt) ----------------------------------------------------

    var googleSchluessel: String
        get() = leseGeheim(SCHL_GOOGLE_KEY)
        set(wert) = schreibeGeheim(SCHL_GOOGLE_KEY, wert)

    var alibabaSchluessel: String
        get() = leseGeheim(SCHL_ALIBABA_KEY)
        set(wert) = schreibeGeheim(SCHL_ALIBABA_KEY, wert)

    var groqSchluessel: String
        get() = leseGeheim(SCHL_GROQ_KEY)
        set(wert) = schreibeGeheim(SCHL_GROQ_KEY, wert)

    private fun leseGeheim(schluessel: String): String =
        geheim?.getString(schluessel, "").orEmpty()

    private fun schreibeGeheim(schluessel: String, wert: String) {
        val ablage = geheim
        if (ablage == null) {
            // Nicht still schlucken: Der Benutzer glaubt sonst, der Schlüssel sei gespeichert.
            KompassLog.error(
                "EinstellungenStore",
                "schreibeGeheim",
                "Schlüssel konnte nicht gespeichert werden — verschlüsselte Ablage fehlt",
                mapOf("feld" to schluessel),
            )
            return
        }
        // Leerzeichen aus der Zwischenablage sind die häufigste Ursache für ein abgelehntes 401.
        ablage.edit().putString(schluessel, wert.filterNot(Char::isWhitespace)).apply()
    }

    private fun schlFilter(schicht: Int) = "filter_schicht_$schicht"

    companion object {
        const val OFFENE_ABLAGE = "claude_kompass_prefs"
        const val GEHEIME_ABLAGE = "claude_kompass_secure_prefs"
        const val STANDARD_GROQ_MODELL = "whisper-large-v3-turbo"
        const val MIN_TEMPO = 0.5f
        const val MAX_TEMPO = 2.0f

        private const val SCHL_THEME = "theme_modus"
        private const val SCHL_TTS_ANBIETER = "tts_anbieter"
        private const val SCHL_GOOGLE_STIMME = "tts_google_stimme"
        private const val SCHL_EDGE_STIMME = "tts_edge_stimme"
        private const val SCHL_QWEN_STIMME = "tts_qwen_stimme"
        private const val SCHL_TEMPO = "tts_tempo"
        private const val SCHL_LIEBLINGSSTIMMEN = "tts_lieblingsstimmen"
        private const val SCHL_GROQ_MODELL = "groq_modell"
        private const val SCHL_MODELL = "ki_modell"
        private const val SCHL_MODELL_LABEL = "ki_modell_label"
        private const val SCHL_DENKTIEFE = "ki_denktiefe"
        private const val SCHL_EIGENE_MODELLE = "ki_eigene_modelle"
        private const val SCHL_APP_SPERRE = "app_sperre"
        private const val SCHL_SPERRE_NACH = "app_sperre_nach"
        private const val SCHL_GOOGLE_KEY = "google_api_key"
        private const val SCHL_ALIBABA_KEY = "alibaba_api_key"
        private const val SCHL_GROQ_KEY = "groq_api_key"
    }
}
