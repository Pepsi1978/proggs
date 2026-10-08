package de.frank.aufgaben.ui.szenen

import android.graphics.Matrix
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ---------- Hilfen ----------
internal fun an(t: Float, start: Float, dauer: Float) = ((t - start) / dauer).coerceIn(0f, 1f)
internal fun weich(x: Float) = x * x * (3 - 2 * x)
internal fun mix(a: Float, b: Float, x: Float) = a + (b - a) * x.coerceIn(0f, 1f)
internal fun rad(g: Float) = g * PI.toFloat() / 180f
internal fun Color.dunkler(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
internal fun Color.heller(f: Float) = Color(red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha)
/** Blinzeln alle paar Sekunden, jeweils 0,15 s. */
internal fun blinzelt(t: Float, versatz: Float = 0f) = ((t + versatz) % 3.7f) < 0.15f

internal class MenschFarben(val haut: Color, val haar: Color, val oben: Color, val hose: Color, val schuh: Color, val augen: Color = Color(0xFF2B211B))

/**
 * Körperhaltung in Seitenansicht. Winkel in Grad ab senkrecht nach unten, positiv = in Blickrichtung.
 * Beine: Oberschenkel h, Kniebeugung k (Unterschenkel = h − k). Arme: Oberarm s, Ellbogen e (Unterarm = s + e).
 */
internal data class Pose(
    val x: Float, val boden: Float, val dir: Float = 1f, val lean: Float = 0f,
    val hL: Float = 0f, val kL: Float = 0f, val hR: Float = 0f, val kR: Float = 0f,
    val sL: Float = 6f, val eL: Float = 8f, val sR: Float = -6f, val eR: Float = 8f,
    val kopf: Float = 0f, val hueftY: Float? = null, val blinzeln: Boolean = false, val lachen: Float = 0.35f,
)

internal fun gehen(x: Float, boden: Float, dir: Float, phase: Float, blinzeln: Boolean = false): Pose {
    val sw = sin(phase)
    return Pose(
        x, boden, dir, lean = 4f,
        hL = 24f * sw, kL = if (sw < 0) -sw * 38f else 5f,
        hR = -24f * sw, kR = if (sw > 0) sw * 38f else 5f,
        sL = -22f * sw, eL = 22f, sR = 22f * sw, eR = 22f, blinzeln = blinzeln,
    )
}

internal fun sitzen(x: Float, hueftY: Float, dir: Float) = Pose(x, hueftY, dir, lean = -4f, hL = 84f, kL = 84f, hR = 88f, kR = 88f, hueftY = hueftY)

/** Hüfthöhe, wie [mensch] sie zeichnet (auch für stehende Posen ohne feste Hüfte). */
internal fun Pose.huefteBei(s: Float): Float {
    fun beinHoehe(h: Float, k: Float) = 0.25f * s * cos(rad(h)) + 0.24f * s * cos(rad(h - k))
    return hueftY ?: (boden - maxOf(beinHoehe(hL, kL), beinHoehe(hR, kR)) - 0.035f * s)
}

/** Blendet weich zu [b] über. Die Blickrichtung läuft dabei durch 0 – das sieht aus wie ein Umdrehen. */
internal fun Pose.mischen(b: Pose, k: Float, s: Float): Pose {
    if (k <= 0f) return this
    if (k >= 1f) return b
    fun m(x: Float, y: Float) = x + (y - x) * k
    val beideStehen = hueftY == null && b.hueftY == null
    return Pose(
        m(x, b.x), m(boden, b.boden), m(dir, b.dir), m(lean, b.lean),
        m(hL, b.hL), m(kL, b.kL), m(hR, b.hR), m(kR, b.kR),
        m(sL, b.sL), m(eL, b.eL), m(sR, b.sR), m(eR, b.eR),
        m(kopf, b.kopf), if (beideStehen) null else m(huefteBei(s), b.huefteBei(s)),
        if (k < 0.5f) blinzeln else b.blinzeln, m(lachen, b.lachen),
    )
}

/** Ein Abschnitt der Handlung: ab [start] gilt die Haltung aus [pose]. */
internal class Takt(val start: Float, val pose: (Float) -> Pose)

/** Haltung zur Zeit [t]; an jeder Grenze gleitet die alte Haltung in [blende] Sekunden in die neue. */
internal fun choreo(t: Float, s: Float, takte: List<Takt>, blende: Float = 0.55f): Pose {
    val i = takte.indexOfLast { t >= it.start }.coerceAtLeast(0)
    val jetzt = takte[i].pose(t)
    if (i == 0) return jetzt
    val k = (t - takte[i].start) / blende
    if (k >= 1f) return jetzt
    return takte[i - 1].pose(t).mischen(jetzt, weich(k), s)
}

/** Freudensprünge zwischen [start] und [ende]: kleine Hüpfer, die weich beginnen und weich auslaufen. */
internal fun huepfen(t: Float, start: Float, ende: Float, hoehe: Float, takt: Float = 6.5f): Float {
    if (t <= start || t >= ende) return 0f
    val huelle = weich(an(t, start, 0.5f)) * (1f - weich(an(t, ende - 0.6f, 0.6f)))
    return maxOf(0f, sin((t - start) * takt)) * hoehe * huelle
}

// ---------- Perspektive ----------
/** Matrix, die das flache Rechteck [quelle] auf das Viereck [ziel] abbildet (oben links, oben rechts, unten rechts, unten links). */
internal fun perspektive(quelle: Rect, ziel: List<Offset>): Matrix = Matrix().apply {
    setPolyToPoly(
        floatArrayOf(quelle.left, quelle.top, quelle.right, quelle.top, quelle.right, quelle.bottom, quelle.left, quelle.bottom), 0,
        floatArrayOf(ziel[0].x, ziel[0].y, ziel[1].x, ziel[1].y, ziel[2].x, ziel[2].y, ziel[3].x, ziel[3].y), 0, 4,
    )
}

/** Zeichnet [block] flach, aber durch [m] räumlich verzerrt. */
internal inline fun DrawScope.verzerrt(m: Matrix, block: DrawScope.() -> Unit) {
    drawIntoCanvas { it.nativeCanvas.save(); it.nativeCanvas.concat(m) }
    block()
    drawIntoCanvas { it.nativeCanvas.restore() }
}

internal fun Matrix.abbilden(p: Offset): Offset {
    val a = floatArrayOf(p.x, p.y)
    mapPoints(a)
    return Offset(a[0], a[1])
}

/** Gefülltes Viereck aus vier Ecken. */
internal fun DrawScope.flaeche(ecken: List<Offset>, farbe: Color) {
    drawPath(Path().apply { moveTo(ecken[0].x, ecken[0].y); for (i in 1 until ecken.size) lineTo(ecken[i].x, ecken[i].y); close() }, farbe)
}

internal fun DrawScope.flaeche(ecken: List<Offset>, pinsel: Brush) {
    drawPath(Path().apply { moveTo(ecken[0].x, ecken[0].y); for (i in 1 until ecken.size) lineTo(ecken[i].x, ecken[i].y); close() }, pinsel)
}

/** Sichelmond: helle Scheibe, aus der rechts oben eine zweite Scheibe ausgeschnitten ist. */
internal fun DrawScope.mondsichel(mitte: Offset, r: Float, farbe: Color) {
    val scheibe = Path().apply { addOval(Rect(mitte, r)) }
    val schatten = Path().apply { addOval(Rect(mitte + Offset(r * 0.48f, -r * 0.2f), r * 0.92f)) }
    drawPath(Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, scheibe, schatten), farbe)
}

