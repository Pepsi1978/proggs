package de.frank.codexkompass.tts

import android.util.Base64
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

/** Eine geklonte Stimme aus dem eigenen Alibaba-Konto. */
data class GeklonteStimme(
    val id: String,
    val name: String,
    val angelegtAm: String,
)

class QwenStimmFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)

/**
 * Legt eigene Stimmen an, listet sie auf und löscht sie wieder (Referenz, Baustein E).
 *
 * Die Aufnahme reist als eingebettete Base64-Datenadresse mit. Damit muss nichts vorher an eine
 * öffentliche Adresse hochgeladen werden — das Handy spricht direkt mit Model Studio.
 */
class QwenStimmVerwaltung(private val schluesselGeber: () -> String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    /** Meldet [wav] als neue Stimme an und liefert deren Kennung zurück. */
    suspend fun lege(name: String, wav: ByteArray): String = withContext(Dispatchers.IO) {
        val schluessel = pruefeSchluessel()
        if (wav.isEmpty()) throw QwenStimmFehler("Die Aufnahme ist leer. Nimm sie bitte noch einmal auf.")

        val kodiert = Base64.encodeToString(wav, Base64.NO_WRAP)
        val rumpf = JSONObject()
            .put("model", ANMELDE_MODELL)
            .put(
                "input",
                JSONObject()
                    .put("action", "create")
                    // Klonen und Sprechen MÜSSEN dasselbe Modell nennen, sonst wird die
                    // Stimmkennung beim Vorlesen später abgelehnt.
                    .put("target_model", QwenModell.ID)
                    .put("preferred_name", saeubereName(name))
                    .put("audio", JSONObject().put("data", "data:audio/wav;base64,$kodiert")),
            )
        val antwort = rufe(rumpf, schluessel)
        antwort.optJSONObject("output")?.optString("voice").orEmpty().ifBlank {
            throw QwenStimmFehler("Alibaba hat keine Stimmkennung zurückgegeben.")
        }.also {
            KompassLog.info("QwenStimmen", "lege", "Neue eigene Stimme angelegt", mapOf("name" to name))
        }
    }

    suspend fun liste(): List<GeklonteStimme> = withContext(Dispatchers.IO) {
        val schluessel = schluesselGeber().filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) return@withContext emptyList()

        val rumpf = JSONObject()
            .put("model", ANMELDE_MODELL)
            .put("input", JSONObject().put("action", "list"))
        val antwort = rufe(rumpf, schluessel)
        val liste = antwort.optJSONObject("output")?.optJSONArray("voice_list")
            ?: return@withContext emptyList()
        (0 until liste.length()).mapNotNull { index ->
            val eintrag = liste.optJSONObject(index) ?: return@mapNotNull null
            val id = eintrag.optString("voice")
            if (id.isBlank()) return@mapNotNull null
            GeklonteStimme(
                id = id,
                name = anzeigeName(id),
                angelegtAm = deutschesDatum(eintrag.optString("gmt_create")),
            )
        }
    }

    suspend fun loesche(stimmId: String): Unit = withContext(Dispatchers.IO) {
        val schluessel = pruefeSchluessel()
        val rumpf = JSONObject()
            .put("model", ANMELDE_MODELL)
            .put(
                "input",
                JSONObject()
                    .put("action", "delete")
                    .put("voice", stimmId.filterNot(Char::isWhitespace)),
            )
        rufe(rumpf, schluessel)
        KompassLog.info("QwenStimmen", "loesche", "Eigene Stimme gelöscht", mapOf("id" to stimmId.take(24)))
    }

    fun beende() = client.beendeSanft("QwenStimmen")

    private fun pruefeSchluessel(): String {
        val schluessel = schluesselGeber().filterNot(Char::isWhitespace)
        if (schluessel.isBlank()) {
            throw QwenStimmFehler("Es fehlt der Alibaba-Schlüssel. Trag ihn in den Einstellungen ein.")
        }
        return schluessel
    }

    private suspend fun rufe(rumpf: JSONObject, schluessel: String): JSONObject {
        val anfrage = Request.Builder()
            .url(VERWALTUNGS_URL)
            .addHeader("Authorization", "Bearer $schluessel")
            .post(rumpf.toString().toRequestBody(JSON_TYP))
            .build()
        val text = try {
            client.newCall(anfrage).awaitAntwort().use { antwort ->
                val inhalt = antwort.body?.string().orEmpty()
                if (!antwort.isSuccessful) {
                    KompassLog.error(
                        "QwenStimmen",
                        "rufe",
                        "Alibaba hat abgelehnt",
                        mapOf("code" to antwort.code, "meldung" to inhalt.take(300)),
                    )
                    throw QwenStimmFehler(lesbarerFehler(inhalt, antwort.code))
                }
                inhalt
            }
        } catch (fehler: QwenStimmFehler) {
            throw fehler
        } catch (fehler: Exception) {
            throw QwenStimmFehler("Die Verbindung zu Alibaba ist fehlgeschlagen: ${fehler.message}", fehler)
        }
        return runCatching { JSONObject(text) }
            .getOrElse { throw QwenStimmFehler("Alibaba hat eine unlesbare Antwort geliefert.", it) }
    }

    private companion object {
        const val VERWALTUNGS_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/audio/tts/customization"
        const val ANMELDE_MODELL = "qwen-voice-enrollment"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()

        /** Alibaba backt den Namen in die Kennung und nimmt nur Buchstaben und Ziffern an. */
        fun saeubereName(roh: String): String = roh
            .filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
            .take(16)
            .ifBlank { "Stimme" }

        /** Holt den gewählten Namen aus Kennungen wie `qwen-tts-vc-FrankHD-voice-2026…-4d0a`. */
        fun anzeigeName(id: String): String {
            val name = Regex("^qwen-tts-vc-(.+?)-voice-").find(id)?.groupValues?.get(1) ?: return id
            // Alibaba erlaubt keine Leerzeichen im Namen; ein Grossbuchstabe nach einem
            // Kleinbuchstaben ist deshalb die Wortgrenze: aus FrankHD wird Frank HD.
            return name.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
        }

        /** Macht aus `2026-08-03 23:21:42` das Datum `03.08.2026, 23:21`. */
        fun deutschesDatum(roh: String): String {
            val treffer = Regex("""(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})""").find(roh) ?: return roh
            val (jahr, monat, tag, stunde, minute) = treffer.destructured
            return "$tag.$monat.$jahr, $stunde:$minute"
        }

        fun lesbarerFehler(inhalt: String, code: Int): String {
            val meldung = runCatching { JSONObject(inhalt).optString("message") }.getOrNull()
            return when {
                !meldung.isNullOrBlank() -> meldung
                code == 401 || code == 403 -> "Der Alibaba-Schlüssel wurde abgelehnt."
                else -> "Alibaba hat mit Fehler $code geantwortet."
            }
        }
    }
}

/** Der Text, der beim Aufnehmen der eigenen Stimme auf dem Bildschirm steht. */
object StimmProbeText {
    /**
     * Alibaba nimmt höchstens 60 Sekunden Referenzton. Ruhig gelesen sind das rund 105 Wörter —
     * der Text ist deshalb nach Wortzahl bemessen, nicht nach Satzanzahl.
     */
    const val ZIEL_WOERTER = 105

    val abschnitte: List<String> = listOf(
        "Ich lese jetzt ein paar Sätze vor, damit meine Stimme aufgenommen werden kann.",
        "Codex CLI ist ein Werkzeug, mit dem ich am Rechner programmiere und Aufgaben erledige.",
        "Es kennt viele Befehle, die alle mit einem Schrägstrich beginnen, zum Beispiel Hilfe oder Modell.",
        "Dazu kommen Einstellungen, mit denen ich festlege, was ohne Rückfrage laufen darf.",
        "Ich spreche ruhig und gleichmässig weiter, damit die Aufnahme sauber und natürlich klingt.",
        "So klingt meine normale Sprechstimme, wenn ich jemandem etwas in Ruhe erkläre.",
        "Damit ist die Aufnahme lang genug, und ich kann sie jetzt beenden.",
    )
}
