package de.frank.wecker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/** Eine Premium-Stimme aus dem festen Katalog. [region] ist das Land, [art] die kurze Klangbeschreibung. */
data class PremiumStimme(val id: String, val name: String, val weiblich: Boolean, val art: String) {
    val sprache: String get() = id.substringBefore('-')
    val region: String get() = id.split('-').getOrElse(1) { "" }
    val anzeige: String get() = "$name · ${if (weiblich) "weiblich" else "männlich"} · $art"
    val regionName: String get() = Locale(sprache, region).getDisplayCountry(Locale.GERMAN)
}

/**
 * Kuratierte Premium-Stimmen je Sprache (Microsoft-Neural-Stimmen, abgerufen am 27.09.2026 über voices/list).
 * Je Sprache die natürlichsten Stimmen, getrennt nach Region. Erste Stimme einer Region = Vorgabe dieser Region.
 */
object PremiumKatalog {
    const val PROVIDER = "edge_tts"
    val STIMMEN = listOf(
        PremiumStimme("de-DE-SeraphinaMultilingualNeural", "Seraphina", true, "warm, sehr natürlich"),
        PremiumStimme("de-DE-FlorianMultilingualNeural", "Florian", false, "ruhig, sehr natürlich"),
        PremiumStimme("de-DE-KatjaNeural", "Katja", true, "klar, freundlich"),
        PremiumStimme("de-DE-ConradNeural", "Conrad", false, "tief, sonor"),
        PremiumStimme("de-DE-AmalaNeural", "Amala", true, "hell, lebhaft"),
        PremiumStimme("de-DE-KillianNeural", "Killian", false, "jung, frisch"),
        PremiumStimme("de-AT-IngridNeural", "Ingrid", true, "österreichisch"),
        PremiumStimme("de-AT-JonasNeural", "Jonas", false, "österreichisch"),
        PremiumStimme("de-CH-LeniNeural", "Leni", true, "schweizerisch"),
        PremiumStimme("de-CH-JanNeural", "Jan", false, "schweizerisch"),
        PremiumStimme("en-US-AvaMultilingualNeural", "Ava", true, "warm, ausdrucksstark"),
        PremiumStimme("en-US-AndrewMultilingualNeural", "Andrew", false, "warm, souverän"),
        PremiumStimme("en-US-EmmaMultilingualNeural", "Emma", true, "fröhlich, klar"),
        PremiumStimme("en-US-BrianMultilingualNeural", "Brian", false, "locker, nahbar"),
        PremiumStimme("en-GB-SoniaNeural", "Sonia", true, "britisch"),
        PremiumStimme("en-GB-RyanNeural", "Ryan", false, "britisch"),
        PremiumStimme("fr-FR-VivienneMultilingualNeural", "Vivienne", true, "warm, sehr natürlich"),
        PremiumStimme("fr-FR-RemyMultilingualNeural", "Rémy", false, "ruhig, sehr natürlich"),
        PremiumStimme("fr-FR-DeniseNeural", "Denise", true, "klar, freundlich"),
        PremiumStimme("fr-FR-HenriNeural", "Henri", false, "sonor"),
        PremiumStimme("es-ES-XimenaNeural", "Ximena", true, "warm, natürlich"),
        PremiumStimme("es-ES-AlvaroNeural", "Álvaro", false, "ruhig, klar"),
        PremiumStimme("es-ES-ElviraNeural", "Elvira", true, "klar, freundlich"),
        PremiumStimme("es-MX-DaliaNeural", "Dalia", true, "mexikanisch"),
        PremiumStimme("es-MX-JorgeNeural", "Jorge", false, "mexikanisch"),
        PremiumStimme("pt-BR-ThalitaMultilingualNeural", "Thalita", true, "warm, sehr natürlich"),
        PremiumStimme("pt-BR-AntonioNeural", "Antônio", false, "ruhig, klar"),
        PremiumStimme("pt-BR-FranciscaNeural", "Francisca", true, "klar, freundlich"),
        PremiumStimme("pt-PT-RaquelNeural", "Raquel", true, "europäisch"),
        PremiumStimme("pt-PT-DuarteNeural", "Duarte", false, "europäisch"),
    )