/**
 * Quader in leichter Aufsicht: Vorderseite [vorne] (Rechteck), dazu sichtbare Oberseite und Seite, die um
 * [tiefe] nach hinten zum Fluchtpunkt [flucht] laufen. Gibt die vier Ecken der Oberseite zurück.
 */
internal fun DrawScope.quader(vorne: Rect, tiefe: Float, flucht: Offset, farbe: Color): List<Offset> {
    fun hinten(p: Offset) = p + (flucht - p) * tiefe
    val ol = Offset(vorne.left, vorne.top); val or = Offset(vorne.right, vorne.top)
    val ur = Offset(vorne.right, vorne.bottom); val ul = Offset(vorne.left, vorne.bottom)
    val oben = listOf(hinten(ol), hinten(or), or, ol)
    // Seite, die zum Betrachter zeigt: links vom Fluchtpunkt die rechte Seite, rechts davon die linke.
    if (vorne.right < flucht.x) flaeche(listOf(or, hinten(or), hinten(ur), ur), farbe.dunkler(0.28f))
    else if (vorne.left > flucht.x) flaeche(listOf(ol, hinten(ol), hinten(ul), ul), farbe.dunkler(0.28f))
    if (vorne.top > flucht.y) flaeche(oben, farbe.heller(0.16f))
    drawRect(farbe, vorne.topLeft, vorne.size)
    return oben
}

internal class Punkte(val handL: Offset, val handR: Offset, val kopf: Offset, val schulter: Offset)

