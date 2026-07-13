package de.frank.cortex.audio

import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Microsoft Edge TTS (kostenloser Read-Aloud-Dienst von Edge). Synthetisiert einen Text-Chunk
 * ueber die WebSocket-Schnittstelle von speech.platform.bing.com und gibt die fertigen MP3-Bytes
 * zurueck (24 kHz, 96 kbit mono). 1:1 dasselbe Protokoll wie in BestJournalFrank, hier aber als
 * suspend-Synthese-zu-Bytes (KEINE Wiedergabe) - die uebernimmt der Mp3Player im Chunk-Pipeline.
 *
 * Kein API-Key noetig; die Absicherung laeuft ueber den Sec-MS-GEC-Token (SHA-256 ueber
 * 5-Minuten-Zeitfenster + festen Client-Token), exakt wie es der Edge-Browser macht.
 */
class EdgeTtsSynthesizer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // readTimeout greift bei OkHttp-WebSockets nach dem Handshake NICHT mehr — erst der
        // Ping-Rahmen erkennt eine halbtote Verbindung (Netzwechsel WLAN->Mobilfunk) und feuert
        // onFailure, statt die Synthese unbegrenzt haengen zu lassen.
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

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
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(strToHash.toByteArray(Charsets.US_ASCII)).joinToString("") {
                "%02X".format(it)
            }
        }

        fun generateMuid(): String = UUID.randomUUID().toString().replace("-", "").uppercase()
    }

    /**
     * Synthetisiert [text] mit [voice] (voller Name, z.B. de-DE-SeraphinaMultilingualNeural) und
     * gibt die MP3-Bytes zurueck. Wirft bei Fehler/Cancel. Abbrechbar (WebSocket wird gecancelt).
     * Hartes 45s-Dach: antwortet der Dienst nach onOpen nie (weder turn.end noch onFailure),
     * hing die Vorlese-Pipeline sonst unbegrenzt mit isSpeaking=true fest.
     */
    suspend fun synthesize(text: String, voice: String): ByteArray = try {
        kotlinx.coroutines.withTimeout(45_000) { synthesizeInternal(text, voice) }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        // Als IOException statt CancellationException melden: Aufrufer sollen das als FEHLER
        // behandeln (Fallback/Meldung), nicht als stilles Abbrechen der eigenen Coroutine.
        throw java.io.IOException("Edge-TTS-Timeout nach 45 s (keine Antwort vom Dienst)")
    }

    private suspend fun synthesizeInternal(text: String, voice: String): ByteArray = suspendCancellableCoroutine { cont ->
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val secMsGec = generateSecMsGec()
        val url =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                "&Sec-MS-GEC=$secMsGec" +
                "&Sec-MS-GEC-Version=1-$CHROMIUM_FULL_VERSION" +
                "&ConnectionId=$connectionId"

        val request = Request.Builder()
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

        val audioBuffer = ByteArrayOutputStream()
        // Genau EINMAL fortsetzen (WS-Callbacks + Cancellation koennen aus mehreren Richtungen kommen).
        val done = java.util.concurrent.atomic.AtomicBoolean(false)

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val config =
                    "Content-Type:application/json; charset=utf-8\r\n" +
                        "Path:speech.config\r\n\r\n" +
                        """{"context":{"synthesis":{"audio":{"metadataOptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"""
                webSocket.send(config)

                val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val ssml =
                    "X-RequestId:$requestId\r\n" +
                        "Content-Type:application/ssml+xml\r\n" +
                        "Path:ssml\r\n\r\n" +
                        "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
                        "<voice name='$voice'>$escaped</voice>" +
                        "</speak>"
                webSocket.send(ssml)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                if (data.size > 2) {
                    val headerLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                    val audioStart = headerLen + 2
                    if (data.size > audioStart) {
                        audioBuffer.write(data, audioStart, data.size - audioStart)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    webSocket.close(1000, null)
                    if (done.compareAndSet(false, true)) cont.resume(audioBuffer.toByteArray())
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                CortexLog.warn("EdgeTts", "synthesize", "WebSocket-Fehler: ${t.message}",
                    mapOf("text_len" to text.length, "voice" to voice))
                if (done.compareAndSet(false, true)) cont.resumeWithException(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Falls turn.end nie kam, aber die Verbindung sauber schloss: mit dem Bisherigen antworten.
                if (done.compareAndSet(false, true)) cont.resume(audioBuffer.toByteArray())
            }
        })

        cont.invokeOnCancellation { ws.cancel() }
    }
}
