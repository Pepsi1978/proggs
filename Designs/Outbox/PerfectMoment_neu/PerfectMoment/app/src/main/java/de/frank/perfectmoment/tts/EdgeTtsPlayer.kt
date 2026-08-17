package de.frank.perfectmoment.tts

import de.frank.perfectmoment.network.shutdownSafely
import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger
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
    private val lock = Any()
    private val streamLock = Any()
    private val generation = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var outputStream: FileOutputStream? = null
    private var mediaPlayer: MediaPlayer? = null
    private var watchdogJob: Job? = null
    private var activeFile: File? = null

    fun speak(
        text: String,
        voice: String = TtsCatalog.DEFAULT_EDGE_VOICE,
        speechRate: Float = 1f,
        onPlaybackStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        stop()
        val requestGeneration = generation.incrementAndGet()
        val callbacks = PlaybackCallbacks(onPlaybackStart, onComplete, onError)
        if (text.isBlank()) {
            finishComplete(requestGeneration, callbacks)
            return
        }

        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val url = EDGE_TTS_URL +
            "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&Sec-MS-GEC=${generateSecMsGec()}" +
            "&Sec-MS-GEC-Version=1-$CHROMIUM_FULL_VERSION" +
            "&ConnectionId=$connectionId"
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 " +
                    "Safari/537.36 Edg/$CHROMIUM_MAJOR_VERSION.0.0.0",
            )
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Cookie", "muid=${generateMuid()};")
            .build()

        val audioFile = File(
            appContext.cacheDir,
            "edge_tts_${requestGeneration}_${UUID.randomUUID()}.mp3",
        )
        val stream = try {
            FileOutputStream(audioFile)
        } catch (error: Exception) {
            finishError(requestGeneration, callbacks, error)
            return
        }
        synchronized(lock) {
            if (requestGeneration != generation.get()) {
                stream.close()
                audioFile.delete()
                return
            }
            outputStream = stream
            activeFile = audioFile
        }

        val turnEnded = AtomicBoolean(false)
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (requestGeneration != generation.get()) {
                    webSocket.cancel()
                    return
                }
                startWatchdog(webSocket, requestGeneration, callbacks, WATCHDOG_INITIAL_MS)
                val config = "Content-Type:application/json; charset=utf-8\r\n" +
                    "Path:speech.config\r\n\r\n" +
                    """{"context":{"synthesis":{"audio":{"metadataOptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"""
                val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val ratePercent = ((speechRate.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
                val formattedRate = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
                val ssml = "X-RequestId:$requestId\r\n" +
                    "Content-Type:application/ssml+xml\r\n" +
                    "Path:ssml\r\n\r\n" +
                    "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' " +
                    "xml:lang='de-DE'><voice name='$voice'><prosody rate='$formattedRate'>" +
                    "$escaped</prosody></voice></speak>"
                if (!webSocket.send(config) || !webSocket.send(ssml)) {
                    finishError(
                        requestGeneration,
                        callbacks,
                        TtsPlaybackException("Edge TTS request could not be sent."),
                    )
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (requestGeneration != generation.get() || callbacks.terminal.get()) return
                val data = bytes.toByteArray()
                if (data.size <= 2) return
                val headerLength = ((data[0].toInt() and 0xFF) shl 8) or
                    (data[1].toInt() and 0xFF)
                val audioStart = headerLength + 2
                if (audioStart >= data.size) return
                try {
                    synchronized(streamLock) {
                        if (requestGeneration == generation.get()) {
                            stream.write(data, audioStart, data.size - audioStart)
                        }
                    }
                    startWatchdog(webSocket, requestGeneration, callbacks, WATCHDOG_IDLE_MS)
                } catch (error: Exception) {
                    finishError(requestGeneration, callbacks, error)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (requestGeneration != generation.get() ||
                    !text.contains("Path:turn.end") ||
                    !turnEnded.compareAndSet(false, true)
                ) {
                    return
                }
                cancelWatchdog()
                synchronized(streamLock) {
                    try {
                        stream.close()
                    } catch (_: Exception) {
                        // A concurrent stop may already have closed it.
                    }
                }
                synchronized(lock) {
                    if (outputStream === stream) outputStream = null
                }
                if (audioFile.length() == 0L) {
                    finishError(
                        requestGeneration,
                        callbacks,
                        TtsPlaybackException("Edge TTS returned no audio data."),
                    )
                    return
                }
                scope.launch { playFile(audioFile, requestGeneration, callbacks) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (turnEnded.get() || requestGeneration != generation.get()) return
                logger.warning("Edge TTS WebSocket failed: ${t.message}")
                finishError(
                    requestGeneration,
                    callbacks,
                    TtsPlaybackException("Edge TTS connection failed.", t),
                )
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!turnEnded.get() && requestGeneration == generation.get()) {
                    finishError(
                        requestGeneration,
                        callbacks,
                        TtsPlaybackException("Edge TTS connection closed before audio completed."),
                    )
                }
            }
        }

        val socket = try {
            client.newWebSocket(request, listener)
        } catch (error: Exception) {
            finishError(requestGeneration, callbacks, error)
            return
        }
        synchronized(lock) {
            if (requestGeneration == generation.get() && !callbacks.terminal.get()) {
                webSocket = socket
            } else {
                socket.cancel()
            }
        }
    }

    private fun playFile(
        file: File,
        requestGeneration: Long,
        callbacks: PlaybackCallbacks,
    ) {
        if (requestGeneration != generation.get() || callbacks.terminal.get()) {
            file.delete()
            return
        }
        val player = MediaPlayer()
        try {
            synchronized(lock) {
                if (requestGeneration != generation.get()) {
                    player.release()
                    file.delete()
                    return
                }
                mediaPlayer = player
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener {
                    finishComplete(requestGeneration, callbacks)
                }
                player.setOnErrorListener { _, what, extra ->
                    finishError(
                        requestGeneration,
                        callbacks,
                        TtsPlaybackException("Edge MediaPlayer error $what/$extra"),
                    )
                    true
                }
                player.prepare()
                player.start()
            }
            signalStart(requestGeneration, callbacks)
        } catch (error: Exception) {
            finishError(requestGeneration, callbacks, error)
        }
    }

    fun stop() {
        generation.incrementAndGet()
        cleanupCurrent()
    }

    fun pause(): Boolean = synchronized(lock) {
        val player = mediaPlayer ?: return@synchronized false
        try {
            if (player.isPlaying) player.pause()
            true
        } catch (error: Exception) {
            logger.warning("Edge TTS pause failed: ${error.message}")
            false
        }
    }

    fun resume(): Boolean = synchronized(lock) {
        val player = mediaPlayer ?: return@synchronized false
        try {
            player.start()
            true
        } catch (error: Exception) {
            logger.warning("Edge TTS resume failed: ${error.message}")
            false
        }
    }

    fun shutdown() {
        stop()
        scope.cancel()
        client.shutdownSafely(logger)
    }

    private fun startWatchdog(
        socket: WebSocket,
        requestGeneration: Long,
        callbacks: PlaybackCallbacks,
        timeoutMs: Long,
    ) {
        val job = scope.launch {
            delay(timeoutMs)
            if (requestGeneration == generation.get() && !callbacks.terminal.get()) {
                logger.warning("Edge TTS watchdog fired after $timeoutMs ms")
                // A watchdog timeout is a completed attempt so the session can advance.
                finishComplete(requestGeneration, callbacks)
                socket.cancel()
            }
        }
        val oldJob: Job?
        synchronized(lock) {
            if (requestGeneration != generation.get()) {
                job.cancel()
                return
            }
            oldJob = watchdogJob
            watchdogJob = job
        }
        oldJob?.cancel()
    }

    private fun cancelWatchdog() {
        val job = synchronized(lock) {
            watchdogJob.also { watchdogJob = null }
        }
        job?.cancel()
    }

    private fun signalStart(requestGeneration: Long, callbacks: PlaybackCallbacks) {
        if (requestGeneration != generation.get() || callbacks.terminal.get()) return
        if (callbacks.started.compareAndSet(false, true)) callbacks.onStart()
    }

    private fun finishComplete(requestGeneration: Long, callbacks: PlaybackCallbacks) {
        finish(requestGeneration, callbacks) { callbacks.onComplete() }
    }

    private fun finishError(
        requestGeneration: Long,
        callbacks: PlaybackCallbacks,
        error: Exception,
    ) {
        finish(requestGeneration, callbacks) { callbacks.onError(error) }
    }

    private fun finish(
        requestGeneration: Long,
        callbacks: PlaybackCallbacks,
        terminalCallback: () -> Unit,
    ) {
        if (requestGeneration != generation.get() ||
            !callbacks.terminal.compareAndSet(false, true)
        ) {
            return
        }
        cleanupCurrent()
        scope.launch {
            if (requestGeneration == generation.get()) terminalCallback()
        }
    }

    private fun cleanupCurrent() {
        val socket: WebSocket?
        val stream: FileOutputStream?
        val player: MediaPlayer?
        val watchdog: Job?
        val file: File?
        synchronized(lock) {
            socket = webSocket
            stream = outputStream
            player = mediaPlayer
            watchdog = watchdogJob
            file = activeFile
            webSocket = null
            outputStream = null
            mediaPlayer = null
            watchdogJob = null
            activeFile = null
        }
        watchdog?.cancel()
        socket?.cancel()
        synchronized(streamLock) {
            try {
                stream?.close()
            } catch (_: Exception) {
                // Cleanup is best-effort.
            }
        }
        try {
            player?.stop()
        } catch (_: Exception) {
            // MediaPlayer can already be completed or in an error state.
        }
        player?.release()
        file?.delete()
    }

    private class PlaybackCallbacks(
        val onStart: () -> Unit,
        val onComplete: () -> Unit,
        val onError: (Exception) -> Unit,
    ) {
        val started = AtomicBoolean(false)
        val terminal = AtomicBoolean(false)
    }

    private companion object {
        const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        const val CHROMIUM_MAJOR_VERSION = "143"
        const val WINDOWS_EPOCH_SECONDS = 11_644_473_600L
        const val WATCHDOG_INITIAL_MS = 30_000L
        const val WATCHDOG_IDLE_MS = 30_000L
        const val EDGE_TTS_URL =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"

        val logger = Logger.getLogger(EdgeTtsPlayer::class.java.name)

        fun generateSecMsGec(): String {
            val unixSeconds = System.currentTimeMillis() / 1_000L
            val roundedSeconds = unixSeconds - (unixSeconds % 300L)
            val ticks = (roundedSeconds + WINDOWS_EPOCH_SECONDS) * 10_000_000L
            val source = "$ticks$TRUSTED_CLIENT_TOKEN"
            return MessageDigest.getInstance("SHA-256")
                .digest(source.toByteArray(Charsets.US_ASCII))
                .joinToString("") { byte -> String.format(Locale.ROOT, "%02X", byte) }
        }

        fun generateMuid(): String = UUID.randomUUID().toString().replace("-", "").uppercase()
    }
}
