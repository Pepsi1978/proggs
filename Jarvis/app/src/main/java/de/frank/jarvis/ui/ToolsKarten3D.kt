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
            if (size.minDimension < 2f) return@Canvas
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
    // Mentalität: tiefes Nacht-Indigo/Violett
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

    // Ruhige, edle 3D-Schwenk-Parallax für Klemmbrett und Einkaufswagen
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

    // Nur die aus aktueller Perspektive sichtbare 3D-Seitenkante zeichnen (verhindert Durchscheinen)
    if (yaw <= 0f) {
        fuelleQuad(xR, yO, zV, xR, yO, zH, xR, yU, zH, xR, yU, zV, LISTE_KANTE)
    } else {
        fuelleQuad(xL, yO, zH, xL, yO, zV, xL, yU, zV, xL, yU, zH, LISTE_KANTE)
    }
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
// 3D-Szene 2: Mentalität — Weich modelliertes, sofort erkennbares 3D-Gehirn (beide Hemisphären,
// Schläfenlappen, Kleinhirn, Hirnstamm, plastische Windungen & fließende Gedankenwellen — ohne Ringe)
// ================================================================================================

private const val HIRN_ZYKLUS_S = 8.4f
private const val EINSTELLUNG_ZYKLUS_S = 14.0f
private const val HIRN_STAUB_ANZAHL = 14

private data class HirnKurve(
    val x: FloatArray,
    val y: FloatArray,
    val z: FloatArray,
    val breite: Float,
    val tiefeFurche: Boolean = false,
)

private data class GedankenSpur(
    val kurvenIndex: Int,
    val farbe: Color,
    val versatz: Float,
    val richtung: Int,
)

private data class HirnStaub(
    val x: Float,
    val y: Float,
    val z: Float,
    val phase: Float,
    val radius: Float,
)

private object GehirnModell {
    // Anatomische Großhirn-Silhouette (geschlossen, im Uhrzeigersinn, Stirn links -> Scheitel -> Hinterhaupt -> Schläfenlappen)
    val grosshirnX = floatArrayOf(
        -0.93f, -0.92f, -0.80f, -0.58f, -0.28f, 0.06f, 0.40f, 0.68f, 0.87f,
        0.95f, 0.91f, 0.77f, 0.56f, 0.34f, 0.14f, -0.06f, -0.30f, -0.54f, -0.76f, -0.90f,
    )
    val grosshirnY = floatArrayOf(
        -0.02f, -0.26f, -0.49f, -0.65f, -0.74f, -0.76f, -0.70f, -0.55f, -0.32f,
        -0.04f, 0.18f, 0.28f, 0.24f, 0.23f, 0.33f, 0.38f, 0.35f, 0.23f, 0.24f, 0.14f,
    )

    // Kleinhirn (Cerebellum) rechts unten unter dem Hinterhauptslappen — macht die Gehirnform sofort unverkennbar
    val kleinhirnX = floatArrayOf(0.24f, 0.44f, 0.68f, 0.81f, 0.77f, 0.60f, 0.39f, 0.24f)
    val kleinhirnY = floatArrayOf(0.24f, 0.20f, 0.22f, 0.33f, 0.48f, 0.57f, 0.54f, 0.40f)

    // Weicher Hirnstamm-Ansatz (Brainstem) unten mittig-rechts
    val stammX = floatArrayOf(0.02f, 0.24f, 0.30f, 0.27f, 0.16f, 0.05f, 0.01f)
    val stammY = floatArrayOf(0.27f, 0.27f, 0.44f, 0.68f, 0.76f, 0.69f, 0.48f)

    /** Berechnet eine weiche 3D-Kuppelwölbung (z) für einen Punkt auf der Hemisphären-Oberfläche. */
    private fun kuppelZ(x: Float, y: Float, basisZ: Float): Float {
        val nx = (x / 0.96f).coerceIn(-1f, 1f)
        val ny = ((y + 0.16f) / 0.64f).coerceIn(-1f, 1f)
        val r2 = (nx * nx + ny * ny).coerceIn(0f, 1f)
        return basisZ - sqrt((1f - r2 * 0.88f).coerceAtLeast(0.04f)) * 0.30f
    }

