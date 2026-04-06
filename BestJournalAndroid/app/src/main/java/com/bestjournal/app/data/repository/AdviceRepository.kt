package com.bestjournal.app.data.repository

import com.bestjournal.app.data.local.dao.AdviceDashboardDao
import com.bestjournal.app.util.Constants
import com.bestjournal.app.data.local.entity.AdviceBlockEntity
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.domain.model.Advice
import com.bestjournal.app.domain.model.AdviceBlock
import com.bestjournal.app.domain.model.AdvicePriority
import com.bestjournal.app.domain.model.DerivationEntry
import com.bestjournal.app.domain.model.TopAction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AdviceRepository
@Inject
constructor(
    private val firebaseAiService: FirebaseAiService,
    private val adviceDashboardDao: AdviceDashboardDao,
    private val encryptedPrefs: android.content.SharedPreferences,
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

MENGEN-REGEL — VOLLSTÄNDIGKEIT VOR KÜRZE:
Die Gesamtzahl aller Ratschläge über alle Kategorien hinweg soll
mindestens 15 betragen. Weniger als 10 ist ein Fehler.
Jeder einzelne Hinweis, jede Beobachtung, jedes Problem aus den
Einträgen verdient einen eigenen Ratschlag. Fasse NICHT zusammen.
Wenn ein Eintrag 3 verschiedene Probleme nennt, entstehen daraus
3 separate Ratschläge — nicht einer der alles zusammenfasst.
Das JSON darf lang werden — Vollständigkeit ist wichtiger als Kürze.

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
   - 0.0–0.33 = Niedrig (guter Zustand)
   - 0.34–0.66 = Mittel (Aufmerksamkeit nötig)
   - 0.67–1.0 = Hoch (sofortiges Handeln empfohlen)

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
     "beschreibung": "16–24 Wörter — kurz und knackig: was genau tun und warum.",
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

   RATSCHLÄGE pro Kategorie — MENGE:
   Generiere ALLE Ratschläge die du aus den Einträgen ableiten kannst.
   Lieber zu viele als zu wenige — 5 bis 20 pro Kategorie sind normal.
   Jeder einzelne Hinweis, jede Beobachtung, jedes Problem aus den
   Einträgen verdient einen eigenen Ratschlag. Fasse NICHT zusammen.
   Wenn ein Eintrag 3 verschiedene Probleme nennt, entstehen daraus
   3 separate Ratschläge — nicht einer der alles zusammenfasst.
   Jeder Ratschlag muss sich auf KONKRETE Aussagen aus den Einträgen beziehen.
   Sortiert nach Priorität: "hoch" zuerst, dann "mittel", dann "niedrig".

   Schema pro Ratschlag:
   {
     "titel": "Kurzer Titel (max. 6 Wörter)",
     "beschreibung": "16–24 Wörter — konkret und direkt: was genau tun und warum.",
     "prioritaet": "hoch|mittel|niedrig",
     "verknuepfung": "1–2 andere Kategorienamen die zusammenhängen,
                      plus ein Satz warum. Falls keine: null",
     "herleitung": [
       {
         "datum": "Datum des Eintrags (z.B. 28.03.2026)",
         "zusammenfassung": "Was in diesem Eintrag relevant war (1–2 Sätze)."
       }
     ]
   }

   PRIORITÄT-BEDEUTUNG:
   - "hoch" = Dringend, sofort handeln (größte Entropie-Quelle)
   - "mittel" = Spürbar, bald angehen
   - "niedrig" = Beobachten, langfristig bearbeiten

AUSGABEFORMAT — STRENGE REGELN:
- Antworte NUR mit dem JSON-Objekt.
- Kein Text davor oder danach.
- Keine Markdown-Backticks.
- Beginne direkt mit { und ende mit }.
- Valides JSON — keine fehlenden Kommas, keine doppelten Schlüssel.
    """.trimIndent()

    private val goalsAnalysisSystemPrompt = """
Du bist ein aufmerksamer, motivierender Ziel-Analyst und Fortschritts-Tracker.

DEINE AUFGABE:
Analysiere die Tagebucheintr${"\u00e4"}ge eines Nutzers. Erkenne alle Ziele, W${"\u00fc"}nsche,
Vorhaben und Pl${"\u00e4"}ne — auch wenn sie nur beil${"\u00e4"}ufig erw${"\u00e4"}hnt werden. Verfolge
den Fortschritt ${"\u00fc"}ber mehrere Eintr${"\u00e4"}ge hinweg. Erstelle daraus ein strukturiertes
Ziele-Dashboard im JSON-Format.

DEFINITION — WAS IST EIN ZIEL:
Alles, was der Nutzer erreichen, ver${"\u00e4"}ndern, anfangen, beenden, verbessern
oder aufbauen m${"\u00f6"}chte. Auch indirekte Hinweise z${"\u00e4"}hlen:
- Direkt: "Ich will abnehmen", "Ich muss den Zahnarzt anrufen"
- Indirekt: "W${"\u00e4"}re sch${"\u00f6"}n, mal wieder laufen zu gehen" = Ziel Fitness
- Klagen: "Mein Schlaf ist so schlecht" = implizites Ziel Schlafverbesserung
- Tr${"\u00e4"}ume: "Irgendwann m${"\u00f6"}chte ich nach Schweden" = Langfrist-Ziel Reise

OBERSTE REGEL — KEIN EINTRAG DARF FEHLEN:
Du erh${"\u00e4"}ltst nummerierte Eintr${"\u00e4"}ge (z.B. "EINTRAG 1 von 5").
Du MUSST JEDEN EINZELNEN Eintrag lesen, analysieren und einbeziehen.

UMGANG MIT WENIGEN EINTR${"\u00c4"}GEN:
- Bei 1–2 Eintr${"\u00e4"}gen: Erkenne Einzelziele, bewerte Fortschritt als "unbekannt".
- Ab 3 Eintr${"\u00e4"}gen: Verfolge aktiv den Fortschritt und erkenne Muster.

SPRACHREGELN:
- Deutsch. Einfach, klar. Keine Fremdw${"\u00f6"}rter.
- Motivierend und ehrlich — feiere Fortschritt, besch${"\u00f6"}nige nichts.

MENGEN-REGEL — JEDES ZIEL Z${"\u00c4"}HLT:
Erkenne ALLE Ziele — auch kleine. Fasse NICHT zusammen.

JSON-AUSGABE-SCHEMA:
{
  "gesamtanalyse": "...",
  "top_massnahmen": [...],
  "kategorien": [...]
}

1) "gesamtanalyse" (15–25 S${"\u00e4"}tze): Alle Ziele benennen, Fortschritt erkennen.

