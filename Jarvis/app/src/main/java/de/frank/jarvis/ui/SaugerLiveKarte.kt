package de.frank.jarvis.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.frank.jarvis.staubsauger.ProscenicMehr
import de.frank.jarvis.staubsauger.SaugerLive
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.delay

/** Breite der Saugspur auf der Karte in Metern (Bürste des M7 Pro, etwa 25 cm), damit der Strich die abgesaugte Fläche zeigt. */
private const val SPUR_BREITE_M = 0.25

private fun liveZustand(m: String?) = when (m) {
    null, "" -> "Zustand wird gelesen …"
    "charge" -> "lädt in der Station"; "fullcharge" -> "voll geladen in der Station"; "sweep" -> "reinigt"; "pause" -> "pausiert"
    "backcharge" -> "fährt zur Ladestation"; "findchargerpause" -> "Rückfahrt pausiert"; "DustCenterWorking" -> "Staubsammlung läuft"
    "idle" -> "bereit"; "fault" -> "Störung"; else -> m
}

/**
 * Live-Karte im Vollbild (Franks Wunsch vom 17.11.2026, wie in der Proscenic-App): Die Position des Roboters gleitet flüssig
 * von Messpunkt zu Messpunkt, die gefahrene Strecke der laufenden Reinigung steht als breiter Strich (abgesaugter Bereich) mit
 * Mittellinie auf der Karte. Zwei Finger zoomen und verschieben. Die Daten liefert [SaugerLive], solange der Dialog offen ist.
 */
