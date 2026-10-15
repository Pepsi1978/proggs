package de.frank.wecker.design

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.wecker.rememberResumed
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Morgenruhes Motiv als Zimmer: ein ruhiges Schlafzimmer in Zentralperspektive — Decke, Rückwand,
 * angeschnittene Seitenwände und ein Dielenboden, dessen Fugen in einen Fluchtpunkt auf Augenhöhe
 * laufen. Alles steht auf diesem Boden oder hängt an dieser Wand: das Bett längs unter dem Fenster,
 * daneben der Nachttisch mit Lampe und Wecker, die Blume auf der Fensterbank, ein Bild an der Wand,
 * links die Tür. Seit 15.10.2026 statt der frei verteilten Einzelobjekte auf leerem Grund.
 *
 * Die Geschichte bleibt: Die Tür geht auf, die Person im gestreiften Schlafanzug kommt herein, die
 * Nachttischlampe brennt, sie legt sich hin, die Lampe geht aus, die Katze schläft am Fußende. Die
 * Sonne geht im Fenster auf, der Wecker klingelt, die Person setzt sich auf, streckt sich, steht auf
 * und geht wieder hinaus. Danach Ruhe, dann von vorn.
 *
 * Breitenunabhängig: Höhe bestimmt den Maßstab, zusätzliche Breite wird zu mehr Raum (breitere
 * Rückwand, Kommode und Pflanze), nicht zu größeren Möbeln. In schmalen oder quadratischen Feldern
 * steht das Zimmer als Band mittig. Die Zeit wird nur im Zeichenblock gelesen; die Szene läuft nur,
 * solange der Bildschirm vorne ist, bei reduzierter Bewegung steht ein ruhiges Morgenbild.
 */
@Composable
internal fun SchlafSzene(modifier: Modifier) {
    val gold = LocalGold.current
    val dunkel = gold.istDunkel
    val reduziert = LocalBewegungReduziert.current
    val aktiv = rememberResumed() && !reduziert
    val zeit = remember { mutableLongStateOf(0L) }
    if (aktiv) LaunchedEffect(Unit) {
        var start = -1L
        var letzte = 0L
        while (true) {
            // ~30 Bilder/s reichen für die ruhige Szene und schonen den Akku.
            withFrameMillis { t ->
                if (start < 0) start = t - zeit.longValue
                if (t - letzte >= 32) { zeit.longValue = t - start; letzte = t }
            }
        }
    }
    val farben = SzenenFarben.fuer(dunkel, gold.textGedaempft)
    Canvas(modifier) {
        val t = if (reduziert) STANDBILD else (zeit.longValue % (ZYKLUS * 1000).toLong()) / 1000f
        zeichneZimmer(t, farben)
    }
}

private const val ZYKLUS = 26.6f
/** Standbild: Morgen, die Person sitzt im Bett und streckt sich. */
private const val STANDBILD = 15.8f

// --- Raum: Zentralperspektive. y in Anteilen der Bandhöhe, x in Anteilen der Höhe ab der Mitte. ---
/** Augenhöhe und Fluchtpunkt. */
private const val FLUCHT = 0.40f
/** Bodenlinie der vordersten Ebene (Tiefenmaß k = 1). */
private const val VORN = 0.90f
/** Tiefenmaß der Rückwand: Alles an der Wand erscheint um diesen Faktor kleiner. */
private const val KW = 0.52f
private const val WAND_FUSS = FLUCHT + (VORN - FLUCHT) * KW
private const val WAND_KOPF = FLUCHT - (VORN - FLUCHT) * KW
/** Ein Band ist mindestens so breit; schmalere Felder bekommen das Band mittig. */
private const val MIN_SEITENVERHAELTNIS = 1.7f

// Bett (Vorderebenen-Maße): Fußteil links, Kopfteil rechts, Rücken an der Wand.
private const val BETT_FUSS = -0.20f
private const val BETT_KOPF = 0.76f
private const val BETT_VORN = 0.70f
private const val MATRATZE = 0.20f

// Person in eigenen Einheiten; PS rechnet sie in Vorderebenen-Maße um.
private const val PS = 1.65f
private const val TORSO = 0.12f
private const val HALS = 0.047f
private const val KOPF = 0.034f
private const val ARM = 0.10f
private const val BEIN = 0.15f

private class SzenenFarben(
    val holz: Color, val holzDunkel: Color, val laken: Color, val decke: Color, val kissen: Color,
    val anzug: Color, val streifen: Color, val haut: Color, val haar: Color, val innen: Color,
    val sonne: Color, val mond: Color, val z: Color, val schatten: Color,
    val teppich: Color, val vorhang: Color, val katze: Color,
    val wand: Color, val seitenwand: Color, val zimmerdecke: Color, val boden: Color, val bodenNah: Color,
    val leiste: Color, val nacht: Color, val nachtStaerke: Float, val schirm: Color, val bluete: Color, val blatt: Color,
) {
    companion object {
        fun fuer(dunkel: Boolean, gedaempft: Color) = if (dunkel) SzenenFarben(
            holz = Color(0xFF8C6C4D), holzDunkel = Color(0xFF5E4631), laken = Color(0xFFD3CBBC),
            decke = Color(0xFF6F86A8), kissen = Color(0xFFE2DBCF), anzug = Color(0xFFA9BEDB),
            streifen = Color(0xFF5F7CA6), haut = Color(0xFFE2BC98), haar = Color(0xFF4E3B2E),
            innen = Color(0xFF15120F), sonne = Color(0xFFFFC65C), mond = Color(0xFFF3E7C4),
            z = gedaempft, schatten = Color.Black.copy(alpha = .45f),
            teppich = Color(0xFF7E5E55), vorhang = Color(0xFF8C6F6A), katze = Color(0xFFC98A4E),
            // Im Dunkelmodus ein warm gedämpftes Zimmer, kein schwarzes Loch.
            wand = Color(0xFF4E4652), seitenwand = Color(0xFF433B47), zimmerdecke = Color(0xFF575060),
            boden = Color(0xFF4D392B), bodenNah = Color(0xFF634A37), leiste = Color(0xFF6C6372),
            nacht = Color(0xFF0E1226), nachtStaerke = .26f, schirm = Color(0xFFE9D6B4),
            bluete = Color(0xFFE07A8A), blatt = Color(0xFF5E8A5A),
        ) else SzenenFarben(
            holz = Color(0xFFB08A62), holzDunkel = Color(0xFF7A5A3D), laken = Color(0xFFF3EEE5),
            decke = Color(0xFF8098BA), kissen = Color(0xFFFFFCF6), anzug = Color(0xFFB9CDE6),
            streifen = Color(0xFF6F8DB8), haut = Color(0xFFEBC6A3), haar = Color(0xFF5B4535),
            innen = Color(0xFF2B2622), sonne = Color(0xFFFFB94A), mond = Color(0xFFE9D9A8),
            z = gedaempft, schatten = Color.Black.copy(alpha = .20f),
            teppich = Color(0xFFC49A8A), vorhang = Color(0xFFD6B5AE), katze = Color(0xFFE3A263),
            wand = Color(0xFFEFE5D5), seitenwand = Color(0xFFE2D5C1), zimmerdecke = Color(0xFFF7F1E7),
            boden = Color(0xFFBF946A), bodenNah = Color(0xFFD2AA80), leiste = Color(0xFFFBF7F0),
            nacht = Color(0xFF1C2340), nachtStaerke = .30f, schirm = Color(0xFFF6E7CC),
            bluete = Color(0xFFE2687C), blatt = Color(0xFF6E9A62),
        )
    }
}

