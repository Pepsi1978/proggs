package de.frank.stacklabor.werftstudio.service.tts

import android.util.Base64
import de.frank.stacklabor.werftstudio.service.network.awaitResponse
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject

class GoogleCloudTtsProvider(
    private val credentials: TtsCredentials,
    private val cache: TtsAudioCache,
    private val usageStore: TtsUsageStore,
    private val client: OkHttpClient = cloudClient(),
) : TtsProvider {
    override val id: TtsProviderId = TtsProviderId.GOOGLE_CLOUD

    override suspend fun synthesize(request: TtsSynthesisRequest): SynthesizedAudio = withContext(Dispatchers.IO) {
        val key = cache.key(id, MODEL, request)
        cache.find(key, "mp3")?.let { return@withContext SynthesizedAudio(it, "audio/mpeg", true, 0) }
        val apiKey = credentials.googleApiKey()?.trim().orEmpty()
        if (apiKey.isBlank()) throw TtsException("Bitte in den Einstellungen den Google-API-Schlüssel eintragen.")
        val body = JSONObject()
            .put("input", JSONObject().put("text", request.text))
            .put(
                "voice",
                JSONObject().put("languageCode", "de-DE").put("name", request.voiceId),
            )
            .put(
                "audioConfig",
                JSONObject()
                    .put("audioEncoding", "MP3")
                    .put("speakingRate", request.speechRate.coerceIn(0.7f, 1.3f).toDouble()),
            )
        val httpRequest = Request.Builder()
            .url(GOOGLE_TTS_URL)
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val bytes = client.newCall(httpRequest).awaitResponse().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw TtsException("Google Cloud TTS meldet HTTP ${response.code}.")
            val encoded = runCatching { JSONObject(responseBody).getString("audioContent") }
                .getOrElse { throw TtsException("Google Cloud TTS hat keine Audiodaten geliefert.", it) }
            Base64.decode(encoded, Base64.DEFAULT).takeIf(ByteArray::isNotEmpty)
                ?: throw TtsException("Google Cloud TTS hat leere Audiodaten geliefert.")
        }
        val file = cache.put(key, "mp3", bytes)
        usageStore.add(id, request.text.length)
        SynthesizedAudio(file, "audio/mpeg", false, request.text.length)
    }

    private companion object {
        const val MODEL = "chirp3-hd"
        const val GOOGLE_TTS_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
    }
}

class QwenTtsProvider(
    private val credentials: TtsCredentials,
    private val cache: TtsAudioCache,
    private val usageStore: TtsUsageStore,
    private val client: OkHttpClient = cloudClient(readSeconds = 90),
) : TtsProvider {
    override val id: TtsProviderId = TtsProviderId.QWEN

    override suspend fun synthesize(request: TtsSynthesisRequest): SynthesizedAudio = withContext(Dispatchers.IO) {
        val configuredVoice = credentials.qwenVoiceId()?.filterNot(Char::isWhitespace).orEmpty()
        val voice = request.voiceId.takeIf { it.isNotBlank() && it != TtsConfiguration.DEFAULT_EDGE_VOICE }
            ?: configuredVoice
        val normalizedRequest = request.copy(voiceId = voice)
        val key = cache.key(id, MODEL, normalizedRequest)
        cache.find(key, "wav")?.let { return@withContext SynthesizedAudio(it, "audio/wav", true, 0) }
        val apiKey = credentials.qwenApiKey()?.filterNot(Char::isWhitespace).orEmpty()
        if (apiKey.isBlank()) throw TtsException("Bitte in den Einstellungen den Alibaba-Schlüssel (DashScope) eintragen.")
        if (voice.isBlank()) throw TtsException("Bitte in den Einstellungen die Alibaba-Stimmen-ID eintragen.")
        val body = JSONObject()
            .put("model", MODEL)
            .put(
                "input",
                JSONObject().put("text", request.text).put("voice", voice).put("language_type", "German"),
            )
        val synthesis = Request.Builder()
            .url(QWEN_TTS_URL)
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val audioUrl = client.newCall(synthesis).awaitResponse().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw TtsException("Qwen TTS meldet HTTP ${response.code}.")
            JSONObject(responseBody).optJSONObject("output")?.optJSONObject("audio")?.optString("url")
                ?.takeIf(String::isNotBlank)
                ?.replace(Regex("^http://"), "https://")
                ?: throw TtsException("Qwen TTS hat keinen Audiolink geliefert.")
        }
        val bytes = client.newCall(Request.Builder().url(audioUrl).build()).awaitResponse().use { response ->
            if (!response.isSuccessful) throw TtsException("Qwen-Audiodownload meldet HTTP ${response.code}.")
            response.body?.bytes()?.takeIf(ByteArray::isNotEmpty)
                ?: throw TtsException("Qwen TTS hat leere Audiodaten geliefert.")
        }
        val file = cache.put(key, "wav", bytes)
        usageStore.add(id, request.text.length)
        SynthesizedAudio(file, "audio/wav", false, request.text.length)
    }

    companion object {
        const val MODEL = "qwen3-tts-vc-2026-01-22"
        private const val QWEN_TTS_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
    }
}

