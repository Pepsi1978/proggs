package de.frank.karteikartenlernen.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class EdgeTtsPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var webSocket: WebSocket? = null
    private var watchdogJob: Job? = null
    @Volatile private var currentOutputStream: FileOutputStream? = null
    @Volatile private var generation = 0L

    fun speak(
        text: String,
        voice: String = TtsVoiceRegistry.DEFAULT_VOICE_ID,
        speechRate: Float = 1f,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit = {},
    ) {
        stop()
        if (text.isBlank()) {
            onComplete()
            return
        }

        val requestGeneration = generation
        val resolvedVoice = TtsVoiceRegistry.resolveVoiceId(voice)
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val request = Request.Builder()
            .url(
                "$EDGE_TTS_URL?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                    "&Sec-MS-GEC=${generateSecMsGec()}" +
                    "&Sec-MS-GEC-Version=1-$CHROMIUM_FULL_VERSION" +
                    "&ConnectionId=$connectionId",
            )
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36 " +
                    "Edg/$CHROMIUM_MAJOR_VERSION.0.0.0",
            )
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Cookie", "muid=${generateMuid()};")
            .build()

        val audioFile = File(appContext.cacheDir, "tts_audio.mp3")
        val outputStream = try {
            FileOutputStream(audioFile).also { currentOutputStream = it }
        } catch (error: Exception) {
            Log.e(TAG, "Could not open TTS cache file", error)
            onComplete()
            return
        }

        try {
            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (requestGeneration != generation) return
                        startWatchdog(webSocket, WATCHDOG_INITIAL_MS, requestGeneration, onComplete)
                        webSocket.send(SPEECH_CONFIG)
                        webSocket.send(buildSsmlFrame(requestId, text, resolvedVoice, speechRate))
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        if (requestGeneration != generation) return
                        val data = bytes.toByteArray()
                        if (data.size <= 2) return
                        val headerLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                        val audioStart = headerLength + 2
                        if (data.size > audioStart) {
                            runCatching { outputStream.write(data, audioStart, data.size - audioStart) }
                                .onFailure { Log.e(TAG, "Could not write TTS audio", it) }
                            startWatchdog(webSocket, WATCHDOG_IDLE_MS, requestGeneration, onComplete)
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (requestGeneration != generation || !text.contains("Path:turn.end")) return
                        cancelWatchdog()
                        closeOutputStream(outputStream)
                        scope.launch {
                            if (requestGeneration == generation) {
                                playFile(audioFile, requestGeneration, onPlaybackStart, onComplete)
                            }
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, error: Throwable, response: Response?) {
                        if (requestGeneration != generation) return
                        Log.e(TAG, "WebSocket failure: ${error.message}", error)
                        cancelWatchdog()
                        closeOutputStream(outputStream)
                        scope.launch { if (requestGeneration == generation) onComplete() }
                    }
                },
            )
        } catch (error: Exception) {
            Log.e(TAG, "Creating Edge TTS WebSocket failed", error)
            closeOutputStream(outputStream)
            if (requestGeneration == generation) onComplete()
        }
    }

    fun stop() {
        generation++
        cancelWatchdog()
        webSocket?.cancel()
        webSocket = null
        closeOutputStream(currentOutputStream)
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun startWatchdog(webSocket: WebSocket, timeoutMs: Long, requestGeneration: Long, onComplete: () -> Unit) {
        cancelWatchdog()
        watchdogJob = scope.launch {
            delay(timeoutMs)
            if (requestGeneration != generation) return@launch
            Log.w(TAG, "No Edge TTS audio received for ${timeoutMs}ms; aborting")
            generation++
            webSocket.cancel()
            closeOutputStream(currentOutputStream)
            onComplete()
        }
    }

    private fun cancelWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    private fun playFile(
        file: File,
        requestGeneration: Long,
        onPlaybackStart: (() -> Unit)?,
        onComplete: () -> Unit,
    ) {
        mediaPlayer?.release()
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                if (requestGeneration == generation) onComplete()
                file.delete()
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                if (requestGeneration == generation) onComplete()
                file.delete()
                true
            }
            }
            player.prepare()
            player.start()
            onPlaybackStart?.invoke()
        } catch (error: Exception) {
            Log.e(TAG, "Could not play Edge TTS audio", error)
            player.release()
            if (mediaPlayer === player) mediaPlayer = null
            file.delete()
            if (requestGeneration == generation) onComplete()
        }
    }

    private fun closeOutputStream(stream: FileOutputStream?) {
        runCatching { stream?.close() }
        if (currentOutputStream === stream) currentOutputStream = null
    }

    companion object {
        private const val TAG = "EdgeTTS"
        private const val EDGE_TTS_URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        private const val CHROMIUM_MAJOR_VERSION = "143"
        private const val WINDOWS_EPOCH_SECONDS = 11644473600L
        private const val WATCHDOG_INITIAL_MS = 30_000L
        private const val WATCHDOG_IDLE_MS = 30_000L
        private const val SPEECH_CONFIG =
            "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                "{\"context\":{\"synthesis\":{\"audio\":{\"metadataOptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-96kbitrate-mono-mp3\"}}}}"

        internal fun buildSsmlFrame(requestId: String, text: String, voice: String, speechRate: Float): String {
            val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val percent = ((speechRate.coerceIn(0.6f, 1.6f) - 1f) * 100).roundToInt()
            val rate = if (percent >= 0) "+$percent%" else "$percent%"
            val language = TtsVoiceRegistry.extractLocale(voice)
            return "X-RequestId:$requestId\r\n" +
                "Content-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$language'>" +
                "<voice name='$voice'><prosody rate='$rate'>$escaped</prosody></voice></speak>"
        }

        private fun generateSecMsGec(): String {
            var ticks = (System.currentTimeMillis() / 1000.0) + WINDOWS_EPOCH_SECONDS
            ticks -= ticks % 300
            ticks *= 1e7
            val bytes = "${ticks.toLong()}$TRUSTED_CLIENT_TOKEN".toByteArray(Charsets.US_ASCII)
            return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        }

        private fun generateMuid(): String = UUID.randomUUID().toString().replace("-", "").uppercase()
    }
}
