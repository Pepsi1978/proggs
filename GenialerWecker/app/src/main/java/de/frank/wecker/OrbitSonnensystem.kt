package de.frank.wecker

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Orbits Hero-Hintergrund: ein Sonnensystem in Schrägansicht. Die Sonne glüht links, sechs Planeten
 * ziehen auf geneigten Bahnen um sie herum — vorn größer und heller, hinten kleiner und hinter der
 * Sonne. Die Erde hat ihren Mond, Saturn seinen Ring, Jupiter seine Bänder. Licht fällt immer von
 * der Sonne auf die Planeten.
 *
 * Die Zeit wird nur im Zeichenblock gelesen (neu gezeichnet wird allein diese Ebene), ~30 Bilder/s,
 * nur solange der Bildschirm vorne ist; bei reduzierter Bewegung steht ein Standbild.
 */
@Composable
internal fun Sonnensystem(modifier: Modifier) {
    val gold = LocalGold.current
    val dunkel = gold.istDunkel
    val reduziert = LocalBewegungReduziert.current
    val aktiv = rememberResumed() && !reduziert
    val zeit = remember { mutableLongStateOf(0L) }
    if (aktiv) LaunchedEffect(Unit) {
        var start = -1L
        var letzte = 0L
        while (true) {
            withFrameMillis { t ->
                if (start < 0) start = t - zeit.longValue
                if (t - letzte >= 32) { zeit.longValue = t - start; letzte = t }
            }
        }
    }
    val bahnFarbe = gold.primaer
    val sternFarbe = if (dunkel) Color(0xFFE6FFEA) else gold.primaerGedaempft
    Canvas(modifier) {
        val t = if (reduziert) 7.5f else zeit.longValue / 1000f
        zeichneSystem(t, dunkel, bahnFarbe, sternFarbe)
    }
}

private class Planet(
    val bahn: Float, val umlauf: Float, val start: Float, val radiusDp: Float,
    val hell: Color, val dunkel: Color, val art: Int = 0,
)

private const val ERDE = 1
private const val JUPITER = 2
private const val SATURN = 3

private val PLANETEN = listOf(
    Planet(0.085f, 7f, 0.4f, 2.4f, Color(0xFFD9D4CC), Color(0xFF6E6860)),
    Planet(0.125f, 11f, 2.1f, 3.4f, Color(0xFFFFE9B8), Color(0xFFA9824A)),
    Planet(0.175f, 17f, 4.0f, 3.8f, Color(0xFF7FC4FF), Color(0xFF1B4E9A), ERDE),
    Planet(0.225f, 27f, 1.0f, 3.0f, Color(0xFFFF9C6B), Color(0xFF8E3316)),
    Planet(0.32f, 55f, 3.2f, 7.0f, Color(0xFFF4DDB8), Color(0xFF9A6B3F), JUPITER),
    Planet(0.43f, 95f, 5.4f, 5.8f, Color(0xFFF6E5B5), Color(0xFFA8864C), SATURN),
)

private fun DrawScope.zeichneSystem(t: Float, dunkel: Boolean, bahnFarbe: Color, sternFarbe: Color) {
    val w = size.width
    val h = size.height
    val sonne = Offset(w * 0.19f, h * 0.52f)
    val neigung = 0.34f
    val skala = w

    // Sterne im Hintergrund, fest verteilt, leise funkelnd.
    val zufall = java.util.Random(11)
    repeat(34) { i ->
        val x = zufall.nextFloat() * w
        val y = zufall.nextFloat() * h
        val g = zufall.nextFloat()
        val funkeln = 0.55f + 0.45f * sin(t * (0.8f + g) + i)
        drawCircle(sternFarbe.copy(alpha = (0.15f + g * 0.35f) * funkeln), (0.5f + g * 0.9f).dp.toPx(), Offset(x, y))
    }

    // Bahnen: hinten zarter als vorn, damit die Neigung lesbar wird.
    PLANETEN.forEach { p ->
        val rx = p.bahn * skala
        val ry = rx * neigung
        val oben = Rect(sonne.x - rx, sonne.y - ry, sonne.x + rx, sonne.y + ry)
        drawArc(bahnFarbe.copy(alpha = if (dunkel) .14f else .20f), 180f, 180f, false, oben.topLeft, oben.size,
            style = Stroke(0.8f.dp.toPx()))
        drawArc(bahnFarbe.copy(alpha = if (dunkel) .30f else .38f), 0f, 180f, false, oben.topLeft, oben.size,
            style = Stroke(1.1f.dp.toPx()))
    }

    // Planetenlagen berechnen; hinten liegende (oberer Bahnteil) vor der Sonne zeichnen.
    data class Lage(val p: Planet, val m: Offset, val r: Float, val tiefe: Float)
    val lagen = PLANETEN.map { p ->
        val winkel = p.start + t * 2f * PI.toFloat() / p.umlauf
        val rx = p.bahn * skala
        val tiefe = sin(winkel)
        val m = Offset(sonne.x + rx * cos(winkel), sonne.y + rx * neigung * tiefe)
        Lage(p, m, p.radiusDp.dp.toPx() * (1f + 0.22f * tiefe), tiefe)
    }
    lagen.filter { it.tiefe < 0f }.sortedBy { it.tiefe }.forEach { planet(it.p, it.m, it.r, it.tiefe, sonne, t, dunkel) }
    zeichneSonne(sonne, t)
    lagen.filter { it.tiefe >= 0f }.sortedBy { it.tiefe }.forEach { planet(it.p, it.m, it.r, it.tiefe, sonne, t, dunkel) }
}

