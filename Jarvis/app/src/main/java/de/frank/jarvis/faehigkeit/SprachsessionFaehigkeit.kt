package de.frank.jarvis.faehigkeit

/**
 * Brücke vom Werkzeug zur Sprachschleife in der App. Die App trägt [beenden] ein, solange sie läuft; der Aufruf merkt
 * das Ende der Sprachsession vor und liefert, ob gerade eine freihändige Sprachsession lief.
 */
object Sprachsession {
    @Volatile var beenden: (() -> Boolean)? = null
}

/**
 * „Sprachsession beenden“ als Werkzeug für das eigene Modell von Jarvis: für frei formulierte Wünsche, die die App nicht
 * schon selbst erkannt hat. Beendet nur das Zuhören, nie das Gespräch, die App oder laufende Aufträge.
 */
class SprachsessionFaehigkeit : Faehigkeit {
    override val id = "sprachsession"
    override val name = "Sprachsession"
    override val beschreibung = "Beendet die laufende Sprachsession: Nach der nächsten Antwort öffnet Jarvis das Mikrofon nicht mehr von selbst."
    override val hinweise =
        "Will Frank die Sprachsession, das Live-Gespräch, den Gesprächsmodus oder das Zuhören beenden oder das Mikrofon schließen, auch frei formuliert " +
            "(„bitte beenden jetzt“, „hör auf zuzuhören“, „mach das Mikro aus“, „das war's für jetzt“), rufe sprachsession_beenden auf und " +
            "bestätige danach in einem kurzen Satz, etwa „Okay, Sprachsession beendet.“. Erst das Werkzeug schließt das Mikrofon: " +
            "Behaupte nie ohne diesen Aufruf, die Session sei beendet, und sag nie, Frank müsse das Mikrofon selbst ausschalten. " +
            "Das Gespräch, die App, Programmieraufträge und Agenten laufen dabei weiter."

    override fun stoerung(): String? = null

    /**
     * Auch im Plugin: Spricht Frank mit ChatGPT, kann eine gleichzeitig offene Sprachsession von Jarvis (Mikrofon, Live-Gespräch)
     * stören; ChatGPT kann sie dann auf Franks Wunsch schließen.
     */
    override val imPlugin: Boolean get() = true

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "sprachsession_beenden",
            titel = "Sprachsession beenden",
            beschreibung = "Jarvis: beendet die laufende Sprachsession auf dem Handy, auch ein laufendes Live-Gespräch samt Verbindung. Nach deiner Antwort piept es nicht mehr und das Mikrofon " +
                "bleibt zu, bis Frank Jarvis wieder ausdrücklich aufruft. Gespräch und Aufträge bleiben unberührt. Aus ChatGPT: wenn Frank Jarvis' eigenes Zuhören oder Live-Gespräch " +
                "auf dem Handy beenden will, etwa weil er gerade mit ChatGPT spricht.",
            schema = schema(),
            // Ändert keine Daten: kein Eintrag im Live-Gedächtnis, keine „Erledigt“-Notiz.
            nurLesen = true,
        ) {
            val beenden = Sprachsession.beenden
            when {
                beenden == null -> Ergebnis("Die Sprachsession lässt sich nur in der laufenden Jarvis-App beenden.", fehler = true)
                beenden() -> Ergebnis("Sprachsession beendet: Nach deiner Antwort piept es nicht mehr und das Mikrofon bleibt zu. Bestätige Frank das kurz.")
                else -> Ergebnis("Es läuft keine freihändige Sprachsession; nach deiner Antwort öffnet das Mikrofon nicht von selbst. Bestätige Frank das kurz.")
            }
        },
    )
}
