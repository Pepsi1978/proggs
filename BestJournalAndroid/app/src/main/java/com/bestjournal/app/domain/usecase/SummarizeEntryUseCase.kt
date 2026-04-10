package com.bestjournal.app.domain.usecase

import android.util.Log
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.data.repository.JournalRepository
import javax.inject.Inject
import kotlinx.coroutines.delay

class SummarizeEntryUseCase
@Inject
constructor(
    private val firebaseAiService: FirebaseAiService,
    private val journalRepository: JournalRepository,
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
        for (attempt in 1..MAX_RETRIES) {
            try {
                Log.d(TAG, "Generating summary for entry $entryId (attempt $attempt)")
                val result =
                    firebaseAiService.generateContent(
                        prompt = buildPrompt(displayText),
                        temperature = 0.3f,
                    )
                val resultText = result.getOrNull()?.trim()?.replace("—", ", ")
                if (resultText == null) {
                    val errorMsg = result.exceptionOrNull()?.message ?: "unknown"
                    if (errorMsg.contains("quota", ignoreCase = true) && attempt < MAX_RETRIES) {
                        val waitSec = parseRetrySeconds(errorMsg)
                        Log.w(TAG, "Rate limited, waiting ${waitSec}s (attempt $attempt)")
                        delay((waitSec * 1000).toLong())
                        continue
                    }
                    Log.e(TAG, "Gemini error: $errorMsg")
                    return
                }

                val lines = resultText.lines()
                val title =
                    lines.firstOrNull { it.startsWith("TITEL:") }?.removePrefix("TITEL:")?.trim()
                val summaryLines = lines.filter { it.trimStart().startsWith("•") }
                val summary = summaryLines.joinToString("\n").trim().ifBlank { null }

                val entry = journalRepository.getEntryById(entryId) ?: return
                journalRepository.updateEntry(entry.copy(summary = summary, title = title))
                Log.d(TAG, "Entry $entryId updated with title='$title'")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed for entry $entryId (attempt $attempt)", e)
                if (attempt < MAX_RETRIES) delay(10_000L)
            }
        }
    }

    private fun parseRetrySeconds(errorMsg: String): Double {
        // Parse "Please retry in 3.41872126s." from Gemini error
        val match = Regex("""retry in (\d+\.?\d*)s""").find(errorMsg)
        return (match?.groupValues?.get(1)?.toDoubleOrNull() ?: 15.0) + 2.0 // +2s safety margin
    }

    companion object {
        private const val TAG = "SummarizeEntry"
        private const val MAX_RETRIES = 5
    }
}
