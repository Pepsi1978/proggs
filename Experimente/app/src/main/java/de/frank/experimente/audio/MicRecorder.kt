package de.frank.experimente.audio

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
import java.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MicRecorder(context: Context) {
    private val appContext = context.applicationContext
    private val lifecycleLock = Any()
    private val bufferLock = Any()
    private val pcmBuffer = ByteArrayOutputStream()
    private val recording = AtomicBoolean(false)

    @Volatile
    private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null
    private var startedAtMs = 0L

    /** The rate the running capture actually got, which the WAV header has to match. */
    private var activeSampleRate = SAMPLE_RATE

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
        recorder = activeRecorder
        startedAtMs = System.currentTimeMillis()
        recordingJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ByteArray(minBufferSize)
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
                        read > 0 -> synchronized(bufferLock) {
                            if (pcmBuffer.size() + read <= MAX_BUFFER_BYTES) {
                                pcmBuffer.write(readBuffer, 0, read)
                            } else {
                                logger.warning("Maximum recording buffer reached")
                                return@launch
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

    fun release() {
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

        /** What the reference recording that cloned well was made at. */
        const val CLONING_SAMPLE_RATE = 24_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val CHANNEL_COUNT = 1
        private const val BITS_PER_SAMPLE = 16
        private const val WAV_HEADER_BYTES = 44
        private const val MAX_BUFFER_BYTES = SAMPLE_RATE * 2 * 60 * 10
        private const val STOP_JOIN_TIMEOUT_MS = 1_500L
        private val logger = Logger.getLogger(MicRecorder::class.java.name)
    }
}