    fun istPremium(id: String): Boolean = STIMMEN.any { it.id == id }
    fun finde(id: String): PremiumStimme? = STIMMEN.firstOrNull { it.id == id }

    /** Stimmen einer Sprache, die Region des Geräts zuerst – so sieht jeder nur seine Sprache. */
    fun fuer(sprache: String, region: String = ""): List<PremiumStimme> =
        STIMMEN.filter { it.sprache == sprache }.sortedByDescending { it.region.equals(region, ignoreCase = true) }

    /** Vorgabe einer Sprache passend zur Region des Geräts (z. B. es-MX in Lateinamerika). */
    fun vorgabe(sprache: String, region: String = ""): String? {
        val eigene = STIMMEN.filter { it.sprache == sprache }
        val regional = when {
            sprache == "es" && region.uppercase() in LATEINAMERIKA -> "MX"
            sprache == "en" && region.uppercase() in listOf("GB", "IE") -> "GB"
            sprache == "pt" && region.uppercase() != "BR" && region.isNotBlank() -> "PT"
            sprache == "de" && region.uppercase() in listOf("AT", "CH") -> null // Hauptstimmen klingen natürlicher; Regionalstimmen stehen gleich darunter.
            else -> null
        }
        return (regional?.let { r -> eigene.firstOrNull { it.region == r } } ?: eigene.firstOrNull())?.id
    }

    private val LATEINAMERIKA = setOf("MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU", "BO", "DO", "HN", "PY", "SV", "NI", "CR", "PA", "UY", "US")
}

/**
 * Erzeugt Premium-Sprache als MP3-Datei über den Vorlese-Dienst von Microsoft Edge. Nur Datei, kein Abspielen:
 * Die Weckansage wird vorab erzeugt und beim Wecken offline abgespielt. Almanach: bugs/apis/tts-provider.md E/ET.
 */
object EdgeStimmen {
    private const val TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val CHROMIUM_VOLL = "143.0.3650.75"
    private const val CHROMIUM_MAJOR = "143"
    private const val WINDOWS_EPOCHE = 11_644_473_600L
    private const val URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
    private const val ZEITLIMIT_MS = 45_000L

