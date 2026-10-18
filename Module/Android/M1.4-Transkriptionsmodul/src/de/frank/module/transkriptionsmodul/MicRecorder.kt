// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v2
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** [kleidiaiAbschalten]: siehe [SileroVad] — true auf Prozessoren mit SME, aber ohne SME2. */
class MicRecorder(context: Context, private val kleidiaiAbschalten: Boolean) {
    private val appContext = context.applicationContext
    private val lifecycleLock = Any()
    private val bufferLock = Any()
    private val pcmBuffer = ByteArrayOutputStream()
    private val recording = AtomicBoolean(false)

    /**
     * Die aktuelle Lautstärke von 0 bis 1 — für die Pegel-Anzeige am Aufnahmeknopf (N.7).
     * Bewusst echt gemessen und nicht simuliert: Eine erfundene Welle verrät nicht, ob das
     * Mikrofon überhaupt etwas hört.
     */
    private val _pegel = MutableStateFlow(0f)
    val pegel: StateFlow<Float> = _pegel.asStateFlow()

    @Volatile
    private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null
    private var startedAtMs = 0L

    /** The rate the running capture actually got, which the WAV header has to match. */
    private var activeSampleRate = SAMPLE_RATE

    /**
     * Sprachaktivitätserkennung (Silero). Im Hintergrund geladen und dann behalten; schlägt das Laden fehl, läuft
     * alles wie früher über den Pegel. Die Leseschleife wartet nie darauf: Sonst liefe beim Kaltstart der
     * Aufnahmepuffer über, solange das Modell lädt, und die ersten Silben fehlten. Ist es erst während einer Aufnahme
     * fertig, bekommt es das bis dahin Aufgenommene nachgereicht. [release] schließt es wieder.
     */
    private val vadLock = Any()
    @Volatile private var vadModell: SileroVad? = null
    @Volatile private var vadFehlgeschlagen = false
    private var vadLaedt = false
    private var vadGeneration = 0

    /** Der zuletzt gestartete Lese-Job; er benutzt das Modell, bis er ganz zu Ende ist. */
    @Volatile
    private var letzterLeseJob: Job? = null

    @Volatile
    private var zeitachse: SprachZeitachse? = null

    /**
     * Wie viele 32-ms-Rahmen der laufenden Aufnahme bisher sicher Sprache waren; wächst nur. Null ohne
     * Spracherkennung (Modell fehlt, andere Abtastrate): Dann zählt wie früher der Pegel.
     */
    val sprachRahmen: Long? get() = zeitachse?.sprachRahmen

    /** Nach [stop]: je 32-ms-Rahmen, ob die Aufnahme dort nach Stimme klang; null ohne Spracherkennung. */
    var letzteStimmhaft: BooleanArray? = null
        private set

    init {
        vadLaden()
    }

    /** Lädt Silero im Hintergrund, falls es weder geladen ist noch gerade lädt noch schon gescheitert ist. */
    private fun vadLaden() {
        val generation = synchronized(vadLock) {
            if (vadModell != null || vadFehlgeschlagen || vadLaedt) return
            vadLaedt = true
            vadGeneration
        }
        Thread({
            val geladen = runCatching { SileroVad(appContext, kleidiaiAbschalten) }
                .onFailure { logger.warning("Silero VAD not available: ${it.message}") }
                .getOrNull()
            val behalten = synchronized(vadLock) {
                if (generation != vadGeneration) {
                    false // Inzwischen freigegeben: Dieses Modell gehört niemandem mehr.
                } else {
                    vadLaedt = false
                    if (geladen == null) vadFehlgeschlagen = true else vadModell = geladen
                    true
                }
            }
            if (!behalten) runCatching { geladen?.close() }
        }, "vad-laden").apply { isDaemon = true }.start()
    }

    /** Gibt das Modell frei, erst wenn [leseJob] ganz zu Ende ist: Er könnte noch mitten in einer Rechnung stecken. */
    private fun vadFreigeben(leseJob: Job?) {
        val modell = synchronized(vadLock) {
            vadGeneration++
            vadLaedt = false
            vadFehlgeschlagen = false
            vadModell.also { vadModell = null }
        } ?: return
        if (leseJob == null || leseJob.isCompleted) {
            runCatching { modell.close() }
        } else {
            leseJob.invokeOnCompletion { runCatching { modell.close() } }
        }
    }

    /**
     * Starts capturing.
     *
     * [requestedSampleRate] lets voice cloning record in higher quality than speech recognition
     * needs. Only 44.1 kHz is guaranteed on every device, so a rate the device refuses falls
     * back to the 16 kHz that dictation has always used rather than failing the recording.
     */
    fun start(scope: CoroutineScope, requestedSampleRate: Int = SAMPLE_RATE): Boolean = synchronized(lifecycleLock) {
        if (!recording.compareAndSet(false, true)) return true
        if (!scope.isActive) {
            recording.set(false)
            logger.warning("Recording scope is not active")
            return false
        }

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            recording.set(false)
            logger.warning("RECORD_AUDIO permission is missing")
            return false
        }

