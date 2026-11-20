package de.frank.jarvis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.glas
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Welche Karte ihr Symbol zeigt. */
enum class KartenArt { AUFGABEN, BIOMARKER, RAUMKLIMA, SOLAR, AUSWERTUNG, MAIL }

private val ROT = Color(0xFFE5484D)
private val ROT_HELL = Color(0xFFFF7A7E)
private val GELB = Color(0xFFF5C451)
private val GRUEN = Color(0xFF3DBE7A)
private val BLAU = Color(0xFF4DA3F0)
private val PAPIER = Color(0xFFF7F7F5)
private val LINIE = Color(0xFFB9B9B4)
private val KLAMMER = Color(0xFF9A7B5C)

/**
 * Selbst gezeichnetes Symbol links im Kartenkopf, im Stil der Kaffeetassen (Franks Wunsch vom 18.11.2026): Umriss in der leisen
 * Textfarbe, farbige Füllung, auf einer kleinen Glasfläche. [wert] (0 bis 1) zeigt, wo es passt, den Live-Stand: Ladezustand der
 * Batterie, Anteil erledigter Aufgaben.
 */
@Composable
fun KartenSymbol(art: KartenArt, modifier: Modifier = Modifier, wert: Float? = null) {
    val f = LocalFarben.current
    val rand = f.textLeise
    val primaer = f.primaer
    Box(modifier.size(42.dp).glas(f, 14.dp, erhoeht = 0.5f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(32.dp)) {
            when (art) {
                KartenArt.AUFGABEN -> klemmbrett(rand, wert)
                KartenArt.BIOMARKER -> herz()
                KartenArt.RAUMKLIMA -> thermometer(rand)
                KartenArt.SOLAR -> solarBatterie(rand, wert)
                KartenArt.AUSWERTUNG -> auswertung(primaer)
                KartenArt.MAIL -> umschlag(rand, wert)
            }
        }
    }
}

