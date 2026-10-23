package de.frank.jarvis.speech

import android.content.Context
import android.util.Base64
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.tts.EdgeTtsPlayer
import de.frank.jarvis.tts.RueckfallStufe
import de.frank.jarvis.tts.TtsCatalog
import de.frank.jarvis.tts.TtsProvider
import de.frank.jarvis.tts.deutschesSsml
import de.frank.jarvis.tts.chirpAussprachen
import de.frank.jarvis.tts.ssmlEscape
import kotlin.math.roundToInt
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
import kotlinx.coroutines.ensureActive
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
import org.json.JSONArray
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
        TtsProvider.GOOGLE_CLOUD.id, TtsProvider.GEMINI_FLASH.id, TtsProvider.GEMINI_LITE.id,
        TtsProvider.QWEN_CLONE.id, TtsProvider.QWEN.id, TtsProvider.EDGE.id, TtsProvider.AZURE.id -> true
        else -> false
    }

    /**
     * Prüft den Google-Schlüssel unabhängig von der gerade gewählten Stimme. Ohne diesen Weg
     * würde die Prüfung über [synthetisiere] laufen und bei einer Edge- oder Alibaba-Stimme
     * mit "kann nicht vorab erzeugt werden" abbrechen, obwohl der Schlüssel in Ordnung ist.
     */
    suspend fun pruefeGoogle(text: String = "Probe."): File = gleichzeitig.withPermit { google(text, null, settings.ttsTempo(TtsProvider.GOOGLE_CLOUD.id)) }

    /** Anbieter der [wahl], sonst der in Jarvis eingestellte. */
    private fun anbieter(wahl: StimmWahl?): String =
        wahl?.stimme?.let { TtsCatalog.anbieterZu(it)?.id } ?: settings.ttsProvider

    /**
     * Erzeugt den Absatz mit der gewählten Stimme. Scheitert sie, geht es die Rückfallkette
     * ([SecureSettings.ttsRueckfallKette]) der Reihe nach durch, jede Stufe mit ihrer eigenen eingestellten Stimme.
     * Netzfehler laufen unverändert durch — die wiederholt der Vorleser selbst. Scheitern alle, zählt der Fehler der Hauptstimme.
     */
    suspend fun synthetisiere(text: String, wahl: StimmWahl? = null): File = gleichzeitig.withPermit {
        val sonder = wahl?.stimme?.takeIf { TtsCatalog.anbieterZu(it) != null }
        // Jede Stimme mit ihrem eigenen Tempo, auch als Rückfallstufe; ein Sondertempo der Meldung gilt für alle.
        fun tempo(anbieter: String) = wahl?.tempo ?: settings.ttsTempo(anbieter)
        val haupt = anbieter(wahl)
        // Eine Stufe ohne eigenen Schlüssel ist dieselbe Stimme wie die Hauptstimme und brächte nichts; Stufen ohne Schlüssel laufen gar nicht erst.
        val kette = settings.ttsRueckfallKette.mapNotNull(TtsCatalog::rueckfallStufe)
            .filterNot { it.anbieter.id == haupt && it.schluessel == null }
            .filter(settings::rueckfallBereit)
        val hauptFehler = try {
            return@withPermit erzeuge(haupt, text, sonder, tempo(haupt), null, mitAusweg = kette.isNotEmpty())
        } catch (fehler: Exception) {
            if (!rueckfallFaehig(fehler)) throw fehler
            fehler
        }
        var zeitlimit: SyntheseZeitlimit? = null
        kette.forEachIndexed { i, stufe ->
            try {
                val datei = erzeuge(stufe.anbieter.id, text, null, tempo(stufe.anbieter.id), stufe.schluessel, mitAusweg = i < kette.lastIndex)
                Logbuch.sprache("Vorlese-Rückfall", "stufe=${stufe.id} haupt=$haupt grund=${hauptFehler.message?.take(80)}")
                return@withPermit datei
            } catch (fehler: Exception) {
                // Auch Netzfehler einer Stufe führen zur nächsten: Sonst wiederholte der Vorleser die ganze Kette samt Hauptstimme.
                if (!rueckfallFaehig(fehler) && fehler !is IOException) throw fehler
                if (fehler is SyntheseZeitlimit) zeitlimit = fehler
                Logbuch.netz("Vorlese-Rückfall: Stufe gescheitert", "stufe=${stufe.id} ${fehler.message?.take(120)}")
            }
        }
        // Hat eine Stufe schon ihre Frist abgewartet, wird der Teil übersprungen statt halbiert und die Kette erneut abgewartet.
        throw if (hauptFehler is SyntheseFehler) zeitlimit ?: hauptFehler else hauptFehler
    }

    private fun rueckfallFaehig(fehler: Exception) = fehler is SyntheseAbbruch || fehler is SyntheseFehler || fehler is SyntheseZeitlimit

    /** [schluessel] nur für Gemini-Stufen der Rückfallkette: nur der kostenlose bzw. nur der bezahlte Schlüssel. */
    private suspend fun erzeuge(anbieter: String, text: String, sonder: String?, tempo: Float, schluessel: String?, mitAusweg: Boolean): File =
        when (anbieter) {
            TtsProvider.GOOGLE_CLOUD.id -> google(text, sonder, tempo)
            TtsProvider.GEMINI_FLASH.id -> gemini(text, GEMINI_FLASH_MODELL, sonder ?: settings.geminiFlashTtsVoice, schluessel, mitAusweg, tempo)
            TtsProvider.GEMINI_LITE.id -> gemini(text, GEMINI_LITE_MODELL, sonder ?: settings.geminiLiteTtsVoice, schluessel, mitAusweg, tempo)
            TtsProvider.QWEN_CLONE.id -> qwen(text, geklont = true)
            TtsProvider.QWEN.id -> qwen(text, geklont = false, sonder)
            TtsProvider.EDGE.id -> edge(text, sonder, tempo)
            TtsProvider.AZURE.id -> azure(text, sonder ?: settings.azureTtsVoice, tempo)
            else -> throw SyntheseAbbruch("Diese Stimme kann nicht vorab erzeugt werden.")
        }

    /**
     * Azure AI Speech über REST: Schlüssel und Region der eigenen Speech-Ressource (F0 kostenlos oder S0).
     * HD-Stimmen (DragonHD) bekommen weder Tempo noch <lang>-Rahmen: Sie bestimmen Betonung und Tempo selbst.
     */
    private suspend fun azure(text: String, stimmeId: String, tempo: Float): File {
        val schluessel = settings.azureTtsKey.filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw SyntheseAbbruch("Vorlesen fehlgeschlagen: Es ist kein Azure-Schlüssel hinterlegt. Trag ihn in den Einstellungen ein.")
        }
        val region = TtsCatalog.azureRegion(settings.azureTtsRegion) ?: throw SyntheseAbbruch(
            "Vorlesen fehlgeschlagen: Die Azure-Region „${settings.azureTtsRegion.take(40)}“ verstehe ich nicht. Trag den Regionsnamen ein, etwa westeurope.",
        )
        val stimme = TtsCatalog.azureName(stimmeId)
        val hd = TtsCatalog.istAzureHd(stimmeId)
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = ssmlEscape(text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " "))
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die übrigen sprechen ohnehin nur Deutsch.
        val inhalt = if (settings.immerDeutschVorlesen && stimme.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val prozent = ((tempo.coerceIn(0.7f, 1.3f) - 1f) * 100).roundToInt()
        val mitTempo = if (hd || prozent == 0) inhalt else "<prosody rate='${if (prozent > 0) "+" else ""}$prozent%'>$inhalt</prosody>"
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$stimme'>$mitTempo</voice></speak>"
        val beginn = System.currentTimeMillis()
        val anfrage = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", schluessel)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .addHeader("User-Agent", "Jarvis")
            .post(ssml.toRequestBody(SSML_TYP))
            .build()
        val bytes = azureAnfrage(anfrage, hd)
        if (bytes.isEmpty()) throw SyntheseFehler("Azure hat kein Audio geliefert.")
        Logbuch.sprache("Azure-Absatz erzeugt", "stimme=$stimme zeichen=${text.length}", System.currentTimeMillis() - beginn)
        return schreibeDatei(bytes, "mp3")
    }

    /**
     * Schickt die Azure-Anfrage; die Antwort ist Ton statt Text. F0 erlaubt nur rund 20 Anfragen je Minute: Ein 429 ist
     * meist dieses Ratenlimit, nicht das Monatskontingent — darum nach Retry-After (höchstens eine Minute) warten.
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
                    val ms = warteSek?.let { (it * 1000).coerceIn(1_000L, 60_000L) } ?: AZURE_WARTEZEITEN_MS[versuch]
                    de.frank.jarvis.observability.IdeenLog.warn("Synthese", "azureAnfrage", "Rate-Limit, warte erneut", mapOf("ms" to ms, "versuch" to versuch))
                    kotlinx.coroutines.delay(ms)
                    versuch++
                }
                code == 401 || code == 403 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Azure hat Schlüssel oder Region abgelehnt (Fehler $code). Prüf beides in den Einstellungen.",
                )
                code == 429 -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.",
                )
                code == 400 && hd -> throw SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.",
                )
                else -> throw SyntheseFehler("Azure antwortete mit Fehler $code: ${koerper?.toString(Charsets.UTF_8)?.take(200).orEmpty()}")
            }
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
     * Gemini 3.8 Flash bzw. Flash-Lite TTS. Als Hauptstimme gilt die Reihenfolge laut [SecureSettings.geminiTtsSchluesselWahl]:
     * erst der kostenlose, dann der bezahlte Schlüssel. Als Stufe der Rückfallkette nimmt [nurSchluessel] genau einen davon.
     * [mitAusweg]: Danach kommt noch eine Rückfallstufe — dann wird bei 429 nicht gewartet und der kostenlose Schlüssel
     * dieses Modells nach einem Fehler eine Weile übersprungen. Das Kontingent gilt je Modell, darum die Sperre auch.
     */
    private suspend fun gemini(text: String, modell: String, stimmeId: String, nurSchluessel: String?, mitAusweg: Boolean, tempo: Float): File {
        val wahl = when (nurSchluessel) {
            RueckfallStufe.FREI -> SecureSettings.GEMINI_TTS_NUR_FREI
            RueckfallStufe.BEZAHLT -> SecureSettings.GEMINI_TTS_NUR_BEZAHLT
            else -> settings.geminiTtsSchluesselWahl
        }
        val frei = settings.geminiTtsKeyFrei.filterNot(Char::isWhitespace)
        val bezahlt = settings.geminiTtsKeyBezahlt.filterNot(Char::isWhitespace)
        var fehlerFrei: Exception? = null
        if (wahl != SecureSettings.GEMINI_TTS_NUR_BEZAHLT) {
            val gesperrt = System.currentTimeMillis() < (freiGesperrtBis[modell] ?: 0L)
            // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
            val ausweg = mitAusweg || (wahl == SecureSettings.GEMINI_TTS_FREI_DANN_BEZAHLT && bezahlt.isNotBlank())
            if (frei.isNotBlank() && !gesperrt) {
                try {
                    return geminiAnfrage(text, modell, stimmeId, frei, "kostenlos", warteBei429 = !ausweg, tempo)
                } catch (fehler: Exception) {
                    if (fehler !is SyntheseAbbruch && fehler !is SyntheseFehler) throw fehler
                    fehlerFrei = fehler
                    if (ausweg) freiGesperrtBis[modell] = System.currentTimeMillis() + FREI_PAUSE_MS
                    Logbuch.netz("Gemini-Stimme: kostenloser Schlüssel gescheitert", "modell=$modell ${fehler.message?.take(120)}")
                }
            } else if (frei.isNotBlank() && wahl == SecureSettings.GEMINI_TTS_NUR_FREI) {
                fehlerFrei = SyntheseAbbruch("Vorlesen fehlgeschlagen: Der kostenlose Gemini-Schlüssel pausiert nach einem Fehler.")
            }
            if (wahl == SecureSettings.GEMINI_TTS_NUR_FREI) {
                throw fehlerFrei ?: SyntheseAbbruch(
                    "Vorlesen fehlgeschlagen: Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.",
                )
            }
        }
        if (bezahlt.isBlank()) {
            throw fehlerFrei ?: SyntheseAbbruch(
                "Vorlesen fehlgeschlagen: Es ist kein Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.",
            )
        }
        return geminiAnfrage(text, modell, stimmeId, bezahlt, "bezahlt", warteBei429 = true, tempo)
    }

    private suspend fun geminiAnfrage(text: String, modell: String, stimmeId: String, schluessel: String, art: String, warteBei429: Boolean, tempo: Float): File {
        val beginn = System.currentTimeMillis()
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
        if (daten.isBlank()) throw SyntheseFehler("Gemini hat kein Audio geliefert ($ende).")
        if (ende.isNotBlank() && ende != "STOP") throw SyntheseFehler("Gemini hat die Stimme abgebrochen ($ende).")
        val roh = Base64.decode(daten, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(teil?.optString("mimeType").orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val riff = roh.size >= 12 && String(roh, 0, 4, Charsets.US_ASCII) == "RIFF"
        // Aus einer WAV-Antwort nur den Ton behalten (ohne C2PA-Anhang); ein unbekanntes Format bleibt, wie es kam.
        val ton = if (riff) pcmAusWav(roh) else roh to rate
        val wav = ton?.let { mitWavKopf(it.first, it.second) } ?: roh
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val sekunden = (ton?.first?.size ?: 0) / ((ton?.second ?: rate) * 2.0)
        if (ton != null && text.length >= 60 && sekunden < text.length / 25.0) {
            throw SyntheseFehler("Gemini-Audio zu kurz (${"%.1f".format(sekunden)} s für ${text.length} Zeichen).")
        }
        val fertig = mitTempo(wav, tempo)
        Logbuch.sprache("Gemini-Absatz erzeugt", "modell=$modell schluessel=$art zeichen=${text.length} tempo=$tempo", System.currentTimeMillis() - beginn)
        return schreibeDatei(fertig, "wav")
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
     * Gemini kennt kein Sprechtempo, und eine Anweisung im Text würde mitgesprochen: Darum wird der fertige Ton
     * (16 Bit mono) per WSOLA gestreckt oder gestaucht. Die Tonhöhe bleibt dabei gleich. Anfang und Ende bleiben
     * ungeblendet erhalten; ein unbekannter WAV-Kopf bleibt ungestreckt.
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
            // Den Ausschnitt um das Ziel suchen, der am besten an den vorigen anschließt (grob gerastert, reicht für Sprache);
            // nach Energie normiert, sonst gewönne ein lauter, schlecht passender Ausschnitt.
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
            // Der erste Ausschnitt hat keinen Vorgänger: seine erste Hälfte ungeblendet, sonst finge jeder Absatz mit einer Blende an.
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

    /** Rohes PCM (16 Bit, mono) bekommt einen WAV-Kopf, damit der MediaPlayer es abspielt. */
    private fun mitWavKopf(pcm: ByteArray, rate: Int): ByteArray {
        val kopf = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size)
        return kopf.array() + pcm
    }

    /**
     * Liest aus einer WAV-Antwort Ton (PCM) und Abtastrate, Chunk für Chunk. Gemini hängt hinter den data-Chunk
     * einen C2PA-Herkunftsnachweis (rund 6 KB) an; als Ton gelesen klänge er am Ende wie Fernsehrauschen.
     * null, wenn es kein PCM 16 Bit mono ist.
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

    private fun wavRate(wav: ByteArray): Int =
        java.nio.ByteBuffer.wrap(wav, 24, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.takeIf { it > 0 } ?: 24_000

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

    private suspend fun fuehreAus(anfrage: Request, warteBei429: Boolean = true): String {
        var versuch = 0
        while (true) {
            val antwort = client.newCall(anfrage).warte()
            val text = antwort.use { it.body?.string().orEmpty() to it.code }
            val (koerper, code) = text
            when {
                code in 200..299 -> return koerper
                // 429: exponentiell warten und erneut versuchen (Baustein D 4.4).
                code == 429 && warteBei429 && versuch < WARTEZEITEN_MS.size -> {
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
        const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        const val GEMINI_FLASH_MODELL = "gemini-3.8-flash-tts"
        const val GEMINI_LITE_MODELL = "gemini-3.8-flash-lite-tts"

        /** So lange bleibt der kostenlose Gemini-Schlüssel nach einem Fehler übersprungen, wenn es einen Ausweichweg gibt. */
        const val FREI_PAUSE_MS = 10 * 60_000L

        /** Gilt für alle Instanzen: Sonst probierte jeder neue Vorleser den gescheiterten Schlüssel erneut. */
        val freiGesperrtBis = java.util.concurrent.ConcurrentHashMap<String, Long>()
        const val QWEN_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
        /** Die fertigen Standardstimmen. */
        const val QWEN_MODELL = "qwen3-tts-flash"

        /** Die geklonten Stimmen — dasselbe Modell wie beim Anlegen (Baustein E). */
        const val QWEN_KLON_MODELL = "qwen3-tts-vc-2026-01-22"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
        val SSML_TYP = "application/ssml+xml".toMediaType()
        val WARTEZEITEN_MS = longArrayOf(1_000L, 3_000L, 8_000L)

        /** Reicht zusammen über das Minutenfenster von F0. */
        val AZURE_WARTEZEITEN_MS = longArrayOf(3_000L, 10_000L, 20_000L, 30_000L)

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
