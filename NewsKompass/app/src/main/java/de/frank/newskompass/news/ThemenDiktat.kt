package de.frank.newskompass.news

import de.frank.newskompass.NewsApplication
import de.frank.newskompass.audio.GroqTranscriber
import de.frank.newskompass.audio.MicRecorder
import de.frank.newskompass.observability.KompassLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Ein fertig eingesprochener Thementext, den die Themenkarte noch übernehmen muss. */
data class Diktat(val themaId: String, val vorher: String, val text: String, val nummer: Int)

data class DiktatZustand(
    /** Das Thema, für das gerade aufgenommen oder verstanden wird. */
    val themaId: String? = null,
    val stufe: SprachStufe = SprachStufe.BEREIT,
    /** Hinweis unter dem Feld [meldungFuer], etwa „Ich habe nichts verstanden.“ */
    val meldung: String = "",
    val meldungFuer: String? = null,
    val fertig: Diktat? = null,
)

/**
 * Einsprechen und KI-Verbessern der Thementexte in den Einstellungen.
 *
 * Die Aufnahme läuft über denselben Weg wie die gesprochene Frage: Groq Whisper mit den vier
 * Schichten gegen Stille-Halluzinationen (Pegelprüfung, Konfidenz je Segment, Abgleich mit dem
 * gemessenen Ton, Floskel-Sperrliste). Der erkannte Text wird hinten an den Thementext angehängt; die
 * Karte merkt sich den alten, damit „Zurück“ ihn wiederherstellt.
 */
class ThemenDiktat(private val app: NewsApplication) {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mikrofon = MicRecorder(app)

    private val _zustand = MutableStateFlow(DiktatZustand())
    val zustand: StateFlow<DiktatZustand> = _zustand.asStateFlow()

    private var generation = 0
    private var nummer = 0

    fun tippe(themaId: String, erlaubnisDa: Boolean, frageErlaubnis: () -> Unit) {
        val jetzt = _zustand.value
        when {
            jetzt.stufe == SprachStufe.NIMMT_AUF && jetzt.themaId == themaId -> stoppeUndUebernimm()
            jetzt.stufe != SprachStufe.BEREIT -> Unit
            app.einstellungen.groqSchluessel.isBlank() ->
                melde(themaId, "Bitte unten bei „Spracheingabe“ einen Groq-Schlüssel hinterlegen.")
            erlaubnisDa -> starte(themaId)
            else -> {
                _zustand.update { it.copy(themaId = themaId) }
                frageErlaubnis()
            }
        }
    }

    fun erlaubnisErhalten(erteilt: Boolean) {
        val themaId = _zustand.value.themaId ?: return
        if (erteilt) starte(themaId) else melde(themaId, "Ohne Mikrofon-Erlaubnis kann ich dich nicht hören.")
    }

    private fun starte(themaId: String) {
        if (_zustand.value.stufe != SprachStufe.BEREIT) return
        app.vorleser.stoppe()
        generation++
        if (mikrofon.start(bereich)) {
            _zustand.update { it.copy(themaId = themaId, stufe = SprachStufe.NIMMT_AUF, meldung = "", meldungFuer = null) }
            KompassLog.info("ThemenDiktat", "starte", "Aufnahme gestartet")
        } else {
            melde(themaId, "Die Aufnahme konnte nicht gestartet werden.")
        }
    }

