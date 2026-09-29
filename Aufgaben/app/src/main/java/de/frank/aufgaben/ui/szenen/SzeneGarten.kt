package de.frank.aufgaben.ui.szenen

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import de.frank.aufgaben.ui.theme.Farben
import kotlin.math.cos
import kotlin.math.sin

/**
 * Garten: weite Landschaft mit Tiefe (Hügel, Weg zum Horizont, Zaun). Drei Beete sind drei Aufgaben:
 * gießen, wachsen lassen, ernten. Schmetterlinge fliegen, die Katze jagt sie, ein Vogelschwarm zieht vorbei.
 */
internal fun DrawScope.szeneGarten(t: Float, f: Farben) {
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.92f
    val s = h * 0.56f
    val horizont = h * 0.5f

    // Himmel, Sonne im Bogen, Wolken
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF0C1A22), Color(0xFF1B3328)) else listOf(Color(0xFFCFE8F2), Color(0xFFF2F4E4)), 0f, horizont + h * 0.1f))
    val sonne = Offset(w * (0.15f + 0.7f * (t / 26f)), horizont - sin(t / 26f * 3.14f) * h * 0.38f)
    val sonnenFarbe = if (d) Color(0xFFEFE6C8) else Color(0xFFFFD27A)
    drawCircle(Brush.radialGradient(listOf(sonnenFarbe.copy(alpha = 0.55f), Color.Transparent), sonne, h * 0.2f), h * 0.2f, sonne)
    drawCircle(sonnenFarbe, h * 0.05f, sonne)
    if (d) drawCircle(Color(0xFF1B3328), h * 0.045f, sonne + Offset(h * 0.02f, -h * 0.01f))
    for (i in 0 until 3) {
        val wx = ((t * (6f + i * 3f) + i * w * 0.4f) % (w * 1.3f)) - w * 0.15f
        val wy = h * (0.1f + i * 0.08f)
        val wf = Color.White.copy(alpha = if (d) 0.15f else 0.85f)
        drawOval(wf, Offset(wx, wy), Size(h * 0.2f, h * 0.06f))
        drawOval(wf, Offset(wx + h * 0.05f, wy - h * 0.035f), Size(h * 0.12f, h * 0.07f))
    }
    // Vögel
    val vogelX = ((t * 22f) % (w * 1.6f)) - w * 0.3f
    for (i in 0 until 3) {
        val v = Offset(vogelX - i * h * 0.07f, h * (0.15f + i * 0.03f))
        val flug = sin(t * 9f + i) * h * 0.012f
        val p = Path().apply { moveTo(v.x - h * 0.025f, v.y - flug); quadraticTo(v.x - h * 0.01f, v.y - h * 0.01f, v.x, v.y); quadraticTo(v.x + h * 0.01f, v.y - h * 0.01f, v.x + h * 0.025f, v.y - flug) }
        drawPath(p, f.text.copy(alpha = 0.55f), style = Stroke(1.8f, cap = StrokeCap.Round))
    }
    // Hügel in Luftperspektive: hinten heller
    val gruen = f.primaer
    val ferne = lerp(gruen, if (d) Color(0xFF1B3328) else Color(0xFFE6F0E0), 0.65f)
    val mitte = lerp(gruen, if (d) Color(0xFF1B3328) else Color(0xFFE6F0E0), 0.4f)
    drawPath(Path().apply { moveTo(0f, horizont); cubicTo(w * 0.2f, horizont - h * 0.2f, w * 0.45f, horizont - h * 0.05f, w * 0.65f, horizont - h * 0.14f); cubicTo(w * 0.8f, horizont - h * 0.22f, w * 0.95f, horizont - h * 0.08f, w, horizont - h * 0.1f); lineTo(w, h); lineTo(0f, h); close() }, ferne)
    // Bäumchen am Horizont
    for (i in 0 until 6) {
        val bx = w * (0.08f + i * 0.17f)
        val by = horizont - h * 0.06f - sin(i * 1.7f) * h * 0.04f
        drawLine(ferne.dunkler(0.3f), Offset(bx, by), Offset(bx, by + h * 0.03f), 2f)
        drawCircle(ferne.dunkler(0.2f), h * 0.022f, Offset(bx, by))
    }
    drawPath(Path().apply { moveTo(0f, horizont + h * 0.06f); cubicTo(w * 0.3f, horizont - h * 0.04f, w * 0.6f, horizont + h * 0.1f, w, horizont + h * 0.02f); lineTo(w, h); lineTo(0f, h); close() }, mitte)
    // Wiese vorne
    drawPath(Path().apply { moveTo(0f, h * 0.66f); cubicTo(w * 0.35f, h * 0.62f, w * 0.7f, h * 0.7f, w, h * 0.64f); lineTo(w, h); lineTo(0f, h); close() }, Brush.verticalGradient(listOf(lerp(gruen, Color.White, if (d) 0.05f else 0.35f), lerp(gruen, Color.Black, if (d) 0.45f else 0.1f)), h * 0.62f, h))
    // Weg zum Horizont (Fluchtpunkt)
    drawPath(Path().apply { moveTo(w * 0.47f, horizont + h * 0.05f); lineTo(w * 0.49f, horizont + h * 0.05f); cubicTo(w * 0.5f, h * 0.7f, w * 0.62f, h * 0.8f, w * 0.7f, h); lineTo(w * 0.5f, h); cubicTo(w * 0.47f, h * 0.8f, w * 0.46f, h * 0.7f, w * 0.47f, horizont + h * 0.05f); close() }, if (d) Color(0xFF3A3326) else Color(0xFFE8D9B5))
    // Zaun mit perspektivisch kleiner werdenden Pfosten
    for (i in 0 until 12) {
        val fx = w * (0.52f + i * 0.045f)
        val gross = 1f - i * 0.05f
        val fy = h * 0.66f - i * h * 0.004f
        drawLine(if (d) Color(0xFF6B5A44) else Color(0xFFA88A62), Offset(fx, fy), Offset(fx, fy - h * 0.09f * gross), 3f * gross)
    }
    drawLine(if (d) Color(0xFF6B5A44) else Color(0xFFA88A62), Offset(w * 0.52f, h * 0.62f), Offset(w, h * 0.585f), 2.5f)
    // Baum links mit wehender Krone
    val stamm = Offset(w * 0.1f, boden - h * 0.05f)
    drawLine(Color(0xFF7A5A3E), stamm, stamm - Offset(0f, h * 0.4f), h * 0.04f, StrokeCap.Round)
    rotate(sin(t * 0.9f) * 2f, stamm) {
        for ((dx, dy, r) in listOf(Triple(-0.07f, -0.5f, 0.12f), Triple(0.06f, -0.52f, 0.13f), Triple(0f, -0.62f, 0.14f), Triple(-0.02f, -0.44f, 0.1f))) {
            drawCircle(lerp(gruen, Color.Black, if (d) 0.35f else 0.15f), h * r, stamm + Offset(h * dx, h * dy))
            drawCircle(lerp(gruen, Color.White, 0.2f).copy(alpha = 0.35f), h * r * 0.5f, stamm + Offset(h * dx - h * r * 0.3f, h * dy - h * r * 0.3f))
        }
    }

    // Drei Beete mit Pflanzen
    val beete = listOf(w * 0.34f, w * 0.56f, w * 0.78f)
    val giessStart = listOf(2.6f, 8f, 13.5f)
    beete.forEachIndexed { i, bx ->
        drawOval(Color(0xFF5C3F2B), Offset(bx - h * 0.12f, boden - h * 0.045f), Size(h * 0.24f, h * 0.07f))
        drawOval(Color(0xFF7A5639), Offset(bx - h * 0.1f, boden - h * 0.04f), Size(h * 0.2f, h * 0.04f))
        val wachsen = weich(an(t, giessStart[i] + 1f, 3f)) * (1f - an(t, 24.8f, 1.2f))
        val bluete = weich(an(t, giessStart[i] + 3.4f, 1f)) * (1f - an(t, 24.8f, 1.2f))
        if (wachsen > 0f) for (j in -1..1) {
            val px = bx + j * h * 0.06f
            val hoehe = h * (0.12f + 0.06f * (j + 1) % 2) * wachsen
            val wind = sin(t * 1.6f + j + i) * 3f
            drawLine(Color(0xFF3D8A55), Offset(px, boden - h * 0.03f), Offset(px + wind, boden - h * 0.03f - hoehe), 2.5f, StrokeCap.Round)
            if (wachsen > 0.4f) for (b in 0..1) {
                val by = boden - h * 0.03f - hoehe * (0.35f + b * 0.3f)
                val seite = if ((b + j) % 2 == 0) 1f else -1f
                drawOval(Color(0xFF4FA768), Offset(px + wind * 0.5f + (if (seite > 0) 0f else -h * 0.035f), by - h * 0.01f), Size(h * 0.035f, h * 0.018f))
            }
            if (bluete > 0f) {
                val kopf = Offset(px + wind, boden - h * 0.03f - hoehe)
                val bf = if (j == 0) Color(0xFFFFF4E0) else lerp(gruen, Color.White, 0.75f)
                for (bl in 0 until 5) {
                    val wk = bl * 72f + t * 10f
                    drawCircle(bf, h * 0.016f * bluete, kopf + Offset(cos(rad(wk)) * h * 0.016f * bluete, sin(rad(wk)) * h * 0.016f * bluete))
                }
                drawCircle(Color(0xFFF2C14E), h * 0.01f * bluete, kopf)
            }
        }
        // Schildchen mit Haken nach der Ernte
        val schild = an(t, 18.2f + i * 0.5f, 0.4f) * (1f - an(t, 24.8f, 1.2f))
        if (schild > 0f) {
            val sp = Offset(bx + h * 0.1f, boden - h * 0.02f)
            drawLine(Color(0xFF8C6A48), sp, sp - Offset(0f, h * 0.12f * schild), 3f)
            drawRoundRect(Color(0xFFF3E6CF), sp - Offset(h * 0.035f, h * 0.17f * schild), Size(h * 0.07f, h * 0.05f), CornerRadius(4f))
            haken(sp - Offset(0f, h * 0.145f * schild), h * 0.035f, an(t, 18.5f + i * 0.5f, 0.3f), f.primaer, 2.5f)
        }
    }

    // Person mit Gießkanne
    val mf = MenschFarben(Color(0xFFE6BE98), Color(0xFF6B4A2E), lerp(f.primaer, Color(0xFFD9D2C0), 0.35f), Color(0xFF5A5344), Color(0xFF3A3024))
    val blink = blinzelt(t)
    val stehX = beete.map { it - h * 0.24f }
    val giessDauer = 3.2f
    val pose: Pose
    var giesst = false
    var kanneAbgestellt = false
    when {
        t < giessStart[0] -> pose = gehen(mix(-0.1f * w, stehX[0], an(t, 0f, giessStart[0])), boden, 1f, t * 8.5f, blink).copy(sR = 20f, eR = 10f)
        t < giessStart[0] + giessDauer -> { pose = Pose(stehX[0], boden, 1f, lean = 8f, sR = 70f, eR = 10f, blinzeln = blink); giesst = true }
        t < giessStart[1] -> pose = gehen(mix(stehX[0], stehX[1], an(t, giessStart[0] + giessDauer, giessStart[1] - giessStart[0] - giessDauer)), boden, 1f, t * 8.5f, blink).copy(sR = 20f, eR = 10f)
        t < giessStart[1] + giessDauer -> { pose = Pose(stehX[1], boden, 1f, lean = 8f, sR = 70f, eR = 10f, blinzeln = blink); giesst = true }
        t < giessStart[2] -> pose = gehen(mix(stehX[1], stehX[2], an(t, giessStart[1] + giessDauer, giessStart[2] - giessStart[1] - giessDauer)), boden, 1f, t * 8.5f, blink).copy(sR = 20f, eR = 10f)
        t < giessStart[2] + giessDauer -> { pose = Pose(stehX[2], boden, 1f, lean = 8f, sR = 70f, eR = 10f, blinzeln = blink); giesst = true }
        t < 18f -> { pose = Pose(stehX[2], boden, 1f, sR = mix(70f, 150f, an(t, 17f, 0.8f)), eR = 60f, lean = 4f, blinzeln = blink); kanneAbgestellt = true }
        t < 21.5f -> { val hub = maxOf(0f, sin((t - 18f) * 7f)) * 0.07f * s; pose = Pose(stehX[2], boden - hub, -1f, sL = 170f, eL = 0f, sR = 165f, eR = 5f, lachen = 1f, blinzeln = blink); kanneAbgestellt = true }
        else -> { pose = Pose(stehX[2], boden, -1f, sR = 150f + sin(t * 8f) * 15f, eR = 30f, lachen = 0.8f, blinzeln = blink); kanneAbgestellt = true }
    }
    val punkte = mensch(pose, s, mf)
    // Gießkanne in der Hand oder abgestellt
    val kanne = if (kanneAbgestellt) Offset(stehX[2] + h * 0.1f, boden - h * 0.04f) else punkte.handR + Offset(0f, h * 0.05f)
    val neigung = if (giesst) 32f else 0f
    rotate(neigung, kanne) {
        drawRoundRect(lerp(f.primaer, Color(0xFF9FB3A8), 0.4f), kanne - Offset(h * 0.04f, h * 0.035f), Size(h * 0.08f, h * 0.07f), CornerRadius(8f))
        drawLine(lerp(f.primaer, Color(0xFF9FB3A8), 0.4f), kanne + Offset(h * 0.035f, 0f), kanne + Offset(h * 0.09f, -h * 0.045f), 4f, StrokeCap.Round)
        drawArc(Color(0xFF6D7F75), 180f, 180f, false, kanne - Offset(h * 0.03f, h * 0.06f), Size(h * 0.06f, h * 0.05f), style = Stroke(3f))
    }
    if (giesst) {
        val tuelle = kanne + Offset(cos(rad(neigung)) * h * 0.09f + sin(rad(neigung)) * h * 0.045f, sin(rad(neigung)) * h * 0.09f - cos(rad(neigung)) * h * 0.045f)
        for (i in 0 until 9) {
            val q = ((t * 2.2f + i / 9f) % 1f)
            drawCircle(Color(0xFF7FC4E8).copy(alpha = 1f - q * 0.6f), 2.2f, tuelle + Offset(q * h * 0.08f + (i % 3) * 2f, q * q * (boden - tuelle.y)))
        }
    }

    // Schmetterlinge
    val falter = List(2) { i -> Offset(w * (0.45f + 0.3f * sin(t * 0.37f + i * 2f)), h * (0.48f + 0.15f * sin(t * 0.83f + i * 1.3f))) }
    falter.forEachIndexed { i, p ->
        val schlag = kotlin.math.abs(sin(t * 12f + i)) * 0.8f + 0.2f
        val farbe = if (i == 0) lerp(f.primaer, Color.White, 0.5f) else Color(0xFFF2C14E)
        drawOval(farbe, Offset(p.x - h * 0.03f * schlag, p.y - h * 0.02f), Size(h * 0.03f * schlag, h * 0.03f))
        drawOval(farbe.dunkler(0.1f), Offset(p.x, p.y - h * 0.02f), Size(h * 0.03f * schlag, h * 0.03f))
        drawLine(Color(0xFF3A3024), p - Offset(0f, h * 0.018f), p + Offset(0f, h * 0.012f), 2f)
    }
    // Katze jagt den ersten Falter
    val katzeF = KatzenFarben(Color(0xFF9C8B7A), Color(0xFF5E5146), Color(0xFFEDE3D6))
    val cs = h * 0.17f
    when {
        t < 4f -> katze(w * 0.18f, boden, cs, 1f, 1, 0f, t, katzeF, pfote = maxOf(0f, sin(t * 3f)) * 0.8f)
        t < 15f -> {
            val ziel = falter[0].x.coerceIn(w * 0.18f, w * 0.9f)
            val x = mix(w * 0.18f, ziel, an(t, 4f, 1.5f))
            val sprung = listOf(7f, 10.2f, 13f).firstOrNull { t in it..it + 0.7f }
            if (sprung != null) katze(x, boden, cs, if (ziel > w * 0.5f) 1f else -1f, 3, 0f, t, katzeF, hoehe = sin(an(t, sprung, 0.7f) * 3.14f) * h * 0.22f)
            else katze(x, boden, cs, if (sin(t * 0.37f) > 0f) 1f else -1f, 0, t * 12f, t, katzeF)
        }
        t < 17f -> katze(mix(w * 0.6f, stehX[2] + h * 0.25f, an(t, 15f, 2f)), boden, cs, 1f, 0, t * 10f, t, katzeF)
        else -> katze(stehX[2] + h * 0.25f, boden, cs, -1f, 1, 0f, t, katzeF)
    }
    // Gras im Vordergrund
    for (i in 0 until 40) {
        val gx = i * w / 40f + (i % 3) * 4f
        val wind = sin(t * 1.8f + i * 0.4f) * 4f
        drawLine(lerp(gruen, Color.Black, if (d) 0.3f else 0.05f), Offset(gx, h), Offset(gx + wind, h - h * (0.04f + (i % 4) * 0.012f)), 2f, StrokeCap.Round)
    }
}
