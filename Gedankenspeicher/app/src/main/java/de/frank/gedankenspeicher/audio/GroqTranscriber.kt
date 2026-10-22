package de.frank.gedankenspeicher.audio

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import de.frank.gedankenspeicher.network.shutdownSafely
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
        if (analysis != null && analysis.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) {
            logger.info("Layer 1: recording rejected before upload (${analysis.voicedMs} ms voiced)")
            return@withContext ""
        }

        var aktuellesModell = modellFuerAufnahme(model)
        val text = try {
            sendeMitEchoSchutz(wav, analysis, aktuellesModell)
        } catch (gesperrt: ModellGesperrtException) {
            // large-v3 ist fuer die Groq-Organisation nicht freigeschaltet: Aufnahme nicht
            // verlieren, sofort mit Turbo wiederholen und 30 Minuten direkt Turbo nehmen.
            logger.warning("Model $aktuellesModell blocked for organization, falling back to $TURBO")
            gesperrtBisMs = System.currentTimeMillis() + SPERRE_MS
            aktuellesModell = TURBO
            sendeMitEchoSchutz(wav, analysis, aktuellesModell)
        }
        text
    }

    private suspend fun sendeMitEchoSchutz(
        wav: ByteArray,
        analysis: SpeechAnalysis?,
        modell: String,
    ): String {
        val ergebnis = sendeEinmal(wav, analysis, modell, woerterbuchPrompt)
        // Whisper gibt bei Stille/Gemurmel gern den Prompt selbst aus. Dann einmal ohne
        // Woerterbuch wiederholen statt das Diktat zu verlieren.
        if (isPromptEcho(ergebnis.roh, woerterbuchPrompt) || isPromptEcho(ergebnis.text, woerterbuchPrompt)) {
            logger.warning("Prompt echo detected, retrying without vocabulary prompt")
            return sendeEinmal(wav, analysis, modell, null).text
        }
        return ergebnis.text
    }

    private class Ergebnis(val roh: String, val text: String)

    private class ModellGesperrtException : Exception("model_permission_blocked_org")

    private suspend fun sendeEinmal(
        wav: ByteArray,
        analysis: SpeechAnalysis?,
        modell: String,
        prompt: String?,
    ): Ergebnis {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", wav.toRequestBody(WAV_MEDIA_TYPE))
            .addFormDataPart("model", modell)
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
                if (modell == LARGE_V3 && istModellSperre(responseBody)) {
                    throw ModellGesperrtException()
                }
                throw GroqTranscriptionException(httpError(response.code, responseBody))
            }
            val parsed = runCatching { parseResponse(responseBody) }.getOrElse { error ->
                throw GroqTranscriptionException("Groq hat eine ungueltige Antwort geliefert.", error)
            }
            Ergebnis(parsed.text, filter.filter(parsed, analysis))
        }
    }

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
        const val TURBO = "whisper-large-v3-turbo"
        const val LARGE_V3 = "whisper-large-v3"
        const val DEFAULT_MODEL = TURBO

        /** Vorbelegung des persoenlichen Woerterbuchs (Stand TerminalVoiceOverlay). */
        const val STANDARD_WOERTERBUCH =
            "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, " +
                "Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, " +
                "Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic"

        private const val MAX_BEGRIFFE = 100
        private const val MAX_PROMPT_ZEICHEN = 350
        private const val ECHO_MARKER = "Begriffe"
        private const val SPERRE_MS = 30 * 60 * 1000L

        @Volatile
        private var gesperrtBisMs = 0L

        /** Nur die zwei bekannten Modelle; alles andere wird Turbo. */
        fun normalisiereModell(modell: String?): String = if (modell == LARGE_V3) LARGE_V3 else TURBO

        /** Gewaehltes Modell fuer diese Aufnahme; large-v3 wird nach einer Sperre voruebergehend durch Turbo ersetzt. */
        fun modellFuerAufnahme(modell: String): String {
            val gewaehlt = normalisiereModell(modell)
            return if (gewaehlt == LARGE_V3 && System.currentTimeMillis() < gesperrtBisMs) TURBO else gewaehlt
        }

        /** Nach einer bewussten Modellwahl die Sperre vergessen, damit large-v3 erneut versucht wird. */
        fun sperreZuruecksetzen() {
            gesperrtBisMs = 0L
        }

        internal fun istModellSperre(body: String): Boolean =
            body.contains("model_permission_blocked_org") ||
                body.contains("blocked at the organization level")

        /** Begriffe aus dem Textfeld: getrennt an Zeilenumbruch, Komma, Semikolon. */
        fun begriffeAus(text: String): List<String> {
            val gesehen = HashSet<String>()
            return text.split('\n', '\r', ',', ';')
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .filter { gesehen.add(it.lowercase()) }
                .take(MAX_BEGRIFFE)
        }

        /** Woerterbuch als natuerlicher Satz; Whisper uebernimmt daraus Schreibweisen. */
        fun woerterbuchPrompt(begriffe: List<String>): String? {
            if (begriffe.isEmpty()) return null
            val sb = StringBuilder("$ECHO_MARKER: ")
            var hinzugefuegt = 0
            for (begriff in begriffe) {
                // Zu lange Eintraege ueberspringen, nicht abbrechen: die uebrigen Begriffe zaehlen.
                if (sb.length + begriff.length + 3 > MAX_PROMPT_ZEICHEN) continue
                if (hinzugefuegt++ > 0) sb.append(", ")
                sb.append(begriff)
            }
            return if (hinzugefuegt == 0) null else sb.append('.').toString()
        }

        /** Prompt fuer die Einstellungen der App; null, wenn das Woerterbuch aus oder leer ist. */
        fun woerterbuchPrompt(aktiv: Boolean, text: String): String? =
            if (aktiv) woerterbuchPrompt(begriffeAus(text)) else null

        private fun normalisiere(s: String): String = s.lowercase().filter(Char::isLetterOrDigit)

        /** Echo = Whisper wiederholt den Prompt ab seinem Anfang ("Begriffe: ..."), ganz oder teilweise. */
        internal fun isPromptEcho(text: String, prompt: String?): Boolean {
            if (prompt.isNullOrEmpty() || text.isBlank()) return false
            val t = normalisiere(text)
            val p = normalisiere(prompt)
            val marker = normalisiere(ECHO_MARKER)
            return t.startsWith(marker) && p.contains(t.substring(marker.length))
        }

        private val WAV_MEDIA_TYPE = "audio/wav".toMediaType()
        private val logger = Logger.getLogger(GroqTranscriber::class.java.name)
    }
}

class GroqTranscriptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
