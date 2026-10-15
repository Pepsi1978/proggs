package de.frank.jarvis.audio

import java.util.logging.Logger
import kotlin.math.sqrt

class SpeechAnalysis(
    val voicedMs: Int,
    val frameMs: Int,
    private val voicedFrames: BooleanArray,
    /** Effektivwert (0 bis 1) je Rahmen — für den Stille-Schnitt relativ zum Rauschboden. */
    private val rahmenRms: FloatArray = FloatArray(0),
) {
    /**
     * Schwelle für den Stille-Schnitt, relativ zum Rauschboden dieser Aufnahme: das 10. Perzentil der
     * Rahmen-Pegel mal [SCHNITT_FAKTOR], mindestens [SCHNITT_MIN]. Nie strenger als die feste
     * Sprach-Schwelle [SpeechAnalyzer.RMS_THRESHOLD] — sonst fielen leise Wortanfänge und Satzenden
     * aus der Ferne weg, die nur knapp über dem Rauschen liegen.
     */
    fun schnittSchwelle(): Double {
        if (rahmenRms.isEmpty()) return SpeechAnalyzer.RMS_THRESHOLD
        val sortiert = rahmenRms.copyOf().also { it.sort() }
        val boden = sortiert[(sortiert.size / 10).coerceAtMost(sortiert.lastIndex)].toDouble()
        return maxOf(boden * SCHNITT_FAKTOR, SCHNITT_MIN).coerceAtMost(SpeechAnalyzer.RMS_THRESHOLD)
    }

    /** Index des ersten Rahmens mit Pegel ab [schwelle], -1 ohne. Ohne Pegelwerte: der erste stimmhafte. */
    fun ersterFrameUeber(schwelle: Double): Int =
        if (rahmenRms.isEmpty()) voicedFrames.indexOfFirst { it } else rahmenRms.indexOfFirst { it >= schwelle }

    /** Index des letzten Rahmens mit Pegel ab [schwelle], -1 ohne. Ohne Pegelwerte: der letzte stimmhafte. */
    fun letzterFrameUeber(schwelle: Double): Int =
        if (rahmenRms.isEmpty()) voicedFrames.indexOfLast { it } else rahmenRms.indexOfLast { it >= schwelle }

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

        /** Schnitt-Schwelle = Rauschboden × Faktor (etwa +6 dB). */
        const val SCHNITT_FAKTOR = 2.0

        /** Untergrenze der Schnitt-Schwelle (etwa -48 dBFS), damit reines Rauschen nicht als Ton zählt. */
        const val SCHNITT_MIN = 0.004
    }
}

class SpeechAnalyzer {
    /**
     * Mit [stimmhaft] (Zeitachse der Spracherkennung, je [SprachZeitachse.RAHMEN_MS]) zählt nur erkannte Stimme als
     * Sprache, nicht der Pegel: Fahrgeräusch liegt über [RMS_THRESHOLD] und hätte sonst jeden Rahmen „stimmhaft“
     * gemacht. Der Schnitt richtet sich dann ebenfalls nach der Stimme (ohne Pegelwerte, siehe [SpeechAnalysis]).
     */
    fun analyze(wav: ByteArray?, stimmhaft: BooleanArray? = null): SpeechAnalysis? {
        if (stimmhaft != null && wav != null && wav.size > WAV_HEADER_BYTES + 4) {
            return SpeechAnalysis(
                voicedMs = stimmhaft.count { it } * SprachZeitachse.RAHMEN_MS,
                frameMs = SprachZeitachse.RAHMEN_MS,
                voicedFrames = stimmhaft,
            )
        }
        return analyzePegel(wav)
    }

    private fun analyzePegel(wav: ByteArray?): SpeechAnalysis? = runCatching { decode(wav) }
        .getOrElse { error ->
            logger.warning("PCM analysis failed open: ${error.message}")
            null
        }

    private fun decode(wav: ByteArray?): SpeechAnalysis? {
        if (wav == null || wav.size <= WAV_HEADER_BYTES + 4) return null

        val sampleRate = abtastrate(wav)

        val samplesPerFrame = (sampleRate * FRAME_MS / 1_000).coerceAtLeast(1)
        val bytesPerFrame = samplesPerFrame * 2
        val frameCount = (wav.size - WAV_HEADER_BYTES) / bytesPerFrame
        if (frameCount <= 0) return null

        val voicedFrames = BooleanArray(frameCount)
        val rahmenRms = FloatArray(frameCount)
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
            val rms = sqrt(sumSquares / samplesPerFrame)
            rahmenRms[frame] = rms.toFloat()
            voicedFrames[frame] = rms >= RMS_THRESHOLD
        }

        return SpeechAnalysis(
            voicedMs = voicedFrames.count { it } * FRAME_MS,
            frameMs = FRAME_MS,
            voicedFrames = voicedFrames,
            rahmenRms = rahmenRms,
        )
    }

    companion object {
        const val FRAME_MS = 20
        const val RMS_THRESHOLD = 0.015
        const val MIN_SPEECH_MS = 150

        private const val WAV_HEADER_BYTES = 44
        private const val FALLBACK_SAMPLE_RATE = 16_000
        private val logger = Logger.getLogger(SpeechAnalyzer::class.java.name)

        /** Die Abtastrate aus dem WAV-Kopf (Byte 24–27); manche Geräte nehmen mit 44,1 kHz auf. */
        fun abtastrate(wav: ByteArray): Int {
            if (wav.size < 28) return FALLBACK_SAMPLE_RATE
            val rate = (wav[24].toInt() and 0xFF) or
                ((wav[25].toInt() and 0xFF) shl 8) or
                ((wav[26].toInt() and 0xFF) shl 16) or
                ((wav[27].toInt() and 0xFF) shl 24)
            return if (rate <= 0) FALLBACK_SAMPLE_RATE else rate
        }
    }
}
