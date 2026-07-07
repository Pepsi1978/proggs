package com.entropyjournal.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.entropyjournal.data.audio.SpeechAnalysis
import com.entropyjournal.data.audio.WavSpeechAnalyzer
import com.entropyjournal.data.local.whisper.LocalWhisperTranscriber
import com.entropyjournal.data.remote.groq.GroqApi
import com.entropyjournal.data.remote.groq.GroqTranscriptionRequest
import com.entropyjournal.data.remote.groq.GroqTranscriptionResponse
import com.entropyjournal.util.Constants
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class TranscriptionEngine { GROQ, LOCAL }

data class TranscriptionOutcome(
    val text: String,
    val engine: TranscriptionEngine,
    // Set when Groq was configured and tried but failed, and Local handled the audio.
    // Callers can surface this so the user knows the fallback kicked in.
    val groqError: String? = null,
)

@Singleton
class TranscriptionRepository @Inject constructor(
    private val groqApi: GroqApi,
    private val localWhisper: LocalWhisperTranscriber,
    private val encryptedPrefs: SharedPreferences,
    private val speechAnalyzer: WavSpeechAnalyzer
) {
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionOutcome> {
        // ── Anti-Halluzinations-Kette (bugs/desktop/groq-transkription.md §2) ──────────
        // Whisper (Groq large-v3-turbo UND lokales sherpa) erfindet bei Stille Floskeln
        // ("Vielen Dank", "Untertitel des ZDF") und haengt nach End-Pausen Woerter an ("Ja").
        // Drei funktionserhaltende Schichten — echte (auch leise/kurze) Sprache bleibt IMMER.

        // Schicht 1: Sprachgehalt-Vorfilter VOR allem. Eine Aufnahme mit < 150 ms echter
        // Sprache wird gar nicht transkribiert — weder via Groq noch lokal (beide halluzinieren
        // bei reiner Stille mit HOHER Confidence, die Schicht 2 nicht faengt). analysis == null
        // (Decode-Fehler) -> NICHT filtern, normal weiter (funktionserhaltend).
        val analysis = speechAnalyzer.analyze(audioFile)
        if (analysis != null && analysis.voicedMs < MIN_SPEECH_MS) {
            Log.d(
                TAG,
                "Schicht 1: Aufnahme ohne Sprachgehalt (${analysis.voicedMs} ms laut " +
                    "< $MIN_SPEECH_MS ms) — nicht transkribiert"
            )
            return Result.success(TranscriptionOutcome(text = "", engine = TranscriptionEngine.GROQ))
        }

        val groqKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: ""

        // Try Groq whisper-large-v3-turbo first if the user configured a key.
        var groqFailureReason: String? = null
        if (groqKey.isNotBlank()) {
            try {
                val response = groqApi.transcribe(
                    file = GroqTranscriptionRequest.createFilePart(audioFile),
                    model = GroqTranscriptionRequest.createModelPart(),
                    language = GroqTranscriptionRequest.createLanguagePart(),
                    responseFormat = GroqTranscriptionRequest.createResponseFormatPart(),
                    temperature = GroqTranscriptionRequest.createTemperaturePart()
                )
                // Hatte Groq ueberhaupt Text? Leere Rohantwort -> Groq hat nichts erkannt ->
                // auf lokales Whisper zuruckfallen (wie bisher). Hatte Groq Text, greifen
                // Schicht 2+3; ein danach leeres Ergebnis IST das Resultat (Stille/Halluzination
                // erkannt) und faellt NICHT auf lokal zurueck — das wuerde die Stille erneut halluzinieren.
                if (response.text.isNotBlank()) {
                    val filtered = filterTranscription(response, analysis)
                    return Result.success(
                        TranscriptionOutcome(text = filtered, engine = TranscriptionEngine.GROQ)
                    )
                }
            } catch (e: Exception) {
                // Never swallow silently (Direktive #3) — log and remember the reason
                // so the caller can tell the user why Groq was skipped.
                groqFailureReason = e.message ?: e.javaClass.simpleName
                Log.w(TAG, "Groq transcription failed, falling back to local Whisper", e)
            }
        }

        // Fallback: Local Whisper via sherpa-onnx.
        return localWhisper.transcribe(audioFile).map { text ->
            TranscriptionOutcome(
                text = text,
                engine = TranscriptionEngine.LOCAL,
                groqError = groqFailureReason,
            )
        }
    }

    /**
     * Schicht 2 (Confidence-Gate) + Schicht 3 (Segment-Audio-Abgleich) der Anti-
     * Halluzinations-Kette (bugs/desktop/groq-transkription.md §2.3). Beide funktionserhaltend.
     */
    private fun filterTranscription(
        response: GroqTranscriptionResponse,
        analysis: SpeechAnalysis?
    ): String {
        val segments = response.segments
        if (segments.isEmpty()) {
            // Ultrakurze Clips liefern oft keine segments — top-level text durchreichen
            // (Schicht 1 hat reine Stille bereits abgefangen).
            return response.text.trim()
        }

        // Schicht 2: Confidence-Gate. UND-Logik (nie ODER) — echte leise Sprache hat zwar
        // erhoehtes no_speech_prob, aber gutes avg_logprob -> bleibt erhalten.
        val confident = segments.filter { seg ->
            val silence = seg.noSpeechProb > NO_SPEECH_THRESHOLD && seg.avgLogprob < AVG_LOGPROB_THRESHOLD
            val repetition = seg.compressionRatio > COMPRESSION_RATIO_THRESHOLD
            val miniNoise = (seg.end - seg.start) < MINI_NOISE_MAX_SEC && seg.noSpeechProb > NO_SPEECH_THRESHOLD
            val drop = silence || repetition || miniNoise
            if (drop) {
                Log.d(
                    TAG,
                    "Schicht 2: Segment verworfen ('${seg.text.trim().take(40)}', " +
                        "no_speech=%.2f, logprob=%.2f, compression=%.2f)"
                            .format(seg.noSpeechProb, seg.avgLogprob, seg.compressionRatio)
                )
            }
            !drop
        }
        if (confident.isEmpty()) {
            Log.d(TAG, "Schicht 2: alle ${segments.size} Segmente verworfen — leeres Ergebnis")
            return ""
        }

        // Schicht 3: Segment-Audio-Abgleich gegen die Voiced-Timeline der Aufnahme.
        val aligned = if (analysis == null) {
            confident
        } else {
            confident.filter { seg ->
                val hasSpeech = analysis.segmentHasSpeech(seg.start, seg.end)
                if (!hasSpeech) {
                    Log.w(
                        TAG,
                        "Schicht 3: Segment ohne Schall im Zeitfenster verworfen " +
                            "('${seg.text.trim().take(40)}', %.1f–%.1f s)".format(seg.start, seg.end)
                    )
                }
                hasSpeech
            }
        }

        // Drift-Sicherung: Whisper-Timestamps koennen driften. Wuerde Schicht 3 ALLES
        // verwerfen, obwohl die Confidence-Pruefung Segmente liess, behalten wir die
        // Confidence-gefilterten — nie einen echten Satz verlieren.
        val kept = if (aligned.isEmpty()) {
            Log.w(
                TAG,
                "Schicht 3: haette ALLE ${confident.size} Segmente verworfen — " +
                    "Timestamp-Drift vermutet, Confidence-Ergebnis behalten"
            )
            confident
        } else {
            aligned
        }

        return kept.joinToString(" ") { it.text.trim() }.trim()
    }

    private companion object {
        private const val TAG = "TranscriptionRepo"

        /** Schicht 1: min. absolute laute Zeit (wie TVO/CVO/overlays; KEINE Ratio — Denkpausen erlaubt). */
        private const val MIN_SPEECH_MS = 150

        /** Schicht 2: Stille-Gate nur in UND-Kombination mit [AVG_LOGPROB_THRESHOLD]. */
        private const val NO_SPEECH_THRESHOLD = 0.6
        private const val AVG_LOGPROB_THRESHOLD = -1.0

        /** Schicht 2: Repetitions-Halluzination ("danke danke danke …"). */
        private const val COMPRESSION_RATIO_THRESHOLD = 2.4

        /** Schicht 2: Mini-Noise — sehr kurze Segmente mit Stille-Verdacht. */
        private const val MINI_NOISE_MAX_SEC = 0.4
    }
}
