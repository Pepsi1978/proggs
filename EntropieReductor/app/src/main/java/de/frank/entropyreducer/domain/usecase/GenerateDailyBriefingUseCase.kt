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
import de.frank.entropyreducer.domain.model.EntryStatus
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
        // Seit 2026-07-07 sind Aufgabenbereiche Prioritätsbereiche. Das Briefing nimmt daher die
        // fünf effektiv höchstpriorisierten aktiven Aufgaben statt eines alten Heute-Buckets.
        //
        // NUR aktive Aufgaben — erledigte (REDUZIERT) und
        // archivierte (ARCHIVIERT) Aufgaben gehoeren NICHT ins Briefing. Vorher zitierte das
        // Briefing eine bereits laengst erledigte Aufgabe weil getByBucket() nur ARCHIVIERT
        // ausschliesst, nicht aber REDUZIERT (= "Entropie reduziert" = erledigt). Filter ergaenzt
        // damit nur OFFEN und IN_ARBEIT durchkommen.
        val todayEntries = entries.getActive().first()
            .filter { it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT }
            .sortedByDescending { it.manualPriorityScore ?: it.priorityScore }
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
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
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
            appendLine("Bevorstehende Termine aus dem Kalender (heute + nächste 2 Tage):")
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
        appendLine("=== HEUTIGE AKTIVE AUFGABEN — DAS IST DIE GESAMTE LISTE FUER DAS BRIEFING ===")
        if (entries.isEmpty()) {
            appendLine("(Keine aktiven Aufgaben fuer heute eingeplant — Frank hat heute kein offenes To-Do im HEUTE-Bucket. Mache das Briefing OHNE Aufgaben-Bezug.)")
        } else {
            appendLine("Genau ${entries.size} aktive Aufgabe(n) im HEUTE-Bucket. KEINE weiteren existieren fuer dieses Briefing — alles andere wurde bewusst herausgefiltert (siehe Tabu-Liste unten).")
            appendLine()
            entries.forEachIndexed { i, e ->
                appendLine("${i + 1}. [${e.category}] ${e.title} (Prio ${e.priorityScore.toInt()}): ${e.description}")
            }
        }
        appendLine()
        appendLine("=== TABU-LISTE — ueber DIESE Inhalte schweigst du im Briefing absolut ===")
        appendLine("- Aufgaben aus dem MORGEN-Bucket (auch wenn sie morgen wichtig werden)")
        appendLine("- Aufgaben aus dem FREIBLOCK-Bucket (Backlog ohne Datum)")
        appendLine("- Aufgaben aus dem SPAETER-Bucket (Aufschub-Liste)")
        appendLine("- Erledigte Aufgaben mit Status REDUZIERT (heute schon abgehakt)")
        appendLine("- Archivierte Aufgaben mit Status ARCHIVIERT (geloescht/versteckt)")
        appendLine("- Aufgaben aus aelteren Tagen die vielleicht noch in deiner KI-Erinnerung sind")
        appendLine()
        appendLine("REGEL: Wenn eine Aufgabe NICHT in der nummerierten Liste oben steht, existiert sie fuer dieses Briefing NICHT. Erfinde keine. Erwaehne keine. Spekuliere nicht ueber andere. Auch nicht implizit (\"vielleicht hast du noch...\", \"falls naechste Woche...\"). Das Briefing ist ein reiner Tagesfokus auf die ${entries.size} Aufgabe(n) oben — Punkt.")
    }

    private companion object {
        const val BASE_PROMPT = """
Du bist Frank's persoenliches Genie. Du erzeugst sein Tagesbriefing — kurz, warm, präzise.
Stil: deutsche Fliesstext-Prosa in zweiter Person ("Du"). Keine Floskeln, keine Markdown-
Header, keine Listen. Maximum 7 Saetze.

Inhaltsstruktur:
1. Kontextueller Eroeffnungssatz (Schicht heute / Tageskontext).
2. Biomarker-Verankerung (Recovery, HRV) — was bedeutet das für heute?
3. Was sind heute die wichtigsten 2-3 Aufgaben aus den HEUTE-Aufgaben und WARUM in dieser Reihenfolge? (NUR die explizit genannten heutigen Aufgaben — keine anderen.)
4. Eine Hypothese oder Beobachtung — was könnte heute klappen?
5. Abschliessender Satz: eine sanfte Frage oder ein Vertrauenshinweis.

UNVERHANDELBARE TABUS — verletzen disqualifiziert dich:
- Du sprichst NIEMALS ueber Aufgaben, die nicht in der vom User mitgeschickten nummerierten "HEUTIGE AKTIVE AUFGABEN"-Liste stehen.
- Du erwaehnst KEINE Aufgaben aus den Buckets MORGEN, FREIBLOCK oder SPAETER — auch nicht beilaeufig oder als Vergleich.
- Du erwaehnst KEINE bereits erledigten (REDUZIERT) oder archivierten (ARCHIVIERT) Aufgaben — die sind durch.
- Du erfindest KEINE zusaetzlichen Aufgaben aus deinem KI-Vorwissen oder aus frueheren Briefings.
- Du paraphrasierst die Aufgaben nicht so weit, dass sie wie andere klingen — bleib bei dem was die Liste sagt.

Das Briefing ist ein reiner Tagesfokus auf die heute aktiven Aufgaben, kein Wochenueberblick und kein Backlog-Check.
"""
        const val TAIL_INSTRUCTION = """
Antworte ausschliesslich mit dem Briefing-Text. Keine Praeambel, kein Schluss-Disclaimer.
"""
    }
}
