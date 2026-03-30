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

            // Whisper processes audio in 30-second windows — split at silence points
            val chunks = splitAtSilence(samples)
            val results = StringBuilder()

            for (chunk in chunks) {
                val stream = rec.createStream()
                stream.acceptWaveform(chunk, SAMPLE_RATE)
                rec.decode(stream)
                val chunkText = rec.getResult(stream).text.trim()

                if (chunkText.isNotBlank()) {
                    if (results.isNotEmpty()) results.append(" ")
                    results.append(chunkText)
                }
            }

            val text = results.toString().trim()
            if (text.isBlank()) {
                Result.failure(Exception("Kein Text erkannt"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Split audio samples into chunks at natural silence points.
     * Each chunk is up to 30 seconds. The split point is chosen where
     * the audio is quietest in a 3-second search window before the 30s mark.
     */
    private fun splitAtSilence(samples: FloatArray): List<FloatArray> {
        val maxChunkSamples = SAMPLE_RATE * MAX_CHUNK_SECONDS
        if (samples.size <= maxChunkSamples) return listOf(samples)

        val chunks = mutableListOf<FloatArray>()
        var offset = 0

        while (offset < samples.size) {
            val remaining = samples.size - offset
            if (remaining <= maxChunkSamples) {
                chunks.add(samples.copyOfRange(offset, samples.size))
                break
            }

            // Search for quietest point in window [27s..30s] from chunk start
            val searchStart = offset + SAMPLE_RATE * SEARCH_WINDOW_START_SECONDS
            val searchEnd = offset + maxChunkSamples
            val splitPoint = findQuietestPoint(samples, searchStart, searchEnd)

            chunks.add(samples.copyOfRange(offset, splitPoint))
            offset = splitPoint
        }

        return chunks
    }

    /**
     * Find the sample position with lowest energy (quietest moment)
     * using a sliding RMS window.
     */
    private fun findQuietestPoint(samples: FloatArray, searchStart: Int, searchEnd: Int): Int {
        val windowSamples = SAMPLE_RATE * SILENCE_WINDOW_MS / 1000 // 300ms window
        var bestPos = searchEnd
        var bestEnergy = Float.MAX_VALUE

        val start = searchStart.coerceIn(0, samples.size)
        val end = (searchEnd - windowSamples).coerceIn(start, samples.size)

        var pos = start
        while (pos < end) {
            var sum = 0f
            for (i in pos until minOf(pos + windowSamples, samples.size)) {
                sum += samples[i] * samples[i]
            }
            val energy = sum / windowSamples

            if (energy < bestEnergy) {
                bestEnergy = energy
                bestPos = pos + windowSamples / 2 // split at center of quiet window
            }
            pos += windowSamples / 4 // slide by 75ms steps for speed
        }

        return bestPos.coerceIn(0, samples.size)
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val MAX_CHUNK_SECONDS = 30
        private const val SEARCH_WINDOW_START_SECONDS = 25 // start searching 5s before max
        private const val SILENCE_WINDOW_MS = 300 // 300ms window for RMS energy
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
