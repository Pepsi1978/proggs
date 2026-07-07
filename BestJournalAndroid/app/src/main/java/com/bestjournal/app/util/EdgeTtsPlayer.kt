package com.bestjournal.app.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
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

class EdgeTtsPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var webSocket: WebSocket? = null
    // @Volatile because currentOutputStream is read from the OkHttp WebSocket thread
    // (onMessage callback) and written/closed from the Main thread (stop/speak).
    @Volatile private var currentOutputStream: java.io.FileOutputStream? = null
    private var onDone: (() -> Unit)? = null
    private var onPlayStart: (() -> Unit)? = null
    private var watchdogJob: Job? = null
    private var currentVoice: String = ""

    // Single owned scope — cancelled in shutdown(). stop() only cancels the current watchdog job,
    // not the scope itself, since stop() is called frequently (e.g. on each new speak() call).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .build()

    private companion object {
        const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        const val CHROMIUM_MAJOR_VERSION = "143"
        const val WIN_EPOCH = 11644473600L

        // Watchdog: abort if no audio bytes received within this window.
        // Timer resets on every received audio chunk, so long streaming audio
        // (40s+ generated speech) is NOT affected — only genuine silence triggers.
        // 30s initial gives Microsoft plenty of time to start streaming even on
        // slow mobile networks or when the server is under load.
        const val WATCHDOG_INITIAL_MS = 30_000L // from onOpen until first audio byte
        const val WATCHDOG_IDLE_MS = 30_000L // gap between any two audio chunks

        fun generateSecMsGec(): String {
            var ticks = (System.currentTimeMillis() / 1000.0) + WIN_EPOCH
            ticks -= ticks % 300
            ticks *= 1e7
            val strToHash = "${ticks.toLong()}$TRUSTED_CLIENT_TOKEN"
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            return digest.digest(strToHash.toByteArray(Charsets.US_ASCII)).joinToString("") {
                "%02X".format(it)
            }
        }

        fun generateMuid(): String = UUID.randomUUID().toString().replace("-", "").uppercase()
    }

    fun speak(
        text: String,
        voice: String = TtsVoiceRegistry.getLocaleVoices().defaultVoiceId,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
    ) {
        stop()

        // Guard: empty text would hang the WebSocket (no audio returned)
        if (text.isBlank()) {
            onComplete()
            return
        }

        onDone = onComplete
        onPlayStart = onPlaybackStart
        currentVoice = voice

        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")

        val secMsGec = generateSecMsGec()
        val url =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                "&Sec-MS-GEC=$secMsGec" +
                "&Sec-MS-GEC-Version=1-$CHROMIUM_FULL_VERSION" +
                "&ConnectionId=$connectionId"

        val request =
            Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" +
                        " (KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36" +
                        " Edg/$CHROMIUM_MAJOR_VERSION.0.0.0",
                )
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("Cookie", "muid=${generateMuid()};")
                .build()

        val audioFile = File(context.cacheDir, "tts_audio.mp3")
        try {
            currentOutputStream?.close()
        } catch (_: Exception) {}
        // Assign directly to the field BEFORE opening the WebSocket so stop()'s
        // cleanup path (which closes currentOutputStream) also covers the case
        // where client.newWebSocket() itself throws — previously the stream
        // existed only as a local var and would leak on constructor failure.
        val outputStream = FileOutputStream(audioFile)
        currentOutputStream = outputStream

        try {
            webSocket =
                client.newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            android.util.Log.d("EdgeTTS", "WebSocket connected")
                            startWatchdog(webSocket, WATCHDOG_INITIAL_MS, initial = true)
                            val config =
                                "Content-Type:application/json; charset=utf-8\r\n" +
                                    "Path:speech.config\r\n\r\n" +
                                    """{"context":{"synthesis":{"audio":{"metadataOptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"""
                            webSocket.send(config)

                            val escaped =
                                text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

                            val lang = TtsVoiceRegistry.extractLocale(voice)
                            // Earlier attempt wrapped the body in <lang xml:lang='...'>
                            // to stop multilingual voices from mispronouncing compound
                            // German words. Edge TTS's consumer endpoint silently
                            // dropped those requests (no audio, no turn.end), so the
                            // UI hung on the spinner forever. The wrapper was reverted;
                            // the multilingual pronunciation issue is now handled on
                            // the caller side (see speakText builder in EntryDetailScreen).
                            val ssml =
                                "X-RequestId:$requestId\r\n" +
                                    "Content-Type:application/ssml+xml\r\n" +
                                    "Path:ssml\r\n\r\n" +
                                    "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$lang'>" +
                                    "<voice name='$voice'>$escaped</voice>" +
                                    "</speak>"
                            webSocket.send(ssml)
                        }

                        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                            val data = bytes.toByteArray()
                            if (data.size > 2) {
                                val headerLen =
                                    ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                                val audioStart = headerLen + 2
                                if (data.size > audioStart) {
                                    outputStream.write(data, audioStart, data.size - audioStart)
                                    // Reset watchdog: bytes flowed, server is alive
                                    startWatchdog(webSocket, WATCHDOG_IDLE_MS, initial = false)
                                }
                            }
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            if (text.contains("Path:turn.end")) {
                                watchdogJob?.cancel()
                                watchdogJob = null
                                outputStream.close()
                                scope.launch { playFile(audioFile) }
                            }
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            android.util.Log.e("EdgeTTS", "WebSocket failure: ${t.message}", t)
                            watchdogJob?.cancel()
                            watchdogJob = null
                            logTtsFailure(currentVoice, t.message ?: "unknown")
                            try {
                                outputStream.close()
                            } catch (_: Exception) {}
                            scope.launch { onDone?.invoke() }
                        }
                    },
                )
        } catch (e: Exception) {
            android.util.Log.e("EdgeTTS", "newWebSocket failed — releasing file stream", e)
            try {
                currentOutputStream?.close()
            } catch (_: Exception) {}
            currentOutputStream = null
            logTtsFailure(currentVoice, "websocket-ctor: ${e.message ?: e.javaClass.simpleName}")
            scope.launch { onDone?.invoke() }
        }
    }

    /**
     * Starts or restarts the watchdog timer. If the timer expires before it is reset by incoming
     * audio bytes or cancelled by turn.end/failure, the WebSocket is aborted and the UI is
     * unblocked via onDone(). A Firebase Analytics event is sent so the developer can see — from
     * aggregated user data — which voices/locales are failing.
     *
     * Long audio generation (40s+ streaming speech) is NOT affected because the timer resets on
     * every received chunk. Only genuine silence triggers abort.
     */
    private fun startWatchdog(ws: WebSocket, timeoutMs: Long, initial: Boolean) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            delay(timeoutMs)
            val reason = if (initial) "initial_silence" else "idle_during_stream"
            val detail =
                if (initial) "no audio received within ${timeoutMs}ms after onOpen"
                else "no audio bytes for ${timeoutMs}ms during streaming"
            android.util.Log.w("EdgeTTS", "Watchdog fired: $detail — aborting")
            logTtsWatchdogFired(currentVoice, reason)
            ws.cancel()
            try {
                currentOutputStream?.close()
            } catch (_: Exception) {}
            onDone?.invoke()
        }
    }

    /**
     * Log a watchdog abort to Firebase Analytics so failures in locales the developer does not
     * personally use (e.g. Italian, Chinese) become visible in the dashboard. Event parameters:
     * voice, locale, reason (initial_silence | idle_during_stream).
     */
    private fun logTtsWatchdogFired(voice: String, reason: String) {
        try {
            val locale = TtsVoiceRegistry.extractLocale(voice)
            analytics.logEvent(
                "tts_watchdog_fired",
                Bundle().apply {
                    putString("voice", voice.take(100))
                    putString("locale", locale.take(20))
                    putString("reason", reason.take(20))
                },
            )
        } catch (e: Exception) {
            android.util.Log.w("EdgeTTS", "Failed to log watchdog event: ${e.message}")
        }
    }

    /**
     * Log a WebSocket-level failure (network error, SSL issue, protocol error) to Firebase
     * Analytics. Complements the watchdog event to cover all failure modes.
     */
    private fun logTtsFailure(voice: String, error: String) {
        try {
            val locale = TtsVoiceRegistry.extractLocale(voice)
            analytics.logEvent(
                "tts_failure",
                Bundle().apply {
                    putString("voice", voice.take(100))
                    putString("locale", locale.take(20))
                    putString("error", error.take(100))
                },
            )
        } catch (e: Exception) {
            android.util.Log.w("EdgeTTS", "Failed to log failure event: ${e.message}")
        }
    }

    private fun playFile(file: File) {
        mediaPlayer?.release()
        mediaPlayer =
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    onDone?.invoke()
                    file.delete()
                }
                setOnErrorListener { _, _, _ ->
                    onDone?.invoke()
                    file.delete()
                    true
                }
                prepare()
                start()
                onPlayStart?.invoke()
            }
    }

    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        webSocket?.cancel()
        webSocket = null
        try {
            currentOutputStream?.close()
        } catch (_: Exception) {}
        currentOutputStream = null
        try {
            mediaPlayer?.stop()
        } catch (_: IllegalStateException) {}
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }
}
