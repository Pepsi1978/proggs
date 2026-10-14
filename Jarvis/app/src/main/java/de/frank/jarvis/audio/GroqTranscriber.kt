package de.frank.jarvis.audio

import java.io.IOException
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
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
) {
    private val analyzer = SpeechAnalyzer()
    private val filter = WhisperHallucinationFilter()

    /** Der gemeinsame Client aus [GroqNetz] — die Verbindung bleibt über Aufnahmen hinweg offen. */
    private val client get() = GroqNetz.client

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun transcribe(wav: ByteArray): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw GroqTranscriptionException("Groq ist auf diesem Geraet nicht konfiguriert.")
        }
        if (wav.size > MAX_FILE_BYTES) {
            throw GroqTranscriptionException("Die Aufnahme ist fuer die Groq-Transkription zu gross.")
        }

        val roheAnalyse = analyzer.analyze(wav)
        if (filterStille && roheAnalyse != null && roheAnalyse.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) {
            logger.info("Layer 1: recording rejected before upload (${roheAnalyse.voicedMs} ms voiced)")
            return@withContext ""
        }

        // Stille vorn und hinten nicht mit hochladen: Im Gesprächsmodus ist oft mehr als die Hälfte
        // der Aufnahme Schweigen. Danach neu analysieren, damit die Zeitstempel des
        // Halluzinationsfilters zur hochgeladenen Datei passen. Der Stille-Filter ist zugleich der
        // Schalter: Ist er aus, geht die Aufnahme unverändert hoch.
        val hochladen = if (filterStille && roheAnalyse != null) stilleAbschneiden(wav, roheAnalyse) else wav
        val analysis = if (hochladen === wav) roheAnalyse else analyzer.analyze(hochladen)

        val beginn = System.currentTimeMillis()
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", hochladen.toRequestBody(WAV_MEDIA_TYPE))
            .addFormDataPart("model", model)
            .addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0")
            .build()
        val request = Request.Builder()
            .url(TRANSCRIPTIONS_URL)
            .header("Authorization", "Bearer $apiKey")
            .post(multipart)
            .build()

        client.newCall(request).await().use { response ->
            val responseBody = response.body?.string().orEmpty()
            Logbuch.sprache(
                "Groq-Mitschrift",
                "code=${response.code} bytes=${hochladen.size} roh=${wav.size}",
                System.currentTimeMillis() - beginn,
            )
            if (!response.isSuccessful) {
                throw GroqTranscriptionException(httpError(response.code, responseBody))
            }
            val parsed = runCatching { parseResponse(responseBody) }.getOrElse { error ->
                throw GroqTranscriptionException("Groq hat eine ungueltige Antwort geliefert.", error)
            }
            filter.filter(parsed, analysis, filterMetriken, filterZeitstempel, filterFloskeln)
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

    /**
     * Absichtlich ohne Wirkung: Der Client gehört [GroqNetz] und wird von allen Aufnahmen geteilt.
     * Ihn hier herunterzufahren, liesse jede spätere Mitschrift an einem toten Client scheitern.
     * Bleibt nur, damit bestehende Aufrufer weiter übersetzen.
     */
    fun shutdown() = Unit

    /**
     * Schneidet führende und nachlaufende Stille ab, mit [STILLE_RAND_VORN_MS] Rand davor und
     * [STILLE_RAND_HINTEN_MS] dahinter. Die Grenze ist relativ zum Rauschboden der Aufnahme
     * ([SpeechAnalysis.schnittSchwelle]), nicht die feste Sprach-Schwelle: Leise Sprache aus der Ferne
     * liegt oft darunter, und nur betonte Silben kämen sonst darüber. Bringt der Schnitt weniger als
     * [MIN_ERSPARNIS_MS], bleibt die Aufnahme, wie sie ist.
     */
    private fun stilleAbschneiden(wav: ByteArray, analyse: SpeechAnalysis): ByteArray {
        if (wav.size <= WAV_KOPF_BYTES) return wav
        val schwelle = analyse.schnittSchwelle()
        val erster = analyse.ersterFrameUeber(schwelle)
        val letzter = analyse.letzterFrameUeber(schwelle)
        if (erster < 0 || letzter < erster) return wav
        val abtastrate = SpeechAnalyzer.abtastrate(wav)
        // 16 Bit mono: zwei Byte je Abtastwert.
        val bytesJeMs = abtastrate * 2 / 1_000.0
        val datenLaenge = wav.size - WAV_KOPF_BYTES
        fun alsByte(ms: Long): Int {
            val roh = (ms * bytesJeMs).toLong().coerceIn(0L, datenLaenge.toLong()).toInt()
            return roh - roh % 2
        }
        val vonMs = (erster.toLong() * analyse.frameMs - STILLE_RAND_VORN_MS).coerceAtLeast(0L)
        val bisMs = (letzter + 1).toLong() * analyse.frameMs + STILLE_RAND_HINTEN_MS
        val von = alsByte(vonMs)
        val bis = alsByte(bisMs)
        val gespart = datenLaenge - (bis - von)
        if (bis <= von || gespart < MIN_ERSPARNIS_MS * bytesJeMs) return wav
        val ergebnis = ByteArray(WAV_KOPF_BYTES + (bis - von))
        System.arraycopy(wav, 0, ergebnis, 0, WAV_KOPF_BYTES)
        System.arraycopy(wav, WAV_KOPF_BYTES + von, ergebnis, WAV_KOPF_BYTES, bis - von)
        // Die Längen im Kopf müssen stimmen, sonst nimmt kein Dienst die Datei an.
        schreibeLittleEndian(ergebnis, 4, ergebnis.size - 8)
        schreibeLittleEndian(ergebnis, 40, bis - von)
        logger.info("Silence trimmed: ${wav.size} -> ${ergebnis.size} bytes (cut threshold $schwelle)")
        return ergebnis
    }

    private fun schreibeLittleEndian(ziel: ByteArray, offset: Int, wert: Int) {
        ziel[offset] = (wert and 0xFF).toByte()
        ziel[offset + 1] = ((wert shr 8) and 0xFF).toByte()
        ziel[offset + 2] = ((wert shr 16) and 0xFF).toByte()
        ziel[offset + 3] = ((wert shr 24) and 0xFF).toByte()
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
        const val DEFAULT_MODEL = "whisper-large-v3-turbo"

        private val WAV_MEDIA_TYPE = "audio/wav".toMediaType()

        /** Ein WAV-Kopf aus [MicRecorder] ist genau 44 Byte lang. */
        private const val WAV_KOPF_BYTES = 44

        /** Rand, der vor dem Gesprochenen stehen bleibt (leise Anlaute wie ein gehauchtes „Und …“). */
        private const val STILLE_RAND_VORN_MS = 700L

        /**
         * Rand hinter dem Gesprochenen: leise Satzenden und nachgeschobene Wörter. Im Gesprächsmodus
         * folgen ohnehin 4,5 s Stille, es bleiben also über 3 s Ersparnis.
         */
        private const val STILLE_RAND_HINTEN_MS = 1_200L

        /** Unter dieser Ersparnis lohnt kein Schnitt. */
        private const val MIN_ERSPARNIS_MS = 500L
        private val logger = Logger.getLogger(GroqTranscriber::class.java.name)
    }
}

class GroqTranscriptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
