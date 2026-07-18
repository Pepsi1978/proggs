package de.frank.karteikartenlernen.transcription

import kotlin.math.sqrt

class SpeechAnalysis(
    val voicedMs: Int,
    private val frameMs: Int,
    private val voicedFrames: BooleanArray,
) {
    fun segmentHasSpeech(startSec: Double, endSec: Double, minVoicedRatio: Double = 0.10): Boolean {
        if (voicedFrames.isEmpty() || endSec <= startSec) return true
        val first = (startSec * 1000.0 / frameMs).toInt().coerceIn(0, voicedFrames.lastIndex)
        val last = (endSec * 1000.0 / frameMs).toInt().coerceIn(first, voicedFrames.lastIndex)
        var voiced = 0
        for (index in first..last) if (voicedFrames[index]) voiced++
        return voiced.toDouble() / (last - first + 1).toDouble() >= minVoicedRatio
    }
}

class SpeechAnalyzer {
    fun analyze(wav: ByteArray?): SpeechAnalysis? = runCatching { decode(wav) }.getOrNull()

    private fun decode(wav: ByteArray?): SpeechAnalysis? {
        if (wav == null || wav.size <= WAV_HEADER_BYTES + 4) return null

        var sampleRate = (wav[24].toInt() and 0xFF) or
            ((wav[25].toInt() and 0xFF) shl 8) or
            ((wav[26].toInt() and 0xFF) shl 16) or
            ((wav[27].toInt() and 0xFF) shl 24)
        if (sampleRate <= 0) sampleRate = DEFAULT_SAMPLE_RATE

        val frameSamples = (sampleRate * FRAME_MS / 1000).coerceAtLeast(1)
        val frameBytes = frameSamples * 2
        val frameCount = (wav.size - WAV_HEADER_BYTES) / frameBytes
        if (frameCount <= 0) return null

        val voicedFrames = BooleanArray(frameCount)
        for (frame in 0 until frameCount) {
            val base = WAV_HEADER_BYTES + frame * frameBytes
            var sumSquares = 0.0
            for (sampleIndex in 0 until frameSamples) {
                val index = base + sampleIndex * 2
                val sample = ((wav[index].toInt() and 0xFF) or (wav[index + 1].toInt() shl 8)).toShort()
                val normalized = sample / 32768.0
                sumSquares += normalized * normalized
            }
            voicedFrames[frame] = sqrt(sumSquares / frameSamples) >= RMS_THRESHOLD
        }

        return SpeechAnalysis(
            voicedMs = voicedFrames.count { it } * FRAME_MS,
            frameMs = FRAME_MS,
            voicedFrames = voicedFrames,
        )
    }

    companion object {
        private const val WAV_HEADER_BYTES = 44
        private const val DEFAULT_SAMPLE_RATE = 16_000
        const val FRAME_MS = 20
        const val RMS_THRESHOLD = 0.015
        const val MIN_SPEECH_MS = 150
    }
}
