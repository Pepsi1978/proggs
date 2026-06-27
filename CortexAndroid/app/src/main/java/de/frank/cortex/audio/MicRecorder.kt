package de.frank.cortex.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MicRecorder {

    private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null
    private val buffer = ByteArrayOutputStream()
    private var isRecording = false

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    fun start(scope: CoroutineScope): Boolean {
        if (isRecording) return true

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (bufferSize <= 0) {
            CortexLog.error("MicRecorder", "start", "getMinBufferSize fehlgeschlagen: $bufferSize")
            return false
        }

        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, // Samsung-kompatibel (statt MIC)
            SAMPLE_RATE, CHANNEL, ENCODING, bufferSize * 2
        )

        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            CortexLog.error("MicRecorder", "start", "AudioRecord konnte nicht initialisiert werden (state=${recorder?.state})")
            recorder?.release()
            recorder = null
            return false
        }

        buffer.reset()
        recorder?.startRecording()
        isRecording = true

        recordingJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ByteArray(bufferSize)
            while (isActive && isRecording) {
                val read = recorder?.read(readBuffer, 0, readBuffer.size) ?: 0
                if (read > 0) {
                    buffer.write(readBuffer, 0, read)
                }
            }
        }

        CortexLog.info("MicRecorder", "start", "Aufnahme gestartet", mapOf("bufferSize" to bufferSize))
        return true
    }

    fun stop(): ByteArray? {
        if (!isRecording) return null

        isRecording = false
        recordingJob?.cancel()
        recorder?.stop()
        recorder?.release()
        recorder = null

        val pcmData = buffer.toByteArray()
        buffer.reset()

        CortexLog.info("MicRecorder", "stop", "Aufnahme gestoppt", mapOf("pcm_bytes" to pcmData.size))
        return if (pcmData.isNotEmpty()) pcmDataToWav(pcmData) else null
    }

    fun isRecording(): Boolean = isRecording

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
