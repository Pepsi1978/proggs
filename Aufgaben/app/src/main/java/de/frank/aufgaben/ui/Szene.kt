package de.frank.aufgaben.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.ui.theme.Design
import de.frank.aufgaben.ui.theme.Farben
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.glas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val ZYKLUS = 24f
private const val STANDBILD = 16f
private val PHASEN = listOf("Nachdenken", "Planen", "Umsetzen", "Freuen")

private fun phase(t: Float): Int = when {
    t < 5f -> 0
    t < 10f -> 1
    t < 18f -> 2
    else -> 3
}

/**
 * Kleine Endlosszene rund um Aufgaben: nachdenken (Glühbirne geht an), planen (Liste entsteht),
 * umsetzen (Haken für Haken) und sich freuen (Konfetti). Jedes Design hat eine eigene Kulisse:
 * Polarlicht, wachsender Garten, Sonnenuntergang oder startende Rakete.
 */
@Composable
fun AufgabenSzene(modifier: Modifier = Modifier, erledigtAnteil: Float) {
    val f = LocalFarben.current
    val bewegung = LocalBewegung.current
    val zeit = rememberSzenenZeit()
    val aktivePhase by remember { derivedStateOf { if (!bewegung) 2 else phase((zeit.value % (ZYKLUS * 1000).toLong()) / 1000f) } }
    Column(modifier.glas(f, erhoeht = 1.2f).padding(10.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val t = if (!bewegung) STANDBILD else (zeit.value % (ZYKLUS * 1000).toLong()) / 1000f
                clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(f.radius.toPx() * 0.7f))) }) {
                    zeichneSzene(t, f, erledigtAnteil)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PHASEN.forEachIndexed { i, name ->
                val farbe by animateColorAsState(if (i == aktivePhase) f.primaer else f.textSchwach, label = "phase")
                Text(name, color = farbe, fontSize = 12.sp, fontWeight = if (i == aktivePhase) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

private fun lerp(a: Float, b: Float, x: Float) = a + (b - a) * x.coerceIn(0f, 1f)
private fun an(t: Float, start: Float, dauer: Float) = ((t - start) / dauer).coerceIn(0f, 1f)
private fun weich(x: Float) = x * x * (3 - 2 * x)

private fun DrawScope.zeichneSzene(t: Float, f: Farben, anteil: Float) {
    val w = size.width
    val h = size.height
    val ausblenden = 1f - an(t, 22f, 1.6f)
    // Raum
    drawRect(Brush.verticalGradient(listOf(f.flaecheStark.copy(alpha = 0.55f), f.flaeche.copy(alpha = 0.2f))))
    val boden = h * 0.9f
    drawRect(f.primaer.copy(alpha = if (f.dunkel) 0.18f else 0.12f), Offset(0f, boden), Size(w, h - boden))
    drawLine(f.primaer.copy(alpha = 0.35f), Offset(0f, boden), Offset(w, boden), 2f)

    // Fenster mit Kulisse
    val fenster = Rect(w * 0.04f, h * 0.1f, w * 0.34f, h * 0.66f)
    kulisse(t, f, fenster)

    // Tafel
    val tafel = Rect(w * 0.6f, h * 0.08f, w * 0.96f, h * 0.76f)
    drawRoundRect(Color.White.copy(alpha = if (f.dunkel) 0.12f else 0.75f), tafel.topLeft, tafel.size, CornerRadius(14f))
    drawRoundRect(f.primaer.copy(alpha = 0.35f), tafel.topLeft, tafel.size, CornerRadius(14f), style = Stroke(2f))
    val zeilen = 4
    for (i in 0 until zeilen) {
        val y = tafel.top + tafel.height * (0.2f + i * 0.19f)
        val auf = weich(an(t, 5.4f + i * 1.15f, 0.9f)) * ausblenden
        if (auf <= 0f) continue
        val kx = tafel.left + tafel.width * 0.09f
        val k = tafel.height * 0.1f
        drawRoundRect(f.primaer.copy(alpha = 0.9f * auf), Offset(kx, y - k / 2), Size(k, k), CornerRadius(4f), style = Stroke(2.2f))
        val laenge = tafel.width * (0.55f - (i % 2) * 0.12f) * auf
        drawLine(f.textLeise.copy(alpha = 0.7f * auf), Offset(kx + k * 1.6f, y), Offset(kx + k * 1.6f + laenge, y), 4f, StrokeCap.Round)
        val haken = weich(an(t, 11f + i * 2f, 0.5f)) * ausblenden
        if (haken > 0f) {
            val p = Path().apply {
                moveTo(kx + k * 0.15f, y)
                lineTo(kx + k * 0.42f, y + k * 0.3f)
                lineTo(kx + k * (0.42f + 0.7f * haken), y + k * (0.3f - 0.9f * haken))
            }
            drawPath(p, f.erfolg, style = Stroke(3.5f, cap = StrokeCap.Round))
            drawLine(f.textLeise.copy(alpha = 0.5f * haken), Offset(kx + k * 1.6f, y), Offset(kx + k * 1.6f + laenge, y), 2f)
            val funke = an(t, 11f + i * 2f, 0.8f)
            if (funke in 0.01f..0.99f) for (s in 0 until 6) {
                val wkl = s / 6f * 2 * PI.toFloat()
                val r = k * (0.6f + funke * 1.4f)
                drawCircle(f.sekundaer.copy(alpha = 1f - funke), 2.5f, Offset(kx + k / 2 + cos(wkl) * r, y + sin(wkl) * r))
            }
        }
    }
    // Fortschrittsbalken der Tafel
    val fort = (an(t, 11f, 8f) * ausblenden)
    val by = tafel.bottom - tafel.height * 0.08f
    drawLine(f.textSchwach.copy(alpha = 0.3f), Offset(tafel.left + 14f, by), Offset(tafel.right - 14f, by), 6f, StrokeCap.Round)
    if (fort > 0f) drawLine(Brush.horizontalGradient(listOf(f.primaer, f.sekundaer)), Offset(tafel.left + 14f, by), Offset(tafel.left + 14f + (tafel.width - 28f) * fort, by), 6f, StrokeCap.Round)

    // Garten: Pflanze wächst mit jedem Haken
    if (f.design == Design.GARTEN) pflanze(t, f, Offset(w * 0.24f, boden), h, ausblenden)

    // Figur
    val freu = an(t, 18f, 0.4f) * ausblenden
    val sprung = if (t in 18f..22f) abs(sin((t - 18f) * PI.toFloat() * 1.6f)) * h * 0.08f else 0f
    val fx = w * 0.47f
    val armR: Float
    val armL: Float
    when (phase(t)) {
        0 -> { armR = 150f + sin(t * 2f) * 6f; armL = 12f }
        1, 2 -> { armR = 100f + sin(t * 9f) * 8f; armL = 14f }
        else -> { armR = lerp(100f, 165f, freu); armL = lerp(14f, -165f, freu) }
    }
    figur(f, fx, boden - sprung, h, armL, armR, kopfNeigung = if (phase(t) == 0) sin(t * 1.5f) * 8f else 0f)

    // Gedankenblasen mit Glühbirne
    val denken = (an(t, 0.3f, 0.8f) * (1f - an(t, 5f, 0.6f)))
    if (denken > 0f) {
        val kopf = Offset(fx, boden - h * 0.66f)
        listOf(0.25f to 0.03f, 0.5f to 0.045f).forEachIndexed { i, (d, r) ->
            drawCircle(Color.White.copy(alpha = 0.85f * denken), h * r, Offset(kopf.x + w * 0.035f * (i + 1), kopf.y - h * d * 0.5f))
        }
        val blase = Offset(kopf.x + w * 0.12f, kopf.y - h * 0.3f)
        drawOval(Color.White.copy(alpha = 0.92f * denken), Offset(blase.x - h * 0.13f, blase.y - h * 0.1f), Size(h * 0.26f, h * 0.2f))
        val licht = an(t, 3.2f, 0.4f)
        if (licht < 1f) drawIntoText(f, blase, h, denken * (1f - licht))
        if (licht > 0f) birne(f, blase, h * 0.07f, licht * denken, t)
    }

    // Konfetti beim Freuen
    if (t in 18f..23.5f) {
        val k = t - 18f
        for (i in 0 until 34) {
            val wkl = (i * 137.5f) * PI.toFloat() / 180f
            val v = h * (0.35f + (i % 5) * 0.08f)
            val x = fx + cos(wkl) * v * k * 0.55f
            val y = boden - h * 0.7f - sin(wkl).let { abs(it) } * v * k * 0.8f + h * 0.22f * k * k
            val farbe = listOf(f.primaer, f.sekundaer, f.tertiaer, f.erfolg, Color(0xFFFFD166))[i % 5]
            rotate(k * 300f + i * 20f, Offset(x, y)) {
                drawRect(farbe.copy(alpha = (1f - k / 5.5f).coerceIn(0f, 1f)), Offset(x - 4f, y - 2f), Size(8f, 4.5f))
            }
        }
    }
}

private fun DrawScope.drawIntoText(f: Farben, mitte: Offset, h: Float, alpha: Float) {
    // Fragezeichen aus Bogen und Punkt
    val r = h * 0.035f
    drawArc(f.primaer.copy(alpha = alpha), 200f, 250f, false, Offset(mitte.x - r, mitte.y - r * 2.2f), Size(r * 2, r * 2), style = Stroke(h * 0.018f, cap = StrokeCap.Round))
    drawLine(f.primaer.copy(alpha = alpha), Offset(mitte.x + r * 0.2f, mitte.y - r * 0.2f), Offset(mitte.x, mitte.y + r * 0.5f), h * 0.018f, StrokeCap.Round)
    drawCircle(f.primaer.copy(alpha = alpha), h * 0.012f, Offset(mitte.x, mitte.y + r * 1.3f))
}

private fun DrawScope.birne(f: Farben, mitte: Offset, r: Float, a: Float, t: Float) {
    val glut = 0.6f + 0.4f * sin(t * 6f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE066).copy(alpha = 0.7f * a * glut), Color.Transparent), mitte, r * 3f), r * 3f, mitte)
    drawCircle(Color(0xFFFFD43B).copy(alpha = a), r, mitte.copy(y = mitte.y - r * 0.2f))
    drawRect(Color(0xFF9AA0A6).copy(alpha = a), Offset(mitte.x - r * 0.45f, mitte.y + r * 0.7f), Size(r * 0.9f, r * 0.5f))
}

