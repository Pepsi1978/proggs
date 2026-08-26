package de.frank.gedankenspeicher.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray

/**
 * **Die Nachträge einer Notiz.**
 *
 * Ein Nachtrag ist ein später gedachter Zusatz: er bekommt beim Einspeichern eine eigene
 * Zeile — „— Nachtrag vom 26.08.2026, 14:30 —" — mitten im Text. Die Zeile ist **bewusst
 * ganz normaler Text**: sie überlebt jedes spätere Bearbeiten, wandert mit in Export und
 * KI-Kontext und braucht keine getrennte Speicherstruktur, die mit dem Text
 * auseinanderlaufen könnte. Nur die rohen Zeitpunkte liegen zusätzlich als JSON bei,
 * damit die Karte oben rechts den neuesten zeigen kann, ohne den Text zurückparsen zu
 * müssen.
 */
object Nachtraege {

    private val zeitformat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMAN)

    /** Erkennt eine Nachtragszeile und fängt das Datum dahinter ein. */
    val zeilenMuster = Regex(
        "^\\s*—\\s*Nachtrag vom (\\d{2}\\.\\d{2}\\.\\d{4}, \\d{2}:\\d{2})\\s*—\\s*$",
        RegexOption.MULTILINE,
    )

    fun zeitpunkt(zeit: Long): String = zeitformat.format(Date(zeit))

    /** Die Überschriftenzeile, wie sie in den Text eingesetzt wird. */
    fun zeile(zeit: Long): String = "— Nachtrag vom ${zeitpunkt(zeit)} —"

    /** Die Überschriftenzeile aus einem bereits formatierten Datum — zum Wiederzusammensetzen. */
    fun zeileVon(zeitpunktText: String): String = "— Nachtrag vom $zeitpunktText —"

    // --- Die rohen Zeitpunkte als JSON-Feld -------------------------------------------------

    fun zeitenAusJson(roh: String): List<Long> = runCatching {
        val feld = JSONArray(roh)
        (0 until feld.length()).map(feld::optLong)
    }.getOrDefault(emptyList())

    fun zeitenAlsJson(zeiten: List<Long>): String = JSONArray(zeiten).toString()

    /** Der jüngste Nachtrag — oder null, wenn die Notiz nie einen bekommen hat. */
    fun letzter(notiz: Notiz): Long? = zeitenAusJson(notiz.nachtragzeitenJson).maxOrNull()

    /**
     * Der Text in seinen Abschnitten: der ursprüngliche Teil zuerst, dann je Nachtrag
     * einer — [Textabschnitt.nachtragVom] sagt, ob und wann es ein Nachtrag ist.
     */
    fun abschnitte(text: String): List<Textabschnitt> {
        val treffer = zeilenMuster.findAll(text).toList()
        if (treffer.isEmpty()) return listOf(Textabschnitt(null, text))
        val ergebnis = mutableListOf<Textabschnitt>()
        val erster = treffer.first()
        text.substring(0, erster.range.first).takeIf(String::isNotBlank)?.let {
            ergebnis += Textabschnitt(null, it)
        }
        treffer.forEachIndexed { nr, trefferZeile ->
            val ende = treffer.getOrNull(nr + 1)?.range?.first ?: text.length
            ergebnis += Textabschnitt(
                nachtragVom = trefferZeile.groupValues[1],
                text = text.substring(trefferZeile.range.last + 1, ende),
            )
        }
        return ergebnis
    }
}

/** Ein Stück Notiztext: entweder der Ursprung oder alles unter einem Nachtrag. */
data class Textabschnitt(val nachtragVom: String?, val text: String)
