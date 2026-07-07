package de.frank.voicekey.wake

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import de.frank.voicekey.data.WakeLang
import de.frank.voicekey.obs.Obs
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Wake-Word-Erkennung mit Vosk: EIN AudioRecord-Stream (16 kHz mono) fuettert pro Sprache einen
 * Recognizer mit Grammatik-Spotting — die Grammatik enthaelt NUR die aktiven (Favoriten-)Phrasen +
 * "[unk]". Das macht die Erkennung deutlich treffsicherer als freies Diktat (Best-Practices
 * voice-assistant-trigger §2).
 *
 * WICHTIG (Almanach §8/§9): KEIN setPrivacySensitive; nach stop() gibt der Audio-Thread das
 * Mikrofon vollstaendig frei, damit ChatGPT es uebernehmen kann.
 *
 * Akku/Waerme — Sprach-Gate ("Ok Google"-Prinzip): Ein WebRTC-VAD (GMM, Mikrosekunden pro Frame)
 * prueft jeden 100-ms-Chunk, ob ueberhaupt jemand spricht. NUR bei Sprache laufen die teuren
 * Vosk-Recognizer (neuronale ASR). In ruhiger Umgebung (nachts, Schreibtisch) spart das den
 * Grossteil der CPU-Dauerlast. Ein Vorlauf-Ringpuffer fuettert beim Sprachbeginn die letzten Chunks
 * nach, damit der Wortanfang nicht verloren geht; nach dem Sprachende bleibt das Gate lange genug
 * offen, damit Vosk die Aeusserung mit Stille finalisieren kann.
 *
 * Resilienz: Jede Thread-Generation hat ihr EIGENES Stop-Flag — ein neuer start() kann einen alten,
 * im join-Timeout haengen gebliebenen Thread nie wiederbeleben (das frueher geteilte running-Flag
 * konnte einen Zombie-Thread mit ZWEITEM Mikrofon-Stream erzeugen). Stirbt der Loop unerwartet
 * (Mikrofon-Konflikt, Lesefehler, Exception), meldet [onEngineDied] Grund und Laufzeit an den
 * Service-Watchdog — vorher zeigte die Notification "Lauscht…", waehrend niemand zuhoerte.
 */
