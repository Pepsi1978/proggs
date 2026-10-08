package de.frank.aufgaben.ui.szenen

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.ui.theme.Farben
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Länge eines Durchlaufs; Ende und Anfang sind dasselbe Bild (dunkler Raum, Rakete auf der Rampe), ohne Schnitt. */
internal const val KOSMOS_ZYKLUS = 44f

/**
 * Kosmos: Kontrollraum mit Bodenraster in Zentralperspektive, Pult als Block und schräg gestellten Bildschirmen, großes
 * Rundfenster mit tiefer Laibung. Checkliste am Pult abarbeiten, zum Fenster drehen, Countdown, Raketenstart, Jubel,
 * Winken. Im Dunkeln kommen Person und Katze links herein, das Licht geht an; die Katze sitzt auf dem Drehstuhl, springt
 * aufs Pult, tatzt nach Lämpchen und schaut zur Rakete. Am Ende gehen beide links hinaus, das Licht geht aus, und die
 * nächste Rakete fährt auf die Rampe hoch – dann beginnt alles von vorn.
 */
internal fun DrawScope.szeneKosmos(t: Float, f: Farben, schrift: TextMeasurer) {
    val zyklus = KOSMOS_ZYKLUS
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.9f
    val s = h * 0.6f
    val blau = f.primaer
    val flucht = Offset(w * 0.5f, h * 0.3f)
    val startZeit = 21f
    // Tageslicht draußen vor dem Bullauge (1 = Tag, 0 = Nacht)
    val tag = weich(an(t, 2.5f, 3f)) * (1f - weich(an(t, 35.5f, 3f)))
    // Raumlicht: an beim Hereinkommen, aus beim Hinausgehen
    val licht = weich(an(t, 2.5f, 1.5f)) * (1f - weich(an(t, 37.6f, 1.6f)))
    val schirm = weich(an(t, 3.6f, 0.8f)) * (1f - weich(an(t, 36.8f, 0.8f)))

    // Raum: Wand mit Paneelen, Boden mit Fluchtpunkt-Raster
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF0B1224), Color(0xFF060A16)) else listOf(Color(0xFFE6EEFB), Color(0xFFD2DDF3)), 0f, boden))
    for (i in 1..9) drawLine(Color.Black.copy(alpha = if (d) 0.25f else 0.06f), Offset(i * w / 10f, 0f), Offset(i * w / 10f, h * 0.62f), 2f)
    drawRect(if (d) Color(0xFF101A33) else Color(0xFFC3D0EA), Offset(0f, h * 0.62f), Size(w, boden - h * 0.62f))
    drawRect(if (d) Color(0xFF0A1122) else Color(0xFFB4C3E2), Offset(0f, boden), Size(w, h - boden))
    for (i in -8..8) drawLine(blau.copy(alpha = 0.22f), Offset(flucht.x + i * w * 0.02f, boden), Offset(flucht.x + i * w * 0.2f, h), 1.5f)
    for (j in 0 until 4) { val y = boden + (h - boden) * (j / 4f) * (j / 4f + 0.3f); drawLine(blau.copy(alpha = 0.2f), Offset(0f, y), Offset(w, y), 1.2f) }
    // Deckenlichter als flache Leuchtfelder in Untersicht
    for (i in 0 until 4) {
        val x0 = w * (0.1f + i * 0.22f)
        val l = Offset(x0, h * 0.02f); val r = Offset(x0 + w * 0.1f, h * 0.02f)
        flaeche(listOf(l, r, r + (flucht - r) * 0.06f, l + (flucht - l) * 0.06f), Color.White.copy(alpha = if (d) 0.25f else 0.7f))
    }

    // Rundfenster mit tiefer Laibung und leichtem Blickversatz (Parallaxe) beim Raketenstart
    val fm = Offset(w * 0.79f, h * 0.4f)
    val fr = h * 0.34f
    val wackeln = if (t in startZeit..startZeit + 3f) sin(t * 60f) * 2f * (1f - an(t, startZeit, 3f)) else 0f
    val laibung = (flucht - fm) * 0.06f
    clipPath(Path().apply { addOval(Rect(fm, fr)) }) {
        translate(wackeln, 0f) {
            // Himmel über der Startrampe: tagsüber blau, nachts tiefdunkel mit Sternen
            drawRect(Brush.verticalGradient(listOf(lerp(Color(0xFF020615), Color(0xFF2F63B8), tag), lerp(Color(0xFF0B1A4A), Color(0xFF5D8FD8), tag), lerp(Color(0xFF1D2F6E), Color(0xFFA6C8EE), tag)), fm.y - fr, fm.y + fr), fm - Offset(fr, fr), Size(fr * 2, fr * 2))
            for (i in 0 until 24) drawCircle(Color.White.copy(alpha = (0.35f + 0.6f * ((welle(t, zyklus, 18, i * 1.9f) + 1) / 2)) * (1f - 0.92f * tag)), 1.4f, Offset(fm.x - fr + fr * 2 * ((i * 0.37f) % 1f), fm.y - fr + fr * 1.4f * ((i * 0.53f) % 1f)))
            // Sonne am Tag, Mond in der Nacht: beide gehen links auf und rechts unter
            val sonnenLauf = an(t, 2.5f, 35f)
            val sonnenSicht = (sin(sonnenLauf * 3.14f) * 3f).coerceIn(0f, 1f)
            if (sonnenSicht > 0f) {
                val sonne = Offset(fm.x - fr * 0.85f + fr * 1.7f * sonnenLauf, fm.y + fr * 0.66f - sin(sonnenLauf * 3.14f) * fr * 1.05f)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE7A8).copy(alpha = 0.7f * sonnenSicht), Color.Transparent), sonne, fr * 0.5f), fr * 0.5f, sonne)
                drawCircle(Color(0xFFFFD36B).copy(alpha = sonnenSicht), fr * 0.1f, sonne)
            }
            val mondLauf = ((t - 36f + zyklus) % zyklus) / 12f
            if (mondLauf < 1f) {
                val mondSicht = (sin(mondLauf * 3.14f) * 3f).coerceIn(0f, 1f) * (1f - tag)
                val mond = Offset(fm.x - fr * 0.85f + fr * 1.7f * mondLauf, fm.y + fr * 0.66f - sin(mondLauf * 3.14f) * fr * 1.0f)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFEFF3FF).copy(alpha = 0.35f * mondSicht), Color.Transparent), mond, fr * 0.35f), fr * 0.35f, mond)
                mondsichel(mond, fr * 0.09f, Color(0xFFE6EAF2).copy(alpha = mondSicht))
            }
            // Planet mit Ring, schattiert
            val planet = fm + Offset(-fr * 0.45f, -fr * 0.45f)
            drawCircle(Brush.linearGradient(listOf(blau.heller(0.3f), blau.dunkler(0.4f)), planet - Offset(fr * 0.15f, fr * 0.15f), planet + Offset(fr * 0.15f, fr * 0.15f)), fr * 0.16f, planet)
            rotate(-18f, planet) { drawOval(blau.heller(0.5f).copy(alpha = 0.7f), planet - Offset(fr * 0.3f, fr * 0.04f), Size(fr * 0.6f, fr * 0.08f), style = Stroke(2.5f)) }
            // Startrampe
            val erde = fm.y + fr * 0.62f
            drawRect(Color(0xFF26314F), Offset(fm.x - fr, erde), Size(fr * 2, fr))
            val turm = fm.x + fr * 0.25f
            drawLine(Color(0xFF7A869E), Offset(turm, erde), Offset(turm, erde - fr * 0.55f), 4f)
            for (i in 0 until 5) drawLine(Color(0xFF7A869E), Offset(turm, erde - fr * 0.1f * i), Offset(turm - fr * 0.08f, erde - fr * 0.1f * (i + 1)), 1.5f)
            // Rakete: hebt langsam ab und wird schneller
            val start = weich(an(t, startZeit, 5.5f))
            val beschleunigt = start * start
            // Nach dem Start: im Dunkeln fährt die nächste Rakete von unten auf die Rampe
            val nachschub = 1f - weich(an(t, 37.5f, 5f))
            val rakete = if (t < 34f) Offset(fm.x + fr * 0.08f, erde - fr * 0.2f - beschleunigt * fr * 2.2f)
            else Offset(fm.x + fr * 0.08f, erde - fr * 0.2f + nachschub * fr * 0.5f)
            if (t > startZeit - 0.4f) for (i in 0 until 8) {
                val q = an(t, startZeit - 0.4f + i * 0.15f, 3f)
                val rw = fr * (0.08f + 0.2f * q)
                drawCircle(Color(0xFFE6E9F0).copy(alpha = (0.8f - q * 0.6f) * (1f - an(t, startZeit + 7f, 3f))), rw, Offset(fm.x + fr * 0.08f + (i - 4) * fr * 0.08f * (0.3f + q), erde - rw * 0.3f))
            }
            if (t > startZeit - 0.2f && t < 34f && rakete.y > fm.y - fr * 1.3f) {
                val flamme = fr * (0.2f + 0.08f * sin(t * 40f)) * weich(an(t, startZeit - 0.2f, 0.4f))
                drawOval(Brush.verticalGradient(listOf(Color(0xFFFFF1B0), Color(0xFFFF9A3D), Color.Transparent), rakete.y + fr * 0.12f, rakete.y + fr * 0.12f + flamme), Offset(rakete.x - fr * 0.045f, rakete.y + fr * 0.12f), Size(fr * 0.09f, flamme))
            }
            clipRect(fm.x - fr, fm.y - fr * 3f, fm.x + fr, erde + fr * 0.02f) {
            val koerper = Path().apply {
                moveTo(rakete.x, rakete.y - fr * 0.2f)
                cubicTo(rakete.x + fr * 0.07f, rakete.y - fr * 0.12f, rakete.x + fr * 0.06f, rakete.y + fr * 0.05f, rakete.x + fr * 0.05f, rakete.y + fr * 0.13f)
                lineTo(rakete.x - fr * 0.05f, rakete.y + fr * 0.13f)
                cubicTo(rakete.x - fr * 0.06f, rakete.y + fr * 0.05f, rakete.x - fr * 0.07f, rakete.y - fr * 0.12f, rakete.x, rakete.y - fr * 0.2f); close()
            }
            drawPath(koerper, Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD5DCEA)), rakete.x - fr * 0.07f, rakete.x + fr * 0.07f))
            drawPath(Path().apply { moveTo(rakete.x - fr * 0.05f, rakete.y + fr * 0.02f); lineTo(rakete.x - fr * 0.1f, rakete.y + fr * 0.15f); lineTo(rakete.x - fr * 0.05f, rakete.y + fr * 0.13f); close() }, blau)
            drawPath(Path().apply { moveTo(rakete.x + fr * 0.05f, rakete.y + fr * 0.02f); lineTo(rakete.x + fr * 0.1f, rakete.y + fr * 0.15f); lineTo(rakete.x + fr * 0.05f, rakete.y + fr * 0.13f); close() }, blau.dunkler(0.2f))
            drawCircle(blau.heller(0.2f), fr * 0.03f, rakete - Offset(0f, fr * 0.06f))
            drawCircle(Color.White, fr * 0.03f, rakete - Offset(0f, fr * 0.06f), style = Stroke(1.5f))
            }
        }
        // Innenwand der Laibung: dunkler Ring zum Raum hin versetzt, gibt dem Fenster Tiefe
        drawCircle(Color.Black.copy(alpha = 0.35f), fr * 1.04f, fm + laibung, style = Stroke(fr * 0.16f))
    }
    drawCircle(Brush.sweepGradient(listOf(if (d) Color(0xFF3A4A75) else Color(0xFFA9BADC), if (d) Color(0xFF1E2A4A) else Color(0xFF7F93BD), if (d) Color(0xFF3A4A75) else Color(0xFFA9BADC)), fm), fr, fm, style = Stroke(h * 0.04f))
    drawCircle(Color.White.copy(alpha = if (d) 0.08f else 0.35f), fr + h * 0.014f, fm, style = Stroke(h * 0.006f))
    for (i in 0 until 12) { val wk = i * 30f; drawCircle(if (d) Color(0xFF4A5A85) else Color(0xFFE6EEFB), h * 0.006f, fm + Offset(cos(rad(wk)) * fr, sin(rad(wk)) * fr)) }

    // Porträt von Albert Einstein an der Wand zwischen Pult und Bullauge
    val bild = Rect(w * 0.53f - h * 0.085f, h * 0.06f, w * 0.53f + h * 0.085f, h * 0.29f)
    drawRect(Color.Black.copy(alpha = 0.2f), bild.topLeft + Offset(3f, 4f), bild.size)
    drawRect(if (d) Color(0xFF3A4668) else Color(0xFF6E7FA6), bild.topLeft, bild.size)
    val innen = Rect(bild.left + h * 0.012f, bild.top + h * 0.012f, bild.right - h * 0.012f, bild.bottom - h * 0.012f)
    clipRect(innen.left, innen.top, innen.right, innen.bottom) {
        val iw = innen.width; val ih = innen.height
        val kopf = Offset(innen.center.x, innen.top + ih * 0.47f)
        val haar = Color(0xFFECECF0)
        drawRect(Brush.verticalGradient(listOf(Color(0xFFB9AE9C), Color(0xFF8E8474)), innen.top, innen.bottom), innen.topLeft, innen.size)
        // Jackett und Hemdkragen
        drawOval(Color(0xFF2B2F3A), Offset(innen.left - iw * 0.05f, innen.top + ih * 0.76f), Size(iw * 1.1f, ih * 0.6f))
        drawPath(Path().apply { moveTo(kopf.x - iw * 0.12f, innen.top + ih * 0.78f); lineTo(kopf.x, innen.top + ih * 0.95f); lineTo(kopf.x + iw * 0.12f, innen.top + ih * 0.78f); close() }, Color(0xFFE8E4DA))
        // Wilde weiße Haare rund um den Kopf
        for (i in 0 until 11) {
            val wk = rad(170f + i * 20f)
            drawCircle(if (i % 2 == 0) haar else haar.dunkler(0.1f), iw * (0.13f + (i * 7 % 3) * 0.015f), kopf + Offset(cos(wk) * iw * 0.24f, sin(wk) * ih * 0.2f - ih * 0.04f))
        }
        // Gesicht
        drawOval(Color(0xFFD8B090), Offset(kopf.x - iw * 0.19f, kopf.y - ih * 0.17f), Size(iw * 0.38f, ih * 0.42f))
        drawOval(haar, Offset(kopf.x - iw * 0.17f, kopf.y - ih * 0.2f), Size(iw * 0.34f, ih * 0.1f))
        for (seite in listOf(-1f, 1f)) {
            drawLine(haar.dunkler(0.15f), kopf + Offset(seite * iw * 0.13f, -ih * 0.045f), kopf + Offset(seite * iw * 0.04f, -ih * 0.035f), 2.2f, StrokeCap.Round)
            drawCircle(Color(0xFF2A2420), iw * 0.022f, kopf + Offset(seite * iw * 0.085f, ih * 0.005f))
        }
        drawLine(Color(0xFFB98E6E), kopf + Offset(0f, ih * 0.0f), kopf + Offset(iw * 0.015f, ih * 0.08f), 2f, StrokeCap.Round)
        // Schnurrbart
        drawOval(haar.dunkler(0.08f), Offset(kopf.x - iw * 0.12f, kopf.y + ih * 0.095f), Size(iw * 0.24f, ih * 0.06f))
    }
    drawRect(Color.White.copy(alpha = if (d) 0.1f else 0.35f), bild.topLeft, bild.size, style = Stroke(1.5f))

    // Pult als Block mit sichtbarer Oberseite
    val pult = Rect(w * 0.03f, h * 0.62f, w * 0.4f, boden)
    val pultFarbe = if (d) Color(0xFF1A2440) else Color(0xFF8C9CBF)
    val oben = quader(pult, 0.2f, flucht, pultFarbe)
    drawLine(pultFarbe.heller(0.3f), Offset(pult.left, pult.top), Offset(pult.right, pult.top), 2f)
    // Tastatur quer auf dem Pult: die lange Seite läuft in die Tiefe und zeigt zur Person, die rechts davor steht
    fun aufPult(x: Float, tiefe: Float) = Offset(x, pult.top) + (flucht - Offset(x, pult.top)) * tiefe
    val tastatur = listOf(aufPult(w * 0.352f, 0.185f), aufPult(w * 0.382f, 0.185f), aufPult(w * 0.382f, 0.02f), aufPult(w * 0.352f, 0.02f))
    flaeche(tastatur.map { it + Offset(0f, h * 0.006f) }, Color.Black.copy(alpha = 0.25f))
    flaeche(tastatur, Color(0xFF1B2238))
    for (reihe in 1..3) {
        val a = lerp(tastatur[3], tastatur[2], reihe / 4f)
        val b = lerp(tastatur[0], tastatur[1], reihe / 4f)
        for (taste in 0 until 7) drawLine(blau.heller(0.3f).copy(alpha = 0.55f * (0.3f + 0.7f * licht)), lerp(a, b, (taste + 0.2f) / 7f), lerp(a, b, (taste + 0.75f) / 7f), 1.6f, StrokeCap.Round)
    }
    // Lämpchen auf der Pultkante
    for (i in 0 until 9) {
        val an = welle(t, zyklus, 14 + i * 5, i.toFloat()) > 0.2f
        val farbe = when (i % 3) { 0 -> blau; 1 -> blau.heller(0.25f); else -> Color(0xFFFFB74D) }
        drawCircle(if (an) farbe else farbe.copy(alpha = 0.25f), h * 0.011f, Offset(pult.left + w * 0.035f + i * w * 0.035f, pult.top + h * 0.045f))
    }
    // Zwei Bildschirme, leicht zum Raum gedreht: Inhalte flach gezeichnet und perspektivisch verzerrt
    val mon1 = Rect(0f, 0f, w * 0.15f, h * 0.32f)
    val mon2 = Rect(0f, 0f, w * 0.16f, h * 0.32f)
    val ecken1 = listOf(Offset(w * 0.05f, h * 0.18f), Offset(w * 0.2f, h * 0.21f), Offset(w * 0.2f, h * 0.51f), Offset(w * 0.05f, h * 0.54f))
    val ecken2 = listOf(Offset(w * 0.22f, h * 0.2f), Offset(w * 0.38f, h * 0.22f), Offset(w * 0.38f, h * 0.5f), Offset(w * 0.22f, h * 0.52f))
    for (e in listOf(ecken1, ecken2)) {
        val mitte = Offset((e[0].x + e[1].x) / 2f, (e[2].y + e[3].y) / 2f)
        drawRect(Color(0xFF2A3350), Offset(mitte.x - 3f, mitte.y), Size(6f, (oben[0].y + oben[3].y) / 2f - mitte.y))
        // Gehäusekante rechts
        flaeche(listOf(e[1], e[1] + Offset(w * 0.008f, h * 0.006f), e[2] + Offset(w * 0.008f, -h * 0.004f), e[2]), Color(0xFF070B18))
    }
    val punkteZeit = floatArrayOf(7.2f, 9.6f, 12f, 14.4f)
    verzerrt(perspektive(mon1, ecken1)) {
        drawRoundRect(Color(0xFF0E1428), mon1.topLeft, mon1.size, CornerRadius(6f))
        drawRoundRect(blau.copy(alpha = 0.3f), mon1.topLeft, mon1.size, CornerRadius(6f), style = Stroke(2f))
        for (i in 0 until 4) {
            val y = mon1.top + mon1.height * (0.2f + i * 0.2f)
            val ok = an(t, punkteZeit[i], 0.3f)
            drawCircle(lerp(Color(0xFF55607A), blau.heller(0.2f), ok), h * 0.014f, Offset(mon1.left + mon1.width * 0.14f, y))
            drawLine(lerp(Color(0xFF55607A), blau.heller(0.4f), ok), Offset(mon1.left + mon1.width * 0.28f, y), Offset(mon1.left + mon1.width * (0.8f - (i % 2) * 0.15f), y), 3f, StrokeCap.Round)
        }
        if (schirm < 1f) drawRoundRect(Color(0xFF05070F).copy(alpha = 1f - schirm), mon1.topLeft, mon1.size, CornerRadius(6f))
    }
    verzerrt(perspektive(mon2, ecken2)) {
        drawRoundRect(Color(0xFF0E1428), mon2.topLeft, mon2.size, CornerRadius(6f))
        drawRoundRect(blau.copy(alpha = 0.3f), mon2.topLeft, mon2.size, CornerRadius(6f), style = Stroke(2f))
        // Kurve, dann Countdown, dann Start, dann Haken
        when {
            t < 16.6f -> {
                val p = Path()
                for (x in 0..40) {
                    val xx = mon2.left + mon2.width * (0.08f + 0.84f * x / 40f)
                    val yy = mon2.center.y + sin(x * 0.5f + t * 3f) * mon2.height * 0.18f * (0.5f + 0.5f * sin(x * 0.13f))
                    if (x == 0) p.moveTo(xx, yy) else p.lineTo(xx, yy)
                }
                drawPath(p, blau.heller(0.3f), style = Stroke(2.5f))
            }
            t < startZeit -> {
                val zahl = (startZeit - t).toInt().coerceIn(1, 3).toString()
                val txt = schrift.measure(zahl, TextStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                drawText(txt, topLeft = mon2.center - Offset(txt.size.width / 2f, txt.size.height / 2f))
            }
            t < startZeit + 6f -> {
                val txt = schrift.measure("START", TextStyle(color = f.sekundaer, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold))
                drawText(txt, topLeft = mon2.center - Offset(txt.size.width / 2f, txt.size.height / 2f))
            }
            else -> haken(mon2.center, mon2.height * 0.35f, an(t, startZeit + 6f, 0.5f), f.sekundaer, 5f)
        }
        if (schirm < 1f) drawRoundRect(Color(0xFF05070F).copy(alpha = 1f - schirm), mon2.topLeft, mon2.size, CornerRadius(6f))
    }

    // Bürostuhl mit fünf Rollen, Lehne fest zum Raum hin (die Sitzfläche schaut zum Pult)
    val stuhlX = w * 0.3f
    val stuhlF = if (d) Color(0xFF3A4668) else Color(0xFF5A6788)
    drawOval(Color.Black.copy(alpha = 0.15f), Offset(stuhlX - h * 0.12f, boden - h * 0.025f), Size(h * 0.24f, h * 0.04f))
    for (dx in listOf(-0.1f, -0.045f, 0.045f, 0.1f)) {
        drawLine(stuhlF.dunkler(0.15f), Offset(stuhlX, boden - h * 0.045f), Offset(stuhlX + dx * h, boden - h * 0.02f), 4f, StrokeCap.Round)
        drawCircle(Color(0xFF1A1F2E), h * 0.011f, Offset(stuhlX + dx * h, boden - h * 0.01f))
    }
    drawLine(Color(0xFF9AA3B8), Offset(stuhlX, boden - h * 0.045f), Offset(stuhlX, boden - h * 0.2f), 4f)
    drawRoundRect(stuhlF.dunkler(0.2f), Offset(stuhlX - h * 0.018f, boden - h * 0.12f), Size(h * 0.036f, h * 0.06f), CornerRadius(4f))
    // Lehne mit Träger, fest
    drawLine(stuhlF.dunkler(0.1f), Offset(stuhlX + h * 0.06f, boden - h * 0.23f), Offset(stuhlX + h * 0.095f, boden - h * 0.3f), 5f, StrokeCap.Round)
    drawRoundRect(stuhlF.dunkler(0.05f), Offset(stuhlX + h * 0.075f, boden - h * 0.46f), Size(h * 0.05f, h * 0.2f), CornerRadius(10f))
    drawRoundRect(stuhlF.heller(0.12f), Offset(stuhlX + h * 0.082f, boden - h * 0.44f), Size(h * 0.016f, h * 0.15f), CornerRadius(6f))
    // Sitzfläche mit Polster
    drawRoundRect(stuhlF, Offset(stuhlX - h * 0.1f, boden - h * 0.235f), Size(h * 0.2f, h * 0.035f), CornerRadius(8f))
    drawRoundRect(stuhlF.heller(0.12f), Offset(stuhlX - h * 0.1f, boden - h * 0.25f), Size(h * 0.2f, h * 0.022f), CornerRadius(8f))

    // Katze: kommt mit herein, springt auf den Drehstuhl, später aufs Pult, tatzt, schaut zur Rakete, geht mit hinaus
    val katzeF = KatzenFarben(Color(0xFFB9BFCC), Color(0xFF7E8596), Color(0xFFF2F4F8), Color(0xFF7FD3F0))
    val cs = h * 0.16f
    val sitzY = boden - h * 0.24f
    val pultY = (oben[0].y + oben[3].y) / 2f
    val pultKatzeX = w * 0.2f
    val tatzen = maxOf(0f, sin((t - 13.6f) * 6f)) * weich(an(t, 13.6f, 0.4f)) * (1f - weich(an(t, 16f, 0.4f)))
    when {
        t < 2.5f -> Unit
        t < 4.6f -> katze(mix(-0.25f * w, stuhlX - h * 0.1f, weich(an(t, 2.5f, 2.1f))), boden, cs, 1f, 0, t * 10f, t, katzeF)
        t < 5.2f -> { val q = an(t, 4.6f, 0.6f); katze(mix(stuhlX - h * 0.1f, stuhlX, weich(q)), mix(boden, sitzY, q), cs, mix(1f, -1f, weich(q)), 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.08f) }
        t < 12.6f -> katze(stuhlX, sitzY, cs, -1f, 1, 0f, t, katzeF)
        t < 13.4f -> { val q = an(t, 12.6f, 0.8f); katze(mix(stuhlX, pultKatzeX, weich(q)), mix(sitzY, pultY, q), cs, -1f, 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.15f) }
        // Erst zum Pult, dann zur Rakete schauen, zum Gehen wieder nach links drehen
        t < 33.4f -> katze(pultKatzeX, pultY, cs, if (t < 32.8f) mix(-1f, 1f, weich(an(t, 16.4f, 0.6f))) else mix(1f, -1f, weich(an(t, 32.8f, 0.6f))), 1, 0f, t, katzeF, pfote = tatzen)
        t < 34f -> { val q = an(t, 33.4f, 0.6f); katze(mix(pultKatzeX, w * 0.12f, weich(q)), mix(pultY, boden, q), cs, -1f, 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.08f) }
        else -> katze(mix(w * 0.12f, -0.25f * w, an(t, 34.2f, 2.4f)), boden, cs, -1f, 0, t * 10f, t, katzeF)
    }

    // Person: kommt, drückt im Wechsel die Knöpfe, dreht sich zum Fenster, Countdown, Start, Jubel, Winken, geht
    val mf = MenschFarben(Color(0xFFD9B08C), Color(0xFF1E2230), lerp(f.primaer, Color(0xFFDDE3EE), 0.45f), Color(0xFF2A3350), Color(0xFF1A1F2E))
    val blink = blinzelt(t)
    val pultX = w * 0.44f
    val pose = choreo(t, s, listOf(
        Takt(0f) { gehen(mix(-0.12f * w, pultX, an(it, 2.5f, 2.9f)), boden, 1f, it * 8.5f, blink) },
        Takt(5.6f) {
            // Über die Tastatur gebeugt, abwechselnd tippt die linke und die rechte Hand mit einem weichen Anschlag
            val takt = (it - 5.6f) / 0.6f
            val links = floor(takt).toInt() % 2 == 0
            val druck = sin(PI.toFloat() * (takt - floor(takt))).let { x -> x * x }
            Pose(pultX, boden, -1f, lean = 22f, sL = 38f - (if (links) 7f * druck else 0f), eL = 22f - (if (links) 10f * druck else 0f), sR = 42f - (if (!links) 7f * druck else 0f), eR = 20f - (if (!links) 10f * druck else 0f), kopf = 12f, blinzeln = blink)
        },
        Takt(15.6f) { Pose(pultX, boden, 1f, lean = 6f, sR = 20f, eR = 100f, sL = 10f, eL = 20f, kopf = -4f, lachen = 0.2f, blinzeln = blink) },
        Takt(startZeit + 0.4f) { Pose(pultX, boden - huepfen(it, startZeit + 0.8f, startZeit + 5f, 0.07f * s), 1f, sL = 170f, eL = 6f, sR = 160f, eR = 10f, lachen = 1f, blinzeln = blink) },
        Takt(startZeit + 5.2f) { Pose(pultX, boden, 1f, sR = 140f + sin(it * 5f) * 18f * (1f - weich(an(it, startZeit + 9f, 1f))), eR = 40f, sL = 8f, eL = 10f, lachen = 0.85f, blinzeln = blink) },
        Takt(startZeit + 10.5f) { Pose(pultX, boden, 1f, sR = 8f, eR = 12f, sL = 8f, eL = 10f, kopf = -3f, lachen = 0.8f, blinzeln = blink) },
        Takt(33.6f) { gehen(mix(pultX, -0.15f * w, an(it, 33.6f, 4.6f)), boden, -1f, it * 8.5f, blink) },
    ))
    mensch(pose, s, mf)

    // Raumlicht aus: Schleier über dem Raum, das Fenster ins All bleibt hell
    if (licht < 1f) drawPath(
        Path().apply { fillType = PathFillType.EvenOdd; addRect(Rect(0f, 0f, w, h)); addOval(Rect(fm, fr)) },
        Color(0xFF02040C).copy(alpha = 0.62f * (1f - licht)),
    )
}
