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
     * @param processedIds Bereits verarbeitete Ideen-IDs (werden gefiltert)
     * @return Pair aus (neue Vorschlaege, aktualisierte processedIds)
     */
    suspend fun generateHabitSuggestions(
        ideas: List<IdeenEntry>,
        processedIds: Set<String>,
    ): Result<Pair<List<Mental>, Set<String>>> {
        val apiKey = secrets.geminiApiKey
            ?: return Result.failure(IllegalArgumentException("Bitte Gemini-API-Key in den Einstellungen hinterlegen."))

        if (ideas.isEmpty()) return Result.success(emptyList<Mental>() to processedIds)

        val newIdeas = ideas.filter { it.id !in processedIds }
        if (newIdeas.isEmpty()) return Result.success(emptyList<Mental>() to processedIds)

        val ideenText = newIdeas.joinToString("\n") { "- ${it.text}" }
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

        val newSuggestions = parseHabitSuggestionsJson(json)
        val updatedProcessedIds = processedIds + newIdeas.map { it.id }

        return Result.success(newSuggestions to updatedProcessedIds)
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
Rolle und Aufgabe: Du verwandelst Ideen in Aufgaben.

WICHTIGSTE REGEL: Erfinde KEINE neuen Inhalte. Nimm exakt die Information aus der Idee und formuliere sie als Aufgabe. Wenn die Idee wenig Infos enthält, enthält auch die Aufgabe wenig Infos. Erfinde keine zusätzlichen Schritte, keine Begründungen, keine Hintergründe.

Beispiel 1:
Ideе: "Ich muss morgen auf jeden Fall noch mit dem neuen Hund Gassi gehen"
Aufgabe: "Mit dem neuen Hund Gassi gehen"

Beispiel 2:
Ideе: "Ich möchte ein Bild von Raumschiff Enterprise malen, weil ich das so schön finde"
Aufgabe: "Bild von Raumschiff Enterprise malen"
Description: "Ich bin inspiriert von Raumschiff Enterprise und finde es schön."

Beispiel 3:
Ideе: "Bücherregal sortieren"
Aufgabe: "Bücherregal sortieren"

Context: Android-APP, Ideen zu Aufgaben umwandeln. Keine Gewohnheiten — diese werden woanders behandelt.

Format: Deutsch, 11. Klasse, echte Umlaute (ä, ö, ü, ß).

Für jede Aufgabe:
- title: max. 5 Wörter, prägnant, die Kernaufgabe aus der Idee
- description: 1–3 Sätze, NUR die Infos aus der Idee verwenden, keine neuen erfinden

Regeln:
- NUR direkte Tätigkeiten (einmalig abarbeitbar), keine Gewohnheiten
- KEINE neuen Informationen erfinden — nur die Idee in Aufgaben-Form umwandeln
- Keine zusätzlichen Tipps, Begründungen oder Hintergründe erfinden
- Antworte NUR mit JSON-Array: [{"title": "...", "description": "..."}]
- Keine Einleitung, keine Erklärung.
"""

        private const val HABIT_SYSTEM_PROMPT = """
Rolle und Aufgabe: Du verwandelst Ideen in Gewohnheitsvorschläge.

WICHTIGSTE REGEL: Erfinde KEINE neuen Inhalte. Nimm exakt die Information aus der Idee und formuliere sie als Gewohnheit in Ich-Form. Wenn die Idee wenig Infos enthält, enthält auch die Gewohnheit wenig Infos. Erfinde keine zusätzlichen Handlungen, keine Begründungen, keine Hintergründe.

Beispiel 1:
Ideе: "Ich möchte regelmäßig alle zwei Tage ein Buch lesen"
Gewohnheit: "Ich lese alle zwei Tage ein Buch."

Beispiel 2:
Ideе: "Jeden Morgen meditieren"
Gewohnheit: "Jeden Morgen meditiere ich."

Beispiel 3:
Ideе: "Ich will öfter aufräumen"
Gewohnheit: "Ich räume regelmäßig auf."

Context: Android-APP, Ideen zu Gewohnheiten umwandeln. Keine Aufgaben — diese werden woanders behandelt.

Format: Deutsch, 11. Klasse, echte Umlaute (ä, ö, ü, ß).

Für jede Gewohnheit:
- Ein Satz im Ich-Format, 1–3 Zeilen
- NUR die Infos aus der Idee verwenden, keine neuen erfinden

Regeln:
- NUR regelmäßiges, wiederkehrendes Handeln (täglich, wöchentlich usw.)
- KEINE neuen Informationen erfinden — nur die Idee in Gewohnheits-Form umwandeln
- Keine zusätzlichen Tipps, Begründungen oder Hintergründe erfinden
- Maximal 5 Vorschläge
- Jeder Vorschlag soll einen klaren Auslöser/Zeitpunkt enthalten, wenn in der Idee vorhanden
- Antworte NUR mit JSON-Array von Strings: ["Gewohnheit 1", "Gewohnheit 2"]
- Keine Einleitung, keine Erklärung.
"""
    }
}
