package de.frank.experimente.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Maße und Raster aus 02-UI-SPEC.md §4. Grundraster 4 dp, alle Werte Vielfache davon.
 */
object Mass {
    val raster = 4.dp

    val seitenrand = 20.dp
    val kartenAbstand = 12.dp
    val karteInnen = 20.dp
    val abschnittAbstand = 32.dp

    val untereLeiste = 72.dp
    val obereLeiste = 64.dp

    /** Jedes bedienbare Element hält diese Mindestgröße ein. */
    val tippflaeche = 48.dp

    val sprechknopfGross = 88.dp
    val sprechknopfKlein = 56.dp

    val symbolListe = 24.dp
    val symbolKarte = 28.dp

    val randstaerke = 1.dp

    /** Höchstens 62 Zeichen je Zeile, damit lange Auswertungen lesbar bleiben. */
    const val MAX_ZEICHEN_JE_ZEILE = 62
}
