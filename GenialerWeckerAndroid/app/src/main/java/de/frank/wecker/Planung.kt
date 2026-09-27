package de.frank.wecker

/**
 * Gemeinsames Tor für alle Planungen im AlarmManager, über alle AlarmScheduler-Instanzen hinweg (Receiver, Dienst,
 * ViewModel). Der aktuelle Stand wird INNERHALB des Tors gelesen, damit ein verspäteter Auftrag nie einen älteren
 * Zustand scharfschaltet oder einen neu geplanten Slot abräumt.
 *
 * Sperrreihenfolge: Planung → SchlafErinnerung → AlarmStore. Umgekehrt nimmt niemand diese Sperre, deshalb keine
 * Verklemmung. Der Store-Monitor wird nur für die einzelnen Lesezugriffe genommen, nie über das ganze Tor gehalten.
 */
internal object Planung {
    private val lock = Any()

    /** Liest den Zustand und wendet ihn an — beides unter demselben Tor, nacheinander für alle Aufrufer. */
    fun <S, T> unter(lesen: () -> S, anwenden: (S) -> T): T = synchronized(lock) { anwenden(lesen()) }
}