internal fun DrawScope.mensch(p: Pose, s: Float, c: MenschFarben): Punkte {
    val ober = 0.25f * s; val unter = 0.24f * s; val rumpf = 0.3f * s
    val oa = 0.15f * s; val ua = 0.14f * s; val kr = 0.078f * s
    fun v(w: Float, l: Float) = Offset(sin(rad(w)) * p.dir * l, cos(rad(w)) * l)
    fun beinHoehe(h: Float, k: Float) = ober * cos(rad(h)) + unter * cos(rad(h - k))
    val hueftY = p.hueftY ?: (p.boden - maxOf(beinHoehe(p.hL, p.kL), beinHoehe(p.hR, p.kR)) - 0.035f * s)
    val huefte = Offset(p.x, hueftY)
    val schulter = huefte + Offset(sin(rad(p.lean)) * p.dir * rumpf, -cos(rad(p.lean)) * rumpf)

    fun bein(h: Float, k: Float, farbe: Color) {
        val knie = huefte + v(h, ober)
        val fuss = knie + v(h - k, unter)
        drawLine(farbe, huefte, knie, 0.085f * s, StrokeCap.Round)
        drawLine(farbe, knie, fuss, 0.072f * s, StrokeCap.Round)
        val lx = if (p.dir > 0) fuss.x - 0.035f * s else fuss.x - 0.085f * s
        drawRoundRect(c.schuh, Offset(lx, fuss.y - 0.02f * s), Size(0.12f * s, 0.05f * s), CornerRadius(0.025f * s))
        drawLine(c.schuh.heller(0.25f), Offset(lx + 0.01f * s, fuss.y + 0.028f * s), Offset(lx + 0.11f * s, fuss.y + 0.028f * s), 0.008f * s)
    }
    fun arm(sw: Float, e: Float, farbe: Color): Offset {
        val ell = schulter + v(sw, oa)
        val hand = ell + v(sw + e, ua)
        drawLine(farbe, schulter, ell, 0.07f * s, StrokeCap.Round)
        drawLine(farbe, ell, hand, 0.06f * s, StrokeCap.Round)
        drawCircle(c.haut, 0.033f * s, hand)
        return hand
    }
    // Bodenschatten
    if (p.hueftY == null) drawOval(Color.Black.copy(alpha = 0.16f), Offset(p.x - 0.14f * s, p.boden - 0.012f * s), Size(0.28f * s, 0.035f * s))
    bein(p.hL, p.kL, c.hose.dunkler(0.25f))
    val handL = arm(p.sL, p.eL, c.oben.dunkler(0.22f))
    // Rumpf mit Kragen und Gürtel
    rotate(p.lean * p.dir, huefte) {
        drawRoundRect(c.oben, Offset(p.x - 0.075f * s, hueftY - rumpf - 0.015f * s), Size(0.15f * s, rumpf + 0.05f * s), CornerRadius(0.055f * s))
        drawRoundRect(c.oben.heller(0.18f), Offset(p.x - 0.075f * s + (if (p.dir > 0) 0.09f * s else 0.01f * s), hueftY - rumpf), Size(0.05f * s, rumpf * 0.8f), CornerRadius(0.025f * s))
        drawRect(c.hose.dunkler(0.15f), Offset(p.x - 0.075f * s, hueftY - 0.03f * s), Size(0.15f * s, 0.045f * s))
        drawLine(c.oben.dunkler(0.3f), Offset(p.x - 0.02f * s, hueftY - rumpf + 0.005f * s), Offset(p.x + 0.035f * s * p.dir, hueftY - rumpf + 0.05f * s), 0.012f * s)
    }
    bein(p.hR, p.kR, c.hose)
    // Hals und Kopf
    val kopf = schulter + Offset(0.012f * s * p.dir, -0.035f * s - kr)
    drawLine(c.haut.dunkler(0.08f), schulter, kopf + Offset(0f, kr * 0.6f), 0.052f * s, StrokeCap.Round)
    rotate(p.kopf * p.dir, kopf + Offset(0f, kr)) {
        drawCircle(c.haut, kr, kopf)
        // Haare: oben und hinten, dazu ein Pony
        drawArc(c.haar, if (p.dir > 0) 118f else -148f, 210f, true, Offset(kopf.x - kr * 1.06f, kopf.y - kr * 1.1f), Size(kr * 2.12f, kr * 2.0f))
        drawCircle(c.haar, kr * 0.35f, kopf + Offset(kr * 0.35f * p.dir, -kr * 0.78f))
        // Ohr
        drawCircle(c.haut.dunkler(0.1f), kr * 0.22f, kopf + Offset(-kr * 0.12f * p.dir, kr * 0.08f))
        // Auge, Braue, Nase, Mund
        val auge = kopf + Offset(kr * 0.48f * p.dir, -kr * 0.08f)
        if (p.blinzeln) drawLine(c.augen, auge - Offset(kr * 0.12f, 0f), auge + Offset(kr * 0.12f, 0f), kr * 0.08f)
        else { drawCircle(Color.White, kr * 0.15f, auge); drawCircle(c.augen, kr * 0.1f, auge + Offset(kr * 0.03f * p.dir, 0f)) }
        drawLine(c.haar, auge + Offset(-kr * 0.16f, -kr * 0.26f), auge + Offset(kr * 0.16f, -kr * 0.3f), kr * 0.07f, StrokeCap.Round)
        drawCircle(c.haut.dunkler(0.06f), kr * 0.16f, kopf + Offset(kr * 0.95f * p.dir, kr * 0.12f))
        val mund = kopf + Offset(kr * 0.55f * p.dir, kr * 0.45f)
        drawArc(Color(0xFF8A3B2E), 20f, 140f, false, mund - Offset(kr * 0.22f, kr * (0.12f + 0.1f * p.lachen)), Size(kr * 0.44f, kr * (0.2f + 0.25f * p.lachen)), style = Stroke(kr * 0.07f, cap = StrokeCap.Round))
        drawCircle(Color(0xFFFF8A80).copy(alpha = 0.25f), kr * 0.18f, kopf + Offset(kr * 0.42f * p.dir, kr * 0.3f))
    }
    val handR = arm(p.sR, p.eR, c.oben)
    return Punkte(handL, handR, kopf, schulter)
}

