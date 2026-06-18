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
Du bist ein strenger Filter. Deine Aufgabe: Prüfe jede Idee und entscheide: Ist das eine GEWOHNHEIT oder eine AUFGABE?

WICHTIGSTE REGEL: Wenn die Idee ein Wiederholungsmuster enthält ("alle X Tage", "regelmäßig", "täglich", "jeden Tag", "immer wieder", "öfter"), dann ist es eine GEWOHNHEIT — antworte mit [] (leeres Array).

Wiederholungsmuster erkennen:
- "alle drei Tage", "alle zwei Tage", "alle X Tage"
- "regelmäßig", "täglich", "wöchentlich", "monatlich"
- "jeden Tag", "jeden Morgen", "jeden Abend"
- "immer wieder", "öfter", "mehrmals"

BEISPIELE:
"Ich möchte alle drei Tage Federball spielen" → [] (GEWOHNHEIT, hat "alle drei Tage")
"Ich möchte regelmäßig alle zwei Tage ein Buch lesen" → [] (GEWOHNHEIT)
"Ich möchte einen Baum pflanzen" → [{"title": "Baum pflanzen", ...}] (AUFGABE, kein Wiederholungsmuster)
"Ich muss morgen Gassi gehen" → [{"title": "Mit Hund Gassi gehen", ...}] (AUFGABE)
"Bild malen" → [{"title": "Bild malen", ...}] (AUFGABE)

NUR wenn KEIN Wiederholungsmuster vorhanden ist, erstelle eine AUFGABE.

Format: ECHTE deutsche Umlaute (ä, ö, ü, ß).
Für jede AUFGABE:
- title: max. 5 Wörter, prägnant
- description: NUR die Infos aus der Idee verwenden, nichts dazufügen
Antworte NUR mit JSON-Array: [{"title": "...", "description": "..."}] oder [].
Keine Einleitung, keine Erklärung.
"""

        private const val HABIT_SYSTEM_PROMPT = """
Du bist ein strenger Filter. Deine Aufgabe: Prüfe jede Idee und entscheide: Ist das eine GEWOHNHEIT oder eine AUFGABE?

WICHTIGSTE REGEL: Wenn die Idee ein Wiederholungsmuster enthält ("alle X Tage", "regelmäßig", "täglich", "jeden Tag", "immer wieder", "öfter"), dann ist es eine GEWOHNHEIT. Sonst ist es eine AUFGABE und du antwortest mit [].

Wiederholungsmuster erkennen:
- "alle drei Tage", "alle zwei Tage", "alle X Tage"
- "regelmäßig", "täglich", "wöchentlich", "monatlich"
- "jeden Tag", "jeden Morgen", "jeden Abend"
- "immer wieder", "öfter", "mehrmals"

BEISPIELE:
"Ich möchte alle drei Tage Federball spielen mit den zwei Mädels" → GEWOHNHEIT
"Ich möchte regelmäßig alle zwei Tage ein Buch lesen" → GEWOHNHEIT
"Ich möchte einen Baum pflanzen" → [] (AUFGABE, kein Wiederholungsmuster)
"Ich muss morgen Gassi gehen" → [] (AUFGABE)

WICHTIG: Nimm ALLE Infos aus der Idee und formuliere sie als Gewohnheit. NICHTS weglassen!
- "Ich möchte alle drei Tage Federball spielen mit den zwei Mädels, die ich kennengelernt habe"
→ "Ich gehe alle drei Tage Federball spielen mit den zwei Mädels, die ich kennengelernt habe."

Format: ECHTE deutsche Umlaute (ä, ö, ü, ß).
Für jede GEWOHNHEIT: Ein Satz im Ich-Format. ALLE Infos aus der Idee übernehmen, nichts weglassen.
Antworte NUR mit JSON-Array: ["Gewohnheit 1", "Gewohnheit 2"] oder [].
Keine Einleitung, keine Erklärung.
"""
    }
}