    private fun kurve(punkte: List<Pair<Float, Float>>, basisZ: Float, breite: Float, tiefeFurche: Boolean = false): HirnKurve {
        val n = punkte.size
        val xs = FloatArray(n)
        val ys = FloatArray(n)
        val zs = FloatArray(n)
        for (i in 0 until n) {
            val (px, py) = punkte[i]
            xs[i] = px
            ys[i] = py
            zs[i] = kuppelZ(px, py, basisZ)
        }
        return HirnKurve(xs, ys, zs, breite, tiefeFurche)
    }

    // Ikonische, weich geschwungene Hirnwindungen (Gyri) & die Sylvische Furche auf der vorderen Hemisphäre
    val windungen: List<HirnKurve> = listOf(
        // 0: Charakteristische Sylvische Furche (trennt Stirn-/Scheitellappen klar vom Schläfenlappen)
        kurve(
            listOf(-0.56f to 0.14f, -0.34f to 0.08f, -0.10f to 0.02f, 0.14f to -0.06f, 0.34f to -0.15f),
            basisZ = -0.16f, breite = 2.6f, tiefeFurche = true,
        ),
        // 1: Obere Stirn- und Scheitelwindung (großer geschwungener Bogen)
        kurve(
            listOf(-0.78f to -0.24f, -0.58f to -0.44f, -0.30f to -0.56f, 0.02f to -0.58f, 0.32f to -0.50f),
            basisZ = -0.18f, breite = 3.1f,
        ),
        // 2: Mittlere Stirnwindung (S-förmig geschwungen)
        kurve(
            listOf(-0.80f to -0.02f, -0.60f to -0.18f, -0.36f to -0.16f, -0.18f to -0.32f, 0.06f to -0.36f),
            basisZ = -0.20f, breite = 2.9f,
        ),
        // 3: Zentrale Scheitel-Schleife (senkrecht gewellt wie echte Zentralfurche)
        kurve(
            listOf(-0.10f to -0.66f, -0.18f to -0.44f, -0.06f to -0.22f, -0.16f to -0.04f),
            basisZ = -0.20f, breite = 2.8f,
        ),
        // 4: Hintere Scheitel- und Hinterhauptswindung
        kurve(
            listOf(0.16f to -0.64f, 0.36f to -0.42f, 0.56f to -0.46f, 0.74f to -0.26f, 0.78f to -0.04f),
            basisZ = -0.18f, breite = 3.0f,
        ),
        // 5: Hinterhaupt-Bogen (Okzipitalwindung)
        kurve(
            listOf(0.32f to -0.24f, 0.52f to -0.16f, 0.68f to -0.02f, 0.74f to 0.14f, 0.56f to 0.16f),
            basisZ = -0.18f, breite = 2.8f,
        ),
        // 6: Obere Schläfenwindung (Temporal-Bogen unterhalb der Sylvischen Furche)
        kurve(
            listOf(-0.46f to 0.22f, -0.22f to 0.20f, 0.02f to 0.16f, 0.24f to 0.08f, 0.42f to 0.04f),
            basisZ = -0.20f, breite = 2.9f,
        ),
        // 7: Untere Schläfenwindung
        kurve(
            listOf(-0.34f to 0.29f, -0.10f to 0.30f, 0.14f to 0.24f, 0.30f to 0.16f),
            basisZ = -0.18f, breite = 2.5f,
        ),
        // 8: Stirnpol-Schleife vorn links
        kurve(
            listOf(-0.74f to 0.12f, -0.58f to -0.01f, -0.40f to -0.04f, -0.30f to -0.26f),
            basisZ = -0.19f, breite = 2.5f,
        ),
        // 9: Mittlere Querschleife (verbindet Scheitel und Hinterhaupt)
        kurve(
            listOf(0.06f to -0.22f, 0.24f to -0.34f, 0.46f to -0.30f, 0.60f to -0.16f),
            basisZ = -0.20f, breite = 2.6f,
        ),
    )

