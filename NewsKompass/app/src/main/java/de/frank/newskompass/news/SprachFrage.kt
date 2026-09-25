package de.frank.newskompass.news

import de.frank.newskompass.NewsApplication
import de.frank.newskompass.audio.GroqTranscriber
import de.frank.newskompass.audio.MicRecorder
import de.frank.newskompass.observability.KompassLog
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Was der schwebende Mikrofon-Knopf gerade anzeigt. */
enum class SprachStufe { BEREIT, NIMMT_AUF, VERSTEHT }

data class SprachZustand(
    val stufe: SprachStufe = SprachStufe.BEREIT,
    /** Hinweis unter dem Knopf, etwa „Ich habe nichts verstanden.“ */
    val meldung: String = "",
    /** Die Meldung lässt sich nur in den Einstellungen beheben (Schlüssel, Anmeldung). */
    val zuEinstellungen: Boolean = false,
    /** Fragen dieser Sitzung, deren Antwort nach dem Eintreffen vorgelesen wird. */
    val vorlesen: Set<UUID> = emptySet(),
)

/**
 * Der Mikrofon-Knopf: einmal tippen und sprechen, noch einmal tippen zum Senden.
 *
 * Die Aufnahme geht an Groq Whisper Large V3 Turbo und durch dieselben vier Schichten gegen
 * Stille-Halluzinationen wie in Perfect Moment (Pegelprüfung vor dem Hochladen, Konfidenz je
 * Segment, Abgleich der Segmente mit dem gemessenen Ton, Floskel-Sperrliste). Was übrig bleibt,
 * wird als Frage an [Zeitplan.starteFrage] übergeben.
 */
class SprachFrage(private val app: NewsApplication) {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mikrofon = MicRecorder(app)

    private val _zustand = MutableStateFlow(SprachZustand())
    val zustand: StateFlow<SprachZustand> = _zustand.asStateFlow()

    /** Zählt jeden Start und Abbruch — eine veraltete Transkription darf nichts mehr auslösen. */
    private var generation = 0
    private var laufenderTranskribierer: GroqTranscriber? = null

    fun tippe(erlaubnisDa: Boolean, frageErlaubnis: () -> Unit) {
        when (_zustand.value.stufe) {
            SprachStufe.BEREIT -> {
                if (app.einstellungen.groqSchluessel.isBlank()) {
                    melde("Bitte zuerst in den Einstellungen einen Groq-Schlüssel hinterlegen.", zuEinstellungen = true)
                    return
                }
                if (!app.codex.istVerbunden) {
                    melde("Bitte zuerst in den Einstellungen bei Codex anmelden.", zuEinstellungen = true)
                    return
                }
                if (erlaubnisDa) starte() else frageErlaubnis()
            }
            SprachStufe.NIMMT_AUF -> stoppeUndSende()
            SprachStufe.VERSTEHT -> Unit
        }
    }

    fun erlaubnisErhalten(erteilt: Boolean) {
        if (erteilt) starte() else melde("Ohne Mikrofon-Erlaubnis kann ich deine Frage nicht hören.")
    }

    private fun starte() {
        if (_zustand.value.stufe != SprachStufe.BEREIT) return
        // Sonst nimmt das Mikrofon die eigene Vorlesestimme mit auf.
        app.vorleser.stoppe()
        generation++
        if (mikrofon.start(bereich)) {
            _zustand.update { it.copy(stufe = SprachStufe.NIMMT_AUF, meldung = "", zuEinstellungen = false) }
            KompassLog.info("SprachFrage", "starte", "Aufnahme gestartet")
        } else {
            melde("Die Aufnahme konnte nicht gestartet werden.")
        }
    }

    private fun stoppeUndSende() {
        _zustand.update { it.copy(stufe = SprachStufe.VERSTEHT) }
        val meineGeneration = generation
        bereich.launch {
            var transkribierer: GroqTranscriber? = null
            try {
                val wav = mikrofon.stop()
                if (meineGeneration != generation) return@launch
                if (wav == null) {
                    melde("Ich habe nichts verstanden.")
                    return@launch
                }
                transkribierer = GroqTranscriber(app.einstellungen.groqSchluessel)
                laufenderTranskribierer = transkribierer
                val frage = transkribierer.transcribe(wav).trim()
                if (meineGeneration != generation) return@launch
                if (frage.isBlank()) {
                    melde("Ich habe nichts verstanden.")
                    return@launch
                }
                KompassLog.info("SprachFrage", "stoppeUndSende", "Frage erkannt", mapOf("zeichen" to frage.length))
                val id = Zeitplan.starteFrage(app, frage)
                _zustand.update { it.copy(vorlesen = it.vorlesen + id) }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.warn("SprachFrage", "stoppeUndSende", "Spracheingabe gescheitert", mapOf("grund" to fehler.message))
                if (meineGeneration == generation) melde(fehler.message ?: "Die Spracheingabe ist fehlgeschlagen.")
            } finally {
                transkribierer?.shutdown()
                if (laufenderTranskribierer === transkribierer) laufenderTranskribierer = null
                if (meineGeneration == generation) _zustand.update { it.copy(stufe = SprachStufe.BEREIT) }
            }
        }
    }

    /**
     * Verwirft eine laufende Aufnahme — etwa wenn die App in den Hintergrund geht, wo Android
     * das Mikrofon ohnehin stumm schaltet. Eine schon laufende Transkription darf zu Ende laufen.
     */
    fun brichAufnahmeAb() {
        if (_zustand.value.stufe != SprachStufe.NIMMT_AUF) return
        generation++
        mikrofon.release()
        _zustand.update { it.copy(stufe = SprachStufe.BEREIT) }
        KompassLog.info("SprachFrage", "brichAufnahmeAb", "Aufnahme verworfen")
    }

    /** Die Antwort auf [id] ist vorgelesen oder wird nicht mehr gebraucht. */
    fun erledigt(id: UUID) = _zustand.update { it.copy(vorlesen = it.vorlesen - id) }

    fun loescheMeldung() = _zustand.update { it.copy(meldung = "", zuEinstellungen = false) }

    private fun melde(text: String, zuEinstellungen: Boolean = false) =
        _zustand.update { it.copy(meldung = text, zuEinstellungen = zuEinstellungen) }
}