/** Zeichnet den rechten Arm von [p] noch einmal, z. B. vor einer Katze auf dem Schoß. */
internal fun DrawScope.rechterArm(p: Pose, s: Float, c: MenschFarben, schulter: Offset) {
    fun v(w: Float, l: Float) = Offset(sin(rad(w)) * p.dir * l, cos(rad(w)) * l)
    val ell = schulter + v(p.sR, 0.15f * s)
    val hand = ell + v(p.sR + p.eR, 0.14f * s)
    drawLine(c.oben, schulter, ell, 0.07f * s, StrokeCap.Round)
    drawLine(c.oben, ell, hand, 0.06f * s, StrokeCap.Round)
    drawCircle(c.haut, 0.033f * s, hand)
}

/** Zweigelenk-Arm: berechnet Ellbogen so, dass die Hand genau am Ziel liegt (soweit erreichbar). */
internal fun ik(a: Offset, ziel: Offset, l1: Float, l2: Float, seite: Float): Pair<Offset, Offset> {
    val d = ziel - a
    val laenge = d.getDistance().coerceAtLeast(0.001f)
    val dist = laenge.coerceIn(abs(l1 - l2) + 0.01f, l1 + l2 - 0.01f)
    val richtung = d / laenge
    val hand = a + richtung * dist
    val winkel = acos(((l1 * l1 + dist * dist - l2 * l2) / (2 * l1 * dist)).coerceIn(-1f, 1f))
    val basis = atan2(richtung.y, richtung.x) + seite * winkel
    return Offset(a.x + cos(basis) * l1, a.y + sin(basis) * l1) to hand
}

/**
 * Wie [ik], aber mit dem Ellbogen, den ein echter Arm nimmt: nach unten (Schwerkraft) und eher nach außen
 * ([aussen] = −1 linke Körperseite, +1 rechte). Gerechnet wie im Raum: Der Ellbogen liegt auf einem Kreis um die Linie
 * Schulter–Hand und zeigt etwas zum Betrachter. So wandert er bei jeder Bewegung stetig von einer Seite zur anderen
 * (dabei kurz perspektivisch verkürzt), statt umzuspringen.
 */
internal fun ikNatuerlich(a: Offset, ziel: Offset, l1: Float, l2: Float, aussen: Float): Pair<Offset, Offset> {
    val d = ziel - a
    val laenge = d.getDistance().coerceAtLeast(0.001f)
    val dist = laenge.coerceIn(abs(l1 - l2) + 0.01f, l1 + l2 - 0.01f)
    val u = d / laenge
    val hand = a + u * dist
    val entlang = (l1 * l1 + dist * dist - l2 * l2) / (2f * dist)
    val radius = kotlin.math.sqrt((l1 * l1 - entlang * entlang).coerceAtLeast(0f))
    val quer = Offset(-u.y, u.x)
    // Bevorzugte Ellbogenrichtung: nach außen und unten, dazu ein fester Anteil zum Betrachter hin
    val wunsch = aussen * 0.8f * quer.x + 1f * quer.y
    val phi = atan2(0.45f, wunsch)
    return (a + u * entlang + quer * (radius * cos(phi))) to hand
}

/** Rechte Hand einer Seitenansicht-Pose, ohne zu zeichnen (gleiche Maße wie [mensch]). */
internal fun Pose.handR(s: Float): Offset {
    fun v(w: Float, l: Float) = Offset(sin(rad(w)) * dir * l, cos(rad(w)) * l)
    val huefte = Offset(x, huefteBei(s))
    val schulter = huefte + Offset(sin(rad(lean)) * dir * 0.3f * s, -cos(rad(lean)) * 0.3f * s)
    return schulter + v(sR, 0.15f * s) + v(sR + eR, 0.14f * s)
}

