package com.bestjournal.app.data.audio

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Ergebnis der PCM-Analyse einer Aufnahme: wie viel ECHTE Sprache (laute Frames) steckt
 * drin und WO auf der Zeitachse sie liegt (Voiced-Timeline).
 *
 * Grundlage der Whisper-Anti-Halluzinations-Kette (bugs/desktop/groq-transkription.md §2):
 * - Schicht 1 nutzt [voicedMs] (sprach-arme Clips gar nicht erst an Groq senden).
 * - Schicht 3 nutzt [segmentHasSpeech] (halluzinierte Segmente haben keinen Schall in
 *   ihrem Zeitfenster — z.B. ein angehaengtes "Ja" nach der End-Pause).
 */
class SpeechAnalysis(
    /** Aufsummierte Dauer aller lauten 20-ms-Frames in Millisekunden. */
    val voicedMs: Int,
    private val frameMs: Int,
    private val voicedFrames: BooleanArray,
) {
    /**
     * Prueft ob im Zeitfenster [startSec]..[endSec] genug laute Frames liegen. Unter
     * [minVoicedRatio] (10 %) gilt das Segment als Stille-Halluzination. Leere Timeline
     * (Analyse-Luecke) -> true (funktionserhaltend: nie echten Text verlieren).
     */
    fun segmentHasSpeech(startSec: Double, endSec: Double, minVoicedRatio: Double = 0.10): Boolean {
        if (voicedFrames.isEmpty()) return true
        val first = (startSec * 1000.0 / frameMs).toInt().coerceIn(0, voicedFrames.lastIndex)
        val last = (endSec * 1000.0 / frameMs).toInt().coerceIn(first, voicedFrames.lastIndex)
        var voiced = 0
        for (i in first..last) if (voicedFrames[i]) voiced++
        val total = last - first + 1
        return voiced.toDouble() / total.toDouble() >= minVoicedRatio
    }
}

/**
 * Liest eine 16-bit-PCM-WAV-Aufnahme (wie [com.bestjournal.app.util.AudioRecorderHelper] sie
 * schreibt: 16 kHz mono, Standard-RIFF-Header) und misst den Sprachgehalt.
 *
 * WAV ist bereits unkomprimiertes PCM — anders als die m4a-Aufnahmen anderer Apps ist KEIN
 * MediaCodec-Decode noetig; der RIFF-Header wird direkt geparst. RMS wird pro 20-ms-Frame
 * gemessen; Frames mit RMS >= 0.015 (normalisiert auf -1..1) zaehlen als laut. Bei mehreren
 * Kanaelen wird nur Kanal 0 gemessen.
 *
 * Robustheit (Direktive #3): JEDER Fehler (kaputter Header, unerwartete Bittiefe, IO) liefert
 * null — der Aufrufer sendet dann ungefiltert an Groq (lieber einmal zu viel senden als echte
 * Sprache verwerfen).
 */
@Singleton
class WavSpeechAnalyzer @Inject constructor() {

    fun analyze(file: File): SpeechAnalysis? = runCatching { decode(file) }.getOrNull()

    private fun decode(file: File): SpeechAnalysis? {
        DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
            val riff = ByteArray(12)
            input.readFully(riff)
            if (String(riff, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                String(riff, 8, 4, Charsets.US_ASCII) != "WAVE"
            ) {
                return null
            }

            var sampleRate = 16_000
            var channels = 1
            var bits = 16

            val chunkHeader = ByteArray(8)
            while (true) {
                input.readFully(chunkHeader)
                val id = String(chunkHeader, 0, 4, Charsets.US_ASCII)
                val size = le32(chunkHeader, 4)
                if (size < 0) return null
                when (id) {
                    "fmt " -> {
                        val fmt = ByteArray(size)
                        input.readFully(fmt)
                        if (size >= 16) {
                            channels = le16(fmt, 2).coerceAtLeast(1)
                            sampleRate = le32(fmt, 4).coerceAtLeast(1)
                            bits = le16(fmt, 14)
                        }
                    }
                    "data" -> return measure(input, size, sampleRate, channels, bits)
                    else -> {
                        // Unbekannter Chunk (z.B. LIST) — ueberspringen (Padding auf gerade Laenge).
                        var left = size.toLong() + (size and 1)
                        while (left > 0) {
                            val skipped = input.skip(left)
                            if (skipped <= 0) return null
                            left -= skipped
                        }
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
    }

    private fun measure(
        input: DataInputStream,
        dataSize: Int,
        sampleRate: Int,
        channels: Int,
        bits: Int,
    ): SpeechAnalysis? {
        // AudioRecorderHelper schreibt immer 16-bit PCM. Andere Bittiefen -> nicht analysierbar
        // -> null (Aufrufer sendet ungefiltert).
        if (bits != 16) return null
        val ch = channels.coerceAtLeast(1)
        val samplesPerFrame = (sampleRate * FRAME_MS / 1000).coerceAtLeast(1)

        val voiced = ArrayList<Boolean>(512)
        var sumSquares = 0.0
        var samplesInFrame = 0

        val buffer = ByteArray(8192)
        var pending = false // halbes Sample am Block-Ende
        var pendingLo = 0
        var chPos = 0 // Kanal-Index im interleaved Stream (nur Kanal 0 wird gemessen)
        var bytesLeft = dataSize

        while (bytesLeft > 0) {
            val want = minOf(buffer.size, bytesLeft)
            val n = input.read(buffer, 0, want)
            if (n <= 0) break
            bytesLeft -= n
            var i = 0
            while (i < n) {
                val lo: Int
                val hi: Int
                if (pending) {
                    lo = pendingLo
                    hi = buffer[i].toInt() and 0xFF
                    i += 1
                    pending = false
                } else if (i + 1 >= n) {
                    pendingLo = buffer[i].toInt() and 0xFF
                    pending = true
                    i += 1
                    continue
                } else {
                    lo = buffer[i].toInt() and 0xFF
                    hi = buffer[i + 1].toInt() and 0xFF
                    i += 2
                }
                if (chPos == 0) {
                    val sample = (((hi shl 8) or lo).toShort()) / 32768.0
                    sumSquares += sample * sample
                    samplesInFrame++
                    if (samplesInFrame >= samplesPerFrame) {
                        voiced.add(sqrt(sumSquares / samplesInFrame) >= RMS_THRESHOLD)
                        sumSquares = 0.0
                        samplesInFrame = 0
                    }
                }
                chPos = (chPos + 1) % ch
            }
        }

        // Rest-Frame (< 20 ms) mitzaehlen, damit ein kurzes Wort am Ende nicht verloren geht.
        if (samplesInFrame > 0) {
            voiced.add(sqrt(sumSquares / samplesInFrame) >= RMS_THRESHOLD)
        }

        val frames = BooleanArray(voiced.size) { voiced[it] }
        val voicedMs = frames.count { it } * FRAME_MS
        return SpeechAnalysis(voicedMs = voicedMs, frameMs = FRAME_MS, voicedFrames = frames)
    }

    private fun le16(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)

    private fun le32(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)

    companion object {
        /** Frame-Raster der Voiced-Timeline (wie TVO/CVO/overlays/EntropieReductor). */
        const val FRAME_MS = 20

        /** RMS-Schwelle (normalisiert -1..1) ab der ein Frame als laut gilt. */
        const val RMS_THRESHOLD = 0.015
    }
}
