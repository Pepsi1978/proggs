package de.frank.cortex.audio

import de.frank.cortex.observability.CortexLog
import kotlin.math.sqrt

/**
 * Ergebnis der PCM-Analyse einer Aufnahme: wie viel ECHTE Sprache (laute Frames)
 * steckt drin, und WO auf der Zeitachse sie liegt (Voiced-Timeline).
 *
 * Grundlage der Whisper-Anti-Halluzinations-Kette (bugs/desktop/groq-transkription.md §2):
 * - Schicht 1 nutzt [voicedMs] (sprach-arme Clips gar nicht erst an Groq senden).
 * - Schicht 3 nutzt [segmentHasSpeech] (halluzinierte Segmente haben keinen Schall
 *   in ihrem Zeitfenster — z.B. ein angehaengtes "Ja" nach der End-Pause).
 * - Schicht 4 nutzt [voicedMs] fuer den Stille-Kontext der Floskel-Blocklist.
 */
class SpeechAnalysis(
    /** Aufsummierte Dauer aller lauten 20-ms-Frames in Millisekunden. */
    val voicedMs: Int,
    private val frameMs: Int,
    private val voicedFrames: BooleanArray,
) {
    /**
     * Prueft ob im Zeitfenster [startSec]..[endSec] genug laute Frames liegen.
     * Unter [minVoicedRatio] (10 %) gilt das Segment als Stille-Halluzination.
     * Leere/ungueltige Timeline (Analyse-Luecke) -> true (funktionserhaltend: nie echten Text verlieren).
     */
    fun segmentHasSpeech(startSec: Double, endSec: Double, minVoicedRatio: Double = 0.10): Boolean {
        if (voicedFrames.isEmpty()) return true
        if (!(endSec > startSec)) return true
        val first = (startSec * 1000.0 / frameMs).toInt().coerceIn(0, voicedFrames.lastIndex)
        val last = (endSec * 1000.0 / frameMs).toInt().coerceIn(first, voicedFrames.lastIndex)
        var voiced = 0
        for (i in first..last) if (voicedFrames[i]) voiced++
        val total = last - first + 1
        return voiced.toDouble() / total.toDouble() >= minVoicedRatio
    }
}

/**
 * Misst den Sprachgehalt einer rohen 16-bit-PCM-mono-WAV (wie [MicRecorder] sie liefert:
 * 16 kHz, mono, 44-Byte-Header). Anders als das Android-Pendant der Entropie-App (M4A/AAC
 * via MediaCodec) ist Cortex-Audio bereits unkomprimiertes PCM — der RMS pro 20-ms-Frame
 * wird direkt aus den WAV-Bytes gelesen, ohne Decoder (schneller + crashsicher).
 *
 * Robustheit (Direktive #3): JEDER Fehler (zu kurz, kaputter Header) liefert null — der
 * Aufrufer sendet dann ungefiltert an Groq (lieber einmal zu viel senden als echte Sprache
 * verwerfen). Die Sample-Rate wird aus dem Header (Bytes 24-27) gelesen; Frame-Raster,
 * RMS-Schwelle und Ratio sind 1:1 die Werte der Desktop-Overlays (TVO/CVO).
 */
class SpeechAnalyzer {

    fun analyze(wav: ByteArray?): SpeechAnalysis? =
        runCatching { decode(wav) }.getOrElse {
            CortexLog.warn("SpeechAnalyzer", "analyze", "PCM-Analyse fehlgeschlagen: ${it.message}")
            null
        }

    private fun decode(wav: ByteArray?): SpeechAnalysis? {
        if (wav == null || wav.size <= HEADER_SIZE + 4) return null

        // Sample-Rate aus dem WAV-Header (little-endian, Bytes 24-27); Fallback 16 kHz.
        var sampleRate = (wav[24].toInt() and 0xFF) or
            ((wav[25].toInt() and 0xFF) shl 8) or
            ((wav[26].toInt() and 0xFF) shl 16) or
            ((wav[27].toInt() and 0xFF) shl 24)
        if (sampleRate <= 0) sampleRate = 16_000

        val frameSamples = (sampleRate * FRAME_MS / 1000).coerceAtLeast(1)
        val frameBytes = frameSamples * 2 // 16-bit mono
        val frameCount = (wav.size - HEADER_SIZE) / frameBytes
        if (frameCount <= 0) return null

        val voiced = BooleanArray(frameCount)
        for (f in 0 until frameCount) {
            val base = HEADER_SIZE + f * frameBytes
            var sumSq = 0.0
            for (s in 0 until frameSamples) {
                val idx = base + s * 2
                val sample = ((wav[idx].toInt() and 0xFF) or (wav[idx + 1].toInt() shl 8)).toShort()
                val v = sample / 32768.0
                sumSq += v * v
            }
            voiced[f] = sqrt(sumSq / frameSamples) >= RMS_THRESHOLD
        }

        val voicedMs = voiced.count { it } * FRAME_MS
        return SpeechAnalysis(voicedMs = voicedMs, frameMs = FRAME_MS, voicedFrames = voiced)
    }

    companion object {
        /** WAV-Header von [MicRecorder.pcmDataToWav] (RIFF/fmt/data, ohne Extra-Chunks). */
        private const val HEADER_SIZE = 44

        /** Frame-Raster der Voiced-Timeline (wie TVO/CVO/overlays). */
        const val FRAME_MS = 20

        /** RMS-Schwelle (normalisiert -1..1) ab der ein Frame als laut gilt. */
        const val RMS_THRESHOLD = 0.015

        /** Schicht 1: min. absolute laute Zeit (KEINE Ratio — Denkpausen erlaubt). */
        const val MIN_SPEECH_MS = 150
    }
}