@Composable
fun SaugerLiveDialog(start: ProscenicMehr.Live?, schliessen: () -> Unit) {
    val f = LocalFarben.current
    val context = LocalContext.current.applicationContext
    DisposableEffect(Unit) {
        SaugerLive.starte(context, start)
        onDispose { SaugerLive.stoppe() }
    }
    val spur by SaugerLive.spur.collectAsState()
    Dialog(onDismissRequest = schliessen, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(f.hgOben)) {
            val k = spur.karte
            if (k != null) LiveKarte(spur, k, Modifier.fillMaxSize())
            else Text(spur.fehler ?: "Karte wird geladen …", color = f.textLeise, fontSize = 15.sp, modifier = Modifier.align(Alignment.Center).padding(24.dp))
            // Leisten ausblenden wie beim Karten-Vollbild; Knöpfe halten deren Platz frei.
            val dialogFenster = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            LaunchedEffect(dialogFenster) {
                dialogFenster?.let { w ->
                    androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                        systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            val rand = Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.systemBarsIgnoringVisibility)
                .displayCutoutPadding().padding(12.dp)
            // Alter der letzten Position, jede Sekunde neu, damit sichtbar ist, ob die Live-Daten fließen.
            var jetzt by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) { while (true) { delay(1000); jetzt = System.currentTimeMillis() } }
            Row(Modifier.fillMaxWidth().then(rand), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).glas(f, 14.dp, erhoeht = 0.5f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val frisch = spur.positionUm > 0 && jetzt - spur.positionUm < 10_000
                        Canvas(Modifier.size(10.dp)) { drawCircle(if (frisch) Color(0xFFE53935) else Color(0xFF9E9E9E)) }
                        Text("Live-Karte", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                    }
                    Text(
                        listOfNotNull(liveZustand(spur.modus), spur.akku?.let { "Akku $it %" }).joinToString(" · "),
                        color = f.textLeise, fontSize = 13.sp,
                    )
                    val hinweis = spur.fehler ?: when {
                        spur.positionUm == 0L -> "Warte auf die erste Position …"
                        jetzt - spur.positionUm > 10_000 -> "Letzte Position vor ${(jetzt - spur.positionUm) / 1000} s"
                        else -> null
                    }
                    hinweis?.let { Text(it, color = f.textLeise, fontSize = 12.sp) }
                }
                Spacer(Modifier.size(10.dp))
                Box(Modifier.size(42.dp).glas(f, 21.dp, erhoeht = 0.5f).antippen { schliessen() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Schließen", tint = f.text, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveKarte(spur: SaugerLive.Spur, k: ProscenicMehr.Karte, modifier: Modifier) {
    val bild = remember(k) { k.bild.asImageBitmap() }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    // Position in Millimetern, gleitend zwischen den Messpunkten (linear über den Abfragetakt), damit der Roboter wie im Video fährt.
    val pos = remember { Animatable(Offset.Unspecified, Offset.VectorConverter) }
    LaunchedEffect(spur.roboter) {
        val ziel = spur.roboter?.let { Offset(it.first.toFloat(), it.second.toFloat()) } ?: return@LaunchedEffect
        val alt = pos.value
        if (alt == Offset.Unspecified || (alt - ziel).getDistance() > 3000f) pos.snapTo(ziel)
        else pos.animateTo(ziel, tween(durationMillis = 2100, easing = LinearEasing))
    }
    val puls by rememberInfiniteTransition(label = "puls").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "puls",
    )
    Canvas(
        modifier.pointerInput(k) {
            detectTransformGestures { mitte, verschiebung, faktor, _ ->
                val nz = (zoom * faktor).coerceIn(1f, 8f)
                val p = mitte - (mitte - pan) * (nz / zoom) + verschiebung
                zoom = nz
                pan = Offset(p.x.coerceIn(size.width * (1 - nz), 0f), p.y.coerceIn(size.height * (1 - nz), 0f))
            }
        },
    ) {
        val basis = minOf(size.width / k.breite, size.height / k.hoehe)
        val bx = (size.width - k.breite * basis) / 2; val by = (size.height - k.hoehe * basis) / 2
        fun bildPunkt(px: Float, py: Float) = Offset(pan.x + zoom * (bx + basis * px), pan.y + zoom * (by + basis * py))
        fun pt(x: Float, y: Float) = bildPunkt(k.px(x.toInt()), k.py(y.toInt()))
        val massstab = basis * zoom
        clipRect {
            val o = bildPunkt(0f, 0f)
            drawImage(bild, IntOffset.Zero, IntSize(k.breite, k.hoehe), IntOffset(o.x.toInt(), o.y.toInt()),
                IntSize((k.breite * massstab).toInt(), (k.hoehe * massstab).toInt()), filterQuality = FilterQuality.None)
            spur.zonen.filter { it.sperre }.forEach { z ->
                val e = z.ecken; if (e.isEmpty()) return@forEach
                val a = pt(e.minOf { it.first }.toFloat(), e.maxOf { it.second }.toFloat()); val b = pt(e.maxOf { it.first }.toFloat(), e.minOf { it.second }.toFloat())
                drawRect(Color(0x33E53935), a, Size(b.x - a.x, b.y - a.y))
            }
            // Gefahrene Strecke: breite, halbdurchsichtige Spur (abgesaugte Fläche) und darüber die Mittellinie; endet am gleitenden Roboter.
            val r = pos.value.takeIf { it != Offset.Unspecified }
            if (spur.pfad.isNotEmpty()) {
                val p = Path()
                spur.pfad.forEachIndexed { i, (x, y) -> val q = pt(x.toFloat(), y.toFloat()); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
                if (r != null && ProscenicMehr.aktiv(spur.modus)) pt(r.x, r.y).let { p.lineTo(it.x, it.y) }
                val breite = (SPUR_BREITE_M / k.aufloesung * massstab).toFloat().coerceAtLeast(3.dp.toPx())
                drawPath(p, Color(0x5542A5F5), style = Stroke(breite, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(p, Color(0xDD1E88E5), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            spur.station?.let { (x, y) -> drawCircle(Color(0xFF43A047), 7.dp.toPx(), pt(x.toFloat(), y.toFloat())) }
            if (r != null) {
                val q = pt(r.x, r.y)
                if (ProscenicMehr.aktiv(spur.modus)) drawCircle(Color(0xFF1565C0).copy(alpha = 0.35f * (1 - puls)), 9.dp.toPx() + 14.dp.toPx() * puls, q)
                drawCircle(Color.White, 9.dp.toPx(), q)
                drawCircle(Color(0xFF1565C0), 7.dp.toPx(), q)
            }
        }
    }
}