    private fun stoppeUndUebernimm() {
        val themaId = _zustand.value.themaId ?: return
        _zustand.update { it.copy(stufe = SprachStufe.VERSTEHT) }
        val meineGeneration = generation
        bereich.launch {
            var transkribierer: GroqTranscriber? = null
            try {
                val wav = mikrofon.stop()
                if (meineGeneration != generation) return@launch
                if (wav == null) {
                    melde(themaId, "Ich habe nichts verstanden.")
                    return@launch
                }
                transkribierer = GroqTranscriber(app.einstellungen.groqSchluessel)
                val text = transkribierer.transcribe(wav).trim()
                if (meineGeneration != generation) return@launch
                if (text.isBlank()) {
                    melde(themaId, "Ich habe nichts verstanden.")
                    return@launch
                }
                val themen = app.einstellungen.stand.value.themen
                val vorher = themen.firstOrNull { it.id == themaId }?.text ?: return@launch
                // Das Eingesprochene kommt hinten an den bisherigen Text; „Zurück“ nimmt es wieder weg.
                val neu = if (vorher.isBlank()) text else vorher.trimEnd() + " " + text
                app.einstellungen.setzeThemen(themen.map { if (it.id == themaId) it.copy(text = neu) else it })
                _zustand.update { it.copy(fertig = Diktat(themaId, vorher, neu, ++nummer)) }
                KompassLog.info("ThemenDiktat", "stoppeUndUebernimm", "Thema eingesprochen", mapOf("zeichen" to text.length))
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.warn("ThemenDiktat", "stoppeUndUebernimm", "Diktat gescheitert", mapOf("grund" to fehler.message))
                if (meineGeneration == generation) melde(themaId, fehler.message ?: "Die Spracheingabe ist fehlgeschlagen.")
            } finally {
                transkribierer?.shutdown()
                if (meineGeneration == generation) _zustand.update { it.copy(stufe = SprachStufe.BEREIT, themaId = null) }
            }
        }
    }

    /** Die Karte hat das Diktat übernommen. */
    fun quittiere(diktat: Diktat) = _zustand.update { if (it.fertig == diktat) it.copy(fertig = null) else it }

    /** Verwirft eine laufende Aufnahme, etwa wenn die App in den Hintergrund geht. */
    fun brichAufnahmeAb() {
        if (_zustand.value.stufe != SprachStufe.NIMMT_AUF) return
        generation++
        mikrofon.release()
        _zustand.update { it.copy(stufe = SprachStufe.BEREIT, themaId = null) }
    }

    fun melde(themaId: String, text: String) =
        _zustand.update { it.copy(meldung = text, meldungFuer = themaId) }

    fun loescheMeldung() = _zustand.update { it.copy(meldung = "", meldungFuer = null) }

    /**
     * Bringt einen Thementext in gutes Deutsch, ohne seinen Inhalt zu ändern. [bisher] sind die
     * schon vorgeschlagenen Fassungen — jeder Druck liefert eine neue Formulierung.
     */
    suspend fun verbessere(text: String, bisher: List<String>): String {
        val stand = app.einstellungen.stand.value
        val anweisung = buildString {
            append(
                "Der Text beschreibt ein Nachrichtenthema, über das eine App den Nutzer regelmäßig informiert. " +
                    "Er stammt oft aus einer Spracherkennung und ist deshalb unsauber. Erkenne seine Absicht und gib " +
                    "genau diese Absicht in klarem, sehr gutem Deutsch wieder. Korrigiere Grammatik, Satzbau, " +
                    "Rechtschreibung und falsch erkannte Namen, löse Versprecher, Füllwörter und Wiederholungen auf. " +
                    "Füge NICHTS hinzu, was nicht im Text steht, lass NICHTS weg und schwäche nichts ab. Behalte die " +
                    "Aussageform bei: aus einem Stichwort wird ein Stichwort, aus einer Frage eine Frage. " +
                    "Antworte nur mit der umgeschriebenen Fassung, ohne Vorrede und ohne Anführungszeichen.",
            )
            if (bisher.isNotEmpty()) {
                append("\n\nDiese Fassungen wurden bereits vorgeschlagen. Liefere eine deutlich andere Formulierung bei gleichem Inhalt:\n")
                bisher.forEach { append("- ").append(it).append('\n') }
            }
        }
        val antwort = app.codex.frage(
            anweisung = anweisung,
            eingabe = text.trim(),
            modellId = stand.modellId,
            denktiefe = stand.modelle.firstOrNull { it.id == stand.modellId }?.stufen
                ?.let { stufen -> "low".takeIf { it in stufen } ?: stand.denktiefe } ?: stand.denktiefe,
        )
        return antwort.text.trim().removeSurrounding("\"").removeSurrounding("„", "“").trim()
    }
}
