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
        if (apiKey.isBlank()) throw TtsException("Google Cloud TTS ist nicht konfiguriert.")
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
        if (apiKey.isBlank() || voice.isBlank()) throw TtsException("Qwen-Stimmklon ist nicht konfiguriert.")
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

private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

private fun cloudClient(readSeconds: Long = 60): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(readSeconds, TimeUnit.SECONDS)
    .callTimeout(readSeconds + 30, TimeUnit.SECONDS)
    .build()

private fun parseHttpDate(value: String?): Long? = runCatching {
    ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
}.getOrNull()