class EdgeTtsProvider(
    private val cache: TtsAudioCache,
    private val usageStore: TtsUsageStore,
    private val client: OkHttpClient = cloudClient(),
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : TtsProvider {
    override val id: TtsProviderId = TtsProviderId.EDGE
    private val clockSkewMillis = AtomicLong(0L)

    override suspend fun synthesize(request: TtsSynthesisRequest): SynthesizedAudio = withContext(Dispatchers.IO) {
        val key = cache.key(id, CHROMIUM_FULL_VERSION, request)
        cache.find(key, "mp3")?.let { return@withContext SynthesizedAudio(it, "audio/mpeg", true, 0) }
        val bytes = try {
            synthesizeOnce(request)
        } catch (error: EdgeHandshakeException) {
            val serverMillis = error.serverDateMillis ?: throw error
            clockSkewMillis.set(serverMillis - clockMillis())
            synthesizeOnce(request)
        }
        val file = cache.put(key, "mp3", bytes)
        usageStore.add(id, request.text.length)
        SynthesizedAudio(file, "audio/mpeg", false, request.text.length)
    }

    private suspend fun synthesizeOnce(request: TtsSynthesisRequest): ByteArray = withTimeout(60_000L) {
        suspendCancellableCoroutine { continuation ->
            val connectionId = UUID.randomUUID().toString().replace("-", "")
            val requestId = UUID.randomUUID().toString().replace("-", "")
            val url = "$EDGE_TTS_URL?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                "&Sec-MS-GEC=${secMsGec()}&Sec-MS-GEC-Version=1-$CHROMIUM_FULL_VERSION" +
                "&ConnectionId=$connectionId"
            val webSocketRequest = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR.0.0.0 Safari/537.36 Edg/$CHROMIUM_MAJOR.0.0.0",
                )
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("Cookie", "muid=${UUID.randomUUID().toString().replace("-", "").uppercase()};")
                .build()
            val output = ByteArrayOutputStream()
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val config = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                        "{\"context\":{\"synthesis\":{\"audio\":{\"metadataOptions\":" +
                        "{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"}," +
                        "\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                    val rate = ((request.speechRate.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
                    val rateText = if (rate >= 0) "+$rate%" else "$rate%"
                    val ssml = "X-RequestId:$requestId\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
                        "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
                        "<voice name='${request.voiceId}'><prosody rate='$rateText' pitch='+0Hz'>" +
                        "${escapeXml(request.text)}</prosody></voice></speak>"
                    if (!webSocket.send(config) || !webSocket.send(ssml)) {
                        continuation.resumeWith(Result.failure(TtsException("Edge TTS konnte die Anfrage nicht senden.")))
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val data = bytes.toByteArray()
                    if (data.size <= 2) return
                    val headerLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                    val audioStart = 2 + headerLength
                    if (audioStart < data.size) output.write(data, audioStart, data.size - audioStart)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!text.contains("Path:turn.end") || !continuation.isActive) return
                    val audio = output.toByteArray()
                    if (audio.isEmpty()) {
                        continuation.resumeWith(Result.failure(TtsException("Edge TTS hat keine Audiodaten geliefert.")))
                    } else {
                        continuation.resume(audio) { _, _, _ -> }
                    }
                    webSocket.close(1000, null)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (!continuation.isActive) return
                    val exception = if (response?.code == 403) {
                        EdgeHandshakeException(parseHttpDate(response.header("Date")), t)
                    } else {
                        TtsException("Edge TTS ist nicht erreichbar.", t)
                    }
                    continuation.resumeWith(Result.failure(exception))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(TtsException("Edge TTS hat die Verbindung vorzeitig beendet.")))
                    }
                }
            }
            val socket = client.newWebSocket(webSocketRequest, listener)
            continuation.invokeOnCancellation { socket.cancel() }
        }
    }

    private fun secMsGec(): String {
        val unixSeconds = (clockMillis() + clockSkewMillis.get()) / 1_000L
        val roundedSeconds = unixSeconds - unixSeconds % 300L
        val ticks = (roundedSeconds + WINDOWS_EPOCH_SECONDS) * 10_000_000L
        return MessageDigest.getInstance("SHA-256")
            .digest("$ticks$TRUSTED_CLIENT_TOKEN".toByteArray(Charsets.US_ASCII))
            .joinToString("") { byte -> String.format(Locale.ROOT, "%02X", byte) }
    }

    private fun escapeXml(text: String): String = text
        .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " ")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private class EdgeHandshakeException(val serverDateMillis: Long?, cause: Throwable) :
        TtsException("Edge TTS hat die Anmeldung abgelehnt.", cause)

    private companion object {
        const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        const val CHROMIUM_MAJOR = "143"
        const val WINDOWS_EPOCH_SECONDS = 11_644_473_600L
        const val EDGE_TTS_URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
    }
}

