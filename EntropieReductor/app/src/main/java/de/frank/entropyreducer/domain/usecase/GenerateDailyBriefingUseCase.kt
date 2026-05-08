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
import de.frank.entropyreducer.domain.model.TimeBucket
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
        // Frank-Wunsch 2026-05-09: Briefing soll sich AUSSCHLIESSLICH auf die 5 HEUTE-Aufgaben
        // konzentrieren — nicht auf alle aktiven Eintraege. Andere Buckets (MORGEN, FREIBLOCK,
        // SPAETER) sind fuer das Tagesbriefing irrelevant. autoBalanceBuckets sorgt dafuer dass
        // der HEUTE-Bucket maximal 5 Eintraege haelt; das take(5) hier ist redundant aber sicher.
        val todayEntries = entries.getByBucket(TimeBucket.HEUTE).first()
            .sortedByDescending { it.priorityScore }
            .take(5)
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

        val userPayload = buildUserPayload(todayEntries, latestBiomarker, todayDay, tomorrowDay, upcomingEvents)

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
        appendLine("Heutige Aufgaben (HEUTE-Bucket, max 5, sortiert nach Prioritaet — alle anderen Buckets sind fuer das Briefing IRRELEVANT):")
        if (entries.isEmpty()) {
            appendLine("(Keine Aufgaben fuer heute eingeplant)")
        } else {
            entries.forEachIndexed { i, e ->
                appendLine("${i + 1}. [${e.category}] ${e.title} (Prio ${e.priorityScore.toInt()}): ${e.description}")
            }
        }
        appendLine()
        appendLine("WICHTIG: Beziehe dich im Briefing AUSSCHLIESSLICH auf die oben genannten heutigen Aufgaben. Andere Aufgaben (Morgen, Freiblock, Spaeter) existieren — sind heute aber nicht relevant. Schweige darueber.")
    }

    private companion object {
        const val BASE_PROMPT = """
Du bist Frank's persoenliches Genie. Du erzeugst sein Tagesbriefing — kurz, warm, praezise.
Stil: deutsche Fliesstext-Prosa in zweiter Person ("Du"). Keine Floskeln, keine Markdown-
Header, keine Listen. Maximum 7 Saetze.

Inhaltsstruktur:
1. Kontextueller Eroeffnungssatz (Schicht heute / Tageskontext).
2. Biomarker-Verankerung (Recovery, HRV) — was bedeutet das für heute?
3. Was sind heute die wichtigsten 2-3 Aufgaben aus den HEUTE-Aufgaben und WARUM in dieser Reihenfolge? (NUR die explizit genannten heutigen Aufgaben — keine anderen.)
4. Eine Hypothese oder Beobachtung — was koennte heute klappen?
5. Abschliessender Satz: eine sanfte Frage oder ein Vertrauenshinweis.

Sprich NIEMALS ueber Aufgaben, die nicht in der heutigen Liste stehen. Das Briefing ist ein Tagesfokus-Werkzeug, kein Wochenueberblick.
"""
        const val TAIL_INSTRUCTION = """
Antworte ausschliesslich mit dem Briefing-Text. Keine Praeambel, kein Schluss-Disclaimer.
"""
    }
}
