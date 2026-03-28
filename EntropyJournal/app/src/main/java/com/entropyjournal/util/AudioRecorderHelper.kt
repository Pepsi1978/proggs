package com.entropyjournal.util

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderHelper @Inject constructor() {

    @Volatile
    private var isRecording = false

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds

    private val sampleRate = Constants.AUDIO_SAMPLE_RATE
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    suspend fun startRecording(outputFile: File): Unit = withContext(Dispatchers.IO) {
        if (isRecording) return@withContext

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord konnte nicht initialisiert werden")
        }

        isRecording = true
        _durationSeconds.value = 0
        _amplitude.value = 0f

        recorder.startRecording()

        val buffer = ShortArray(bufferSize / 2)
        var totalSamplesWritten = 0L
        val startTime = System.currentTimeMillis()

        val outputStream = FileOutputStream(outputFile)
        try {
            // Write placeholder WAV header (44 bytes)
            outputStream.write(ByteArray(44))

            while (isRecording && isActive) {
                val readCount = recorder.read(buffer, 0, buffer.size)
                if (readCount > 0) {
                    // Calculate amplitude for visualization
                    var sum = 0L
                    for (i in 0 until readCount) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = Math.sqrt(sum.toDouble() / readCount).toFloat()
                    _amplitude.value = (rms / Short.MAX_VALUE).coerceIn(0f, 1f)

                    // Write PCM data as little-endian bytes
                    val byteBuffer = ByteArray(readCount * 2)
                    for (i in 0 until readCount) {
                        byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                        byteBuffer[i * 2 + 1] = (buffer[i].toInt() shr 8 and 0xFF).toByte()
                    }
                    outputStream.write(byteBuffer)
                    totalSamplesWritten += readCount

                    val elapsed = (System.currentTimeMillis() - startTime) / 1000
                    _durationSeconds.value = elapsed.toInt()
                }
            }
        } finally {
            outputStream.close()
            try { recorder.stop() } catch (_: Exception) {}
            recorder.release()
        }

        // Update WAV header with correct file size
        val totalDataSize = totalSamplesWritten * 2
        writeWavHeader(outputFile, totalDataSize)

        _amplitude.value = 0f
    }

    fun stopRecording() {
        isRecording = false
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    private fun writeWavHeader(file: File, dataSize: Long) {
        val raf = RandomAccessFile(file, "rw")
        val totalFileSize = dataSize + 36

        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeIntLE((totalFileSize).toInt())
        raf.writeBytes("WAVE")

        raf.writeBytes("fmt ")
        raf.writeIntLE(16)
        raf.writeShortLE(1)
        raf.writeShortLE(Constants.AUDIO_CHANNELS.toShort())
        raf.writeIntLE(sampleRate)
        raf.writeIntLE(sampleRate * Constants.AUDIO_CHANNELS * 2)
        raf.writeShortLE((Constants.AUDIO_CHANNELS * 2).toShort())
        raf.writeShortLE(16)

        raf.writeBytes("data")
        raf.writeIntLE(dataSize.toInt())

        raf.close()
    }
}

private fun RandomAccessFile.writeIntLE(value: Int) {
    write(value and 0xFF)
    write((value shr 8) and 0xFF)
    write((value shr 16) and 0xFF)
    write((value shr 24) and 0xFF)
}

private fun RandomAccessFile.writeShortLE(value: Short) {
    write(value.toInt() and 0xFF)
    write((value.toInt() shr 8) and 0xFF)
}
