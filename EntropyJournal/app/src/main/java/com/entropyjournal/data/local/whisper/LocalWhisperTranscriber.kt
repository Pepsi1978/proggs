package com.entropyjournal.data.local.whisper

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalWhisperTranscriber @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recognizer: OfflineRecognizer? = null

    private fun getOrCreateRecognizer(): OfflineRecognizer {
        recognizer?.let { return it }

        val whisperConfig = OfflineWhisperModelConfig(
            encoder = "whisper/base-encoder.int8.onnx",
            decoder = "whisper/base-decoder.int8.onnx",
            language = "de",
            task = "transcribe"
        )

        val modelConfig = OfflineModelConfig()
        modelConfig.whisper = whisperConfig
        modelConfig.tokens = "whisper/base-tokens.txt"
        modelConfig.numThreads = 4

        val featConfig = FeatureConfig()

        val config = OfflineRecognizerConfig(
            featConfig = featConfig,
            modelConfig = modelConfig
        )

        val rec = OfflineRecognizer(context.assets, config)
        recognizer = rec
        return rec
    }

    suspend fun transcribe(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rec = getOrCreateRecognizer()

            val samples = readWavSamples(audioFile)
            if (samples.isEmpty()) {
                return@withContext Result.failure(Exception("Audio-Datei ist leer"))
            }

            val stream = rec.createStream()
            stream.acceptWaveform(samples, 16000)
            rec.decode(stream)
            val text = rec.getResult(stream).text.trim()

            if (text.isBlank()) {
                Result.failure(Exception("Kein Text erkannt"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readWavSamples(file: File): FloatArray {
        val bytes = file.readBytes()
        if (bytes.size < 46) return floatArrayOf()

        val dataStart = 44
        val numSamples = (bytes.size - dataStart) / 2

        val samples = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            val offset = dataStart + i * 2
            val lo = bytes[offset].toInt() and 0xFF
            val hi = bytes[offset + 1].toInt()
            val sample = (hi shl 8) or lo
            samples[i] = sample.toShort().toFloat() / 32768.0f
        }
        return samples
    }
}
