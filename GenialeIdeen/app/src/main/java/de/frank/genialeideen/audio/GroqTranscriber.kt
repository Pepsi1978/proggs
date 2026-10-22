package de.frank.genialeideen.audio

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import de.frank.genialeideen.network.shutdownSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class GroqTranscriber(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val filterStille: Boolean = true,
    private val filterMetriken: Boolean = true,
    private val filterZeitstempel: Boolean = true,
    private val filterFloskeln: Boolean = true,
    /** Personal vocabulary prompt ([Woerterbuch.prompt]); null = send no prompt. */
    private val woerterbuchPrompt: String? = null,
) {
    private val analyzer = SpeechAnalyzer()
    private val filter = WhisperHallucinationFilter()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun transcribe(wav: ByteArray): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw GroqTranscriptionException("Groq ist auf diesem Geraet nicht konfiguriert.")
        }
        if (wav.size > MAX_FILE_BYTES) {
            throw GroqTranscriptionException("Die Aufnahme ist fuer die Groq-Transkription zu gross.")
        }

        val analysis = analyzer.analyze(wav)
        if (filterStille && analysis != null && analysis.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) {
            logger.info("Layer 1: recording rejected before upload (${analysis.voicedMs} ms voiced)")
            return@withContext ""
        }

        val gewaehlt = modellFuerAufnahme(model)
        try {
            transcribeOnce(wav, analysis, gewaehlt)
        } catch (e: ModelBlockedException) {
            // large-v3 is not enabled for this Groq organization: keep the dictation, retry once
            // with Turbo and use Turbo directly for the next 30 minutes.
            logger.warning("Groq: $gewaehlt blocked for this organization, falling back to $MODELL_TURBO")
            blockedUntilMs = System.currentTimeMillis() + BLOCKED_PAUSE_MS
            transcribeOnce(wav, analysis, MODELL_TURBO)
        }
    }

    private suspend fun transcribeOnce(wav: ByteArray, analysis: SpeechAnalysis?, model: String): String {
        val (roh, text) = request(wav, analysis, model, woerterbuchPrompt)
        if (Woerterbuch.istPromptEcho(roh, woerterbuchPrompt) || Woerterbuch.istPromptEcho(text, woerterbuchPrompt)) {
            // Whisper repeated the vocabulary prompt instead of transcribing: once more without it.
            logger.warning("Groq: prompt echo detected, retrying without vocabulary prompt")
            return request(wav, analysis, model, null).second
        }
        return text
    }

    /** Returns raw Whisper text and the filtered text. */
    private suspend fun request(
        wav: ByteArray,
        analysis: SpeechAnalysis?,
        model: String,
        prompt: String?,
    ): Pair<String, String> {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", wav.toRequestBody(WAV_MEDIA_TYPE))
            .addFormDataPart("model", model)
            .addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0")
        if (prompt != null) builder.addFormDataPart("prompt", prompt)
        val request = Request.Builder()
            .url(TRANSCRIPTIONS_URL)
            .header("Authorization", "Bearer $apiKey")
            .post(builder.build())
            .build()

        return client.newCall(request).await().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (model == MODELL_LARGE_V3 && istModellGesperrt(responseBody)) throw ModelBlockedException()
                throw GroqTranscriptionException(httpError(response.code, responseBody))
            }
            val parsed = runCatching { parseResponse(responseBody) }.getOrElse { error ->
                throw GroqTranscriptionException("Groq hat eine ungueltige Antwort geliefert.", error)
            }
            parsed.text to filter.filter(parsed, analysis, filterMetriken, filterZeitstempel, filterFloskeln)
        }
    }

    private class ModelBlockedException : Exception("model blocked for organization")

    internal fun parseResponse(body: String): GroqTranscriptionResponse {
        val json = JSONObject(body)
        val segments = json.optJSONArray("segments")?.let { array ->
            (0 until array.length()).map { index ->
                val segment = array.getJSONObject(index)
                GroqSegment(
                    start = segment.optDoubleOrNull("start"),
                    end = segment.optDoubleOrNull("end"),
                    text = segment.optString("text"),
                    noSpeechProbability = segment.optDoubleOrNull("no_speech_prob"),
                    averageLogProbability = segment.optDoubleOrNull("avg_logprob"),
                    compressionRatio = segment.optDoubleOrNull("compression_ratio"),
                )
            }
        }
        return GroqTranscriptionResponse(json.optString("text").trim(), segments)
    }

    fun shutdown() {
        client.shutdownSafely(logger)
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        optDouble(name).takeUnless(Double::isNaN)

    private fun httpError(status: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return when (status) {
            401, 403 -> "Der konfigurierte Groq-Schluessel wurde abgelehnt."
            413 -> "Die Aufnahme ist fuer die Groq-Transkription zu gross."
            429 -> "Groq hat zu viele Anfragen erhalten. Bitte versuche es spaeter erneut."
            in 500..599 -> "Groq ist derzeit nicht erreichbar. Bitte versuche es spaeter erneut."
            else -> detail?.takeIf(String::isNotBlank)?.let { "Groq-Fehler $status: $it" }
                ?: "Groq-Fehler $status."
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        GroqTranscriptionException(
                            "Groq ist ueber die aktuelle Verbindung nicht erreichbar.",
                            e,
                        ),
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) {
                    continuation.resume(response) { _, value, _ -> value.close() }
                } else {
                    response.close()
                }
            }
        })
    }

    companion object {
        internal const val TRANSCRIPTIONS_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions"
        internal const val MAX_FILE_BYTES = 25 * 1024 * 1024
        const val MODELL_TURBO = "whisper-large-v3-turbo"
        const val MODELL_LARGE_V3 = "whisper-large-v3"
        const val DEFAULT_MODEL = MODELL_TURBO

        /** Selectable Whisper models: id to display name. */
        val MODELLE = listOf(
            MODELL_TURBO to "Large V3 Turbo (schnell)",
            MODELL_LARGE_V3 to "Large V3 (genauer)",
        )

        /** Unknown values fall back to Turbo. */
        fun gueltigesModell(id: String?): String = if (id == MODELL_LARGE_V3) MODELL_LARGE_V3 else MODELL_TURBO

        private const val BLOCKED_PAUSE_MS = 30 * 60 * 1000L

        // After a "blocked at the organization level" answer use Turbo directly for 30 minutes.
        @Volatile private var blockedUntilMs = 0L

        private fun modellFuerAufnahme(model: String): String =
            if (model == MODELL_LARGE_V3 && System.currentTimeMillis() < blockedUntilMs) MODELL_TURBO else model

        private fun istModellGesperrt(body: String): Boolean =
            body.contains("model_permission_blocked_org") || body.contains("blocked at the organization level")

        private val WAV_MEDIA_TYPE = "audio/wav".toMediaType()
        private val logger = Logger.getLogger(GroqTranscriber::class.java.name)
    }
}

class GroqTranscriptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
