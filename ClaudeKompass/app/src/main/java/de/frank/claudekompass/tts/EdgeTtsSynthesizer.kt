package de.frank.claudekompass.tts

import de.frank.claudekompass.network.beendeSanft
import de.frank.claudekompass.observability.KompassLog
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * Microsoft Edge — der kostenlose Rückfall (Referenz, Baustein D).
 *
 * Der Dienst spricht kein gewöhnliches HTTP, sondern eine WebSocket-Verbindung mit einem
 * eigenen Rahmenformat: Jede Binärnachricht beginnt mit zwei Byte Kopflänge, danach folgt der
 * Kopf und erst dahinter das eigentliche Tonmaterial. Das Ende meldet eine Textnachricht mit
 * `Path:turn.end`.
 *
 * Der Wachhund ist nicht optional: Bricht die Verbindung ab, ohne dass `turn.end` kommt,
 * würde die Vorlese-Reihe sonst für immer auf diesen einen Absatz warten.
 */
class EdgeTtsSynthesizer : TtsSynthesizer {

    override val anbieterId = "edge"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun synthetisiere(
        text: String,
        stimme: String,
        tempo: Float,
    ): SyntheseErgebnis = suspendCancellableCoroutine { fortsetzung ->
        val puffer = ByteArrayOutputStream()
        val anfrageId = UUID.randomUUID().toString().replace("-", "")
        val verbindungsId = UUID.randomUUID().toString().replace("-", "")
        var beendet = false

        fun fertig(ergebnis: Result<SyntheseErgebnis>) {
            if (beendet) return
            beendet = true
            if (!fortsetzung.isActive) return
            ergebnis.fold(
                onSuccess = { fortsetzung.resume(it) },
                onFailure = { fortsetzung.resumeWith(Result.failure(it)) },
            )
        }

        val adresse = EDGE_URL +
            "?TrustedClientToken=$VERTRAUENS_TOKEN" +
            "&Sec-MS-GEC=${erzeugeSecMsGec()}" +
            "&Sec-MS-GEC-Version=1-$CHROMIUM_VOLLVERSION" +
            "&ConnectionId=$verbindungsId"
        val anfrage = Request.Builder()
            .url(adresse)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$CHROMIUM_HAUPTVERSION.0.0.0 Safari/537.36 Edg/$CHROMIUM_HAUPTVERSION.0.0.0",
            )
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Cookie", "muid=${UUID.randomUUID().toString().replace("-", "").uppercase()};")
            .build()

        val zuhoerer = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val einstellung = "Content-Type:application/json; charset=utf-8\r\n" +
                    "Path:speech.config\r\n\r\n" +
                    """{"context":{"synthesis":{"audio":{"metadataOptions":""" +
                    """{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},""" +
                    """"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"""
                val entschaerft = text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                val prozent = ((tempo.coerceIn(0.5f, 2.0f) - 1f) * 100).roundToInt()
                val tempoText = if (prozent >= 0) "+$prozent%" else "$prozent%"
                val ssml = "X-RequestId:$anfrageId\r\n" +
                    "Content-Type:application/ssml+xml\r\n" +
                    "Path:ssml\r\n\r\n" +
                    "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
                    "<voice name='$stimme'><prosody rate='$tempoText'>$entschaerft</prosody></voice></speak>"
                if (!webSocket.send(einstellung) || !webSocket.send(ssml)) {
                    fertig(Result.failure(TtsFehler(TtsFehlerArt.NETZ, "Die Edge-Anfrage ging nicht raus.", wiederholbar = true)))
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val daten = bytes.toByteArray()
                if (daten.size <= 2) return
                val kopflaenge = ((daten[0].toInt() and 0xFF) shl 8) or (daten[1].toInt() and 0xFF)
                val tonBeginn = kopflaenge + 2
                if (tonBeginn >= daten.size) return
                synchronized(puffer) { puffer.write(daten, tonBeginn, daten.size - tonBeginn) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!text.contains("Path:turn.end")) return
                val ton = synchronized(puffer) { puffer.toByteArray() }
                webSocket.close(1000, null)
                if (ton.isEmpty()) {
                    fertig(Result.failure(TtsFehler(TtsFehlerArt.INHALT, "Edge hat für diesen Absatz keinen Ton geliefert.")))
                } else {
                    fertig(Result.success(SyntheseErgebnis(ton, "mp3")))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                KompassLog.warn("EdgeTts", "onFailure", "Verbindung gescheitert", mapOf("grund" to t.message))
                fertig(
                    Result.failure(
                        TtsFehler(TtsFehlerArt.NETZ, "Die Edge-Verbindung ist abgebrochen.", wiederholbar = true, ursache = t),
                    ),
                )
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Kommt der Abschluss ohne turn.end, wartet sonst niemand mehr auf eine Antwort.
                fertig(
                    Result.failure(
                        TtsFehler(
                            TtsFehlerArt.NETZ,
                            "Edge hat die Verbindung beendet, bevor der Ton vollständig war.",
                            wiederholbar = true,
                        ),
                    ),
                )
            }
        }

        val verbindung = client.newWebSocket(anfrage, zuhoerer)
        fortsetzung.invokeOnCancellation { verbindung.cancel() }
    }

    override fun beende() = client.beendeSanft("EdgeTts")

    private companion object {
        const val VERTRAUENS_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        const val CHROMIUM_VOLLVERSION = "143.0.3650.75"
        const val CHROMIUM_HAUPTVERSION = "143"
        const val WINDOWS_EPOCHE_SEKUNDEN = 11_644_473_600L
        const val EDGE_URL =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"

        /** Der Dienst erwartet einen Prüfwert, der sich alle fünf Minuten ändert. */
        fun erzeugeSecMsGec(): String {
            val sekunden = System.currentTimeMillis() / 1_000L
            val gerundet = sekunden - (sekunden % 300L)
            val ticks = (gerundet + WINDOWS_EPOCHE_SEKUNDEN) * 10_000_000L
            return MessageDigest.getInstance("SHA-256")
                .digest("$ticks$VERTRAUENS_TOKEN".toByteArray(Charsets.US_ASCII))
                .joinToString("") { String.format(Locale.ROOT, "%02X", it) }
        }
    }
}
