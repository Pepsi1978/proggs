package de.frank.aufgaben.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini 3.8 Flash / Flash-Lite TTS und Azure Neural (Port aus Jarvis, speech/Synthese.kt).
 *
 * Erst wird der ganze Text in eine Datei gesprochen, dann spielt der MediaPlayer sie ab — wie beim
 * Google-Player. Gemini nimmt erst den kostenlosen, dann den bezahlten Schlüssel; ist der Chirp-Rückfall
 * an, liest nach einem Fehler des kostenlosen Schlüssels Google Chirp 3 HD mit derselben Stimme weiter.
 */
class NeueStimmenPlayer(context: Context) {
    /** Schlüssel und Wahl aus den Einstellungen, als Schnappschuss für genau eine Ansage. */
    data class Zugang(
        val geminiKeyFrei: String,
        val geminiKeyBezahlt: String,
        val schluesselWahl: String,
        val chirpRueckfall: Boolean,
        val googleKey: String,
        val azureKey: String,
        val azureRegion: String,
        val erzwingeDeutsch: Boolean,
    )

    private class Abbruch(message: String) : Exception(message)
    private class Fehler(message: String) : Exception(message)

    private val appContext = context.applicationContext
    private val lock = Any()
    private val generation = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    private var activeJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var activeFile: File? = null

