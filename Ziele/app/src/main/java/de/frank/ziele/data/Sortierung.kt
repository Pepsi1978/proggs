package de.frank.ziele.data

/** Wie die Vorschläge eines Ziels geordnet werden. Notwendig und Unterstützend bleiben immer getrennt. */
enum class Sortierung(val anzeige: String, val erklaerung: String) {
    WICHTIGKEIT("Wichtigkeit", "Was am meisten zum Ziel beiträgt, steht oben."),
    REIHENFOLGE("Reihenfolge", "Voraussetzungen zuerst: so kannst du die Liste von oben nach unten abarbeiten."),
    AUFWAND("Aufwand", "Kleine Aufgaben zuerst, große zuletzt."),
    HEBEL("Hebel", "Viel Wirkung bei wenig Aufwand: die schnellen Erfolge zuerst."),
    PHASE("Zeitpunkt", "Was sofort dran ist, dann was bald kommt, dann Späteres.");

    companion object {
        fun von(name: String?): Sortierung = entries.firstOrNull { it.name == name } ?: WICHTIGKEIT
    }
}

fun List<Vorschlag>.sortiert(s: Sortierung): List<Vorschlag> = when (s) {
    Sortierung.WICHTIGKEIT -> sortedWith(compareByDescending<Vorschlag> { it.wichtigkeit }.thenBy { it.phase }.thenBy { it.minuten })
    Sortierung.AUFWAND -> sortedWith(compareBy<Vorschlag>({ it.aufwandKlasse.rang }, { it.minuten }, { -it.wichtigkeit }))
    Sortierung.HEBEL -> sortedWith(compareByDescending<Vorschlag> { it.hebel }.thenByDescending { it.wichtigkeit })
    Sortierung.PHASE -> sortedWith(compareBy<Vorschlag>({ it.phase }, { -it.wichtigkeit }, { it.minuten }))
    Sortierung.REIHENFOLGE -> topologisch(this)
}

/**
 * Voraussetzungen zuerst (Kahn-Verfahren). Unter den gerade freien Aufgaben entscheidet die Phase, dann die Wichtigkeit.
 * Zeigt eine Voraussetzung auf einen Vorschlag aus der anderen Gruppe oder auf einen unbekannten Schlüssel, zählt sie hier nicht.
 * Ein Kreis (sollte die KI nicht liefern) wird aufgebrochen, statt Vorschläge zu verlieren.
 */
private fun topologisch(liste: List<Vorschlag>): List<Vorschlag> {
    val schluessel = liste.map { it.schluessel }.toSet()
    val offen = liste.associateBy { it.schluessel }.toMutableMap()
    val rest = liste.associate { v -> v.schluessel to v.voraussetzungen.filter { it in schluessel && it != v.schluessel }.toMutableSet() }
    val ergebnis = mutableListOf<Vorschlag>()
    val reihung = compareBy<Vorschlag>({ it.phase }, { -it.wichtigkeit }, { it.minuten })
    while (offen.isNotEmpty()) {
        val frei = offen.values.filter { v -> rest[v.schluessel].orEmpty().none { it in offen } }
        val naechster = (frei.ifEmpty { offen.values.toList() }).minWithOrNull(reihung) ?: break
        ergebnis += naechster
        offen.remove(naechster.schluessel)
    }
    return ergebnis
}
