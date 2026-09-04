package de.frank.codexkompass.tts

import android.util.Base64
import de.frank.codexkompass.network.awaitAntwort
import de.frank.codexkompass.network.beendeSanft
import de.frank.codexkompass.observability.KompassLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Google Chirp 3 HD — der Standardweg (Referenz, Baustein D).
 *
 * Wichtige Falle: Die Chirp-3-HD-Stimmen kennen keinen `pitch`-Parameter. Wird er trotzdem
 * mitgeschickt, antwortet der Dienst mit Fehler 400. Deshalb geht `pitch` hier ausschließlich
 * an Stimmen, die nicht „Chirp" im Namen tragen.
 */
class GoogleTtsSynthesizer(private val schluesselGeber: () -> String) : TtsSynthesizer {

    override val anbieterId = "google"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun synthetisiere(
        text: String,
        stimme: String,
        tempo: Float,
    ): SyntheseErgebnis = withContext(Dispatchers.IO) {
        val schluessel = schluesselGeber().filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Vorlesen mit Google ist nicht möglich: Es ist kein Google-Schlüssel hinterlegt. " +
                    "Trag ihn in den Einstellungen unter Vorlesen ein.",
            )
        }

        val rumpf = JSONObject().apply {
            put("input", JSONObject().put("text", text))
            put("voice", JSONObject().put("languageCode", "de-DE").put("name", stimme))
            put(
                "audioConfig",
                JSONObject()
                    .put("audioEncoding", "MP3")
                    .put("speakingRate", tempo.coerceIn(0.5f, 2.0f).toDouble()),
            )
        }
        val adresse = SYNTHESE_URL.toHttpUrl().newBuilder()
            .addQueryParameter("key", schluessel)
            .build()
        val anfrage = Request.Builder()
            .url(adresse)
            .post(rumpf.toString().toRequestBody(JSON_TYP))
            .build()

        client.newCall(anfrage).awaitAntwort().use { antwort ->
            val text2 = antwort.body?.string().orEmpty()
            if (!antwort.isSuccessful) throw deuteFehler(antwort.code, text2)
            val inhalt = JSONObject(text2).optString("audioContent")
            if (inhalt.isBlank()) {
                throw TtsFehler(TtsFehlerArt.INHALT, "Google hat für diesen Absatz keinen Ton geliefert.")
            }
            SyntheseErgebnis(Base64.decode(inhalt, Base64.DEFAULT), "mp3")
        }
    }

    private fun deuteFehler(code: Int, rumpf: String): TtsFehler {
        val meldung = runCatching {
            JSONObject(rumpf).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf(String::isNotBlank) ?: rumpf.take(200)
        KompassLog.warn(
            "GoogleTts",
            "deuteFehler",
            "Google hat abgelehnt",
            mapOf("code" to code, "meldung" to meldung.take(200)),
        )
        return when (code) {
            400 -> TtsFehler(
                TtsFehlerArt.INHALT,
                "Google hat diesen Absatz abgelehnt (400). Meist passt die gewählte Stimme " +
                    "nicht zur Anfrage. Meldung: $meldung",
            )
            401, 403 -> TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Der Google-Schlüssel wurde abgelehnt ($code). Prüf ihn in den Einstellungen — " +
                    "häufig ist die Cloud-Text-to-Speech-API im Projekt noch nicht freigeschaltet.",
            )
            429 -> TtsFehler(
                TtsFehlerArt.KONTINGENT,
                "Das Google-Kontingent fürs Vorlesen ist erreicht (429). Versuch es später erneut.",
                wiederholbar = true,
            )
            in 500..599 -> TtsFehler(
                TtsFehlerArt.NETZ,
                "Google ist gerade nicht erreichbar ($code). Ein erneuter Versuch lohnt sich.",
                wiederholbar = true,
            )
            else -> TtsFehler(TtsFehlerArt.NETZ, "Google-Fehler $code: $meldung")
        }
    }

    override fun beende() = client.beendeSanft("GoogleTts")
}

private val JSON_TYP = "application/json; charset=utf-8".toMediaType()
private const val SYNTHESE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"
