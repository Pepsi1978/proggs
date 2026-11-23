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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.jarvis.ui.theme.Farben
import de.frank.jarvis.ui.theme.LocalFarben
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAU = (2.0 * PI).toFloat()
private const val FOKAL = 3.25f

private val ATEM_SMARAGD = Color(0xFF2EE2B4)
private val ATEM_JADE = Color(0xFF7AF2D6)
private val ATEM_SONNE = Color(0xFFFFE5A3)
private val ATEM_HORIZONT = Color(0xFF176661)
private val ATEM_PERLE = Color(0xFFF3FFF9)

private val HIRN_FOKUS = Color(0xFF6B8CFF)
private val HIRN_CYAN = Color(0xFF4FC8FF)
private val HIRN_KREATIV = Color(0xFFB98CFF)
private val HIRN_MAGENTA = Color(0xFFE58CFF)
private val HIRN_RUHE = Color(0xFF4FE0B5)
private val HIRN_GOLD = Color(0xFFFFD166)

private val LISTE_KUPFER = Color(0xFFFFB259)
private val LISTE_BRETT_HELL = Color(0xFF9C5E37)
private val LISTE_BRETT_DUNKEL = Color(0xFF5D331B)
private val LISTE_KANTE = Color(0xFF3E2010)
private val LISTE_PAPIER = Color(0xFFFFF6E8)
private val LISTE_PAPIER_SCHATTEN = Color(0xFFE9D6BC)
private val LISTE_CLIP_GOLD = Color(0xFFFFD678)
private val LISTE_HAKEN_GRUEN = Color(0xFF2BD68B)
private val LISTE_ZEILE_TEXT = Color(0xFF6E4B35)
private val LISTE_ZEILE_ERLEDIGT = Color(0xFFB89E88)
private val LISTE_KORB_METALL = Color(0xFFFFE7C7)
private val LISTE_KORB_SCHATTEN = Color(0xFFD1A475)
private val LISTE_GRIFF_KORALLE = Color(0xFFFF6E54)
private val LISTE_PAKET_1 = Color(0xFFFF6E54)
private val LISTE_PAKET_2 = Color(0xFF39D99B)
private val LISTE_PAKET_3 = Color(0xFFFFCB52)

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
    eckenRadius: Dp = 18.dp,
) {
    val f = LocalFarben.current
    val form = RoundedCornerShape(eckenRadius)
    val buehnenHintergrund = remember(toolId, f.dunkel) { buehnenVerlauf(toolId, f) }
    val randFarbe = remember(toolId, f.dunkel) {
        val akzent = when (toolId) {
            "atem" -> ATEM_JADE
            "listen" -> LISTE_KUPFER
            else -> HIRN_KREATIV
        }
        akzent.copy(alpha = if (f.dunkel) 0.34f else 0.44f)
    }
    // Wiederverwendbare Zeichenpuffer je Karte: keine Objekt-Erzeugung im 60/120-Hz-Takt.
    val atemPuffer = remember(toolId) { if (toolId == "atem") AtemZeichenPuffer() else null }
    val hirnPuffer = remember(toolId) { if (toolId == "mentalitaet") GehirnZeichenPuffer() else null }
    val listenPuffer = remember(toolId) { if (toolId == "listen") ListenZeichenPuffer() else null }

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
                "listen" -> zeichneListen3D(t, listenPuffer ?: return@Canvas)
            }
        }
    }
}

private fun buehnenVerlauf(toolId: String, f: Farben): Brush = when (toolId) {
    // Atemübungen: heller, luftig-frischer Morgenhimmel-Verlauf (Meergrün -> Jade -> warmes Morgenlicht)
    "atem" -> if (f.dunkel) {
        Brush.verticalGradient(listOf(Color(0xFF0E4752), Color(0xFF1A776D), Color(0xFF5FA88A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF156774), Color(0xFF2D9C8F), Color(0xFFAAD8BA)))
    }
    // Listen: warmes, edles Kupfer-Bernstein-Atelier (schräg von warmem Kupfer-Karamell nach Espresso)
    "listen" -> if (f.dunkel) {
        Brush.linearGradient(listOf(Color(0xFF4A2917), Color(0xFF2D180F), Color(0xFF160C08)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF693B20), Color(0xFF432313), Color(0xFF27140B)))
    }
    // Mentalität (unverändert): tiefes Nacht-Indigo/Violett
    else -> if (f.dunkel) {
        Brush.radialGradient(listOf(Color(0xFF1E1942), Color(0xFF110F29), Color(0xFF09091A)))
    } else {
        Brush.radialGradient(listOf(Color(0xFF282258), Color(0xFF1B1740), Color(0xFF13112E)))
    }
}

// ================================================================================================
// 3D-Szene 1: Atemübungen — 3D-Atemflügel (organische 3D-Lungen-Lamellen), vertikaler Atemstrom
// und sanfte 3D-Morgenwellen am Horizont (ruhiges Entfalten & Senken statt Kreiselrotation)
// ================================================================================================

