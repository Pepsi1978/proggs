package de.frank.stacklabor.vorlesen

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Franks geklonte Stimme ("Meine Stimme") ueber Alibaba Model Studio.
 *
 * Mechanik aus `QwenTtsPlayer.kt` (PerfectMoment): Es sind zwei Aufrufe noetig — der erste
 * beantwortet die Anfrage mit einem Link auf die fertige Aufnahme, der zweite laedt sie.
 * Tempo gibt es hier bewusst nicht: Das Modell setzt die Betonung selbst, ein Tempo-Parameter
 * wird nicht angeboten.
 *
 * Schluessel und Stimm-Kennung kommen als Funktionen herein und stehen nie im Quelltext.
 */
class QwenStimme(
    private val schluessel: suspend () -> String,
    private val stimmKennung: suspend () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
) : Sprecher {

    override val anbieter: Anbieter = Anbieter.QWEN_KLON

    override suspend fun sprich(text: String, stimmeId: String, tempo: Float): ByteArray =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext ByteArray(0)
            // Zweite Verteidigungslinie: Frueher gespeicherte Werte koennen Leerzeichen enthalten,
            // die der Dienst mit 401 quittiert.
            val geheim = schluessel().filterNot(Char::isWhitespace)
            val stimme = stimmeId.ifBlank { stimmKennung() }.filterNot(Char::isWhitespace)
            if (geheim.isBlank() || stimme.isBlank()) {
                throw VorleseFehler(
                    "Die eigene Stimme ist noch nicht eingerichtet. " +
                        "Schluessel und Stimm-Kennung stehen in den Einstellungen.",
                )
            }

            val audioAdresse = holeAudioAdresse(text, stimme, geheim)
            val herunterladen = Request.Builder().url(audioAdresse).build()
            try {
                client.newCall(herunterladen).antwort().use { antwort ->
                    if (!antwort.isSuccessful) {
                        throw VorleseFehler("Die eigene Stimme konnte nicht geladen werden (${antwort.code}).")
                    }
                    antwort.body?.bytes()?.takeIf { it.isNotEmpty() }
                        ?: throw VorleseFehler("Die eigene Stimme hat keine Audiodaten geliefert.")
                }
            } catch (netzfehler: IOException) {
                throw VorleseFehler("Die eigene Stimme ist nicht erreichbar.", netzfehler)
            }
        }

    override fun schliesse() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    /** Erster Aufruf: Der Dienst antwortet mit einem Link auf die fertige Aufnahme. */
    private suspend fun holeAudioAdresse(text: String, stimme: String, geheim: String): String {
        val rumpf = JSONObject().apply {
            put("model", MODELL)
            put(
                "input",
                JSONObject()
                    .put("text", text)
                    .put("voice", stimme)
                    .put("language_type", "German"),
            )
        }
        val anfrage = Request.Builder()
            .url(SYNTHESE_ADRESSE)
            .addHeader("Authorization", "Bearer $geheim")
            .post(rumpf.toString().toRequestBody(JSON_TYP))
            .build()

        val inhalt = try {
            client.newCall(anfrage).antwort().use { antwort ->
                val text2 = antwort.body?.string().orEmpty()
                if (!antwort.isSuccessful) {
                    throw VorleseFehler(
                        "Die eigene Stimme hat abgelehnt (${antwort.code}). " +
                            "Bitte den Schluessel pruefen oder den Anbieter wechseln.",
                    )
                }
                text2
            }
        } catch (netzfehler: IOException) {
            throw VorleseFehler("Die eigene Stimme ist nicht erreichbar.", netzfehler)
        }

        val adresse = JSONObject(inhalt)
            .optJSONObject("output")
            ?.optJSONObject("audio")
            ?.optString("url")
            .orEmpty()
        if (adresse.isBlank()) {
            throw VorleseFehler("Die eigene Stimme hat keinen Audio-Link geliefert.")
        }
        // Der Link kommt als einfaches http zurueck, das Android sonst blockiert.
        return if (adresse.startsWith("http://")) {
            "https://" + adresse.removePrefix("http://")
        } else {
            adresse
        }
    }

    private companion object {
        const val SYNTHESE_ADRESSE =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"

        /** Klonen und Sprechen muessen dasselbe Modell nutzen, sonst wird die Stimm-Kennung abgelehnt. */
        const val MODELL = "qwen3-tts-vc-2026-01-22"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
    }
}
