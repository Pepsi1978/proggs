package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Setzt den System-Prompt aus mehreren Schichten zusammen — Reihenfolge nach Spec §7:
 * 1. Basis-Prompt je Use Case (UseCase liefert)
 * 2. Genie-Identitaet (immer)
 * 3. Profil-Text
 * 4. Aktive Memory-Eintraege
 * 5. Biomarker-Kontext
 * 6. Kalender-Kontext
 * 7. Aktive Nutzer-Prompts
 * 8. Use-Case-spezifischer Schluss
 */
@Singleton
class SystemPromptBuilder @Inject constructor() {

    fun build(
        basePrompt: String,
        profileText: String,
        memories: List<MemoryEntryEntity>,
        biomarker: BiomarkerSnapshotEntity?,
        calendarToday: CalendarDayEntity?,
        calendarTomorrow: CalendarDayEntity?,
        userPrompts: List<SavedPromptEntity>,
        tail: String? = null,
        recentResolutionMethods: List<String> = emptyList(),
        archivedResolutionEntries: List<ArchivedMethodSummary> = emptyList(),
        activeHypotheses: List<HypothesisEntity> = emptyList(),
        confirmedInsights: List<InsightEntity> = emptyList(),
        priorityMemories: List<de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity> =
            emptyList(),
    ): String = buildString {
        appendLine(basePrompt.trim())
        appendLine()
        appendLine(GENIE_IDENTITY.trim())
        appendLine()

        if (profileText.isNotBlank()) {
            appendLine("## Profil")
            appendLine(profileText.trim())
            appendLine()
        }

        if (memories.isNotEmpty()) {
            appendLine("## Aktives Gedaechtnis")
            memories.forEach { m -> appendLine("- ${m.content.trim()}") }
            appendLine()
        }

        if (confirmedInsights.isNotEmpty()) {
            appendLine("## Bestätigte Methoden im Insight-Board (wirken nachgewiesen entropie-reduzierend)")
            appendLine("Diese Methoden hat Frank als wirksam bestätigt — sie haben hohe Konfidenz und sollen bei der Bewertung neuer Aufgaben mitberücksichtigt werden. Wenn eine neue Aufgabe durch eine dieser Methoden lösbar oder unterstützbar ist, erhöhe ihren priorityScore: +5 bis +20 abhängig von Konfidenz und Breite der Methode-Wirkung. Methoden die mehrere Kategorien gleichzeitig adressieren (Wellbeing-First-Hebel) wirken stärker und heben den Score deutlicher.")
            confirmedInsights.forEach { i ->
                val cats = (listOf(i.targetCategory) + i.additionalCategories).joinToString(", ") { it.name }
                appendLine("- \"${i.title}\" [$cats] (Konfidenz ${i.confidence}%, ${i.successCount}/${i.attemptCount} Erfolge)")
                if (i.description.isNotBlank()) appendLine("  → ${i.description.trim()}")
            }
            appendLine()
        }

        if (priorityMemories.isNotEmpty()) {
            appendLine("## Prioritaets-Gedaechtnis (frueher manuell gesetzte Prioritaeten)")
            appendLine(
                "Vergleiche die neue Aufgabe SPEZIFISCH mit diesen Eintraegen (genaue Match-Regeln " +
                    "in der priorityScore-Doktrin unten). Format je Eintrag: Titel | Beschreibung | Prioritaet:",
            )
            priorityMemories.forEach { m ->
                appendLine("- \"${m.title}\" | ${m.description} | Prioritaet ${m.priority.toInt()}")
            }
            appendLine()
        }

        if (recentResolutionMethods.isNotEmpty()) {
            appendLine("## Bewaehrte Methoden des Nutzers (zuletzt erfolgreich angewandt)")
            appendLine("Diese Methoden hat Frank in den letzten Wochen explizit als wirksam dokumentiert. Sie sind reale, gelebte Erfahrung — nicht Theorie. Beziehe sie aktiv in deine Hypothesen ein, vermeide Wiederholungs-Vorschlaege, baue darauf auf:")
            recentResolutionMethods.forEach { method -> appendLine("- ${method.trim()}") }
            appendLine()
        }

        if (archivedResolutionEntries.isNotEmpty()) {
            appendLine("## Langzeit-Archiv (vergangene Entropie-Reduktionen)")
            appendLine("Diese erledigten Aufgaben sind älter als 14 Tage und liegen im Archiv. Nutze sie um Muster und Zusammenhaenge zu erkennen — wiederkehrende Aufgabentypen, dauerhaft wirksame Methoden, saisonale Verlaeufe. Pro Eintrag siehst du Aufgabe + Kategorie + die vom Nutzer eingesprochene Lösungsmethode:")
            archivedResolutionEntries.forEach { e ->
                appendLine("- [${e.category}] \"${e.title}\" (${e.dateLabel})")
                if (e.method.isNotBlank()) appendLine("  → Methode: ${e.method.trim()}")
            }
            appendLine()
        }

        if (activeHypotheses.isNotEmpty()) {
            appendLine("## Laufende Experimente (gerade aktiv)")
            appendLine("Diese Hypothesen testet Frank gerade — schlage keine Hypothese vor, die mit einem laufenden Experiment kollidiert oder es trivialerweise wiederholt:")
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.").withZone(ZoneId.systemDefault())
            activeHypotheses.forEach { h ->
                val start = dateFormatter.format(Instant.ofEpochMilli(h.actualStartDate ?: h.plannedStartDate))
                val end = dateFormatter.format(Instant.ofEpochMilli(h.plannedEndDate))
                appendLine("- \"${h.title}\" ($start–$end): ${h.description}")
            }
            appendLine()
        }

        biomarker?.let { snapshot ->
            appendLine("## Biomarker — heute")
            snapshot.recoveryScore?.let { appendLine("- Recovery: $it%") }
            snapshot.hrvMs?.let { appendLine("- HRV: ${"%.1f".format(it)} ms") }
            snapshot.restingHeartRate?.let { appendLine("- Ruhepuls: $it bpm") }
            snapshot.sleepPerformance?.let { appendLine("- Schlafqualitaet: $it%") }
            snapshot.sleepTotalMinutes?.let { appendLine("- Schlafdauer: ${it / 60}h ${it % 60}min") }
            appendLine()
        }

        if (calendarToday != null || calendarTomorrow != null) {
            appendLine("## Kalender-Kontext")
            calendarToday?.let { appendLine("- Heute: ${it.shiftCode.name} (verfuegbar ~${it.availableMinutesEstimate} Min.)") }
            calendarTomorrow?.let { appendLine("- Morgen: ${it.shiftCode.name} (verfuegbar ~${it.availableMinutesEstimate} Min.)") }
            appendLine()
        }

        if (userPrompts.isNotEmpty()) {
            appendLine("## Eigene Prompts (Verhaltensregeln)")
            userPrompts.forEach { p ->
                appendLine("### ${p.name}")
                appendLine(p.content.trim())
                appendLine()
            }
        }

        tail?.let {
            appendLine()
            appendLine(it.trim())
        }
    }

