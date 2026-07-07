package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.data.repository.InsightRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

/**
 * Wenn eine Hypothese abgeschlossen wird (Spec §16.6):
 *  1) Gemini wird gefragt, ob die Hypothese thematisch zu einem existierenden Insight passt.
 *  2) Bei Match: existierender Insight wird mit ConfidenceCalculator neu bewertet.
 *  3) Kein Match + Outcome ERFOLGREICH: neuer Insight mit Default-Werten anlegen.
 *  4) Kein Match + andere Outcomes: nichts tun.
 *
 * Optional kann der Aufrufer das LLM-Matching ueberspringen (z.B. wenn der Nutzer
 * im Detail-Sheet manuell „Daraus einen Insight machen" gewählt hat).
 */
class MatchInsightUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val hypotheses: HypothesisRepository,
    private val insights: InsightRepository,
    private val confidence: ConfidenceCalculator,
    private val json: Json,
) {

    /**
     * Verarbeitet die abgeschlossene Hypothese — egal ob über den Auto-Flow oder
     * manuell ausgeloest. Liefert den Insight zurück, der erstellt/aktualisiert wurde
     * (oder null, wenn nichts angelegt wurde).
     */
    suspend operator fun invoke(
        completed: HypothesisEntity,
        forceCreateNew: Boolean = false,
    ): InsightEntity? {
        // 1. Match versuchen
        val match = if (forceCreateNew) null else findMatch(completed)

        // 2. Insight bestimmen — bei Match die Hypothesen-ID an sourceHypothesisIds anhaengen,
        //    damit kuenftige Confidence-Berechnungen die volle Match-Historie sehen.
        //    Vorher: bei wiederholten Matches wurden frueher gemerkte Hypothesen verloren,
        //    weil sourceHypothesisIds nie aktualisiert wurde — Confidence verfaelschte sich.
        val targetInsight = match?.let {
            it.copy(sourceHypothesisIds = (it.sourceHypothesisIds + completed.id).distinct())
        } ?: run {
            // Kein Match: bei ERFOLGREICH einen neuen anlegen, sonst nichts tun.
            if (completed.outcome != HypothesisOutcome.ERFOLGREICH && !forceCreateNew) return null
            createNewInsight(completed)
        }

        val allCompletedForInsight = collectCompletedHypotheses(targetInsight, completed)
        val updated = confidence.computeForInsight(targetInsight, allCompletedForInsight)
        insights.upsert(updated)
        return updated
    }

    private suspend fun findMatch(h: HypothesisEntity): InsightEntity? {
        val key = secrets.geminiApiKey ?: return null
        val model = settings.geminiModelFlow.first()
        val all = insights.observeAll().first()
        if (all.isEmpty()) return null

        val payload = buildString {
            appendLine("Hypothese: ${h.title}")
            appendLine("Beschreibung: ${h.description}")
            appendLine()
            appendLine("Bestehende Insights:")
            all.forEachIndexed { idx, i ->
                appendLine("[$idx] (${i.targetCategory.name}) ${i.title}: ${i.description}")
            }
            appendLine()
            appendLine("Antworte AUSSCHLIESSLICH mit JSON: {\"matchedIndex\": <Integer oder -1>, \"category\": \"KOERPERLICH|MENTAL|...\"}.")
            appendLine("matchedIndex = -1 wenn keine inhaltliche Uebereinstimmung besteht.")
        }

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(SYSTEM_PROMPT))),
                    contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(payload)))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.2,
                        responseMimeType = "application/json",
                    ),
                ),
            )
            val raw = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return null
            val parsed = runCatching { json.decodeFromString(MatchDto.serializer(), raw) }.getOrNull()
                ?: return null
            if (parsed.matchedIndex < 0 || parsed.matchedIndex >= all.size) return null
            all[parsed.matchedIndex]
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun createNewInsight(h: HypothesisEntity): InsightEntity {
        // Erste Heuristik: Kategorie aus relatedEntryIds nicht zuverlaessig — wir setzen SONSTIGES
        // und ueberlassen es Frank, die Kategorie später zu korrigieren.
        val now = System.currentTimeMillis()
        val insight = InsightEntity(
            id = UUID.randomUUID().toString(),
            title = h.title,
            description = h.description,
            targetCategory = EntropyCategory.SONSTIGES,
            confidence = 30,
            successCount = 0,
            attemptCount = 0,
            avgBiomarkerImpact = null,
            avgFeltImpact = null,
            createdAt = now,
            updatedAt = now,
            sourceHypothesisIds = listOf(h.id),
        )
        insights.upsert(insight)
        return insight
    }

    private suspend fun collectCompletedHypotheses(
        insight: InsightEntity,
        thisOne: HypothesisEntity,
    ): List<HypothesisEntity> {
        val all = hypotheses.observeAll().first()
        val attached = all.filter { it.id in insight.sourceHypothesisIds }
        // Sicherstellen dass die aktuelle drin ist (auch wenn der Insight neu ist)
        val merged = (attached + thisOne).distinctBy { it.id }
        return merged.filter { it.outcome != null }
    }

    @Serializable
    private data class MatchDto(
        val matchedIndex: Int,
        val category: String? = null,
    )

    companion object {
        private const val SYSTEM_PROMPT = """
Du bist ein analytisches Werkzeug, das prueft, ob eine neu abgeschlossene Hypothese inhaltlich zu einem bestehenden Insight passt. Antworte ausschliesslich in JSON wie unten beschrieben.
        """
    }
}
