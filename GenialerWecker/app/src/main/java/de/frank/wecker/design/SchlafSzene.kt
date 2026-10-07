package de.frank.wecker.design

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * Morgenruhes Motiv als kleine Endlosszene im Takt des [Tageslauf]s: Tagsüber ist die Person
 * unterwegs. Abends geht die Sonne unter, die Tür geht auf, die Person im gestreiften Schlafanzug
 * kommt heim, knipst die Stehlampe an, setzt sich auf die Bettkante, schwingt die Beine unter die
 * Decke, legt sich hin und schläft (Z steigen auf). Morgens geht die Sonne auf, die Person wacht auf,
 * streckt sich, steht auf und geht wieder durch die Tür.
 *
 * Seit 1.1.114 breit statt quadratisch: Die Szene füllt ihr Feld in der Höhe und verteilt Tür,
 * Fenster und Bett auf die Breite, statt als kleines Quadrat an den rechten Rand gedrückt zu sein.
 * Breite Felder bekommen dazu Stehlampe und Zimmerpflanze.
 *
 * Alles wird gezeichnet (kein Bild), das Bett in Schrägansicht mit Front, Oberseite und Kopfteil.
 * Die Zeit wird nur im Zeichenblock gelesen: Neu gezeichnet wird allein diese Ebene, nie die App.
 * Läuft nur, solange der Bildschirm vorne ist; bei reduzierter Bewegung steht ein ruhiges Standbild.
 */
@Composable
internal fun SchlafSzene(modifier: Modifier) {
    val gold = LocalGold.current
    val reduziert = LocalBewegungReduziert.current
    val zeit = rememberTageszeit(rememberResumed() && !reduziert)
    val farben = SzenenFarben.fuer(gold.istDunkel, gold.textGedaempft)
    Canvas(modifier) {
        val t = if (reduziert) Tageslauf.STANDBILD else zeit.value
        zeichneSzene(zustand(t), farben)
    }
}

// Maße in Anteilen der Einheit s (siehe zeichneSzene).
private const val BODEN = 0.86f
private const val TORSO = 0.12f
private const val HALS = 0.047f
private const val KOPF = 0.034f
private const val ARM = 0.10f
private const val BEIN = 0.15f
private const val TIEFE_X = 0.05f
private const val TIEFE_Y = 0.05f
private const val MATRATZE = 0.66f
/** Senkrecht reicht die Szene von der Gardinenstange bis unter den Teppich. */
private const val INHALT_OBEN = 0.12f
private const val INHALT_HOEHE = 0.80f
/** So viel Zimmer darf ein breites Feld höchstens dazubekommen, bevor die Szene mittig steht. */
private const val MEHR_BREITE = 1.2f

private class SzenenFarben(
    val holz: Color, val holzDunkel: Color, val laken: Color, val decke: Color, val kissen: Color,
    val anzug: Color, val streifen: Color, val haut: Color, val haar: Color, val innen: Color,
    val sonne: Color, val mond: Color, val z: Color, val schatten: Color,
    val teppich: Color, val vorhang: Color, val katze: Color, val blatt: Color, val topf: Color,
    val lampenLicht: Color,
) {
    companion object {
        fun fuer(dunkel: Boolean, gedaempft: Color) = if (dunkel) SzenenFarben(
            holz = Color(0xFF8C6C4D), holzDunkel = Color(0xFF5E4631), laken = Color(0xFFD3CBBC),
            decke = Color(0xFF6F86A8), kissen = Color(0xFFE2DBCF), anzug = Color(0xFFA9BEDB),
            streifen = Color(0xFF5F7CA6), haut = Color(0xFFE2BC98), haar = Color(0xFF4E3B2E),
            innen = Color(0xFF15120F), sonne = Color(0xFFFFC65C), mond = Color(0xFFF3E7C4),
            z = gedaempft, schatten = Color.Black.copy(alpha = .45f),
            teppich = Color(0xFF7E5E55), vorhang = Color(0xFF8C6F6A), katze = Color(0xFFC98A4E),
            blatt = Color(0xFF5E8A5A), topf = Color(0xFFA0624A), lampenLicht = Color(0xFFFFD58A),
        ) else SzenenFarben(
            holz = Color(0xFFB08A62), holzDunkel = Color(0xFF7A5A3D), laken = Color(0xFFF3EEE5),
            decke = Color(0xFF8098BA), kissen = Color(0xFFFFFCF6), anzug = Color(0xFFB9CDE6),
            streifen = Color(0xFF6F8DB8), haut = Color(0xFFEBC6A3), haar = Color(0xFF5B4535),
            innen = Color(0xFF2B2622), sonne = Color(0xFFFFB94A), mond = Color(0xFFE9D9A8),
            z = gedaempft, schatten = Color.Black.copy(alpha = .20f),
            teppich = Color(0xFFC49A8A), vorhang = Color(0xFFD6B5AE), katze = Color(0xFFE3A263),
            blatt = Color(0xFF6E9E68), topf = Color(0xFFC07A5E), lampenLicht = Color(0xFFFFC870),
        )
    }
}

