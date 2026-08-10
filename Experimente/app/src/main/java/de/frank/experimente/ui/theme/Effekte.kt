package de.frank.experimente.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Die Tiefen-Schicht des Entwurfs: Verläufe, farbige Scheine, Lichtsäume.
 *
 * Sie ist die Schicht, die beim Bauen am leichtesten verlorengeht — und in Lauf 01 auch
 * verlorenging, weil ein v1-Satz ("Keine Schatten. Keine Verläufe.") über den gemessenen
 * Werten stand. Verbindlich sind die Messwerte aus `02-UI-SPEC.md` §5.
 */

/**
 * `radial-gradient(circle at 92% 8%, aktion 18% 0, transparent 32%),`
 * `radial-gradient(circle at 0% 82%, erledigt 12% 0, transparent 28%), grund`
 *
 * Der Hintergrund jedes Bildschirms — zwei Auren über dem Grundton (gradient(4)/gradient(6)).
 */
fun Modifier.grundAuren(farben: Farben): Modifier = drawBehind {
    drawRect(farben.grund)
    val warm = mische(farben.aktion, Color.Transparent, 0.82f)
    val kuehl = mische(farben.erledigt, Color.Transparent, 0.88f)
    val mitteWarm = Offset(size.width * 0.92f, size.height * 0.08f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(warm, Color.Transparent),
            center = mitteWarm,
            radius = size.minDimension * 0.64f,
        ),
        center = mitteWarm,
        radius = size.minDimension * 0.64f,
    )
    val mitteKuehl = Offset(0f, size.height * 0.82f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(kuehl, Color.Transparent),
            center = mitteKuehl,
            radius = size.minDimension * 0.56f,
        ),
        center = mitteKuehl,
        radius = size.minDimension * 0.56f,
    )
}

/**
 * `linear-gradient(145deg, mix(aktion, text 10%), aktion 58%, mix(aktion, #000 16%))`
 * — der Sprechknopf (gradient(5)/gradient(9)).
 */
fun sprechknopfVerlauf(farben: Farben): Brush = Brush.linearGradient(
    0.00f to mische(farben.aktion, farben.text, 0.10f),
    0.58f to farben.aktion,
    1.00f to mische(farben.aktion, Color.Black, 0.16f),
    start = Offset.Zero,
    end = Offset(88f, 88f),
)

/**
 * `linear-gradient(145deg, mix(aktionGedeckt 88%, text 6%), aktionGedeckt)`
 * — die gedeckte Fläche hinter Hinweisen (gradient(7)).
 */
fun gedeckterVerlauf(farben: Farben): Brush = Brush.linearGradient(
    0.00f to mische(farben.aktionGedeckt, farben.text, 0.06f),
    1.00f to farben.aktionGedeckt,
)

/**
 * `inset 0 1px 0 color-mix(in srgb, currentColor 18%, transparent)`
 *
 * Compose kennt keine `inset`-Schatten. Der Lichtsaum oben wird deshalb als 1-dp-Linie
 * innen nachgezeichnet — weglassen ist nicht zulässig, er trägt die Plastizität.
 */
fun Modifier.lichtsaum(farbe: Color, anteil: Float = 0.18f): Modifier = drawWithContent {
    drawContent()
    drawRect(
        color = farbe.copy(alpha = anteil),
        size = androidx.compose.ui.geometry.Size(size.width, 1f),
    )
}

/** Die 1-dp-Trennlinie, die der Entwurf über die Oberkante der unteren Leiste zieht. */
fun Modifier.oberkante(farbe: Color): Modifier = drawBehind {
    drawRect(color = farbe, size = androidx.compose.ui.geometry.Size(size.width, 1f))
}
