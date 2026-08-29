package de.frank.genialeideen.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Die verbindliche Gold-Palette aus Baustein A — beide Modi vollständig durchgezeichnet.
 * Dynamic Color bleibt aus: Gold ist die Markenfarbe.
 */
@Immutable
data class GoldPalette(
    val hintergrund: Color,
    val flaeche: Color,
    val flaecheErhoeht: Color,
    val primaer: Color,
    val primaerGedaempft: Color,
    val aufPrimaer: Color,
    val akzentWarm: Color,
    val textPrimaer: Color,
    val textGedaempft: Color,
    val rahmen: Color,
    val eingabefeld: Color,
    val istDunkel: Boolean,
)

val DunkleGoldPalette = GoldPalette(
    hintergrund = Color(0xFF121212),
    flaeche = Color(0xFF181818),
    flaecheErhoeht = Color(0xFF282828),
    primaer = Color(0xFFE3B341),
    primaerGedaempft = Color(0xFFC9922B),
    aufPrimaer = Color(0xFF1A1408),
    akzentWarm = Color(0xFFC25E00),
    textPrimaer = Color(0xFFEDE7DA),
    textGedaempft = Color(0xFFA79C86),
    rahmen = Color(0xFF2C2620),
    eingabefeld = Color(0xFF141414),
    istDunkel = true,
)

val HelleGoldPalette = GoldPalette(
    hintergrund = Color(0xFFFAF7F0),
    flaeche = Color(0xFFFFFFFF),
    flaecheErhoeht = Color(0xFFF4EFE3),
    primaer = Color(0xFF8B6914),
    primaerGedaempft = Color(0xFFA9812A),
    aufPrimaer = Color(0xFFFFFFFF),
    akzentWarm = Color(0xFFA34F00),
    textPrimaer = Color(0xFF1B1710),
    textGedaempft = Color(0xFF6B6151),
    rahmen = Color(0xFFE6DFCF),
    eingabefeld = Color(0xFFF7F3EA),
    istDunkel = false,
)

/** Semantische Farben — in beiden Modi gleich, Gold bleibt der Marke vorbehalten. */
object Semantisch {
    val erfolg = Color(0xFF4CAF7D)
    val warnung = Color(0xFFFFB300)
    val fehler = Color(0xFFFF5252)
    val info = Color(0xFF4ECDC4)
}
