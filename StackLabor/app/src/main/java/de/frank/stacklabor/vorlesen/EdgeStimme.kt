package de.frank.stacklabor.vorlesen

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * Microsoft Edge als Sprecher (Vorgabe-Anbieter laut F-18).
 *
 * Die Mechanik ist aus `EdgeTtsPlayer.kt` (PerfectMoment) uebernommen: Der Dienst spricht ein
 * WebSocket-Protokoll, das zuerst eine Sprach-Konfiguration und danach SSML entgegennimmt und
 * die Audiodaten in binaeren Teilstuecken zurueckschickt. Jedes Teilstueck traegt einen Kopf,
 * dessen Laenge in den ersten zwei Bytes steht. Das Ende meldet eine Textnachricht mit
 * `Path:turn.end`.
 *
 * Anders als die Vorlage spielt diese Klasse nichts ab — sie liefert nur die fertigen MP3-Daten.
 * Ein Schluessel wird nicht gebraucht; Edge ist kostenfrei erreichbar.
 */
class EdgeStimme(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) : Sprecher {

    override val anbieter: Anbieter = Anbieter.EDGE

    override suspend fun sprich(text: String, stimmeId: String, tempo: Float): ByteArray =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext ByteArray(0)
            val stimme = stimmeId.ifBlank { StimmenKatalog.VORGABE_EDGE_STIMME }
            try {
                withTimeout(ZEITGRENZE_MS) { hole(text, stimme, tempo) }
            } catch (abgelaufen: TimeoutCancellationException) {
                // DECISION: Die Zeitgrenze ist ein Klartext-Fehler und kein stiller Abbruch, sonst
                // haengt die Absatzfolge in VorleseSteuerung ohne erkennbaren Grund.
                throw VorleseFehler("Microsoft Edge hat nicht rechtzeitig geantwortet.", abgelaufen)
            }
        }

    override fun schliesse() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private suspend fun hole(text: String, stimme: String, tempo: Float): ByteArray =
        suspendCancellableCoroutine { fortsetzung ->
            val audio = ByteArrayOutputStream()
            val beendet = AtomicBoolean(false)
            val anfrageKennung = UUID.randomUUID().toString().replace("-", "")

            fun gelungen(daten: ByteArray) {
                if (beendet.compareAndSet(false, true) && fortsetzung.isActive) {
                    fortsetzung.resume(daten)
                }
            }

            fun gescheitert(fehler: Throwable) {
                if (beendet.compareAndSet(false, true) && fortsetzung.isActive) {
                    fortsetzung.resumeWith(Result.failure(fehler))
                }
            }

            val zuhoerer = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val konfiguration = "Content-Type:application/json; charset=utf-8\r\n" +
                        "Path:speech.config\r\n\r\n" +
                        """{"context":{"synthesis":{"audio":{"metadataOptions":""" +
                        """{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},""" +
                        """"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"""
                    val ssml = "X-RequestId:$anfrageKennung\r\n" +
                        "Content-Type:application/ssml+xml\r\n" +
                        "Path:ssml\r\n\r\n" +
                        "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' " +
                        "xml:lang='de-DE'><voice name='$stimme'>" +
                        "<prosody rate='${tempoAlsProzent(tempo)}' pitch='+0%'>" +
                        "${maskiere(text)}</prosody></voice></speak>"
                    if (!webSocket.send(konfiguration) || !webSocket.send(ssml)) {
                        gescheitert(VorleseFehler("Die Anfrage an Microsoft Edge konnte nicht gesendet werden."))
                        webSocket.cancel()
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val daten = bytes.toByteArray()
                    if (daten.size <= 2) return
                    // Die ersten zwei Bytes tragen die Kopflaenge; danach beginnt das Audio.
                    val kopfLaenge = ((daten[0].toInt() and 0xFF) shl 8) or (daten[1].toInt() and 0xFF)
                    val start = kopfLaenge + 2
                    if (start >= daten.size) return
                    synchronized(audio) { audio.write(daten, start, daten.size - start) }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!text.contains("Path:turn.end")) return
                    val daten = synchronized(audio) { audio.toByteArray() }
                    if (daten.isEmpty()) {
                        gescheitert(VorleseFehler("Microsoft Edge hat keine Audiodaten geliefert."))
                    } else {
                        gelungen(daten)
                    }
                    webSocket.close(1000, null)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    gescheitert(VorleseFehler("Microsoft Edge ist nicht erreichbar.", t))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    gescheitert(
                        VorleseFehler("Die Verbindung zu Microsoft Edge endete vor dem Ende der Aufnahme."),
                    )
                }
            }

            val socket = client.newWebSocket(baueAnfrage(), zuhoerer)
            fortsetzung.invokeOnCancellation { socket.cancel() }
        }

    private fun baueAnfrage(): Request {
        val verbindungsKennung = UUID.randomUUID().toString().replace("-", "")
        val adresse = EDGE_ADRESSE +
            "?TrustedClientToken=$VERTRAUENS_TOKEN" +
            "&Sec-MS-GEC=${erzeugeSicherheitsMarke()}" +
            "&Sec-MS-GEC-Version=1-$CHROMIUM_VOLLVERSION" +
            "&ConnectionId=$verbindungsKennung"
        return Request.Builder()
            .url(adresse)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/$CHROMIUM_HAUPTVERSION.0.0.0 " +
                    "Safari/537.36 Edg/$CHROMIUM_HAUPTVERSION.0.0.0",
            )
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Cookie", "muid=${UUID.randomUUID().toString().replace("-", "").uppercase()};")
            .build()
    }

    private companion object {
        const val EDGE_ADRESSE =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
        const val VERTRAUENS_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        const val CHROMIUM_VOLLVERSION = "143.0.3650.75"
        const val CHROMIUM_HAUPTVERSION = "143"
        const val WINDOWS_EPOCHE_SEKUNDEN = 11_644_473_600L
        const val ZEITGRENZE_MS = 45_000L

        fun maskiere(text: String): String =
            text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        /** Edge erwartet das Tempo als Prozent-Abweichung mit Vorzeichen, etwa `+10%`. */
        fun tempoAlsProzent(tempo: Float): String {
            val prozent = ((tempo.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
            return if (prozent >= 0) "+$prozent%" else "$prozent%"
        }

        /**
         * Sicherheitsmarke des Dienstes: SHA-256 ueber die auf fuenf Minuten gerundete Windows-Zeit
         * und das Vertrauens-Token. Ohne diese Marke lehnt Edge die Verbindung ab.
         */
        fun erzeugeSicherheitsMarke(): String {
            val sekunden = System.currentTimeMillis() / 1_000L
            val gerundet = sekunden - (sekunden % 300L)
            val ticks = (gerundet + WINDOWS_EPOCHE_SEKUNDEN) * 10_000_000L
            return MessageDigest.getInstance("SHA-256")
                .digest("$ticks$VERTRAUENS_TOKEN".toByteArray(Charsets.US_ASCII))
                .joinToString("") { byte -> String.format(Locale.ROOT, "%02X", byte) }
        }
    }
}
