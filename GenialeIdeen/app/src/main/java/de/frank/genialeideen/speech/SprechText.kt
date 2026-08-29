package de.frank.genialeideen.speech

import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.text.UmlautKorrektur

/**
 * Macht Text vorlesbar, bevor er an eine Stimme geht.
 *
 * Zwei Regeln stecken hier drin:
 *  - **Baustein M.3:** Was gesprochen wird, läuft vorher durch die Umlaut-Korrektur — sonst
 *    spricht die Stimme „Bueromoebel" statt „Büromöbel".
 *  - **TTS-freundlich (Zusatzregel):** Sonderzeichen, Internetadressen, Quellenangaben und
 *    Auszeichnungszeichen werden entfernt oder in Worte übersetzt. Ein TTS-Modell liest
 *    „Sternchen Sternchen", „h t t p s Doppelpunkt Schrägstrich" oder „(vgl. S. 12)" vor,
 *    und das zerstört jeden Satz.
 */
object SprechText {

    /** Adressen im Netz — werden ganz entfernt, nicht buchstabiert. */
    private val URL = Regex("""(?i)\b(?:https?://|www\.)\S+|\b[\w.-]+@[\w.-]+\.\w{2,}""")

    /** Quellenangaben in Klammern: (vgl. Meier 2020), (S. 12), [3], [Quelle: …]. */
    private val QUELLE = Regex(
        """\((?:vgl\.|siehe|vergleiche|Quelle:?|S\.|Seite|Abb\.|Kap\.)[^)]*\)""" +
            """|\[[^\]]{0,80}\]""",
        RegexOption.IGNORE_CASE,
    )

    /** Code in Zaunzeichen — vorgelesener Quelltext ergibt nie einen hörbaren Satz. */
    private val CODEBLOCK = Regex("```[\\s\\S]*?```")
    private val INLINE_CODE = Regex("`([^`]*)`")

    /** Auszeichnungszeichen, die als Zeichen gesprochen würden. */
    private val MARKDOWN_ZEICHEN = Regex("""[*_~>#|]""")

    /** Reste, die kein TTS gut spricht. Der Wert ist, wie es gesprochen werden soll. */
    private val ERSATZ = linkedMapOf(
        "&" to " und ",
        "%" to " Prozent ",
        "€" to " Euro ",
        "$" to " Dollar ",
        "+" to " plus ",
        "=" to " gleich ",
        "<" to " kleiner als ",
        ">" to " grösser als ",
        "→" to " führt zu ",
        "←" to " kommt von ",
        "•" to ". ",
        "·" to ". ",
        "…" to ". ",
        "„" to "",
        "“" to "",
        "”" to "",
        "\"" to "",
        "«" to "",
        "»" to "",
        "–" to " - ",
        "—" to " - ",
        "/" to " oder ",
        "\\" to " ",
        "^" to " ",
        "©" to "",
        "®" to "",
        "™" to "",
    )

    /** Zeichen, die ausser Buchstaben, Ziffern und der normalen Satzzeichen übrig bleiben. */
    private val UNERWUENSCHT = Regex("""[^\p{L}\p{N} .,!?;:()\-'\n]""")

    /**
     * Bereitet [roh] fürs Vorlesen auf: Auszeichnung raus, Adressen und Quellen raus,
     * Sonderzeichen in Worte, Umlaute wieder echt.
     */
    fun fuerStimme(roh: String): String {
        if (roh.isBlank()) return ""
        var text = roh
        text = CODEBLOCK.replace(text, " ")
        text = INLINE_CODE.replace(text) { it.groupValues[1] }
        text = URL.replace(text, " ")
        text = QUELLE.replace(text, " ")
        // Markdown-Links: [Beschriftung](Adresse) — die Beschriftung bleibt, die Adresse geht.
        text = Regex("""\[([^\]]+)]\([^)]*\)""").replace(text) { it.groupValues[1] }
        text = MARKDOWN_ZEICHEN.replace(text, "")
        ERSATZ.forEach { (zeichen, wort) -> text = text.replace(zeichen, wort) }
        text = UNERWUENSCHT.replace(text, " ")
        text = UmlautKorrektur.korrigiere(text) { ersetzung ->
            IdeenLog.debug(
                "SprechText",
                "fuerStimme",
                "Umlaut korrigiert",
                mapOf("vorher" to ersetzung.vorher, "nachher" to ersetzung.nachher),
            )
        }
        // Mehrfache Leerzeichen und verwaiste Satzzeichen aufräumen, Absätze bleiben erhalten.
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex(" ?([.,!?;:]) ?"), "\$1 ")
        text = text.replace(Regex("([.,!?;:])(?:\\s*\\1)+"), "\$1")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        return text.lines().joinToString("\n") { it.trim() }.trim()
    }

    /**
     * Zerlegt den Text in Vorlese-Einheiten (Baustein D 4.2):
     * ein Absatz ist eine Einheit, Absätze werden weder zusammengelegt noch mitten drin geteilt.
     * Nur Absätze über [MAX_ZEICHEN] werden an Satzgrenzen geteilt.
     */
    fun absaetze(text: String, maxZeichen: Int = MAX_ZEICHEN): List<String> {
        val roh = text.split(Regex("\\n\\s*\\n")).map(String::trim).filter(String::isNotBlank)
        return roh.flatMap { absatz ->
            if (absatz.length <= maxZeichen) listOf(absatz) else teileAnSatzgrenzen(absatz, maxZeichen)
        }
    }

    private fun teileAnSatzgrenzen(absatz: String, maxZeichen: Int): List<String> {
        val saetze = Regex("(?<=[.!?])\\s+").split(absatz)
        val teile = mutableListOf<String>()
        val aktuell = StringBuilder()
        saetze.forEach { satz ->
            when {
                satz.length > maxZeichen -> {
                    if (aktuell.isNotEmpty()) {
                        teile += aktuell.toString().trim()
                        aktuell.clear()
                    }
                    // Ein einzelner Satz über der Grenze: hart schneiden ist das kleinere Übel
                    // gegenüber einer abgelehnten Anfrage.
                    satz.chunked(maxZeichen).forEach(teile::add)
                }
                aktuell.length + satz.length + 1 > maxZeichen -> {
                    teile += aktuell.toString().trim()
                    aktuell.clear()
                    aktuell.append(satz)
                }
                else -> {
                    if (aktuell.isNotEmpty()) aktuell.append(' ')
                    aktuell.append(satz)
                }
            }
        }
        if (aktuell.isNotEmpty()) teile += aktuell.toString().trim()
        return teile.filter(String::isNotBlank)
    }

    /** Sicherheitsgrenze der Sprachdienste. */
    const val MAX_ZEICHEN = 1000
}
