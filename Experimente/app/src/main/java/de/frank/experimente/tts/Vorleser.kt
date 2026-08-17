package de.frank.experimente.tts

import android.content.Context
import de.frank.experimente.data.settings.Einstellungen
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * F-12 — Vorlesen über den in B-08 gewählten Weg.
 *
 * Bündelt die vier Wiedergabe-Wege (Google Chirp 3 HD, eigene Stimme über DashScope,
 * Microsoft Edge, Stimme des Geräts) hinter einem Knopf. Nur **eine** Wiedergabe
 * gleichzeitig: ein neuer Vorlesevorgang bricht den laufenden ab.
 *
 * Die Stimme des Geräts ist zugleich die Rückfallebene. Drei der vier Wege brauchen Netz,
 * zwei zusätzlich einen Schlüssel — fehlt eines davon, blieb bisher jeder Lautsprecher stumm
 * und meldete etwas Technisches. Vorlesen darf daran nicht scheitern.
 *
 * **Vorgelesen wird Absatz für Absatz.** Ein langer Eintrag — eine ausführliche Auswertung
 * etwa — ging vorher am Stück an die Sprach-KI, und die fing dann gar nicht erst an: zu viel
 * Text auf einmal. Jetzt wird der Text in Absätze zerlegt (`Absaetze`), jeder einzeln
 * synthetisiert und einzeln gespielt, und während einer läuft, entstehen im Hintergrund
 * schon die nächsten — immer nur eine Handvoll voraus, damit die Sprach-KI nicht wieder
 * überfordert wird. Derselbe Weg wie in der Cortex-App.
 */
class Vorleser(context: Context, private val einstellungen: Einstellungen) {

    private val google = GoogleCloudTtsPlayer(context)
    private val edge = EdgeTtsPlayer(context)
    private val qwen = QwenTtsPlayer(context)

    /**
     * Die Rückfallebene. Sie wird erst beim ersten Bedarf eingerichtet — Androids
     * `TextToSpeech` startet beim Erzeugen einen Dienst, und das soll nicht jede App-Sitzung
     * kosten, in der niemand vorliest.
     */
    private var geraetEingerichtet = false
    private val geraet: GeraetTtsPlayer by lazy {
        geraetEingerichtet = true
        GeraetTtsPlayer(context)
    }

    /** Spielt die fertigen Absätze — einen nach dem anderen, mit Anhalten und Fortsetzen. */
    private val abspieler = Absatzabspieler()

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Der laufende Vorlesevorgang. Ein neuer bricht ihn ab — es spricht immer nur einer. */
    private var vorlesejob: Job? = null

    private val _laeuft = MutableStateFlow(false)
    val laeuft: StateFlow<Boolean> = _laeuft

    private val _pausiert = MutableStateFlow(false)
    val pausiert: StateFlow<Boolean> = _pausiert

    /**
     * Welcher Weg gerade spricht — damit Anhalten und Fortsetzen den richtigen trifft.
     *
     * Als **Anbieter**, nicht als Zeichenkette: `lies()` löst eine unbekannte Kennung über
     * den Katalog auf, `umschalten()` verglich sie danach aber wieder mit Literalen und fiel
     * in seinen `else`-Zweig. Pause und Fortsetzen trafen dann Edge, während in Wirklichkeit
     * Google sprach — der Druck blieb wirkungslos.
     */
    private var derzeit: TtsProvider? = null

