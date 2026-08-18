package de.frank.denknotiz.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import de.frank.denknotiz.DenknotizApplication
import de.frank.denknotiz.MainActivity
import de.frank.denknotiz.R
import de.frank.denknotiz.data.SettingsSnapshot
import de.frank.denknotiz.data.TtsProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

data class SpeechState(
    val active: Boolean = false,
    val paused: Boolean = false,
    val paragraphIndex: Int = -1,
    val paragraphCount: Int = 0,
    val provider: TtsProvider? = null,
    val paragraphText: String = "",
    val error: String = "",
)

object Paragraphs {
    const val MAX_CHARS = 1000
    const val PREFETCH = 2
    fun split(text: String): List<String> = text.replace("\r\n", "\n").replace('\r', '\n').trim()
        .split(Regex("\n{2,}")).map { it.replace('\n', ' ').replace(Regex("[ \\t]+"), " ").trim() }
        .filter(String::isNotBlank).flatMap(::splitLong)

    private fun splitLong(paragraph: String): List<String> {
        if (paragraph.length <= MAX_CHARS) return listOf(paragraph)
        val result = mutableListOf<String>()
        var current = ""
        paragraph.split(Regex("(?<=[.!?])\\s+")).forEach { sentence ->
            if (sentence.length > MAX_CHARS) {
                if (current.isNotBlank()) result += current.trim()
                current = ""
                var rest = sentence.trim()
                while (rest.length > MAX_CHARS) {
                    val index = rest.take(MAX_CHARS).lastIndexOf(' ').takeIf { it > MAX_CHARS / 2 } ?: MAX_CHARS
                    result += rest.take(index).trim(); rest = rest.drop(index).trim()
                }
                if (rest.isNotBlank()) current = rest
            } else {
                val next = if (current.isBlank()) sentence else "$current $sentence"
                if (next.length <= MAX_CHARS) current = next else { result += current.trim(); current = sentence }
            }
        }
        if (current.isNotBlank()) result += current.trim()
        return result
    }
}

class SpeechController(private val context: Context) {
    val state: StateFlow<SpeechState> = SpeechService.state
    fun play(text: String) = send(SpeechService.ACTION_PLAY) { putExtra(SpeechService.EXTRA_TEXT, text) }
    fun pauseResume() = send(SpeechService.ACTION_TOGGLE)
    fun stop() = send(SpeechService.ACTION_STOP)
    fun previous() = send(SpeechService.ACTION_PREVIOUS)
    fun next() = send(SpeechService.ACTION_NEXT)
    private fun send(action: String, configure: Intent.() -> Unit = {}) {
        val intent = Intent(context, SpeechService::class.java).setAction(action).apply(configure)
        if (action == SpeechService.ACTION_PLAY) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
    }
}

