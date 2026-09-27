package de.frank.genialeideen.ui

data class Meldung(
    val text: String,
    val istFehler: Boolean = false,
    val zuEinstellungen: Boolean = false,
    val wiederholen: (() -> Unit)? = null,
)
