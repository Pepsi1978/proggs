package de.frank.ziele.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.frank.ziele.ui.theme.ZieleTheme

@Composable
fun ZieleApp(vm: AppViewModel, activity: ComponentActivity) {
    ZieleTheme(vm.einstellungen.modus) {
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
                    is Bildschirm.Detail -> DetailBildschirm(vm, b.id)
                    is Bildschirm.Bearbeiten -> BearbeitenBildschirm(vm)
                    Bildschirm.Kontext -> KontextBildschirm(vm)
                    Bildschirm.Einstellungen -> EinstellungenBildschirm(vm, activity)
                }
            }
            Meldung(vm, Modifier.align(Alignment.BottomCenter))
        }
    }
}
