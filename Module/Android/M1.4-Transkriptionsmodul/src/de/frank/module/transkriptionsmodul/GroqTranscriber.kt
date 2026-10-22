// ──────────────────────────────────────────────────────────────────────
// Modul M1.4 — Transkriptionsmodul · Stand v4
// Quelle: Module/Android/M1.4-Transkriptionsmodul/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.4 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.transkriptionsmodul

import java.io.IOException
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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
    /**
     * Persönliches Wörterbuch als Whisper-Prompt (aus [woerterbuchPrompt]); null = ohne Prompt.
     * Whisper übernimmt daraus Schreibweisen von Eigennamen und Fachbegriffen.
     */
    private val prompt: String? = null,
) {
    private val analyzer = SpeechAnalyzer()
    private val filter = WhisperHallucinationFilter()

    /** Der gemeinsame Client aus [GroqNetz] — die Verbindung bleibt über Aufnahmen hinweg offen. */
    private val client get() = GroqNetz.client

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    /**
     * [stimmhaft]: Zeitachse der Spracherkennung zu genau dieser Aufnahme (je 32 ms, aus [MicRecorder]). Mit ihr
     * zählt für Zuschnitt und Filter nur erkannte Stimme statt des Pegels: Im Auto liegt das Fahrgeräusch überall
     * über der Pegel-Schwelle, dann griffe kein Zuschnitt und Whisper bekäme die Rauschpause am Ende mit.
     */
    suspend fun transcribe(wav: ByteArray, stimmhaft: BooleanArray? = null): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            throw GroqTranscriptionException("Groq ist auf diesem Geraet nicht konfiguriert.")
        }
        if (wav.size > MAX_FILE_BYTES) {
            throw GroqTranscriptionException("Die Aufnahme ist fuer die Groq-Transkription zu gross.")
        }

        val roheAnalyse = analyzer.analyze(wav, stimmhaft)
        if (filterStille && roheAnalyse != null && roheAnalyse.voicedMs < SpeechAnalyzer.MIN_SPEECH_MS) {
            logger.info("Layer 1: recording rejected before upload (${roheAnalyse.voicedMs} ms voiced)")
            return@withContext ""
        }

        // Stille vorn und hinten nicht mit hochladen: Im Gesprächsmodus ist oft mehr als die Hälfte
        // der Aufnahme Schweigen. Danach neu analysieren, damit die Zeitstempel des
        // Halluzinationsfilters zur hochgeladenen Datei passen. Der Stille-Filter ist zugleich der
        // Schalter: Ist er aus, geht die Aufnahme unverändert hoch.
        val schnitt = if (filterStille && roheAnalyse != null) stilleAbschneiden(wav, roheAnalyse) else null
        val hochladen = schnitt?.first ?: wav
        val analysis = if (schnitt == null) {
            roheAnalyse
        } else {
            // Die Zeitachse um den abgeschnittenen Anfang verschieben, damit sie zur hochgeladenen Datei passt.
            val ab = (schnitt.second / SprachZeitachse.RAHMEN_MS).toInt()
            analyzer.analyze(hochladen, stimmhaft?.let { it.copyOfRange(ab.coerceAtMost(it.size), it.size) })
        }

        // Whisper gibt bei Stille/Gemurmel gern den Prompt selbst aus („Begriffe: …“).
        // Dann einmal ohne Wörterbuch wiederholen, statt das Diktat zu verlieren.
        val mitPrompt = einmal(hochladen, wav.size, prompt)
        val parsed = if (prompt != null && istPromptEcho(mitPrompt.text, prompt)) {
            Protokoll.aktiv.warn("Transkription", "transcribe", "Prompt-Echo erkannt, erneut ohne Wörterbuch", mapOf("zeichen" to mitPrompt.text.length))
            einmal(hochladen, wav.size, null)
        } else {
            mitPrompt
        }
        filter.filter(parsed, analysis, filterMetriken, filterZeitstempel, filterFloskeln)
    }

    /**
     * Ein Upload mit dem gewählten Modell. Ist large-v3 für die Groq-Organisation gesperrt
     * (403 model_permission_blocked_org), wird dieselbe Aufnahme sofort mit Turbo wiederholt
     * und large-v3 für [SPERRE_MS] übersprungen — der Nutzer sieht keinen Fehler.
     */
    private suspend fun einmal(hochladen: ByteArray, rohBytes: Int, prompt: String?): GroqTranscriptionResponse {
        var modell = if (model == MODELL_LARGE_V3 && System.currentTimeMillis() < gesperrtBisMs) MODELL_TURBO else model
        var (code, body) = sende(hochladen, rohBytes, modell, prompt)
        if (code !in 200..299 && modell == MODELL_LARGE_V3 && istModellGesperrt(body)) {
            Protokoll.aktiv.warn("Transkription", "transcribe", "large-v3 für die Organisation gesperrt, Rückfall auf Turbo", mapOf("code" to code))
            gesperrtBisMs = System.currentTimeMillis() + SPERRE_MS
            modell = MODELL_TURBO
            val zweiter = sende(hochladen, rohBytes, modell, prompt)
            code = zweiter.first
            body = zweiter.second
        }
        if (code !in 200..299) {
            throw GroqTranscriptionException(httpError(code, body))
        }
        return runCatching { parseResponse(body) }.getOrElse { error ->
            throw GroqTranscriptionException("Groq hat eine ungueltige Antwort geliefert.", error)
        }
    }

    /** Ein einzelner Upload; liefert HTTP-Code und Antworttext. */
    private suspend fun sende(hochladen: ByteArray, rohBytes: Int, modell: String, prompt: String?): Pair<Int, String> {
        val beginn = System.currentTimeMillis()
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", hochladen.toRequestBody(WAV_MEDIA_TYPE))
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
            Protokoll.aktiv.sprache(
                "Groq-Mitschrift",
                "code=${response.code} bytes=${hochladen.size} roh=$rohBytes modell=$modell prompt=${prompt != null}",
                System.currentTimeMillis() - beginn,
            )
            response.code to responseBody
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
    /** Liefert die geschnittene Aufnahme und wie viele ms vorn wegfielen; null, wenn sie bleibt, wie sie ist. */
    private fun stilleAbschneiden(wav: ByteArray, analyse: SpeechAnalysis): Pair<ByteArray, Long>? {
        if (wav.size <= WAV_KOPF_BYTES) return null
        val schwelle = analyse.schnittSchwelle()
        val erster = analyse.ersterFrameUeber(schwelle)
        val letzter = analyse.letzterFrameUeber(schwelle)
        if (erster < 0 || letzter < erster) return null
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
        if (bis <= von || gespart < MIN_ERSPARNIS_MS * bytesJeMs) return null
        val ergebnis = ByteArray(WAV_KOPF_BYTES + (bis - von))
        System.arraycopy(wav, 0, ergebnis, 0, WAV_KOPF_BYTES)
        System.arraycopy(wav, WAV_KOPF_BYTES + von, ergebnis, WAV_KOPF_BYTES, bis - von)
        // Die Längen im Kopf müssen stimmen, sonst nimmt kein Dienst die Datei an.
        schreibeLittleEndian(ergebnis, 4, ergebnis.size - 8)
        schreibeLittleEndian(ergebnis, 40, bis - von)
        logger.info("Silence trimmed: ${wav.size} -> ${ergebnis.size} bytes (cut threshold $schwelle)")
        return ergebnis to (von / bytesJeMs).toLong()
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
        const val MODELL_TURBO = "whisper-large-v3-turbo"
        const val MODELL_LARGE_V3 = "whisper-large-v3"
        const val DEFAULT_MODEL = MODELL_TURBO

        /** Vorbelegung des persönlichen Wörterbuchs (Stand TerminalVoiceOverlay). */
        const val STANDARD_WOERTERBUCH =
            "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic"

        /** Groq empfiehlt kurze Prompts; wie im TerminalVoiceOverlay höchstens 350 Zeichen. */
        private const val MAX_PROMPT_ZEICHEN = 350
        private const val MAX_BEGRIFFE = 100
        private const val ECHO_MARKER = "Begriffe"

        /** Nach einer large-v3-Sperre so lange direkt Turbo nehmen, statt jedes Diktat doppelt hochzuladen. */
        private const val SPERRE_MS = 30 * 60 * 1000L

        @Volatile
        private var gesperrtBisMs = 0L

        /** Begriffe aus dem Wörterbuch-Text: getrennt an Zeilenumbruch, Komma, Semikolon; ohne Leeres, #-Kommentare und Duplikate. */
        fun begriffe(text: String): List<String> =
            text.split('\n', '\r', ',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .distinctBy { it.lowercase() }
                .take(MAX_BEGRIFFE)

        /** Wörterbuch als natürlicher Satz („Begriffe: A, B.“); ein zu langer Begriff wird übersprungen. Keine Begriffe → null. */
        fun woerterbuchPrompt(begriffe: List<String>): String? {
            if (begriffe.isEmpty()) return null
            val sb = StringBuilder("$ECHO_MARKER: ")
            var hinzu = 0
            for (w in begriffe) {
                if (sb.length + w.length + 2 > MAX_PROMPT_ZEICHEN) continue
                if (hinzu++ > 0) sb.append(", ")
                sb.append(w)
            }
            return if (hinzu == 0) null else sb.append('.').toString()
        }

        /**
         * Echo = Whisper wiederholt den Prompt ab seinem Anfang („Begriffe: …“), ganz oder teilweise.
         * Ein Diktat, das nur Wörterbuch-Wörter enthält, beginnt nicht mit dem Marker und bleibt erhalten.
         */
        fun istPromptEcho(text: String, prompt: String?): Boolean {
            if (prompt.isNullOrEmpty() || text.isBlank()) return false
            fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
            val t = norm(text)
            val marker = norm(ECHO_MARKER)
            return t.startsWith(marker) && norm(prompt).contains(t.substring(marker.length))
        }

        /** Groq-Antwort „Modell für die Organisation gesperrt“. */
        internal fun istModellGesperrt(body: String): Boolean =
            body.contains("model_permission_blocked_org") || body.contains("blocked at the organization level")

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
