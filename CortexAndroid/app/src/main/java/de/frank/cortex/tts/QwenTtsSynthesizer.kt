package de.frank.cortex.tts

import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class QwenTtsException(message: String) : Exception(message)

/**
 * Spricht mit Franks eigener geklonter Stimme, gehostet bei Alibaba Model Studio.
 *
 * Anders als die anderen Motoren braucht dieser ZWEI Aufrufe: die Synthese-Anfrage antwortet mit
 * einem Link auf die fertige Audiodatei, die anschliessend geladen wird. Sprechtempo und Tonhoehe
 * fehlen mit Absicht — das Modell setzt die Betonung selbst, jede Wiederholung klingt neu gesprochen.
 *
 * Liefert WAV-Bytes; abgespielt werden sie wie die Edge-Haeppchen ueber den Mp3Player (MediaPlayer
 * erkennt das Format am Inhalt, nicht an der Dateiendung).
 */
class QwenTtsSynthesizer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    /**
     * Synthetisiert [text] mit der geklonten Stimme [rawVoiceId] und gibt die WAV-Bytes zurueck.
     * Wirft [QwenTtsException] bei fehlender Konfiguration oder Fehlern der Gegenstelle.
     */
    suspend fun synthesize(text: String, rawVoiceId: String, rawApiKey: String): ByteArray =
        withContext(Dispatchers.IO) {
            // Zweite Verteidigungslinie: vor der Eingabe-Saeuberung gespeicherte Werte tragen sonst
            // noch Leerzeichen mit sich und scheitern mit 401.
            val apiKey = rawApiKey.filterNot(Char::isWhitespace)
            val voiceId = rawVoiceId.filterNot(Char::isWhitespace)
            if (apiKey.isBlank() || voiceId.isBlank()) {
                throw QwenTtsException("Die eigene Stimme ist nicht konfiguriert.")
            }

            val body = JSONObject().apply {
                put("model", MODEL)
                put(
                    "input",
                    JSONObject()
                        .put("text", text)
                        .put("voice", voiceId)
                        .put("language_type", "German")
                )
            }
            val request = Request.Builder()
                .url(SYNTHESIS_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body.toString().toRequestBody(QwenVoiceDirectory.JSON_MEDIA_TYPE))
                .build()

            val audioUrl = client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    CortexLog.error("QwenTts", "synthesize", "Fehler ${response.code}: ${payload.take(300)}")
                    throw QwenTtsException(QwenVoiceEnrollment.readableError(payload, response.code))
                }
                val url = JSONObject(payload)
                    .optJSONObject("output")
                    ?.optJSONObject("audio")
                    ?.optString("url")
                    .orEmpty()
                if (url.isBlank()) throw QwenTtsException("Alibaba hat keinen Ton-Link geliefert.")
                // Der Link kommt als einfaches http, was Android standardmaessig blockiert.
                if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url
            }

            client.newCall(Request.Builder().url(audioUrl).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw QwenTtsException("Der Ton konnte nicht geladen werden (${response.code}).")
                }
                response.body?.bytes() ?: throw QwenTtsException("Der geladene Ton war leer.")
            }
        }

    companion object {
        private const val SYNTHESIS_URL =
            "https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"

        /** Klonen und Sprechen muessen dasselbe Modell nennen, sonst wird die Stimm-Kennung abgelehnt. */
        const val MODEL = "qwen3-tts-vc-2026-01-22"
    }
}
