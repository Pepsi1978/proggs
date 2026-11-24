package de.frank.aufgaben.ui

/** Achsenparalleles Rechteck in px, ohne Compose-Abhängigkeit, damit die Entscheidung als reine Rechnung prüfbar bleibt. */
internal data class Rechteck(val links: Float, val oben: Float, val rechts: Float, val unten: Float) {
    /** Schneiden oder Berühren: [rand] px Abstand zählen schon als Kontakt. */
    fun beruehrt(o: Rechteck, rand: Float) =
        links - rand < o.rechts && o.links < rechts + rand && oben - rand < o.unten && o.oben < unten + rand
}

/**
 * Wo die Uhrzeit der Jetzt-Linie steht. Mittig als roter Knopf mit „Uhr“; berührt oder überdeckt dieser eine Aufgabenkarte,
 * wechselt sie nach links in die Beschriftungsspalte ([links] = true) und ersetzt dort die Stundenbeschriftungen in
 * [verdeckt] (Indizes in der übergebenen Liste der Stunden-y-Werte). Wird jedes Mal neu gerechnet, der Rückweg in die
 * Mitte ergibt sich also von selbst, sobald dort wieder Platz ist.
 */
internal data class UhrLage(val links: Boolean, val verdeckt: Set<Int>)

internal fun uhrLage(
    knopf: Rechteck, karten: List<Rechteck>, rand: Float,
    jetztY: Float, linksHoehe: Float, stundenY: List<Float>, stundeHoehe: Float, abstand: Float,
): UhrLage {
    if (karten.none { knopf.beruehrt(it, rand) }) return UhrLage(false, emptySet())
    val grenze = (linksHoehe + stundeHoehe) / 2f + abstand
    return UhrLage(true, stundenY.indices.filter { kotlin.math.abs(stundenY[it] - jetztY) < grenze }.toSet())
}
