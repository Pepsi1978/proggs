package de.frank.aufgaben.ui.szenen

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import de.frank.aufgaben.ui.theme.Farben
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Länge eines Durchlaufs; Ende und Anfang sind dasselbe Bild (dunkler Raum, leere Tafel), die Szene läuft ohne Schnitt. */
internal const val AURORA_ZYKLUS = 46f

/** Ein Strich auf der Tafel: Zeitraum und Position des Stifts bei Fortschritt 0..1 (in flachen Tafel-Koordinaten). */
private class Strich(val start: Float, val dauer: Float, val punkt: (Float) -> Offset, val zeichne: DrawScope.(Float) -> Unit)

/**
 * Aurora: über die Schulter an die Tafel geschaut, die Tafel steht leicht schräg im Raum. Im Dunkeln kommen Person und
 * Katze (mit ihrem Wollknäuel im Maul) links herein, das Licht geht an. Nachdenken, eine Mindmap skizzieren, eine
 * Liste schreiben und abhaken, mit einem Sprung zum Betrachter drehen und freuen, mit einem Sprung zurück, die Tafel
 * sauber wischen und mit der Katze rechts hinausgehen; das Licht geht aus, nach einer Pause beginnt alles von vorn.
 */
internal fun DrawScope.szeneAurora(t: Float, f: Farben) {
    val zyklus = AURORA_ZYKLUS
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.95f
    val s = h * 0.78f
    val stift = f.primaer.dunkler(0.1f)
    // Blick von links: Fluchtpunkt links der Mitte, die Wandkante steigt nach rechts leicht an.
    val flucht = Offset(w * 0.3f, h * 0.34f)
    fun kante(x: Float) = h * 0.82f - h * 0.04f * x / w
    // Licht im Raum: geht beim Hereinkommen an und beim Hinausgehen aus.
    val licht = weich(an(t, 2.5f, 1.5f)) * (1f - weich(an(t, 42.6f, 1.6f)))

    // Wand und Boden
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF17142B), Color(0xFF0F0D1E)) else listOf(Color(0xFFF1EEFB), Color(0xFFE2DCF6))))
    val bodenFarbe = if (d) Color(0xFF1C1833) else Color(0xFFD3CBEF)
    flaeche(listOf(Offset(0f, kante(0f)), Offset(w, kante(w)), Offset(w, h), Offset(0f, h)), Brush.verticalGradient(listOf(bodenFarbe.dunkler(0.06f), bodenFarbe.heller(0.08f)), kante(w), h))
    for (i in -6..14) {
        val x = i * w / 8f
        val y = kante(x)
        drawLine(Color.Black.copy(alpha = if (d) 0.18f else 0.06f), Offset(x, y), Offset(x + (x - flucht.x) * (h - y) / (y - flucht.y), h), 1.5f)
    }
    drawLine(Color.Black.copy(alpha = 0.12f), Offset(0f, kante(0f)), Offset(w, kante(w)), 2f)
    // Pflanze links, Uhr rechts
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(w * 0.01f, h * 0.8f), Size(h * 0.11f, h * 0.025f))
    drawRoundRect(Color(0xFFB9A48F), Offset(w * 0.015f, h * 0.68f), Size(h * 0.09f, h * 0.13f), CornerRadius(6f))
    drawRoundRect(Color(0xFF9C8873), Offset(w * 0.015f + h * 0.065f, h * 0.68f), Size(h * 0.025f, h * 0.13f), CornerRadius(6f))
    for (i in 0 until 7) {
        val wk = -70f + i * 23f + welle(t, zyklus, 8, i.toFloat()) * 4f
        rotate(wk, Offset(w * 0.015f + h * 0.045f, h * 0.68f)) { drawOval(Color(0xFF6E9E7A), Offset(w * 0.015f + h * 0.03f, h * 0.48f), Size(h * 0.03f, h * 0.2f)) }
    }
    val uhr = Offset(w * 0.955f, h * 0.18f)
    drawCircle(Color.Black.copy(alpha = 0.1f), h * 0.06f, uhr + Offset(3f, 4f))
    drawCircle(if (d) Color(0xFF2A2548) else Color.White, h * 0.06f, uhr)
    drawCircle(f.primaer, h * 0.06f, uhr, style = Stroke(3f))
    rotate(runde(t, zyklus, 3) * 360f, uhr) { drawLine(f.text, uhr, uhr - Offset(0f, h * 0.045f), 2f) }
    rotate(runde(t, zyklus, 1) * 360f, uhr) { drawLine(f.text, uhr, uhr - Offset(0f, h * 0.03f), 3f, StrokeCap.Round) }

    // Tafel: flach entworfen, perspektivisch auf die Wand gesetzt (linke Kante näher, also größer).
    val tafel = Rect(w * 0.08f, h * 0.05f, w * 0.9f, h * 0.66f)
    val ecken = listOf(Offset(w * 0.08f, h * 0.035f), Offset(w * 0.9f, h * 0.085f), Offset(w * 0.9f, h * 0.625f), Offset(w * 0.08f, h * 0.675f))
    val m = perspektive(tafel, ecken)
    flaeche(ecken.map { it + Offset(5f, 7f) }, Color.Black.copy(alpha = 0.12f))
    // Sichtbare linke Stirnseite der Tafel
    flaeche(listOf(ecken[0], ecken[3], ecken[3] + Offset(-w * 0.012f, -h * 0.004f), ecken[0] + Offset(-w * 0.012f, h * 0.006f)), Color(0xFF8E8AA3))
    val flaecheT = Rect(tafel.left + 6f, tafel.top + 6f, tafel.right - 6f, tafel.bottom - 6f)
    val tafelWeiss = if (d) Color(0xFFE6E3F1) else Color.White

    // Die Striche
    val mm = Offset(flaecheT.left + flaecheT.width * 0.24f, flaecheT.top + flaecheT.height * 0.45f)
    val r = flaecheT.height * 0.14f
    val knoten = listOf(
        Offset(flaecheT.left + flaecheT.width * 0.08f, flaecheT.top + flaecheT.height * 0.18f),
        Offset(flaecheT.left + flaecheT.width * 0.42f, flaecheT.top + flaecheT.height * 0.16f),
        Offset(flaecheT.left + flaecheT.width * 0.1f, flaecheT.top + flaecheT.height * 0.8f),
        Offset(flaecheT.left + flaecheT.width * 0.4f, flaecheT.top + flaecheT.height * 0.78f),
    )
    val listeX = flaecheT.left + flaecheT.width * 0.58f
    val zeilen = List(4) { flaecheT.top + flaecheT.height * (0.2f + it * 0.2f) }
    val k = flaecheT.height * 0.09f
    val striche = mutableListOf<Strich>()
    striche += Strich(11.4f, 1.6f, { p -> mm + Offset(cos(p * 2 * PI.toFloat() - PI.toFloat() / 2) * r, sin(p * 2 * PI.toFloat() - PI.toFloat() / 2) * r) }) { p ->
        drawArc(stift, -90f, 360f * p, false, mm - Offset(r, r), Size(r * 2, r * 2), style = Stroke(4f, cap = StrokeCap.Round))
    }
    knoten.forEachIndexed { i, n ->
        val von = mm + (n - mm) * (r / (n - mm).getDistance())
        striche += Strich(13.2f + i * 1.2f, 0.6f, { p -> von + (n - von) * p }) { p ->
            drawLine(stift, von, von + (n - von) * p, 3f, StrokeCap.Round)
            if (p >= 1f) drawCircle(f.primaer.copy(alpha = 0.25f), flaecheT.height * 0.06f, n)
            if (p >= 1f) drawCircle(stift, flaecheT.height * 0.06f, n, style = Stroke(3f))
        }
        // Wort neben dem Knoten, gleich danach mit demselben Stift
        val wort = n + Offset(flaecheT.height * 0.09f, 0f)
        val lang = flaecheT.width * 0.08f
        striche += Strich(13.85f + i * 1.2f, 0.45f, { p -> wort + Offset(lang * p, 0f) }) { p ->
            kringel(wort, lang, flaecheT.height * 0.05f, p, stift.copy(alpha = 0.7f), 2f, i)
        }
    }
    zeilen.forEachIndexed { i, y ->
        striche += Strich(18.8f + i * 1.6f, 1.3f, { p -> Offset(listeX + k * 1.6f + flaecheT.width * (0.26f - (i % 2) * 0.05f) * p, y) }) { p ->
            drawRect(stift, Offset(listeX, y - k / 2), Size(k, k), style = Stroke(3f))
            kringel(Offset(listeX + k * 1.6f, y), flaecheT.width * (0.26f - (i % 2) * 0.05f), k * 0.8f, p, stift, 3f, i + 3)
        }
    }
    zeilen.forEachIndexed { i, y ->
        striche += Strich(26.2f + i * 1.1f, 0.5f, { p -> Offset(listeX + k * (0.1f + 0.9f * p), y + k * (0.3f - 0.8f * p)) }) { p ->
            haken(Offset(listeX + k / 2, y), k * 1.1f, p, f.primaer, 4f)
        }
    }
    // Wischen: der Schwamm fährt in Schleifen von links nach rechts über die ganze Tafel
    val wischStart = 38.6f
    val wischDauer = 3f
    striche += Strich(wischStart, wischDauer, { p -> Offset(flaecheT.left + flaecheT.width * p, flaecheT.top + flaecheT.height * (0.5f + 0.36f * sin(p * PI.toFloat() * 14f))) }) { }
    striche.sortBy { it.start }

    verzerrt(m) {
        drawRoundRect(Color(0xFFB5B2C4), tafel.topLeft, tafel.size, CornerRadius(10f))
        drawRoundRect(tafelWeiss, flaecheT.topLeft, flaecheT.size, CornerRadius(6f))
        striche.forEach { st -> val p = an(t, st.start, st.dauer); if (p > 0f) st.zeichne(this, p) }
        // Was der Schwamm schon überstrichen hat, ist wieder weiß
        val wisch = an(t, wischStart, wischDauer)
        if (wisch > 0f) drawRect(tafelWeiss, flaecheT.topLeft, Size((flaecheT.width * wisch + k).coerceAtMost(flaecheT.width), flaecheT.height))
        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0f), Color(0xFF8E86B8).copy(alpha = 0.08f)), flaecheT.topLeft, flaecheT.bottomRight), flaecheT.topLeft, flaecheT.size, CornerRadius(6f))
        drawLine(Color.White.copy(alpha = 0.6f), Offset(flaecheT.left + flaecheT.width * 0.62f, flaecheT.top + 8f), Offset(flaecheT.left + flaecheT.width * 0.8f, flaecheT.top + 8f), 6f, StrokeCap.Round)
        // Ablage mit Stiften
        drawRect(Color(0xFF9E9AB2), Offset(tafel.left + tafel.width * 0.1f, tafel.bottom), Size(tafel.width * 0.8f, h * 0.02f))
        for (i in 0 until 3) {
            val farbe = listOf(stift, Color(0xFF55506B), f.primaer.heller(0.35f))[i]
            drawRoundRect(farbe, Offset(tafel.left + tafel.width * (0.55f + i * 0.05f), tafel.bottom - h * 0.018f), Size(h * 0.08f, h * 0.018f), CornerRadius(4f))
        }
    }

    // Wo ist der Stift? Zwischen zwei Strichen gleitet er durch die Luft, am Anfang und Ende kommt die Hand weich dazu.
    val (stiftPunkt, schreibt) = run {
        val laeuft = striche.firstOrNull { t >= it.start && t <= it.start + it.dauer }
        if (laeuft != null) return@run m.abbilden(laeuft.punkt(an(t, laeuft.start, laeuft.dauer))) to 1f
        val vor = striche.lastOrNull { it.start + it.dauer < t }
        val nach = striche.firstOrNull { it.start > t }
        val ende = vor?.let { it.start + it.dauer } ?: 0f
        val pa = vor?.let { m.abbilden(it.punkt(1f)) }
        val pb = nach?.let { m.abbilden(it.punkt(0f)) }
        if (pa != null && pb != null && nach.start - ende < 1.6f) return@run lerp(pa, pb, weich((t - ende) / (nach.start - ende))) to 1f
        val raus = if (pa != null) 1f - weich(an(t, ende, 0.5f)) else 0f
        val rein = if (pb != null && nach != null) weich(an(t, nach.start - 0.5f, 0.5f)) else 0f
        if (raus >= rein) (pa ?: Offset.Zero) to raus else (pb ?: Offset.Zero) to rein
    }

    // Person: Grundweg in weichen Schritten, beim Schreiben und Wischen ein Stück dem Stift nach (steht links davon).
    val wege = listOf(
        0f to -0.18f, 2.5f to -0.18f, 6.5f to 0.5f, 10.6f to 0.5f, 11.3f to 0.3f, 17.4f to 0.3f, 18.4f to 0.66f,
        30.4f to 0.66f, 31.2f to 0.5f, 37.4f to 0.5f, 38.5f to 0.06f, 41.6f to 0.87f, 41.8f to 0.87f, 44.6f to 1.18f,
    )
    fun basis(z: Float): Float {
        var x = wege.first().second
        for (i in 1 until wege.size) {
            val (t0, x0) = wege[i - 1]
            val (t1, x1) = wege[i]
            if (z >= t0) x = mix(x0, x1, weich(an(z, t0, t1 - t0)))
        }
        return x * w
    }
    val b = basis(t)
    val personX = b + ((stiftPunkt.x - 0.2f * s) - b).coerceIn(-w * 0.16f, w * 0.16f) * 0.85f * schreibt
    val geht = abs(basis(t + 0.1f) - b) > w * 0.002f
    val mf = MenschFarben(Color(0xFFE2B893), Color(0xFF2E2230), lerp(f.primaer, Color(0xFF6E6680), 0.3f), Color(0xFF30334A), Color(0xFF22212B))
    val blink = blinzelt(t)
    // Umdrehen per Sprung: hoch, in der Luft die Seite wechseln, landen
    fun sprung(start: Float) = sin(PI.toFloat() * an(t, start, 0.7f)) * 0.14f * s
    val vonHinten = t < 31.85f || t >= 36.95f
    val hub = sprung(31.5f) + sprung(36.6f) + huepfen(t, 32.6f, 36.2f, 0.08f * s)
    val restL = Offset(personX - 0.15f * s, boden - 0.47f * s - hub)
    val restR = Offset(personX + 0.15f * s, boden - 0.47f * s - hub)
    val denken = weich(an(t, 6.5f, 0.6f)) * (1f - weich(an(t, 10.0f, 0.6f)))
    val jubel = weich(an(t, 32.3f, 0.6f)) * (1f - weich(an(t, 35.9f, 0.6f)))
    val zielL = if (vonHinten) lerp(restL, Offset(personX - 0.06f * s, boden - 0.9f * s), denken)
    else lerp(restL, Offset(personX - 0.26f * s, boden - 1.06f * s - hub), jubel)
    val zielR = if (vonHinten) lerp(restR, stiftPunkt, schreibt)
    else lerp(restR, Offset(personX + 0.26f * s, boden - 1.06f * s - hub), jubel)
    val punkte = menschFront(
        personX, boden, s, mf, vonHinten = vonHinten,
        zielL = zielL, zielR = zielR,
        sprung = hub, kopfNeigung = sin(t * 1.4f) * 8f * denken + 4f * weich(an(t, 30.4f, 0.5f)) * (1f - weich(an(t, 31.3f, 0.3f))),
        blinzeln = blink, lachen = if (t < 36.2f) 1f else 0.75f, schritt = if (geht) t * 9f else 0f,
    )
    // Schwamm in der Hand beim Wischen
    val schwamm = weich(an(t, wischStart - 0.6f, 0.4f)) * (1f - weich(an(t, wischStart + wischDauer + 0.1f, 0.3f)))
    if (schwamm > 0f) drawRoundRect(Color(0xFFF2C14E).copy(alpha = schwamm), punkte.handR - Offset(h * 0.025f, h * 0.018f), Size(h * 0.05f, h * 0.036f), CornerRadius(5f))
    // Gedankenblase beim Nachdenken
    val blase = weich(an(t, 7.1f, 0.8f)) * (1f - weich(an(t, 9.9f, 0.6f)))
    if (blase > 0f) {
        for ((dx, dy, rr) in listOf(Triple(0.1f, 0.98f, 0.016f), Triple(0.14f, 1.04f, 0.022f), Triple(0.22f, 1.13f, 0.045f))) {
            val c = Offset(personX + dx * s, boden - dy * s)
            drawCircle(lerp(Color.White, f.primaer, 0.12f).copy(alpha = 0.95f * blase), h * rr, c)
            drawCircle(f.primaer.copy(alpha = 0.55f * blase), h * rr, c, style = Stroke(1.5f))
        }
        drawCircle(f.primaer.copy(alpha = blase), h * 0.012f + h * 0.004f * sin(t * 4f), Offset(personX + 0.22f * s, boden - 1.13f * s))
    }

    // Katze mit Wollknäuel: bringt es mit, legt es ab, spielt, nimmt es beim Gehen wieder mit
    val katzeF = KatzenFarben(Color(0xFF2E2A33), Color(0xFF1C1A20), Color(0xFFF2EFF5), Color(0xFFE8C547))
    val cs = h * 0.18f
    val kr = h * 0.024f
    val wolle = f.primaer.heller(0.25f)
    val platz1 = w * 0.9f
    val ball1 = Offset(katzenMaul(platz1, boden, cs, -1f, 1).x, boden - kr)
    val ball2 = Offset(ball1.x - w * 0.14f, boden - kr)
    val platz2 = ball2.x + 0.24f * cs
    val tatzen = { start: Float, ende: Float -> maxOf(0f, sin((t - start) * 7f)) * weich(an(t, start, 0.3f)) * (1f - weich(an(t, ende - 0.3f, 0.3f))) }
    when {
        t < 2.5f -> Unit
        t < 7f -> {
            val dir = mix(1f, -1f, weich(an(t, 6.4f, 0.6f)))
            val x = mix(-0.25f * w, platz1, weich(an(t, 2.5f, 4.5f)))
            katze(x, boden, cs, dir, 0, t * 10f, t, katzeF)
            knaeuel(katzenMaul(x, boden, cs, dir, 0), kr, wolle)
        }
        t < 21.5f -> {
            katze(platz1, boden, cs, -1f, 1, 0f, t, katzeF, pfote = if (t in 19f..20.8f) tatzen(19f, 20.8f) else 0f)
            // Knäuel fällt aus dem Maul, liegt, rollt nach dem Tatzen davon
            val fall = an(t, 7f, 0.35f)
            val start = katzenMaul(platz1, boden, cs, -1f, 1)
            val pos = when {
                fall < 1f -> Offset(start.x, mix(start.y, ball1.y, fall * fall))
                t < 20.6f -> ball1
                else -> lerp(ball1, ball2, weich(an(t, 20.6f, 1.2f)))
            }
            rotate(if (t > 20.6f) -weich(an(t, 20.6f, 1.2f)) * 540f else 0f, pos) { knaeuel(pos, kr, wolle) }
        }
        t < 22.6f -> {
            katze(mix(platz1, platz2, weich(an(t, 21.5f, 1.1f))), boden, cs, -1f, 0, t * 10f, t, katzeF)
            rotate(-540f, ball2) { knaeuel(ball2, kr, wolle) }
        }
        t < 41.2f -> {
            katze(platz2, boden, cs, -1f, 1, 0f, t, katzeF, pfote = if (t in 27f..28.6f) tatzen(27f, 28.6f) else 0f)
            val maul = katzenMaul(platz2, boden, cs, -1f, 1)
            knaeuel(lerp(ball2, maul, weich(an(t, 40.8f, 0.4f))), kr, wolle)
        }
        else -> {
            // Umdrehen und mit dem Knäuel im Maul hinter der Person her hinaus
            val dir = mix(-1f, 1f, weich(an(t, 41.2f, 0.5f)))
            val modus = if (t < 41.7f) 1 else 0
            val x = mix(platz2, 1.25f * w, an(t, 41.7f, 3.1f))
            katze(x, boden, cs, dir, modus, t * 10f, t, katzeF)
            knaeuel(katzenMaul(x, boden, cs, dir, modus), kr, wolle)
        }
    }

    // Raumlicht: im Dunkeln liegt ein bläulicher Schleier über allem
    drawRect(Color(0xFF0B0820).copy(alpha = 0.62f * (1f - licht)))
}
