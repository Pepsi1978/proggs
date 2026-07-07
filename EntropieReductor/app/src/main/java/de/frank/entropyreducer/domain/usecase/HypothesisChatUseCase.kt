package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.HypothesisMessageRepository
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.model.ScientistRole
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Eine Runde im Inline-Hypothesen-Dialog. Frank diskutiert mit dem Wissenschaftler innerhalb
 * EINER Hypothese — der Verlauf ist getrennt vom globalen Forscher-Chat. Die KI darf die
 * Hypothese hier auch anpassen (per [HYPOTHESEN-UPDATE]-Block).
 */
class HypothesisChatUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val prompts: PromptRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val insightDao: InsightDao,
    private val hypotheses: HypothesisRepository,
    private val hypothesisMessages: HypothesisMessageRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val hypothesisParser: HypothesisParser,
) {

    suspend operator fun invoke(
        hypothesisId: String,
        userText: String?,
    ): Result<HypothesisMessageEntity> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()

        val hypothesis = hypotheses.get(hypothesisId)
            ?: return Result.failure(IllegalStateException("Hypothese nicht gefunden"))

        // 1. Nutzer-Message persistieren
        if (!userText.isNullOrBlank()) {
            hypothesisMessages.insert(
                HypothesisMessageEntity(
                    id = UUID.randomUUID().toString(),
                    hypothesisId = hypothesisId,
                    role = ScientistRole.NUTZER,
                    content = userText.trim(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }

        // 2. Kontext laden — gleicher Tiefenkontext wie globaler Chat, plus diese Hypothese im Detail
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()
        val activePrompts = prompts.getActiveByCategory(de.frank.entropyreducer.domain.model.PromptCategory.FORSCHER).first()
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val calendarToday = calendarDao.getDay(today).first()
        val calendarTomorrow = calendarDao.getDay(tomorrow).first()
        val biomarkerLatest = biomarkerDao.getLatest().first()
        val confirmedInsights = insightDao.getConfirmed().first()
        val activeEntries = entries.getActive().first()
        val activeHypotheses = hypotheses.observeByStatus(HypothesisStatus.AKTIV).first()
        val fourteenDaysAgo = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        val recentlyResolved = entries.getRecentlyResolved(fourteenDaysAgo).first()
        val resolutionMethods = recentlyResolved
            .mapNotNull { ScientistChatUseCase.extractResolutionMethod(it.aiNotes) }
            .take(10)

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = biomarkerLatest,
            calendarToday = calendarToday,
            calendarTomorrow = calendarTomorrow,
            userPrompts = activePrompts,
            recentResolutionMethods = resolutionMethods,
            activeHypotheses = activeHypotheses,
            tail = buildString {
                appendLine(TAIL_INSTRUCTION.trim())
                appendLine()
                appendLine("## Diese Hypothese im Detail (worüber wir gerade sprechen)")
                appendLine("Titel: ${hypothesis.title}")
                appendLine("Beschreibung: ${hypothesis.description}")
                appendLine("Begruendung: ${hypothesis.rationale}")
                appendLine("Status: ${hypothesis.status.name}")
                hypothesis.outcome?.let { appendLine("Ergebnis: ${it.name}") }
                hypothesis.outcomeNotes?.let { appendLine("Ergebnis-Notiz: $it") }
                appendLine()
                if (activeEntries.isNotEmpty()) {
                    appendLine("## Aktive Eintraege (zum Querbezug)")
                    activeEntries.take(20).forEach {
                        appendLine("- [${it.category.name} sev=${it.severity}] ${it.title}: ${it.description}")
                    }
                    appendLine()
                }
                if (confirmedInsights.isNotEmpty()) {
                    appendLine("## Bestätigte Insights")
                    confirmedInsights.forEach {
                        appendLine("- ${it.title} (conf=${it.confidence}): ${it.description}")
                    }
                }
            },
        )

        // 3. Konversations-Historie dieser Hypothese
        val history = hypothesisMessages.getForHypothesis(hypothesisId)
        val contents = mutableListOf<GeminiContent>()
        history.forEach { msg ->
            val role = if (msg.role == ScientistRole.NUTZER) "user" else "model"
            contents += GeminiContent(role = role, parts = listOf(GeminiPart(msg.content)))
        }
        if (history.isEmpty() || (history.size == 1 && history.first().role == ScientistRole.NUTZER && userText.isNullOrBlank())) {
            // Wenn die Hypothese gerade erst geoeffnet wurde, eroeffnet die KI das Gespraech.
            if (history.isEmpty()) {
                contents += GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(KICKOFF_TRIGGER)),
                )
            }
        }

        // 4. Gemini aufrufen
        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = contents,
                    generationConfig = GeminiGenerationConfig(temperature = 0.6),
                ),
            )

            val raw = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: return Result.failure(IllegalStateException("Leere Antwort von Gemini"))

            val parsed = hypothesisParser.parseUpdate(raw)

            // 4a. Optionales Hypothesen-Update anwenden
            parsed.update?.let { update ->
                val updated = hypothesis.copy(
                    title = update.title ?: hypothesis.title,
                    description = update.description ?: hypothesis.description,
                    rationale = update.rationale ?: hypothesis.rationale,
                    plannedEndDate = update.plannedDurationDays?.let { days ->
                        (hypothesis.actualStartDate ?: hypothesis.plannedStartDate) +
                            days * 24L * 60 * 60 * 1000
                    } ?: hypothesis.plannedEndDate,
                )
                hypotheses.update(updated)
            }

            // 4b. Memory-Vorschlaege als KI_VORSCHLAG (isActive=false) persistieren
            val now = System.currentTimeMillis()
            parsed.memorySuggestions.forEach { content ->
                memories.upsert(
                    MemoryEntryEntity(
                        id = UUID.randomUUID().toString(),
                        content = content,
                        source = MemorySource.KI_VORSCHLAG,
                        isActive = false,
                        confidence = 60,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }

            // 4c. KI-Message persistieren
            val msg = HypothesisMessageEntity(
                id = UUID.randomUUID().toString(),
                hypothesisId = hypothesisId,
                role = ScientistRole.KI,
                content = parsed.narrative.ifBlank { raw },
                createdAt = now,
            )
            hypothesisMessages.insert(msg)
            Result.success(msg)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    companion object {
        private const val BASE_PROMPT = """
Spezifische Aufgabe: Du diskutierst mit Frank EINE konkrete Hypothese im Detail. Dies ist eine fokussierte Diskussion innerhalb dieser Hypothese — kein neuer Hypothesen-Vorschlag.

Dein Ziel:
1. Reflektiere kurz, was Frank zuletzt gesagt hat (max. 2 Saetze).
2. Wenn Franks Beitrag eine inhaltliche Anpassung der Hypothese rechtfertigt (Titel präziser fassen, Beschreibung erweitern, Begruendung klarer machen, Dauer anpassen): Antworte mit einem [HYPOTHESEN-UPDATE]-Block, der nur die zu ändernden Felder enthaelt. Nicht alle Felder müssen drin sein.
3. Stelle EINE meta-intelligente Folgefrage, die das Experiment scharfer/besser macht.
4. Wenn Frank Wissen teilt das dauerhaft Wert hat: kennzeichne es mit [MEMORY-VORSCHLAG]: <Inhalt>.

WICHTIG: Schlage hier KEINE neuen Hypothesen vor (kein [HYPOTHESE]-Block). Der Fokus ist diese eine Hypothese.
        """
        private const val TAIL_INSTRUCTION = """
Format des Updates (optional, nur wenn inhaltliche Anpassung sinnvoll ist):
[HYPOTHESEN-UPDATE]
Titel: <neu, falls geaendert>
Beschreibung: <neu, falls geaendert>
Begruendung: <neu, falls geaendert>
Geplante Dauer: <Anzahl> Tage (falls geaendert)
[/HYPOTHESEN-UPDATE]

Sprache: Deutsch. Tonfall: neugierig, scharfsinnig, wertschaetzend. Keine Emojis. Reine Fliesstext-Absaetze ausserhalb der Marker-Bloecke.
        """
        private const val KICKOFF_TRIGGER =
            "Eroeffne die Diskussion zu dieser Hypothese. Stelle eine scharfe Frage, die das Experiment in Richtung einer guten Durchführung schiebt. Keine neuen Hypothesen vorschlagen — Fokus auf DIESE eine."
    }
}
