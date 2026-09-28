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
    // Das Tagesblatt liegt eine Stufe über der Seite: 1,30:1 statt der 1,08:1 von `flaeche`.
    heroFlaeche = Color(0xFF232C31),
    heroRahmen = Color(0xFF3A464D),
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
    // Leinen eine Stufe tiefer (1,20:1 zur Seite) plus ein deutlich sichtbarer Rahmen (1,58:1).
    // Morgenruhe trägt bewusst keinen Schatten — die Kante muss die Arbeit machen.
    heroFlaeche = Color(0xFFE3DDD2),
    heroRahmen = Color(0xFFC9C1B3),
    // Der gewohnte gedämpfte Ton läge auf dieser Fläche bei 4,05:1; dieser bei 4,94:1.
    heroTextGedaempft = Color(0xFF525E65),
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
    // Die Kuppel hebt sich jetzt mit 1,33:1 von der Seite ab statt mit 1,16:1. Bewusst kein
    // Verlauf nach unten dunkler: Genau an der unteren Rundung zählt die Abgrenzung.
    // Seit 28.09.2026 dunkler (#342519 → #271D17): Die Kuppel ist Nachthimmel und stand zu hell
    // unter der dunklen Leiste. Die Leiste ist dafür heller, beide treffen sich in der Mitte.
    heroFlaeche = Color(0xFF271D17),
    heroRahmen = Color(0xFF3E2D20),
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
    // Warm-helle Pfirsichkuppel mit leichtem Verlauf. Die Fläche allein trägt nur 1,25:1 zur
    // Seite — deshalb kommen ein deutlich sichtbarer Rahmen (1,64:1) und die farbige
    // Umgebungsschicht dazu. Erst zusammen ergibt das eine erkennbare eigene Fläche.
    heroFlaeche = Color(0xFFEFD5C0),
    heroFlaecheUnten = Color(0xFFF3DCCB),
    heroRahmen = Color(0xFFD9B99E),
    // Auf Pfirsich fiele der Primärton auf 3,81:1. Der gedämpfte trägt mit 5,32:1.
    heroPrimaer = Color(0xFF93380C),
    heroTextGedaempft = Color(0xFF66544A),
)

// --- C · Orbit: Fast-Schwarz mit Matrix-Grün, helles Limettengelb als Signal ------------------
// Seit 28.09.2026 grün statt eisblau (Wunsch): wie die echte Matrix, im Hellen dunkles Waldgrün auf Weiß.

val OrbitDunkel = GoldPalette(
    hintergrund = Color(0xFF040906),
    flaeche = Color(0xFF0B140E),
    flaecheErhoeht = Color(0xFF111D15),
    primaer = Color(0xFF3CF06E),
    primaerGedaempft = Color(0xFF22A849),
    aufPrimaer = Color(0xFF02140A),
    akzentWarm = Color(0xFFD7FF6B),
    textPrimaer = Color(0xFFE2F5E6),
    textGedaempft = Color(0xFF6E9477),
    rahmen = Color(0xFF1C3323),
    eingabefeld = Color(0xFF07100A),
    istDunkel = true,
    // Das Hauptinstrument liegt knapp über der Tafel — bei Orbit reicht wenig, weil die
    // harten Kanten und Trennlinien die Abgrenzung übernehmen.
    heroFlaeche = Color(0xFF0F1C13),
    heroRahmen = Color(0xFF28503A),
    heroTextGedaempft = Color(0xFF84AA8C),
)

val OrbitHell = GoldPalette(
    hintergrund = Color(0xFFE8F1EA),
    flaeche = Color(0xFFFFFFFF),
    flaecheErhoeht = Color(0xFFF3F9F4),
    primaer = Color(0xFF0A7A2C),
    primaerGedaempft = Color(0xFF075C21),
    aufPrimaer = Color(0xFFF0FFF3),
    akzentWarm = Color(0xFF6B8A00),
    textPrimaer = Color(0xFF0E1A11),
    textGedaempft = Color(0xFF52705C),
    rahmen = Color(0xFFCADBCE),
    eingabefeld = Color(0xFFF6FAF7),
    istDunkel = false,
    heroFlaeche = Color(0xFFFFFFFF),
    heroRahmen = Color(0xFFB2CCB8),
)