2) "top_massnahmen" (genau 5): N${"\u00e4"}chste Schritte.
   { "titel": "...", "beschreibung": "16–24 W${"\u00f6"}rter", "erklaerung": "5–8 S${"\u00e4"}tze" }

3) "kategorien": Ziel-Bereiche.
   {
     "name": "max 12 Zeichen", "icon": "material_icon", "farbe": "#HEX",
     "entropie_level": 0.0, "zusammenfassung": "3–5 S${"\u00e4"}tze",
     "ratschlaege": [{
       "titel": "...",
       "beschreibung": "16–24 W${"\u00f6"}rter. Status: [offen/in Arbeit/blockiert/erreicht]. N${"\u00e4"}chster Schritt: [...].",
       "prioritaet": "hoch|mittel|niedrig",
       "verknuepfung": "...", "herleitung": [{"datum":"...","zusammenfassung":"..."}]
     }]
   }
   Bereiche: Fitness, Gesundheit, Arbeit, Karriere, Finanzen, Beziehungen,
   Projekte, Lernen, Schlaf, Psyche, Reise, Ordnung, Kreativit${"\u00e4"}t
   Priorit${"\u00e4"}t: hoch=blockiert, mittel=offen, niedrig=in Arbeit/erreicht

AUSGABEFORMAT: NUR JSON. Keine Backticks. Beginne mit {.
    """.trimIndent()

    private fun getActiveSystemPrompt(): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return when (scenario) {
            3 -> goalsAnalysisSystemPrompt
            4 -> {
                val custom = encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                if (custom.isNotBlank()) custom else entropyAnalysisSystemPrompt
            }
            else -> entropyAnalysisSystemPrompt
        }
    }

    private fun getActiveUserPromptPrefix(freshAnalysis: Boolean): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return if (scenario == 3 && freshAnalysis) {
            "=== FRISCHE ZIEL-ANALYSE — Erstelle eine komplett neue, eigenst${"\u00e4"}ndige Analyse. ==="
        } else if (freshAnalysis) {
            "=== FRISCHE ANALYSE — Erstelle eine komplett neue, eigenst${"\u00e4"}ndige Analyse. ==="
        } else ""
    }

    private fun getActiveUserPromptSuffix(entryCount: Int): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return if (scenario == 3) {
            "=== PFLICHT-CHECK: Du hast $entryCount Eintr${"\u00e4"}ge erhalten. Jeder muss auf Ziele durchsucht werden. ==="
        } else {
            "=== PFLICHT-CHECK: Du hast $entryCount Eintr${"\u00e4"}ge erhalten. Jeder muss in der Analyse erscheinen. ==="
        }
    }

    // Undo support: store previous state in memory
    private var previousBlocks: List<AdviceBlockEntity>? = null

    val canUndo: Boolean
        get() = previousBlocks != null

    suspend fun undoLastRefresh(): Boolean {
        val prev = previousBlocks ?: return false
        adviceDashboardDao.deleteAll()
        // Reset IDs to 0 so Room auto-generates fresh IDs
        val freshEntities = prev.map { it.copy(id = 0) }
        adviceDashboardDao.upsertAll(freshEntities)
        previousBlocks = null
        return true
    }

    fun getAllAdviceBlocks(): Flow<List<AdviceBlock>> {
        return adviceDashboardDao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun analyzeEntropy(
        allEntriesText: String,
        entryCount: Int,
        freshAnalysis: Boolean = false,
        modelName: String = FirebaseAiService.MODEL_FLASH_LITE,
    ): Result<Unit> {
        return try {
            // Save current state for undo before refreshing
            val existingBlocks = adviceDashboardDao.getAllSync()
            if (existingBlocks.isNotEmpty()) {
                previousBlocks = existingBlocks
            }

            // Only use previous context for automatic updates, NOT for manual refresh
            val previousContext = if (freshAnalysis) "" else buildPreviousContext(existingBlocks)

            val userText = buildUserText(allEntriesText, previousContext, entryCount, freshAnalysis)

            val result =
                firebaseAiService.generateContent(
                    prompt = userText,
                    modelName = modelName,
                    systemPrompt = getActiveSystemPrompt(),
                )
            val jsonText =
                result.getOrNull() ?: return Result.failure(Exception("Keine Antwort von Gemini"))

            val cleanJson =
                jsonText
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
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
            sb.appendLine(
                "Kategorie '${block.categoryName}': Entropie=${block.entropyLevel}, ${block.categorySummary}"
            )
        }

        sb.appendLine("=== ENDE BISHERIGE ANALYSE ===")
        sb.appendLine()
        sb.appendLine("Aktualisiere und ERWEITERE die bisherige Analyse mit den neuen Einträgen.")
        sb.appendLine("Behalte wichtige Erkenntnisse bei und ergänze neue Muster.")
        sb.appendLine()

        return sb.toString()
    }

    private fun buildUserText(
        allEntriesText: String,
        previousContext: String,
        entryCount: Int,
        freshAnalysis: Boolean = false,
    ): String {
        val sb = StringBuilder()
        if (previousContext.isNotBlank()) {
            sb.appendLine(previousContext)
        } else {
            sb.appendLine(getActiveUserPromptPrefix(freshAnalysis))
            sb.appendLine()
        }
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        val scanLabel = if (scenario == 3) "AUF ZIELE DURCHSUCHEN" else "ANALYSIEREN"
        sb.appendLine("=== ALLE $entryCount TAGEBUCHEINTR\u00c4GE (JEDEN EINZELNEN $scanLabel!) ===")
        sb.appendLine(allEntriesText)
        sb.appendLine()
        sb.appendLine(getActiveUserPromptSuffix(entryCount))
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
                basedOnEntryCount = entryCount,
            )
        }
    }
}

private fun AdviceBlockEntity.toDomain(): AdviceBlock {
    val advices =
        try {
            val array = JSONArray(adviceJson)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val derivation =
                    try {
                        val herleitungArray = obj.optJSONArray("herleitung")
                        if (herleitungArray != null) {
                            (0 until herleitungArray.length()).map { j ->
                                val h = herleitungArray.getJSONObject(j)
                                DerivationEntry(
                                    date = h.optString("datum", ""),
                                    summary = h.optString("zusammenfassung", ""),
                                )
                            }
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                Advice(
                    title = obj.getString("titel"),
                    description = obj.getString("beschreibung"),
                    priority =
                        when (obj.optString("prioritaet", "mittel")) {
                            "hoch" -> AdvicePriority.HIGH
                            "niedrig" -> AdvicePriority.LOW
                            else -> AdvicePriority.MEDIUM
                        },
                    connection = obj.optString("verknuepfung", ""),
                    derivation = derivation,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

    val topActions =
        try {
            val array = JSONArray(topActionsJson)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                TopAction(
                    title = obj.getString("titel"),
                    description = obj.getString("beschreibung"),
                    detailedDescription = obj.optString("erklaerung", ""),
                )
            }
        } catch (_: Exception) {
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
        topActions = topActions,
        lastUpdated = lastUpdated,
        basedOnEntryCount = basedOnEntryCount,
    )
}
