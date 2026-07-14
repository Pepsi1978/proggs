package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.SupplementLogDao
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.KiTriggerRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * KI-Trigger-Engine (Spec §16.2). Lauft Mi+So 11:00.
 *
 * Lasst Gemini bis zu 3 Trigger-Vorschlaege auf Basis der Daten der letzten
 * 30 Tage erzeugen. Antwort als JSON. Pro Vorschlag wird ein neuer
 * KiTriggerEntity mit `isActive=false`, `approvedAt=null` angelegt — die UI
 * zeigt einen Banner "Die KI schlaegt N neue Trigger vor".
 */
class GenerateKiTriggersUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val entries: EntryRepository,
    private val memories: MemoryRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val supplementLogDao: SupplementLogDao,
    private val hypothesisDao: HypothesisDao,
    private val triggerRepo: KiTriggerRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
) {

    suspend operator fun invoke(): Result<Int> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()

        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

        val activeEntries = entries.getActive().first()
        val biomarkers = biomarkerDao.getRange(thirtyDaysAgo, now).first()
        val logs = supplementLogDao.getRange(thirtyDaysAgo, now).first()
        val hypothesesCompleted = hypothesisDao.getAll().first()
            .filter { it.outcome != null && (it.actualEndDate ?: 0L) >= thirtyDaysAgo }

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = biomarkers.lastOrNull(),
            calendarToday = null,
            calendarTomorrow = null,
            userPrompts = emptyList(),
            tail = TAIL_INSTRUCTION,
        )

        val payload = buildString {
            appendLine("Daten der letzten 30 Tage:")
            appendLine()
            appendLine("Eintraege (${activeEntries.size}):")
            activeEntries.take(30).forEach { e ->
                appendLine("- [${e.category}] ${e.title}")
            }
            appendLine()
            appendLine("Biomarker (${biomarkers.size}):")
            biomarkers.takeLast(7).forEach { b ->
                appendLine(
                    "- ${java.time.Instant.ofEpochMilli(b.capturedAt)}: HRV ${b.hrvMs ?: "?"} ms, " +
                        "Recovery ${b.recoveryScore ?: "?"}%, Sleep ${b.sleepPerformance ?: "?"}%",
                )
            }
            appendLine()
            appendLine("Supplement-Logs (${logs.size}):")
            logs.takeLast(20).forEach {
                appendLine("- ${it.stackType} (${if (it.skippedItems.isEmpty()) "vollstaendig" else "ohne ${it.skippedItems.joinToString()}"})")
            }
            appendLine()
            appendLine("Abgeschlossene Hypothesen: ${hypothesesCompleted.size}")
        }

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(payload))),
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.4),
                ),
            )
            val raw = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                .orEmpty()

            val triggers = parseTriggers(raw)
            triggers.forEach { triggerRepo.upsert(it) }
            Result.success(triggers.size)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Robuster JSON-Parser: extrahiert das erste `{ "triggers": [...] }`-Objekt aus
     * der Gemini-Antwort, auch wenn Pre-/Postamble dabei steht. Tolerant gegenueber
     * abgeschnittenem JSON — abgebrochene Eintraege werden uebersprungen.
     */
    private fun parseTriggers(raw: String): List<KiTriggerEntity> {
        val now = System.currentTimeMillis()
        val openingBrace = raw.indexOf('{')
        val closingBrace = raw.lastIndexOf('}')
        if (openingBrace < 0 || closingBrace <= openingBrace) {
            throw IllegalArgumentException("Gemini-Antwort enthaelt kein gueltiges Trigger-JSON")
        }
        val jsonText = raw.substring(openingBrace, closingBrace + 1)
        return try {
            val obj = JSONObject(jsonText)
            val arr = obj.optJSONArray("triggers")
                ?: throw IllegalArgumentException("Gemini-JSON enthaelt kein triggers-Array")
            val out = mutableListOf<KiTriggerEntity>()
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i) ?: continue
                // optString() returnt den String "null" wenn der Wert JSONObject.NULL ist,
                // nicht "". Deshalb explizit per isNull() prüfen.
                // Quelle: developer.android.com/reference/org/json/JSONObject#optString
                val name = if (t.isNull("name")) "" else t.optString("name").trim()
                val condition = if (t.isNull("condition")) "" else t.optString("condition").trim()
                val action = if (t.isNull("proposedAction")) "" else t.optString("proposedAction").trim()
                if (name.isBlank() || condition.isBlank() || action.isBlank()) continue
                out += KiTriggerEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    condition = condition,
                    proposedAction = action,
                    isActive = false,
                    createdAt = now,
                    proposedAt = now,
                    approvedAt = null,
                    triggerCount = 0,
                    lastTriggeredAt = null,
                )
            }
            out
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Gemini-Trigger-JSON ist nicht lesbar", e)
        }
    }

    private companion object {
        const val BASE_PROMPT = """
Du bist Frank's persoenliches Genie. Du beobachtest seine Daten und schlaegst
automatische Trigger vor — Bedingungen, die wenn erfuellt, eine kleine
KI-Aktion ausloesen sollen (z.B. eine Frage am Morgen, eine Prio-Erhoehung).

Antworte AUSSCHLIESSLICH in JSON, kein Markdown, keine Erklärung davor/danach:
{
  "triggers": [
    {
      "name": "Kurzer Name",
      "condition": "Klartext-Bedingung, z.B. 'HRV unter 35 ms am Morgen'",
      "proposedAction": "Was die App tun soll, kurz und konkret",
      "rationale": "Warum dieser Trigger Sinn ergibt, mit Datenbezug",
      "confidence": 75
    }
  ]
}

Schlage maximal 3 Trigger vor und nur solche, die du robust mit den Daten
begruenden kannst. Lieber 0 Trigger als ein schwacher.
"""
        const val TAIL_INSTRUCTION = "Antworte ausschliesslich mit dem JSON-Objekt. Keine Erklärung."
    }
}
