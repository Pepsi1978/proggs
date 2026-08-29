package de.frank.claudekompass.tts

import android.content.Context
import de.frank.claudekompass.data.EinstellungenStore
import de.frank.claudekompass.data.model.TtsAnbieter
import de.frank.claudekompass.observability.KompassLog
import de.frank.claudekompass.observability.probe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Was der Lautsprecher-Knopf gerade anzeigt. */
enum class VorleseStufe { AUS, LAEDT, SPRICHT }

/**
 * Sichtbarer Zustand des Vorlesens.
 *
 * [quelleId] sagt, WELCHER Knopf leuchtet. Ohne diese Angabe würden bei mehreren Lautsprechern
 * auf einem Bildschirm alle gleichzeitig „spricht" anzeigen.
 */
data class VorleseZustand(
    val stufe: VorleseStufe = VorleseStufe.AUS,
    val quelleId: String = "",
    val absatzNummer: Int = 0,
    val absatzAnzahl: Int = 0,
    val fehler: String = "",
)

/**
 * Die Absatz-Pipeline (Referenz, Baustein D, Punkt 4.2).
 *
 * Der Ablauf in einem Satz: Während Absatz *n* gesprochen wird, sind *n+1* und *n+2* schon in
 * Arbeit — deshalb kommt der erste Ton nach Bruchteilen einer Sekunde und nicht erst, wenn der
 * ganze Text synthetisiert wurde.
 *
 * Vier Dinge, die dabei leicht schiefgehen und hier bewusst geregelt sind:
 *  - **Reihenfolge:** Abgespielt wird streng nacheinander. Das Vorausschauen betrifft nur die
 *    Synthese, nie die Wiedergabe.
 *  - **Gleichzeitigkeit:** Ein Zähler begrenzt die parallelen Anfragen. Ohne ihn laufen alle
 *    Absätze gleichzeitig gegen das Anfragelimit des Dienstes.
 *  - **Fehlerschwere:** Ein abgelehnter Schlüssel hält die ganze Reihe an und wird im Klartext
 *    gemeldet. Ein einzelner abgelehnter Absatz wird übersprungen, damit der Rest weiterläuft.
 *  - **Abbruch:** Jeder Lauf hat eine Nummer. Kommt ein neuer Auftrag, gilt die alte Nummer
 *    nicht mehr, und alle noch laufenden Synthesen des alten Laufs werden abgebrochen.
 */
