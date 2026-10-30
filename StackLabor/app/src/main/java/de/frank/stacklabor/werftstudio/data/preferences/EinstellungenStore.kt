package de.frank.stacklabor.werftstudio.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.service.codex.CodexModel
import de.frank.stacklabor.werftstudio.service.codex.ReasoningEffort
import de.frank.stacklabor.werftstudio.service.speech.GroqTranscriber
import de.frank.stacklabor.werftstudio.service.tts.GeminiKeyChoice
import de.frank.stacklabor.werftstudio.service.tts.TtsVoices
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.stackLaborEinstellungenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "stacklabor_einstellungen",
)

/** AUTOMATIK folgt der Sonne in Neuenhagen (nicht dem System); HELL und DUNKEL sind fest. */
enum class Erscheinung(val code: Int) { HELL(0), DUNKEL(1), AUTOMATIK(2) }
enum class TtsAnbieter(val code: Int) { EDGE(0), GOOGLE_CLOUD(1), QWEN_CLONE(2), GEMINI_FLASH(3), GEMINI_LITE(4), AZURE(5) }
enum class Absatzpause(val code: Int) { KURZ(0), MITTEL(1), LANG(2) }

data class AppEinstellungen(
    val erscheinung: Erscheinung = Erscheinung.AUTOMATIK,
    val dosisVariante: DosisVariante = DosisVariante.FREI,
    val ttsAnbieter: TtsAnbieter = TtsAnbieter.EDGE,
    val ttsStimme: String = "de-DE-SeraphinaMultilingualNeural",
    val ttsTempo: Float = 1.0f,
    val absatzpause: Absatzpause = Absatzpause.KURZ,
    val abschaltzeitMinuten: Int = 30,
    val ttsVerbrauchZeichen: Long = 0,
    /**
     * Schlüssel, die der Benutzer selbst in den Einstellungen hinterlegt. Leer heißt: nicht
     * eingetragen, der betroffene Anbieter ist dann nicht nutzbar.
     */
    val googleApiKey: String = "",
    val qwenApiKey: String = "",
    val qwenStimmenId: String = "",
    /** Gemini-3.8-Stimme: Schlüssel ohne bzw. mit Abrechnung, Schlüsselwahl und Rückfall auf Chirp 3 HD. */
    val geminiTtsKeyFrei: String = "",
    val geminiTtsKeyBezahlt: String = "",
    val geminiTtsSchluesselWahl: String = GeminiKeyChoice.FREE_THEN_PAID,
    val geminiTtsChirpRueckfall: Boolean = false,
    /** Je Gemini-Anbieter und für Azure die zuletzt gewählte Stimme. */
    val geminiFlashStimme: String = TtsVoices.DEFAULT_GEMINI_FLASH_VOICE,
    val geminiLiteStimme: String = TtsVoices.DEFAULT_GEMINI_LITE_VOICE,
    val azureTtsKey: String = "",
    val azureTtsRegion: String = TtsVoices.DEFAULT_AZURE_REGION,
    val azureStimme: String = TtsVoices.DEFAULT_AZURE_VOICE,
    /** Als Favorit markierte Stimmen (Kennungen aller Anbieter); sie stehen in der Stimmliste oben. */
    val stimmFavoriten: Set<String> = emptySet(),
    val groqApiKey: String = "",
    /** Groq-Whisper-Modell (Standard Turbo) und persönliches Wörterbuch für die Spracheingabe. */
    val groqModell: String = GroqTranscriber.MODEL_TURBO,
    val woerterbuchAn: Boolean = true,
    val woerterbuch: String = GroqTranscriber.STANDARD_WOERTERBUCH,
    /** Löslichkeits-Ansicht: fettlösliche zuerst (Standard) oder wasserlösliche zuerst. */
    val loeslichkeitFettZuerst: Boolean = true,
    val codexModell: String = "gpt-5.6-terra",
    val codexDenkstufe: String = "high",
    val bewegungReduziert: Boolean,
    /** Ist die App durch den Fingerabdruck geschützt? */
    val fingerabdruckAktiv: Boolean = false,
)

