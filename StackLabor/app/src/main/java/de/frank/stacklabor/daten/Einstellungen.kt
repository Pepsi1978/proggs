package de.frank.stacklabor.daten

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.speicher by preferencesDataStore(name = "stacklabor_einstellungen")

/**
 * Alle Einstellungen der App (F-18).
 *
 * Die Vorgaben stammen aus `01-FUNKTIONS-SPEC.md` §3 Datenmodell:
 * Erscheinung **hell**, Anbieter **Edge** mit Seraphina, Modell **Terra**, Denkstufe **hoch**,
 * Dosis-Variante **A** (Frei).
 */
class Einstellungen(private val ctx: Context) {

    private object Schluessel {
        val erscheinung = stringPreferencesKey("erscheinung")
        val bewegungReduziert = booleanPreferencesKey("bewegung_reduziert")
        val ttsAnbieter = stringPreferencesKey("tts_anbieter")
        val ttsStimme = stringPreferencesKey("tts_stimme")
        val ttsTempo = floatPreferencesKey("tts_tempo")
        val ttsPauseMs = intPreferencesKey("tts_pause_ms")
        val ttsAbschaltungMin = intPreferencesKey("tts_abschaltung_min")
        val codexModell = stringPreferencesKey("codex_modell")
        val codexDenkstufe = stringPreferencesKey("codex_denkstufe")
        val dosisVariante = stringPreferencesKey("dosis_variante")
        val sortieransicht = stringPreferencesKey("sortieransicht")
        val startbestandEingelesen = booleanPreferencesKey("startbestand_eingelesen")
    }

    /** HELL ist der Standard — so im Grilling entschieden (Entscheidung 9). */
    val dunkel: Flow<Boolean> =
        ctx.speicher.data.map { it[Schluessel.erscheinung] == "DUNKEL" }

    val bewegungReduziert: Flow<Boolean> =
        ctx.speicher.data.map { it[Schluessel.bewegungReduziert] ?: false }

    val ttsAnbieter: Flow<String> =
        ctx.speicher.data.map { it[Schluessel.ttsAnbieter] ?: "EDGE" }

    val ttsStimme: Flow<String> =
        ctx.speicher.data.map { it[Schluessel.ttsStimme] ?: "de-DE-SeraphinaMultilingualNeural" }

    val ttsTempo: Flow<Float> =
        ctx.speicher.data.map { it[Schluessel.ttsTempo] ?: 1.0f }

    val ttsPauseMs: Flow<Int> =
        ctx.speicher.data.map { it[Schluessel.ttsPauseMs] ?: 400 }

    /** 0 = keine automatische Abschaltung */
    val ttsAbschaltungMin: Flow<Int> =
        ctx.speicher.data.map { it[Schluessel.ttsAbschaltungMin] ?: 0 }

    val codexModell: Flow<String> =
        ctx.speicher.data.map { it[Schluessel.codexModell] ?: "gpt-5.6-terra" }

    val codexDenkstufe: Flow<String> =
        ctx.speicher.data.map { it[Schluessel.codexDenkstufe] ?: "high" }

    /** „A" = Frei, „B" = Dienst (F-27) */
    val dosisVariante: Flow<String> =
        ctx.speicher.data.map { it[Schluessel.dosisVariante] ?: "A" }

    /** „LOESLICHKEIT" (Vorgabe) oder „EINNAHME" (F-06) */
    val sortieransicht: Flow<String> =
        ctx.speicher.data.map { it[Schluessel.sortieransicht] ?: "LOESLICHKEIT" }

    val startbestandEingelesen: Flow<Boolean> =
        ctx.speicher.data.map { it[Schluessel.startbestandEingelesen] ?: false }

    suspend fun setzeDunkel(wert: Boolean) = ctx.speicher.edit {
        it[Schluessel.erscheinung] = if (wert) "DUNKEL" else "HELL"
    }

    suspend fun setzeBewegungReduziert(wert: Boolean) = ctx.speicher.edit {
        it[Schluessel.bewegungReduziert] = wert
    }

    suspend fun setzeTtsAnbieter(wert: String) = ctx.speicher.edit { it[Schluessel.ttsAnbieter] = wert }
    suspend fun setzeTtsStimme(wert: String) = ctx.speicher.edit { it[Schluessel.ttsStimme] = wert }
    suspend fun setzeTtsTempo(wert: Float) = ctx.speicher.edit { it[Schluessel.ttsTempo] = wert }
    suspend fun setzeTtsPause(ms: Int) = ctx.speicher.edit { it[Schluessel.ttsPauseMs] = ms }
    suspend fun setzeTtsAbschaltung(min: Int) = ctx.speicher.edit { it[Schluessel.ttsAbschaltungMin] = min }
    suspend fun setzeCodexModell(wert: String) = ctx.speicher.edit { it[Schluessel.codexModell] = wert }
    suspend fun setzeCodexDenkstufe(wert: String) = ctx.speicher.edit { it[Schluessel.codexDenkstufe] = wert }
    suspend fun setzeDosisVariante(wert: String) = ctx.speicher.edit { it[Schluessel.dosisVariante] = wert }
    suspend fun setzeSortieransicht(wert: String) = ctx.speicher.edit { it[Schluessel.sortieransicht] = wert }
    suspend fun merkeStartbestandEingelesen() = ctx.speicher.edit {
        it[Schluessel.startbestandEingelesen] = true
    }
}
