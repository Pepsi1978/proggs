package de.frank.entropyreducer.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.local.entities.OriginType
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.presentation.ideen.IdeenEntry
import de.frank.entropyreducer.presentation.mental.Mental
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================
// Datenklassen
// ============================================================================

data class AutoTaskSuggestion(
    val id: String,
    val title: String,
    val description: String,
    // ID-Architektur Etappe 2d (Frank-Wunsch 2026-06-19): Herkunft = die Quell-Idee.
    // null, wenn die KI keine (gueltige) sourceIndex geliefert hat (Fallback) oder beim Altbestand.
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
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

/**
 * Kombiniertes Ergebnis: Aufgaben UND Gewohnheiten aus EINEM Gemini-Aufruf.
 */
data class SuggestionResult(
    val tasks: List<AutoTaskSuggestion>,
    val habits: List<Mental>,
)

// ============================================================================
// Use Case: Zentrale Generierung von Aufgaben- und Gewohnheitsvorschlaegen
// ============================================================================

/**
 * Agentic Use Case fuer die automatische Vorschlaggenerierung aus Ideen.
 *
 * Verwendet EINEN kombinierten Prompt fuer Aufgaben UND Gewohnheiten.
 * Die KI entscheidet pro Idee exklusiv: Aufgabe oder Gewohnheit.
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

    /**
     * Generiert Aufgaben- UND Gewohnheitsvorschlaege aus den uebergebenen Ideen.
     * EIN Gemini-Aufruf, EIN Prompt, exklusive Entscheidung pro Idee.
     *
     * @param ideas Alle Ideen (aus dem DataStore geladen)
     * @param processedIds Bereits verarbeitete Ideen-IDs (werden gefiltert)
     * @return Pair aus (SuggestionResult, aktualisierte processedIds)
     */
    suspend fun generateSuggestions(
        ideas: List<IdeenEntry>,
        processedIds: Set<String>,
    ): Result<Pair<SuggestionResult, Set<String>>> {
        val apiKey = secrets.geminiApiKey
            ?: return Result.failure(IllegalArgumentException("Bitte Gemini-API-Key in den Einstellungen hinterlegen."))

        if (ideas.isEmpty()) return Result.success(SuggestionResult(emptyList(), emptyList()) to processedIds)

        val newIdeas = ideas.filter { it.id !in processedIds }
        if (newIdeas.isEmpty()) return Result.success(SuggestionResult(emptyList(), emptyList()) to processedIds)

        // ID-Architektur Etappe 2d: Ideen nummeriert (Index in newIdeas), damit die KI pro Vorschlag
        // die Quell-Idee per sourceIndex zurueckgeben kann -> Herkunft Idee -> Vorschlag.
        val ideenText = newIdeas.mapIndexed { index, idea -> "$index: ${idea.text}" }.joinToString("\n")
        val model = settings.geminiModelFlow.first()

        val response = gemini.generateContent(
            model = model,
            apiKey = apiKey,
            request = GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(COMBINED_SYSTEM_PROMPT)),
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

        val result = parseCombinedJson(json, newIdeas)
        val updatedProcessedIds = processedIds + newIdeas.map { it.id }

        return Result.success(result to updatedProcessedIds)
    }

    // ========================================================================
    // Parsing
    // ========================================================================

    private fun parseCombinedJson(raw: String, newIdeas: List<IdeenEntry>): SuggestionResult {
        return runCatching {
            val obj = JSONObject(raw)

            // Tasks parsen — Herkunft (Etappe 2d) ueber sourceIndex an die Quell-Idee binden.
            val tasksArr = obj.optJSONArray("tasks") ?: JSONArray()
            val tasks = buildList {
                for (i in 0 until tasksArr.length()) {
                    val o = tasksArr.optJSONObject(i) ?: continue
                    val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue
                    val description = o.optString("description")
                    // sourceIndex = Nummer der Quell-Idee aus dem nummerierten Prompt. Fehlt sie oder
                    // ist sie ungueltig (KI-Aussetzer) -> origin bleibt null (Fallback, nichts kaputt).
                    val sourceIdx = if (o.has("sourceIndex")) o.optInt("sourceIndex", -1) else -1
                    val sourceIdea = newIdeas.getOrNull(sourceIdx)
                    add(
                        AutoTaskSuggestion(
                            id = java.util.UUID.randomUUID().toString(),
                            title = title.take(60),
                            description = description.take(500),
                            originId = sourceIdea?.id,
                            originType = sourceIdea?.let { OriginType.IDEA },
                            // Die Idee ist aktuell immer Ursprung der Kette (Entropie->Idee erst Stufe 5)
                            // -> rootId = die Idee selbst.
                            rootId = sourceIdea?.id,
                        )
                    )
                }
            }

            // Habits parsen
            val habitsArr = obj.optJSONArray("habits") ?: JSONArray()
            val habits = buildList {
                for (i in 0 until habitsArr.length()) {
                    val text = habitsArr.optString(i).trim()
                    if (text.isNotBlank()) add(Mental.create(text))
                }
            }

            SuggestionResult(tasks, habits)
        }.getOrDefault(SuggestionResult(emptyList(), emptyList()))
    }

    // ========================================================================
    // System-Prompt (KOMBINIERT)
    // ========================================================================

    companion object {
        private const val COMBINED_SYSTEM_PROMPT = """
Du bist ein exakter Filter. Du bekommst eine LISTE nummerierter Ideen (jede Zeile im Format
"Nummer: Text"). Ordne JEDE Idee EINZELN und EXKLUSIV einer von drei Kategorien zu:
- AUFGABE (einmalig abarbeitbar)
- GEWOHNHEIT (wiederkehrend, mit klarem Wiederholungssignal)
- NICHTS (weder Aufgabe noch Gewohnheit)

=== HAUPTREGELN ===
1. Eine Idee ist EXKLUSIV entweder Aufgabe ODER Gewohnheit. NIEMALS beides.
2. Eine GEWOHNHEIT muss ein klares Wiederholungssignal enthalten. Ohne dieses Signal ist es NIEMALS eine Gewohnheit.
3. Wenn du unsicher bist, ordne die Idee NICHTS zu (also weder in tasks noch in habits).
4. Du fügst NICHTS hinzu und lässt NICHTS weg. Du erkennst nur und formulierst um.
5. Bei jeder AUFGABE gibst du im Feld "sourceIndex" die Nummer der Idee an, aus der sie stammt.

=== ENTSCHEIDUNGSLOGIK ===
Prüfe die Idee streng in dieser Reihenfolge:

SCHRITT 1 — Wiederholungssignal erkennen:
Enthält die Idee ein klares Zeichen für Wiederholung? Beispiele für solche Signale (nicht abschließend, erkenne auch sinngleiche Formulierungen):
- "täglich", "wöchentlich", "monatlich", "jährlich", "regelmäßig"
- "jeden Tag", "jeden Morgen", "jeden Abend", "jeden Montag"
- "alle X Tage", "alle X Wochen"
- "jedes Jahr", "immer wieder"
→ JA: Das ist eine GEWOHNHEIT. Gehe zu SCHRITT 2.
→ NEIN: Das ist KEINE Gewohnheit. Gehe zu SCHRITT 3.

SCHRITT 2 — GEWOHNHEIT formulieren:
Formuliere einen Satz, der IMMER mit "Ich" beginnt und ALLE Infos der Idee enthält. Nichts weglassen, nichts dazuerfinden.

SCHRITT 3 — AUFGABE erkennen:
Enthält die Idee eine konkrete Tätigkeit, die einmalig abarbeitbar ist?
→ JA: Erstelle eine AUFGABE mit:
   - title: maximal 4 Wörter, kurze Bezeichnung
   - description: der vollständige Inhalt der Idee
→ NEIN: Antworte mit NICHTS (leere Arrays).

=== BEISPIELE ===
"Ich möchte alle drei Tage Federball spielen mit den zwei Mädels"
→ GEWOHNHEIT: "Ich gehe alle drei Tage Federball spielen mit den zwei Mädels."

"Ich möchte regelmäßig alle zwei Tage ein Buch lesen"
→ GEWOHNHEIT: "Ich lese alle zwei Tage ein Buch."

"Jeden Morgen meditieren"
→ GEWOHNHEIT: "Ich meditiere jeden Morgen."

"Jeden Freitag Lebensmittel einkaufen für Papa"
→ GEWOHNHEIT: "Ich gehe jeden Freitag Lebensmittel einkaufen für Papa."

"Ich möchte einen Baum im Garten von Papa pflanzen"
→ AUFGABE: title: "Baum pflanzen", description: "Einen Baum im Garten von Papa pflanzen."

"Ich muss morgen mit dem neuen Hund Gassi gehen"
→ AUFGABE: title: "Mit Hund Gassi gehen", description: "Mit dem neuen Hund Gassi gehen."

"Ich möchte ein Bild von Raumschiff Enterprise malen"
→ AUFGABE: title: "Bild malen", description: "Ein Bild von Raumschiff Enterprise malen."

"Ich brauche neue Glühbirnen"
→ NICHTS (weder Aufgabe noch Gewohnheit klar erkennbar)

=== FORMAT ===
Antworte NUR mit diesem JSON. Keine Einleitung, keine Erklärung, keine Code-Klammern (kein ```), nur das reine JSON.
"sourceIndex" ist die Nummer der Idee (aus der Eingabe "Nummer: Text"), aus der die Aufgabe stammt:
{
  "tasks": [{"title": "...", "description": "...", "sourceIndex": 0}],
  "habits": ["Gewohnheit 1"]
}
Wenn keine Aufgabe: "tasks": []
Wenn keine Gewohnheit: "habits": []
Wenn beides leer: {"tasks": [], "habits": []}

WICHTIG: Verwende echte deutsche Umlaute (ä, ö, ü, ß).
"""
    }
}