    private val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).build()
    /** Höchstens ein Stream zugleich (Almanach E7: parallele Streams führen zu Sperren). */
    private val mutex = Mutex()
    /** Abweichung der Geräteuhr vom Server in Sekunden (Almanach E2), aus dem Date-Header einer 403-Antwort. */
    @Volatile private var uhrVersatz = 0L

    /** Gibt es gerade ein nutzbares Netz? Ohne Netz gar nicht erst versuchen (spart 12 s Wartezeit je Absatz). */
    fun netzDa(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun synthetisiere(text: String, stimme: String, tempo: Float, ziel: File) = mutex.withLock {
        val sauber = text.map { if (it.code < 32 && it !in "\t\n\r") ' ' else it }.joinToString("").trim()
        if (sauber.none { it.isLetterOrDigit() }) throw SyntheseAbbruch("Kein sprechbarer Text.")
        withContext(Dispatchers.IO) {
            try { einVersuch(sauber, stimme, tempo, ziel) }
            catch (e: VersatzKorrigiert) { delay(400); einVersuch(sauber, stimme, tempo, ziel) }
        }
    }

    private class VersatzKorrigiert : Exception()

    private suspend fun einVersuch(text: String, stimme: String, tempo: Float, ziel: File) = withTimeout(ZEITLIMIT_MS) {
        suspendCancellableCoroutine { fortsetzung ->
            val ausgabe = FileOutputStream(ziel)
            val fertig = AtomicBoolean(false)
            fun ende(fehler: Throwable?, socket: WebSocket?) {
                if (!fertig.compareAndSet(false, true)) return
                runCatching { ausgabe.close() }
                socket?.cancel()
                if (fehler == null && ziel.length() > 0) fortsetzung.resumeSafe(Unit)
                else fortsetzung.failSafe(fehler ?: SyntheseAbbruch("Die Premium-Stimme hat keinen Ton geliefert."))
            }
            val anfrage = Request.Builder()
                .url("$URL?TrustedClientToken=$TOKEN&Sec-MS-GEC=${secMsGec()}&Sec-MS-GEC-Version=1-$CHROMIUM_VOLL&ConnectionId=${uuid()}")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/$CHROMIUM_MAJOR.0.0.0 Safari/537.36 Edg/$CHROMIUM_MAJOR.0.0.0")
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("Pragma", "no-cache").header("Cache-Control", "no-cache")
                .header("Cookie", "muid=${uuid().uppercase()};")
                .build()
            val socket = client.newWebSocket(anfrage, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val konfig = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                        """{"context":{"synthesis":{"audio":{"metadataOptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-96kbitrate-mono-mp3"}}}}"""
                    val prozent = ((tempo.coerceIn(.5f, 2f) - 1f) * 100).roundToInt()
                    val rate = if (prozent >= 0) "+$prozent%" else "$prozent%"
                    val escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    val lang = stimme.split('-').take(2).joinToString("-")
                    val ssml = "X-RequestId:${uuid()}\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
                        "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$lang'><voice name='$stimme'>" +
                        "<prosody rate='$rate' pitch='+0Hz' volume='+0%'>$escaped</prosody></voice></speak>"
                    if (!webSocket.send(konfig) || !webSocket.send(ssml)) ende(SyntheseAbbruch("Die Anfrage an die Premium-Stimme ließ sich nicht senden."), webSocket)
                }
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val daten = bytes.toByteArray()
                    if (daten.size <= 2) return
                    val kopf = ((daten[0].toInt() and 0xFF) shl 8) or (daten[1].toInt() and 0xFF)
                    val start = kopf + 2
                    if (start >= daten.size) return
                    try { if (!fertig.get()) ausgabe.write(daten, start, daten.size - start) } catch (e: Exception) { ende(e, webSocket) }
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("Path:turn.end")) ende(null, webSocket)
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (response?.code == 403) {
                        val server = response.headers.getDate("Date")?.time
                        if (server != null) {
                            val neu = server / 1000 - System.currentTimeMillis() / 1000
                            if (kotlin.math.abs(neu - uhrVersatz) > 60) { uhrVersatz = neu; ende(VersatzKorrigiert(), webSocket); return }
                        }
                    }
                    ende(SyntheseAbbruch("Die Premium-Stimme ist gerade nicht erreichbar.", t), webSocket)
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    ende(SyntheseAbbruch("Die Verbindung zur Premium-Stimme wurde vorzeitig beendet."), webSocket)
                }
            })
            fortsetzung.invokeOnCancellation { if (fertig.compareAndSet(false, true)) { runCatching { ausgabe.close() }; socket.cancel() } }
        }
    }

    private fun <T> CancellableContinuation<T>.resumeSafe(wert: T) { if (isActive) resume(wert) }
    private fun CancellableContinuation<*>.failSafe(fehler: Throwable) { if (isActive) resumeWithException(fehler) }

    private fun uuid() = UUID.randomUUID().toString().replace("-", "")

    private fun secMsGec(): String {
        val sekunden = System.currentTimeMillis() / 1000 + uhrVersatz
        val gerundet = sekunden - sekunden % 300
        val ticks = (gerundet + WINDOWS_EPOCHE) * 10_000_000L
        return MessageDigest.getInstance("SHA-256").digest("$ticks$TOKEN".toByteArray(Charsets.US_ASCII))
            .joinToString("") { String.format(Locale.ROOT, "%02X", it) }
    }
}