class SpeechService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val player = FilePlayer()
    private var job: Job? = null
    private var paragraphs: List<String> = emptyList()
    private var currentIndex = 0
    private val settings: SettingsSnapshot get() = (application as DenknotizApplication).container.settings.state.value

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                paragraphs = Paragraphs.split(intent.getStringExtra(EXTRA_TEXT).orEmpty())
                currentIndex = 0
                startForeground(NOTIFICATION_ID, notification())
                startFrom(0)
            }
            ACTION_TOGGLE -> toggle()
            ACTION_STOP -> finishPlayback()
            ACTION_PREVIOUS -> if (paragraphs.isNotEmpty()) startFrom((currentIndex - 1).coerceAtLeast(0))
            ACTION_NEXT -> if (paragraphs.isNotEmpty()) startFrom((currentIndex + 1).coerceAtMost(paragraphs.lastIndex))
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startFrom(index: Int) {
        job?.cancel(); player.stop()
        if (paragraphs.isEmpty()) { finishPlayback(); return }
        currentIndex = index
        val selected = settings
        stateMutable.value = SpeechState(true, false, index, paragraphs.size, selected.ttsProvider, paragraphs[index])
        updateNotification()
        job = scope.launch {
            try {
                playPipeline(index, selected)
                finishPlayback()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                stateMutable.value = stateMutable.value.copy(active = false, paused = false,
                    error = readableError(error, selected.ttsProvider))
                updateNotification(); stopForeground(STOP_FOREGROUND_DETACH); stopSelf()
            }
        }
    }

    private suspend fun playPipeline(start: Int, selected: SettingsSnapshot) = coroutineScope {
        val pending = mutableMapOf<Int, Deferred<File>>()
        fun enqueue(index: Int) {
            if (index in paragraphs.indices && pending[index] == null) pending[index] = async(Dispatchers.IO) {
                val bytes = synthesize(paragraphs[index], selected)
                File(cacheDir, "speech_${UUID.randomUUID()}.audio").apply { writeBytes(bytes) }
            }
        }
        repeat(minOf(Paragraphs.PREFETCH + 1, paragraphs.size - start)) { enqueue(start + it) }
        try {
            for (index in start..paragraphs.lastIndex) {
                currentIndex = index
                stateMutable.value = stateMutable.value.copy(paragraphIndex = index, paragraphText = paragraphs[index])
                updateNotification()
                val file = pending.remove(index)?.await() ?: continue
                enqueue(index + Paragraphs.PREFETCH + 1)
                try { player.playAndWait(file) } finally { file.delete() }
            }
        } finally {
            pending.values.forEach { it.cancel() }
            pending.clear()
        }
    }

    private suspend fun synthesize(text: String, selected: SettingsSnapshot): ByteArray = when (selected.ttsProvider) {
        TtsProvider.CHIRP -> GoogleSynthesizer.synthesize(text, selected.googleKey, selected.chirpVoice, selected.speechRate)
        TtsProvider.EDGE -> EdgeSynthesizer.synthesize(text, selected.edgeVoice, selected.speechRate)
        TtsProvider.QWEN -> QwenSynthesizer.synthesize(text, selected.qwenVoiceId, selected.qwenKey)
    }

    private fun toggle() {
        if (!stateMutable.value.active) return
        if (stateMutable.value.paused) {
            if (player.resume()) stateMutable.value = stateMutable.value.copy(paused = false)
        } else if (player.pause()) stateMutable.value = stateMutable.value.copy(paused = true)
        updateNotification()
    }

    private fun finishPlayback() {
        job?.cancel(); job = null; player.stop()
        stateMutable.value = SpeechState()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    private fun notification(): Notification {
        val launch = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(value: String, request: Int) = PendingIntent.getService(this, request,
            Intent(this, SpeechService::class.java).setAction(value), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val current = stateMutable.value
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_app_icon).setContentTitle("Denknotiz liest vor")
            .setContentText(if (current.paragraphCount > 0) "Absatz ${current.paragraphIndex + 1} von ${current.paragraphCount}" else "Wird vorbereitet")
            .setContentIntent(launch).setOnlyAlertOnce(true).setOngoing(current.active)
            .addAction(0, "Zurück", action(ACTION_PREVIOUS, 2))
            .addAction(0, if (current.paused) "Weiter" else "Pause", action(ACTION_TOGGLE, 3))
            .addAction(0, "Vor", action(ACTION_NEXT, 4))
            .addAction(0, "Stopp", action(ACTION_STOP, 5)).build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Vorlesen", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onDestroy() { job?.cancel(); player.stop(); scope.cancel(); super.onDestroy() }

    companion object {
        const val ACTION_PLAY = "de.frank.denknotiz.PLAY"
        const val ACTION_TOGGLE = "de.frank.denknotiz.TOGGLE"
        const val ACTION_STOP = "de.frank.denknotiz.STOP"
        const val ACTION_PREVIOUS = "de.frank.denknotiz.PREVIOUS"
        const val ACTION_NEXT = "de.frank.denknotiz.NEXT"
        const val EXTRA_TEXT = "text"
        private const val CHANNEL = "speech"
        private const val NOTIFICATION_ID = 4102
        private val stateMutable = MutableStateFlow(SpeechState())
        val state: StateFlow<SpeechState> = stateMutable.asStateFlow()
    }
}

private class FilePlayer {
    private val lock = Any()
    private var media: MediaPlayer? = null
    suspend fun playAndWait(file: File) = suspendCancellableCoroutine { continuation ->
        val done = AtomicBoolean(false)
        val next = MediaPlayer()
        synchronized(lock) {
            stop()
            media = next
            try {
                next.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                next.setDataSource(file.absolutePath)
                next.setOnCompletionListener { if (done.compareAndSet(false, true)) { release(next); continuation.resume(Unit) } }
                next.setOnErrorListener { _, what, extra ->
                    if (done.compareAndSet(false, true)) { release(next); continuation.resumeWithException(IOException("Wiedergabefehler $what/$extra")) }
                    true
                }
                next.prepare(); next.start()
            } catch (error: Exception) {
                if (done.compareAndSet(false, true)) { release(next); continuation.resumeWithException(error) }
            }
        }
        continuation.invokeOnCancellation { if (done.compareAndSet(false, true)) release(next) }
    }
    fun pause(): Boolean = synchronized(lock) { runCatching { media?.pause(); media != null }.getOrDefault(false) }
    fun resume(): Boolean = synchronized(lock) { runCatching { media?.start(); media != null }.getOrDefault(false) }
    fun stop() = synchronized(lock) { media?.let(::release) }
    private fun release(value: MediaPlayer) = synchronized(lock) {
        if (media === value) media = null
        runCatching { value.stop() }; value.release()
    }
}

private object GoogleSynthesizer {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    suspend fun synthesize(text: String, key: String, voice: String, rate: Float): ByteArray = withContext(Dispatchers.IO) {
        require(key.isNotBlank()) { "Google-TTS-Schlüssel fehlt." }
        val body = JSONObject().put("input", JSONObject().put("text", text))
            .put("voice", JSONObject().put("languageCode", "de-DE").put("name", voice))
            .put("audioConfig", JSONObject().put("audioEncoding", "MP3").put("speakingRate", rate.coerceIn(0.7f, 1.3f)))
        val url = "https://texttospeech.googleapis.com/v1/text:synthesize".toHttpUrl().newBuilder().addQueryParameter("key", key.trim()).build()
        client.newCall(Request.Builder().url(url).post(body.toString().toRequestBody(JSON)).build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Google TTS ${response.code}: ${raw.take(300)}")
            Base64.decode(JSONObject(raw).getString("audioContent"), Base64.DEFAULT)
        }
    }
}

private object QwenSynthesizer {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build()
    suspend fun synthesize(text: String, voice: String, key: String): ByteArray = withContext(Dispatchers.IO) {
        require(key.isNotBlank() && voice.isNotBlank()) { "Qwen-Schlüssel oder Stimme fehlt." }
        val body = JSONObject().put("model", QWEN_MODEL)
            .put("input", JSONObject().put("text", text).put("voice", voice.trim()).put("language_type", "German"))
        val request = Request.Builder().url("https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
            .header("Authorization", "Bearer ${key.filterNot(Char::isWhitespace)}").post(body.toString().toRequestBody(JSON)).build()
        val url = client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Qwen TTS ${response.code}: ${raw.take(300)}")
            JSONObject(raw).optJSONObject("output")?.optJSONObject("audio")?.optString("url").orEmpty()
                .ifBlank { throw IOException("Qwen hat keinen Ton-Link geliefert.") }
        }.replace("http://", "https://")
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Qwen-Audio ${response.code}")
            response.body?.bytes() ?: throw IOException("Qwen-Audio ist leer.")
        }
    }
}

