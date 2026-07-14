package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Erzeugt Wochen- oder Monatsrueckblick (Spec §16.3) als deutsche Fliesstext-Prosa
 * in zweiter Person ("Du"). Vier Absaetze:
 *   1. Was diese Woche / dieser Monat gepraegt hat
 *   2. Welche Entropie der Benutzer reduziert hat
 *   3. Welche Muster das Genie beobachtet hat
 *   4. Eine Frage für die kommende Woche / den kommenden Monat (1 Satz)
 *
 * Hartes Wort-Limit: 350 (Woche) / 700 (Monat). Kein Markdown, kein Emoji.
 */
class GenerateReviewUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val hypothesisDao: HypothesisDao,
    private val insightDao: InsightDao,
    private val systemPromptBuilder: SystemPromptBuilder,
) {

    enum class Range(val days: Int, val maxWords: Int, val label: String) {
        WEEK(7, 350, "Woche"),
        MONTH(30, 700, "Monat"),
    }

    suspend operator fun invoke(range: Range): Result<String> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()

        val now = System.currentTimeMillis()
        val rangeStart = now - range.days * 24L * 60 * 60 * 1000

        val activeEntries = entries.getActive().first()
        val recentEntries = activeEntries.filter { it.createdAt >= rangeStart }
        val biomarkers = biomarkerDao.getRange(rangeStart, now).first()
        val hypothesesAll = hypothesisDao.getAll().first()
        val completedHypotheses = hypothesesAll.filter {
            it.outcome != null && (it.actualEndDate ?: 0L) >= rangeStart
        }
        val newInsights = insightDao.getConfirmed().first().filter { it.createdAt >= rangeStart }

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = buildBasePrompt(range),
            profileText = profile,
            memories = activeMemories,
            biomarker = biomarkers.lastOrNull(),
            calendarToday = null,
            calendarTomorrow = null,
            userPrompts = emptyList(),
            tail = "Antworte ausschliesslich mit dem Rueckblick-Text. Kein Disclaimer.",
        )

        val payload = buildString {
            appendLine("Mein letzter ${range.label}srueckblick — Daten:")
            appendLine()
            appendLine("Eintraege (${recentEntries.size}):")
            recentEntries.take(40).forEach { e ->
                appendLine("- [${e.category}] ${e.title}")
            }
            appendLine()
            appendLine("Biomarker (${biomarkers.size} Snapshots):")
            biomarkers.takeLast(7).forEach { b ->
                appendLine(
                    "- HRV ${b.hrvMs ?: "?"} ms, Recovery ${b.recoveryScore ?: "?"}%, " +
                        "Sleep Performance ${b.sleepPerformance ?: "?"}%",
                )
            }
            appendLine()
            appendLine("Abgeschlossene Hypothesen (${completedHypotheses.size}):")
            completedHypotheses.forEach { h: HypothesisEntity ->
                appendLine(
                    "- ${h.title} -> ${
                        when (h.outcome) {
                            HypothesisOutcome.ERFOLGREICH -> "ERFOLGREICH"
                            HypothesisOutcome.TEILWEISE_ERFOLGREICH -> "TEILWEISE_ERFOLGREICH"
                            HypothesisOutcome.ERFOLGLOS -> "ERFOLGLOS"
                            HypothesisOutcome.UNKLAR -> "UNKLAR"
                            null -> "OFFEN"
                        }
                    }",
                )
            }
            appendLine()
            appendLine("Neue bestätigte Insights (${newInsights.size}):")
            newInsights.forEach { i: InsightEntity ->
                appendLine("- [${i.targetCategory}] ${i.title} (Confidence ${i.confidence})")
            }
        }

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(payload)),
                        ),
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.55),
                ),
            )
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(IllegalStateException("Leere Antwort von Gemini"))
            } else {
                Result.success(text)
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun buildBasePrompt(range: Range): String = """
Du bist Frank's persoenliches Genie. Du verfasst seinen ${range.label}srueckblick.

Stil: deutsche Fliesstext-Prosa, zweite Person ("Du"), warm aber präzise, ohne Floskeln.
Keine Markdown-Header, keine Listen, keine Emojis. Maximum ${range.maxWords} Woerter.

Struktur (4 Absaetze, durch Leerzeile getrennt):
1. Was diese ${range.label} gepraegt hat (1 Absatz).
2. Welche Entropie du reduziert hast (1 Absatz).
3. Welche Muster ich beobachtet habe (1 Absatz).
4. Eine Frage für die kommende ${range.label} (1 Satz).
"""
}
