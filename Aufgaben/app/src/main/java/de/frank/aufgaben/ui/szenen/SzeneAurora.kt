package de.frank.aufgaben.ui.szenen

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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import de.frank.aufgaben.ui.theme.Farben
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Ein Strich auf der Tafel: Zeitraum und Position des Stifts bei Fortschritt 0..1. */
private class Strich(val start: Float, val dauer: Float, val punkt: (Float) -> Offset, val zeichne: DrawScope.(Float) -> Unit)

/**
 * Aurora: über die Schulter an die Tafel geschaut. Nachdenken, eine Mindmap skizzieren, eine Liste
 * schreiben und abhaken, dann umdrehen und jubeln. Die Katze jagt die heruntergefallene Stiftkappe.
 */
internal fun DrawScope.szeneAurora(t: Float, f: Farben) {
    val w = size.width
    val h = size.height
    val d = f.dunkel
    val boden = h * 0.95f
    val s = h * 0.78f
    val stift = f.primaer.dunkler(0.1f)
    val weg = 1f - an(t, 22.4f, 1.4f)

    // Wand und Boden
    drawRect(Brush.verticalGradient(if (d) listOf(Color(0xFF17142B), Color(0xFF0F0D1E)) else listOf(Color(0xFFF1EEFB), Color(0xFFE2DCF6))))
    drawRect(if (d) Color(0xFF1C1833) else Color(0xFFD3CBEF), Offset(0f, h * 0.8f), Size(w, h * 0.2f))
    // Pflanze links, Uhr rechts
    drawRoundRect(Color(0xFFB9A48F), Offset(w * 0.015f, h * 0.68f), Size(h * 0.09f, h * 0.13f), CornerRadius(6f))
    for (i in 0 until 7) {
        val wk = -70f + i * 23f + sin(t * 1.1f + i) * 4f
        rotate(wk, Offset(w * 0.015f + h * 0.045f, h * 0.68f)) { drawOval(Color(0xFF6E9E7A), Offset(w * 0.015f + h * 0.03f, h * 0.48f), Size(h * 0.03f, h * 0.2f)) }
    }
    val uhr = Offset(w * 0.955f, h * 0.18f)
    drawCircle(if (d) Color(0xFF2A2548) else Color.White, h * 0.06f, uhr)
    drawCircle(f.primaer, h * 0.06f, uhr, style = Stroke(3f))
    rotate(t * 25f, uhr) { drawLine(f.text, uhr, uhr - Offset(0f, h * 0.045f), 2f) }
    rotate(t * 3f, uhr) { drawLine(f.text, uhr, uhr - Offset(0f, h * 0.03f), 3f, StrokeCap.Round) }

    // Tafel
    val tafel = Rect(w * 0.08f, h * 0.05f, w * 0.9f, h * 0.66f)
    drawRoundRect(Color.Black.copy(alpha = 0.12f), tafel.topLeft + Offset(4f, 6f), tafel.size, CornerRadius(10f))
    drawRoundRect(Color(0xFFB5B2C4), tafel.topLeft, tafel.size, CornerRadius(10f))
    val flaeche = Rect(tafel.left + 6f, tafel.top + 6f, tafel.right - 6f, tafel.bottom - 6f)
    drawRoundRect(if (d) Color(0xFFE6E3F1) else Color.White, flaeche.topLeft, flaeche.size, CornerRadius(6f))
    drawLine(Color.White.copy(alpha = 0.6f), Offset(flaeche.left + flaeche.width * 0.62f, flaeche.top + 8f), Offset(flaeche.left + flaeche.width * 0.8f, flaeche.top + 8f), 6f, StrokeCap.Round)
    // Ablage mit Stiften
    drawRect(Color(0xFF9E9AB2), Offset(tafel.left + tafel.width * 0.1f, tafel.bottom), Size(tafel.width * 0.8f, h * 0.02f))
    val kappeFaellt = t >= 11.6f
    for (i in 0 until 3) {
        val farbe = listOf(stift, Color(0xFF55506B), f.primaer.heller(0.35f))[i]
        drawRoundRect(farbe, Offset(tafel.left + tafel.width * (0.55f + i * 0.05f), tafel.bottom - h * 0.018f), Size(h * 0.08f, h * 0.018f), CornerRadius(4f))
    }

    // Die Striche
    val m = Offset(flaeche.left + flaeche.width * 0.24f, flaeche.top + flaeche.height * 0.45f)
    val r = flaeche.height * 0.14f
    val knoten = listOf(
        Offset(flaeche.left + flaeche.width * 0.08f, flaeche.top + flaeche.height * 0.18f),
        Offset(flaeche.left + flaeche.width * 0.42f, flaeche.top + flaeche.height * 0.16f),
        Offset(flaeche.left + flaeche.width * 0.1f, flaeche.top + flaeche.height * 0.8f),
        Offset(flaeche.left + flaeche.width * 0.4f, flaeche.top + flaeche.height * 0.78f),
    )
    val listeX = flaeche.left + flaeche.width * 0.58f
    val zeilen = List(4) { flaeche.top + flaeche.height * (0.2f + it * 0.2f) }
    val k = flaeche.height * 0.09f
    val striche = mutableListOf<Strich>()
    striche += Strich(3f, 1.4f, { p -> m + Offset(cos(p * 2 * PI.toFloat() - PI.toFloat() / 2) * r, sin(p * 2 * PI.toFloat() - PI.toFloat() / 2) * r) }) { p ->
        drawArc(stift, -90f, 360f * p, false, m - Offset(r, r), Size(r * 2, r * 2), style = Stroke(4f, cap = StrokeCap.Round))
    }
    knoten.forEachIndexed { i, n ->
        val von = m + (n - m) * (r / (n - m).getDistance())
        striche += Strich(4.5f + i * 1f, 0.6f, { p -> von + (n - von) * p }) { p ->
            drawLine(stift, von, von + (n - von) * p, 3f, StrokeCap.Round)
            if (p >= 1f) drawCircle(f.primaer.copy(alpha = 0.25f), flaeche.height * 0.06f, n)
            if (p >= 1f) drawCircle(stift, flaeche.height * 0.06f, n, style = Stroke(3f))
        }
    }
    zeilen.forEachIndexed { i, y ->
        striche += Strich(9f + i * 1.4f, 1.2f, { p -> Offset(listeX + k * 1.6f + flaeche.width * 0.26f * p, y) }) { p ->
            drawRect(stift, Offset(listeX, y - k / 2), Size(k, k), style = Stroke(3f))
            kringel(Offset(listeX + k * 1.6f, y), flaeche.width * (0.26f - (i % 2) * 0.05f), k * 0.8f, p, stift, 3f, i + 3)
        }
    }
    zeilen.forEachIndexed { i, y ->
        striche += Strich(15.2f + i * 1f, 0.45f, { p -> Offset(listeX + k * (0.1f + 0.9f * p), y + k * (0.3f - 0.8f * p)) }) { p ->
            haken(Offset(listeX + k / 2, y), k * 1.1f, p, f.primaer, 4f)
        }
    }
    // Wörter neben den Knoten
    knoten.forEachIndexed { i, n ->
        kringel(n + Offset(flaeche.height * 0.09f, 0f), flaeche.width * 0.08f, flaeche.height * 0.05f, an(t, 5.2f + i, 0.5f) * weg, stift.copy(alpha = 0.7f), 2f, i)
    }
    if (weg > 0f) striche.forEach { st -> val p = an(t, st.start, st.dauer); if (p > 0f) { drawContext.canvas.save(); st.zeichne(this, p); drawContext.canvas.restore() } }
    // Wegwischen am Ende
    if (weg < 1f) drawRect(if (d) Color(0xFFE6E3F1) else Color.White, flaeche.topLeft, Size(flaeche.width * (1f - weg), flaeche.height))

    // Wo ist der Stift gerade?
    val aktiv = striche.lastOrNull { t >= it.start && t <= it.start + it.dauer + 0.25f }
    val stiftPunkt = aktiv?.let { it.punkt(an(t, it.start, it.dauer)) }

    // Person: Position folgt dem Stift
    val mf = MenschFarben(Color(0xFFE2B893), Color(0xFF2E2230), lerp(f.primaer, Color(0xFF6E6680), 0.3f), Color(0xFF30334A), Color(0xFF22212B))
    val zielX = when {
        t < 3f -> w * 0.5f
        stiftPunkt != null -> stiftPunkt.x + 0.12f * s
        t < 9f -> w * 0.34f
        t < 19f -> w * 0.7f
        else -> w * 0.5f
    }
    val personX = personXGlatt(t, zielX, w)
    val gehtGerade = kotlin.math.abs(zielX - personX) > w * 0.01f
    val umgedreht = t in 19.3f..22.4f
    val blink = blinzelt(t)
    if (!umgedreht) {
        val kopfZiel = if (t < 3f) sin(t * 1.4f) * 8f else 0f
        menschFront(
            personX, boden, s, mf, vonHinten = true,
            zielL = if (t < 3f) Offset(personX - 0.06f * s, boden - 0.9f * s) else null,
            zielR = stiftPunkt,
            kopfNeigung = kopfZiel, schritt = if (gehtGerade) t * 9f else 0f,
        )
        if (t < 3f) {
            val q = an(t, 0.4f, 0.5f)
            drawCircle(Color.White.copy(alpha = 0.9f * q), h * 0.022f, Offset(personX + 0.12f * s, boden - 1.02f * s))
            drawCircle(Color.White.copy(alpha = 0.9f * q), h * 0.04f, Offset(personX + 0.2f * s, boden - 1.12f * s))
            drawCircle(f.primaer.copy(alpha = q), h * 0.012f, Offset(personX + 0.2f * s, boden - 1.1f * s))
        }
    } else {
        val hub = maxOf(0f, sin((t - 19.5f) * 6.5f)) * 0.08f * s
        menschFront(
            personX, boden, s, mf, vonHinten = false,
            zielL = Offset(personX - 0.28f * s, boden - 1.08f * s - hub), zielR = Offset(personX + 0.28f * s, boden - 1.08f * s - hub),
            sprung = hub, blinzeln = blink, lachen = 1f,
        )
        for (i in 0 until 8) {
            val q = ((t - 19.4f) * 0.7f + i / 8f) % 1f
            val wk = i * 45f + t * 20f
            val p = Offset(personX + cos(rad(wk)) * q * h * 0.45f, boden - s * 0.95f + sin(rad(wk)) * q * h * 0.35f)
            drawLine(f.primaer.copy(alpha = 1f - q), p - Offset(4f, 0f), p + Offset(4f, 0f), 2.5f, StrokeCap.Round)
            drawLine(f.primaer.copy(alpha = 1f - q), p - Offset(0f, 4f), p + Offset(0f, 4f), 2.5f, StrokeCap.Round)
        }
    }

    // Stiftkappe fällt, rollt, Katze jagt sie
    val kappeStart = Offset(tafel.left + tafel.width * 0.62f, tafel.bottom)
    val kappe: Offset = when {
        !kappeFaellt -> kappeStart
        t < 12.2f -> { val q = an(t, 11.6f, 0.6f); Offset(kappeStart.x - q * w * 0.03f, kappeStart.y + q * q * (boden - kappeStart.y)) }
        t < 14.2f -> Offset(kappeStart.x - w * 0.03f - weich(an(t, 12.2f, 2f)) * w * 0.14f, boden)
        t < 16.5f -> Offset(kappeStart.x - w * 0.17f - weich(an(t, 15f, 1.2f)) * w * 0.12f, boden)
        else -> Offset(kappeStart.x - w * 0.29f, boden)
    }
    if (kappeFaellt) rotate(if (t < 14.2f) (t - 11.6f) * 400f else 0f, kappe - Offset(0f, h * 0.012f)) {
        drawRoundRect(stift, kappe - Offset(h * 0.018f, h * 0.024f), Size(h * 0.036f, h * 0.022f), CornerRadius(5f))
    }
    val katzeF = KatzenFarben(Color(0xFF2E2A33), Color(0xFF1C1A20), Color(0xFFF2EFF5), Color(0xFFE8C547))
    val cs = h * 0.18f
    val cy = boden
    when {
        t < 1f -> Unit
        t < 6f -> katze(mix(w * 1.08f, w * 0.78f, an(t, 1f, 5f)), cy, cs, -1f, 0, t * 10f, t, katzeF)
        t < 12.4f -> katze(w * 0.78f, cy, cs, -1f, 1, 0f, t, katzeF)
        t < 13.2f -> katze(w * 0.78f, cy, cs, -1f, 3, 0f, t, katzeF)
        t < 14.6f -> { val q = an(t, 13.2f, 1.4f); katze(mix(w * 0.78f, kappe.x + cs * 0.9f, q), cy, cs, -1f, 3, 0f, t, katzeF, hoehe = sin(q * 3.14f) * h * 0.12f) }
        t < 17f -> katze(kappe.x + cs * 0.75f, cy, cs, -1f, 1, 0f, t, katzeF, pfote = maxOf(0f, sin((t - 14.6f) * 7f)))
        else -> katze(kappe.x + cs * 0.75f, cy, cs, -1f, 1, 0f, t, katzeF)
    }
}

/** Die Person geht weich zur Zielposition — ohne Zustand, rein aus der Zeit berechnet. */
private fun personXGlatt(t: Float, zielX: Float, w: Float): Float {
    // Stützpunkte des Weges, zwischen denen weich übergeblendet wird.
    val wege = listOf(0f to 0.5f, 2.6f to 0.5f, 3.2f to 0.38f, 8.6f to 0.36f, 9.4f to 0.7f, 18.8f to 0.72f, 19.4f to 0.5f)
    var x = wege.first().second
    for (i in 1 until wege.size) {
        val (t0, x0) = wege[i - 1]
        val (t1, x1) = wege[i]
        if (t >= t0) x = mix(x0, x1, weich(an(t, t0, t1 - t0)))
    }
    // Beim Schreiben der Liste folgt die Person der Zeile leicht mit
    return x * w + (zielX - x * w).coerceIn(-w * 0.1f, w * 0.1f) * 0.7f
}
