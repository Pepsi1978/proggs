package de.frank.entropyreducer.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.presentation.ideen.IdeenEntry
import de.frank.entropyreducer.presentation.ideen.ideenEntriesFlow
import de.frank.entropyreducer.presentation.mental.Mental
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.json.JSONArray

// ============================================================================
// Datenklassen
// ============================================================================

data class AutoTaskSuggestion(
    val id: String,
    val title: String,
    val description: String,
) {
    companion object {
        fun create(title: String, description: String): AutoTaskSuggestion =
            AutoTaskSuggestion(
                id = java.util.UUID.randomUUID().toString(),
                title = title.take(60),
                description = description.take(500),
            )
    }
}

// ============================================================================
// Use Case: Zentrale Generierung von Aufgaben- und Gewohnheitsvorschlaegen
// ============================================================================

/**
 * Agentic Use Case fuer die automatische Vorschlaggenerierung aus Ideen.
 *
 * Rein stateless — liest Ideen, ruft Gemini auf, parsed die Antwort.
 * Kein DataStore-Zugriff (der bleibt in den ViewModels).
 *
 * Wird aufgerufen:
 *  1. Beim App-Start (via StartupViewModel)
 *  2. Beim Aktualisieren-Button im Aufgaben-Reiter (via TasksViewModel.refreshAll)
 *  3. Beim KI-Button in den jeweiligen Screens (via die bestehenden ViewModels)
 */
@Singleton
class GenerateSuggestionsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) {

    // ========================================================================
    // Task-Vorschlaege generieren
    // ========================================================================

