package de.frank.wecker.design

import androidx.compose.ui.graphics.Color
import de.frank.genialeideen.ui.theme.DunkleGoldPalette
import de.frank.genialeideen.ui.theme.GoldPalette
import de.frank.genialeideen.ui.theme.HelleGoldPalette

/**
 * Die Farbwelt je Design und Modus. Die Felder sind dieselben wie bisher, deshalb zieht jeder
 * vorhandene Baustein ohne Änderung mit.
 *
 * [Design.SCHLICHT] gibt unverändert die bestehenden Paletten zurück — dieselben Objekte, keine
 * nachgebauten Werte. Damit ist die gewohnte Optik nicht nur ähnlich, sondern identisch.
 *
 * Die drei anderen übernehmen die Farben der abgestimmten Entwürfe unter `design/varianten`.
 */
fun paletteFuer(design: Design, dunkel: Boolean): GoldPalette = when (design) {
    Design.SCHLICHT -> if (dunkel) DunkleGoldPalette else HelleGoldPalette
    Design.MORGENRUHE -> if (dunkel) MorgenruheDunkel else MorgenruheHell
    Design.TRAUMRAUM -> if (dunkel) TraumraumDunkel else TraumraumHell
    Design.ORBIT -> if (dunkel) OrbitDunkel else OrbitHell
}

// --- B · Morgenruhe: Salbei und Creme am Tag, Tannengrün in der Nacht, Terrakotta als Akzent ----

val MorgenruheDunkel = GoldPalette(
    hintergrund = Color(0xFF131C18),
    flaeche = Color(0xFF1B2721),
    flaecheErhoeht = Color(0xFF23312A),
    primaer = Color(0xFFD98B63),
    primaerGedaempft = Color(0xFFB37050),
    aufPrimaer = Color(0xFF1A1009),
    akzentWarm = Color(0xFFE0A87F),
    textPrimaer = Color(0xFFECF1EA),
    textGedaempft = Color(0xFF9DB0A4),
    rahmen = Color(0xFF32453B),
    eingabefeld = Color(0xFF16201B),
    istDunkel = true,
)

val MorgenruheHell = GoldPalette(
    hintergrund = Color(0xFFF2F1E7),
    flaeche = Color(0xFFFFFFFF),
    flaecheErhoeht = Color(0xFFE8ECE1),
    primaer = Color(0xFFB8562F),
    primaerGedaempft = Color(0xFF964626),
    aufPrimaer = Color(0xFFFFFAF6),
    akzentWarm = Color(0xFFC96F45),
    textPrimaer = Color(0xFF22302A),
    textGedaempft = Color(0xFF61735F),
    rahmen = Color(0xFFD3D9CB),
    eingabefeld = Color(0xFFF7F8F2),
    istDunkel = false,
)

// --- D · Traumraum: Pflaume, Rosé und Perlmutt -----------------------------------------------

val TraumraumDunkel = GoldPalette(
    hintergrund = Color(0xFF171029),
    flaeche = Color(0xFF2C2049),
    flaecheErhoeht = Color(0xFF3A2B5E),
    primaer = Color(0xFFF0A6BD),
    primaerGedaempft = Color(0xFFD5799A),
    aufPrimaer = Color(0xFF2A0F1D),
    akzentWarm = Color(0xFFE8D9C4),
    textPrimaer = Color(0xFFF4ECFF),
    textGedaempft = Color(0xFFB0A1C9),
    rahmen = Color(0xFF453466),
    eingabefeld = Color(0xFF241A3D),
    istDunkel = true,
)

val TraumraumHell = GoldPalette(
    hintergrund = Color(0xFFF7F0F3),
    flaeche = Color(0xFFFFFFFF),
    flaecheErhoeht = Color(0xFFF6EEF2),
    primaer = Color(0xFFA8446B),
    primaerGedaempft = Color(0xFF8B2F53),
    aufPrimaer = Color(0xFFFFF4F8),
    akzentWarm = Color(0xFF8A6F52),
    textPrimaer = Color(0xFF2C1B33),
    textGedaempft = Color(0xFF7B6482),
    rahmen = Color(0xFFE4D3DC),
    eingabefeld = Color(0xFFFBF5F8),
    istDunkel = false,
)

// --- C · Orbit: Fast-Schwarz mit Eisblau, Limette als Signal ---------------------------------

val OrbitDunkel = GoldPalette(
    hintergrund = Color(0xFF08090C),
    flaeche = Color(0xFF101319),
    flaecheErhoeht = Color(0xFF171B23),
    primaer = Color(0xFF6FD3E8),
    primaerGedaempft = Color(0xFF4FA8BC),
    aufPrimaer = Color(0xFF05171C),
    akzentWarm = Color(0xFFC6F24A),
    textPrimaer = Color(0xFFE9EDF4),
    textGedaempft = Color(0xFF79839A),
    rahmen = Color(0xFF262C38),
    eingabefeld = Color(0xFF0C0F14),
    istDunkel = true,
)

val OrbitHell = GoldPalette(
    hintergrund = Color(0xFFECEEF2),
    flaeche = Color(0xFFFFFFFF),
    flaecheErhoeht = Color(0xFFF4F6FA),
    primaer = Color(0xFF0F6D85),
    primaerGedaempft = Color(0xFF0B5064),
    aufPrimaer = Color(0xFFF2FBFF),
    akzentWarm = Color(0xFF4D7A05),
    textPrimaer = Color(0xFF11151D),
    textGedaempft = Color(0xFF5C6578),
    rahmen = Color(0xFFD2D7E0),
    eingabefeld = Color(0xFFF8F9FC),
    istDunkel = false,
)
