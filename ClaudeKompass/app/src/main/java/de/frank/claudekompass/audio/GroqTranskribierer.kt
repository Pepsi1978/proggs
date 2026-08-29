package de.frank.claudekompass.audio

import de.frank.claudekompass.network.awaitAntwort
import de.frank.claudekompass.network.beendeSanft
import de.frank.claudekompass.observability.KompassLog
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TranskriptionsFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)

/**
 * Wandelt eine Aufnahme in Text (Referenz, Baustein F).
 *
 * Zwei Dinge sind hier nicht verhandelbar:
 *  - `response_format` MUSS `verbose_json` sein. Ohne dieses Format fehlen die Kennzahlen je
 *    Abschnitt, und die Halluzinations-Schichten 2 und 3 hätten nichts zum Prüfen.
 *  - Eine zu grosse Aufnahme wird VOR dem Senden geteilt. Der Ablehnungsfehler 413 lässt sich
 *    nicht wiederholen; ohne das Teilen wäre ein langes Diktat vollständig verloren.
 */
class GroqTranskribierer(
    private val schluesselGeber: () -> String,
    private val modellGeber: () -> String,
    private val schalterGeber: () -> FilterSchalter,
) {

    private val analysator = SprachAnalysator()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /**
     * Liefert den erkannten Text. Ein leerer Text bedeutet: Es wurde nichts Verwertbares
     * gesprochen — das ist ein gültiges Ergebnis, kein Fehler.
     */
    suspend fun transkribiere(wav: ByteArray): String = withContext(Dispatchers.IO) {
        val schluessel = schluesselGeber().filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw TranskriptionsFehler(
                "Spracheingabe geht nicht: Es ist kein Groq-Schlüssel hinterlegt. " +
                    "Trag ihn in den Einstellungen unter Spracheingabe ein.",
            )
        }

        val schalter = schalterGeber()
        val teile = WavSchneider.teileWennNoetig(wav)
        val ergebnisse = mutableListOf<String>()
        var letzterFehler: Exception? = null

        teile.forEachIndexed { index, teil ->
            // --- Schicht 1: gar nicht erst senden, wenn nichts gesprochen wurde ------------
            val analyse = analysator.analysiere(teil)
            if (schalter.schicht1Stille && analyse != null &&
                analyse.gesprochenMs < SprachAnalysator.MIND_SPRACHE_MS
            ) {
                KompassLog.info(
                    "GroqTranskribierer",
                    "schicht1",
                    "Teil vor dem Hochladen verworfen — zu wenig Sprache",
                    mapOf("teil" to index, "gesprochenMs" to analyse.gesprochenMs),
                )
                return@forEachIndexed
            }

            try {
                val roh = sende(teil, schluessel)
                val gefiltert = HalluzinationsFilter(schalter).filtere(roh, analyse)
                if (gefiltert.isNotBlank()) ergebnisse += gefiltert
            } catch (fehler: Exception) {
                // Ein ausgefallener Teil kostet nur seine Sekunden. Der Rest wird weiter
                // verarbeitet, statt das ganze Diktat wegzuwerfen.
                letzterFehler = fehler
                KompassLog.error(
                    "GroqTranskribierer",
                    "transkribiere",
                    "Teil fehlgeschlagen",
                    mapOf("teil" to index, "vonInsgesamt" to teile.size, "grund" to fehler.message),
                )
            }
        }

        if (ergebnisse.isEmpty() && letzterFehler != null) throw letzterFehler
        if (ergebnisse.size < teile.size && letzterFehler != null) {
            KompassLog.warn(
                "GroqTranskribierer",
                "transkribiere",
                "Nicht alle Teile kamen durch — Text ist unvollständig",
                mapOf("erhalten" to ergebnisse.size, "erwartet" to teile.size),
            )
        }
        ergebnisse.joinToString(" ").trim()
    }

    private suspend fun sende(wav: ByteArray, schluessel: String): GroqAntwort {
        val mehrteilig = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "aufnahme.wav", wav.toRequestBody(WAV_TYP))
            .addFormDataPart("model", modellGeber())
            .addFormDataPart("language", "de")
            // Ohne verbose_json fehlen die Kennzahlen, auf denen die Schichten 2 und 3 stehen.
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0")
            .build()
        val anfrage = Request.Builder()
            .url(TRANSKRIPTIONS_URL)
            .header("Authorization", "Bearer $schluessel")
            .post(mehrteilig)
            .build()

        return client.newCall(anfrage).awaitAntwort().use { antwort ->
            val inhalt = antwort.body?.string().orEmpty()
            if (!antwort.isSuccessful) throw TranskriptionsFehler(deuteFehler(antwort.code, inhalt))
            runCatching { lies(inhalt) }.getOrElse {
                throw TranskriptionsFehler("Groq hat eine unlesbare Antwort geliefert.", it)
            }
        }
    }

    internal fun lies(inhalt: String): GroqAntwort {
        val json = JSONObject(inhalt)
        val abschnitte = json.optJSONArray("segments")?.let { feld ->
            (0 until feld.length()).map { index ->
                val eintrag = feld.getJSONObject(index)
                GroqAbschnitt(
                    von = eintrag.optDoubleOderNull("start"),
                    bis = eintrag.optDoubleOderNull("end"),
                    text = eintrag.optString("text"),
                    keineSpracheWahrscheinlichkeit = eintrag.optDoubleOderNull("no_speech_prob"),
                    mittlereLogWahrscheinlichkeit = eintrag.optDoubleOderNull("avg_logprob"),
                    kompressionsRate = eintrag.optDoubleOderNull("compression_ratio"),
                )
            }
        }
        return GroqAntwort(json.optString("text").trim(), abschnitte)
    }

    private fun JSONObject.optDoubleOderNull(name: String): Double? =
        optDouble(name).takeUnless(Double::isNaN)

    private fun deuteFehler(code: Int, inhalt: String): String {
        val meldung = runCatching {
            JSONObject(inhalt).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf(String::isNotBlank)
        return when (code) {
            401, 403 -> "Der Groq-Schlüssel wurde abgelehnt ($code). Prüf ihn in den Einstellungen."
            413 -> "Die Aufnahme war für Groq zu gross, obwohl sie geteilt wurde. " +
                "Nimm bitte kürzer auf."
            429 -> "Groq hat zu viele Anfragen bekommen (429). Versuch es gleich noch einmal."
            in 500..599 -> "Groq ist gerade nicht erreichbar ($code). Ein erneuter Versuch lohnt sich."
            else -> meldung?.let { "Groq-Fehler $code: $it" } ?: "Groq-Fehler $code."
        }
    }

    fun beende() = client.beendeSanft("GroqTranskribierer")

    companion object {
        internal const val TRANSKRIPTIONS_URL = "https://api.groq.com/openai/v1/audio/transcriptions"

        /** Die Modelle, die zur Auswahl stehen. Das erste ist die Voreinstellung. */
        val MODELLE = listOf("whisper-large-v3-turbo", "whisper-large-v3")
        private val WAV_TYP = "audio/wav".toMediaType()
    }
}
