package com.entropyjournal.data.repository

import android.content.SharedPreferences
import android.util.Log
import retrofit2.HttpException
import com.entropyjournal.data.local.dao.AdviceDashboardDao
import com.entropyjournal.data.local.entity.AdviceBlockEntity
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.domain.model.AdviceBlock
import com.entropyjournal.domain.model.AdvicePriority
import com.entropyjournal.domain.model.Advice
import com.entropyjournal.domain.model.DerivationEntry
import com.entropyjournal.domain.model.TopAction
import com.entropyjournal.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdviceRepository @Inject constructor(
    private val geminiApi: GeminiApi,
    private val adviceDashboardDao: AdviceDashboardDao,
    private val encryptedPrefs: SharedPreferences
) {
    private val entropyAnalysisSystemPrompt = """
Du bist ein empathischer, hochintelligenter Lebensberater und Muster-Analyst.

DEINE AUFGABE:
Analysiere die Tagebucheinträge eines Nutzers. Finde wiederkehrende Quellen
persönlicher Entropie. Erstelle daraus ein strukturiertes Ratschlags-Dashboard
im JSON-Format.

DEFINITION — PERSÖNLICHE ENTROPIE:
Alles, was Unordnung, Stress, Energieverlust, Schmerz, Schlafprobleme,
emotionale Belastung oder Kontrollverlust im Leben des Nutzers erzeugt.

OBERSTE REGEL — KEIN EINTRAG DARF FEHLEN:
Du erhältst nummerierte Einträge (z.B. "EINTRAG 1 von 5").
Du MUSST JEDEN EINZELNEN Eintrag lesen, analysieren und einbeziehen.
Bevor du antwortest, zähle: Habe ich ALLE Einträge berücksichtigt?
Wenn einer fehlt — ergänze ihn SOFORT.

UMGANG MIT WENIGEN EINTRÄGEN:
- Bei 1–2 Einträgen: Benenne Einzelbeobachtungen statt Muster.
  Kennzeichne Ratschläge als "vorläufig" in der Beschreibung.
- Ab 3 Einträgen: Suche aktiv nach Mustern und Querverbindungen.

ZEITLICHE GEWICHTUNG:
Jeder Eintrag muss berücksichtigt werden. Bei widersprüchlichen Aussagen
zum selben Thema beachte den zeitlichen Verlauf — neuere Einträge zeigen
den aktuellen Stand. Ältere Einträge liefern Kontext und Mustererkennung.

SPRACHREGELN (gelten für ALLE Textfelder im JSON):
- Schreibe auf Deutsch.
- Einfache, klare Sprache. Kurze Sätze.
- Keine Fremdwörter, keine Fachbegriffe, keine Floskeln.
- Jeder soll den Text sofort verstehen, ohne nachzudenken.
- Empathisch, direkt und konkret — keine Allgemeinplätze.

JSON-AUSGABE-SCHEMA:
{
  "gesamt_entropie": 0.0,
  "trend": "steigend|stabil|sinkend|unbekannt",
  "gesamtanalyse": "...",
  "fortschritte": [...],
  "top_massnahmen": [...],
  "kategorien": [...]
}

FELD-DEFINITIONEN:

1) "gesamt_entropie" (Zahl, 0.0 bis 1.0)
   Gewichteter Durchschnitt aller Kategorie-Entropie-Levels.

2) "trend" (Text)
   Nur wenn mindestens 3 Einträge über mehrere Tage vorliegen.
   - "sinkend" = Belastung nimmt ab
   - "stabil" = Belastung bleibt gleich
   - "steigend" = Belastung nimmt zu
   - "unbekannt" = Zu wenig Daten für Trendaussage

3) "gesamtanalyse" (Text, 15–25 Sätze)
   - Gehe Eintrag für Eintrag durch und extrahiere das Hauptthema.
   - Benenne JEDES Thema aus JEDEM Eintrag namentlich.
   - Erkenne Zusammenhänge zwischen den Themen.
   - Erkenne auch FORTSCHRITTE und STÄRKEN, nicht nur Probleme.
   - Sei empathisch und persönlich — sprich den Nutzer direkt an.

4) "fortschritte" (Array, 0–5 Einträge)
   Erkenne, wo sich Belastung REDUZIERT hat oder wo funktionierende
   Gewohnheiten und Stärken sichtbar sind.
   Schema pro Fortschritt:
   {
     "titel": "Kurzer Titel (max. 5 Wörter)",
     "beschreibung": "Was genau sich verbessert hat oder gut läuft (2–3 Sätze).",
     "bezug": "Aus welchem Eintrag/welchen Einträgen das hervorgeht (1 Satz)."
   }
   Bei nur 1 Eintrag oder keinen erkennbaren Fortschritten: leeres Array [].

5) "top_massnahmen" (Array, genau 5 Einträge)
   Die 5 wichtigsten Maßnahmen, die die persönliche Entropie am
   STÄRKSTEN und NACHHALTIGSTEN senken würden.
   Sortiert nach Wirksamkeit (stärkste zuerst).
   Kategorieübergreifend — ganzheitlich denken.
   Schema pro Maßnahme:
   {
     "titel": "Kurzer Titel (max. 6 Wörter)",
     "beschreibung": "Max. 30 Wörter — kurz und knackig: was genau tun und warum.",
     "erklaerung": "Ausführliche Begründung (5–8 Sätze). Warum gerade diese
                    Maßnahme? Welche Einträge zeigen das Problem? Was passiert,
                    wenn man es umsetzt?"
   }

6) "kategorien" (Array, so viele wie nötig)
   Für JEDES erkannte Thema eine eigene Kategorie.
   Schema pro Kategorie:
   {
     "name": "Kategoriename (max. 12 Zeichen, 1–2 Wörter)",
     "icon": "material_icon_name",
     "farbe": "#HEX",
     "entropie_level": 0.0,
     "zusammenfassung": "Zusammenfassung dieser Kategorie (3–5 Sätze).",
     "ratschlaege": [...]
   }

   KATEGORIENAMEN — kurz und prägnant:
   RICHTIG: "Schlaf", "Arbeit", "Fitness", "Psyche", "Projekte"
   FALSCH: "Persönliche Entwicklung" → "Entwicklung"

   KATEGORIEN — DYNAMISCH:
   Nutze diese als Basis, aber erstelle NEUE wenn ein Thema nicht passt:
   - Schlaf (icon: bedtime, farbe: #6C63FF)
   - Arbeit (icon: work, farbe: #FF6B6B)
   - Fitness (icon: fitness_center, farbe: #4ECDC4)
   - Ernährung (icon: restaurant, farbe: #FFE66D)
   - Psyche (icon: psychology, farbe: #A78BFA)
   - Beziehungen (icon: people, farbe: #F472B6)
   - Zuhause (icon: home, farbe: #34D399)
   - Entwicklung (icon: trending_up, farbe: #60A5FA)
   - Projekte (icon: code, farbe: #F59E0B)
   - Gesundheit (icon: health_and_safety, farbe: #EF4444)
   - Finanzen (icon: account_balance, farbe: #10B981)
   - Freizeit (icon: sports_esports, farbe: #EC4899)
   - Natur (icon: grass, farbe: #22C55E)
   - Schmerz (icon: healing, farbe: #DC2626)
   Weitere Icons: spa, coffee, self_improvement, nights_stay, directions_run,
   child_care, school, computer, timer, cleaning_services, music_note, pets, wb_sunny, lightbulb

   ENTROPIE-LEVEL pro Kategorie (0.0 bis 1.0):
   - 0.0–0.33 = Niedrig (grün)
   - 0.34–0.66 = Mittel (gelb)
   - 0.67–1.0 = Hoch (rot)

   RATSCHLÄGE pro Kategorie:
   So viele wie die Datenlage hergibt — keine Obergrenze, aber jeder
   muss sich auf KONKRETE Aussagen aus den Einträgen beziehen.
   Sortiert nach Priorität: "hoch" zuerst, dann "mittel", dann "niedrig".
   Schema pro Ratschlag:
   {
     "titel": "Kurzer Titel (max. 6 Wörter)",
     "beschreibung": "Ausführliche Empfehlung (3–5 Sätze). Konkret und
                      direkt — was genau tun, wie, und warum.",
     "prioritaet": "hoch|mittel|niedrig",
     "verknuepfung": "Kategorienamen die zusammenhängen plus warum. Falls keine: null",
     "herleitung": [
       {
         "datum": "Datum des Eintrags (z.B. 28.03.2026)",
         "zusammenfassung": "Was in diesem Eintrag relevant war (1–2 Sätze)."
       }
     ]
   }

   PRIORITÄT-BEDEUTUNG:
   - "hoch" = Dringend, sofort handeln
   - "mittel" = Spürbar, bald angehen
   - "niedrig" = Beobachten, langfristig bearbeiten

AUSGABEFORMAT:
- Antworte NUR mit dem JSON-Objekt.
- Kein Text davor oder danach.
- Keine Markdown-Backticks.
- Beginne direkt mit { und ende mit }.
- Valides JSON.
    """.trimIndent()

    // Undo support: store previous state in memory
    private var previousBlocks: List<AdviceBlockEntity>? = null

    val canUndo: Boolean get() = previousBlocks != null

    suspend fun undoLastRefresh(): Boolean {
        val prev = previousBlocks ?: return false
        adviceDashboardDao.deleteAll()
        // Reset IDs to 0 so Room auto-generates fresh IDs
        val freshEntities = prev.map { it.copy(id = 0) }
        adviceDashboardDao.upsertAll(freshEntities)
        previousBlocks = null
        return true
    }

    private fun getSelectedModel(): String {
        return encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL)
            ?: Constants.DEFAULT_GEMINI_MODEL
    }

    fun getAllAdviceBlocks(): Flow<List<AdviceBlock>> {
        return adviceDashboardDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun analyzeEntropy(allEntriesText: String, entryCount: Int, freshAnalysis: Boolean = false): Result<Unit> {
        return try {
            val apiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
            if (apiKey.isBlank()) return Result.failure(IllegalStateException("Bitte Gemini API-Key in den Einstellungen eingeben"))

            // Save current state for undo before refreshing
            val existingBlocks = adviceDashboardDao.getAllSync()
            if (existingBlocks.isNotEmpty()) {
                previousBlocks = existingBlocks
            }

            // Only use previous context for automatic updates, NOT for manual refresh
            val previousContext = if (freshAnalysis) "" else buildPreviousContext(existingBlocks)

            val userText = buildUserText(allEntriesText, previousContext, entryCount)

            val selectedModel = getSelectedModel()
            Log.d("GeminiDebug", "Model: $selectedModel, API-Key length: ${apiKey.length}, Entries: $entryCount")

            val request = GeminiRequestBuilder.build(
                userText = userText,
                systemPrompt = entropyAnalysisSystemPrompt
            )

            // Try selected model first, fallback to gemini-2.5-flash-lite on HTTP 400
            val response = try {
                geminiApi.generateContent(
                    model = selectedModel,
                    apiKey = apiKey,
                    request = request
                )
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400 && selectedModel != Constants.DEFAULT_GEMINI_MODEL) {
                    Log.w("GeminiDebug", "Model $selectedModel returned 400, falling back to ${Constants.DEFAULT_GEMINI_MODEL}")
                    geminiApi.generateContent(
                        model = Constants.DEFAULT_GEMINI_MODEL,
                        apiKey = apiKey,
                        request = request
                    )
                } else {
                    throw e
                }
            }
            val jsonText = response.extractText() ?: return Result.failure(Exception("Keine Antwort von Gemini"))

            val cleanJson = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val blocks = parseAdviceJson(cleanJson, entryCount)

            adviceDashboardDao.deleteAll()
            adviceDashboardDao.upsertAll(blocks)

            Result.success(Unit)
        } catch (e: HttpException) {
            Log.e("GeminiDebug", "HTTP ${e.code()}: ${e.message()}")
            val msg = when (e.code()) {
                400 -> "Gemini-Modell nicht verfügbar. Bitte anderes Modell in Einstellungen wählen."
                401, 403 -> "Gemini API-Key ungültig oder abgelaufen."
                429 -> "Zu viele Anfragen. Bitte kurz warten."
                else -> "Gemini-Fehler (HTTP ${e.code()})"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            Log.e("GeminiDebug", "API error: ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    private fun buildPreviousContext(existingBlocks: List<AdviceBlockEntity>): String {
        if (existingBlocks.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("=== BISHERIGE ANALYSE (baue darauf auf, überschreibe sie nicht) ===")

        val overallAnalysis = existingBlocks.firstOrNull()?.overallAnalysis ?: ""
        if (overallAnalysis.isNotBlank()) {
            sb.appendLine("Bisherige Gesamtanalyse: $overallAnalysis")
            sb.appendLine()
        }

        existingBlocks.forEach { block ->
            sb.appendLine("Kategorie '${block.categoryName}': Entropie=${block.entropyLevel}, ${block.categorySummary}")
        }

        sb.appendLine("=== ENDE BISHERIGE ANALYSE ===")
        sb.appendLine()
        sb.appendLine("Aktualisiere und ERWEITERE die bisherige Analyse mit den neuen Einträgen.")
        sb.appendLine("Behalte wichtige Erkenntnisse bei und ergänze neue Muster.")
        sb.appendLine()

        return sb.toString()
    }

    private fun buildUserText(allEntriesText: String, previousContext: String, entryCount: Int): String {
        val sb = StringBuilder()
        if (previousContext.isNotBlank()) {
            sb.appendLine(previousContext)
        } else {
            sb.appendLine("=== FRISCHE ANALYSE — Erstelle eine komplett neue, eigenständige Analyse. ===")
            sb.appendLine()
        }
        sb.appendLine("=== ALLE $entryCount TAGEBUCHEINTRÄGE (JEDEN EINZELNEN ANALYSIEREN!) ===")
        sb.appendLine(allEntriesText)
        sb.appendLine()
        sb.appendLine("=== PFLICHT-CHECK: Du hast $entryCount Einträge erhalten. Jeder muss in der Analyse und in mindestens einer Kategorie erscheinen. ===")
        return sb.toString()
    }

    private fun parseAdviceJson(jsonString: String, entryCount: Int): List<AdviceBlockEntity> {
        val json = JSONObject(jsonString)
        val overallAnalysis = json.getString("gesamtanalyse")
        val topActionsJson = json.optJSONArray("top_massnahmen")?.toString() ?: "[]"
        val categories = json.getJSONArray("kategorien")
        val now = System.currentTimeMillis()

        return (0 until categories.length()).map { i ->
            val cat = categories.getJSONObject(i)
            val adviceArray = cat.getJSONArray("ratschlaege")

            AdviceBlockEntity(
                categoryName = cat.getString("name"),
                categoryIcon = cat.getString("icon"),
                categoryColor = cat.getString("farbe"),
                entropyLevel = cat.getDouble("entropie_level").toFloat(),
                categorySummary = cat.getString("zusammenfassung"),
                adviceJson = adviceArray.toString(),
                overallAnalysis = overallAnalysis,
                topActionsJson = topActionsJson,
                lastUpdated = now,
                basedOnEntryCount = entryCount
            )
        }
    }
}

private fun AdviceBlockEntity.toDomain(): AdviceBlock {
    val advices = try {
        val array = JSONArray(adviceJson)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val derivation = try {
                val herleitungArray = obj.optJSONArray("herleitung")
                if (herleitungArray != null) {
                    (0 until herleitungArray.length()).map { j ->
                        val h = herleitungArray.getJSONObject(j)
                        DerivationEntry(
                            date = h.optString("datum", ""),
                            summary = h.optString("zusammenfassung", "")
                        )
                    }
                } else emptyList()
            } catch (_: Exception) { emptyList() }

            Advice(
                title = obj.getString("titel"),
                description = obj.getString("beschreibung"),
                priority = when (obj.optString("prioritaet", "mittel")) {
                    "hoch" -> AdvicePriority.HIGH
                    "niedrig" -> AdvicePriority.LOW
                    else -> AdvicePriority.MEDIUM
                },
                connection = obj.optString("verknuepfung", ""),
                derivation = derivation
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    val topActions = try {
        val topArray = JSONArray(topActionsJson)
        (0 until topArray.length()).map { i ->
            val obj = topArray.getJSONObject(i)
            TopAction(
                title = obj.getString("titel"),
                description = obj.getString("beschreibung"),
                detailedDescription = obj.optString("erklaerung", "")
            )
        }
    } catch (_: Exception) { emptyList() }

    return AdviceBlock(
        id = id,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        categoryColor = categoryColor,
        entropyLevel = entropyLevel,
        categorySummary = categorySummary,
        advices = advices,
        overallAnalysis = overallAnalysis,
        topActions = topActions,
        lastUpdated = lastUpdated,
        basedOnEntryCount = basedOnEntryCount
    )
}