/** Körperhaltung: Hüfte und Winkel (0° = nach unten, 90° = nach rechts, 180° = nach oben). */
private data class Haltung(val x: Float, val y: Float, val torso: Float, val bein: Float, val arm: Float)

private fun stehend(x: Float) = Haltung(x, BODEN - BEIN, 180f, 0f, 0f)
/** Auf der Bettkante sitzend, die Beine hängen vor dem Bett herab. */
private val BETTKANTE = Haltung(0.60f, 0.665f, 180f, 4f, 12f)
/** Aufrecht im Bett, die Beine liegen unter der Decke. */
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
    val imBett: Float, /** 1 = Decke bis zum Kinn hochgezogen. */ val zugedeckt: Float,
    /** Die Stehlampe brennt vom Heimkommen bis zum Einschlafen. */ val lampe: Float,
    val daemmerung: Float, val t: Float,
)

private fun ab(t: Float, a: Float, b: Float) = Tageslauf.ab(t, a, b)
private fun weich(x: Float) = Tageslauf.weich(x)
private fun huegel(t: Float, a: Float, b: Float, c: Float, d: Float) = Tageslauf.huegel(t, a, b, c, d)

private fun zustand(t: Float): Zustand {
    val tuer = huegel(t, 11f, 12f, 14f, 15f) + huegel(t, 33.6f, 34.6f, 37f, 38f)
    val imBett = when {
        t < 15.9f -> 0f
        t < 16.7f -> weich(ab(t, 15.9f, 16.7f))
        t < 32.3f -> 1f
        t < 33f -> 1f - weich(ab(t, 32.3f, 33f))
        else -> 0f
    }
    val zugedeckt = when {
        t < 16.8f -> 0f
        t < 17.7f -> weich(ab(t, 16.8f, 17.7f))
        t < 30f -> 1f
        t < 30.8f -> 1f - weich(ab(t, 30f, 30.8f))
        else -> 0f
    }
    val schritt = { s: Float -> (sin(s * 2f * PI.toFloat() * 1.6f) * 26f) }
    var alpha = 1f
    var schwung = 0f
    var blick = 1f
    var streckt = 0f
    val haltung: Haltung? = when {
        t < 11.6f || t > 37.5f -> null
        t < 12.2f -> { alpha = ab(t, 11.6f, 12.2f); stehend(0.15f) }
        t < 15.2f -> { schwung = schritt(t - 12.2f); stehend(0.15f + 0.37f * ab(t, 12.2f, 15.2f)) }
        t < 15.9f -> { val f = weich(ab(t, 15.2f, 15.9f)); blick = if (f < .5f) 1f else -1f; mische(stehend(0.52f), BETTKANTE, f) }
        t < 16.7f -> { blick = -1f; mische(BETTKANTE, SITZEND, weich(ab(t, 15.9f, 16.7f))) }
        t < 17.6f -> { blick = -1f; mische(SITZEND, LIEGEND, weich(ab(t, 16.7f, 17.6f))) }
        t < 30f -> { blick = -1f; LIEGEND }
        t < 30.9f -> { blick = -1f; mische(LIEGEND, SITZEND, weich(ab(t, 30f, 30.9f))) }
        t < 32.3f -> { blick = -1f; streckt = huegel(t, 30.9f, 31.5f, 31.7f, 32.3f); SITZEND }
        t < 33f -> { blick = -1f; mische(SITZEND, BETTKANTE, weich(ab(t, 32.3f, 33f))) }
        t < 33.7f -> { blick = -1f; mische(BETTKANTE, stehend(0.52f), weich(ab(t, 33f, 33.7f))) }
        t < 36.9f -> { blick = -1f; schwung = schritt(t - 33.7f); stehend(0.52f - 0.37f * ab(t, 33.7f, 36.9f)) }
        else -> { blick = -1f; alpha = 1f - ab(t, 36.9f, 37.5f); stehend(0.15f) }
    }
    val schlaeft = t in 17.3f..30f
    val lampe = huegel(t, 12f, 12.6f, 17.8f, 18.6f)
    return Zustand(haltung, schwung, blick, alpha, streckt, schlaeft, t - 17.6f, tuer, Tageslauf.tag(t),
        imBett, zugedeckt, lampe, Tageslauf.daemmerung(t), t)
}

private fun richtung(winkel: Float): Offset {
    val r = Math.toRadians(winkel.toDouble())
    return Offset(sin(r).toFloat(), cos(r).toFloat())
}

/**
 * Die Szene in einem frei geformten Feld. Die Einheit s richtet sich nach der Höhe; was das Feld
 * breiter ist als ein Quadrat, wird zwischen Tür und Bett als Zimmer verteilt: Die Tür bleibt links,
 * das Fenster rückt in die Mitte, das Bett nach rechts. Kein Teil ragt mehr aus dem Feld heraus.
 */
