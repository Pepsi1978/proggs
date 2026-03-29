package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.util.Constants
import javax.inject.Inject

class SummarizeEntryUseCase @Inject constructor(
    private val geminiApi: GeminiApi,
    private val journalRepository: JournalRepository,
    private val encryptedPrefs: SharedPreferences
) {
    private fun buildPrompt(text: String): String = """
Du erhältst einen Tagebucheintrag. Fasse ihn in 2-4 kurzen Stichpunkten zusammen.

REGELN:
- Jeder Stichpunkt beginnt mit "• " (Aufzählungszeichen + Leerzeichen)
- Jeder Stichpunkt ist maximal eine Zeile lang
- Kurz und prägnant — keine ganzen Sätze, nur Kernaussagen
- Behalte die wichtigsten Themen und Emotionen bei
- Sprache: Deutsch
- Gib NUR die Stichpunkte zurück, nichts anderes

TEXT:
$text
    """.trim()

    suspend operator fun invoke(entryId: Long, displayText: String) {
        val apiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
        if (apiKey.isBlank()) return

        try {
            val model = encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL)
                ?: Constants.DEFAULT_GEMINI_MODEL
            val request = GeminiRequestBuilder.build(
                userText = buildPrompt(displayText),
                temperature = 0.3f,
                maxOutputTokens = 512
            )
            val response = geminiApi.generateContent(model = model, apiKey = apiKey, request = request)
            val summary = response.extractText()?.trim() ?: return

            val entry = journalRepository.getEntryById(entryId) ?: return
            journalRepository.updateEntry(entry.copy(summary = summary))
        } catch (_: Exception) {
            // Summary is optional — silently skip on error
        }
    }
}
