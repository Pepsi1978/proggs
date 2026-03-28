package com.entropyjournal.data.repository

import android.content.SharedPreferences
import com.entropyjournal.data.local.dao.AdviceDashboardDao
import com.entropyjournal.data.local.entity.AdviceBlockEntity
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.domain.model.AdviceBlock
import com.entropyjournal.domain.model.AdvicePriority
import com.entropyjournal.domain.model.Advice
import com.entropyjournal.domain.model.DerivationEntry
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
Deine Aufgabe ist es, aus den Tagebucheinträgen eines Nutzers wiederkehrende Quellen
persönlicher Entropie zu identifizieren und daraus ein kohärentes Ratschlags-Dashboard
zu erstellen.

Definition: „Persönliche Entropie" umfasst alles, was Unordnung, Stress, Energieverlust,
Schmerz, Schlafprobleme, emotionale Belastung oder Kontrollverlust im Leben des Nutzers
erzeugt.

KRITISCHE PFLICHT — ALLE EINTRÄGE GLEICHWERTIG ANALYSIEREN:
Du erhältst nummerierte Einträge (z.B. "EINTRAG 1 von 5", "EINTRAG 2 von 5").
Du MUSST JEDEN EINZELNEN Eintrag lesen, analysieren und in die Gesamtanalyse einbeziehen.
Es ist ein FEHLER, wenn ein Eintrag in der Analyse fehlt oder weniger Beachtung bekommt.

Prüfe am Ende selbst: Habe ich JEDEN nummerierten Eintrag berücksichtigt?
Wenn nicht — ergänze die fehlenden Themen SOFORT.

GESAMTANALYSE (8–12 Sätze):
- Gehe Eintrag für Eintrag durch und extrahiere das Hauptthema.
- Benenne JEDES Thema aus JEDEM Eintrag namentlich in der Gesamtanalyse.
- Ältere Einträge sind GENAUSO wichtig wie neuere — kein Eintrag darf fehlen.
- Wenn 3 Einträge vorliegen (z.B. Schlaf, Arbeit, KI-Projekte), müssen ALLE 3
  Themen in der Gesamtanalyse UND als eigene Kategorien mit Ratschlägen erscheinen.
- Sei empathisch, direkt und konkret — keine Allgemeinplätze.

RATSCHLÄGE — FÜR JEDE KATEGORIE AUS JEDEM EINTRAG:
- Jede Kategorie muss Ratschläge enthalten die sich auf KONKRETE Aussagen
  aus den Tagebucheinträgen beziehen — nicht auf allgemeine Tipps.
- Wenn ein Eintrag über "zu viel Licht beim Schlafen" spricht, muss ein
  konkreter Ratschlag dazu erscheinen, nicht nur "regelmäßiger schlafen".

KATEGORIEN — DYNAMISCH:
Verwende diese Standard-Kategorien als Basis, aber erstelle NEUE Kategorien wenn du
in den Einträgen Themen findest die nicht in die Standard-Liste passen.
Die Kategorien sollen die TATSÄCHLICHEN Themen des Nutzers widerspiegeln.