// ---------- Endlosschleife ----------
/** Sinus mit genau [n] Schwingungen pro Durchlauf: beim Neustart der Szene springt nichts. */
internal fun welle(t: Float, zyklus: Float, n: Int, phase: Float = 0f) = sin(2f * PI.toFloat() * n * t / zyklus + phase)

/** Gleichmäßiger Lauf 0..1, genau [n] Runden pro Durchlauf. */
internal fun runde(t: Float, zyklus: Float, n: Int, versatz: Float = 0f): Float { val x = n * t / zyklus + versatz; return x - kotlin.math.floor(x) }

/** Wollknäuel der Katze. */
internal fun DrawScope.knaeuel(m: Offset, r: Float, farbe: Color) {
    drawCircle(farbe, r, m)
    for (i in 0..2) drawArc(farbe.dunkler(0.25f), 200f + i * 50f, 120f, false, Offset(m.x - r * 0.8f, m.y - r * 0.8f + i * r * 0.15f), Size(r * 1.6f, r * 1.4f), style = Stroke(r * 0.18f))
}

/** Maul der Katze (für Spielzeug im Maul): modus 0 = stehen/laufen, 1 = sitzen. */
internal fun katzenMaul(x: Float, boden: Float, s: Float, dir: Float, modus: Int): Offset =
    if (modus == 1) Offset(x + 0.24f * s * dir, boden - 0.7f * s) else Offset(x + 0.68f * s * dir, boden - 0.55f * s)

/**
 * Mensch von vorne oder von hinten. Die Hände gehen per [ik] zu [zielL]/[zielR] (null = locker hängend).
 */
internal fun DrawScope.menschFront(
    x: Float, boden: Float, s: Float, c: MenschFarben, vonHinten: Boolean,
    zielL: Offset? = null, zielR: Offset? = null, sprung: Float = 0f, kopfNeigung: Float = 0f,
    blinzeln: Boolean = false, lachen: Float = 0.4f, schritt: Float = 0f,
): Punkte {
    val bein = 0.47f * s
    val huefteY = boden - bein - sprung
    val rumpf = 0.31f * s
    val schulterY = huefteY - rumpf
    val sb = 0.1f * s
    drawOval(Color.Black.copy(alpha = 0.15f * (1f - sprung / (0.3f * s)).coerceIn(0.3f, 1f)), Offset(x - 0.16f * s, boden - 0.015f * s), Size(0.32f * s, 0.04f * s))
    // Beine
    for (seite in listOf(-1f, 1f)) {
        val hub = if (seite < 0) maxOf(0f, sin(schritt)) else maxOf(0f, -sin(schritt))
        val fuss = Offset(x + seite * 0.06f * s, boden - sprung - hub * 0.05f * s)
        drawLine(c.hose.dunkler(if (seite < 0) 0.12f else 0f), Offset(x + seite * 0.045f * s, huefteY), fuss, 0.085f * s, StrokeCap.Round)
        drawRoundRect(c.schuh, Offset(fuss.x - 0.05f * s, fuss.y - 0.02f * s), Size(0.1f * s, 0.05f * s), CornerRadius(0.025f * s))
    }
    // Arme hinter dem Rumpf, wenn von hinten gesehen und erhoben
    val schulterL = Offset(x - sb, schulterY + 0.03f * s)
    val schulterR = Offset(x + sb, schulterY + 0.03f * s)
    fun arm(schulter: Offset, ziel: Offset?, seite: Float): Offset {
        val z = ziel ?: (schulter + Offset(seite * 0.05f * s, 0.28f * s))
        val (ell, hand) = ikNatuerlich(schulter, z, 0.15f * s, 0.15f * s, seite)
        drawLine(c.oben.dunkler(0.1f), schulter, ell, 0.07f * s, StrokeCap.Round)
        drawLine(c.oben.dunkler(0.1f), ell, hand, 0.06f * s, StrokeCap.Round)
        drawCircle(c.haut, 0.035f * s, hand)
        return hand
    }
    // Rumpf
    drawRoundRect(c.oben, Offset(x - 0.12f * s, schulterY), Size(0.24f * s, rumpf + 0.04f * s), CornerRadius(0.07f * s))
    drawRect(c.hose.dunkler(0.15f), Offset(x - 0.115f * s, huefteY - 0.03f * s), Size(0.23f * s, 0.05f * s))
    if (!vonHinten) {
        drawLine(c.oben.dunkler(0.25f), Offset(x, schulterY + 0.02f * s), Offset(x, huefteY - 0.04f * s), 0.008f * s)
        drawArc(c.haut.dunkler(0.05f), 0f, 180f, true, Offset(x - 0.04f * s, schulterY - 0.03f * s), Size(0.08f * s, 0.07f * s))
    } else {
        drawLine(c.oben.dunkler(0.18f), Offset(x - 0.07f * s, schulterY + 0.1f * s), Offset(x + 0.07f * s, schulterY + 0.1f * s), 0.006f * s)
    }
    val handL = arm(schulterL, zielL, -1f)
    val handR = arm(schulterR, zielR, 1f)
    // Kopf
    val kr = 0.085f * s
    val kopf = Offset(x, schulterY - 0.035f * s - kr)
    drawLine(c.haut.dunkler(0.08f), Offset(x, schulterY), kopf, 0.055f * s, StrokeCap.Round)
    rotate(kopfNeigung, Offset(x, schulterY)) {
        drawCircle(c.haut.dunkler(0.1f), kr * 0.24f, kopf + Offset(-kr * 0.98f, kr * 0.1f))
        drawCircle(c.haut.dunkler(0.1f), kr * 0.24f, kopf + Offset(kr * 0.98f, kr * 0.1f))
        drawCircle(c.haut, kr, kopf)
        if (vonHinten) {
            drawCircle(c.haar, kr * 1.03f, kopf + Offset(0f, -kr * 0.05f))
            drawArc(c.haar.heller(0.12f), 200f, 60f, false, Offset(kopf.x - kr * 0.8f, kopf.y - kr * 0.8f), Size(kr * 1.6f, kr * 1.6f), style = Stroke(kr * 0.08f))
        } else {
            drawArc(c.haar, 180f, 180f, true, Offset(kopf.x - kr * 1.05f, kopf.y - kr * 1.12f), Size(kr * 2.1f, kr * 1.5f))
            drawArc(c.haar, 0f, 100f, true, Offset(kopf.x - kr * 1.0f, kopf.y - kr * 0.9f), Size(kr * 1.2f, kr * 0.8f))
            for (seite in listOf(-1f, 1f)) {
                val auge = kopf + Offset(seite * kr * 0.36f, kr * 0.02f)
                if (blinzeln) drawLine(c.augen, auge - Offset(kr * 0.12f, 0f), auge + Offset(kr * 0.12f, 0f), kr * 0.08f)
                else { drawCircle(Color.White, kr * 0.16f, auge); drawCircle(c.augen, kr * 0.1f, auge + Offset(0f, kr * 0.02f)) }
                drawCircle(Color(0xFFFF8A80).copy(alpha = 0.28f), kr * 0.17f, kopf + Offset(seite * kr * 0.55f, kr * 0.35f))
            }
            drawArc(Color(0xFF8A3B2E), 15f, 150f, lachen > 0.7f, Offset(kopf.x - kr * 0.3f, kopf.y + kr * (0.2f - 0.1f * lachen)), Size(kr * 0.6f, kr * (0.3f + 0.3f * lachen)), style = if (lachen > 0.7f) androidx.compose.ui.graphics.drawscope.Fill else Stroke(kr * 0.08f, cap = StrokeCap.Round))
        }
    }
    return Punkte(handL, handR, kopf, Offset(x, schulterY))
}