private fun DrawScope.figur(f: Farben, x: Float, fuss: Float, h: Float, armL: Float, armR: Float, kopfNeigung: Float) {
    val haut = if (f.dunkel) Color(0xFFE2BC98) else Color(0xFFEBC6A3)
    val haar = if (f.dunkel) Color(0xFF4E3B2E) else Color(0xFF5B4535)
    val hose = f.text.copy(alpha = 0.75f)
    val huefte = Offset(x, fuss - h * 0.22f)
    val schulter = Offset(x, huefte.y - h * 0.24f)
    // Schatten
    drawOval(Color.Black.copy(alpha = if (f.dunkel) 0.35f else 0.12f), Offset(x - h * 0.1f, fuss - h * 0.015f), Size(h * 0.2f, h * 0.03f))
    // Beine
    drawLine(hose, huefte, Offset(x - h * 0.05f, fuss), h * 0.055f, StrokeCap.Round)
    drawLine(hose, huefte, Offset(x + h * 0.05f, fuss), h * 0.055f, StrokeCap.Round)
    // Rumpf
    drawLine(Brush.verticalGradient(listOf(f.primaer, f.sekundaer), schulter.y, huefte.y), schulter, huefte, h * 0.1f, StrokeCap.Round)
    // Arme
    fun arm(winkel: Float) {
        val rad = winkel * PI.toFloat() / 180f
        val ende = Offset(schulter.x + sin(rad) * h * 0.16f, schulter.y + h * 0.02f + cos(rad) * h * 0.16f)
        drawLine(f.primaer, schulter.copy(y = schulter.y + h * 0.02f), ende, h * 0.04f, StrokeCap.Round)
        drawCircle(haut, h * 0.026f, ende)
    }
    arm(armL)
    arm(armR)
    // Kopf
    val kopf = Offset(schulter.x, schulter.y - h * 0.1f)
    rotate(kopfNeigung, schulter) {
        drawCircle(haut, h * 0.07f, kopf)
        drawArc(haar, 180f, 180f, true, Offset(kopf.x - h * 0.072f, kopf.y - h * 0.075f), Size(h * 0.144f, h * 0.11f))
        drawCircle(f.text.copy(alpha = 0.8f), h * 0.008f, Offset(kopf.x + h * 0.025f, kopf.y + h * 0.005f))
        drawArc(f.text.copy(alpha = 0.6f), 20f, 120f, false, Offset(kopf.x - h * 0.005f, kopf.y + h * 0.01f), Size(h * 0.04f, h * 0.03f), style = Stroke(h * 0.008f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.pflanze(t: Float, f: Farben, boden: Offset, h: Float, a: Float) {
    val haken = (0 until 4).count { t >= 11f + it * 2f + 0.3f }
    val wachsen = weich(an(t, 10.5f, 8f)) * a
    val topf = Rect(boden.x - h * 0.08f, boden.y - h * 0.12f, boden.x + h * 0.08f, boden.y)
    drawRoundRect(Color(0xFFC0714F), topf.topLeft, topf.size, CornerRadius(6f))
    drawRect(Color(0xFFA65B3D), Offset(topf.left - 4f, topf.top), Size(topf.width + 8f, h * 0.03f))
    val stielH = h * (0.08f + 0.32f * wachsen)
    val spitze = Offset(boden.x, topf.top - stielH)
    drawLine(Color(0xFF2E9D6A), Offset(boden.x, topf.top), spitze, 4f, StrokeCap.Round)
    for (i in 0 until (if (a > 0f) haken else 0)) {
        val y = topf.top - stielH * (0.25f + i * 0.2f)
        val seite = if (i % 2 == 0) 1f else -1f
        val blatt = Path().apply {
            moveTo(boden.x, y)
            quadraticTo(boden.x + seite * h * 0.1f, y - h * 0.06f, boden.x + seite * h * 0.15f, y - h * 0.01f)
            quadraticTo(boden.x + seite * h * 0.08f, y + h * 0.03f, boden.x, y)
        }
        drawPath(blatt, Color(0xFF3FBF7F))
    }
    val bluete = an(t, 18.2f, 0.8f) * a
    if (bluete > 0f) for (b in 0 until 6) {
        val wkl = b / 6f * 2 * PI.toFloat()
        drawCircle(f.sekundaer, h * 0.03f * bluete, Offset(spitze.x + cos(wkl) * h * 0.035f * bluete, spitze.y + sin(wkl) * h * 0.035f * bluete))
    }
    if (bluete > 0f) drawCircle(Color(0xFFFFE066), h * 0.022f * bluete, spitze)
}

private fun DrawScope.kulisse(t: Float, f: Farben, r: Rect) {
    val form = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(r, CornerRadius(12f))) }
    clipPath(form) {
        when (f.design) {
            Design.AURORA -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0D0A2E), Color(0xFF2B1D6B)), r.top, r.bottom), r.topLeft, r.size)
                for (i in 0 until 14) drawCircle(Color.White.copy(alpha = 0.3f + 0.5f * ((sin(t * 2 + i) + 1) / 2)), 1.6f, Offset(r.left + r.width * ((i * 0.37f) % 1f), r.top + r.height * ((i * 0.23f) % 0.6f)))
                val staerke = 0.5f + 0.5f * an(t, 18f, 1f) * (1f - an(t, 22f, 1.5f))
                for (band in 0 until 2) {
                    val p = Path()
                    val basis = r.top + r.height * (0.35f + band * 0.14f)
                    p.moveTo(r.left, basis)
                    var x = r.left
                    while (x <= r.right) {
                        p.lineTo(x, basis + sin((x - r.left) / r.width * 6f + t * 0.8f + band) * r.height * 0.08f)
                        x += 4f
                    }
                    drawPath(p, Brush.verticalGradient(listOf(Color(0xFF53F5B5).copy(alpha = 0.7f * staerke), Color(0xFFB77CFF).copy(alpha = 0.4f * staerke)), basis - 20f, basis + 30f), style = Stroke(r.height * 0.1f, cap = StrokeCap.Round))
                }
            }
            Design.GARTEN -> {
                drawRect(Brush.verticalGradient(listOf(if (f.dunkel) Color(0xFF12304A) else Color(0xFF9ED8F5), if (f.dunkel) Color(0xFF1E4D3A) else Color(0xFFE6F6D8)), r.top, r.bottom), r.topLeft, r.size)
                drawCircle(Color(0xFFFFD166), r.height * 0.1f, Offset(r.left + r.width * 0.75f, r.top + r.height * 0.25f))
                val wolkeX = r.left + ((t * 6f) % (r.width + 60f)) - 30f
                drawOval(Color.White.copy(alpha = 0.85f), Offset(wolkeX, r.top + r.height * 0.18f), Size(40f, 16f))
                drawOval(Color(0xFF6CC58E), Offset(r.left - r.width * 0.2f, r.top + r.height * 0.6f), Size(r.width * 0.9f, r.height * 0.8f))
                drawOval(Color(0xFF3FA66C), Offset(r.left + r.width * 0.35f, r.top + r.height * 0.68f), Size(r.width * 0.9f, r.height * 0.8f))
            }
            Design.GLUT -> {
                val sonne = r.top + r.height * (0.25f + 0.45f * weich(an(t, 0f, 17f)) - 0.3f * weich(an(t, 18f, 3f)))
                drawRect(Brush.verticalGradient(listOf(Color(0xFF5B2A86), Color(0xFFF26B38), Color(0xFFFFC857)), r.top, r.bottom), r.topLeft, r.size)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFB347).copy(alpha = 0f)), Offset(r.center.x, sonne), r.height * 0.4f), r.height * 0.4f, Offset(r.center.x, sonne))
                drawCircle(Color(0xFFFFE08A), r.height * 0.13f, Offset(r.center.x, sonne))
                drawRect(Color(0xFF3B1F4F).copy(alpha = 0.85f), Offset(r.left, r.top + r.height * 0.72f), Size(r.width, r.height * 0.3f))
                for (i in 0 until 4) drawLine(Color(0xFFFFC857).copy(alpha = 0.5f), Offset(r.center.x - 20f + i * 4f, r.top + r.height * (0.76f + i * 0.05f)), Offset(r.center.x + 20f - i * 4f, r.top + r.height * (0.76f + i * 0.05f)), 2f)
            }
            Design.KOSMOS -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF020616), Color(0xFF0B1A4A)), r.top, r.bottom), r.topLeft, r.size)
                for (i in 0 until 18) drawCircle(Color.White.copy(alpha = 0.3f + 0.6f * ((sin(t * 3 + i * 1.7f) + 1) / 2)), 1.4f, Offset(r.left + r.width * ((i * 0.41f) % 1f), r.top + r.height * ((i * 0.29f) % 1f)))
                val planet = Offset(r.left + r.width * 0.28f, r.top + r.height * 0.3f)
                drawCircle(Brush.linearGradient(listOf(Color(0xFF62A8FF), Color(0xFF8E5CFF)), planet - Offset(12f, 12f), planet + Offset(12f, 12f)), r.height * 0.1f, planet)
                drawOval(Color(0xFFC6FF4A).copy(alpha = 0.7f), Offset(planet.x - r.height * 0.17f, planet.y - 3f), Size(r.height * 0.34f, 6f), style = Stroke(2f))
                val start = weich(an(t, 18.3f, 3.5f))
                val rakete = Offset(r.left + r.width * 0.7f, r.bottom - r.height * 0.18f - start * r.height * 1.1f)
                if (start > 0f) drawOval(Brush.verticalGradient(listOf(Color(0xFFFFD166), Color(0xFFFF6B35).copy(alpha = 0f)), rakete.y + 10f, rakete.y + 40f), Offset(rakete.x - 6f, rakete.y + 10f), Size(12f, 30f + 10f * sin(t * 30f)))
                val koerper = Path().apply {
                    moveTo(rakete.x, rakete.y - 18f); quadraticTo(rakete.x + 9f, rakete.y - 6f, rakete.x + 7f, rakete.y + 12f)
                    lineTo(rakete.x - 7f, rakete.y + 12f); quadraticTo(rakete.x - 9f, rakete.y - 6f, rakete.x, rakete.y - 18f); close()
                }
                drawPath(koerper, Color(0xFFEAF3FF))
                drawCircle(Color(0xFF2F6BFF), 3.5f, Offset(rakete.x, rakete.y - 4f))
                drawLine(Color(0xFFFF6B7D), Offset(rakete.x - 7f, rakete.y + 12f), Offset(rakete.x - 12f, rakete.y + 16f), 3f)
                drawLine(Color(0xFFFF6B7D), Offset(rakete.x + 7f, rakete.y + 12f), Offset(rakete.x + 12f, rakete.y + 16f), 3f)
            }
        }
    }
    drawRoundRect(Color.White.copy(alpha = if (f.dunkel) 0.25f else 0.9f), r.topLeft, r.size, CornerRadius(12f), style = Stroke(5f))
    drawLine(Color.White.copy(alpha = if (f.dunkel) 0.2f else 0.8f), Offset(r.center.x, r.top), Offset(r.center.x, r.bottom), 3f)
}
