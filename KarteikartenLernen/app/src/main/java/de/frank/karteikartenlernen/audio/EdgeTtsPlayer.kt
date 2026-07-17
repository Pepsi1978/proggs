package de.frank.karteikartenlernen.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
    private var watchdogOwner: String? = null
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
        val chunks = splitForTts(text)
        speakChunk(chunks, 0, resolvedVoice, speechRate, requestGeneration, onPlaybackStart, onComplete)
    }

    private fun speakChunk(
        chunks: List<String>,
        index: Int,
        voice: String,
        speechRate: Float,
        requestGeneration: Long,
        onPlaybackStart: (() -> Unit)?,
        onComplete: () -> Unit,
    ) {
        if (requestGeneration != generation || index !in chunks.indices) return
        val text = chunks[index]
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val terminal = AtomicBoolean(false)
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
                        startWatchdog(webSocket, WATCHDOG_INITIAL_MS, requestGeneration, requestId, terminal, onComplete)
                        webSocket.send(SPEECH_CONFIG)
                        webSocket.send(buildSsmlFrame(requestId, text, voice, speechRate))
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
                            startWatchdog(webSocket, WATCHDOG_IDLE_MS, requestGeneration, requestId, terminal, onComplete)
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (requestGeneration != generation || !text.contains("Path:turn.end") || !terminal.compareAndSet(false, true)) return
                        cancelWatchdog(requestId)
                        closeOutputStream(outputStream)
                        webSocket.close(1000, "turn.end")
                        if (this@EdgeTtsPlayer.webSocket === webSocket) this@EdgeTtsPlayer.webSocket = null
                        scope.launch {
                            if (requestGeneration == generation) {
                                playFile(
                                    audioFile,
                                    requestGeneration,
                                    if (index == 0) onPlaybackStart else null,
                                ) {
                                    if (index + 1 < chunks.size) {
                                        speakChunk(chunks, index + 1, voice, speechRate, requestGeneration, null, onComplete)
                                    } else {
                                        onComplete()
                                    }
                                }
                            }
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, error: Throwable, response: Response?) {
                        if (requestGeneration != generation || !terminal.compareAndSet(false, true)) return
                        Log.e(TAG, "WebSocket failure: ${error.message}", error)
                        cancelWatchdog(requestId)
                        closeOutputStream(outputStream)
                        audioFile.delete()
                        if (this@EdgeTtsPlayer.webSocket === webSocket) this@EdgeTtsPlayer.webSocket = null
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

    private fun startWatchdog(
        webSocket: WebSocket,
        timeoutMs: Long,
        requestGeneration: Long,
        owner: String,
        terminal: AtomicBoolean,
        onComplete: () -> Unit,
    ) {
        cancelWatchdog(owner)
        watchdogOwner = owner
        watchdogJob = scope.launch {
            delay(timeoutMs)
            if (requestGeneration != generation || watchdogOwner != owner || !terminal.compareAndSet(false, true)) return@launch
            Log.w(TAG, "No Edge TTS audio received for ${timeoutMs}ms; aborting")
            generation++
            webSocket.cancel()
            closeOutputStream(currentOutputStream)
            watchdogOwner = null
            watchdogJob = null
            if (this@EdgeTtsPlayer.webSocket === webSocket) this@EdgeTtsPlayer.webSocket = null
            onComplete()
        }
    }

    private fun cancelWatchdog(owner: String? = null) {
        if (owner != null && watchdogOwner != owner) return
        watchdogJob?.cancel()
        watchdogJob = null
        watchdogOwner = null
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
            setOnCompletionListener { completedPlayer ->
                completedPlayer.release()
                if (mediaPlayer === completedPlayer) mediaPlayer = null
                if (requestGeneration == generation) onComplete()
                file.delete()
            }
            setOnErrorListener { failedPlayer, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                failedPlayer.release()
                if (mediaPlayer === failedPlayer) mediaPlayer = null
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

        internal fun splitForTts(text: String, maxUtf8Bytes: Int = 3_800): List<String> {
            require(maxUtf8Bytes > 0)
            val chunks = mutableListOf<String>()
            var current = StringBuilder()

            fun flush() {
                if (current.isNotEmpty()) chunks += current.toString()
                current = StringBuilder()
            }

            text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (candidate.toByteArray(Charsets.UTF_8).size <= maxUtf8Bytes) {
                    current = StringBuilder(candidate)
                } else {
                    flush()
                    if (word.toByteArray(Charsets.UTF_8).size <= maxUtf8Bytes) {
                        current.append(word)
                    } else {
                        val codePoints = word.codePoints().iterator()
                        while (codePoints.hasNext()) {
                            val character = String(Character.toChars(codePoints.nextInt()))
                            if ((current.toString() + character).toByteArray(Charsets.UTF_8).size > maxUtf8Bytes) flush()
                            current.append(character)
                        }
                    }
                }
            }
            flush()
            return chunks.ifEmpty { listOf("") }
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
