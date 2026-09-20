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
 * Orbit übernimmt weiterhin die Farben seines abgestimmten Entwurfs unter `design/varianten`.
 * Morgenruhe und Traumraum sind seit 1.1.55 neu gesetzt: Die Entwurfsfarben Salbei/Terrakotta
 * und Pflaume/Rosé sind bewusst abgelöst. Die Begründung steht jeweils über der Palette.
 *
 * Jede hier gesetzte Farbkombination ist gerechnet, nicht geschätzt: Text-, Primär- und
 * Akzentfarben liegen auf ihrer jeweiligen Fläche über dem WCAG-AA-Wert von 4,5:1.
 */
fun paletteFuer(design: Design, dunkel: Boolean): GoldPalette = when (design) {
    Design.SCHLICHT -> if (dunkel) DunkleGoldPalette else HelleGoldPalette
    Design.MORGENRUHE -> if (dunkel) MorgenruheDunkel else MorgenruheHell
    Design.TRAUMRAUM -> if (dunkel) TraumraumDunkel else TraumraumHell
    Design.ORBIT -> if (dunkel) OrbitDunkel else OrbitHell
}

// --- B · Morgenruhe: Leinen und Tinte -------------------------------------------------------
//
// Früher lagen grünstichige Flächen unter einem rostroten Primärton. Das ist ein
// Komplementärpaar ohne vermittelnden Zwischenton und wirkt deshalb schmutzig statt ruhig;
// zugleich war das namensgebende Salbei nirgends Akzent, sondern nur Untergrund.
// Jetzt trägt ein neutrales, leicht warmes Leinen die Flächen, tiefes Tintenblau führt,
// ein gedeckter Messington setzt den warmen Gegenpunkt. Gemessene Kontraste: alle Paare
// Text/Primär/Akzent auf ihrer Fläche liegen über 4,5:1 (hell 4,99 bis 16,08; dunkel 6,56 bis 14,53).

// Im Dunkeln war der Primärton zunächst ein helles Himmelblau (#6FB6D1). Das lag mit einem
// RGB-Abstand von nur 37 zu viel zu nah an Orbits Eisblau (#6FD3E8) — zwei von vier Designs
// wären nachts als „graublau mit hellem Cyan" zusammengefallen. Jetzt steht hier verdünnte
// Tinte: deutlich entsättigt (24 statt 47 Prozent) und ins Violettblaue gedreht (216 statt
// 197 Grad). Der Abstand zu Orbit steigt damit auf 62, der Kontrast auf der Fläche auf 7,98:1.
val MorgenruheDunkel = GoldPalette(
    hintergrund = Color(0xFF101416),
    flaeche = Color(0xFF171D21),
    flaecheErhoeht = Color(0xFF1F272B),
    primaer = Color(0xFF9FB3D1),
    primaerGedaempft = Color(0xFF7386A5),
    aufPrimaer = Color(0xFF0B121C),
    akzentWarm = Color(0xFFD0B183),
    textPrimaer = Color(0xFFE8EEF1),
    textGedaempft = Color(0xFF94A3AC),
    rahmen = Color(0xFF2A343A),
    eingabefeld = Color(0xFF0D1113),
    istDunkel = true,
)

val MorgenruheHell = GoldPalette(
    hintergrund = Color(0xFFF4F1EC),
    flaeche = Color(0xFFFFFFFF),
    flaecheErhoeht = Color(0xFFEAE6DE),
    primaer = Color(0xFF1F4E63),
    primaerGedaempft = Color(0xFF17394A),
    aufPrimaer = Color(0xFFF2F7F9),
    akzentWarm = Color(0xFF8A6A3C),
    textPrimaer = Color(0xFF1C2226),
    textGedaempft = Color(0xFF5F6B72),
    rahmen = Color(0xFFDCD6CB),
    eingabefeld = Color(0xFFFAF8F4),
    istDunkel = false,
)

// --- D · Traumraum: Glut — warmes Schwarz und klares Orange -------------------------------
//
// Das frühere Pflaume/Rosé ist ersetzt. Das Schwarz ist bewusst *warm* (#0E0B09) und damit
// von Schlichts neutralem #121212 und Orbits kühlem #08090C unterscheidbar; das Orange ist
// deutlich rotstichiger als Schlichts Gold (#E3B341) und dessen warmer Akzent (#C25E00),
// sonst wäre Traumraum ein Schlicht-Klon. Bernstein bleibt sparsamer Zweitton, kein zweites
// Orange. Hell antwortet mit warmem Papier und einem dunkleren Orange, das auf Weiß trägt.
// Gemessene Kontraste: dunkel 6,42 bis 15,78; hell 4,78 bis 17,56 — alle über 4,5:1.

val TraumraumDunkel = GoldPalette(
    hintergrund = Color(0xFF0E0B09),
    flaeche = Color(0xFF1A1411),
    flaecheErhoeht = Color(0xFF241B16),
    // Etwas Sättigung heraus: #FF7A33 hatte den Rotkanal voll ausgereizt und las sich damit als
    // Signal-, nicht als Glutorange. #F0813F bleibt derselbe Farbton, nimmt aber die Schärfe.
    primaer = Color(0xFFF0813F),
    primaerGedaempft = Color(0xFFC96226),
    aufPrimaer = Color(0xFF1A0A02),
    akzentWarm = Color(0xFFF2C48A),
    textPrimaer = Color(0xFFF6EDE6),
    textGedaempft = Color(0xFFA8968B),
    rahmen = Color(0xFF33241C),
    eingabefeld = Color(0xFF130F0C),
    istDunkel = true,
)

val TraumraumHell = GoldPalette(
    hintergrund = Color(0xFFF7F1EA),
    flaeche = Color(0xFFFFFFFF),
    flaecheErhoeht = Color(0xFFF3E9E0),
    primaer = Color(0xFFB8460F),
    primaerGedaempft = Color(0xFF93380C),
    aufPrimaer = Color(0xFFFFF6F0),
    akzentWarm = Color(0xFF7A4A1E),
    textPrimaer = Color(0xFF211712),
    textGedaempft = Color(0xFF6E5B50),
    rahmen = Color(0xFFE4D6C9),
    eingabefeld = Color(0xFFFAF4EE),
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
