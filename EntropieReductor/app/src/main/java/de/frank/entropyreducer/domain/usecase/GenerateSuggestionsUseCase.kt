package de.frank.entropyreducer.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.HabitDao
import de.frank.entropyreducer.data.local.dao.HabitSuggestionDao
import de.frank.entropyreducer.data.local.dao.TaskSuggestionDao
import de.frank.entropyreducer.data.local.entities.OriginType
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.presentation.ideen.IdeenEntry
import de.frank.entropyreducer.presentation.tagebuch.TagebuchEntry
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
 * Gewohnheits-Vorschlag mit Herkunft (ID-Architektur Etappe 3d, analog [AutoTaskSuggestion]).
 * originId/originType/rootId = die Quell-Idee; null, wenn die KI keine (gueltige) sourceIndex lieferte.
 */
data class AutoHabitSuggestion(
    val id: String,
    val text: String,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

/**
 * Kombiniertes Ergebnis: Aufgaben UND Gewohnheiten aus EINEM Gemini-Aufruf.
 */
data class SuggestionResult(
    val tasks: List<AutoTaskSuggestion>,
    val habits: List<AutoHabitSuggestion>,
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
    // ID-Architektur Dedup-Ablösung (Frank-Wunsch 2026-06-19): Ketten-Dedup ueber die Herkunft.
    private val taskSuggestionDao: TaskSuggestionDao,
    private val habitSuggestionDao: HabitSuggestionDao,
    // Direktive #3 robust (Frank-Wunsch 2026-06-20): Ketten-Dedup auch ueber die ANGENOMMENEN
    // Endpunkte (entropy_entries/habits mit rootId = Quell-Idee), damit die Kette beim Annehmen
    // eines Vorschlags nicht bricht.
    private val entropyEntryDao: EntropyEntryDao,
    private val habitDao: HabitDao,
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
    ): Result<Pair<SuggestionResult, Set<String>>> =
        generateSuggestionsForSources(
            sources = ideas.map { SuggestionSource(it.id, it.text, OriginType.IDEA) },
            processedIds = processedIds,
            systemPrompt = COMBINED_SYSTEM_PROMPT,
            inputLabel = "Ideen",
        )

    /** Dieselbe Vorschlags-Pipeline fuer Lerneintraege, mit Umsetzungsfokus fuer Erkenntnisse. */
    suspend fun generateLearningSuggestions(
        entries: List<TagebuchEntry>,
        processedIds: Set<String>,
    ): Result<Pair<SuggestionResult, Set<String>>> =
        generateSuggestionsForSources(
            sources = entries.map {
                SuggestionSource(
                    id = it.id,
                    text = it.improvedText?.takeIf(String::isNotBlank) ?: it.text,
                    originType = OriginType.LEARNING,
                )
            },
            processedIds = processedIds,
            systemPrompt = LEARNING_SYSTEM_PROMPT,
            inputLabel = "Lerneintraege",
        )

    private suspend fun generateSuggestionsForSources(
        sources: List<SuggestionSource>,
        processedIds: Set<String>,
        systemPrompt: String,
        inputLabel: String,
    ): Result<Pair<SuggestionResult, Set<String>>> {
        val apiKey = secrets.geminiApiKey
            ?: return Result.failure(IllegalArgumentException("Bitte Gemini-API-Key in den Einstellungen hinterlegen."))

        if (sources.isEmpty()) return Result.success(SuggestionResult(emptyList(), emptyList()) to processedIds)

        // Dedup-Ablösung (Frank-Wahl "alte Liste als Schutz behalten"): Eine Idee wird NICHT erneut
        // verarbeitet, wenn aus ihr bereits ein Vorschlag hervorging — erkennbar an der Herkunfts-Kette
        // (countByOriginId in task_suggestions ODER habit_suggestions). Das ist robust auch nach
        // Listen-Verlust/Geraetewechsel (der Doppelvorschlag-Bug der Spec). Zusaetzlich greift weiterhin
        // die alte processed_idea_ids-Liste als Altbestand-Schutz (Bestands-Vorschlaege ohne Herkunft +
        // Ideen, die zu NICHTS wurden). NIE blockierend: eine neue/aehnliche Idee hat eine neue id ->
        // countByOriginId == 0 -> wird verarbeitet.
        // Live-Logik-Sonde (Checkpoint): zaehlt die Dedup-Gruende MIT, ohne die Entscheidung zu
        // veraendern (die continue-Logik ist identisch). So ist live pruefbar, ob der Ketten-Dedup
        // (countByOriginId) so greift wie gemeint.
        var skipProcessedList = 0
        var skipChain = 0
        var skipAccepted = 0
        val newSources = buildList {
            for (source in sources) {
                if (source.id in processedIds) {
                    skipProcessedList++
                    continue
                }
                if (taskSuggestionDao.countByOriginId(source.id) > 0) {
                    skipChain++
                    continue
                }
                if (habitSuggestionDao.countByOriginId(source.id) > 0) {
                    skipChain++
                    continue
                }
                // Direktive #3 robust (Frank-Wunsch 2026-06-20): Die Kette darf beim Annehmen NICHT
                // brechen. Ein angenommener Vorschlag wird zur Aufgabe (entropy_entries) bzw.
                // Gewohnheit (habits) mit rootId = Quell-Idee; der Vorschlag selbst ist dann geloescht,
                // sodass countByOriginId nicht mehr greift. Deshalb hier zusaetzlich die ANGENOMMENEN
                // Endpunkte pruefen (countByRootId) -> die Idee bleibt dedupliziert, auch nachdem ihr
                // Vorschlag verbraucht wurde. Cross-device robust, weil entropy_entries ihre rootId ab
                // Backup-Schema v19 mitnehmen (habits-Backup ist separate Folgeaufgabe).
                if (entropyEntryDao.countByRootId(source.id) > 0) {
                    skipAccepted++
                    continue
                }
                if (habitDao.countByRootId(source.id) > 0) {
                    skipAccepted++
                    continue
                }
                add(source)
            }
        }
        Diag.i(
            DiagnosticArea.AGENTIC,
            TAG,
            "CHECKPOINT Dedup: sourcesTotal=${sources.size} skipProcessedList=$skipProcessedList " +
                "skipChain=$skipChain skipAccepted=$skipAccepted toProcess=${newSources.size}",
        )
        if (newSources.isEmpty()) {
            Diag.i(DiagnosticArea.AGENTIC, TAG, "CHECKPOINT Dedup: nichts Neues -> kein Gemini-Aufruf")
            return Result.success(SuggestionResult(emptyList(), emptyList()) to processedIds)
        }

        // ID-Architektur Etappe 2d: Ideen nummeriert (Index in newIdeas), damit die KI pro Vorschlag
        // die Quell-Idee per sourceIndex zurueckgeben kann -> Herkunft Idee -> Vorschlag.
        val sourceText = newSources.mapIndexed { index, source -> "$index: ${source.text}" }.joinToString("\n")
        val model = settings.geminiModelFlow.first()

        val response = gemini.generateContent(
            model = model,
            apiKey = apiKey,
            request = GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(systemPrompt)),
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart("Hier sind meine $inputLabel:\n\n$sourceText")),
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

        val result = parseCombinedJson(json, newSources)

        // Live-Logik-Sonde (Checkpoint): bestaetigt das Origin-Tracking. Erwartet: jeder erzeugte
        // Vorschlag traegt eine Herkunft (originId = Quell-Idee). originId=NULL! markiert die
        // Abweichung (KI lieferte keine gueltige sourceIndex -> Fallback), damit das live auffaellt.
        val tasksMitHerkunft = result.tasks.count { it.originId != null }
        val habitsMitHerkunft = result.habits.count { it.originId != null }
        Diag.i(
            DiagnosticArea.AGENTIC,
            TAG,
            "CHECKPOINT Origin: tasks=${result.tasks.size} (mitHerkunft=$tasksMitHerkunft) " +
                "habits=${result.habits.size} (mitHerkunft=$habitsMitHerkunft)",
        )
        result.tasks.forEach { t ->
            Diag.i(
                DiagnosticArea.AGENTIC,
                TAG,
                "CHECKPOINT Origin task '${t.title}': originId=${t.originId ?: "NULL!"} " +
                    "originType=${t.originType ?: "-"} rootId=${t.rootId ?: "-"}",
            )
        }
        result.habits.forEach { h ->
            Diag.i(
                DiagnosticArea.AGENTIC,
                TAG,
                "CHECKPOINT Origin habit '${h.text.take(40)}': originId=${h.originId ?: "NULL!"} " +
                    "rootId=${h.rootId ?: "-"}",
            )
        }

        val updatedProcessedIds = processedIds + newSources.map { it.id }

        return Result.success(result to updatedProcessedIds)
    }

    // ========================================================================
    // Parsing
    // ========================================================================

    private fun parseCombinedJson(raw: String, newSources: List<SuggestionSource>): SuggestionResult {
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
                    // ist sie ungueltig (KI-Aussetzer): wurde nur GENAU EINE Idee verarbeitet, ist die
                    // Herkunft trotzdem eindeutig (singleOrNull) -> Frank-Wunsch 2026-06-20, schliesst
                    // die origin=NULL-Luecke. Bei mehreren Ideen bleibt sie null (eine falsche Zuordnung
                    // waere schlimmer als keine).
                    val sourceIdx = if (o.has("sourceIndex")) o.optInt("sourceIndex", -1) else -1
                    val source = newSources.getOrNull(sourceIdx) ?: newSources.singleOrNull()
                    add(
                        AutoTaskSuggestion(
                            id = java.util.UUID.randomUUID().toString(),
                            title = title.take(60),
                            description = description.take(500),
                            originId = source?.id,
                            originType = source?.originType,
                            // Die Idee ist aktuell immer Ursprung der Kette (Entropie->Idee erst Stufe 5)
                            // -> rootId = die Idee selbst.
                            rootId = source?.id,
                        )
                    )
                }
            }

            // Habits parsen — Herkunft (Etappe 3d) ueber sourceIndex an die Quell-Idee binden.
            val habitsArr = obj.optJSONArray("habits") ?: JSONArray()
            val habits = buildList {
                for (i in 0 until habitsArr.length()) {
                    val o = habitsArr.optJSONObject(i) ?: continue
                    val text = o.optString("text").takeIf { it.isNotBlank() } ?: continue
                    // KI-Aussetzer-Fallback (Frank-Wunsch 2026-06-20): nur 1 Idee verarbeitet -> eindeutig.
                    val sourceIdx = if (o.has("sourceIndex")) o.optInt("sourceIndex", -1) else -1
                    val source = newSources.getOrNull(sourceIdx) ?: newSources.singleOrNull()
                    add(
                        AutoHabitSuggestion(
                            id = java.util.UUID.randomUUID().toString(),
                            text = text,
                            originId = source?.id,
                            originType = source?.originType,
                            rootId = source?.id,
                        )
                    )
                }
            }

            SuggestionResult(tasks, habits)
        }.getOrDefault(SuggestionResult(emptyList(), emptyList()))
    }

    // ========================================================================
    // System-Prompt (KOMBINIERT)
    // ========================================================================

    companion object {
        private const val TAG = "GenSuggest"

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
5. Bei jeder AUFGABE und jeder GEWOHNHEIT gibst du im Feld "sourceIndex" die Nummer der Idee an, aus der sie stammt.

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
  "habits": [{"text": "Gewohnheit 1", "sourceIndex": 0}]
}
Wenn keine Aufgabe: "tasks": []
Wenn keine Gewohnheit: "habits": []
Wenn beides leer: {"tasks": [], "habits": []}

