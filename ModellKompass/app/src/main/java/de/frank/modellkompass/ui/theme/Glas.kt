package de.frank.modellkompass.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Glasfläche mit echter Tiefe: selbst gezeichneter weicher Schatten (die Fläche ist ausgestanzt, damit
 * durch das halbtransparente Glas nichts Helles durchscheint), Verlauf, Lichtkante oben, Glanz.
 */
fun Modifier.glas(
    farben: Farben,
    radius: Dp = farben.radius,
    erhoeht: Float = 1f,
    fuellung: Color = farben.flaeche,
    rand: Boolean = true,
    toenung: Color? = null,
): Modifier = drawWithCache {
    val r = radius.toPx().coerceAtMost(size.minDimension / 2)
    val form = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
    val schattenFarbe = if (farben.dunkel) Color.Black else farben.schatten
    val stufen = 7
    val tiefe = 3.dp.toPx() * erhoeht
    val verlauf = Brush.verticalGradient(
        listOf(
            fuellung.copy(alpha = (fuellung.alpha * 1.15f).coerceAtMost(1f)),
            fuellung.copy(alpha = fuellung.alpha * 0.82f),
        ),
    )
    val tonVerlauf = toenung?.let { Brush.linearGradient(listOf(it.copy(alpha = 0.16f), Color.Transparent), Offset.Zero, Offset(size.width * 0.9f, size.height)) }
    val kante = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (farben.dunkel) 0.34f else 0.95f),
        0.5f to Color.White.copy(alpha = if (farben.dunkel) 0.08f else 0.35f),
        1f to Color.White.copy(alpha = if (farben.dunkel) 0.03f else 0.2f),
    )
    val glanz = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (farben.dunkel) 0.10f else 0.45f),
        0.45f to Color.Transparent,
        endY = size.height,
    )
    // Kreisrunde Knöpfe bekommen keine Licht-/Schattenrichtung: Ein nach unten versetzter Schatten
    // und eine helle Oberkante ließen den Kreis oben wie abgeschnitten wirken.
    val kreis = istKreis(r)
    onDrawBehind {
        if (kreis) {
            if (erhoeht > 0f) clipPath(form, ClipOp.Difference) { kreisSchatten(schattenFarbe, (if (farben.dunkel) 0.16f else 0.07f) * erhoeht, erhoeht) }
            drawPath(form, fuellung)
            tonVerlauf?.let { drawPath(form, it) }
            if (rand) drawPath(form, Color.White.copy(alpha = if (farben.dunkel) 0.16f else 0.55f), style = Stroke(1.dp.toPx()))
            return@onDrawBehind
        }
        if (erhoeht > 0f) clipPath(form, ClipOp.Difference) {
            for (i in 1..stufen) {
                val a = (if (farben.dunkel) 0.16f else 0.07f) * (1f - i / (stufen + 1f)) * erhoeht
                val g = i * 1.6.dp.toPx() * erhoeht
                drawRoundRect(
                    color = schattenFarbe.copy(alpha = a),
                    topLeft = Offset(-g * 0.6f, -g * 0.3f + tiefe * i / stufen * 2f),
                    size = Size(size.width + g * 1.2f, size.height + g * 1.2f),
                    cornerRadius = CornerRadius(r + g),
                )
            }
        }
        drawPath(form, verlauf)
        tonVerlauf?.let { drawPath(form, it) }
        drawPath(form, glanz)
        if (rand) drawPath(form, kante, style = Stroke(1.dp.toPx()))
    }
}

/** Quadratische Fläche mit voller Rundung, also ein Kreis. Längliche Pillen behalten ihren 3D-Effekt. */
private fun CacheDrawScope.istKreis(r: Float): Boolean =
    abs(size.width - size.height) < 1f && r >= size.minDimension / 2 - 0.5f

/** Weicher Schatten, der den Kreis rundum gleichmäßig umgibt (ohne Versatz nach unten). */
private fun DrawScope.kreisSchatten(farbe: Color, staerke: Float, erhoeht: Float) {
    val radius = size.minDimension / 2
    for (i in 1..6) {
        drawCircle(farbe.copy(alpha = staerke * (1f - i / 7f)), radius + i * 1.5.dp.toPx() * erhoeht)
    }
}