    // Feine parallele Kleinhirn-Rillen (Folia) — typische Struktur des Kleinhirns
    val kleinhirnFalten: List<HirnKurve> = listOf(
        kurve(listOf(0.32f to 0.31f, 0.50f to 0.28f, 0.71f to 0.32f), basisZ = -0.06f, breite = 1.6f),
        kurve(listOf(0.30f to 0.38f, 0.52f to 0.36f, 0.74f to 0.40f), basisZ = -0.06f, breite = 1.6f),
        kurve(listOf(0.33f to 0.45f, 0.52f to 0.44f, 0.71f to 0.47f), basisZ = -0.06f, breite = 1.5f),
        kurve(listOf(0.39f to 0.51f, 0.55f to 0.51f, 0.66f to 0.52f), basisZ = -0.06f, breite = 1.3f),
    )

    // Weiche Gedankenwellen, die direkt entlang der Hirnwindungen fließen (ohne kantiges Netz)
    val spuren: List<GedankenSpur> = listOf(
        GedankenSpur(kurvenIndex = 1, farbe = HIRN_CYAN, versatz = 0.00f, richtung = 1),
        GedankenSpur(kurvenIndex = 4, farbe = HIRN_MAGENTA, versatz = 0.24f, richtung = 1),
        GedankenSpur(kurvenIndex = 2, farbe = HIRN_GOLD, versatz = 0.50f, richtung = 1),
        GedankenSpur(kurvenIndex = 6, farbe = HIRN_RUHE, versatz = 0.74f, richtung = -1),
        GedankenSpur(kurvenIndex = 9, farbe = HIRN_KREATIV, versatz = 0.38f, richtung = 1),
    )

    // Zarte, sanft atmende Lichtstäubchen im weichen Raum (keine harten Linien)
    val staub: List<HirnStaub> = List(HIRN_STAUB_ANZAHL) { i ->
        val winkel = (i.toFloat() / HIRN_STAUB_ANZAHL) * TAU + 0.3f
        val r = 1.05f + (i % 3) * 0.18f
        HirnStaub(
            x = cos(winkel) * r,
            y = sin(winkel) * r * 0.82f - 0.04f,
            z = ((i % 5) - 2) * 0.22f,
            phase = i.toFloat() / HIRN_STAUB_ANZAHL,
            radius = 1.4f + (i % 3) * 0.65f,
        )
    }
}

private class GehirnZeichenPuffer {
    val pfad = Path()
    val kurvePfad = Path()
    val schattenPfad = Path()
    val glanzPfad = Path()
    val px = FloatArray(24)
    val py = FloatArray(24)
    val ps = FloatArray(24)
}

