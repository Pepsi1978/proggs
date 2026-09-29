package de.frank.longevity.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.ui.theme.Design
import de.frank.longevity.ui.theme.Farben
import de.frank.longevity.ui.theme.LocalBewegung
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.glas
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// Zyklus, Phasen und Standbilder
// ─────────────────────────────────────────────────────────────────────────────

private const val SZ_ZYKLUS = 24f
private const val SZ_PI = 3.1415927f

private val SZ_PHASEN_MORGENROT = listOf("Abend", "Schlafen", "Sonnenaufgang", "Bewegen")
private val SZ_PHASEN_LEBENSBAUM = listOf("Samen", "Wachsen", "Blühen", "Jahresring")
private val SZ_PHASEN_ATEM = listOf("Einatmen", "Halten", "Ausatmen", "Ruhe")
private val SZ_PHASEN_HELIX = listOf("Zelle", "Reparatur", "Erneuerung", "Jugend")

private fun szPhasenNamen(d: Design): List<String> = when (d) {
    Design.MORGENROT -> SZ_PHASEN_MORGENROT
    Design.LEBENSBAUM -> SZ_PHASEN_LEBENSBAUM
    Design.ATEM -> SZ_PHASEN_ATEM
    Design.HELIX -> SZ_PHASEN_HELIX
}

private fun szPhase(d: Design, t: Float): Int = when (d) {
    Design.MORGENROT -> when { t < 6f -> 0; t < 12f -> 1; t < 17f -> 2; else -> 3 }
    Design.LEBENSBAUM -> when { t < 5f -> 0; t < 12f -> 1; t < 18f -> 2; else -> 3 }
    Design.ATEM -> when { t < 4f -> 0; t < 8f -> 1; t < 14f -> 2; else -> 3 }
    Design.HELIX -> when { t < 5f -> 0; t < 13f -> 1; t < 19f -> 2; else -> 3 }
}

/** Zeitpunkt für das Standbild, wenn Animationen systemweit aus sind. */
private fun szStandbild(d: Design): Float = when (d) {
    Design.MORGENROT -> 20f
    Design.LEBENSBAUM -> 16f
    Design.ATEM -> 6f
    Design.HELIX -> 16f
}

private fun szZyklusZeit(ms: Long): Float = (ms % (SZ_ZYKLUS * 1000).toLong()) / 1000f

/** Wiederverwendbare Pfade — pro Bild wird nur zurückgesetzt, nie neu angelegt. */
private class SzPfade {
    val a = Path()
    val b = Path()
}

/**
 * Kleine Endlosszene rund um ein langes, gesundes Leben. Jedes Design erzählt seine eigene Geschichte:
 * Morgenröte (Abend, Schlaf, Sonnenaufgang, Bewegung), Lebensbaum (Samen bis Jahresring),
 * Atem (4-4-6-Atmung mit ruhigem Herzschlag) und Helix (DNA-Reparatur und Erneuerung).
 * [fortschritt] (0..1) macht die Kulisse reicher: mehr Blätter, Blüten, Licht und Funkeln.
 */
