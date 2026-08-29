package de.frank.genialeideen.speech

import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.text.UmlautKorrektur

/**
 * Macht Text vorlesbar, bevor er an eine Stimme geht (Kapitel 4.5).
 *
 * Zwei Stellen setzen die Regel durch: der Systemprompt an das Modell erzeugt den Text schon
 * sauber, und diese Aufbereitung ist das Netz davor. Sie läuft **immer**, auch bei selbst
 * getipptem Text.
 *
 * Wichtig: **Der angezeigte Text ändert sich nicht.** Hier wird nur der Weg zur Synthese
 * aufbereitet — auf dem Bildschirm bleibt alles stehen, wie es geschrieben wurde. Und was nie
 * gesprochen wird (Protokoll, Diagnose, technische Bezeichner), läuft hier gar nicht erst durch.
 */
object SprechText {

    private val CODEBLOCK = Regex("```[\\s\\S]*?```")
    private val INLINE_CODE = Regex("`([^`]*)`")
    private val URL = Regex("""(?i)\b(?:https?://|www\.)\S+""")
    private val EMAIL = Regex("""\b[\w.-]+@[\w.-]+\.\w{2,}\b""")
    private val DATEIPFAD = Regex("""(?:[A-Za-z]:\\|/)[\w./\\-]{4,}""")

    /** Quellenangaben: (vgl. Meier 2020), (S. 12), [3], [Quelle: …]. */
    private val QUELLE = Regex(
        """\((?:vgl\.|siehe|vergleiche|Quelle:?|S\.|Seite|Abb\.|Kap\.)[^)]*\)""" +
            """|\[[^\]]{0,80}\]""",
        RegexOption.IGNORE_CASE,
    )

    /** Aufzählungszeichen am Zeilenanfang — die Zeile wird ein eigener Satz. */
    private val AUFZAEHLUNG = Regex("""(?m)^\s{0,6}(?:[-*•·+]|\d{1,2}[.)])\s+""")

    /** Markdown-Auszeichnung, die sonst als Zeichen gesprochen würde. */
    private val MARKDOWN_ZEICHEN = Regex("""[*_~>#]""")

    /** Zeichen, die ausgesprochen statt buchstabiert werden. */
    private val ERSATZ = linkedMapOf(
        "&" to " und ",
        "%" to " Prozent ",
        "€" to " Euro ",
        "$" to " Dollar ",
        "£" to " Pfund ",
        "+" to " plus ",
        "=" to " gleich ",
        "<" to " kleiner als ",
        ">" to " grösser als ",
        "→" to " führt zu ",
        "←" to " kommt von ",
        "@" to " at ",
        "/" to " oder ",
        "|" to ". ",
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
        "\\" to " ",
        "^" to " ",
        "~" to " ",
        "©" to "",
        "®" to "",
        "™" to "",
    )

    /**
     * Abkürzungen, die eine Stimme sonst buchstabiert. Ersetzt werden nur genau diese —
     * geraten wird nie (dieselbe Vorsicht wie bei der Umlaut-Korrektur, Baustein M.4).
     */
    private val ABKUERZUNGEN = linkedMapOf(
        "z. B." to "zum Beispiel",
        "z.B." to "zum Beispiel",
        "u. a." to "unter anderem",
        "u.a." to "unter anderem",
        "d. h." to "das heisst",
        "d.h." to "das heisst",
        "bzw." to "beziehungsweise",
        "ca." to "circa",
        "ggf." to "gegebenenfalls",
        "evtl." to "eventuell",
        "inkl." to "inklusive",
        "exkl." to "exklusive",
        "usw." to "und so weiter",
        "etc." to "und so weiter",
        "vgl." to "vergleiche",
        "Nr." to "Nummer",
        "Abb." to "Abbildung",
        "Kap." to "Kapitel",
        "Mio." to "Millionen",
        "Mrd." to "Milliarden",
        "Std." to "Stunden",
        "Min." to "Minuten",
        "Sek." to "Sekunden",
        "Tsd." to "Tausend",
        "max." to "maximal",
        "min." to "mindestens",
        "s. o." to "siehe oben",
        "s. u." to "siehe unten",
    )

    /** Einheiten hinter einer Zahl. */
    private val EINHEITEN = linkedMapOf(
        "kg" to "Kilogramm",
        "g" to "Gramm",
        "km" to "Kilometer",
        "cm" to "Zentimeter",
        "mm" to "Millimeter",
        "m" to "Meter",
        "l" to "Liter",
        "ml" to "Milliliter",
        "h" to "Stunden",
        "kWh" to "Kilowattstunden",
        "°C" to "Grad",
    )

