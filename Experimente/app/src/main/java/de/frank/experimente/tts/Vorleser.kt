package de.frank.experimente.tts

import android.content.Context
import de.frank.experimente.data.settings.Einstellungen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
            "spricht ueber ${weg.id} (gespeichert: $anbieter, Stimme: ${stimmeVon(weg)})",
        )

        val start = { _laeuft.value = true; _pausiert.value = false }
        val fertig = { _laeuft.value = false; _pausiert.value = false; derzeit = null }
        val aufgeben = { f: Exception, gescheitert: TtsProvider ->
            _laeuft.value = false
            _pausiert.value = false
            derzeit = null
            beiFehler(verstaendlich(f, gescheitert))
        }

        /** Scheitert der gewählte Weg, übernimmt das Gerät. Es selbst hat keinen Rückfall. */
        val fehler: (Exception) -> Unit = if (weg == TtsProvider.GERAET) {
            { f -> aufgeben(f, TtsProvider.GERAET) }
        } else {
            { f ->
                android.util.Log.w("Vorleser", "${weg.id} fehlgeschlagen: ${f.message}", f)
                beiFehler(rueckfallHinweis(weg, f))
                sprichMitGeraet(text, tempo, start, fertig) { letzter ->
                    aufgeben(letzter, TtsProvider.GERAET)
                }
            }
        }

        when (weg) {
            TtsProvider.GOOGLE_CLOUD -> {
                val schluessel = einstellungen.googleTtsSchluessel
                if (schluessel.isBlank()) {
                    // Kein Grund zu schweigen: das Gerät kann es auch.
                    beiFehler(
                        "Für die Google-Stimme fehlt der Schlüssel (Einstellungen → Zugänge). " +
                            "Ich lese mit der Stimme des Geräts vor.",
                    )
                    sprichMitGeraet(text, tempo, start, fertig) { f ->
                        aufgeben(f, TtsProvider.GERAET)
                    }
                    return
                }
                google.speak(
                    text = text,
                    apiKey = schluessel,
                    voiceName = einstellungen.stimmeGoogle,
                    speechRate = tempo,
                    onPlaybackStart = start,
                    onComplete = fertig,
                    onError = fehler,
                )
            }

            TtsProvider.QWEN_CLONE -> {
                val schluessel = einstellungen.qwenSchluessel
                val stimme = einstellungen.stimmeQwen
                if (schluessel.isBlank() || stimme.isBlank()) {
                    beiFehler(
                        "Für deine eigene Stimme fehlt noch der Schlüssel oder die Aufnahme. " +
                            "Ich lese mit der Stimme des Geräts vor.",
                    )
                    sprichMitGeraet(text, tempo, start, fertig) { f ->
                        aufgeben(f, TtsProvider.GERAET)
                    }
                    return
                }
                qwen.speak(
                    text = text,
                    rawApiKey = schluessel,
                    rawVoiceId = stimme,
                    onPlaybackStart = start,
                    onComplete = fertig,
                    onError = fehler,
                )
            }

            TtsProvider.EDGE -> edge.speak(
                text = text,
                voice = einstellungen.stimmeEdge,
                speechRate = tempo,
                onPlaybackStart = start,
                onComplete = fertig,
                onError = fehler,
            )

            TtsProvider.GERAET -> sprichMitGeraet(text, tempo, start, fertig, fehler)
        }
    }

    /** Die Rückfallebene sprechen lassen. */
    private fun sprichMitGeraet(
        text: String,
        tempo: Float,
        start: () -> Unit,
        fertig: () -> Unit,
        beiFehler: (Exception) -> Unit,
    ) {
        derzeit = TtsProvider.GERAET
        geraet.speak(
            text = text,
            speechRate = tempo,
            onPlaybackStart = start,
            onComplete = fertig,
            onError = beiFehler,
        )
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
        if (_pausiert.value) {
            val ging = when (weg) {
                TtsProvider.GOOGLE_CLOUD -> google.resume()
                TtsProvider.QWEN_CLONE -> qwen.resume()
                TtsProvider.EDGE -> edge.resume()
                TtsProvider.GERAET -> geraet.resume()
            }
            if (ging) _pausiert.value = false
        } else {
            val ging = when (weg) {
                TtsProvider.GOOGLE_CLOUD -> google.pause()
                TtsProvider.QWEN_CLONE -> qwen.pause()
                TtsProvider.EDGE -> edge.pause()
                // Androids Sprachausgabe kennt kein Fortsetzen: ein Druck beendet sie.
                TtsProvider.GERAET -> {
                    geraet.stop()
                    _laeuft.value = false
                    derzeit = null
                    false
                }
            }
            if (ging) _pausiert.value = true
        }
    }

    /** Beim Wechsel in den Hintergrund wird eine laufende Wiedergabe gestoppt (§6). */
    fun halteAn() {
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
        google.shutdown()
        edge.shutdown()
        qwen.shutdown()
        if (geraetEingerichtet) geraet.shutdown()
    }
}
