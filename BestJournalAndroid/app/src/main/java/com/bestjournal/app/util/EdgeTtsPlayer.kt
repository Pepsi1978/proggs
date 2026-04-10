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

    fun speak(text: String, onComplete: () -> Unit) {
        stop()
        onDone = onComplete

        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")

        val url =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                "?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4" +
                "&ConnectionId=$connectionId"

        val request =
            Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                )
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdmdber")
                .build()

        val audioFile = File(context.cacheDir, "tts_audio.mp3")
        val outputStream = FileOutputStream(audioFile)

        webSocket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
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
                                "<voice name='de-DE-ConradNeural'>$escaped</voice>" +
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
