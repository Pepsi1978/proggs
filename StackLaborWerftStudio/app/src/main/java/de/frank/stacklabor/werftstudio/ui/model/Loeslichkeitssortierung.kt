package de.frank.stacklabor.werftstudio.ui.model

/**
 * Die Reihenfolge der Löslichkeits-Ansicht.
 *
 * Standard sind die fettlöslichen zuerst — die brauchen eine Mahlzeit, gehören also an den
 * Anfang der Einnahme. Ein erneuter Druck auf „Löslichkeit“ dreht die Richtung um.
 *
 * Mittel, die beides sind, stehen immer in der Mitte: sie passen zu beiden Gruppen und
 * bilden so den Übergang, statt an einem Ende zu kleben.
 */
fun sortiereNachLoeslichkeit(mittel: List<MedicineUi>, fettZuerst: Boolean): List<MedicineUi> =
    mittel.sortedWith(compareBy({ it.solubility.rang(fettZuerst) }, { it.name.lowercase() }))

internal fun Solubility.rang(fettZuerst: Boolean): Int = when (this) {
    Solubility.Fat -> if (fettZuerst) 0 else 2
    Solubility.Both -> 1
    Solubility.Water -> if (fettZuerst) 2 else 0
}
