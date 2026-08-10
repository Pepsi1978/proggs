package de.frank.experimente.ai

import de.frank.experimente.data.local.ChatTurn
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.Goal
import de.frank.experimente.data.local.Insight
import de.frank.experimente.data.local.LogDay
import de.frank.experimente.data.local.Rolle
import de.frank.experimente.data.local.WatchlistItem
import java.time.format.DateTimeFormatter

/**
 * Der Kontext, den die KI bekommt — **in der Reihenfolge aus F-03 Schritt 1**:
 * Selbstbild · Wünsche & Ziele · Aktuelles Log · Langzeit-Log · Erkenntnisse ·
 * laufende Experimente · heutige Lage.
 *
 * Die Reihenfolge ist nicht beliebig: Punkt 1 des Projekts sagt „Die KI muss Frank wirklich
 * kennen — im Zweifel gewinnt der Kontext", und das Selbstbild geht als **erster** Block ein
 * (F-21).
 */
data class Kontext(
    val selbstbild: String,
    val ziele: List<Goal>,
    val aktuellesLog: List<LogDay>,
    val langzeitLog: List<LogDay>,
    val erkenntnisse: List<Insight>,
    val laufende: List<Experiment>,
    val heutigeLage: String?,
    val merkliste: List<WatchlistItem> = emptyList(),
) {
    fun alsText(): String = buildString {
        abschnitt("SELBSTBILD", selbstbild.ifBlank { "(noch nichts hinterlegt)" })

        abschnitt(
            "WÜNSCHE UND ZIELE",
            if (ziele.isEmpty()) "(noch keine)"
            else ziele.joinToString("\n") { "- ${it.text}" },
        )

        abschnitt(
            "AKTUELLES LOG (die letzten 15 Tage, ausführlich)",
            if (aktuellesLog.isEmpty()) "(noch leer)"
            else aktuellesLog.joinToString("\n\n") { "${it.date.format(TAG)}:\n${it.detailText.orEmpty()}" },
        )

        abschnitt(
            "LANGZEIT-LOG (verdichtet, dauerhaft)",
            if (langzeitLog.isEmpty()) "(noch leer)"
            else langzeitLog.joinToString("\n\n") { "${it.date.format(TAG)}:\n${it.compactText.orEmpty()}" },
        )

        abschnitt(
            "ERKENNTNISSE",
            if (erkenntnisse.isEmpty()) "(noch keine)"
            else erkenntnisse.joinToString("\n") { "- ${it.text}" },
        )

        abschnitt(
            "LAUFENDE EXPERIMENTE",
            if (laufende.isEmpty()) "(keines offen)"
            else laufende.joinToString("\n") {
                "- ${it.title} (seit ${it.startedAt.format(TAG)}, ${it.days} Tag(e), Stufe ${it.level.name.lowercase()})"
            },
        )

        if (merkliste.isNotEmpty()) {
            abschnitt(
                "MERKLISTE",
                merkliste.joinToString("\n") { eintrag ->
                    "- ${eintrag.title}: ${eintrag.description}" +
                        (eintrag.note?.let { "\n  (beim letzten Mal im Weg: $it)" } ?: "")
                },
            )
        }

        heutigeLage?.takeIf { it.isNotBlank() }?.let { abschnitt("HEUTIGE LAGE", it) }
    }

    private fun StringBuilder.abschnitt(titel: String, inhalt: String) {
        if (isNotEmpty()) append("\n\n")
        append("## ").append(titel).append('\n').append(inhalt)
    }

    private companion object {
        val TAG: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}

/** Der bisherige Gesprächsfaden zu einem Experiment (F-09 Schritt 4). */
fun List<ChatTurn>.alsFaden(): String = joinToString("\n") { runde ->
    val wer = if (runde.role == Rolle.ICH) "Frank" else "Du"
    "$wer: ${runde.text}"
}
