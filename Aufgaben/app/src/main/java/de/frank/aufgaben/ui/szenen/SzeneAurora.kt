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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import de.frank.aufgaben.ui.theme.Farben
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Ein Strich auf der Tafel: Zeitraum und Position des Stifts bei Fortschritt 0..1 (in flachen Tafel-Koordinaten). */
private class Strich(val start: Float, val dauer: Float, val punkt: (Float) -> Offset, val zeichne: DrawScope.(Float) -> Unit)

/**
 * Aurora: über die Schulter an die Tafel geschaut, die Tafel steht leicht schräg im Raum. Nachdenken, eine Mindmap
 * skizzieren, eine Liste schreiben und abhaken, zurücktreten, sich umdrehen und freuen. Die Katze jagt die
 * heruntergefallene Stiftkappe. Zeiten passen zu `ablauf()` in Szene.kt (36 s).
 */
internal fun DrawScope.szeneAurora(t: Float, f: Farben) {
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.95f
    val s = h * 0.78f
    val stift = f.primaer.dunkler(0.1f)
    // Blick von links: Fluchtpunkt links der Mitte, die Wandkante steigt nach rechts leicht an.
    val flucht = Offset(w * 0.3f, h * 0.34f)
    fun kante(x: Float) = h * 0.82f - h * 0.04f * x / w

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
        val wk = -70f + i * 23f + sin(t * 1.1f + i) * 4f
        rotate(wk, Offset(w * 0.015f + h * 0.045f, h * 0.68f)) { drawOval(Color(0xFF6E9E7A), Offset(w * 0.015f + h * 0.03f, h * 0.48f), Size(h * 0.03f, h * 0.2f)) }
    }
    val uhr = Offset(w * 0.955f, h * 0.18f)
    drawCircle(Color.Black.copy(alpha = 0.1f), h * 0.06f, uhr + Offset(3f, 4f))
    drawCircle(if (d) Color(0xFF2A2548) else Color.White, h * 0.06f, uhr)
    drawCircle(f.primaer, h * 0.06f, uhr, style = Stroke(3f))
    rotate(t * 25f, uhr) { drawLine(f.text, uhr, uhr - Offset(0f, h * 0.045f), 2f) }
    rotate(t * 3f, uhr) { drawLine(f.text, uhr, uhr - Offset(0f, h * 0.03f), 3f, StrokeCap.Round) }

    // Tafel: flach entworfen, perspektivisch auf die Wand gesetzt (linke Kante näher, also größer).
    val tafel = Rect(w * 0.08f, h * 0.05f, w * 0.9f, h * 0.66f)
    val ecken = listOf(Offset(w * 0.08f, h * 0.035f), Offset(w * 0.9f, h * 0.085f), Offset(w * 0.9f, h * 0.625f), Offset(w * 0.08f, h * 0.675f))
    val m = perspektive(tafel, ecken)
    flaeche(ecken.map { it + Offset(5f, 7f) }, Color.Black.copy(alpha = 0.12f))
    // Sichtbare linke Stirnseite der Tafel
    flaeche(listOf(ecken[0], ecken[3], ecken[3] + Offset(-w * 0.012f, -h * 0.004f), ecken[0] + Offset(-w * 0.012f, h * 0.006f)), Color(0xFF8E8AA3))
    val flaecheT = Rect(tafel.left + 6f, tafel.top + 6f, tafel.right - 6f, tafel.bottom - 6f)

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
    striche += Strich(5.4f, 1.6f, { p -> mm + Offset(cos(p * 2 * PI.toFloat() - PI.toFloat() / 2) * r, sin(p * 2 * PI.toFloat() - PI.toFloat() / 2) * r) }) { p ->
        drawArc(stift, -90f, 360f * p, false, mm - Offset(r, r), Size(r * 2, r * 2), style = Stroke(4f, cap = StrokeCap.Round))
    }
    knoten.forEachIndexed { i, n ->
        val von = mm + (n - mm) * (r / (n - mm).getDistance())
        striche += Strich(7.2f + i * 1.2f, 0.6f, { p -> von + (n - von) * p }) { p ->
            drawLine(stift, von, von + (n - von) * p, 3f, StrokeCap.Round)
            if (p >= 1f) drawCircle(f.primaer.copy(alpha = 0.25f), flaecheT.height * 0.06f, n)
            if (p >= 1f) drawCircle(stift, flaecheT.height * 0.06f, n, style = Stroke(3f))
        }
        // Wort neben dem Knoten, gleich danach mit demselben Stift
        val wort = n + Offset(flaecheT.height * 0.09f, 0f)
        val lang = flaecheT.width * 0.08f
        striche += Strich(7.85f + i * 1.2f, 0.45f, { p -> wort + Offset(lang * p, 0f) }) { p ->
            kringel(wort, lang, flaecheT.height * 0.05f, p, stift.copy(alpha = 0.7f), 2f, i)
        }
    }
    zeilen.forEachIndexed { i, y ->
        striche += Strich(12.8f + i * 1.6f, 1.3f, { p -> Offset(listeX + k * 1.6f + flaecheT.width * (0.26f - (i % 2) * 0.05f) * p, y) }) { p ->
            drawRect(stift, Offset(listeX, y - k / 2), Size(k, k), style = Stroke(3f))
            kringel(Offset(listeX + k * 1.6f, y), flaecheT.width * (0.26f - (i % 2) * 0.05f), k * 0.8f, p, stift, 3f, i + 3)
        }
    }
    zeilen.forEachIndexed { i, y ->
        striche += Strich(20.2f + i * 1.1f, 0.5f, { p -> Offset(listeX + k * (0.1f + 0.9f * p), y + k * (0.3f - 0.8f * p)) }) { p ->
            haken(Offset(listeX + k / 2, y), k * 1.1f, p, f.primaer, 4f)
        }
    }
    striche.sortBy { it.start }

    // Kappe: wackelt, fällt von der Ablage, rollt; später schubst die Katze sie weiter.
    val kappeFlach = Offset(tafel.left + tafel.width * 0.62f, tafel.bottom)
    val kappeStart = m.abbilden(kappeFlach)
    val kappeFaellt = t >= 15f

    verzerrt(m) {
        drawRoundRect(Color(0xFFB5B2C4), tafel.topLeft, tafel.size, CornerRadius(10f))
        drawRoundRect(if (d) Color(0xFFE6E3F1) else Color.White, flaecheT.topLeft, flaecheT.size, CornerRadius(6f))
        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0f), Color(0xFF8E86B8).copy(alpha = 0.08f)), flaecheT.topLeft, flaecheT.bottomRight), flaecheT.topLeft, flaecheT.size, CornerRadius(6f))
        drawLine(Color.White.copy(alpha = 0.6f), Offset(flaecheT.left + flaecheT.width * 0.62f, flaecheT.top + 8f), Offset(flaecheT.left + flaecheT.width * 0.8f, flaecheT.top + 8f), 6f, StrokeCap.Round)
        // Ablage mit Stiften
        drawRect(Color(0xFF9E9AB2), Offset(tafel.left + tafel.width * 0.1f, tafel.bottom), Size(tafel.width * 0.8f, h * 0.02f))
        for (i in 0 until 3) {
            val farbe = listOf(stift, Color(0xFF55506B), f.primaer.heller(0.35f))[i]
            drawRoundRect(farbe, Offset(tafel.left + tafel.width * (0.55f + i * 0.05f), tafel.bottom - h * 0.018f), Size(h * 0.08f, h * 0.018f), CornerRadius(4f))
        }
        if (!kappeFaellt) rotate(sin(t * 18f) * 10f * an(t, 14.3f, 0.7f), kappeFlach) {
            drawRoundRect(stift, kappeFlach - Offset(h * 0.018f, h * 0.024f), Size(h * 0.036f, h * 0.022f), CornerRadius(5f))
        }
        striche.forEach { st -> val p = an(t, st.start, st.dauer); if (p > 0f) st.zeichne(this, p) }
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

    // Person: Grundweg in weichen Schritten, beim Schreiben ein Stück dem Stift nach (steht links davon, schreibt rechts).
    val wege = listOf(0f to 0.5f, 4.6f to 0.5f, 5.3f to 0.3f, 11.4f to 0.3f, 12.4f to 0.66f, 24.4f to 0.66f, 25.2f to 0.5f)
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
    val personX = b + ((stiftPunkt.x - 0.1f * s) - b).coerceIn(-w * 0.14f, w * 0.14f) * 0.8f * schreibt
    val geht = abs(basis(t + 0.1f) - b) > w * 0.002f
    val mf = MenschFarben(Color(0xFFE2B893), Color(0xFF2E2230), lerp(f.primaer, Color(0xFF6E6680), 0.3f), Color(0xFF30334A), Color(0xFF22212B))
    val blink = blinzelt(t)
    // Umdrehen: schmal werden, Seite wechseln, wieder breit werden
    val dreh = an(t, 25.4f, 0.8f)
    val vonHinten = dreh < 0.5f
    val breite = abs(cos(dreh * PI.toFloat())).coerceAtLeast(0.06f)
    val hub = huepfen(t, 26.6f, 30.2f, 0.08f * s)
    val restL = Offset(personX - 0.15f * s, boden - 0.47f * s - hub)
    val restR = Offset(personX + 0.15f * s, boden - 0.47f * s - hub)
    val denken = weich(an(t, 0f, 0.6f)) * (1f - weich(an(t, 4.0f, 0.6f)))
    val jubel = weich(an(t, 26.2f, 0.6f)) * (1f - weich(an(t, 30.0f, 0.8f)))
    val zielL = if (vonHinten) lerp(restL, Offset(personX - 0.06f * s, boden - 0.9f * s), denken)
    else lerp(restL, Offset(personX - 0.28f * s, boden - 1.08f * s - hub), jubel)
    val zielR = if (vonHinten) lerp(restR, stiftPunkt, schreibt)
    else lerp(restR, Offset(personX + 0.28f * s, boden - 1.08f * s - hub), jubel)
    scale(breite, 1f, Offset(personX, boden)) {
        menschFront(
            personX, boden, s, mf, vonHinten = vonHinten,
            zielL = zielL, zielR = zielR,
            sprung = hub, kopfNeigung = sin(t * 1.4f) * 8f * denken + 4f * weich(an(t, 24.4f, 0.5f)) * (1f - weich(an(t, 25.3f, 0.3f))),
            blinzeln = blink, lachen = if (t < 30.2f) 1f else 0.75f, schritt = if (geht) t * 9f else 0f,
        )
    }
    // Gedankenblase beim Nachdenken
    val blase = weich(an(t, 0.6f, 0.8f)) * (1f - weich(an(t, 3.8f, 0.6f)))
    if (blase > 0f) {
        for ((dx, dy, rr) in listOf(Triple(0.1f, 0.98f, 0.016f), Triple(0.14f, 1.04f, 0.022f), Triple(0.22f, 1.13f, 0.045f))) {
            val c = Offset(personX + dx * s, boden - dy * s)
            drawCircle(lerp(Color.White, f.primaer, 0.12f).copy(alpha = 0.95f * blase), h * rr, c)
            drawCircle(f.primaer.copy(alpha = 0.55f * blase), h * rr, c, style = Stroke(1.5f))
        }
        drawCircle(f.primaer.copy(alpha = blase), h * 0.012f + h * 0.004f * sin(t * 4f), Offset(personX + 0.22f * s, boden - 1.13f * s))
    }

    // Stiftkappe fällt, rollt, die Katze schubst sie weiter
    val kappe1 = Offset(kappeStart.x - w * 0.15f, boden)
    val kappe2 = Offset(kappe1.x - w * 0.1f, boden)
    val kappe: Offset = when {
        !kappeFaellt -> kappeStart
        t < 15.6f -> { val q = an(t, 15f, 0.6f); Offset(kappeStart.x - q * w * 0.03f, kappeStart.y + q * q * (boden - kappeStart.y)) }
        t < 18.6f -> Offset(mix(kappeStart.x - w * 0.03f, kappe1.x, weich(an(t, 15.6f, 1.4f))), boden)
        else -> Offset(mix(kappe1.x, kappe2.x, weich(an(t, 18.6f, 0.8f))), boden)
    }
    if (kappeFaellt) {
        val drehung = if (t < 17f) (t - 15f) * 400f else if (t in 18.6f..19.4f) 800f + (t - 18.6f) * 500f else if (t < 18.6f) 800f else 1200f
        rotate(drehung, kappe - Offset(0f, h * 0.012f)) {
            drawRoundRect(stift, kappe - Offset(h * 0.018f, h * 0.024f), Size(h * 0.036f, h * 0.022f), CornerRadius(5f))
        }
    }
    val katzeF = KatzenFarben(Color(0xFF2E2A33), Color(0xFF1C1A20), Color(0xFFF2EFF5), Color(0xFFE8C547))
    val cs = h * 0.18f
    val abstand = cs * 0.8f
    val pfote = { start: Float -> maxOf(0f, sin((t - start) * 7f)) * weich(an(t, start, 0.3f)) }
    when {
        t < 1f -> Unit
        t < 6f -> katze(mix(w * 1.08f, w * 0.9f, weich(an(t, 1f, 5f))), boden, cs, -1f, 0, t * 10f, t, katzeF)
        t < 15.2f -> katze(w * 0.9f, boden, cs, -1f, 1, 0f, t, katzeF)
        t < 16f -> katze(w * 0.9f, boden, cs, -1f, 3, 0f, t, katzeF)
        t < 17.2f -> { val q = an(t, 16f, 1.2f); katze(mix(w * 0.9f, kappe1.x + abstand, weich(q)), boden, cs, -1f, 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.12f) }
        t < 18.9f -> katze(kappe1.x + abstand, boden, cs, -1f, 1, 0f, t, katzeF, pfote = if (t < 18.6f) pfote(17.6f) else 0f)
        t < 20f -> katze(mix(kappe1.x + abstand, kappe2.x + abstand, weich(an(t, 18.9f, 1.1f))), boden, cs, -1f, 0, t * 10f, t, katzeF)
        else -> katze(kappe2.x + abstand, boden, cs, -1f, 1, 0f, t, katzeF, pfote = if (t < 21.6f) pfote(20.3f) else 0f)
    }
}
