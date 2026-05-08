package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
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
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

/**
 * Wandelt einen rohen Transkript-Text in einen strukturierten EntropyEntry um.
 * Spec §9.1.
 */
class ProcessEntryUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val memories: MemoryRepository,
    private val prompts: PromptRepository,
    private val entries: EntryRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val json: Json,
) {

    suspend operator fun invoke(
        rawTranscript: String,
        source: EntrySource,
    ): Result<EntropyEntryEntity> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()
        val activePrompts = prompts.getActive().first()

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

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart("Hier ist meine gesprochene Notiz, transkribiert: $rawTranscript")),
                        ),
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4,
                        responseMimeType = "application/json",
                    ),
                ),
            )

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.let { stripMarkdownCodeFence(it) }

            val parsed = rawJson?.let {
                runCatching {
                    json.decodeFromString(StructuredEntryDto.serializer(), it)
                }.getOrNull()
            }

            val entry = if (parsed != null) {
                val now = System.currentTimeMillis()
                EntropyEntryEntity(
                    id = UUID.randomUUID().toString(),
                    rawTranscript = rawTranscript,
                    title = parsed.title.take(60),
                    description = parsed.description,
                    category = runCatching { EntropyCategory.valueOf(parsed.category) }
                        .getOrDefault(EntropyCategory.SONSTIGES),
                    severity = parsed.severity.coerceIn(1, 10),
                    priorityScore = parsed.priorityScore.coerceIn(0.0, 100.0),
                    priorityReason = parsed.priorityReason,
                    status = EntryStatus.OFFEN,
                    timeBucket = runCatching { TimeBucket.valueOf(parsed.timeBucket) }
                        .getOrDefault(TimeBucket.HEUTE),
                    estimatedDurationMinutes = parsed.estimatedDurationMinutes,
                    createdAt = now,
                    updatedAt = now,
                    resolvedAt = null,
                    tags = parsed.tags,
                    aiNotes = parsed.aiNotes,
                    source = source,
                    biomarkerSnapshotId = null,
                )
            } else {
                fallbackEntry(rawTranscript, source)
            }
            entries.upsert(entry)
            Result.success(entry)
        } catch (t: Throwable) {
            // Fallback-Eintrag mit OFFEN-Status speichern, damit der Nutzer ihn
            // später erneut bewerten lassen kann (Spec §19).
            val fallback = fallbackEntry(rawTranscript, source)
            entries.upsert(fallback)
            Result.failure(t)
        }
    }

    private fun fallbackEntry(transcript: String, source: EntrySource): EntropyEntryEntity {
        val now = System.currentTimeMillis()
        return EntropyEntryEntity(
            id = UUID.randomUUID().toString(),
            rawTranscript = transcript,
            title = transcript.take(60).ifBlank { "Eintrag" },
            description = transcript.ifBlank { "(leeres Transkript)" },
            category = EntropyCategory.SONSTIGES,
            severity = 5,
            priorityScore = 50.0,
            priorityReason = "KI-Verarbeitung fehlgeschlagen — bitte erneut bewerten lassen.",
            status = EntryStatus.OFFEN,
            timeBucket = TimeBucket.HEUTE,
            estimatedDurationMinutes = null,
            createdAt = now,
            updatedAt = now,
            resolvedAt = null,
            tags = listOf("parse_fehler"),
            aiNotes = null,
            source = source,
            biomarkerSnapshotId = null,
        )
    }

    private fun stripMarkdownCodeFence(s: String): String {
        val trimmed = s.trim()
        return when {
            trimmed.startsWith("```json") -> trimmed.removePrefix("```json").removeSuffix("```").trim()
            trimmed.startsWith("```") -> trimmed.removePrefix("```").removeSuffix("```").trim()
            else -> trimmed
        }
    }

    @Serializable
    data class StructuredEntryDto(
        val title: String,
        val description: String,
        val category: String,
        val severity: Int,
        val priorityScore: Double,
        val priorityReason: String,
        val timeBucket: String,
        val estimatedDurationMinutes: Int? = null,
        val tags: List<String> = emptyList(),
        val aiNotes: String? = null,
    )

    /**
     * Re-Bewertung eines bestehenden Eintrags mit der aktuellen priorityScore-Doktrin.
     * Frank-Wunsch 2026-05-09: nach Aenderung der Bewertungsregeln (Entropie-
     * Reduktions-Skala in 5 Farbbereichen) sollen alle bestehenden Aufgaben mit
     * der neuen Logik neu eingestuft werden, damit die farbige Prio-Zahl auch
     * bei alten Eintraegen stimmt. Es wird AUSSCHLIESSLICH priorityScore und
     * priorityReason aktualisiert — Title, Beschreibung, Kategorie, Tags und
     * timeBucket bleiben unveraendert (der Eintrag ist ja schon strukturiert).
     */
    suspend fun rescoreExisting(entry: EntropyEntryEntity): Result<EntropyEntryEntity> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()
        val activePrompts = prompts.getActive().first()

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = RESCORE_BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = null,
            calendarToday = null,
            calendarTomorrow = null,
            userPrompts = activePrompts,
            tail = RESCORE_TAIL_INSTRUCTION,
        )

        val entrySummary = buildString {
            appendLine("Bestehender Eintrag (zur Neubewertung):")
            appendLine("- Titel: ${entry.title}")
            appendLine("- Beschreibung: ${entry.description}")
            appendLine("- Kategorie: ${entry.category.name}")
            appendLine("- Schwere (severity): ${entry.severity}/10")
            appendLine("- Bisheriger priorityScore: ${entry.priorityScore.toInt()}/100")
            appendLine("- Bisherige Begruendung: ${entry.priorityReason}")
            entry.tags.takeIf { it.isNotEmpty() }?.let {
                appendLine("- Tags: ${it.joinToString(", ")}")
            }
            entry.estimatedDurationMinutes?.let {
                appendLine("- Geschaetzte Dauer: $it min")
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
                            parts = listOf(GeminiPart(entrySummary)),
                        ),
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.2,
                        responseMimeType = "application/json",
                    ),
                ),
            )

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.let { stripMarkdownCodeFence(it) }

            val parsed = rawJson?.let {
                runCatching {
                    json.decodeFromString(RescoreDto.serializer(), it)
                }.getOrNull()
            } ?: return Result.failure(IllegalStateException("Re-Score-Antwort nicht parsebar"))

            val updated = entry.copy(
                priorityScore = parsed.priorityScore.coerceIn(0.0, 100.0),
                priorityReason = parsed.priorityReason,
                updatedAt = System.currentTimeMillis(),
            )
            entries.update(updated)
            Result.success(updated)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    @Serializable
    data class RescoreDto(
        val priorityScore: Double,
        val priorityReason: String,
    )

    companion object {
        private const val PRIORITY_DOCTRINE = """
priorityScore-Doktrin (Frank-Spezifikation 2026-05-09): Der Wert misst, wieviel
persoenliche Entropie durch die Erledigung dieser Aufgabe entfernt wuerde. Je
mehr Entropie weg, desto hoeher der Score. Skala in fuenf bewussten Farbstufen:

  90-100 (Rot, sehr wichtig):    Aufgabe entfernt fast die gesamte aktuelle
                                  persoenliche Entropie auf einmal — das eine
                                  Ding das gerade quer ueber alle Lebensbereiche
                                  blockiert. Selten. Nur vergeben wenn die
                                  Erledigung wirklich vieles auf einmal aufloest.

  80-89  (Rot, sehr wichtig):    Aufgabe entfernt sehr grosse Mengen Entropie
                                  in mehreren Lebensbereichen gleichzeitig.

  60-79  (Orange):                Aufgabe entfernt grosse Entropie in einem
                                  klaren Lebensbereich, plus Streueffekte in
                                  Nachbarbereichen.

  40-59  (Gelb):                  Aufgabe entfernt mittlere Entropie, fokussiert
                                  auf einen klaren Lebensbereich.

  20-39  (Blau):                  Aufgabe entfernt nur kleine Entropie in einem
                                  klaren Bereich. Wichtig aber nicht dringend.

  0-19   (Gruen, geringste Prio): Aufgabe entfernt nur einen winzigen Teil
                                  Entropie in einem ganz speziellen,
                                  abgegrenzten Bereich. Kann lange warten.

Wichtige Regeln:
- Score basiert auf ENTFERNTER Entropie, NICHT auf Schweregrad allein. Eine
  hohe severity bedeutet nicht automatisch hohe priorityScore — wenn die
  Aufgabe nur einen kleinen Bereich beeinflusst, bleibt der Score niedrig
  trotz hoher severity.
- Nutzer-Prompts, Biomarker und Kalender-Verfuegbarkeit modulieren den Score
  nach oben oder unten, aber das Grundprinzip bleibt: ENTROPIE-REDUKTION.
- Aufgaben die nur "nice-to-have" sind oder Lebensqualitaet steigern (ohne
  bestehende Entropie zu reduzieren) bekommen niedrige Scores (Gruen/Blau).
- Im Zweifel lieber konservativer einstufen — Rot soll Frank den Atem
  stocken lassen ("oh, das ist wirklich wichtig"), nicht inflationaer sein.
"""

        private const val BASE_PROMPT =
            "Deine Aufgabe: Wandle die folgende gesprochene Notiz des Nutzers in einen strukturierten Entropie-Eintrag um."

        private val TAIL_INSTRUCTION = """
Antworte AUSSCHLIESSLICH in JSON, ohne Markdown-Codeblock, ohne Einleitung, ohne Schluss:

{
  "title": "Kurzer prägnanter Titel, max. 60 Zeichen",
  "description": "Strukturierte Beschreibung in 1-3 Saetzen",
  "category": "KOERPERLICH | MENTAL | ZEITLICH | EMOTIONAL | GESUNDHEITLICH | UMGEBUNG | SONSTIGES",
  "severity": 1-10,
  "priorityScore": 0.0-100.0,
  "priorityReason": "Begruendung in 1 Satz — bezieht sich konkret auf die Entropie-Reduktion",
  "timeBucket": "HEUTE | MORGEN | FREIBLOCK | SPAETER",
  "estimatedDurationMinutes": null,
  "tags": ["tag1","tag2"],
  "aiNotes": null
}

severity ist die rohe Schwere des Problems (1-10).
$PRIORITY_DOCTRINE
        """.trimIndent()

        private const val RESCORE_BASE_PROMPT =
            "Deine Aufgabe: Bewerte einen bestehenden Entropie-Eintrag NEU nach der aktualisierten priorityScore-Doktrin. Du aenderst KEINE Inhalte, nur priorityScore und priorityReason."

        private val RESCORE_TAIL_INSTRUCTION = """
Antworte AUSSCHLIESSLICH in JSON, ohne Markdown-Codeblock, ohne Einleitung, ohne Schluss:

{
  "priorityScore": 0.0-100.0,
  "priorityReason": "Begruendung in 1 Satz — bezieht sich konkret auf die Entropie-Reduktion"
}

$PRIORITY_DOCTRINE
        """.trimIndent()
    }
}
