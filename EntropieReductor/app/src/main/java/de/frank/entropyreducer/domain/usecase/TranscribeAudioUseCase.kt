package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.audio.SpeechAnalysis
import de.frank.entropyreducer.data.audio.SpeechAnalyzer
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.data.remote.GroqWhisperApi
import de.frank.entropyreducer.data.remote.TranscriptionResponse
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Whisper-Transkription via Groq. Spec §8.2.
 *
 * Anti-Halluzinations-Kette (Frank-Wunsch 2026-06-10, portiert aus TVO/CVO/overlays —
 * bugs/desktop/groq-transkription.md §2): Whisper halluziniert bei Stille Floskeln
 * ("Vielen Dank", "Untertitel des ZDF") und haengt nach End-Pausen Woerter an ("Ja").
 * Drei funktionserhaltende Schichten:
 *
 *  1. Sprachgehalt-Vorfilter VOR dem Senden — Aufnahme dekodieren ([SpeechAnalyzer]),
 *     unter 150 ms echter Sprache gar nicht erst an Groq schicken (faengt REINE
 *     Stille, die Schicht 2 nicht fangen kann: Floskeln kommen mit HOHER Confidence).
 *  2. verbose_json-Confidence-Gate — pro Segment verwerfen bei
 *     no_speech_prob > 0.6 UND avg_logprob < -1.0 (UND-Logik schuetzt leise Sprache),
 *     compression_ratio > 2.4 (Repetition), Mini-Noise (< 0.4 s + no_speech > 0.6).
 *  3. Segment-Audio-Abgleich — Segment-Zeitfenster gegen die Voiced-Timeline der
 *     eigenen Aufnahme pruefen; < 10 % laute Frames = Trailing-Halluzination.
 *     Drift-Sicherung: verwirft Schicht 3 ALLE Segmente, die Schicht 2 liess,
 *     bleiben die Confidence-gefilterten erhalten (Whisper-Timestamps koennen driften
 *     — nie einen echten Satz verlieren).
 *
 * Leeres Ergebnis ("alles gefiltert") ist Result.success("") — die Aufrufer behandeln
 * Blank bereits als "nichts einfuegen". Jede Filter-Entscheidung wird im Diagnose-Log
 * (Bereich GROQ) protokolliert.
 */
class TranscribeAudioUseCase @Inject constructor(
    private val api: GroqWhisperApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val analyzer: SpeechAnalyzer,
    private val diag: DiagnosticLogger,
) {

    suspend operator fun invoke(audioFile: File): Result<String> {
        val key = secrets.groqApiKey
            ?: return Result.failure(IllegalStateException("Kein Groq-Key hinterlegt"))
        val model = settings.whisperModelFlow.first()
        val language = settings.transcriptionLanguageFlow.first()

        // Schicht 1: Sprachgehalt-Vorfilter. analysis == null (Decode-Fehler) ->
        // NICHT filtern, trotzdem senden (funktionserhaltend).
        val analysis = analyzer.analyze(audioFile)
        if (analysis != null && analysis.voicedMs < MIN_SPEECH_MS) {
            diag.info(
                DiagnosticArea.GROQ,
                "Schicht 1: Aufnahme ohne Sprachgehalt (${analysis.voicedMs} ms laut " +
                    "< $MIN_SPEECH_MS ms) — nicht an Groq gesendet",
            )
            audioFile.delete()
            return Result.success("")
        }

        val result = runCatching {
            val requestFile = audioFile.asRequestBody("audio/m4a".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val plain = "text/plain".toMediaType()
            val response = api.transcribe(
                bearer = "Bearer $key",
                file = filePart,
                model = model.toRequestBody(plain),
                language = language.toRequestBody(plain),
                // Schicht 2: verbose_json liefert die Confidence-Felder — bei Groq
                // ohne Mehrlatenz/-kosten. temperature=0 als Basis (kein Schutz allein).
                responseFormat = "verbose_json".toRequestBody(plain),
                temperature = "0".toRequestBody(plain),
            )
            filterTranscription(response, analysis)
        }
        // Spec §20: Audio nach erfolgreicher Transkription sofort löschen.
        // Bei Fehler behalten — sonst kann der Nutzer den Eintrag später nicht
        // erneut bewerten lassen. Tageweises Cache-Aufraeumen folgt mit Stufe 2.
        if (result.isSuccess) audioFile.delete()
        return result
    }

    /** Schicht 2 (Confidence-Gate) + Schicht 3 (Segment-Audio-Abgleich). */
    private fun filterTranscription(
        response: TranscriptionResponse,
        analysis: SpeechAnalysis?,
    ): String {
        val segments = response.segments
        if (segments.isEmpty()) {
            // Ultrakurze Clips liefern oft keine segments — top-level text durchreichen
            // (Schicht 1 hat reine Stille bereits abgefangen).
            return response.text.trim()
        }

        // Schicht 2: Confidence-Gate. UND-Logik (nie ODER) — echte leise Sprache hat
        // zwar erhoehtes no_speech_prob, aber gutes avg_logprob -> bleibt erhalten.
        val confident = segments.filter { seg ->
            val silence = seg.noSpeechProb > NO_SPEECH_THRESHOLD && seg.avgLogprob < AVG_LOGPROB_THRESHOLD
            val repetition = seg.compressionRatio > COMPRESSION_RATIO_THRESHOLD
            val miniNoise = (seg.end - seg.start) < MINI_NOISE_MAX_SEC && seg.noSpeechProb > NO_SPEECH_THRESHOLD
            val drop = silence || repetition || miniNoise
            if (drop) {
                diag.info(
                    DiagnosticArea.GROQ,
                    "Schicht 2: Segment verworfen ('${seg.text.trim().take(60)}', " +
                        "no_speech=%.2f, logprob=%.2f, compression=%.2f)"
                            .format(seg.noSpeechProb, seg.avgLogprob, seg.compressionRatio),
                )
            }
            !drop
        }
        if (confident.isEmpty()) {
            diag.info(DiagnosticArea.GROQ, "Schicht 2: alle ${segments.size} Segmente verworfen — leeres Ergebnis")
            return ""
        }

        // Schicht 3: Segment-Audio-Abgleich gegen die Voiced-Timeline der Aufnahme.
        val aligned = if (analysis == null) {
            confident
        } else {
            confident.filter { seg ->
                val hasSpeech = analysis.segmentHasSpeech(seg.start, seg.end)
                if (!hasSpeech) {
                    diag.warn(
                        DiagnosticArea.GROQ,
                        "Schicht 3: Segment ohne Schall im Zeitfenster verworfen " +
                            "('${seg.text.trim().take(60)}', %.1f–%.1f s)".format(seg.start, seg.end),
                    )
                }
                hasSpeech
            }
        }

        // Drift-Sicherung: Whisper-Timestamps koennen driften. Wuerde Schicht 3 ALLES
        // verwerfen, obwohl die Confidence-Pruefung Segmente liess, behalten wir die
        // Confidence-gefilterten — nie einen echten Satz verlieren.
        val kept = if (aligned.isEmpty()) {
            diag.warn(
                DiagnosticArea.GROQ,
                "Schicht 3: haette ALLE ${confident.size} Segmente verworfen — " +
                    "Timestamp-Drift vermutet, Confidence-Ergebnis behalten",
            )
            confident
        } else {
            aligned
        }

        return kept.joinToString(" ") { it.text.trim() }.trim()
    }

    companion object {
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
