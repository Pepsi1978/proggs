package de.frank.modellkompass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import de.frank.modellkompass.data.Bereich
import de.frank.modellkompass.ui.theme.LocalFarben
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Große, zurückhaltende Strichzeichnung je Bereich. Sie liegt als Hintergrundmotiv in den Karten, in
 * gedämpftem Orange, damit die Beschriftung davor jederzeit gut lesbar bleibt.
 */
@Composable
fun BereichMotiv(b: Bereich, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val linie = if (f.dunkel) f.primaer.copy(alpha = 0.24f) else f.tertiaer.copy(alpha = 0.22f)
    Canvas(modifier) { Zeichner(this, linie, linie.copy(alpha = linie.alpha * 0.35f)).zeichne(b) }
}

/** Zeichnet in einem Einheitsquadrat (0…1), skaliert auf die kürzere Seite der Fläche. */
private class Zeichner(val d: DrawScope, val linie: Color, val flaeche: Color) {
    val u = d.size.minDimension
    val sw = u * 0.022f

    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun pfad(block: Path.() -> Unit) = Path().apply(block)
    fun Path.m(x: Float, y: Float) = moveTo(x * u, y * u)
    fun Path.l(x: Float, y: Float) = lineTo(x * u, y * u)
    fun Path.c(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) = cubicTo(x1 * u, y1 * u, x2 * u, y2 * u, x * u, y * u)
    fun Path.zug(vararg pt: Float) { m(pt[0], pt[1]); var i = 2; while (i < pt.size) { l(pt[i], pt[i + 1]); i += 2 } }

    fun strich(path: Path, breite: Float = 1f, gestrichelt: Boolean = false) = d.drawPath(
        path, linie,
        style = Stroke(sw * breite, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = if (gestrichelt) PathEffect.dashPathEffect(floatArrayOf(sw * 2.5f, sw * 2f)) else null),
    )
    fun form(path: Path) { d.drawPath(path, flaeche); strich(path) }
    fun linie(x1: Float, y1: Float, x2: Float, y2: Float, breite: Float = 1f) = d.drawLine(linie, p(x1, y1), p(x2, y2), sw * breite, StrokeCap.Round)
    fun eck(x: Float, y: Float, w: Float, h: Float, r: Float = 0.04f) = pfad { addRoundRect(RoundRect(x * u, y * u, (x + w) * u, (y + h) * u, CornerRadius(r * u))) }
    fun kreis(x: Float, y: Float, r: Float) = pfad { addOval(Rect(p(x, y), r * u)) }
    fun bogen(x: Float, y: Float, r: Float, start: Float, winkel: Float, breite: Float = 1f) = d.drawArc(
        linie, start, winkel, false, p(x - r, y - r), Size(2 * r * u, 2 * r * u), style = Stroke(sw * breite, cap = StrokeCap.Round),
    )

