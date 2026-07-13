package de.frank.cortex.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import de.frank.cortex.CortexApp
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MicRecorder {

    @Volatile private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null
    private val buffer = ByteArrayOutputStream()
    // AtomicBoolean statt plain Boolean: wird von Main (toggle) und IO (Loop/stop-Body)
    // gleichzeitig genutzt; compareAndSet macht Doppel-stop() eindeutig (nur EINER liefert Daten).
    private val recording = java.util.concurrent.atomic.AtomicBoolean(false)
    private var startedAtMs = 0L

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        // Sicherheitsdeckel ~10 Minuten (16 kHz * 2 Byte, mono): ohne Limit wuchs der Puffer bei
        // vergessener Aufnahme unbegrenzt (~115 MB/h) Richtung OOM.
        private const val MAX_BUFFER_BYTES = SAMPLE_RATE * 2 * 60 * 10
    }

    fun start(scope: CoroutineScope): Boolean {
        if (recording.get()) return true
        if (ContextCompat.checkSelfPermission(CortexApp.appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            CortexLog.error("MicRecorder", "start", "RECORD_AUDIO nicht gewährt")
            return false
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (bufferSize <= 0) {
            CortexLog.error("MicRecorder", "start", "getMinBufferSize fehlgeschlagen: $bufferSize")
            return false
        }

        recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, // Samsung-kompatibel (statt MIC)
                SAMPLE_RATE, CHANNEL, ENCODING, bufferSize * 2
            )
        } catch (e: SecurityException) {
            CortexLog.error("MicRecorder", "start", "AudioRecord ohne Mikrofonberechtigung blockiert: ${e.message}")
            return false
        } catch (e: Exception) {
            CortexLog.error("MicRecorder", "start", "AudioRecord konnte nicht erstellt werden: ${e.message}")
            return false
        }

        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            CortexLog.error("MicRecorder", "start", "AudioRecord konnte nicht initialisiert werden (state=${recorder?.state})")
            recorder?.release()
            recorder = null
            return false
        }

        buffer.reset()
        try {
            recorder?.startRecording()
        } catch (e: SecurityException) {
            CortexLog.error("MicRecorder", "start", "startRecording ohne Mikrofonberechtigung blockiert: ${e.message}")
            recorder?.release()
            recorder = null
            return false
        } catch (e: IllegalStateException) {
            CortexLog.error("MicRecorder", "start", "startRecording fehlgeschlagen: ${e.message}")
            recorder?.release()
            recorder = null
            return false
        }
        recording.set(true)
        startedAtMs = System.currentTimeMillis()

        recordingJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ByteArray(bufferSize)
            try {
                while (isActive && recording.get()) {
                    val read = recorder?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (read > 0) {
                        if (buffer.size() + read > MAX_BUFFER_BYTES) {
                            CortexLog.warn("MicRecorder", "record", "Max. Aufnahmepuffer erreicht — es wird nichts weiter gepuffert")
                            break
                        }
                        buffer.write(readBuffer, 0, read)
                    } else if (read < 0) {
                        // Fehlercode (z.B. ERROR_DEAD_OBJECT, wenn eine andere App das Mikrofon
                        // uebernimmt): ohne break drehte die Schleife als heisser Busy-Loop weiter.
                        CortexLog.error("MicRecorder", "record", "AudioRecord.read-Fehler: $read — Loop beendet")
                        break
                    }
                }
            } finally {
                // Stirbt der Scope (ViewModel-Ende) OHNE stop(): Recorder freigeben, sonst bleibt
                // das Mikrofon (inkl. Privacy-Indikator) bis zum Prozess-Tod belegt.
                if (!isActive && recording.get()) {
                    release()
                }
            }
        }

        CortexLog.info("MicRecorder", "start", "Aufnahme gestartet", mapOf("bufferSize" to bufferSize))
        return true
    }

    suspend fun stop(): ByteArray? {
        // compareAndSet: zwei parallele stop()-Aufrufe (Doppel-Tipp) lasen sonst beide den Puffer
        // aus und dieselbe Aufnahme wurde doppelt transkribiert — jetzt gewinnt genau einer.
        if (!recording.compareAndSet(true, false)) return null

        recordingJob?.cancelAndJoin()
        recordingJob = null

        val activeRecorder = recorder
        try {
            activeRecorder?.stop()
        } catch (e: IllegalStateException) {
            // AudioRecord wirft auf manchen Geräten "stop failed: 0", wenn sehr kurz oder bereits gestoppt.
            // Die bis dahin gelesenen PCM-Daten bleiben trotzdem verwertbar.
            CortexLog.warn("MicRecorder", "stop", "AudioRecord.stop ignoriert: ${e.message}")
        } finally {
            try { activeRecorder?.release() } catch (e: Exception) {
                CortexLog.warn("MicRecorder", "stop", "AudioRecord.release fehlgeschlagen: ${e.message}")
            }
            recorder = null
        }

        val pcmData = buffer.toByteArray()
        buffer.reset()

        CortexLog.info("MicRecorder", "stop", "Aufnahme gestoppt", mapOf(
            "pcm_bytes" to pcmData.size,
            "duration_ms" to (System.currentTimeMillis() - startedAtMs)
        ))
        return if (pcmData.isNotEmpty()) pcmDataToWav(pcmData) else null
    }

    fun isRecording(): Boolean = recording.get()

    /** Synchrones Aufraeumen (z.B. onCleared): beendet den Lese-Job und gibt den AudioRecord
     *  sofort frei — ohne diese Freigabe blieb das Mikrofon nach App-Ende belegt. */
    fun release() {
        recording.set(false)
        recordingJob?.cancel()
        recordingJob = null
        val activeRecorder = recorder
        recorder = null
        try { activeRecorder?.stop() } catch (_: Exception) {}
        try { activeRecorder?.release() } catch (_: Exception) {}
    }

    private fun pcmDataToWav(pcmData: ByteArray): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = SAMPLE_RATE * 1 * 16 / 8 // mono, 16-bit

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(totalDataLen)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // PCM
            putShort(1) // PCM
            putShort(1) // mono
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort((1 * 16 / 8).toShort()) // block align
            putShort(16) // bits per sample
            put("data".toByteArray())
            putInt(pcmData.size)
        }.array()

        return header + pcmData
    }
}