WICHTIG: Verwende echte deutsche Umlaute (ä, ö, ü, ß).
"""

        private const val LEARNING_SYSTEM_PROMPT = COMBINED_SYSTEM_PROMPT + """

=== ZUSATZREGELN FUER LERNEINTRAEGE ===
Die Eintraege beschreiben Erkenntnisse, die der Nutzer praktisch umsetzen moechte.
- Laesst sich eine Erkenntnis wiederholt im Alltag anwenden, formuliere daraus bevorzugt eine GEWOHNHEIT, auch wenn das Wiederholungssignal nur sinngemaess enthalten ist.
- Formuliere die Gewohnheit als konkrete, dauerhaft wiederholbare Ich-Handlung. Erhalte alle relevanten Informationen und erfinde keine fremden Details.
- Verlangt die Erkenntnis stattdessen einen einmaligen konkreten Umsetzungsschritt, formuliere eine AUFGABE.
- Ist die Erkenntnis rein faktisch und ergibt sich daraus keine konkrete persoenliche Handlung, ordne sie NICHTS zu.
- Auch hier gilt exklusiv: genau AUFGABE oder GEWOHNHEIT oder NICHTS, niemals mehrere Ergebnisse fuer denselben Lerneintrag.
"""
    }

    private data class SuggestionSource(
        val id: String,
        val text: String,
        val originType: String,
    )
}