class VorleseManager(
    context: Context,
    private val einstellungen: EinstellungenStore,
) {

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val abspieler = AudioAbspieler(context)
    private val gleichzeitig = Semaphore(MAX_GLEICHZEITIG)

    private val google = GoogleTtsSynthesizer { einstellungen.googleSchluessel }
    private val edge = EdgeTtsSynthesizer()
    private val qwen = QwenTtsSynthesizer { einstellungen.alibabaSchluessel }

    private val _zustand = MutableStateFlow(VorleseZustand())
    val zustand: StateFlow<VorleseZustand> = _zustand.asStateFlow()

    private var laufNummer = 0
    private var laufJob: Job? = null

    init {
        abspieler.beiFokusVerlust = {
            KompassLog.info("VorleseManager", "fokus", "Vorlesen wegen Audiofokus angehalten")
            stoppe()
        }
    }

    /**
     * Liest [text] vor. Ein erneuter Aufruf mit derselben [quelleId] hält an — genau das
     * erwartet man von einem Lautsprecher-Knopf.
     */
    fun schalteUm(quelleId: String, text: String) {
        if (_zustand.value.quelleId == quelleId && _zustand.value.stufe != VorleseStufe.AUS) {
            stoppe()
            return
        }
        lies(quelleId, text)
    }

    fun lies(quelleId: String, text: String) {
        stoppe()
        val absaetze = TextSaeuberer.teileInAbsaetze(text)
        if (absaetze.isEmpty()) {
            KompassLog.info("VorleseManager", "lies", "Nichts zu lesen", mapOf("quelle" to quelleId))
            return
        }
        probe(
            absaetze.all { it.length <= TextSaeuberer.MAX_ZEICHEN },
            "Ein Absatz überschreitet die Dienstgrenze und würde abgelehnt",
            "VorleseManager",
            "lies",
            mapOf("laengster" to absaetze.maxOf { it.length }),
        )

        laufNummer += 1
        val meinLauf = laufNummer
        _zustand.value = VorleseZustand(VorleseStufe.LAEDT, quelleId, 0, absaetze.size)
        KompassLog.info(
            "VorleseManager",
            "lies",
            "Vorlesen gestartet",
            mapOf("quelle" to quelleId, "absaetze" to absaetze.size, "anbieter" to einstellungen.ttsAnbieter.id),
        )

        laufJob = bereich.launch {
            val fokus = abspieler.fordereFokusAn()
            if (!fokus) {
                KompassLog.warn("VorleseManager", "lies", "Kein Audiofokus erhalten, lese trotzdem")
            }
            try {
                spieleReihe(meinLauf, quelleId, absaetze)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                if (meinLauf == laufNummer) {
                    KompassLog.error("VorleseManager", "lies", "Vorlesen abgebrochen", mapOf("grund" to fehler.message))
                    _zustand.value = VorleseZustand(
                        stufe = VorleseStufe.AUS,
                        quelleId = quelleId,
                        fehler = fehler.message ?: "Das Vorlesen ist fehlgeschlagen.",
                    )
                }
            } finally {
                abspieler.gibFokusFrei()
                if (meinLauf == laufNummer && _zustand.value.fehler.isEmpty()) {
                    _zustand.value = VorleseZustand()
                }
            }
        }
    }

    private suspend fun spieleReihe(meinLauf: Int, quelleId: String, absaetze: List<String>) = coroutineScope {
        val anbieter = einstellungen.ttsAnbieter
        val synthesizer = waehleSynthesizer(anbieter)
        val stimme = waehleStimme(anbieter)
        val tempo = einstellungen.sprechtempo
        // Anbieter, die das Tempo selbst können, bekommen es mitgegeben; beim Abspielen bleibt
        // es dann bei 1.0, sonst würde die Geschwindigkeit zweimal angewendet.
        val tempoImDienst = anbieter != TtsAnbieter.QWEN
        val tempoBeimAbspielen = if (tempoImDienst) 1f else tempo

        val offen = mutableMapOf<Int, Deferred<SyntheseErgebnis?>>()

        fun beauftrage(index: Int) {
            if (index !in absaetze.indices || offen.containsKey(index)) return
            offen[index] = async(Dispatchers.IO) {
                gleichzeitig.withPermit {
                    synthetisiereMitGeduld(
                        synthesizer = synthesizer,
                        text = absaetze[index],
                        stimme = stimme,
                        tempo = if (tempoImDienst) tempo else 1f,
                        index = index,
                    )
                }
            }
        }

        fun brichOffeneAb() {
            offen.values.forEach { it.cancel() }
            offen.clear()
        }

        // Vorausschau: den laufenden plus die nächsten beiden Absätze anstossen.
        repeat(minOf(VORAUSSCHAU + 1, absaetze.size)) { beauftrage(it) }

        for (index in absaetze.indices) {
            if (meinLauf != laufNummer) {
                brichOffeneAb()
                return@coroutineScope
            }

            val ergebnis = try {
                offen.remove(index)?.await()
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: TtsFehler) {
                if (fehler.sitzungsweit) {
                    // Betrifft jeden weiteren Absatz — anhalten und den echten Grund zeigen,
                    // statt den ganzen Text still zu überspringen.
                    brichOffeneAb()
                    throw fehler
                }
                KompassLog.warn(
                    "VorleseManager",
                    "spieleReihe",
                    "Absatz übersprungen",
                    mapOf("index" to index, "grund" to fehler.message),
                )
                null
            }

            beauftrage(index + VORAUSSCHAU + 1)
            if (meinLauf != laufNummer) {
                brichOffeneAb()
                return@coroutineScope
            }
            if (ergebnis == null) continue

            _zustand.value = VorleseZustand(VorleseStufe.SPRICHT, quelleId, index + 1, absaetze.size)
            abspieler.spieleUndWarte(ergebnis.audio, ergebnis.endung, tempoBeimAbspielen)

            // Hörbarer Atem zwischen zwei Absätzen — beim letzten entfällt er.
            if (index < absaetze.lastIndex && meinLauf == laufNummer) {
                delay(ABSATZ_PAUSE_MS)
            }
        }
        KompassLog.info("VorleseManager", "spieleReihe", "Vorlesen beendet", mapOf("absaetze" to absaetze.size))
    }

    /**
     * Holt einen Absatz, mit Geduld bei vorübergehenden Störungen.
     *
     * Bei einem Anfragelimit wird wachsend gewartet statt sofort aufzugeben. Kommt trotzdem
     * nichts, wird der Absatz halbiert und erneut versucht — manche Ablehnungen hängen an der
     * Länge, nicht am Inhalt.
     */
    private suspend fun synthetisiereMitGeduld(
        synthesizer: TtsSynthesizer,
        text: String,
        stimme: String,
        tempo: Float,
        index: Int,
    ): SyntheseErgebnis? {
        var versuch = 0
        while (true) {
            try {
                return withTimeout(SYNTHESE_ZEITGRENZE_MS) {
                    synthesizer.synthetisiere(text, stimme, tempo)
                }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: TtsFehler) {
                if (!fehler.wiederholbar || versuch >= WIEDERHOLUNGEN.size) {
                    if (fehler.art == TtsFehlerArt.INHALT && text.length > TEIL_MINDESTLAENGE) {
                        return teileUndVersucheErneut(synthesizer, text, stimme, tempo, index)
                    }
                    throw fehler
                }
                val wartezeit = maxOf(WIEDERHOLUNGEN[versuch], fehler.wartezeitMs)
                KompassLog.warn(
                    "VorleseManager",
                    "synthetisiereMitGeduld",
                    "Vorübergehender Fehler, warte und versuche erneut",
                    mapOf("index" to index, "versuch" to versuch, "wartetMs" to wartezeit, "grund" to fehler.message),
                )
                delay(wartezeit)
                versuch += 1
            } catch (fehler: Exception) {
                if (versuch >= WIEDERHOLUNGEN.size) {
                    throw TtsFehler(TtsFehlerArt.NETZ, "Der Absatz konnte nicht geholt werden: ${fehler.message}", ursache = fehler)
                }
                delay(WIEDERHOLUNGEN[versuch])
                versuch += 1
            }
        }
    }

    /** Halbiert einen abgelehnten Absatz an einer Satzgrenze und fügt die Töne zusammen. */
    private suspend fun teileUndVersucheErneut(
        synthesizer: TtsSynthesizer,
        text: String,
        stimme: String,
        tempo: Float,
        index: Int,
    ): SyntheseErgebnis? {
        val teile = TextSaeuberer.teileInAbsaetze(text, maxZeichen = text.length / 2 + 1)
        if (teile.size < 2) return null
        KompassLog.info(
            "VorleseManager",
            "teileUndVersucheErneut",
            "Absatz halbiert und erneut versucht",
            mapOf("index" to index, "teile" to teile.size),
        )
        val stuecke = teile.mapNotNull { teil ->
            runCatching { synthesizer.synthetisiere(teil, stimme, tempo) }.getOrNull()
        }
        if (stuecke.isEmpty()) return null
        // MP3-Rahmen lassen sich hintereinanderhängen; bei WAV ginge das nicht, deshalb wird
        // in dem Fall nur das erste Stück genommen — lieber ein halber Absatz als gar keiner.
        return if (stuecke.first().endung == "mp3") {
            SyntheseErgebnis(
                audio = stuecke.fold(ByteArray(0)) { gesammelt, teil -> gesammelt + teil.audio },
                endung = "mp3",
            )
        } else {
            stuecke.first()
        }
    }

    private fun waehleSynthesizer(anbieter: TtsAnbieter): TtsSynthesizer = when (anbieter) {
        TtsAnbieter.GOOGLE -> google
        TtsAnbieter.EDGE -> edge
        TtsAnbieter.QWEN -> qwen
    }

    private fun waehleStimme(anbieter: TtsAnbieter): String = when (anbieter) {
        TtsAnbieter.GOOGLE -> einstellungen.googleStimme
        TtsAnbieter.EDGE -> einstellungen.edgeStimme
        TtsAnbieter.QWEN -> einstellungen.qwenStimmeId
    }

    /** Einzelnen Satz sofort sprechen — für den Probe-Knopf in den Einstellungen. */
    fun probiere(anbieter: TtsAnbieter, stimme: String, beiFehler: (String) -> Unit) {
        stoppe()
        laufNummer += 1
        val meinLauf = laufNummer
        _zustand.value = VorleseZustand(VorleseStufe.LAEDT, PROBE_ID, 0, 1)
        laufJob = bereich.launch {
            try {
                abspieler.fordereFokusAn()
                val ton = waehleSynthesizer(anbieter)
                    .synthetisiere(TtsCatalog.PROBETEXT, stimme, einstellungen.sprechtempo)
                if (meinLauf != laufNummer) return@launch
                _zustand.value = VorleseZustand(VorleseStufe.SPRICHT, PROBE_ID, 1, 1)
                abspieler.spieleUndWarte(ton.audio, ton.endung, 1f)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                beiFehler(fehler.message ?: "Die Probe hat nicht geklappt.")
            } finally {
                abspieler.gibFokusFrei()
                if (meinLauf == laufNummer) _zustand.value = VorleseZustand()
            }
        }
    }

    fun stoppe() {
        laufNummer += 1
        laufJob?.cancel()
        laufJob = null
        abspieler.stoppe()
        _zustand.value = VorleseZustand()
    }

    fun loescheFehler() {
        _zustand.value = _zustand.value.copy(fehler = "")
    }

    fun beende() {
        stoppe()
        bereich.cancel()
        google.beende()
        edge.beende()
        qwen.beende()
    }

    companion object {
        /** Wie viele Absätze im Voraus synthetisiert werden (Referenz: 2). */
        const val VORAUSSCHAU = 2

        /** Hörbarer Atem zwischen zwei Absätzen (Referenz: rund eine Sekunde). */
        const val ABSATZ_PAUSE_MS = 1_000L

        /**
         * Nur so viele Anfragen gleichzeitig. Eine genügt: Das Abspielen eines Absatzes dauert
         * länger als die Synthese des nächsten — die Vorausschau bleibt der Wiedergabe also
         * ohnehin voraus, und das Anfragelimit wird deutlich seltener getroffen.
         */
        const val MAX_GLEICHZEITIG = 1

        /**
         * Hält eine hängende Verbindung auf. Ohne diese Grenze wartet die Reihe unbegrenzt auf
         * einen Absatz, der nie kommt — beim WebSocket von Edge ist das ein realer Fall.
         */
        const val SYNTHESE_ZEITGRENZE_MS = 45_000L

        const val TEIL_MINDESTLAENGE = 260
        const val PROBE_ID = "__probe__"
        val WIEDERHOLUNGEN = longArrayOf(1_000L, 3_000L, 7_000L)
    }
}