/** Rechnet Raumkoordinaten in Pixel. [b] Breite, [h] Bandhöhe, [oy] oberer Rand des Bands. */
private class Raum(val b: Float, val h: Float, val oy: Float) {
    val cx = b / 2f
    /** Halbe sichtbare Breite in Höheneinheiten. */
    val halb = b / 2f / h
    /** Halbe Breite der Rückwand: der Rest wird Seitenwand. Breite Felder bekommen mehr Raum. */
    val wb = (halb - 0.14f).coerceIn(0.6f, 1.55f)
    val v = Offset(cx, oy + FLUCHT * h)
    /** Bildschirmpunkt aus Wandkoordinaten (x ab der Mitte, y ab Bandoberkante). */
    fun s(x: Float, y: Float) = Offset(cx + x * h, oy + y * h)
    /** Punkt der Vorderebene (x, Höhe über dem Boden), auf Tiefe k in den Raum geschoben. */
    fun r(x: Float, hoch: Float, k: Float) = Offset(cx + x * k * h, oy + (FLUCHT + (VORN - hoch - FLUCHT) * k) * h)
    /** Wie weit die Kante zwischen Rückwand und Seitenwand bis an den Bildrand läuft. */
    val seitenFaktor get() = halb / wb
}

/** Körperhaltung: Hüfte (x, Höhe über dem Boden, Tiefe k) und Winkel (0° = unten, 90° = rechts, 180° = oben). */
private data class Haltung(val x: Float, val hoch: Float, val k: Float, val torso: Float, val bein: Float, val arm: Float)

private fun stehend(x: Float, k: Float) = Haltung(x, BEIN * PS, k, 180f, 0f, 0f)
/** Vor dem Bett auf dem Teppich. */
private val VOR_DEM_BETT = stehend(0.05f, 0.86f)
private val SITZEND = Haltung(0.30f, 0.215f, 0.61f, 180f, -90f, -35f)
private val LIEGEND = Haltung(0.32f, 0.225f, 0.61f, 90f, -90f, -90f)

private fun mische(a: Haltung, b: Haltung, f: Float) = Haltung(
    a.x + (b.x - a.x) * f, a.hoch + (b.hoch - a.hoch) * f, a.k + (b.k - a.k) * f,
    a.torso + (b.torso - a.torso) * f, a.bein + (b.bein - a.bein) * f, a.arm + (b.arm - a.arm) * f,
)

private class Zustand(
    val haltung: Haltung?, val schwung: Float, val blick: Float, val alpha: Float, val streckt: Float,
    val schlaeft: Boolean, val schlafZeit: Float, val tuer: Float, val sonne: Float,
    /** 1 = die Person liegt oder sitzt im Bett (Beine unter der Decke). */
    val imBett: Float, /** 1 = Decke bis zum Kinn hochgezogen. */ val zugedeckt: Float,
    /** Nachttischlampe 0..1. */ val lampe: Float, /** Wecker klingelt 0..1. */ val klingeln: Float, val t: Float,
)

