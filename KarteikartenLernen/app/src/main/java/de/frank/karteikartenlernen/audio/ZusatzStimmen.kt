package de.frank.karteikartenlernen.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini 3.8 Flash / Flash-Lite TTS, Azure Neural und (für den Rückfall) Google Chirp 3 HD —
 * derselbe Weg wie in Jarvis (Synthese.kt). Katalog, Erzeugung und Wiedergabe an einer Stelle.
 */
object ZusatzStimmen {
    const val PROVIDER_GEMINI_FLASH = "gemini_flash"
    const val PROVIDER_GEMINI_LITE = "gemini_lite"
    const val PROVIDER_AZURE = "azure_neural"
    const val PROVIDER_GOOGLE = "google_cloud"

    data class Stimme(val id: String, val name: String, val weiblich: Boolean)

    private val CHIRP_NAMEN_W = listOf(
        "Achernar", "Aoede", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux", "Kore",
        "Laomedeia", "Leda", "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr",
    )
    private val CHIRP_NAMEN_M = listOf(
        "Achird", "Algenib", "Algieba", "Alnilam", "Charon", "Enceladus", "Fenrir", "Iapetus",
        "Orus", "Puck", "Rasalgethi", "Sadachbia", "Sadaltager", "Schedar", "Umbriel", "Zubenelgenubi",
    )

    /** Die 30 Chirp-3-HD-Stimmen (Kennung „de-DE-Chirp3-HD-Kore“). */
    val googleStimmen: List<Stimme> =
        CHIRP_NAMEN_W.map { Stimme("de-DE-Chirp3-HD-$it", "$it — weiblich", true) } +
            CHIRP_NAMEN_M.map { Stimme("de-DE-Chirp3-HD-$it", "$it — männlich", false) }
    const val DEFAULT_GOOGLE_VOICE = "de-DE-Chirp3-HD-Kore"

    /**
     * Gemini 3.8 TTS hat dieselben 30 Stimmnamen wie Chirp 3 HD. Die Kennung trägt den Anbieter vorn
     * („gemini_flash:Leda“), damit [anbieterZu] Flash, Flash-Lite und Chirp auseinanderhält.
     */
    val geminiFlashStimmen = geminiStimmen(PROVIDER_GEMINI_FLASH)
    val geminiLiteStimmen = geminiStimmen(PROVIDER_GEMINI_LITE)
    const val DEFAULT_GEMINI_FLASH_VOICE = "$PROVIDER_GEMINI_FLASH:Leda"
    const val DEFAULT_GEMINI_LITE_VOICE = "$PROVIDER_GEMINI_LITE:Leda"

    private fun geminiStimmen(anbieter: String) =
        (CHIRP_NAMEN_W.map { Stimme("$anbieter:$it", "$it — weiblich", true) } +
            CHIRP_NAMEN_M.map { Stimme("$anbieter:$it", "$it — männlich", false) })

    /** Der Stimmname, den Gemini erwartet („Leda“). */
    fun geminiName(stimmeId: String): String = stimmeId.substringAfter(':')

    /** Dieselbe Stimme bei Chirp 3 HD — für den Rückfall, wenn der kostenlose Gemini-Schlüssel scheitert. */
    fun chirpZu(stimmeId: String): String = "de-DE-Chirp3-HD-${geminiName(stimmeId)}"

    const val GEMINI_TTS_FREI_DANN_BEZAHLT = "frei_dann_bezahlt"
    const val GEMINI_TTS_NUR_FREI = "nur_frei"
    const val GEMINI_TTS_NUR_BEZAHLT = "nur_bezahlt"
    val GEMINI_TTS_SCHLUESSEL_WAHLEN = listOf(
        GEMINI_TTS_FREI_DANN_BEZAHLT to "Erst kostenlos, dann bezahlt",
        GEMINI_TTS_NUR_FREI to "Nur kostenlos",
        GEMINI_TTS_NUR_BEZAHLT to "Nur bezahlt",
    )

