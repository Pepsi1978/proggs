package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.util.Constants
import javax.inject.Inject

class SummarizeEntryUseCase
@Inject
constructor(
    private val geminiApi: GeminiApi,
    private val journalRepository: JournalRepository,
    private val encryptedPrefs: SharedPreferences,
) {
    private fun buildPrompt(text: String): String =
        """
Du erhältst einen Tagebucheintrag. Erstelle:
1. Eine Überschrift in 3-4 Wörtern (erste Zeile)
2. Dann 2-4 Stichpunkte als Zusammenfassung

FORMAT (exakt so):
TITEL: [3-4 Wörter Überschrift]
• [Stichpunkt 1]
• [Stichpunkt 2]
• [Stichpunkt 3]

REGELN:
- Überschrift: Maximal 4 Wörter, fängt den Kern des Eintrags ein
- Stichpunkte: Kurz und prägnant, nur Kernaussagen
- Keine langen Gedankenstriche (—). Nutze Kommas oder Punkte.
- Sprache: Deutsch
- Gib NUR das Format oben zurück, nichts anderes

TEXT:
$text
    """
            .trim()

    suspend operator fun invoke(entryId: Long, displayText: String) {
        val apiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
        if (apiKey.isBlank()) return

        try {
            val model =
                encryptedPrefs.getString(
                    Constants.PREF_GEMINI_MODEL,
                    Constants.DEFAULT_GEMINI_MODEL,
                ) ?: Constants.DEFAULT_GEMINI_MODEL
            val request =
                GeminiRequestBuilder.build(
                    userText = buildPrompt(displayText),
                    temperature = 0.3f,
                    maxOutputTokens = 512,
                )
            val response =
                geminiApi.generateContent(model = model, apiKey = apiKey, request = request)
            val result = response.extractText()?.trim()?.replace("—", ", ") ?: return

            // Parse title and summary from response
            val lines = result.lines()
            val titleLine = lines.firstOrNull { it.startsWith("TITEL:") }
            val title = titleLine?.removePrefix("TITEL:")?.trim()
            val summaryLines = lines.filter { it.trimStart().startsWith("•") }
            val summary = summaryLines.joinToString("\n").trim().ifBlank { null }

            val entry = journalRepository.getEntryById(entryId) ?: return
            journalRepository.updateEntry(entry.copy(summary = summary, title = title))
        } catch (_: Exception) {
            // Summary is optional — silently skip on error
        }
    }
}
