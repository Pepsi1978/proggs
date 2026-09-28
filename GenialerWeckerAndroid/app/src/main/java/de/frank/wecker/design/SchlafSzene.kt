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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.wecker.rememberResumed
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Morgenruhes Motiv als kleine Endlosszene: Links geht eine Tür auf, eine Person im gestreiften
 * Schlafanzug kommt heraus, geht zum Bett, legt sich hin und schläft (Z steigen auf). Dann geht die
 * Sonne auf, die Person wacht auf, setzt sich auf, streckt die Arme aus, steht auf und geht wieder
 * durch die Tür. Danach 5 Sekunden Ruhe, dann beginnt alles von vorn.
 *
 * Alles wird gezeichnet (kein Bild), das Bett in Schrägansicht mit Front, Oberseite und Kopfteil.
 * Die Zeit wird nur im Zeichenblock gelesen: Neu gezeichnet wird allein diese Ebene, nie die App.
 * Läuft nur, solange der Bildschirm vorne ist; bei reduzierter Bewegung steht ein ruhiges Standbild.
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
        zeichneSzene(zustand(t), farben)
    }
}

private const val ZYKLUS = 26.6f
private const val STANDBILD = 8f

// Maße in Anteilen der kürzeren Kantenlänge.
private const val BODEN = 0.86f
private const val TORSO = 0.12f
private const val HALS = 0.047f
private const val KOPF = 0.034f
private const val ARM = 0.10f
private const val BEIN = 0.15f
private const val TIEFE_X = 0.05f
private const val TIEFE_Y = 0.05f
private const val MATRATZE = 0.66f

private class SzenenFarben(
    val holz: Color, val holzDunkel: Color, val laken: Color, val decke: Color, val kissen: Color,
    val anzug: Color, val streifen: Color, val haut: Color, val haar: Color, val innen: Color,
    val sonne: Color, val mond: Color, val z: Color, val schatten: Color,
) {
    companion object {
        fun fuer(dunkel: Boolean, gedaempft: Color) = if (dunkel) SzenenFarben(
            holz = Color(0xFF8C6C4D), holzDunkel = Color(0xFF5E4631), laken = Color(0xFFD3CBBC),
            decke = Color(0xFF6F86A8), kissen = Color(0xFFE2DBCF), anzug = Color(0xFFA9BEDB),
            streifen = Color(0xFF5F7CA6), haut = Color(0xFFE2BC98), haar = Color(0xFF4E3B2E),
            innen = Color(0xFF15120F), sonne = Color(0xFFFFC65C), mond = Color(0xFFF3E7C4),
            z = gedaempft, schatten = Color.Black.copy(alpha = .45f),
        ) else SzenenFarben(
            holz = Color(0xFFB08A62), holzDunkel = Color(0xFF7A5A3D), laken = Color(0xFFF3EEE5),
            decke = Color(0xFF8098BA), kissen = Color(0xFFFFFCF6), anzug = Color(0xFFB9CDE6),
            streifen = Color(0xFF6F8DB8), haut = Color(0xFFEBC6A3), haar = Color(0xFF5B4535),
            innen = Color(0xFF2B2622), sonne = Color(0xFFFFB94A), mond = Color(0xFFE9D9A8),
            z = gedaempft, schatten = Color.Black.copy(alpha = .20f),
        )
    }
}

/** Körperhaltung: Hüfte und Winkel (0° = nach unten, 90° = nach rechts, 180° = nach oben). */
private data class Haltung(val x: Float, val y: Float, val torso: Float, val bein: Float, val arm: Float)

private fun stehend(x: Float) = Haltung(x, BODEN - BEIN, 180f, 0f, 0f)
private val SITZEND = Haltung(0.68f, 0.645f, 180f, -90f, -35f)
private val LIEGEND = Haltung(0.68f, 0.64f, 90f, -90f, -90f)

