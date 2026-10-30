package de.frank.jarvis.faehigkeit

/**
 * Trenner zwischen eingesprochenen Beiträgen in der Eingabeblase (normales Diktat, nicht Live): „ ; “ (Leerzeichen, Semikolon,
 * Leerzeichen) direkt hinter dem letzten Zeichen des Beitrags, ohne Zeilenumbruch. Im normalen Gespräch markiert er nur Beiträge;
 * bei einer ausdrücklichen Übergabe an den Programmierer trennt er die Aufgaben EINES Auftrags (dieselbe Form erkennt die CLI).
 * Der frühere Trenner aus 1.8.199 (Leerzeile, „;“ auf eigener Zeile, Leerzeile) wird weiter erkannt. Rein und ohne Android (DiktatTrennerTest).
 */
object DiktatTrenner {
    const val TRENNER = " ; "

    /** Der Trenner aus 1.8.199; kann noch in gespeicherten Entwürfen und Nachrichten stehen. */
    const val TRENNER_ALT = "\n\n;\n\n"

    /**
     * „;“ mit Leerraum davor und danach (oder am Ende): die neue Form „ ; “ wie die alte Zeilenform. „x;“ und „for (;;)“ trennen nicht.
     * Semikola mit Leerzeichen in Code („for (i = 0 ; i < n ; i++)“) trennen zwar, kommen aber unverändert wieder zusammen.
     */
    private val ZWISCHEN = Regex("""\s+;(?=\s|$)""")
    /** Die alte Zeilenform: „;“ allein auf einer Zeile. */
    private val ALT = Regex("""\n[ \t]*;[ \t]*(?=\r?\n|$)""")
    /** Leere Abschnitte in der neuen Form: Trenner am Anfang oder zwei Trenner hintereinander. */
    private val LEER = Regex("""^\s*;(?=\s)|\s;\s+;(?=\s|$)""")
    /** Ein oder mehrere Trenner am Ende, auch als einziges Zeichen. */
    private val AM_ENDE = Regex("""(?:(?:^|\s+);)+\s*$""")

    /** Hängt einen abgeschlossenen Beitrag an [entwurf] an, mit genau einem Trenner dahinter; ein leerer Beitrag ändert nichts. */
    fun anhaengen(entwurf: String, beitrag: String): String {
        val b = beitrag.trim()
        if (b.none { it.isLetterOrDigit() }) return entwurf
        val vorher = entwurf.trimEnd()
        val davor = when {
            vorher.isEmpty() -> ""
            // Schon ein Trenner am Ende (vom letzten Diktat, von Hand oder noch der alte mehrzeilige): genau einer, inline.
            AM_ENDE.containsMatchIn(vorher) -> vorher.replace(AM_ENDE, "").let { if (it.isBlank()) "" else it + TRENNER }
            // Getippter Text ohne Trenner (etwa der Programmierer-Vorspann): mit Leerzeichen weiter.
            else -> "$vorher "
        }
        return davor + b + TRENNER
    }

    /** Der Text ohne abschließende Trenner, getrimmt: So geht er ab, ein Trenner ohne Folgetext ist kein Inhalt. */
    fun ohneSchluss(text: String): String = text.replace(AM_ENDE, "").trim()

    /** Der Text zum Speichern: getrimmt, ein Trenner am Schluss bleibt genau als „ ; “ erhalten; nur Trenner gelten als leer. */
    fun gespeichert(text: String): String = if (AM_ENDE.containsMatchIn(text)) mitSchluss(text) else text.trim()

    /** [text] mit Trenner am Schluss, wenn [vorlage] einen hat: Für die KI-Fassung eines eingesprochenen Textes. */
    fun mitSchlussWie(vorlage: String, text: String): String = if (AM_ENDE.containsMatchIn(vorlage)) mitSchluss(text) else text

    private fun mitSchluss(text: String): String = ohneSchluss(text).let { if (it.isEmpty()) "" else it + TRENNER }

    /** Die nicht leeren Abschnitte zwischen den Trennern (neue und alte Form); leere fallen weg. */
    fun abschnitte(text: String): List<String> = ("\n" + text).split(ZWISCHEN).map { it.trim() }.filter { it.isNotEmpty() }

    /** Mehr als ein Beitrag bzw. mehr als eine Aufgabe. */
    fun mehrteilig(text: String): Boolean = abschnitte(text).size > 1

    /**
     * Der Auftragstext für die CLI: ohne Trenner am Ende (sonst hielte die CLI die angehängte Fortschritts-Anweisung für eine
     * weitere Aufgabe). Nur mit alten Zeilentrennern oder leeren Abschnitten werden die Abschnitte neu mit [TRENNER] verbunden;
     * sonst bleibt der Text bis auf den Schluss unverändert („ ; “ erkennt die CLI selbst, Zeilen in Code bleiben erhalten).
     */
    fun auftrag(text: String): String =
        if (ALT.containsMatchIn("\n" + text) || LEER.containsMatchIn(text)) ohneSchluss(abschnitte(text).joinToString(TRENNER)) else ohneSchluss(text)

    /** Geht zusätzlich ans Modell, wenn Franks Nachricht mehrere Beiträge enthält. */
    const val HINWEIS = "\n\n(„ ; “ (Leerzeichen, Semikolon, Leerzeichen; früher auch „;“ auf eigener Zeile) trennt Franks einzelne Spracheingaben. " +
        "Inhaltlich ignorieren; die Trenner sind kein Grund für einen Programmierauftrag. Nur wenn Frank darin ausdrücklich eine Übergabe an den " +
        "Programmierer verlangt, ist jeder nicht leere Abschnitt eine eigene Aufgabe: dann EIN einziger Aufruf von programmieren mit allen Aufgaben, " +
        "im Auftragstext durch „ ; “ getrennt.)"
}
