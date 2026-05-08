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

    companion object {
        private const val BASE_PROMPT =
            "Deine Aufgabe: Wandle die folgende gesprochene Notiz des Nutzers in einen strukturierten Entropie-Eintrag um."
        private const val TAIL_INSTRUCTION = """
Antworte AUSSCHLIESSLICH in JSON, ohne Markdown-Codeblock, ohne Einleitung, ohne Schluss:

{
  "title": "Kurzer praegnanter Titel, max. 60 Zeichen",
  "description": "Strukturierte Beschreibung in 1-3 Saetzen",
  "category": "KOERPERLICH | MENTAL | ZEITLICH | EMOTIONAL | GESUNDHEITLICH | UMGEBUNG | SONSTIGES",
  "severity": 1-10,
  "priorityScore": 0.0-100.0,
  "priorityReason": "Begruendung in 1 Satz",
  "timeBucket": "HEUTE | MORGEN | FREIBLOCK | SPAETER",
  "estimatedDurationMinutes": null,
  "tags": ["tag1","tag2"],
  "aiNotes": null
}

severity ist die rohe Schwere. priorityScore beruecksichtigt Schwere + alle aktiven Nutzer-Prompts + Biomarker-Status + Kalender-Verfuegbarkeit.
        """
    }
}
