package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.data.repository.EntryFollowUpRepository
import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.util.Constants
import com.entropyjournal.util.stripEmDashes
import javax.inject.Inject

class SummarizeEntryUseCase
@Inject
constructor(
    private val geminiApi: GeminiApi,
    private val journalRepository: JournalRepository,
    private val entryFollowUpRepository: EntryFollowUpRepository,
    private val encryptedPrefs: SharedPreferences,
) {
    companion object {
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
- Sprache: Deutsch
- Gib NUR das Format oben zurück, nichts anderes

${Constants.NO_EM_DASH_RULE}

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

    /** Builds the combined text (main entry + follow-ups) for summarization. */
    private suspend fun buildCombinedText(entryId: Long, primaryText: String): String {
        val followUps = entryFollowUpRepository.getForEntryOnce(entryId).sortedBy { it.createdAt }
        if (followUps.isEmpty()) return primaryText
        val builder = StringBuilder(primaryText.trim()).append("\n")
        followUps.forEachIndexed { index, fu ->
            val chosen =
                if (fu.isImproved && !fu.improvedText.isNullOrBlank()) fu.improvedText
                else fu.rawText.ifBlank { fu.text }
            builder.append("\n[Nachtrag ${index + 1}]: ${chosen.trim()}")
        }
        return builder.toString().trim()
    }

    suspend operator fun invoke(entryId: Long, displayText: String) {
        val apiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
        if (apiKey.isBlank()) return

        try {
            val combinedText = buildCombinedText(entryId, displayText)
            val model =
                Constants.resolveValidModel(encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL))
            val request =
                GeminiRequestBuilder.build(
                    userText = buildPrompt(combinedText),
                    temperature = 0.3f,
                    maxOutputTokens = 1024,
                )
            val response =
                geminiApi.generateContent(model = model, apiKey = apiKey, request = request)
            val result = response.extractText()?.trim()?.stripEmDashes() ?: return

            // Parse title and summary from response
            val lines = result.lines()
            val titleLine = lines.firstOrNull { it.startsWith("TITEL:") }
            val rawTitle = titleLine?.removePrefix("TITEL:")?.trim()
            val title = rawTitle?.let { clampTitle(it) }
            val summaryLines = lines.filter { it.trimStart().startsWith("•") }
            val summary = summaryLines.joinToString("\n").trim().ifBlank { null }

            val entry = journalRepository.getEntryById(entryId) ?: return
            journalRepository.updateEntry(entry.copy(summary = summary, title = title))
        } catch (_: Exception) {
            // Summary is optional — silently skip on error
        }
    }
}
