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

/** Länge eines Durchlaufs; Ende und Anfang sind dasselbe Bild (Nacht, leere Beete), die Szene läuft ohne Schnitt. */
internal const val GARTEN_ZYKLUS = 48f

/**
 * Garten: weite Landschaft mit Tiefe (Hügel, Weg zum Horizont, Zaun). Nachts kommen Person und Katze links herein, die
 * Sonne geht auf. Die Person gießt auf dem Gartenweg die drei Hochbeete, stellt die Kanne ab, schaut beim Wachsen zu,
 * erntet Beet für Beet in einen Strauß und hält ihn der Katze vor die Nase; die Katze rückt heran und schnuppert. Dann
 * nimmt die Person die Kanne und geht mit der Katze links hinaus, wo sie hereingekommen sind; die Sonne geht unter, Sterne
 * funkeln, der Mond zieht über den Himmel, und nach einer Pause beginnt alles neu.
 */
internal fun DrawScope.szeneGarten(t: Float, f: Farben) {
    val zyklus = GARTEN_ZYKLUS
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.93f
    val s = h * 0.56f
    val horizont = h * 0.5f
    val flucht = Offset(w * 0.5f, h * 0.42f)
    val gruen = f.primaer
    // Tageslicht: Sonnenaufgang beim Hereinkommen, Sonnenuntergang beim Hinausgehen (1 = Tag, 0 = Nacht)
    val tag = weich(an(t, 2.5f, 4f)) * (1f - weich(an(t, 39f, 4f)))
    val daemmer = 4f * tag * (1f - tag)

    // Himmel mit Morgen- und Abendrot, Sterne in der Nacht, Sonne im Bogen von links nach rechts
    val obenTag = if (d) Color(0xFF0C1A22) else Color(0xFFCFE8F2)
    val untenTag = if (d) Color(0xFF1B3328) else Color(0xFFF2F4E4)
    val himmelOben = lerp(lerp(Color(0xFF070D1C), obenTag, tag), Color(0xFF6B4A8C), daemmer * 0.35f)
    val himmelUnten = lerp(lerp(Color(0xFF16213A), untenTag, tag), Color(0xFFFF9E5E), daemmer * 0.55f)
    drawRect(Brush.verticalGradient(listOf(himmelOben, himmelUnten), 0f, horizont + h * 0.1f))
    // Viele funkelnde Sterne, jeder in eigenem Takt
    if (tag < 1f) for (i in 0 until 70) {
        val funkeln = 0.5f + 0.5f * welle(t, zyklus, 24 + (i % 9) * 5, i * 1.7f)
        drawCircle(Color.White.copy(alpha = (1f - tag) * (0.25f + 0.7f * funkeln)), 1f + (i % 4) * 0.45f + funkeln * 0.6f, Offset(w * ((i * 0.37f + i * i * 0.013f) % 1f), h * 0.46f * ((i * 0.53f + i * 0.071f) % 1f)))
    }
    // Mond: geht nach Sonnenuntergang links auf, wandert über den Himmel und geht vor Sonnenaufgang rechts unter
    val mondLauf = ((t - 40f + zyklus) % zyklus) / 13f
    if (mondLauf < 1f) {
        val mondSicht = (sin(mondLauf * 3.14f) * 3f).coerceIn(0f, 1f) * (1f - tag)
        val mond = Offset(w * (0.1f + 0.8f * mondLauf), horizont + h * 0.04f - sin(mondLauf * 3.14f) * h * 0.36f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6DC).copy(alpha = 0.4f * mondSicht), Color.Transparent), mond, h * 0.14f), h * 0.14f, mond)
        drawCircle(Color(0xFFF4EFD9).copy(alpha = mondSicht), h * 0.042f, mond)
        drawCircle(Color(0xFFDCD5BC).copy(alpha = mondSicht), h * 0.01f, mond + Offset(-h * 0.012f, -h * 0.01f))
        drawCircle(Color(0xFFDCD5BC).copy(alpha = mondSicht), h * 0.007f, mond + Offset(h * 0.014f, h * 0.012f))
    }
    val bogen = an(t, 2.5f, 38.5f)
    val sonne = Offset(w * (0.06f + 0.88f * bogen), horizont + h * 0.06f - sin(bogen * 3.14f) * h * 0.44f)
    val sonnenFarbe = lerp(Color(0xFFFF9A4D), if (d) Color(0xFFEFE6C8) else Color(0xFFFFD27A), (sin(bogen * 3.14f) * 1.6f).coerceIn(0f, 1f))
    // Am Horizont blendet die Sonne ganz aus, damit beim Neustart kein Schein von rechts nach links springt
    val sonnenSicht = (sin(bogen * 3.14f) * 3f).coerceIn(0f, 1f)
    if (sonnenSicht > 0f) {
        drawCircle(Brush.radialGradient(listOf(sonnenFarbe.copy(alpha = 0.55f * sonnenSicht), Color.Transparent), sonne, h * 0.2f), h * 0.2f, sonne)
        drawCircle(sonnenFarbe.copy(alpha = sonnenSicht), h * 0.05f, sonne)
    }
    // Wolken nur am Tag (nachts wirken ihre Umrisse wie Ufos)
    if (tag > 0f) for (i in 0 until 3) {
        val wx = runde(t, zyklus, 1, i * 0.33f) * w * 1.3f - w * 0.15f
        val wy = h * (0.1f + i * 0.08f)
        val wf = Color.White.copy(alpha = (if (d) 0.15f else 0.85f) * weich(an(tag, 0.55f, 0.45f)))
        drawOval(wf, Offset(wx, wy), Size(h * 0.2f, h * 0.06f))
        drawOval(wf, Offset(wx + h * 0.05f, wy - h * 0.035f), Size(h * 0.12f, h * 0.07f))
    }
    // Vögel am Tag
    val vogelX = runde(t, zyklus, 2) * w * 1.6f - w * 0.3f
    for (i in 0 until 3) {
        val v = Offset(vogelX - i * h * 0.07f, h * (0.15f + i * 0.03f))
        val flug = welle(t, zyklus, 64, i.toFloat()) * h * 0.012f
        val p = Path().apply { moveTo(v.x - h * 0.025f, v.y - flug); quadraticTo(v.x - h * 0.01f, v.y - h * 0.01f, v.x, v.y); quadraticTo(v.x + h * 0.01f, v.y - h * 0.01f, v.x + h * 0.025f, v.y - flug) }
        drawPath(p, f.text.copy(alpha = 0.55f * tag), style = Stroke(1.8f, cap = StrokeCap.Round))
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
    drawOval(Color.Black.copy(alpha = 0.12f * tag), Offset(stamm.x - h * 0.16f, stamm.y - h * 0.02f), Size(h * 0.36f, h * 0.05f))
    drawLine(Color(0xFF7A5A3E), stamm, stamm - Offset(0f, h * 0.42f), h * 0.04f, StrokeCap.Round)
    drawLine(Color(0xFF5E4430), stamm + Offset(h * 0.01f, 0f), stamm - Offset(-h * 0.01f, h * 0.42f), h * 0.012f, StrokeCap.Round)
    rotate(welle(t, zyklus, 6) * 2f, stamm) {
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
    val giessStart = listOf(6f, 11.6f, 17.2f)
    val giessDauer = 3.6f
    val ernteStart = listOf(26.6f, 29.2f, 31.8f)
    val holz = if (d) Color(0xFF6B4E36) else Color(0xFF9C7350)
    val erde = Color(0xFF5C3F2B)
    val erdeY = beete.map { bx ->
        val oben = quader(Rect(bx - h * 0.12f, h * 0.75f, bx + h * 0.12f, h * 0.81f), 0.2f, flucht, holz)
        val mitteOben = Offset((oben[0].x + oben[1].x + oben[2].x + oben[3].x) / 4f, (oben[0].y + oben[1].y + oben[2].y + oben[3].y) / 4f)
        flaeche(oben.map { it + (mitteOben - it) * 0.14f }, erde)
        drawLine(holz.dunkler(0.2f), Offset(bx - h * 0.12f, h * 0.78f), Offset(bx + h * 0.12f, h * 0.78f), 1.5f)
        mitteOben.y
    }
    var geerntet = 0
    beete.forEachIndexed { i, bx ->
        val boden0 = erdeY[i]
        val wachsen = weich(an(t, giessStart[i] + giessDauer, 3.5f))
        val bluete = weich(an(t, giessStart[i] + giessDauer + 3.4f, 1f))
        if (wachsen > 0f) for (j in -1..1) {
            // Beim Ernten wandert jede Pflanze in den Strauß
            val ernte = ernteStart[i] + (j + 1) * 0.4f
            val bleibt = 1f - weich(an(t, ernte, 0.3f))
            if (t >= ernte + 0.3f) geerntet++
            if (bleibt <= 0f) continue
            val px = bx + j * h * 0.06f
            val hoehe = h * (0.12f + 0.06f * (j + 1) % 2) * wachsen * bleibt
            val wind = welle(t, zyklus, 11, (j + i).toFloat()) * 3f
            drawLine(Color(0xFF3D8A55), Offset(px, boden0), Offset(px + wind, boden0 - hoehe), 2.5f, StrokeCap.Round)
            if (wachsen > 0.4f) for (b in 0..1) {
                val by = boden0 - hoehe * (0.35f + b * 0.3f)
                val seite = if ((b + j) % 2 == 0) 1f else -1f
                drawOval(Color(0xFF4FA768), Offset(px + wind * 0.5f + (if (seite > 0) 0f else -h * 0.035f), by - h * 0.01f), Size(h * 0.035f * bleibt, h * 0.018f * bleibt))
            }
            if (bluete > 0f) bluetenKopf(Offset(px + wind, boden0 - hoehe), h, bluete * bleibt, if (j == 0) Color(0xFFFFF4E0) else lerp(gruen, Color.White, 0.75f), t)
        }
    }
    // Gras an der Wegkante
    for (i in 0 until 40) {
        val gx = i * w / 40f + (i % 3) * 4f
        val wind = welle(t, zyklus, 13, i * 0.4f) * 3f
        drawLine(lerp(gruen, Color.Black, if (d) 0.3f else 0.05f), Offset(gx, h * 0.865f), Offset(gx + wind, h * 0.865f - h * (0.02f + (i % 4) * 0.008f)), 2f, StrokeCap.Round)
    }

    // Schmetterlinge am Tag: der erste taucht für die Katze tief und entwischt nach oben
    val frei0 = Offset(w * (0.32f + 0.12f * welle(t, zyklus, 3) + 0.04f * welle(t, zyklus, 9)), h * (0.5f + 0.08f * welle(t, zyklus, 6) + 0.03f * welle(t, zyklus, 16)))
    val tief = weich(an(t, 14f, 1.8f)) * (1f - weich(an(t, 16.2f, 1.4f)))
    val flucht0 = weich(an(t, 16.1f, 1f)) * (1f - weich(an(t, 19f, 3f)))
    val falter = listOf(
        lerp(frei0, Offset(w * 0.28f, h * 0.72f), tief) - Offset(0f, h * 0.18f * flucht0),
        Offset(w * (0.6f + 0.25f * welle(t, zyklus, 2, 2f)), h * (0.45f + 0.12f * welle(t, zyklus, 6, 1f))),
    )

    // Person: Haltungen gleiten weich ineinander
    val mf = MenschFarben(Color(0xFFE6BE98), Color(0xFF6B4A2E), lerp(f.primaer, Color(0xFFD9D2C0), 0.35f), Color(0xFF5A5344), Color(0xFF3A3024))
    val blink = blinzelt(t)
    val stehX = beete.map { it - h * 0.26f }
    val pflueckX = beete.map { it - h * 0.2f }
    val tragen = { x: Float, z: Float, dir: Float -> gehen(x, boden, dir, z * 8.5f, blink).copy(sR = 20f, eR = 10f) }
    val giessen = { i: Int -> Pose(stehX[i], boden, 1f, lean = 8f, sR = 70f, eR = 10f, blinzeln = blink) }
    val pfluecken = { i: Int, z: Float -> Pose(pflueckX[i], boden, 1f, lean = 14f, sR = 72f + sin(z * 5f) * 6f, eR = 8f, blinzeln = blink) }
    val hocke = { x: Float, dir: Float, sR: Float -> Pose(x, boden, dir, lean = 25f, hL = 70f, kL = 120f, hR = 60f, kR = 110f, sR = sR, eR = 10f, sL = 0f, eL = 10f, blinzeln = blink) }
    val geben = hocke(pflueckX[2], -1f, 15f).copy(lean = 30f, lachen = 0.9f)
    val pose = choreo(t, s, listOf(
        // Im Dunkeln links draußen, dann mit der Kanne herein zum ersten Beet
        Takt(0f) { tragen(mix(-0.12f * w, stehX[0], weich(an(it, 2.5f, 3.5f))), it, 1f) },
        Takt(giessStart[0]) { giessen(0) },
        Takt(giessStart[0] + giessDauer) { tragen(mix(stehX[0], stehX[1], an(it, giessStart[0] + giessDauer, giessStart[1] - giessStart[0] - giessDauer)), it, 1f) },
        Takt(giessStart[1]) { giessen(1) },
        Takt(giessStart[1] + giessDauer) { tragen(mix(stehX[1], stehX[2], an(it, giessStart[1] + giessDauer, giessStart[2] - giessStart[1] - giessDauer)), it, 1f) },
        Takt(giessStart[2]) { giessen(2) },
        // Kanne abstellen, zum ersten Beet zurück, beim Wachsen zuschauen
        Takt(20.8f) { hocke(stehX[2], 1f, 10f) },
        Takt(22.2f) { gehen(mix(stehX[2], pflueckX[0], an(it, 22.2f, 2.6f)), boden, -1f, it * 8.5f, blink) },
        Takt(24.8f) { Pose(pflueckX[0], boden, 1f, sL = -30f, eL = 75f, sR = -30f, eR = 75f, kopf = sin(it * 1.2f) * 4f, lachen = 0.6f, blinzeln = blink) },
        // Ernten, Beet für Beet
        Takt(ernteStart[0] - 0.2f) { pfluecken(0, it) },
        Takt(ernteStart[0] + 1.2f) { gehen(mix(pflueckX[0], pflueckX[1], an(it, ernteStart[0] + 1.2f, 1.2f)), boden, 1f, it * 8.5f, blink) },
        Takt(ernteStart[1] - 0.2f) { pfluecken(1, it) },
        Takt(ernteStart[1] + 1.2f) { gehen(mix(pflueckX[1], pflueckX[2], an(it, ernteStart[1] + 1.2f, 1.2f)), boden, 1f, it * 8.5f, blink) },
        Takt(ernteStart[2] - 0.2f) { pfluecken(2, it) },
        // Zur Katze umdrehen, in die Hocke, den Strauß vor ihre Nase halten
        Takt(33f) { geben },
        // Aufstehen, Kanne nehmen, mit der Katze links hinaus, wo sie hereingekommen sind
        Takt(36.4f) { Pose(pflueckX[2], boden, 1f, lachen = 0.8f, blinzeln = blink) },
        Takt(37f) { hocke(pflueckX[2], 1f, 30f) },
        Takt(38f) { tragen(mix(pflueckX[2], -0.15f * w, an(it, 38f, 6.6f)), it, -1f) },
    ))

    // Katze: kommt mit, sitzt unter dem Baum, lauert dem Falter auf, springt einmal, kommt zum Strauß, schnuppert, geht mit
    val katzeF = KatzenFarben(Color(0xFF9C8B7A), Color(0xFF5E5146), Color(0xFFEDE3D6))
    val cs = h * 0.17f
    val strauss = geben.handR(s) + Offset(-h * 0.05f, h * 0.005f)
    val schnupperX = strauss.x - h * 0.006f - 0.26f * cs
    val wartX = schnupperX - h * 0.06f
    when {
        t < 2.5f -> Unit
        t < 5.5f -> katze(mix(-0.25f * w, w * 0.17f, weich(an(t, 2.5f, 3f))), boden, cs, 1f, 0, t * 10f, t, katzeF)
        t < 14.4f -> katze(w * 0.17f, boden, cs, 1f, 1, 0f, t, katzeF, pfote = maxOf(0f, sin((t - 8f) * 3f)) * 0.6f * weich(an(t, 8f, 0.4f)) * (1f - weich(an(t, 10.5f, 0.4f))))
        t < 15.8f -> katze(w * 0.17f, boden, cs, 1f, 3, 0f, t, katzeF)
        t < 16.5f -> { val q = an(t, 15.8f, 0.7f); katze(mix(w * 0.17f, w * 0.27f, weich(q)), boden, cs, 1f, 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.22f) }
        t < 31f -> katze(w * 0.27f, boden, cs, 1f, 1, 0f, t, katzeF)
        t < 33.6f -> katze(mix(w * 0.27f, wartX, weich(an(t, 31f, 2.6f))), boden, cs, 1f, 0, t * 10f, t, katzeF)
        t < 34.4f -> katze(wartX, boden, cs, 1f, 1, 0f, t, katzeF)
        // Ein kleines Stück näher an den Strauß, dann schnuppern
        t < 34.9f -> katze(mix(wartX, schnupperX, weich(an(t, 34.4f, 0.5f))), boden, cs, 1f, 0, t * 6f, t, katzeF)
        t < 36.4f -> katze(schnupperX + sin(t * 16f) * h * 0.004f * weich(an(t, 34.9f, 0.2f)) * (1f - weich(an(t, 36.1f, 0.3f))), boden, cs, 1f, 1, 0f, t, katzeF)
        t < 38.6f -> katze(schnupperX, boden, cs, mix(1f, -1f, weich(an(t, 38.1f, 0.5f))), 1, 0f, t, katzeF)
        else -> katze(mix(schnupperX, -0.25f * w, an(t, 38.6f, 5.2f)), boden, cs, -1f, 0, t * 10f, t, katzeF)
    }

    val punkte = mensch(pose, s, mf)

    // Gießkanne: in der Hand, beim Gießen geneigt, dann abgestellt und zum Gehen wieder aufgenommen
    val kanneBoden = Offset(stehX[2] + h * 0.16f, boden - h * 0.04f)
    val inHand = punkte.handR + Offset(0f, h * 0.05f)
    val kanne = when {
        t < 21.2f -> inHand
        t < 37.4f -> lerp(inHand, kanneBoden, weich(an(t, 21.2f, 0.6f)))
        else -> lerp(kanneBoden, inHand, weich(an(t, 37.4f, 0.4f)))
    }
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
    // Strauß: wächst mit jeder geernteten Pflanze; beim Geben nach vorn gehalten, beim Gehen in der linken Hand
    if (geerntet > 0) {
        val dir = pose.dir
        val hand = if (t < 37f) punkte.handR else punkte.handL
        val vorn = if (t in 33f..36.4f) Offset(-h * 0.05f, h * 0.005f) else Offset(dir * h * 0.01f, -h * 0.05f)
        val kopf = hand + vorn
        drawLine(Color(0xFF3D8A55), hand, kopf, 3f, StrokeCap.Round)
        for (n in 0 until geerntet) {
            val wk = n * 137.5f
            val r = h * 0.012f * kotlin.math.sqrt(n.toFloat())
            bluetenKopf(kopf + Offset(cos(rad(wk)) * r, sin(rad(wk)) * r), h, 0.7f, if (n % 3 == 1) Color(0xFFFFF4E0) else lerp(gruen, Color.White, 0.75f), t)
        }
    }

    // Schmetterlinge zuletzt, sie fliegen vor allem
    if (tag > 0f) falter.forEachIndexed { i, p ->
        val schlag = kotlin.math.abs(sin(t * 12f + i)) * 0.8f + 0.2f
        val farbe = (if (i == 0) lerp(f.primaer, Color.White, 0.5f) else Color(0xFFF2C14E)).copy(alpha = tag)
        drawOval(farbe, Offset(p.x - h * 0.03f * schlag, p.y - h * 0.02f), Size(h * 0.03f * schlag, h * 0.03f))
        drawOval(farbe.dunkler(0.1f), Offset(p.x, p.y - h * 0.02f), Size(h * 0.03f * schlag, h * 0.03f))
        drawLine(Color(0xFF3A3024).copy(alpha = tag), p - Offset(0f, h * 0.018f), p + Offset(0f, h * 0.012f), 2f)
    }

    // Nachtschleier
    drawRect(Color(0xFF07101E).copy(alpha = 0.48f * (1f - tag)))
}

private fun DrawScope.bluetenKopf(kopf: Offset, h: Float, g: Float, farbe: Color, t: Float) {
    for (bl in 0 until 5) {
        val wk = bl * 72f + t * 10f
        drawCircle(farbe, h * 0.016f * g, kopf + Offset(cos(rad(wk)) * h * 0.016f * g, sin(rad(wk)) * h * 0.016f * g))
    }
    drawCircle(Color(0xFFF2C14E), h * 0.01f * g, kopf)
}
