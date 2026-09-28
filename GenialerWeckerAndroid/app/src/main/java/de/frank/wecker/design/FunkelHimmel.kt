package de.frank.wecker.design

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import de.frank.genialeideen.ui.theme.LocalBewegungReduziert
import de.frank.wecker.rememberResumed
import kotlin.math.floor
import kotlin.math.sin

/**
 * Traumraums lebendiger Nachthimmel: Sterne gehen auf, funkeln und verlöschen wieder, jedes Mal an
 * einer neuen Stelle. Dazu ziehen Satelliten mit blinkendem Positionslicht vorbei, ab und zu fällt
 * eine Sternschnuppe.
 *
 * [dichte] = Sterne je 10 000 dp²; [satelliten] schaltet Satelliten und Sternschnuppen zu.
 * Die Zeit wird nur im Zeichenblock gelesen, ~30 Bilder/s, nur solange der Bildschirm vorne ist;
 * bei reduzierter Bewegung steht ein ruhiges Standbild.
 */
@Composable
internal fun FunkelHimmel(
    modifier: Modifier,
    stern: Color,
    akzent: Color,
    dichte: Float = 1.2f,
    satelliten: Boolean = true,
    staerke: Float = 1f,
) {
    val reduziert = LocalBewegungReduziert.current
    val aktiv = rememberResumed() && !reduziert
    val zeit = remember { mutableLongStateOf(0L) }
    if (aktiv) LaunchedEffect(Unit) {
        var start = -1L
        var letzte = 0L
        while (true) {
            withFrameMillis { t ->
                if (start < 0) start = t - zeit.longValue
                if (t - letzte >= 32) { zeit.longValue = t - start; letzte = t }
            }
        }
    }
    Canvas(modifier) {
        val t = if (reduziert) 4.2f else zeit.longValue / 1000f
        val flaeche = size.width / density * size.height / density
        val anzahl = (flaeche / 10_000f * dichte).toInt().coerceIn(12, 140)
        repeat(anzahl) { i -> zeichneStern(i, t, stern, akzent, staerke) }
        if (satelliten) {
            satellit(t, 11f, 0f, 0.18f, 0.10f, stern, staerke)
            satellit(t, 17f, 6f, 0.52f, -0.14f, stern, staerke)
            sternschnuppe(t, 13f, 4f, stern, staerke)
        }
    }
}

/** Kleiner, stabiler Pseudozufall aus zwei ganzen Zahlen: 0 ≤ Ergebnis < 1. */
private fun wurf(a: Int, b: Int): Float {
    var h = a * 374761393 + b * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)) and 0xFFFFFF) / 16777216f
}

private fun DrawScope.zeichneStern(i: Int, t: Float, farbe: Color, akzent: Color, staerke: Float) {
    val periode = 6f + wurf(i, 1) * 9f
    val lauf = t / periode + wurf(i, 2)
    val runde = floor(lauf).toInt()
    val phase = lauf - runde
    // Aufgehen, leuchten, verlöschen, dann kurz aus – danach an neuer Stelle.
    val sicht = when {
        phase < 0.18f -> phase / 0.18f
        phase < 0.70f -> 1f
        phase < 0.88f -> 1f - (phase - 0.70f) / 0.18f
        else -> 0f
    }
    if (sicht <= 0.01f) return
    val x = wurf(i * 7 + runde, 3) * size.width
    val y = wurf(i * 13 + runde, 4) * size.height
    val gross = wurf(i, 5)
    val funkeln = 0.6f + 0.4f * sin(t * (2f + gross * 3f) + i)
    val a = (0.25f + gross * 0.65f) * sicht * funkeln * staerke
    val c = if (i % 7 == 0) akzent else farbe
    val r = (0.6f + gross * 1.5f).dp.toPx()
    val m = Offset(x, y)
    drawCircle(c.copy(alpha = a.coerceIn(0f, 1f)), r, m)
    // Die hellsten bekommen einen Kreuzschein, der mitfunkelt.
    if (gross > 0.8f) {
        val l = r * (2.4f + 1.6f * funkeln)
        val strich = c.copy(alpha = (a * .6f).coerceIn(0f, 1f))
        drawLine(strich, m - Offset(l, 0f), m + Offset(l, 0f), 0.7f.dp.toPx())
        drawLine(strich, m - Offset(0f, l), m + Offset(0f, l), 0.7f.dp.toPx())
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = (a * .35f).coerceIn(0f, 1f)), Color.Transparent), center = m, radius = r * 4f), r * 4f, m)
    }
}

/** Ein Satellit quert langsam den Himmel: Körper, zwei Solarflügel, blinkendes Licht. */
private fun DrawScope.satellit(t: Float, periode: Float, versatz: Float, hoehe: Float, neigung: Float, farbe: Color, staerke: Float) {
    val dauer = periode * 0.7f
    val u = ((t + versatz) % periode) / dauer
    if (u !in 0f..1f) return
    val rechtsNachLinks = neigung < 0f
    val x = if (rechtsNachLinks) size.width * (1.05f - 1.1f * u) else size.width * (-0.05f + 1.1f * u)
    val y = size.height * (hoehe + kotlin.math.abs(neigung) * u)
    val m = Offset(x, y)
    val k = 1.6f.dp.toPx()
    val a = (0.85f * staerke).coerceIn(0f, 1f)
    rotate(if (rechtsNachLinks) -8f else 8f, m) {
        drawRect(farbe.copy(alpha = a), m - Offset(k * .6f, k * .6f), Size(k * 1.2f, k * 1.2f))
        drawRect(Color(0xFF6E8FC8).copy(alpha = a), m - Offset(k * 3.4f, k * .4f), Size(k * 2.4f, k * .8f))
        drawRect(Color(0xFF6E8FC8).copy(alpha = a), m + Offset(k, -k * .4f), Size(k * 2.4f, k * .8f))
    }
    if ((t * 1.25f % 1f) < 0.18f) {
        drawCircle(Color(0xFFFF5A4A).copy(alpha = a), k * .7f, m + Offset(0f, k * .9f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFF5A4A).copy(alpha = a * .5f), Color.Transparent), center = m, radius = k * 4f), k * 4f, m)
    }
}

/** Gelegentliche Sternschnuppe: kurzer heller Strich mit ausblendendem Schweif. */
private fun DrawScope.sternschnuppe(t: Float, periode: Float, versatz: Float, farbe: Color, staerke: Float) {
    val runde = floor((t + versatz) / periode).toInt()
    val u = (((t + versatz) % periode) / 0.9f)
    if (u !in 0f..1f) return
    val start = Offset(size.width * (0.25f + wurf(runde, 9) * 0.6f), size.height * (0.05f + wurf(runde, 10) * 0.3f))
    val richtung = Offset(-0.9f, 0.45f)
    val weg = size.minDimension.coerceAtLeast(120.dp.toPx()) * 0.5f
    val kopf = start + richtung * (weg * u)
    val schweif = kopf - richtung * (weg * 0.35f)
    val a = (sin(u * Math.PI).toFloat() * staerke).coerceIn(0f, 1f)
    drawLine(Brush.linearGradient(listOf(Color.Transparent, farbe.copy(alpha = a)), schweif, kopf), schweif, kopf, 1.4f.dp.toPx(), StrokeCap.Round)
    drawCircle(farbe.copy(alpha = a), 1.3f.dp.toPx(), kopf)
}