class EinstellungenStore(
    private val dataStore: DataStore<Preferences>,
    private val systemBewegungReduziert: Boolean,
) {
    val einstellungen: Flow<AppEinstellungen> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw throwable
        }
        .map(::mappe)

    suspend fun setzeErscheinung(value: Erscheinung): Unit = schreibe(ERSCHEINUNG, value.code)
    suspend fun setzeDosisVariante(value: DosisVariante): Unit = schreibe(DOSIS_VARIANTE, value.code)
    suspend fun setzeTtsAnbieter(value: TtsAnbieter): Unit = schreibe(TTS_ANBIETER, value.code)
    suspend fun setzeTtsStimme(value: String): Unit = schreibe(TTS_STIMME, value)
    suspend fun setzeTtsTempo(value: Float) {
        require(value in 0.5f..2.0f) { "TTS-Tempo muss zwischen 0,5 und 2,0 liegen." }
        schreibe(TTS_TEMPO, value)
    }
    suspend fun setzeAbsatzpause(value: Absatzpause): Unit = schreibe(ABSATZPAUSE, value.code)
    suspend fun setzeAbschaltzeitMinuten(value: Int) {
        require(value > 0) { "Die Abschaltzeit muss positiv sein." }
        schreibe(ABSCHALTZEIT_MINUTEN, value)
    }
    suspend fun addiereTtsVerbrauch(zeichen: Long) {
        require(zeichen >= 0) { "Der Verbrauch darf nicht negativ sein." }
        dataStore.edit { preferences ->
            preferences[TTS_VERBRAUCH] = (preferences[TTS_VERBRAUCH] ?: 0L) + zeichen
        }
    }
    suspend fun setzeLoeslichkeitFettZuerst(value: Boolean): Unit = schreibe(LOESLICHKEIT_FETT_ZUERST, value)

    /** Leerer Wert löscht den Schlüssel. */
    suspend fun setzeGoogleApiKey(value: String): Unit = schreibe(GOOGLE_API_KEY, value.trim())
    suspend fun setzeQwenApiKey(value: String): Unit = schreibe(QWEN_API_KEY, value.filterNot(Char::isWhitespace))
    suspend fun setzeQwenStimmenId(value: String): Unit = schreibe(QWEN_STIMMEN_ID, value.filterNot(Char::isWhitespace))
    suspend fun setzeGeminiTtsKeyFrei(value: String): Unit = schreibe(GEMINI_TTS_KEY_FREI, value.filterNot(Char::isWhitespace))
    suspend fun setzeGeminiTtsKeyBezahlt(value: String): Unit = schreibe(GEMINI_TTS_KEY_BEZAHLT, value.filterNot(Char::isWhitespace))
    suspend fun setzeGeminiTtsSchluesselWahl(value: String): Unit = schreibe(GEMINI_TTS_SCHLUESSEL_WAHL, value)
    suspend fun setzeGeminiTtsChirpRueckfall(value: Boolean): Unit = schreibe(GEMINI_TTS_CHIRP_RUECKFALL, value)
    suspend fun setzeGeminiFlashStimme(value: String): Unit = schreibe(GEMINI_FLASH_STIMME, value)
    suspend fun setzeGeminiLiteStimme(value: String): Unit = schreibe(GEMINI_LITE_STIMME, value)
    suspend fun setzeAzureTtsKey(value: String): Unit = schreibe(AZURE_TTS_KEY, value.filterNot(Char::isWhitespace))
    suspend fun setzeAzureTtsRegion(value: String): Unit = schreibe(AZURE_TTS_REGION, value.trim())
    suspend fun setzeAzureStimme(value: String): Unit = schreibe(AZURE_STIMME, value)

    /** Schaltet eine Stimme als Favorit an oder aus. */
    suspend fun schalteStimmFavorit(stimmeId: String) {
        dataStore.edit { preferences ->
            val aktuell = preferences[STIMM_FAVORITEN].orEmpty()
            preferences[STIMM_FAVORITEN] = if (stimmeId in aktuell) aktuell - stimmeId else aktuell + stimmeId
        }
    }
    suspend fun setzeGroqApiKey(value: String): Unit = schreibe(GROQ_API_KEY, value.filterNot(Char::isWhitespace))
    suspend fun setzeGroqModell(value: String): Unit =
        schreibe(GROQ_MODELL, if (value == GroqTranscriber.MODEL_LARGE_V3) value else GroqTranscriber.MODEL_TURBO)
    suspend fun setzeWoerterbuchAn(value: Boolean): Unit = schreibe(WOERTERBUCH_AN, value)
    suspend fun setzeWoerterbuch(value: String): Unit = schreibe(WOERTERBUCH, value)

    suspend fun setzeCodexModell(value: String) {
        require(value in CODEX_MODELLE) { "Unbekanntes Codex-Modell: $value" }
        dataStore.edit { preferences ->
            preferences[CODEX_MODELL] = value
            preferences[CODEX_DENKSTUFE] = CodexModel.fromApiId(value)
                .normalizeReasoning(preferences[CODEX_DENKSTUFE]).apiValue
        }
    }
    suspend fun setzeCodexDenkstufe(value: String) {
        require(value in DENKSTUFEN) { "Unbekannte Denkstufe: $value" }
        dataStore.edit { preferences ->
            preferences[CODEX_DENKSTUFE] = CodexModel.fromApiId(preferences[CODEX_MODELL])
                .normalizeReasoning(value).apiValue
        }
    }
    suspend fun setzeBewegungReduziert(value: Boolean): Unit = schreibe(BEWEGUNG_REDUZIERT, value)
    suspend fun setzeFingerabdruckAktiv(value: Boolean): Unit = schreibe(FINGERABDRUCK_AKTIV, value)

    private fun mappe(preferences: Preferences): AppEinstellungen = AppEinstellungen(
        erscheinung = enumMitCode(Erscheinung.entries, preferences[ERSCHEINUNG] ?: Erscheinung.AUTOMATIK.code) { it.code },
        dosisVariante = enumMitCode(DosisVariante.entries, preferences[DOSIS_VARIANTE] ?: DosisVariante.FREI.code) { it.code },
        ttsAnbieter = enumMitCode(TtsAnbieter.entries, preferences[TTS_ANBIETER] ?: TtsAnbieter.EDGE.code) { it.code },
        ttsStimme = preferences[TTS_STIMME] ?: "de-DE-SeraphinaMultilingualNeural",
        ttsTempo = preferences[TTS_TEMPO] ?: 1.0f,
        absatzpause = enumMitCode(Absatzpause.entries, preferences[ABSATZPAUSE] ?: Absatzpause.KURZ.code) { it.code },
        abschaltzeitMinuten = preferences[ABSCHALTZEIT_MINUTEN] ?: 30,
        ttsVerbrauchZeichen = preferences[TTS_VERBRAUCH] ?: 0L,
        googleApiKey = preferences[GOOGLE_API_KEY].orEmpty(),
        qwenApiKey = preferences[QWEN_API_KEY].orEmpty(),
        qwenStimmenId = preferences[QWEN_STIMMEN_ID].orEmpty(),
        geminiTtsKeyFrei = preferences[GEMINI_TTS_KEY_FREI].orEmpty(),
        geminiTtsKeyBezahlt = preferences[GEMINI_TTS_KEY_BEZAHLT].orEmpty(),
        geminiTtsSchluesselWahl = preferences[GEMINI_TTS_SCHLUESSEL_WAHL] ?: GeminiKeyChoice.FREE_THEN_PAID,
        geminiTtsChirpRueckfall = preferences[GEMINI_TTS_CHIRP_RUECKFALL] ?: false,
        geminiFlashStimme = preferences[GEMINI_FLASH_STIMME] ?: TtsVoices.DEFAULT_GEMINI_FLASH_VOICE,
        geminiLiteStimme = preferences[GEMINI_LITE_STIMME] ?: TtsVoices.DEFAULT_GEMINI_LITE_VOICE,
        azureTtsKey = preferences[AZURE_TTS_KEY].orEmpty(),
        azureTtsRegion = preferences[AZURE_TTS_REGION] ?: TtsVoices.DEFAULT_AZURE_REGION,
        azureStimme = preferences[AZURE_STIMME] ?: TtsVoices.DEFAULT_AZURE_VOICE,
        stimmFavoriten = preferences[STIMM_FAVORITEN].orEmpty(),
        groqApiKey = preferences[GROQ_API_KEY].orEmpty(),
        groqModell = preferences[GROQ_MODELL] ?: GroqTranscriber.MODEL_TURBO,
        woerterbuchAn = preferences[WOERTERBUCH_AN] ?: true,
        woerterbuch = preferences[WOERTERBUCH] ?: GroqTranscriber.STANDARD_WOERTERBUCH,
        loeslichkeitFettZuerst = preferences[LOESLICHKEIT_FETT_ZUERST] ?: true,
        codexModell = CodexModel.fromApiId(preferences[CODEX_MODELL]).apiId,
        codexDenkstufe = CodexModel.fromApiId(preferences[CODEX_MODELL])
            .normalizeReasoning(preferences[CODEX_DENKSTUFE]).apiValue,
        bewegungReduziert = preferences[BEWEGUNG_REDUZIERT] ?: systemBewegungReduziert,
        fingerabdruckAktiv = preferences[FINGERABDRUCK_AKTIV] ?: false,
    )

    private suspend fun <T> schreibe(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private fun <T> enumMitCode(values: Iterable<T>, code: Int, selector: (T) -> Int): T =
        values.firstOrNull { selector(it) == code }
            ?: throw IllegalStateException("Unbekannter Einstellungs-Code: $code")

    private companion object {
        val ERSCHEINUNG = intPreferencesKey("erscheinung")
        val DOSIS_VARIANTE = intPreferencesKey("dosis_variante")
        val TTS_ANBIETER = intPreferencesKey("tts_anbieter")
        val TTS_STIMME = stringPreferencesKey("tts_stimme")
        val TTS_TEMPO = floatPreferencesKey("tts_tempo")
        val ABSATZPAUSE = intPreferencesKey("absatzpause")
        val ABSCHALTZEIT_MINUTEN = intPreferencesKey("abschaltzeit_minuten")
        val TTS_VERBRAUCH = longPreferencesKey("tts_verbrauch_zeichen")
        val GOOGLE_API_KEY = stringPreferencesKey("google_api_key")
        val QWEN_API_KEY = stringPreferencesKey("qwen_api_key")
        val QWEN_STIMMEN_ID = stringPreferencesKey("qwen_stimmen_id")
        val GEMINI_TTS_KEY_FREI = stringPreferencesKey("gemini_tts_key_frei")
        val GEMINI_TTS_KEY_BEZAHLT = stringPreferencesKey("gemini_tts_key_bezahlt")
        val GEMINI_TTS_SCHLUESSEL_WAHL = stringPreferencesKey("gemini_tts_schluessel_wahl")
        val GEMINI_TTS_CHIRP_RUECKFALL = booleanPreferencesKey("gemini_tts_chirp_rueckfall")
        val GEMINI_FLASH_STIMME = stringPreferencesKey("gemini_flash_stimme")
        val GEMINI_LITE_STIMME = stringPreferencesKey("gemini_lite_stimme")
        val AZURE_TTS_KEY = stringPreferencesKey("azure_tts_key")
        val AZURE_TTS_REGION = stringPreferencesKey("azure_tts_region")
        val AZURE_STIMME = stringPreferencesKey("azure_stimme")
        val STIMM_FAVORITEN = stringSetPreferencesKey("stimm_favoriten")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val GROQ_MODELL = stringPreferencesKey("groq_modell")
        val WOERTERBUCH_AN = booleanPreferencesKey("woerterbuch_an")
        val WOERTERBUCH = stringPreferencesKey("woerterbuch")
        val LOESLICHKEIT_FETT_ZUERST = booleanPreferencesKey("loeslichkeit_fett_zuerst")
        val CODEX_MODELL = stringPreferencesKey("codex_modell")
        val CODEX_DENKSTUFE = stringPreferencesKey("codex_denkstufe")
        val BEWEGUNG_REDUZIERT = booleanPreferencesKey("bewegung_reduziert")
        val FINGERABDRUCK_AKTIV = booleanPreferencesKey("fingerabdruck_aktiv")

        val CODEX_MODELLE = CodexModel.entries.map { it.apiId }.toSet()
        val DENKSTUFEN = ReasoningEffort.entries.map { it.apiValue }.toSet()
    }
}
