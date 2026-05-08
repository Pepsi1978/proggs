package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.dao.CalendarEventDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.CalendarEventEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Erzeugt das Tagesbriefing — eine kurze, persoenliche Begruessung am Morgen.
 *
 * Inhalt (5-7 Saetze, deutsch, in zweiter Person, ohne Floskeln):
 *  - 1 Satz Kontext: Schicht heute, kurze Wetter-/Stimmungsverankerung
 *  - 1 Satz Biomarker-Trend (Recovery, HRV)
 *  - 2-3 Saetze: Was die obersten Aufgaben heute sind und WARUM diese
 *    in dieser Reihenfolge sinnvoll sind
 *  - 1 Satz Hypothese / Beobachtung
 *  - 1 abschliessende Frage oder Hinweis
 *
 * Stil: Genie-Identitaet (siehe SystemPromptBuilder), warm aber praezise.
 */
class GenerateDailyBriefingUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val calendarDao: CalendarDayDao,
    private val calendarEventDao: CalendarEventDao,
    private val systemPromptBuilder: SystemPromptBuilder,
) {

    suspend operator fun invoke(): Result<String> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val activeEntries = entries.getActive().first().take(10)
        val latestBiomarker = biomarkerDao.getLatest().first()
        val todayDay = calendarDao.getDay(today.toString()).first()
        val tomorrowDay = calendarDao.getDay(tomorrow.toString()).first()
        // Termine aus Google Calendar für heute + morgen + uebermorgen — damit
        // das Genie sagen kann "Du hast heute 14:00 Arzttermin" oder "Morgen
        // ist Urlaub geplant".
        val nextThreeDays = listOf(today, tomorrow, today.plusDays(2))
        val upcomingEvents = nextThreeDays.flatMap { d ->
            calendarEventDao.getByDate(d.toString()).first()
        }

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = latestBiomarker,
            calendarToday = todayDay,
            calendarTomorrow = tomorrowDay,
            userPrompts = emptyList(),
            tail = TAIL_INSTRUCTION,
        )

        val userPayload = buildUserPayload(activeEntries, latestBiomarker, todayDay, tomorrowDay, upcomingEvents)

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
                    generationConfig = GeminiGenerationConfig(temperature = 0.6),
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
        biomarker: BiomarkerSnapshotEntity?,
        today: CalendarDayEntity?,
        tomorrow: CalendarDayEntity?,
        events: List<CalendarEventEntity>,
    ): String = buildString {
        appendLine("Generiere mein heutiges Tagesbriefing.")
        appendLine()
        appendLine("Heutige Schicht: ${today?.shiftCode ?: "unbekannt"}.")
        appendLine("Morgige Schicht: ${tomorrow?.shiftCode ?: "unbekannt"}.")
        biomarker?.let {
            appendLine(
                "Letzter Biomarker: HRV ${it.hrvMs ?: "?"} ms, Recovery ${it.recoveryScore ?: "?"}%, " +
                    "Sleep Performance ${it.sleepPerformance ?: "?"}%.",
            )
        }
        if (events.isNotEmpty()) {
            appendLine()
            appendLine("Bevorstehende Termine aus dem Kalender (heute + naechste 2 Tage):")
            events.take(20).forEach { ev ->
                val timeLabel = if (ev.allDay) "ganztags" else
                    java.text.DateFormat
                        .getTimeInstance(java.text.DateFormat.SHORT)
                        .format(java.util.Date(ev.startMs))
                appendLine("- ${ev.date} $timeLabel: ${ev.summary}" +
                    (ev.location?.let { " ($it)" } ?: ""))
            }
        }
        appendLine()
        appendLine("Top-Aufgaben (max 10, sortiert nach Prioritaet):")
        if (entries.isEmpty()) {
            appendLine("(Keine offenen Aufgaben)")
        } else {
            entries.forEachIndexed { i, e ->
                appendLine("${i + 1}. [${e.category}] ${e.title} (Prio ${e.priorityScore.toInt()})")
            }
        }
    }

    private companion object {
        const val BASE_PROMPT = """
Du bist Frank's persoenliches Genie. Du erzeugst sein Tagesbriefing — kurz, warm, praezise.
Stil: deutsche Fliesstext-Prosa in zweiter Person ("Du"). Keine Floskeln, keine Markdown-
Header, keine Listen. Maximum 7 Saetze.

Inhaltsstruktur:
1. Kontextueller Eroeffnungssatz (Schicht heute / Tageskontext).
2. Biomarker-Verankerung (Recovery, HRV) — was bedeutet das für heute?
3. Was sind heute die wichtigsten 2-3 Aufgaben und WARUM in dieser Reihenfolge?
4. Eine Hypothese oder Beobachtung — was koennte heute klappen?
5. Abschliessender Satz: eine sanfte Frage oder ein Vertrauenshinweis.
"""
        const val TAIL_INSTRUCTION = """
Antworte ausschliesslich mit dem Briefing-Text. Keine Praeambel, kein Schluss-Disclaimer.
"""
    }
}
