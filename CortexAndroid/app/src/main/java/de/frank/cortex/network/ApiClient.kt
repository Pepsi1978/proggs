package de.frank.cortex.network

import com.squareup.moshi.Moshi
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.TtsUsageStore
import de.frank.cortex.data.model.*
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 429 von Gemini-TTS. Eigener Typ statt generischer Exception, damit der Aufrufer
 * (ChatViewModel) gezielt drosseln statt sofort blind weiter-haemmern kann.
 * [retryAfterMs] kommt aus dem `Retry-After`-Header, falls Gemini ihn mitsendet.
 */
class GeminiRateLimitException(message: String, val retryAfterMs: Long?) : Exception(message)

/**
 * 403/401 von Cloud-TTS (z.B. API nicht freigeschaltet, Key ohne Zugriff). Eigener Typ, damit der
 * Aufrufer das NICHT wie einen voruebergehenden Fehler behandelt (kein sinnloses Retry) und dem
 * Nutzer eine klare, umsetzbare Meldung zeigt, statt still nichts vorzulesen.
 */
class GeminiTtsAccessException(message: String) : Exception(message)

class ChatStreamException(
    cause: Throwable,
    val headersReceived: Boolean,
    val anyEventReceived: Boolean,
    val deltaReceived: Boolean,
    val doneReceived: Boolean,
) : IOException(cause.message ?: cause.javaClass.simpleName, cause)

// Vom Server auswaehlbare GPT-5.6-Codex-Modelle (Modell-Picker in den Einstellungen).
internal val CODEX_GPT56_MODELS = listOf(
    "gpt-5.6-sol", "gpt-5.6-sol-fast",
    "gpt-5.6-terra", "gpt-5.6-terra-fast",
    "gpt-5.6-luna", "gpt-5.6-luna-fast",
)

private val CODEX_FAST_MODEL_ALIASES = mapOf(
    "gpt-5.6-sol-fast" to "gpt-5.6-sol",
    "gpt-5.6-terra-fast" to "gpt-5.6-terra",
    "gpt-5.6-luna-fast" to "gpt-5.6-luna",
)

internal data class CodexModelRequestConfig(val model: String, val serviceTier: String?)

internal fun codexModelRequestConfig(selectedModel: String): CodexModelRequestConfig {
    val selected = selectedModel.trim()
    val baseModel = CODEX_FAST_MODEL_ALIASES[selected.lowercase()]
    return CodexModelRequestConfig(
        model = baseModel ?: selected,
        serviceTier = if (baseModel != null) "priority" else null,
    )
}

object ApiClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // --- Moshi Singleton (Best Practice §1.7 / §2.1) ---
    private val moshi: Moshi = Moshi.Builder().build()

    private val moshiFactory = MoshiConverterFactory.create(moshi)

    // --- Debug-Logging: keine Bodies auf dem Chat-Hot-Path loggen. ---
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        CortexLog.info("HTTP", "log", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
        redactHeader("x-goog-api-key")
    }

    private val timingInterceptor = Interceptor { chain ->
        val request = chain.request()
        val startedAt = System.nanoTime()
        try {
            val response = chain.proceed(request)
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            CortexLog.info(
                "HTTP",
                "timing",
                "${request.method} ${request.url.encodedPath} -> ${response.code}",
                mapOf("elapsed_ms" to elapsedMs, "request_bytes" to (request.body?.contentLength() ?: 0L))
            )
            response
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            CortexLog.warn(
                "HTTP",
                "timing",
                "${request.method} ${request.url.encodedPath} fehlgeschlagen: ${e.message}",
                mapOf("elapsed_ms" to elapsedMs)
            )
            throw e
        }
    }

    // --- EIN OkHttpClient pro Backend (Best Practice §1.1) ---
    // Agent + Brain teilen sich einen Client mit Bearer-Auth-Interceptor
    private val authClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val key = SettingsStore.sbApiKey
                val req = chain.request().newBuilder()
                    .header("Authorization", "Bearer $key")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(timingInterceptor)
            .addInterceptor(loggingInterceptor) // LETZTER Interceptor
            // Agent-Antworten mit Internet-Suche brauchen server-seitig 30-40s (Router-LLM +
            // Websuche + Antwort-Synthese, nacheinander). readTimeout 60s war zu knapp -> "Fehler
            // Timeout" bei langen Suchen. Grosszuegiger: readTimeout 120s, callTimeout 180s (hartes
            // Dach, Almanach §L4/BP §6.1) — kein Streaming, daher readTimeout>0 ok (2026-07-01).
            .callTimeout(180, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** Verwirft Verbindungen, deren TCP-Route nach einem VPN-/Mobilfunkwechsel nicht mehr stimmt. */
    fun resetAgentConnections(reason: String) {
        authClient.connectionPool.evictAll()
        CortexLog.info("HTTP", "resetConnections", "Agent-Verbindungspool geleert",
            mapOf("reason" to reason))
    }

    // Dashboard: kein Auth
    private val dashboardClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(timingInterceptor)
            .addInterceptor(loggingInterceptor)
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Externer Client für Groq/Gemini (ohne Auth-Interceptor, eigene Keys pro Request)
    private val externalClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(timingInterceptor)
            .addInterceptor(loggingInterceptor)
            .callTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // TTS liefert große Base64-Audioantworten. BODY-Logging würde den ersten Ton stark verzögern
    // und private Antworttexte ins Log schreiben, deshalb läuft TTS über einen eigenen schlanken Client.
    private val ttsClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // --- Retrofit-Instanzen: EINE pro Backend (Best Practice §1.3), aber URL-bewusst ---
    // Frueher waren die Instanzen `by lazy` und froren die Basis-URL vom ersten Zugriff ein:
    // Aenderte Frank Host/Port in den Einstellungen, ging chatStream (liest die URL live) sofort
    // an den neuen Server, waehrend /chat, Kategorien & Dashboard bis zum App-Neustart an den
    // ALTEN gingen. Jetzt wird die Instanz beim naechsten Zugriff neu gebaut, sobald sich die
    // Basis-URL geaendert hat (OkHttp-Clients bleiben geteilt — Pool/Threads unveraendert).
    private class UrlBoundApi<T>(
        private val urlProvider: () -> String,
        private val create: (String) -> T
    ) {
        // EIN @Volatile-Paar statt zweier getrennter Felder: ein lock-freier Leser konnte sonst
        // die neue Instanz zusammen mit der ALTEN URL sehen (nicht-atomare Publikation).
        @Volatile private var bound: Pair<String, T>? = null
        fun get(): T {
            val url = urlProvider()
            bound?.let { if (it.first == url) return it.second }
            synchronized(this) {
                bound?.let { if (it.first == url) return it.second }
                val fresh = create(url)
                bound = url to fresh
                return fresh
            }
        }
    }

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create()) // VOR Moshi (§1.7)
            .addConverterFactory(moshiFactory)
            .build()

    private val agentApiHolder = UrlBoundApi({ SettingsStore.agentUrl() }) {
        buildRetrofit(it, authClient).create(AgentApi::class.java)
    }
    private val brainApiHolder = UrlBoundApi({ SettingsStore.brainUrl() }) {
        buildRetrofit(it, authClient).create(BrainApi::class.java)
    }
    private val dashboardApiHolder = UrlBoundApi({ SettingsStore.dashboardUrl() }) {
        buildRetrofit(it, dashboardClient).create(DashboardApi::class.java)
    }

    // /chat-Fallback (und Outbox-Flush): der Server sendet bis zur fertigen Antwort KEIN Byte —
    // lange Läufe (Tool-Loop bis 240 s + Selbstregel-Audit + Dedup-Wartefenster 240 s) sprengten
    // das 120s-readTimeout des authClient, obwohl der Streaming-Pfad dieselbe Antwort mit seinem
    // 600s-Dach geliefert hätte. Eigener Client mit demselben Gesamt-Dach wie das Streaming.
    private val chatFallbackClient: OkHttpClient by lazy {
        authClient.newBuilder()
            .callTimeout(600, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .build()
    }
    private val agentChatFallbackHolder = UrlBoundApi({ SettingsStore.agentUrl() }) {
        buildRetrofit(it, chatFallbackClient).create(AgentApi::class.java)
    }

    // --- Service-Interfaces ---
    fun agentApi(): AgentApi = agentApiHolder.get()
    fun brainApi(): BrainApi = brainApiHolder.get()
    fun dashboardApi(): DashboardApi = dashboardApiHolder.get()
    /** Nur fuer POST /chat als Stream-Fallback bzw. Outbox-Flush (grosses Timeout-Dach). */
    fun agentChatFallbackApi(): AgentApi = agentChatFallbackHolder.get()

    // --- /config-Zwischenspeicher (Performance 2026-07-12) -------------------
    // Die Einstellungen (und der App-Start-Sync) warteten bisher bei JEDEM Aufruf sichtbar auf
    // GET /config. Die letzte Antwort wird jetzt lokal gespeichert: UI zeigt sie sofort,
    // der frische Server-Stand ersetzt sie still, sobald er da ist (Stale-while-Revalidate).
    private val agentConfigAdapter by lazy { moshi.adapter(AgentConfigResponse::class.java) }

    fun cacheAgentConfig(config: AgentConfigResponse) {
        try {
            SettingsStore.cachedAgentConfigJson = agentConfigAdapter.toJson(config)
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.warn("ApiClient", "cacheAgentConfig", "Config-Cache nicht schreibbar: ${e.message}")
        }
    }

    fun cachedAgentConfig(): AgentConfigResponse? = try {
        SettingsStore.cachedAgentConfigJson.takeIf { it.isNotBlank() }
            ?.let { agentConfigAdapter.fromJson(it) }
    } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
        CortexLog.warn("ApiClient", "cachedAgentConfig", "Config-Cache nicht lesbar: ${e.message}")
        null
    }

    // --- Dashboard-Zwischenspeicher (Performance 2026-07-13) ------------------
    // Beim Neustart stand oben ewig „0 Einträge", bis VPN-Connect + Server-Roundtrip durch waren.
    // Die letzte Übersicht wird lokal gespeichert: UI zeigt sie sofort, frische Daten ersetzen sie
    // still (Stale-while-Revalidate, gleiches Muster wie der /config-Cache).
    private val dashboardSnapshotAdapter by lazy { moshi.adapter(DashboardSnapshot::class.java) }

    fun cacheDashboard(snapshot: DashboardSnapshot) {
        try {
            SettingsStore.cachedDashboardJson = dashboardSnapshotAdapter.toJson(snapshot)
        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
            CortexLog.warn("ApiClient", "cacheDashboard", "Dashboard-Cache nicht schreibbar: ${e.message}")
        }
    }

    fun cachedDashboard(): DashboardSnapshot? = try {
        SettingsStore.cachedDashboardJson.takeIf { it.isNotBlank() }
            ?.let { dashboardSnapshotAdapter.fromJson(it) }
    } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
        CortexLog.warn("ApiClient", "cachedDashboard", "Dashboard-Cache nicht lesbar: ${e.message}")
        null
    }

    // --- Streaming-Chat (SSE, /chat/stream) ---------------------------------
    private val chatRequestAdapter by lazy { moshi.adapter(ChatRequest::class.java) }
    private val chatResponseAdapter by lazy { moshi.adapter(ChatResponse::class.java) }

    // Eigener Client fuers Streaming (teilt Pool/Auth mit authClient): grosses Gesamtdach fuer
    // lange XL-Antworten; readTimeout gilt PRO Chunk (jeder Delta setzt die Uhr zurueck).
    private val streamingClient: OkHttpClient by lazy {
        authClient.newBuilder()
            .callTimeout(600, TimeUnit.SECONDS)
            // Zwei verpasste 15-s-Heartbeats plus 5 s Toleranz beweisen einen stummen Kanal.
            // Idempotenz + frische Fallback-Verbindung retten die laufende Server-Antwort.
            .readTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    /**
     * POST /chat/stream — Antwort-Text kommt live als SSE-Deltas (onDelta, aus dem IO-Thread),
     * am Ende liefert der Server die finale ChatResponse (bereinigter Reply). Die App ersetzt
     * den gestreamten Rohtext durch diesen finalen Reply — verlorene Deltas heilen sich so selbst.
     */
    suspend fun chatStream(request: ChatRequest, onDelta: (String) -> Unit): ChatResponse =
        withContext(Dispatchers.IO) {
            val startedAt = System.currentTimeMillis()
            val requestId = request.request_id.orEmpty()
            var headersReceived = false
            var anyEventReceived = false
            var deltaReceived = false
            var doneReceived = false
            var eventCount = 0
            var deltaCount = 0
            var heartbeatCount = 0
            CortexLog.info("ChatStream", "start", "SSE-Chat gestartet",
                mapOf("request_id" to requestId.take(12)))
            // Ein Chat darf nie einen Socket wiederverwenden, dessen Route vor dem letzten
            // WireGuard-/Mobilfunkwechsel aufgebaut wurde.
            resetAgentConnections("chat_stream_start")
            val body = chatRequestAdapter.toJson(request).toRequestBody(jsonMediaType)
            val httpReq = Request.Builder()
                .url(SettingsStore.agentUrl() + "/chat/stream")
                .post(body)
                .build()
            val call = streamingClient.newCall(httpReq)
            try {
                coroutineScope {
                // Watcher bricht den Socket bei Cancel SOFORT ab: readUtf8Line() blockiert sonst
                // weiter, bis der naechste Heartbeat (bis 15 s) bzw. das readTimeout (60 s) greift.
                val watcher = launch {
                    try {
                        awaitCancellation()
                    } finally {
                        call.cancel()
                    }
                }
                try {
                executeCancellable(call).use { resp ->
                    headersReceived = true
                    CortexLog.info("ChatStream", "headers", "SSE-Header empfangen",
                        mapOf("request_id" to requestId.take(12), "status" to resp.code,
                            "elapsed_ms" to (System.currentTimeMillis() - startedAt)))
                    if (!resp.isSuccessful) {
                        val err = resp.body?.string().orEmpty().take(300)
                        throw IOException("Stream-HTTP ${resp.code}: $err")
                    }
                    val source = resp.body?.source() ?: throw IOException("Leerer Stream-Body")
                    while (true) {
                        ensureActive()
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val payload = line.removePrefix("data:").trim()
                        if (payload.isEmpty()) continue
                        val evt = JSONObject(payload)
                        val type = evt.optString("type")
                        eventCount++
                        if (!anyEventReceived) {
                            anyEventReceived = true
                            CortexLog.info("ChatStream", "firstEvent", "Erstes SSE-Event empfangen",
                                mapOf("request_id" to requestId.take(12), "type" to type,
                                    "elapsed_ms" to (System.currentTimeMillis() - startedAt)))
                        }
                        when (type) {
                            "ready" -> Unit
                            "heartbeat" -> {
                                heartbeatCount++
                                CortexLog.info("ChatStream", "heartbeat", "SSE-Heartbeat empfangen",
                                    mapOf("request_id" to requestId.take(12), "count" to heartbeatCount,
                                        "elapsed_ms" to (System.currentTimeMillis() - startedAt)))
                            }
                            "delta" -> evt.optString("text").takeIf { it.isNotEmpty() }?.let {
                                if (!deltaReceived) {
                                    CortexLog.info("ChatStream", "firstDelta", "Erstes SSE-Textdelta empfangen",
                                        mapOf("request_id" to requestId.take(12),
                                            "elapsed_ms" to (System.currentTimeMillis() - startedAt)))
                                }
                                deltaReceived = true
                                deltaCount++
                                onDelta(it)
                            }
                            "done" -> {
                                val result = chatResponseAdapter.fromJson(evt.getJSONObject("response").toString())
                                    ?: throw IOException("Done-Event ohne gültige Antwort")
                                doneReceived = true
                                CortexLog.info("ChatStream", "done", "SSE-Abschluss empfangen",
                                    mapOf("request_id" to requestId.take(12),
                                        "elapsed_ms" to (System.currentTimeMillis() - startedAt),
                                        "events" to eventCount, "deltas" to deltaCount,
                                        "heartbeats" to heartbeatCount))
                                return@coroutineScope result
                            }
                            "error" -> throw IOException("Agent-Stream-Fehler: ${evt.optString("message")}")
                        }
                    }
                    throw IOException("Stream endete ohne Abschluss-Event")
                }
                } finally {
                    watcher.cancel()
                }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ChatStreamException) {
                throw e
            } catch (e: Exception) {
                CortexLog.warn("ChatStream", "failure", "SSE-Chat fehlgeschlagen: ${e.message}",
                    mapOf("request_id" to requestId.take(12),
                        "elapsed_ms" to (System.currentTimeMillis() - startedAt),
                        "headers" to headersReceived, "events" to anyEventReceived,
                        "delta" to deltaReceived, "done" to doneReceived))
                throw ChatStreamException(e, headersReceived, anyEventReceived, deltaReceived, doneReceived)
            }
        }

    // --- Groq STT (direkt über OkHttp, multipart) ---

    // optDouble liefert bei fehlendem Feld NaN (nicht null!) — alle Schwellwert-Vergleiche im
    // WhisperHallucinationFilter liefen mit NaN still auf false und die Anti-Halluzinations-
    // Schichten waeren lautlos deaktiviert gewesen.
    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        optDouble(name).takeUnless { it.isNaN() }

    // Komplett auf IO: vorher lief nur execute() auf IO, aber body().string() (Socket-Read!)
    // und das Parsen aller verbose_json-Segmente auf dem Aufrufer-Thread — im Chat war das der
    // Main-Thread (Jank/ANR-Risiko nach jedem Aufnahme-Stopp).
    suspend fun groqTranscribe(wavBytes: ByteArray): GroqTranscriptionResponse = withContext(Dispatchers.IO) {
        val key = SettingsStore.groqApiKey
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "audio.wav",
                wavBytes.toRequestBody("audio/wav".toMediaType())
            )
            .addFormDataPart("model", "whisper-large-v3-turbo")
            .addFormDataPart("language", "de")
            .addFormDataPart("response_format", "verbose_json")
            // temperature=0 als Basis der Anti-Halluzinations-Kette (Almanach §1.2 — reicht allein
            // NICHT gegen Stille-Halluzination, aber Grundlage; die 4 Schichten liegen im ViewModel/Filter).
            .addFormDataPart("temperature", "0")
            .build()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/transcriptions")
            .header("Authorization", "Bearer $key")
            .post(body)
            .build()

        val (code, responseBody) = executeForString(externalClient, request)
        if (responseBody.isEmpty()) throw Exception("Leere Groq-Antwort")

        if (code !in 200..299) {
            throw Exception("Groq-Fehler $code: $responseBody")
        }

        val json = JSONObject(responseBody)
        val text = json.getString("text")
        val segments = json.optJSONArray("segments")?.let { arr ->
            (0 until arr.length()).map { i ->
                val seg = arr.getJSONObject(i)
                GroqSegment(
                    start = seg.optDoubleOrNull("start"),
                    end = seg.optDoubleOrNull("end"),
                    text = seg.optString("text"),
                    no_speech_prob = seg.optDoubleOrNull("no_speech_prob"),
                    avg_logprob = seg.optDoubleOrNull("avg_logprob"),
                    compression_ratio = seg.optDoubleOrNull("compression_ratio")
                )
            }
        }

        CortexLog.info("Groq", "transcribe", "Transkription erhalten", mapOf("text_length" to text.length))
        GroqTranscriptionResponse(text = text, segments = segments)
    }

    // --- Google Cloud Text-to-Speech (Chirp 3: HD) ---
    // Nutzt bewusst die DEDIZIERTE Cloud-TTS-API (texttospeech.googleapis.com), NICHT das
    // Gemini-2.5-Flash-Preview-TTS-Modell. Grund (Frank 2026-07-01): Das Preview-Modell hat ein
    // hartes Free-Tier-Tageslimit (~100 Anfragen/Tag -> staendige 429). Cloud Chirp 3: HD erlaubt
    // 200 Anfragen PRO MINUTE pro Projekt (1 Mio Zeichen/Monat frei, danach ~30$/Mio). Gleiche
    // Stimmen (Autonoe, Kore, ...), nur ueber den richtigen Endpunkt - wie in BestJournalFrank.
    // Voraussetzung: "Cloud Text-to-Speech API" im Projekt des API-Keys aktiviert.
    private const val CHIRP3_VOICE_PREFIX = "de-DE-Chirp3-HD-"

    // Komplett auf IO: body().string() liest die grosse Base64-Audio-Antwort blockierend vom
    // Socket — im Settings-Stimmen-Test lief das auf dem Main-Thread (NetworkOnMainThreadException).
    suspend fun geminiTts(text: String, voice: String = SettingsStore.ttsVoice): ByteArray = withContext(Dispatchers.IO) {
        // Dedizierter Google-TTS-Schluessel falls gesetzt, sonst Fallback auf den Gemini-Schluessel.
        val key = SettingsStore.ttsApiKey
        if (key.isBlank()) throw IllegalStateException("TTS-Schlüssel fehlt (Google-TTS- oder Gemini-Schlüssel setzen)")
        val startedAt = System.currentTimeMillis()

        // Cortex speichert die Stimme als blossen Namen (z.B. "Autonoe"); Cloud TTS braucht den
        // vollen Chirp3-HD-Namen "de-DE-Chirp3-HD-Autonoe" (Almanach tts-provider G9).
        val fullVoiceName = if (voice.startsWith(CHIRP3_VOICE_PREFIX)) voice else "$CHIRP3_VOICE_PREFIX$voice"

        // LINEAR16 @ 24 kHz -> WAV-Container (44-Byte-Header) + rohes PCM16 mono, exakt das
        // Format, das der bestehende PcmPlayer erwartet. So bleibt die ganze Chunk-/Player-
        // Pipeline unveraendert. Kein speaking_rate hier - das Tempo setzt PcmPlayer beim Abspielen.
        val body = JSONObject().apply {
            put("input", JSONObject().put("text", text))
            put("voice", JSONObject().apply {
                put("languageCode", "de-DE")
                put("name", fullVoiceName)
            })
            put("audioConfig", JSONObject().apply {
                put("audioEncoding", "LINEAR16")
                put("sampleRateHertz", 24000)
            })
        }.toString()

        val request = Request.Builder()
            .url("https://texttospeech.googleapis.com/v1/text:synthesize")
            .header("x-goog-api-key", key) // sicherer als ?key= im Query (Almanach GA1)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        executeCancellable(ttsClient, request).use { response ->
            if (response.code == 429) {
                // Retry-After ist laut RFC entweder Sekunden oder ein HTTP-Datum — hier reicht Sekunden-Fall.
                val retryAfterMs = response.header("Retry-After")?.toLongOrNull()?.times(1000)
                val errBody = response.body?.string().orEmpty()
                CortexLog.warn("CloudTts", "tts", "429 Rate-Limit", mapOf(
                    "text_len" to text.length, "retry_after_ms" to (retryAfterMs ?: -1L)
                ))
                throw GeminiRateLimitException("Cloud-TTS-Rate-Limit (429): $errBody", retryAfterMs)
            }

            val responseBody = response.body?.string() ?: throw Exception("Leere Cloud-TTS-Antwort")

            if (response.code == 403 || response.code == 401) {
                // Typischer Fall: "Cloud Text-to-Speech API has not been used ... or it is disabled"
                // (SERVICE_DISABLED) oder Key ohne Zugriff. Kein Retry - klar melden.
                CortexLog.error("CloudTts", "tts", "Zugriff verweigert (${response.code})",
                    mapOf("text_len" to text.length, "body" to responseBody.take(300)))
                throw GeminiTtsAccessException("Cloud-TTS-Zugriff verweigert (${response.code}): $responseBody")
            }

            if (!response.isSuccessful) {
                throw Exception("Cloud-TTS-Fehler ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            val audioContent = json.optString("audioContent", "")
            if (audioContent.isBlank()) throw Exception("Keine Audio-Daten in Cloud-TTS-Antwort")

            val raw = android.util.Base64.decode(audioContent, android.util.Base64.DEFAULT)
            // LINEAR16-Antwort ist ein WAV mit 44-Byte-RIFF-Header -> abstreifen ergibt rohes PCM.
            val pcm = if (raw.size > 44) raw.copyOfRange(44, raw.size) else raw

            CortexLog.info("CloudTts", "tts", "TTS-Audio erhalten", mapOf(
                "text_len" to text.length,
                "voice" to fullVoiceName,
                "raw_bytes" to raw.size,
                "pcm_bytes" to pcm.size,
                "elapsed_ms" to (System.currentTimeMillis() - startedAt)
            ))
            TtsUsageStore.addSuccessfulSynthesis(text.length)
            return@withContext pcm
        }
    }

    private suspend fun executeCancellable(client: OkHttpClient, request: Request): Response =
        executeCancellable(client.newCall(request))

    private suspend fun executeCancellable(call: okhttp3.Call): Response =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { call.cancel() }
                try {
                    val response = call.execute()
                    if (continuation.isActive) {
                        // onCancellation-Variante: wird die Coroutine GENAU zwischen isActive-Check
                        // und resume abgebrochen, schliesst OkHttp die Response trotzdem (kein Leak).
                        continuation.resume(response) { _ -> response.close() }
                    } else {
                        response.close()
                    }
                } catch (e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        }

    /** Cancellable ausfuehren, Body als String lesen, Response sicher schliessen — alles auf IO.
     *  Ersetzt die frueheren blockierenden `client.newCall(request).execute()`-Aufrufe, die ein
     *  Coroutine-Cancel NICHT abbrach (Call lief bis zum callTimeout weiter) und deren
     *  body().string() teils auf dem Main-Thread las. */
    private suspend fun executeForString(client: OkHttpClient, request: Request): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            executeCancellable(client, request).use { resp ->
                resp.code to resp.body?.string().orEmpty()
            }
        }

    // --- Gemini Text verbessern ---

    suspend fun geminiImprove(text: String, extraInstruction: String? = null): String {
        val key = SettingsStore.geminiApiKey
        if (key.isBlank()) throw IllegalStateException("Gemini-Schlüssel fehlt")
        val basePrompt = """
            Du bist ein deutscher Korrektur- und Formulierungsassistent für diktierte oder getippte Notizen.
            Nimm den folgenden Text vollständig als Quelle.
            Erkenne aus dieser Quelle die gemeinte Intention und formuliere sie in sehr gutes, klares, natürliches Deutsch um.
            Korrigiere Grammatik, Rechtschreibung, Zeichensetzung, Satzbau, Diktierfehler, Wiederholungen und holprige Formulierungen.
            Du darfst Sätze neu ordnen und verständlicher formulieren, wenn die Bedeutung erhalten bleibt.
            Füge keine neuen Fakten, Aufgaben, Bewertungen, Beispiele oder Schlussfolgerungen hinzu.
            Lasse keine vorhandenen Informationen, Einschränkungen, Absichten, Namen, Zahlen oder Details weg.
            Wenn der Quelltext unklar ist, bewahre die Unklarheit, statt sie zu erfinden.
            Gib ausschließlich den verbesserten Text zurück, ohne Erklärung, Markdown, Überschrift oder Anführungszeichen.
        """.trimIndent()
        val prompt = listOfNotNull(basePrompt, extraInstruction?.trim()?.takeIf { it.isNotBlank() })
            .joinToString("\n\n")
        val body = """
        {
            "contents": [{"parts": [
                {"text": ${JSONObject.quote(prompt)}},
                {"text": ${JSONObject.quote(text)}}
            ]}],
            "generationConfig": {
                "thinkingConfig": {"thinkingBudget": 128},
                "temperature": 0.2,
                "maxOutputTokens": 4096
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val (code, responseBody) = executeForString(externalClient, request)
        if (responseBody.isEmpty()) throw Exception("Leere Gemini-Antwort")

        if (code !in 200..299) {
            throw Exception("Gemini-Fehler $code: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val finishReason = candidate?.optString("finishReason")
        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (blockReason.isNotBlank()) throw Exception("Gemini blockiert: $blockReason")
        if (finishReason == "MAX_TOKENS") {
            CortexLog.warn("Gemini", "improve", "MAX_TOKENS erreicht — Antwort evtl. unvollstaendig")
        }

        val result = candidate
            ?.getJSONObject("content")
            ?.getJSONArray("parts")
            ?.getJSONObject(0)
            ?.getString("text")
            ?: throw Exception("Kein Text in Gemini-Antwort")

        CortexLog.info("Gemini", "improve", "Text verbessert", mapOf("input_len" to text.length, "output_len" to result.length))
        return result
    }

    /**
     * Intentions-Titel fuer die Sessions-Leiste (Frank-Wunsch 2026-07-02): fasst die ABSICHT der
     * ersten Frage in maximal vier Woertern zusammen. Laeuft im Hintergrund auf gemini-3.1-flash-lite
     * mit Thinking medium (thinkingBudget 4096, gleiche Stufe wie serverseitig 'medium').
     */
    suspend fun geminiSessionTitle(text: String): String {
        val key = SettingsStore.geminiApiKey
        if (key.isBlank()) throw IllegalStateException("Gemini-Schlüssel fehlt")
        val prompt = """
            Fasse die Absicht der folgenden Nutzer-Nachricht als kurzen Titel zusammen.
            Der Titel soll sofort erkennen lassen, was der Nutzer wollte (die Intention, nicht den Wortlaut).
            Länge: maximal vier Wörter. Niemals mehr als vier Wörter.
            Deutsch, mit echten Umlauten. Keine neuen Informationen erfinden.
            Keine Anführungszeichen, kein Punkt am Ende, keine Erklärung, kein Markdown.
            Gib ausschließlich den Titel zurück.
        """.trimIndent()
        val body = """
        {
            "contents": [{"parts": [
                {"text": ${JSONObject.quote(prompt)}},
                {"text": ${JSONObject.quote(text.take(4000))}}
            ]}],
            "generationConfig": {
                "thinkingConfig": {"thinkingBudget": 4096},
                "temperature": 0.2,
                "maxOutputTokens": 4200
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val (code, responseBody) = executeForString(externalClient, request)
        if (responseBody.isEmpty()) throw Exception("Leere Gemini-Antwort")
        if (code !in 200..299) throw Exception("Gemini-Fehler $code: $responseBody")

        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (blockReason.isNotBlank()) throw Exception("Gemini blockiert: $blockReason")
        val result = candidate
            ?.getJSONObject("content")
            ?.getJSONArray("parts")
            ?.getJSONObject(0)
            ?.getString("text")
            ?.trim()
            ?.trim('"', '„', '“')
            ?: throw Exception("Kein Titel in Gemini-Antwort")
        CortexLog.info("Gemini", "sessionTitle", "Session-Titel erzeugt", mapOf("input_len" to text.length, "title" to result.take(80)))
        return result
    }

    suspend fun geminiTitle(text: String, attempt: Int = 1): String {
        val key = SettingsStore.geminiApiKey
        if (key.isBlank()) throw IllegalStateException("Gemini-Schlüssel fehlt")
        val variantInstruction = if (attempt > 1) {
            "\nDies ist Titelversuch $attempt. Erzeuge bewusst eine andere passende Titelvariante als zuvor, ohne Fakten zu erfinden."
        } else {
            ""
        }
        val prompt = """
            Erzeuge aus dem folgenden deutschen Eintrag einen kurzen, präzisen Titel.
            Der Titel soll den Inhalt oder die wichtigste Intention zusammenfassen.
            Maximal 6 bis 7 Wörter.
            Keine neuen Informationen hinzufügen.
            Keine Details erfinden.
            Keine Anführungszeichen, kein Punkt am Ende, keine Erklärung, kein Markdown.
            Gib ausschließlich den Titel zurück.
            $variantInstruction
        """.trimIndent()
        val body = """
        {
            "contents": [{"parts": [
                {"text": ${JSONObject.quote(prompt)}},
                {"text": ${JSONObject.quote(text)}}
            ]}],
            "generationConfig": {
                "thinkingConfig": {"thinkingBudget": 64},
                "temperature": ${if (attempt > 1) "0.55" else "0.15"},
                "maxOutputTokens": 128
            }
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val (code, responseBody) = executeForString(externalClient, request)
        if (responseBody.isEmpty()) throw Exception("Leere Gemini-Antwort")

        if (code !in 200..299) {
            throw Exception("Gemini-Fehler $code: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        if (blockReason.isNotBlank()) throw Exception("Gemini blockiert: $blockReason")

        val result = candidate
            ?.getJSONObject("content")
            ?.getJSONArray("parts")
            ?.getJSONObject(0)
            ?.getString("text")
            ?.trim()
            ?.trim('"', '„', '“', '.', ' ')
            ?: throw Exception("Kein Titel in Gemini-Antwort")

        CortexLog.info("Gemini", "title", "Titel erzeugt", mapOf("input_len" to text.length, "attempt" to attempt, "title" to result))
        return result
    }
}
