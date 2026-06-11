package de.frank.voicekey.wake

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import de.frank.voicekey.data.WakeLang
import de.frank.voicekey.obs.Obs
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Wake-Word-Erkennung mit Vosk: EIN AudioRecord-Stream (16 kHz mono) fuettert pro Sprache
 * einen Recognizer mit Grammatik-Spotting — die Grammatik enthaelt NUR die aktiven
 * (Favoriten-)Phrasen + "[unk]". Das macht die Erkennung deutlich treffsicherer als
 * freies Diktat (Best-Practices voice-assistant-trigger §2).
 *
 * WICHTIG (Almanach §8/§9): KEIN setPrivacySensitive; nach stop() gibt der Audio-Thread
 * das Mikrofon vollstaendig frei, damit ChatGPT es uebernehmen kann.
 */
class VoskWakeEngine(
    private val onWakeWord: (phrase: String, lang: WakeLang) -> Unit,
    private val onStopWord: (phrase: String, lang: WakeLang) -> Unit,
) {

    private data class LangSetup(val model: Model, val phrases: List<String>, val stopPhrases: List<String>)

    @Volatile private var running = false
    private var audioThread: Thread? = null

    /** Startet das Lauschen. Pro Sprache: geladenes Modell + aktive Phrasen (lowercase-Match). */
    fun start(
        setups: Map<WakeLang, Pair<Model, List<String>>>,
        stopPhrases: Map<WakeLang, List<String>>,
    ) {
        if (running) {
            Obs.w("VoskWakeEngine", "start", "Engine laeuft bereits — Start ignoriert")
            return
        }
        val active = setups
            .mapValues { (lang, v) ->
                LangSetup(
                    v.first,
                    v.second.map { it.trim().lowercase() }.filter { it.isNotEmpty() },
                    (stopPhrases[lang] ?: emptyList()).map { it.trim().lowercase() }.filter { it.isNotEmpty() },
                )
            }
            .filterValues { it.phrases.isNotEmpty() }
        if (!Obs.probe(active.isNotEmpty(), "Keine aktiven Wake-Woerter — Engine startet nicht", "VoskWakeEngine", "start")) return

        running = true
        audioThread = Thread({ audioLoop(active) }, "WakeAudio").apply { start() }
    }

    /** Stoppt das Lauschen und gibt das Mikrofon frei. Blockiert bis der Audio-Thread beendet ist. */
    fun stop() {
        running = false
        audioThread?.join(2_000)
        audioThread = null
    }

    @SuppressLint("MissingPermission") // RECORD_AUDIO wird vom Service vor dem Start geprueft.
    private fun audioLoop(setups: Map<WakeLang, LangSetup>) {
        val recognizers = mutableMapOf<WakeLang, Recognizer>()
        var audio: AudioRecord? = null
        try {
            setups.forEach { (lang, setup) ->
                val grammar = JSONArray().apply {
                    setup.phrases.forEach { put(it) }
                    setup.stopPhrases.forEach { put(it) }
                    put("[unk]")
                }.toString()
                recognizers[lang] = Recognizer(setup.model, SAMPLE_RATE.toFloat(), grammar)
                Obs.i("VoskWakeEngine", "audioLoop", "Recognizer bereit", mapOf("lang" to lang, "grammar" to grammar))
            }

            val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            audio = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer * 4, SAMPLE_RATE), // grosszuegiger Puffer gegen Drops
            )
            if (!Obs.probe(audio.state == AudioRecord.STATE_INITIALIZED, "AudioRecord nicht initialisiert", "VoskWakeEngine", "audioLoop")) {
                return
            }
            audio.startRecording()
            Obs.checkpoint(
                step = "Lauschen aktiv",
                intent = "Always-On-Mikrofon erkennt Wake-Woerter DE+EN parallel",
                expected = "RECORDSTATE_RECORDING",
                actual = if (audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) "RECORDSTATE_RECORDING" else "state=${audio.recordingState}",
                ctx = mapOf("sprachen" to setups.keys.joinToString(), "phrasen" to setups.values.sumOf { it.phrases.size }),
            )

            val buffer = ShortArray(SAMPLE_RATE / 10) // 100 ms Audio pro Durchlauf
            var lastWakeAtMs = 0L // koppelt "Wake + beenden" auch ueber getrennte Erkenner/Chunks
            while (running) {
                val read = audio.read(buffer, 0, buffer.size)
                if (read <= 0) {
                    Obs.w("VoskWakeEngine", "audioLoop", "AudioRecord.read lieferte $read — Schleife endet")
                    break
                }
                // Wake ("ok chatty") und Beenden ("beenden") landen oft in VERSCHIEDENEN Sprach-
                // Erkennern (EN hoert "ok chatty [unk]", DE hoert "beenden"). Darum ZUERST alle
                // Erkenner dieses Chunks zusammen auswerten, DANN entscheiden.
                var chunkWake: String? = null
                var chunkWakeLang: WakeLang? = null
                var chunkStop: String? = null
                var chunkStopLang: WakeLang? = null
                var chunkText = ""
                for ((lang, recognizer) in recognizers) {
                    if (!recognizer.acceptWaveForm(buffer, read)) continue
                    val text = extractText(recognizer.result)
                    val setup = setups.getValue(lang)
                    if (chunkStop == null) {
                        setup.stopPhrases.firstOrNull { text.contains(it) }?.let { chunkStop = it; chunkStopLang = lang }
                    }
                    if (chunkWake == null) {
                        setup.phrases.firstOrNull { text.contains(it) }?.let { chunkWake = it; chunkWakeLang = lang }
                    }
                    if (text.isNotEmpty()) chunkText = text
                    recognizer.reset() // nur die feuernden Erkenner; sonst wird der Parallel-Treffer verworfen
                }
                val nowMs = SystemClock.uptimeMillis()
                if (chunkWake != null) lastWakeAtMs = nowMs
                // Beenden NUR zusammen mit (kurz vorher gesagtem) Wake-Wort. "beenden" allein -> ignorieren.
                if (chunkStop != null && nowMs - lastWakeAtMs <= WAKE_STOP_WINDOW_MS) {
                    val sh = chunkStop!!
                    val sl = chunkStopLang!!
                    Obs.checkpoint(
                        step = "Beenden-Wort erkannt",
                        intent = "Wake-Wort + Beenden-Wort beendet die Session",
                        expected = sh,
                        actual = sh,
                        ctx = mapOf("lang" to sl, "wake" to chunkWake, "rohtext" to chunkText),
                    )
                    onStopWord(sh, sl)
                } else if (chunkWake != null) {
                    val wh = chunkWake!!
                    val wl = chunkWakeLang!!
                    Obs.checkpoint(
                        step = "Wake-Word erkannt",
                        intent = "Gesprochenes Favoriten-Wort wird erkannt",
                        expected = wh,
                        actual = wh,
                        ctx = mapOf("lang" to wl, "rohtext" to chunkText),
                    )
                    onWakeWord(wh, wl)
                    if (!running) break // Service stoppt die Engine nach einem Wake-Treffer
                }
            }
        } catch (e: Exception) {
            Obs.e("VoskWakeEngine", "audioLoop", "Audio-Schleife abgebrochen", emptyMap(), e)
        } finally {
            // Mic VOLLSTAENDIG freigeben — ChatGPT braucht es gleich (Almanach §8).
            try {
                audio?.let {
                    if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop()
                    it.release()
                }
            } catch (e: Exception) {
                Obs.w("VoskWakeEngine", "audioLoop", "Mic-Freigabe-Fehler", emptyMap(), e)
            }
            recognizers.values.forEach { runCatching { it.close() } }
            Obs.i("VoskWakeEngine", "audioLoop", "Audio-Thread beendet, Mikrofon freigegeben")
        }
    }

    private fun extractText(resultJson: String): String = try {
        JSONObject(resultJson).optString("text", "").lowercase()
    } catch (e: Exception) {
        Obs.w("VoskWakeEngine", "extractText", "Result-JSON unlesbar", mapOf("raw" to resultJson.take(120)), e)
        ""
    }

    companion object {
        const val SAMPLE_RATE = 16_000

        /** Max. Abstand zwischen Wake-Wort und Beenden-Wort, damit beide als ein Befehl zaehlen. */
        private const val WAKE_STOP_WINDOW_MS = 1_500L
    }
}
