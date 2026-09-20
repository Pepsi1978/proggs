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

/**
 * Semantische Farben. Die Werte hier sind unveraendert die bisherigen und bleiben die Vorgabe
 * für dunkle Oberflächen, wo sie gemessen tragen (Erfolg 6,54:1, Warnung 9,89:1, Fehler 5,56:1,
 * Info 9,18:1 auf #181818).
 *
 * Auf hellen Oberflächen tragen dieselben Werte **nicht**: gemessen 2,71 / 1,79 / 3,19 / 1,93:1
 * auf Weiß — alle unter dem AA-Wert von 4,5:1. Ein Warnhinweis war dort faktisch nicht lesbar.
 * Deshalb gibt es je eine dunklere Schwesterfarbe für den hellen Modus. Der Farbcharakter bleibt
 * erhalten: Warnung bleibt Bernstein und wird NICHT wie ein Fehler rot. Abgerufen wird das über
 * [semantischeFarben]; Icon und Text bleiben in jedem Fall zusammen stehen.
 */
object Semantisch {
    val erfolg = Color(0xFF4CAF7D)
    val warnung = Color(0xFFFFB300)
    val fehler = Color(0xFFFF5252)
    val info = Color(0xFF4ECDC4)

    /** Gemessen auf Weiß: 5,32 / 6,39 / 5,62 / 6,09:1 — alle über 4,5:1. */
    val erfolgHell = Color(0xFF217A4B)
    val warnungHell = Color(0xFF8A5200)
    val fehlerHell = Color(0xFFC62828)
    val infoHell = Color(0xFF106E67)
}

/** Die vier semantischen Farben in der Fassung, die auf dem aktuellen Untergrund lesbar ist. */
@Immutable
data class SemantischeFarben(
    val erfolg: Color,
    val warnung: Color,
    val fehler: Color,
    val info: Color,
)

fun semantischeFarben(dunkel: Boolean): SemantischeFarben = if (dunkel) {
    SemantischeFarben(Semantisch.erfolg, Semantisch.warnung, Semantisch.fehler, Semantisch.info)
} else {
    SemantischeFarben(Semantisch.erfolgHell, Semantisch.warnungHell, Semantisch.fehlerHell, Semantisch.infoHell)
}
