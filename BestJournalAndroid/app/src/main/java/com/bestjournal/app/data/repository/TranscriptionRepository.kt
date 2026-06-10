package com.bestjournal.app.data.repository

import android.content.Context
import android.util.Log
import com.bestjournal.app.BuildConfig
import com.bestjournal.app.R
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.audio.SpeechAnalysis
import com.bestjournal.app.data.audio.WavSpeechAnalyzer
import com.bestjournal.app.data.local.whisper.LocalWhisperTranscriber
import com.bestjournal.app.data.remote.ai.AiPhase
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.groq.GroqApi
import com.bestjournal.app.data.remote.groq.GroqTranscriptionResponse
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.DeviceLocale
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class TranscriptionResult(
    val text: String,
    val model: String
)

@Singleton
class TranscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localWhisper: LocalWhisperTranscriber,
    private val groqApi: GroqApi,
    private val billingManager: BillingManager,
    private val aiUsageTracker: AiUsageTracker,
    private val speechAnalyzer: WavSpeechAnalyzer
) {
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionResult> {
        // ── Anti-Halluzinations-Kette (bugs/desktop/groq-transkription.md §2) ──────────
        // Whisper (Groq UND lokal) erfindet bei Stille Floskeln ("Vielen Dank",
        // "Untertitel des ZDF") und haengt nach End-Pausen Woerter an ("Ja"). Drei
        // funktionserhaltende Schichten — echte (auch leise/kurze) Sprache bleibt IMMER.

        // Schicht 1: Sprachgehalt-Vorfilter VOR allem. Eine Aufnahme mit < 150 ms echter
        // Sprache wird gar nicht transkribiert — weder via Groq noch lokal (beide
        // halluzinieren bei reiner Stille mit HOHER Confidence, die Schicht 2 nicht faengt).
        // analysis == null (Decode-Fehler) -> NICHT filtern, normal weiter (funktionserhaltend).
        val analysis = speechAnalyzer.analyze(audioFile)
        if (analysis != null && analysis.voicedMs < MIN_SPEECH_MS) {
            Log.d(
                "Transcription",
                "Schicht 1: Aufnahme ohne Sprachgehalt (${analysis.voicedMs} ms laut " +
                    "< $MIN_SPEECH_MS ms) — nicht transkribiert"
            )
            return Result.success(
                TranscriptionResult("", context.getString(R.string.transcription_model_groq))
            )
        }

        // Use Groq for subscribers AND trial users (free tier covers trial usage).
        // Debug builds always use Groq — mirrors Frank's behaviour so developers
        // can exercise the hosted whisper-large-v3-turbo path without juggling
        // subscription state on their test devices. Release builds keep the
        // paywall unchanged.
        val isSubscribed = billingManager.subscriptionState.value is SubscriptionState.Subscribed
        // Record usage day so trial clock starts even for voice-only users
        aiUsageTracker.recordUsageDay()
        val isTrialActive = aiUsageTracker.getCurrentPhase() == AiPhase.TRIAL
        val isDebug = BuildConfig.DEBUG
        if (isSubscribed || isTrialActive || isDebug) {
            try {
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                remoteConfig.fetchAndActivate().await()
                val groqKey = remoteConfig.getString(Constants.REMOTE_CONFIG_GROQ_KEY)

                // Groq API file size limit: 25 MB. Skip Groq for large files (~14+ min recordings)
                val fileSizeMb = audioFile.length() / (1024.0 * 1024.0)
                if (groqKey.isNotBlank() && fileSizeMb <= 25.0) {
                    val userType = when {
                        isSubscribed -> "premium subscriber"
                        isTrialActive -> "trial user"
                        else -> "debug build"
                    }
                    Log.d("Transcription", "Using Groq API for $userType (${String.format("%.1f", fileSizeMb)} MB)")
                    val filePart = MultipartBody.Part.createFormData(
                        "file", audioFile.name,
                        audioFile.asRequestBody("audio/wav".toMediaType())
                    )
                    val response = groqApi.transcribe(
                        authorization = "Bearer $groqKey",
                        file = filePart,
                        model = Constants.GROQ_TRANSCRIPTION_MODEL.toRequestBody("text/plain".toMediaType()),
                        language = DeviceLocale.languageCode.toRequestBody("text/plain".toMediaType()),
                        // Schicht 2: verbose_json liefert die Confidence-Felder — bei Groq
                        // ohne Mehrlatenz/-kosten. temperature=0 als Basis (kein Schutz allein).
                        responseFormat = "verbose_json".toRequestBody("text/plain".toMediaType()),
                        temperature = "0".toRequestBody("text/plain".toMediaType())
                    )
                    // Hatte Groq ueberhaupt Text? Wenn die Rohantwort leer ist, hat Groq nichts
                    // erkannt -> auf lokales Whisper zuruckfallen (wie bisher). Hatte Groq Text,
                    // greifen Schicht 2+3; ein danach leeres Ergebnis IST das Resultat (Stille/
                    // Halluzination erkannt) und faellt NICHT auf lokal zurueck — das wuerde die
                    // Stille nur erneut halluzinieren.
                    if (response.text.isNotBlank()) {
                        val filtered = filterTranscription(response, analysis)
                        return Result.success(
                            TranscriptionResult(filtered, context.getString(R.string.transcription_model_groq))
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("Transcription", "Groq failed, falling back to local Whisper: ${e.message}")
            }
        }

        // Fallback: local offline Whisper via sherpa-onnx
        // Used for: expired trial (freemium) users, OR when Groq API fails/rate-limited
        return localWhisper.transcribe(audioFile).map {
            TranscriptionResult(it, context.getString(R.string.transcription_model_local))
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
                    "Transcription",
                    "Schicht 2: Segment verworfen ('${seg.text.trim().take(40)}', " +
                        "no_speech=%.2f, logprob=%.2f, compression=%.2f)"
                            .format(seg.noSpeechProb, seg.avgLogprob, seg.compressionRatio)
                )
            }
            !drop
        }
        if (confident.isEmpty()) {
            Log.d("Transcription", "Schicht 2: alle ${segments.size} Segmente verworfen — leeres Ergebnis")
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
                        "Transcription",
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
                "Transcription",
                "Schicht 3: haette ALLE ${confident.size} Segmente verworfen — " +
                    "Timestamp-Drift vermutet, Confidence-Ergebnis behalten"
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