    /**
     * Azure AI Speech (eigenes Konto, Schlüssel + Region): dieselben Neural-Stimmen wie Edge, offiziell und
     * mit allen deutschen Stimmen, dazu die HD-Stimmen (DragonHD). Die Kennung trägt „azure:“ vorn, damit
     * [anbieterZu] sie von den gleichnamigen Edge-Stimmen unterscheidet. HD zählt nicht zum kostenlosen F0-Kontingent.
     */
    val azureStimmen = listOf(
        Stimme("azure:de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD — weiblich", true),
        Stimme("azure:de-DE-Florian:DragonHDLatestNeural", "Florian HD — männlich", false),
        Stimme("azure:de-DE-SeraphinaMultilingualNeural", "Seraphina — weiblich", true),
        Stimme("azure:de-DE-KatjaNeural", "Katja — weiblich", true),
        Stimme("azure:de-DE-AmalaNeural", "Amala — weiblich", true),
        Stimme("azure:de-DE-ElkeNeural", "Elke — weiblich", true),
        Stimme("azure:de-DE-GiselaNeural", "Gisela (Kind) — weiblich", true),
        Stimme("azure:de-DE-KlarissaNeural", "Klarissa — weiblich", true),
        Stimme("azure:de-DE-LouisaNeural", "Louisa — weiblich", true),
        Stimme("azure:de-DE-MajaNeural", "Maja — weiblich", true),
        Stimme("azure:de-DE-TanjaNeural", "Tanja — weiblich", true),
        Stimme("azure:de-DE-FlorianMultilingualNeural", "Florian — männlich", false),
        Stimme("azure:de-DE-ConradNeural", "Conrad — männlich", false),
        Stimme("azure:de-DE-KillianNeural", "Killian — männlich", false),
        Stimme("azure:de-DE-BerndNeural", "Bernd — männlich", false),
        Stimme("azure:de-DE-ChristophNeural", "Christoph — männlich", false),
        Stimme("azure:de-DE-KasperNeural", "Kasper — männlich", false),
        Stimme("azure:de-DE-KlausNeural", "Klaus — männlich", false),
        Stimme("azure:de-DE-RalfNeural", "Ralf — männlich", false),
    )
    const val DEFAULT_AZURE_VOICE = "azure:de-DE-SeraphinaMultilingualNeural"
    const val DEFAULT_AZURE_REGION = "westeurope"

    /** Der Stimmname, den Azure erwartet („de-DE-KatjaNeural“). */
    fun azureName(stimmeId: String): String = stimmeId.removePrefix("azure:")

    fun istAzureHd(stimmeId: String): Boolean = stimmeId.contains("DragonHD")

    /**
     * Der Regionsname für die Adresse („westeurope“). Nimmt auch den eingefügten Endpunkt aus dem Portal
     * („https://westeurope.api.cognitive.microsoft.com/“); null, wenn nichts Brauchbares darin steht.
     */
    fun azureRegion(eingabe: String): String? {
        val roh = eingabe.trim().lowercase().ifBlank { DEFAULT_AZURE_REGION }
        val ausEndpunkt = Regex("^(?:https?://)?([a-z0-9]+)\\.(?:api\\.cognitive|tts\\.speech)\\.").find(roh)?.groupValues?.get(1)
        return (ausEndpunkt ?: roh).takeIf { Regex("^[a-z0-9]{2,40}$").matches(it) }
    }

    /** Findet den Anbieter zu einer Stimm-Kennung (nur die hier geführten Anbieter). */
    fun anbieterZu(stimmeId: String): String? = when {
        azureStimmen.any { it.id == stimmeId } -> PROVIDER_AZURE
        googleStimmen.any { it.id == stimmeId } -> PROVIDER_GOOGLE
        geminiFlashStimmen.any { it.id == stimmeId } -> PROVIDER_GEMINI_FLASH
        geminiLiteStimmen.any { it.id == stimmeId } -> PROVIDER_GEMINI_LITE
        else -> null
    }
}

