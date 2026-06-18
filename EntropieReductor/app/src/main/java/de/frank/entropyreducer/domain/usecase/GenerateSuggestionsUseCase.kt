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

DEFINITIONEN (strikte Trennung):
- GEWOHNHEIT: Etwas das sich WIEDERHOLT. Die Idee muss EXPLIZIT ein Wiederholungsmuster enthalten (z.B. "regelmäßig", "täglich", "jeden Tag", "alle X Tage", "immer wieder", "öfter").
- AUFGABE: Alles andere. Auch wenn es einmalig ist, in der Zukunft liegt oder einen Termin hat.

WANN IST ES EINE AUFGABE? (ALLES was kein Wiederholungsmuster enthält):
- "Baum pflanzen" → AUFGABE
- "Mit Hund Gassi gehen" → AUFGABE
- "Bild malen" → AUFGABE
- "Bücherregal sortieren" → AUFGABE
- "Reinigung machen" → AUFGABE
- "Einkaufen gehen" → AUFGABE

BEISPIELE:
"Ich möchte einen Baum pflanzen" → AUFGABE, title: "Baum pflanzen"
"Ich muss morgen Gassi gehen" → AUFGABE, title: "Mit Hund Gassi gehen"
"Bild von Raumschiff Enterprise malen" → AUFGABE, title: "Bild von Raumschiff Enterprise malen"

WICHTIGSTE REGEL: Wenn du UNSICHER bist, ist es eine AUFGABE. Erfinde NIEMALS "regelmäßig" wenn es nicht explizit im Text steht.

Format: ECHTE deutsche Umlaute (ä, ö, ü, ß).

Für jede AUFGABE:
- title: max. 5 Wörter, prägnant, die Kernaufgabe aus der Idee
- description: 1–3 Sätze, NUR die Infos aus der Idee verwenden, keine neuen erfinden

Regeln:
- NUR direkte Tätigkeiten (einmalig abarbeitbar)
- KEINE neuen Informationen erfinden — nur die Idee in Aufgaben-Form umwandeln
- Keine zusätzlichen Tipps, Begründungen oder Hintergründe erfinden
- Antworte NUR mit JSON-Array: [{"title": "...", "description": "..."}]
- Keine Einleitung, keine Erklärung.
"""

        private const val HABIT_SYSTEM_PROMPT = """
Du bist ein strenger Filter. Deine Aufgabe: Prüfe jede Idee und entscheide: Ist das eine GWOHNHEIT oder eine AUFGABE?

DEFINITIONEN (strikte Trennung):
- GWOHNHEIT: Etwas das sich WIEDERHOLT. Die Idee muss EXPLIZIT ein Wiederholungsmuster enthalten.
- AUFGABE: Etwas das EINMALIG passiert. Auch wenn es in der Zukunft liegt oder ein Termin genannt wird.

WANN IST ES EINE GEWOHNHEIT? (NUR wenn mindestens EIN Wort im Text darauf hinweist):
- "regelmäßig", "täglich", "wöchentlich", "jeden Tag", "jeden Morgen", "jeden Abend"
- "alle zwei Tage", "alle X Tage", "mehrmals pro Woche"
- "immer wieder", "öfter", "von Zeit zu Zeit"
- "jeden Monat", "monatlich", "jährlich"

WANN IST ES EINE AUFGABE? (Alles andere ist eine AUFGABE):
- "Ich möchte einen Baum pflanzen" → AUFGABE (kein Wiederholungsmuster)
- "Ich muss morgen Gassi gehen" → AUFGABE (einmalig, auch mit Termin)
- "Bild malen" → AUFGABE
- "Bücherregal sortieren" → AUFGABE
- "Reinigung machen" → AUFGABE

BEISPIELE:
"Ich möchte regelmäßig alle zwei Tage ein Buch lesen" → GEWOHNHEIT (Wort "regelmäßig")
"Ich pflanze einen Baum im Garten meines Vaters" → AUFGABE (kein Wiederholungsmuster!)
"Jeden Morgen meditieren" → GEWOHNHEIT (Wort "jeden Morgen")
"Ich muss mit dem Hund Gassi gehen" → AUFGABE (kein Wiederholungsmuster)
"Ich will öfter aufräumen" → GEWOHNHEIT (Wort "öfter")

WICHTIGSTE REGEL: Wenn du UNSICHER bist, ist es eine AUFGABE. Erfinde NIEMALS "regelmäßig" wenn es nicht explizit im Text steht.

Format: ECHTE deutsche Umlaute (ä, ö, ü, ß).
Für jede GEWOHNHEIT: Ein Satz im Ich-Format, 1–3 Zeilen.
Antworte NUR mit JSON-Array: ["Gewohnheit 1", "Gewohnheit 2"]
Wenn keine Gewohnheit erkannt wird: antworte mit [] (leeres Array).
Keine Einleitung, keine Erklärung.
"""
    }
}
