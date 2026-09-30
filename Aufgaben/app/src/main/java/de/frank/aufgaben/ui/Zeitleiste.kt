package de.frank.aufgaben.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import java.time.LocalTime
import kotlinx.coroutines.delay

private val STUNDE = 42.dp
private val OBEN = 12.dp
private val SPALTE = 54.dp

/**
 * Spanne der Leiste in Minuten. Mit Automatik: eine Stunde vor dem ersten bis eine Stunde nach dem
 * letzten Termin — aber nur, solange nichts gezogen wird; beim Ziehen gilt die eingestellte Spanne.
 */
private fun spanne(termine: List<Aufgabe>, von: Int, bis: Int, auto: Boolean, zieht: Boolean): Pair<Int, Int> {
    if (!auto || zieht || termine.isEmpty()) return von to bis
    val erster = termine.minOf { it.minuten ?: von }
    val letzter = termine.maxOf { (it.minuten ?: von) + maxOf(it.dauer, 30) }
    val a = ((erster / 60 - 1) * 60).coerceIn(0, 23 * 60)
    val b = (((letzter + 59) / 60 + 1) * 60).coerceIn(a + 60, 24 * 60)
    return a to b
}

/**
 * Senkrechter Zeitpfeil über die eingestellte Spanne. Karten, die darüber gezogen werden, zeigen links die
 * Uhrzeit, die beim Loslassen gilt (in 15-Minuten-Schritten, sie wandert mit dem Finger mit).
 */
