package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.repository.ScientistRepository
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
 * Eine Runde im Wissenschaftler-Dialog (Spec §12.4).
 *
 * Eingabe: aktuelle Session-ID + Nutzer-Text (oder null für den Kickoff in einer leeren Session).
 * - System-Prompt nach §7 + §12.4 zusammenbauen.
 * - Komplette Session-Historie als contents anhaengen (alternierend user/model).
 * - Antwort parsen: narrativer Text → in DB als ScientistMessage.KI;
 *   Hypothesen → als VORGESCHLAGEN persistieren und IDs an die Message haengen;
 *   MEMORY-VORSCHLAG-Zeilen → als MemoryEntryEntity (KI_VORSCHLAG, isActive=false).
 */
class ScientistChatUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val prompts: PromptRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val insightDao: InsightDao,
    private val scientist: ScientistRepository,
    private val hypotheses: HypothesisRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val hypothesisParser: HypothesisParser,
) {

    /**
     * Persistiert die Nutzer-Nachricht (falls nicht null), fragt Gemini, persistiert die KI-Antwort
     * inkl. abgeleiteter Hypothesen + Memory-Vorschlaege. Gibt die KI-Message zurück.
     */
    suspend operator fun invoke(
        sessionId: String,
        userText: String?,
    ): Result<ScientistMessageEntity> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()

        // 1. Nutzer-Message persistieren (sofern vorhanden)
        if (!userText.isNullOrBlank()) {
            scientist.insertMessage(
                ScientistMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = ScientistRole.NUTZER,
                    content = userText.trim(),
                    createdAt = System.currentTimeMillis(),
                    attachedHypothesisIds = emptyList(),
                ),
            )
            scientist.touchSession(sessionId)
        }

        // 2. Kontext-Blocks für System-Prompt
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
            .mapNotNull { extractResolutionMethod(it.aiNotes) }
            .take(10)
        // Archiv-Eintraege fuer Mustererkennung (Frank-Wunsch 2026-05-09): die
        // letzten 50 archivierten Aufgaben mit ihrer Methode liefern dem Forscher
        // Stoff fuer Hypothesen ueber wiederkehrende Aufgabentypen, dauerhaft
        // wirksame Methoden und saisonale Verlaeufe.
        val archivedEntries = entries.getArchived().first().take(50)
        val archivedSummaries = archivedEntries.map { entry ->
            val resolvedTime = entry.resolvedAt ?: entry.updatedAt
            val dateLabel = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(resolvedTime))
            SystemPromptBuilder.ArchivedMethodSummary(
                title = entry.title,
                category = entry.category.name,
                method = extractResolutionMethod(entry.aiNotes) ?: "",
                dateLabel = dateLabel,
            )
        }

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = biomarkerLatest,
            calendarToday = calendarToday,
            calendarTomorrow = calendarTomorrow,
            userPrompts = activePrompts,
            recentResolutionMethods = resolutionMethods,
            archivedResolutionEntries = archivedSummaries,
            activeHypotheses = activeHypotheses,
            tail = buildString {
                appendLine(TAIL_INSTRUCTION.trim())
                if (activeEntries.isNotEmpty()) {
                    appendLine()
                    appendLine("## Aktive Eintraege (${activeEntries.size})")
                    activeEntries.take(40).forEach {
                        appendLine("- [${it.category.name} sev=${it.severity}] ${it.title}: ${it.description}")
                    }
                }
                if (confirmedInsights.isNotEmpty()) {
                    appendLine()
                    appendLine("## Bestätigte Insights")
                    confirmedInsights.forEach {
                        appendLine("- ${it.title} (conf=${it.confidence}): ${it.description}")
                    }
                }
            },
        )

        // 3. Historie als contents (alternierend user/model)
        val history = scientist.observeMessages(sessionId).first()
        val contents = mutableListOf<GeminiContent>()
        history.forEach { msg ->
            val role = if (msg.role == ScientistRole.NUTZER) "user" else "model"
            contents += GeminiContent(role = role, parts = listOf(GeminiPart(msg.content)))
        }
        if (history.isEmpty()) {
            // Kickoff: trigger an Gemini, ohne sichtbare Nutzer-Nachricht.
            contents += GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(KICKOFF_TRIGGER)),
            )
        }

        // 4. Aufruf
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

            val parsed = hypothesisParser.parse(raw)

            // 4a. Hypothesen persistieren
            val now = System.currentTimeMillis()
            val hypothesisIds = parsed.hypotheses.map { p ->
                val id = UUID.randomUUID().toString()
                val plannedStart = now
                val plannedEnd = now + p.plannedDurationDays * 24L * 60 * 60 * 1000
                hypotheses.upsert(
                    HypothesisEntity(
                        id = id,
                        title = p.title,
                        description = p.description,
                        rationale = p.rationale,
                        createdAt = now,
                        plannedStartDate = plannedStart,
                        plannedEndDate = plannedEnd,
                        actualStartDate = null,
                        actualEndDate = null,
                        status = HypothesisStatus.VORGESCHLAGEN,
                        outcome = null,
                        outcomeNotes = null,
                        biomarkerBeforeId = null,
                        biomarkerAfterId = null,
                        felltEntropyChange = null,
                        relatedEntryIds = emptyList(),
                    ),
                )
                id
            }

            // 4b. Memory-Vorschlaege persistieren
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

            // 4c. KI-Message speichern (Narrativ — Hypothesen-Karten werden anhand der IDs gerendert)
            val msg = ScientistMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = ScientistRole.KI,
                content = parsed.narrative.ifBlank { raw },
                createdAt = now,
                attachedHypothesisIds = hypothesisIds,
            )
            scientist.insertMessage(msg)
            scientist.touchSession(sessionId)
            Result.success(msg)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    companion object {
        /**
         * Extrahiert die "Methode:"-Anteile aus dem aiNotes-Feld eines Eintrags.
         * Format der Notiz: "Methode: <Text>" (kann angehaengt werden, daher mehrere
         * "Methode:"-Vorkommen moeglich). Gibt den juengsten/letzten Methode-Text zurueck,
         * oder null wenn keine "Methode:"-Markierung vorhanden ist.
         */
        internal fun extractResolutionMethod(aiNotes: String?): String? {
            if (aiNotes.isNullOrBlank()) return null
            val regex = Regex("""Methode:\s*(.+?)(?=\n\s*Methode:|$)""", RegexOption.DOT_MATCHES_ALL)
            val matches = regex.findAll(aiNotes).map { it.groupValues[1].trim() }.toList()
            return matches.lastOrNull()?.takeIf { it.isNotBlank() }?.take(300)
        }

        private const val BASE_PROMPT = """
Spezifische Aufgabe: Du arbeitest mit Frank im offenen Dialog. Dein Ziel in jedem Beitrag:

1. Reflektiere kurz, was Frank zuletzt gesagt hat (max. 2 Saetze).
2. Formuliere optional eine oder mehrere konkrete neue Hypothesen oder Experimente, die Frank wahrscheinlich noch nicht ausprobiert hat. Markiere jede explizit als „[HYPOTHESE]" mit Titel, Beschreibung, Begruendung, vorgeschlagener Dauer in Tagen.
3. Stelle EINE meta-intelligente Folgefrage — eine Frage, die dir neue Information über Frank erschliesst, die du noch nicht hast.
4. Falls du erkennst, dass etwas Wahres dauerhaft Wert hat als Memory, kennzeichne es mit „[MEMORY-VORSCHLAG]: <Inhalt>".
        """
        private const val TAIL_INSTRUCTION = """
Format der Hypothese (innerhalb deiner Antwort):
[HYPOTHESE]
Titel: <kurz>
Beschreibung: <2-3 Saetze>
Begruendung: <1-2 Saetze, mit Bezug zu konkreten Eintraegen oder Biomarkern>
Geplante Dauer: <Anzahl> Tage
[/HYPOTHESE]

Sprache: Deutsch. Tonfall: neugierig, scharfsinnig, wertschaetzend. Keine Emojis. Reine Fliesstext-Absaetze ausserhalb der HYPOTHESE-Bloecke.
        """
        private const val KICKOFF_TRIGGER =
            "Beginne die Session. Eroeffne mit einer scharfen Beobachtung aus den vorhandenen Eintraegen, Biomarkern oder Mustern und stelle deine erste meta-intelligente Frage. Optional eine erste Hypothese."
    }
}