internal class KatzenFarben(val fell: Color, val streifen: Color, val bauch: Color, val augen: Color = Color(0xFF9BCB3B))

/**
 * Katze in Seitenansicht. modus: 0 = stehen/laufen ([lauf] = Schrittphase), 1 = sitzen, 2 = schlafen,
 * 3 = strecken/lauern.
 */
internal fun DrawScope.katze(x: Float, boden: Float, s: Float, dir: Float, modus: Int, lauf: Float, t: Float, c: KatzenFarben, hoehe: Float = 0f, pfote: Float = 0f) {
    val b = boden - hoehe
    drawOval(Color.Black.copy(alpha = 0.14f), Offset(x - 0.5f * s, boden - 0.03f * s), Size(s, 0.07f * s))
    val schwanz = sin(t * 3.1f) * 0.18f * s
    when (modus) {
        2 -> {
            drawOval(c.fell, Offset(x - 0.5f * s, b - 0.36f * s), Size(s, 0.36f * s))
            for (i in 0..2) drawArc(c.streifen, 200f, 80f, false, Offset(x - 0.3f * s + i * 0.18f * s, b - 0.36f * s), Size(0.2f * s, 0.3f * s), style = Stroke(0.035f * s))
            val kopf = Offset(x + 0.36f * s * dir, b - 0.16f * s)
            drawCircle(c.fell, 0.19f * s, kopf)
            ohren(kopf, s * 0.85f, dir, c)
            drawArc(c.streifen.dunkler(0.3f), 20f, 140f, false, kopf + Offset(0.02f * s * dir - 0.05f * s, -0.02f * s), Size(0.1f * s, 0.05f * s), style = Stroke(0.02f * s))
            val p = Path().apply { moveTo(x - 0.45f * s * dir, b - 0.08f * s); quadraticTo(x - 0.2f * s * dir, b + 0.02f * s, x + 0.2f * s * dir, b - 0.02f * s) }
            drawPath(p, c.fell.dunkler(0.08f), style = Stroke(0.09f * s, cap = StrokeCap.Round))
            // Zzz
            for (i in 0..2) {
                val z = ((t * 0.5f + i / 3f) % 1f)
                drawZ(kopf + Offset(0.1f * s * dir + z * 0.2f * s * dir, -0.25f * s - z * 0.5f * s), 0.07f * s * (0.6f + z), c.streifen.copy(alpha = 1f - z))
            }
        }
        1 -> {
            val p = Path().apply { moveTo(x - 0.3f * s * dir, b); quadraticTo(x - 0.6f * s * dir, b - 0.05f * s, x - 0.55f * s * dir, b - 0.25f * s + schwanz * 0.4f) }
            drawPath(p, c.fell.dunkler(0.08f), style = Stroke(0.08f * s, cap = StrokeCap.Round))
            drawOval(c.fell, Offset(x - 0.3f * s, b - 0.65f * s), Size(0.55f * s, 0.65f * s))
            drawOval(c.bauch, Offset(x - 0.05f * s + 0.05f * s * dir, b - 0.5f * s), Size(0.18f * s, 0.4f * s))
            for (i in 0..2) drawLine(c.streifen, Offset(x - 0.25f * s * dir, b - 0.55f * s + i * 0.14f * s), Offset(x - 0.12f * s * dir, b - 0.5f * s + i * 0.14f * s), 0.03f * s, StrokeCap.Round)
            // Vorderpfoten, eine hebt sich beim Tatzen
            drawLine(c.fell.heller(0.1f), Offset(x + 0.1f * s * dir, b - 0.3f * s), Offset(x + 0.14f * s * dir, b), 0.08f * s, StrokeCap.Round)
            val tatze = Offset(x + (0.2f + 0.2f * pfote) * s * dir, b - pfote * 0.28f * s)
            drawLine(c.fell.heller(0.1f), Offset(x + 0.04f * s * dir, b - 0.3f * s), tatze, 0.08f * s, StrokeCap.Round)
            val kopf = Offset(x + 0.08f * s * dir, b - 0.78f * s)
            kopf(kopf, s, dir, t, c)
        }
        else -> {
            val strecken = if (modus == 3) 1f else 0f
            val koerperY = b - (0.42f - 0.1f * strecken) * s
            // Schwanz
            val p = Path().apply {
                moveTo(x - 0.45f * s * dir, koerperY)
                cubicTo(x - 0.7f * s * dir, koerperY - 0.1f * s, x - 0.65f * s * dir + schwanz * dir, koerperY - 0.45f * s, x - 0.55f * s * dir + schwanz * dir, koerperY - 0.6f * s)
            }
            drawPath(p, c.fell.dunkler(0.08f), style = Stroke(0.08f * s, cap = StrokeCap.Round))
            // Beine
            for (i in 0 until 4) {
                val vorne = i >= 2
                val bx = x + (if (vorne) 0.3f else -0.3f) * s * dir + (if (i % 2 == 0) -0.05f else 0.05f) * s
                val sw = if (lauf == 0f) 0f else sin(lauf + i * PI.toFloat() * (if (i % 2 == 0) 1f else 0f) + (if (vorne) 0f else PI.toFloat() / 2))
                val fx = bx + sw * 0.12f * s * dir + (if (vorne) strecken * 0.15f * s * dir else 0f)
                drawLine(if (i % 2 == 0) c.fell.dunkler(0.15f) else c.fell, Offset(bx, koerperY + 0.05f * s), Offset(fx, b - maxOf(0f, sw) * 0.05f * s), 0.075f * s, StrokeCap.Round)
            }
            drawOval(c.fell, Offset(x - 0.5f * s, koerperY - 0.16f * s), Size(s, 0.34f * s))
            drawOval(c.bauch, Offset(x - 0.25f * s, koerperY + 0.02f * s), Size(0.5f * s, 0.14f * s))
            for (i in 0..3) drawLine(c.streifen, Offset(x + (-0.3f + i * 0.17f) * s, koerperY - 0.16f * s), Offset(x + (-0.27f + i * 0.17f) * s, koerperY - 0.05f * s), 0.035f * s, StrokeCap.Round)
            val kopf = Offset(x + 0.52f * s * dir, koerperY - (0.2f - 0.12f * strecken) * s)
            kopf(kopf, s, dir, t, c)
        }
    }
}

