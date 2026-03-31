package com.bestjournal.app.domain.usecase

import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.data.repository.JournalRepository
import javax.inject.Inject

class SummarizeEntryUseCase @Inject constructor(
    private val firebaseAiService: FirebaseAiService,
    private val journalRepository: JournalRepository
) {
    private fun buildPrompt(text: String): String = """
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
- Sprache: Deutsch
- Gib NUR das Format oben zurück, nichts anderes

TEXT:
$text
    """.trim()

    suspend operator fun invoke(entryId: Long, displayText: String) {
        try {
            val result = firebaseAiService.generateContent(
                prompt = buildPrompt(displayText),
                temperature = 0.3f,
                maxOutputTokens = 512
            )
            val resultText = result.getOrNull()?.trim() ?: return

            // Parse title and summary from response
            val lines = resultText.lines()
            val titleLine = lines.firstOrNull { it.startsWith("TITEL:") }
            val title = titleLine?.removePrefix("TITEL:")?.trim()
            val summaryLines = lines.filter { it.trimStart().startsWith("•") }
            val summary = summaryLines.joinToString("\n").trim().ifBlank { null }

            val entry = journalRepository.getEntryById(entryId) ?: return
            journalRepository.updateEntry(entry.copy(
                summary = summary,
                title = title
            ))
        } catch (_: Exception) {
            // Summary is optional — silently skip on error
        }
    }
}
