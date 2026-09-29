package de.frank.longevity.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.LongevityTheme
import de.frank.longevity.ui.theme.antippen
import de.frank.longevity.ui.theme.glas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private fun Bildschirm.tiefe(): Int = when (this) {
    Bildschirm.Liste -> 0
    is Bildschirm.Detail, Bildschirm.Neu, Bildschirm.Einstellungen -> 1
    Bildschirm.Protokoll -> 2
}

@Composable
fun LongevityApp(vm: AppViewModel, activity: ComponentActivity) {
    LongevityTheme(vm.einstellungen.design, vm.einstellungen.modus) {
        Box(Modifier.fillMaxSize()) {
            Hintergrund(Modifier.fillMaxSize())
            BackHandler(vm.bildschirm != Bildschirm.Liste) { vm.zurueck() }
            AnimatedContent(
                vm.bildschirm,
                transitionSpec = {
                    val vor = targetState.tiefe() >= initialState.tiefe()
                    if (vor) (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.94f))
                    else (fadeIn(tween(220)) + scaleIn(initialScale = 0.95f))
                        .togetherWith(slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(180)))
                },
                label = "bildschirm",
            ) { b ->
                when (b) {
                    Bildschirm.Liste -> ListeBildschirm(vm)
                    is Bildschirm.Detail -> DetailBildschirm(vm, b.id)
                    Bildschirm.Neu -> NeuBildschirm(vm)
                    Bildschirm.Einstellungen -> EinstellungenBildschirm(vm, activity)
                    Bildschirm.Protokoll -> ProtokollBildschirm(vm)
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
    LaunchedEffect(text) { if (text != null) { delay(if (vm.rueckgaengig != null) 5000 else 2800); vm.meldungWeg() } }
    AnimatedVisibility(text != null, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Row(
            Modifier.navigationBarsPadding().padding(start = 16.dp, end = 96.dp, bottom = 26.dp).fillMaxWidth()
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

/** Konfetti für jeden erledigten Schritt. */
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
