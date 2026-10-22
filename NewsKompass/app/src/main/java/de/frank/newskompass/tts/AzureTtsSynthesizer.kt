package de.frank.newskompass.tts

import de.frank.newskompass.data.EinstellungenStore
import de.frank.newskompass.network.awaitAntwort
import de.frank.newskompass.network.beendeSanft
import de.frank.newskompass.observability.KompassLog
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Azure AI Speech über REST (Port aus Jarvis): Schlüssel und Region der eigenen Speech-Ressource
 * (F0 kostenlos oder S0). HD-Stimmen (DragonHD) bekommen kein Tempo: Sie bestimmen Betonung und Tempo selbst.
 *
 * F0 erlaubt nur rund 20 Anfragen je Minute: Ein 429 ist meist dieses Ratenlimit, nicht das
 * Monatskontingent. Die Wartezeit aus Retry-After geht an den VorleseManager, der dann erneut versucht.
 */
class AzureTtsSynthesizer(private val einstellungen: EinstellungenStore) : TtsSynthesizer {

    override val anbieterId = "azure_neural"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun synthetisiere(
        text: String,
        stimme: String,
        tempo: Float,
    ): SyntheseErgebnis = withContext(Dispatchers.IO) {
        val schluessel = einstellungen.azureSchluessel.filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Vorlesen mit Azure ist nicht möglich: Es ist kein Azure-Schlüssel hinterlegt. " +
                    "Trag ihn in den Einstellungen unter Vorlesen ein.",
            )
        }
        val region = TtsCatalog.azureRegion(einstellungen.azureRegion) ?: throw TtsFehler(
            TtsFehlerArt.SCHLUESSEL,
            "Die Azure-Region „${einstellungen.azureRegion.take(40)}“ verstehe ich nicht. " +
                "Trag den Regionsnamen ein, etwa westeurope.",
        )
        val name = TtsCatalog.azureName(stimme)
        val hd = TtsCatalog.istAzureHd(stimme)
        // Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
        val escaped = xmlEscape(text.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " "))
        // <lang> kennt Azure nur bei den mehrsprachigen Stimmen; die übrigen sprechen ohnehin nur Deutsch.
        val inhalt = if (name.contains("Multilingual")) "<lang xml:lang='de-DE'>$escaped</lang>" else escaped
        val prozent = ((tempo.coerceIn(0.5f, 2.0f) - 1f) * 100).roundToInt()
        val mitTempo = if (hd || prozent == 0) {
            inhalt
        } else {
            "<prosody rate='${if (prozent > 0) "+" else ""}$prozent%'>$inhalt</prosody>"
        }
        val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
            "<voice name='$name'>$mitTempo</voice></speak>"
        val beginn = System.currentTimeMillis()
        val anfrage = Request.Builder()
            .url("https://$region.tts.speech.microsoft.com/cognitiveservices/v1")
            .addHeader("Ocp-Apim-Subscription-Key", schluessel)
            .addHeader("X-Microsoft-OutputFormat", "audio-24khz-96kbitrate-mono-mp3")
            .addHeader("User-Agent", "Kompass")
            .post(ssml.toRequestBody(SSML_TYP))
            .build()

        client.newCall(anfrage).awaitAntwort().use { antwort ->
            if (!antwort.isSuccessful) {
                throw deuteFehler(antwort.code, antwort.body?.string().orEmpty(), hd, antwort.header("Retry-After"))
            }
            val bytes = antwort.body?.bytes() ?: ByteArray(0)
            if (bytes.isEmpty()) throw TtsFehler(TtsFehlerArt.INHALT, "Azure hat kein Audio geliefert.")
            KompassLog.info(
                "AzureTts",
                "synthetisiere",
                "Azure-Absatz erzeugt",
                mapOf("stimme" to name, "zeichen" to text.length, "ms" to System.currentTimeMillis() - beginn),
            )
            SyntheseErgebnis(bytes, "mp3")
        }
    }

    private fun deuteFehler(code: Int, rumpf: String, hd: Boolean, retryAfter: String?): TtsFehler {
        KompassLog.warn(
            "AzureTts",
            "deuteFehler",
            "Azure hat abgelehnt",
            mapOf("code" to code, "hd" to hd, "meldung" to rumpf.take(200)),
        )
        return when {
            code == 401 || code == 403 -> TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Azure hat Schlüssel oder Region abgelehnt (Fehler $code). Prüf beides in den Einstellungen.",
            )
            code == 429 -> TtsFehler(
                TtsFehlerArt.KONTINGENT,
                "Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.",
                wiederholbar = true,
            ).apply {
                // F0 erlaubt rund 20 Anfragen je Minute; ohne Angabe genügen ein paar Sekunden meist nicht.
                wartezeitMs = retryAfter?.trim()?.toLongOrNull()?.let { (it * 1000).coerceIn(1_000L, 30_000L) }
                    ?: AZURE_WARTEZEIT_MS
            }
            code == 400 && hd -> TtsFehler(
                TtsFehlerArt.SCHLUESSEL,
                "Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und " +
                    "nicht in jeder Region.",
            )
            code == 400 -> TtsFehler(TtsFehlerArt.INHALT, "Azure hat diesen Absatz abgelehnt (400): ${rumpf.take(200)}")
            code in 500..599 -> TtsFehler(
                TtsFehlerArt.NETZ,
                "Azure ist gerade nicht erreichbar ($code). Ein erneuter Versuch lohnt sich.",
                wiederholbar = true,
            )
            else -> TtsFehler(TtsFehlerArt.NETZ, "Azure antwortete mit Fehler $code: ${rumpf.take(200)}")
        }
    }

    private fun xmlEscape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    override fun beende() = client.beendeSanft("AzureTts")

    private companion object {
        val SSML_TYP = "application/ssml+xml".toMediaType()
        const val AZURE_WARTEZEIT_MS = 10_000L
    }
}
