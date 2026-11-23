package de.frank.kompass.tts

import android.util.Base64
import de.frank.kompass.data.EinstellungenStore
import de.frank.kompass.network.awaitAntwort
import de.frank.kompass.network.beendeSanft
import de.frank.kompass.observability.KompassLog
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini 3.8 Flash bzw. Flash-Lite TTS (Port aus Jarvis).
 *
 * Reihenfolge laut [EinstellungenStore.geminiSchluesselWahl]: erst der kostenlose, dann der bezahlte
 * Schlüssel. Ist [EinstellungenStore.geminiChirpRueckfall] an, geht es nach einem Fehler des kostenlosen
 * Schlüssels nicht zum bezahlten, sondern mit derselben Stimme zu Chirp 3 HD ([chirp]).
 * Netzfehler laufen unverändert durch — die wiederholt der VorleseManager selbst.
 *
 * Gemini liefert rohes PCM (16 Bit, mono); es bekommt einen WAV-Kopf, damit der MediaPlayer es abspielt.
 * Das Sprechtempo kennt Gemini nicht — es spricht im eigenen Tempo.
 */
class GeminiTtsSynthesizer(
    override val anbieterId: String,
    private val modell: String,
    private val einstellungen: EinstellungenStore,
    private val chirp: GoogleTtsSynthesizer,
) : TtsSynthesizer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun synthetisiere(
        text: String,
        stimme: String,
        tempo: Float,
    ): SyntheseErgebnis = withContext(Dispatchers.IO) {
        val wahl = einstellungen.geminiSchluesselWahl
        val frei = einstellungen.geminiSchluesselFrei.filterNot(Char::isWhitespace)
        val bezahlt = einstellungen.geminiSchluesselBezahlt.filterNot(Char::isWhitespace)
        val chirpRueckfall = einstellungen.geminiChirpRueckfall
        var fehlerFrei: TtsFehler? = null
        if (wahl != EinstellungenStore.GEMINI_NUR_BEZAHLT) {
            val gesperrt = System.currentTimeMillis() < freiGesperrtBis
            if (frei.isNotBlank() && !gesperrt) {
                // Mit Ausweichweg nicht erst bei 429 warten — der nächste Weg ist schneller als jede Wartezeit.
                val ausweg = chirpRueckfall ||
                    (wahl == EinstellungenStore.GEMINI_FREI_DANN_BEZAHLT && bezahlt.isNotBlank())
                try {
                    return@withContext anfrage(text, stimme, frei, "kostenlos")
                } catch (fehler: TtsFehler) {
                    if (!ausweg) throw fehler
                    fehlerFrei = fehler
                    freiGesperrtBis = System.currentTimeMillis() + FREI_PAUSE_MS
                    KompassLog.warn(
                        "GeminiTts",
                        "synthetisiere",
                        "Kostenloser Gemini-Schlüssel gescheitert",
                        mapOf("modell" to modell, "grund" to fehler.message?.take(120)),
                    )
                }
            }
            if (chirpRueckfall) {
                KompassLog.info(
                    "GeminiTts",
                    "synthetisiere",
                    "Rückfall auf Chirp 3 HD",
                    mapOf("stimme" to TtsCatalog.geminiName(stimme), "gesperrt" to gesperrt),
                )
                return@withContext chirp.synthetisiere(text, TtsCatalog.chirpZu(stimme), tempo)
            }
            if (wahl == EinstellungenStore.GEMINI_NUR_FREI) {
                throw fehlerFrei ?: TtsFehler(
                    TtsFehlerArt.SCHLUESSEL,
                    "Vorlesen mit Gemini ist nicht möglich: Es ist kein kostenloser Gemini-Schlüssel für die " +
                        "Stimme hinterlegt. Trag ihn in den Einstellungen unter Vorlesen ein.",
                )
            }
        }
        if (bezahlt.isBlank()) {
            throw fehlerFrei ?: TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Vorlesen mit Gemini ist nicht möglich: Es ist kein Gemini-Schlüssel für die Stimme " +
                    "hinterlegt. Trag ihn in den Einstellungen unter Vorlesen ein.",
            )
        }
        anfrage(text, stimme, bezahlt, "bezahlt")
    }

    private suspend fun anfrage(text: String, stimme: String, schluessel: String, art: String): SyntheseErgebnis {
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
                            JSONObject().put(
                                "prebuiltVoiceConfig",
                                JSONObject().put("voiceName", TtsCatalog.geminiName(stimme)),
                            ),
                        ),
                    ),
            )
        val anfrage = Request.Builder()
            .url("$GEMINI_URL$modell:generateContent")
            .addHeader("x-goog-api-key", schluessel)
            .post(koerper.toString().toRequestBody(JSON_TYP))
            .build()
        val antwortText = client.newCall(anfrage).awaitAntwort().use { antwort ->
            val rumpf = antwort.body?.string().orEmpty()
            if (!antwort.isSuccessful) {
                throw deuteFehler(antwort.code, rumpf, art, antwort.header("Retry-After"))
            }
            rumpf
        }
        val kandidat = JSONObject(antwortText).optJSONArray("candidates")?.optJSONObject(0)
        val ende = kandidat?.optString("finishReason").orEmpty()
        val teil = kandidat?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
            ?.optJSONObject("inlineData")
        val daten = teil?.optString("data").orEmpty()
        if (daten.isBlank()) throw TtsFehler(TtsFehlerArt.INHALT, "Gemini hat kein Audio geliefert ($ende).")
        if (ende.isNotBlank() && ende != "STOP") {
            throw TtsFehler(TtsFehlerArt.INHALT, "Gemini hat die Stimme abgebrochen ($ende).")
        }
        val roh = Base64.decode(daten, Base64.DEFAULT)
        val rate = Regex("rate=(\\d+)").find(teil?.optString("mimeType").orEmpty())
            ?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val wav = if (roh.size > 44 && String(roh, 0, 4, Charsets.US_ASCII) == "RIFF") roh else mitWavKopf(roh, rate)
        // Abgeschnittenes Audio meldet Gemini teils trotzdem als fertig: Unter 25 Zeichen je Sekunde spricht niemand.
        val sekunden = (wav.size - 44) / (wavRate(wav) * 2.0)
        if (text.length >= 60 && sekunden < text.length / 25.0) {
            throw TtsFehler(
                TtsFehlerArt.INHALT,
                "Gemini-Audio zu kurz (${"%.1f".format(sekunden)} s für ${text.length} Zeichen).",
            )
        }
        KompassLog.info(
            "GeminiTts",
            "anfrage",
            "Gemini-Absatz erzeugt",
            mapOf("modell" to modell, "schluessel" to art, "zeichen" to text.length, "ms" to System.currentTimeMillis() - beginn),
        )
        return SyntheseErgebnis(wav, "wav")
    }

    private fun deuteFehler(code: Int, rumpf: String, art: String, retryAfter: String?): TtsFehler {
        val meldung = runCatching {
            JSONObject(rumpf).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf(String::isNotBlank) ?: rumpf.take(200)
        KompassLog.warn(
            "GeminiTts",
            "deuteFehler",
            "Gemini hat abgelehnt",
            mapOf("code" to code, "schluessel" to art, "meldung" to meldung.take(200)),
        )
        return when (code) {
            400 -> TtsFehler(
                TtsFehlerArt.INHALT,
                "Gemini hat diesen Absatz abgelehnt (400). Meldung: $meldung",
            )
            401, 403 -> TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Der $art Gemini-Schlüssel wurde abgelehnt ($code). Prüf ihn in den Einstellungen.",
            )
            429 -> TtsFehler(
                TtsFehlerArt.KONTINGENT,
                "Das Kontingent der Gemini-Stimme ($art) ist erschöpft (429). Versuch es später erneut.",
                wiederholbar = true,
            ).apply {
                wartezeitMs = retryAfter?.trim()?.toLongOrNull()?.let { (it * 1000).coerceIn(1_000L, 30_000L) } ?: 0L
            }
            in 500..599 -> TtsFehler(
                TtsFehlerArt.NETZ,
                "Gemini ist gerade nicht erreichbar ($code). Ein erneuter Versuch lohnt sich.",
                wiederholbar = true,
            )
            else -> TtsFehler(TtsFehlerArt.NETZ, "Gemini-Fehler $code: $meldung")
        }
    }

    override fun beende() = client.beendeSanft("GeminiTts")

    companion object {
        const val FLASH_MODELL = "gemini-3.8-flash-tts"
        const val LITE_MODELL = "gemini-3.8-flash-lite-tts"
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        private val JSON_TYP = "application/json; charset=utf-8".toMediaType()

        /** So lange bleibt der kostenlose Gemini-Schlüssel nach einem Fehler übersprungen, wenn es einen Ausweichweg gibt. */
        const val FREI_PAUSE_MS = 10 * 60_000L

        /** Gilt für alle Instanzen (Flash und Flash-Lite): Sonst probierte jede den gescheiterten Schlüssel erneut. */
        @Volatile
        var freiGesperrtBis = 0L

        /** Rohes PCM (16 Bit, mono) bekommt einen WAV-Kopf, damit der MediaPlayer es abspielt. */
        fun mitWavKopf(pcm: ByteArray, rate: Int): ByteArray {
            val kopf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
                .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
                .putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(pcm.size)
            return kopf.array() + pcm
        }

        fun wavRate(wav: ByteArray): Int =
            ByteBuffer.wrap(wav, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int.takeIf { it > 0 } ?: 24_000

        /**
         * Hängt mehrere WAV-Stücke mit 44-Byte-Kopf aneinander (für den halbierten Absatz).
         * null, wenn eines der Stücke einen anderen Kopf hat — dann lässt sich nicht sicher verbinden.
         */
        fun verbindeWav(stuecke: List<ByteArray>): ByteArray? {
            if (stuecke.isEmpty()) return null
            val alleStandard = stuecke.all {
                it.size > 44 && String(it, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                    String(it, 36, 4, Charsets.US_ASCII) == "data"
            }
            if (!alleStandard) return null
            val rate = wavRate(stuecke.first())
            val pcm = stuecke.fold(ByteArray(0)) { gesammelt, stueck -> gesammelt + stueck.copyOfRange(44, stueck.size) }
            return mitWavKopf(pcm, rate)
        }
    }
}
