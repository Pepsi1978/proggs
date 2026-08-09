package de.frank.experimente.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Die Tiefen-Schicht des Designs (02-UI-SPEC.md §5 „Formen und Tiefe", gemessen in
 * `WERFT-DESIGN/bildschirme/design.css` als `--werft-schatten-*`).
 *
 * Der Entwurf ist **nicht** flach: Flächen tragen einen dunklen Schlagschatten, einen weichen
 * Schein in *Aktion* und oben innen eine 1 dp helle Kante. Der Sprechknopf trägt zusätzlich
 * einen Verlauf. Das ist keine Zutat — es steht so im Messpaket des Designers.
 */

/** `color-mix(in srgb, ⟨grund⟩, ⟨zusatz⟩ ⟨anteil⟩)` — mischt zwei Farben. */
fun mischen(grund: Color, zusatz: Color, anteil: Float): Color = Color(
    red = grund.red * (1f - anteil) + zusatz.red * anteil,
    green = grund.green * (1f - anteil) + zusatz.green * anteil,
    blue = grund.blue * (1f - anteil) + zusatz.blue * anteil,
    alpha = grund.alpha * (1f - anteil) + zusatz.alpha * anteil,
)

/**
 * `--werft-schatten-karte` — `0 16px 32px #000/18%`, `0 0 24px Aktion/7%`,
 * `inset 0 1px 0 Text/12%`. Der innere Lichtsaum wird als 1 dp Linie oben nachgezeichnet,
 * weil Compose keine `inset`-Schatten kennt.
 */
fun Modifier.schattenKarte(farben: AppColors, form: Shape): Modifier = this
    .shadow(
        elevation = 16.dp,
        shape = form,
        clip = false,
        ambientColor = farben.aktion.copy(alpha = 0.18f),
        spotColor = Color.Black.copy(alpha = 0.45f),
    )
    .kanteOben(farben.text.copy(alpha = 0.12f), form)

/** `--werft-schatten-leiste` — die schwebende untere Leiste. */
fun Modifier.schattenLeiste(farben: AppColors, form: Shape): Modifier = this
    .shadow(
        elevation = 20.dp,
        shape = form,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.20f),
        spotColor = Color.Black.copy(alpha = 0.50f),
    )
    .kanteOben(farben.text.copy(alpha = 0.10f), form)

/** `--werft-schatten-kontrolle-kompakt` — kleine Knöpfe wie „Lieber tippen". */
fun Modifier.schattenKontrolle(farben: AppColors, form: Shape): Modifier = this
    .shadow(
        elevation = 8.dp,
        shape = form,
        clip = false,
        ambientColor = farben.aktion.copy(alpha = 0.16f),
        spotColor = Color.Black.copy(alpha = 0.40f),
    )
    .kanteOben(farben.text.copy(alpha = 0.12f), form)

/**
 * `--werft-schatten-aktion` — der Sprechknopf: weit gestreuter Schein in *Aktion*
 * (`0 16px 32px Aktion/30%`) über einem dunklen Kern.
 */
fun Modifier.schattenAktion(farben: AppColors, form: Shape): Modifier = this.shadow(
    elevation = 24.dp,
    shape = form,
    clip = false,
    ambientColor = farben.aktion.copy(alpha = 0.60f),
    spotColor = farben.aktion.copy(alpha = 0.70f),
)

/** Die 1 dp helle Kante oben innen (`inset 0 1px 0 …`). */
private fun Modifier.kanteOben(farbe: Color, form: Shape): Modifier = this.drawWithContent {
    drawContent()
    val radius = if (form is RoundedCornerShape) size.height * 0.18f else 0f
    drawLine(
        color = farbe,
        start = Offset(radius, 0.5f),
        end = Offset(size.width - radius, 0.5f),
        strokeWidth = 1f,
    )
}

/**
 * Der Verlauf gefüllter Flächen in *Aktion* (gemessen:
 * `linear-gradient(145deg, mix(Aktion, Text 10%), Aktion 58%, mix(Aktion, #000 16%))`).
 */
fun aktionsVerlauf(farben: AppColors): Brush = Brush.linearGradient(
    0.0f to mischen(farben.aktion, farben.text, 0.10f),
    0.58f to farben.aktion,
    1.0f to mischen(farben.aktion, Color.Black, 0.16f),
    start = Offset(0f, 0f),
    end = Offset(220f, 320f),
)

/**
 * Der Hintergrund der Hauptbildschirme: zwei weiche Auren
 * (`radial-gradient(circle at 92% 8%, Aktion/18% … 32%)` und
 * `radial-gradient(circle at 0% 82%, Erledigt/12% … 28%)`) über *Grund*,
 * dazu die beiden 224 dp großen Ringe, die halb aus dem Bild ragen.
 */
fun Modifier.aurenHintergrund(farben: AppColors): Modifier = this
    .background(farben.grund)
    .drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(farben.aktion.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.08f),
                radius = size.minDimension * 0.62f,
            ),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.92f, size.height * 0.08f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(farben.erledigt.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(0f, size.height * 0.82f),
                radius = size.minDimension * 0.55f,
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(0f, size.height * 0.82f),
        )
        // Die beiden Ringe: 224 dp, 1 dp Rand in Aktion/22 %, Deckkraft 56 %.
        val ring = 224.dp.toPx()
        drawCircle(
            color = farben.aktion.copy(alpha = 0.22f * 0.56f),
            radius = ring / 2f,
            center = Offset(size.width + ring / 2f - 112.dp.toPx(), 112.dp.toPx() + ring / 2f),
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = farben.aktion.copy(alpha = 0.22f * 0.56f),
            radius = ring / 2f,
            center = Offset(-112.dp.toPx() + ring / 2f, size.height - 220.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )
    }

/** Die Fläche einer Karte: durchscheinend, mit Rand, Schatten und Lichtkante. */
fun Modifier.karteFlaeche(farben: AppColors, form: Shape = AppForm.karte): Modifier = this
    .schattenKarte(farben, form)
    .background(farben.flaeche.copy(alpha = 0.90f), form)
    .border(1.dp, farben.rand.copy(alpha = 0.88f), form)

@Suppress("unused")
private val unbenutzt = Size.Zero
