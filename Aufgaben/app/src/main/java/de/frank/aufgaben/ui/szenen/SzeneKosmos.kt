package de.frank.aufgaben.ui.szenen

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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.ui.theme.Farben
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kosmos: Kontrollraum mit Bodenraster in Zentralperspektive und großem Rundfenster. Checkliste am
 * Pult abarbeiten, Countdown, Raketenstart, Jubel. Die Katze dreht sich auf dem Drehstuhl und tatzt nach Lämpchen.
 */
internal fun DrawScope.szeneKosmos(t: Float, f: Farben, schrift: TextMeasurer) {
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.9f
    val s = h * 0.6f
    val blau = f.primaer

    // Raum: Wand mit Paneelen, Boden mit Fluchtpunkt-Raster
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF0B1224), Color(0xFF060A16)) else listOf(Color(0xFFE6EEFB), Color(0xFFD2DDF3)), 0f, boden))
    for (i in 1..9) drawLine(Color.Black.copy(alpha = if (d) 0.25f else 0.06f), Offset(i * w / 10f, 0f), Offset(i * w / 10f, h * 0.62f), 2f)
    drawRect(if (d) Color(0xFF101A33) else Color(0xFFC3D0EA), Offset(0f, h * 0.62f), Size(w, boden - h * 0.62f))
    val flucht = Offset(w * 0.5f, h * 0.3f)
    drawRect(if (d) Color(0xFF0A1122) else Color(0xFFB4C3E2), Offset(0f, boden), Size(w, h - boden))
    for (i in -8..8) drawLine(blau.copy(alpha = 0.22f), Offset(flucht.x + i * w * 0.02f, boden), Offset(flucht.x + i * w * 0.2f, h), 1.5f)
    for (j in 0 until 4) { val y = boden + (h - boden) * (j / 4f) * (j / 4f + 0.3f); drawLine(blau.copy(alpha = 0.2f), Offset(0f, y), Offset(w, y), 1.2f) }
    // Deckenlichter
    for (i in 0 until 4) drawRoundRect(Color.White.copy(alpha = if (d) 0.25f else 0.7f), Offset(w * (0.1f + i * 0.22f), h * 0.02f), Size(w * 0.1f, h * 0.018f), CornerRadius(6f))

    // Rundfenster
    val fm = Offset(w * 0.75f, h * 0.4f)
    val fr = h * 0.34f
    val wackeln = if (t in 14f..17f) sin(t * 60f) * 2f * (1f - an(t, 14f, 3f)) else 0f
    clipPath(Path().apply { addOval(Rect(fm, fr)) }) {
        translate(wackeln, 0f) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF020615), Color(0xFF0B1A4A), Color(0xFF1D2F6E)), fm.y - fr, fm.y + fr), fm - Offset(fr, fr), Size(fr * 2, fr * 2))
            for (i in 0 until 24) drawCircle(Color.White.copy(alpha = 0.35f + 0.6f * ((sin(t * 2.5f + i * 1.9f) + 1) / 2)), 1.4f, Offset(fm.x - fr + fr * 2 * ((i * 0.37f) % 1f), fm.y - fr + fr * 1.4f * ((i * 0.53f) % 1f)))
            // Planet mit Ring
            val planet = fm + Offset(-fr * 0.45f, -fr * 0.45f)
            drawCircle(Brush.linearGradient(listOf(blau.heller(0.3f), blau.dunkler(0.4f)), planet - Offset(fr * 0.15f, fr * 0.15f), planet + Offset(fr * 0.15f, fr * 0.15f)), fr * 0.16f, planet)
            rotate(-18f, planet) { drawOval(blau.heller(0.5f).copy(alpha = 0.7f), planet - Offset(fr * 0.3f, fr * 0.04f), Size(fr * 0.6f, fr * 0.08f), style = Stroke(2.5f)) }
            // Mond wandert
            drawCircle(Color(0xFFD9DEE8), fr * 0.06f, fm + Offset(fr * (0.5f - t * 0.02f), -fr * 0.55f))
            // Startrampe
            val erde = fm.y + fr * 0.62f
            drawRect(Color(0xFF26314F), Offset(fm.x - fr, erde), Size(fr * 2, fr))
            val turm = fm.x + fr * 0.25f
            drawLine(Color(0xFF7A869E), Offset(turm, erde), Offset(turm, erde - fr * 0.55f), 4f)
            for (i in 0 until 5) drawLine(Color(0xFF7A869E), Offset(turm, erde - fr * 0.1f * i), Offset(turm - fr * 0.08f, erde - fr * 0.1f * (i + 1)), 1.5f)
            // Rakete
            val start = weich(an(t, 14f, 4.5f))
            val beschleunigt = start * start
            val rakete = Offset(fm.x + fr * 0.08f, erde - fr * 0.2f - beschleunigt * fr * 2.2f)
            // Rauchwolken
            if (t > 13.6f) for (i in 0 until 8) {
                val q = an(t, 13.6f + i * 0.15f, 3f)
                val rw = fr * (0.08f + 0.2f * q)
                drawCircle(Color(0xFFE6E9F0).copy(alpha = (0.8f - q * 0.6f) * (1f - an(t, 21f, 2f))), rw, Offset(fm.x + fr * 0.08f + (i - 4) * fr * 0.08f * (0.3f + q), erde - rw * 0.3f))
            }
            if (t > 13.8f && rakete.y > fm.y - fr * 1.3f) {
                val flamme = fr * (0.2f + 0.08f * sin(t * 40f))
                drawOval(Brush.verticalGradient(listOf(Color(0xFFFFF1B0), Color(0xFFFF9A3D), Color.Transparent), rakete.y + fr * 0.12f, rakete.y + fr * 0.12f + flamme), Offset(rakete.x - fr * 0.045f, rakete.y + fr * 0.12f), Size(fr * 0.09f, flamme))
            }
            val koerper = Path().apply {
                moveTo(rakete.x, rakete.y - fr * 0.2f)
                cubicTo(rakete.x + fr * 0.07f, rakete.y - fr * 0.12f, rakete.x + fr * 0.06f, rakete.y + fr * 0.05f, rakete.x + fr * 0.05f, rakete.y + fr * 0.13f)
                lineTo(rakete.x - fr * 0.05f, rakete.y + fr * 0.13f)
                cubicTo(rakete.x - fr * 0.06f, rakete.y + fr * 0.05f, rakete.x - fr * 0.07f, rakete.y - fr * 0.12f, rakete.x, rakete.y - fr * 0.2f); close()
            }
            drawPath(koerper, Color(0xFFF1F4FA))
            drawPath(Path().apply { moveTo(rakete.x - fr * 0.05f, rakete.y + fr * 0.02f); lineTo(rakete.x - fr * 0.1f, rakete.y + fr * 0.15f); lineTo(rakete.x - fr * 0.05f, rakete.y + fr * 0.13f); close() }, blau)
            drawPath(Path().apply { moveTo(rakete.x + fr * 0.05f, rakete.y + fr * 0.02f); lineTo(rakete.x + fr * 0.1f, rakete.y + fr * 0.15f); lineTo(rakete.x + fr * 0.05f, rakete.y + fr * 0.13f); close() }, blau)
            drawCircle(blau.heller(0.2f), fr * 0.03f, rakete - Offset(0f, fr * 0.06f))
            drawCircle(Color.White, fr * 0.03f, rakete - Offset(0f, fr * 0.06f), style = Stroke(1.5f))
        }
    }
    drawCircle(if (d) Color(0xFF2A3A63) else Color(0xFF8FA3CC), fr, fm, style = Stroke(h * 0.035f))
    for (i in 0 until 12) { val wk = i * 30f; drawCircle(if (d) Color(0xFF4A5A85) else Color(0xFFE6EEFB), h * 0.006f, fm + Offset(cos(rad(wk)) * fr, sin(rad(wk)) * fr)) }

    // Pult mit Bildschirmen
    val pult = Rect(w * 0.03f, h * 0.6f, w * 0.4f, h * 0.66f)
    drawPath(Path().apply { moveTo(pult.left, boden); lineTo(pult.left, pult.bottom); lineTo(pult.left + w * 0.02f, pult.top); lineTo(pult.right, pult.top); lineTo(pult.right, boden); close() }, if (d) Color(0xFF1A2440) else Color(0xFF8C9CBF))
    drawRect(if (d) Color(0xFF26335A) else Color(0xFFA9B7D6), pult.topLeft, Size(pult.width, h * 0.02f))
    for (i in 0 until 9) {
        val an = sin(t * (2f + i * 0.7f) + i) > 0.2f
        val farbe = when (i % 3) { 0 -> blau; 1 -> blau.heller(0.25f); else -> Color(0xFFFFB74D) }
        drawCircle(if (an) farbe else farbe.copy(alpha = 0.25f), h * 0.011f, Offset(pult.left + w * 0.035f + i * w * 0.035f, pult.top + h * 0.045f))
    }
    val mon1 = Rect(w * 0.05f, h * 0.2f, w * 0.2f, h * 0.52f)
    val mon2 = Rect(w * 0.22f, h * 0.2f, w * 0.38f, h * 0.52f)
    for (mon in listOf(mon1, mon2)) {
        drawRect(Color(0xFF2A3350), Offset(mon.center.x - 3f, mon.bottom), Size(6f, pult.top - mon.bottom))
        drawRoundRect(Color(0xFF0E1428), mon.topLeft, mon.size, CornerRadius(6f))
        drawRoundRect(blau.copy(alpha = 0.3f), mon.topLeft, mon.size, CornerRadius(6f), style = Stroke(2f))
    }
    // Monitor 1: Checkliste
    val punkteZeit = floatArrayOf(3.2f, 5.2f, 7.2f, 9.2f)
    for (i in 0 until 4) {
        val y = mon1.top + mon1.height * (0.2f + i * 0.2f)
        val ok = an(t, punkteZeit[i], 0.3f)
        drawCircle(lerp(Color(0xFF55607A), blau.heller(0.2f), ok), h * 0.014f, Offset(mon1.left + mon1.width * 0.14f, y))
        drawLine(lerp(Color(0xFF55607A), blau.heller(0.4f), ok), Offset(mon1.left + mon1.width * 0.28f, y), Offset(mon1.left + mon1.width * (0.8f - (i % 2) * 0.15f), y), 3f, StrokeCap.Round)
    }
    // Monitor 2: Kurve, dann Countdown, dann Start
    when {
        t < 10f -> {
            val p = Path()
            for (x in 0..40) {
                val xx = mon2.left + mon2.width * (0.08f + 0.84f * x / 40f)
                val yy = mon2.center.y + sin(x * 0.5f + t * 3f) * mon2.height * 0.18f * (0.5f + 0.5f * sin(x * 0.13f))
                if (x == 0) p.moveTo(xx, yy) else p.lineTo(xx, yy)
            }
            drawPath(p, blau.heller(0.3f), style = Stroke(2.5f))
        }
        t < 14f -> {
            val zahl = (14f - t).toInt().coerceIn(1, 3).toString()
            val txt = schrift.measure(zahl, TextStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold))
            drawText(txt, topLeft = mon2.center - Offset(txt.size.width / 2f, txt.size.height / 2f))
        }
        t < 20f -> {
            val txt = schrift.measure("START", TextStyle(color = f.sekundaer, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
            drawText(txt, topLeft = mon2.center - Offset(txt.size.width / 2f, txt.size.height / 2f))
        }
        else -> haken(mon2.center, mon2.height * 0.35f, 1f, f.sekundaer, 5f)
    }

    // Drehstuhl mit Katze
    val stuhlX = w * 0.3f
    val drehung = sin(t * 2.2f)
    val stuhlF = if (d) Color(0xFF3A4668) else Color(0xFF5A6788)
    drawLine(stuhlF, Offset(stuhlX, boden - h * 0.03f), Offset(stuhlX, boden - h * 0.2f), 5f)
    drawLine(stuhlF, Offset(stuhlX - h * 0.1f, boden - h * 0.02f), Offset(stuhlX + h * 0.1f, boden - h * 0.02f), 5f, StrokeCap.Round)
    drawRoundRect(stuhlF, Offset(stuhlX - h * 0.1f * kotlin.math.abs(drehung).coerceAtLeast(0.4f), boden - h * 0.24f), Size(h * 0.2f * kotlin.math.abs(drehung).coerceAtLeast(0.4f), h * 0.04f), CornerRadius(8f))
    drawRoundRect(stuhlF.dunkler(0.1f), Offset(stuhlX - h * 0.02f - drehung * h * 0.08f, boden - h * 0.45f), Size(h * 0.04f, h * 0.22f), CornerRadius(8f))
    val katzeF = KatzenFarben(Color(0xFFB9BFCC), Color(0xFF7E8596), Color(0xFFF2F4F8), Color(0xFF7FD3F0))
    val cs = h * 0.16f
    val sitzY = boden - h * 0.24f
    when {
        t < 10f -> katze(stuhlX, sitzY, cs, if (drehung > 0) 1f else -1f, 1, 0f, t, katzeF)
        t < 10.8f -> { val q = an(t, 10f, 0.8f); katze(mix(stuhlX, w * 0.2f, q), mix(sitzY, pult.top, q), cs, -1f, 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.15f) }
        t < 14f -> katze(w * 0.2f, pult.top, cs, -1f, 1, 0f, t, katzeF, pfote = maxOf(0f, sin((t - 10.8f) * 6f)))
        else -> katze(w * 0.2f, pult.top, cs, 1f, 1, 0f, t, katzeF)
    }
    // Helm-Andeutung: Katze trägt eine kleine Antenne
    // Person
    val mf = MenschFarben(Color(0xFFD9B08C), Color(0xFF1E2230), lerp(f.primaer, Color(0xFFDDE3EE), 0.45f), Color(0xFF2A3350), Color(0xFF1A1F2E))
    val blink = blinzelt(t)
    val pultX = w * 0.44f
    val pose: Pose = when {
        t < 2f -> gehen(mix(w * 1.02f, pultX, an(t, 0f, 2f)), boden, -1f, t * 8.5f, blink)
        t < 10f -> {
            val druecken = ((t - 2f) % 2f) < 0.35f
            val links = ((t - 2f) / 2f).toInt() % 2 == 0
            Pose(pultX, boden, -1f, lean = 10f, sR = if (!links && druecken) 88f else 55f, eR = if (!links && druecken) 5f else 40f, sL = if (links && druecken) 88f else 50f, eL = if (links && druecken) 5f else 40f, kopf = 8f, blinzeln = blink)
        }
        t < 14f -> Pose(pultX, boden, 1f, lean = 6f, sR = 20f, eR = 100f, sL = 10f, eL = 20f, kopf = -4f, blinzeln = blink, lachen = 0.2f)
        t < 20f -> { val hub = maxOf(0f, sin((t - 14f) * 7f)) * 0.07f * s; Pose(pultX, boden - hub, 1f, sL = 170f, eL = 0f, sR = 160f, eR = 10f, lachen = 1f, blinzeln = blink) }
        else -> Pose(pultX, boden, 1f, sR = 120f, eR = 60f, sL = 8f, eL = 10f, lachen = 0.8f, blinzeln = blink)
    }
    mensch(pose, s, mf)
    // Headset
    // Sternenfunken beim Start
    if (t in 14.5f..20f) for (i in 0 until 12) {
        val q = ((t - 14.5f) * 0.6f + i / 12f) % 1f
        val wk = i * 30f
        drawCircle(f.sekundaer.copy(alpha = 1f - q), 2.5f, Offset(pultX + cos(rad(wk)) * q * h * 0.4f, boden - s + sin(rad(wk)) * q * h * 0.3f))
    }
}