class VoskWakeEngine(
    private val onWakeWord: (phrase: String, lang: WakeLang) -> Unit,
    private val onStopWord: (phrase: String, lang: WakeLang) -> Unit,
    private val onEngineDied: (reason: String, uptimeMs: Long) -> Unit = { _, _ -> },
    // Gate-Bypass: liefert true, solange eine fremde Voice-Session laeuft (ChatGPT hat das Mic,
    // unsere Aufnahme ist gedaempft/stummgeschaltet). DANN das VAD-Gate AUSSETZEN und jeden Chunk
    // an die Recognizer geben — sonst stuft das aggressive VAD das gedaempfte Session-Audio als
    // Stille ein und "Wake-Wort + beenden" wird nie gehoert (Regression durch das Gate, 0.6.0).
    // Im Normalbetrieb (keine Session) bleibt das Gate aktiv und spart Akku.
    private val bypassGate: () -> Boolean = { false },
) {

    private data class LangSetup(
        val model: Model,
        val phrases: List<String>,
        val stopPhrases: List<String>,
    )

    /** Stop-Flag PRO Thread-Generation — wird nie zwischen altem und neuem Thread geteilt. */
    private class Generation {
        val active = AtomicBoolean(true)
    }

    private var generation: Generation? = null
    private var audioThread: Thread? = null

    /** Startet das Lauschen. Pro Sprache: geladenes Modell + aktive Phrasen (lowercase-Match). */
    fun start(
        setups: Map<WakeLang, Pair<Model, List<String>>>,
        stopPhrases: Map<WakeLang, List<String>>,
    ) {
        if (audioThread?.isAlive == true) {
            Obs.w("VoskWakeEngine", "start", "Engine laeuft bereits — Start ignoriert")
            return
        }
        val active =
            setups
                .mapValues { (lang, v) ->
                    LangSetup(
                        v.first,
                        v.second.map { it.trim().lowercase() }.filter { it.isNotEmpty() },
                        (stopPhrases[lang] ?: emptyList())
                            .map { it.trim().lowercase() }
                            .filter { it.isNotEmpty() },
                    )
                }
                .filterValues { it.phrases.isNotEmpty() }
        if (
            !Obs.probe(
                active.isNotEmpty(),
                "Keine aktiven Wake-Woerter — Engine startet nicht",
                "VoskWakeEngine",
                "start",
            )
        )
            return

        val gen = Generation()
        generation = gen
        audioThread = Thread({ audioLoop(active, gen) }, "WakeAudio").apply { start() }
    }

    /**
     * Stoppt das Lauschen und gibt das Mikrofon frei. Blockiert bis der Audio-Thread beendet ist.
     */
    fun stop() {
        generation?.active?.set(false)
        generation = null
        val thread = audioThread
        audioThread = null
        thread?.join(2_000)
        if (thread?.isAlive == true) {
            // Kein Zombie-Risiko: sein Generations-Flag ist false, er beendet sich beim naechsten
            // read()-Durchlauf von selbst und kann durch start() nie wieder aktiviert werden.
            Obs.w(
                "VoskWakeEngine",
                "stop",
                "Audio-Thread reagiert nicht binnen 2 s — stirbt am Generations-Flag von selbst",
            )
        }
    }

    /**
     * Sprach-Gate erzeugen. Schlaegt die VAD-Initialisierung fehl, lauscht die Engine OHNE Gate
     * weiter (Graceful Degradation: lieber hoeherer Akkuverbrauch als gar kein Wake-Word).
     */
    private fun createVad(): VadWebRTC? =
        try {
            VadWebRTC(
                sampleRate = SampleRate.SAMPLE_RATE_16K,
                frameSize = FrameSize.FRAME_SIZE_320, // 20 ms pro VAD-Frame
                // VERY_AGGRESSIVE ist Pflicht: NORMAL klassifizierte schon Raum-Rauschen als
                // Sprache — das Gate war dauernd offen, Vosk lief wie ohne Gate (70 % CPU bei
                // Stille, live gemessen 2026-06-12). Normal gesprochene Wake-Woerter erkennt
                // auch der strengste Modus zuverlaessig.
                mode = Mode.VERY_AGGRESSIVE,
                speechDurationMs = VAD_SPEECH_MIN_MS,
                silenceDurationMs = VAD_SILENCE_HANGOVER_MS,
            )
        } catch (e: Exception) {
            Obs.w(
                "VoskWakeEngine",
                "createVad",
                "VAD-Init fehlgeschlagen — lausche ohne Sprach-Gate (hoeherer Akkuverbrauch)",
                emptyMap(),
                e,
            )
            null
        }

    @SuppressLint("MissingPermission") // RECORD_AUDIO wird vom Service vor dem Start geprueft.
    private fun audioLoop(setups: Map<WakeLang, LangSetup>, gen: Generation) {
        val startedAt = SystemClock.uptimeMillis()
        var deathReason = "Audio-Schleife unerwartet beendet"
        val recognizers = mutableMapOf<WakeLang, Recognizer>()
        var audio: AudioRecord? = null
        var vad: VadWebRTC? = null
        try {
            setups.forEach { (lang, setup) ->
                val grammar =
                    JSONArray()
                        .apply {
                            setup.phrases.forEach { put(it) }
                            setup.stopPhrases.forEach { put(it) }
                            put("[unk]")
                        }
                        .toString()
                recognizers[lang] = Recognizer(setup.model, SAMPLE_RATE.toFloat(), grammar)
                Obs.i(
                    "VoskWakeEngine",
                    "audioLoop",
                    "Recognizer bereit",
                    mapOf("lang" to lang, "grammar" to grammar),
                )
            }
            vad = createVad()

            val minBuffer =
                AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            audio =
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer * 4, SAMPLE_RATE), // grosszuegiger Puffer gegen Drops
                )
            if (
                !Obs.probe(
                    audio.state == AudioRecord.STATE_INITIALIZED,
                    "AudioRecord nicht initialisiert",
                    "VoskWakeEngine",
                    "audioLoop",
                )
            ) {
                deathReason = "AudioRecord nicht initialisiert (Mikrofon belegt?)"
                return
            }
            audio.startRecording()
            Obs.checkpoint(
                step = "Lauschen aktiv",
                intent = "Always-On-Mikrofon erkennt Wake-Woerter DE+EN parallel",
                expected = "RECORDSTATE_RECORDING",
                actual =
                    if (audio.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                        "RECORDSTATE_RECORDING"
                    else "state=${audio.recordingState}",
                ctx =
                    mapOf(
                        "sprachen" to setups.keys.joinToString(),
                        "phrasen" to setups.values.sumOf { it.phrases.size },
                        "sprachGate" to (vad != null),
                    ),
            )

            val buffer = ShortArray(SAMPLE_RATE / 10) // 100 ms Audio pro Durchlauf
            val vadFrame = ShortArray(VAD_FRAME_SIZE) // wiederverwendet, keine Allokation im Loop
            val preRoll = ArrayDeque<ShortArray>() // letzte Stille-Chunks (Wortanfang-Vorlauf)
            var gateOpen = false
            // Gate-Statistik-Sonde: belegt im Log, wie viel CPU das Sprach-Gate wirklich spart.
            var statChunksTotal = 0
            var statChunksOpen = 0
            var statLastReportMs = SystemClock.uptimeMillis()
            var lastWakeAtMs = 0L // koppelt "Wake + beenden" auch ueber getrennte Erkenner/Chunks
            while (gen.active.get()) {
                val read = audio.read(buffer, 0, buffer.size)
                if (read <= 0) {
                    deathReason = "AudioRecord.read lieferte $read"
                    Obs.w(
                        "VoskWakeEngine",
                        "audioLoop",
                        "AudioRecord.read lieferte $read — Schleife endet",
                    )
                    break
                }

                // ---- Sprach-Gate (Akku): erst der billige VAD, Vosk nur bei Sprache. ----
                // Laeuft eine fremde Voice-Session (ChatGPT hat das Mic), Gate KOMPLETT umgehen:
                // unser Audio ist dann gedaempft, das aggressive VAD wuerde es als Stille werten
                // und
                // "Wake-Wort + beenden" verschlucken. Im Normalbetrieb spart das Gate weiter Akku.
                val bypass = bypassGate()
                // Alle 20-ms-Frames durch den VAD schicken (sein eingebautes Glaetten ueber
                // speech/silenceDurationMs braucht den kontinuierlichen Strom).
                var speech = bypass || vad == null // bypass/ohne VAD: Gate offen (wie vor 0.6.0)
                if (!bypass) {
                    vad?.let { v ->
                        var off = 0
                        while (off + VAD_FRAME_SIZE <= read) {
                            System.arraycopy(buffer, off, vadFrame, 0, VAD_FRAME_SIZE)
                            if (v.isSpeech(vadFrame)) speech = true
                            off += VAD_FRAME_SIZE
                        }
                    }
                }
                statChunksTotal++
                if (speech) statChunksOpen++
                val statNowMs = SystemClock.uptimeMillis()
                if (statNowMs - statLastReportMs >= GATE_STAT_INTERVAL_MS) {
                    Obs.d(
                        "VoskWakeEngine",
                        "audioLoop",
                        "Sprach-Gate-Statistik",
                        mapOf(
                            "offenProzent" to (100 * statChunksOpen / maxOf(statChunksTotal, 1)),
                            "chunks" to statChunksTotal,
                        ),
                    )
                    statChunksTotal = 0
                    statChunksOpen = 0
                    statLastReportMs = statNowMs
                }
                if (!speech) {
                    if (gateOpen) {
                        gateOpen = false
                        // Aeusserung ist laengst finalisiert (Hangover > Vosk-Endpoint) —
                        // Reststate verwerfen, damit nichts in die naechste Aeusserung blutet.
                        recognizers.values.forEach { runCatching { it.reset() } }
                    }
                    // Vorlauf-Ring pflegen: die letzten Chunks VOR dem Sprachbeginn.
                    if (preRoll.size >= PRE_ROLL_CHUNKS) preRoll.removeFirst()
                    preRoll.addLast(buffer.copyOf(read))
                    continue // KEIN Vosk bei Stille — das ist die eigentliche Ersparnis
                }
                if (!gateOpen) {
                    gateOpen = true
                    // Wortanfang nachfuettern: VAD braucht ~50 ms zum Anschlagen, der Anfang
                    // des Wake-Worts liegt noch im Vorlauf-Puffer.
                    for (chunk in preRoll) {
                        recognizers.values.forEach { it.acceptWaveForm(chunk, chunk.size) }
                    }
                    preRoll.clear()
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
                        setup.stopPhrases
                            .firstOrNull { text.contains(it) }
                            ?.let {
                                chunkStop = it
                                chunkStopLang = lang
                            }
                    }
                    if (chunkWake == null) {
                        setup.phrases
                            .firstOrNull { text.contains(it) }
                            ?.let {
                                chunkWake = it
                                chunkWakeLang = lang
                            }
                    }
                    if (text.isNotEmpty()) chunkText = text
                    // Nur die feuernden Erkenner zuruecksetzen; sonst wird der Parallel-Treffer
                    // des jeweils anderen Erkenners verworfen.
                    recognizer.reset()
                }
                val nowMs = SystemClock.uptimeMillis()
                if (chunkWake != null) lastWakeAtMs = nowMs
                // Beenden NUR zusammen mit (kurz vorher gesagtem) Wake-Wort. "beenden" allein ->
                // ignorieren.
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
                    if (!gen.active.get()) break // Service stoppt die Engine nach Wake-Treffer
                }
            }
        } catch (e: Exception) {
            deathReason = "Exception: ${e.message}"
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
            vad?.let { runCatching { it.close() } }
            Obs.i("VoskWakeEngine", "audioLoop", "Audio-Thread beendet, Mikrofon freigegeben")
            // Unerwartetes Ende (stop() wurde NIE gerufen) -> Watchdog im Service informieren.
            // Ohne diese Meldung zeigte die Notification "Lauscht…", obwohl niemand mehr zuhoerte.
            if (gen.active.getAndSet(false)) {
                onEngineDied(deathReason, SystemClock.uptimeMillis() - startedAt)
            }
        }
    }

    private fun extractText(resultJson: String): String =
        try {
            JSONObject(resultJson).optString("text", "").lowercase()
        } catch (e: Exception) {
            Obs.w(
                "VoskWakeEngine",
                "extractText",
                "Result-JSON unlesbar",
                mapOf("raw" to resultJson.take(120)),
                e,
            )
            ""
        }

    companion object {
        const val SAMPLE_RATE = 16_000

        /** Max. Abstand zwischen Wake-Wort und Beenden-Wort, damit beide als ein Befehl zaehlen. */
        private const val WAKE_STOP_WINDOW_MS = 1_500L

        /** WebRTC-VAD-Framegroesse bei 16 kHz: 320 Samples = 20 ms (gueltig: 160/320/480). */
        private const val VAD_FRAME_SIZE = 320

        /** So viel Sprache muss der VAD hoeren, bevor das Gate oeffnet (Fehltrigger-Filter). */
        private const val VAD_SPEECH_MIN_MS = 50

        /**
         * So lange bleibt das Gate nach dem letzten Sprach-Frame offen. MUSS groesser sein als
         * Vosks Endpoint-Stille (~500 ms), sonst bekommt Vosk nie genug Stille, um die Aeusserung
         * zu finalisieren — das Wake-Wort wuerde erst beim naechsten Sprechen gemeldet.
         */
        private const val VAD_SILENCE_HANGOVER_MS = 800

        /** Vorlauf-Chunks (je 100 ms) fuer den Wortanfang vor dem VAD-Anschlag. */
        private const val PRE_ROLL_CHUNKS = 3

        /** Abstand der Gate-Statistik-Logs (Beleg im Log, wie viel das Gate spart). */
        private const val GATE_STAT_INTERVAL_MS = 30_000L
    }
}