/** Fehler einer Zusatz-Stimme mit lesbarer Meldung (enthält den HTTP-Code, wo es einen gibt). */
class ZusatzStimmenFehler(message: String) : Exception(message)

/** Einstellungen, die eine Zusatz-Stimme zum Sprechen braucht. */
data class ZusatzStimmenWahl(
    val anbieter: String,
    val stimme: String,
    val tempo: Float = 1f,
    val geminiKeyFrei: String = "",
    val geminiKeyBezahlt: String = "",
    val geminiSchluesselWahl: String = ZusatzStimmen.GEMINI_TTS_FREI_DANN_BEZAHLT,
    val geminiChirpRueckfall: Boolean = false,
    val googleKey: String = "",
    val azureKey: String = "",
    val azureRegion: String = ZusatzStimmen.DEFAULT_AZURE_REGION,
)

/**
 * Erzeugt den Ton einer Zusatz-Stimme als Datei und spielt ihn mit dem MediaPlayer ab —
 * mit derselben Rückruf-Form (Start, Ende, Fehler) wie die übrigen Player der App.
 */
class ZusatzStimmenPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()
    private var job: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    fun speak(
        text: String,
        wahl: ZusatzStimmenWahl,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        stop()
        job = bereich.launch {
            val datei = try {
                withContext(Dispatchers.IO) { erzeuge(text, wahl) }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                Log.e(TAG, "Zusatz-Stimme ${wahl.anbieter} gescheitert: ${fehler.message}")
                onError(fehler)
                return@launch
            }
            spiele(datei, onPlaybackStart, onComplete, onError)
        }
    }

    /** Nur erzeugen, nicht abspielen. */
    suspend fun erzeuge(text: String, wahl: ZusatzStimmenWahl): File = when (wahl.anbieter) {
        ZusatzStimmen.PROVIDER_GEMINI_FLASH, ZusatzStimmen.PROVIDER_GEMINI_LITE -> gemini(text, wahl)
        ZusatzStimmen.PROVIDER_AZURE -> azure(text, wahl)
        ZusatzStimmen.PROVIDER_GOOGLE -> chirp(text, wahl.stimme, wahl.googleKey, wahl.tempo)
        else -> throw ZusatzStimmenFehler("Unbekannte Stimme: ${wahl.anbieter}")
    }

    private fun spiele(datei: File, onPlaybackStart: (() -> Unit)?, onComplete: () -> Unit, onError: (Exception) -> Unit) {
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            player.setDataSource(datei.absolutePath)
            player.setOnCompletionListener {
                datei.delete()
                if (mediaPlayer === player) { player.release(); mediaPlayer = null }
                onComplete()
            }
            player.setOnErrorListener { _, was, extra ->
                datei.delete()
                if (mediaPlayer === player) { player.release(); mediaPlayer = null }
                onError(ZusatzStimmenFehler("Wiedergabe gescheitert ($was/$extra)."))
                true
            }
            player.prepare()
            player.start()
            onPlaybackStart?.invoke()
        } catch (fehler: Exception) {
            datei.delete()
            runCatching { player.release() }
            if (mediaPlayer === player) mediaPlayer = null
            onError(fehler)
        }
    }

    fun pause(): Boolean = runCatching { mediaPlayer?.takeIf { it.isPlaying }?.pause(); mediaPlayer != null }.getOrDefault(false)

    fun resume(): Boolean = runCatching { mediaPlayer?.start(); mediaPlayer != null }.getOrDefault(false)

    fun stop() {
        job?.cancel()
        job = null
        mediaPlayer?.let { runCatching { it.stop() }; runCatching { it.release() } }
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    /**
     * Reihenfolge laut Schlüsselwahl: erst der kostenlose, dann der bezahlte Schlüssel. Ist der
     * Chirp-Rückfall an, geht es nach einem Fehler des kostenlosen Schlüssels nicht zum bezahlten,
     * sondern mit derselben Stimme über Google Chirp 3 HD weiter. Netzfehler laufen unverändert durch.
     */
    private suspend fun gemini(text: String, w: ZusatzStimmenWahl): File {
        val modell = if (w.anbieter == ZusatzStimmen.PROVIDER_GEMINI_LITE) GEMINI_LITE_MODELL else GEMINI_FLASH_MODELL
        val frei = w.geminiKeyFrei.filterNot(Char::isWhitespace)
        val bezahlt = w.geminiKeyBezahlt.filterNot(Char::isWhitespace)
        val wahl = w.geminiSchluesselWahl
        var fehlerFrei: Exception? = null
        if (wahl != ZusatzStimmen.GEMINI_TTS_NUR_BEZAHLT) {
            val gesperrt = System.currentTimeMillis() < freiGesperrtBis
            if (frei.isNotBlank() && !gesperrt) {
                // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
                val ausweg = w.geminiChirpRueckfall || (wahl == ZusatzStimmen.GEMINI_TTS_FREI_DANN_BEZAHLT && bezahlt.isNotBlank())
                try {
                    return geminiAnfrage(text, modell, w.stimme, frei, "kostenlos", warteBei429 = !ausweg)
                } catch (fehler: ZusatzStimmenFehler) {
                    fehlerFrei = fehler
                    if (ausweg) freiGesperrtBis = System.currentTimeMillis() + FREI_PAUSE_MS
                    Log.w(TAG, "Gemini-Stimme: kostenloser Schlüssel gescheitert ($modell): ${fehler.message?.take(120)}")
                }
            }
            if (w.geminiChirpRueckfall) {
                Log.i(TAG, "Gemini-Stimme: Rückfall auf Chirp 3 HD (${ZusatzStimmen.geminiName(w.stimme)}, gesperrt=$gesperrt)")
                return chirp(text, ZusatzStimmen.chirpZu(w.stimme), w.googleKey, w.tempo)
            }
            if (wahl == ZusatzStimmen.GEMINI_TTS_NUR_FREI) {
                throw fehlerFrei ?: ZusatzStimmenFehler("Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt.")
            }
        }
        if (bezahlt.isBlank()) {
            throw fehlerFrei ?: ZusatzStimmenFehler("Es ist kein Gemini-Schlüssel für die Stimme hinterlegt.")
        }
        return geminiAnfrage(text, modell, w.stimme, bezahlt, "bezahlt", warteBei429 = true)
    }

    private suspend fun geminiAnfrage(text: String, modell: String, stimmeId: String, schluessel: String, art: String, warteBei429: Boolean): File {
        // Gemini liest den Text wörtlich vor; eine Anweisung im Text würde mitgesprochen.
        val koerper = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))))
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", ZusatzStimmen.geminiName(stimmeId))),
                        ),
                    ),
            )
        val anfrage = Request.Builder()
            .url("$GEMINI_URL$modell:generateContent")
            .addHeader("x-goog-api-key", schluessel)
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val kandidat = JSONObject(String(fuehreAus(anfrage, warteBei429, "Gemini"), Charsets.UTF_8))
            .optJSONArray("candidates")?.optJSONObject(0)
        val ende = kandidat?.optString("finishReason").orEmpty()
        val teil = kandidat?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("inlineData")
        val daten = teil?.optString("data").orEmpty()
        if (daten.isBlank()) throw ZusatzStimmenFehler("Gemini hat kein Audio geliefert ($ende).")
        if (ende.isNotBlank() && ende != "STOP") throw ZusatzStimmenFehler("Gemini hat die Stimme abgebrochen ($ende).")
        val roh = Base64.decode(daten, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(teil?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val wav = if (roh.size > 44 && String(roh, 0, 4, Charsets.US_ASCII) == "RIFF") roh else mitWavKopf(roh, rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val sekunden = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (text.length >= 60 && sekunden < text.length / 25.0) {
            throw ZusatzStimmenFehler("Gemini-Audio zu kurz (${"%.1f".format(sekunden)} s für ${text.length} Zeichen).")
        }
        Log.i(TAG, "Gemini-Absatz erzeugt: modell=$modell schluessel=$art zeichen=${text.length}")
        return schreibeDatei(wav, "wav")
    }

    /**
     * Azure AI Speech über REST: Schlüssel und Region der eigenen Speech-Ressource (F0 kostenlos oder S0).
     * HD-Stimmen (DragonHD) bekommen weder Tempo noch <lang>-Rahmen: Sie bestimmen Betonung und Tempo selbst.
     */
    private suspend fun azure(text: String, w: ZusatzStimmenWahl): File {
        val schluessel = w.azureKey.filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) throw ZusatzStimmenFehler("Es ist kein Azure-Schlüssel hinterlegt.")
        val region = ZusatzStimmen.azureRegion(w.azureRegion)
            ?: throw ZusatzStimmenFehler("Die Azure-Region „${w.azureRegion.take(40)}“ ist unbekannt. Trag den Regionsnamen ein, etwa westeurope.")
        val stimme = ZusatzStimmen.azureName(w.stimme)
        val hd = ZusatzStimmen.istAzureHd(w.stimme)
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = ssmlEscape(text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " "))
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die übrigen sprechen ohnehin nur Deutsch.
        val inhalt = if (stimme.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val prozent = ((w.tempo.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
        val mitTempo = if (hd || prozent == 0) inhalt else "<prosody rate='${if (prozent > 0) "+" else ""}$prozent%'>$inhalt</prosody>"
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$stimme'>$mitTempo</voice></speak>"
        val anfrage = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", schluessel)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .addHeader("User-Agent", "Vorleser")
            .post(ssml.toRequestBody(SSML_TYP))
            .build()
        val bytes = azureAnfrage(anfrage, hd)
        if (bytes.isEmpty()) throw ZusatzStimmenFehler("Azure hat kein Audio geliefert.")
        Log.i(TAG, "Azure-Absatz erzeugt: stimme=$stimme zeichen=${text.length}")
        return schreibeDatei(bytes, "mp3")
    }

    /**
     * F0 erlaubt nur rund 20 Anfragen je Minute: Ein 429 ist meist dieses Ratenlimit, nicht das
     * Monatskontingent — darum nach Retry-After (höchstens eine Minute) warten.
     */
    private suspend fun azureAnfrage(anfrage: Request, hd: Boolean): ByteArray {
        var versuch = 0
        while (true) {
            val antwort = client.newCall(anfrage).warte()
            val warteSek = antwort.header("Retry-After")?.trim()?.toLongOrNull()
            val (koerper, code) = antwort.use { (if (it.isSuccessful) it.body?.bytes() else it.body?.string()?.toByteArray()) to it.code }
            when {
                code in 200..299 -> return koerper ?: ByteArray(0)
                code == 429 && versuch < AZURE_WARTEZEITEN_MS.size -> {
                    delay(warteSek?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: AZURE_WARTEZEITEN_MS[versuch])
                    versuch++
                }
                code == 401 || code == 403 -> throw ZusatzStimmenFehler("Azure hat Schlüssel oder Region abgelehnt (Fehler $code).")
                code == 429 -> throw ZusatzStimmenFehler("Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.")
                code == 400 && hd -> throw ZusatzStimmenFehler("Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.")
                else -> throw ZusatzStimmenFehler("Azure antwortete mit Fehler $code: ${koerper?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
    }

    /** Google Chirp 3 HD (texttospeech.googleapis.com) — eigene Stimme und Rückfall für Gemini. */
    private suspend fun chirp(text: String, stimme: String, schluesselRoh: String, tempo: Float): File {
        val schluessel = schluesselRoh.trim()
        if (schluessel.isBlank()) throw ZusatzStimmenFehler("Für Chirp 3 HD fehlt der Google-TTS-Schlüssel.")
        val koerper = JSONObject()
            .put("input", JSONObject().put("text", text))
            .put("voice", JSONObject().put("languageCode", "de-DE").put("name", stimme))
            .put(
                "audioConfig",
                JSONObject().put("audioEncoding", "MP3").put("speakingRate", tempo.coerceIn(0.25f, 4f).toDouble()),
            )
        val adresse = GOOGLE_URL.toHttpUrl().newBuilder().addQueryParameter("key", schluessel).build()
        val anfrage = Request.Builder().url(adresse).post(koerper.toString().toRequestBody(JSON_TYP)).build()
        val json = JSONObject(String(fuehreAus(anfrage, true, "Google"), Charsets.UTF_8))
        val audio = json.optString("audioContent")
        if (audio.isBlank()) throw ZusatzStimmenFehler("Google hat kein Audio geliefert.")
        return schreibeDatei(Base64.decode(audio, Base64.DEFAULT), "mp3")
    }

    private suspend fun fuehreAus(anfrage: Request, warteBei429: Boolean, wer: String): ByteArray {
        var versuch = 0
        while (true) {
            val (koerper, code) = client.newCall(anfrage).warte().use { it.body?.bytes() to it.code }
            when {
                code in 200..299 -> return koerper ?: ByteArray(0)
                code == 429 && warteBei429 && versuch < WARTEZEITEN_MS.size -> {
                    delay(WARTEZEITEN_MS[versuch])
                    versuch++
                }
                code == 401 || code == 403 -> throw ZusatzStimmenFehler("$wer hat den Schlüssel abgelehnt (Fehler $code).")
                code == 429 -> throw ZusatzStimmenFehler("Das Kontingent der $wer-Stimme ist erschöpft (Fehler 429).")
                else -> throw ZusatzStimmenFehler("$wer antwortete mit Fehler $code: ${koerper?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
    }

    /** Rohes PCM (16 Bit, mono) bekommt einen WAV-Kopf, damit der MediaPlayer es abspielt. */
    private fun mitWavKopf(pcm: ByteArray, rate: Int): ByteArray {
        val kopf = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size)
        return kopf.array() + pcm
    }

    private fun wavRate(wav: ByteArray): Int =
        java.nio.ByteBuffer.wrap(wav, 24, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.takeIf { it > 0 } ?: 24_000

    private fun ssmlEscape(text: String): String = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun schreibeDatei(bytes: ByteArray, endung: String): File =
        File(appContext.cacheDir, "zusatz_tts_${UUID.randomUUID()}.$endung").apply { writeBytes(bytes) }

    private suspend fun Call.warte(): Response = suspendCancellableCoroutine { fortsetzung ->
        fortsetzung.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (fortsetzung.isActive) fortsetzung.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (fortsetzung.isActive) fortsetzung.resume(response) else response.close()
            }
        })
    }

    private companion object {
        const val TAG = "ZusatzStimmen"
        const val GOOGLE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
        const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val GEMINI_FLASH_MODELL = "gemini-3.8-flash-tts"
        const val GEMINI_LITE_MODELL = "gemini-3.8-flash-lite-tts"

        /** So lange bleibt der kostenlose Gemini-Schlüssel nach einem Fehler übersprungen, wenn es einen Ausweichweg gibt. */
        const val FREI_PAUSE_MS = 10 * 60_000L

        /** Gilt für alle Instanzen: Sonst probierte jeder neue Player den gescheiterten Schlüssel erneut. */
        @Volatile var freiGesperrtBis = 0L

        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        val SSML_TYP = "application/ssml+xml".toMediaType()
        val WARTEZEITEN_MS = longArrayOf(1_000L, 3_000L, 8_000L)

        /** Reicht zusammen über das Minutenfenster von F0. */
        val AZURE_WARTEZEITEN_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
    }
}