private object EdgeSynthesizer {
    private const val TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val VERSION = "143.0.3650.75"
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).pingInterval(15, TimeUnit.SECONDS).build()
    suspend fun synthesize(text: String, voice: String, rate: Float): ByteArray = withTimeout(45_000L) {
        suspendCancellableCoroutine { continuation ->
            val connection = UUID.randomUUID().toString().replace("-", "")
            val requestId = UUID.randomUUID().toString().replace("-", "")
            val ticks = ((((System.currentTimeMillis() / 1000.0) + 11644473600L) / 300).toLong() * 300 * 1e7).toLong()
            val gec = MessageDigest.getInstance("SHA-256").digest("$ticks$TOKEN".toByteArray(Charsets.US_ASCII))
                .joinToString("") { "%02X".format(it) }
            val url = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                "?TrustedClientToken=$TOKEN&Sec-MS-GEC=$gec&Sec-MS-GEC-Version=1-$VERSION&ConnectionId=$connection"
            val request = Request.Builder().url(url).header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("User-Agent", "Mozilla/5.0 Edg/143.0.0.0").build()
            val output = ByteArrayOutputStream(); val done = AtomicBoolean(false)
            val socket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                        "{\"context\":{\"synthesis\":{\"audio\":{\"outputFormat\":\"audio-24khz-96kbitrate-mono-mp3\"}}}}")
                    val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    val percent = ((rate.coerceIn(0.7f, 1.3f) - 1f) * 100).toInt()
                    webSocket.send("X-RequestId:$requestId\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
                        "<speak version='1.0' xml:lang='de-DE'><voice name='$voice'><prosody rate='$percent%'>$escaped</prosody></voice></speak>")
                }
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val data = bytes.toByteArray(); if (data.size > 2) {
                        val header = ((data[0].toInt() and 255) shl 8) or (data[1].toInt() and 255)
                        if (data.size > header + 2) output.write(data, header + 2, data.size - header - 2)
                    }
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("Path:turn.end") && done.compareAndSet(false, true)) {
                        webSocket.close(1000, null); continuation.resume(output.toByteArray())
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (done.compareAndSet(false, true)) continuation.resumeWithException(t)
                }
            })
            continuation.invokeOnCancellation { socket.cancel() }
        }
    }
}

private val JSON = "application/json; charset=utf-8".toMediaType()
const val QWEN_MODEL = "qwen3-tts-vc-2026-01-22"

private fun readableError(error: Exception, provider: TtsProvider): String {
    val raw = error.message.orEmpty()
    return when {
        raw.contains("401") || raw.contains("403") -> "${provider.label}: Der Schlüssel wurde abgelehnt."
        raw.contains("429") -> "${provider.label}: Das Kontingent ist erschöpft."
        raw.contains("timeout", true) || raw.contains("resolve", true) -> "${provider.label}: Netzwerk nicht erreichbar."
        else -> "${provider.label}: ${raw.ifBlank { "Vorlesen fehlgeschlagen." }}"
    }
}
