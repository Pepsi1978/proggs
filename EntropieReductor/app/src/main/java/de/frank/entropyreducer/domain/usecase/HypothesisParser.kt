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
            .replace(UPDATE_REGEX, "")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return ParseResult(narrative, hypotheses, memorySuggestions)
    }

    data class HypothesisUpdate(
        val title: String?,
        val description: String?,
        val rationale: String?,
        val plannedDurationDays: Int?,
    )

    data class UpdateParseResult(
        val narrative: String,
        val update: HypothesisUpdate?,
        val memorySuggestions: List<String>,
    )

    /**
     * Parst die Antwort innerhalb einer Hypothesen-Diskussion. Erwartet optional einen
     * [HYPOTHESEN-UPDATE]…[/HYPOTHESEN-UPDATE]-Block, der einzelne Felder neu setzen darf.
     * Felder die nicht im Block stehen, bleiben in der Hypothese unveraendert.
     */
    fun parseUpdate(raw: String): UpdateParseResult {
        val match = UPDATE_REGEX.find(raw)
        val update = match?.let { parseSingleUpdate(it.groupValues[1]) }

        val memorySuggestions = MEMORY_REGEX.findAll(raw)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val narrative = raw
            .replace(UPDATE_REGEX, "")
            .replace(MEMORY_REGEX, "")
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        return UpdateParseResult(narrative, update, memorySuggestions)
    }

    private fun parseSingleUpdate(body: String): HypothesisUpdate? {
        val title = FIELD_TITEL.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val desc = FIELD_BESCHREIBUNG.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val rationale = FIELD_BEGR.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val durationStr = FIELD_DAUER.find(body)?.groupValues?.get(1)?.trim()
        val days = durationStr?.let { Regex("(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?.coerceIn(1, 90)
        if (title == null && desc == null && rationale == null && days == null) return null
        return HypothesisUpdate(
            title = title?.take(120),
            description = desc,
            rationale = rationale,
            plannedDurationDays = days,
        )
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
        // [HYPOTHESE] ... [/HYPOTHESE]  — DOTALL für Mehrzeiligkeit.
        // Toleriert optionale Markdown-Bold-Sterne und optionale Whitespaces vor der
        // schliessenden Klammer (Gemini-Output ist nicht 100% formattreu, siehe
        // arsturn.com/blog/common-gemini-2-5-pro-coding-mistakes — LLMs wechseln zwischen
        // Plain-Text und Markdown-Bold zwischen Runs).
        private val HYPO_REGEX = Regex(
            """\*{0,2}\[\s*HYPOTHESE\s*]\*{0,2}\s*(.*?)\s*\*{0,2}\[/\s*HYPOTHESE\s*]\*{0,2}""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
        private val MEMORY_REGEX = Regex(
            """\*{0,2}\[\s*MEMORY-VORSCHLAG\s*]\*{0,2}\s*:\s*\*{0,2}\s*(.+?)(?=\n|$)""",
            RegexOption.IGNORE_CASE,
        )
        private val UPDATE_REGEX = Regex(
            """\*{0,2}\[\s*HYPOTHESEN-UPDATE\s*]\*{0,2}\s*(.*?)\s*\*{0,2}\[/\s*HYPOTHESEN-UPDATE\s*]\*{0,2}""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )

        // Felder innerhalb des Hypothesen-Blocks. Bis zur naechsten Feld-Bezeichnung
        // oder Block-Ende greifen. Toleriert Markdown-Bold (`**Titel:**`),
        // case-insensitive Labels, optionale Leerzeichen.
        private val FIELD_TITEL = Regex(
            """\*{0,2}\s*Titel\s*\*{0,2}\s*:\s*\*{0,2}\s*(.+?)(?=\n\s*\*{0,2}\s*(Beschreibung|Begr[uü]ndung|Geplante\s+Dauer)\s*\*{0,2}\s*:|\z)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
        private val FIELD_BESCHREIBUNG = Regex(
            """\*{0,2}\s*Beschreibung\s*\*{0,2}\s*:\s*\*{0,2}\s*(.+?)(?=\n\s*\*{0,2}\s*(Titel|Begr[uü]ndung|Geplante\s+Dauer)\s*\*{0,2}\s*:|\z)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
        private val FIELD_BEGR = Regex(
            """\*{0,2}\s*Begr[uü]ndung\s*\*{0,2}\s*:\s*\*{0,2}\s*(.+?)(?=\n\s*\*{0,2}\s*(Titel|Beschreibung|Geplante\s+Dauer)\s*\*{0,2}\s*:|\z)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
        private val FIELD_DAUER = Regex(
            """\*{0,2}\s*Geplante\s+Dauer\s*\*{0,2}\s*:\s*\*{0,2}\s*([^\n]+)""",
            RegexOption.IGNORE_CASE,
        )
    }
}