        val sampleRate = listOf(requestedSampleRate, SAMPLE_RATE)
            .distinct()
            .firstOrNull { AudioRecord.getMinBufferSize(it, CHANNEL, ENCODING) > 0 }
        val minBufferSize = sampleRate?.let { AudioRecord.getMinBufferSize(it, CHANNEL, ENCODING) } ?: 0
        if (sampleRate == null || minBufferSize <= 0) {
            recording.set(false)
            logger.warning("AudioRecord.getMinBufferSize failed for $requestedSampleRate Hz")
            return false
        }
        if (sampleRate != requestedSampleRate) {
            logger.warning("$requestedSampleRate Hz unavailable, recording at $sampleRate Hz")
        }
        activeSampleRate = sampleRate

        val activeRecorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                CHANNEL,
                ENCODING,
                minBufferSize * 2,
            )
        } catch (error: Exception) {
            recording.set(false)
            logger.warning("AudioRecord creation failed: ${error.message}")
            return false
        }

        if (activeRecorder.state != AudioRecord.STATE_INITIALIZED) {
            recording.set(false)
            activeRecorder.release()
            logger.warning("AudioRecord was not initialized")
            return false
        }

        try {
            activeRecorder.startRecording()
        } catch (error: Exception) {
            recording.set(false)
            activeRecorder.release()
            logger.warning("AudioRecord.startRecording failed: ${error.message}")
            return false
        }

        synchronized(bufferLock) { pcmBuffer.reset() }
        letzteStimmhaft = null
        zeitachse = null
        recorder = activeRecorder
        startedAtMs = System.currentTimeMillis()
        // Während gesprochen wird, schon die Verbindung zu Groq öffnen — beim Hochladen steht sie dann.
        // Nur für Sprachaufnahmen, nicht für die Stimm-Aufnahme in höherer Qualität.
        if (requestedSampleRate == SAMPLE_RATE) GroqNetz.vorwaermen()
        // Silero kennt nur 16 kHz; die Stimm-Aufnahme in höherer Qualität läuft ohne.
        if (sampleRate == SAMPLE_RATE) vadLaden()
        recordingJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ByteArray(minBufferSize)
            var achse: SprachZeitachse? = null
            var vadMoeglich = sampleRate == SAMPLE_RATE
            try {
                while (isActive && recording.get()) {
                    val read = activeRecorder.read(
                        readBuffer,
                        0,
                        readBuffer.size,
                        AudioRecord.READ_BLOCKING,
                    )
                    if (!recording.get()) break
                    when {
                        read > 0 -> {
                            _pegel.value = messePegel(readBuffer, read)
                            if (achse == null && vadMoeglich) {
                                val modell = vadModell
                                if (modell != null) {
                                    // Nie auf das Modell warten, sondern ab jetzt mitrechnen. Was schon im Puffer
                                    // liegt, geht zuerst hinein, damit die Zeitachse genau zur WAV-Datei passt.
                                    val bisher = synchronized(bufferLock) { pcmBuffer.toByteArray() }
                                    if (bisher.size > MAX_NACHREICHEN_BYTES) {
                                        vadMoeglich = false
                                    } else {
                                        achse = try {
                                            modell.zuruecksetzen()
                                            SprachZeitachse(modell::wahrscheinlichkeit).also { a ->
                                                if (bisher.isNotEmpty()) a.pcm(bisher, bisher.size)
                                            }
                                        } catch (error: Throwable) {
                                            logger.warning("Silero VAD failed, falling back to level: ${error.message}")
                                            vadMoeglich = false
                                            null
                                        }
                                        zeitachse = achse
                                    }
                                } else if (vadFehlgeschlagen) {
                                    vadMoeglich = false
                                }
                            }
                            achse?.let { a ->
                                try {
                                    a.pcm(readBuffer, read)
                                } catch (error: Throwable) {
                                    // Nie die Aufnahme opfern: ohne Spracherkennung weiter, wie früher über den Pegel.
                                    logger.warning("Silero VAD failed, falling back to level: ${error.message}")
                                    achse = null
                                    vadMoeglich = false
                                    zeitachse = null
                                }
                            }
                            synchronized(bufferLock) {
                                if (pcmBuffer.size() + read <= MAX_BUFFER_BYTES) {
                                    pcmBuffer.write(readBuffer, 0, read)
                                } else {
                                    logger.warning("Maximum recording buffer reached")
                                    return@launch
                                }
                            }
                        }
                        read < 0 -> {
                            logger.warning("AudioRecord.read failed: $read")
                            return@launch
                        }
                    }
                }
            } finally {
                if (recording.get() && !isActive && recording.compareAndSet(true, false)) {
                    synchronized(lifecycleLock) {
                        if (recorder === activeRecorder) recorder = null
                        if (recordingJob === coroutineContext[Job]) recordingJob = null
                    }
                    stopAndRelease(activeRecorder)
                } else if (recording.get()) {
                    // A full buffer or read error ends capture but leaves stop() able to return
                    // the bytes collected so far.
                    synchronized(lifecycleLock) {
                        if (recorder === activeRecorder) recorder = null
                        if (recordingJob === coroutineContext[Job]) recordingJob = null
                    }
                    stopAndRelease(activeRecorder)
                }
            }
        }
        letzterLeseJob = recordingJob
        true
    }

    suspend fun stop(): ByteArray? {
        val activeRecorder: AudioRecord?
        val activeJob: Job?
        synchronized(lifecycleLock) {
            if (!recording.compareAndSet(true, false)) return null
            activeRecorder = recorder
            activeJob = recordingJob
            recorder = null
            recordingJob = null
        }

        // AudioRecord.stop() wakes a blocking read. The bounded join keeps vendor-specific
        // AudioRecord implementations from deadlocking the caller if they fail to wake it.
        stopAndRelease(activeRecorder)
        activeJob?.cancel()
        val joined = withTimeoutOrNull(STOP_JOIN_TIMEOUT_MS) {
            activeJob?.let { joinAll(it) }
            true
        } ?: false
        if (!joined) logger.warning("AudioRecord read job did not stop within timeout")
        // Erst nach dem Ende des Lese-Jobs: Er schreibt die Zeitachse.
        letzteStimmhaft = if (joined) runCatching { zeitachse?.stimmhaft() }.getOrNull() else null
        zeitachse = null

        val pcm = synchronized(bufferLock) {
            pcmBuffer.toByteArray().also { pcmBuffer.reset() }
        }
        logger.info(
            "Recording stopped: ${pcm.size} PCM bytes, " +
                "${System.currentTimeMillis() - startedAtMs} ms",
        )
        return pcm.takeIf { it.isNotEmpty() }?.let(::pcmToWav)
    }

    fun isRecording(): Boolean = recording.get()

    /** Effektivwert des Blocks, auf 0 bis 1 gebracht und leicht angehoben, damit leise Sprache sichtbar bleibt. */
    private fun messePegel(puffer: ByteArray, laenge: Int): Float {
        if (laenge < 2) return 0f
        var summe = 0.0
        var index = 0
        var anzahl = 0
        while (index + 1 < laenge) {
            val wert = ((puffer[index + 1].toInt() shl 8) or (puffer[index].toInt() and 0xFF)).toShort()
            summe += wert.toDouble() * wert.toDouble()
            index += 2
            anzahl++
        }
        if (anzahl == 0) return 0f
        val effektivwert = kotlin.math.sqrt(summe / anzahl) / Short.MAX_VALUE
        return (kotlin.math.sqrt(effektivwert) * 1.6).toFloat().coerceIn(0f, 1f)
    }

    fun release() {
        _pegel.value = 0f
        val activeRecorder: AudioRecord?
        val activeJob: Job?
        synchronized(lifecycleLock) {
            recording.set(false)
            activeRecorder = recorder
            activeJob = recordingJob
            recorder = null
            recordingJob = null
        }
        activeJob?.cancel()
        stopAndRelease(activeRecorder)
        // Die native ONNX-Sitzung lebt sonst bis zum Prozessende weiter. Die nächste Aufnahme lädt neu.
        vadFreigeben(letzterLeseJob)
    }

    private fun stopAndRelease(activeRecorder: AudioRecord?) {
        try {
            activeRecorder?.stop()
        } catch (_: Exception) {
            // Data captured before a short-recording/vendor stop failure remains usable.
        }
        try {
            activeRecorder?.release()
        } catch (_: Exception) {
            // Release is best-effort during lifecycle cleanup.
        }
    }

    private fun pcmToWav(pcm: ByteArray): ByteArray {
        val sampleRate = activeSampleRate
        val byteRate = sampleRate * CHANNEL_COUNT * BITS_PER_SAMPLE / 8
        val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size + 36)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(CHANNEL_COUNT.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((CHANNEL_COUNT * BITS_PER_SAMPLE / 8).toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size)
        }.array()
        return header + pcm
    }

    companion object {
        const val SAMPLE_RATE = 16_000

        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val CHANNEL_COUNT = 1
        private const val BITS_PER_SAMPLE = 16
        private const val WAV_HEADER_BYTES = 44
        private const val MAX_BUFFER_BYTES = SAMPLE_RATE * 2 * 60 * 10
        private const val STOP_JOIN_TIMEOUT_MS = 1_500L

        /** Höchstens so viel schon Aufgenommenes (10 s) rechnet ein spät geladenes Modell nach, sonst läuft der Puffer über. */
        private const val MAX_NACHREICHEN_BYTES = SAMPLE_RATE * 2 * 10
        private val logger = Logger.getLogger(MicRecorder::class.java.name)
    }
}