private fun DrawScope.zeichneSzene(z: Zustand, f: SzenenFarben) {
    val s = min(size.height / INHALT_HOEHE, size.width)
    val mehr = (size.width / s - 1f).coerceIn(0f, MEHR_BREITE)
    val ox = (size.width - (1f + mehr) * s) / 2f
    val oy = (size.height - INHALT_HOEHE * s) / 2f - INHALT_OBEN * s
    // Versatz der drei Gruppen in Einheiten: Tür 0, Fenster halb, Bett ganz.
    val dFenster = mehr * 0.5f
    val dBett = mehr
    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    fun poly(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    val nacht = 1f - z.sonne

    // Bodenschatten unter Tür und Bett, Teppich mit Rand und Fransen, Fußmatte vor der Tür.
    drawOval(Brush.radialGradient(listOf(f.schatten, Color.Transparent), center = p(0.15f, 0.865f), radius = 0.14f * s),
        topLeft = p(0.02f, 0.85f), size = Size(0.26f * s, 0.03f * s))
    drawRect(lerp(f.teppich, f.holzDunkel, .4f), p(0.07f, 0.858f), Size(0.16f * s, 0.018f * s))
    translate(left = dBett * s) {
        drawOval(Brush.radialGradient(listOf(f.schatten, Color.Transparent), center = p(0.66f, 0.865f), radius = 0.34f * s),
            topLeft = p(0.32f, 0.84f), size = Size(0.68f * s, 0.05f * s))
        val teppich = Rect(p(0.33f, 0.852f), p(0.95f, 0.905f))
        drawOval(f.teppich, teppich.topLeft, teppich.size)
        drawOval(lerp(f.teppich, Color.White, .35f), teppich.topLeft + Offset(0.02f * s, 0.006f * s),
            Size(teppich.width - 0.04f * s, teppich.height - 0.012f * s), style = Stroke(0.004f * s))
        listOf(teppich.left, teppich.right).forEachIndexed { seite, x ->
            repeat(4) { i ->
                val y = teppich.center.y + (i - 1.5f) * 0.008f * s
                val d = if (seite == 0) -1f else 1f
                drawLine(lerp(f.teppich, Color.White, .3f), Offset(x - d * 0.004f * s, y), Offset(x + d * 0.012f * s, y), 0.003f * s)
            }
        }
    }

    translate(left = dFenster * s) { fenster(z, f, s, ::p) }

    // Licht fällt schräg aus dem Fenster aufs Bett: morgens und tags warm, nachts ein Hauch Mondlicht.
    val lichtFarbe = lerp(f.mond, f.sonne, z.sonne)
    val lichtAlpha = .16f * z.sonne + .07f * nacht
    if (lichtAlpha > 0.005f) {
        drawPath(poly(p(0.51f + dFenster, 0.46f), p(0.79f + dFenster, 0.46f), p(0.92f + dBett, 0.70f), p(0.60f + dBett, 0.70f)),
            Brush.verticalGradient(listOf(lichtFarbe.copy(alpha = lichtAlpha), Color.Transparent), startY = p(0f, 0.46f).y, endY = p(0f, 0.70f).y))
    }

    // Wanduhr über der Tür; die Zeiger rasen, als liefe der Tag im Zeitraffer.
    val uhr = p(0.15f, 0.34f)
    val ur = 0.036f * s
    drawCircle(f.holzDunkel, ur * 1.15f, uhr)
    drawCircle(lerp(f.laken, Color.White, .4f), ur, uhr)
    repeat(12) { i ->
        val d = richtung(i * 30f)
        drawLine(f.holzDunkel, uhr + d * (ur * .78f), uhr + d * (ur * .92f), 0.003f * s)
    }
    val umlauf = z.t / Tageslauf.ZYKLUS
    drawLine(f.holzDunkel, uhr, uhr + richtung(180f - umlauf * 720f) * (ur * .5f), 0.006f * s, StrokeCap.Round)
    drawLine(f.holzDunkel, uhr, uhr + richtung(180f - umlauf * 360f * 12f) * (ur * .78f), 0.004f * s, StrokeCap.Round)
    drawCircle(Color(0xFFA8322B), 0.005f * s, uhr)
    tuer(z, f, s, ::p)

    // Breite Felder: Zimmerpflanze und Stehlampe zwischen Tür und Bett.
    if (mehr > 0.3f) {
        pflanze(p(0.31f + mehr * 0.22f, BODEN), z, f, s)
        stehlampe(p(0.31f + mehr * 0.68f, BODEN), z, f, s)
    }

    val figur = z.haltung
    translate(left = dBett * s) {
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

        val huelle = decke(z, f, s, ::p)
        katze(z, f, s, ::p)

        // Im Bett: Was unter der Decke liegt, verschwindet weich unter ihr, statt mit einem Mal
        // hinter sie zu springen. Außerhalb der Decke bleibt die Person voll sichtbar.
        if (figur != null && z.imBett > 0f) {
            clipPath(huelle, ClipOp.Difference) { person(figur, z, f, s, ::p) }
            val rest = 1f - z.imBett
            if (rest > 0.01f) clipPath(huelle) {
                val ebene = Rect(Offset.Zero, size)
                drawContext.canvas.saveLayer(ebene, Paint().apply { alpha = rest })
                person(figur, z, f, s, ::p)
                drawContext.canvas.restore()
            }
        }

        bettVorn(f, s, ::p)
    }

    // Außerhalb des Bettes steht die Person vor allem anderen; ihr Versatz folgt der Hüfte von der
    // Tür zum Bett — als Ganzes, damit der Körper beim Gehen nicht verzerrt wird.
    if (figur != null && z.imBett <= 0f) {
        val versatz = dBett * ab(figur.x, 0.15f, 0.52f)
        translate(left = versatz * s) { person(figur, z, f, s, ::p) }
    }

    // Z steigen vom Kopf auf, solange die Person schläft.
    if (z.schlaeft && figur != null && z.schlafZeit > 0.4f) translate(left = dBett * s) {
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

/** Fenster: Himmel von Tag über Abendrot zu Nacht, Sonne und Mond ziehen darin, Hügel, Gardinen. */
private fun DrawScope.fenster(z: Zustand, f: SzenenFarben, s: Float, p: (Float, Float) -> Offset) {
    val fenster = Rect(p(0.50f, 0.16f), p(0.80f, 0.46f))
    val nacht = 1f - z.sonne
    val abendrot = Color(0xFFFF9A5C)
    val oben = lerp(lerp(Color(0xFF1B2440), Color(0xFF8EC2EE), z.sonne), Color(0xFF7A5C8E), z.daemmerung * .5f)
    val unten = lerp(lerp(Color(0xFF3B4870), Color(0xFFFFD49A), z.sonne), abendrot, z.daemmerung * .7f)
    drawRect(Brush.verticalGradient(listOf(oben, unten), startY = fenster.top, endY = fenster.bottom), fenster.topLeft, fenster.size)
    clipRect(fenster.left, fenster.top, fenster.right, fenster.bottom) {
        if (nacht > 0.01f) {
            listOf(0.54f to 0.20f, 0.72f to 0.19f, 0.77f to 0.29f, 0.60f to 0.33f, 0.69f to 0.25f, 0.53f to 0.30f).forEachIndexed { i, (x, y) ->
                val funkeln = 0.5f + 0.5f * sin(z.t * 2.2f + i * 1.7f)
                drawCircle(f.mond.copy(alpha = nacht * funkeln), (0.004f + (i % 2) * 0.003f) * s, p(x, y))
            }
            val m = p(0.585f, 0.235f)
            val mr = 0.032f * s
            val sichel = Path.combine(PathOperation.Difference,
                Path().apply { addOval(Rect(m, mr)) },
                Path().apply { addOval(Rect(m + Offset(mr * .45f, -mr * .25f), mr * .86f)) })
            drawCircle(Brush.radialGradient(listOf(f.mond.copy(alpha = .30f * nacht), Color.Transparent), center = m, radius = mr * 2.4f), mr * 2.4f, m)
            drawPath(sichel, f.mond.copy(alpha = nacht))
            // Eine kleine Fledermaus flattert dreimal am Fenster vorbei.
            if (z.t in 15f..27f) {
                val q = ((z.t - 15f) % 4f) / 4f
                val bx = p(0.48f + 0.36f * q, 0.215f + 0.02f * sin(q * 14f))
                fledermaus(bx, 0.018f * s, sin(z.t * 32f), Color(0xFF0E1018).copy(alpha = nacht))
            }
        }
        if (z.sonne > 0.01f) {
            val c = p(0.665f, 0.52f - 0.25f * z.sonne)
            val r = 0.045f * s
            val warm = lerp(f.sonne, abendrot, z.daemmerung * .6f)
            drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = .45f * z.sonne), Color.Transparent), center = c, radius = r * 3f), r * 3f, c)
            val dreh = z.t / Tageslauf.ZYKLUS * 540f
            repeat(10) { i ->
                val d = richtung(i * 36f + dreh)
                drawLine(warm.copy(alpha = .75f * z.sonne), c + d * (r * 1.3f), c + d * (r * 1.75f), 0.009f * s, StrokeCap.Round)
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE7A8).copy(alpha = z.sonne), warm.copy(alpha = z.sonne)),
                center = c - Offset(r * .3f, r * .3f), radius = r * 1.3f), r, c)
        }
        // Zwei Hügel mit ein paar Bäumen.
        val huegelFarbe = lerp(Color(0xFF151C2A), Color(0xFF6E9670), z.sonne)
        drawPath(Path().apply {
            moveTo(fenster.left, fenster.bottom); lineTo(fenster.left, p(0f, 0.41f).y)
            quadraticTo(p(0.58f, 0.37f).x, p(0f, 0.37f).y, p(0.65f, 0.415f).x, p(0f, 0.415f).y)
            quadraticTo(p(0.73f, 0.38f).x, p(0f, 0.38f).y, fenster.right, p(0f, 0.405f).y)
            lineTo(fenster.right, fenster.bottom); close()
        }, huegelFarbe)
        listOf(0.55f to 0.39f, 0.575f to 0.385f, 0.745f to 0.39f).forEach { (x, y) ->
            drawPath(Path().apply {
                moveTo(p(x, y - 0.03f).x, p(x, y - 0.03f).y); lineTo(p(x + 0.012f, y).x, p(x + 0.012f, y).y); lineTo(p(x - 0.012f, y).x, p(x - 0.012f, y).y); close()
            }, lerp(huegelFarbe, Color.Black, .25f))
        }
    }
    // Rahmen mit Sprossen und Fensterbank.
    val rahmen = lerp(f.laken, f.holzDunkel, .15f)
    drawRect(rahmen, fenster.topLeft, fenster.size, style = Stroke(0.012f * s))
    drawLine(rahmen, Offset(fenster.center.x, fenster.top), Offset(fenster.center.x, fenster.bottom), 0.008f * s)
    drawLine(rahmen, Offset(fenster.left, fenster.center.y), Offset(fenster.right, fenster.center.y), 0.008f * s)
    drawRect(f.holz, p(0.485f, 0.46f), Size(0.33f * s, 0.016f * s))
    // Gardinen an einer Stange, mit Falten.
    drawLine(f.holzDunkel, p(0.45f, 0.135f), p(0.85f, 0.135f), 0.007f * s, StrokeCap.Round)
    listOf(0.45f, 0.85f).forEach { x -> drawCircle(f.holzDunkel, 0.009f * s, p(x, 0.135f)) }
    listOf(false, true).forEach { rechts ->
        fun q(x: Float, y: Float) = if (rechts) p(1.3f - x, y) else p(x, y)
        val vorhang = Path().apply {
            moveTo(q(0.462f, 0.135f).x, q(0.462f, 0.135f).y); lineTo(q(0.535f, 0.135f).x, q(0.535f, 0.135f).y)
            quadraticTo(q(0.505f, 0.30f).x, q(0.505f, 0.30f).y, q(0.525f, 0.50f).x, q(0.525f, 0.50f).y)
            lineTo(q(0.462f, 0.50f).x, q(0.462f, 0.50f).y); close()
        }
        drawPath(vorhang, Brush.horizontalGradient(listOf(f.vorhang, lerp(f.vorhang, Color.White, .2f), f.vorhang),
            startX = q(0.462f, 0f).x, endX = q(0.535f, 0f).x))
        listOf(0.478f, 0.495f).forEach { x -> drawLine(lerp(f.vorhang, Color.Black, .2f), q(x, 0.145f), q(x + 0.004f, 0.49f), 0.003f * s) }
    }
}

