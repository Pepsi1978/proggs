package de.frank.aufgaben.ui.szenen

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
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
 * Garten: weite Landschaft mit Tiefe (Hügel, Weg zum Horizont, Zaun). Drei Hochbeete hinter einem Gartenweg sind drei
 * Aufgaben: Die Person geht auf dem Weg von Beet zu Beet und gießt, stellt die Kanne ab, schaut beim Wachsen zu,
 * pflückt eine Blume und freut sich. Die Katze lauert einem Schmetterling auf, springt einmal, verfehlt ihn und setzt
 * sich dann zur Person. Zeiten passen zu `ablauf()` in Szene.kt (38 s).
 */
internal fun DrawScope.szeneGarten(t: Float, f: Farben) {
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.93f
    val s = h * 0.56f
    val horizont = h * 0.5f
    val flucht = Offset(w * 0.5f, h * 0.42f)
    val gruen = f.primaer

    // Himmel, Sonne im langsamen Bogen, Wolken
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF0C1A22), Color(0xFF1B3328)) else listOf(Color(0xFFCFE8F2), Color(0xFFF2F4E4)), 0f, horizont + h * 0.1f))
    val bogen = t / 38f
    val sonne = Offset(w * (0.15f + 0.7f * bogen), horizont - sin(bogen * 3.14f) * h * 0.38f)
    val sonnenFarbe = if (d) Color(0xFFEFE6C8) else Color(0xFFFFD27A)
    drawCircle(Brush.radialGradient(listOf(sonnenFarbe.copy(alpha = 0.55f), Color.Transparent), sonne, h * 0.2f), h * 0.2f, sonne)
    drawCircle(sonnenFarbe, h * 0.05f, sonne)
    if (d) drawCircle(Color(0xFF1B3328), h * 0.045f, sonne + Offset(h * 0.02f, -h * 0.01f))
    for (i in 0 until 3) {
        val wx = ((t * (5f + i * 2.5f) + i * w * 0.4f) % (w * 1.3f)) - w * 0.15f
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
    val ferne = lerp(gruen, if (d) Color(0xFF1B3328) else Color(0xFFE6F0E0), 0.65f)
    val mitte = lerp(gruen, if (d) Color(0xFF1B3328) else Color(0xFFE6F0E0), 0.4f)
    drawPath(Path().apply { moveTo(0f, horizont); cubicTo(w * 0.2f, horizont - h * 0.2f, w * 0.45f, horizont - h * 0.05f, w * 0.65f, horizont - h * 0.14f); cubicTo(w * 0.8f, horizont - h * 0.22f, w * 0.95f, horizont - h * 0.08f, w, horizont - h * 0.1f); lineTo(w, h); lineTo(0f, h); close() }, ferne)
    for (i in 0 until 6) {
        val bx = w * (0.08f + i * 0.17f)
        val by = horizont - h * 0.06f - sin(i * 1.7f) * h * 0.04f
        drawLine(ferne.dunkler(0.3f), Offset(bx, by), Offset(bx, by + h * 0.03f), 2f)
        drawCircle(ferne.dunkler(0.2f), h * 0.022f, Offset(bx, by))
    }
    drawPath(Path().apply { moveTo(0f, horizont + h * 0.06f); cubicTo(w * 0.3f, horizont - h * 0.04f, w * 0.6f, horizont + h * 0.1f, w, horizont + h * 0.02f); lineTo(w, h); lineTo(0f, h); close() }, mitte)
    // Wiese
    drawPath(Path().apply { moveTo(0f, h * 0.66f); cubicTo(w * 0.35f, h * 0.62f, w * 0.7f, h * 0.7f, w, h * 0.64f); lineTo(w, h); lineTo(0f, h); close() }, Brush.verticalGradient(listOf(lerp(gruen, Color.White, if (d) 0.05f else 0.35f), lerp(gruen, Color.Black, if (d) 0.45f else 0.1f)), h * 0.62f, h))
    // Weg vom Horizont nach vorne links zum Gartenweg (Fluchtpunkt)
    val wegFarbe = if (d) Color(0xFF3A3326) else Color(0xFFE8D9B5)
    drawPath(Path().apply { moveTo(w * 0.47f, horizont + h * 0.05f); lineTo(w * 0.49f, horizont + h * 0.05f); cubicTo(w * 0.47f, h * 0.68f, w * 0.34f, h * 0.76f, w * 0.3f, h * 0.87f); lineTo(w * 0.12f, h * 0.87f); cubicTo(w * 0.24f, h * 0.76f, w * 0.44f, h * 0.68f, w * 0.47f, horizont + h * 0.05f); close() }, wegFarbe)
    // Zaun mit perspektivisch kleiner werdenden Pfosten
    for (i in 0 until 12) {
        val fx = w * (0.52f + i * 0.045f)
        val gross = 1f - i * 0.05f
        val fy = h * 0.66f - i * h * 0.004f
        drawLine(if (d) Color(0xFF6B5A44) else Color(0xFFA88A62), Offset(fx, fy), Offset(fx, fy - h * 0.09f * gross), 3f * gross)
    }
    drawLine(if (d) Color(0xFF6B5A44) else Color(0xFFA88A62), Offset(w * 0.52f, h * 0.62f), Offset(w, h * 0.585f), 2.5f)
    // Baum links mit wehender Krone und Schatten
    val stamm = Offset(w * 0.1f, h * 0.86f)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(stamm.x - h * 0.16f, stamm.y - h * 0.02f), Size(h * 0.36f, h * 0.05f))
    drawLine(Color(0xFF7A5A3E), stamm, stamm - Offset(0f, h * 0.42f), h * 0.04f, StrokeCap.Round)
    drawLine(Color(0xFF5E4430), stamm + Offset(h * 0.01f, 0f), stamm - Offset(-h * 0.01f, h * 0.42f), h * 0.012f, StrokeCap.Round)
    rotate(sin(t * 0.9f) * 2f, stamm) {
        for ((dx, dy, r) in listOf(Triple(-0.07f, -0.52f, 0.12f), Triple(0.06f, -0.54f, 0.13f), Triple(0f, -0.64f, 0.14f), Triple(-0.02f, -0.46f, 0.1f))) {
            drawCircle(lerp(gruen, Color.Black, if (d) 0.35f else 0.15f), h * r, stamm + Offset(h * dx, h * dy))
            drawCircle(lerp(gruen, Color.White, 0.2f).copy(alpha = 0.35f), h * r * 0.5f, stamm + Offset(h * dx - h * r * 0.3f, h * dy - h * r * 0.3f))
        }
    }

    // Gartenweg vorne quer, mit Trittsteinen in Aufsicht
    drawRect(Brush.verticalGradient(listOf(wegFarbe.dunkler(0.04f), wegFarbe.heller(0.1f)), h * 0.86f, h), Offset(0f, h * 0.86f), Size(w, h * 0.14f))
    drawLine(Color.Black.copy(alpha = 0.1f), Offset(0f, h * 0.86f), Offset(w, h * 0.86f), 2f)
    for (i in 0 until 9) drawOval(wegFarbe.dunkler(0.12f), Offset(w * (0.03f + i * 0.115f), h * 0.94f), Size(h * 0.14f, h * 0.035f))

    // Drei Hochbeete als Holzkisten mit Erde, die man von oben sieht
    val beete = listOf(w * 0.36f, w * 0.58f, w * 0.8f)
    val giessStart = listOf(3f, 8.6f, 14.2f)
    val giessDauer = 3.6f
    val holz = if (d) Color(0xFF6B4E36) else Color(0xFF9C7350)
    val erde = Color(0xFF5C3F2B)
    val erdeY = beete.map { bx ->
        val oben = quader(Rect(bx - h * 0.12f, h * 0.75f, bx + h * 0.12f, h * 0.81f), 0.2f, flucht, holz)
        val mitteOben = Offset((oben[0].x + oben[1].x + oben[2].x + oben[3].x) / 4f, (oben[0].y + oben[1].y + oben[2].y + oben[3].y) / 4f)
        flaeche(oben.map { it + (mitteOben - it) * 0.14f }, erde)
        drawLine(holz.dunkler(0.2f), Offset(bx - h * 0.12f, h * 0.78f), Offset(bx + h * 0.12f, h * 0.78f), 1.5f)
        mitteOben.y
    }
    beete.forEachIndexed { i, bx ->
        val boden0 = erdeY[i]
        val wachsen = weich(an(t, giessStart[i] + giessDauer, 3.5f))
        val bluete = weich(an(t, giessStart[i] + giessDauer + 3.4f, 1f))
        if (wachsen > 0f) for (j in -1..1) {
            val px = bx + j * h * 0.06f
            val hoehe = h * (0.12f + 0.06f * (j + 1) % 2) * wachsen
            val wind = sin(t * 1.6f + j + i) * 3f
            drawLine(Color(0xFF3D8A55), Offset(px, boden0), Offset(px + wind, boden0 - hoehe), 2.5f, StrokeCap.Round)
            if (wachsen > 0.4f) for (b in 0..1) {
                val by = boden0 - hoehe * (0.35f + b * 0.3f)
                val seite = if ((b + j) % 2 == 0) 1f else -1f
                drawOval(Color(0xFF4FA768), Offset(px + wind * 0.5f + (if (seite > 0) 0f else -h * 0.035f), by - h * 0.01f), Size(h * 0.035f, h * 0.018f))
            }
            // Die vorderste Blume im letzten Beet wird später gepflückt
            val gepflueckt = i == 2 && j == -1 && t >= 24.5f
            if (bluete > 0f && !gepflueckt) bluetenKopf(Offset(px + wind, boden0 - hoehe), h, bluete, if (j == 0) Color(0xFFFFF4E0) else lerp(gruen, Color.White, 0.75f), t)
        }
        // Schildchen mit Haken, sobald das Beet blüht
        val schild = an(t, giessStart[i] + giessDauer + 4.4f, 0.4f)
        if (schild > 0f) {
            val sp = Offset(bx + h * 0.1f, boden0)
            drawLine(Color(0xFF8C6A48), sp, sp - Offset(0f, h * 0.12f * schild), 3f)
            drawRoundRect(Color(0xFFF3E6CF), sp - Offset(h * 0.035f, h * 0.17f * schild), Size(h * 0.07f, h * 0.05f), CornerRadius(4f))
            haken(sp - Offset(0f, h * 0.145f * schild), h * 0.035f, an(t, giessStart[i] + giessDauer + 4.7f, 0.3f), f.primaer, 2.5f)
        }
    }
    // Gras an der Wegkante
    for (i in 0 until 40) {
        val gx = i * w / 40f + (i % 3) * 4f
        val wind = sin(t * 1.8f + i * 0.4f) * 3f
        drawLine(lerp(gruen, Color.Black, if (d) 0.3f else 0.05f), Offset(gx, h * 0.865f), Offset(gx + wind, h * 0.865f - h * (0.02f + (i % 4) * 0.008f)), 2f, StrokeCap.Round)
    }

    // Schmetterlinge: der erste taucht für die Katze tief und entwischt nach oben
    val frei0 = Offset(w * (0.32f + 0.12f * sin(t * 0.41f) + 0.04f * sin(t * 1.3f)), h * (0.5f + 0.08f * sin(t * 0.9f) + 0.03f * sin(t * 2.3f)))
    val tief = weich(an(t, 11f, 1.8f)) * (1f - weich(an(t, 13.2f, 1.4f)))
    val flucht0 = weich(an(t, 13.1f, 1f)) * (1f - weich(an(t, 16f, 3f)))
    val falter = listOf(
        lerp(frei0, Offset(w * 0.28f, h * 0.72f), tief) - Offset(0f, h * 0.18f * flucht0),
        Offset(w * (0.6f + 0.25f * sin(t * 0.33f + 2f)), h * (0.45f + 0.12f * sin(t * 0.77f + 1f))),
    )

    // Katze: sitzt unter dem Baum, lauert, springt einmal, schaut dem Falter nach, setzt sich dann zur Person
    val katzeF = KatzenFarben(Color(0xFF9C8B7A), Color(0xFF5E5146), Color(0xFFEDE3D6))
    val cs = h * 0.17f
    val katzeEnde = beete[2] - h * 0.48f
    when {
        t < 11.4f -> katze(w * 0.17f, boden, cs, 1f, 1, 0f, t, katzeF, pfote = maxOf(0f, sin((t - 5f) * 3f)) * 0.6f * weich(an(t, 5f, 0.4f)) * (1f - weich(an(t, 7.5f, 0.4f))))
        t < 12.8f -> katze(w * 0.17f, boden, cs, 1f, 3, 0f, t, katzeF)
        t < 13.5f -> { val q = an(t, 12.8f, 0.7f); katze(mix(w * 0.17f, w * 0.27f, weich(q)), boden, cs, 1f, 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.22f) }
        t < 20f -> katze(w * 0.27f, boden, cs, 1f, 1, 0f, t, katzeF)
        t < 23f -> katze(mix(w * 0.27f, katzeEnde, weich(an(t, 20f, 3f))), boden, cs, 1f, 0, t * 10f, t, katzeF)
        else -> katze(katzeEnde, boden, cs, 1f, 1, 0f, t, katzeF)
    }

    // Person mit Gießkanne auf dem Weg vor den Beeten
    val mf = MenschFarben(Color(0xFFE6BE98), Color(0xFF6B4A2E), lerp(f.primaer, Color(0xFFD9D2C0), 0.35f), Color(0xFF5A5344), Color(0xFF3A3024))
    val blink = blinzelt(t)
    val stehX = beete.map { it - h * 0.26f }
    val tragen = { x: Float, z: Float -> gehen(x, boden, 1f, z * 8.5f, blink).copy(sR = 20f, eR = 10f) }
    val giessen = { i: Int -> Pose(stehX[i], boden, 1f, lean = 8f, sR = 70f, eR = 10f, blinzeln = blink) }
    val pflueckX = beete[2] - h * 0.2f
    val pose = choreo(t, s, listOf(
        Takt(0f) { tragen(mix(-0.1f * w, stehX[0], an(it, 0f, giessStart[0])), it) },
        Takt(giessStart[0]) { giessen(0) },
        Takt(giessStart[0] + giessDauer) { tragen(mix(stehX[0], stehX[1], an(it, giessStart[0] + giessDauer, giessStart[1] - giessStart[0] - giessDauer)), it) },
        Takt(giessStart[1]) { giessen(1) },
        Takt(giessStart[1] + giessDauer) { tragen(mix(stehX[1], stehX[2], an(it, giessStart[1] + giessDauer, giessStart[2] - giessStart[1] - giessDauer)), it) },
        Takt(giessStart[2]) { giessen(2) },
        // Kanne abstellen: in die Hocke
        Takt(17.8f) { Pose(stehX[2], boden, 1f, lean = 25f, hL = 70f, kL = 120f, hR = 60f, kR = 110f, sR = 10f, eR = 5f, sL = 0f, eL = 10f, blinzeln = blink) },
        // Zuschauen beim Wachsen, Hände in die Hüften
        Takt(19.2f) { Pose(stehX[2], boden, 1f, sL = -30f, eL = 75f, sR = -30f, eR = 75f, kopf = sin(it * 1.2f) * 4f, lachen = 0.6f, blinzeln = blink) },
        // Ernten: zwei Schritte ans Beet, Blume pflücken
        Takt(23.4f) { gehen(mix(stehX[2], pflueckX, an(it, 23.4f, 0.8f)), boden, 1f, it * 8.5f, blink) },
        Takt(24.1f) { Pose(pflueckX, boden, 1f, lean = 14f, sR = 75f, eR = 5f, blinzeln = blink) },
        // Umdrehen zur Katze und freuen
        Takt(24.9f) { Pose(pflueckX, boden, -1f, sR = 150f, eR = 10f, lachen = 0.8f, blinzeln = blink) },
        Takt(25.7f) { Pose(pflueckX, boden - huepfen(it, 26f, 29.6f, 0.07f * s), -1f, sL = 170f, eL = 0f, sR = 165f, eR = 5f, lachen = 1f, blinzeln = blink) },
        // An der Blume riechen
        Takt(29.8f) { Pose(pflueckX, boden, -1f, sR = 18f, eR = 140f, kopf = -6f, lachen = 0.8f, blinzeln = blink) },
    ))
    val punkte = mensch(pose, s, mf)

    // Gießkanne: in der Hand, beim Gießen geneigt, dann abgestellt
    val kanneBoden = Offset(stehX[2] + h * 0.16f, boden - h * 0.04f)
    val abstellen = weich(an(t, 18.2f, 0.6f))
    val kanne = lerp(punkte.handR + Offset(0f, h * 0.05f), kanneBoden, abstellen)
    val giesst = giessStart.firstOrNull { t in it..it + giessDauer }
    val neigung = if (giesst != null) 32f * weich(an(t, giesst + 0.3f, 0.5f)) * (1f - weich(an(t, giesst + giessDauer - 0.7f, 0.5f))) else 0f
    val kannenFarbe = lerp(f.primaer, Color(0xFF9FB3A8), 0.4f)
    rotate(neigung, kanne) {
        drawRoundRect(kannenFarbe, kanne - Offset(h * 0.04f, h * 0.035f), Size(h * 0.08f, h * 0.07f), CornerRadius(8f))
        drawRoundRect(kannenFarbe.dunkler(0.15f), kanne - Offset(-h * 0.02f, h * 0.035f), Size(h * 0.02f, h * 0.07f), CornerRadius(8f))
        drawLine(kannenFarbe, kanne + Offset(h * 0.035f, 0f), kanne + Offset(h * 0.09f, -h * 0.045f), 4f, StrokeCap.Round)
        drawArc(Color(0xFF6D7F75), 180f, 180f, false, kanne - Offset(h * 0.03f, h * 0.06f), Size(h * 0.06f, h * 0.05f), style = Stroke(3f))
    }
    if (giesst != null && neigung > 20f) {
        val tuelle = kanne + Offset(cos(rad(neigung)) * h * 0.09f + sin(rad(neigung)) * h * 0.045f, sin(rad(neigung)) * h * 0.09f - cos(rad(neigung)) * h * 0.045f)
        val ziel = Offset(beete[giessStart.indexOf(giesst)] - h * 0.03f, erdeY[giessStart.indexOf(giesst)])
        val staerke = (neigung - 20f) / 12f
        for (i in 0 until 9) {
            val q = ((t * 2.2f + i / 9f) % 1f)
            val p = lerp(tuelle, ziel, q) + Offset((i % 3) * 2f, -sin(q * 3.14f) * h * 0.03f)
            drawCircle(Color(0xFF7FC4E8).copy(alpha = (1f - q * 0.6f) * staerke), 2.2f, p)
        }
    }
    // Gepflückte Blume in der Hand
    if (t >= 24.5f) {
        val hand = punkte.handR
        drawLine(Color(0xFF3D8A55), hand, hand - Offset(0f, h * 0.06f), 2.5f, StrokeCap.Round)
        bluetenKopf(hand - Offset(0f, h * 0.06f), h, 1f, Color(0xFFFFF4E0), t)
    }

    // Schmetterlinge zuletzt, sie fliegen vor allem
    falter.forEachIndexed { i, p ->
        val schlag = kotlin.math.abs(sin(t * 12f + i)) * 0.8f + 0.2f
        val farbe = if (i == 0) lerp(f.primaer, Color.White, 0.5f) else Color(0xFFF2C14E)
        drawOval(farbe, Offset(p.x - h * 0.03f * schlag, p.y - h * 0.02f), Size(h * 0.03f * schlag, h * 0.03f))
        drawOval(farbe.dunkler(0.1f), Offset(p.x, p.y - h * 0.02f), Size(h * 0.03f * schlag, h * 0.03f))
        drawLine(Color(0xFF3A3024), p - Offset(0f, h * 0.018f), p + Offset(0f, h * 0.012f), 2f)
    }
}

private fun DrawScope.bluetenKopf(kopf: Offset, h: Float, g: Float, farbe: Color, t: Float) {
    for (bl in 0 until 5) {
        val wk = bl * 72f + t * 10f
        drawCircle(farbe, h * 0.016f * g, kopf + Offset(cos(rad(wk)) * h * 0.016f * g, sin(rad(wk)) * h * 0.016f * g))
    }
    drawCircle(Color(0xFFF2C14E), h * 0.01f * g, kopf)
}
