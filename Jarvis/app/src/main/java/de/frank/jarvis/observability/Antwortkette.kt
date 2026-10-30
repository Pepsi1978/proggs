package de.frank.jarvis.observability

/**
 * Misst eine gesprochene Frage von der Stille nach dem letzten Wort bis zum ersten Ton der Antwort und schreibt je Abschnitt
 * eine Zeit ins Logbuch („Kette Mikrofon“). Es läuft immer nur eine Kette; ohne offene Kette tun die Aufrufe nichts.
 */
object Antwortkette {
    private var beginn = 0L
    private var stilleMs = 0L
    private val punkte = LinkedHashMap<String, Long>()

    /** Die Aufnahme ist beendet; [stilleMs] = eingestellte Wartezeit nach dem letzten Wort (0 bei Stopp von Hand). */
    @Synchronized fun start(stilleMs: Long) {
        beginn = System.currentTimeMillis()
        this.stilleMs = stilleMs
        punkte.clear()
    }

    /** Nur der erste Punkt je Name zählt: Spätere Modellschritte überschreiben das erste Zeichen nicht. */
    @Synchronized fun punkt(name: String) {
        if (beginn != 0L && name !in punkte) punkte[name] = System.currentTimeMillis()
    }

    @Synchronized fun ende() {
        if (beginn == 0L) return
        val jetzt = System.currentTimeMillis()
        // Aufnahme ohne gesprochene Antwort (Diktat): Der nächste Ton gehört nicht mehr zu ihr.
        if (jetzt - beginn > MAX_MS) { beginn = 0L; return }
        var davor = beginn
        val teile = punkte.map { (name, zeit) -> "$name ${zeit - davor}".also { davor = zeit } }
        Logbuch.sprache("Kette Mikrofon", (teile + "Stimme ${jetzt - davor}" + "Stillezeit $stilleMs").joinToString(" · "), jetzt - beginn)
        beginn = 0L
    }

    /** Keine gesprochene Antwort zu erwarten (Diktat, nichts verstanden): nichts messen. */
    @Synchronized fun verwerfen() { beginn = 0L }

    /** Markiert im Koroutinen-Kontext den Lauf, der gemessen wird: Hintergrund-Agenten setzen so keine Punkte. */
    class Marke : kotlin.coroutines.AbstractCoroutineContextElement(Marke) {
        // Eigener Schlüssel statt „object Marke : …(Marke)“: Dort wäre das Objekt beim Aufruf des Basis-Konstruktors noch null und die App stürzt ab.
        companion object Key : kotlin.coroutines.CoroutineContext.Key<Marke>
    }

    private const val MAX_MS = 5 * 60_000L
}