private const val ATEM_ZYKLUS_S = 7.2f
private const val ATEM_LAMELLEN = 7
private const val ATEM_BOGEN_PUNKTE = 18
private const val ATEM_HORIZONT_PUNKTE = 16
private const val RING_PUNKTE = 40

private data class AtemPerle(
    val xVersatz: Float,
    val zTiefe: Float,
    val phase: Float,
    val tempo: Float,
    val weite: Float,
    val radius: Float,
)

private object AtemModell {
    val perlen: List<AtemPerle> = List(22) { i ->
        val norm = i.toFloat() / 22f
        val spur = ((i * 7) % 11) / 10f - 0.5f
        AtemPerle(
            xVersatz = spur * 0.42f,
            zTiefe = ((i % 5) - 2) * 0.16f,
            phase = norm,
            tempo = 0.72f + (i % 4) * 0.14f,
            weite = 0.06f + (i % 3) * 0.045f,
            radius = 1.35f + (i % 4) * 0.55f,
        )
    }
}

private class AtemZeichenPuffer {
    val bx = FloatArray(ATEM_BOGEN_PUNKTE + 1)
    val by = FloatArray(ATEM_BOGEN_PUNKTE + 1)
    val bs = FloatArray(ATEM_BOGEN_PUNKTE + 1)
    val pfad = Path()
    val wellePfad = Path()
}