    fun speak(
        text: String,
        provider: TtsProvider,
        voiceId: String,
        speechRate: Float,
        zugang: Zugang,
        onPlaybackStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
        /** Nur erzeugen: fertiges Audio abholen, ohne es jetzt abzuspielen (vorbereitete Ansagen). */
        onAudioReady: ((File) -> Unit)? = null,
    ) {
        stop()
        val requestGeneration = generation.incrementAndGet()
        val started = AtomicBoolean(false)
        val terminal = AtomicBoolean(false)
        fun finish(callback: () -> Unit) {
            if (requestGeneration != generation.get() || !terminal.compareAndSet(false, true)) return
            cleanupCurrent()
            scope.launch { if (requestGeneration == generation.get()) callback() }
        }
        val job = scope.launch(Dispatchers.IO) {
            try {
                val (bytes, endung) = when (provider) {
                    TtsProvider.GEMINI_FLASH -> gemini(text, GEMINI_FLASH_MODELL, voiceId, speechRate, zugang) to "wav"
                    TtsProvider.GEMINI_LITE -> gemini(text, GEMINI_LITE_MODELL, voiceId, speechRate, zugang) to "wav"
                    TtsProvider.AZURE -> azure(text, voiceId, speechRate, zugang) to "mp3"
                    else -> throw Abbruch("Dieser Anbieter wird hier nicht unterstützt.")
                }
                if (requestGeneration != generation.get()) return@launch
                // Rückfall auf Chirp liefert MP3 — die Endung ist dem MediaPlayer egal, er erkennt den Inhalt.
                val file = File(appContext.cacheDir, "neue_stimme_${requestGeneration}_${UUID.randomUUID()}.$endung")
                file.writeBytes(bytes)
                synchronized(lock) {
                    if (requestGeneration != generation.get()) {
                        file.delete()
                        return@launch
                    }
                    activeFile = file
                }
                if (onAudioReady != null) {
                    // Der Aufrufer kopiert die Datei, danach räumt finish() sie weg.
                    try {
                        onAudioReady(file)
                        finish(onComplete)
                    } catch (error: Exception) {
                        finish { onError(error) }
                    }
                    return@launch
                }
                scope.launch {
                    if (requestGeneration != generation.get() || terminal.get()) return@launch
                    val player = MediaPlayer()
                    try {
                        synchronized(lock) {
                            if (requestGeneration != generation.get()) {
                                player.release()
                                return@launch
                            }
                            mediaPlayer = player
                            activeJob = null
                        }
                        player.setAudioAttributes(SpeechLoudness.attributes)
                        player.setDataSource(file.absolutePath)
                        player.setOnCompletionListener { finish(onComplete) }
                        player.setOnErrorListener { _, what, extra ->
                            finish { onError(IllegalStateException("Wiedergabe fehlgeschlagen ($what/$extra).")) }
                            true
                        }
                        player.prepare()
                        SpeechLoudness.boost(player)
                        player.start()
                        if (requestGeneration == generation.get() && !terminal.get() && started.compareAndSet(false, true)) {
                            onPlaybackStart()
                        }
                    } catch (error: Exception) {
                        finish { onError(error) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Neue Stimme fehlgeschlagen: ${error.message}")
                finish { onError(error) }
            }
        }
        synchronized(lock) {
            if (requestGeneration == generation.get() && !terminal.get()) activeJob = job else job.cancel()
        }
    }

    fun stop() {
        generation.incrementAndGet()
        cleanupCurrent()
    }

    fun pause(): Boolean = synchronized(lock) {
        val player = mediaPlayer ?: return@synchronized false
        runCatching { if (player.isPlaying) player.pause(); true }.getOrDefault(false)
    }

    fun resume(): Boolean = synchronized(lock) {
        val player = mediaPlayer ?: return@synchronized false
        runCatching { player.start(); true }.getOrDefault(false)
    }

    fun shutdown() {
        stop()
        scope.cancel()
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    private fun cleanupCurrent() {
        val job: Job?
        val player: MediaPlayer?
        val file: File?
        synchronized(lock) {
            job = activeJob
            player = mediaPlayer
            file = activeFile
            activeJob = null
            mediaPlayer = null
            activeFile = null
        }
        job?.cancel()
        runCatching { player?.stop() }
        SpeechLoudness.release(player)
        player?.release()
        file?.delete()
    }

    // ---- Gemini 3.8 Flash / Flash-Lite TTS ----

    private suspend fun gemini(text: String, modell: String, stimmeId: String, tempo: Float, z: Zugang): ByteArray {
        val wahl = z.schluesselWahl
        val frei = z.geminiKeyFrei.filterNot(Char::isWhitespace)
        val bezahlt = z.geminiKeyBezahlt.filterNot(Char::isWhitespace)
        val chirp = z.chirpRueckfall
        var fehlerFrei: Exception? = null
        if (wahl != GEMINI_TTS_NUR_BEZAHLT) {
            val gesperrt = System.currentTimeMillis() < freiGesperrtBis
            if (frei.isNotBlank() && !gesperrt) {
                // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
                val ausweg = chirp || (wahl == GEMINI_TTS_FREI_DANN_BEZAHLT && bezahlt.isNotBlank())
                try {
                    return geminiAnfrage(text, modell, stimmeId, frei, "kostenlos", warteBei429 = !ausweg)
                } catch (fehler: Exception) {
                    if (fehler !is Abbruch && fehler !is Fehler) throw fehler
                    fehlerFrei = fehler
                    if (ausweg) freiGesperrtBis = System.currentTimeMillis() + FREI_PAUSE_MS
                    Log.w(TAG, "Gemini-Stimme: kostenloser Schlüssel gescheitert ($modell): ${fehler.message?.take(120)}")
                }
            }
            if (chirp) {
                Log.i(TAG, "Gemini-Stimme: Rückfall auf Chirp 3 HD (${TtsCatalog.geminiName(stimmeId)})")
                return chirp(text, TtsCatalog.chirpZu(stimmeId), tempo, z.googleKey)
            }
            if (wahl == GEMINI_TTS_NUR_FREI) {
                throw fehlerFrei ?: Abbruch(
                    "Vorlesen fehlgeschlagen: Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.",
                )
            }
        }
        if (bezahlt.isBlank()) {
            throw fehlerFrei ?: Abbruch(
                "Vorlesen fehlgeschlagen: Es ist kein Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.",
            )
        }
        return geminiAnfrage(text, modell, stimmeId, bezahlt, "bezahlt", warteBei429 = true)
    }

    private suspend fun geminiAnfrage(text: String, modell: String, stimmeId: String, schluessel: String, art: String, warteBei429: Boolean): ByteArray {
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
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", TtsCatalog.geminiName(stimmeId))),
                        ),
                    ),
            )
        val anfrage = Request.Builder()
            .url("$GEMINI_URL$modell:generateContent")
            .addHeader("x-goog-api-key", schluessel)
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val kandidat = JSONObject(fuehreAus(anfrage, warteBei429)).optJSONArray("candidates")?.optJSONObject(0)
        val ende = kandidat?.optString("finishReason").orEmpty()
        val teil = kandidat?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optJSONObject("inlineData")
        val daten = teil?.optString("data").orEmpty()
        if (daten.isBlank()) throw Fehler("Gemini hat kein Audio geliefert ($ende).")
        if (ende.isNotBlank() && ende != "STOP") throw Fehler("Gemini hat die Stimme abgebrochen ($ende).")
        val roh = Base64.decode(daten, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(teil?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val wav = if (roh.size > 44 && String(roh, 0, 4, Charsets.US_ASCII) == "RIFF") roh else mitWavKopf(roh, rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val sekunden = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (text.length >= 60 && sekunden < text.length / 25.0) {
            throw Fehler("Gemini-Audio zu kurz (${"%.1f".format(sekunden)} s für ${text.length} Zeichen).")
        }
        Log.i(TAG, "Gemini-Ansage erzeugt: modell=$modell schluessel=$art zeichen=${text.length}")
        return wav
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

    /** Google Chirp 3 HD — der Rückfall für die Gemini-Stimmen, mit dem vorhandenen Google-TTS-Schlüssel. */
    private suspend fun chirp(text: String, stimme: String, tempo: Float, googleKey: String): ByteArray {
        val schluessel = googleKey.trim()
        if (schluessel.isBlank()) {
            throw Abbruch("Vorlesen fehlgeschlagen: Für den Rückfall auf Chirp 3 HD fehlt der Google-Schlüssel. Trag ihn in den Einstellungen ein.")
        }
        val koerper = JSONObject()
            .put("input", JSONObject().put("text", text))
            .put("voice", JSONObject().put("languageCode", "de-DE").put("name", stimme))
            .put("audioConfig", JSONObject().put("audioEncoding", "MP3").put("speakingRate", tempo.coerceIn(0.25f, 4f).toDouble()))
        val adresse = GOOGLE_URL.toHttpUrl().newBuilder().addQueryParameter("key", schluessel).build()
        val anfrage = Request.Builder().url(adresse).post(koerper.toString().toRequestBody(JSON_TYP)).build()
        val audio = JSONObject(fuehreAus(anfrage)).optString("audioContent")
        if (audio.isBlank()) throw Fehler("Google hat kein Audio geliefert.")
        return Base64.decode(audio, Base64.DEFAULT)
    }

    // ---- Azure Neural ----

    /** HD-Stimmen (DragonHD) bekommen weder Tempo noch <lang>-Rahmen: Sie bestimmen Betonung und Tempo selbst. */
    private suspend fun azure(text: String, stimmeId: String, tempo: Float, z: Zugang): ByteArray {
        val schluessel = z.azureKey.filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw Abbruch("Vorlesen fehlgeschlagen: Es ist kein Azure-Schlüssel hinterlegt. Trag ihn in den Einstellungen ein.")
        }
        val region = TtsCatalog.azureRegion(z.azureRegion) ?: throw Abbruch(
            "Vorlesen fehlgeschlagen: Die Azure-Region „${z.azureRegion.take(40)}“ verstehe ich nicht. Trag den Regionsnamen ein, etwa westeurope.",
        )
        val stimme = TtsCatalog.azureName(stimmeId)
        val hd = TtsCatalog.istAzureHd(stimmeId)
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = xmlEscape(text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " "))
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die übrigen sprechen ohnehin nur Deutsch.
        val inhalt = if (z.erzwingeDeutsch && stimme.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val prozent = ((tempo.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
        val mitTempo = if (hd || prozent == 0) inhalt else "<prosody rate='${if (prozent > 0) "+" else ""}$prozent%'>$inhalt</prosody>"
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$stimme'>$mitTempo</voice></speak>"
        val anfrage = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", schluessel)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .addHeader("User-Agent", "Aufgaben")
            .post(ssml.toRequestBody(SSML_TYP))
            .build()
        val bytes = azureAnfrage(anfrage, hd)
        if (bytes.isEmpty()) throw Fehler("Azure hat kein Audio geliefert.")
        Log.i(TAG, "Azure-Ansage erzeugt: stimme=$stimme zeichen=${text.length}")
        return bytes
    }

    /**
     * F0 erlaubt nur rund 20 Anfragen je Minute: Ein 429 ist meist dieses Ratenlimit, nicht das Monatskontingent —
     * darum nach Retry-After (höchstens eine Minute) warten.
     */
    private suspend fun azureAnfrage(anfrage: Request, hd: Boolean): ByteArray {
        var versuch = 0
        while (true) {
            val (koerper, code, warteSek) = client.newCall(anfrage).execute().use {
                Triple(if (it.isSuccessful) it.body?.bytes() else it.body?.string()?.toByteArray(), it.code, it.header("Retry-After")?.trim()?.toLongOrNull())
            }
            when {
                code in 200..299 -> return koerper ?: ByteArray(0)
                code == 429 && versuch < AZURE_WARTEZEITEN_MS.size -> {
                    val ms = warteSek?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: AZURE_WARTEZEITEN_MS[versuch]
                    Log.w(TAG, "Azure-Ratenlimit, warte $ms ms (Versuch $versuch)")
                    delay(ms)
                    versuch++
                }
                code == 401 || code == 403 -> throw Abbruch(
                    "Vorlesen fehlgeschlagen: Azure hat Schlüssel oder Region abgelehnt (Fehler $code). Prüf beides in den Einstellungen.",
                )
                code == 429 -> throw Abbruch(
                    "Vorlesen fehlgeschlagen: Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.",
                )
                code == 400 && hd -> throw Abbruch(
                    "Vorlesen fehlgeschlagen: Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.",
                )
                else -> throw Fehler("Azure antwortete mit Fehler $code: ${koerper?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
        }
    }

    private suspend fun fuehreAus(anfrage: Request, warteBei429: Boolean = true): String {
        var versuch = 0
        while (true) {
            val (koerper, code) = client.newCall(anfrage).execute().use { it.body?.string().orEmpty() to it.code }
            when {
                code in 200..299 -> return koerper
                code == 429 && warteBei429 && versuch < WARTEZEITEN_MS.size -> {
                    delay(WARTEZEITEN_MS[versuch])
                    versuch++
                }
                code == 401 || code == 403 -> throw Abbruch(
                    "Vorlesen fehlgeschlagen: Der Schlüssel wurde abgelehnt (Fehler $code). Prüf ihn in den Einstellungen.",
                )
                code == 429 -> throw Abbruch("Vorlesen fehlgeschlagen: Das Kontingent der Stimme ist erschöpft (Fehler 429).")
                else -> throw Fehler("Die Stimme antwortete mit Fehler $code: ${koerper.take(200)}")
            }
        }
    }

    private fun xmlEscape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    companion object {
        private const val TAG = "NeueStimmen"
        private const val GOOGLE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        private const val GEMINI_FLASH_MODELL = "gemini-3.8-flash-tts"
        private const val GEMINI_LITE_MODELL = "gemini-3.8-flash-lite-tts"

        const val GEMINI_TTS_FREI_DANN_BEZAHLT = "frei_dann_bezahlt"
        const val GEMINI_TTS_NUR_FREI = "nur_frei"
        const val GEMINI_TTS_NUR_BEZAHLT = "nur_bezahlt"
        val GEMINI_TTS_SCHLUESSEL_WAHLEN = listOf(
            GEMINI_TTS_FREI_DANN_BEZAHLT to "Erst kostenlos, dann bezahlt",
            GEMINI_TTS_NUR_FREI to "Nur kostenlos",
            GEMINI_TTS_NUR_BEZAHLT to "Nur bezahlt",
        )

        /** So lange bleibt der kostenlose Gemini-Schlüssel nach einem Fehler übersprungen, wenn es einen Ausweichweg gibt. */
        private const val FREI_PAUSE_MS = 10 * 60_000L

        /** Gilt für alle Instanzen: Sonst probierte jeder neue Player den gescheiterten Schlüssel erneut. */
        @Volatile private var freiGesperrtBis = 0L

        private val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        private val SSML_TYP = "application/ssml+xml".toMediaType()
        private val WARTEZEITEN_MS = longArrayOf(1_000L, 3_000L, 8_000L)

        /** Reicht zusammen über das Minutenfenster von F0. */
        private val AZURE_WARTEZEITEN_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
    }
}
