package de.frank.stacklabor.werftstudio.service.speech

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
    private var activeSampleRate = SAMPLE_RATE

    fun start(scope: CoroutineScope): Boolean = synchronized(lifecycleLock) {
        if (!recording.compareAndSet(false, true)) return true
        if (!scope.isActive || ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recording.set(false)
            return false
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBufferSize <= 0) {
            recording.set(false)
            return false
        }
        activeSampleRate = SAMPLE_RATE
        val activeRecorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL,
                ENCODING,
                minBufferSize * 2,
            )
        } catch (_: Exception) {
            recording.set(false)
            return false
        }
        if (activeRecorder.state != AudioRecord.STATE_INITIALIZED) {
            recording.set(false)
            activeRecorder.release()
            return false
        }
        try {
            activeRecorder.startRecording()
        } catch (_: Exception) {
            recording.set(false)
            activeRecorder.release()
            return false
        }

        synchronized(bufferLock) { pcmBuffer.reset() }
        recorder = activeRecorder
        recordingJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ByteArray(minBufferSize)
            try {
                while (isActive && recording.get()) {
                    val read = activeRecorder.read(readBuffer, 0, readBuffer.size, AudioRecord.READ_BLOCKING)
                    if (!recording.get()) break
                    when {
                        read > 0 -> synchronized(bufferLock) {
                            if (pcmBuffer.size() + read <= MAX_BUFFER_BYTES) pcmBuffer.write(readBuffer, 0, read)
                            else return@launch
                        }
                        read < 0 -> return@launch
                    }
                }
            } finally {
                if (recording.get()) {
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
        stopAndRelease(activeRecorder)
        activeJob?.cancel()
        withTimeoutOrNull(STOP_JOIN_TIMEOUT_MS) { activeJob?.let { joinAll(it) } }
        val pcm = synchronized(bufferLock) {
            pcmBuffer.toByteArray().also { pcmBuffer.reset() }
        }
        return pcm.takeIf(ByteArray::isNotEmpty)?.let(::pcmToWav)
    }

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
        synchronized(bufferLock) { pcmBuffer.reset() }
    }

    private fun stopAndRelease(activeRecorder: AudioRecord?) {
        try {
            activeRecorder?.stop()
        } catch (_: Exception) {
        }
        try {
            activeRecorder?.release()
        } catch (_: Exception) {
        }
    }

    private fun pcmToWav(pcm: ByteArray): ByteArray {
        val byteRate = activeSampleRate * CHANNEL_COUNT * BITS_PER_SAMPLE / 8
        val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size + 36)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(CHANNEL_COUNT.toShort())
            putInt(activeSampleRate)
            putInt(byteRate)
            putShort((CHANNEL_COUNT * BITS_PER_SAMPLE / 8).toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size)
        }.array()
        return header + pcm
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val CHANNEL_COUNT = 1
        const val BITS_PER_SAMPLE = 16
        const val WAV_HEADER_BYTES = 44
        const val MAX_BUFFER_BYTES = SAMPLE_RATE * 2 * 60 * 10
        const val STOP_JOIN_TIMEOUT_MS = 1_500L
    }
}
