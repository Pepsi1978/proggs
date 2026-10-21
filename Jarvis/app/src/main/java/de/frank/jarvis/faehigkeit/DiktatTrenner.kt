package de.frank.jarvis.faehigkeit

/**
 * Trenner zwischen eingesprochenen Beiträgen in der Eingabeblase (normales Diktat, nicht Live): Leerzeile, „;“ auf eigener
 * Zeile, Leerzeile. Im normalen Gespräch markiert er nur Beiträge; bei einer ausdrücklichen Übergabe an den Programmierer
 * trennt er die Aufgaben EINES Auftrags (gilt ebenso für Franks getipptes „ ; “). Rein und ohne Android (DiktatTrennerTest).
 */
object DiktatTrenner {
    const val TRENNER = "\n\n;\n\n"

    /**
     * „;“ allein auf einer Zeile. Franks Tippform „ ; “ bleibt bewusst unangetastet: Die CLI erkennt sie selbst (samt der
     * Ausnahme für Semikola in Code, SQL und Adressen), ein Umschreiben hier zerlegte etwa „for (i = 0 ; i < n ; i++)“.
     */
    private val ZWISCHEN = Regex("""\s*\n[ \t]*;[ \t]*(?=\r?\n|$)""")
    /** Ein oder mehrere Trenner am Ende, auch in Tippform („Text ;“) oder als einziges Zeichen. */
    private val AM_ENDE = Regex("""(?:(?:^|\s+);)+\s*$""")
    /** Endet schon mit einem Trenner: „;“ nach Leerraum oder ganz allein. */
    private val ENDET_MIT_TRENNER = Regex("""(?:^|\s);$""")

    /** Hängt einen abgeschlossenen Beitrag an [entwurf] an, mit genau einem Trenner dahinter; ein leerer Beitrag ändert nichts. */
    fun anhaengen(entwurf: String, beitrag: String): String {
        val b = beitrag.trim()
        if (b.none { it.isLetterOrDigit() }) return entwurf
        val vorher = entwurf.trimEnd()
        val davor = when {
            vorher.isEmpty() -> ""
            // Schon ein Trenner am Ende (vom letzten Diktat oder von Hand): kein zweiter, der Beitrag folgt nach der Leerzeile.
            ENDET_MIT_TRENNER.containsMatchIn(vorher) -> "$vorher\n\n"
            // Getippter Text ohne Trenner (etwa der Programmierer-Vorspann): wie bisher mit Leerzeichen weiter.
            else -> "$vorher "
        }
        return davor + b + TRENNER
    }

    /** Der Text ohne abschließende Trenner, getrimmt: So geht er ab, ein Trenner ohne Folgetext ist kein Inhalt. */
    fun ohneSchluss(text: String): String = text.replace(AM_ENDE, "").trim()

    /** Die nicht leeren Abschnitte zwischen den „;“-Zeilen; leere fallen weg. */
    fun abschnitte(text: String): List<String> = ("\n" + text).split(ZWISCHEN).map { it.trim() }.filter { it.isNotEmpty() }

    /** Mehr als ein Beitrag bzw. mehr als eine Aufgabe. */
    fun mehrteilig(text: String): Boolean = abschnitte(text).size > 1

    /**
     * Der Auftragstext für die CLI: Abschnitte einheitlich mit [TRENNER] getrennt, ohne leere Abschnitte und ohne Trenner am
     * Ende (sonst hielte die CLI die angehängte Fortschritts-Anweisung für eine weitere Aufgabe). Ohne Trenner nur getrimmt.
     */
    fun auftrag(text: String): String = ohneSchluss(abschnitte(text).joinToString(TRENNER))

    /** Geht zusätzlich ans Modell, wenn Franks Nachricht mehrere Beiträge enthält. */
    const val HINWEIS = "\n\n(Die Zeilen mit nur „;“ (ebenso „ ; “) trennen Franks einzelne Spracheingaben. Inhaltlich ignorieren; sie sind " +
        "kein Grund für einen Programmierauftrag. Nur wenn Frank darin ausdrücklich eine Übergabe an den Programmierer verlangt, ist " +
        "jeder nicht leere Abschnitt eine eigene Aufgabe: dann EIN einziger Aufruf von programmieren mit allen Aufgaben, im Auftragstext " +
        "durch Leerzeile, „;“ auf eigener Zeile, Leerzeile getrennt.)"
}
