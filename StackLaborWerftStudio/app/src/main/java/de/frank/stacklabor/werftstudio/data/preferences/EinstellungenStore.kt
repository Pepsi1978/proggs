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
import androidx.datastore.preferences.preferencesDataStore
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.stackLaborEinstellungenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "stacklabor_einstellungen",
)

enum class Erscheinung(val code: Int) { HELL(0), DUNKEL(1) }
enum class TtsAnbieter(val code: Int) { EDGE(0), GOOGLE_CLOUD(1), QWEN_CLONE(2) }
enum class Absatzpause(val code: Int) { KURZ(0), MITTEL(1), LANG(2) }

data class AppEinstellungen(
    val erscheinung: Erscheinung = Erscheinung.HELL,
    val dosisVariante: DosisVariante = DosisVariante.FREI,
    val ttsAnbieter: TtsAnbieter = TtsAnbieter.EDGE,
    val ttsStimme: String = "de-DE-SeraphinaMultilingualNeural",
    val ttsTempo: Float = 1.0f,
    val absatzpause: Absatzpause = Absatzpause.KURZ,
    val abschaltzeitMinuten: Int = 30,
    val ttsVerbrauchZeichen: Long = 0,
    /**
     * Schlüssel, die der Benutzer selbst in den Einstellungen hinterlegt. Leer heißt: es gilt
     * der Schlüssel aus dem SK-Ordner, der beim Bauen in die App gelegt wurde.
     */
    val googleApiKey: String = "",
    val qwenApiKey: String = "",
    val qwenStimmenId: String = "",
    val groqApiKey: String = "",
    /** Löslichkeits-Ansicht: fettlösliche zuerst (Standard) oder wasserlösliche zuerst. */
    val loeslichkeitFettZuerst: Boolean = true,
    val codexModell: String = "gpt-5.6-terra",
    val codexDenkstufe: String = "high",
    val bewegungReduziert: Boolean,
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

    /** Leerer Wert bedeutet: wieder den Schlüssel aus dem SK-Ordner verwenden. */
    suspend fun setzeGoogleApiKey(value: String): Unit = schreibe(GOOGLE_API_KEY, value.trim())
    suspend fun setzeQwenApiKey(value: String): Unit = schreibe(QWEN_API_KEY, value.filterNot(Char::isWhitespace))
    suspend fun setzeQwenStimmenId(value: String): Unit = schreibe(QWEN_STIMMEN_ID, value.filterNot(Char::isWhitespace))
    suspend fun setzeGroqApiKey(value: String): Unit = schreibe(GROQ_API_KEY, value.filterNot(Char::isWhitespace))

    suspend fun setzeCodexModell(value: String) {
        require(value in CODEX_MODELLE) { "Unbekanntes Codex-Modell: $value" }
        schreibe(CODEX_MODELL, value)
    }
    suspend fun setzeCodexDenkstufe(value: String) {
        require(value in DENKSTUFEN) { "Unbekannte Denkstufe: $value" }
        schreibe(CODEX_DENKSTUFE, value)
    }
    suspend fun setzeBewegungReduziert(value: Boolean): Unit = schreibe(BEWEGUNG_REDUZIERT, value)

    private fun mappe(preferences: Preferences): AppEinstellungen = AppEinstellungen(
        erscheinung = enumMitCode(Erscheinung.entries, preferences[ERSCHEINUNG] ?: Erscheinung.HELL.code) { it.code },
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
        groqApiKey = preferences[GROQ_API_KEY].orEmpty(),
        loeslichkeitFettZuerst = preferences[LOESLICHKEIT_FETT_ZUERST] ?: true,
        codexModell = preferences[CODEX_MODELL] ?: "gpt-5.6-terra",
        codexDenkstufe = preferences[CODEX_DENKSTUFE] ?: "high",
        bewegungReduziert = preferences[BEWEGUNG_REDUZIERT] ?: systemBewegungReduziert,
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
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val LOESLICHKEIT_FETT_ZUERST = booleanPreferencesKey("loeslichkeit_fett_zuerst")
        val CODEX_MODELL = stringPreferencesKey("codex_modell")
        val CODEX_DENKSTUFE = stringPreferencesKey("codex_denkstufe")
        val BEWEGUNG_REDUZIERT = booleanPreferencesKey("bewegung_reduziert")

        val CODEX_MODELLE = setOf("gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna")
        val DENKSTUFEN = setOf("low", "medium", "high", "xhigh", "max")
    }
}
