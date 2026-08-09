package de.frank.experimente.audio

import java.util.logging.Logger
import kotlin.math.sqrt

class SpeechAnalysis(
    val voicedMs: Int,
    private val frameMs: Int,
    private val voicedFrames: BooleanArray,
) {
    fun segmentHasSpeech(
        startSec: Double,
        endSec: Double,
        minVoicedRatio: Double = MIN_VOICED_RATIO,
    ): Boolean {
        if (voicedFrames.isEmpty() || endSec <= startSec) return true
        val first = (startSec * 1_000.0 / frameMs).toInt().coerceIn(0, voicedFrames.lastIndex)
        val last = (endSec * 1_000.0 / frameMs).toInt().coerceIn(first, voicedFrames.lastIndex)
        var voicedCount = 0
        for (index in first..last) if (voicedFrames[index]) voicedCount++
        return voicedCount.toDouble() / (last - first + 1).toDouble() >= minVoicedRatio
    }

    private companion object {
        const val MIN_VOICED_RATIO = 0.10
    }
}

class SpeechAnalyzer {
    fun analyze(wav: ByteArray?): SpeechAnalysis? = runCatching { decode(wav) }
        .getOrElse { error ->
            logger.warning("PCM analysis failed open: ${error.message}")
            null
        }

    private fun decode(wav: ByteArray?): SpeechAnalysis? {
        if (wav == null || wav.size <= WAV_HEADER_BYTES + 4) return null

        var sampleRate = (wav[24].toInt() and 0xFF) or
            ((wav[25].toInt() and 0xFF) shl 8) or
            ((wav[26].toInt() and 0xFF) shl 16) or
            ((wav[27].toInt() and 0xFF) shl 24)
        if (sampleRate <= 0) sampleRate = FALLBACK_SAMPLE_RATE

        val samplesPerFrame = (sampleRate * FRAME_MS / 1_000).coerceAtLeast(1)
        val bytesPerFrame = samplesPerFrame * 2
        val frameCount = (wav.size - WAV_HEADER_BYTES) / bytesPerFrame
        if (frameCount <= 0) return null

        val voicedFrames = BooleanArray(frameCount)
        for (frame in 0 until frameCount) {
            val frameStart = WAV_HEADER_BYTES + frame * bytesPerFrame
            var sumSquares = 0.0
            for (sampleIndex in 0 until samplesPerFrame) {
                val byteIndex = frameStart + sampleIndex * 2
                val sample = (
                    (wav[byteIndex].toInt() and 0xFF) or
                        (wav[byteIndex + 1].toInt() shl 8)
                    ).toShort()
                val normalized = sample / 32_768.0
                sumSquares += normalized * normalized
            }
            voicedFrames[frame] = sqrt(sumSquares / samplesPerFrame) >= RMS_THRESHOLD
        }

        return SpeechAnalysis(
            voicedMs = voicedFrames.count { it } * FRAME_MS,
            frameMs = FRAME_MS,
            voicedFrames = voicedFrames,
        )
    }

    companion object {
        const val FRAME_MS = 20
        const val RMS_THRESHOLD = 0.015
        const val MIN_SPEECH_MS = 150

        private const val WAV_HEADER_BYTES = 44
        private const val FALLBACK_SAMPLE_RATE = 16_000
        private val logger = Logger.getLogger(SpeechAnalyzer::class.java.name)
    }
}
