package com.bestjournal.app.data.repository

import com.bestjournal.app.data.local.dao.AdviceDashboardDao
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
) {
    private val entropyAnalysisSystemPrompt =
        """
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
        - Schreibe in einfacher, klarer Sprache. Kurze Sätze. Keine Fremdwörter.
          Jeder soll den Text sofort verstehen können, ohne nachzudenken.

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

        Weitere verfügbare Icons (nutze den passendsten für das erkannte Thema):
        Gesundheit: health, medical, spa, coffee, fitness_center, directions_run
        Psyche: psychology, self_improvement, mood_bad, nights_stay
        Beziehungen: people, child_care, cake (Dating), volunteer_activism
        Arbeit: work, trending_up (Karriere), school, code, computer, timer
        Finanzen: finance, account_balance, shopping
        Alltag: home, cleaning_services (Haushalt), directions_car, gavel (Recht/Bürokratie)
        Freizeit: menu_book, music_note, palette (Kunst), sports_esports, photo_camera, grass (Hobby/Garten), travel (Urlaub), pets
        Sucht: smoking_rooms (Rauchen), local_bar (Alkohol/Sucht)
        Spiritualität: church
        Sonstiges: nature, wb_sunny, lightbulb, star, emoji_events, thermostat

        Verwende KURZE Kategorienamen (1-2 Wörter, max. 15 Zeichen). Beispiele:
        RICHTIG: "Schlaf", "Arbeit", "Fitness", "Finanzen", "Karriere", "Haushalt"
        FALSCH: "Persönliche Entwicklung" (zu lang) → stattdessen "Entwicklung"
        FALSCH: "Mentale Gesundheit" (zu lang) → stattdessen "Psyche"

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
          "top_massnahmen": [
            {
              "titel": "Kurzer, prägnanter Titel der Maßnahme",
              "beschreibung": "Konkrete Beschreibung in 2 Zeilen — was genau tun und warum es die Entropie stark senkt"
            }
          ],
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

        TOP-5-MASSNAHMEN (PFLICHT — "top_massnahmen"):
        Identifiziere aus ALLEN Einträgen, der Gesamtanalyse und allen Kategorien die
        5 wichtigsten, allumfassendsten Maßnahmen, die die persönliche Entropie des
        Tagebuchschreibers in der Zukunft am STÄRKSTEN und NACHHALTIGSTEN senken würden.
        - Genau 5 Maßnahmen, sortiert nach Stärke der Entropie-Senkung (stärkste zuerst).
        - Jede Maßnahme in 2 Zeilen: Was genau tun + warum es die Entropie stark senkt.
        - Diese Maßnahmen sollen KATEGORIEÜBERGREIFEND sein — sie fassen die wichtigsten
          Erkenntnisse aus ALLEN Kategorien zusammen.
        - Sei konkret und direkt — keine Allgemeinplätze wie "mehr Sport" sondern
          "Täglich 20 Minuten spazieren gehen nach der Arbeit" basierend auf den Einträgen.
        - Denke ganzheitlich: Welche 5 Veränderungen hätten den größten Dominoeffekt?

        Regeln:
        - Schreibe auf Deutsch, einfach und direkt, ohne Floskeln und ohne Fremdwörter.
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
        """
            .trimIndent()

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

            val userText = buildUserText(allEntriesText, previousContext, entryCount)

            val result =
                firebaseAiService.generateContent(
                    prompt = userText,
                    modelName = modelName,
                    systemPrompt = entropyAnalysisSystemPrompt,
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
    ): String {
        val sb = StringBuilder()
        if (previousContext.isNotBlank()) {
            sb.appendLine(previousContext)
        } else {
            sb.appendLine(
                "=== FRISCHE ANALYSE — Erstelle eine komplett neue, eigenständige Analyse. ==="
            )
            sb.appendLine()
        }
        sb.appendLine("=== ALLE $entryCount TAGEBUCHEINTRÄGE (JEDEN EINZELNEN ANALYSIEREN!) ===")
        sb.appendLine(allEntriesText)
        sb.appendLine()
        sb.appendLine(
            "=== ERINNERUNG: Du hast $entryCount Einträge erhalten. JEDER muss in der Analyse berücksichtigt werden. ==="
        )
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
