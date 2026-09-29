package de.frank.longevity.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.data.Evidenz
import de.frank.longevity.data.Faktor
import de.frank.longevity.data.Kategorie
import de.frank.longevity.ui.theme.Chip
import de.frank.longevity.ui.theme.LocalBewegung
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.antippen
import de.frank.longevity.ui.theme.glas

/** Wächst einmal von 0 auf 1, wenn das Diagramm erscheint. */
@Composable
private fun wachsen(schluessel: Any?, verzoegerung: Int = 0): Float {
    val bewegung = LocalBewegung.current
    val a = remember(schluessel) { Animatable(if (bewegung) 0f else 1f) }
    LaunchedEffect(schluessel) { if (bewegung) a.animateTo(1f, tween(900, verzoegerung, FastOutSlowInEasing)) }
    return a.value
}

/** Die Übersichtskarte mit drei Ansichten: Wirkung, Lebensbereiche, Evidenz. */
@Composable
fun Ueberblick(liste: List<Faktor>, oeffnen: (Long) -> Unit) {
    val f = LocalFarben.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().einblenden(120).glas(f, erhoeht = 1.1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ÜBERBLICK", color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Text("${liste.size} Faktoren", color = f.textSchwach, fontSize = 12.sp)
        }
        ChipReihe {
            Chip("Wirkung", tab == 0) { tab = 0 }
            Chip("Lebensbereiche", tab == 1) { tab = 1 }
            Chip("Evidenz", tab == 2) { tab = 2 }
        }
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "tab") { t ->
            when (t) {
                0 -> WirkungsBalken(liste.take(10), oeffnen)
                1 -> Ring(liste.groupBy { it.kat }.map { (k, l) -> Triple("${k.emoji} ${k.anzeige}", l.sumOf { it.wirkung }.toFloat(), kategorieFarbe(k)) }.sortedByDescending { it.second }, "Wirkung je Lebensbereich")
                else -> Ring(Evidenz.entries.map { e -> Triple(e.anzeige, liste.count { it.ev == e }.toFloat(), f.evidenzFarbe(e)) }.filter { it.second > 0 }, "Faktoren je Evidenzstufe")
            }
        }
    }
}

@Composable
private fun kategorieFarbe(k: Kategorie): Color {
    val f = LocalFarben.current
    val basis = listOf(f.primaer, f.sekundaer, f.tertiaer, f.erfolg, f.blob1, f.blob2, f.blob3, f.gefahr, Color(0xFF8E7CFF), Color(0xFF4FB6FF))
    return basis[k.ordinal % basis.size]
}

/** Die zehn wichtigsten Faktoren als wachsende Balken (geschätzte Jahre). */
@Composable
fun WirkungsBalken(liste: List<Faktor>, oeffnen: (Long) -> Unit) {
    val f = LocalFarben.current
    val max = (liste.maxOfOrNull { it.jahre } ?: 1f).coerceAtLeast(0.5f)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        liste.forEachIndexed { i, x ->
            val w = wachsen(x.id to x.jahre, i * 60)
            Row(Modifier.fillMaxWidth().antippen(haptik = false) { oeffnen(x.id) }, verticalAlignment = Alignment.CenterVertically) {
                Text("${x.rang}", color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(x.titel, color = f.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val c = f.evidenzFarbe(x.ev)
                    Canvas(Modifier.fillMaxWidth().height(9.dp).padding(top = 2.dp)) {
                        drawRoundRect(f.textSchwach.copy(alpha = 0.15f), cornerRadius = CornerRadius(size.height / 2))
                        drawRoundRect(
                            Brush.horizontalGradient(listOf(f.primaer, c)),
                            size = Size(size.width * (x.jahre / max) * w, size.height),
                            cornerRadius = CornerRadius(size.height / 2),
                        )
                    }
                }
                Text(jahreText(x.jahre), color = f.primaer, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).width(52.dp))
            }
        }
        Text("Geschätzte zusätzliche gesunde Lebensjahre bei konsequenter Umsetzung. Farbe = Evidenz.", color = f.textSchwach, fontSize = 11.sp)
    }
}

/** Ringdiagramm mit Legende. */
@Composable
fun Ring(teile: List<Triple<String, Float, Color>>, titel: String) {
    val f = LocalFarben.current
    val summe = teile.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.001f)
    val w = wachsen(teile.map { it.second })
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(120.dp)) {
                val dicke = 18.dp.toPx()
                var start = -90f
                teile.forEach { (_, wert, farbe) ->
                    val bogen = 360f * wert / summe * w
                    drawArc(farbe, start, (bogen - 2f).coerceAtLeast(0.5f), false, Offset(dicke / 2, dicke / 2), Size(size.width - dicke, size.height - dicke), style = Stroke(dicke, cap = StrokeCap.Butt))
                    start += bogen
                }
            }
            Text("${teile.size}", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titel, color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            teile.take(8).forEach { (name, wert, farbe) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).glas(f, 99.dp, 0f, farbe, rand = false))
                    Text(name, color = f.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    Text("${(wert / summe * 100).toInt()} %", color = f.textLeise, fontSize = 11.sp)
                }
            }
        }
    }
}

/** Tacho für die Detailseite: Wirkung 0–100 als Bogen, in der Mitte die Jahre. */
@Composable
fun Tacho(wirkung: Int, jahre: Float, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val w = wachsen(wirkung)
    Box(modifier.size(150.dp, 100.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.size(150.dp, 150.dp).padding(8.dp)) {
            val dicke = 14.dp.toPx()
            val tl = Offset(dicke / 2, dicke / 2)
            val gr = Size(size.width - dicke, size.height - dicke)
            drawArc(f.textSchwach.copy(alpha = 0.18f), 180f, 180f, false, tl, gr, style = Stroke(dicke, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(f.sekundaer, f.primaer, f.tertiaer, f.sekundaer)), 180f, 180f * wirkung / 100f * w, false, tl, gr, style = Stroke(dicke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 4.dp)) {
            Text(jahreText(jahre * w), color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Wirkung ${(wirkung * w).toInt()}/100", color = f.textLeise, fontSize = 11.sp)
        }
    }
}

/** Kleiner Fortschrittsring (erledigte Punkte). */
@Composable
fun MiniRing(anteil: Float, modifier: Modifier = Modifier, dickeDp: Float = 3f) {
    val f = LocalFarben.current
    Canvas(modifier) {
        val d = dickeDp.dp.toPx()
        drawArc(f.textSchwach.copy(alpha = 0.2f), 0f, 360f, false, Offset(d / 2, d / 2), Size(size.width - d, size.height - d), style = Stroke(d))
        if (anteil > 0f) drawArc(f.erfolg, -90f, 360f * anteil, false, Offset(d / 2, d / 2), Size(size.width - d, size.height - d), style = Stroke(d, cap = StrokeCap.Round))
    }
}
