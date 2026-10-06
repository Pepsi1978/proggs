package de.frank.aufgaben.widget

import de.frank.aufgaben.data.Aufgabe

/**
 * Wo die rote Jetzt-Linie im Zeitstrahl des Widgets steht: in der Zeile [stunde], vor dem Termin mit dem
 * Index [davor] unter den Terminen dieser Stunde (0 = vor dem ersten, Anzahl = hinter dem letzten).
 */
data class JetztLinie(val stunde: Int, val davor: Int)

/** Die Zeile, in der ein Termin steht; Termine außerhalb von von–bis landen in der ersten bzw. letzten. */
internal fun stundeVon(a: Aufgabe, vonH: Int, bisH: Int): Int = ((a.minuten ?: 0) / 60).coerceIn(vonH, bisH)

/** Ein Termin, der gerade läuft und noch offen ist: Die Linie wartet davor, bis er erledigt ist. */
private fun laeuftOffen(a: Aufgabe, jetzt: Int): Boolean {
    val start = a.minuten ?: return false
    return !a.erledigt && start <= jetzt && jetzt < start + a.dauer.coerceAtLeast(1)
}

/**
 * Die Linie geht nie durch einen Termin. Läuft gerade ein offener Termin, steht sie davor, bis er erledigt
 * ist oder seine Zeit vorbei ist; danach springt sie dahinter. Sonst steht sie an der aktuellen Uhrzeit.
 * [termine] müssen nach Uhrzeit sortiert sein.
 */
internal fun jetztLinie(termine: List<Aufgabe>, jetzt: Int, vonH: Int, bisH: Int): JetztLinie {
    val offen = termine.firstOrNull { laeuftOffen(it, jetzt) }
    if (offen != null) {
        val stunde = stundeVon(offen, vonH, bisH)
        return JetztLinie(stunde, termine.filter { stundeVon(it, vonH, bisH) == stunde }.indexOf(offen))
    }
    val stunde = (jetzt / 60).coerceIn(vonH, bisH)
    return JetztLinie(stunde, termine.count { stundeVon(it, vonH, bisH) == stunde && (it.minuten ?: 0) <= jetzt })
}

/** Nächste Minute nach [jetzt], an der die Linie weiterspringt: volle Stunde, Beginn oder Ende eines Termins. */
internal fun naechsterSprung(termine: List<Aufgabe>, jetzt: Int): Int {
    val punkte = termine.flatMap { a -> a.minuten?.let { listOf(it, it + a.dauer.coerceAtLeast(1)) }.orEmpty() }
    return (punkte.filter { it > jetzt } + (jetzt / 60 + 1) * 60).min()
}