    /**
     * Generiert Aufgabenvorschlaege aus den uebergebenen Ideen.
     * @param ideas Alle Ideen (aus dem DataStore geladen)
     * @param processedIds Bereits verarbeitete Ideen-IDs (werden gefiltert)
     * @return Pair aus (neue Vorschlaege, aktualisierte processedIds)
     */
    suspend fun generateTaskSuggestions(
        ideas: List<IdeenEntry>,
        processedIds: Set<String>,
    ): Result<Pair<List<AutoTaskSuggestion>, Set<String>>> {
        val apiKey = secrets.geminiApiKey
            ?: return Result.failure(IllegalArgumentException("Bitte Gemini-API-Key in den Einstellungen hinterlegen."))

        if (ideas.isEmpty()) return Result.success(emptyList<AutoTaskSuggestion>() to processedIds)

        val newIdeas = ideas.filter { it.id !in processedIds }
        if (newIdeas.isEmpty()) return Result.success(emptyList<AutoTaskSuggestion>() to processedIds)

        val ideenText = newIdeas.joinToString("\n") { "- ${it.text}" }
        val model = settings.geminiModelFlow.first()

        val response = gemini.generateContent(
            model = model,
            apiKey = apiKey,
            request = GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(TASK_SYSTEM_PROMPT)),
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart("Hier sind meine Ideen zur Entropie-Reduktion:\n\n$ideenText")),
                    ),
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.6,
                    responseMimeType = "application/json",
                ),
            ),
        )

        val json = response.candidates
            ?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            ?: return Result.failure(IllegalStateException("Leere Antwort von Gemini"))

        val newSuggestions = parseTaskSuggestionsJson(json)
        val updatedProcessedIds = processedIds + newIdeas.map { it.id }

        return Result.success(newSuggestions to updatedProcessedIds)
    }

    // ========================================================================
    // Gewohnheits-Vorschlaege generieren
    // ========================================================================

    /**
     * Generiert Gewohnheitsvorschlaege aus den uebergebenen Ideen.
     * @param ideas Alle Ideen (aus dem DataStore geladen)
     * @return Liste der Vorschlaege
     */
    suspend fun generateHabitSuggestions(
        ideas: List<IdeenEntry>,
    ): Result<List<Mental>> {
        val apiKey = secrets.geminiApiKey
            ?: return Result.failure(IllegalArgumentException("Bitte Gemini-API-Key in den Einstellungen hinterlegen."))

        if (ideas.isEmpty()) return Result.success(emptyList())

        val ideenText = ideas.joinToString("\n") { "- ${it.text}" }
        val model = settings.geminiModelFlow.first()

        val response = gemini.generateContent(
            model = model,
            apiKey = apiKey,
            request = GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(HABIT_SYSTEM_PROMPT)),
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart("Hier sind meine Ideen:\n\n$ideenText")),
                    ),
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.6,
                    responseMimeType = "application/json",
                ),
            ),
        )

        val json = response.candidates
            ?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            ?: return Result.failure(IllegalStateException("Leere Antwort von Gemini"))

        return Result.success(parseHabitSuggestionsJson(json))
    }

    // ========================================================================
    // Parsing
    // ========================================================================

    private fun parseTaskSuggestionsJson(raw: String): List<AutoTaskSuggestion> {
        val arr = runCatching { JSONArray(raw) }.getOrNull()
            ?: runCatching { JSONArray("[$raw]") }.getOrNull()
            ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").takeIf { it.isNotBlank() }
                    ?: java.util.UUID.randomUUID().toString()
                val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue
                val description = o.optString("description")
                add(AutoTaskSuggestion(id = id, title = title, description = description))
            }
        }
    }

    private fun parseHabitSuggestionsJson(raw: String): List<Mental> {
        val arr = runCatching { JSONArray(raw) }.getOrNull()
            ?: runCatching { JSONArray("[$raw]") }.getOrNull()
            ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val text = arr.optString(i).trim()
                if (text.isNotBlank()) add(Mental.create(text))
            }
        }
    }

    // ========================================================================
    // System-Prompts
    // ========================================================================

    companion object {
        private const val TASK_SYSTEM_PROMPT = """
Rolle und Aufgabe: Du bist ein Coach für Entropie-Reduktion. Analysiere die folgenden Ideen und
erstelle daraus konkrete, umsetzbare Aufgaben die die persönliche Entropie
(Ordnung, Klarheit, Struktur) reduzieren.

Context: Es handelt sich um Ideen in einer Android APP, die zu sinnvollen Aufgaben umgewandelt werden sollen. Aber es dürfen nur Aufgaben erkannt werden, keine Gewohnheiten, Gewohnheiten werden aus den Ideen in einem anderen Workflow behandelt.

Format: Schreibe in leicht lesbaren Deutsch auf dem Niveau von 11. Klasslern ohne zu schwierige Fremdwörter zu benutzen. Sollten Fremdwörter nötig sein, diese in Klammern kurz verständlich erklären.

WICHTIG: Verwende IMMER echte deutsche Umlaute (ä, ö, ü, ß) — niemals ASCII-Ersatzschreibweisen wie ae, oe, ue oder ss. Auch in der Antwort muss jedes Wort echte Umlaute enthalten.

Für jede geeignete Idee erstelle EINE Aufgabe mit:
- title: max. 3 Wörter, prägnant (z.B. "Tiefenreinigung im Badezimmer")
- description: 2–5 Sätze die beschreiben was genau zu tun ist und warum es die
  Entropie reduziert

Regeln:
- Es muss sich um direkte Tätigkeiten handeln, die abgearbeitet werden können, also echte Aufgaben, keine Gewohnheiten. Unterscheide klar, ist das eine sinnvolle Aufgabe oder eine sinnvolle Gewohnheit. Es sollen NUR Aufgaben gewählt werden, denn Gewohnheiten, die regeläßiges Handeln erfordern, werden woanders verarbeitet in der APP.
- Alle sinnvollen Aufgaben die du erkennst
- Jeder Vorschlag ist ein Objekt mit "title" und "description".
- Aufgaben müssen konkret und sofort umsetzbar sein.
- Fokus auf persönliche Entropie-Reduktion jeglicher Art (mental, emotional, Energie, Umgebung, etc.): Ordnung schaffen, Chaos reduzieren, Strukturen
  aufbauen, vermeidbare Komplexität eliminieren, alles was die persönliche Entropie von Menschen senkt.
- Antworte NUR mit einem JSON-Array von Objekten, z.B.:
  [{"title": "Tiefenreinigung Badezimmer", "description": "Alle Oberflächen, Fliesen und Armaturen gründlich reinigen."}]
- Keine Einleitung, keine Erklärung, nur das JSON-Array.
"""

        private const val HABIT_SYSTEM_PROMPT = """
Du bist ein Coach für Gewohnheitsbildung. Analysiere die folgenden Ideen und
identifiziere jene, die sich eignen, daraus eine tägliche oder regelmäßige
Gewohnheit zu machen.

Für jede geeignete Idee formuliere EINEN Gewohnheitsvorschlag als kurzen,
präzisen Satz. Format: "Was ich wann am besten mache" — maximal 3 Zeilen.
Beispiel: "Jeden Morgen nach dem Aufstehen 5 Minuten meditieren, bevor ich
das Handy checke."

Regeln:
- Gib NUR jene Ideen zurück, die wirklich als Gewohnheit umsetzbar sind.
- Maximal 5 Vorschläge.
- Jeder Vorschlag ist ein einzelner String, maximal 3 Zeilen lang.
- Antworte NUR mit einem JSON-Array von Strings, z.B.:
  ["Vorschlag 1", "Vorschlag 2", "Vorschlag 3"]
- Keine Einleitung, keine Erklärung, nur das JSON-Array.
"""
    }
}
