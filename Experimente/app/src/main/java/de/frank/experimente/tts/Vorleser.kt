package de.frank.experimente.tts

import android.content.Context
import de.frank.experimente.data.settings.Einstellungen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * F-12 — Vorlesen über den in B-08 gewählten Weg.
 *
 * Bündelt die drei übernommenen Wiedergabe-Wege (Google Chirp 3 HD, eigene Stimme über
 * DashScope, Microsoft Edge) hinter einem Knopf. Nur **eine** Wiedergabe gleichzeitig: ein
 * neuer Vorlesevorgang bricht den laufenden ab.
 */
class Vorleser(context: Context, private val einstellungen: Einstellungen) {

    private val google = GoogleCloudTtsPlayer(context)
    private val edge = EdgeTtsPlayer(context)
    private val qwen = QwenTtsPlayer(context)

    private val _laeuft = MutableStateFlow(false)
    val laeuft: StateFlow<Boolean> = _laeuft

    private val _pausiert = MutableStateFlow(false)
    val pausiert: StateFlow<Boolean> = _pausiert

    /** Welcher Weg gerade spricht — damit Anhalten und Fortsetzen den richtigen trifft. */
    private var derzeit: String? = null

    fun lies(text: String, beiFehler: (String) -> Unit) {
        if (text.isBlank()) return
        halteAn()
        val anbieter = einstellungen.ttsAnbieter
        val tempo = einstellungen.sprechtempo
        derzeit = anbieter

        val start = { _laeuft.value = true; _pausiert.value = false }
        val fertig = { _laeuft.value = false; _pausiert.value = false; derzeit = null }
        val fehler = { f: Exception ->
            _laeuft.value = false
            _pausiert.value = false
            derzeit = null
            beiFehler(f.message ?: "Das Vorlesen hat nicht geklappt.")
        }

        // Verteilt wird über den Katalog, nicht über lose Zeichenketten. Vorher stand hier
        // ein `when` mit Literalen und einem `else`, das alles Unbekannte an Edge gab —
        // eine abweichende Kennung („qwen" statt „qwen_clone") landete damit **stumm** bei
        // der falschen Stimme, ohne jede Fehlermeldung. Jetzt ist jeder Anbieter benannt.
        when (TtsProvider.entries.firstOrNull { it.id == anbieter } ?: TtsCatalog.DEFAULT_PROVIDER) {
            TtsProvider.GOOGLE_CLOUD -> {
                val schluessel = einstellungen.googleTtsSchluessel
                if (schluessel.isBlank()) {
                    beiFehler("Für diese Stimme fehlt der Schlüssel. Er steht in den Einstellungen.")
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
                    beiFehler("Für deine eigene Stimme fehlt noch der Schlüssel oder die Aufnahme.")
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
        }
    }

    /** Erneuter Druck hält an; ein weiterer setzt fort (F-12 Schritt 3). */
    fun umschalten() {
        if (!_laeuft.value) return
        if (_pausiert.value) {
            val ging = when (derzeit) {
                "google_cloud" -> google.resume()
                "qwen_clone" -> qwen.resume()
                else -> edge.resume()
            }
            if (ging) _pausiert.value = false
        } else {
            val ging = when (derzeit) {
                "google_cloud" -> google.pause()
                "qwen_clone" -> qwen.pause()
                else -> edge.pause()
            }
            if (ging) _pausiert.value = true
        }
    }

    /** Beim Wechsel in den Hintergrund wird eine laufende Wiedergabe gestoppt (§6). */
    fun halteAn() {
        google.stop()
        edge.stop()
        qwen.stop()
        _laeuft.value = false
        _pausiert.value = false
        derzeit = null
    }

    fun schliesse() {
        google.shutdown()
        edge.shutdown()
        qwen.shutdown()
    }
}