/**
 * Gemini 3.8 Flash or Flash-Lite TTS. Order per [TtsCredentials.geminiKeyChoice]: first the free key, then the paid
 * one. If [TtsCredentials.geminiChirpFallback] is on, a failure of the free key does not go to the paid key but to
 * Chirp 3 HD with the same voice ([chirpFallback]). Network errors pass through unchanged.
 */
class GeminiTtsProvider(
    override val id: TtsProviderId,
    private val model: String,
    private val credentials: TtsCredentials,
    private val cache: TtsAudioCache,
    private val usageStore: TtsUsageStore,
    private val chirpFallback: TtsProvider,
    private val client: OkHttpClient = cloudClient(readSeconds = 90),
) : TtsProvider {

    override suspend fun synthesize(request: TtsSynthesisRequest): SynthesizedAudio = withContext(Dispatchers.IO) {
        val key = cache.key(id, model, request)
        cache.find(key, "wav")?.let { return@withContext SynthesizedAudio(it, "audio/wav", true, 0) }
        val choice = credentials.geminiKeyChoice()
        val free = credentials.geminiFreeKey()?.filterNot(Char::isWhitespace).orEmpty()
        val paid = credentials.geminiPaidKey()?.filterNot(Char::isWhitespace).orEmpty()
        val chirp = credentials.geminiChirpFallback()
        var freeError: TtsException? = null
        if (choice != GeminiKeyChoice.PAID_ONLY) {
            val blocked = System.currentTimeMillis() < freeBlockedUntil
            if (free.isNotBlank() && !blocked) {
                // With an escape route do not wait on 429 — the next route is faster than any wait.
                val escape = chirp || (choice == GeminiKeyChoice.FREE_THEN_PAID && paid.isNotBlank())
                try {
                    return@withContext store(key, request, geminiRequest(request, free, waitOn429 = !escape))
                } catch (error: TtsException) {
                    freeError = error
                    if (escape) freeBlockedUntil = System.currentTimeMillis() + FREE_PAUSE_MS
                    android.util.Log.w(TAG, "Gemini free key failed (model=$model): ${error.message?.take(120)}")
                }
            }
            if (chirp) {
                android.util.Log.i(TAG, "Gemini voice falls back to Chirp 3 HD (blocked=$blocked)")
                return@withContext chirpFallback.synthesize(request.copy(voiceId = TtsVoices.chirpFor(request.voiceId)))
            }
            if (choice == GeminiKeyChoice.FREE_ONLY) {
                throw freeError ?: TtsException("Bitte in den Einstellungen den kostenlosen Gemini-Schlüssel für die Stimme eintragen.")
            }
        }
        if (paid.isBlank()) {
            throw freeError ?: TtsException("Bitte in den Einstellungen einen Gemini-Schlüssel für die Stimme eintragen.")
        }
        store(key, request, geminiRequest(request, paid, waitOn429 = true))
    }

    private fun store(key: String, request: TtsSynthesisRequest, wav: ByteArray): SynthesizedAudio {
        val file = cache.put(key, "wav", wav)
        usageStore.add(id, request.text.length)
        return SynthesizedAudio(file, "audio/wav", false, request.text.length)
    }

    private suspend fun geminiRequest(request: TtsSynthesisRequest, apiKey: String, waitOn429: Boolean): ByteArray {
        // Gemini reads the text literally; an instruction inside the text would be spoken too.
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", request.text)))))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", TtsVoices.geminiName(request.voiceId))),
                        ),
                    ),
            )
        val httpRequest = Request.Builder()
            .url("$GEMINI_URL$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val responseBody = post(httpRequest, waitOn429)
        val candidate = runCatching { JSONObject(responseBody).optJSONArray("candidates")?.optJSONObject(0) }.getOrNull()
        val finish = candidate?.optString("finishReason").orEmpty()
        val part = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("inlineData")
        val data = part?.optString("data").orEmpty()
        if (data.isBlank()) throw TtsException("Gemini hat kein Audio geliefert ($finish).")
        if (finish.isNotBlank() && finish != "STOP") throw TtsException("Gemini hat die Stimme abgebrochen ($finish).")
        val raw = Base64.decode(data, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(part?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val wav = if (raw.size > 44 && String(raw, 0, 4, Charsets.US_ASCII) == "RIFF") raw else withWavHeader(raw, rate)
        // Gemini sometimes reports cut-off audio as finished: nobody speaks faster than 25 characters per second.
        val seconds = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (request.text.length >= 60 && seconds < request.text.length / 25.0) {
            throw TtsException("Gemini-Audio zu kurz (${"%.1f".format(seconds)} s für ${request.text.length} Zeichen).")
        }
        return wav
    }

    private suspend fun post(httpRequest: Request, waitOn429: Boolean): String {
        var attempt = 0
        while (true) {
            val (text, code) = client.newCall(httpRequest).awaitResponse().use { it.body?.string().orEmpty() to it.code }
            when {
                code in 200..299 -> return text
                code == 429 && waitOn429 && attempt < WAIT_MS.size -> { delay(WAIT_MS[attempt]); attempt++ }
                code == 401 || code == 403 -> throw TtsException("Gemini hat den Schlüssel abgelehnt (HTTP $code). Bitte in den Einstellungen prüfen.")
                code == 429 -> throw TtsException("Das Kontingent der Gemini-Stimme ist erschöpft (HTTP 429).")
                else -> throw TtsException("Gemini TTS meldet HTTP $code: ${text.take(200)}")
            }
        }
    }

    /** Raw PCM (16 bit, mono) gets a WAV header so the player can play it. */
    private fun withWavHeader(pcm: ByteArray, rate: Int): ByteArray {
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size)
        return header.array() + pcm
    }

    private fun wavRate(wav: ByteArray): Int =
        java.nio.ByteBuffer.wrap(wav, 24, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.takeIf { it > 0 } ?: 24_000

    companion object {
        const val FLASH_MODEL = "gemini-3.8-flash-tts"
        const val LITE_MODEL = "gemini-3.8-flash-lite-tts"
        private const val TAG = "GeminiTts"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        /** How long the free Gemini key stays skipped after a failure when there is an escape route. */
        private const val FREE_PAUSE_MS = 10 * 60_000L

        /** Shared by all instances (Flash and Lite): otherwise every new reader would retry the failed key. */
        @Volatile private var freeBlockedUntil = 0L
        private val WAIT_MS = longArrayOf(1_000L, 3_000L, 8_000L)
    }
}

/**
 * Azure AI Speech via REST: key and region of the own Speech resource (F0 free or S0).
 * HD voices (DragonHD) get neither tempo nor a <lang> frame: they choose stress and tempo themselves.
 */
class AzureTtsProvider(
    private val credentials: TtsCredentials,
    private val cache: TtsAudioCache,
    private val usageStore: TtsUsageStore,
    private val client: OkHttpClient = cloudClient(),
) : TtsProvider {
    override val id: TtsProviderId = TtsProviderId.AZURE

    override suspend fun synthesize(request: TtsSynthesisRequest): SynthesizedAudio = withContext(Dispatchers.IO) {
        val key = cache.key(id, MODEL, request)
        cache.find(key, "mp3")?.let { return@withContext SynthesizedAudio(it, "audio/mpeg", true, 0) }
        val apiKey = credentials.azureKey()?.filterNot(Char::isWhitespace).orEmpty()
        if (apiKey.isBlank()) throw TtsException("Bitte in den Einstellungen den Azure-Speech-Schlüssel eintragen.")
        val regionInput = credentials.azureRegion()
        val region = TtsVoices.azureRegion(regionInput) ?: throw TtsException(
            "Die Azure-Region „${regionInput.take(40)}“ ist unbekannt. Bitte den Regionsnamen eintragen, etwa westeurope.",
        )
        val voice = TtsVoices.azureName(request.voiceId)
        val hd = TtsVoices.isAzureHd(request.voiceId)
        // Control characters are forbidden in XML; Azure would reject the whole SSML with 400.
        val escaped = request.text
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " ")
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        // Azure knows <lang> only for the multilingual voices; the others speak German anyway.
        val content = if (voice.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val percent = ((request.speechRate.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
        val withRate = if (hd || percent == 0) content else "<prosody rate='${if (percent > 0) "+" else ""}$percent%'>$content</prosody>"
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$voice'>$withRate</voice></speak>"
        val httpRequest = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .header("Ocp-Apim-Subscription-Key", apiKey)
            .header("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .header("User-Agent", "StackLabor")
            .post(ssml.toRequestBody(SSML_MEDIA_TYPE))
            .build()
        val bytes = azureRequest(httpRequest, hd)
        if (bytes.isEmpty()) throw TtsException("Azure hat kein Audio geliefert.")
        val file = cache.put(key, "mp3", bytes)
        usageStore.add(id, request.text.length)
        SynthesizedAudio(file, "audio/mpeg", false, request.text.length)
    }

    /**
     * F0 allows only about 20 requests per minute: a 429 is mostly this rate limit, not the monthly quota —
     * so wait per Retry-After (at most one minute).
     */
    private suspend fun azureRequest(httpRequest: Request, hd: Boolean): ByteArray {
        var attempt = 0
        while (true) {
            val response = client.newCall(httpRequest).awaitResponse()
            val retryAfter = response.header("Retry-After")?.trim()?.toLongOrNull()
            val (body, code) = response.use {
                (if (it.isSuccessful) it.body?.bytes() else it.body?.string()?.toByteArray()) to it.code
            }
            when {
                code in 200..299 -> return body ?: ByteArray(0)
                code == 429 && attempt < WAIT_MS.size -> {
                    delay(retryAfter?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: WAIT_MS[attempt])
                    attempt++
                }
                code == 401 || code == 403 ->
                    throw TtsException("Azure hat Schlüssel oder Region abgelehnt (HTTP $code). Bitte beides in den Einstellungen prüfen.")
                code == 429 ->
                    throw TtsException("Azure lässt gerade keine Anfragen zu (HTTP 429): Ratenlimit oder Monatskontingent erschöpft.")
                code == 400 && hd ->
                    throw TtsException("Azure lehnt die HD-Stimme ab (HTTP 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.")
                else -> throw TtsException("Azure TTS meldet HTTP $code: ${body?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
    }

    private companion object {
        const val MODEL = "azure-neural"

        /** Together they span the one-minute window of F0. */
        val WAIT_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
        val SSML_MEDIA_TYPE = "application/ssml+xml".toMediaType()
    }
}

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

private fun cloudClient(readSeconds: Long = 60): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(readSeconds, TimeUnit.SECONDS)
    .callTimeout(readSeconds + 30, TimeUnit.SECONDS)
    .build()

private fun parseHttpDate(value: String?): Long? = runCatching {
    ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
}.getOrNull()