private fun DrawScope.zeichneSonne(m: Offset, t: Float) {
    val r = minOf(size.height * 0.15f, 17.dp.toPx())
    val puls = 1f + 0.05f * sin(t * 1.6f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC94D).copy(alpha = .45f), Color(0xFFFF9A1F).copy(alpha = .12f), Color.Transparent),
        center = m, radius = r * 3.4f * puls), r * 3.4f * puls, m)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF7D6), Color(0xFFFFD35C), Color(0xFFF08A1C)),
        center = m - Offset(r * .3f, r * .3f), radius = r * 1.35f), r, m)
    // Feine Granulation: zwei langsam wandernde helle Flecken.
    repeat(2) { i ->
        val a = t * (0.3f + i * 0.2f) + i * 2.5f
        val f = m + Offset(cos(a) * r * .45f, sin(a) * r * .35f)
        drawCircle(Color(0xFFFFF3C4).copy(alpha = .35f), r * .22f, f)
    }
}

private fun DrawScope.planet(p: Planet, m: Offset, r: Float, tiefe: Float, sonne: Offset, t: Float, dunkel: Boolean) {
    // Hinten liegende Planeten treten leicht zurück.
    val sicht = 0.72f + 0.28f * ((tiefe + 1f) / 2f)
    val zurSonne = (sonne - m).let { d -> val l = hypot(d.x, d.y).coerceAtLeast(1f); Offset(d.x / l, d.y / l) }
    val licht = m + zurSonne * (r * .45f)
    val koerper = Brush.radialGradient(listOf(lerp(p.hell, Color.White, .25f), p.hell, p.dunkel, lerp(p.dunkel, Color.Black, .55f)),
        center = licht, radius = r * 2.1f)

    if (p.art == SATURN) ring(m, r, sicht, vorn = false)
    drawCircle(koerper, r, m, alpha = sicht)
    if (p.art == JUPITER) {
        clipPath(Path().apply { addOval(Rect(m, r)) }) {
            listOf(-.45f to .16f, -.05f to .12f, .35f to .18f).forEach { (y, d) ->
                drawRect(Color(0xFF8A5A33).copy(alpha = .35f * sicht), Offset(m.x - r, m.y + y * r), Size(r * 2, d * r))
            }
            drawOval(Color(0xFFC2512B).copy(alpha = .55f * sicht), Offset(m.x + r * .15f, m.y + r * .22f), Size(r * .38f, r * .22f))
        }
    }
    if (p.art == ERDE) {
        // Kontinente als zwei grüne Flecken, dazu der Mond auf einer eigenen kleinen Bahn.
        clipPath(Path().apply { addOval(Rect(m, r)) }) {
            val dreh = (t * 0.6f) % (2f * PI.toFloat())
            drawCircle(Color(0xFF4FAE5A).copy(alpha = .8f * sicht), r * .45f, m + Offset(cos(dreh) * r * .5f, -r * .15f))
            drawCircle(Color(0xFF4FAE5A).copy(alpha = .7f * sicht), r * .3f, m + Offset(cos(dreh + 2.4f) * r * .55f, r * .35f))
        }
    }
    // Schattenseite von der Sonne abgewandt, für die Kugelwirkung.
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (dunkel) .45f else .25f)),
        center = licht, radius = r * 2f), r, m, alpha = sicht)
    if (p.art == ERDE) {
        val a = t * 2f * PI.toFloat() / 3.2f
        val mond = m + Offset(cos(a) * r * 2.4f, sin(a) * r * 2.4f * .4f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFEDEDED), Color(0xFF7A7A7A)), center = mond + zurSonne * (r * .2f), radius = r * .7f),
            r * .38f, mond, alpha = sicht)
    }
    if (p.art == SATURN) ring(m, r, sicht, vorn = true)
}

private fun DrawScope.ring(m: Offset, r: Float, sicht: Float, vorn: Boolean) {
    val rx = r * 2.2f
    val ry = r * .62f
    val topLeft = Offset(m.x - rx, m.y - ry)
    val farbe = Color(0xFFE3CC94).copy(alpha = .8f * sicht)
    drawArc(farbe, if (vorn) 0f else 180f, 180f, false, topLeft, Size(rx * 2, ry * 2), style = Stroke(r * .32f))
    drawArc(Color(0xFFB39460).copy(alpha = .6f * sicht), if (vorn) 0f else 180f, 180f, false,
        Offset(m.x - rx * .82f, m.y - ry * .82f), Size(rx * 1.64f, ry * 1.64f), style = Stroke(r * .14f))
}
