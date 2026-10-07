package de.frank.aufgaben.ui

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import de.frank.aufgaben.data.Aufgabe
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.ui.theme.AufgabenTheme
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun AufgabenApp(vm: AppViewModel, activity: ComponentActivity) {
    AufgabenTheme(vm.einstellungen.design, vm.einstellungen.modus) {
        Box(Modifier.fillMaxSize()) {
            Hintergrund(Modifier.fillMaxSize())
            BackHandler(vm.bildschirm != Bildschirm.Liste) { vm.zurueck() }
            AnimatedContent(
                vm.bildschirm,
                transitionSpec = {
                    val vor = targetState != Bildschirm.Liste
                    if (vor) (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.97f))
                    else (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f))
                        .togetherWith(slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(180)))
                },
                contentKey = { it::class },
                label = "bildschirm",
            ) { b ->
                when (b) {
                    Bildschirm.Liste -> ListeBildschirm(vm)
                    is Bildschirm.Bearbeiten -> BearbeitenBildschirm(vm)
                    Bildschirm.Einstellungen -> EinstellungenBildschirm(vm, activity)
                    is Bildschirm.Fokus -> FokusBildschirm(vm, b.id)
                }
            }
            Konfetti(vm.konfetti)
            Meldung(vm, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Meldung(vm: AppViewModel, modifier: Modifier) {
    val f = LocalFarben.current
    val text = vm.meldung
    LaunchedEffect(text) { if (text != null) { delay(if (vm.rueckgaengig != null) 5000 else 2600); vm.meldungWeg() } }
    AnimatedVisibility(text != null, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Row(
            Modifier.navigationBarsPadding().padding(start = 16.dp, end = 104.dp, bottom = 26.dp).fillMaxWidth()
                .glas(f, 18.dp, 2f, f.flaecheStark).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(vm.meldung.orEmpty(), color = f.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            vm.rueckgaengig?.let { r ->
                Text("Rückgängig", color = f.primaer, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.antippen { r(); vm.meldungWeg() }.padding(start = 12.dp))
            }
        }
    }
}

/** Konfetti bei jeder erledigten Aufgabe. */
@Composable
private fun Konfetti(ausloeser: Int) {
    val f = LocalFarben.current
    val fort = remember { Animatable(1f) }
    LaunchedEffect(ausloeser) {
        if (ausloeser > 0) { fort.snapTo(0f); fort.animateTo(1f, tween(1700, easing = LinearEasing)) }
    }
    if (fort.value < 1f) Canvas(Modifier.fillMaxSize()) {
        val t = fort.value
        val farben = listOf(f.primaer, f.sekundaer, f.tertiaer, Color.White, f.primaer.copy(alpha = 0.6f))
        for (i in 0 until 70) {
            val wkl = (i * 47f) * PI.toFloat() / 180f
            val v = size.height * (0.5f + (i % 7) * 0.07f)
            val x = size.width / 2 + cos(wkl) * v * t * 0.7f
            val y = size.height * 0.62f - kotlin.math.abs(sin(wkl)) * v * t * 1.1f + size.height * 0.9f * t * t
            rotate(t * 720f + i * 31f, Offset(x, y)) {
                drawRect(farben[i % farben.size].copy(alpha = (1f - t).coerceIn(0f, 1f)), Offset(x - 7f, y - 4f), Size(14f, 8f))
            }
        }
    }
}

@Composable
private fun FokusBildschirm(vm: AppViewModel, id: Long?) {
    val alle by vm.aufgaben.collectAsState()
    val a = id?.let { i -> alle.firstOrNull { it.id == i } }
    // Bildschirm bleibt an, Status- und Navigationsleiste verschwinden (Wischen holt sie kurz zurück).
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        val fenster = (view.context as? Activity)?.window
        val leisten = fenster?.let { WindowCompat.getInsetsController(it, view) }
        leisten?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        leisten?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            view.keepScreenOn = false
            leisten?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().displayCutoutPadding().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        if (maxWidth > maxHeight && maxHeight < 560.dp) {
            // Querformat: Uhr links, Zeitwahl rechts, damit Schnellwahl und Regler immer erreichbar bleiben.
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                FokusMitte(vm, a, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        RundKnopf(Icons.Rounded.Close, "Beenden") { vm.fokusBeenden() }
                    }
                    FokusZeitwahl(vm)
                }
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth()) {
                    RundKnopf(Icons.Rounded.Close, "Beenden") { vm.fokusBeenden() }
                }
                FokusMitte(vm, a, Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                FokusZeitwahl(vm)
            }
        }
    }
}