private fun mische(a: Haltung, b: Haltung, f: Float) = Haltung(
    a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f, a.torso + (b.torso - a.torso) * f,
    a.bein + (b.bein - a.bein) * f, a.arm + (b.arm - a.arm) * f,
)

private class Zustand(
    val haltung: Haltung?, val schwung: Float, val blick: Float, val alpha: Float, val streckt: Float,
    val schlaeft: Boolean, val schlafZeit: Float, val tuer: Float, val sonne: Float,
    /** 1 = die Person liegt oder sitzt im Bett (Beine unter der Decke). */
    val imBett: Float, /** 1 = Decke bis zum Kinn hochgezogen. */ val zugedeckt: Float, val t: Float,
)

private fun ab(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun weich(x: Float) = x * x * (3f - 2f * x)
/** 0 → 1 zwischen a und b, hält, 1 → 0 zwischen c und d; außerhalb 0. */
private fun huegel(t: Float, a: Float, b: Float, c: Float, d: Float) =
    if (t < a || t > d) 0f else if (t < c) weich(ab(t, a, b)) else 1f - weich(ab(t, c, d))

private fun zustand(t: Float): Zustand {
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
    val schritt = { s: Float -> (sin(s * 2f * PI.toFloat() * 1.6f) * 26f) }
    var alpha = 1f
    var schwung = 0f
    var blick = 1f
    var streckt = 0f
    val haltung: Haltung? = when {
        t < 0.6f || t > 20.8f -> null
        t < 1.2f -> { alpha = ab(t, 0.6f, 1.2f); stehend(0.15f) }
        t < 3.6f -> { schwung = schritt(t - 1.2f); stehend(0.15f + 0.37f * ab(t, 1.2f, 3.6f)) }
        t < 4.8f -> { val f = weich(ab(t, 3.6f, 4.8f)); blick = if (f < .5f) 1f else -1f; mische(stehend(0.52f), SITZEND, f) }
        t < 5.8f -> { blick = -1f; mische(SITZEND, LIEGEND, weich(ab(t, 4.8f, 5.8f))) }
        t < 14f -> { blick = -1f; LIEGEND }
        t < 15f -> { blick = -1f; mische(LIEGEND, SITZEND, weich(ab(t, 14f, 15f))) }
        t < 16.6f -> { blick = -1f; streckt = huegel(t, 15f, 15.6f, 16f, 16.6f); SITZEND }
        t < 17.8f -> { blick = -1f; mische(SITZEND, stehend(0.52f), weich(ab(t, 16.6f, 17.8f))) }
        t < 20.2f -> { blick = -1f; schwung = schritt(t - 17.8f); stehend(0.52f - 0.37f * ab(t, 17.8f, 20.2f)) }
        else -> { blick = -1f; alpha = 1f - ab(t, 20.2f, 20.8f); stehend(0.15f) }
    }
    val schlaeft = t in 5.5f..14f
    return Zustand(haltung, schwung, blick, alpha, streckt, schlaeft, t - 5.8f, tuer, sonne, imBett, zugedeckt, t)
}

private fun richtung(winkel: Float): Offset {
    val r = Math.toRadians(winkel.toDouble())
    return Offset(sin(r).toFloat(), cos(r).toFloat())
}

private fun DrawScope.zeichneSzene(z: Zustand, f: SzenenFarben) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun poly(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }

    // Bodenschatten unter Tür und Bett.
    drawOval(Brush.radialGradient(listOf(f.schatten, Color.Transparent), center = p(0.66f, 0.865f), radius = 0.34f * s),
        topLeft = p(0.32f, 0.84f), size = Size(0.68f * s, 0.05f * s))
    drawOval(Brush.radialGradient(listOf(f.schatten, Color.Transparent), center = p(0.15f, 0.865f), radius = 0.14f * s),
        topLeft = p(0.02f, 0.85f), size = Size(0.26f * s, 0.03f * s))

    // Nachthimmel: Mond und ein paar Sterne, die mit dem Sonnenaufgang verblassen.
    val nacht = 1f - z.sonne
    if (nacht > 0.01f) {
        val m = p(0.58f, 0.20f)
        val mr = 0.045f * s
        val sichel = Path.combine(PathOperation.Difference,
            Path().apply { addOval(Rect(m, mr)) },
            Path().apply { addOval(Rect(m + Offset(mr * .45f, -mr * .25f), mr * .86f)) })
        drawCircle(Brush.radialGradient(listOf(f.mond.copy(alpha = .30f * nacht), Color.Transparent), center = m, radius = mr * 2.4f), mr * 2.4f, m)
        drawPath(sichel, f.mond.copy(alpha = nacht))
        listOf(0.44f to 0.14f, 0.70f to 0.10f, 0.78f to 0.26f, 0.36f to 0.28f, 0.66f to 0.33f).forEachIndexed { i, (x, y) ->
            val funkeln = 0.55f + 0.45f * sin(z.t * 2.2f + i * 1.7f)
            drawCircle(f.mond.copy(alpha = nacht * funkeln), (0.006f + (i % 2) * 0.003f) * s, p(x, y))
        }
    }

    // Sonne: geht hinter dem Bett auf, mit weichem Morgenlicht und langsam drehenden Strahlen.
    if (z.sonne > 0.01f) {
        val c = p(0.66f, 0.74f - 0.50f * z.sonne)
        val r = 0.065f * s
        drawCircle(Brush.radialGradient(listOf(f.sonne.copy(alpha = .40f * z.sonne), Color.Transparent), center = c, radius = r * 3f), r * 3f, c)
        repeat(10) { i ->
            val w = i * 36f + z.t * 12f
            val d = richtung(w)
            drawLine(f.sonne.copy(alpha = .75f * z.sonne), c + d * (r * 1.3f), c + d * (r * 1.75f), 0.012f * s, StrokeCap.Round)
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE7A8).copy(alpha = z.sonne), f.sonne.copy(alpha = z.sonne)),
            center = c - Offset(r * .3f, r * .3f), radius = r * 1.3f), r, c)
    }

    // Tür: Zarge, dunkle Öffnung, Türblatt schwenkt an der linken Angel nach vorn auf.
    drawRect(f.holzDunkel, p(0.04f, 0.30f), Size(0.22f * s, 0.56f * s))
    drawRect(Brush.verticalGradient(listOf(f.innen, f.innen.copy(alpha = .85f)), startY = p(0f, 0.32f).y, endY = p(0f, BODEN).y),
        p(0.06f, 0.32f), Size(0.18f * s, (BODEN - 0.32f) * s))
    val winkel = z.tuer * 78f
    val blattBreite = 0.18f * cos(Math.toRadians(winkel.toDouble())).toFloat()
    val vor = 0.03f * sin(Math.toRadians(winkel.toDouble())).toFloat()
    val a0 = p(0.06f, 0.32f); val a1 = p(0.06f, BODEN)
    val b0 = p(0.06f + blattBreite, 0.32f - vor); val b1 = p(0.06f + blattBreite, BODEN + vor * .4f)
    val licht = 1f - z.tuer * .35f
    drawPath(poly(a0, b0, b1, a1), Brush.horizontalGradient(
        listOf(lerp(f.holzDunkel, f.holz, licht), lerp(f.holzDunkel, f.holz, licht * .85f)), startX = a0.x, endX = b0.x.coerceAtLeast(a0.x + 1f)))
    if (blattBreite > 0.03f) {
        // Zwei Füllungen und die Klinke, perspektivisch mit dem Blatt gestaucht.
        fun auf(u: Float, v: Float): Offset {
            val oben = a0 + (b0 - a0) * u; val unten = a1 + (b1 - a1) * u
            return oben + (unten - oben) * v
        }
        listOf(0.08f to 0.44f, 0.54f to 0.92f).forEach { (v0, v1) ->
            drawPath(poly(auf(.18f, v0), auf(.82f, v0), auf(.82f, v1), auf(.18f, v1)), f.holzDunkel.copy(alpha = .35f),
                style = Stroke(0.006f * s))
        }
        drawCircle(Color(0xFFD9B35E), 0.011f * s, auf(.86f, .55f))
    }
    drawRect(f.holzDunkel.copy(alpha = .6f), p(0.04f, 0.30f), Size(0.22f * s, 0.56f * s), style = Stroke(0.006f * s))

    // Bett: hintere Beine, Fußteil, Rahmen, Matratze, Kissen.
    val hx = TIEFE_X; val hy = TIEFE_Y
    listOf(0.45f, 0.95f).forEach { x -> drawRect(f.holzDunkel, p(x, 0.71f), Size(0.02f * s, 0.10f * s)) }
    drawPath(poly(p(0.375f, 0.60f), p(0.40f, 0.60f), p(0.40f + hx, 0.60f - hy), p(0.375f + hx, 0.60f - hy)), f.holz)
    drawRect(Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = p(0f, 0.60f).y, endY = p(0f, BODEN).y),
        p(0.375f, 0.60f), Size(0.025f * s, (BODEN - 0.60f) * s))
    drawRect(Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = p(0f, 0.705f).y, endY = p(0f, 0.76f).y),
        p(0.40f, 0.705f), Size(0.505f * s, 0.055f * s))
    drawRect(Brush.verticalGradient(listOf(f.laken, lerp(f.laken, f.holzDunkel, .18f)), startY = p(0f, MATRATZE).y, endY = p(0f, 0.705f).y),
        p(0.40f, MATRATZE), Size(0.505f * s, 0.045f * s))
    drawPath(poly(p(0.40f, MATRATZE), p(0.905f, MATRATZE), p(0.905f + hx, MATRATZE - hy), p(0.40f + hx, MATRATZE - hy)),
        Brush.verticalGradient(listOf(lerp(f.laken, Color.White, .3f), f.laken), startY = p(0f, MATRATZE - hy).y, endY = p(0f, MATRATZE).y))
    val kissen = p(0.865f, 0.628f)
    drawOval(Brush.radialGradient(listOf(f.kissen, lerp(f.kissen, f.holzDunkel, .15f)), center = kissen - Offset(0.01f * s, 0.01f * s),
        radius = 0.07f * s), topLeft = kissen - Offset(0.055f * s, 0.024f * s), size = Size(0.11f * s, 0.048f * s))

    // Decke: flach gemacht, mit Beinen darunter gewölbt, beim Zudecken bis zum Kinn hochgezogen.
    val atmen = if (z.schlaeft) sin(z.t * 2f * PI.toFloat() / 3.5f) * 0.003f else 0f
    val wulst = 0.022f * z.imBett + atmen * z.zugedeckt
    val kante = 0.62f + 0.08f * z.imBett + 0.10f * z.zugedeckt
    fun decke() {
        val x0 = 0.395f; val yf = MATRATZE; val yb = 0.708f
        val mitte = (0.55f + kante) / 2f
        val oben = Path().apply {
            moveTo(p(x0, yb).x, p(x0, yb).y)
            lineTo(p(x0, yf).x, p(x0, yf).y)
            lineTo(p(x0 + hx, yf - hy).x, p(x0 + hx, yf - hy).y)
            quadraticTo(p(mitte + hx, yf - hy - wulst * 1.3f).x, p(mitte + hx, yf - hy - wulst * 1.3f).y,
                p(kante + hx, yf - hy - wulst * .5f).x, p(kante + hx, yf - hy - wulst * .5f).y)
            lineTo(p(kante, yf - wulst * .5f).x, p(kante, yf - wulst * .5f).y)
            lineTo(p(kante, yb).x, p(kante, yb).y)
            close()
        }
        drawPath(oben, lerp(f.decke, Color.White, .18f))
        val vorn = Path().apply {
            moveTo(p(x0, yf).x, p(x0, yf).y)
            quadraticTo(p(mitte, yf - wulst).x, p(mitte, yf - wulst).y, p(kante, yf - wulst * .5f).x, p(kante, yf - wulst * .5f).y)
            lineTo(p(kante, yb).x, p(kante, yb).y)
            // Leicht gewellter Saum.
            quadraticTo(p((x0 + kante) / 2f, yb + 0.012f).x, p((x0 + kante) / 2f, yb + 0.012f).y, p(x0, yb).x, p(x0, yb).y)
            close()
        }
        drawPath(vorn, Brush.verticalGradient(listOf(f.decke, lerp(f.decke, Color.Black, .18f)), startY = p(0f, yf - wulst).y, endY = p(0f, yb).y))
        // Umgeschlagener Lakenrand an der Deckenkante.
        drawPath(poly(p(kante - 0.022f, yf - wulst * .5f), p(kante, yf - wulst * .5f), p(kante + hx, yf - hy - wulst * .5f),
            p(kante - 0.022f + hx, yf - hy - wulst * .5f)), lerp(f.laken, Color.White, .4f))
    }

    val figur = z.haltung
    if (z.imBett < 0.5f) decke()
    if (figur != null && z.imBett >= 0.5f) person(figur, z, f, ::p)
    if (z.imBett >= 0.5f) decke()

    // Kopfteil (rechts) und vordere Beine liegen vor Matratze und Kopf.
    drawPath(poly(p(0.905f, 0.48f), p(0.93f, 0.48f), p(0.93f + hx, 0.48f - hy), p(0.905f + hx, 0.48f - hy)), lerp(f.holz, Color.White, .15f))
    drawPath(poly(p(0.93f, 0.48f), p(0.93f + hx, 0.48f - hy), p(0.93f + hx, 0.81f), p(0.93f, BODEN)),
        Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = p(0f, 0.43f).y, endY = p(0f, BODEN).y))
    drawRect(Brush.verticalGradient(listOf(lerp(f.holz, Color.White, .08f), f.holzDunkel), startY = p(0f, 0.48f).y, endY = p(0f, BODEN).y),
        p(0.905f, 0.48f), Size(0.025f * s, (BODEN - 0.48f) * s))
    listOf(0.405f, 0.88f).forEach { x ->
        drawRect(Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = p(0f, 0.76f).y, endY = p(0f, BODEN).y),
            p(x, 0.76f), Size(0.02f * s, (BODEN - 0.76f) * s))
    }

    if (figur != null && z.imBett < 0.5f) person(figur, z, f, ::p)

    // Z steigen vom Kopf auf, solange die Person schläft.
    if (z.schlaeft && figur != null && z.schlafZeit > 0.4f) {
        val kopf = kopfMitte(figur)
        repeat(3) { i ->
            val lauf = z.schlafZeit - 0.4f - i * 0.8f
            if (lauf < 0f) return@repeat
            val q = (lauf % 2.4f) / 2.4f
            val a = sin(q * PI.toFloat()) * 0.9f
            val g = (0.016f + q * 0.018f) * s
            val m = p(kopf.x - 0.02f - q * 0.06f, kopf.y - 0.06f - q * 0.16f)
            val zPfad = Path().apply {
                moveTo(m.x - g / 2, m.y - g / 2); lineTo(m.x + g / 2, m.y - g / 2)
                lineTo(m.x - g / 2, m.y + g / 2); lineTo(m.x + g / 2, m.y + g / 2)
            }
            drawPath(zPfad, f.z.copy(alpha = a), style = Stroke(0.009f * s, cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

private fun kopfMitte(h: Haltung): Offset {
    val u = richtung(h.torso)
    return Offset(h.x, h.y) + u * (TORSO + HALS)
}

/** Die Person: Beine, Oberkörper im gestreiften Schlafanzug, Arme, Kopf mit Haar. [p] rechnet Anteile in Pixel. */
private fun DrawScope.person(h: Haltung, z: Zustand, f: SzenenFarben, p: (Float, Float) -> Offset) {
    val s = size.minDimension
    val a = z.alpha
    if (a <= 0.01f) return
    val u = richtung(h.torso)
    val hufte = Offset(h.x, h.y)
    val schulter = hufte + u * (TORSO * 0.9f)
    val kopf = hufte + u * (TORSO + HALS)
    val rund = StrokeCap.Round
    fun linie(von: Offset, bis: Offset, farbe: Color, breite: Float) = drawLine(farbe.copy(alpha = farbe.alpha * a), p(von.x, von.y), p(bis.x, bis.y), breite * s, rund)

    // Arme: beim Gehen gegenläufig, beim Strecken weit nach oben ausgebreitet.
    val armHinten = h.arm + z.schwung + (160f - h.arm - z.schwung) * z.streckt
    val armVorn = h.arm - z.schwung + (-160f - h.arm + z.schwung) * z.streckt
    val beinHinten = h.bein - z.schwung
    val beinVorn = h.bein + z.schwung
    val dunkler = lerp(f.anzug, Color.Black, .15f)

    // Hinteres Bein und hinterer Arm zuerst, etwas dunkler.
    val fussH = hufte + richtung(beinHinten) * BEIN
    linie(hufte, fussH, dunkler, 0.046f)
    linie(fussH, fussH + Offset(0.012f * z.blick, 0f), f.haut, 0.02f)
    val handH = schulter + richtung(armHinten) * ARM
    linie(schulter, handH, dunkler, 0.036f)
    drawCircle(f.haut.copy(alpha = a), 0.013f * s, p(handH.x, handH.y))

    // Oberkörper mit zwei Längsstreifen.
    linie(hufte, schulter, f.anzug, 0.078f)
    val quer = Offset(-u.y, u.x) * 0.018f
    linie(hufte + quer, schulter + quer, f.streifen, 0.006f)
    linie(hufte - quer, schulter - quer, f.streifen, 0.006f)

    val fussV = hufte + richtung(beinVorn) * BEIN
    linie(hufte, fussV, f.anzug, 0.048f)
    linie(fussV, fussV + Offset(0.012f * z.blick, 0f), f.haut, 0.02f)

    // Kopf: Haut, Haar auf Hinterkopf und Scheitel, Auge (im Schlaf geschlossen).
    val c = p(kopf.x, kopf.y)
    val r = KOPF * s
    drawCircle(f.haut.copy(alpha = a), r, c)
    val gesicht = Offset(-u.y, u.x) * z.blick
    val haarRichtung = u * 0.8f - gesicht * 0.6f
    val haarWinkel = Math.toDegrees(atan2(haarRichtung.y.toDouble(), haarRichtung.x.toDouble())).toFloat()
    drawArc(f.haar.copy(alpha = a), haarWinkel - 105f, 210f, useCenter = true, topLeft = c - Offset(r * 1.05f, r * 1.05f),
        size = Size(r * 2.1f, r * 2.1f))
    val auge = c + gesicht * (r * .5f) + u * (r * .05f)
    if (z.schlaeft) drawLine(f.haar.copy(alpha = a), auge - gesicht * (r * .18f), auge + gesicht * (r * .18f), r * .14f, rund)
    else drawCircle(f.haar.copy(alpha = a), r * .13f, auge)

    val handV = schulter + richtung(armVorn) * ARM
    linie(schulter, handV, f.anzug, 0.038f)
    drawCircle(f.haut.copy(alpha = a), 0.014f * s, p(handV.x, handV.y))
}