private fun DrawScope.ohren(kopf: Offset, s: Float, dir: Float, c: KatzenFarben) {
    for (o in listOf(-0.1f, 0.1f)) {
        val basis = kopf + Offset(o * s * dir, -0.13f * s)
        val p = Path().apply {
            moveTo(basis.x - 0.07f * s, basis.y + 0.04f * s)
            lineTo(basis.x + 0.02f * s * dir, basis.y - 0.14f * s)
            lineTo(basis.x + 0.08f * s, basis.y + 0.04f * s); close()
        }
        drawPath(p, c.fell)
        drawPath(Path().apply {
            moveTo(basis.x - 0.035f * s, basis.y + 0.02f * s); lineTo(basis.x + 0.02f * s * dir, basis.y - 0.08f * s); lineTo(basis.x + 0.045f * s, basis.y + 0.02f * s); close()
        }, Color(0xFFF2A7A0))
    }
}

private fun DrawScope.kopf(kopf: Offset, s: Float, dir: Float, t: Float, c: KatzenFarben) {
    ohren(kopf, s, dir, c)
    drawCircle(c.fell, 0.2f * s, kopf)
    drawOval(c.bauch, Offset(kopf.x + 0.02f * s * dir - 0.1f * s, kopf.y + 0.02f * s), Size(0.2f * s, 0.14f * s))
    drawLine(c.streifen, kopf + Offset(-0.03f * s, -0.18f * s), kopf + Offset(0f, -0.1f * s), 0.03f * s, StrokeCap.Round)
    val auge = kopf + Offset(0.09f * s * dir, -0.03f * s)
    if (blinzelt(t, 1.3f)) drawLine(Color(0xFF2B211B), auge - Offset(0.04f * s, 0f), auge + Offset(0.04f * s, 0f), 0.02f * s)
    else {
        drawOval(c.augen, Offset(auge.x - 0.045f * s, auge.y - 0.035f * s), Size(0.09f * s, 0.07f * s))
        drawOval(Color(0xFF1A1410), Offset(auge.x - 0.012f * s, auge.y - 0.032f * s), Size(0.024f * s, 0.064f * s))
    }
    val nase = kopf + Offset(0.18f * s * dir, 0.03f * s)
    drawCircle(Color(0xFFE88A8A), 0.025f * s, nase)
    for (i in -1..1) drawLine(Color.White.copy(alpha = 0.75f), nase + Offset(-0.02f * s * dir, 0.02f * s), nase + Offset(0.16f * s * dir, 0.02f * s + i * 0.05f * s), 0.008f * s)
}

