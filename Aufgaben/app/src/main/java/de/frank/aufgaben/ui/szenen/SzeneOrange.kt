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

/** Länge eines Durchlaufs; Ende und Anfang sind dasselbe Bild (dunkler, leerer Raum), die Szene läuft ohne Schnitt. */
internal const val ORANGE_ZYKLUS = 46f

/**
 * Orange: Werkstatt, Seitenansicht mit räumlicher Tiefe (Fluchtpunkt in Augenhöhe). Im Dunkeln kommen Person und Katze
 * links herein, dabei geht die Sonne auf. Die Katze rollt sich auf dem Teppich ein, die Person setzt sich, schaltet den
 * Bildschirm ein, schreibt und hakt Aufgaben ab, macht beim Dämmern die Lampe am Tischschalter an, trinkt Kaffee. Die
 * Katze wacht auf und springt auf den Schoß, später aufs Fensterbrett. Abends: letzte Aufgaben, aufstehen, freuen, Lampe
 * aus, und beide gehen links wieder hinaus, wo sie hereingekommen sind; im Fenster geht der Mond auf. Nach einer kurzen Pause beginnt alles wieder von vorn.
 */
internal fun DrawScope.szeneOrange(t: Float, f: Farben) {
    val zyklus = ORANGE_ZYKLUS
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.9f
    val s = h * 0.62f
    val cs = h * 0.19f
    val flucht = Offset(w * 0.5f, h * 0.42f)
    fun hinten(p: Offset, k: Float) = p + (flucht - p) * k
    // Nacht → Sonnenaufgang beim Hereinkommen → langer Tag → langsamer Sonnenuntergang → Nacht (1 = Nacht, 0 = Tag).
    val abend = maxOf(1f - weich(an(t, 3f, 5f)), weich(an(t, 18f, 14f)))
    val lampeAn = an(t, 17f, 0.3f) * (1f - an(t, 38.6f, 0.3f))

    // Wand mit Tapetenstreifen
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF241A13), Color(0xFF16100C)) else listOf(Color(0xFFFFF2E4), Color(0xFFFADFC4))))
    for (i in 0..24) drawLine(Color.Black.copy(alpha = if (d) 0.08f else 0.03f), Offset(i * w / 24f, 0f), Offset(i * w / 24f, boden), 2f)
    // Der Boden reicht in die Tiefe bis hinter die hinteren Tischbeine: Dort stößt er an die Wand.
    val wandFuss = boden + (flucht.y - boden) * 0.2f
    // Fußleiste mit Oberkante
    drawRect(if (d) Color(0xFF2E2118) else Color(0xFFE9C9A6), Offset(0f, wandFuss - h * 0.03f), Size(w, h * 0.03f))
    drawRect(Color.White.copy(alpha = if (d) 0.06f else 0.4f), Offset(0f, wandFuss - h * 0.03f), Size(w, h * 0.005f))

    // Fenster mit Laibung (Tiefe) und Sonnenuntergang
    val fen = Rect(w * 0.83f, h * 0.08f, w * 0.97f, h * 0.56f)
    val himmelOben = lerp(lerp(Color(0xFF8FC3E8), Color(0xFF6B4A8C), weich(an(abend, 0.3f, 0.5f))), Color(0xFF141733), weich(an(abend, 0.75f, 0.25f)))
    val himmelUnten = lerp(lerp(Color(0xFFFFE3B8), Color(0xFFFF8A3D), weich(an(abend, 0.2f, 0.5f))), Color(0xFF3A2244), weich(an(abend, 0.8f, 0.2f)))
    clipRect(fen.left, fen.top, fen.right, fen.bottom) {
        drawRect(Brush.verticalGradient(listOf(himmelOben, himmelUnten), fen.top, fen.bottom), fen.topLeft, fen.size)
        val sonne = Offset(fen.center.x, fen.top + fen.height * (0.2f + 0.85f * abend))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD08A).copy(alpha = 0.8f), Color.Transparent), sonne, fen.width * 0.6f), fen.width * 0.6f, sonne)
        drawCircle(Color(0xFFFFB347), fen.width * 0.13f, sonne)
        if (abend > 0.8f) for (i in 0 until 9) drawCircle(Color.White.copy(alpha = (abend - 0.8f) * 5f * (0.5f + 0.5f * welle(t, zyklus, 22, i.toFloat()))), 1.5f, Offset(fen.left + fen.width * ((i * 0.37f) % 1f), fen.top + fen.height * ((i * 0.21f) % 0.5f)))
        // Mond: geht nach Sonnenuntergang auf und vor Sonnenaufgang wieder unter (über den Neustart hinweg)
        val mondLauf = ((t - 30f + zyklus) % zyklus) / 20f
        if (mondLauf < 1f) {
            val r = fen.width * 0.11f
            val mond = Offset(fen.left + fen.width * (0.25f + 0.5f * mondLauf), fen.bottom - fen.height * (0.02f + 0.82f * sin(mondLauf * 3.14f)))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4D6).copy(alpha = 0.35f), Color.Transparent), mond, r * 3f), r * 3f, mond)
            mondsichel(mond, r, Color(0xFFF4EFD9))
        }
        // Dächer
        val dach = if (d) Color(0xFF0E0A08) else Color(0xFF6B4A38)
        drawRect(dach, Offset(fen.left, fen.bottom - fen.height * 0.22f), Size(fen.width * 0.4f, fen.height * 0.22f))
        drawRect(dach.heller(0.1f), Offset(fen.left + fen.width * 0.45f, fen.bottom - fen.height * 0.32f), Size(fen.width * 0.35f, fen.height * 0.32f))
        for (i in 0..2) drawRect(Color(0xFFFFD27A).copy(alpha = abend), Offset(fen.left + fen.width * (0.52f + i * 0.09f), fen.bottom - fen.height * 0.24f), Size(fen.width * 0.05f, fen.height * 0.05f))
        // Drittes Haus bis an den rechten Rand: Himmel und Dächer füllen das ganze Fenster, keine Laibung davor
        drawRect(dach, Offset(fen.left + fen.width * 0.8f, fen.bottom - fen.height * 0.26f), Size(fen.width * 0.2f, fen.height * 0.26f))
        drawRect(Color(0xFFFFD27A).copy(alpha = abend), Offset(fen.left + fen.width * 0.86f, fen.bottom - fen.height * 0.19f), Size(fen.width * 0.05f, fen.height * 0.05f))
        drawRect(dach, Offset(fen.left + fen.width * 0.4f, fen.bottom - fen.height * 0.16f), Size(fen.width * 0.05f, fen.height * 0.16f))
    }
    val rahmen = if (d) Color(0xFF3A2A1F) else Color.White
    drawRect(rahmen, fen.topLeft, fen.size, style = Stroke(h * 0.02f))
    drawLine(rahmen, Offset(fen.center.x, fen.top), Offset(fen.center.x, fen.bottom), h * 0.012f)
    // Fensterbank als flacher Quader
    quader(Rect(fen.left - h * 0.02f, fen.bottom, fen.right + h * 0.02f, fen.bottom + h * 0.025f), 0.06f, flucht, if (d) Color(0xFF4A3526) else Color(0xFFF3E4D2))
    // Vorhang mit Faltenschatten
    val vorhang = Path().apply {
        moveTo(fen.left - h * 0.05f, fen.top - h * 0.03f)
        lineTo(fen.left + h * 0.02f, fen.top - h * 0.03f)
        quadraticTo(fen.left - h * 0.01f + welle(t, zyklus, 6) * 3f, fen.center.y, fen.left + h * 0.03f, fen.bottom + h * 0.05f)
        lineTo(fen.left - h * 0.06f, fen.bottom + h * 0.05f); close()
    }
    drawPath(vorhang, Brush.horizontalGradient(listOf(f.primaer.copy(alpha = if (d) 0.35f else 0.45f), f.primaer.copy(alpha = if (d) 0.55f else 0.65f), f.primaer.copy(alpha = if (d) 0.4f else 0.5f)), fen.left - h * 0.06f, fen.left + h * 0.03f))
    // Zweiter Vorhang rechts, gespiegelt
    val vorhangR = Path().apply {
        moveTo(fen.right + h * 0.05f, fen.top - h * 0.03f)
        lineTo(fen.right - h * 0.02f, fen.top - h * 0.03f)
        quadraticTo(fen.right + h * 0.01f - welle(t, zyklus, 6, 1.7f) * 3f, fen.center.y, fen.right - h * 0.03f, fen.bottom + h * 0.05f)
        lineTo(fen.right + h * 0.06f, fen.bottom + h * 0.05f); close()
    }
    drawPath(vorhangR, Brush.horizontalGradient(listOf(f.primaer.copy(alpha = if (d) 0.4f else 0.5f), f.primaer.copy(alpha = if (d) 0.55f else 0.65f), f.primaer.copy(alpha = if (d) 0.35f else 0.45f)), fen.right - h * 0.03f, fen.right + h * 0.06f))

    // Regal mit Büchern, Pflanze und Bild
    val regalY = h * 0.34f
    quader(Rect(w * 0.03f, regalY, w * 0.23f, regalY + h * 0.022f), 0.08f, flucht, if (d) Color(0xFF4A3526) else Color(0xFFB9855A))
    val buchFarben = listOf(f.primaer, f.primaer.dunkler(0.35f), Color(0xFF8C7B6E), f.tertiaer, Color(0xFFD9C2AE), f.primaer.heller(0.3f))
    var bx = w * 0.04f
    for (i in 0 until 7) {
        val bh = h * (0.1f + (i * 37 % 5) * 0.012f)
        val bw = h * (0.028f + (i % 3) * 0.006f)
        if (i == 5) rotate(-14f, Offset(bx, regalY)) { drawRect(buchFarben[i % 6], Offset(bx, regalY - bh), Size(bw, bh)) }
        else {
            // Buchrücken plus schmale Seitenfläche Richtung Fluchtpunkt
            flaeche(listOf(Offset(bx + bw, regalY - bh), hinten(Offset(bx + bw, regalY - bh), 0.05f), hinten(Offset(bx + bw, regalY), 0.05f), Offset(bx + bw, regalY)), buchFarben[i % 6].dunkler(0.3f))
            drawRect(buchFarben[i % 6], Offset(bx, regalY - bh), Size(bw, bh))
        }
        drawLine(Color.White.copy(alpha = 0.25f), Offset(bx + bw * 0.3f, regalY - bh * 0.8f), Offset(bx + bw * 0.3f, regalY - bh * 0.3f), 1.5f)
        bx += bw + 2f
    }
    val topf = Offset(w * 0.19f, regalY)
    drawRect(Color(0xFFC0714F), Offset(topf.x - h * 0.025f, topf.y - h * 0.05f), Size(h * 0.05f, h * 0.05f))
    drawRect(Color(0xFF9A5A3E), Offset(topf.x + h * 0.025f, topf.y - h * 0.05f), Size(h * 0.008f, h * 0.05f))
    for (i in 0 until 5) {
        val wk = -60f + i * 30f + welle(t, zyklus, 9, i.toFloat()) * 5f
        rotate(wk, Offset(topf.x, topf.y - h * 0.05f)) { drawOval(Color(0xFF5E9E6B), Offset(topf.x - h * 0.012f, topf.y - h * 0.13f), Size(h * 0.024f, h * 0.08f)) }
    }
    val bild = Rect(w * 0.27f, h * 0.12f, w * 0.37f, h * 0.3f)
    drawRect(Color.Black.copy(alpha = 0.12f), bild.topLeft + Offset(4f, 5f), bild.size)
    drawRect(if (d) Color(0xFF4A3526) else Color(0xFFFFFFFF), bild.topLeft, bild.size)
    drawRect(Brush.verticalGradient(listOf(f.primaer.heller(0.4f), f.primaer)), bild.topLeft + Offset(4f, 4f), Size(bild.width - 8f, bild.height - 8f))
    drawPath(Path().apply { moveTo(bild.left + 4f, bild.bottom - 4f); lineTo(bild.left + bild.width * 0.4f, bild.top + bild.height * 0.45f); lineTo(bild.left + bild.width * 0.6f, bild.top + bild.height * 0.65f); lineTo(bild.left + bild.width * 0.75f, bild.top + bild.height * 0.5f); lineTo(bild.right - 4f, bild.bottom - 4f); close() }, f.primaer.dunkler(0.45f))
    // Wanduhr
    val uhr = Offset(w * 0.47f, h * 0.17f)
    drawCircle(Color.Black.copy(alpha = 0.12f), h * 0.07f, uhr + Offset(3f, 4f))
    drawCircle(if (d) Color(0xFF3A2A1F) else Color.White, h * 0.07f, uhr)
    drawCircle(f.primaer.dunkler(0.2f), h * 0.07f, uhr, style = Stroke(h * 0.01f))
    rotate(runde(t, zyklus, 4) * 360f, uhr) { drawLine(f.text, uhr, uhr + Offset(0f, -h * 0.055f), 2f) }
    rotate(runde(t, zyklus, 1) * 360f + 90f, uhr) { drawLine(f.text, uhr, uhr + Offset(0f, -h * 0.035f), 3.5f, StrokeCap.Round) }

    // Boden aus Dielen, die zum Fluchtpunkt laufen
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF241911), Color(0xFF33251A)) else listOf(Color(0xFFCFA277), Color(0xFFE2B88D)), wandFuss, h), Offset(0f, wandFuss), Size(w, h - wandFuss))
    for (i in -4..18) {
        val x = i * w / 14f
        drawLine(Color.Black.copy(alpha = 0.12f), hinten(Offset(x, boden), 0.2f), Offset(x + (x - flucht.x) * (h - boden) / (boden - flucht.y), h), 1.5f)
    }
    drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, wandFuss), Offset(w, wandFuss), 2f)
    // Teppich in Aufsicht
    drawOval(f.primaer.copy(alpha = 0.3f), Offset(w * 0.02f, boden - h * 0.015f), Size(w * 0.28f, h * 0.06f))
    drawOval(f.primaer.copy(alpha = 0.3f), Offset(w * 0.04f, boden - h * 0.005f), Size(w * 0.24f, h * 0.04f), style = Stroke(2f))

    // ---- Schreibtisch als Quader ----
    val sitzX = w * 0.4f
    val sitzHuefte = boden - 0.285f * s
    val tischY = sitzHuefte - 0.2f * s
    val tisch = Rect(sitzX + 0.14f * s, tischY, w * 0.8f, tischY + h * 0.035f)
    val holz = if (d) Color(0xFF5A3E2B) else Color(0xFFB07A4F)
    // Hintere Beine (kürzer, Richtung Fluchtpunkt), dann vordere
    for (x in listOf(tisch.left + h * 0.02f, tisch.right - h * 0.05f)) {
        val oben = hinten(Offset(x, tisch.bottom), 0.16f)
        drawRect(holz.dunkler(0.35f), oben, Size(h * 0.025f, hinten(Offset(x, boden), 0.16f).y - oben.y))
    }
    drawOval(Color.Black.copy(alpha = 0.14f), Offset(tisch.left, boden - h * 0.02f), Size(tisch.width, h * 0.04f))
    drawRect(holz.dunkler(0.2f), Offset(tisch.left + h * 0.02f, tisch.bottom), Size(h * 0.03f, boden - tisch.bottom))
    drawRect(holz.dunkler(0.2f), Offset(tisch.right - h * 0.05f, tisch.bottom), Size(h * 0.03f, boden - tisch.bottom))
    drawRect(holz.dunkler(0.1f), Offset(tisch.right - w * 0.12f, tisch.bottom), Size(w * 0.1f, h * 0.12f))
    drawCircle(Color(0xFFE8C9A0), h * 0.008f, Offset(tisch.right - w * 0.07f, tisch.bottom + h * 0.06f))
    quader(tisch, 0.16f, flucht, holz)
    drawRect(holz.heller(0.25f), tisch.topLeft, Size(tisch.width, h * 0.005f))

    // Bildschirm: schräg gestellt, die rechte Kante kommt nach vorne. Inhalt flach gezeichnet und perspektivisch verzerrt.
    val monL = w * 0.49f
    val monR = w * 0.66f
    val ecken = listOf(
        Offset(monL, tischY - h * 0.32f), Offset(monR, tischY - h * 0.37f),
        Offset(monR, tischY - h * 0.05f), Offset(monL, tischY - h * 0.09f),
    )
    val fussMitte = Offset((monL + monR) / 2f, hinten(Offset((monL + monR) / 2f, tischY), 0.08f).y)
    drawOval(Color(0xFF2A2522), Offset(fussMitte.x - h * 0.06f, fussMitte.y - h * 0.012f), Size(h * 0.12f, h * 0.024f))
    drawRect(Color(0xFF2A2522), Offset(fussMitte.x - h * 0.012f, tischY - h * 0.1f), Size(h * 0.024f, fussMitte.y - tischY + h * 0.09f))
    // Flaches Display ohne Gehäusetiefe: nur der abgerundete schwarze Rahmen
    val flach = Rect(0f, 0f, monR - monL, h * 0.29f)
    val schirmAus = maxOf(1f - an(t, 8.2f, 0.6f), an(t, 38.6f, 0.5f))
    verzerrt(perspektive(flach, ecken)) {
        drawRoundRect(Color(0xFF1E1A18), flach.topLeft, flach.size, CornerRadius(8f))
        val bild2 = Rect(flach.left + 5f, flach.top + 5f, flach.right - 5f, flach.bottom - 5f)
        drawRoundRect(if (d) Color(0xFF191410) else Color(0xFFFFFAF4), bild2.topLeft, bild2.size, CornerRadius(5f))
        drawRect(f.primaer, bild2.topLeft, Size(bild2.width, h * 0.03f))
        for (i in 0 until 3) drawCircle(Color.White.copy(alpha = 0.8f), h * 0.006f, Offset(bild2.left + h * (0.015f + i * 0.018f), bild2.top + h * 0.015f))
        val haken = floatArrayOf(15.2f, 16.0f, 31.4f, 32.6f)
        for (i in 0 until 4) {
            val y = bild2.top + h * (0.07f + i * 0.047f)
            val da = an(t, 10.2f + i * 1.4f, 0.9f)
            if (da <= 0f) continue
            val k = h * 0.024f
            drawRoundRect(f.primaer.copy(alpha = weich(an(da, 0f, 0.3f))), Offset(bild2.left + h * 0.015f, y - k / 2), Size(k, k), CornerRadius(3f), style = Stroke(2f))
            kringel(Offset(bild2.left + h * 0.055f, y), bild2.width * (0.6f - (i % 2) * 0.12f), h * 0.02f, da, f.textLeise, 2f, i)
            haken(Offset(bild2.left + h * 0.015f + k / 2, y), k, an(t, haken[i], 0.4f), f.primaer, 2.6f)
        }
        // Spiegelung auf dem Glas
        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), bild2.topLeft, Offset(bild2.right, bild2.bottom * 0.6f)), bild2.topLeft, bild2.size, CornerRadius(5f))
        if (schirmAus > 0f) drawRoundRect(Color(0xFF0E0B0A).copy(alpha = schirmAus), bild2.topLeft, bild2.size, CornerRadius(5f))
    }
    // Schein des Bildschirms auf der Tischplatte
    drawOval(Brush.radialGradient(listOf(f.primaer.copy(alpha = 0.18f * (1f - schirmAus)), Color.Transparent), Offset(monL, tischY - h * 0.01f), h * 0.12f), Offset(monL - h * 0.12f, tischY - h * 0.04f), Size(h * 0.24f, h * 0.06f))

    // Tastatur flach auf der Platte
    val tl = Offset(sitzX + 0.2f * s, tischY - h * 0.004f)
    val tr = Offset(sitzX + 0.34f * s, tischY - h * 0.004f)
    flaeche(listOf(tl, tr, hinten(tr, 0.05f), hinten(tl, 0.05f)), Color(0xFF3A3431))
    drawLine(Color(0xFF55504C), tl, tr, 2f)
    // Lampenschalter auf der Platte, Kabel zur Lampe
    val schalter = Offset(sitzX + 0.27f * s, hinten(Offset(sitzX + 0.27f * s, tischY), 0.12f).y)
    val lFuss = Offset(w * 0.725f, hinten(Offset(w * 0.725f, tischY), 0.1f).y)
    drawPath(Path().apply { moveTo(schalter.x, schalter.y); quadraticTo((schalter.x + lFuss.x) / 2f, tischY - h * 0.006f, lFuss.x - h * 0.02f, lFuss.y) }, Color(0xFF2A2522), style = Stroke(1.5f))
    drawRoundRect(Color(0xFF3A3431), Offset(schalter.x - h * 0.016f, schalter.y - h * 0.008f), Size(h * 0.032f, h * 0.012f), CornerRadius(4f))
    drawCircle(lerp(Color(0xFF6B5E55), Color(0xFFFFC266), lampeAn), h * 0.004f, schalter + Offset(0f, -h * 0.002f))

    // Lampe
    val gelenk = Offset(w * 0.765f, h * 0.27f)
    val schirm = Offset(w * 0.708f, h * 0.31f)
    if (lampeAn > 0f) drawPath(
        Path().apply { moveTo(schirm.x - h * 0.03f, schirm.y); lineTo(schirm.x - h * 0.15f, tischY); lineTo(schirm.x + h * 0.12f, tischY); lineTo(schirm.x + h * 0.04f, schirm.y); close() },
        Brush.verticalGradient(listOf(Color(0xFFFFD08A).copy(alpha = 0.45f * lampeAn), Color(0xFFFFD08A).copy(alpha = 0.05f * lampeAn)), schirm.y, tischY),
    )
    if (lampeAn > 0f) drawOval(Color(0xFFFFD08A).copy(alpha = 0.25f * lampeAn), Offset(schirm.x - h * 0.15f, tischY - h * 0.03f), Size(h * 0.27f, h * 0.03f))
    drawOval(Color(0xFF3A3431), Offset(lFuss.x - h * 0.035f, lFuss.y - h * 0.01f), Size(h * 0.07f, h * 0.018f))
    drawLine(Color(0xFF3A3431), lFuss, gelenk, 4f, StrokeCap.Round)
    drawLine(Color(0xFF3A3431), gelenk, schirm, 4f, StrokeCap.Round)
    drawPath(Path().apply { moveTo(schirm.x - h * 0.045f, schirm.y + h * 0.02f); lineTo(schirm.x - h * 0.01f, schirm.y - h * 0.04f); lineTo(schirm.x + h * 0.04f, schirm.y - h * 0.02f); lineTo(schirm.x + h * 0.05f, schirm.y + h * 0.03f); close() }, Brush.linearGradient(listOf(f.primaer.heller(0.1f), f.primaer.dunkler(0.3f)), schirm - Offset(h * 0.04f, h * 0.04f), schirm + Offset(h * 0.05f, h * 0.03f)))
    if (lampeAn > 0f) drawCircle(Color(0xFFFFE6B0).copy(alpha = lampeAn), h * 0.014f, schirm + Offset(0f, h * 0.02f))

    // Stuhl mit Schatten
    val stuhl = if (d) Color(0xFF3A3431) else Color(0xFF4A4340)
    drawOval(Color.Black.copy(alpha = 0.15f), Offset(sitzX - 0.16f * s, boden - h * 0.02f), Size(0.32f * s, h * 0.035f))
    drawRoundRect(stuhl.heller(0.12f), Offset(sitzX - 0.12f * s, sitzHuefte + 0.015f * s), Size(0.26f * s, 0.03f * s), CornerRadius(6f))
    drawRoundRect(stuhl, Offset(sitzX - 0.12f * s, sitzHuefte + 0.03f * s), Size(0.26f * s, 0.05f * s), CornerRadius(6f))
    drawRoundRect(stuhl, Offset(sitzX - 0.16f * s, sitzHuefte - 0.3f * s), Size(0.06f * s, 0.36f * s), CornerRadius(8f))
    drawLine(stuhl, Offset(sitzX, sitzHuefte + 0.08f * s), Offset(sitzX, boden - h * 0.03f), 5f)
    drawLine(stuhl, Offset(sitzX - 0.12f * s, boden - h * 0.02f), Offset(sitzX + 0.12f * s, boden - h * 0.02f), 5f, StrokeCap.Round)
    for (r in listOf(-0.12f, 0.12f)) drawCircle(Color(0xFF2A2522), h * 0.013f, Offset(sitzX + r * s, boden - h * 0.01f))

    // ---- Person: jede Haltung gleitet weich in die nächste ----
    val mf = MenschFarben(Color(0xFFE8C09B), Color(0xFF3B2A20), lerp(f.primaer, Color(0xFF7A6A60), 0.25f), Color(0xFF363A42), Color(0xFF2A2522))
    val blink = blinzelt(t)
    val standHuefte = boden - 0.49f * s - 0.035f * s
    val sitz = sitzen(sitzX, sitzHuefte, 1f).copy(blinzeln = blink)
    fun tippen(lachen: Float = 0.35f) = sitz.copy(sR = 62f + sin(t * 17f) * 4f, eR = 30f, sL = 58f + sin(t * 17f + 1.5f) * 4f, eL = 32f, kopf = sin(t * 1.3f) * 3f, lachen = lachen)
    val greifen = sitz.copy(sR = 75f, eR = 8f, sL = 58f, eL = 32f, lean = 2f)
    val pose = choreo(t, s, listOf(
        // Im Dunkeln draußen links, dann herein zum Stuhl
        Takt(0f) { gehen(mix(-0.12f * w, sitzX, an(it, 3f, 4.4f)), boden, 1f, it * 8.5f, blink) },
        Takt(7.4f) { val k = weich(an(it, 7.4f, 1.2f)); Pose(sitzX, boden, 1f, lean = mix(0f, -4f, k), hL = 84f * k, kL = 84f * k, hR = 88f * k, kR = 88f * k, hueftY = mix(standHuefte, sitzHuefte, k), blinzeln = blink) },
        Takt(8.8f) { tippen() },
        // Es dämmert: Lampe am Tischschalter einschalten
        Takt(16.5f) { greifen },
        Takt(17.3f) { tippen() },
        // Kaffee: greifen, trinken, zurückstellen
        Takt(18.3f) { greifen },
        Takt(18.9f) { sitz.copy(sR = 18f, eR = 140f, sL = 58f, eL = 32f, kopf = -8f, lachen = 0.1f) },
        Takt(19.8f) { greifen },
        Takt(20.4f) { tippen() },
        // Katze auf dem Schoß: streicheln
        Takt(23.8f) { sitz.copy(sR = 28f + sin(it * 4f) * 10f, eR = 45f, sL = 58f, eL = 32f, kopf = 12f, lachen = 0.9f) },
        Takt(30.0f) { tippen(0.6f) },
        // Feierabend: aufstehen, freuen, Lampe aus, mit der Katze rechts hinaus
        Takt(34.0f) { val k = 1f - weich(an(it, 34f, 1.2f)); Pose(sitzX, boden, 1f, hL = 84f * k, kL = 84f * k, hR = 88f * k, kR = 88f * k, hueftY = mix(standHuefte, sitzHuefte, k), blinzeln = blink) },
        Takt(35.3f) { Pose(sitzX, boden - huepfen(it, 35.6f, 38f, 0.06f * s), 1f, sL = 168f, eL = 6f, sR = 172f, eR = 6f, lachen = 1f, blinzeln = blink) },
        Takt(38.0f) { Pose(sitzX, boden, 1f, lean = 30f, sR = 20f, eR = 10f, sL = 10f, eL = 10f, lachen = 0.7f, blinzeln = blink) },
        Takt(39.0f) { gehen(mix(sitzX, -0.15f * w, an(it, 39f, 4.4f)), boden, -1f, it * 8.5f, blink) },
    ))

    // Tasse: steht hinten auf der Platte, beim Trinken in der Hand
    val tasseRuhe = Offset(sitzX + 0.165f * h, hinten(Offset(sitzX + 0.165f * h, tischY), 0.1f).y)
    val tasseInHand = t in 18.75f..20.25f
    fun tasse(boden: Offset) {
        val c = Color.White.copy(alpha = if (d) 0.85f else 1f)
        drawRoundRect(c, Offset(boden.x - h * 0.02f, boden.y - h * 0.05f), Size(h * 0.04f, h * 0.05f), CornerRadius(4f))
        drawOval(Color(0xFF6B3F22), Offset(boden.x - h * 0.018f, boden.y - h * 0.054f), Size(h * 0.036f, h * 0.01f))
        drawArc(c, -90f, 180f, false, Offset(boden.x + h * 0.01f, boden.y - h * 0.04f), Size(h * 0.025f, h * 0.025f), style = Stroke(3f))
    }
    if (!tasseInHand) {
        tasse(tasseRuhe)
        for (i in 0..1) {
            val q = runde(t, zyklus, 18, i * 0.5f)
            drawLine(Color.White.copy(alpha = 0.35f * (1f - q)), Offset(tasseRuhe.x + h * (-0.008f + i * 0.015f) + sin(q * 6f) * 3f, tasseRuhe.y - h * 0.06f - q * h * 0.08f), Offset(tasseRuhe.x + h * (-0.008f + i * 0.015f) + sin(q * 6f + 1f) * 3f, tasseRuhe.y - h * 0.08f - q * h * 0.08f), 2f, StrokeCap.Round)
        }
    }

    // ---- Katze: kommt mit herein, schläft auf dem Teppich, springt auf den Schoß, sitzt am Fenster, geht mit hinaus ----
    val katzeF = KatzenFarben(Color(0xFFE39A52), Color(0xFFB8672A), Color(0xFFF7E1C4))
    val katzeAufSchoss = t in 23.95f..30.05f
    val schossX = sitzX + 0.14f * s
    val schossBoden = sitzHuefte - 0.04f * s
    val fensterX = fen.center.x - h * 0.05f
    val brettX = fensterX + h * 0.08f
    fun zeichneKatze() {
        when {
            t < 3f -> Unit
            t < 6.6f -> katze(mix(-0.22f * w, w * 0.14f, weich(an(t, 3f, 3.6f))), boden, cs, 1f, 0, t * 11f, t, katzeF)
            t < 7.4f -> katze(w * 0.14f, boden, cs, 1f, 1, 0f, t, katzeF)
            t < 20f -> katze(w * 0.14f, boden, cs, 1f, 2, 0f, t, katzeF)
            t < 21.4f -> katze(w * 0.14f, boden, cs, 1f, 3, 0f, t, katzeF)
            t < 23.4f -> katze(mix(w * 0.14f, sitzX + 0.36f * s, weich(an(t, 21.4f, 2f))), boden, cs, mix(1f, -1f, weich(an(t, 23.05f, 0.35f))), 0, t * 11f, t, katzeF)
            t < 24f -> { val k = an(t, 23.4f, 0.6f); katze(mix(sitzX + 0.36f * s, schossX, k), boden, cs, -1f, 3, 0f, t, katzeF, hoehe = sin(k * 3.14f) * h * 0.25f + k * (boden - schossBoden)) }
            t < 30f -> katze(schossX, schossBoden, cs, mix(-1f, 1f, weich(an(t, 29.6f, 0.4f))), 2, 0f, t, katzeF)
            t < 30.6f -> { val k = an(t, 30f, 0.6f); katze(mix(schossX, sitzX + 0.45f * s, k), boden, cs, 1f, 3, 0f, t, katzeF, hoehe = (1f - k) * (boden - schossBoden) + sin(k * 3.14f) * h * 0.1f) }
            t < 32.6f -> katze(mix(sitzX + 0.45f * s, fensterX, weich(an(t, 30.6f, 2f))), boden, cs, 1f, 0, t * 11f, t, katzeF)
            t < 33.1f -> { val k = an(t, 32.6f, 0.5f); katze(fensterX + k * h * 0.08f, boden, cs, 1f, 3, 0f, t, katzeF, hoehe = k * (boden - fen.bottom) + sin(k * 3.14f) * h * 0.08f) }
            // Sitzt am Fenster, dreht sich zum Gehen um, springt herunter und läuft der Person nach links hinterher
            t < 38.4f -> katze(brettX, fen.bottom, cs, mix(1f, -1f, weich(an(t, 37.9f, 0.5f))), 1, 0f, t, katzeF)
            t < 38.9f -> { val k = an(t, 38.4f, 0.5f); katze(brettX - k * h * 0.1f, boden, cs, -1f, 3, 0f, t, katzeF, hoehe = (1f - k) * (boden - fen.bottom) + sin(k * 3.14f) * h * 0.05f) }
            else -> katze(mix(brettX - h * 0.1f, -0.25f * w, an(t, 38.9f, 4.8f)), boden, cs, -1f, 0, t * 11f, t, katzeF)
        }
    }
    if (!katzeAufSchoss) zeichneKatze()
    val punkte = mensch(pose, s, mf)
    if (katzeAufSchoss) {
        zeichneKatze()
        // Die streichelnde Hand liegt auf der Katze
        rechterArm(pose, s, mf, punkte.schulter)
        for (i in 0..2) {
            val q = ((t - 24.4f) * 0.6f + i / 3f) % 1f
            val ein = weich(an(t, 24.4f, 0.8f)) * (1f - weich(an(t, 29.4f, 0.6f)))
            if (t > 24.4f) herz(Offset(sitzX + 0.2f * s + sin(q * 6f + i) * 8f, sitzHuefte - 0.2f * s - q * h * 0.35f), h * 0.03f * (0.6f + q * 0.5f), f.primaer.copy(alpha = (1f - q) * ein))
        }
    }
    if (tasseInHand) tasse(punkte.handR + Offset(-h * 0.005f, h * 0.03f))

    // Nacht und Dämmerung dunkeln den Raum ab, das Lampenlicht hebt es teilweise wieder auf
    drawRect(Color(0xFF1A0C20).copy(alpha = (if (d) 0.4f else 0.34f) * weich(an(abend, 0.55f, 0.45f)) * (1f - 0.6f * lampeAn)))
}