private fun DrawScope.zeichneAtem3D(t: Float, puffer: AtemZeichenPuffer) {
    val mitteX = size.width * 0.5f
    val mitteY = size.height * 0.47f
    val skala = size.minDimension * 0.44f

    // Vollständig nahtlose (C-unendlich) Atemwelle: 0 = entspannt ausgeatmet, 1 = voll eingeatmet.
    val w = (t / ATEM_ZYKLUS_S) * TAU
    val atem = 0.5f - 0.5f * cos(w)
    val fluss = sin(w) // +1 Einatmen, -1 Ausatmen

    // Ruhige, frontale Atem-Kamera: KEINE 360°-Drehung, nur ganz sanftes perspektivisches Atmen.
    val yaw = sin(w * 0.5f) * 0.14f
    val pitch = 0.18f - atem * 0.09f
    val cy = cos(yaw); val sy = sin(yaw)
    val cx = cos(pitch); val sx = sin(pitch)

    // 1) Sanftes Morgenlicht oben mittig (hell-luftige Atmosphäre statt dunkler Weltraum-Kugel)
    val lichtY = size.height * (0.28f - atem * 0.06f)
    val lichtR = size.maxDimension * (0.58f + atem * 0.18f)
    drawCircle(
        Brush.radialGradient(
            listOf(
                ATEM_SONNE.copy(alpha = 0.28f + atem * 0.14f),
                ATEM_JADE.copy(alpha = 0.16f),
                Color.Transparent,
            ),
            Offset(mitteX, lichtY),
            lichtR,
        ),
        radius = lichtR,
        center = Offset(mitteX, lichtY),
    )

    // 2) Zwei geschichtete 3D-Horizont-Atemwellen unten (hinter den Flügeln)
    zeichneAtemHorizont(puffer, t, atem, schicht = 0)
    zeichneAtemHorizont(puffer, t, atem, schicht = 1)

    // 3) Zentrale aufsteigende/sinkende 3D-Atem-Lichtsäule zwischen den beiden Flügel-Fächern
    val saeuleOben = mitteY - skala * (0.64f + atem * 0.22f)
    val saeuleUnten = mitteY + skala * 0.66f
    val saeuleBreite = skala * (0.13f + atem * 0.11f)
    drawLine(
        brush = Brush.verticalGradient(
            listOf(
                Color.Transparent,
                ATEM_SONNE.copy(alpha = 0.28f + atem * 0.22f),
                ATEM_PERLE.copy(alpha = 0.44f + atem * 0.24f),
                ATEM_JADE.copy(alpha = 0.22f),
                Color.Transparent,
            ),
            startY = saeuleOben,
            endY = saeuleUnten,
        ),
        start = Offset(mitteX, saeuleOben),
        end = Offset(mitteX, saeuleUnten),
        strokeWidth = saeuleBreite,
        cap = StrokeCap.Round,
    )

    // 4) Hintere aufsteigende 3D-Atemperlen (z > 0)
    zeichneAtemPerlen(mitteX, mitteY, skala, t, atem, vorne = false, cy, sy, cx, sx)

    // 5) Symmetrische 3D-Atemflügel (7 tiefengestaffelte 3D-Lamellenbögen je Seite, von hinten nach vorn)
    // Beim Einatmen entfalten sie sich weit nach außen/oben wie Lungenflügel / Lotus-Lamellen;
    // beim Ausatmen senken und schließen sie sich sanft zur Mitte.
    for (tiefeIdx in ATEM_LAMELLEN - 1 downTo 0) {
        val norm = tiefeIdx.toFloat() / (ATEM_LAMELLEN - 1) // 0 = vorderste Hauptlamelle, 1 = hinterste
        // Sanfter Phasenversatz über die Tiefe: die Atembewegung fließt als echte 3D-Welle durch die Lamellen!
        val lokW = w - norm * 0.58f
        val lokAtem = 0.5f - 0.5f * cos(lokW)
        val zBasis = -0.34f + norm * 0.78f
        val spannWeite = (0.44f + lokAtem * 0.58f) * (1.06f - norm * 0.26f)
        val bogenHoehe = (0.56f + lokAtem * 0.26f) * (1.04f - norm * 0.20f)
        val hebung = -0.08f - lokAtem * 0.13f + norm * 0.05f
        val spalt = 0.055f + (1f - lokAtem) * 0.015f

        val tiefenLicht = 1f - norm * 0.55f
        val lamellenFarbe = lerp(
            lerp(ATEM_SMARAGD, ATEM_SONNE, lokAtem * 0.36f),
            ATEM_PERLE,
            (1f - norm) * 0.45f,
        )
        val fuellFarbe = lerp(ATEM_HORIZONT, ATEM_JADE, 0.42f + lokAtem * 0.34f)

        for (seiteIdx in 0..1) {
            val seite = if (seiteIdx == 0) -1f else 1f
            puffer.pfad.rewind()
            for (i in 0..ATEM_BOGEN_PUNKTE) {
                val u = i.toFloat() / ATEM_BOGEN_PUNKTE // 0 = oben an Achse, 1 = unten an Achse
                val profil = sin(u * PI.toFloat())
                // Organische Flügelkontur: oben weiter gewölbt, unten weich zulaufend
                val fluegelKurve = profil * (0.78f + 0.34f * sin(u * PI.toFloat() * 0.75f))
                val x3 = seite * (spalt + fluegelKurve * spannWeite)
                val y3 = (u - 0.5f) * 2f * bogenHoehe + hebung - profil * lokAtem * 0.12f
                // Sanfte 3D-Wölbung der Flügel nach vorn beim Öffnen
                val z3 = zBasis - profil * (0.14f + lokAtem * 0.16f)

                val rx = x3 * cy + z3 * sy
                val rz1 = -x3 * sy + z3 * cy
                val ry = y3 * cx - rz1 * sx
                val rz = y3 * sx + rz1 * cx
                val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
                val px = mitteX + rx * skala * s
                val py = mitteY + ry * skala * s
                puffer.bx[i] = px
                puffer.by[i] = py
                puffer.bs[i] = s
                if (i == 0) puffer.pfad.moveTo(px, py) else puffer.pfad.lineTo(px, py)
            }
            puffer.pfad.close()

            // Seidig-transluzente 3D-Lamellenfläche (verleiht Volumen ohne Kugel-Look)
            drawPath(
                path = puffer.pfad,
                color = fuellFarbe.copy(alpha = (0.10f + 0.13f * tiefenLicht) * (0.75f + 0.25f * lokAtem)),
            )

            // Leuchtende Außenkontur der 3D-Rippe
            val kantenAlpha = (0.32f + 0.58f * tiefenLicht).coerceIn(0f, 0.94f)
            val strich = (1.15f + 1.55f * tiefenLicht) * density
            for (i in 0 until ATEM_BOGEN_PUNKTE) {
                val segF = sin(((i + 0.5f) / ATEM_BOGEN_PUNKTE) * PI.toFloat()).coerceIn(0.2f, 1f)
                drawLine(
                    color = lamellenFarbe.copy(alpha = kantenAlpha * (0.45f + 0.55f * segF)),
                    start = Offset(puffer.bx[i], puffer.by[i]),
                    end = Offset(puffer.bx[i + 1], puffer.by[i + 1]),
                    strokeWidth = strich * puffer.bs[i] * (0.65f + 0.45f * segF),
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    // 6) Zarter organischer Atem-Stamm / Luftkanal in der Mitte (zwei feine Lichtfäden)
    for (fadenIdx in 0..1) {
        val faden = if (fadenIdx == 0) -1f else 1f
        val xStart = mitteX + faden * skala * 0.03f
        val bogen = faden * skala * (0.04f + atem * 0.04f)
        puffer.pfad.rewind()
        puffer.pfad.moveTo(xStart, mitteY + skala * 0.54f)
        puffer.pfad.cubicTo(
            xStart + bogen, mitteY + skala * 0.14f,
            xStart - bogen, mitteY - skala * 0.20f,
            xStart + faden * skala * (0.05f + atem * 0.05f), mitteY - skala * (0.56f + atem * 0.16f),
        )
        drawPath(
            path = puffer.pfad,
            color = ATEM_PERLE.copy(alpha = 0.45f + 0.25f * abs(fluss)),
            style = Stroke(width = 1.4f * density, cap = StrokeCap.Round),
        )
    }

    // 7) Vordere aufsteigende 3D-Atemperlen (z <= 0)
    zeichneAtemPerlen(mitteX, mitteY, skala, t, atem, vorne = true, cy, sy, cx, sx)
}

private fun DrawScope.zeichneAtemHorizont(puffer: AtemZeichenPuffer, t: Float, atem: Float, schicht: Int) {
    val basisY = size.height * (if (schicht == 0) 0.77f else 0.85f) + atem * size.height * (if (schicht == 0) 0.035f else 0.02f)
    val amp = size.height * (if (schicht == 0) 0.052f else 0.038f)
    val tempo = if (schicht == 0) 0.75f else -0.58f
    val phase = if (schicht == 0) 0.0f else 2.2f

    puffer.wellePfad.rewind()
    puffer.wellePfad.moveTo(0f, size.height)
    for (i in 0..ATEM_HORIZONT_PUNKTE) {
        val u = i.toFloat() / ATEM_HORIZONT_PUNKTE
        val x = u * size.width
        val y = basisY + sin(u * TAU * 1.15f + t * tempo + phase) * amp
        puffer.wellePfad.lineTo(x, y)
    }
    puffer.wellePfad.lineTo(size.width, size.height)
    puffer.wellePfad.close()

    val farbe = if (schicht == 0) Color(0xFF0A3A40) else Color(0xFF072B30)
    drawPath(puffer.wellePfad, farbe.copy(alpha = if (schicht == 0) 0.28f else 0.42f))
}

private fun DrawScope.zeichneAtemPerlen(
    mitteX: Float,
    mitteY: Float,
    skala: Float,
    t: Float,
    atem: Float,
    vorne: Boolean,
    cy: Float,
    sy: Float,
    cx: Float,
    sx: Float,
) {
    // Stetig aufsteigender, sanft im Atemrhythmus schwingender Lichthauch (ohne Umbruch-Sprung).
    val auftrieb = t * 0.20f + atem * 0.26f
    for (p in AtemModell.perlen) {
        val u = ((p.phase + auftrieb * p.tempo) % 1f + 1f) % 1f
        val h = sin(u * PI.toFloat()).coerceIn(0f, 1f)
        val huelle = h * h
        if (huelle < 0.015f) continue

        val spreiz = 0.65f + u * (0.45f + atem * 0.55f)
        val x3 = p.xVersatz * spreiz + sin(u * TAU + t * 0.9f + p.phase * TAU) * p.weite
        val y3 = 0.72f - u * 1.48f
        val z3 = p.zTiefe * (0.7f + 0.3f * spreiz)

        val rx = x3 * cy + z3 * sy
        val rz1 = -x3 * sy + z3 * cy
        val ry = y3 * cx - rz1 * sx
        val rz = y3 * sx + rz1 * cx
        if (vorne && rz > 0f) continue
        if (!vorne && rz <= 0f) continue

        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
        val px = mitteX + rx * skala * s
        val py = mitteY + ry * skala * s
        val r = p.radius * density * s * (0.6f + 0.4f * h)
        val alpha = (huelle * (if (vorne) 0.85f else 0.42f)).coerceIn(0f, 0.92f)
        val farbe = lerp(ATEM_PERLE, ATEM_SONNE, u * 0.45f)
        drawCircle(farbe.copy(alpha = alpha), r, Offset(px, py))
    }
}

// ================================================================================================
// 3D-Szene 3: Listen — Plastisches 3D-Klemmbrett mit sequenziell abhakenden 3D-Punkten und
// einem rollenden 3D-Einkaufswagen mit bunten 3D-Paketen (warmes Kupfer-Bernstein-Studio)
// ================================================================================================

private const val LISTEN_ZYKLUS_S = 6.6f
private val LISTEN_ZEILEN_LAENGEN = floatArrayOf(0.56f, 0.46f, 0.52f)

private class ListenZeichenPuffer {
    val pfad = Path()
    val hakenStand = FloatArray(3)
    val zeilenY = FloatArray(3)
    val radX = FloatArray(2)
}

private fun DrawScope.zeichneListen3D(t: Float, puffer: ListenZeichenPuffer) {
    val mitteX = size.width * 0.49f
    val mitteY = size.height * 0.50f
    val skala = size.minDimension * 0.45f

    // Vollständig nahtlose 6,6-Sekunden-Schleife: u ∈ [0, 1)
    val u = ((t % LISTEN_ZYKLUS_S) + LISTEN_ZYKLUS_S) % LISTEN_ZYKLUS_S / LISTEN_ZYKLUS_S
    val w = u * TAU

    // Ruhige, edle 3D-Schwenk-Parallax (kein 360°-Kreisel wie Mentalität, kein vertikales Flügel-Atmen wie Atem)
    val yaw = -0.28f + 0.15f * sin(w)
    val pitch = 0.20f + 0.05f * cos(w)
    val cy = cos(yaw); val sy = sin(yaw)
    val cx = cos(pitch); val sx = sin(pitch)

    fun projX(x: Float, y: Float, z: Float): Float {
        val rx = x * cy + z * sy
        val rz1 = -x * sy + z * cy
        val rz = y * sx + rz1 * cx
        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.65f)
        return mitteX + rx * skala * s
    }

    fun projY(x: Float, y: Float, z: Float): Float {
        val rz1 = -x * sy + z * cy
        val ry = y * cx - rz1 * sx
        val rz = y * sx + rz1 * cx
        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.65f)
        return mitteY + ry * skala * s
    }

    fun projS(x: Float, y: Float, z: Float): Float {
        val rz1 = -x * sy + z * cy
        val rz = y * sx + rz1 * cx
        return FOKAL / (FOKAL + rz).coerceAtLeast(0.65f)
    }

    fun fuelleQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        farbe: Color,
    ) {
        puffer.pfad.rewind()
        puffer.pfad.moveTo(projX(x0, y0, z0), projY(x0, y0, z0))
        puffer.pfad.lineTo(projX(x1, y1, z1), projY(x1, y1, z1))
        puffer.pfad.lineTo(projX(x2, y2, z2), projY(x2, y2, z2))
        puffer.pfad.lineTo(projX(x3, y3, z3), projY(x3, y3, z3))
        puffer.pfad.close()
        drawPath(puffer.pfad, farbe)
    }

    fun umrissQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        farbe: Color,
        breite: Float,
    ) {
        puffer.pfad.rewind()
        puffer.pfad.moveTo(projX(x0, y0, z0), projY(x0, y0, z0))
        puffer.pfad.lineTo(projX(x1, y1, z1), projY(x1, y1, z1))
        puffer.pfad.lineTo(projX(x2, y2, z2), projY(x2, y2, z2))
        puffer.pfad.lineTo(projX(x3, y3, z3), projY(x3, y3, z3))
        puffer.pfad.close()
        drawPath(puffer.pfad, farbe, style = Stroke(width = breite, join = StrokeJoin.Round))
    }

    fun linie3D(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, farbe: Color, breite: Float) {
        drawLine(
            color = farbe,
            start = Offset(projX(x0, y0, z0), projY(x0, y0, z0)),
            end = Offset(projX(x1, y1, z1), projY(x1, y1, z1)),
            strokeWidth = breite,
            cap = StrokeCap.Round,
        )
    }

    // 1) Warmes Bernstein-Spotlicht oben links + weicher Raum-Bodenschatten
    val spotR = size.maxDimension * 0.72f
    drawCircle(
        Brush.radialGradient(
            listOf(LISTE_KUPFER.copy(alpha = 0.24f), LISTE_CLIP_GOLD.copy(alpha = 0.06f), Color.Transparent),
            Offset(size.width * 0.30f, size.height * 0.26f),
            spotR,
        ),
        radius = spotR,
        center = Offset(size.width * 0.30f, size.height * 0.26f),
    )
    // Weicher 3D-Bodenschatten
    val schattenOrt = Offset(mitteX + skala * 0.04f, mitteY + skala * 0.86f)
    drawCircle(
        Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.42f), Color.Transparent),
            schattenOrt,
            skala * 0.92f,
        ),
        radius = skala * 0.92f,
        center = schattenOrt,
    )

    // 2) Fortschritt der 3 Abhak-Punkte im nahtlosen Zyklus (0 = offen, 1 = abgehakt)
    // Alle 3 klingen zwischen u = 0.76..0.95 weich wieder aus -> C-unendlich nahtlose Schleife!
    val ausklang = if (u > 0.76f) 1f - weich((u - 0.76f) / 0.19f) else 1f
    val haken0 = weich((u - 0.05f) / 0.14f) * ausklang
    val haken1 = weich((u - 0.26f) / 0.14f) * ausklang
    val haken2 = weich((u - 0.47f) / 0.14f) * ausklang
    puffer.hakenStand[0] = haken0
    puffer.hakenStand[1] = haken1
    puffer.hakenStand[2] = haken2

    // Sanftes Schweben des 3D-Klemmbretts (leicht nach links gerückt, damit rechts vorn Platz für den 3D-Einkaufswagen ist)
    val bx = -0.15f
    val by = -0.02f + sin(w) * 0.035f
    val bz = 0.10f
    val bw = 0.56f
    val bh = 0.74f
    val dicke = 0.10f

    // 3) 3D-Klemmbrett: Extrusions-Kanten (rechts & unten, echte 3D-Plastizität)
    val xL = bx - bw; val xR = bx + bw
    val yO = by - bh; val yU = by + bh
    val zV = bz; val zH = bz + dicke

    // Rechte 3D-Seitenkante (sichtbar wegen negativer yaw-Drehung)
    fuelleQuad(xR, yO, zV, xR, yO, zH, xR, yU, zH, xR, yU, zV, LISTE_KANTE)
    // Linke 3D-Seitenkante (falls yaw leicht positiv schwenkt)
    fuelleQuad(xL, yO, zH, xL, yO, zV, xL, yU, zV, xL, yU, zH, LISTE_KANTE)
    // Untere 3D-Kante
    fuelleQuad(xL, yU, zV, xR, yU, zV, xR, yU, zH, xL, yU, zH, Color(0xFF2B150A))

    // Vorderseite des 3D-Klemmbretts (warmes Kupfer-Walnuss)
    fuelleQuad(xL, yO, zV, xR, yO, zV, xR, yU, zV, xL, yU, zV, LISTE_BRETT_HELL)
    // Subtiler 3D-Schattierungsverlauf über das Brett
    fuelleQuad(xL, by + 0.06f, zV - 0.002f, xR, by + 0.06f, zV - 0.002f, xR, yU, zV - 0.002f, xL, yU, zV - 0.002f, LISTE_BRETT_DUNKEL.copy(alpha = 0.42f))
    umrissQuad(xL, yO, zV, xR, yO, zV, xR, yU, zV, xL, yU, zV, LISTE_KUPFER.copy(alpha = 0.48f), 1.2f * density)

    // 4) 3D-Papierstapel (hinteres leicht schräges Blatt + vorderes Hauptblatt)
    val pL = xL + 0.09f; val pR = xR - 0.09f
    val pO = yO + 0.13f; val pU = yU - 0.09f
    val zBlatt1 = zV - 0.035f
    val zBlatt2 = zV - 0.068f
    fuelleQuad(pL + 0.03f, pO + 0.02f, zBlatt1, pR + 0.03f, pO + 0.04f, zBlatt1, pR + 0.02f, pU + 0.03f, zBlatt1, pL + 0.02f, pU + 0.01f, zBlatt1, LISTE_PAPIER_SCHATTEN)
    fuelleQuad(pL, pO, zBlatt2, pR, pO, zBlatt2, pR, pU, zBlatt2, pL, pU, zBlatt2, LISTE_PAPIER)
    umrissQuad(pL, pO, zBlatt2, pR, pO, zBlatt2, pR, pU, zBlatt2, pL, pU, zBlatt2, Color.White.copy(alpha = 0.72f), 0.9f * density)

    // 5) Plastischer 3D-Metall-Klemmbügel oben mittig (Sockel + gewölbter Bügel)
    val cL = bx - 0.21f; val cR = bx + 0.21f
    val cO = yO - 0.05f; val cU = yO + 0.14f
    val zClip = zBlatt2 - 0.055f
    // Clip-Schatten auf dem Blatt
    fuelleQuad(cL + 0.02f, cO + 0.09f, zBlatt2 - 0.004f, cR + 0.02f, cO + 0.09f, zBlatt2 - 0.004f, cR + 0.02f, cU + 0.04f, zBlatt2 - 0.004f, cL + 0.02f, cU + 0.04f, zBlatt2 - 0.004f, Color.Black.copy(alpha = 0.16f))
    // Clip-Unterkante (3D-Tiefe)
    fuelleQuad(cL, cU, zClip, cR, cU, zClip, cR, cU, zBlatt2, cL, cU, zBlatt2, Color(0xFFB37D2C))
    // Clip-Front
    fuelleQuad(cL, cO, zClip, cR, cO, zClip, cR, cU, zClip, cL, cU, zClip, LISTE_CLIP_GOLD)
    // Glanzstreifen auf dem Clip
    linie3D(cL + 0.04f, cO + 0.045f, zClip - 0.004f, cR - 0.04f, cO + 0.045f, zClip - 0.004f, Color.White.copy(alpha = 0.78f), 1.6f * density)

    // 6) Drei schwebende 3D-Checklisten-Zeilen mit lebendiger Abhak-Animation
    puffer.zeilenY[0] = pO + 0.26f
    puffer.zeilenY[1] = pO + 0.62f
    puffer.zeilenY[2] = pO + 0.98f
    for (idx in 0 until 3) {
        val h = puffer.hakenStand[idx]
        // Beim Abhaken springt die Zeile sanft nach vorn in Z (echter 3D-Pop!)
        val pop = sin(h * PI.toFloat()).coerceIn(0f, 1f)
        val zZ = zBlatt2 - 0.015f - h * 0.032f - pop * 0.055f
        val zy = puffer.zeilenY[idx]

        // Sanfter Schatten unter der schwebenden Checkbox
        val boxL = pL + 0.09f
        val boxR = boxL + 0.18f
        val boxO = zy - 0.09f
        val boxU = zy + 0.09f
        if (h > 0.05f) {
            fuelleQuad(
                boxL + 0.02f, boxO + 0.03f, zBlatt2 - 0.004f,
                boxR + 0.02f, boxO + 0.03f, zBlatt2 - 0.004f,
                boxR + 0.02f, boxU + 0.03f, zBlatt2 - 0.004f,
                boxL + 0.02f, boxU + 0.03f, zBlatt2 - 0.004f,
                Color.Black.copy(alpha = 0.14f * h),
            )
        }

        // 3D-Checkbox
        val boxFarbe = lerp(Color(0xFFEDD9BE), LISTE_HAKEN_GRUEN, h)
        fuelleQuad(boxL, boxO, zZ, boxR, boxO, zZ, boxR, boxU, zZ, boxL, boxU, zZ, boxFarbe)
        umrissQuad(boxL, boxO, zZ, boxR, boxO, zZ, boxR, boxU, zZ, boxL, boxU, zZ, lerp(LISTE_ZEILE_TEXT.copy(alpha = 0.55f), Color.White.copy(alpha = 0.85f), h), 1.15f * density)

        // Schwungvoll gezeichnetes 3D-Häkchen in der Checkbox
        if (h > 0.04f) {
            val zHaken = zZ - 0.016f
            val h1 = (h * 2.1f).coerceIn(0f, 1f)
            val h2 = ((h - 0.42f) / 0.58f).coerceIn(0f, 1f)
            val ax = boxL + 0.035f; val ay = zy + 0.005f
            val mx = boxL + 0.078f; val my = zy + 0.052f
            val ex = boxR - 0.025f; val ey = zy - 0.052f
            val strich = 2.1f * density * projS(mx, my, zHaken)
            linie3D(ax, ay, zHaken, ax + (mx - ax) * h1, ay + (my - ay) * h1, zHaken, Color.White, strich)
            if (h2 > 0f) {
                linie3D(mx, my, zHaken, mx + (ex - mx) * h2, my + (ey - my) * h2, zHaken, Color.White, strich)
            }
            // Goldener Aufblitz-Funke beim Abhaken
            if (pop > 0.12f) {
                val fx = projX(boxR + 0.03f, boxO - 0.03f * pop, zHaken - 0.03f)
                val fy = projY(boxR + 0.03f, boxO - 0.03f * pop, zHaken - 0.03f)
                val fr = 5.5f * density * pop
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f * pop), LISTE_CLIP_GOLD.copy(alpha = 0.55f * pop), Color.Transparent), Offset(fx, fy), fr),
                    radius = fr,
                    center = Offset(fx, fy),
                    blendMode = BlendMode.Plus,
                )
            }
        }

        // 3D-Textbalken rechts neben der Checkbox (Hauptzeile + dezente Unterzeile)
        val tL = boxR + 0.08f
        val tR = (tL + LISTEN_ZEILEN_LAENGEN[idx]).coerceAtMost(pR - 0.07f)
        val balkenFarbe = lerp(LISTE_ZEILE_TEXT, LISTE_ZEILE_ERLEDIGT, h * 0.85f)
        val sZ = projS(tL, zy, zZ)
        linie3D(tL, zy - 0.025f, zZ, tR, zy - 0.025f, zZ, balkenFarbe, 3.2f * density * sZ)
        linie3D(tL, zy + 0.050f, zZ, tL + (tR - tL) * 0.62f, zy + 0.050f, zZ, balkenFarbe.copy(alpha = 0.48f), 2.1f * density * sZ)
        // Feine grüne Erledigt-Spur beim Abhaken
        if (h > 0.20f) {
            val fortschritt = ((h - 0.20f) / 0.80f).coerceIn(0f, 1f)
            linie3D(tL, zy - 0.025f, zZ - 0.008f, tL + (tR - tL) * fortschritt, zy - 0.025f, zZ - 0.008f, LISTE_HAKEN_GRUEN.copy(alpha = 0.72f), 1.4f * density * sZ)
        }
    }

    // 7) Kleiner plastischer 3D-Einkaufswagen im Vordergrund unten rechts
    // Gleitet weich hin und her, Räder drehen sich synchron zur Bewegung, Pakete hüpfen beim Abhaken mit!
    val fahrt = 0.09f * sin(w)
    val wx = 0.36f + fahrt
    val wy = 0.36f + 0.015f * sin(w * 2f)
    val wz = -0.34f
    val radDreh = fahrt * 14f

    // Korb-Abmessungen im 3D-Raum (trapezförmig: oben breiter, nach hinten verjüngt)
    val kO = wy - 0.22f; val kU = wy + 0.14f
    val kLO = wx - 0.26f; val kRO = wx + 0.30f
    val kLU = wx - 0.19f; val kRU = wx + 0.22f
    val kZV = wz - 0.16f; val kZH = wz + 0.16f
    val draht = 1.65f * density * projS(wx, wy, wz)

    // Hintere Korbwand (z = kZH)
    fuelleQuad(kLO, kO, kZH, kRO, kO, kZH, kRU, kU, kZH, kLU, kU, kZH, LISTE_KORB_SCHATTEN.copy(alpha = 0.20f))
    umrissQuad(kLO, kO, kZH, kRO, kO, kZH, kRU, kU, kZH, kLU, kU, kZH, LISTE_KORB_SCHATTEN.copy(alpha = 0.75f), draht * 0.85f)

    // Korbboden
    fuelleQuad(kLU, kU, kZV, kRU, kU, kZV, kRU, kU, kZH, kLU, kU, kZH, LISTE_KORB_SCHATTEN.copy(alpha = 0.32f))

    // Drei bunte 3D-Einkaufspakete im Korb — hüpfen beim Abhaken des jeweiligen Listenpunkts auf!
    fun zeichnePaket(px: Float, py: Float, pz: Float, wB: Float, hB: Float, tB: Float, farbe: Color, huepf: Float) {
        val yTop = py - hB - huepf * 0.12f
        val yBot = py - huepf * 0.12f
        val x0 = px - wB * 0.5f; val x1 = px + wB * 0.5f
        val z0 = pz - tB * 0.5f; val z1 = pz + tB * 0.5f
        // Rechte Seitenfläche (dunkler)
        fuelleQuad(x1, yTop, z0, x1, yTop, z1, x1, yBot, z1, x1, yBot, z0, lerp(farbe, Color.Black, 0.34f))
        // Deckel (heller)
        fuelleQuad(x0, yTop, z1, x1, yTop, z1, x1, yTop, z0, x0, yTop, z0, lerp(farbe, Color.White, 0.28f))
        // Vorderseite
        fuelleQuad(x0, yTop, z0, x1, yTop, z0, x1, yBot, z0, x0, yBot, z0, farbe)
    }

    val pop0 = sin(haken0 * PI.toFloat()).coerceIn(0f, 1f)
    val pop1 = sin(haken1 * PI.toFloat()).coerceIn(0f, 1f)
    val pop2 = sin(haken2 * PI.toFloat()).coerceIn(0f, 1f)
    // Hinteres Paket zuerst, dann vordere (Painter's Order)
    zeichnePaket(wx + 0.04f, kU - 0.01f, wz + 0.04f, 0.17f, 0.20f, 0.13f, LISTE_PAKET_3, pop2)
    zeichnePaket(wx - 0.07f, kU - 0.01f, wz - 0.03f, 0.16f, 0.18f, 0.13f, LISTE_PAKET_1, pop0)
    zeichnePaket(wx + 0.10f, kU - 0.01f, wz - 0.05f, 0.14f, 0.15f, 0.12f, LISTE_PAKET_2, pop1)

    // Linke und rechte 3D-Korb-Querverbindungen + Schiebegriff
    linie3D(kLO, kO, kZV, kLO, kO, kZH, LISTE_KORB_METALL, draht)
    linie3D(kRO, kO, kZV, kRO, kO, kZH, LISTE_KORB_METALL, draht)
    linie3D(kLU, kU, kZV, kLU, kU, kZH, LISTE_KORB_SCHATTEN, draht * 0.9f)
    linie3D(kRU, kU, kZV, kRU, kU, kZH, LISTE_KORB_SCHATTEN, draht * 0.9f)

    // Vordere Korbwand mit Gitterstreben (z = kZV)
    fuelleQuad(kLO, kO, kZV, kRO, kO, kZV, kRU, kU, kZV, kLU, kU, kZV, LISTE_KORB_METALL.copy(alpha = 0.16f))
    // Waagerechte Gitterlinie mittig
    val mY = (kO + kU) * 0.5f
    linie3D((kLO + kLU) * 0.5f, mY, kZV, (kRO + kRU) * 0.5f, mY, kZV, LISTE_KORB_METALL.copy(alpha = 0.72f), draht * 0.75f)
    // Senkrechte Gitterstreben
    for (g in 1..2) {
        val fG = g / 3f
        linie3D(kLO + (kRO - kLO) * fG, kO, kZV, kLU + (kRU - kLU) * fG, kU, kZV, LISTE_KORB_METALL.copy(alpha = 0.72f), draht * 0.75f)
    }
    umrissQuad(kLO, kO, kZV, kRO, kO, kZV, kRU, kU, kZV, kLU, kU, kZV, LISTE_KORB_METALL, draht)

    // Schiebegriff links oben (am Korb-Ende mit farbigem Griffstück)
    val gX = kLO - 0.11f; val gY = kO - 0.12f
    linie3D(kLO, kO, kZV, gX, gY, kZV, LISTE_KORB_METALL, draht)
    linie3D(kLO, kO, kZH, gX, gY, kZH, LISTE_KORB_SCHATTEN, draht * 0.9f)
    linie3D(gX, gY, kZV, gX, gY, kZH, LISTE_GRIFF_KORALLE, draht * 1.9f)

    // Untergestell (Stütze + Achse)
    val achseY = kU + 0.12f
    linie3D(kLU, kU, kZV, kLU - 0.04f, achseY, kZV, LISTE_KORB_METALL, draht * 0.9f)
    linie3D(kLU - 0.04f, achseY, kZV, kRU + 0.02f, achseY, kZV, LISTE_KORB_METALL, draht * 0.9f)
    linie3D(kLU - 0.04f, achseY, kZH, kRU + 0.02f, achseY, kZH, LISTE_KORB_SCHATTEN, draht * 0.8f)

    // Zwei plastische 3D-Räder mit rotierenden Speichen
    puffer.radX[0] = kLU - 0.01f
    puffer.radX[1] = kRU - 0.01f
    val radY = achseY + 0.07f
    val radR = 0.068f
    for (rIdx in 0..1) {
        val rxPos = puffer.radX[rIdx]
        val cx2d = projX(rxPos, radY, kZV)
        val cy2d = projY(rxPos, radY, kZV)
        val rPx = radR * skala * projS(rxPos, radY, kZV)
        drawCircle(Color(0xFF1F1410), rPx, Offset(cx2d, cy2d))
        drawCircle(LISTE_KORB_METALL, rPx, Offset(cx2d, cy2d), style = Stroke(width = 1.6f * density))
        // Rotierende Speiche
        val sx0 = cos(radDreh) * rPx * 0.7f
        val sy0 = sin(radDreh) * rPx * 0.7f
        drawLine(LISTE_KUPFER, Offset(cx2d - sx0, cy2d - sy0), Offset(cx2d + sx0, cy2d + sy0), strokeWidth = 1.2f * density, cap = StrokeCap.Round)
        drawCircle(LISTE_CLIP_GOLD, rPx * 0.28f, Offset(cx2d, cy2d))
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
