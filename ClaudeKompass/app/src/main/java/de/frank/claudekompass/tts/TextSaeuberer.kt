package de.frank.claudekompass.tts

/**
 * Macht aus Anzeigetext gesprochenen Text.
 *
 * Ohne diesen Schritt liest die Stimme „Sternchen Sternchen Compact Sternchen Sternchen" und
 * buchstabiert Internetadressen zeichenweise. Entfernt werden deshalb Auszeichnungen, Code,
 * Adressen und Emoji — der Sinn bleibt, der Zeichensalat geht.
 */
object TextSaeuberer {

    private val codeBlockRegex = Regex("```[\\s\\S]*?```")
    private val inlineCodeRegex = Regex("`([^`\\n]+)`")
    private val fettRegex = Regex("\\*\\*(.+?)\\*\\*")
    private val kursivRegex = Regex("(?<![*\\w])\\*([^*\\n]+)\\*(?![*\\w])")
    private val ueberschriftRegex = Regex("(?m)^#{1,6}\\s*")
    private val aufzaehlungRegex = Regex("(?m)^\\s*[-*•]\\s+")
    private val nummerierungRegex = Regex("(?m)^\\s*\\d+[.)]\\s+")
    private val markdownLinkRegex = Regex("\\[([^\\]]+)]\\([^)\\s]+(?:\\s+\"[^\"]*\")?\\)")
    private val nackteAdresseRegex = Regex("\\b(?:https?://|www\\.)[^\\s<>()]+", RegexOption.IGNORE_CASE)
    private val tabellenTrennerRegex = Regex("(?m)^\\s*\\|?[\\s:|-]{6,}\\|?\\s*$")
    private val mehrfachLeerzeileRegex = Regex("\\n{3,}")
    private val leerraumVorSatzzeichenRegex = Regex("[ \\t]+([,.;:!?])")

    fun saeubere(text: String): String {
        var ergebnis = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        ergebnis = ergebnis
            // Ein Code-Block vorgelesen ist reine Qual. Er wird durch einen Hinweis ersetzt,
            // statt ihn ersatzlos zu streichen — sonst fehlt im Gehörten der Zusammenhang.
            .replace(codeBlockRegex, " (Codebeispiel) ")
            .replace(tabellenTrennerRegex, "")
            .replace(markdownLinkRegex, "$1")
            .replace(nackteAdresseRegex, " (Internetadresse) ")
            .replace(inlineCodeRegex, "$1")
            .replace(fettRegex, "$1")
            .replace(kursivRegex, "$1")
            .replace(ueberschriftRegex, "")
            .replace(aufzaehlungRegex, "")
            .replace(nummerierungRegex, "")
            .replace("|", " ")

        ergebnis = entferneEmoji(ergebnis)

        return ergebnis
            .replace(leerraumVorSatzzeichenRegex, "$1")
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex("(?m)[ \\t]+$"), "")
            .replace(mehrfachLeerzeileRegex, "\n\n")
            .trim()
    }

    /**
     * Zerlegt den Text in Vorlese-Einheiten (Referenz, Baustein D, Punkt 1 bis 3).
     *
     * Ein Absatz ist eine Einheit. Absätze werden weder zusammengelegt noch mitten drin
     * geteilt — nur ein Absatz über [MAX_ZEICHEN] wird an Satzgrenzen aufgeteilt, weil die
     * Dienste sonst die Anfrage ablehnen.
     */
    fun teileInAbsaetze(text: String, maxZeichen: Int = MAX_ZEICHEN): List<String> {
        val bereinigt = saeubere(text)
        if (bereinigt.isBlank()) return emptyList()
        return bereinigt
            .split(Regex("\n{2,}"))
            .map { it.replace('\n', ' ').replace(Regex("\\s{2,}"), " ").trim() }
            .filter { it.isNotBlank() }
            .flatMap { teileLangenAbsatz(it, maxZeichen) }
    }

    private fun teileLangenAbsatz(absatz: String, maxZeichen: Int): List<String> {
        if (absatz.length <= maxZeichen) return listOf(absatz)
        val saetze = absatz.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val teile = mutableListOf<String>()
        var laufend = ""

        fun schreibeWeg() {
            if (laufend.isNotBlank()) {
                teile += laufend.trim()
                laufend = ""
            }
        }

        for (satz in saetze) {
            if (satz.length > maxZeichen) {
                schreibeWeg()
                teile += teileAnWortgrenzen(satz, maxZeichen)
            } else {
                val kandidat = if (laufend.isBlank()) satz else "$laufend $satz"
                if (kandidat.length <= maxZeichen) {
                    laufend = kandidat
                } else {
                    schreibeWeg()
                    laufend = satz
                }
            }
        }
        schreibeWeg()
        return teile
    }

    /** Letzter Ausweg für einen einzelnen Satz ohne jede Satzgrenze. */
    private fun teileAnWortgrenzen(text: String, maxZeichen: Int): List<String> {
        val teile = mutableListOf<String>()
        var rest = text.trim()
        while (rest.length > maxZeichen) {
            val kandidat = rest.take(maxZeichen).indexOfLast { it == ' ' }
            // Liegt die letzte Lücke im ersten Drittel, wäre das Stück lächerlich kurz —
            // dann lieber hart schneiden.
            val schnitt = if (kandidat > maxZeichen / 2) kandidat else maxZeichen
            teile += rest.take(schnitt).trim()
            rest = rest.drop(schnitt).trim()
        }
        if (rest.isNotBlank()) teile += rest
        return teile
    }

    private fun entferneEmoji(text: String): String {
        val ergebnis = StringBuilder(text.length)
        var stelle = 0
        while (stelle < text.length) {
            val zeichen = text.codePointAt(stelle)
            if (istEmoji(zeichen)) {
                if (ergebnis.isNotEmpty() && !ergebnis.last().isWhitespace()) ergebnis.append(' ')
            } else {
                ergebnis.appendCodePoint(zeichen)
            }
            stelle += Character.charCount(zeichen)
        }
        return ergebnis.toString()
    }

    private fun istEmoji(zeichen: Int): Boolean = when (zeichen) {
        in 0x1F000..0x1FAFF,
        in 0x2600..0x27BF,
        in 0x2300..0x23FF,
        in 0x2B00..0x2BFF,
        in 0x1F1E6..0x1F1FF,
        in 0x1F3FB..0x1F3FF,
        in 0xE0020..0xE007F,
        0x00A9, 0x00AE, 0x203C, 0x2049, 0x200D, 0x20E3, 0x2122, 0x2139,
        0x25AA, 0x25AB, 0x25B6, 0x25C0, 0x3030, 0x303D, 0x3297, 0x3299,
        0xFE0E, 0xFE0F,
        -> true
        in 0x25FB..0x25FE -> true
        else -> false
    }

    /** Sicherheitsgrenze der Vorlese-Dienste, nicht die gewünschte Absatzlänge. */
    const val MAX_ZEICHEN = 1000
}