Standard-Kategorien (nur als Ausgangspunkt — erstelle NEUE passend zum Inhalt):
- Schlaf (icon: bedtime, farbe: #6C63FF)
- Arbeit (icon: work, farbe: #FF6B6B)
- Fitness (icon: fitness_center, farbe: #4ECDC4)
- Ernährung (icon: restaurant, farbe: #FFE66D)
- Mentale Gesundheit (icon: psychology, farbe: #A78BFA)
- Beziehungen (icon: people, farbe: #F472B6)
- Zuhause (icon: home, farbe: #34D399)
- Persönliche Entwicklung (icon: trending_up, farbe: #60A5FA)

Weitere verfügbare Icons für dynamische Kategorien:
code, computer, school, self_improvement, favorite, health, medical,
finance, shopping, car, running, travel, nature, sun, pets, music,
art, design, gaming, lightbulb, timer, star, achievement, social, energy

ENTROPIE-LEVEL (0.0 bis 1.0):
Diese Skala zeigt die INTENSITÄT der Belastung in jeder Kategorie:
- 0.0–0.33 = Niedrig (grün) — Wenig Belastung, guter Zustand
- 0.34–0.66 = Mittel (gelb) — Spürbare Belastung, Aufmerksamkeit nötig
- 0.67–1.0 = Hoch (rot) — Starke Belastung, sofortiges Handeln empfohlen

PRIORITÄT der Ratschläge:
- "hoch" = Dringend, sofort handeln (roter Punkt)
- "mittel" = Aufmerksamkeit nötig, bald angehen (oranger Punkt)
- "niedrig" = Beobachten, langfristig bearbeiten (blauer Punkt)

JSON-Schema:
{
  "gesamtanalyse": "Ausführlicher, empathischer Text (8–12 Sätze)...",
  "kategorien": [
    {
      "name": "Kategoriename",
      "icon": "material_icon_name",
      "farbe": "#HEX",
      "entropie_level": 0.0,
      "zusammenfassung": "Zusammenfassung dieser Kategorie (3–5 Sätze)...",
      "ratschlaege": [
        {
          "titel": "Kurzer Titel",
          "beschreibung": "Ausführliche Empfehlung (3–5 Sätze)...",
          "prioritaet": "hoch|mittel|niedrig",
          "verknuepfung": "Querverbindung zu anderen Kategorien...",
          "herleitung": [
            {
              "datum": "Datum des Eintrags (z.B. 28.03.2026, 14:30)",
              "zusammenfassung": "Kurze Zusammenfassung was in diesem Eintrag relevant war (1–2 Sätze)"
            }
          ]
        }
      ]
    }
  ]
}

Regeln:
- Schreibe auf Deutsch, klar und direkt, ohne Floskeln.
- Jeder Ratschlag muss sich auf konkrete Muster aus den Einträgen beziehen.
- Mindestens 3 Ratschläge pro Kategorie, gerne mehr (5–8) wenn die Datenlage es hergibt.
- Erstelle für JEDES erkannte Thema eine eigene Kategorie — auch wenn nur EIN Eintrag davon handelt.
- Wenn 5 Einträge vorliegen, müssen die Themen aus ALLEN 5 Einträgen als Kategorien erscheinen.
- Innerhalb eines Eintrags können MEHRERE Themen vorkommen — jedes wird eine eigene Kategorie.
  Beispiel: Ein Eintrag über "Schlafprobleme durch Licht und Stress bei der Arbeit"
  → Kategorie "Schlaf" MIT konkretem Ratschlag zu Licht UND Kategorie "Arbeit" MIT Ratschlag zu Stress.
- Jeder einzelne Aspekt der in einem Eintrag erwähnt wird verdient einen eigenen Ratschlag.
  Nicht zusammenfassen! Lieber 2 separate Ratschläge als 1 allgemeiner.
- Sortiere die Ratschläge in jeder Kategorie nach Priorität: "hoch" zuerst, dann "mittel", dann "niedrig".
- Antworte ausschließlich mit dem JSON-Objekt.
    """.trimIndent()

    // Undo support: store previous state in memory
    private var previousBlocks: List<AdviceBlockEntity>? = null

    val canUndo: Boolean get() = previousBlocks != null

    suspend fun undoLastRefresh(): Boolean {
        val prev = previousBlocks ?: return false
        adviceDashboardDao.deleteAll()
        adviceDashboardDao.upsertAll(prev)
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

    suspend fun analyzeEntropy(allEntriesText: String, entryCount: Int): Result<Unit> {
        return try {
            val apiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
            if (apiKey.isBlank()) return Result.failure(IllegalStateException("Gemini API-Key nicht konfiguriert"))

            // Save current state for undo before refreshing
            val existingBlocks = adviceDashboardDao.getAllSync()
            if (existingBlocks.isNotEmpty()) {
                previousBlocks = existingBlocks
            }

            // Read existing analysis for context continuity
            val previousContext = buildPreviousContext(existingBlocks)

            val userText = buildUserText(allEntriesText, previousContext, entryCount)

            val request = GeminiRequestBuilder.build(
                userText = userText,
                systemPrompt = entropyAnalysisSystemPrompt
            )
            val response = geminiApi.generateContent(
                model = getSelectedModel(),
                apiKey = apiKey,
                request = request
            )
            val jsonText = response.extractText() ?: return Result.failure(Exception("Keine Antwort von Gemini"))

            val cleanJson = jsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val blocks = parseAdviceJson(cleanJson, entryCount)

            adviceDashboardDao.deleteAll()
            adviceDashboardDao.upsertAll(blocks)

            Result.success(Unit)
        } catch (e: Exception) {
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
        }
        sb.appendLine("=== ALLE $entryCount TAGEBUCHEINTRÄGE (JEDEN EINZELNEN ANALYSIEREN!) ===")
        sb.appendLine(allEntriesText)
        sb.appendLine()
        sb.appendLine("=== ERINNERUNG: Du hast $entryCount Einträge erhalten. JEDER muss in der Analyse berücksichtigt werden. ===")
        return sb.toString()
    }

    private fun parseAdviceJson(jsonString: String, entryCount: Int): List<AdviceBlockEntity> {
        val json = JSONObject(jsonString)
        val overallAnalysis = json.getString("gesamtanalyse")
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

    return AdviceBlock(
        id = id,
        categoryName = categoryName,
        categoryIcon = categoryIcon,
        categoryColor = categoryColor,
        entropyLevel = entropyLevel,
        categorySummary = categorySummary,
        advices = advices,
        overallAnalysis = overallAnalysis,
        lastUpdated = lastUpdated,
        basedOnEntryCount = basedOnEntryCount
    )
}