    private val MONATE = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember",
    )

    /** Was nach allem übrig bleiben darf: Buchstaben, Ziffern und die guten Satzzeichen. */
    private val UNERWUENSCHT = Regex("""[^\p{L}\p{N} .,!?;:\-'\n]""")

    /**
     * Bereitet [roh] fürs Vorlesen auf. Im Zweifel wird ein Zeichen gestrichen, nie ein Wort —
     * nichts wird sinnentstellend gekürzt.
     */
    fun fuerStimme(roh: String): String {
        if (roh.isBlank()) return ""
        var text = roh

        // Code ergibt gesprochen nie einen Satz.
        text = CODEBLOCK.replace(text, " Codebeispiel. ")
        text = INLINE_CODE.replace(text) { it.groupValues[1] }

        // Adressen und Pfade werden benannt, nicht buchstabiert.
        text = Regex("""\[([^\]]+)]\([^)]*\)""").replace(text) { it.groupValues[1] }
        text = URL.replace(text, " Link ")
        text = EMAIL.replace(text, " E-Mail-Adresse ")
        text = DATEIPFAD.replace(text, " Dateipfad ")
        text = QUELLE.replace(text, " ")

        // Aufzählungen werden zu eigenen Sätzen.
        text = AUFZAEHLUNG.replace(text, "")
        text = MARKDOWN_ZEICHEN.replace(text, "")

        // Klammerzeichen weg, Inhalt bleibt als Einschub.
        text = text.replace(Regex("""[(){}\[\]]"""), ", ")

        text = schreibeAbkuerzungenAus(text)
        text = schreibeZahlenAus(text)

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

        // Aufräumen: Mehrfachzeichen, doppelte Satzzeichen, Absätze bleiben erhalten.
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("([!?.])\\1{1,}"), "$1")
        text = text.replace(Regex(" ?([.,!?;:]) ?"), "$1 ")
        text = text.replace(Regex("([.,!?;:])(?:\\s*\\1)+"), "$1")
        text = text.replace(Regex(",\\s*\\."), ".")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        return text.lines().joinToString("\n") { it.trim() }.trim()
    }

    private fun schreibeAbkuerzungenAus(roh: String): String {
        var text = roh
        ABKUERZUNGEN.forEach { (kurz, lang) ->
            text = text.replace(kurz, lang, ignoreCase = false)
        }
        return text
    }

    /**
     * Bringt Datum, Uhrzeit, Kommazahlen und Einheiten in gesprochene Form.
     * Nur eindeutige Muster — eine Zahl ohne klaren Zusammenhang bleibt, wie sie ist.
     */
    private fun schreibeZahlenAus(roh: String): String {
        var text = roh

        // 12.5. oder 12.05.2026 → zwölfter Mai (zweitausendsechsundzwanzig)
        text = Regex("""\b(\d{1,2})\.(\d{1,2})\.(\d{4})?""").replace(text) { treffer ->
            val tag = treffer.groupValues[1].toIntOrNull() ?: return@replace treffer.value
            val monat = treffer.groupValues[2].toIntOrNull() ?: return@replace treffer.value
            if (tag !in 1..31 || monat !in 1..12) return@replace treffer.value
            val jahr = treffer.groupValues[3]
            buildString {
                append(ordnungszahl(tag))
                append(' ')
                append(MONATE[monat - 1])
                if (jahr.isNotBlank()) {
                    append(' ')
                    append(jahr)
                }
            }
        }

        // 14:30 → vierzehn Uhr dreissig
        text = Regex("""\b(\d{1,2}):(\d{2})\b""").replace(text) { treffer ->
            val stunde = treffer.groupValues[1].toIntOrNull() ?: return@replace treffer.value
            val minute = treffer.groupValues[2].toIntOrNull() ?: return@replace treffer.value
            if (stunde !in 0..23 || minute !in 0..59) return@replace treffer.value
            if (minute == 0) "$stunde Uhr" else "$stunde Uhr $minute"
        }

        // 3,5 kg → drei Komma fünf Kilogramm
        text = Regex("""\b(\d+),(\d+)\s*([a-zA-Z°]{1,4})?""").replace(text) { treffer ->
            val ganz = treffer.groupValues[1]
            val teil = treffer.groupValues[2]
            val einheit = EINHEITEN[treffer.groupValues[3]]
            val zahl = "$ganz Komma $teil"
            if (einheit != null) "$zahl $einheit" else zahl
        }

        // 5 kg → fünf Kilogramm (Einheit hinter ganzer Zahl)
        text = Regex("""\b(\d+)\s*([a-zA-Z°]{1,4})\b""").replace(text) { treffer ->
            val einheit = EINHEITEN[treffer.groupValues[2]] ?: return@replace treffer.value
            "${treffer.groupValues[1]} $einheit"
        }

        return text
    }

    private fun ordnungszahl(zahl: Int): String = when (zahl) {
        1 -> "erster"
        3 -> "dritter"
        7 -> "siebter"
        else -> if (zahl < 20) "${zahl}ter" else "${zahl}ster"
    }

    /**
     * Zerlegt den Text in Vorlese-Einheiten (Baustein D 4.2): ein Absatz ist eine Einheit,
     * Absätze werden weder zusammengelegt noch mitten drin geteilt. Nur Absätze über
     * [MAX_ZEICHEN] werden an Satzgrenzen geteilt.
     *
     * Bleibt nach der Aufbereitung ein leerer Absatz übrig, wird er übersprungen statt als
     * Stille abgespielt (Kapitel 4.5).
     */
    fun absaetze(text: String, maxZeichen: Int = MAX_ZEICHEN): List<String> {
        val roh = text.split(Regex("\\n\\s*\\n")).map(String::trim).filter { absatz ->
            absatz.isNotBlank() && absatz.any(Char::isLetterOrDigit)
        }
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
