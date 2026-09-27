package de.frank.kompass.update

/** Zwischenstand, den die Oberfläche während des Laufs anzeigt. */
data class LaufFortschritt(
    val laeuft: Boolean = false,
    val schritt: String = "",
    val erledigt: Int = 0,
    val gesamt: Int = 0,
    val neuAnzahl: Int = 0,
    val entferntAnzahl: Int = 0,
    val geaendertAnzahl: Int = 0,
    val geloeschtAnzahl: Int = 0,
    val erklaertAnzahl: Int = 0,
    /** Wie viele Einträge noch auf ihre deutsche Erklärung warten. */
    val offeneErklaerungen: Int = 0,
    val neueNamen: List<String> = emptyList(),
    val entfernteNamen: List<String> = emptyList(),
    val geaenderteNamen: List<String> = emptyList(),
    val geloeschteNamen: List<String> = emptyList(),
    val gefundeneVersion: String = "",
    val fehler: String = "",
    val fertig: Boolean = false,
) {
    /** Hat sich überhaupt etwas geändert? Entscheidet, ob der Bericht sich zu lesen lohnt. */
    val hatAenderungen: Boolean
        get() = neuAnzahl > 0 || entferntAnzahl > 0 || geaendertAnzahl > 0 ||
            geloeschtAnzahl > 0 || erklaertAnzahl > 0
}
