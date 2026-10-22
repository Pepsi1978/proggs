package de.frank.kompass.audio

import de.frank.kompass.network.awaitAntwort
import de.frank.kompass.network.beendeSanft
import de.frank.kompass.observability.KompassLog
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
 *  - Eine zu große Aufnahme wird VOR dem Senden geteilt. Der Ablehnungsfehler 413 lässt sich
 *    nicht wiederholen; ohne das Teilen wäre ein langes Diktat vollständig verloren.
 */
class GroqTranskribierer(
    private val schluesselGeber: () -> String,
    private val modellGeber: () -> String,
    private val schalterGeber: () -> FilterSchalter,
    /** Begriffe des persönlichen Wörterbuchs als Rohtext, oder null, wenn der Schalter aus ist. */
    private val woerterbuchGeber: () -> String? = { null },
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
        var modell = loeseModell(modellGeber())
        val prompt = woerterbuchGeber()?.let { baueWoerterbuchPrompt(parseBegriffe(it)) }
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
                var roh = try {
                    sende(teil, schluessel, modell, prompt)
                } catch (gesperrt: ModellGesperrt) {
                    // large-v3 ist für die Groq-Organisation nicht freigeschaltet: Diktat nicht
                    // verlieren, dieselbe Aufnahme mit Turbo wiederholen und 30 Minuten dabei bleiben.
                    KompassLog.warn(
                        "GroqTranskribierer",
                        "modellGesperrt",
                        "Modell gesperrt — weiter mit Turbo",
                        mapOf("modell" to modell),
                    )
                    gesperrtBis = System.currentTimeMillis() + SPERRE_MS
                    modell = TURBO
                    sende(teil, schluessel, modell, prompt)
                }
                if (prompt != null && istPromptEcho(roh.text, prompt)) {
                    // Whisper gibt bei Stille/Gemurmel gern den Prompt selbst aus: einmal ohne
                    // Wörterbuch wiederholen statt das Diktat zu verlieren.
                    KompassLog.warn(
                        "GroqTranskribierer",
                        "promptEcho",
                        "Wörterbuch-Echo erkannt — erneut ohne Wörterbuch",
                        mapOf("teil" to index),
                    )
                    roh = sende(teil, schluessel, modell, null)
                }
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

    private suspend fun sende(
        wav: ByteArray,
        schluessel: String,
        modell: String,
        prompt: String?,
    ): GroqAntwort {
        val bauer = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "aufnahme.wav", wav.toRequestBody(WAV_TYP))
            .addFormDataPart("model", modell)
            .addFormDataPart("language", "de")
            // Ohne verbose_json fehlen die Kennzahlen, auf denen die Schichten 2 und 3 stehen.
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("temperature", "0")
        // Persönliches Wörterbuch: Whisper übernimmt daraus die Schreibweisen von Eigennamen.
        if (prompt != null) bauer.addFormDataPart("prompt", prompt)
        val mehrteilig = bauer.build()
        val anfrage = Request.Builder()
            .url(TRANSKRIPTIONS_URL)
            .header("Authorization", "Bearer $schluessel")
            .post(mehrteilig)
            .build()

        return client.newCall(anfrage).awaitAntwort().use { antwort ->
            val inhalt = antwort.body?.string().orEmpty()
            if (!antwort.isSuccessful) {
                if (modell == LARGE_V3 && istModellGesperrt(inhalt)) throw ModellGesperrt()
                throw TranskriptionsFehler(deuteFehler(antwort.code, inhalt))
            }
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
        if (istModellGesperrt(inhalt)) {
            return "Das gewählte Whisper-Modell ist für deine Groq-Organisation nicht freigeschaltet ($code)."
        }
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

    /** Groq meldet: Modell für die Organisation gesperrt (large-v3 nicht freigeschaltet). */
    private class ModellGesperrt : Exception("model_permission_blocked_org")

    companion object {
        internal const val TRANSKRIPTIONS_URL = "https://api.groq.com/openai/v1/audio/transcriptions"

        const val TURBO = "whisper-large-v3-turbo"
        const val LARGE_V3 = "whisper-large-v3"

        /** Die Modelle, die zur Auswahl stehen. Das erste ist die Voreinstellung. */
        val MODELLE = listOf(TURBO, LARGE_V3)

        /** Anzeigename eines Modells in den Einstellungen. */
        fun modellName(modell: String): String = when (modell) {
            TURBO -> "Large V3 Turbo (schnell)"
            LARGE_V3 -> "Large V3 (genauer)"
            else -> modell
        }

        /** Vorbelegung des persönlichen Wörterbuchs (Stand TerminalVoiceOverlay). */
        const val STANDARD_WOERTERBUCH = "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, " +
            "UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, " +
            "ChatGPT, Claude, Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic"

        private const val MAX_BEGRIFFE = 100
        private const val MAX_PROMPT_ZEICHEN = 350
        private const val ECHO_MARKER = "Begriffe"
        private const val SPERRE_MS = 30 * 60 * 1000L

        /** Nach einer 403-Sperre von large-v3 bis zu diesem Zeitpunkt direkt Turbo nehmen. */
        @Volatile
        private var gesperrtBis = 0L

        private val WAV_TYP = "audio/wav".toMediaType()

        internal fun loeseModell(gewaehlt: String): String =
            if (gewaehlt == LARGE_V3 && System.currentTimeMillis() < gesperrtBis) TURBO else gewaehlt

        internal fun istModellGesperrt(inhalt: String): Boolean =
            inhalt.contains("model_permission_blocked_org") ||
                inhalt.contains("blocked at the organization level")

        /** Begriffe trennen, trimmen, Kommentare und Doppelte entfernen, höchstens 100. */
        fun parseBegriffe(roh: String): List<String> =
            roh.split('\n', '\r', ',', ';')
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .distinctBy { it.lowercase() }
                .take(MAX_BEGRIFFE)

        /** Wörterbuch als natürlicher Satz, höchstens 350 Zeichen; zu lange Begriffe werden übersprungen. */
        fun baueWoerterbuchPrompt(begriffe: List<String>): String? {
            if (begriffe.isEmpty()) return null
            val sb = StringBuilder("$ECHO_MARKER: ")
            var hinzu = 0
            for (begriff in begriffe) {
                if (sb.length + begriff.length + 2 > MAX_PROMPT_ZEICHEN) continue
                if (hinzu++ > 0) sb.append(", ")
                sb.append(begriff)
            }
            return if (hinzu == 0) null else sb.append('.').toString()
        }

        /** Whisper wiederholt den Prompt ab seinem Anfang ("Begriffe: …"), ganz oder teilweise. */
        fun istPromptEcho(text: String, prompt: String?): Boolean {
            if (prompt.isNullOrEmpty() || text.isBlank()) return false
            fun norm(s: String) = s.lowercase().filter(Char::isLetterOrDigit)
            val t = norm(text)
            val p = norm(prompt)
            val marker = norm(ECHO_MARKER)
            return t.startsWith(marker) && p.contains(t.substring(marker.length))
        }
    }
}
