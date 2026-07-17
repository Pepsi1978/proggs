package de.frank.karteikartenlernen.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WavAudioRecorder(context: Context) {
    private val cacheDir = context.applicationContext.cacheDir
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var session: RecordingSession? = null

    suspend fun start() = withContext(Dispatchers.IO) {
        val minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        require(minimum > 0) { "Das Mikrofon unterstützt das benötigte Aufnahmeformat nicht." }
        val bufferSize = maxOf(minimum * 2, SAMPLE_RATE * 2)
        val file = File(cacheDir, "groq_recording_${System.currentTimeMillis()}.wav")
        val output = FileOutputStream(file)
        output.write(wavHeader(0))
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            output.close()
            file.delete()
            error("Das Mikrofon konnte nicht initialisiert werden.")
        }

        val current = RecordingSession(file, output, recorder, bufferSize)
        try {
            synchronized(lock) {
                check(session == null) { "Es läuft bereits eine Aufnahme." }
                recorder.startRecording()
                session = current
                current.writer = scope.launch { writeAudio(current) }
            }
        } catch (error: Exception) {
            recorder.release()
            output.close()
            file.delete()
            throw error
        }
    }

    suspend fun stop(): File = withContext(Dispatchers.IO) {
        val current = synchronized(lock) {
            session?.also {
                session = null
                it.running.set(false)
            } ?: error("Es läuft keine Aufnahme.")
        }
        runCatching { current.recorder.stop() }
        current.writer.join()
        current.recorder.release()
        current.failure?.let {
            current.file.delete()
            throw it
        }
        val dataBytes = (current.file.length() - WAV_HEADER_BYTES).coerceAtLeast(0)
        RandomAccessFile(current.file, "rw").use { wav ->
            wav.seek(0)
            wav.write(wavHeader(dataBytes))
        }
        current.file
    }

    fun cancel() {
        val current = synchronized(lock) {
            session?.also {
                session = null
                it.running.set(false)
            }
        } ?: return
        runCatching { current.recorder.stop() }
        current.writer.cancel()
        current.recorder.release()
        runCatching { current.output.close() }
        current.file.delete()
    }

    fun shutdown() {
        cancel()
        scope.cancel()
    }

    private fun writeAudio(current: RecordingSession) {
        val buffer = ByteArray(current.bufferSize)
        try {
            while (current.running.get()) {
                val read = current.recorder.read(buffer, 0, buffer.size)
                if (read > 0) current.output.write(buffer, 0, read)
                else if (read < 0 && current.running.get()) error("Mikrofon-Lesefehler: $read")
            }
        } catch (error: Exception) {
            if (current.running.get()) current.failure = error
        } finally {
            runCatching { current.output.close() }
        }
    }

    private class RecordingSession(
        val file: File,
        val output: FileOutputStream,
        val recorder: AudioRecord,
        val bufferSize: Int,
        val running: AtomicBoolean = AtomicBoolean(true),
    ) {
        lateinit var writer: Job
        @Volatile var failure: Exception? = null
    }

    companion object {
        internal const val SAMPLE_RATE = 16_000
        internal const val WAV_HEADER_BYTES = 44L
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        internal fun wavHeader(dataBytes: Long): ByteArray = ByteBuffer.allocate(WAV_HEADER_BYTES.toInt())
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt((36L + dataBytes).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)
                putShort(1.toShort())
                putShort(1.toShort())
                putInt(SAMPLE_RATE)
                putInt(SAMPLE_RATE * 2)
                putShort(2.toShort())
                putShort(16.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(dataBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }
            .array()
    }
}
