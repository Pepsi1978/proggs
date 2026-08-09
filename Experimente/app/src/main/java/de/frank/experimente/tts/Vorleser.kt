package de.frank.experimente.tts

import android.content.Context
import de.frank.experimente.data.settings.Einstellungen

/**
 * F-12 — Vorlesen über den in den Einstellungen gewählten Anbieter.
 *
 * Es läuft immer **nur eine** Wiedergabe: ein neuer Aufruf bricht den laufenden ab.
 * Fehlt der Schlüssel für den gewählten Weg, meldet der Vorleser das, statt still nichts
 * zu tun.
 */
class Vorleser(context: Context, private val einstellungen: Einstellungen) {

    private val google = GoogleCloudTtsPlayer(context)
    private val qwen = QwenTtsPlayer(context)
    private val edge = EdgeTtsPlayer(context)

    private var laufenderAnbieter: TtsProvider? = null

    fun sprich(
        text: String,
        onStart: () -> Unit,
        onFertig: () -> Unit,
        onFehler: (Exception) -> Unit,
    ) {
        stopp()
        val anbieter = TtsProvider.entries
            .firstOrNull { it.id == einstellungen.ttsAnbieter } ?: TtsProvider.GOOGLE_CLOUD
        laufenderAnbieter = anbieter
        val tempo = einstellungen.sprechtempo

        when (anbieter) {
            TtsProvider.GOOGLE_CLOUD -> {
                val schluessel = einstellungen.googleTtsSchluessel
                if (schluessel.isBlank()) {
                    onFehler(FehlenderSchluessel()); return
                }
                google.speak(
                    text = text,
                    apiKey = schluessel,
                    voiceName = einstellungen.stimmeGoogle,
                    speechRate = tempo,
                    onPlaybackStart = onStart,
                    onComplete = onFertig,
                    onError = onFehler,
                )
            }

            TtsProvider.QWEN_CLONE -> {
                val schluessel = einstellungen.qwenSchluessel
                val stimme = einstellungen.stimmeEigen
                if (schluessel.isBlank() || stimme.isBlank()) {
                    onFehler(FehlenderSchluessel()); return
                }
                qwen.speak(
                    text = text,
                    rawApiKey = schluessel,
                    rawVoiceId = stimme,
                    onPlaybackStart = onStart,
                    onComplete = onFertig,
                    onError = onFehler,
                )
            }

            TtsProvider.EDGE -> edge.speak(
                text = text,
                voice = einstellungen.stimmeEdge,
                speechRate = tempo,
                onPlaybackStart = onStart,
                onComplete = onFertig,
                onError = onFehler,
            )
        }
    }

    fun stopp() {
        google.stop()
        qwen.stop()
        edge.stop()
        laufenderAnbieter = null
    }

    fun beenden() {
        google.shutdown()
        qwen.shutdown()
        edge.shutdown()
    }

    /** „Für diese Stimme fehlt der Schlüssel.“ (02-UI-SPEC §8) */
    class FehlenderSchluessel : Exception("Für diese Stimme fehlt der Schlüssel.")
}