@Composable
fun Zeitleiste(
    tag: Long, termine: List<Aufgabe>, istHeute: Boolean, zustand: ZiehZustand, vonEinst: Int, bisEinst: Int, auto: Boolean,
    onTipp: (Aufgabe) -> Unit, onErledigt: (Aufgabe) -> Unit,
) {
    val f = LocalFarben.current
    val dichte = LocalDensity.current
    val messer = rememberTextMeasurer()
    val stundePx = with(dichte) { STUNDE.toPx() }
    val obenPx = with(dichte) { OBEN.toPx() }
    val (vonMin, bisMin) = spanne(termine, vonEinst, bisEinst, auto, zustand.aufgabe != null)
    val stunden = (bisMin - vonMin) / 60f
    val hoehe = OBEN + STUNDE * stunden + 30.dp
    var jetzt by remember { mutableIntStateOf(LocalTime.now().let { it.hour * 60 + it.minute }) }
    if (istHeute) LaunchedEffect(Unit) {
        while (true) { delay(30_000); jetzt = LocalTime.now().let { it.hour * 60 + it.minute } }
    }
    DisposableEffect(tag) { onDispose { zustand.entferneLeiste(tag) } }
    val schweben = zustand.hoverZeit?.takeIf { it.first == tag }?.second
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(hoehe)
            .onGloballyPositioned { c ->
                val b = c.boundsInRoot()
                zustand.registriereLeiste(tag, LeistenMass(Rect(b.left, b.top, b.right, b.bottom), b.top + obenPx, stundePx, vonMin, bisMin))
            },
    ) {
        val breite = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val x = SPALTE.toPx() - 8.dp.toPx()
            val y0 = obenPx
            val y1 = obenPx + stundePx * stunden
            // Stundenraster
            for (s in (vonMin + 59) / 60..bisMin / 60) {
                val y = y0 + (s * 60 - vonMin) / 60f * stundePx
                drawLine(f.textSchwach.copy(alpha = 0.18f), Offset(x + 8.dp.toPx(), y), Offset(size.width, y), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
                drawLine(f.textLeise.copy(alpha = 0.6f), Offset(x - 6.dp.toPx(), y), Offset(x + 4.dp.toPx(), y), 2f, StrokeCap.Round)
                val text = messer.measure("%02d:00".format(s), TextStyle(color = f.textLeise, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                drawText(text, topLeft = Offset(x - 10.dp.toPx() - text.size.width, y - text.size.height / 2f))
                if (s * 60 + 30 <= bisMin) drawLine(f.textSchwach.copy(alpha = 0.5f), Offset(x - 3.dp.toPx(), y + stundePx / 2), Offset(x + 2.dp.toPx(), y + stundePx / 2), 1.5f)
            }
            // Der Zeitpfeil
            drawLine(Brush.verticalGradient(listOf(f.primaer, f.sekundaer), y0, y1), Offset(x, y0 - 6.dp.toPx()), Offset(x, y1 + 12.dp.toPx()), 3.5.dp.toPx(), StrokeCap.Round)
            val spitze = Path().apply {
                moveTo(x - 7.dp.toPx(), y1 + 10.dp.toPx()); lineTo(x, y1 + 22.dp.toPx()); lineTo(x + 7.dp.toPx(), y1 + 10.dp.toPx()); close()
            }
            drawPath(spitze, f.sekundaer)
            // Jetzt-Linie
            if (vonMin % 60 != 0) drawLine(f.textSchwach.copy(alpha = 0.5f), Offset(x - 3.dp.toPx(), y0), Offset(x + 2.dp.toPx(), y0), 1.5f)
            if (istHeute && jetzt in vonMin..bisMin) {
                val y = y0 + (jetzt - vonMin) / 60f * stundePx
                drawLine(f.gefahr, Offset(x, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
                drawCircle(f.gefahr, 6.dp.toPx(), Offset(x, y))
                drawCircle(Color.White, 2.5.dp.toPx(), Offset(x, y))
            }
        }
        // Termine, Überlappungen nebeneinander
        val sortiert = termine.sortedBy { it.minuten }
        val spalten = mutableListOf<Int>()
        val spalteVon = HashMap<Long, Int>()
        sortiert.forEach { a ->
            val start = a.minuten ?: 0
            val i = spalten.indexOfFirst { it <= start }.let { if (it < 0) { spalten.add(0); spalten.lastIndex } else it }
            spalten[i] = start + maxOf(a.dauer, 30)
            spalteVon[a.id] = i
        }
        val nSpalten = spalten.size.coerceAtLeast(1)
        val flaeche = breite - SPALTE - 6.dp
        sortiert.forEach { a ->
            val min = (a.minuten ?: vonMin).coerceIn(vonMin, bisMin)
            val oben = OBEN + STUNDE * ((min - vonMin) / 60f)
            val h = (STUNDE * (maxOf(a.dauer, 30) / 60f) - 3.dp).coerceAtLeast(34.dp)
            val sp = spalteVon[a.id] ?: 0
            Box(
                Modifier.offset(x = SPALTE + flaeche / nSpalten * sp, y = oben)
                    .width(flaeche / nSpalten - 4.dp).height(h),
            ) { TerminBlock(a, zustand, onTipp = { onTipp(a) }, onErledigt = { onErledigt(a) }) }
        }
        // Vorschau beim Ziehen: Geisterblock plus Uhrzeit links
        if (schweben != null) {
            val ziel by animateFloatAsState(
                with(dichte) { (OBEN + STUNDE * ((schweben - vonMin) / 60f)).toPx() },
                spring(dampingRatio = 0.9f, stiffness = 900f), label = "zeit",
            )
            val dauer = zustand.aufgabe?.dauer ?: 30
            Box(
                Modifier.graphicsLayer { translationY = ziel }.offset(x = SPALTE).width(flaeche).height(STUNDE * (maxOf(dauer, 30) / 60f))
                    .glas(f, radius = 12.dp, erhoeht = 0f, fuellung = f.primaer.copy(alpha = 0.22f)),
            )
            Box(
                Modifier.graphicsLayer { translationY = ziel - with(dichte) { 13.dp.toPx() } }.offset(x = 0.dp)
                    .width(SPALTE - 4.dp).height(26.dp).knopf3d(f.primaer, f.sekundaer, 13.dp, f.dunkel),
                contentAlignment = Alignment.Center,
            ) { Text(Tage.zeit(schweben), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun TerminBlock(a: Aufgabe, zustand: ZiehZustand, onTipp: () -> Unit, onErledigt: () -> Unit) {
    val f = LocalFarben.current
    val farbe = f.prio(a.prio)
    val gezogen = zustand.aufgabe?.id == a.id
    val ende = (a.minuten ?: 0) + a.dauer
    Row(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = if (gezogen) 0.25f else if (a.erledigt) 0.55f else 1f; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .glas(f, radius = 12.dp, erhoeht = 1f, fuellung = f.flaecheStark, toenung = farbe)
            .ziehbar(a, zustand)
            .antippen(haptik = false) { if (zustand.tippErlaubt()) onTipp() },
    ) {
        Box(Modifier.width(5.dp).fillMaxSize().knopf3d(farbe, farbe.copy(alpha = 0.7f), 3.dp, f.dunkel))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                a.titel, color = f.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (a.erledigt) TextDecoration.LineThrough else null,
            )
            Row {
                Text("${Tage.zeit(a.minuten ?: 0)}–${Tage.zeit(ende % (24 * 60))}", color = f.textLeise, fontSize = 11.sp)
                if (a.erinnerung) Icon(Icons.Rounded.NotificationsActive, null, tint = f.sekundaer, modifier = Modifier.padding(start = 4.dp).size(12.dp))
            }
        }
        Box(Modifier.size(34.dp).antippen(haptik = true, aktion = onErledigt), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                drawCircle(farbe, size.minDimension / 2 - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                if (a.erledigt) drawCircle(farbe, size.minDimension / 2 - 4.dp.toPx())
            }
        }
    }
}
