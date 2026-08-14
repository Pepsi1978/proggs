package de.frank.stacklabor.vorlesen

import android.util.Base64
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

/**
 * Google Chirp 3 HD als Sprecher.
 *
 * Mechanik aus `GoogleCloudTtsPlayer.kt` (PerfectMoment): eine einzige REST-Anfrage, die das
 * fertige MP3 als Base64-Text zurueckgibt.
 *
 * Der Schluessel kommt als Funktion herein — nie aus dem Quelltext. Woher er stammt, entscheidet
 * der Aufrufer (siehe `SchluesselSpeicher` in [VorleseSteuerung]: Einstellung oder die aus
 * `$HOME/SK/StackLabor/` auf das Geraet gelegte Schluesseldatei).
 */
class GoogleStimme(
    private val schluessel: suspend () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) : Sprecher {

    override val anbieter: Anbieter = Anbieter.GOOGLE_CLOUD

    override suspend fun sprich(text: String, stimmeId: String, tempo: Float): ByteArray =
        withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext ByteArray(0)
            val geheim = schluessel().trim()
            if (geheim.isBlank()) {
                throw VorleseFehler(
                    "Fuer Google Chirp 3 HD fehlt der Schluessel. Er wird in den Einstellungen hinterlegt.",
                )
            }
            val stimme = stimmeId.ifBlank { StimmenKatalog.VORGABE_GOOGLE_STIMME }

            val rumpf = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put(
                    "voice",
                    JSONObject().put("languageCode", "de-DE").put("name", stimme),
                )
                put(
                    "audioConfig",
                    JSONObject()
                        .put("audioEncoding", "MP3")
                        .put("speakingRate", tempo.coerceIn(0.7f, 1.3f).toDouble()),
                )
            }
            val adresse = GOOGLE_ADRESSE.toHttpUrl().newBuilder()
                .addQueryParameter("key", geheim)
                .build()
            val anfrage = Request.Builder()
                .url(adresse)
                .post(rumpf.toString().toRequestBody(JSON_TYP))
                .build()

            val antwortText = try {
                client.newCall(anfrage).antwort().use { antwort ->
                    val inhalt = antwort.body?.string().orEmpty()
                    if (!antwort.isSuccessful) {
                        throw VorleseFehler(
                            "Google Chirp 3 HD hat abgelehnt (${antwort.code}). " +
                                "Bitte den Schluessel pruefen oder den Anbieter wechseln.",
                        )
                    }
                    inhalt
                }
            } catch (netzfehler: IOException) {
                throw VorleseFehler("Google Chirp 3 HD ist nicht erreichbar.", netzfehler)
            }

            val audioText = JSONObject(antwortText).optString("audioContent")
            if (audioText.isBlank()) {
                throw VorleseFehler("Google Chirp 3 HD hat keine Audiodaten geliefert.")
            }
            Base64.decode(audioText, Base64.DEFAULT)
        }

    override fun schliesse() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private companion object {
        const val GOOGLE_ADRESSE = "https://texttospeech.googleapis.com/v1/text:synthesize"
        val JSON_TYP = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * Wartet abbruchfest auf die Antwort: Wird der Vorlese-Auftrag gestoppt, bricht auch die
 * laufende Anfrage ab. Wird von [GoogleStimme] und [QwenStimme] genutzt.
 */
internal suspend fun Call.antwort(): Response = suspendCancellableCoroutine { fortsetzung ->
    fortsetzung.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (fortsetzung.isActive) fortsetzung.resumeWith(Result.failure(e))
        }

        override fun onResponse(call: Call, response: Response) {
            if (fortsetzung.isActive) fortsetzung.resume(response) else response.close()
        }
    })
}
