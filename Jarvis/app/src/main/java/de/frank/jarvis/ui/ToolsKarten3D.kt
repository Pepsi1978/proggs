package de.frank.jarvis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import de.frank.jarvis.atem.AtemFarben
import de.frank.jarvis.ui.theme.Farben
import de.frank.jarvis.ui.theme.LocalFarben
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val TAU = (2.0 * PI).toFloat()
private const val FOKAL = 3.25f

private val ATEM_EIN = Color(AtemFarben.EIN)
private val ATEM_WIND_CYAN = Color(0xFF56DCFF)
private val ATEM_HALT = Color(AtemFarben.HALTEN)
private val ATEM_AUS = Color(AtemFarben.AUS)
private val ATEM_WARM = Color(0xFFFFCA7A)

private val HIRN_FOKUS = Color(0xFF6B8CFF)
private val HIRN_CYAN = Color(0xFF4FC8FF)
private val HIRN_KREATIV = Color(0xFFB98CFF)
private val HIRN_MAGENTA = Color(0xFFE58CFF)
private val HIRN_RUHE = Color(0xFF4FE0B5)
private val HIRN_GOLD = Color(0xFFFFD166)

private fun weich(x: Float): Float = 0.5f - 0.5f * cos(PI.toFloat() * x.coerceIn(0f, 1f))

/**
 * 3D-Bühne für eine Karte im Bereich „Tools“: zeigt ohne Beschreibungstext direkt auf der Karte,
 * wofür das jeweilige Tool da ist. Die Uhr [zeitMs] wird erst beim Zeichnen gelesen, sodass
 * weder die Karte noch das Raster pro Bild neu zusammengesetzt werden.
 */
@Composable
internal fun ToolKarten3DBuehne(
    toolId: String,
    zeitMs: LongState,
    modifier: Modifier = Modifier,
) {
    val f = LocalFarben.current
    val form = RoundedCornerShape(18.dp)
    val buehnenHintergrund = remember(toolId, f.dunkel) { buehnenVerlauf(toolId, f) }
    val randFarbe = remember(toolId, f.dunkel) {
        val akzent = if (toolId == "atem") ATEM_WIND_CYAN else HIRN_KREATIV
        akzent.copy(alpha = if (f.dunkel) 0.28f else 0.36f)
    }
    // Wiederverwendbare Zeichenpuffer je Karte: keine Objekt-Erzeugung im 60/120-Hz-Takt.
    val atemPuffer = remember(toolId) { if (toolId == "atem") AtemZeichenPuffer() else null }
    val hirnPuffer = remember(toolId) { if (toolId == "mentalitaet") GehirnZeichenPuffer() else null }

    Box(
        modifier
            .clip(form)
            .background(buehnenHintergrund)
            .border(1.dp, randFarbe, form),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(),
        ) {
            val t = zeitMs.longValue / 1000f
            when (toolId) {
                "atem" -> zeichneAtem3D(t, atemPuffer ?: return@Canvas)
                "mentalitaet" -> zeichneGehirn3D(t, hirnPuffer ?: return@Canvas)
            }
        }
    }
}

private fun buehnenVerlauf(toolId: String, f: Farben): Brush = when (toolId) {
    "atem" -> if (f.dunkel) {
        Brush.radialGradient(listOf(Color(0xFF0E2938), Color(0xFF081624), Color(0xFF050D17)))
    } else {
        Brush.radialGradient(listOf(Color(0xFF153B52), Color(0xFF0F273B), Color(0xFF0B1C2D)))
    }
    else -> if (f.dunkel) {
        Brush.radialGradient(listOf(Color(0xFF1E1942), Color(0xFF110F29), Color(0xFF09091A)))
    } else {
        Brush.radialGradient(listOf(Color(0xFF282258), Color(0xFF1B1740), Color(0xFF13112E)))
    }
}

// ================================================================================================
// 3D-Szene 1: Atemübungen — 3D-Atemkugel mit sichtbaren Luftströmungen und Wind (Ein- & Ausatmen)
// ================================================================================================

private const val ATEM_ZYKLUS_S = 7.6f
private const val WIND_SEGMENTE = 14
private const val RING_PUNKTE = 40

private data class WindStrom(
    val winkel: Float,
    val neigung: Float,
    val drall: Float,
    val welle: Float,
    val phase: Float,
    val abstandAussen: Float,
    val tempo: Float,
    val breite: Float,
    val istBoee: Boolean,
)

private data class LuftPartikel(
    val winkel: Float,
    val neigung: Float,
    val drall: Float,
    val phase: Float,
    val tempo: Float,
    val groesse: Float,
)

private data class WindBand(
    val yVersatz: Float,
    val zTiefe: Float,
    val amplitude: Float,
    val phase: Float,
    val breite: Float,
)

private object AtemModell {
    val stroeme: List<WindStrom> = run {
        val r = Random(31)
        List(16) { i ->
            val basis = (i.toFloat() / 16f) * TAU + (r.nextFloat() - 0.5f) * 0.26f
            // Ausgewogen über obere, mittlere und untere 3D-Breitengrade verteilt.
            val elev = ((i % 4) - 1.5f) * 0.44f + (r.nextFloat() - 0.5f) * 0.16f
            WindStrom(
                winkel = basis,
                neigung = elev,
                drall = (if (i % 2 == 0) 1f else -1f) * (0.75f + r.nextFloat() * 0.55f),
                welle = 0.08f + r.nextFloat() * 0.09f,
                phase = r.nextFloat(),
                abstandAussen = 1.28f + r.nextFloat() * 0.24f,
                tempo = 0.85f + r.nextFloat() * 0.35f,
                breite = if (i % 3 == 0) 2.5f else 1.75f,
                istBoee = i % 3 == 0,
            )
        }
    }

    val partikel: List<LuftPartikel> = run {
        val r = Random(73)
        List(38) {
            LuftPartikel(
                winkel = r.nextFloat() * TAU,
                neigung = (r.nextFloat() - 0.5f) * 1.55f,
                drall = (if (r.nextBoolean()) 1f else -1f) * (0.5f + r.nextFloat() * 0.8f),
                phase = r.nextFloat(),
                tempo = 0.72f + r.nextFloat() * 0.65f,
                groesse = 1.3f + r.nextFloat() * 2.0f,
            )
        }
    }

    // Vier weite 3D-Windbänder in unterschiedlichen Tiefenebenen, die sich beim Einatmen
    // zusammenziehen und beim Ausatmen wie sichtbare Windböen aufweiten.
    val baender: List<WindBand> = listOf(
        WindBand(yVersatz = -0.34f, zTiefe = 0.46f, amplitude = 0.18f, phase = 0.0f, breite = 2.0f),
        WindBand(yVersatz = 0.02f, zTiefe = -0.52f, amplitude = 0.22f, phase = 2.1f, breite = 2.4f),
        WindBand(yVersatz = 0.36f, zTiefe = 0.32f, amplitude = 0.16f, phase = 4.2f, breite = 1.9f),
        WindBand(yVersatz = -0.14f, zTiefe = -0.36f, amplitude = 0.19f, phase = 1.1f, breite = 1.7f),
    )
}

