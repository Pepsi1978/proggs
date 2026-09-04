package de.frank.codexkompass.tts

import de.frank.codexkompass.network.awaitAntwort
import de.frank.codexkompass.network.beendeSanft
import de.frank.codexkompass.observability.KompassLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Die eigene, bei Alibaba geklonte Stimme (Referenz, Bausteine D und E).
 *
 * Anders als die übrigen Dienste braucht dieser zwei Schritte: Die erste Anfrage liefert einen
 * Link auf die fertige Tondatei, die danach heruntergeladen wird.
 *
 * Zwei Fallen, die beide schon einmal Zeit gekostet haben:
 *  - Klonen und Sprechen MÜSSEN dasselbe Modell benutzen, sonst wird die Stimmkennung
 *    abgelehnt. Deshalb steht der Modellname genau einmal in [QwenModell].
 *  - Der zurückgelieferte Link kommt teils als `http://`. Android blockt einfaches HTTP —
 *    der Link wird deshalb vor dem Laden auf `https://` gehoben.
 */
object QwenModell {
    /** Eine Konstante für Klonen UND Sprechen. Nie zwei Stellen daraus machen. */
    const val ID = "qwen3-tts-vc-2026-01-22"
}

class QwenTtsSynthesizer(
    private val schluesselGeber: () -> String,
) : TtsSynthesizer {

    override val anbieterId = "qwen"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    override suspend fun synthetisiere(
        text: String,
        stimme: String,
        tempo: Float,
    ): SyntheseErgebnis = withContext(Dispatchers.IO) {
        val schluessel = schluesselGeber().filterNot(Char::isWhitespace)
        val stimmId = stimme.filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Vorlesen mit deiner Stimme geht nicht: Es ist kein Alibaba-Schlüssel hinterlegt. " +
                    "Trag ihn in den Einstellungen unter Vorlesen ein.",
            )
        }
        if (stimmId.isBlank()) {
            throw TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Es ist noch keine eigene Stimme ausgewählt. Nimm in den Einstellungen eine auf " +
                    "oder wähle eine vorhandene aus.",
            )
        }

        val rumpf = JSONObject()
            .put("model", QwenModell.ID)
            .put(
                "input",
                JSONObject()
                    .put("text", text)
                    .put("voice", stimmId)
                    .put("language_type", "German"),
            )
        val anfrage = Request.Builder()
            .url(SYNTHESE_URL)
            .addHeader("Authorization", "Bearer $schluessel")
            .post(rumpf.toString().toRequestBody(JSON_TYP))
            .build()

        val tonAdresse = client.newCall(anfrage).awaitAntwort().use { antwort ->
            val inhalt = antwort.body?.string().orEmpty()
            if (!antwort.isSuccessful) throw deuteFehler(antwort.code, inhalt)
            val adresse = JSONObject(inhalt)
                .optJSONObject("output")
                ?.optJSONObject("audio")
                ?.optString("url")
                .orEmpty()
            if (adresse.isBlank()) {
                throw TtsFehler(TtsFehlerArt.INHALT, "Alibaba hat für diesen Absatz keinen Ton-Link geliefert.")
            }
            if (adresse.startsWith("http://")) "https://" + adresse.removePrefix("http://") else adresse
        }

        val ladeAnfrage = Request.Builder().url(tonAdresse).build()
        client.newCall(ladeAnfrage).awaitAntwort().use { antwort ->
            if (!antwort.isSuccessful) {
                throw TtsFehler(
                    TtsFehlerArt.NETZ,
                    "Der Ton deiner Stimme liess sich nicht laden (${antwort.code}).",
                    wiederholbar = true,
                )
            }
            val bytes = antwort.body?.bytes()
            if (bytes == null || bytes.isEmpty()) {
                throw TtsFehler(TtsFehlerArt.INHALT, "Die geladene Tondatei war leer.")
            }
            SyntheseErgebnis(bytes, "wav")
        }
    }

    private fun deuteFehler(code: Int, rumpf: String): TtsFehler {
        val meldung = runCatching { JSONObject(rumpf).optString("message") }
            .getOrNull()?.takeIf(String::isNotBlank) ?: rumpf.take(200)
        KompassLog.warn("QwenTts", "deuteFehler", "Alibaba hat abgelehnt", mapOf("code" to code, "meldung" to meldung.take(200)))
        return when (code) {
            400 -> TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Alibaba hat die Anfrage abgelehnt (400). Meist passt die Stimmkennung nicht zum " +
                    "Modell ${QwenModell.ID} — die Stimme muss mit genau diesem Modell geklont worden sein. " +
                    "Meldung: $meldung",
            )
            401, 403 -> TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Der Alibaba-Schlüssel wurde abgelehnt ($code). Prüf ihn in den Einstellungen.",
            )
            429 -> TtsFehler(
                TtsFehlerArt.KONTINGENT,
                "Das Alibaba-Kontingent ist erreicht (429). Versuch es später erneut.",
                wiederholbar = true,
            )
            in 500..599 -> TtsFehler(
                TtsFehlerArt.NETZ,
                "Alibaba ist gerade nicht erreichbar ($code).",
                wiederholbar = true,
            )
            else -> TtsFehler(TtsFehlerArt.NETZ, "Alibaba-Fehler $code: $meldung")
        }
    }

    override fun beende() = client.beendeSanft("QwenTts")

    private companion object {
        const val SYNTHESE_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
    }
}