private fun DrawScope.zeichneGehirn3D(t: Float, puffer: GehirnZeichenPuffer) {
    val mitteX = size.width * 0.50f
    val mitteY = size.height * 0.51f
    val skala = size.minDimension * 0.44f

    // Nahtlose, ruhige 3D-Schwebebewegung in ikonischer 3/4-Seitenansicht:
    // Kein 360°-Kreiseln mehr — das Gehirn bleibt immer sofort als Gehirn erkennbar!
    val w = (t / HIRN_ZYKLUS_S) * TAU
    val yaw = -0.18f + 0.22f * sin(w)
    val pitch = 0.11f + 0.06f * cos(w)
    val schwebeY = sin(w) * 0.032f
    val cy = cos(yaw); val sy = sin(yaw)
    val cx = cos(pitch); val sx = sin(pitch)

    // Innere Einstellungen: sanfter, weicher Farbwandel (Fokus -> Kreativität -> Ruhe)
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

    fun projX(x: Float, y: Float, z: Float): Float {
        val yw = y + schwebeY
        val rx = x * cy + z * sy
        val rz1 = -x * sy + z * cy
        val rz = yw * sx + rz1 * cx
        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
        return mitteX + rx * skala * s
    }

    fun projY(x: Float, y: Float, z: Float): Float {
        val yw = y + schwebeY
        val rz1 = -x * sy + z * cy
        val ry = yw * cx - rz1 * sx
        val rz = yw * sx + rz1 * cx
        val s = FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
        return mitteY + ry * skala * s
    }

    fun projS(x: Float, y: Float, z: Float): Float {
        val yw = y + schwebeY
        val rz1 = -x * sy + z * cy
        val rz = yw * sx + rz1 * cx
        return FOKAL / (FOKAL + rz).coerceAtLeast(0.7f)
    }

    /** Baut einen glatten, weich gerundeten geschlossenen Pfad aus projizierten 3D-Konturpunkten. */
    fun baueWeicheKontur(
        xs: FloatArray,
        ys: FloatArray,
        zEbene: Float,
        xVersatz: Float,
        yVersatz: Float,
        xSkal: Float,
        ySkal: Float,
        ziel: Path,
    ) {
        val n = xs.size
        for (i in 0 until n) {
            val vx = xs[i] * xSkal + xVersatz
            val vy = ys[i] * ySkal + yVersatz
            puffer.px[i] = projX(vx, vy, zEbene)
            puffer.py[i] = projY(vx, vy, zEbene)
        }
        ziel.rewind()
        val startX = (puffer.px[n - 1] + puffer.px[0]) * 0.5f
        val startY = (puffer.py[n - 1] + puffer.py[0]) * 0.5f
        ziel.moveTo(startX, startY)
        for (i in 0 until n) {
            val next = (i + 1) % n
            val mx = (puffer.px[i] + puffer.px[next]) * 0.5f
            val my = (puffer.py[i] + puffer.py[next]) * 0.5f
            ziel.quadraticTo(puffer.px[i], puffer.py[i], mx, my)
        }
        ziel.close()
    }

    /** Baut eine weiche, offene Kurve durch projizierte 3D-Punkte (optional mit Pixel-Versatz). */
    fun baueWeicheKurve(k: HirnKurve, dxPx: Float, dyPx: Float, ziel: Path): Float {
        val n = k.x.size
        var sSumme = 0f
        for (i in 0 until n) {
            puffer.px[i] = projX(k.x[i], k.y[i], k.z[i]) + dxPx
            puffer.py[i] = projY(k.x[i], k.y[i], k.z[i]) + dyPx
            val s = projS(k.x[i], k.y[i], k.z[i])
            puffer.ps[i] = s
            sSumme += s
        }
        ziel.rewind()
        ziel.moveTo(puffer.px[0], puffer.py[0])
        if (n == 2) {
            ziel.lineTo(puffer.px[1], puffer.py[1])
        } else {
            for (i in 1 until n - 1) {
                val mx = (puffer.px[i] + puffer.px[i + 1]) * 0.5f
                val my = (puffer.py[i] + puffer.py[i + 1]) * 0.5f
                ziel.quadraticTo(puffer.px[i], puffer.py[i], mx, my)
            }
            ziel.lineTo(puffer.px[n - 1], puffer.py[n - 1])
        }
        return sSumme / n
    }

    // 1) Weicher, organischer Lichthof im Hintergrund (KEINE scharfkantigen Ringe!)
    val auraR = skala * (1.28f + 0.08f * sin(w))
    drawCircle(
        Brush.radialGradient(
            listOf(
                haltungFarbe.copy(alpha = 0.34f),
                haltungZweit.copy(alpha = 0.14f),
                Color.Transparent,
            ),
            Offset(mitteX - skala * 0.06f, mitteY - skala * 0.08f),
            auraR,
        ),
        radius = auraR,
        center = Offset(mitteX, mitteY),
    )

    // Zarte, weich leuchtende Lichtstäubchen (harmonisch-organisch, ohne Linien)
    for (p in GehirnModell.staub) {
        val u = ((t * 0.09f + p.phase) % 1f + 1f) % 1f
        val puls = sin(u * PI.toFloat()).coerceIn(0f, 1f)
        val alpha = puls * puls * 0.42f
        if (alpha < 0.02f) continue
        val sx2d = projX(p.x + sin(w + p.phase * TAU) * 0.06f, p.y - puls * 0.08f, p.z)
        val sy2d = projY(p.x + sin(w + p.phase * TAU) * 0.06f, p.y - puls * 0.08f, p.z)
        val r = p.radius * density * (0.7f + 0.3f * puls)
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = alpha), haltungZweit.copy(alpha = alpha * 0.45f), Color.Transparent),
                Offset(sx2d, sy2d),
                r * 2.4f,
            ),
            radius = r * 2.4f,
            center = Offset(sx2d, sy2d),
            blendMode = BlendMode.Plus,
        )
    }

    // 2) Hintere Gehirnhälfte (leicht nach hinten-oben versetzt für echte 3D-Tiefe beider Hemisphären)
    baueWeicheKontur(
        GehirnModell.grosshirnX, GehirnModell.grosshirnY,
        zEbene = 0.34f, xVersatz = 0.06f, yVersatz = -0.07f, xSkal = 0.96f, ySkal = 0.96f,
        ziel = puffer.pfad,
    )
    drawPath(
        path = puffer.pfad,
        brush = Brush.radialGradient(
            listOf(
                lerp(haltungFarbe, Color.White, 0.16f).copy(alpha = 0.62f),
                lerp(haltungFarbe, Color(0xFF141033), 0.52f).copy(alpha = 0.72f),
                Color(0xFF0E0B26).copy(alpha = 0.65f),
            ),
            center = Offset(mitteX - skala * 0.18f, mitteY - skala * 0.45f),
            radius = skala * 1.25f,
        ),
    )
    drawPath(
        path = puffer.pfad,
        color = lerp(haltungFarbe, Color.White, 0.30f).copy(alpha = 0.24f),
        style = Stroke(width = 1.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // 3) Weicher Hirnstamm (Brainstem) unten mittig-rechts
    baueWeicheKontur(
        GehirnModell.stammX, GehirnModell.stammY,
        zEbene = 0.08f, xVersatz = 0f, yVersatz = 0f, xSkal = 1f, ySkal = 1f,
        ziel = puffer.pfad,
    )
    drawPath(
        path = puffer.pfad,
        brush = Brush.linearGradient(
            listOf(
                lerp(haltungFarbe, Color(0xFF1C1642), 0.22f).copy(alpha = 0.88f),
                lerp(haltungFarbe, Color(0xFF0C0A22), 0.62f).copy(alpha = 0.72f),
            ),
            start = Offset(mitteX, mitteY + skala * 0.22f),
            end = Offset(mitteX + skala * 0.18f, mitteY + skala * 0.82f),
        ),
    )

    // 4) Kleinhirn (Cerebellum) rechts unten mit seinen typischen feinen weichen Falten
    baueWeicheKontur(
        GehirnModell.kleinhirnX, GehirnModell.kleinhirnY,
        zEbene = 0.02f, xVersatz = 0f, yVersatz = 0f, xSkal = 1f, ySkal = 1f,
        ziel = puffer.pfad,
    )
    drawPath(
        path = puffer.pfad,
        brush = Brush.radialGradient(
            listOf(
                lerp(haltungZweit, Color.White, 0.24f).copy(alpha = 0.86f),
                lerp(haltungFarbe, haltungZweit, 0.45f).copy(alpha = 0.84f),
                lerp(haltungFarbe, Color(0xFF100C2B), 0.62f).copy(alpha = 0.88f),
            ),
            center = Offset(projX(0.46f, 0.30f, 0f), projY(0.46f, 0.30f, 0f)),
            radius = skala * 0.46f,
        ),
    )
    for (falte in GehirnModell.kleinhirnFalten) {
        val sF = baueWeicheKurve(falte, 0f, 0.8f * density, puffer.schattenPfad)
        drawPath(
            path = puffer.schattenPfad,
            color = Color(0xFF110B2E).copy(alpha = 0.45f),
            style = Stroke(width = falte.breite * 1.35f * density * sF, cap = StrokeCap.Round),
        )
        baueWeicheKurve(falte, 0f, 0f, puffer.kurvePfad)
        drawPath(
            path = puffer.kurvePfad,
            color = lerp(haltungZweit, Color.White, 0.45f).copy(alpha = 0.50f),
            style = Stroke(width = falte.breite * 0.72f * density * sF, cap = StrokeCap.Round),
        )
    }

    // 5) Vordere Haupt-Hemisphäre (weich plastisch ausgeleuchtete Großhirn-Form)
    baueWeicheKontur(
        GehirnModell.grosshirnX, GehirnModell.grosshirnY,
        zEbene = -0.14f, xVersatz = 0f, yVersatz = 0f, xSkal = 1f, ySkal = 1f,
        ziel = puffer.pfad,
    )
    // Weicher 3D-Hauptkörper mit Licht von oben links und sanftem Schatten unten rechts
    drawPath(
        path = puffer.pfad,
        brush = Brush.radialGradient(
            listOf(
                lerp(haltungFarbe, Color.White, 0.56f).copy(alpha = 0.94f),
                lerp(haltungFarbe, haltungZweit, 0.28f).copy(alpha = 0.90f),
                lerp(haltungFarbe, Color(0xFF141038), 0.56f).copy(alpha = 0.92f),
            ),
            center = Offset(projX(-0.34f, -0.38f, -0.2f), projY(-0.34f, -0.38f, -0.2f)),
            radius = skala * 1.48f,
        ),
    )
    // Inneres weiches Subsurface-Leuchten im Zentrum des Gehirns
    val innenX = projX(-0.04f, -0.16f, -0.22f)
    val innenY = projY(-0.04f, -0.16f, -0.22f)
    drawCircle(
        Brush.radialGradient(
            listOf(
                lerp(haltungZweit, Color.White, 0.40f).copy(alpha = 0.28f + 0.08f * sin(w)),
                haltungFarbe.copy(alpha = 0.10f),
                Color.Transparent,
            ),
            Offset(innenX, innenY),
            skala * 0.78f,
        ),
        radius = skala * 0.78f,
        center = Offset(innenX, innenY),
        blendMode = BlendMode.Plus,
    )
    // Seidiger weicher Rand-Schimmer (Rim-Light) für 3D-Volumen ohne harte Kontur
    drawPath(
        path = puffer.pfad,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.55f),
                haltungZweit.copy(alpha = 0.26f),
                haltungFarbe.copy(alpha = 0.42f),
            ),
            start = Offset(mitteX - skala, mitteY - skala),
            end = Offset(mitteX + skala, mitteY + skala),
        ),
        style = Stroke(width = 1.9f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // 6) Plastische, weich geschwungene 3D-Hirnwindungen (Gyri & Sulci)
    for (windung in GehirnModell.windungen) {
        val sM = baueWeicheKurve(windung, 0.9f * density, 1.4f * density, puffer.schattenPfad)
        val bPx = windung.breite * density * sM
        // Weiche dunkle Furche (Sulcus) für echtes organisches Relief
        drawPath(
            path = puffer.schattenPfad,
            color = Color(0xFF100B2C).copy(alpha = if (windung.tiefeFurche) 0.58f else 0.40f),
            style = Stroke(width = bPx * (if (windung.tiefeFurche) 1.55f else 1.30f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        if (!windung.tiefeFurche) {
            // Weicher gewölbter Gyrus-Wulst
            baueWeicheKurve(windung, 0f, 0f, puffer.kurvePfad)
            drawPath(
                path = puffer.kurvePfad,
                color = lerp(haltungFarbe, haltungZweit, 0.35f).copy(alpha = 0.52f),
                style = Stroke(width = bPx * 1.05f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Seidiger Glanzkamm oben links auf jeder Windung
            baueWeicheKurve(windung, -0.7f * density, -0.9f * density, puffer.glanzPfad)
            drawPath(
                path = puffer.glanzPfad,
                color = Color.White.copy(alpha = 0.46f),
                style = Stroke(width = bPx * 0.42f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            // Zarter Lichtsaum an der Kante der Sylvischen Furche
            baueWeicheKurve(windung, -0.6f * density, -1.0f * density, puffer.glanzPfad)
            drawPath(
                path = puffer.glanzPfad,
                color = Color.White.copy(alpha = 0.28f),
                style = Stroke(width = bPx * 0.38f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }

    // 7) Weiche, leuchtende Gedankenwellen fließen direkt entlang der Hirnwindungen (nahtlose Schleife)
    val impulsBasis = ((t % HIRN_ZYKLUS_S) + HIRN_ZYKLUS_S) % HIRN_ZYKLUS_S / HIRN_ZYKLUS_S
    for (spur in GehirnModell.spuren) {
        val p = ((impulsBasis - spur.versatz) % 1f + 1f) % 1f
        if (p > 0.72f) continue
        val lauf = (p / 0.72f).coerceIn(0f, 1f)
        val h = sin(lauf * PI.toFloat()).coerceIn(0f, 1f)
        val huelle = h * h
        if (huelle < 0.02f) continue

        val k = GehirnModell.windungen[spur.kurvenIndex]
        val segmente = k.x.size - 1
        val kopfU = if (spur.richtung > 0) lauf else 1f - lauf
        val fPos = kopfU * segmente
        val idx0 = fPos.toInt().coerceIn(0, segmente - 1)
        val bruch = (fPos - idx0).coerceIn(0f, 1f)

        val kx = k.x[idx0] + (k.x[idx0 + 1] - k.x[idx0]) * bruch
        val ky = k.y[idx0] + (k.y[idx0 + 1] - k.y[idx0]) * bruch
        val kz = k.z[idx0] + (k.z[idx0 + 1] - k.z[idx0]) * bruch - 0.03f
        val x2d = projX(kx, ky, kz)
        val y2d = projY(kx, ky, kz)
        val s2d = projS(kx, ky, kz)

        // Sanft glühender Schweif kurz hinter dem Gedanken-Impuls
        val schwU = (kopfU - spur.richtung * 0.22f).coerceIn(0f, 1f)
        val sPos = schwU * segmente
        val sIdx = sPos.toInt().coerceIn(0, segmente - 1)
        val sBruch = (sPos - sIdx).coerceIn(0f, 1f)
        val sx3 = k.x[sIdx] + (k.x[sIdx + 1] - k.x[sIdx]) * sBruch
        val sy3 = k.y[sIdx] + (k.y[sIdx + 1] - k.y[sIdx]) * sBruch
        val sz3 = k.z[sIdx] + (k.z[sIdx + 1] - k.z[sIdx]) * sBruch - 0.03f
        val sx2d = projX(sx3, sy3, sz3)
        val sy2d = projY(sx3, sy3, sz3)

        if (abs(x2d - sx2d) + abs(y2d - sy2d) > 0.5f) {
            drawLine(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, spur.farbe.copy(alpha = 0.65f * huelle), Color.White.copy(alpha = 0.80f * huelle)),
                    start = Offset(sx2d, sy2d),
                    end = Offset(x2d, y2d),
                ),
                start = Offset(sx2d, sy2d),
                end = Offset(x2d, y2d),
                strokeWidth = 3.2f * density * s2d,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus,
            )
        }

        // Weicher Licht-Tropfen als Gedanken-Spitze
        val haloR = 8.5f * density * s2d * (0.7f + 0.3f * h)
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.88f * huelle), spur.farbe.copy(alpha = 0.55f * huelle), Color.Transparent),
                Offset(x2d, y2d),
                haloR,
            ),
            radius = haloR,
            center = Offset(x2d, y2d),
            blendMode = BlendMode.Plus,
        )
    }
}