private fun DrawScope.klemmbrett(rand: Color, anteil: Float?) {
    val w = size.width
    val h = size.height
    val strich = Stroke(w * 0.055f)
    drawRoundRect(PAPIER, Offset(w * 0.16f, h * 0.12f), Size(w * 0.68f, h * 0.84f), CornerRadius(w * 0.08f))
    drawRoundRect(rand, Offset(w * 0.16f, h * 0.12f), Size(w * 0.68f, h * 0.84f), CornerRadius(w * 0.08f), style = strich)
    drawRoundRect(KLAMMER, Offset(w * 0.34f, h * 0.04f), Size(w * 0.32f, h * 0.15f), CornerRadius(w * 0.05f))
    val erledigt = anteil?.let { (it.coerceIn(0f, 1f) * 3).roundToInt() } ?: 2
    for (i in 0 until 3) {
        val y = h * (0.38f + i * 0.19f)
        val x = w * 0.26f
        if (i < erledigt) {
            val haken = Path().apply { moveTo(x, y); lineTo(x + w * 0.05f, y + h * 0.05f); lineTo(x + w * 0.13f, y - h * 0.06f) }
            drawPath(haken, GRUEN, style = Stroke(w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawCircle(rand.copy(alpha = 0.7f), w * 0.05f, Offset(x + w * 0.06f, y), style = Stroke(w * 0.04f))
        }
        drawLine(LINIE, Offset(w * 0.46f, y), Offset(w * 0.74f, y), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.herz() {
    val w = size.width
    val h = size.height
    val form = Path().apply {
        moveTo(w * 0.5f, h * 0.9f)
        cubicTo(w * 0.12f, h * 0.64f, w * 0.0f, h * 0.4f, w * 0.12f, h * 0.22f)
        cubicTo(w * 0.26f, h * 0.04f, w * 0.46f, h * 0.1f, w * 0.5f, h * 0.27f)
        cubicTo(w * 0.54f, h * 0.1f, w * 0.74f, h * 0.04f, w * 0.88f, h * 0.22f)
        cubicTo(w * 1.0f, h * 0.4f, w * 0.88f, h * 0.64f, w * 0.5f, h * 0.9f)
        close()
    }
    drawPath(form, Brush.verticalGradient(listOf(ROT_HELL, ROT), startY = 0f, endY = h))
    val puls = Path().apply {
        moveTo(w * 0.12f, h * 0.48f); lineTo(w * 0.34f, h * 0.48f); lineTo(w * 0.43f, h * 0.3f)
        lineTo(w * 0.54f, h * 0.66f); lineTo(w * 0.62f, h * 0.44f); lineTo(w * 0.88f, h * 0.44f)
    }
    drawPath(puls, Color.White, style = Stroke(w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.thermometer(rand: Color) {
    val w = size.width
    val h = size.height
    val mx = w * 0.34f
    val kugelY = h * 0.76f
    val kugel = w * 0.15f
    val rohr = w * 0.16f
    val oben = h * 0.08f
    val umriss = Path.combine(
        PathOperation.Union,
        Path().apply { addRoundRect(RoundRect(mx - rohr / 2, oben, mx + rohr / 2, kugelY, CornerRadius(rohr / 2))) },
        Path().apply { addOval(Rect(Offset(mx, kugelY), kugel)) },
    )
    drawPath(umriss, PAPIER)
    drawRoundRect(ROT, Offset(mx - rohr * 0.22f, h * 0.34f), Size(rohr * 0.44f, kugelY - h * 0.34f), CornerRadius(rohr * 0.22f))
    drawCircle(ROT, kugel * 0.66f, Offset(mx, kugelY))
    drawPath(umriss, rand, style = Stroke(w * 0.055f))
    // Tropfen für die Luftfeuchtigkeit.
    val dx = w * 0.74f
    val tropfen = Path().apply {
        moveTo(dx, h * 0.32f)
        cubicTo(dx + w * 0.04f, h * 0.44f, dx + w * 0.16f, h * 0.52f, dx + w * 0.16f, h * 0.64f)
        cubicTo(dx + w * 0.16f, h * 0.74f, dx + w * 0.08f, h * 0.8f, dx, h * 0.8f)
        cubicTo(dx - w * 0.08f, h * 0.8f, dx - w * 0.16f, h * 0.74f, dx - w * 0.16f, h * 0.64f)
        cubicTo(dx - w * 0.16f, h * 0.52f, dx - w * 0.04f, h * 0.44f, dx, h * 0.32f)
        close()
    }
    drawPath(tropfen, BLAU)
    drawOval(Color.White.copy(alpha = 0.6f), Offset(dx - w * 0.09f, h * 0.58f), Size(w * 0.05f, h * 0.11f))
}

private fun DrawScope.sonne(mx: Float, my: Float, r: Float) {
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val c = cos(a).toFloat()
        val s = sin(a).toFloat()
        drawLine(GELB, Offset(mx + c * r * 1.45f, my + s * r * 1.45f), Offset(mx + c * r * 2.0f, my + s * r * 2.0f), strokeWidth = r * 0.42f, cap = StrokeCap.Round)
    }
    drawCircle(GELB, r, Offset(mx, my))
}

private fun DrawScope.solarBatterie(rand: Color, anteil: Float?) {
    val w = size.width
    val h = size.height
    sonne(w * 0.22f, h * 0.24f, w * 0.1f)
    val x = w * 0.5f
    val y = h * 0.26f
    val b = w * 0.36f
    val ho = h * 0.68f
    drawRoundRect(rand, Offset(x + b * 0.28f, h * 0.17f), Size(b * 0.44f, h * 0.1f), CornerRadius(w * 0.03f))
    drawRoundRect(PAPIER, Offset(x, y), Size(b, ho), CornerRadius(w * 0.07f))
    val stand = (anteil ?: 0.6f).coerceIn(0f, 1f)
    val farbe = if (anteil == null) LINIE else if (stand > 0.5f) GRUEN else if (stand > 0.2f) GELB else ROT
    val innen = w * 0.06f
    val voll = (ho - 2 * innen) * stand.coerceAtLeast(0.06f)
    clipRect(x + innen, y + ho - innen - voll, x + b - innen, y + ho - innen) {
        drawRoundRect(farbe, Offset(x + innen, y + innen), Size(b - 2 * innen, ho - 2 * innen), CornerRadius(w * 0.04f))
    }
    drawRoundRect(rand, Offset(x, y), Size(b, ho), CornerRadius(w * 0.07f), style = Stroke(w * 0.055f))
}

private fun DrawScope.auswertung(primaer: Color) {
    val w = size.width
    val h = size.height
    sonne(w * 0.24f, h * 0.24f, w * 0.1f)
    val boden = h * 0.94f
    listOf(0.28f to 0.55f, 0.46f to 0.78f, 0.66f to 1f).forEachIndexed { i, (hoehe, deckung) ->
        val x = w * (0.1f + i * 0.29f)
        drawRoundRect(primaer.copy(alpha = deckung), Offset(x, boden - h * hoehe), Size(w * 0.22f, h * hoehe), CornerRadius(w * 0.05f))
    }
}

/** Briefumschlag; [anteil] > 0 = es gibt wichtige E-Mails (roter Punkt oben rechts). */
private fun DrawScope.umschlag(rand: Color, anteil: Float?) {
    val w = size.width
    val h = size.height
    val x = w * 0.08f
    val y = h * 0.24f
    val b = w * 0.84f
    val ho = h * 0.6f
    drawRoundRect(PAPIER, Offset(x, y), Size(b, ho), CornerRadius(w * 0.07f))
    val klappe = Path().apply { moveTo(x + w * 0.04f, y + h * 0.04f); lineTo(x + b / 2, y + ho * 0.58f); lineTo(x + b - w * 0.04f, y + h * 0.04f) }
    drawPath(klappe, BLAU, style = Stroke(w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawRoundRect(rand, Offset(x, y), Size(b, ho), CornerRadius(w * 0.07f), style = Stroke(w * 0.055f))
    if ((anteil ?: 0f) > 0f) {
        drawCircle(Color.White, w * 0.15f, Offset(x + b - w * 0.02f, y))
        drawCircle(ROT, w * 0.12f, Offset(x + b - w * 0.02f, y))
    }
}
