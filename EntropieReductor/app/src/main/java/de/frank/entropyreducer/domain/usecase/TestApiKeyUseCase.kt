package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.remote.GoogleTtsApi
import de.frank.entropyreducer.data.remote.GroqWhisperApi
import de.frank.entropyreducer.data.settings.AppSettings
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Echte Verbindungs-Tests für die Settings → API-Schluessel-Sektion.
 * Statt nur "Key vorhanden?" wird ein realer Roundtrip gemacht — bei
 * ungueltigem Key liefert der Server 401/403, bei gueltigem 200.
 */
class TestApiKeyUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val groq: GroqWhisperApi,
    private val tts: GoogleTtsApi,
    private val settings: AppSettings,
) {

    /**
     * Groq-Test: GET /openai/v1/models mit Bearer-Auth.
     * Quelle: console.groq.com/docs/api-reference (Stand 2025).
     */
    suspend fun testGroq(apiKey: String): Result<Unit> = runCatching {
        groq.listModels(bearer = "Bearer $apiKey")
        Unit
    }

    /**
     * Gemini-Test: kleiner generateContent-Call mit "Antworte mit OK".
     * Quelle: ai.google.dev/api/rest/v1beta/models/generateContent.
     */
    suspend fun testGemini(apiKey: String): Result<Unit> = runCatching {
        val model = settings.geminiModelFlow.first()
        gemini.generateContent(
            model = model,
            apiKey = apiKey,
            request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart("Antworte mit OK"))),
                ),
            ),
        )
        Unit
    }

    /**
     * TTS-Test: GET /v1/voices?key={key}&languageCode=de-DE.
     * Quelle: cloud.google.com/text-to-speech/docs/reference/rest/v1/voices/list.
     * API-Key wird als Query-Parameter uebergeben (Standard für Google-Cloud-APIs).
     */
    suspend fun testTts(apiKey: String): Result<Unit> = runCatching {
        tts.listVoices(apiKey = apiKey, languageCode = "de-DE")
        Unit
    }
}