/** Titel, Uhr und Knöpfe; die Uhr richtet ihre Größe nach dem Platz, der übrig ist. */
@Composable
private fun FokusMitte(vm: AppViewModel, a: Aufgabe?, modifier: Modifier) {
    val f = LocalFarben.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val kompakt = maxHeight < 420.dp
        val seite = minOf(maxWidth * 0.86f, 330.dp, maxHeight - if (kompakt) 100.dp else 210.dp).coerceAtLeast(110.dp)
        val knopf = if (kompakt) 56.dp else 72.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!kompakt) Text("Fokus", color = f.textLeise, fontSize = 16.sp)
            Text(
                a?.titel ?: if (vm.fokusLaeuft) "Ganz bei der Sache" else "Zeit wählen und los",
                color = f.text, fontSize = if (kompakt) 18.sp else 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2,
                modifier = Modifier.padding(top = if (kompakt) 0.dp else 6.dp, bottom = if (kompakt) 8.dp else 18.dp),
            )
            FokusUhr(vm, seite)
            Spacer(Modifier.height(if (kompakt) 10.dp else 22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                val bereit = vm.fokusGesamt > 0
                Box(
                    Modifier.size(knopf).graphicsLayer { alpha = if (bereit) 1f else 0.45f }
                        .knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel)
                        .antippen { if (vm.fokusLaeuft) vm.fokusPause() else if (bereit) { vm.fokusMerken(); vm.fokusFortsetzen() } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (vm.fokusLaeuft) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (vm.fokusLaeuft) "Pause" else "Start", tint = Color.White, modifier = Modifier.size(knopf / 2))
                }
                if (a != null) Box(Modifier.size(knopf).knopf3d(f.erfolg, f.erfolg.copy(alpha = 0.7f), 99.dp, f.dunkel).antippen { vm.erledigen(a, true); vm.fokusBeenden() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, "Erledigt", tint = Color.White, modifier = Modifier.size(knopf / 2))
                }
            }
        }
    }
}

/** Animierte Fokus-Uhr: atmender Glaskreis, Fortschrittsring mit leuchtender Spitze, kreisende Lichtpunkte und Wellen. */
@Composable
private fun FokusUhr(vm: AppViewModel, seite: Dp) {
    val f = LocalFarben.current
    val gesamt = vm.fokusGesamt.coerceAtLeast(1L).toFloat()
    val anteil by animateFloatAsState(if (vm.fokusGesamt > 0) 1f - vm.fokusRest / gesamt else 0f, tween(300), label = "fokus")
    val leben by animateFloatAsState(if (vm.fokusLaeuft) 1f else 0.3f, tween(900), label = "leben")
    val takt = rememberInfiniteTransition(label = "takt")
    val atem by takt.animateFloat(0f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "atem")
    val dreh by takt.animateFloat(0f, 360f, infiniteRepeatable(tween(36_000, easing = LinearEasing)), label = "dreh")
    val welle by takt.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "welle")
    val skala = 1f + (atem - 0.5f) * 0.08f * leben
    Box(Modifier.size(seite), contentAlignment = Alignment.Center) {
        // Hintergrund: Leuchten, Wellen, gestrichelter Außenring und kreisende Lichtpunkte.
        Canvas(Modifier.fillMaxSize()) {
            val m = center
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(f.primaer.copy(alpha = 0.10f + 0.16f * leben * atem), Color.Transparent), m, r), r, m)
            for (i in 0 until 3) {
                val p = (welle + i / 3f) % 1f
                drawCircle(f.sekundaer.copy(alpha = (1f - p) * 0.30f * leben), r * (0.66f + 0.34f * p), m, style = Stroke(1.5.dp.toPx()))
            }
            rotate(dreh, m) {
                drawCircle(
                    f.textSchwach.copy(alpha = 0.35f), r * 0.95f, m,
                    style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 9.dp.toPx()))),
                )
            }
            val punktFarben = listOf(f.primaer, f.sekundaer, f.tertiaer)
            for (i in 0 until 14) {
                // Ganzzahlige Umläufe, damit die Bewegung beim Neustart der Drehung nahtlos weiterläuft.
                val umlauf = if (i % 3 == 0) -1 else if (i % 3 == 1) 1 else 2
                val wkl = (i * 360f / 14f + dreh * umlauf) * PI.toFloat() / 180f
                val bahn = r * (0.80f + 0.03f * (i % 4)) * skala
                val funkeln = 0.5f + 0.5f * sin(wkl * 3f + i)
                drawCircle(
                    punktFarben[i % 3].copy(alpha = (0.25f + 0.55f * funkeln) * (0.4f + 0.6f * leben)),
                    (1.6f + 1.6f * (i % 3) * 0.5f).dp.toPx(),
                    Offset(m.x + cos(wkl) * bahn, m.y + sin(wkl) * bahn),
                )
            }
        }
        Box(Modifier.fillMaxSize(0.74f).graphicsLayer { scaleX = skala; scaleY = skala }.glas(f, 999.dp, 2f))
        // Fortschrittsring mit leuchtender Spitze.
        Canvas(Modifier.fillMaxSize(0.66f)) {
            val w = 14.dp.toPx()
            val bogen = Size(size.width - w, size.height - w)
            drawArc(f.textSchwach.copy(alpha = 0.2f), 0f, 360f, false, Offset(w / 2, w / 2), bogen, style = Stroke(w))
            if (anteil > 0f) {
                rotate(-90f) {
                    drawArc(Brush.sweepGradient(listOf(f.sekundaer, f.primaer, f.sekundaer)), 0f, 360f * anteil, false, Offset(w / 2, w / 2), bogen, style = Stroke(w, cap = StrokeCap.Round))
                }
                val wkl = (-90f + 360f * anteil) * PI.toFloat() / 180f
                val rr = (size.width - w) / 2
                val spitze = Offset(center.x + cos(wkl) * rr, center.y + sin(wkl) * rr)
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f + 0.35f * atem * leben), f.primaer.copy(alpha = 0.35f), Color.Transparent), spitze, w * 1.6f), w * 1.6f, spitze)
                drawCircle(Color.White, w * 0.28f, spitze)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val rest = vm.fokusRest / 1000
            Text(
                if (rest >= 3600) "%d:%02d:%02d".format(rest / 3600, rest / 60 % 60, rest % 60) else "%d:%02d".format(rest / 60, rest % 60),
                color = f.text, fontSize = (seite.value / 6.4f).coerceIn(26f, 52f).sp, fontWeight = FontWeight.Light,
            )
            Text(
                when {
                    vm.fokusGesamt == 0L -> "Zeit wählen"
                    vm.fokusLaeuft -> "von ${minutenText((vm.fokusGesamt / 60_000L).toInt())}"
                    vm.fokusRest == 0L -> "geschafft"
                    vm.fokusRest == vm.fokusGesamt -> "bereit"
                    else -> "pausiert"
                },
                color = f.textLeise, fontSize = 13.sp,
            )
        }
    }
}