internal fun DrawScope.drawZ(m: Offset, g: Float, farbe: Color) {
    val p = Path().apply { moveTo(m.x - g / 2, m.y - g / 2); lineTo(m.x + g / 2, m.y - g / 2); lineTo(m.x - g / 2, m.y + g / 2); lineTo(m.x + g / 2, m.y + g / 2) }
    drawPath(p, farbe, style = Stroke(g * 0.18f, cap = StrokeCap.Round))
}

internal fun DrawScope.herz(m: Offset, g: Float, farbe: Color) {
    val p = Path().apply {
        moveTo(m.x, m.y + g * 0.35f)
        cubicTo(m.x - g, m.y - g * 0.3f, m.x - g * 0.4f, m.y - g * 0.9f, m.x, m.y - g * 0.35f)
        cubicTo(m.x + g * 0.4f, m.y - g * 0.9f, m.x + g, m.y - g * 0.3f, m.x, m.y + g * 0.35f)
        close()
    }
    drawPath(p, farbe)
}

internal fun DrawScope.haken(m: Offset, g: Float, fort: Float, farbe: Color, breite: Float) {
    if (fort <= 0f) return
    val a = m + Offset(-g * 0.45f, 0f)
    val b = m + Offset(-g * 0.12f, g * 0.32f)
    val c = m + Offset(g * 0.5f, -g * 0.42f)
    val p = Path().apply {
        moveTo(a.x, a.y)
        val f1 = (fort / 0.4f).coerceAtMost(1f)
        lineTo(a.x + (b.x - a.x) * f1, a.y + (b.y - a.y) * f1)
        if (fort > 0.4f) { val f2 = (fort - 0.4f) / 0.6f; lineTo(b.x + (c.x - b.x) * f2, b.y + (c.y - b.y) * f2) }
    }
    drawPath(p, farbe, style = Stroke(breite, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

/** Handschrift-Kringel als Platzhalter für Text, wird von links nach rechts geschrieben. */
internal fun DrawScope.kringel(start: Offset, laenge: Float, hoehe: Float, fort: Float, farbe: Color, breite: Float, samen: Int) {
    if (fort <= 0f) return
    val p = Path()
    p.moveTo(start.x, start.y)
    var x = 0f
    val ende = laenge * fort
    while (x < ende) {
        val y = sin(x / hoehe * 2.3f + samen) * hoehe * 0.45f + sin(x / hoehe * 0.7f + samen * 2) * hoehe * 0.15f
        p.lineTo(start.x + x, start.y + y)
        x += 2f
    }
    drawPath(p, farbe, style = Stroke(breite, cap = StrokeCap.Round))
}
