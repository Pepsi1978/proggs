package com.bestjournal.app.domain.usecase

import android.content.Context
import android.util.Log
import com.bestjournal.app.R
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.data.repository.EntryFollowUpRepository
import com.bestjournal.app.data.repository.JournalRepository
import com.bestjournal.app.util.DeviceLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay

class SummarizeEntryUseCase
@Inject
constructor(
    private val firebaseAiService: FirebaseAiService,
    private val journalRepository: JournalRepository,
    private val entryFollowUpRepository: EntryFollowUpRepository,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "SummarizeEntry"
        private const val MAX_RETRIES = 5
        const val MAX_TITLE_CHARS = 30
    }

    private fun buildPrompt(text: String): String =
        """
Du erhältst einen Tagebucheintrag, eventuell mit Nachträgen. Erstelle:
1. Eine Überschrift (erste Zeile)
2. Dann 2 bis 10 Stichpunkte als Zusammenfassung, die den Haupteintrag UND alle Nachträge abdecken. Nutze so viele Stichpunkte wie nötig — bei kurzen Einträgen genügen 2-3, bei langen und komplexen Einträgen mit mehreren Nachträgen können es auch 8-10 sein. Lieber ein Stichpunkt mehr als ein wichtiger Aspekt weg.

FORMAT (exakt so, für jeden Stichpunkt eine neue Zeile, beginnend mit •):
TITEL: [Überschrift — max $MAX_TITLE_CHARS Zeichen, inklusive Leerzeichen]
• [Stichpunkt 1]
• [Stichpunkt 2]
... (so viele wie nötig, bis zu 10)

REGELN:
- Überschrift: HART maximal $MAX_TITLE_CHARS Zeichen (inkl. Leerzeichen). Kürzer ist besser. 2-3 Wörter.
- Stichpunkte: Kurz und prägnant, nur Kernaussagen. Berücksichtige Haupttext UND Nachträge vollständig.
- Keine langen Gedankenstriche (—). Nutze Kommas oder Punkte.
- Verwende IMMER den Marker "TITEL:" gefolgt vom Titel, unabhängig von der Antwortsprache
- Gib NUR das Format oben zurück, nichts anderes

${context.getString(R.string.ai_prompt_response_language)}

TEXT:
$text
    """
            .trim()

    /** Truncates the title at a word boundary so it fits in MAX_TITLE_CHARS. */
    private fun clampTitle(raw: String): String {
        val cleaned = raw.trim().replace(Regex("\\s+"), " ")
        if (cleaned.length <= MAX_TITLE_CHARS) return cleaned
        val hardCut = cleaned.take(MAX_TITLE_CHARS)
        val lastSpace = hardCut.lastIndexOf(' ')
        return if (lastSpace > MAX_TITLE_CHARS / 2) hardCut.substring(0, lastSpace).trim()
        else hardCut.trim()
    }

    /**
     * Builds the combined text (main entry + all Nachträge) that gets sent to
     * the model so the summary + title reflect the full picture of that entry,
     * including anything the user added later through follow-ups.
     */
    private suspend fun buildCombinedText(entryId: Long, primaryText: String): String {
        val followUps = entryFollowUpRepository.getForEntryOnce(entryId).sortedBy { it.createdAt }
        if (followUps.isEmpty()) return primaryText
        val builder = StringBuilder(primaryText.trim()).append("\n")
        followUps.forEachIndexed { index, fu ->
            val chosen =
                if (fu.isImproved && !fu.improvedText.isNullOrBlank()) fu.improvedText
                else fu.rawText.ifBlank { fu.text }
            builder.append("\n[Nachtrag ${index + 1}]: ${chosen?.trim()}")
        }
        return builder.toString().trim()
    }

    suspend operator fun invoke(entryId: Long, displayText: String) {
        val combinedText = buildCombinedText(entryId, displayText)
        for (attempt in 1..MAX_RETRIES) {
            try {
                Log.d(TAG, "Generating summary for entry $entryId (attempt $attempt)")
                val result =
                    firebaseAiService.generateContent(
                        prompt = buildPrompt(combinedText),
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
                val rawTitle =
                    lines.firstOrNull { it.startsWith("TITEL:") || it.startsWith("TITLE:") }
                        ?.let { it.removePrefix("TITEL:").removePrefix("TITLE:").trim() }
                val title = rawTitle?.let { clampTitle(it) }
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
}