private val FOKUS_STUFEN = listOf(15, 30, 45, 60)
private val FOKUS_RASTEN = listOf(0, 15, 30, 45, 60, 75, 90, 105, 120)

private fun minutenText(m: Int): String = when {
    m >= 60 && m % 60 == 0 -> "${m / 60} Std."
    m > 60 -> "${m / 60} Std. ${m % 60} Min."
    else -> "$m Min."
}

/** Schnellwahl 15/30/45/60 und Schieberegler 0–120 Minuten, der an den Viertelstunden einrastet. */
@Composable
private fun FokusZeitwahl(vm: AppViewModel) {
    val f = LocalFarben.current
    val haptik = LocalHapticFeedback.current
    val minuten = (vm.fokusGesamt / 60_000L).toInt()
    Column(Modifier.fillMaxWidth().widthIn(max = 520.dp).glas(f, 26.dp, 1f, f.flaecheStark).padding(horizontal = 14.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FOKUS_STUFEN.forEach { m ->
                val aktiv = minuten == m
                Box(
                    Modifier.weight(1f).height(42.dp)
                        .then(if (aktiv) Modifier.knopf3d(f.primaer, f.sekundaer, 999.dp, f.dunkel) else Modifier.glas(f, 999.dp, 0.4f, f.flaeche))
                        .antippen { vm.fokusDauer(m, starten = true); vm.fokusMerken() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (m == 60) "1 Std." else "$m Min.", color = if (aktiv) Color.White else f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Eigene Zeit", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(if (minuten == 0) "0 Min." else minutenText(minuten), color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            minuten.toFloat(),
            { roh ->
                // Magnetisch: nahe einer Viertelstunde springt der Regler auf sie und gibt einen spürbaren Klick.
                val rast = FOKUS_RASTEN.firstOrNull { abs(roh - it) < 2.5f }
                val neu = rast ?: roh.roundToInt()
                if (neu != minuten) {
                    if (rast != null) haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.fokusDauer(neu)
                }
            },
            onValueChangeFinished = { vm.fokusMerken() },
            valueRange = 0f..120f,
            colors = SliderDefaults.colors(thumbColor = f.primaer, activeTrackColor = f.primaer, inactiveTrackColor = f.textSchwach.copy(alpha = 0.3f)),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            listOf("0", "30 Min.", "1 Std.", "1,5 Std.", "2 Std.").forEachIndexed { i, t ->
                Text(
                    t, color = f.textSchwach, fontSize = 11.sp, modifier = Modifier.weight(1f),
                    textAlign = when (i) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center },
                )
            }
        }
    }
}
