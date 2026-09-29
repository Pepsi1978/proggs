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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import de.frank.aufgaben.ui.theme.Farben
import kotlin.math.sin

/**
 * Orange: Feierabend-Werkstatt, Seitenansicht. Ankommen, am Bildschirm Aufgaben abarbeiten, die Katze
 * wacht auf und springt auf den Schoß, Sonnenuntergang, am Ende Strecken und Freude.
 */
internal fun DrawScope.szeneOrange(t: Float, f: Farben) {
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.9f
    val s = h * 0.62f
    val cs = h * 0.19f
    val abend = weich(an(t, 0f, 21f))
    val lampeAn = an(t, 5.1f, 0.3f) * (1f - an(t, 24.6f, 0.8f))

    // Wand mit Tapetenstreifen
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF241A13), Color(0xFF16100C)) else listOf(Color(0xFFFFF2E4), Color(0xFFFADFC4))))
    for (i in 0..24) drawLine(Color.Black.copy(alpha = if (d) 0.08f else 0.03f), Offset(i * w / 24f, 0f), Offset(i * w / 24f, boden), 2f)
    drawRect(if (d) Color(0xFF2E2118) else Color(0xFFE9C9A6), Offset(0f, boden - h * 0.07f), Size(w, h * 0.012f))

    // Fenster mit Sonnenuntergang
    val fen = Rect(w * 0.83f, h * 0.08f, w * 0.97f, h * 0.56f)
    val himmelOben = lerp(lerp(Color(0xFF8FC3E8), Color(0xFF6B4A8C), weich(an(abend, 0.3f, 0.5f))), Color(0xFF141733), weich(an(abend, 0.75f, 0.25f)))
    val himmelUnten = lerp(lerp(Color(0xFFFFE3B8), Color(0xFFFF8A3D), weich(an(abend, 0.2f, 0.5f))), Color(0xFF3A2244), weich(an(abend, 0.8f, 0.2f)))
    clipRect(fen.left, fen.top, fen.right, fen.bottom) {
        drawRect(Brush.verticalGradient(listOf(himmelOben, himmelUnten), fen.top, fen.bottom), fen.topLeft, fen.size)
        val sonne = Offset(fen.center.x, fen.top + fen.height * (0.2f + 0.85f * abend))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD08A).copy(alpha = 0.8f), Color.Transparent), sonne, fen.width * 0.6f), fen.width * 0.6f, sonne)
        drawCircle(Color(0xFFFFB347), fen.width * 0.13f, sonne)
        if (abend > 0.8f) for (i in 0 until 9) drawCircle(Color.White.copy(alpha = (abend - 0.8f) * 5f * (0.5f + 0.5f * sin(t * 3 + i))), 1.5f, Offset(fen.left + fen.width * ((i * 0.37f) % 1f), fen.top + fen.height * ((i * 0.21f) % 0.5f)))
        // Dächer
        val dach = if (d) Color(0xFF0E0A08) else Color(0xFF6B4A38)
        drawRect(dach, Offset(fen.left, fen.bottom - fen.height * 0.22f), Size(fen.width * 0.4f, fen.height * 0.22f))
        drawRect(dach.heller(0.1f), Offset(fen.left + fen.width * 0.45f, fen.bottom - fen.height * 0.32f), Size(fen.width * 0.35f, fen.height * 0.32f))
        for (i in 0..2) drawRect(Color(0xFFFFD27A).copy(alpha = abend), Offset(fen.left + fen.width * (0.52f + i * 0.09f), fen.bottom - fen.height * 0.24f), Size(fen.width * 0.05f, fen.height * 0.05f))
    }
    drawRect(if (d) Color(0xFF3A2A1F) else Color.White, fen.topLeft, fen.size, style = Stroke(h * 0.02f))
    drawLine(if (d) Color(0xFF3A2A1F) else Color.White, Offset(fen.center.x, fen.top), Offset(fen.center.x, fen.bottom), h * 0.012f)
    drawRect(if (d) Color(0xFF4A3526) else Color(0xFFF3E4D2), Offset(fen.left - h * 0.02f, fen.bottom), Size(fen.width + h * 0.04f, h * 0.03f))
    // Vorhang
    val vorhang = Path().apply {
        moveTo(fen.left - h * 0.05f, fen.top - h * 0.03f)
        lineTo(fen.left + h * 0.02f, fen.top - h * 0.03f)
        quadraticTo(fen.left - h * 0.01f + sin(t * 0.8f) * 3f, fen.center.y, fen.left + h * 0.03f, fen.bottom + h * 0.05f)
        lineTo(fen.left - h * 0.06f, fen.bottom + h * 0.05f); close()
    }
    drawPath(vorhang, f.primaer.copy(alpha = if (d) 0.45f else 0.55f))

    // Regal mit Büchern, Pflanze und Bild
    val regalY = h * 0.34f
    drawRect(if (d) Color(0xFF4A3526) else Color(0xFFB9855A), Offset(w * 0.03f, regalY), Size(w * 0.2f, h * 0.022f))
    val buchFarben = listOf(f.primaer, f.primaer.dunkler(0.35f), Color(0xFF8C7B6E), f.tertiaer, Color(0xFFD9C2AE), f.primaer.heller(0.3f))
    var bx = w * 0.04f
    for (i in 0 until 7) {
        val bh = h * (0.1f + (i * 37 % 5) * 0.012f)
        val bw = h * (0.028f + (i % 3) * 0.006f)
        if (i == 5) rotate(-14f, Offset(bx, regalY)) { drawRect(buchFarben[i % 6], Offset(bx, regalY - bh), Size(bw, bh)) }
        else drawRect(buchFarben[i % 6], Offset(bx, regalY - bh), Size(bw, bh))
        drawLine(Color.White.copy(alpha = 0.25f), Offset(bx + bw * 0.3f, regalY - bh * 0.8f), Offset(bx + bw * 0.3f, regalY - bh * 0.3f), 1.5f)
        bx += bw + 2f
    }
    val topf = Offset(w * 0.19f, regalY)
    drawRect(Color(0xFFC0714F), Offset(topf.x - h * 0.025f, topf.y - h * 0.05f), Size(h * 0.05f, h * 0.05f))
    for (i in 0 until 5) {
        val wk = -60f + i * 30f + sin(t * 1.2f + i) * 5f
        rotate(wk, Offset(topf.x, topf.y - h * 0.05f)) { drawOval(Color(0xFF5E9E6B), Offset(topf.x - h * 0.012f, topf.y - h * 0.13f), Size(h * 0.024f, h * 0.08f)) }
    }
    val bild = Rect(w * 0.27f, h * 0.12f, w * 0.37f, h * 0.3f)
    drawRect(if (d) Color(0xFF4A3526) else Color(0xFFFFFFFF), bild.topLeft, bild.size)
    drawRect(Brush.verticalGradient(listOf(f.primaer.heller(0.4f), f.primaer)), bild.topLeft + Offset(4f, 4f), Size(bild.width - 8f, bild.height - 8f))
    drawPath(Path().apply { moveTo(bild.left + 4f, bild.bottom - 4f); lineTo(bild.left + bild.width * 0.4f, bild.top + bild.height * 0.45f); lineTo(bild.left + bild.width * 0.6f, bild.top + bild.height * 0.65f); lineTo(bild.left + bild.width * 0.75f, bild.top + bild.height * 0.5f); lineTo(bild.right - 4f, bild.bottom - 4f); close() }, f.primaer.dunkler(0.45f))
    // Wanduhr
    val uhr = Offset(w * 0.47f, h * 0.17f)
    drawCircle(if (d) Color(0xFF3A2A1F) else Color.White, h * 0.07f, uhr)
    drawCircle(f.primaer.dunkler(0.2f), h * 0.07f, uhr, style = Stroke(h * 0.01f))
    rotate(t * 30f, uhr) { drawLine(f.text, uhr, uhr + Offset(0f, -h * 0.055f), 2f) }
    rotate(t * 2.5f + 90f, uhr) { drawLine(f.text, uhr, uhr + Offset(0f, -h * 0.035f), 3.5f, StrokeCap.Round) }

    // Boden aus Dielen
    drawRect(if (d) Color(0xFF2C1F16) else Color(0xFFD9AE83), Offset(0f, boden), Size(w, h - boden))
    for (i in 0..14) drawLine(Color.Black.copy(alpha = 0.12f), Offset(i * w / 14f, boden), Offset(i * w / 14f - 14f, h), 1.5f)
    drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, boden), Offset(w, boden), 2f)
    // Teppich
    drawOval(f.primaer.copy(alpha = 0.3f), Offset(w * 0.02f, boden - h * 0.015f), Size(w * 0.28f, h * 0.06f))
    drawOval(f.primaer.copy(alpha = 0.3f), Offset(w * 0.04f, boden - h * 0.005f), Size(w * 0.24f, h * 0.04f), style = Stroke(2f))

    // Person
    val sitzX = w * 0.4f
    val sitzHuefte = boden - 0.285f * s
    val tischY = sitzHuefte - 0.2f * s
    // Schreibtisch
    val tisch = Rect(sitzX + 0.14f * s, tischY, w * 0.8f, tischY + h * 0.035f)
    val holz = if (d) Color(0xFF5A3E2B) else Color(0xFFB07A4F)
    drawRect(holz.dunkler(0.2f), Offset(tisch.left + h * 0.02f, tisch.bottom), Size(h * 0.03f, boden - tisch.bottom))
    drawRect(holz.dunkler(0.2f), Offset(tisch.right - h * 0.05f, tisch.bottom), Size(h * 0.03f, boden - tisch.bottom))
    drawRect(holz.dunkler(0.1f), Offset(tisch.right - w * 0.12f, tisch.bottom), Size(w * 0.1f, h * 0.12f))
    drawCircle(Color(0xFFE8C9A0), h * 0.008f, Offset(tisch.right - w * 0.07f, tisch.bottom + h * 0.06f))
    drawRect(holz, tisch.topLeft, tisch.size)
    drawRect(holz.heller(0.2f), tisch.topLeft, Size(tisch.width, h * 0.008f))
    // Bildschirm mit Aufgabenliste
    val mon = Rect(w * 0.55f, tischY - h * 0.36f, w * 0.72f, tischY - h * 0.07f)
    drawRect(Color(0xFF2A2522), Offset(mon.center.x - h * 0.01f, mon.bottom), Size(h * 0.02f, h * 0.07f))
    drawRect(Color(0xFF2A2522), Offset(mon.center.x - h * 0.05f, tischY - h * 0.012f), Size(h * 0.1f, h * 0.012f))
    drawRoundRect(Color(0xFF1E1A18), mon.topLeft, mon.size, CornerRadius(8f))
    val bild2 = Rect(mon.left + 5f, mon.top + 5f, mon.right - 5f, mon.bottom - 5f)
    drawRoundRect(if (d) Color(0xFF191410) else Color(0xFFFFFAF4), bild2.topLeft, bild2.size, CornerRadius(5f))
    drawRect(f.primaer, bild2.topLeft, Size(bild2.width, h * 0.03f))
    for (i in 0 until 3) drawCircle(Color.White.copy(alpha = 0.8f), h * 0.006f, Offset(bild2.left + h * (0.015f + i * 0.018f), bild2.top + h * 0.015f))
    val haken = floatArrayOf(11.5f, 12.6f, 19.3f, 20.3f)
    for (i in 0 until 4) {
        val y = bild2.top + h * (0.07f + i * 0.047f)
        val da = an(t, 6.3f + i * 1.2f, 0.9f)
        if (da <= 0f) continue
        val k = h * 0.024f
        drawRoundRect(f.primaer, Offset(bild2.left + h * 0.015f, y - k / 2), Size(k, k), CornerRadius(3f), style = Stroke(2f))
        kringel(Offset(bild2.left + h * 0.055f, y), bild2.width * (0.6f - (i % 2) * 0.12f), h * 0.02f, da, f.textLeise, 2f, i)
        haken(Offset(bild2.left + h * 0.015f + k / 2, y), k, an(t, haken[i], 0.4f), f.primaer, 2.6f)
    }
    // Tastatur, Tasse
    drawRoundRect(Color(0xFF3A3431), Offset(sitzX + 0.2f * s, tischY - h * 0.012f), Size(0.14f * s, h * 0.014f), CornerRadius(3f))
    val tasse = Offset(w * 0.5f, tischY)
    drawRoundRect(Color.White.copy(alpha = if (d) 0.8f else 1f), Offset(tasse.x, tasse.y - h * 0.05f), Size(h * 0.04f, h * 0.05f), CornerRadius(4f))
    drawArc(Color.White.copy(alpha = if (d) 0.8f else 1f), -90f, 180f, false, Offset(tasse.x + h * 0.03f, tasse.y - h * 0.04f), Size(h * 0.025f, h * 0.025f), style = Stroke(3f))
    for (i in 0..1) {
        val q = (t * 0.4f + i * 0.5f) % 1f
        drawLine(Color.White.copy(alpha = 0.35f * (1f - q)), Offset(tasse.x + h * (0.012f + i * 0.015f) + sin(q * 6f) * 3f, tasse.y - h * 0.06f - q * h * 0.08f), Offset(tasse.x + h * (0.012f + i * 0.015f) + sin(q * 6f + 1f) * 3f, tasse.y - h * 0.08f - q * h * 0.08f), 2f, StrokeCap.Round)
    }
    // Lampe
    val lFuss = Offset(w * 0.77f, tischY)
    val gelenk = Offset(w * 0.75f, h * 0.3f)
    val schirm = Offset(w * 0.7f, h * 0.33f)
    if (lampeAn > 0f) drawPath(
        Path().apply { moveTo(schirm.x - h * 0.03f, schirm.y); lineTo(schirm.x - h * 0.15f, tischY); lineTo(schirm.x + h * 0.12f, tischY); lineTo(schirm.x + h * 0.04f, schirm.y); close() },
        Brush.verticalGradient(listOf(Color(0xFFFFD08A).copy(alpha = 0.45f * lampeAn), Color(0xFFFFD08A).copy(alpha = 0.05f * lampeAn)), schirm.y, tischY),
    )
    drawRect(Color(0xFF3A3431), Offset(lFuss.x - h * 0.035f, lFuss.y - h * 0.012f), Size(h * 0.07f, h * 0.012f))
    drawLine(Color(0xFF3A3431), lFuss, gelenk, 4f, StrokeCap.Round)
    drawLine(Color(0xFF3A3431), gelenk, schirm, 4f, StrokeCap.Round)
    drawPath(Path().apply { moveTo(schirm.x - h * 0.045f, schirm.y + h * 0.02f); lineTo(schirm.x - h * 0.01f, schirm.y - h * 0.04f); lineTo(schirm.x + h * 0.04f, schirm.y - h * 0.02f); lineTo(schirm.x + h * 0.05f, schirm.y + h * 0.03f); close() }, f.primaer.dunkler(0.1f))
    if (lampeAn > 0f) drawCircle(Color(0xFFFFE6B0).copy(alpha = lampeAn), h * 0.014f, schirm + Offset(0f, h * 0.02f))

    // Stuhl
    val stuhl = if (d) Color(0xFF3A3431) else Color(0xFF4A4340)
    drawRoundRect(stuhl, Offset(sitzX - 0.12f * s, sitzHuefte + 0.03f * s), Size(0.26f * s, 0.05f * s), CornerRadius(6f))
    drawRoundRect(stuhl, Offset(sitzX - 0.16f * s, sitzHuefte - 0.3f * s), Size(0.06f * s, 0.36f * s), CornerRadius(8f))
    drawLine(stuhl, Offset(sitzX, sitzHuefte + 0.08f * s), Offset(sitzX, boden - h * 0.03f), 5f)
    drawLine(stuhl, Offset(sitzX - 0.12f * s, boden - h * 0.02f), Offset(sitzX + 0.12f * s, boden - h * 0.02f), 5f, StrokeCap.Round)
    for (r in listOf(-0.12f, 0.12f)) drawCircle(Color(0xFF2A2522), h * 0.013f, Offset(sitzX + r * s, boden - h * 0.01f))

    // Katze und Person nach Zeitplan
    val katzeF = KatzenFarben(Color(0xFFE39A52), Color(0xFFB8672A), Color(0xFFF7E1C4))
    val mf = MenschFarben(Color(0xFFE8C09B), Color(0xFF3B2A20), lerp(f.primaer, Color(0xFF7A6A60), 0.25f), Color(0xFF363A42), Color(0xFF2A2522))
    val blink = blinzelt(t)
    val standHuefte = boden - 0.49f * s - 0.035f * s
    val pose: Pose = when {
        t < 3.6f -> gehen(mix(-0.1f * w, sitzX, t / 3.6f), boden, 1f, t * 8.5f, blink)
        t < 4.6f -> { val k = weich(an(t, 3.6f, 1f)); Pose(sitzX, boden, 1f, lean = mix(0f, -4f, k), hL = 84f * k, kL = 84f * k, hR = 88f * k, kR = 88f * k, hueftY = mix(standHuefte, sitzHuefte, k), blinzeln = blink) }
        t < 5.4f -> sitzen(sitzX, sitzHuefte, 1f).copy(sR = mix(-6f, 110f, weich(an(t, 4.6f, 0.4f))), eR = 5f, blinzeln = blink)
        t < 15.3f && t in 9.4f..10.6f -> sitzen(sitzX, sitzHuefte, 1f).copy(sR = 18f, eR = 150f, sL = 58f, eL = 32f, kopf = -8f, blinzeln = blink, lachen = 0.1f)
        t < 15.3f -> sitzen(sitzX, sitzHuefte, 1f).copy(sR = 62f + sin(t * 17f) * 4f, eR = 30f, sL = 58f + sin(t * 17f + 1.5f) * 4f, eL = 32f, kopf = sin(t * 1.3f) * 3f, blinzeln = blink)
        t < 19f -> sitzen(sitzX, sitzHuefte, 1f).copy(sR = 28f + sin(t * 4f) * 10f, eR = 45f, sL = 58f, eL = 32f, kopf = 12f, lachen = 0.9f, blinzeln = blink)
        t < 21f -> sitzen(sitzX, sitzHuefte, 1f).copy(sR = 62f + sin(t * 17f) * 4f, eR = 30f, sL = 58f + sin(t * 17f + 1.5f) * 4f, eL = 32f, blinzeln = blink, lachen = 0.6f)
        t < 22f -> { val k = 1f - weich(an(t, 21f, 1f)); Pose(sitzX, boden, 1f, hL = 84f * k, kL = 84f * k, hR = 88f * k, kR = 88f * k, hueftY = mix(standHuefte, sitzHuefte, k), blinzeln = blink) }
        t < 24.6f -> { val hub = maxOf(0f, sin((t - 22f) * 7f)) * 0.06f * s; Pose(sitzX, boden - hub, 1f, sL = 168f, eL = 5f, sR = 172f, eR = -5f, lachen = 1f, blinzeln = blink) }
        else -> gehen(mix(sitzX, sitzX + 0.3f * w, an(t, 24.6f, 1.4f)), boden, 1f, t * 8.5f, blink)
    }
    // Katze hinter oder auf der Person
    val katzeAufSchoss = t in 15.3f..19f
    fun zeichneKatze() {
        when {
            t < 11.5f -> katze(w * 0.14f, boden, cs, 1f, 2, 0f, t, katzeF)
            t < 12.8f -> katze(w * 0.14f, boden, cs, 1f, 3, 0f, t, katzeF)
            t < 14.7f -> katze(mix(w * 0.14f, sitzX + 0.36f * s, an(t, 12.8f, 1.9f)), boden, cs, 1f, 0, t * 11f, t, katzeF)
            t < 15.3f -> { val k = an(t, 14.7f, 0.6f); katze(mix(sitzX + 0.36f * s, sitzX + 0.12f * s, k), boden, cs, -1f, 3, 0f, t, katzeF, hoehe = sin(k * 3.14f) * h * 0.25f + k * (boden - sitzHuefte + 0.05f * s)) }
            t < 19f -> katze(sitzX + 0.14f * s, sitzHuefte - 0.04f * s, cs * 0.95f, -1f, 2, 0f, t, katzeF)
            t < 19.6f -> { val k = an(t, 19f, 0.6f); katze(mix(sitzX + 0.14f * s, sitzX + 0.45f * s, k), boden, cs, 1f, 3, 0f, t, katzeF, hoehe = (1f - k) * (boden - sitzHuefte + 0.04f * s) + sin(k * 3.14f) * h * 0.1f) }
            t < 21.4f -> katze(mix(sitzX + 0.45f * s, fen.center.x - h * 0.05f, an(t, 19.6f, 1.8f)), boden, cs, 1f, 0, t * 11f, t, katzeF)
            t < 21.9f -> { val k = an(t, 21.4f, 0.5f); katze(fen.center.x - h * 0.05f + k * h * 0.08f, boden, cs, 1f, 3, 0f, t, katzeF, hoehe = k * (boden - fen.bottom) + sin(k * 3.14f) * h * 0.08f) }
            else -> katze(fen.center.x + h * 0.03f, fen.bottom, cs * 0.9f, 1f, 1, 0f, t, katzeF)
        }
    }
    if (!katzeAufSchoss) zeichneKatze()
    mensch(pose, s, mf)
    if (katzeAufSchoss) {
        zeichneKatze()
        for (i in 0..2) {
            val q = ((t - 15.5f) * 0.6f + i / 3f) % 1f
            if (t > 15.6f) herz(Offset(sitzX + 0.2f * s + sin(q * 6f + i) * 8f, sitzHuefte - 0.2f * s - q * h * 0.35f), h * 0.03f * (0.6f + q * 0.5f), f.primaer.copy(alpha = 1f - q))
        }
        // Streichelnde Hand über der Katze
        drawCircle(mf.haut, 0.033f * s, Offset(sitzX + (0.15f + 0.05f * sin(t * 4f)) * s, sitzHuefte - 0.1f * s))
    }
    // Funken beim Freuen
    if (t in 22f..24.6f) for (i in 0 until 10) {
        val q = ((t - 22f) * 0.8f + i * 0.1f) % 1f
        val wk = i * 36f
        drawCircle(f.primaer.copy(alpha = 1f - q), 3f, Offset(sitzX + kotlin.math.cos(rad(wk)) * q * h * 0.35f, boden - s - kotlin.math.sin(rad(wk)) * q * h * 0.3f))
    }
    // Abendliche Abdunklung, Lampenlicht hebt sie wieder auf
    drawRect(Color(0xFF1A0C20).copy(alpha = (if (d) 0.25f else 0.18f) * weich(an(abend, 0.55f, 0.45f)) * (1f - 0.6f * lampeAn)))
}
