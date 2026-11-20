package de.frank.jarvis.news

import java.util.Locale

/** Eine Meldung aus News-Kompass, wie Jarvis sie liest. [id] = „Ausgabe/Meldung“ in News-Kompass (je Gerät verschieden). */
data class NewsMeldung(
    val id: String,
    val rubrik: String,
    val titel: String,
    val kurz: String = "",
    val wann: String = "",
    val quelle: String = "",
    val update: Boolean = false,
    val ausgabeUm: Long = 0L,
)

/** Was Frank heute schon genannt oder vorgelesen wurde: genug, um dieselbe Geschichte auch auf dem anderen Gerät wiederzuerkennen. */
data class Genannt(val titel: String, val quelle: String, val um: Long)

/**
 * Die reinen Regeln der News-Anbindung, ohne Android (Unit-Test `NewsRegelnTest`):
 * - Wiedererkennen derselben Geschichte über Gerät, Rubrik und Lauf hinweg (Artikeladresse oder Titelwörter, wie News-Kompass selbst).
 * - Erkennen, ob eine Antwort von Jarvis eine Meldung tatsächlich genannt hat (nicht nur abgerufen).
 * - Spoilerschutz: Spielstände und Ergebnisse von Borussia Dortmund nie ungefragt, auch nicht über die Schlagzeile.
 */
object NewsRegeln {

    private val STOPP = setOf(
        "der", "die", "das", "und", "mit", "für", "von", "auf", "ist", "den", "dem", "des", "ein", "eine", "einen", "einer",
        "zum", "zur", "bei", "nach", "über", "als", "auch", "sich", "wird", "werden", "hat", "haben", "nicht", "mehr",
        "neue", "neuer", "neues", "neuen", "gegen", "aus", "vor", "wie", "noch", "jetzt", "soll", "will", "kann", "sein", "seine",
        "ihre", "beim", "vom", "bis", "nur", "erst", "ersten", "erste",
    )

    fun woerter(text: String): Set<String> =
        text.lowercase(Locale.GERMANY).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 3 && it !in STOPP }.toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Double =
        if (a.isEmpty() || b.isEmpty()) 0.0 else a.intersect(b).size.toDouble() / a.union(b).size

    /** Nur Artikeladressen zählen; eine bloße Startseite würde fremde Geschichten zusammenwerfen. */
    fun adresse(url: String): String? {
        val ohne = url.substringBefore('#').substringBefore('?').lowercase(Locale.ROOT)
        val rest = ohne.substringAfter("://", ohne).removePrefix("www.")
        val pfad = rest.substringAfter('/', "").trimEnd('/')
        return if (pfad.length < 2) null else rest.substringBefore('/') + "/" + pfad
    }

    /** Dieselbe Geschichte? Gleiche Artikeladresse oder sehr ähnlicher Titel. Eine Folgemeldung mit neuer Entwicklung gilt als neu. */
    fun gleich(m: NewsMeldung, g: Genannt): Boolean {
        val a = adresse(m.quelle)
        if (a != null && a == adresse(g.quelle)) return true
        return jaccard(woerter(m.titel), woerter(g.titel)) >= 0.6
    }

    /** Was aus [meldungen] Frank heute noch nicht genannt wurde. */
    fun neu(meldungen: List<NewsMeldung>, genannt: List<Genannt>): List<NewsMeldung> =
        meldungen.filter { m -> genannt.none { gleich(m, it) } }

    /**
     * Hat [antwort] die Meldung genannt? Die Modelle formulieren Schlagzeilen um („17 Prozent“ statt „siebzehn Prozent“),
     * deshalb zählen Wortanfänge der Titelwörter: mindestens zwei Treffer und mindestens zwei Fünftel der Wörter.
     */
    fun genanntIn(antwort: String, m: NewsMeldung): Boolean {
        val titel = woerter(m.titel)
        if (titel.isEmpty()) return false
        // Satz für Satz: Über die ganze Antwort verteilt fänden sich bei vielen Schlagzeilen sonst zufällig gemeinsame Wörter
        // („Dortmund“, „Spiel“), und eine ausgelassene Meldung gälte als genannt.
        return antwort.split(Regex("[.!?;:\\n•]+(\\s|$)|\\n")).any { satz ->
            val gesagt = woerter(satz)
            val treffer = titel.count { w -> val stamm = w.take(6); gesagt.any { it.startsWith(stamm) || (it.length >= 5 && w.startsWith(it)) } }
            treffer >= minOf(2, titel.size) && treffer * 5 >= titel.size * 2
        }
    }

    // ------------------------------------------------------------------ Spoilerschutz Borussia Dortmund

    private val DORTMUND = Regex("dortmund|\\bbvb\\b|borussia d", RegexOption.IGNORE_CASE)
    private val STAND = Regex("\\b\\d{1,2}\\s*[:–-]\\s*\\d{1,2}\\b")
    private val AUSGANG = Regex(
        "\\b(gewinnt|gewann|gewinnen|gewonnen|siegt|siegte|siegen|sieg|siege|besiegt|bezwingt|bezwang|schlägt|schlug|geschlagen|verliert|verlor|verlieren|verloren|" +
            "niederlage|pleite|unentschieden|remis|punktet|punktgewinn|punkteteilung|torlos|trennt sich|trennen sich|ausgeschieden|scheitert|scheiterte|dreht|drehte|" +
            "triumph|triumphiert|blamage|blamiert|kantersieg|heimsieg|auswärtssieg|endstand|spielstand|führt mit|führung|ausgleich|siegtor|tore)\\b",
        RegexOption.IGNORE_CASE,
    )

    /** Verrät der Text einen Dortmunder Spielstand oder ein Ergebnis? */
    fun dortmundSpoiler(text: String): Boolean = DORTMUND.containsMatchIn(text) && (STAND.containsMatchIn(text) || AUSGANG.containsMatchIn(text))

    /** Gilt die ganze Meldung als Dortmund-Meldung mit Ergebnis (Titel oder Kurztext)? */
    fun spoiler(m: NewsMeldung): Boolean = DORTMUND.containsMatchIn(m.titel + " " + m.kurz) &&
        (STAND.containsMatchIn(m.titel + " " + m.kurz) || AUSGANG.containsMatchIn(m.titel + " " + m.kurz))

    /** Satzweise ohne Spielstand und Ergebnis, wenn der Text von Dortmund handelt; sonst unverändert. */
    fun ohneErgebnis(text: String): String {
        if (!DORTMUND.containsMatchIn(text)) return text
        return text.split(Regex("(?<=[.!?])\\s+")).filterNot { STAND.containsMatchIn(it) || AUSGANG.containsMatchIn(it) }.joinToString(" ")
    }
}