    /**
     * F-12 — vorlesen.
     *
     * Der eingestellte Weg kommt zuerst. Kommt er nicht durch — kein Netz, kein Schlüssel,
     * abgelehnter Aufruf —, übernimmt die **Stimme des Geräts**, und Frank hört einen Satz
     * dazu. Vorher endete jeder Fehlschlag in Stille und einer technischen Meldung.
     */
    fun lies(text: String, beiFehler: (String) -> Unit) {
        if (text.isBlank()) return
        halteAn()
        val anbieter = einstellungen.ttsAnbieter
        val tempo = einstellungen.sprechtempo
        val absaetze = Absaetze.teile(text)
        if (absaetze.isEmpty()) return

        // Verteilt wird über den Katalog, nicht über lose Zeichenketten. Vorher stand hier
        // ein `when` mit Literalen und einem `else`, das alles Unbekannte an Edge gab —
        // eine abweichende Kennung („qwen" statt „qwen_clone") landete damit **stumm** bei
        // der falschen Stimme, ohne jede Fehlermeldung. Jetzt ist jeder Anbieter benannt.
        val weg = TtsProvider.entries.firstOrNull { it.id == anbieter } ?: TtsCatalog.DEFAULT_PROVIDER
        derzeit = weg

        // Sonde: welcher Weg wirklich spricht. Genau das war von aussen nicht zu sehen, als
        // „Meine Stimme" stumm bei Edge landete — man hoerte nur, dass etwas nicht stimmt.
        android.util.Log.i(
            "Vorleser",
            "spricht ueber ${weg.id} (gespeichert: $anbieter, Stimme: ${stimmeVon(weg)}, " +
                "${absaetze.size} Absätze, ${text.length} Zeichen)",
        )

        // Fehlt der Schlüssel, ist der Weg schon vor dem ersten Absatz zu Ende — dann sagt
        // die App das und liest mit der Stimme des Geräts weiter, statt zu schweigen.
        val fehlgrund = when (weg) {
            TtsProvider.GOOGLE_CLOUD -> if (einstellungen.googleTtsSchluessel.isBlank()) {
                "Für die Google-Stimme fehlt der Schlüssel (Einstellungen → Zugänge). " +
                    "Ich lese mit der Stimme des Geräts vor."
            } else null

            TtsProvider.QWEN_CLONE ->
                if (einstellungen.qwenSchluessel.isBlank() || einstellungen.stimmeQwen.isBlank()) {
                    "Für deine eigene Stimme fehlt noch der Schlüssel oder die Aufnahme. " +
                        "Ich lese mit der Stimme des Geräts vor."
                } else null

            else -> null
        }

        // Wie ein **einzelner** Absatz zu Ton wird. Mehr braucht die Pipeline nicht zu wissen.
        val synthese: (suspend (String) -> File)? = when {
            fehlgrund != null -> null
            weg == TtsProvider.GOOGLE_CLOUD -> { absatz ->
                google.synthetisiere(
                    text = absatz,
                    apiKey = einstellungen.googleTtsSchluessel,
                    voiceName = einstellungen.stimmeGoogle,
                    speechRate = tempo,
                )
            }

            weg == TtsProvider.QWEN_CLONE -> { absatz ->
                qwen.synthetisiere(
                    text = absatz,
                    rawApiKey = einstellungen.qwenSchluessel,
                    rawVoiceId = einstellungen.stimmeQwen,
                )
            }

            weg == TtsProvider.EDGE -> { absatz ->
                edge.synthetisiere(
                    text = absatz,
                    voice = einstellungen.stimmeEdge,
                    speechRate = tempo,
                )
            }

            else -> null
        }

        vorlesejob = bereich.launch {
            _laeuft.value = true
            _pausiert.value = false
            var geschafft = 0
            try {
                if (fehlgrund != null) {
                    beiFehler(fehlgrund)
                    derzeit = TtsProvider.GERAET
                    liesMitGeraet(absaetze, tempo)
                } else if (synthese != null) {
                    geschafft = liesInAbsaetzen(absaetze, synthese)
                } else {
                    liesMitGeraet(absaetze, tempo)
                }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                if (weg == TtsProvider.GERAET) {
                    beiFehler(verstaendlich(fehler, TtsProvider.GERAET))
                } else if (geschafft == 0) {
                    // Noch kein Ton: der ganze Text geht an die Rückfallebene.
                    android.util.Log.w("Vorleser", "${weg.id} fehlgeschlagen: ${fehler.message}", fehler)
                    beiFehler(rueckfallHinweis(weg, fehler))
                    derzeit = TtsProvider.GERAET
                    try {
                        liesMitGeraet(absaetze, tempo)
                    } catch (letzter: Exception) {
                        beiFehler(verstaendlich(letzter, TtsProvider.GERAET))
                    }
                } else {
                    // Mittendrin abgerissen — dann steht schon Gesprochenes im Raum und ein
                    // Neuanfang von vorn wäre schlimmer als der ehrliche Satz.
                    android.util.Log.w("Vorleser", "Abbruch nach $geschafft Absätzen", fehler)
                    beiFehler(verstaendlich(fehler, weg))
                }
            } finally {
                _laeuft.value = false
                _pausiert.value = false
                derzeit = null
            }
        }
    }

    /**
     * **Die Absatz-Pipeline.** Der laufende Absatz wird gespielt, während die nächsten
     * [Absaetze.VORAUS] schon synthetisiert werden — nie mehr, sonst bekäme die Sprach-KI
     * wieder den ganzen Text auf einmal.
     *
     * @return wie viele Absätze wirklich gesprochen wurden
     */
    private suspend fun liesInAbsaetzen(
        absaetze: List<String>,
        synthese: suspend (String) -> File,
    ): Int = coroutineScope {
        val offen = mutableMapOf<Int, Deferred<File>>()

        fun reihEin(nr: Int) {
            if (nr in absaetze.indices && offen[nr] == null) {
                offen[nr] = async(Dispatchers.IO) { synthese(absaetze[nr]) }
            }
        }

        repeat(minOf(Absaetze.VORAUS + 1, absaetze.size)) { reihEin(it) }
        var gesprochen = 0
        try {
            absaetze.indices.forEach { nr ->
                val datei = offen.remove(nr)?.await() ?: return@forEach
                // Erst nachladen, dann spielen: so läuft die Synthese des übernächsten
                // Absatzes über die volle Spieldauer des laufenden.
                reihEin(nr + Absaetze.VORAUS + 1)
                try {
                    abspieler.spieleUndWarte(datei)
                    gesprochen++
                } finally {
                    datei.delete()
                }
            }
        } finally {
            // Was noch in Arbeit ist, wird nicht mehr gebraucht — sonst wartet
            // `coroutineScope` auf Anfragen, die niemand mehr hört.
            offen.values.forEach { it.cancel() }
            offen.clear()
        }
        gesprochen
    }