private class AtemZeichenPuffer {
    val sx = FloatArray(WIND_SEGMENTE + 1)
    val sy = FloatArray(WIND_SEGMENTE + 1)
    val sz = FloatArray(WIND_SEGMENTE + 1)
    val ss = FloatArray(WIND_SEGMENTE + 1)
    val ringX = FloatArray(RING_PUNKTE + 1)
    val ringY = FloatArray(RING_PUNKTE + 1)
    val ringZ = FloatArray(RING_PUNKTE + 1)
}

private fun DrawScope.zeichneAtem3D(t: Float, puffer: AtemZeichenPuffer) {
    val mitteX = size.width * 0.5f
    val mitteY = size.height * 0.51f
    val skala = size.minDimension * 0.42f

    // Vollständig nahtlose (C-unendlich) Atem-Schleife ohne Phasen-Schnitt oder Sprung:
    // atemStand ∈ [0, 1] schwingt weich über -cos(w), die Strömungsgeschwindigkeit fluss ∈ [-1, +1]
    // ist ihre exakte Ableitung sin(w): +1 = Einatmen (nach innen), 0 = ruhiger Wendepunkt, -1 = Ausatmen (nach außen).
    val w = (t / ATEM_ZYKLUS_S) * TAU
    val atemStand = 0.5f - 0.5f * cos(w)
    val fluss = sin(w)
    val flussBetrag = abs(fluss)

    // Durchgängige, stufenlose Farbmischung über den ganzen Zyklus (kein Übergangs-Knick bei 0 oder 0,5).
    val einAnteil = 0.5f + 0.5f * fluss // 1 beim Einatmen, 0 beim Ausatmen, 0,5 an den Wendepunkten
    val basisFarbe = lerp(ATEM_AUS, ATEM_EIN, weich(einAnteil))
    val wendeFarbe = lerp(ATEM_WIND_CYAN, ATEM_HALT, atemStand)
    val wendeGewicht = (1f - flussBetrag) * 0.48f
    val hauptFarbe = lerp(basisFarbe, wendeFarbe, wendeGewicht)
    val windFarbe = lerp(
        lerp(ATEM_WARM, ATEM_WIND_CYAN, weich(einAnteil)),
        Color.White,
        0.24f + 0.14f * flussBetrag,
    )

    // Kontinuierlicher integraler Strömungsweg: wächst beim Einatmen stetig und kehrt am Wendepunkt
    // mit Geschwindigkeit 0 völlig ruckfrei um (niemals ein Phasen-Reset oder Sprung).
    val stromWeg = atemStand * 1.18f

    val yaw = t * 0.26f + sin(t * 0.38f) * 0.16f
    val pitch = 0.28f + 0.08f * sin(t * 0.52f)
    val cy = cos(yaw); val sy = sin(yaw)
    val cx = cos(pitch); val sx = sin(pitch)

    val kugelR = 0.24f + atemStand * 0.23f
    val kugelPx = kugelR * skala

    // Weicher atmosphärischer Raumschein im Hintergrund
    val hofR = skala * (0.95f + atemStand * 0.45f)
    drawCircle(
        Brush.radialGradient(
            listOf(
                hauptFarbe.copy(alpha = 0.30f + atemStand * 0.12f),
                windFarbe.copy(alpha = 0.09f),
                Color.Transparent,
            ),
            Offset(mitteX, mitteY),
            hofR,
        ),
        radius = hofR,
        center = Offset(mitteX, mitteY),
    )

    // 1) HINTERE EBENE (z > 0): Windbänder, Luftströme, Luftteilchen und Ringe hinter der 3D-Kugel
    zeichneWindBaender(puffer, mitteX, mitteY, skala, t, atemStand, fluss, windFarbe, vorne = false, cy, sy, cx, sx)
    zeichneWindStroeme(puffer, mitteX, mitteY, skala, t, kugelR, stromWeg, fluss, windFarbe, hauptFarbe, vorne = false, cy, sy, cx, sx)
    zeichneLuftPartikel(mitteX, mitteY, skala, t, kugelR, stromWeg, fluss, windFarbe, vorne = false, cy, sy, cx, sx)
    zeichneAtemRinge(puffer, mitteX, mitteY, skala, kugelR, t, hauptFarbe, windFarbe, vorne = false, cy, sy, cx, sx)

    // 2) MITTE: 3D-Atemkugel mit Volumen, Glanzpunkt, innerem Luftkern und Fresnel-Rand
    val lichtOrt = Offset(mitteX - kugelPx * 0.30f, mitteY - kugelPx * 0.34f)
    drawCircle(
        Brush.radialGradient(
            listOf(
                lerp(hauptFarbe, Color.White, 0.72f).copy(alpha = 0.96f),
                hauptFarbe.copy(alpha = 0.88f),
                lerp(hauptFarbe, Color(0xFF061321), 0.56f).copy(alpha = 0.82f),
            ),
            center = lichtOrt,
            radius = (kugelPx * 1.38f).coerceAtLeast(1f),
        ),
        radius = kugelPx,
        center = Offset(mitteX, mitteY),
    )
    // Innerer heller Luftkern (pulsierendes Volumen beim Einatmen)
    drawCircle(
        Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = 0.52f + atemStand * 0.25f),
                windFarbe.copy(alpha = 0.26f),
                Color.Transparent,
            ),
            center = Offset(mitteX - kugelPx * 0.12f, mitteY - kugelPx * 0.14f),
            radius = (kugelPx * 0.76f).coerceAtLeast(1f),
        ),
        radius = kugelPx * 0.72f,
        center = Offset(mitteX, mitteY),
        blendMode = BlendMode.Plus,
    )
    // Glanzlicht oben links für 3D-Plastizität
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.68f), Color.Transparent),
            center = Offset(mitteX - kugelPx * 0.32f, mitteY - kugelPx * 0.34f),
            radius = (kugelPx * 0.38f).coerceAtLeast(1f),
        ),
        radius = kugelPx * 0.38f,
        center = Offset(mitteX - kugelPx * 0.32f, mitteY - kugelPx * 0.34f),
    )
    // Fresnel-Kante der Kugel
    drawCircle(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.58f), hauptFarbe.copy(alpha = 0.28f), windFarbe.copy(alpha = 0.52f)),
            Offset(mitteX - kugelPx, mitteY - kugelPx),
            Offset(mitteX + kugelPx, mitteY + kugelPx),
        ),
        radius = kugelPx,
        center = Offset(mitteX, mitteY),
        style = Stroke(width = 1.4f * density),
    )

    // 3) VORDERE EBENE (z <= 0): 3D-Ringe, Windströme, Windbänder und Partikel VOR der Kugel
    zeichneAtemRinge(puffer, mitteX, mitteY, skala, kugelR, t, hauptFarbe, windFarbe, vorne = true, cy, sy, cx, sx)
    zeichneWindStroeme(puffer, mitteX, mitteY, skala, t, kugelR, stromWeg, fluss, windFarbe, hauptFarbe, vorne = true, cy, sy, cx, sx)
    zeichneWindBaender(puffer, mitteX, mitteY, skala, t, atemStand, fluss, windFarbe, vorne = true, cy, sy, cx, sx)
    zeichneLuftPartikel(mitteX, mitteY, skala, t, kugelR, stromWeg, fluss, windFarbe, vorne = true, cy, sy, cx, sx)
}

