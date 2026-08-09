package de.frank.experimente.ai

import de.frank.experimente.data.local.Auswertung
import de.frank.experimente.data.local.Erkenntnis
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.GespraechsRunde
import de.frank.experimente.data.local.LogTag
import de.frank.experimente.data.local.MerkEintrag
import de.frank.experimente.data.local.Sprecher
import de.frank.experimente.data.local.Ziel

/**
 * Alles, was die KI über Frank weiß — in der Reihenfolge aus 01-FUNKTIONS-SPEC.md F-03:
 * Selbstbild · Wünsche & Ziele · aktuelles Log (15 Tage) · Langzeit-Log · Erkenntnisse ·
 * laufende Experimente · heutige Lage.
 *
 * **Das Wichtigste am ganzen Programm** (00-PROJEKT.md, Rangfolge 1): Die KI muss Frank
 * wirklich kennen. Im Zweifel gewinnt der Kontext.
 */
data class Gedaechtnis(
    val selbstbild: String,
    val ziele: List<Ziel>,
    val aktuellesLog: List<LogTag>,
    val langzeitLog: List<LogTag>,
    val erkenntnisse: List<Erkenntnis>,
    val laufende: List<Experiment>,
    val merkliste: List<MerkEintrag>,
    val heutigeLage: String,
) {
    fun alsText(): String = buildString {
        abschnitt("WER FRANK IST", selbstbild.ifBlank { "(noch nichts hinterlegt)" })

        abschnitt(
            "SEINE WÜNSCHE UND ZIELE",
            if (ziele.isEmpty()) "(noch keine)" else ziele.joinToString("\n") { "- ${it.text}" },
        )

        abschnitt(
            "DIE LETZTEN TAGE, AUSFÜHRLICH",
            if (aktuellesLog.isEmpty()) "(noch nichts)" else aktuellesLog
                .sortedBy { it.datum }
                .joinToString("\n\n") { "### ${it.datum}\n${it.ausfuehrlich.orEmpty()}" },
        )

        abschnitt(
            "FRÜHER, VERDICHTET — auch die Prüfliste gegen Wiederholungen",
            if (langzeitLog.isEmpty()) "(noch nichts)" else langzeitLog
                .sortedBy { it.datum }
                .joinToString("\n") { "### ${it.datum}\n${it.verdichtet.orEmpty()}" },
        )

        abschnitt(
            "WAS ER ÜBER SICH GELERNT HAT",
            if (erkenntnisse.isEmpty()) "(noch nichts)" else erkenntnisse.joinToString("\n") { "- ${it.text}" },
        )

        abschnitt(
            "WAS GERADE LÄUFT",
            if (laufende.isEmpty()) "(nichts)" else laufende.joinToString("\n") {
                "- ${it.titel} (${it.stufe.anzeige}, ${it.tage} Tag(e), begonnen ${it.begonnenAm})"
            },
        )

        abschnitt(
            "SEINE MERKLISTE",
            if (merkliste.isEmpty()) "(leer)" else merkliste.joinToString("\n") {
                buildString {
                    append("- ${it.titel}: ${it.beschreibung}")
                    it.vermerk?.let { v -> append("\n  (beim letzten Mal im Weg: $v)") }
                }
            },
        )

        abschnitt("SEINE LAGE HEUTE", heutigeLage.ifBlank { "(nicht eingesprochen)" })
    }

    private fun StringBuilder.abschnitt(titel: String, inhalt: String) {
        append("## ").append(titel).append('\n').append(inhalt.trim()).append("\n\n")
    }
}

/** Der Gesprächsfaden zu einem Experiment, als Text für die Anfrage. */
fun List<GespraechsRunde>.alsFaden(): String =
    if (isEmpty()) "(noch kein Gespräch)" else joinToString("\n\n") {
        val wer = if (it.sprecher == Sprecher.ICH) "Frank" else "Du"
        "$wer: ${it.text}"
    }

/** Die bisherigen Auswertungen eines Experiments, als Text für die Anfrage. */
fun List<Auswertung>.alsVerlauf(): String =
    if (isEmpty()) "(noch keine)" else joinToString("\n\n") {
        buildString {
            append("### ${it.datum}\nFrank: ${it.eigenerText}")
            it.kiText?.let { k -> append("\nDeine Einschätzung damals: $k") }
        }
    }