/** Drückeffekt in 3D: kippt leicht nach hinten und sinkt ein. */
fun Modifier.druck3d(quelle: MutableInteractionSource, staerke: Float = 1f): Modifier = composed {
    val gedrueckt by quelle.collectIsPressedAsState()
    val f by animateFloatAsState(if (gedrueckt) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "druck")
    graphicsLayer {
        val s = 1f - 0.035f * f * staerke
        scaleX = s; scaleY = s
        rotationX = 5f * f * staerke
        cameraDistance = 14f * density
    }
}

@Composable
fun Modifier.antippen(rolle: Role = Role.Button, haptik: Boolean = true, aktion: () -> Unit): Modifier {
    val quelle = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    return this.druck3d(quelle).clickable(quelle, null, role = rolle) {
        if (haptik) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        aktion()
    }
}

/** Plastischer Knopf mit Verlauf, Glanzbogen und farbigem Schatten. */
fun Modifier.knopf3d(von: Color, bis: Color, radius: Dp, dunkel: Boolean): Modifier = drawWithCache {
    val r = radius.toPx().coerceAtMost(size.minDimension / 2)
    val form = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
    val koerper = Brush.linearGradient(listOf(von, bis), Offset.Zero, Offset(size.width, size.height))
    val unten = Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f))
    // Weicher ovaler Glanz ohne harte Kante — ein Rechteck mit flacher Oberkante zeichnete in runden
    // Knöpfen einen hellen Strich quer über den oberen Rand.
    val glanzMitte = Offset(size.width * 0.5f, size.height * 0.26f)
    val glanz = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.38f),
        0.6f to Color.White.copy(alpha = 0.12f),
        1f to Color.Transparent,
        center = glanzMitte,
        radius = size.width * 0.42f,
    )
    val kante = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.0f), Color.Black.copy(alpha = 0.12f)))
    val kreis = istKreis(r)
    onDrawBehind {
        if (kreis) {
            // Kreisrund (Aufnahme, Plus, Fokus): gleichmäßiger Schein rundum, kein Glanz oben, kein Dunkel unten.
            clipPath(form, ClipOp.Difference) { kreisSchatten(von, if (dunkel) 0.14f else 0.12f, 1f) }
            drawPath(form, koerper)
            drawPath(form, Color.White.copy(alpha = 0.16f), style = Stroke(1.dp.toPx()))
            return@onDrawBehind
        }
        clipPath(form, ClipOp.Difference) {
            for (i in 1..6) {
                val g = i * 1.8.dp.toPx()
                drawRoundRect(
                    color = von.copy(alpha = (if (dunkel) 0.12f else 0.10f) * (1f - i / 7f)),
                    topLeft = Offset(-g * 0.5f, g * 0.9f),
                    size = Size(size.width + g, size.height + g * 0.4f),
                    cornerRadius = CornerRadius(r + g),
                )
            }
        }
        drawPath(form, koerper)
        drawPath(form, unten)
        clipPath(form) {
            drawOval(glanz, topLeft = Offset(size.width * 0.14f, size.height * 0.06f), size = Size(size.width * 0.72f, size.height * 0.44f))
        }
        drawPath(form, kante, style = Stroke(1.dp.toPx()))
    }
}

/** Kleiner Auswahl-Chip, aktiv plastisch eingefärbt. */
@Composable
fun Chip(
    text: String,
    aktiv: Boolean,
    farbe: Color? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    aktion: () -> Unit,
) {
    val f = LocalFarben.current
    val c = farbe ?: f.primaer
    val grund = if (aktiv) Modifier.knopf3d(c, c.copy(alpha = 0.82f), 999.dp, f.dunkel)
    else Modifier.glas(f, 999.dp, erhoeht = 0.4f, fuellung = f.flaeche)
    Row(
        modifier.height(38.dp).then(grund).antippen(aktion = aktion).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (aktiv) Color.White else c, modifier = Modifier.size(16.dp))
        else if (farbe != null && !aktiv) Box(Modifier.size(8.dp).glas(f, 99.dp, 0f, c, rand = false))
        Text(text, color = if (aktiv) Color.White else f.text, fontSize = 14.sp, fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Medium)
    }
}

val RundeEcke = RoundedCornerShape(999.dp)