private fun DrawScope.zeichneAtemRinge(
    puffer: AtemZeichenPuffer,
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    kugelR: Float,
    t: Float,
    hauptFarbe: Color,
    windFarbe: Color,
    vorne: Boolean,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    // Drei gekippte 3D-Atemringe um die Kugel, in Vorder- und Rückhälfte geteilt.
    for (ring in 0 until 3) {
        val radius = kugelR * (1.22f + ring * 0.14f)
        val kippX = 0.45f + ring * 0.62f
        val kippZ = -0.35f + ring * 0.54f
        val dreh = t * (0.45f + ring * 0.18f) * (if (ring % 2 == 0) 1f else -1f)
        val ckx = cos(kippX); val skx = sin(kippX)
        val ckz = cos(kippZ); val skz = sin(kippZ)

        for (i in 0..RING_PUNKTE) {
            val a = (i.toFloat() / RING_PUNKTE) * TAU + dreh
            val lx = cos(a) * radius
            val lz0 = sin(a) * radius
            val ly1 = -lz0 * skx
            val lz1 = lz0 * ckx
            val lx2 = lx * ckz - ly1 * skz
            val ly2 = lx * skz + ly1 * ckz
            val rx = lx2 * cy + lz1 * sy
            val rz1 = -lx2 * sy + lz1 * cy
            val ry = ly2 * cx - rz1 * sx
            val rz = ly2 * sx + rz1 * cx
            val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
            puffer.ringX[i] = mitteX + rx * skala * s
            puffer.ringY[i] = mitteY + ry * skala * s
            puffer.ringZ[i] = rz
        }

        val farbe = if (ring == 1) windFarbe else hauptFarbe
        val basisAlpha = if (vorne) 0.44f - ring * 0.07f else 0.16f - ring * 0.03f
        val dicke = (if (vorne) 1.55f else 1.0f) * density
        for (i in 0 until RING_PUNKTE) {
            val zMitte = (puffer.ringZ[i] + puffer.ringZ[i + 1]) * 0.5f
            if (vorne && zMitte > 0f) continue
            if (!vorne && zMitte <= 0f) continue
            drawLine(
                color = farbe.copy(alpha = basisAlpha.coerceAtLeast(0.05f)),
                start = Offset(puffer.ringX[i], puffer.ringY[i]),
                end = Offset(puffer.ringX[i + 1], puffer.ringY[i + 1]),
                strokeWidth = dicke,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.zeichneWindStroeme(
    puffer: AtemZeichenPuffer,
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    t: Float,
    kugelR: Float,
    stromWeg: Float,
    fluss: Float,
    windFarbe: Color,
    hauptFarbe: Color,
    vorne: Boolean,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    val flussBetrag = abs(fluss)
    // Sanfter Wechsel der Wirbelweite ohne Sprung (1,05 beim Einatmen <-> 1,45 beim Ausatmen)
    val drallStaerke = 1.25f - 0.20f * fluss

    for (strom in AtemModell.stroeme) {
        // kopfU bewegt sich völlig stetig über stromWeg (Einatmen: 0->1 nach innen,
        // am Wendepunkt Geschwindigkeit 0, Ausatmen: 1->0 nach außen).
        // Am Umbruch (0 und 1) geht lebensHuelle exakt auf 0 -> absolut unsichtbarer Übergang.
        val kopfU = ((strom.phase + stromWeg * strom.tempo) % 1f + 1f) % 1f
        val schweifBasis = if (strom.istBoee) 0.50f else 0.38f
        val radialeLaenge = schweifBasis * fluss
        // An den Wendepunkten (fluss ~ 0) schwingt der Luftstrom weich tangential um die Kugel weiter,
        // statt zusammenzuschrumpfen oder umzuspringen.
        val tangentialBogen = (1f - flussBetrag) * 0.42f * strom.drall
        var zSumme = 0f

        for (i in 0..WIND_SEGMENTE) {
            val fSeg = i.toFloat() / WIND_SEGMENTE // 0 = Schweifende, 1 = führende Windspitze
            val schweif = 1f - fSeg
            val u = (kopfU - schweif * radialeLaenge).coerceIn(0f, 1f)
            val r = strom.abstandAussen * (1f - u) + (kugelR * 0.94f) * u
            val wirbelWinkel = strom.winkel +
                t * 0.22f * (if (strom.drall >= 0f) 1f else -1f) +
                strom.drall * (1f - u) * drallStaerke -
                schweif * tangentialBogen +
                sin(t * 1.25f + strom.phase * TAU + u * 3.5f) * 0.14f
            val elev = strom.neigung * (0.45f + 0.55f * (1f - u)) +
                sin(u * 5.0f - t * 1.9f + strom.phase * TAU) * strom.welle

            val cosE = cos(elev)
            val x3 = r * cosE * cos(wirbelWinkel)
            val y3 = r * sin(elev)
            val z3 = r * cosE * sin(wirbelWinkel)

            val rx = x3 * cy + z3 * sy
            val rz1 = -x3 * sy + z3 * cy
            val ry = y3 * cx - rz1 * sx
            val rz = y3 * sx + rz1 * cx
            val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.65f)

            puffer.sx[i] = mitteX + rx * skala * s
            puffer.sy[i] = mitteY + ry * skala * s
            puffer.sz[i] = rz
            puffer.ss[i] = s
            zSumme += rz
        }

        val zMitte = zSumme / (WIND_SEGMENTE + 1)
        if (vorne && zMitte > 0f) continue
        if (!vorne && zMitte <= 0f) continue

        // Quadratische Sinushülle: bei kopfU = 0 und 1 exakt 0,0 Alpha samt Ableitung 0 -> kein Aufblitzen/Springen.
        val h = sin(kopfU * PI.toFloat()).coerceIn(0f, 1f)
        val lebensHuelle = h * h
        if (lebensHuelle < 0.008f) continue
        val tiefenFaktor = ((1.35f - zMitte) / 2.7f).coerceIn(0.28f, 1f)
        val grundAlpha = (0.34f + 0.58f * flussBetrag) * lebensHuelle * tiefenFaktor

        for (i in 0 until WIND_SEGMENTE) {
            val fSeg = (i + 0.5f) / WIND_SEGMENTE
            val profil = sin(fSeg * PI.toFloat()).coerceAtLeast(0.06f) * (0.42f + 0.58f * fSeg)
            val segAlpha = (grundAlpha * profil).coerceIn(0f, 0.92f)
            if (segAlpha < 0.015f) continue
            val farbe = lerp(hauptFarbe, Color.White, fSeg * 0.62f).let { lerp(it, windFarbe, 0.45f) }
            val dicke = strom.breite * density * puffer.ss[i] * (0.42f + 0.85f * profil)
            drawLine(
                color = farbe.copy(alpha = segAlpha),
                start = Offset(puffer.sx[i], puffer.sy[i]),
                end = Offset(puffer.sx[i + 1], puffer.sy[i + 1]),
                strokeWidth = dicke,
                cap = StrokeCap.Round,
            )
        }

        // Leuchtende Windspitze zeigt fließend die Strömungsrichtung
        val spitzeAlpha = (grundAlpha * (0.45f + 0.55f * flussBetrag)).coerceIn(0f, 0.95f)
        if (spitzeAlpha > 0.04f) {
            val spX = puffer.sx[WIND_SEGMENTE]
            val spY = puffer.sy[WIND_SEGMENTE]
            val spR = (if (strom.istBoee) 2.5f else 1.85f) * density * puffer.ss[WIND_SEGMENTE]
            drawCircle(Color.White.copy(alpha = spitzeAlpha), spR, Offset(spX, spY))
        }
    }
}

private fun DrawScope.zeichneWindBaender(
    puffer: AtemZeichenPuffer,
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    t: Float,
    atemStand: Float,
    fluss: Float,
    windFarbe: Color,
    vorne: Boolean,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    val flussBetrag = abs(fluss)
    // Durchgängige Phase (integrale Atembewegung + ruhige Grundströmung): niemals ein Richtungssprung.
    val schub = -atemStand * 3.6f + t * 0.42f
    val weite = 0.88f + (1f - atemStand) * 0.34f

    for (band in AtemModell.baender) {
        var zMitte = 0f
        for (i in 0..WIND_SEGMENTE) {
            val u = (i.toFloat() / WIND_SEGMENTE) * 2f - 1f
            val x3 = u * 1.28f * weite
            val bogen = sin(u * 3.1f + schub + band.phase) * band.amplitude * (0.68f + 0.38f * flussBetrag)
            // Stufenloser Sog nach innen beim Einatmen (fluss > 0) und weiches Aufweiten beim Ausatmen (fluss < 0).
            val sog = (1f - u * u) * (-0.20f * fluss) * band.yVersatz
            val y3 = band.yVersatz * (0.72f + 0.35f * (1f - atemStand)) + bogen + sog
            val z3 = band.zTiefe * cos(u * 0.9f) + sin(u * 2.2f + band.phase + t * 0.35f) * 0.14f

            val rx = x3 * cy + z3 * sy
            val rz1 = -x3 * sy + z3 * cy
            val ry = y3 * cx - rz1 * sx
            val rz = y3 * sx + rz1 * cx
            val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
            puffer.sx[i] = mitteX + rx * skala * s
            puffer.sy[i] = mitteY + ry * skala * s
            puffer.sz[i] = rz
            puffer.ss[i] = s
            zMitte += rz
        }
        zMitte /= (WIND_SEGMENTE + 1)
        if (vorne && zMitte > 0f) continue
        if (!vorne && zMitte <= 0f) continue

        val tiefenAlpha = (if (vorne) 0.44f else 0.18f) * (0.62f + 0.38f * flussBetrag)
        for (i in 0 until WIND_SEGMENTE) {
            val fSeg = (i + 0.5f) / WIND_SEGMENTE
            val huelle = sin(fSeg * PI.toFloat())
            val alpha = tiefenAlpha * huelle
            if (alpha < 0.015f) continue
            drawLine(
                color = lerp(windFarbe, Color.White, 0.5f).copy(alpha = alpha),
                start = Offset(puffer.sx[i], puffer.sy[i]),
                end = Offset(puffer.sx[i + 1], puffer.sy[i + 1]),
                strokeWidth = band.breite * density * puffer.ss[i] * (0.45f + 0.65f * huelle),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.zeichneLuftPartikel(
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    t: Float,
    kugelR: Float,
    stromWeg: Float,
    fluss: Float,
    windFarbe: Color,
    vorne: Boolean,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    val flussBetrag = abs(fluss)
    // Allokationsfreie, stetige 3D-Luftpartikel ohne Umbruch-Sprung (Alpha am Rand exakt 0).
    for (p in AtemModell.partikel) {
        val u = ((p.phase + stromWeg * p.tempo * 1.15f) % 1f + 1f) % 1f
        val h = sin(u * PI.toFloat()).coerceIn(0f, 1f)
        val huelle = h * h
        if (huelle < 0.01f) continue

        val r1 = 1.44f * (1f - u) + (kugelR * 0.96f) * u
        val w1 = p.winkel + p.drall * (1f - u) + t * 0.24f
        val e1 = p.neigung * (0.4f + 0.6f * (1f - u))
        val cosE1 = cos(e1)
        val x31 = r1 * cosE1 * cos(w1)
        val y31 = r1 * sin(e1)
        val z31 = r1 * cosE1 * sin(w1)
        val rx1 = x31 * cy + z31 * sy
        val rzTmp1 = -x31 * sy + z31 * cy
        val ry1 = y31 * cx - rzTmp1 * sx
        val rz1 = y31 * sx + rzTmp1 * cx

        if (vorne && rz1 > 0f) continue
        if (!vorne && rz1 <= 0f) continue

        val uVorher = (u - 0.085f * fluss).coerceIn(0f, 1f)
        val r0 = 1.44f * (1f - uVorher) + (kugelR * 0.96f) * uVorher
        // Kleiner tangentialer Schweif bleibt auch am Wendepunkt sanft sichtbar
        val w0 = p.winkel + p.drall * (1f - uVorher) + t * 0.24f - 0.035f
        val e0 = p.neigung * (0.4f + 0.6f * (1f - uVorher))
        val cosE0 = cos(e0)
        val x30 = r0 * cosE0 * cos(w0)
        val y30 = r0 * sin(e0)
        val z30 = r0 * cosE0 * sin(w0)
        val rx0 = x30 * cy + z30 * sy
        val rzTmp0 = -x30 * sy + z30 * cy
        val ry0 = y30 * cx - rzTmp0 * sx
        val rz0 = y30 * sx + rzTmp0 * cx

        val s1 = FOKAL / (FOKAL + rz1).coerceAtLeast(0.7f)
        val s0 = FOKAL / (FOKAL + rz0).coerceAtLeast(0.7f)
        val alpha = ((0.30f + 0.60f * flussBetrag) * huelle * if (vorne) 1f else 0.45f).coerceIn(0f, 0.92f)
        if (alpha < 0.02f) continue

        drawLine(
            color = lerp(windFarbe, Color.White, 0.55f).copy(alpha = alpha),
            start = Offset(mitteX + rx0 * skala * s0, mitteY + ry0 * skala * s0),
            end = Offset(mitteX + rx1 * skala * s1, mitteY + ry1 * skala * s1),
            strokeWidth = p.groesse * density * s1 * 0.72f,
            cap = StrokeCap.Round,
        )
    }
}

// ================================================================================================
// 3D-Szene 2: Mentalität — Animiertes 3D-Gehirn für Denken, Lösungswege und innere Einstellungen
// ================================================================================================

private const val HIRN_FALTE_PUNKTE = 11
private const val WEG_ZYKLUS_S = 9.0f
private const val EINSTELLUNG_ZYKLUS_S = 13.5f

private data class HirnKnoten(val x: Float, val y: Float, val z: Float, val seite: Int, val gewicht: Float)
private data class HirnKante(val a: Int, val b: Int, val bruecke: Boolean)
private data class HirnFalte(val punkteX: FloatArray, val punkteY: FloatArray, val punkteZ: FloatArray, val seite: Int)
private data class LoesungsWeg(val knoten: IntArray, val farbe: Color, val versatz: Float)

private object GehirnModell {
    val knoten: List<HirnKnoten>
    val kanten: List<HirnKante>
    val falten: List<HirnFalte>
    val wege: List<LoesungsWeg>

    /** Formt einen normierten Richtungsvektor auf eine erkennbare 3D-Gehirnhälfte (links/rechts). */
    private fun hirnPunkt(seite: Int, laenge: Float, hoehe: Float, weite: Float): Triple<Float, Float, Float> {
        // laenge: -1 (Hinterhaupt/Okzipital) .. +1 (Stirn/Frontal)
        // hoehe:  -1 (Scheitel oben) .. +1 (Basis unten)
        // weite:   0 (Mittellinie/Fissur) .. 1 (äußere Schläfenwand)
        val z = laenge * 0.96f
        val kuppel = sqrt((1f - (laenge * 0.86f) * (laenge * 0.86f)).coerceAtLeast(0.06f))
        // Schläfenlappen wölbt sich seitlich leicht nach unten-vorn, Stirn rundet weich ab.
        val schlaefe = if (hoehe > 0.1f && laenge > -0.35f && laenge < 0.55f) 0.14f * weite else 0f
        val y = hoehe * 0.62f * kuppel - 0.04f + schlaefe
        val spalt = 0.055f + 0.04f * (1f - abs(hoehe))
        val breiteHuelle = 0.64f * kuppel * (0.86f + 0.18f * (1f - abs(laenge + 0.1f)))
        val x = seite * (spalt + weite * breiteHuelle)
        return Triple(x, y, z)
    }

    init {
        val kListe = mutableListOf<HirnKnoten>()

        // 0: Zentraler Ursprungsknoten im präfrontalen Bereich (Ausgangspunkt der Gedanken)
        kListe += HirnKnoten(0f, -0.06f, 0.58f, 0, 1.35f)
        // 1: Zentrale Balken-Brücke (Corpus callosum)
        kListe += HirnKnoten(0f, -0.18f, 0.05f, 0, 1.15f)
        // 2: Hintere Konvergenz / Erkenntnis-Zentrum
        kListe += HirnKnoten(0f, -0.08f, -0.62f, 0, 1.25f)

        // Je Hemisphäre 15 anatomisch verteilte Kortex-Knoten (3..17 links, 18..32 rechts)
        val muster = listOf(
            // Stirnlappen (Frontal)
            Triple(0.76f, -0.36f, 0.42f),
            Triple(0.58f, -0.68f, 0.52f),
            Triple(0.68f, 0.12f, 0.64f),
            // Scheitellappen (Parietal, obere Kuppel)
            Triple(0.22f, -0.84f, 0.46f),
            Triple(-0.18f, -0.88f, 0.50f),
            Triple(0.18f, -0.46f, 0.86f),
            Triple(-0.24f, -0.48f, 0.88f),
            // Schläfenlappen (Temporal, seitlich unten)
            Triple(0.32f, 0.24f, 0.94f),
            Triple(-0.04f, 0.34f, 0.96f),
            Triple(-0.38f, 0.26f, 0.82f),
            // Hinterhauptslappen (Okzipital)
            Triple(-0.62f, -0.54f, 0.54f),
            Triple(-0.82f, -0.14f, 0.48f),
            Triple(-0.74f, 0.26f, 0.44f),
            // Tiefe innere Knoten (Denk-Netzwerk im Inneren)
            Triple(0.10f, -0.10f, 0.38f),
            Triple(-0.34f, -0.06f, 0.36f),
        )
        for (seite in listOf(-1, 1)) {
            muster.forEachIndexed { idx, (l, h, w) ->
                val (x, y, z) = hirnPunkt(seite, l, h, w)
                val gewicht = if (idx in listOf(0, 3, 7, 11)) 1.15f else 0.92f
                kListe += HirnKnoten(x, y, z, seite, gewicht)
            }
        }
        knoten = kListe

        // Kanten: lokale Kortex-Verbindungen innerhalb jeder Hälfte + Querverbindungen zwischen beiden Hälften
        val eListe = mutableListOf<HirnKante>()
        val gesehen = HashSet<Int>()
        fun fuegeKanteHinzu(a: Int, b: Int, bruecke: Boolean) {
            val lo = minOf(a, b); val hi = maxOf(a, b)
            val key = lo * 100 + hi
            if (gesehen.add(key)) eListe += HirnKante(lo, hi, bruecke)
        }

        val lokalPaare = listOf(
            0 to 1, 0 to 2, 0 to 13, 1 to 3, 1 to 5, 2 to 7, 2 to 13,
            3 to 4, 3 to 5, 3 to 13, 4 to 6, 4 to 10, 5 to 6, 5 to 7, 5 to 8,
            6 to 8, 6 to 9, 6 to 10, 6 to 14, 7 to 8, 7 to 13, 8 to 9, 8 to 14,
            9 to 11, 9 to 12, 9 to 14, 10 to 11, 10 to 14, 11 to 12, 11 to 14, 13 to 14,
        )
        for (halb in 0..1) {
            val basis = 3 + halb * 15
            lokalPaare.forEach { (a, b) -> fuegeKanteHinzu(basis + a, basis + b, false) }
            fuegeKanteHinzu(0, basis + 0, true)
            fuegeKanteHinzu(0, basis + 2, true)
            fuegeKanteHinzu(0, basis + 13, true)
            fuegeKanteHinzu(1, basis + 3, true)
            fuegeKanteHinzu(1, basis + 4, true)
            fuegeKanteHinzu(1, basis + 13, true)
            fuegeKanteHinzu(1, basis + 14, true)
            fuegeKanteHinzu(2, basis + 10, true)
            fuegeKanteHinzu(2, basis + 11, true)
            fuegeKanteHinzu(2, basis + 14, true)
        }
        listOf(1, 3, 4, 10, 13, 14).forEach { idx ->
            fuegeKanteHinzu(3 + idx, 18 + idx, true)
        }
        kanten = eListe

        // 3D-Hirnwindungen (Gyri) auf beiden Hemisphären: verleihen sofort die ikonische Gehirnform.
        val fListe = mutableListOf<HirnFalte>()
        for (seite in listOf(-1, 1)) {
            val spuren = listOf(
                Triple(0.84f, -0.82f, 0.24f) to Pair(-0.56f, 0.16f),
                Triple(0.82f, -0.84f, 0.56f) to Pair(-0.50f, 0.18f),
                Triple(0.76f, -0.82f, 0.82f) to Pair(-0.22f, 0.18f),
                Triple(0.78f, -0.78f, 0.94f) to Pair(0.06f, 0.16f),
                Triple(0.56f, -0.68f, 0.90f) to Pair(0.32f, 0.13f),
                Triple(0.68f, 0.14f, 0.72f) to Pair(-0.34f, 0.22f),
                Triple(-0.14f, -0.76f, 0.74f) to Pair(-0.36f, 0.20f),
                Triple(-0.34f, -0.86f, 0.48f) to Pair(0.56f, 0.08f),
            )
            spuren.forEachIndexed { fIdx, (span, hoeheInfo) ->
                val (lStart, lEnde, basisWeite) = span
                val (basisHoehe, welligkeit) = hoeheInfo
                val px = FloatArray(HIRN_FALTE_PUNKTE)
                val py = FloatArray(HIRN_FALTE_PUNKTE)
                val pz = FloatArray(HIRN_FALTE_PUNKTE)
                for (i in 0 until HIRN_FALTE_PUNKTE) {
                    val u = i.toFloat() / (HIRN_FALTE_PUNKTE - 1)
                    val l = lStart + (lEnde - lStart) * u
                    val schwing = sin(u * PI.toFloat() * 3.0f + fIdx * 1.15f)
                    val h = (basisHoehe + schwing * welligkeit + (u - 0.5f) * (u - 0.5f) * 0.24f).coerceIn(-0.92f, 0.88f)
                    val w = (basisWeite * (0.74f + 0.26f * sin(u * PI.toFloat())) + cos(u * PI.toFloat() * 2.5f + fIdx) * 0.07f).coerceIn(0.12f, 1.0f)
                    val (x, y, z) = hirnPunkt(seite, l, h, w)
                    px[i] = x; py[i] = y; pz[i] = z
                }
                fListe += HirnFalte(px, py, pz, seite)
            }
        }
        falten = fListe

        // 4 verzweigte 3D-Lösungswege vom selben Impuls-Ursprung (Knoten 0) über verschiedene Routen:
        // analytisch (links-oben), kreativ (rechts-weit), tief-intuitiv (Balken-Wechsel), Synthese (beidseitig).
        wege = listOf(
            LoesungsWeg(intArrayOf(0, 3, 4, 6, 7, 13, 2), HIRN_CYAN, 0.00f),
            LoesungsWeg(intArrayOf(0, 20, 25, 26, 27, 29, 2), HIRN_MAGENTA, 0.26f),
            LoesungsWeg(intArrayOf(0, 16, 1, 21, 22, 28, 2), HIRN_GOLD, 0.52f),
            LoesungsWeg(intArrayOf(0, 18, 19, 1, 7, 14, 2), HIRN_RUHE, 0.76f),
        )
    }
}

private class GehirnZeichenPuffer {
    val knotenX = FloatArray(GehirnModell.knoten.size)
    val knotenY = FloatArray(GehirnModell.knoten.size)
    val knotenZ = FloatArray(GehirnModell.knoten.size)
    val knotenS = FloatArray(GehirnModell.knoten.size)
    val knotenErregung = FloatArray(GehirnModell.knoten.size)
    val knotenWegIndex = IntArray(GehirnModell.knoten.size) { -1 }
    val falteX = FloatArray(HIRN_FALTE_PUNKTE)
    val falteY = FloatArray(HIRN_FALTE_PUNKTE)
    val falteZ = FloatArray(HIRN_FALTE_PUNKTE)
    val falteS = FloatArray(HIRN_FALTE_PUNKTE)
    val ringX = FloatArray(RING_PUNKTE + 1)
    val ringY = FloatArray(RING_PUNKTE + 1)
    val ringZ = FloatArray(RING_PUNKTE + 1)
    val knotenReihe = IntArray(GehirnModell.knoten.size) { it }
    val faltenTiefe = FloatArray(GehirnModell.falten.size)
    val faltenReihe = IntArray(GehirnModell.falten.size) { it }
}

private fun DrawScope.zeichneGehirn3D(t: Float, puffer: GehirnZeichenPuffer) {
    val mitteX = size.width * 0.5f
    val mitteY = size.height * 0.51f
    val skala = size.minDimension * 0.41f

    // Kontinuierliche, ruhige 3D-Drehung: zeigt Seitenprofil, 3/4-Tiefe und beide Hemisphären.
    val yaw = 0.58f + t * 0.34f
    val pitch = 0.26f + 0.08f * sin(t * 0.45f)
    val cy = cos(yaw); val sy = sin(yaw)
    val cx = cos(pitch); val sx = sin(pitch)

    // Innere Einstellungen: sanfter Wechsel durch 3 Grundhaltungen (Fokus -> Kreativität -> Ruhe/Zuversicht)
    val haltungU = ((t % EINSTELLUNG_ZYKLUS_S) + EINSTELLUNG_ZYKLUS_S) % EINSTELLUNG_ZYKLUS_S / EINSTELLUNG_ZYKLUS_S * 3f
    val haltungFarbe = when {
        haltungU < 1f -> lerp(HIRN_FOKUS, HIRN_KREATIV, weich(haltungU))
        haltungU < 2f -> lerp(HIRN_KREATIV, HIRN_RUHE, weich(haltungU - 1f))
        else -> lerp(HIRN_RUHE, HIRN_FOKUS, weich(haltungU - 2f))
    }
    val haltungZweit = when {
        haltungU < 1f -> lerp(HIRN_CYAN, HIRN_MAGENTA, weich(haltungU))
        haltungU < 2f -> lerp(HIRN_MAGENTA, HIRN_GOLD, weich(haltungU - 1f))
        else -> lerp(HIRN_GOLD, HIRN_CYAN, weich(haltungU - 2f))
    }

    // Alle 3D-Knoten projizieren und Grundaktivität (Denken) zurücksetzen
    val knoten = GehirnModell.knoten
    for (i in knoten.indices) {
        val k = knoten[i]
        val rx = k.x * cy + k.z * sy
        val rz1 = -k.x * sy + k.z * cy
        val ry = k.y * cx - rz1 * sx
        val rz = k.y * sx + rz1 * cx
        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.65f)
        puffer.knotenX[i] = mitteX + rx * skala * s
        puffer.knotenY[i] = mitteY + ry * skala * s
        puffer.knotenZ[i] = rz
        puffer.knotenS[i] = s
        puffer.knotenErregung[i] = 0.15f + 0.20f * (0.5f + 0.5f * sin(t * 2.1f + i * 0.95f))
        puffer.knotenWegIndex[i] = -1
    }

    // Atmosphärische Gedanken-Aura der aktuellen inneren Einstellung
    val auraR = skala * 1.18f
    drawCircle(
        Brush.radialGradient(
            listOf(
                haltungFarbe.copy(alpha = 0.30f),
                haltungZweit.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            Offset(mitteX, mitteY),
            auraR,
        ),
        radius = auraR,
        center = Offset(mitteX, mitteY),
    )

    // 1) HINTERE 3D-Einstellungsringe (Perspektiv-Ebenen hinter dem Gehirn)
    zeichneEinstellungsRinge(puffer, mitteX, mitteY, skala, t, haltungFarbe, haltungZweit, vorne = false, cy, sy, cx, sx)

    // 2) Transluzente 3D-Hemisphären-Volumenkörper (tiefensortiert), damit die Gehirnform plastisch wirkt
    zeichneHemisphaerenVolumen(mitteX, mitteY, skala, haltungFarbe, haltungZweit, cy, sy, cx, sx)

    // 3) Tiefensortierung der 3D-Hirnwindungen (Gyri)
    val falten = GehirnModell.falten
    for (fIdx in falten.indices) {
        val falte = falten[fIdx]
        val mid = HIRN_FALTE_PUNKTE / 2
        val mx = falte.punkteX[mid]; val my = falte.punkteY[mid]; val mz = falte.punkteZ[mid]
        val rz1 = -mx * sy + mz * cy
        puffer.faltenTiefe[fIdx] = my * sx + rz1 * cx
        puffer.faltenReihe[fIdx] = fIdx
    }
    for (i in 1 until puffer.faltenReihe.size) {
        val k = puffer.faltenReihe[i]
        val z = puffer.faltenTiefe[k]
        var j = i - 1
        while (j >= 0 && puffer.faltenTiefe[puffer.faltenReihe[j]] < z) {
            puffer.faltenReihe[j + 1] = puffer.faltenReihe[j]
            j--
        }
        puffer.faltenReihe[j + 1] = k
    }

    // Hintere Hirnwindungen (z > 0)
    for (ord in puffer.faltenReihe) {
        if (puffer.faltenTiefe[ord] > 0f) {
            zeichneHirnFalte(falten[ord], puffer, mitteX, mitteY, skala, haltungFarbe, haltungZweit, cy, sy, cx, sx)
        }
    }

    // 4) Grund-Synapsennetz (Denken über beide Hemisphären)
    for (kante in GehirnModell.kanten) {
        val a = kante.a; val b = kante.b
        val zMitte = (puffer.knotenZ[a] + puffer.knotenZ[b]) * 0.5f
        val tiefe = ((1.25f - zMitte) / 2.5f).coerceIn(0.18f, 1f)
        val alpha = (if (kante.bruecke) 0.26f else 0.19f) * tiefe
        drawLine(
            color = lerp(haltungFarbe, Color.White, 0.25f).copy(alpha = alpha),
            start = Offset(puffer.knotenX[a], puffer.knotenY[a]),
            end = Offset(puffer.knotenX[b], puffer.knotenY[b]),
            strokeWidth = (if (kante.bruecke) 1.25f else 0.95f) * density * tiefe,
            cap = StrokeCap.Round,
        )
    }

    // 5) Verschiedene verzweigte 3D-Lösungswege mit laufenden Gedanken-Impulsen
    val wegBasis = ((t % WEG_ZYKLUS_S) + WEG_ZYKLUS_S) % WEG_ZYKLUS_S / WEG_ZYKLUS_S
    for (wIdx in GehirnModell.wege.indices) {
        val weg = GehirnModell.wege[wIdx]
        val p = ((wegBasis - weg.versatz) % 1f + 1f) % 1f
        if (p > 0.68f) continue
        val lauf = (p / 0.50f).coerceIn(0f, 1f)
        val ausklang = if (p <= 0.50f) 1f else 1f - weich((p - 0.50f) / 0.18f)
        val segmente = weg.knoten.size - 1
        val kopfPos = lauf * segmente

        for (seg in 0 until segmente) {
            val a = weg.knoten[seg]
            val b = weg.knoten[seg + 1]
            val segAnteil = (kopfPos - seg).coerceIn(0f, 1f)
            if (segAnteil <= 0f) continue

            val ax = puffer.knotenX[a]; val ay = puffer.knotenY[a]
            val bx = puffer.knotenX[b]; val by = puffer.knotenY[b]
            val ex = ax + (bx - ax) * segAnteil
            val ey = ay + (by - ay) * segAnteil
            val zMitte = (puffer.knotenZ[a] + puffer.knotenZ[b]) * 0.5f
            val tiefe = ((1.3f - zMitte) / 2.6f).coerceIn(0.35f, 1f)

            val pfadAlpha = (0.86f * ausklang * tiefe).coerceIn(0f, 0.95f)
            drawLine(
                color = weg.farbe.copy(alpha = pfadAlpha * 0.38f),
                start = Offset(ax, ay),
                end = Offset(ex, ey),
                strokeWidth = 4.6f * density * tiefe,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus,
            )
            drawLine(
                color = lerp(weg.farbe, Color.White, 0.45f).copy(alpha = pfadAlpha),
                start = Offset(ax, ay),
                end = Offset(ex, ey),
                strokeWidth = 1.9f * density * tiefe,
                cap = StrokeCap.Round,
            )

            // Aktivierte Knoten in der Farbe ihres Lösungswegs aufleuchten lassen
            if (puffer.knotenErregung[a] < ausklang) {
                puffer.knotenErregung[a] = ausklang
                puffer.knotenWegIndex[a] = wIdx
            }
            if (segAnteil > 0.85f && puffer.knotenErregung[b] < ausklang) {
                puffer.knotenErregung[b] = ausklang
                puffer.knotenWegIndex[b] = wIdx
            }

            if (segAnteil in 0.02f..0.99f) {
                val funkeR = 5.4f * density * tiefe
                drawCircle(
                    Brush.radialGradient(listOf(Color.White, weg.farbe.copy(alpha = 0.65f), Color.Transparent), Offset(ex, ey), funkeR),
                    radius = funkeR,
                    center = Offset(ex, ey),
                    blendMode = BlendMode.Plus,
                )
            }
        }

        // Erkenntnis-Leuchten am Zielknoten, wenn ein Lösungsweg ankommt
        if (lauf >= 0.96f) {
            val ziel = weg.knoten.last()
            val zx = puffer.knotenX[ziel]; val zy = puffer.knotenY[ziel]
            val haloR = (10f + 8f * (1f - ausklang)) * density * puffer.knotenS[ziel]
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f * ausklang), weg.farbe.copy(alpha = 0.45f * ausklang), Color.Transparent), Offset(zx, zy), haloR),
                radius = haloR,
                center = Offset(zx, zy),
                blendMode = BlendMode.Plus,
            )
        }
    }

    // 6) Tiefensortierte Neuronen-Knoten (back-to-front)
    for (i in 1 until puffer.knotenReihe.size) {
        val k = puffer.knotenReihe[i]
        val z = puffer.knotenZ[k]
        var j = i - 1
        while (j >= 0 && puffer.knotenZ[puffer.knotenReihe[j]] < z) {
            puffer.knotenReihe[j + 1] = puffer.knotenReihe[j]
            j--
        }
        puffer.knotenReihe[j + 1] = k
    }
    for (idx in puffer.knotenReihe) {
        val k = knoten[idx]
        val x = puffer.knotenX[idx]; val y = puffer.knotenY[idx]
        val s = puffer.knotenS[idx]
        val tiefe = ((1.25f - puffer.knotenZ[idx]) / 2.5f).coerceIn(0.24f, 1f)
        val erregt = puffer.knotenErregung[idx]
        val wegIdx = puffer.knotenWegIndex[idx]
        val grundFarbe = lerp(haltungFarbe, haltungZweit, if (k.seite >= 0) 0.55f else 0.15f)
        val farbe = if (wegIdx >= 0) GehirnModell.wege[wegIdx].farbe else grundFarbe
        val kernR = (1.8f + erregt * 1.9f) * k.gewicht * density * s * 0.86f
        val haloR = kernR * 2.9f

        if (erregt > 0.35f) {
            drawCircle(
                Brush.radialGradient(listOf(farbe.copy(alpha = 0.55f * erregt * tiefe), Color.Transparent), Offset(x, y), haloR),
                radius = haloR,
                center = Offset(x, y),
                blendMode = BlendMode.Plus,
            )
        }
        drawCircle(
            color = lerp(farbe, Color.White, 0.35f + 0.55f * erregt).copy(alpha = (0.45f + 0.55f * erregt) * tiefe),
            radius = kernR,
            center = Offset(x, y),
        )
    }

    // 7) Vordere Hirnwindungen (z <= 0) für klare 3D-Gehirn-Plastizität
    for (ord in puffer.faltenReihe) {
        if (puffer.faltenTiefe[ord] <= 0f) {
            zeichneHirnFalte(falten[ord], puffer, mitteX, mitteY, skala, haltungFarbe, haltungZweit, cy, sy, cx, sx)
        }
    }

    // 8) VORDERE 3D-Einstellungsringe (Perspektiv-Ebenen vor dem Gehirn)
    zeichneEinstellungsRinge(puffer, mitteX, mitteY, skala, t, haltungFarbe, haltungZweit, vorne = true, cy, sy, cx, sx)
}

private fun DrawScope.zeichneHemisphaerenVolumen(
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    hauptFarbe: Color,
    zweitFarbe: Color,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    // Zwei sanfte transluzente 3D-Volumenkerne für linke und rechte Hemisphäre (hintere zuerst).
    val xLinks = -0.34f; val xRechts = 0.34f; val yMitte = -0.08f
    val rzLinks = yMitte * sx + (-xLinks * sy) * cx
    val rzRechts = yMitte * sx + (-xRechts * sy) * cx
    val erstLinks = rzLinks >= rzRechts
    for (schritt in 0..1) {
        val links = if (schritt == 0) erstLinks else !erstLinks
        val hx = if (links) xLinks else xRechts
        val rx = hx * cy
        val rz1 = -hx * sy
        val ry = yMitte * cx - rz1 * sx
        val rz = yMitte * sx + rz1 * cx
        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
        val cx2d = mitteX + rx * skala * s
        val cy2d = mitteY + ry * skala * s
        val r = skala * 0.64f * s
        val farbe = if (links) hauptFarbe else lerp(hauptFarbe, zweitFarbe, 0.55f)
        drawCircle(
            Brush.radialGradient(
                listOf(
                    farbe.copy(alpha = 0.24f),
                    farbe.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                Offset(cx2d - r * 0.18f, cy2d - r * 0.22f),
                r,
            ),
            radius = r,
            center = Offset(cx2d, cy2d),
        )
    }
}

private fun DrawScope.zeichneHirnFalte(
    falte: HirnFalte,
    puffer: GehirnZeichenPuffer,
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    hauptFarbe: Color,
    zweitFarbe: Color,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    for (i in 0 until HIRN_FALTE_PUNKTE) {
        val x = falte.punkteX[i]; val y = falte.punkteY[i]; val z = falte.punkteZ[i]
        val rx = x * cy + z * sy
        val rz1 = -x * sy + z * cy
        val ry = y * cx - rz1 * sx
        val rz = y * sx + rz1 * cx
        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.65f)
        puffer.falteX[i] = mitteX + rx * skala * s
        puffer.falteY[i] = mitteY + ry * skala * s
        puffer.falteZ[i] = rz
        puffer.falteS[i] = s
    }
    val seitenFarbe = if (falte.seite < 0) hauptFarbe else lerp(hauptFarbe, zweitFarbe, 0.55f)
    for (i in 0 until HIRN_FALTE_PUNKTE - 1) {
        val zMitte = (puffer.falteZ[i] + puffer.falteZ[i + 1]) * 0.5f
        val tiefe = ((1.22f - zMitte) / 2.44f).coerceIn(0.16f, 1f)
        val enden = sin(((i + 0.5f) / (HIRN_FALTE_PUNKTE - 1)) * PI.toFloat()).coerceIn(0.25f, 1f)
        val dicke = (1.35f + 2.2f * tiefe) * density * puffer.falteS[i] * 0.85f
        drawLine(
            color = seitenFarbe.copy(alpha = (0.14f + 0.44f * tiefe) * enden),
            start = Offset(puffer.falteX[i], puffer.falteY[i]),
            end = Offset(puffer.falteX[i + 1], puffer.falteY[i + 1]),
            strokeWidth = dicke,
            cap = StrokeCap.Round,
        )
        if (tiefe > 0.54f) {
            drawLine(
                color = Color.White.copy(alpha = (tiefe - 0.54f) * 0.72f * enden),
                start = Offset(puffer.falteX[i], puffer.falteY[i] - 0.6f * density),
                end = Offset(puffer.falteX[i + 1], puffer.falteY[i + 1] - 0.6f * density),
                strokeWidth = dicke * 0.36f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.zeichneEinstellungsRinge(
    puffer: GehirnZeichenPuffer,
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    t: Float,
    hauptFarbe: Color,
    zweitFarbe: Color,
    vorne: Boolean,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    // Zwei sanft schwenkende 3D-Perspektivringe um das Gehirn: versinnbildlichen innere Haltungen.
    for (ring in 0 until 2) {
        val radius = 1.12f + ring * 0.14f
        val kippX = (if (ring == 0) 0.58f else -0.46f) + 0.16f * sin(t * 0.42f + ring * 1.7f)
        val kippZ = (if (ring == 0) -0.28f else 0.36f) + 0.14f * cos(t * 0.36f + ring * 2.1f)
        val ckx = cos(kippX); val skx = sin(kippX)
        val ckz = cos(kippZ); val skz = sin(kippZ)
        val dreh = t * (if (ring == 0) 0.24f else -0.19f)

        for (i in 0..RING_PUNKTE) {
            val a = (i.toFloat() / RING_PUNKTE) * TAU + dreh
            val lx = cos(a) * radius
            val lz0 = sin(a) * radius
            val ly1 = -lz0 * skx
            val lz1 = lz0 * ckx
            val lx2 = lx * ckz - ly1 * skz
            val ly2 = lx * skz + ly1 * ckz

            val rx = lx2 * cy + lz1 * sy
            val rz1 = -lx2 * sy + lz1 * cy
            val ry = ly2 * cx - rz1 * sx
            val rz = ly2 * sx + rz1 * cx
            val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
            puffer.ringX[i] = mitteX + rx * skala * s
            puffer.ringY[i] = mitteY + ry * skala * s
            puffer.ringZ[i] = rz
        }

        val farbe = if (ring == 0) hauptFarbe else zweitFarbe
        val alpha = if (vorne) 0.34f else 0.12f
        val dicke = (if (vorne) 1.3f else 0.9f) * density
        for (i in 0 until RING_PUNKTE) {
            val zMitte = (puffer.ringZ[i] + puffer.ringZ[i + 1]) * 0.5f
            if (vorne && zMitte > 0f) continue
            if (!vorne && zMitte <= 0f) continue
            drawLine(
                color = farbe.copy(alpha = alpha),
                start = Offset(puffer.ringX[i], puffer.ringY[i]),
                end = Offset(puffer.ringX[i + 1], puffer.ringY[i + 1]),
                strokeWidth = dicke,
                cap = StrokeCap.Round,
            )
        }
    }
}
