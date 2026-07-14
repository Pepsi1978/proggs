package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.entities.GenieCodexVersionEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.GenieCodexRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.HypothesisStatus
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Genie-Codex-Synthese (Spec §16.5). Wird sonntags 19:00 oder manuell ausgeloest.
 *
 * Liefert die ID der neu erstellten Codex-Version zurück — oder null bei Fehler.
 */
class GenieCodexSynthesizer @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val prompts: PromptRepository,
    private val insightDao: InsightDao,
    private val hypothesisDao: HypothesisDao,
    private val codex: GenieCodexRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
) {

    suspend operator fun invoke(): Result<String> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()

        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()
        val activePrompts = prompts.getActiveByCategory(de.frank.entropyreducer.domain.model.PromptCategory.CODEX).first()
        val activeEntries = entries.getActive().first()
        val confirmedInsights = insightDao.getConfirmed().first()
        val activeHypotheses = hypothesisDao.getByStatus(HypothesisStatus.AKTIV).first()
        val proposedHypotheses = hypothesisDao.getByStatus(HypothesisStatus.VORGESCHLAGEN).first()

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = null,
            calendarToday = null,
            calendarTomorrow = null,
            userPrompts = activePrompts,
            tail = TAIL_INSTRUCTION,
        )

        val payload = buildString {
            appendLine("Daten der letzten 30 Tage als Eingabe für die Synthese:")
            appendLine()
            appendLine("## Aktive Memory-Eintraege (${activeMemories.size})")
            activeMemories.forEach { appendLine("- ${it.content}") }
            appendLine()
            appendLine("## Bestätigte Insights (${confirmedInsights.size})")
            confirmedInsights.forEach {
                appendLine("- (${it.targetCategory.name}, conf=${it.confidence}) ${it.title}: ${it.description}")
            }
            appendLine()
            appendLine("## Aktive + vorgeschlagene Hypothesen")
            (activeHypotheses + proposedHypotheses).forEach {
                appendLine("- [${it.status.name}] ${it.title}: ${it.description}")
            }
            appendLine()
            appendLine("## Aktive Entropie-Eintraege (${activeEntries.size})")
            activeEntries.take(60).forEach {
                appendLine("- [${it.category.name} sev=${it.severity}] ${it.title}: ${it.description}")
            }
        }

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(payload)))),
                    generationConfig = GeminiGenerationConfig(temperature = 0.6),
                ),
            )
            val md = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (md.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Leere Antwort von Gemini"))
            }
            val now = System.currentTimeMillis()
            val version = GenieCodexVersionEntity(
                id = UUID.randomUUID().toString(),
                content = md,
                createdAt = now,
                basedOnEntryIds = activeEntries.map { it.id },
                basedOnInsightIds = confirmedInsights.map { it.id },
                basedOnMemoryIds = activeMemories.map { it.id },
            )
            codex.insert(version)
            settings.setLastCodexSynthese(now)
            Diag.i(DiagnosticArea.AGENTIC, TAG, "Genie-Codex-Synthese geschrieben (id=${version.id})")
            Result.success(version.id)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.AGENTIC, TAG, "Codex-Synthese fehlgeschlagen", t)
            Result.failure(t)
        }
    }

    companion object {
        private const val TAG = "GenieCodexSynthesizer"
        private const val BASE_PROMPT = """
Du baust eine kompakte Synthese „Was ich aktuell über Frank verstehe", basierend auf den unten angehaengten Daten.
        """
        private const val TAIL_INSTRUCTION = """
Format: deutsches Markdown, 4 Sektionen:

## Frank's Entropie-Muster
Welche Kategorien dominieren strukturell? Welche Trigger habe ich beobachtet?

## Frank's bewaehrte Hebel
Welche Methoden funktionieren reproduzierbar?

## Offene Forschungsfragen
Welche Hypothesen laufen gerade, welche Muster sind noch unklar?

## Mein aktuelles mentales Modell
In 2-3 Saetzen: Was ist mein bester Ueberblick über das System Frank?

Maximum 600 Woerter. Schreib in erster Person aus Sicht des Genies. Direkt, präzise, ohne Schmeichelei.
        """
    }
}