/** Tür: Zarge, dunkle Öffnung, Türblatt schwenkt an der linken Angel nach vorn auf. */
private fun DrawScope.tuer(z: Zustand, f: SzenenFarben, s: Float, p: (Float, Float) -> Offset) {
    fun poly(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    drawRect(f.holzDunkel, p(0.04f, 0.42f), Size(0.22f * s, 0.44f * s))
    drawRect(Brush.verticalGradient(listOf(f.innen, f.innen.copy(alpha = .85f)), startY = p(0f, 0.44f).y, endY = p(0f, BODEN).y),
        p(0.06f, 0.44f), Size(0.18f * s, (BODEN - 0.44f) * s))
    val winkel = z.tuer * 78f
    val blattBreite = 0.18f * cos(Math.toRadians(winkel.toDouble())).toFloat()
    val vor = 0.03f * sin(Math.toRadians(winkel.toDouble())).toFloat()
    val a0 = p(0.06f, 0.44f); val a1 = p(0.06f, BODEN)
    val b0 = p(0.06f + blattBreite, 0.44f - vor); val b1 = p(0.06f + blattBreite, BODEN + vor * .4f)
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
    drawRect(f.holzDunkel.copy(alpha = .6f), p(0.04f, 0.42f), Size(0.22f * s, 0.44f * s), style = Stroke(0.006f * s))
}

/** Eine Zimmerpflanze im Terrakottatopf; die Blätter wiegen sich kaum merklich. */
private fun DrawScope.pflanze(fuss: Offset, z: Zustand, f: SzenenFarben, s: Float) {
    val topfOben = fuss.y - 0.07f * s
    drawOval(Brush.radialGradient(listOf(f.schatten, Color.Transparent), center = fuss, radius = 0.05f * s),
        topLeft = fuss - Offset(0.05f * s, 0.008f * s), size = Size(0.10f * s, 0.016f * s))
    val wiegen = Tageslauf.welle(z.t, 9) * 3f
    listOf(-38f, -18f, 0f, 16f, 34f, -60f, 58f).forEachIndexed { i, w ->
        val laenge = (0.13f - (i % 3) * 0.018f) * s
        rotate(w + wiegen * (1f + i * .1f), pivot = Offset(fuss.x, topfOben)) {
            drawOval(lerp(f.blatt, if (i % 2 == 0) Color.White else Color.Black, .12f),
                topLeft = Offset(fuss.x - 0.016f * s, topfOben - laenge), size = Size(0.032f * s, laenge))
        }
    }
    drawPath(Path().apply {
        moveTo(fuss.x - 0.036f * s, topfOben); lineTo(fuss.x + 0.036f * s, topfOben)
        lineTo(fuss.x + 0.026f * s, fuss.y); lineTo(fuss.x - 0.026f * s, fuss.y); close()
    }, Brush.horizontalGradient(listOf(lerp(f.topf, Color.White, .15f), f.topf, lerp(f.topf, Color.Black, .25f)),
        startX = fuss.x - 0.036f * s, endX = fuss.x + 0.036f * s))
    drawRect(lerp(f.topf, Color.Black, .12f), Offset(fuss.x - 0.04f * s, topfOben - 0.01f * s), Size(0.08f * s, 0.012f * s))
}

/** Eine Stehlampe; abends, wenn die Person heimkommt, wirft sie einen warmen Lichtkegel. */
private fun DrawScope.stehlampe(fuss: Offset, z: Zustand, f: SzenenFarben, s: Float) {
    val schirmUnten = fuss.y - 0.30f * s
    val schirmOben = schirmUnten - 0.07f * s
    if (z.lampe > 0.01f) {
        drawPath(Path().apply {
            moveTo(fuss.x - 0.05f * s, schirmUnten); lineTo(fuss.x + 0.05f * s, schirmUnten)
            lineTo(fuss.x + 0.17f * s, fuss.y); lineTo(fuss.x - 0.17f * s, fuss.y); close()
        }, Brush.verticalGradient(listOf(f.lampenLicht.copy(alpha = .30f * z.lampe), Color.Transparent), startY = schirmUnten, endY = fuss.y))
        val mitte = Offset(fuss.x, schirmUnten - 0.03f * s)
        drawCircle(Brush.radialGradient(listOf(f.lampenLicht.copy(alpha = .45f * z.lampe), Color.Transparent), center = mitte, radius = 0.16f * s),
            0.16f * s, mitte)
    }
    drawOval(f.holzDunkel, topLeft = fuss - Offset(0.035f * s, 0.008f * s), size = Size(0.07f * s, 0.014f * s))
    drawLine(f.holzDunkel, fuss, Offset(fuss.x, schirmUnten), 0.006f * s)
    val schirm = lerp(f.laken, f.lampenLicht, .25f + .6f * z.lampe)
    drawPath(Path().apply {
        moveTo(fuss.x - 0.03f * s, schirmOben); lineTo(fuss.x + 0.03f * s, schirmOben)
        lineTo(fuss.x + 0.05f * s, schirmUnten); lineTo(fuss.x - 0.05f * s, schirmUnten); close()
    }, Brush.verticalGradient(listOf(lerp(schirm, Color.White, .2f), schirm), startY = schirmOben, endY = schirmUnten))
}

/**
 * Die Decke: flach gemacht, mit Beinen darunter gewölbt, beim Zudecken bis zum Kinn hochgezogen.
 * Gibt ihre Umrissfläche zurück — darunter verschwindet, was im Bett liegt.
 */
private fun DrawScope.decke(z: Zustand, f: SzenenFarben, s: Float, p: (Float, Float) -> Offset): Path {
    fun poly(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    val hx = TIEFE_X; val hy = TIEFE_Y
    val atmen = if (z.schlaeft) sin(z.t * 2f * PI.toFloat() / 3.5f) * 0.003f else 0f
    val wulst = 0.022f * z.imBett + atmen * z.zugedeckt
    val kante = 0.62f + 0.08f * z.imBett + 0.10f * z.zugedeckt
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
    // Steppnähte als Rautenmuster.
    fun steppung(form: Path, farbe: Color) = clipPath(form) {
        for (k in -8..22) {
            val x = x0 - 0.1f + k * 0.032f
            drawLine(farbe, p(x, yb + 0.01f), p(x + 0.09f, yf - hy - 0.04f), 0.0035f * s)
            drawLine(farbe, p(x + 0.09f, yb + 0.01f), p(x, yf - hy - 0.04f), 0.0035f * s)
        }
    }
    steppung(oben, Color.White.copy(alpha = .20f))
    val vorn = Path().apply {
        moveTo(p(x0, yf).x, p(x0, yf).y)
        quadraticTo(p(mitte, yf - wulst).x, p(mitte, yf - wulst).y, p(kante, yf - wulst * .5f).x, p(kante, yf - wulst * .5f).y)
        lineTo(p(kante, yb).x, p(kante, yb).y)
        // Leicht gewellter Saum.
        quadraticTo(p((x0 + kante) / 2f, yb + 0.012f).x, p((x0 + kante) / 2f, yb + 0.012f).y, p(x0, yb).x, p(x0, yb).y)
        close()
    }
    drawPath(vorn, Brush.verticalGradient(listOf(f.decke, lerp(f.decke, Color.Black, .18f)), startY = p(0f, yf - wulst).y, endY = p(0f, yb).y))
    steppung(vorn, Color.White.copy(alpha = .14f))
    // Borte am Saum.
    drawLine(lerp(f.decke, Color.White, .45f), p(x0, yb - 0.008f), p(kante, yb - 0.008f), 0.004f * s)
    // Umgeschlagener Lakenrand an der Deckenkante.
    drawPath(poly(p(kante - 0.022f, yf - wulst * .5f), p(kante, yf - wulst * .5f), p(kante + hx, yf - hy - wulst * .5f),
        p(kante - 0.022f + hx, yf - hy - wulst * .5f)), lerp(f.laken, Color.White, .4f))
    return Path.combine(PathOperation.Union, oben, vorn)
}

/** Die Katze schläft zusammengerollt am Fußende und hebt morgens kurz den Kopf. */
private fun DrawScope.katze(z: Zustand, f: SzenenFarben, s: Float, p: (Float, Float) -> Offset) {
    val katzeWach = z.sonne > 0.85f && !z.schlaeft
    val katzeAtmen = 1f + 0.04f * Tageslauf.welle(z.t, 18)
    val km = p(0.475f, 0.628f)
    val kb = 0.042f * s
    val katzeHell = f.katze; val katzeDunkel = lerp(f.katze, Color.Black, .3f)
    val schwanz = Path().apply {
        moveTo(km.x + kb * .8f, km.y + kb * .1f)
        quadraticTo(km.x + kb * 1.1f, km.y + kb * .55f + Tageslauf.welle(z.t, 10) * kb * .1f, km.x - kb * .2f, km.y + kb * .45f)
    }
    drawPath(schwanz, katzeDunkel, style = Stroke(0.012f * s, cap = StrokeCap.Round))
    drawOval(Brush.radialGradient(listOf(lerp(katzeHell, Color.White, .2f), katzeHell, katzeDunkel), center = km - Offset(kb * .2f, kb * .3f), radius = kb * 1.2f),
        km - Offset(kb, kb * .45f * katzeAtmen), Size(kb * 2f, kb * .9f * katzeAtmen))
    repeat(3) { i -> drawLine(katzeDunkel.copy(alpha = .6f), km + Offset(kb * (-.1f + i * .3f), -kb * .42f), km + Offset(kb * (.0f + i * .3f), -kb * .1f), 0.003f * s) }
    val kk = km + Offset(-kb * .95f, if (katzeWach) -kb * .5f else -kb * .2f)
    val kr = 0.017f * s
    listOf(-1f, 1f).forEach { seite ->
        drawPath(Path().apply {
            moveTo(kk.x + seite * kr * .9f, kk.y - kr * .2f); lineTo(kk.x + seite * kr * .55f, kk.y - kr * 1.35f); lineTo(kk.x + seite * kr * .05f, kk.y - kr * .8f); close()
        }, katzeDunkel)
    }
    drawCircle(katzeHell, kr, kk)
    if (katzeWach) listOf(-1f, 1f).forEach { seite -> drawCircle(Color(0xFF3C5A2A), kr * .16f, kk + Offset(seite * kr * .4f, -kr * .05f)) }
    else listOf(-1f, 1f).forEach { seite -> drawLine(katzeDunkel, kk + Offset(seite * kr * .6f, 0f), kk + Offset(seite * kr * .2f, 0f), 0.0025f * s) }
}

/** Kopfteil (rechts), vordere Beine, Verzierungen und Hausschuhe — alles, was vor Matratze und Kopf liegt. */
private fun DrawScope.bettVorn(f: SzenenFarben, s: Float, p: (Float, Float) -> Offset) {
    fun poly(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    val hx = TIEFE_X; val hy = TIEFE_Y
    drawPath(poly(p(0.905f, 0.48f), p(0.93f, 0.48f), p(0.93f + hx, 0.48f - hy), p(0.905f + hx, 0.48f - hy)), lerp(f.holz, Color.White, .15f))
    drawPath(poly(p(0.93f, 0.48f), p(0.93f + hx, 0.48f - hy), p(0.93f + hx, 0.81f), p(0.93f, BODEN)),
        Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = p(0f, 0.43f).y, endY = p(0f, BODEN).y))
    drawRect(Brush.verticalGradient(listOf(lerp(f.holz, Color.White, .08f), f.holzDunkel), startY = p(0f, 0.48f).y, endY = p(0f, BODEN).y),
        p(0.905f, 0.48f), Size(0.025f * s, (BODEN - 0.48f) * s))
    listOf(0.405f, 0.88f).forEach { x ->
        drawRect(Brush.verticalGradient(listOf(f.holz, f.holzDunkel), startY = p(0f, 0.76f).y, endY = p(0f, BODEN).y),
            p(x, 0.76f), Size(0.02f * s, (BODEN - 0.76f) * s))
    }
    // Füllungen im Kopfteil, gedrechselte Knäufe an Kopf- und Fußteil.
    drawPath(poly(p(0.94f, 0.50f), p(0.97f, 0.47f), p(0.97f, 0.62f), p(0.94f, 0.65f)), f.holzDunkel.copy(alpha = .45f), style = Stroke(0.004f * s))
    drawPath(poly(p(0.94f, 0.68f), p(0.97f, 0.65f), p(0.97f, 0.79f), p(0.94f, 0.82f)), f.holzDunkel.copy(alpha = .45f), style = Stroke(0.004f * s))
    listOf(p(0.9175f, 0.472f), p(0.3875f, 0.592f)).forEach { k ->
        drawCircle(Brush.radialGradient(listOf(lerp(f.holz, Color.White, .4f), f.holzDunkel), center = k - Offset(0.004f * s, 0.004f * s), radius = 0.016f * s), 0.012f * s, k)
    }
    listOf(0.58f to 0.884f, 0.625f to 0.889f).forEach { (x, y) ->
        val m = p(x, y)
        drawOval(f.decke, m - Offset(0.018f * s, 0.007f * s), Size(0.036f * s, 0.014f * s))
        drawOval(lerp(f.decke, Color.White, .35f), m - Offset(0.004f * s, 0.006f * s), Size(0.02f * s, 0.009f * s))
    }
}

/** Eine kleine Fledermaus als Schattenriss; [schlag] von −1 bis 1 hebt und senkt die Flügel. */
internal fun DrawScope.fledermaus(mitte: Offset, groesse: Float, schlag: Float, farbe: Color) {
    val g = groesse
    val hub = schlag * g * 0.7f
    val fluegel = Path().apply {
        moveTo(mitte.x, mitte.y - g * .15f)
        // Linker Flügel mit gezackter Hinterkante.
        quadraticTo(mitte.x - g * .9f, mitte.y - g * .2f - hub, mitte.x - g * 1.6f, mitte.y - hub * 1.2f)
        quadraticTo(mitte.x - g * 1.25f, mitte.y + g * .05f - hub * .6f, mitte.x - g * 1.05f, mitte.y + g * .25f - hub * .4f)
        quadraticTo(mitte.x - g * .8f, mitte.y + g * .05f - hub * .3f, mitte.x - g * .55f, mitte.y + g * .3f - hub * .2f)
        quadraticTo(mitte.x - g * .35f, mitte.y + g * .1f, mitte.x, mitte.y + g * .3f)
        // Rechter Flügel gespiegelt.
        quadraticTo(mitte.x + g * .35f, mitte.y + g * .1f, mitte.x + g * .55f, mitte.y + g * .3f - hub * .2f)
        quadraticTo(mitte.x + g * .8f, mitte.y + g * .05f - hub * .3f, mitte.x + g * 1.05f, mitte.y + g * .25f - hub * .4f)
        quadraticTo(mitte.x + g * 1.25f, mitte.y + g * .05f - hub * .6f, mitte.x + g * 1.6f, mitte.y - hub * 1.2f)
        quadraticTo(mitte.x + g * .9f, mitte.y - g * .2f - hub, mitte.x, mitte.y - g * .15f)
        close()
    }
    drawPath(fluegel, farbe)
    drawOval(farbe, topLeft = Offset(mitte.x - g * .2f, mitte.y - g * .35f), size = Size(g * .4f, g * .7f))
    // Zwei spitze Ohren.
    listOf(-1f, 1f).forEach { seite ->
        drawPath(Path().apply {
            moveTo(mitte.x + seite * g * .05f, mitte.y - g * .3f); lineTo(mitte.x + seite * g * .16f, mitte.y - g * .55f)
            lineTo(mitte.x + seite * g * .2f, mitte.y - g * .25f); close()
        }, farbe)
    }
}

private fun kopfMitte(h: Haltung): Offset {
    val u = richtung(h.torso)
    return Offset(h.x, h.y) + u * (TORSO + HALS)
}

/** Die Person: Beine, Oberkörper im gestreiften Schlafanzug, Arme, Kopf mit Haar. [p] rechnet Anteile in Pixel. */
private fun DrawScope.person(h: Haltung, z: Zustand, f: SzenenFarben, s: Float, p: (Float, Float) -> Offset) {
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
    // Ein feiner Streifen auch auf der Schlafanzughose.
    linie(hufte + (fussV - hufte) * 0.15f, hufte + (fussV - hufte) * 0.9f, f.streifen.copy(alpha = .7f), 0.005f)
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
