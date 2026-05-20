package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Erzeugt die strukturierte Markdown-Analyse für Dashboard 2 (Spec §11.2).
 *
 * Gemini bekommt: alle aktiven Eintraege als JSON-aehnliche Liste, aktive Memories,
 * den Biomarker-Trend der letzten 30 Tage, den Schichtkalender der letzten 30 Tage,
 * sowie alle bestaetigten Insights.
 *
 * Ergebnis ist Markdown mit den vier Sektionen Muster / Verborgene Zusammenhaenge /
 * Das große Ganze / Strategische Empfehlungen.
 */
class GenerateAnalysisUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val prompts: PromptRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val insightDao: InsightDao,
    private val systemPromptBuilder: SystemPromptBuilder,
) {

    suspend operator fun invoke(): Result<String> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()

        val activeEntries = entries.getActive().first()
        val activeMemories = memories.getActive().first()
        val activePrompts = prompts.getActiveByCategory(de.frank.entropyreducer.domain.model.PromptCategory.ANALYSE).first()

        // 30-Tage-Fenster
        val now = System.currentTimeMillis()
        val thirtyDaysAgoMs = now - 30L * 24 * 60 * 60 * 1000
        val biomarkers = biomarkerDao.getRange(thirtyDaysAgoMs, now).first()

        val today = LocalDate.now()
        val from = today.minusDays(30).toString()
        val to = today.plusDays(1).toString()
        val calendarDays = calendarDao.getRange(from, to).first()

        val confirmedInsights = insightDao.getConfirmed().first()

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = biomarkers.lastOrNull(),
            calendarToday = calendarDays.firstOrNull { it.date == today.toString() },
            calendarTomorrow = calendarDays.firstOrNull { it.date == today.plusDays(1).toString() },
            userPrompts = activePrompts,
            tail = TAIL_INSTRUCTION,
        )

        val userPayload = buildUserPayload(
            entries = activeEntries,
            biomarkers = biomarkers,
            calendarDays = calendarDays,
            confirmedInsights = confirmedInsights,
        )

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(userPayload)),
                        ),
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.5),
                ),
            )
            val md = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
            if (md.isNullOrBlank()) {
                Result.failure(IllegalStateException("Leere Antwort von Gemini"))
            } else {
                Result.success(md)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun buildUserPayload(
        entries: List<EntropyEntryEntity>,
        biomarkers: List<BiomarkerSnapshotEntity>,
        calendarDays: List<CalendarDayEntity>,
        confirmedInsights: List<InsightEntity>,
    ): String = buildString {
        appendLine("Hier sind meine Daten der letzten 30 Tage.")
        appendLine()

        appendLine("## Aktive Eintraege (${entries.size})")
        entries.forEach { e ->
            appendLine("- [${e.category.name} sev=${e.severity} prio=${e.priorityScore.toInt()} bucket=${e.timeBucket.name}] ${e.title}: ${e.description}")
        }
        appendLine()

        appendLine("## Biomarker-Trend (${biomarkers.size} Snapshots)")
        biomarkers.takeLast(30).forEach { b ->
            val date = java.time.Instant.ofEpochMilli(b.capturedAt)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            appendLine("- $date: HRV=${b.hrvMs?.let { "%.1fms".format(it) } ?: "-"} Recovery=${b.recoveryScore ?: "-"}% Schlaf=${b.sleepPerformance ?: "-"}%")
        }
        appendLine()

        appendLine("## Schichtkalender (${calendarDays.size} Tage)")
        calendarDays.takeLast(30).forEach { d ->
            appendLine("- ${d.date}: ${d.shiftCode.name}")
        }
        appendLine()

        if (confirmedInsights.isNotEmpty()) {
            appendLine("## Bestätigte Insights (${confirmedInsights.size})")
            confirmedInsights.forEach { i ->
                appendLine("- [${i.targetCategory.name} conf=${i.confidence}] ${i.title}: ${i.description}")
            }
        }
    }

    companion object {
        private const val BASE_PROMPT = """
Du erhaeltst alle aktiven Entropie-Eintraege des Nutzers, alle aktiven Memory-Eintraege, den Biomarker-Trendverlauf der letzten 30 Tage und den Schichtkalender der letzten 30 Tage. Deine Aufgabe ist NICHT, Prioritaeten zu setzen — sondern Muster, Cluster und das große Ganze zu erkennen.
        """
        private const val TAIL_INSTRUCTION = """
Antworte in strukturiertem deutschen Markdown mit genau diesen vier Abschnitten:

## Muster
Welche Kategorien dominieren? Welche Themen wiederholen sich?

## Verborgene Zusammenhaenge
Korrelationen zwischen Schichtcode, Biomarkern und Entropie-Eintraegen. Beispiel-Mustertyp: „Mental-Eintraege haeufen sich nach Nachtdienst-Tagen."

## Das große Ganze
Eine prägnante Beobachtung in 2-3 Saetzen: was ist der Kern dessen, was Frank gerade durchlebt?

## Strategische Empfehlungen
Maximal drei Hebel. Jeder als „Hypothese:" oder „Experiment-Vorschlag:" gekennzeichnet, mit Begruendung in 1 Satz.

Sei direkt, konkret, ohne Floskeln. Nutze bestehende Insight-Board-Eintraege, bevor du neue Empfehlungen formulierst.
        """
        @Suppress("unused")
        private val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
