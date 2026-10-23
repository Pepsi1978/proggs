package de.frank.ziele.tts

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
import kotlinx.coroutines.ensureActive
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
        /** Danach liest noch eine Rückfallstimme: Bei 429 nicht warten, sondern sofort scheitern. */
        mitAusweg: Boolean = false,
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
                    // Gemini kennt kein Sprechtempo: Der fertige Ton wird hier gestreckt (ein Chirp-Rückfall als MP3 bleibt, wie er ist).
                    TtsProvider.GEMINI_FLASH -> mitTempo(gemini(text, GEMINI_FLASH_MODELL, voiceId, speechRate, zugang, mitAusweg), speechRate) to "wav"
                    TtsProvider.GEMINI_LITE -> mitTempo(gemini(text, GEMINI_LITE_MODELL, voiceId, speechRate, zugang, mitAusweg), speechRate) to "wav"
                    TtsProvider.AZURE -> azure(text, voiceId, speechRate, zugang, mitAusweg) to "mp3"
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

    private suspend fun gemini(text: String, modell: String, stimmeId: String, tempo: Float, z: Zugang, mitAusweg: Boolean): ByteArray {
        // Kurze Texte (etwa die Probe nach jeder Tempoänderung) kommen ungestreckt aus dem Vorrat, statt Gemini erneut zu fragen
        // und ins Ratenlimit (429) zu laufen — auch wenn der kostenlose Schlüssel gerade pausiert. Das Tempo entsteht danach.
        synchronized(tonVorrat) { tonVorrat[vorratSchluessel(modell, stimmeId, text)] }?.let { return it }
        val wahl = z.schluesselWahl
        val frei = z.geminiKeyFrei.filterNot(Char::isWhitespace)
        val bezahlt = z.geminiKeyBezahlt.filterNot(Char::isWhitespace)
        val chirp = z.chirpRueckfall
        var fehlerFrei: Exception? = null
        if (wahl != GEMINI_TTS_NUR_BEZAHLT) {
            val gesperrt = System.currentTimeMillis() < freiGesperrtBis
            if (frei.isNotBlank() && !gesperrt) {
                // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
                val ausweg = mitAusweg || chirp || (wahl == GEMINI_TTS_FREI_DANN_BEZAHLT && bezahlt.isNotBlank())
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
        return geminiAnfrage(text, modell, stimmeId, bezahlt, "bezahlt", warteBei429 = !mitAusweg)
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
        // Aus einer WAV-Antwort nur den Ton behalten: Gemini hängt einen C2PA-Nachweis an, gestreckt klänge er wie Rauschen.
        val wav = if (roh.size > 44 && String(roh, 0, 4, Charsets.US_ASCII) == "RIFF") pcmAusWav(roh)?.let { mitWavKopf(it.first, it.second) } ?: roh
            else mitWavKopf(roh, rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val sekunden = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (text.length >= 60 && sekunden < text.length / 25.0) {
            throw Fehler("Gemini-Audio zu kurz (${"%.1f".format(sekunden)} s für ${text.length} Zeichen).")
        }
        Log.i(TAG, "Gemini-Ansage erzeugt: modell=$modell schluessel=$art zeichen=${text.length}")
        if (text.length <= VORRAT_ZEICHEN) synchronized(tonVorrat) { tonVorrat[vorratSchluessel(modell, stimmeId, text)] = wav }
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

    /**
     * Liest aus einer WAV-Antwort Ton (PCM) und Abtastrate, Chunk für Chunk (Port aus Jarvis). Gemini hängt hinter den
     * data-Chunk einen C2PA-Herkunftsnachweis an. null, wenn es kein PCM 16 Bit mono ist.
     */
    private fun pcmAusWav(wav: ByteArray): Pair<ByteArray, Int>? {
        val b = java.nio.ByteBuffer.wrap(wav).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        fun kennung(ab: Int) = String(wav, ab, 4, Charsets.US_ASCII)
        if (wav.size < 12 || kennung(8) != "WAVE") return null
        var rate = 0
        var pcmMono16 = false
        var pos = 12
        while (pos + 8 <= wav.size) {
            val groesse = b.getInt(pos + 4).toLong() and 0xFFFFFFFFL
            val inhalt = pos + 8
            when (kennung(pos)) {
                "fmt " -> if (groesse >= 16 && inhalt + 16 <= wav.size) {
                    pcmMono16 = b.getShort(inhalt).toInt() == 1 && b.getShort(inhalt + 2).toInt() == 1 && b.getShort(inhalt + 14).toInt() == 16
                    rate = b.getInt(inhalt + 4)
                }
                "data" -> {
                    if (!pcmMono16 || rate <= 0) return null
                    // Platzhalter-Länge (0 oder 0xFFFFFFFF bei gestreamten WAVs): bis zum C2PA-Anhang oder zum Ende.
                    val ende = if (groesse in 1..(wav.size - inhalt).toLong()) inhalt + groesse.toInt()
                    else (inhalt until wav.size - 3 step 2).firstOrNull { kennung(it) == "C2PA" } ?: wav.size
                    val laenge = (ende - inhalt) and 1.inv()
                    return wav.copyOfRange(inhalt, inhalt + laenge) to rate
                }
            }
            if (groesse > wav.size) return null
            pos = inhalt + groesse.toInt() + (groesse.toInt() and 1)
        }
        return null
    }

    /** true bei einem schlichten WAV-Kopf (44 Byte, PCM 16 Bit mono); nur den kann [mitTempo] strecken. */
    private fun schlichtesWav(wav: ByteArray): Boolean {
        if (wav.size < 44) return false
        val b = java.nio.ByteBuffer.wrap(wav).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        fun kennung(ab: Int) = String(wav, ab, 4, Charsets.US_ASCII)
        return kennung(0) == "RIFF" && kennung(8) == "WAVE" && kennung(12) == "fmt " && b.getShort(20).toInt() == 1 &&
            b.getShort(22).toInt() == 1 && b.getShort(34).toInt() == 16 && kennung(36) == "data"
    }

    /**
     * Streckt oder staucht den fertigen Gemini-Ton (16 Bit mono) per WSOLA, die Tonhöhe bleibt gleich (Port aus Jarvis,
     * speech/Synthese.kt). Anfang und Ende bleiben ungeblendet erhalten; ein unbekanntes Format bleibt ungestreckt.
     */
    private suspend fun mitTempo(wav: ByteArray, tempo: Float): ByteArray {
        if (kotlin.math.abs(tempo - 1f) < 0.02f || wav.size < 44 + 4_000 || !schlichtesWav(wav)) return wav
        val rate = wavRate(wav)
        val ein = ShortArray((wav.size - 44) / 2)
        java.nio.ByteBuffer.wrap(wav, 44, ein.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(ein)
        val w = rate * 30 / 1000
        val hs = w / 2
        val such = rate * 10 / 1000
        // Hann-Fenster: Bei halber Überlappung ergänzen sich die Fenster genau zu 1.
        val fenster = FloatArray(w) { (0.5 - 0.5 * kotlin.math.cos(2 * Math.PI * it / w)).toFloat() }
        val aus = FloatArray((maxOf(ein.size.toFloat(), ein.size / tempo)).toInt() + 4 * w + 2 * such)
        var vorher = 0
        var k = 0
        while (true) {
            val ziel = (k * hs * tempo).roundToInt()
            val o = k * hs
            if (ziel + w + such >= ein.size || o + w > aus.size) break
            if (k % 200 == 199) kotlinx.coroutines.currentCoroutineContext().ensureActive()
            var pos = ziel
            val natuerlich = vorher + hs
            // Den Ausschnitt um das Ziel suchen, der am besten an den vorigen anschließt, nach Energie normiert.
            if (k > 0 && natuerlich + w < ein.size) {
                var bester = Double.NEGATIVE_INFINITY
                var d = -such
                while (d <= such) {
                    val c = ziel + d
                    if (c >= 0) {
                        var summe = 0.0
                        var energie = 0.0
                        var i = 0
                        while (i < w) { val x = ein[c + i].toDouble(); summe += ein[natuerlich + i] * x; energie += x * x; i += 4 }
                        val guete = summe / kotlin.math.sqrt(energie + 1.0)
                        if (guete > bester) { bester = guete; pos = c }
                    }
                    d += 2
                }
            }
            // Der erste Ausschnitt hat keinen Vorgänger: seine erste Hälfte ungeblendet.
            for (i in 0 until w) aus[o + i] += ein[pos + i] * (if (k == 0 && i < hs) 1f else fenster[i])
            vorher = pos
            k++
        }
        if (k == 0) return wav
        // Ab der Mitte des letzten Ausschnitts den restlichen Ton unverändert anhängen: So fehlt am Ende nichts.
        val start = (k - 1) * hs + hs
        val rest = ein.size - (vorher + hs)
        for (i in 0 until rest) aus[start + i] = ein[vorher + hs + i].toFloat()
        val laenge = start + rest
        val pcm = java.nio.ByteBuffer.allocate(laenge * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until laenge) pcm.putShort(aus[i].roundToInt().coerceIn(-32768, 32767).toShort())
        return mitWavKopf(pcm.array(), rate)
    }

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
    private suspend fun azure(text: String, stimmeId: String, tempo: Float, z: Zugang, mitAusweg: Boolean): ByteArray {
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
            .addHeader("User-Agent", "Ziele")
            .post(ssml.toRequestBody(SSML_TYP))
            .build()
        val bytes = azureAnfrage(anfrage, hd, warteBei429 = !mitAusweg)
        if (bytes.isEmpty()) throw Fehler("Azure hat kein Audio geliefert.")
        Log.i(TAG, "Azure-Ansage erzeugt: stimme=$stimme zeichen=${text.length}")
        return bytes
    }

    /**
     * F0 erlaubt nur rund 20 Anfragen je Minute: Ein 429 ist meist dieses Ratenlimit, nicht das Monatskontingent —
     * darum nach Retry-After (höchstens eine Minute) warten.
     */
    private suspend fun azureAnfrage(anfrage: Request, hd: Boolean, warteBei429: Boolean): ByteArray {
        var versuch = 0
        while (true) {
            val (koerper, code, warteSek) = client.newCall(anfrage).execute().use {
                Triple(if (it.isSuccessful) it.body?.bytes() else it.body?.string()?.toByteArray(), it.code, it.header("Retry-After")?.trim()?.toLongOrNull())
            }
            when {
                code in 200..299 -> return koerper ?: ByteArray(0)
                code == 429 && warteBei429 && versuch < AZURE_WARTEZEITEN_MS.size -> {
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
        // Die Sperre gilt je Modell (Pfad) und Schlüssel: Ein anderer Schlüssel gehört zu einem anderen Projekt mit eigener Quote.
        val ziel = anfrage.url.encodedPath + "|" + (anfrage.header("x-goog-api-key") ?: anfrage.url.queryParameter("key")).hashCode()
        var versuch = 0
        while (true) {
            // Solange Google dieses Ziel gesperrt hat, verlängert jede weitere Anfrage das Fenster nur: erst abwarten.
            val sperre = (gesperrtBis[ziel] ?: 0L) - System.currentTimeMillis()
            if (sperre > 0) {
                if (!warteBei429 || sperre > RETRY_HOECHSTENS_MS) throw Abbruch(kontingentText(sperre))
                delay(sperre)
            }
            val (koerper, code) = client.newCall(anfrage).execute().use { it.body?.string().orEmpty() to it.code }
            when {
                code in 200..299 -> return koerper
                code == 429 -> {
                    // Google nennt im 429 Quote und Wartezeit („retryDelay": "17s"); die festen 1/3/8 s lagen oft noch im Fenster.
                    val vorgabeMs = RETRY_DELAY.find(koerper)?.groupValues?.get(1)?.toDoubleOrNull()?.let { (it * 1000).toLong() }
                    if (vorgabeMs != null) gesperrtBis[ziel] = System.currentTimeMillis() + vorgabeMs
                    val ms = maxOf(WARTEZEITEN_MS.getOrElse(versuch) { 0L }, (vorgabeMs ?: 0L) + 500L)
                    Log.w(TAG, "Rate-Limit von Google: quote=${QUOTA_ID.find(koerper)?.groupValues?.get(1) ?: "unbekannt"} retryDelayMs=${vorgabeMs ?: -1} warte=$ms versuch=$versuch")
                    // Verlangt Google länger als die Höchstwartezeit (etwa ein Tageskontingent), sofort aufgeben.
                    if (!warteBei429 || versuch >= WARTEZEITEN_MS.size || ms > RETRY_HOECHSTENS_MS) throw Abbruch(kontingentText(vorgabeMs ?: 0L))
                    delay(ms)
                    versuch++
                }
                code == 401 || code == 403 -> throw Abbruch(
                    "Vorlesen fehlgeschlagen: Der Schlüssel wurde abgelehnt (Fehler $code). Prüf ihn in den Einstellungen.",
                )
                else -> throw Fehler("Die Stimme antwortete mit Fehler $code: ${koerper.take(200)}")
            }
        }
    }

    private fun kontingentText(restMs: Long): String =
        "Vorlesen fehlgeschlagen: Google lässt die Stimme gerade nicht zu (Fehler 429, Ratenlimit oder Kontingent)" +
            (if (restMs >= 1_000L) ", wieder in etwa ${(restMs + 999) / 1000} s." else ".")

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

        /** Längste Wartezeit je Versuch, die ein 429 mit retryDelay erzwingen darf; länger = sofort aufgeben. */
        private const val RETRY_HOECHSTENS_MS = 65_000L
        private val RETRY_DELAY = Regex("\"retryDelay\"\\s*:\\s*\"(\\d+(?:\\.\\d+)?)s\"")
        private val QUOTA_ID = Regex("\"quotaId\"\\s*:\\s*\"([^\"]+)\"")

        /** Bis wann Google ein Ziel (Modell + Schlüssel) nach einem 429 gesperrt hat; gilt für alle Instanzen. */
        private val gesperrtBis = java.util.concurrent.ConcurrentHashMap<String, Long>()

        /** Bis zu dieser Länge bleibt der ungestreckte Gemini-Ton im Vorrat (Proben, kurze Ansagen); rund 8 s Ton. */
        private const val VORRAT_ZEICHEN = 120

        /** Ungestreckter Gemini-Ton je Modell, Stimme und Text; die ältesten fallen nach 8 Einträgen heraus. */
        private val tonVorrat = object : LinkedHashMap<String, ByteArray>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>) = size > 8
        }

        private fun vorratSchluessel(modell: String, stimmeId: String, text: String) = "$modell|$stimmeId|$text"

        /** Reicht zusammen über das Minutenfenster von F0. */
        private val AZURE_WARTEZEITEN_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)
    }
}
