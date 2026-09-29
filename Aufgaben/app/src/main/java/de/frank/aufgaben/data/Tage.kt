package de.frank.aufgaben.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Datumshilfe: Heute und Morgen sind rein datumsbasiert — um 0 Uhr wird aus Morgen von selbst Heute. */
object Tage {
    fun heute(): Long = LocalDate.now().toEpochDay()
    fun morgen(): Long = heute() + 1

    fun zeit(minuten: Int): String = "%02d:%02d".format(minuten / 60, minuten % 60)

    fun datum(tag: Long): String {
        val datum = LocalDate.ofEpochDay(tag)
        return when (tag - heute()) {
            0L -> "Heute"
            1L -> "Morgen"
            2L -> "Übermorgen"
            -1L -> "Gestern"
            else -> datum.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN) + ", " +
                datum.format(DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN))
        }
    }

    fun langesDatum(tag: Long): String = LocalDate.ofEpochDay(tag)
        .format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN))

    /** Zeitpunkt in Millisekunden für Tag + Minuten in der lokalen Zeitzone. */
    fun millis(tag: Long, minuten: Int): Long = LocalDate.ofEpochDay(tag)
        .atStartOfDay(ZoneId.systemDefault()).plusMinutes(minuten.toLong()).toInstant().toEpochMilli()

    fun naechsteMitternacht(): Long = LocalDate.now().plusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Nächster Tag einer Wiederholung ab [tag]. */
    fun naechster(tag: Long, wdh: Wiederholung): Long? {
        val d = LocalDate.ofEpochDay(tag)
        return when (wdh) {
            Wiederholung.KEINE -> null
            Wiederholung.TAEGLICH -> d.plusDays(1).toEpochDay()
            Wiederholung.WOECHENTLICH -> d.plusWeeks(1).toEpochDay()
            Wiederholung.MONATLICH -> d.plusMonths(1).toEpochDay()
            Wiederholung.WERKTAGS -> {
                var n = d.plusDays(1)
                while (n.dayOfWeek == DayOfWeek.SATURDAY || n.dayOfWeek == DayOfWeek.SUNDAY) n = n.plusDays(1)
                n.toEpochDay()
            }
        }
    }
}

/**
 * Erkennt im gesprochenen Text Tag, Uhrzeit und Dringlichkeit ("morgen um 10 Uhr", "heute 14:30",
 * "übermorgen halb neun", "dringend"). Die App schlägt das Ergebnis nur vor — übernommen wird es mit einem Tipp.
 */
object Erkennung {
    data class Vorschlag(val tag: Long?, val minuten: Int?, val prioritaet: Prioritaet?) {
        val leer: Boolean get() = tag == null && minuten == null && prioritaet == null
    }

    private val zahlwoerter = mapOf(
        "eins" to 1, "ein" to 1, "eine" to 1, "zwei" to 2, "drei" to 3, "vier" to 4, "fünf" to 5, "sechs" to 6,
        "sieben" to 7, "acht" to 8, "neun" to 9, "zehn" to 10, "elf" to 11, "zwölf" to 12,
    )

    fun erkenne(text: String): Vorschlag {
        val t = text.lowercase(Locale.GERMAN)
        val heute = Tage.heute()
        val tag = when {
            Regex("\\bübermorgen\\b").containsMatchIn(t) -> heute + 2
            Regex("\\bmorgen\\b").containsMatchIn(t) -> heute + 1
            Regex("\\bheute\\b").containsMatchIn(t) -> heute
            else -> null
        }
        var minuten: Int? = null
        Regex("\\b([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b").find(t)?.let {
            minuten = it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()
        }
        if (minuten == null) {
            Regex("\\b([01]?\\d|2[0-3])\\s*uhr(\\s+([0-5]?\\d)\\b)?").find(t)?.let {
                minuten = it.groupValues[1].toInt() * 60 + (it.groupValues[3].toIntOrNull() ?: 0)
            }
        }
        if (minuten == null) {
            Regex("\\bhalb\\s+(\\p{L}+|\\d{1,2})").find(t)?.let { m ->
                val h = m.groupValues[1].toIntOrNull() ?: zahlwoerter[m.groupValues[1]]
                if (h != null && h in 1..24) minuten = ((h - 1) * 60 + 30).let { if (it < 5 * 60) it + 12 * 60 else it }
            }
        }
        if (minuten == null) {
            Regex("\\bum\\s+(\\p{L}+)\\s+uhr").find(t)?.let { m ->
                zahlwoerter[m.groupValues[1]]?.let { h -> minuten = (if (h < 5) h + 12 else h) * 60 }
            }
        }
        val prio = when {
            Regex("\\b(dringend|sehr wichtig|hohe priorität|sofort)\\b").containsMatchIn(t) -> Prioritaet.HOCH
            Regex("\\b(nicht so wichtig|unwichtig|geringe priorität|irgendwann)\\b").containsMatchIn(t) -> Prioritaet.GERING
            Regex("\\b(wichtig|mittlere priorität)\\b").containsMatchIn(t) -> Prioritaet.MITTEL
            else -> null
        }
        return Vorschlag(tag, minuten, prio)
    }
}
