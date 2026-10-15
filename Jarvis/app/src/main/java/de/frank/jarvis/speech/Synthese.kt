package de.frank.jarvis.speech

import android.content.Context
import android.util.Base64
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.tts.EdgeTtsPlayer
import de.frank.jarvis.tts.TtsCatalog
import de.frank.jarvis.tts.TtsProvider
import de.frank.jarvis.tts.deutschesSsml
import de.frank.jarvis.tts.chirpAussprachen
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

/** Ein Fehler, der die ganze Sitzung betrifft — die Pipeline hält dann sofort an. */
class SyntheseAbbruch(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Ein Fehler an genau diesem Absatz — der Absatz wird halbiert und erneut versucht. */
class SyntheseFehler(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Die Stimme hat nicht rechtzeitig geantwortet. Bewusst weder [IOException] noch [SyntheseFehler]:
 * Ein Zeitlimit wird weder halbiert noch wiederholt, sonst stünde das Vorlesen bei hängender Verbindung
 * minutenlang auf „lädt“ (und das Weckwort bliebe so lange taub). Der Teil wird übersprungen.
 */
class SyntheseZeitlimit(message: String) : Exception(message)

/**
 * Eigene Stimme für einzelne Meldungen (z. B. Update-Ansagen von UpdateStation), abweichend von Jarvis' Vorlese-Stimme.
 * [stimme] ist eine Kennung aus dem [TtsCatalog] (Edge, Google oder Alibaba), null = wie Jarvis; [tempo] null = wie Jarvis.
 */
data class StimmWahl(val stimme: String? = null, val tempo: Float? = null)

/**
 * Erzeugt Audio, ohne es abzuspielen — die Voraussetzung für die Vorausschau der
 * Absatz-Pipeline (Baustein D 4.2, Schritt 3).
 */
class Synthese(context: Context, private val settings: SecureSettings) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Begrenzt die Gleichzeitigkeit, sonst laufen die Dienste ins Rate-Limit (Baustein D 4.4). */
    private val gleichzeitig = Semaphore(2)

    /**
     * Ob die gewählte Engine vorab synthetisieren kann. Edge schreibt seinen Ton inzwischen ebenfalls
     * erst in eine Datei — so greift die Vorausschau auch beim Standardanbieter.
     */
    fun kannVorausschauen(wahl: StimmWahl? = null): Boolean = when (anbieter(wahl)) {
        TtsProvider.GOOGLE_CLOUD.id, TtsProvider.QWEN_CLONE.id, TtsProvider.QWEN.id, TtsProvider.EDGE.id -> true
        else -> false
    }

    /**
     * Prüft den Google-Schlüssel unabhängig von der gerade gewählten Stimme. Ohne diesen Weg
     * würde die Prüfung über [synthetisiere] laufen und bei einer Edge- oder Alibaba-Stimme
     * mit "kann nicht vorab erzeugt werden" abbrechen, obwohl der Schlüssel in Ordnung ist.
     */
    suspend fun pruefeGoogle(text: String = "Probe."): File = gleichzeitig.withPermit { google(text, null, settings.ttsSpeechRate) }

    /** Anbieter der [wahl], sonst der in Jarvis eingestellte. */
    private fun anbieter(wahl: StimmWahl?): String =
        wahl?.stimme?.let { TtsCatalog.anbieterZu(it)?.id } ?: settings.ttsProvider

    suspend fun synthetisiere(text: String, wahl: StimmWahl? = null): File = gleichzeitig.withPermit {
        val sonder = wahl?.stimme?.takeIf { TtsCatalog.anbieterZu(it) != null }
        val tempo = wahl?.tempo ?: settings.ttsSpeechRate
        when (anbieter(wahl)) {
            TtsProvider.GOOGLE_CLOUD.id -> google(text, sonder, tempo)
            TtsProvider.QWEN_CLONE.id -> qwen(text, geklont = true)
            TtsProvider.QWEN.id -> qwen(text, geklont = false, sonder)
            TtsProvider.EDGE.id -> edge(text, sonder, tempo)
            else -> throw SyntheseAbbruch("Diese Stimme kann nicht vorab erzeugt werden.")
        }
    }

    private suspend fun google(text: String, sonderStimme: String?, tempo: Float): File {
        val schluessel = settings.googleTtsApiKey.trim()
        if (schluessel.isBlank()) {
            throw SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Es ist kein Google-Schlüssel hinterlegt. " +
                    "Trag ihn in den Einstellungen ein.",
            )
        }
        val stimme = sonderStimme ?: settings.googleTtsVoice
        // Chirp 3 HD versteht kein SSML — dort bleibt es beim reinen Text, die Sprache steuert
        // allein der languageCode. Alle uebrigen Google-Stimmen werden per <lang> festgenagelt.
        val alsSsml = settings.immerDeutschVorlesen && !stimme.contains("Chirp")
        // Chirp: feste deutsche Aussprachen (etwa „Frank“) mitschicken; lehnt Google sie ab, ohne sie erneut.
        val aussprachen = if (!alsSsml && stimme.contains("Chirp")) chirpAussprachen(text) else null
        if (aussprachen != null) {
            try {
                return googleAnfrage(text, stimme, schluessel, alsSsml, aussprachen, tempo)
            } catch (fehler: SyntheseFehler) {
                if (fehler.message?.contains("Fehler 400") != true) throw fehler
            }
        }
        return googleAnfrage(text, stimme, schluessel, alsSsml, null, tempo)
    }

    private suspend fun googleAnfrage(text: String, stimme: String, schluessel: String, alsSsml: Boolean, aussprachen: JSONObject?, tempo: Float): File {
        val koerper = JSONObject()
            .put(
                "input",
                if (alsSsml) {
                    JSONObject().put("ssml", deutschesSsml(text))
                } else {
                    JSONObject().put("text", text).apply { aussprachen?.let { put("customPronunciations", it) } }
                },
            )
            .put("voice", JSONObject().put("languageCode", "de-DE").put("name", stimme))
            .put(
                "audioConfig",
                JSONObject()
                    .put("audioEncoding", "MP3")
                    .put("speakingRate", tempo.coerceIn(0.25f, 4f).toDouble()),
                // Chirp-3-HD-Stimmen kennen keinen pitch-Parameter — er wird gar nicht erst
                // gesendet, sonst antwortet Google mit Fehler 400.
            )
        val adresse = GOOGLE_URL.toHttpUrl().newBuilder().addQueryParameter("key", schluessel).build()
        val anfrage = Request.Builder()
            .url(adresse)
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val antwort = fuehreAus(anfrage)
        val json = JSONObject(antwort)
        val audio = json.optString("audioContent")
        if (audio.isBlank()) throw SyntheseFehler("Google hat kein Audio geliefert.")
        return schreibeDatei(Base64.decode(audio, Base64.DEFAULT), "mp3")
    }

    /**
     * @param geklont true für meine eigene Stimme (Voice-Clone-Modell), false für die fertigen
     *   Alibaba-Standardstimmen. Klonen und Sprechen müssen dasselbe Modell benutzen, sonst wird
     *   die Stimm-Kennung abgelehnt — darum zwei Modellnamen.
     */
    private suspend fun qwen(text: String, geklont: Boolean, sonderStimme: String? = null): File {
        val schluessel = settings.qwenTtsApiKey.filterNot(Char::isWhitespace)
        val stimme = if (geklont) {
            settings.qwenTtsVoiceId.filterNot(Char::isWhitespace)
        } else {
            sonderStimme ?: settings.qwenStandardVoice
        }
        if (schluessel.isBlank()) {
            throw SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Der Alibaba-Schlüssel fehlt. Trag ihn in den Einstellungen ein.",
            )
        }
        if (stimme.isBlank()) {
            throw SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Die eigene Stimme ist nicht eingerichtet. " +
                    "Nimm sie in den Einstellungen auf.",
            )
        }
        val koerper = JSONObject()
            .put("model", if (geklont) QWEN_KLON_MODELL else QWEN_MODELL)
            .put(
                "input",
                JSONObject().put("text", text).put("voice", stimme).put("language_type", "German"),
            )
        val anfrage = Request.Builder()
            .url(QWEN_URL)
            .addHeader("Authorization", "Bearer $schluessel")
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val antwort = fuehreAus(anfrage)
        val rohAdresse = JSONObject(antwort)
            .optJSONObject("output")?.optJSONObject("audio")?.optString("url").orEmpty()
        if (rohAdresse.isBlank()) throw SyntheseFehler("Die eigene Stimme hat kein Audio geliefert.")
        // Die Adresse kommt teils als http — Android blockt das, also anheben.
        val adresse = if (rohAdresse.startsWith("http://")) {
            "https://" + rohAdresse.removePrefix("http://")
        } else {
            rohAdresse
        }
        val bytes = ladeHerunter(adresse)
        return schreibeDatei(bytes, "wav")
    }

    /**
     * Microsoft Edge über den WebSocket: Die Tonrahmen landen in einer Datei, abgespielt wird erst
     * im Vorleser. Fristen: Bis zum ersten Tonrahmen höchstens [EDGE_ERSTE_ANTWORT_MS], danach
     * höchstens [EDGE_LEERLAUF_MS] zwischen zwei Rahmen und insgesamt [EDGE_ZEITLIMIT_MS]. Reißt eine
     * davon, wird der Teil übersprungen ([SyntheseZeitlimit]), ohne Halbieren und Wiederholen.
     * Bewusst ohne withTimeout um die Erzeugung: Ein Zeitlimit-Abbruch wäre sonst eine
     * CancellationException und beendete das ganze Vorlesen statt nur diesen Absatz.
     */
    private suspend fun edge(text: String, sonderStimme: String?, tempo: Float): File {
        val datei = neueDatei("mp3")
        val beginn = System.currentTimeMillis()
        // Zeitpunkt des letzten Tonrahmens; 0 = noch keiner da.
        val letzterRahmen = AtomicLong(0L)
        val fertig: File? = coroutineScope {
            val erzeugung = async { edgeErzeuge(text, datei, letzterRahmen, sonderStimme, tempo) }
            var ergebnis: File? = null
            var abgelaufen = false
            while (ergebnis == null && !abgelaufen) {
                // Ein Fehler der Erzeugung kommt hier über await() heraus, wie zuvor.
                ergebnis = withTimeoutOrNull(EDGE_PRUEF_MS) { erzeugung.await() }
                if (ergebnis == null) {
                    val jetzt = System.currentTimeMillis()
                    val zuletzt = letzterRahmen.get()
                    val leerlauf = if (zuletzt == 0L) {
                        jetzt - beginn > EDGE_ERSTE_ANTWORT_MS
                    } else {
                        jetzt - zuletzt > EDGE_LEERLAUF_MS
                    }
                    abgelaufen = leerlauf || jetzt - beginn > EDGE_ZEITLIMIT_MS
                    // Der Abbruch schließt über invokeOnCancellation auch den WebSocket.
                    if (abgelaufen) erzeugung.cancel()
                }
            }
            ergebnis
        }
        if (fertig == null) {
            datei.delete()
            Logbuch.netz(
                "Edge-Stimme ohne rechtzeitige Antwort",
                "zeichen=${text.length} ton=${letzterRahmen.get() != 0L} ms=${System.currentTimeMillis() - beginn}",
            )
            throw SyntheseZeitlimit("Die Edge-Stimme hat nicht rechtzeitig geantwortet.")
        }
        Logbuch.sprache("Edge-Absatz erzeugt", "zeichen=${text.length}", System.currentTimeMillis() - beginn)
        return fertig
    }

    private suspend fun edgeErzeuge(text: String, datei: File, letzterRahmen: AtomicLong, sonderStimme: String?, tempo: Float): File = suspendCancellableCoroutine { fortsetzung ->
        val strom = FileOutputStream(datei)
        val beendet = AtomicBoolean(false)
        fun schliesse() {
            synchronized(strom) { runCatching { strom.close() } }
        }
        /** [ws] wird dabei geschlossen; null, wenn er schon zu ist (onFailure, onClosing). */
        fun scheitere(fehler: Exception, ws: WebSocket?) {
            if (!beendet.compareAndSet(false, true)) return
            // Ohne das bliebe die Verbindung offen, bis der Server sie beendet: Hier liest niemand mehr.
            ws?.cancel()
            schliesse()
            datei.delete()
            if (fortsetzung.isActive) fortsetzung.resumeWithException(fehler)
        }
        val zuhoerer = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val ssml = EdgeTtsPlayer.ssmlNachricht(
                    text = text,
                    voice = sonderStimme ?: settings.edgeTtsVoice.ifBlank { TtsCatalog.DEFAULT_EDGE_VOICE },
                    speechRate = tempo,
                    pitchPercent = 0,
                    erzwingeDeutsch = settings.immerDeutschVorlesen,
                )
                if (!webSocket.send(EdgeTtsPlayer.KONFIG) || !webSocket.send(ssml)) {
                    scheitere(IOException("Die Anfrage an die Edge-Stimme ging nicht hinaus."), webSocket)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (beendet.get()) return
                val daten = bytes.toByteArray()
                val tonAb = EdgeTtsPlayer.tonBeginn(daten)
                if (tonAb < 0) return
                letzterRahmen.set(System.currentTimeMillis())
                try {
                    synchronized(strom) { strom.write(daten, tonAb, daten.size - tonAb) }
                } catch (fehler: Exception) {
                    scheitere(IOException("Der Ton der Edge-Stimme ließ sich nicht speichern.", fehler), webSocket)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!text.contains("Path:turn.end") || !beendet.compareAndSet(false, true)) return
                schliesse()
                webSocket.close(1000, null)
                if (datei.length() == 0L) {
                    datei.delete()
                    if (fortsetzung.isActive) {
                        fortsetzung.resumeWithException(SyntheseFehler("Die Edge-Stimme hat kein Audio geliefert."))
                    }
                } else if (fortsetzung.isActive) {
                    fortsetzung.resume(datei)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (beendet.get()) return
                // Reine Netzfehler wiederholt der Vorleser einmal, eine Absage des Dienstes zählt als Absatzfehler.
                val code = response?.code
                Logbuch.netz(
                    "Edge-Stimme Verbindung gescheitert",
                    "art=${t.javaClass.simpleName}" + (code?.let { " code=$it" } ?: ""),
                )
                scheitere(
                    if (t is IOException && code == null) {
                        t
                    } else {
                        SyntheseFehler("Die Edge-Stimme ist nicht erreichbar" + (code?.let { " (Fehler $it)" } ?: "") + ".", t)
                    },
                    null,
                )
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (beendet.get()) return
                // Den Schließ-Rahmen beantworten, sonst bleibt die Verbindung halb offen. Bewusst 1000 statt
                // des Server-Codes: Reservierte Codes (etwa 1005) lehnt close() mit einer Ausnahme ab.
                runCatching { webSocket.close(1000, null) }
                scheitere(IOException("Die Edge-Stimme hat die Verbindung vor dem Ende geschlossen."), null)
            }
        }
        val socket = client.newWebSocket(EdgeTtsPlayer.anfrage(), zuhoerer)
        fortsetzung.invokeOnCancellation {
            socket.cancel()
            if (beendet.compareAndSet(false, true)) {
                schliesse()
                datei.delete()
            }
        }
    }

    private suspend fun fuehreAus(anfrage: Request): String {
        var versuch = 0
        while (true) {
            val antwort = client.newCall(anfrage).warte()
            val text = antwort.use { it.body?.string().orEmpty() to it.code }
            val (koerper, code) = text
            when {
                code in 200..299 -> return koerper
                // 429: exponentiell warten und erneut versuchen (Baustein D 4.4).
                code == 429 && versuch < WARTEZEITEN_MS.size -> {
                    kotlinx.coroutines.delay(WARTEZEITEN_MS[versuch])
                    de.frank.jarvis.observability.IdeenLog.warn(
                        "Synthese",
                        "fuehreAus",
                        "Rate-Limit, warte erneut",
                        mapOf("ms" to WARTEZEITEN_MS[versuch], "versuch" to versuch),
                    )
                    versuch++
                }
                code == 401 || code == 403 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Der Schlüssel wurde abgelehnt (Fehler $code). " +
                        "Prüf ihn in den Einstellungen.",
                )
                code == 429 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Das Kontingent der Stimme ist erschöpft (Fehler 429).",
                )
                else -> throw SyntheseFehler("Die Stimme antwortete mit Fehler $code: ${koerper.take(200)}")
            }
        }
    }

    private suspend fun ladeHerunter(adresse: String): ByteArray {
        val antwort = client.newCall(Request.Builder().url(adresse).build()).warte()
        return antwort.use {
            if (!it.isSuccessful) throw SyntheseFehler("Das Audio konnte nicht geladen werden (${it.code}).")
            it.body?.bytes() ?: throw SyntheseFehler("Das geladene Audio war leer.")
        }
    }

    private fun schreibeDatei(bytes: ByteArray, endung: String): File =
        neueDatei(endung).apply { writeBytes(bytes) }

    private fun neueDatei(endung: String): File {
        val verzeichnis = File(appContext.cacheDir, "vorlesen").apply { mkdirs() }
        return File(verzeichnis, "absatz_${UUID.randomUUID()}.$endung")
    }

    /** Räumt alle Zwischendateien weg, die eine beendete Wiedergabe hinterlassen hat. */
    fun raeumeAuf() {
        runCatching { File(appContext.cacheDir, "vorlesen").listFiles()?.forEach(File::delete) }
    }

    private suspend fun Call.warte(): Response = suspendCancellableCoroutine { fortsetzung ->
        fortsetzung.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (fortsetzung.isActive) fortsetzung.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                fortsetzung.resume(response) { _, abgebrochen, _ -> abgebrochen.close() }
            }
        })
    }

    private companion object {
        const val GOOGLE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
        const val QWEN_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
        /** Die fertigen Standardstimmen. */
        const val QWEN_MODELL = "qwen3-tts-flash"

        /** Die geklonten Stimmen — dasselbe Modell wie beim Anlegen (Baustein E). */
        const val QWEN_KLON_MODELL = "qwen3-tts-vc-2026-01-22"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        val WARTEZEITEN_MS = longArrayOf(1_000L, 3_000L, 8_000L)

        /** So lange darf Edge für einen Absatz insgesamt brauchen, bevor er als gescheitert gilt. */
        const val EDGE_ZEITLIMIT_MS = 60_000L

        /** Frist bis zum ersten Tonrahmen. */
        const val EDGE_ERSTE_ANTWORT_MS = 12_000L

        /** Höchste Pause zwischen zwei Tonrahmen; danach gilt die Verbindung als hängend. */
        const val EDGE_LEERLAUF_MS = 10_000L

        /** Wie oft die Fristen geprüft werden. */
        const val EDGE_PRUEF_MS = 500L
    }
}
