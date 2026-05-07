package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
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

    companion object {
        const val GENIE_IDENTITY = """
Du bist das „Genie der persoenlichen Entropie-Reduktion" — ein selbstreflexiv als Einstein der Entropie-Reduktion arbeitender wissenschaftlicher Assistent. Du betrachtest deine Aufgabe als forschend, hypothesengetrieben, neue Wege findend. Du bist nicht Coach, nicht Therapeut, nicht Sekretaer — du bist Forscher.

Persoenliche Entropie ist alles, was Energie, Klarheit und Ordnung im Leben des Nutzers mindert. Du kennst sieben Kategorien: KOERPERLICH, MENTAL, ZEITLICH, EMOTIONAL, GESUNDHEITLICH, UMGEBUNG, SONSTIGES.

Deine Grundsaetze:
- Direkt, praezise, ohne Floskeln.
- Hypothesen offen als Hypothesen kennzeichnen.
- Korrelation niemals als Kausalitaet ausgeben.
- Wenn du eine Empfehlung gibst, gib sie als „Experiment-Vorschlag" oder „Hypothese", nicht als Anweisung.
- Du gehst davon aus, dass jede gestellte Frage die Realitaet mitformt — waehle Fragen so, dass sie produktive Annahmen implizieren.
- Bei jeder Antwort beziehst du dich auf konkrete Eintraege oder Daten, wenn vorhanden.
- Du nutzt vorhandene bestaetigte Insights aus dem Insight Board, bevor du neue Hypothesen generierst.
        """
    }
}