    /**
     * Die Rückfallebene, ebenfalls Absatz für Absatz: Androids Sprachausgabe nimmt je
     * Auftrag nur eine begrenzte Textlänge und schneidet darüber hinaus stillschweigend ab.
     */
    private suspend fun liesMitGeraet(absaetze: List<String>, tempo: Float) {
        derzeit = TtsProvider.GERAET
        absaetze.forEach { absatz ->
            val fehler: Exception? = suspendCancellableCoroutine { fortsetzung ->
                geraet.speak(
                    text = absatz,
                    speechRate = tempo,
                    onPlaybackStart = {},
                    onComplete = { if (fortsetzung.isActive) fortsetzung.resume(null) },
                    onError = { f -> if (fortsetzung.isActive) fortsetzung.resume(f) },
                )
                fortsetzung.invokeOnCancellation { geraet.stop() }
            }
            // Der Fehler der Gerätestimme beendet den Vorgang — sie ist der letzte Weg.
            if (fehler != null) throw fehler
        }
    }

    /** Welche Stimme zu einem Weg gehört — für die Sonde im Log. */
    private fun stimmeVon(weg: TtsProvider): String = when (weg) {
        TtsProvider.GOOGLE_CLOUD -> einstellungen.stimmeGoogle
        TtsProvider.QWEN_CLONE -> einstellungen.stimmeQwen
        TtsProvider.EDGE -> einstellungen.stimmeEdge
        TtsProvider.GERAET -> "Stimme des Geräts"
    }

    /**
     * Aus einem Fehler wird ein Satz, den man lesen kann.
     *
     * Vorher reichte der Vorleser `f.message` unverändert durch — auf dem Bildschirm stand
     * dann „Google TTS error 403: {error:{code:403 …". Das sagt niemandem, was zu tun ist.
     */
    private fun verstaendlich(fehler: Exception, weg: TtsProvider): String {
        val roh = fehler.message.orEmpty()
        return when {
            weg == TtsProvider.GERAET ->
                "Auch die Stimme des Geräts antwortet nicht. Sie lässt sich in den " +
                    "Android-Einstellungen unter „Sprachausgabe“ einrichten."
            roh.contains("401") || roh.contains("403") ->
                "Der Schlüssel für diese Stimme wird abgelehnt. Prüf ihn in den Einstellungen."
            roh.contains("429") ->
                "Das Kontingent dieser Stimme ist erschöpft. Versuch es später noch einmal."
            roh.contains("UnknownHost", ignoreCase = true) ||
                roh.contains("timeout", ignoreCase = true) ||
                roh.contains("Unable to resolve host", ignoreCase = true) ->
                "Dafür brauche ich Netz."
            else -> "Das Vorlesen hat nicht geklappt."
        }
    }

    /** Der Satz, der den Wechsel auf die Gerätestimme ankündigt. */
    private fun rueckfallHinweis(weg: TtsProvider, fehler: Exception): String {
        val roh = fehler.message.orEmpty()
        val grund = when {
            roh.contains("401") || roh.contains("403") -> "der Schlüssel wird abgelehnt"
            roh.contains("429") -> "das Kontingent ist erschöpft"
            else -> "sie ist gerade nicht erreichbar"
        }
        return "${weg.label}: $grund. Ich lese mit der Stimme des Geräts vor."
    }

    /** Erneuter Druck hält an; ein weiterer setzt fort (F-12 Schritt 3). */
    fun umschalten() {
        if (!_laeuft.value) return
        val weg = derzeit ?: return
        // Angehalten wird immer der Absatz, der gerade läuft — alle Netz-Stimmen gehen über
        // denselben Abspieler, deshalb trifft ein Druck sie auch alle.
        if (weg == TtsProvider.GERAET) {
            // Androids Sprachausgabe kennt kein Fortsetzen: ein Druck beendet sie.
            halteAn()
            return
        }
        if (_pausiert.value) {
            if (abspieler.fortsetzen()) _pausiert.value = false
        } else {
            if (abspieler.pause()) _pausiert.value = true
        }
    }

    /** Beim Wechsel in den Hintergrund wird eine laufende Wiedergabe gestoppt (§6). */
    fun halteAn() {
        // Zuerst der laufende Vorgang: er würde sonst den nächsten Absatz nachschieben.
        vorlesejob?.cancel()
        vorlesejob = null
        abspieler.stop()
        google.stop()
        edge.stop()
        qwen.stop()
        // Nur anhalten, wenn die Rückfallebene überhaupt schon eingerichtet wurde — sonst
        // würde allein das Beenden der App Androids Sprachdienst hochfahren.
        if (geraetEingerichtet) geraet.stop()
        _laeuft.value = false
        _pausiert.value = false
        derzeit = null
    }

    fun schliesse() {
        halteAn()
        bereich.cancel()
        google.shutdown()
        edge.shutdown()
        qwen.shutdown()
        if (geraetEingerichtet) geraet.shutdown()
    }
}
