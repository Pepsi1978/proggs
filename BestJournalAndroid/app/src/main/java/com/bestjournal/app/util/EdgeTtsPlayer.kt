package com.bestjournal.app.util

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private var onDone: (() -> Unit)? = null
    private val client = OkHttpClient()

    private companion object {
        const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        const val CHROMIUM_MAJOR_VERSION = "143"
        const val WIN_EPOCH = 11644473600L

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

    fun speak(text: String, onComplete: () -> Unit) {
        stop()
        onDone = onComplete

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
        val outputStream = FileOutputStream(audioFile)

        webSocket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        android.util.Log.d("EdgeTTS", "WebSocket connected")
                        val config =
                            "Content-Type:application/json; charset=utf-8\r\n" +
                                "Path:speech.config\r\n\r\n" +
                                """{"context":{"synthesis":{"audio":{"metadataOptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"""
                        webSocket.send(config)

                        val escaped =
                            text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

                        val ssml =
                            "X-RequestId:$requestId\r\n" +
                                "Content-Type:application/ssml+xml\r\n" +
                                "Path:ssml\r\n\r\n" +
                                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
                                "<voice name='de-DE-KatjaNeural'>$escaped</voice>" +
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
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (text.contains("Path:turn.end")) {
                            outputStream.close()
                            CoroutineScope(Dispatchers.Main).launch { playFile(audioFile) }
                        }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        android.util.Log.e("EdgeTTS", "WebSocket failure: ${t.message}", t)
                        try {
                            outputStream.close()
                        } catch (_: Exception) {}
                        CoroutineScope(Dispatchers.Main).launch { onDone?.invoke() }
                    }
                },
            )
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
            }
    }

    fun stop() {
        webSocket?.cancel()
        webSocket = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
    }
}
