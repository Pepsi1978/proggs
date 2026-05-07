package de.frank.entropyreducer.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parst die KI-Antwort des Wissenschaftlers nach `[HYPOTHESE]…[/HYPOTHESE]`-Bloecken
 * und `[MEMORY-VORSCHLAG]:`-Zeilen. Spec §12.6.
 *
 * Extrahiert ausserdem den narrativen Klartext (alles ausserhalb der Marker-Bloecke).
 */
@Singleton
class HypothesisParser @Inject constructor() {

    data class ParsedHypothesis(
        val title: String,
        val description: String,
        val rationale: String,
        val plannedDurationDays: Int,
    )

    data class ParseResult(
        /** Klartext der KI-Antwort (ohne Marker-Bloecke). */
        val narrative: String,
        val hypotheses: List<ParsedHypothesis>,
        val memorySuggestions: List<String>,
    )

    fun parse(raw: String): ParseResult {
        val hypotheses = HYPO_REGEX.findAll(raw).mapNotNull { m ->
            parseSingleHypothesis(m.groupValues[1])
        }.toList()

        val memorySuggestions = MEMORY_REGEX.findAll(raw)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()

        // Narrativ: alle Marker-Bloecke entfernen.
        val narrative = raw
            .replace(HYPO_REGEX, "")
            .replace(MEMORY_REGEX, "")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return ParseResult(narrative, hypotheses, memorySuggestions)
    }

    private fun parseSingleHypothesis(body: String): ParsedHypothesis? {
        val title = FIELD_TITEL.find(body)?.groupValues?.get(1)?.trim().orEmpty()
        val desc = FIELD_BESCHREIBUNG.find(body)?.groupValues?.get(1)?.trim().orEmpty()
        val rationale = FIELD_BEGR.find(body)?.groupValues?.get(1)?.trim().orEmpty()
        val durationStr = FIELD_DAUER.find(body)?.groupValues?.get(1)?.trim().orEmpty()
        if (title.isBlank() || desc.isBlank()) return null
        val days = Regex("(\\d+)").find(durationStr)?.groupValues?.get(1)?.toIntOrNull() ?: 7
        return ParsedHypothesis(
            title = title.take(120),
            description = desc,
            rationale = rationale,
            plannedDurationDays = days.coerceIn(1, 90),
        )
    }

    companion object {
        // [HYPOTHESE] ... [/HYPOTHESE]  — DOTALL: Inhalt darf Zeilenumbrueche haben.
        private val HYPO_REGEX =
            Regex("""\[HYPOTHESE]\s*(.*?)\s*\[/HYPOTHESE]""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        private val MEMORY_REGEX =
            Regex("""\[MEMORY-VORSCHLAG]:\s*(.+?)(?=\n|$)""", RegexOption.IGNORE_CASE)

        // Felder innerhalb des Hypothesen-Blocks. Bis zur naechsten Feld-Bezeichnung
        // oder Block-Ende greifen — daher ein Lookahead auf das naechste Label.
        private val FIELD_TITEL =
            Regex("""Titel:\s*(.+?)(?=\n\s*(Beschreibung|Begr[uü]ndung|Geplante\s+Dauer):|\z)""", RegexOption.DOT_MATCHES_ALL)
        private val FIELD_BESCHREIBUNG =
            Regex("""Beschreibung:\s*(.+?)(?=\n\s*(Titel|Begr[uü]ndung|Geplante\s+Dauer):|\z)""", RegexOption.DOT_MATCHES_ALL)
        private val FIELD_BEGR =
            Regex("""Begr[uü]ndung:\s*(.+?)(?=\n\s*(Titel|Beschreibung|Geplante\s+Dauer):|\z)""", RegexOption.DOT_MATCHES_ALL)
        private val FIELD_DAUER =
            Regex("""Geplante\s+Dauer:\s*([^\n]+)""", RegexOption.IGNORE_CASE)
    }
}
