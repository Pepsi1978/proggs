package de.frank.aufgaben.ui

import androidx.activity.ComponentActivity
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
        val farben = listOf(f.primaer, f.sekundaer, f.tertiaer, f.erfolg, Color(0xFFFFD166))
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
private fun FokusBildschirm(vm: AppViewModel, id: Long) {
    val f = LocalFarben.current
    val alle by vm.aufgaben.collectAsState()
    val a = alle.firstOrNull { it.id == id }
    val gesamt = vm.einstellungen.fokusMinuten * 60_000f
    val anteil by animateFloatAsState(1f - vm.fokusRest / gesamt, tween(300), label = "fokus")
    val atmen = rememberInfiniteTransition(label = "atmen")
    val a1 by atmen.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "a1")
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            RundKnopf(Icons.Rounded.Close, "Beenden") { vm.fokusBeenden() }
        }
        Spacer(Modifier.weight(1f))
        Text("Fokus", color = f.textLeise, fontSize = 16.sp)
        Text(a?.titel ?: "", color = f.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 30.dp))
        Box(Modifier.size(270.dp).graphicsLayer { if (vm.fokusLaeuft) { scaleX = a1; scaleY = a1 } }.glas(f, 999.dp, 2f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(230.dp)) {
                val w = 14.dp.toPx()
                drawArc(f.textSchwach.copy(alpha = 0.2f), 0f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
                drawArc(Brush.sweepGradient(listOf(f.primaer, f.sekundaer, f.primaer)), -90f, 360f * anteil, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
            }
            val rest = vm.fokusRest / 1000
            Text("%d:%02d".format(rest / 60, rest % 60), color = f.text, fontSize = 52.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(72.dp).knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel).antippen { if (vm.fokusLaeuft) vm.fokusPause() else vm.fokusFortsetzen() }, contentAlignment = Alignment.Center) {
                Icon(if (vm.fokusLaeuft) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (vm.fokusLaeuft) "Pause" else "Weiter", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            if (a != null) Box(Modifier.size(72.dp).knopf3d(f.erfolg, f.erfolg.copy(alpha = 0.7f), 99.dp, f.dunkel).antippen { vm.erledigen(a, true); vm.fokusBeenden() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, "Erledigt", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.weight(1.2f))
    }
}