@Composable
fun LongevitySzene(modifier: Modifier = Modifier, fortschritt: Float) {
    val f = LocalFarben.current
    val bewegung = LocalBewegung.current
    val zeit = rememberSzenenZeit()
    val design = f.design
    val reich = fortschritt.coerceIn(0f, 1f)
    val aktivePhase by remember(design, bewegung) {
        derivedStateOf { szPhase(design, if (!bewegung) szStandbild(design) else szZyklusZeit(zeit.value)) }
    }
    val messer = rememberTextMeasurer()
    val namen = szPhasenNamen(design)
    Column(modifier.glas(f, erhoeht = 1.2f).padding(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .drawWithCache {
                    val eck = f.radius.toPx() * 0.7f
                    val rahmen = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(eck))) }
                    val pfade = SzPfade()
                    val jahrText = messer.measure("+1 Jahr", TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    onDrawBehind {
                        // Zeit wird nur hier gelesen: neu gezeichnet wird allein diese Ebene.
                        val t = if (!bewegung) szStandbild(design) else szZyklusZeit(zeit.value)
                        val tr = if (!bewegung) szStandbild(design) else zeit.value / 1000f
                        clipPath(rahmen) {
                            when (design) {
                                Design.MORGENROT -> szMorgenrot(t, tr, f, reich, pfade)
                                Design.LEBENSBAUM -> szLebensbaum(t, tr, f, reich, pfade, jahrText)
                                Design.ATEM -> szAtem(t, tr, f, reich, pfade)
                                Design.HELIX -> szHelix(t, tr, f, reich, pfade)
                            }
                        }
                    }
                },
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            namen.forEachIndexed { i, name ->
                val farbe by animateColorAsState(if (i == aktivePhase) f.primaer else f.textSchwach, label = "szPhase")
                Text(
                    name,
                    color = farbe,
                    fontSize = 12.sp,
                    maxLines = 1,
                    fontWeight = if (i == aktivePhase) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Kleine Rechenhelfer (alle privat, eindeutig mit „sz“ benannt)
// ─────────────────────────────────────────────────────────────────────────────

private fun szMix(a: Float, b: Float, x: Float): Float = a + (b - a) * x.coerceIn(0f, 1f)
private fun szAn(t: Float, start: Float, dauer: Float): Float = ((t - start) / dauer).coerceIn(0f, 1f)
private fun szWeich(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}
private fun szGlocke(x: Float, mitte: Float, breite: Float): Float {
    val d = (x - mitte) / breite
    return exp(-d * d)
}

/** Feste Pseudo-Zufallszahl 0..1 — kein Speicher, pro Index immer gleich. */
private fun szZufall(i: Int, salz: Int): Float {
    val s = sin(i * 12.9898f + salz * 78.233f) * 43758.547f
    return s - floor(s)
}

private fun szQuad(s: Float, p0: Offset, p1: Offset, p2: Offset): Offset {
    val u = 1f - s
    return Offset(u * u * p0.x + 2f * u * s * p1.x + s * s * p2.x, u * u * p0.y + 2f * u * s * p1.y + s * s * p2.y)
}

/** Heller Ton: im Dunkeln der fast weiße Text, im Hellen reines Weiß der Glasfläche. */
private fun szHell(f: Farben): Color = if (f.dunkel) f.text else f.flaecheStark.copy(alpha = 1f)

/** Hautton der Figuren — warm, aus den Designfarben gemischt. */
private fun szHaut(f: Farben): Color = lerp(f.tertiaer, szHell(f), 0.5f)

// ─────────────────────────────────────────────────────────────────────────────
// Figur und Kleinteile
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.szArm(f: Farben, schulter: Offset, winkel: Float, h: Float, alpha: Float) {
    val rad = winkel * SZ_PI / 180f
    val start = Offset(schulter.x, schulter.y + h * 0.02f)
    val ende = Offset(start.x + sin(rad) * h * 0.15f, start.y + cos(rad) * h * 0.15f)
    drawLine(f.primaer.copy(alpha = alpha), start, ende, h * 0.038f, StrokeCap.Round)
    drawCircle(szHaut(f).copy(alpha = alpha), h * 0.024f, ende)
}

/**
 * Einfache, freundliche Figur: runder Kopf, Linienkörper, runde Kappen.
 * Winkel der Arme in Grad (0 = hängt, 180 = oben, negativ = nach links).
 */
private fun DrawScope.szFigur(
    f: Farben,
    x: Float,
    fuss: Float,
    h: Float,
    schritt: Float,
    beinWeite: Float,
    armL: Float,
    armR: Float,
    neigung: Float,
    augenZu: Boolean,
    alpha: Float,
) {
    if (alpha <= 0.01f) return
    val haut = szHaut(f).copy(alpha = alpha)
    val hose = f.text.copy(alpha = 0.72f * alpha)
    val beinLaenge = h * 0.2f
    val huefte = Offset(x, fuss - beinLaenge)
    val schulter = Offset(x + neigung, huefte.y - h * 0.19f)
    // Schatten am Boden
    drawOval(
        f.schatten.copy(alpha = (if (f.dunkel) 0.35f else 0.14f) * alpha),
        Offset(x - h * 0.09f, fuss - h * 0.012f),
        Size(h * 0.18f, h * 0.026f),
    )
    // Beine
    val schwung = sin(schritt) * beinWeite
    val links = 0.12f + schwung
    val rechts = -0.12f - schwung
    drawLine(hose, huefte, Offset(huefte.x + sin(links) * beinLaenge, huefte.y + cos(links) * beinLaenge), h * 0.048f, StrokeCap.Round)
    drawLine(hose, huefte, Offset(huefte.x + sin(rechts) * beinLaenge, huefte.y + cos(rechts) * beinLaenge), h * 0.048f, StrokeCap.Round)
    // Rumpf
    drawLine(
        Brush.verticalGradient(listOf(f.primaer.copy(alpha = alpha), f.sekundaer.copy(alpha = alpha)), schulter.y, huefte.y),
        schulter,
        huefte,
        h * 0.085f,
        StrokeCap.Round,
    )
    // Arme
    szArm(f, schulter, armL, h, alpha)
    szArm(f, schulter, armR, h, alpha)
    // Kopf
    val kr = h * 0.062f
    val kopf = Offset(schulter.x, schulter.y - h * 0.09f)
    drawCircle(haut, kr, kopf)
    drawArc(
        f.text.copy(alpha = 0.7f * alpha), 180f, 180f, true,
        Offset(kopf.x - kr * 1.04f, kopf.y - kr * 1.1f), Size(kr * 2.08f, kr * 1.5f),
    )
    val auge = Offset(kopf.x + kr * 0.42f, kopf.y + kr * 0.05f)
    if (augenZu) {
        drawArc(
            f.text.copy(alpha = 0.8f * alpha), 20f, 140f, false,
            Offset(auge.x - kr * 0.18f, auge.y - kr * 0.14f), Size(kr * 0.36f, kr * 0.24f),
            style = Stroke(h * 0.008f, cap = StrokeCap.Round),
        )
    } else {
        drawCircle(f.text.copy(alpha = 0.85f * alpha), h * 0.008f, auge)
    }
    drawCircle(f.gefahr.copy(alpha = 0.22f * alpha), kr * 0.22f, Offset(kopf.x + kr * 0.25f, kopf.y + kr * 0.42f))
    drawArc(
        f.text.copy(alpha = 0.6f * alpha), 20f, 120f, false,
        Offset(kopf.x + kr * 0.05f, kopf.y + kr * 0.2f), Size(kr * 0.62f, kr * 0.45f),
        style = Stroke(h * 0.008f, cap = StrokeCap.Round),
    )
}

/** Ein „Z“ aus drei Strichen — für den Schlaf. */
private fun DrawScope.szZ(mitte: Offset, s: Float, farbe: Color, dicke: Float) {
    drawLine(farbe, Offset(mitte.x - s, mitte.y - s), Offset(mitte.x + s, mitte.y - s), dicke, StrokeCap.Round)
    drawLine(farbe, Offset(mitte.x + s, mitte.y - s), Offset(mitte.x - s, mitte.y + s), dicke, StrokeCap.Round)
    drawLine(farbe, Offset(mitte.x - s, mitte.y + s), Offset(mitte.x + s, mitte.y + s), dicke, StrokeCap.Round)
}

// ─────────────────────────────────────────────────────────────────────────────
// MORGENRÖTE: Abend → Schlafen → Sonnenaufgang → Bewegen
// ─────────────────────────────────────────────────────────────────────────────

private fun szSonnenPos(t: Float, w: Float, h: Float, horizont: Float): Offset = when {
    t < 6f -> {
        val p = t / 6f
        Offset(szMix(w * 0.62f, w * 0.8f, p), szMix(h * 0.22f, horizont + h * 0.2f, szWeich(p)))
    }
    t < 12f -> Offset(w * 0.5f, h * 2f)
    t < 17f -> {
        val p = szAn(t, 12f, 5f)
        Offset(szMix(w * 0.14f, w * 0.24f, p), szMix(horizont + h * 0.2f, h * 0.2f, szWeich(p)))
    }
    else -> {
        // Weiter über den Himmel bis zur Startposition des Abends — nahtlos im Kreis.
        val p = szAn(t, 17f, 7f)
        Offset(szMix(w * 0.24f, w * 0.62f, p), h * 0.2f - sin(p * SZ_PI) * h * 0.06f + h * 0.02f * p)
    }
}

private fun DrawScope.szMorgenrot(t: Float, tr: Float, f: Farben, reich: Float, pf: SzPfade) {
    val w = size.width
    val h = size.height
    val hell = szHell(f)
    val nacht = szWeich(szAn(t, 3.5f, 3.5f)) * (1f - szWeich(szAn(t, 11.5f, 3.5f)))
    val glut = (szGlocke(t, 4.8f, 1.6f) + szGlocke(t, 13.4f, 1.6f)).coerceAtMost(1f)

    // Himmel: Tag → Abendrot → Nacht → Morgenrot → Tag
    val tagOben = lerp(f.blob2, f.hgOben, if (f.dunkel) 0.6f else 0.55f)
    val tagUnten = lerp(f.blob1, f.hgUnten, if (f.dunkel) 0.5f else 0.3f)
    val nachtOben = if (f.dunkel) lerp(f.hgOben, f.schatten, 0.35f) else lerp(f.sekundaer, f.text, 0.72f)
    val nachtUnten = if (f.dunkel) lerp(f.hgUnten, f.sekundaer, 0.18f) else lerp(f.sekundaer, f.text, 0.42f)
    val oben = lerp(lerp(tagOben, nachtOben, nacht), hell, 0.1f * reich * (1f - nacht))
    val unten = lerp(lerp(tagUnten, nachtUnten, nacht), f.primaer, 0.55f * glut)
    val horizont = h * 0.7f
    drawRect(Brush.verticalGradient(listOf(oben, unten), 0f, horizont + h * 0.1f))

    // Sterne
    if (nacht > 0.01f) for (i in 0 until 28) {
        val x = szZufall(i, 11) * w
        val y = szZufall(i, 23) * horizont * 0.85f
        val funkeln = 0.35f + 0.65f * (0.5f + 0.5f * sin(tr * (1.3f + szZufall(i, 5) * 2.2f) + i))
        drawCircle(hell.copy(alpha = 0.9f * nacht * funkeln), h * (0.005f + 0.007f * szZufall(i, 7)), Offset(x, y))
    }

    // Mond als Sichel
    if (nacht > 0.01f) {
        val p = szAn(t, 4.5f, 9f)
        val mond = Offset(szMix(w * 0.7f, w * 0.9f, p), h * 0.26f - sin(p * SZ_PI) * h * 0.1f)
        val mr = h * 0.075f
        drawCircle(Brush.radialGradient(listOf(hell.copy(alpha = 0.35f * nacht), hell.copy(alpha = 0f)), mond, mr * 3f), mr * 3f, mond)
        drawCircle(lerp(hell, f.tertiaer, 0.2f).copy(alpha = nacht), mr, mond)
        drawCircle(lerp(oben, unten, 0.2f).copy(alpha = nacht), mr * 0.85f, Offset(mond.x + mr * 0.45f, mond.y - mr * 0.25f))
    }

    // Sonne
    val sr = h * 0.085f
    val sonne = szSonnenPos(t, w, h, horizont)
    if (sonne.y < horizont + sr * 1.5f) {
        val gr = sr * (3.2f + 0.8f * reich)
        drawCircle(
            Brush.radialGradient(listOf(f.tertiaer.copy(alpha = 0.55f), f.primaer.copy(alpha = 0.18f), f.primaer.copy(alpha = 0f)), sonne, gr),
            gr, sonne,
        )
        val strahlen = szAn(t, 15f, 2f) * (1f - szAn(t, 23.2f, 0.8f))
        if (strahlen > 0f) for (i in 0 until 10) {
            val wk = i / 10f * 2f * SZ_PI + tr * 0.25f
            val r1 = sr * 1.35f
            val r2 = sr * (1.8f + 0.15f * sin(tr * 2f + i))
            drawLine(
                f.tertiaer.copy(alpha = 0.7f * strahlen),
                Offset(sonne.x + cos(wk) * r1, sonne.y + sin(wk) * r1),
                Offset(sonne.x + cos(wk) * r2, sonne.y + sin(wk) * r2),
                h * 0.014f, StrokeCap.Round,
            )
        }
        drawCircle(Brush.linearGradient(listOf(f.tertiaer, f.primaer), Offset(sonne.x, sonne.y - sr), Offset(sonne.x, sonne.y + sr)), sr, sonne)
    }

    // Hügel
    val huegel = lerp(
        if (f.dunkel) lerp(f.sekundaer, f.hgUnten, 0.55f) else lerp(f.sekundaer, f.blob2, 0.45f),
        nachtOben, 0.45f * nacht,
    )
    pf.a.reset()
    pf.a.moveTo(0f, horizont + h * 0.02f)
    pf.a.quadraticTo(w * 0.2f, horizont - h * 0.1f, w * 0.42f, horizont)
    pf.a.quadraticTo(w * 0.7f, horizont + h * 0.06f, w, horizont - h * 0.07f)
    pf.a.lineTo(w, h)
    pf.a.lineTo(0f, h)
    pf.a.close()
    drawPath(pf.a, huegel)

    // Boden und Weg
    val bodenY = h * 0.8f
    val bodenFarbe = lerp(
        if (f.dunkel) lerp(f.primaer, f.hgUnten, 0.7f) else lerp(f.blob1, f.primaer, 0.3f),
        nachtUnten, 0.5f * nacht,
    )
    drawRect(bodenFarbe, Offset(0f, bodenY), Size(w, h - bodenY))
    val wegY = h * 0.9f
    drawLine(lerp(bodenFarbe, hell, 0.35f), Offset(0f, wegY), Offset(w, wegY), h * 0.06f)
    for (i in 0 until 10) {
        val x = w * (i / 10f) + w * 0.03f
        drawLine(hell.copy(alpha = 0.35f * (1f - 0.5f * nacht)), Offset(x, wegY), Offset(x + w * 0.035f, wegY), h * 0.008f, StrokeCap.Round)
    }
    // Blumen am Wegrand — mit dem Fortschritt blüht mehr
    val blumen = 2 + (reich * 7f).toInt()
    for (i in 0 until blumen) {
        val bx = w * (0.04f + 0.92f * szZufall(i, 31))
        val by = bodenY + h * 0.03f + sin(tr * 1.2f + i) * h * 0.003f
        drawLine(f.erfolg.copy(alpha = 0.8f), Offset(bx, by + h * 0.035f), Offset(bx, by), h * 0.008f, StrokeCap.Round)
        drawCircle((if (i % 2 == 0) f.tertiaer else f.sekundaer).copy(alpha = 0.95f - 0.4f * nacht), h * 0.015f, Offset(bx, by))
    }

    // Bett
    val fuss = wegY
    val bettL = w * 0.3f
    val bettR = w * 0.54f
    val matratzeY = fuss - h * 0.15f
    val holz = if (f.dunkel) lerp(f.sekundaer, f.text, 0.35f) else lerp(f.primaer, f.text, 0.55f)
    drawRoundRect(holz, Offset(bettL - h * 0.035f, fuss - h * 0.3f), Size(h * 0.035f, h * 0.3f), CornerRadius(h * 0.015f))
    drawRoundRect(holz, Offset(bettR, fuss - h * 0.2f), Size(h * 0.035f, h * 0.2f), CornerRadius(h * 0.015f))
    drawRoundRect(hell.copy(alpha = 0.95f), Offset(bettL, matratzeY), Size(bettR - bettL, h * 0.06f), CornerRadius(h * 0.02f))
    drawRect(holz, Offset(bettL, matratzeY + h * 0.06f), Size(bettR - bettL, h * 0.025f))
    drawOval(lerp(hell, f.blob2, 0.3f), Offset(bettL + h * 0.01f, matratzeY - h * 0.05f), Size(w * 0.06f, h * 0.07f))

    // Schlafende Figur
    val liegen = szAn(t, 4.6f, 0.6f) * (1f - szAn(t, 14.8f, 0.6f))
    val kr = h * 0.062f
    val kopf = Offset(bettL + w * 0.035f, matratzeY - h * 0.045f)
    if (liegen > 0.01f) {
        drawCircle(szHaut(f).copy(alpha = liegen), kr, kopf)
        // Schlafmütze mit Bommel
        drawArc(f.sekundaer.copy(alpha = liegen), 180f, 180f, true, Offset(kopf.x - kr, kopf.y - kr), Size(kr * 2f, kr * 2f))
        drawCircle(f.tertiaer.copy(alpha = liegen), kr * 0.3f, Offset(kopf.x + kr * 1.1f, kopf.y - kr * 0.75f))
        val wach = szAn(t, 13.3f, 0.3f)
        val auge = Offset(kopf.x + kr * 0.4f, kopf.y + kr * 0.3f)
        if (wach < 0.5f) {
            drawArc(
                f.text.copy(alpha = 0.8f * liegen), 20f, 140f, false,
                Offset(auge.x - kr * 0.18f, auge.y - kr * 0.14f), Size(kr * 0.36f, kr * 0.24f),
                style = Stroke(h * 0.008f, cap = StrokeCap.Round),
            )
        } else {
            drawCircle(f.text.copy(alpha = 0.85f * liegen), h * 0.008f, auge)
        }
    }
    // Decke — atmet leise mit
    val deckeH = szMix(h * 0.03f, h * 0.075f, liegen) + sin(tr * 1.6f) * h * 0.005f * liegen
    val deckeL = bettL + w * 0.065f
    drawRoundRect(
        f.sekundaer.copy(alpha = 0.88f),
        Offset(deckeL, matratzeY - deckeH + h * 0.02f),
        Size(bettR - deckeL + h * 0.012f, deckeH + h * 0.02f),
        CornerRadius(h * 0.03f),
    )
    for (s in 1..2) {
        val sx = deckeL + (bettR - deckeL) * s / 3f
        drawLine(hell.copy(alpha = 0.3f), Offset(sx, matratzeY - deckeH + h * 0.03f), Offset(sx, matratzeY + h * 0.035f), h * 0.008f)
    }

    // Aufsteigende „Z“
    val zzz = szAn(t, 6f, 0.5f) * (1f - szAn(t, 13f, 0.5f))
    if (zzz > 0f) for (k in 0 until 3) {
        val p = (((t - 6f) / 2.7f + k / 3f) % 1f + 1f) % 1f
        val m = Offset(
            kopf.x + h * 0.08f + p * h * 0.25f + sin(p * 6f + k) * h * 0.02f,
            kopf.y - h * 0.08f - p * h * 0.4f,
        )
        szZ(m, h * (0.016f + 0.02f * p), hell.copy(alpha = zzz * sin(p * SZ_PI)), h * 0.011f)
    }

    // Abend: Figur steht vor dem Bett und gähnt
    val abend = szAn(t, 0f, 0.8f) * (1f - szAn(t, 4.6f, 0.6f))
    if (abend > 0.01f) {
        val gaehnen = szGlocke(t, 3.6f, 0.7f)
        szFigur(
            f, w * 0.18f, fuss, h, 0f, 0f,
            armL = szMix(-14f, -155f, gaehnen), armR = 14f + sin(tr * 1.2f) * 4f,
            neigung = 0f, augenZu = gaehnen > 0.5f, alpha = abend,
        )
    }
    // Morgen: aufstehen, strecken, loslaufen
    val morgen = szAn(t, 14.8f, 0.6f)
    if (morgen > 0.01f) {
        val lauf = szAn(t, 17.2f, 0.6f)
        val x = szMix(w * 0.64f, w * 1.15f, szAn(t, 17.4f, 6.2f))
        val schritt = tr * 10f
        val strecken = szGlocke(t, 16.2f, 0.6f)
        val huepf = abs(sin(schritt)) * h * 0.025f * lauf
        val armSchwung = sin(schritt) * 50f * lauf
        szFigur(
            f, x, fuss - huepf, h, schritt, 0.55f * lauf,
            armL = szMix(szMix(-14f, -160f, strecken), armSchwung, lauf),
            armR = szMix(szMix(14f, 160f, strecken), -armSchwung, lauf),
            neigung = h * 0.03f * lauf, augenZu = strecken > 0.6f, alpha = morgen,
        )
        // Kleine Staubwölkchen hinter den Füßen
        if (lauf > 0f) for (k in 0 until 3) {
            val p = ((tr * 1.6f + k / 3f) % 1f)
            drawCircle(hell.copy(alpha = 0.35f * (1f - p) * lauf), h * (0.012f + 0.02f * p), Offset(x - h * 0.08f - p * h * 0.12f, fuss - h * 0.01f - p * h * 0.03f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LEBENSBAUM: Samen → Wachsen → Blühen → Jahresring
// ─────────────────────────────────────────────────────────────────────────────

private val SZ_AST_S = floatArrayOf(0.42f, 0.52f, 0.62f, 0.7f, 0.8f, 0.9f)
private val SZ_AST_WINKEL = floatArrayOf(-58f, 52f, -44f, 40f, -28f, 24f)
private val SZ_AST_LAENGE = floatArrayOf(0.5f, 0.46f, 0.42f, 0.38f, 0.3f, 0.26f)
private val SZ_AST_START = floatArrayOf(7f, 7.6f, 8.2f, 8.8f, 9.4f, 10f)

private fun szAstEnde(ansatz: Offset, winkel: Float, laenge: Float): Offset {
    val rad = winkel * SZ_PI / 180f
    return Offset(ansatz.x + sin(rad) * laenge, ansatz.y - cos(rad) * laenge)
}

private fun DrawScope.szLebensbaum(t: Float, tr: Float, f: Farben, reich: Float, pf: SzPfade, jahr: TextLayoutResult) {
    val w = size.width
    val h = size.height
    val hell = szHell(f)

    // Himmel
    drawRect(
        Brush.verticalGradient(
            listOf(
                lerp(f.blob3, f.hgOben, if (f.dunkel) 0.72f else 0.35f),
                lerp(f.blob1, f.hgUnten, if (f.dunkel) 0.72f else 0.5f),
            ),
        ),
    )
    // Sonne mit langsam kreisenden Strahlen
    val sonne = Offset(w * 0.08f, h * 0.17f)
    val sr = h * 0.065f
    val sg = sr * (3f + reich)
    drawCircle(Brush.radialGradient(listOf(f.sekundaer.copy(alpha = 0.45f + 0.2f * reich), f.sekundaer.copy(alpha = 0f)), sonne, sg), sg, sonne)
    for (i in 0 until 8) {
        val wk = i / 8f * 2f * SZ_PI + tr * 0.3f
        drawLine(
            f.sekundaer.copy(alpha = 0.6f),
            Offset(sonne.x + cos(wk) * sr * 1.4f, sonne.y + sin(wk) * sr * 1.4f),
            Offset(sonne.x + cos(wk) * sr * 1.9f, sonne.y + sin(wk) * sr * 1.9f),
            h * 0.012f, StrokeCap.Round,
        )
    }
    drawCircle(lerp(f.sekundaer, hell, 0.25f), sr, sonne)

    // Boden
    val bodenY = h * 0.8f
    pf.a.reset()
    pf.a.moveTo(0f, bodenY + h * 0.03f)
    pf.a.quadraticTo(w * 0.3f, bodenY - h * 0.06f, w * 0.62f, bodenY + h * 0.01f)
    pf.a.quadraticTo(w * 0.85f, bodenY + h * 0.05f, w, bodenY - h * 0.02f)
    pf.a.lineTo(w, h)
    pf.a.lineTo(0f, h)
    pf.a.close()
    drawPath(
        pf.a,
        Brush.verticalGradient(
            listOf(
                lerp(f.primaer, hell, if (f.dunkel) 0f else 0.15f).copy(alpha = 0.85f),
                lerp(f.primaer, if (f.dunkel) f.hgUnten else f.text, 0.35f),
            ),
            bodenY - h * 0.06f, h,
        ),
    )
    // Grashalme — mit dem Fortschritt dichter
    val halme = 8 + (reich * 12f).toInt()
    for (i in 0 until halme) {
        val gx = w * szZufall(i, 81)
        val gy = bodenY + h * 0.06f + szZufall(i, 83) * h * 0.1f
        val neig = sin(tr * 1.4f + i) * h * 0.012f
        drawLine(lerp(f.primaer, hell, 0.25f).copy(alpha = 0.8f), Offset(gx, gy), Offset(gx + neig, gy - h * 0.04f), h * 0.007f, StrokeCap.Round)
    }

    val baumA = 1f - szAn(t, 22.6f, 1.3f)
    val bx = w * 0.3f
    val by = bodenY - h * 0.02f
    val rinde = if (f.dunkel) lerp(f.sekundaer, f.hgUnten, 0.45f) else lerp(f.sekundaer, f.text, 0.55f)
    val samenFarbe = lerp(f.sekundaer, f.text, 0.35f)

    // Samen fällt, landet, wird begossen
    val fall = szAn(t, 0.3f, 1.4f)
    val samenA = szAn(t, 0.2f, 0.4f) * (1f - szAn(t, 5f, 0.6f))
    if (samenA > 0.01f) {
        val sy = szMix(-h * 0.05f, by - h * 0.015f, fall * fall) + szAn(t, 1.9f, 0.8f) * h * 0.03f
        val sx = bx + sin(fall * 7f) * h * 0.05f * (1f - fall)
        rotate(szMix(-40f, 15f, fall), Offset(sx, sy)) {
            drawOval(samenFarbe.copy(alpha = samenA), Offset(sx - h * 0.022f, sy - h * 0.03f), Size(h * 0.044f, h * 0.06f))
            drawOval(hell.copy(alpha = 0.35f * samenA), Offset(sx - h * 0.012f, sy - h * 0.022f), Size(h * 0.012f, h * 0.02f))
        }
        val staub = szAn(t, 1.7f, 0.6f)
        if (staub in 0.01f..0.99f) for (k in 0 until 5) {
            val wk = SZ_PI + k / 4f * SZ_PI
            drawCircle(samenFarbe.copy(alpha = 0.5f * (1f - staub)), h * 0.008f, Offset(bx + cos(wk) * h * 0.08f * staub, by + sin(wk) * h * 0.05f * staub))
        }
    }
    val regen = szAn(t, 1.9f, 0.2f) * (1f - szAn(t, 3.3f, 0.3f))
    if (regen > 0f) for (i in 0 until 5) {
        val p = ((t - 1.9f) * 1.6f + i / 5f) % 1f
        val rx = bx + (i - 2) * h * 0.07f
        val ry = szMix(h * 0.05f, by - h * 0.03f, p)
        drawLine(f.tertiaer.copy(alpha = 0.8f * regen), Offset(rx, ry), Offset(rx, ry + h * 0.035f), h * 0.01f, StrokeCap.Round)
    }

    // Stamm: vom Keimling bis zum Baum (Pfad-Animation über eine Kurve)
    val keim = szWeich(szAn(t, 3f, 2f))
    val wachs = szWeich(szAn(t, 5f, 5f))
    val stammH = h * 0.08f * keim + h * 0.42f * wachs
    val p0 = Offset(bx, by)
    val p1 = Offset(bx - h * 0.05f * wachs, by - stammH * 0.5f)
    val p2 = Offset(bx + h * 0.01f * sin(tr * 0.8f) * wachs, by - stammH)
    if (stammH > 0.5f && baumA > 0.01f) {
        val unten = h * (0.008f + 0.05f * wachs)
        val obenB = h * (0.006f + 0.012f * wachs)
        val stammFarbe = lerp(f.primaer, rinde, wachs).copy(alpha = baumA)
        val n = 14
        for (i in 0 until n) {
            val s0 = i / n.toFloat()
            val s1 = (i + 1) / n.toFloat()
            drawLine(stammFarbe, szQuad(s0, p0, p1, p2), szQuad(s1, p0, p1, p2), szMix(unten, obenB, s0), StrokeCap.Round)
        }
    }
    // Keimblätter
    val keimBlatt = keim * (1f - szAn(t, 5.6f, 0.6f))
    if (keimBlatt > 0.01f) for (seite in 0..1) {
        val v = if (seite == 0) -1f else 1f
        rotate(v * 40f, p2) {
            drawOval(
                lerp(f.primaer, hell, 0.2f).copy(alpha = keimBlatt),
                Offset(p2.x + (if (v < 0f) -h * 0.06f else 0f), p2.y - h * 0.015f),
                Size(h * 0.06f * keimBlatt, h * 0.03f),
            )
        }
    }

    // Äste und Zweige
    if (baumA > 0.01f) for (i in 0 until 6) {
        val g = szWeich(szAn(t, SZ_AST_START[i], 1.6f))
        if (g <= 0f) continue
        val ansatz = szQuad(SZ_AST_S[i], p0, p1, p2)
        val len = SZ_AST_LAENGE[i] * h * 0.5f * g
        val ende = szAstEnde(ansatz, SZ_AST_WINKEL[i], len)
        drawLine(rinde.copy(alpha = baumA), ansatz, ende, h * 0.02f * (1f - SZ_AST_S[i] * 0.5f), StrokeCap.Round)
        val zg = szWeich(szAn(t, SZ_AST_START[i] + 0.7f, 1.4f))
        if (zg > 0f) {
            val zAnsatz = Offset(szMix(ansatz.x, ende.x, 0.6f), szMix(ansatz.y, ende.y, 0.6f))
            val zEnde = szAstEnde(zAnsatz, SZ_AST_WINKEL[i] + (if (SZ_AST_WINKEL[i] < 0f) 30f else -30f), len * 0.45f * zg)
            drawLine(rinde.copy(alpha = baumA), zAnsatz, zEnde, h * 0.009f, StrokeCap.Round)
        }
    }

    // Blätterbüschel — mit dem Fortschritt mehr und größer
    val extra = (reich * 6f).toInt()
    val buechel = 7 + extra
    val blattDunkel = lerp(f.primaer, if (f.dunkel) f.hgUnten else f.text, 0.22f)
    val blattHell = lerp(f.primaer, hell, 0.3f)
    if (baumA > 0.01f) for (i in 0 until buechel) {
        val b = szWeich(szAn(t, 12f + (i % 7) * 0.4f + (i / 7) * 0.3f, 1.2f))
        if (b <= 0f) continue
        val pos = szLebensbaumBueschel(i, p0, p1, p2, h) + Offset(sin(tr * 1.2f + i) * h * 0.006f, 0f)
        val r = h * (0.05f + 0.022f * reich) * b * (if (i >= 7) 0.8f else 1f)
        drawCircle(blattDunkel.copy(alpha = 0.95f * baumA), r * 1.1f, Offset(pos.x - r * 0.4f, pos.y + r * 0.2f))
        drawCircle(f.primaer.copy(alpha = baumA), r, pos)
        drawCircle(blattHell.copy(alpha = baumA), r * 0.6f, Offset(pos.x + r * 0.3f, pos.y - r * 0.3f))
    }
    // Blüten
    val blueten = 3 + (reich * 7f).toInt()
    val bluetenFarbe = lerp(f.gefahr, hell, 0.55f)
    if (baumA > 0.01f) for (j in 0 until blueten) {
        val bl = szWeich(szAn(t, 14f + j * 0.25f, 0.8f))
        if (bl <= 0f) continue
        val basis = szLebensbaumBueschel(j % buechel, p0, p1, p2, h)
        val c = Offset(basis.x + (szZufall(j, 91) - 0.5f) * h * 0.08f, basis.y + (szZufall(j, 93) - 0.5f) * h * 0.07f)
        for (k in 0 until 5) {
            val wk = k / 5f * 2f * SZ_PI + j
            drawCircle(bluetenFarbe.copy(alpha = baumA), h * 0.013f * bl, Offset(c.x + cos(wk) * h * 0.016f * bl, c.y + sin(wk) * h * 0.016f * bl))
        }
        drawCircle(f.sekundaer.copy(alpha = baumA), h * 0.009f * bl, c)
    }
    // Herabschwebende Blütenblätter
    for (k in 0 until 4) {
        val p = szAn(t, 17f + k * 0.8f, 3f)
        if (p <= 0f || p >= 1f) continue
        val start = szLebensbaumBueschel(k, p0, p1, p2, h)
        val pos = Offset(start.x + sin(p * 6f + k) * h * 0.05f, szMix(start.y, by, p))
        drawOval(bluetenFarbe.copy(alpha = (1f - p) * baumA), Offset(pos.x - h * 0.012f, pos.y - h * 0.007f), Size(h * 0.024f, h * 0.014f))
    }

    // Jahresring-Querschnitt
    val cx = w * 0.76f
    val cy = h * 0.46f
    val mitte = Offset(cx, cy)
    val fokus = szWeich(szAn(t, 18f, 1f)) * (1f - szAn(t, 23f, 1f))
    val neu = szWeich(szAn(t, 18.6f, 2f)) * (1f - szAn(t, 23f, 1f))
    val rr = h * 0.27f * (1f + 0.06f * fokus)
    val aussen = rr * (1f + 0.13f * neu)
    val deutlich = 0.55f + 0.45f * fokus
    val ringFarbe = lerp(f.sekundaer, if (f.dunkel) f.hgUnten else f.text, 0.4f)
    drawCircle(f.schatten.copy(alpha = (if (f.dunkel) 0.3f else 0.1f) * deutlich), aussen * 1.04f, Offset(cx + h * 0.01f, cy + h * 0.02f))
    drawCircle(
        Brush.radialGradient(listOf(lerp(f.sekundaer, hell, 0.7f), lerp(f.sekundaer, hell, 0.35f)), mitte, aussen),
        aussen, mitte, alpha = deutlich,
    )
    val ringe = 4 + (reich * 3f).toInt()
    for (k in 1..ringe) {
        val r = rr * k / (ringe + 0.6f)
        drawCircle(ringFarbe.copy(alpha = 0.5f * deutlich), r, mitte, style = Stroke(h * 0.007f))
    }
    if (neu > 0.01f) {
        drawCircle(lerp(f.primaer, hell, 0.55f).copy(alpha = 0.45f * neu), (rr + aussen) / 2f, mitte, style = Stroke(aussen - rr))
        val flimmern = 0.6f + 0.4f * sin(tr * 4f)
        drawCircle(f.primaer.copy(alpha = 0.35f * neu * flimmern), rr, mitte, style = Stroke(h * 0.035f))
        drawCircle(f.primaer.copy(alpha = neu), rr, mitte, style = Stroke(h * 0.012f))
    }
    drawCircle(ringFarbe.copy(alpha = 0.7f * deutlich), h * 0.012f, mitte)
    drawCircle(rinde.copy(alpha = deutlich), aussen, mitte, style = Stroke(h * 0.028f))

    // „+1 Jahr“
    val textA = szAn(t, 19.2f, 0.5f) * (1f - szAn(t, 22.4f, 0.6f))
    if (textA > 0.01f) {
        val steig = szWeich(szAn(t, 19.2f, 1.2f)) * h * 0.06f
        val tw = jahr.size.width.toFloat()
        val th = jahr.size.height.toFloat()
        val links = cx - tw / 2f
        val oben = cy - th / 2f - steig
        val ph = th + h * 0.03f
        drawRoundRect(f.primaer.copy(alpha = 0.95f * textA), Offset(links - h * 0.04f, oben - h * 0.015f), Size(tw + h * 0.08f, ph), CornerRadius(ph / 2f))
        drawText(jahr, color = f.aufPrimaer, topLeft = Offset(links, oben), alpha = textA)
    }
}

/** Position eines Blätterbüschels: 0..5 Astenden, 6 Wipfel, ab 7 Zweigenden. */
private fun szLebensbaumBueschel(i: Int, p0: Offset, p1: Offset, p2: Offset, h: Float): Offset {
    if (i == 6) return Offset(p2.x, p2.y - h * 0.02f)
    val a = if (i < 6) i else (i - 7) % 6
    val ansatz = szQuad(SZ_AST_S[a], p0, p1, p2)
    val len = SZ_AST_LAENGE[a] * h * 0.5f
    val ende = szAstEnde(ansatz, SZ_AST_WINKEL[a], len)
    if (i < 6) return ende
    val zAnsatz = Offset(szMix(ansatz.x, ende.x, 0.6f), szMix(ansatz.y, ende.y, 0.6f))
    return szAstEnde(zAnsatz, SZ_AST_WINKEL[a] + (if (SZ_AST_WINKEL[a] < 0f) 30f else -30f), len * 0.45f)
}

// ─────────────────────────────────────────────────────────────────────────────
// ATEM: Einatmen (4) → Halten (4) → Ausatmen (6) → Ruhe
// ─────────────────────────────────────────────────────────────────────────────

/** Ruhepuls als Anteil des Normalpulses — so gewählt, dass in 24 s genau 22 Schläge liegen (nahtlos). */
private const val SZ_RUHE_RATE = 7f / 9f

private fun szAtemWert(t: Float): Float = when {
    t < 4f -> szWeich(t / 4f)
    t < 8f -> 1f
    t < 14f -> 1f - szWeich((t - 8f) / 6f)
    else -> {
        val s = sin((t - 14f) / 10f * SZ_PI)
        0.06f * s * s
    }
}

/** Anzahl Herzschläge seit Zyklusbeginn; in der Ruhe verlangsamt sich der Puls sanft. */
private fun szHerzPhase(tau: Float): Float {
    var x = tau % SZ_ZYKLUS
    if (x < 0f) x += SZ_ZYKLUS
    return when {
        x < 14f -> x
        x < 16f -> {
            val d = x - 14f
            14f + d + (SZ_RUHE_RATE - 1f) * d * d / 4f
        }
        else -> 15f + SZ_RUHE_RATE + (x - 16f) * SZ_RUHE_RATE
    }
}

private fun szGauss(u: Float, c: Float, b: Float): Float {
    val d = (u - c) / b
    return exp(-d * d)
}

/** Form eines Herzschlags im EKG (P, QRS, T). */
private fun szEkg(u: Float): Float =
    0.1f * szGauss(u, 0.12f, 0.035f) - 0.12f * szGauss(u, 0.27f, 0.012f) + szGauss(u, 0.3f, 0.014f) -
        0.28f * szGauss(u, 0.335f, 0.014f) + 0.2f * szGauss(u, 0.55f, 0.05f)

private fun szHerzPfad(p: Path, c: Offset, s: Float) {
    p.reset()
    p.moveTo(c.x, c.y + s * 0.9f)
    p.cubicTo(c.x - s * 1.4f, c.y + s * 0.1f, c.x - s * 0.9f, c.y - s * 1.1f, c.x, c.y - s * 0.35f)
    p.cubicTo(c.x + s * 0.9f, c.y - s * 1.1f, c.x + s * 1.4f, c.y + s * 0.1f, c.x, c.y + s * 0.9f)
    p.close()
}

private fun DrawScope.szAtem(t: Float, tr: Float, f: Farben, reich: Float, pf: SzPfade) {
    val w = size.width
    val h = size.height
    val hell = szHell(f)
    drawRect(
        Brush.verticalGradient(
            listOf(
                lerp(f.blob1, f.hgOben, if (f.dunkel) 0.72f else 0.45f),
                lerp(f.blob3, f.hgUnten, if (f.dunkel) 0.7f else 0.5f),
            ),
        ),
    )
    val atem = szAtemWert(t)
    val phase = szPhase(Design.ATEM, t)
    val ruhe = szAn(t, 14f, 2f) * (1f - szAn(t, 22.5f, 1.5f))

    // Schwebende Bläschen — mehr mit dem Fortschritt
    val blasen = 4 + (reich * 8f).toInt()
    val spanne = h + 20f
    for (i in 0 until blasen) {
        val y = spanne - ((szZufall(i, 41) * spanne + tr * (6f + szZufall(i, 43) * 8f)) % spanne) - 10f
        val x = szZufall(i, 47) * w + sin(tr * 1.1f + i) * h * 0.03f
        drawCircle(hell.copy(alpha = if (f.dunkel) 0.25f else 0.5f), h * (0.01f + 0.012f * szZufall(i, 49)), Offset(x, y), style = Stroke(h * 0.005f))
    }

    // EKG-Linie hinter dem Atemkreis: läuft von rechts nach links, in der Ruhe langsamer
    val basis = h * 0.62f
    val amp = h * 0.15f * (1f - 0.15f * ruhe)
    val fenster = 4f
    val links = w * 0.04f
    val rechts = w * 0.96f
    pf.a.reset()
    var x = links
    var erster = true
    while (x <= rechts) {
        val tau = t - (rechts - x) / (rechts - links) * fenster
        val ph = szHerzPhase(tau)
        val y = basis - szEkg(ph - floor(ph)) * amp
        if (erster) {
            pf.a.moveTo(x, y)
            erster = false
        } else {
            pf.a.lineTo(x, y)
        }
        x += 3f
    }
    val ekgFarbe = lerp(f.gefahr, f.primaer, 0.25f)
    drawPath(
        pf.a,
        Brush.horizontalGradient(listOf(ekgFarbe.copy(alpha = 0f), ekgFarbe.copy(alpha = 0.55f), ekgFarbe), links, rechts),
        style = Stroke(h * 0.012f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    val jetztPh = szHerzPhase(t)
    val kopfY = basis - szEkg(jetztPh - floor(jetztPh)) * amp
    drawCircle(Brush.radialGradient(listOf(ekgFarbe.copy(alpha = 0.5f), ekgFarbe.copy(alpha = 0f)), Offset(rechts, kopfY), h * 0.05f), h * 0.05f, Offset(rechts, kopfY))
    drawCircle(ekgFarbe, h * 0.013f, Offset(rechts, kopfY))

    // Atemkreis
    val mitte = Offset(w * 0.5f, h * 0.4f)
    val r = szMix(h * 0.11f, h * 0.27f, atem)
    val gr = r * (1.7f + 0.3f * reich)
    drawCircle(Brush.radialGradient(listOf(f.primaer.copy(alpha = 0.35f), f.primaer.copy(alpha = 0f)), mitte, gr), gr, mitte)
    for (k in 1..2) drawCircle(f.sekundaer.copy(alpha = 0.2f / k), r * (1f + 0.18f * k), mitte, style = Stroke(h * 0.006f))
    drawCircle(
        Brush.radialGradient(listOf(lerp(f.sekundaer, hell, 0.35f).copy(alpha = 0.75f), f.primaer.copy(alpha = 0.5f)), mitte, r),
        r, mitte,
    )
    drawCircle(hell.copy(alpha = 0.7f), r, mitte, style = Stroke(h * 0.01f))
    drawCircle(hell.copy(alpha = 0.35f), r * 0.25f, Offset(mitte.x - r * 0.35f, mitte.y - r * 0.4f))

    // Fortschritt der aktuellen Atemphase als Bogen
    val phasenAnteil = when (phase) {
        0 -> t / 4f
        1 -> (t - 4f) / 4f
        2 -> (t - 8f) / 6f
        else -> (t - 14f) / 10f
    }.coerceIn(0f, 1f)
    val ar = r + h * 0.045f
    val bogenOben = Offset(mitte.x - ar, mitte.y - ar)
    drawArc(f.tertiaer.copy(alpha = 0.22f), 0f, 360f, false, bogenOben, Size(ar * 2f, ar * 2f), style = Stroke(h * 0.012f))
    drawArc(f.tertiaer, -90f, 360f * phasenAnteil, false, bogenOben, Size(ar * 2f, ar * 2f), style = Stroke(h * 0.012f, cap = StrokeCap.Round))

    // Halten: vier kreisende Zählpunkte, jede Sekunde leuchtet einer mehr
    val halten = szAn(t, 4f, 0.4f) * (1f - szAn(t, 7.6f, 0.4f))
    if (halten > 0.01f) {
        val gezaehlt = ((t - 4f).toInt() + 1).coerceIn(1, 4)
        for (i in 0 until 4) {
            val wk = i * SZ_PI / 2f + tr * 1.2f
            val od = r + h * 0.1f
            drawCircle(
                f.sekundaer.copy(alpha = halten * (if (i < gezaehlt) 1f else 0.3f)),
                h * 0.018f,
                Offset(mitte.x + cos(wk) * od, mitte.y + sin(wk) * od),
            )
        }
    }
    // Luftteilchen: beim Einatmen hinein, beim Ausatmen hinaus
    val ein = if (t < 4f) szAn(t, 0f, 0.3f) * (1f - szAn(t, 3.7f, 0.3f)) else 0f
    val aus = szAn(t, 8f, 0.3f) * (1f - szAn(t, 13.6f, 0.4f))
    val fluss = maxOf(ein, aus)
    if (fluss > 0.01f) for (i in 0 until 10) {
        val wk = i / 10f * 2f * SZ_PI + szZufall(i, 51)
        var p = (tr * 0.6f + szZufall(i, 53)) % 1f
        if (ein > aus) p = 1f - p
        val d = r * (1.05f + 0.9f * p)
        drawCircle(
            lerp(f.sekundaer, hell, 0.3f).copy(alpha = fluss * sin(p * SZ_PI) * 0.85f),
            h * (0.008f + 0.006f * szZufall(i, 55)),
            Offset(mitte.x + cos(wk) * d, mitte.y + sin(wk) * d),
        )
    }

    // Herz oben rechts pulsiert mit jedem Schlag
    val schlag = szGauss(jetztPh - floor(jetztPh), 0.31f, 0.06f)
    val herz = Offset(w * 0.91f, h * 0.17f)
    val hs = h * 0.055f * (1f + 0.25f * schlag)
    drawCircle(Brush.radialGradient(listOf(f.gefahr.copy(alpha = 0.4f * schlag), f.gefahr.copy(alpha = 0f)), herz, hs * 2.6f), hs * 2.6f, herz)
    szHerzPfad(pf.b, herz, hs)
    drawPath(pf.b, f.gefahr.copy(alpha = 0.9f))

    // Wellen unten — schwellen mit dem Atem an
    for (k in 0 until 3) {
        val wBasis = h * (0.8f + k * 0.065f)
        val wAmp = h * (0.018f + 0.035f * atem) * (1f - k * 0.25f)
        val laenge = w / (1.3f + k * 0.6f)
        pf.a.reset()
        pf.a.moveTo(0f, h)
        var wx = 0f
        while (wx <= w + 6f) {
            pf.a.lineTo(wx, wBasis + sin(wx / laenge * 2f * SZ_PI + tr * (0.7f + k * 0.25f) + k * 1.7f) * wAmp)
            wx += 6f
        }
        pf.a.lineTo(w, h)
        pf.a.close()
        val farbe = when (k) {
            0 -> f.tertiaer
            1 -> f.primaer
            else -> f.sekundaer
        }
        drawPath(pf.a, farbe.copy(alpha = if (f.dunkel) 0.3f else 0.35f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELIX: Zelle → Reparatur → Erneuerung → Jugend
// ─────────────────────────────────────────────────────────────────────────────

private const val SZ_SPROSSEN = 18
private val SZ_DEFEKTE = intArrayOf(4, 9, 14)

private fun szReparaturZeit(di: Int): Float = 6.4f + di * 2.3f

private fun DrawScope.szHelix(t: Float, tr: Float, f: Farben, reich: Float, pf: SzPfade) {
    val w = size.width
    val h = size.height
    val hell = szHell(f)
    val oben = if (f.dunkel) lerp(f.hgOben, f.blob1, 0.14f) else lerp(f.blob1, hell, 0.6f)
    val unten = if (f.dunkel) lerp(f.hgUnten, f.blob3, 0.2f) else lerp(f.blob3, hell, 0.55f)
    drawRect(Brush.verticalGradient(listOf(oben, unten)))

    // Laborraster, gleitet langsam
    val raster = f.primaer.copy(alpha = if (f.dunkel) 0.09f else 0.08f)
    val rasterSchritt = h * 0.2f
    var gx = (tr * 4f) % rasterSchritt
    while (gx < w) {
        drawLine(raster, Offset(gx, 0f), Offset(gx, h), 1f)
        gx += rasterSchritt
    }
    var gy = rasterSchritt * 0.5f
    while (gy < h) {
        drawLine(raster, Offset(0f, gy), Offset(w, gy), 1f)
        gy += rasterSchritt
    }

    // Zellmembran, wabert leicht
    val zelle = 0.35f + 0.65f * szWeich(szAn(t, 0.3f, 1.2f)) * (1f - szWeich(szAn(t, 4f, 1.5f)))
    val mx = w * 0.5f
    val my = h * 0.5f
    pf.a.reset()
    for (i in 0..72) {
        val wk = i / 72f * 2f * SZ_PI
        val wob = 1f + 0.025f * sin(wk * 5f + tr * 0.9f) + 0.015f * sin(wk * 3f - tr * 0.6f)
        val px = mx + cos(wk) * w * 0.47f * wob
        val py = my + sin(wk) * h * 0.44f * wob
        if (i == 0) pf.a.moveTo(px, py) else pf.a.lineTo(px, py)
    }
    pf.a.close()
    drawPath(pf.a, f.tertiaer.copy(alpha = 0.06f + 0.06f * zelle))
    drawPath(pf.a, f.tertiaer.copy(alpha = 0.25f + 0.4f * zelle), style = Stroke(h * (0.008f + 0.006f * zelle)))
    val kern = w * 0.35f
    drawCircle(
        Brush.radialGradient(listOf(f.primaer.copy(alpha = 0.12f + 0.12f * zelle + 0.1f * reich), f.primaer.copy(alpha = 0f)), Offset(mx, my), kern),
        kern, Offset(mx, my),
    )

    // Helix-Geometrie: 4 volle Drehungen pro Zyklus → nahtlos
    val x0 = w * 0.13f
    val x1 = w * 0.87f
    val cy = h * 0.5f
    val amp = h * 0.25f
    val theta = t * SZ_PI / 3f
    val k = 2f * SZ_PI * 1.6f / (x1 - x0)
    val erneuer = szWeich(szAn(t, 13f, 1.5f)) * (1f - szAn(t, 22.5f, 1.5f))
    val bandAktiv = szAn(t, 13.4f, 0.4f) * (1f - szAn(t, 17.6f, 0.4f))
    val bandX = szMix(x0, x1, szAn(t, 13.5f, 4f))
    val jugend = szAn(t, 19f, 0.6f) * (1f - szAn(t, 23f, 1f))
    val puls = (szGlocke(t, 20.3f, 0.9f) + 0.7f * szGlocke(t, 21.9f, 0.8f)).coerceAtMost(1f)
    val glanz = (0.12f + 0.3f * reich + 0.6f * puls * jugend).coerceAtMost(1f)

    // Jugend: Leuchtringe breiten sich aus
    if (jugend > 0.01f) for (j in 0 until 2) {
        val p = szAn(t, 19.6f + j * 1.5f, 2.2f)
        if (p <= 0f || p >= 1f) continue
        drawCircle(
            f.sekundaer.copy(alpha = 0.5f * (1f - p) * jugend),
            szMix(h * 0.1f, w * 0.55f, p), Offset(mx, cy),
            style = Stroke(h * 0.012f * (1f - p) + 1f),
        )
    }

    // Stränge: erst die hinteren Hälften, dann die Sprossen, dann die vorderen Hälften
    val segs = 72
    for (pass in 0..1) {
        if (pass == 1) szHelixSprossen(t, tr, f, hell, x0, x1, cy, amp, k, theta, h)
        for (s in 0 until segs) {
            val xa = szMix(x0, x1, s / segs.toFloat())
            val xb = szMix(x0, x1, (s + 1) / segs.toFloat())
            val aa = (xa - x0) * k + theta
            val ab = (xb - x0) * k + theta
            for (strang in 0..1) {
                val vz = if (strang == 0) 1f else -1f
                val z = vz * cos((aa + ab) * 0.5f)
                if ((z < 0f) != (pass == 0)) continue
                val tiefe = (z + 1f) * 0.5f
                val pa = Offset(xa, cy + vz * sin(aa) * amp)
                val pb = Offset(xb, cy + vz * sin(ab) * amp)
                val band = bandAktiv * szGlocke(xa, bandX, w * 0.05f)
                val farbe = lerp(if (strang == 0) f.primaer else f.tertiaer, f.sekundaer, band * 0.8f)
                val breite = h * (0.012f + 0.014f * tiefe) * (1f + 0.5f * band)
                if (glanz > 0.02f || band > 0.02f) {
                    drawLine(farbe.copy(alpha = ((0.18f * glanz + 0.35f * band) * (0.4f + 0.6f * tiefe)).coerceAtMost(1f)), pa, pb, breite * 3f, StrokeCap.Round)
                }
                drawLine(farbe.copy(alpha = 0.35f + 0.65f * tiefe), pa, pb, breite, StrokeCap.Round)
            }
        }
    }

    // Telomer-Kappen an beiden Enden: wachsen und leuchten in der Erneuerung
    val telo = 0.4f + 0.6f * erneuer
    val teloGlanz = (erneuer * (0.6f + 0.4f * sin(tr * 3f)) + 0.2f * reich).coerceAtMost(1f)
    for (ende in 0..1) {
        val xe = if (ende == 0) x0 else x1
        val richtung = if (ende == 0) -1f else 1f
        val a = (xe - x0) * k + theta
        for (strang in 0..1) {
            val vz = if (strang == 0) 1f else -1f
            val ye = cy + vz * sin(a) * amp
            for (p in 0 until 5) {
                val sichtbar = if (p < 2) 1f else (erneuer * 3f - (p - 2)).coerceIn(0f, 1f)
                if (sichtbar <= 0f) continue
                val pos = Offset(xe + richtung * h * 0.035f * (p + 1), ye)
                val pr = h * 0.016f * (1f - p * 0.08f) * (0.6f + 0.4f * sichtbar)
                if (teloGlanz > 0.02f) {
                    drawCircle(
                        Brush.radialGradient(listOf(f.sekundaer.copy(alpha = 0.55f * teloGlanz * sichtbar), f.sekundaer.copy(alpha = 0f)), pos, pr * 3.5f),
                        pr * 3.5f, pos,
                    )
                }
                drawCircle(lerp(f.sekundaer, hell, 0.2f).copy(alpha = telo * sichtbar), pr, pos)
            }
        }
    }

    // Reparatur-Funken fliegen zu den kaputten Sprossen
    for (di in SZ_DEFEKTE.indices) {
        val i = SZ_DEFEKTE[di]
        val rep = szReparaturZeit(di)
        val p = szAn(t, rep - 1.4f, 1.4f)
        if (p <= 0f || p >= 1f) continue
        val x = szMix(x0, x1, (i + 0.5f) / SZ_SPROSSEN)
        val start = Offset(x + (if (i < SZ_SPROSSEN / 2) -1f else 1f) * w * 0.18f, h * 0.06f)
        for (sp in 0 until 5) {
            val q = szWeich((p - sp * 0.05f).coerceIn(0f, 1f))
            val pos = Offset(szMix(start.x, x, q) + sin(q * 9f) * h * 0.04f * (1f - q), szMix(start.y, cy, q))
            if (sp == 0) {
                drawCircle(Brush.radialGradient(listOf(f.sekundaer.copy(alpha = 0.6f), f.sekundaer.copy(alpha = 0f)), pos, h * 0.06f), h * 0.06f, pos)
            }
            drawCircle(lerp(f.sekundaer, hell, 0.15f * sp).copy(alpha = 1f - sp * 0.18f), h * (0.016f - sp * 0.0025f), pos)
        }
    }

    // Jugend: Funkeln — mehr mit dem Fortschritt
    if (jugend > 0.01f) for (i in 0 until 10 + (reich * 10f).toInt()) {
        val sx = szZufall(i, 61) * w
        val sy = szZufall(i, 67) * h
        val fk = 0.5f + 0.5f * sin(tr * (2f + szZufall(i, 71) * 3f) + i)
        val s = h * 0.022f * fk
        val farbe = f.sekundaer.copy(alpha = jugend * fk)
        drawLine(farbe, Offset(sx - s, sy), Offset(sx + s, sy), h * 0.006f, StrokeCap.Round)
        drawLine(farbe, Offset(sx, sy - s), Offset(sx, sy + s), h * 0.006f, StrokeCap.Round)
    }
}

/** Sprossen (Basenpaare) mit Knoten; drei davon gehen kaputt und werden repariert. */
private fun DrawScope.szHelixSprossen(
    t: Float,
    tr: Float,
    f: Farben,
    hell: Color,
    x0: Float,
    x1: Float,
    cy: Float,
    amp: Float,
    k: Float,
    theta: Float,
    h: Float,
) {
    val fa0 = lerp(f.primaer, hell, 0.25f)
    val fb0 = lerp(f.tertiaer, hell, 0.25f)
    for (i in 0 until SZ_SPROSSEN) {
        val x = szMix(x0, x1, (i + 0.5f) / SZ_SPROSSEN)
        val a = (x - x0) * k + theta
        val ya = cy + sin(a) * amp
        val yb = cy - sin(a) * amp
        val za = cos(a)
        val sichtbar = 0.45f + 0.35f * abs(sin(a))
        var defekt = 0f
        var heilBlitz = 0f
        val di = SZ_DEFEKTE.indexOf(i)
        if (di >= 0) {
            val schaden = szWeich(szAn(t, 0.8f + di * 0.5f, 1.2f))
            val rep = szReparaturZeit(di)
            defekt = schaden * (1f - szWeich(szAn(t, rep, 0.6f)))
            heilBlitz = szGlocke(t, rep + 0.35f, 0.3f)
        }
        val halb = (yb - ya) * 0.5f
        val luecke = 0.45f * defekt
        val dicke = h * 0.012f
        drawLine(lerp(fa0, f.gefahr, defekt).copy(alpha = sichtbar), Offset(x, ya), Offset(x, ya + halb * (1f - luecke)), dicke, StrokeCap.Round)
        drawLine(lerp(fb0, f.gefahr, defekt).copy(alpha = sichtbar), Offset(x, yb), Offset(x, yb - halb * (1f - luecke)), dicke, StrokeCap.Round)
        val mitte = Offset(x, (ya + yb) * 0.5f)
        if (defekt > 0.05f) {
            val flackern = 0.6f + 0.4f * sin(tr * 8f + i)
            drawCircle(f.gefahr.copy(alpha = 0.35f * defekt * flackern), h * 0.035f, mitte)
        }
        if (heilBlitz > 0.02f) {
            val br = h * 0.1f * (0.5f + heilBlitz)
            drawCircle(Brush.radialGradient(listOf(f.erfolg.copy(alpha = 0.8f * heilBlitz), f.erfolg.copy(alpha = 0f)), mitte, br), br, mitte)
        }
        drawCircle(f.primaer.copy(alpha = 0.5f + 0.5f * (za + 1f) * 0.5f), h * (0.012f + 0.01f * (za + 1f) * 0.5f), Offset(x, ya))
        drawCircle(f.tertiaer.copy(alpha = 0.5f + 0.5f * (1f - za) * 0.5f), h * (0.012f + 0.01f * (1f - za) * 0.5f), Offset(x, yb))
    }
}