    fun zeichne(b: Bereich) = when (b) {
        Bereich.BILDER -> {
            form(eck(0.08f, 0.18f, 0.84f, 0.64f, 0.05f))
            form(pfad { zug(0.14f, 0.76f, 0.38f, 0.44f, 0.55f, 0.64f, 0.66f, 0.52f, 0.86f, 0.76f); close() })
            form(kreis(0.70f, 0.34f, 0.07f))
        }
        Bereich.BILD_BEARBEITEN -> {
            strich(eck(0.06f, 0.24f, 0.62f, 0.56f, 0.05f))
            strich(eck(0.15f, 0.34f, 0.32f, 0.28f, 0.01f), gestrichelt = true)
            d.rotate(38f, p(0.73f, 0.44f)) {
                form(pfad { zug(0.66f, 0.10f, 0.80f, 0.10f, 0.80f, 0.62f, 0.73f, 0.80f, 0.66f, 0.62f); close() })
                linie(0.66f, 0.20f, 0.80f, 0.20f)
                linie(0.66f, 0.62f, 0.80f, 0.62f)
            }
        }
        Bereich.VIDEO -> {
            form(eck(0.04f, 0.20f, 0.92f, 0.60f, 0.04f))
            for (i in 0 until 7) {
                val x = 0.09f + i * 0.125f
                strich(eck(x, 0.25f, 0.06f, 0.05f, 0.01f))
                strich(eck(x, 0.70f, 0.06f, 0.05f, 0.01f))
            }
            form(pfad { zug(0.42f, 0.38f, 0.42f, 0.62f, 0.62f, 0.50f); close() })
        }
        Bereich.MEDIZIN -> {
            d.rotate(-35f, p(0.42f, 0.56f)) {
                form(eck(0.12f, 0.44f, 0.60f, 0.24f, 0.12f))
                d.drawPath(eck(0.12f, 0.44f, 0.30f, 0.24f, 0.12f), flaeche)
                linie(0.42f, 0.44f, 0.42f, 0.68f)
            }
            val a = 0.045f
            val l = 0.13f
            form(pfad { zug(0.76f - a, 0.22f - l, 0.76f + a, 0.22f - l, 0.76f + a, 0.22f - a, 0.76f + l, 0.22f - a, 0.76f + l, 0.22f + a, 0.76f + a, 0.22f + a, 0.76f + a, 0.22f + l, 0.76f - a, 0.22f + l, 0.76f - a, 0.22f + a, 0.76f - l, 0.22f + a, 0.76f - l, 0.22f - a, 0.76f - a, 0.22f - a); close() })
        }
        Bereich.ALLGEMEIN -> {
            form(pfad {
                arcTo(Rect(p(0.5f, 0.40f), 0.24f * u), 135f, 270f, true)
                l(0.59f, 0.70f); l(0.41f, 0.70f); close()
            })
            strich(eck(0.41f, 0.70f, 0.18f, 0.14f, 0.03f))
            linie(0.41f, 0.77f, 0.59f, 0.77f)
            strich(pfad { zug(0.44f, 0.60f, 0.44f, 0.44f, 0.48f, 0.38f, 0.52f, 0.46f, 0.56f, 0.38f, 0.56f, 0.60f) }, 0.8f)
            for (w in listOf(-90f, -45f, -135f, 0f, 180f)) {
                val r = w * PI.toFloat() / 180f
                linie(0.5f + cos(r) * 0.31f, 0.40f + sin(r) * 0.31f, 0.5f + cos(r) * 0.39f, 0.40f + sin(r) * 0.39f)
            }
        }
        Bereich.PROGRAMMIEREN -> {
            form(eck(0.06f, 0.16f, 0.88f, 0.68f, 0.05f))
            linie(0.06f, 0.28f, 0.94f, 0.28f)
            linie(0.12f, 0.22f, 0.26f, 0.22f, 0.8f)
            strich(pfad { zug(0.30f, 0.42f, 0.19f, 0.56f, 0.30f, 0.70f) }, 1.3f)
            strich(pfad { zug(0.70f, 0.42f, 0.81f, 0.56f, 0.70f, 0.70f) }, 1.3f)
            linie(0.56f, 0.40f, 0.44f, 0.72f, 1.3f)
        }
        Bereich.TEXT -> {
            form(pfad { zug(0.22f, 0.08f, 0.64f, 0.08f, 0.80f, 0.24f, 0.80f, 0.92f, 0.22f, 0.92f); close() })
            strich(pfad { zug(0.64f, 0.08f, 0.64f, 0.24f, 0.80f, 0.24f) })
            for (i in 0 until 5) linie(0.32f, 0.38f + i * 0.1f, if (i == 4) 0.54f else 0.70f, 0.38f + i * 0.1f, 0.8f)
        }
        Bereich.KREATIV -> {
            form(pfad { m(0.5f, 0.30f); c(0.38f, 0.20f, 0.20f, 0.20f, 0.06f, 0.26f); l(0.06f, 0.80f); c(0.20f, 0.74f, 0.38f, 0.74f, 0.5f, 0.84f); close() })
            form(pfad { m(0.5f, 0.30f); c(0.62f, 0.20f, 0.80f, 0.20f, 0.94f, 0.26f); l(0.94f, 0.80f); c(0.80f, 0.74f, 0.62f, 0.74f, 0.5f, 0.84f); close() })
            for (i in 0 until 4) {
                val y = 0.38f + i * 0.1f
                strich(pfad { m(0.14f, y); c(0.24f, y - 0.04f, 0.34f, y - 0.03f, 0.42f, y + 0.02f) }, 0.7f)
                strich(pfad { m(0.58f, y + 0.02f); c(0.66f, y - 0.03f, 0.76f, y - 0.04f, 0.86f, y) }, 0.7f)
            }
        }
        Bereich.DENKEN -> {
            strich(pfad { zug(0.10f, 0.10f, 0.10f, 0.88f, 0.92f, 0.88f) })
            strich(pfad {
                for (i in 0..40) {
                    val x = 0.12f + i * 0.02f
                    val y = 0.64f - 0.16f * sin((x - 0.12f) * 2f * PI.toFloat() * 1.25f) - (x - 0.12f) * 0.12f
                    if (i == 0) m(x, y) else l(x, y)
                }
            }, 1.2f)
            strich(pfad { zug(0.80f, 0.12f, 0.56f, 0.12f, 0.68f, 0.26f, 0.56f, 0.40f, 0.80f, 0.40f) }, 1.2f)
        }
        Bereich.SEHEN -> {
            form(pfad { m(0.08f, 0.5f); c(0.26f, 0.18f, 0.74f, 0.18f, 0.92f, 0.5f); c(0.74f, 0.82f, 0.26f, 0.82f, 0.08f, 0.5f); close() })
            strich(kreis(0.5f, 0.5f, 0.17f))
            form(kreis(0.5f, 0.5f, 0.07f))
            for ((x, y, sx, sy) in listOf(listOf(0.04f, 0.08f, 1f, 1f), listOf(0.96f, 0.08f, -1f, 1f), listOf(0.04f, 0.92f, 1f, -1f), listOf(0.96f, 0.92f, -1f, -1f))) {
                strich(pfad { zug(x, y + sy * 0.12f, x, y, x + sx * 0.12f, y) })
            }
        }
        Bereich.SPRACHE_ERKENNEN -> {
            form(eck(0.38f, 0.10f, 0.24f, 0.44f, 0.12f))
            bogen(0.5f, 0.40f, 0.22f, 0f, 180f)
            linie(0.5f, 0.62f, 0.5f, 0.78f)
            linie(0.38f, 0.78f, 0.62f, 0.78f)
            listOf(0.10f to 0.08f, 0.17f to 0.16f, 0.24f to 0.10f, 0.76f to 0.12f, 0.83f to 0.18f, 0.90f to 0.07f).forEach { (x, h) -> linie(x, 0.36f - h, x, 0.36f + h) }
        }
        Bereich.SPRACHE_AUSGEBEN -> {
            form(pfad { zug(0.08f, 0.40f, 0.24f, 0.40f, 0.44f, 0.22f, 0.44f, 0.78f, 0.24f, 0.60f, 0.08f, 0.60f); close() })
            for (r in listOf(0.14f, 0.26f, 0.38f)) bogen(0.46f, 0.5f, r, -45f, 90f)
        }
        Bereich.MUSIK -> {
            for (i in 0 until 5) linie(0.04f, 0.30f + i * 0.08f, 0.96f, 0.30f + i * 0.08f, 0.6f)
            d.rotate(-20f, p(0.34f, 0.62f)) { d.drawOval(linie, p(0.27f, 0.575f), Size(0.14f * u, 0.09f * u)) }
            d.rotate(-20f, p(0.66f, 0.50f)) { d.drawOval(linie, p(0.59f, 0.455f), Size(0.14f * u, 0.09f * u)) }
            linie(0.40f, 0.60f, 0.40f, 0.18f)
            linie(0.72f, 0.48f, 0.72f, 0.08f)
            linie(0.40f, 0.18f, 0.72f, 0.08f, 2.4f)
        }
        Bereich.UEBERSETZEN -> {
            form(kreis(0.5f, 0.5f, 0.40f))
            linie(0.10f, 0.5f, 0.90f, 0.5f)
            linie(0.154f, 0.30f, 0.846f, 0.30f, 0.8f)
            linie(0.154f, 0.70f, 0.846f, 0.70f, 0.8f)
            linie(0.5f, 0.10f, 0.5f, 0.90f)
            strich(pfad { addOval(Rect(0.30f * u, 0.10f * u, 0.70f * u, 0.90f * u)) }, 0.8f)
        }
        Bereich.AGENTEN -> {
            form(pfad {
                for (i in 0 until 8) {
                    val a = i * 45f
                    listOf(a - 14f to 0.29f, a - 8f to 0.38f, a + 8f to 0.38f, a + 14f to 0.29f).forEachIndexed { j, (w, r) ->
                        val rad = w * PI.toFloat() / 180f
                        val x = 0.5f + cos(rad) * r
                        val y = 0.5f + sin(rad) * r
                        if (i == 0 && j == 0) m(x, y) else l(x, y)
                    }
                }
                close()
            })
            strich(kreis(0.5f, 0.5f, 0.12f))
            strich(kreis(0.5f, 0.5f, 0.21f), 0.6f, gestrichelt = true)
        }
        Bereich.WISSEN -> {
            strich(eck(0.08f, 0.08f, 0.44f, 0.56f, 0.03f))
            form(eck(0.16f, 0.16f, 0.44f, 0.56f, 0.03f))
            for (i in 0 until 4) linie(0.24f, 0.28f + i * 0.09f, 0.50f, 0.28f + i * 0.09f, 0.7f)
            form(kreis(0.62f, 0.58f, 0.17f))
            linie(0.74f, 0.70f, 0.92f, 0.88f, 2.2f)
        }
        Bereich.DREI_D -> {
            form(pfad { zug(0.5f, 0.12f, 0.83f, 0.31f, 0.5f, 0.5f, 0.17f, 0.31f); close() })
            form(pfad { zug(0.17f, 0.31f, 0.5f, 0.5f, 0.5f, 0.88f, 0.17f, 0.69f); close() })
            strich(pfad { zug(0.5f, 0.5f, 0.83f, 0.31f, 0.83f, 0.69f, 0.5f, 0.88f) })
            strich(pfad { zug(0.17f, 0.69f, 0.5f, 0.5f, 0.83f, 0.69f) }, 0.6f, gestrichelt = true)
        }
        Bereich.TEMPO -> {
            form(pfad { zug(0.60f, 0.06f, 0.28f, 0.52f, 0.48f, 0.52f, 0.40f, 0.94f, 0.76f, 0.42f, 0.55f, 0.42f, 0.66f, 0.06f); close() })
            linie(0.04f, 0.30f, 0.20f, 0.30f)
            linie(0.02f, 0.46f, 0.16f, 0.46f)
            linie(0.06f, 0.62f, 0.18f, 0.62f)
        }
    }
}