    /**
     * Kompakte Repraesentation eines archivierten Eintrags fuer den Forscher-Prompt.
     * Frank-Wunsch 2026-05-09: das Archiv liefert dem Forscher Stoff fuer
     * Mustererkennung und neue Hypothesen.
     */
    data class ArchivedMethodSummary(
        val title: String,
        val category: String,
        val method: String,
        val dateLabel: String,
    )

    companion object {
        const val GENIE_IDENTITY = """
Du bist das „Genie der persoenlichen Entropie-Reduktion" — ein selbstreflexiv als Einstein der Entropie-Reduktion arbeitender wissenschaftlicher Assistent. Du betrachtest deine Aufgabe als forschend, hypothesengetrieben, neue Wege findend. Du bist nicht Coach, nicht Therapeut, nicht Sekretaer — du bist Forscher.

Persoenliche Entropie ist alles, was Energie, Klarheit und Ordnung im Leben des Nutzers mindert. Du kennst sieben Kategorien: KOERPERLICH, MENTAL, ZEITLICH, EMOTIONAL, GESUNDHEITLICH, UMGEBUNG, SONSTIGES.

Deine Grundsaetze:
- Direkt, präzise, ohne Floskeln.
- Hypothesen offen als Hypothesen kennzeichnen.
- Korrelation niemals als Kausalitaet ausgeben.
- Wenn du eine Empfehlung gibst, gib sie als „Experiment-Vorschlag" oder „Hypothese", nicht als Anweisung.
- Du gehst davon aus, dass jede gestellte Frage die Realität mitformt — wähle Fragen so, dass sie produktive Annahmen implizieren.
- Bei jeder Antwort beziehst du dich auf konkrete Eintraege oder Daten, wenn vorhanden.
- Du nutzt vorhandene bestätigte Insights aus dem Insight Board, bevor du neue Hypothesen generierst.
        """
    }
}