private fun ab(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun weich(x: Float) = x * x * (3f - 2f * x)
/** 0 → 1 zwischen a und b, hält, 1 → 0 zwischen c und d; außerhalb 0. */
private fun huegel(t: Float, a: Float, b: Float, c: Float, d: Float) =
    if (t < a || t > d) 0f else if (t < c) weich(ab(t, a, b)) else 1f - weich(ab(t, c, d))

/** [tuerX] ist die Türmitte in Vorderebenen-Maß — sie hängt von der Breite der Rückwand ab. */
private fun zustand(t: Float, tuerX: Float): Zustand {
    val tuer = huegel(t, 0f, 1f, 2.8f, 3.8f) + huegel(t, 17.4f, 18.4f, 20.6f, 21.6f)
    val sonne = when {
        t < 3f -> 1f - weich(ab(t, 0f, 3f))
        t < 11f -> 0f
        else -> weich(ab(t, 11f, 14f))
    }
    val imBett = when {
        t < 3.6f -> 0f
        t < 4.8f -> weich(ab(t, 3.6f, 4.8f))
        t < 16.6f -> 1f
        t < 17.8f -> 1f - weich(ab(t, 16.6f, 17.8f))
        else -> 0f
    }
    val zugedeckt = when {
        t < 4.8f -> 0f
        t < 5.8f -> weich(ab(t, 4.8f, 5.8f))
        t < 14f -> 1f
        t < 15f -> 1f - weich(ab(t, 14f, 15f))
        else -> 0f
    }
    val tuerStand = stehend(tuerX, KW + 0.01f)
    val schritt = { s: Float -> (sin(s * 2f * PI.toFloat() * 1.6f) * 26f) }
    var alpha = 1f
    var schwung = 0f
    var blick = 1f
    var streckt = 0f
    val haltung: Haltung? = when {
        t < 0.6f || t > 20.8f -> null
        t < 1.2f -> { alpha = ab(t, 0.6f, 1.2f); tuerStand }
        t < 3.6f -> { schwung = schritt(t - 1.2f); mische(tuerStand, VOR_DEM_BETT, ab(t, 1.2f, 3.6f)) }
        t < 4.8f -> { val f = weich(ab(t, 3.6f, 4.8f)); blick = if (f < .5f) 1f else -1f; mische(VOR_DEM_BETT, SITZEND, f) }
        t < 5.8f -> { blick = -1f; mische(SITZEND, LIEGEND, weich(ab(t, 4.8f, 5.8f))) }
        t < 14f -> { blick = -1f; LIEGEND }
        t < 15f -> { blick = -1f; mische(LIEGEND, SITZEND, weich(ab(t, 14f, 15f))) }
        t < 16.6f -> { blick = -1f; streckt = huegel(t, 15f, 15.6f, 16f, 16.6f); SITZEND }
        t < 17.8f -> { blick = -1f; mische(SITZEND, VOR_DEM_BETT, weich(ab(t, 16.6f, 17.8f))) }
        t < 20.2f -> { blick = -1f; schwung = schritt(t - 17.8f); mische(VOR_DEM_BETT, tuerStand, ab(t, 17.8f, 20.2f)) }
        else -> { blick = -1f; alpha = 1f - ab(t, 20.2f, 20.8f); tuerStand }
    }
    val schlaeft = t in 5.5f..14f
    // Die Lampe brennt, solange die Person ins Zimmer kommt und sich hinlegt.
    val lampe = huegel(t, 0.4f, 1.0f, 5.6f, 6.4f)
    val klingeln = huegel(t, 13.2f, 13.3f, 14.2f, 14.4f)
    return Zustand(haltung, schwung, blick, alpha, streckt, schlaeft, t - 5.8f, tuer, sonne, imBett, zugedeckt, lampe, klingeln, t)
}

private fun richtung(winkel: Float): Offset {
    val r = Math.toRadians(winkel.toDouble())
    return Offset(sin(r).toFloat(), cos(r).toFloat())
}

// Offset ist eine Wertklasse und darf kein vararg sein, daher feste Überladungen.
private fun poly(punkte: List<Offset>) = Path().apply {
    moveTo(punkte[0].x, punkte[0].y)
    for (i in 1 until punkte.size) lineTo(punkte[i].x, punkte[i].y)
    close()
}
private fun poly(a: Offset, b: Offset, c: Offset) = poly(listOf(a, b, c))
private fun poly(a: Offset, b: Offset, c: Offset, d: Offset) = poly(listOf(a, b, c, d))
private fun poly(a: Offset, b: Offset, c: Offset, d: Offset, e: Offset, f: Offset) = poly(listOf(a, b, c, d, e, f))

/**
 * Ein Quader auf dem Boden, Kanten parallel zu Wand und Bildebene. Sichtbar sind die Front, der
 * Deckel (unter Augenhöhe) und die Flanke, die zur Bildmitte zeigt.
 */
private fun DrawScope.quader(
    r: Raum, x0: Float, x1: Float, unten: Float, oben: Float, kHinten: Float, kVorn: Float,
    front: Brush, deckel: Color, seite: Color,
) {
    val sx = if (x1 < 0f) x1 else if (x0 > 0f) x0 else null
    if (sx != null) drawPath(poly(r.r(sx, unten, kHinten), r.r(sx, oben, kHinten), r.r(sx, oben, kVorn), r.r(sx, unten, kVorn)), seite)
    if (oben < VORN - FLUCHT) drawPath(poly(r.r(x0, oben, kHinten), r.r(x1, oben, kHinten), r.r(x1, oben, kVorn), r.r(x0, oben, kVorn)), deckel)
    drawPath(poly(r.r(x0, unten, kVorn), r.r(x1, unten, kVorn), r.r(x1, oben, kVorn), r.r(x0, oben, kVorn)), front)
}

/** Weicher Schatten auf dem Boden unter einem Gegenstand. */
private fun DrawScope.bodenSchatten(r: Raum, x0: Float, x1: Float, kHinten: Float, kVorn: Float, farbe: Color) {
    val a = r.r(x0, 0f, kVorn); val b = r.r(x1, 0f, kVorn); val hinten = r.r(0f, 0f, kHinten)
    val mitte = Offset((a.x + b.x) / 2f, (a.y + hinten.y) / 2f)
    val breite = (b.x - a.x) * 1.08f
    val hoehe = (a.y - hinten.y) * 1.2f + 0.02f * r.h
    drawOval(Brush.radialGradient(listOf(farbe, Color.Transparent), center = mitte, radius = breite / 2f),
        topLeft = Offset(mitte.x - breite / 2f, mitte.y - hoehe / 2f), size = Size(breite, hoehe))
}

private fun DrawScope.zeichneZimmer(t: Float, f: SzenenFarben) {
    val h = min(size.height, size.width / MIN_SEITENVERHAELTNIS)
    val r = Raum(size.width, h, (size.height - h) / 2f)
    val tuerMitte = -r.wb + 0.18f
    val z = zustand(t, tuerMitte / KW)
    clipRect(0f, r.oy, size.width, r.oy + h) { zeichneRaum(r, z, f) }
}

private fun DrawScope.zeichneRaum(r: Raum, z: Zustand, f: SzenenFarben) {
    val h = r.h
    val wb = r.wb
    val sf = r.seitenFaktor
    val links = -r.halb - 0.02f; val rechts = r.halb + 0.02f
    val kanteBoden = FLUCHT + (WAND_FUSS - FLUCHT) * sf
    val kanteDecke = FLUCHT - (FLUCHT - WAND_KOPF) * sf

    // --- Hülle: Decke, Seitenwände, Boden, Rückwand. ---
    drawRect(Brush.verticalGradient(listOf(lerp(f.zimmerdecke, f.holzDunkel, .06f), f.zimmerdecke), startY = r.oy, endY = r.s(0f, WAND_KOPF).y),
        r.s(links, 0f), Size(size.width + 0.1f * h, WAND_KOPF * h + 2f))
    listOf(-1f, 1f).forEach { seite ->
        drawPath(poly(r.s(seite * wb, WAND_KOPF), r.s(seite * r.halb * 1.02f, kanteDecke), r.s(seite * r.halb * 1.02f, kanteBoden), r.s(seite * wb, WAND_FUSS)), f.seitenwand)
        // Decke zwischen Bildrand und Seitenwand.
        drawPath(poly(r.s(seite * wb, WAND_KOPF), r.s(seite * r.halb * 1.02f, kanteDecke), r.s(seite * r.halb * 1.02f, 0f), r.s(seite * wb, 0f)),
            lerp(f.zimmerdecke, f.holzDunkel, .05f))
    }
    val boden = poly(r.s(-wb, WAND_FUSS), r.s(wb, WAND_FUSS), r.s(rechts, kanteBoden), r.s(rechts, 1.02f), r.s(links, 1.02f), r.s(links, kanteBoden))
    drawPath(boden, Brush.verticalGradient(listOf(f.boden, f.bodenNah), startY = r.s(0f, WAND_FUSS).y, endY = r.s(0f, 1f).y))
    // Dielen: Fugen laufen alle in den Fluchtpunkt.
    val dehnung = (1.02f - FLUCHT) / (WAND_FUSS - FLUCHT)
    clipPath(boden) {
        var x = -wb
        while (x <= wb + 0.001f) {
            drawLine(Color.Black.copy(alpha = .10f), r.s(x, WAND_FUSS), r.s(x * dehnung, 1.02f), 0.004f * h)
            x += 0.10f
        }
        // Ein paar Stoßfugen, nach vorn weiter auseinander.
        listOf(0.70f, 0.76f, 0.84f, 0.95f).forEachIndexed { i, y ->
            val fd = (y - FLUCHT) / (WAND_FUSS - FLUCHT)
            var xs = -wb + (i % 2) * 0.05f
            while (xs < wb) {
                drawLine(Color.Black.copy(alpha = .07f), r.s(xs * fd, y), r.s((xs + 0.05f) * fd, y), 0.003f * h)
                xs += 0.30f
            }
        }
    }
    // Rückwand mit feiner Streifentapete.
    drawRect(Brush.verticalGradient(listOf(lerp(f.wand, f.holzDunkel, .05f), f.wand), startY = r.s(0f, WAND_KOPF).y, endY = r.s(0f, WAND_FUSS).y),
        r.s(-wb, WAND_KOPF), Size(2f * wb * h, (WAND_FUSS - WAND_KOPF) * h))
    run {
        var x = -wb + 0.06f
        while (x < wb) {
            drawLine(f.holzDunkel.copy(alpha = .05f), r.s(x, WAND_KOPF), r.s(x, WAND_FUSS), 0.012f * h)
            x += 0.09f
        }
    }
    // Raumkanten und Deckenleiste.
    val kante = f.holzDunkel.copy(alpha = .16f)
    listOf(-1f, 1f).forEach { s ->
        drawLine(kante, r.s(s * wb, WAND_KOPF), r.s(s * wb, WAND_FUSS), 0.004f * h)
        drawLine(kante, r.s(s * wb, WAND_KOPF), r.s(s * r.halb * 1.02f, kanteDecke), 0.004f * h)
    }
    drawRect(lerp(f.leiste, f.wand, .3f), r.s(-wb, WAND_KOPF), Size(2f * wb * h, 0.012f * h))
    // Fußleisten: an der Rückwand gerade, an den Seitenwänden in die Flucht gezogen.
    val leisteH = 0.024f
    drawRect(f.leiste, r.s(-wb, WAND_FUSS - leisteH), Size(2f * wb * h, leisteH * h))
    drawLine(f.holzDunkel.copy(alpha = .18f), r.s(-wb, WAND_FUSS), r.s(wb, WAND_FUSS), 0.004f * h)
    listOf(-1f, 1f).forEach { s ->
        val obenAussen = FLUCHT + (WAND_FUSS - leisteH - FLUCHT) * sf
        drawPath(poly(r.s(s * wb, WAND_FUSS - leisteH), r.s(s * r.halb * 1.02f, obenAussen), r.s(s * r.halb * 1.02f, kanteBoden), r.s(s * wb, WAND_FUSS)),
            lerp(f.leiste, f.seitenwand, .35f))
        drawLine(f.holzDunkel.copy(alpha = .18f), r.s(s * wb, WAND_FUSS), r.s(s * r.halb * 1.02f, kanteBoden), 0.004f * h)
    }

    // --- Fenster über dem Bett, mit Laibung, Himmel, Fensterbank, Gardinen und Blume. ---
    val oeffnung = Rect(r.s(-0.18f, 0.205f), r.s(0.22f, 0.47f))
    drawRect(Brush.verticalGradient(listOf(lerp(f.wand, f.holzDunkel, .22f), lerp(f.wand, Color.White, .2f)), startY = oeffnung.top, endY = oeffnung.bottom),
        oeffnung.topLeft, oeffnung.size)
    fun zurueck(p: Offset, faktor: Float) = r.v + (p - r.v) * faktor
    val glas = Rect(zurueck(oeffnung.topLeft, 0.93f), zurueck(oeffnung.bottomRight, 0.93f))
    val glasPfad = Path().apply { addRect(glas) }
    zeichneHimmel(glas, z, f, h)
    val rahmen = lerp(f.leiste, f.holzDunkel, .12f)
    drawRect(rahmen, glas.topLeft, glas.size, style = Stroke(0.012f * h))
    drawLine(rahmen, Offset(glas.center.x, glas.top), Offset(glas.center.x, glas.bottom), 0.009f * h)
    drawLine(rahmen, Offset(glas.left, glas.top + glas.height * .42f), Offset(glas.right, glas.top + glas.height * .42f), 0.008f * h)
    // Fensterbank: ragt ein Stück in den Raum.
    val bankL = r.s(-0.215f, 0.47f); val bankR = r.s(0.255f, 0.47f)
    val bankLV = zurueck(bankL, 1.07f); val bankRV = zurueck(bankR, 1.07f)
    drawPath(poly(bankL, bankR, bankRV, bankLV), lerp(f.leiste, Color.White, .3f))
    drawRect(lerp(f.leiste, f.holzDunkel, .15f), bankLV, Size(bankRV.x - bankLV.x, 0.014f * h))
    // Gardinen an einer Stange.
    drawLine(f.holzDunkel, r.s(-0.28f, 0.19f), r.s(0.32f, 0.19f), 0.008f * h, StrokeCap.Round)
    listOf(-0.28f, 0.32f).forEach { x -> drawCircle(f.holzDunkel, 0.011f * h, r.s(x, 0.19f)) }
    listOf(false, true).forEach { gespiegelt ->
        fun q(x: Float, y: Float) = if (gespiegelt) r.s(0.04f - x, y) else r.s(x, y)
        val vorhang = Path().apply {
            moveTo(q(-0.265f, 0.19f).x, q(-0.265f, 0.19f).y); lineTo(q(-0.17f, 0.19f).x, q(-0.17f, 0.19f).y)
            quadraticTo(q(-0.205f, 0.35f).x, q(-0.205f, 0.35f).y, q(-0.185f, 0.525f).x, q(-0.185f, 0.525f).y)
            lineTo(q(-0.265f, 0.525f).x, q(-0.265f, 0.525f).y); close()
        }
        drawPath(vorhang, Brush.horizontalGradient(listOf(f.vorhang, lerp(f.vorhang, Color.White, .22f), f.vorhang),
            startX = q(-0.265f, 0f).x, endX = q(-0.17f, 0f).x))
        listOf(-0.245f, -0.222f).forEach { x -> drawLine(lerp(f.vorhang, Color.Black, .18f), q(x, 0.20f), q(x + 0.004f, 0.515f), 0.003f * h) }
    }
    // Blume im Topf auf der Fensterbank.
    run {
        val fuss = Offset(r.s(0.17f, 0f).x, (bankL.y + bankLV.y) / 2f)
        val tb = 0.05f * h; val th = 0.045f * h
        drawPath(poly(fuss + Offset(-tb / 2f, 0f), fuss + Offset(tb / 2f, 0f), fuss + Offset(tb * .62f, -th), fuss + Offset(-tb * .62f, -th)),
            Brush.verticalGradient(listOf(Color(0xFFC9714E), Color(0xFFA4583B)), startY = fuss.y - th, endY = fuss.y))
        drawRect(Color(0xFFB8633F), fuss + Offset(-tb * .68f, -th - 0.008f * h), Size(tb * 1.36f, 0.01f * h))
        val wiegen = sin(z.t * 0.9f) * 0.004f * h
        listOf(Triple(-0.018f, 0.105f, 0f), Triple(0.004f, 0.135f, 1f), Triple(0.022f, 0.095f, 2f)).forEach { (dx, hoehe, i) ->
            val unten = fuss + Offset(dx * h * .3f, -th)
            val oben = fuss + Offset(dx * h + wiegen, -th - hoehe * h)
            drawLine(f.blatt, unten, oben, 0.005f * h, StrokeCap.Round)
            // Ein Blatt am Stiel.
            val blattMitte = unten + (oben - unten) * .45f
            drawOval(f.blatt, blattMitte - Offset(if (i == 1f) 0.024f * h else 0f, 0.006f * h), Size(0.024f * h, 0.011f * h))
            // Blüte als Tulpenkelch.
            val bl = 0.016f * h
            drawPath(Path().apply {
                moveTo(oben.x - bl, oben.y - bl * 1.1f)
                lineTo(oben.x - bl * .35f, oben.y - bl * .55f); lineTo(oben.x, oben.y - bl * 1.25f)
                lineTo(oben.x + bl * .35f, oben.y - bl * .55f); lineTo(oben.x + bl, oben.y - bl * 1.1f)
                quadraticTo(oben.x + bl * 1.05f, oben.y + bl * .45f, oben.x, oben.y + bl * .35f)
                quadraticTo(oben.x - bl * 1.05f, oben.y + bl * .45f, oben.x - bl, oben.y - bl * 1.1f); close()
            }, Brush.verticalGradient(listOf(lerp(f.bluete, Color.White, .25f), f.bluete), startY = oben.y - bl, endY = oben.y + bl * .4f))
        }
    }

    // --- Bild an der Wand zwischen Tür und Fenster: Kiefern am See. ---
    val tuerL = -wb + 0.07f; val tuerR = -wb + 0.29f
    if (tuerR < -0.68f) {
        val bild = Rect(r.s(-0.62f, 0.27f), r.s(-0.40f, 0.405f))
        drawRect(f.holzDunkel, bild.topLeft - Offset(0.012f * h, 0.012f * h), Size(bild.width + 0.024f * h, bild.height + 0.024f * h))
        drawRect(lerp(f.leiste, Color.White, .4f), bild.topLeft, bild.size)
        val innen = Rect(bild.left + 0.014f * h, bild.top + 0.014f * h, bild.right - 0.014f * h, bild.bottom - 0.014f * h)
        clipRect(innen.left, innen.top, innen.right, innen.bottom) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFBFD8E8), Color(0xFFF2DDB8)), startY = innen.top, endY = innen.bottom), innen.topLeft, innen.size)
            drawRect(Color(0xFF7FA3B8), Offset(innen.left, innen.top + innen.height * .70f), Size(innen.width, innen.height * .3f))
            listOf(0.15f to 0.55f, 0.28f to 0.68f, 0.40f to 0.5f, 0.78f to 0.6f, 0.9f to 0.72f).forEach { (fx, hoehe) ->
                val fussP = Offset(innen.left + fx * innen.width, innen.top + innen.height * .74f)
                val spitze = fussP - Offset(0f, hoehe * innen.height)
                drawPath(poly(spitze, fussP + Offset(innen.width * .06f, 0f), fussP - Offset(innen.width * .06f, 0f)), Color(0xFF3F5E48))
            }
        }
    }

    // --- Tür links in der Rückwand: Zarge, Öffnung, Blatt schwenkt an der linken Angel in den Raum. ---
    val tuerOben = 0.33f
    drawRect(lerp(f.leiste, f.holzDunkel, .2f), r.s(tuerL - 0.015f, tuerOben - 0.018f), Size((tuerR - tuerL + 0.03f) * h, (WAND_FUSS - tuerOben + 0.018f) * h))
    val flur = lerp(f.innen, Color(0xFFFFC07A), .22f * z.tuer)
    drawRect(Brush.verticalGradient(listOf(flur, f.innen), startY = r.s(0f, tuerOben).y, endY = r.s(0f, WAND_FUSS).y),
        r.s(tuerL, tuerOben), Size((tuerR - tuerL) * h, (WAND_FUSS - tuerOben) * h))
    run {
        val winkel = Math.toRadians((z.tuer * 80f).toDouble())
        val blattHoehe = VORN - FLUCHT - (tuerOben - FLUCHT) / KW
        val angelX = tuerL / KW
        val breite = (tuerR - tuerL) / KW
        val freiX = angelX + breite * cos(winkel).toFloat()
        val freiK = KW + 0.16f * sin(winkel).toFloat()
        val a0 = r.s(tuerL, tuerOben); val a1 = r.s(tuerL, WAND_FUSS)
        val b0 = r.r(freiX, blattHoehe, freiK); val b1 = r.r(freiX, 0f, freiK)
        val licht = 1f - z.tuer * .3f
        drawPath(poly(a0, b0, b1, a1), Brush.horizontalGradient(
            listOf(lerp(f.holzDunkel, f.holz, licht), lerp(f.holzDunkel, f.holz, licht * .85f)), startX = a0.x, endX = (a0.x + 1f).coerceAtLeast(b0.x)))
        if (b0.x - a0.x > 0.04f * h) {
            fun auf(u: Float, v: Float): Offset {
                val oben = a0 + (b0 - a0) * u; val unten = a1 + (b1 - a1) * u
                return oben + (unten - oben) * v
            }
            listOf(0.08f to 0.44f, 0.54f to 0.92f).forEach { (v0, v1) ->
                drawPath(poly(auf(.18f, v0), auf(.82f, v0), auf(.82f, v1), auf(.18f, v1)), f.holzDunkel.copy(alpha = .35f), style = Stroke(0.005f * h))
            }
            drawCircle(Color(0xFFD9B35E), 0.009f * h, auf(.86f, .55f))
        }
    }

    // --- Teppich vor dem Bett, flach auf dem Boden, mit Hausschuhen. ---
    run {
        val mitte = r.r(0.22f, 0f, 0.865f)
        val hw = 0.62f * 0.865f * h
        val hh = (r.r(0f, 0f, 0.96f).y - r.r(0f, 0f, 0.77f).y) / 2f
        drawOval(f.teppich, Offset(mitte.x - hw, mitte.y - hh), Size(2f * hw, 2f * hh))
        drawOval(lerp(f.teppich, Color.White, .32f), Offset(mitte.x - hw * .88f, mitte.y - hh * .72f), Size(1.76f * hw, 1.44f * hh), style = Stroke(0.005f * h))
        drawOval(lerp(f.teppich, f.holzDunkel, .15f), Offset(mitte.x - hw * .6f, mitte.y - hh * .4f), Size(1.2f * hw, 0.8f * hh), style = Stroke(0.004f * h))
    }

    // --- Kommode und Pflanze nur, wenn das Zimmer breit genug ist. ---
    if (r.halb > 1.45f) {
        val k1 = 0.58f
        val x0 = (-wb + 0.40f) / k1; val x1 = (-wb + 0.74f) / k1
        // Nur, wenn die Kommode links vom Bild bleibt.
        if (-wb + 0.74f < -0.66f) {
            bodenSchatten(r, x0, x1, KW, k1, f.schatten)
            quader(r, x0, x1, 0f, 0.24f, KW + 0.005f, k1,
                Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = r.r(0f, 0.24f, k1).y, endY = r.r(0f, 0f, k1).y),
                lerp(f.holz, Color.White, .12f), lerp(f.holz, f.holzDunkel, .5f))
            listOf(0.17f, 0.10f).forEach { y -> drawLine(f.holzDunkel.copy(alpha = .5f), r.r(x0 + 0.02f, y, k1), r.r(x1 - 0.02f, y, k1), 0.004f * h) }
            listOf(0.205f, 0.135f, 0.065f).forEach { y -> drawCircle(Color(0xFFD9B35E), 0.007f * h, r.r((x0 + x1) / 2f, y, k1)) }
            // Bücherstapel obenauf.
            listOf(Color(0xFF8A9DB5), Color(0xFFC08A6A), Color(0xFF9BB08A)).forEachIndexed { i, c ->
                quader(r, x0 + 0.08f, x0 + 0.30f - i * 0.02f, 0.24f + i * 0.025f, 0.265f + i * 0.025f, 0.53f, 0.565f, SolidColor(c), lerp(c, Color.White, .3f), lerp(c, Color.Black, .2f))
            }
        }
        val kp = 0.60f
        val px = (wb - 0.20f) / kp
        bodenSchatten(r, px - 0.07f, px + 0.07f, kp - 0.03f, kp + 0.02f, f.schatten)
        quader(r, px - 0.07f, px + 0.07f, 0f, 0.12f, kp - 0.04f, kp,
            Brush.verticalGradient(listOf(Color(0xFFC9714E), Color(0xFF9A5236)), startY = r.r(0f, 0.12f, kp).y, endY = r.r(0f, 0f, kp).y),
            Color(0xFF6B4A33), Color(0xFFA4583B))
        val stamm = r.r(px, 0.12f, kp - 0.02f)
        listOf(-50f, -25f, 0f, 22f, 48f, -10f, 35f).forEachIndexed { i, w ->
            val laenge = (0.20f + (i % 3) * 0.06f) * kp * h
            val d = richtung(180f + w + sin(z.t * 0.7f + i) * 2f)
            val spitze = stamm + d * laenge
            drawLine(lerp(f.blatt, Color.Black, .2f), stamm, spitze, 0.004f * h)
            val quer = Offset(-d.y, d.x) * (laenge * .22f)
            val m = stamm + d * (laenge * .6f)
            drawPath(Path().apply {
                moveTo(stamm.x + d.x * laenge * .2f, stamm.y + d.y * laenge * .2f)
                quadraticTo(m.x + quer.x, m.y + quer.y, spitze.x, spitze.y)
                quadraticTo(m.x - quer.x, m.y - quer.y, stamm.x + d.x * laenge * .2f, stamm.y + d.y * laenge * .2f); close()
            }, lerp(f.blatt, if (i % 2 == 0) Color.White else Color.Black, .12f))
        }
    }

    // --- Nachttisch rechts neben dem Kopfteil, mit Lampe und Wecker. ---
    val nx0 = 0.86f; val nx1 = 1.10f; val nk = 0.62f; val nh = 0.17f
    bodenSchatten(r, nx0, nx1, KW, nk, f.schatten)
    quader(r, nx0, nx1, 0f, nh, KW + 0.005f, nk,
        Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = r.r(0f, nh, nk).y, endY = r.r(0f, 0f, nk).y),
        lerp(f.holz, Color.White, .14f), lerp(f.holz, f.holzDunkel, .45f))
    drawLine(f.holzDunkel.copy(alpha = .55f), r.r(nx0 + 0.015f, 0.10f, nk), r.r(nx1 - 0.015f, 0.10f, nk), 0.004f * h)
    drawCircle(Color(0xFFD9B35E), 0.007f * h, r.r((nx0 + nx1) / 2f, 0.135f, nk))
    // Lampe: Fuß, Stiel, Schirm.
    val lk = 0.565f; val lx = 1.0f
    run {
        val fuss = r.r(lx, nh, lk)
        drawOval(lerp(f.holzDunkel, Color.Black, .2f), fuss - Offset(0.032f * h, 0.008f * h), Size(0.064f * h, 0.016f * h))
        drawLine(Color(0xFFB89A62), fuss, r.r(lx, nh + 0.11f, lk), 0.006f * h)
        val schirm = poly(r.r(lx - 0.075f, nh + 0.10f, lk), r.r(lx + 0.075f, nh + 0.10f, lk), r.r(lx + 0.045f, nh + 0.19f, lk), r.r(lx - 0.045f, nh + 0.19f, lk))
        drawPath(schirm, Brush.verticalGradient(listOf(lerp(f.schirm, f.holzDunkel, .1f), f.schirm), startY = r.r(0f, nh + 0.19f, lk).y, endY = r.r(0f, nh + 0.10f, lk).y))
    }
    // Wecker: zwei Glocken, rundes Zifferblatt; klingelt er, zittert er.
    val weckerMitte = r.r(0.92f, nh + 0.045f, 0.595f)
    val wr = 0.042f * 0.595f * h * 1.4f
    val zittern = if (z.klingeln > 0f) sin(z.t * 70f) * 9f * z.klingeln else 0f
    rotate(zittern, pivot = weckerMitte + Offset(0f, wr)) {
        val messing = Color(0xFFC9A04A)
        listOf(-1f, 1f).forEach { s ->
            drawLine(lerp(messing, Color.Black, .3f), weckerMitte + Offset(s * wr * .5f, wr * .7f), weckerMitte + Offset(s * wr * .8f, wr * 1.15f), 0.004f * h, StrokeCap.Round)
            drawArc(messing, 180f, 180f, useCenter = true, topLeft = weckerMitte + Offset(s * wr * .62f - wr * .42f, -wr * 1.28f),
                size = Size(wr * .84f, wr * .84f))
        }
        drawLine(lerp(messing, Color.Black, .3f), weckerMitte + Offset(-wr * .55f, -wr * 1.05f), weckerMitte + Offset(wr * .55f, -wr * 1.05f), 0.003f * h)
        drawCircle(Color(0xFFA8322B), wr, weckerMitte)
        drawCircle(lerp(f.leiste, Color.White, .5f), wr * .78f, weckerMitte)
        repeat(4) { i -> drawCircle(f.holzDunkel, wr * .06f, weckerMitte + richtung(i * 90f) * (wr * .62f)) }
        drawLine(f.holzDunkel, weckerMitte, weckerMitte + richtung(180f - (z.t * 6f + 180f)) * (wr * .4f), 0.004f * h, StrokeCap.Round)
        drawLine(f.holzDunkel, weckerMitte, weckerMitte + richtung(180f - z.t * 70f) * (wr * .62f), 0.003f * h, StrokeCap.Round)
    }
    if (z.klingeln > 0.05f) repeat(3) { i ->
        val rr = wr * (1.6f + i * .45f)
        drawArc(f.z.copy(alpha = .7f * z.klingeln * (1f - i * .25f)), -60f, 40f, false, weckerMitte - Offset(rr, rr), Size(rr * 2f, rr * 2f), style = Stroke(0.004f * h, cap = StrokeCap.Round))
        drawArc(f.z.copy(alpha = .7f * z.klingeln * (1f - i * .25f)), -160f, 40f, false, weckerMitte - Offset(rr, rr), Size(rr * 2f, rr * 2f), style = Stroke(0.004f * h, cap = StrokeCap.Round))
    }

    // --- Bett: längs an der Wand. Kopf- und Fußteil zuerst, dann Rahmen, Matratze, Kissen, Decke. ---
    val kHinten = KW + 0.005f
    bodenSchatten(r, BETT_FUSS, BETT_KOPF, KW, BETT_VORN, f.schatten)
    val holzFront = { oben: Float, k: Float -> Brush.verticalGradient(listOf(lerp(f.holz, Color.White, .06f), f.holzDunkel), startY = r.r(0f, oben, k).y, endY = r.r(0f, 0f, k).y) }
    quader(r, BETT_KOPF - 0.06f, BETT_KOPF, 0f, 0.42f, kHinten, BETT_VORN + 0.005f, holzFront(0.42f, BETT_VORN), lerp(f.holz, Color.White, .2f), lerp(f.holz, f.holzDunkel, .25f))
    // Füllungen im Kopfteil.
    listOf(0.24f to 0.38f, 0.08f to 0.21f).forEach { (u, o) ->
        drawPath(poly(r.r(BETT_KOPF - 0.06f, u, kHinten + 0.03f), r.r(BETT_KOPF - 0.06f, o, kHinten + 0.03f), r.r(BETT_KOPF - 0.06f, o, BETT_VORN - 0.03f), r.r(BETT_KOPF - 0.06f, u, BETT_VORN - 0.03f)),
            f.holzDunkel.copy(alpha = .35f), style = Stroke(0.004f * h))
    }
    quader(r, BETT_FUSS, BETT_FUSS + 0.04f, 0f, 0.27f, kHinten, BETT_VORN + 0.005f, holzFront(0.27f, BETT_VORN), lerp(f.holz, Color.White, .2f), lerp(f.holz, f.holzDunkel, .2f))
    // Rahmen auf vier Füßen; darunter liegt der dunkle Spalt zum Boden.
    val bx0 = BETT_FUSS + 0.04f; val bx1 = BETT_KOPF - 0.06f
    drawPath(poly(r.r(bx0, 0f, kHinten), r.r(bx1, 0f, kHinten), r.r(bx1, 0f, BETT_VORN), r.r(bx0, 0f, BETT_VORN)), f.schatten.copy(alpha = f.schatten.alpha * 1.4f))
    listOf(bx0 + 0.01f, bx1 - 0.05f).forEach { x ->
        quader(r, x, x + 0.04f, 0f, 0.07f, BETT_VORN - 0.04f, BETT_VORN, SolidColor(f.holzDunkel), f.holzDunkel, lerp(f.holzDunkel, Color.Black, .2f))
    }
    quader(r, bx0, bx1, 0.07f, 0.135f, kHinten, BETT_VORN, holzFront(0.135f, BETT_VORN), f.holz, f.holzDunkel)
    quader(r, bx0, bx1, 0.135f, MATRATZE, kHinten, BETT_VORN,
        Brush.verticalGradient(listOf(f.laken, lerp(f.laken, f.holzDunkel, .18f)), startY = r.r(0f, MATRATZE, BETT_VORN).y, endY = r.r(0f, 0.135f, BETT_VORN).y),
        lerp(f.laken, Color.White, .3f), f.laken)
    // Kissen am Kopfteil.
    run {
        val k = r.r(0.63f, MATRATZE + 0.03f, 0.605f)
        val kb = 0.17f * 0.605f * h; val kh = 0.075f * 0.605f * h
        drawOval(Brush.radialGradient(listOf(f.kissen, lerp(f.kissen, f.holzDunkel, .15f)), center = k - Offset(kb * .15f, kh * .3f), radius = kb * .7f),
            topLeft = k - Offset(kb / 2f, kh / 2f), size = Size(kb, kh))
    }

    // Decke: flach gemacht, mit Beinen darunter gewölbt, beim Zudecken bis zum Kinn hochgezogen.
    val atmen = if (z.schlaeft) sin(z.t * 2f * PI.toFloat() / 3.5f) * 0.004f else 0f
    val wulst = 0.03f * z.imBett + (0.03f + atmen) * z.zugedeckt
    val deckenKante = 0.10f + 0.15f * z.imBett + 0.23f * z.zugedeckt
    fun decke() {
        val x0 = bx0; val top = MATRATZE + 0.012f
        val kv = BETT_VORN + 0.006f; val kh = kHinten
        val km = (kv + kh) / 2f
        val oben = Path().apply {
            val a = r.r(x0, top, kh); moveTo(a.x, a.y)
            val b = r.r(deckenKante, top, kh); lineTo(b.x, b.y)
            val c = r.r(deckenKante, top, kv); lineTo(c.x, c.y)
            val d = r.r(x0, top, kv); lineTo(d.x, d.y); close()
        }
        drawPath(oben, lerp(f.decke, Color.White, .18f))
        // Die Wölbung über dem Körper als weicher Rücken entlang der Bettmitte.
        if (wulst > 0.001f) {
            val a = r.r(x0 + 0.02f, top, km); val e = r.r(deckenKante, top, km)
            val mitte = r.r((x0 + deckenKante) / 2f + 0.05f, top + wulst * 2f, km)
            val spitze = r.r(deckenKante - 0.02f, top + wulst * 1.6f, km)
            val ruecken = Path().apply {
                moveTo(a.x, a.y)
                quadraticTo(mitte.x, mitte.y, spitze.x, spitze.y)
                lineTo(e.x, e.y); close()
            }
            drawPath(ruecken, Brush.verticalGradient(listOf(lerp(f.decke, Color.White, .3f), lerp(f.decke, Color.White, .18f)), startY = mitte.y, endY = a.y))
        }
        // Steppnähte.
        clipPath(oben) {
            var x = x0 + 0.06f
            while (x < deckenKante) {
                drawLine(Color.White.copy(alpha = .22f), r.r(x, top, kh), r.r(x, top, kv), 0.003f * h)
                x += 0.09f
            }
        }
        val vorn = Path().apply {
            val a = r.r(x0, top, kv); moveTo(a.x, a.y)
            val b = r.r(deckenKante, top, kv); lineTo(b.x, b.y)
            val c = r.r(deckenKante, 0.105f, kv + 0.004f); lineTo(c.x, c.y)
            val m = r.r((x0 + deckenKante) / 2f, 0.095f, kv + 0.004f)
            val d = r.r(x0, 0.105f, kv + 0.004f)
            quadraticTo(m.x, m.y, d.x, d.y); close()
        }
        drawPath(vorn, Brush.verticalGradient(listOf(f.decke, lerp(f.decke, Color.Black, .18f)), startY = r.r(0f, top, kv).y, endY = r.r(0f, 0.1f, kv).y))
        drawLine(lerp(f.decke, Color.White, .45f), r.r(x0, 0.125f, kv + 0.004f), r.r(deckenKante, 0.125f, kv + 0.004f), 0.004f * h)
        // Umgeschlagener Lakenrand an der Deckenkante.
        drawPath(poly(r.r(deckenKante - 0.035f, top + 0.002f, kh), r.r(deckenKante, top + 0.002f, kh), r.r(deckenKante, top + 0.002f, kv), r.r(deckenKante - 0.035f, top + 0.002f, kv)),
            lerp(f.laken, Color.White, .4f))
        drawPath(poly(r.r(deckenKante - 0.035f, top, kv), r.r(deckenKante, top, kv), r.r(deckenKante, 0.105f, kv + 0.004f), r.r(deckenKante - 0.035f, 0.105f, kv + 0.004f)),
            lerp(f.laken, f.holzDunkel, .08f))
    }

    val figur = z.haltung
    if (figur != null && z.imBett >= 0.5f) person(figur, z, f, r)
    decke()

    // Die Katze schläft zusammengerollt am Fußende und hebt morgens kurz den Kopf.
    run {
        val katzeWach = z.sonne > 0.85f && !z.schlaeft
        val katzeAtmen = 1f + 0.04f * sin(z.t * 2f * PI.toFloat() / 2.6f)
        val km = r.r(-0.07f, MATRATZE + 0.04f + wulst * 0.3f, 0.61f)
        val kb = 0.042f * PS * 0.61f * h
        val katzeHell = f.katze; val katzeDunkel = lerp(f.katze, Color.Black, .3f)
        val schwanz = Path().apply {
            moveTo(km.x + kb * .8f, km.y + kb * .1f)
            quadraticTo(km.x + kb * 1.1f, km.y + kb * .55f + sin(z.t * 1.3f) * kb * .1f, km.x - kb * .2f, km.y + kb * .45f)
        }
        drawPath(schwanz, katzeDunkel, style = Stroke(0.28f * kb, cap = StrokeCap.Round))
        drawOval(Brush.radialGradient(listOf(lerp(katzeHell, Color.White, .2f), katzeHell, katzeDunkel), center = km - Offset(kb * .2f, kb * .3f), radius = kb * 1.2f),
            km - Offset(kb, kb * .45f * katzeAtmen), Size(kb * 2f, kb * .9f * katzeAtmen))
        repeat(3) { i -> drawLine(katzeDunkel.copy(alpha = .6f), km + Offset(kb * (-.1f + i * .3f), -kb * .42f), km + Offset(kb * (.0f + i * .3f), -kb * .1f), 0.07f * kb) }
        val kk = km + Offset(-kb * .95f, if (katzeWach) -kb * .5f else -kb * .2f)
        val kr = 0.4f * kb
        listOf(-1f, 1f).forEach { seite ->
            drawPath(Path().apply {
                moveTo(kk.x + seite * kr * .9f, kk.y - kr * .2f); lineTo(kk.x + seite * kr * .55f, kk.y - kr * 1.35f); lineTo(kk.x + seite * kr * .05f, kk.y - kr * .8f); close()
            }, katzeDunkel)
        }
        drawCircle(katzeHell, kr, kk)
        if (katzeWach) listOf(-1f, 1f).forEach { seite -> drawCircle(Color(0xFF3C5A2A), kr * .16f, kk + Offset(seite * kr * .4f, -kr * .05f)) }
        else listOf(-1f, 1f).forEach { seite -> drawLine(katzeDunkel, kk + Offset(seite * kr * .6f, 0f), kk + Offset(seite * kr * .2f, 0f), 0.06f * kr) }
    }

    // Hausschuhe auf dem Teppich vor dem Bett.
    listOf(0.06f to 0.80f, 0.14f to 0.81f).forEach { (x, k) ->
        val m = r.r(x, 0f, k)
        val sb = 0.06f * k * h; val sh = 0.022f * k * h
        drawOval(f.decke, m - Offset(sb / 2f, sh / 2f), Size(sb, sh))
        drawOval(lerp(f.decke, Color.White, .35f), m - Offset(sb * .05f, sh * .45f), Size(sb * .5f, sh * .6f))
    }

    if (figur != null && z.imBett < 0.5f) person(figur, z, f, r)

    // Morgenlicht fällt schräg durchs Fenster in den Raum.
    if (z.sonne > 0.01f) {
        drawPath(poly(Offset(glas.left, glas.bottom), Offset(glas.right, glas.bottom), r.r(0.62f, 0f, 0.82f), r.r(0.0f, 0f, 0.82f)),
            Brush.verticalGradient(listOf(f.sonne.copy(alpha = .17f * z.sonne), Color.Transparent), startY = glas.bottom, endY = r.r(0f, 0f, 0.82f).y))
    }

    // Nacht: der Raum wird blauer und dunkler, das Fenster bleibt so hell, wie der Himmel ist.
    val nacht = 1f - z.sonne
    if (nacht > 0.01f) clipPath(glasPfad, ClipOp.Difference) {
        drawRect(f.nacht.copy(alpha = f.nachtStaerke * nacht), Offset(0f, r.oy), Size(size.width, h))
    }
    // Lampenlicht über der Nacht: warmer Schein an Wand und Nachttisch, der Schirm leuchtet.
    if (z.lampe > 0.01f) {
        val mitte = r.r(lx, nh + 0.15f, lk)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC77A).copy(alpha = .38f * z.lampe), Color.Transparent), center = mitte, radius = 0.55f * h), 0.55f * h, mitte)
        drawPath(poly(r.r(lx - 0.075f, nh + 0.10f, lk), r.r(lx + 0.075f, nh + 0.10f, lk), r.r(lx + 0.045f, nh + 0.19f, lk), r.r(lx - 0.045f, nh + 0.19f, lk)),
            Color(0xFFFFE2A8).copy(alpha = .85f * z.lampe))
    }

    // Z steigen vom Kopf auf, solange die Person schläft.
    if (z.schlaeft && figur != null && z.schlafZeit > 0.4f) {
        val m = PS * figur.k * h
        val kopf = r.r(figur.x, figur.hoch, figur.k) + richtung(figur.torso) * ((TORSO + HALS) * m)
        repeat(3) { i ->
            val lauf = z.schlafZeit - 0.4f - i * 0.8f
            if (lauf < 0f) return@repeat
            val q = (lauf % 2.4f) / 2.4f
            val a = sin(q * PI.toFloat()) * 0.9f
            val g = (0.03f + q * 0.03f) * h
            val p = kopf + Offset(-(0.02f + q * 0.10f) * h, -(0.08f + q * 0.24f) * h)
            val zPfad = Path().apply {
                moveTo(p.x - g / 2, p.y - g / 2); lineTo(p.x + g / 2, p.y - g / 2)
                lineTo(p.x - g / 2, p.y + g / 2); lineTo(p.x + g / 2, p.y + g / 2)
            }
            drawPath(zPfad, f.z.copy(alpha = a), style = Stroke(0.012f * h, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Himmel im Fensterglas: Nacht mit Mond und Sternen, dann geht die Sonne hinter Kiefernhügeln auf. */
private fun DrawScope.zeichneHimmel(glas: Rect, z: Zustand, f: SzenenFarben, h: Float) {
    fun g(fx: Float, fy: Float) = Offset(glas.left + fx * glas.width, glas.top + fy * glas.height)
    drawRect(Brush.verticalGradient(listOf(lerp(Color(0xFF1B2440), Color(0xFF8EC2EE), z.sonne), lerp(Color(0xFF3B4870), Color(0xFFFFD49A), z.sonne)),
        startY = glas.top, endY = glas.bottom), glas.topLeft, glas.size)
    clipRect(glas.left, glas.top, glas.right, glas.bottom) {
        val nacht = 1f - z.sonne
        if (nacht > 0.01f) {
            listOf(0.12f to 0.18f, 0.55f to 0.12f, 0.82f to 0.30f, 0.70f to 0.52f, 0.38f to 0.40f, 0.92f to 0.10f, 0.22f to 0.55f).forEachIndexed { i, (x, y) ->
                val funkeln = 0.5f + 0.5f * sin(z.t * 2.2f + i * 1.7f)
                drawCircle(f.mond.copy(alpha = nacht * funkeln), (0.005f + (i % 2) * 0.003f) * h, g(x, y))
            }
            val m = g(0.28f, 0.28f)
            val mr = 0.035f * h
            val sichel = Path.combine(PathOperation.Difference,
                Path().apply { addOval(Rect(m, mr)) },
                Path().apply { addOval(Rect(m + Offset(mr * .45f, -mr * .25f), mr * .86f)) })
            drawCircle(Brush.radialGradient(listOf(f.mond.copy(alpha = .30f * nacht), Color.Transparent), center = m, radius = mr * 2.4f), mr * 2.4f, m)
            drawPath(sichel, f.mond.copy(alpha = nacht))
        }
        if (z.sonne > 0.01f) {
            val c = g(0.64f, 1.05f - 0.62f * z.sonne)
            val sr = 0.05f * h
            drawCircle(Brush.radialGradient(listOf(f.sonne.copy(alpha = .45f * z.sonne), Color.Transparent), center = c, radius = sr * 3f), sr * 3f, c)
            repeat(10) { i ->
                val d = richtung(i * 36f + z.t * 12f)
                drawLine(f.sonne.copy(alpha = .75f * z.sonne), c + d * (sr * 1.3f), c + d * (sr * 1.75f), 0.009f * h, StrokeCap.Round)
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE7A8).copy(alpha = z.sonne), f.sonne.copy(alpha = z.sonne)),
                center = c - Offset(sr * .3f, sr * .3f), radius = sr * 1.3f), sr, c)
        }
        // Hügel mit Kiefern.
        val huegelFarbe = lerp(Color(0xFF151C2A), Color(0xFF6E9670), z.sonne)
        drawPath(Path().apply {
            moveTo(glas.left, glas.bottom); lineTo(g(0f, 0.80f).x, g(0f, 0.80f).y)
            quadraticTo(g(0.25f, 0.66f).x, g(0.25f, 0.66f).y, g(0.52f, 0.80f).x, g(0.52f, 0.80f).y)
            quadraticTo(g(0.78f, 0.70f).x, g(0.78f, 0.70f).y, g(1f, 0.76f).x, g(1f, 0.76f).y)
            lineTo(glas.right, glas.bottom); close()
        }, huegelFarbe)
        val baum = lerp(huegelFarbe, Color.Black, .28f)
        listOf(Triple(0.10f, 0.76f, 0.16f), Triple(0.18f, 0.72f, 0.20f), Triple(0.27f, 0.70f, 0.15f), Triple(0.80f, 0.73f, 0.18f), Triple(0.89f, 0.74f, 0.14f)).forEach { (x, y, hoehe) ->
            val fuss = g(x, y); val spitze = g(x, y - hoehe)
            drawLine(baum, fuss, spitze, 0.004f * h)
            listOf(0.15f, 0.45f, 0.75f).forEach { s ->
                val p = fuss + (spitze - fuss) * s
                val b = (1.1f - s) * 0.03f * h
                drawPath(poly(p + Offset(0f, -0.04f * h * (1f - s * .4f)), p + Offset(b, 0f), p + Offset(-b, 0f)), baum)
            }
        }
    }
}

/** Die Person: Beine, Oberkörper im gestreiften Schlafanzug, Arme, Kopf mit Haar — in der Tiefe k verkleinert. */
private fun DrawScope.person(h: Haltung, z: Zustand, f: SzenenFarben, r: Raum) {
    val a = z.alpha
    if (a <= 0.01f) return
    val m = PS * h.k * r.h
    val basis = r.r(h.x, h.hoch, h.k)
    fun q(o: Offset) = basis + o * m
    val u = richtung(h.torso)
    val hufte = Offset.Zero
    val schulter = u * (TORSO * 0.9f)
    val kopf = u * (TORSO + HALS)
    val rund = StrokeCap.Round
    fun linie(von: Offset, bis: Offset, farbe: Color, breite: Float) = drawLine(farbe.copy(alpha = farbe.alpha * a), q(von), q(bis), breite * m, rund)

    // Schatten auf dem Boden, solange die Person steht oder geht.
    if (z.imBett < 0.5f) {
        val boden = r.r(h.x, 0f, h.k)
        drawOval(Brush.radialGradient(listOf(f.schatten.copy(alpha = f.schatten.alpha * a), Color.Transparent), center = boden, radius = 0.09f * m * 1.4f),
            topLeft = boden - Offset(0.11f * m, 0.025f * m), size = Size(0.22f * m, 0.05f * m))
    }

    // Arme: beim Gehen gegenläufig, beim Strecken weit nach oben ausgebreitet.
    val armHinten = h.arm + z.schwung + (160f - h.arm - z.schwung) * z.streckt
    val armVorn = h.arm - z.schwung + (-160f - h.arm + z.schwung) * z.streckt
    val beinHinten = h.bein - z.schwung
    val beinVorn = h.bein + z.schwung
    val dunkler = lerp(f.anzug, Color.Black, .15f)

    val fussH = hufte + richtung(beinHinten) * BEIN
    linie(hufte, fussH, dunkler, 0.046f)
    linie(fussH, fussH + Offset(0.012f * z.blick, 0f), f.haut, 0.02f)
    val handH = schulter + richtung(armHinten) * ARM
    linie(schulter, handH, dunkler, 0.036f)
    drawCircle(f.haut.copy(alpha = a), 0.013f * m, q(handH))

    linie(hufte, schulter, f.anzug, 0.078f)
    val quer = Offset(-u.y, u.x) * 0.018f
    linie(hufte + quer, schulter + quer, f.streifen, 0.006f)
    linie(hufte - quer, schulter - quer, f.streifen, 0.006f)

    val fussV = hufte + richtung(beinVorn) * BEIN
    linie(hufte, fussV, f.anzug, 0.048f)
    linie(fussV, fussV + Offset(0.012f * z.blick, 0f), f.haut, 0.02f)

    val c = q(kopf)
    val kr = KOPF * m
    drawCircle(f.haut.copy(alpha = a), kr, c)
    val gesicht = Offset(-u.y, u.x) * z.blick
    val haarRichtung = u * 0.8f - gesicht * 0.6f
    val haarWinkel = Math.toDegrees(atan2(haarRichtung.y.toDouble(), haarRichtung.x.toDouble())).toFloat()
    drawArc(f.haar.copy(alpha = a), haarWinkel - 105f, 210f, useCenter = true, topLeft = c - Offset(kr * 1.05f, kr * 1.05f),
        size = Size(kr * 2.1f, kr * 2.1f))
    val auge = c + gesicht * (kr * .5f) + u * (kr * .05f)
    if (z.schlaeft) drawLine(f.haar.copy(alpha = a), auge - gesicht * (kr * .18f), auge + gesicht * (kr * .18f), kr * .14f, rund)
    else drawCircle(f.haar.copy(alpha = a), kr * .13f, auge)

    val handV = schulter + richtung(armVorn) * ARM
    linie(schulter, handV, f.anzug, 0.038f)
    drawCircle(f.haut.copy(alpha = a), 0.014f * m, q(handV))
}
