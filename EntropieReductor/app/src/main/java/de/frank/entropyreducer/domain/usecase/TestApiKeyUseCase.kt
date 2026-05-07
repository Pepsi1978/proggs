package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** "Verbindung testen"-Buttons in Settings → API-Schluessel. */
class TestApiKeyUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val settings: AppSettings,
) {

    /**
     * Ein winziger Gemini-Roundtrip: "Antworte mit OK" → erfolgreich, wenn HTTP 200.
     * Fuer Whisper haben wir keinen "list models"-Endpunkt, also testen wir
     * dort durch einen 1-Sek-Stille-Upload bei Bedarf separat.
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
}
