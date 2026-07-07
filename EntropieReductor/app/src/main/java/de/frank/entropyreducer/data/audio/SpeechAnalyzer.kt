package de.frank.entropyreducer.data.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Ergebnis der PCM-Analyse einer Aufnahme: wie viel ECHTE Sprache (laute Frames)
 * steckt drin, und WO auf der Zeitachse sie liegt (Voiced-Timeline).
 *
 * Grundlage der Whisper-Anti-Halluzinations-Kette (bugs/desktop/groq-transkription.md §2):
 * - Schicht 1 nutzt [voicedMs] (sprach-arme Clips gar nicht erst an Groq senden).
 * - Schicht 3 nutzt [segmentHasSpeech] (halluzinierte Segmente haben keinen Schall
 *   in ihrem Zeitfenster — z.B. ein angehaengtes "Ja" nach der End-Pause).
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
     * Leere Timeline (Analyse-Luecke) -> true (funktionserhaltend: nie echten Text verlieren).
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
 * Dekodiert eine M4A/AAC-Aufnahme zu PCM und misst den Sprachgehalt.
 *
 * Android-Pendant zu `AudioContext.decodeAudioData` der Chrome-Erweiterung bzw. zur
 * direkten WAV-PCM-Analyse der Desktop-Overlays: das MediaRecorder-File ist komprimiert,
 * darum laeuft hier MediaExtractor + MediaCodec (synchroner Decode-Loop). RMS wird pro
 * 20-ms-Frame gemessen; Frames mit RMS >= 0.015 (normalisiert auf -1..1) zaehlen als laut.
 *
 * Robustheit (Direktive #3): JEDER Fehler (kaputtes File, Codec-Problem) liefert null —
 * der Aufrufer sendet dann ungefiltert an Groq (lieber einmal zu viel senden als echte
 * Sprache verwerfen).
 */
@Singleton
class SpeechAnalyzer @Inject constructor() {

    fun analyze(file: File): SpeechAnalysis? = runCatching { decode(file) }.getOrNull()

    private fun decode(file: File): SpeechAnalysis? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    extractor.selectTrack(i)
                    trackFormat = f
                    break
                }
            }
            val format = trackFormat ?: return null
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            var sampleRate = format.optInt(MediaFormat.KEY_SAMPLE_RATE, 16_000)
            var channels = format.optInt(MediaFormat.KEY_CHANNEL_COUNT, 1)
            var samplesPerFrame = (sampleRate * FRAME_MS / 1000).coerceAtLeast(1)

            val voiced = ArrayList<Boolean>(512)
            var sumSquares = 0.0
            var samplesInFrame = 0

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            // Sicherung gegen Endlosschleife bei Codec-Haengern: nach zu vielen
            // Leer-Durchlaeufen ohne Fortschritt abbrechen (-> null -> ungefiltert senden).
            var idleSpins = 0

            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex) ?: return null
                        val size = extractor.readSampleData(inBuf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outIndex >= 0 -> {
                        idleSpins = 0
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null && bufferInfo.size > 0) {
                            // 16-bit PCM little-endian; bei Mehrkanal nur Kanal 0 messen.
                            val shorts = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            var s = 0
                            while (s < shorts.limit()) {
                                val sample = shorts.get(s) / 32768.0
                                sumSquares += sample * sample
                                samplesInFrame++
                                if (samplesInFrame >= samplesPerFrame) {
                                    val rms = sqrt(sumSquares / samplesInFrame)
                                    voiced.add(rms >= RMS_THRESHOLD)
                                    sumSquares = 0.0
                                    samplesInFrame = 0
                                }
                                s += channels
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = codec.outputFormat
                        sampleRate = newFormat.optInt(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        channels = newFormat.optInt(MediaFormat.KEY_CHANNEL_COUNT, channels)
                        samplesPerFrame = (sampleRate * FRAME_MS / 1000).coerceAtLeast(1)
                    }
                    else -> {
                        idleSpins++
                        if (idleSpins > MAX_IDLE_SPINS) return null
                    }
                }
            }

            // Rest-Frame (< 20 ms) mitzaehlen, damit ein kurzes Wort am Ende nicht verloren geht.
            if (samplesInFrame > 0) {
                val rms = sqrt(sumSquares / samplesInFrame)
                voiced.add(rms >= RMS_THRESHOLD)
            }

            val frames = BooleanArray(voiced.size) { voiced[it] }
            val voicedMs = frames.count { it } * FRAME_MS
            return SpeechAnalysis(voicedMs = voicedMs, frameMs = FRAME_MS, voicedFrames = frames)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun MediaFormat.optInt(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    companion object {
        /** Frame-Raster der Voiced-Timeline (wie TVO/CVO/overlays). */
        const val FRAME_MS = 20

        /** RMS-Schwelle (normalisiert -1..1) ab der ein Frame als laut gilt. */
        const val RMS_THRESHOLD = 0.015

        private const val TIMEOUT_US = 10_000L
        private const val MAX_IDLE_SPINS = 500
    }
}
