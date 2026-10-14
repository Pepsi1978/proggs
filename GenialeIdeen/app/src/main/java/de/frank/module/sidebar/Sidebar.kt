// ──────────────────────────────────────────────────────────────────────
// Modul M1.3 — Sidebar · Stand v1
// Quelle: Module/Android/M1.3-Sidebar/
//
// Diese Datei ist eine 1:1-Kopie. Änderungen bitte NUR im Modul vornehmen
// und danach mit "zieh M1.3 nach" an die Konsumenten verteilen —
// sonst driftet diese App still von der Bibliothek weg.
// ──────────────────────────────────────────────────────────────────────
package de.frank.module.sidebar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Linke Seitenleiste, die per Wisch von links nach rechts aufgeht — ohne Ruckeln.
 *
 * Der Trick: Der Finger zieht die Leiste nicht mit. Eine waagerechte Wischgeste wird
 * vor dem nativen Drawer-Drag erkannt und startet die durchgehende open()/close()-Animation.
 * Der native Drag ist nur bei offener, ruhender Leiste an, damit er nichts vorschaltet.
 *
 * `drawerContent` bitte NICHT als LazyColumn bauen (siehe MODUL.md, „Host muss liefern").
 */
@Composable
fun Sidebar(
    schublade: DrawerState,
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bereichsraum = rememberCoroutineScope()

    // Eine offene Schublade schliesst, statt die App zu beenden.
    BackHandler(enabled = schublade.isOpen) { bereichsraum.launch { schublade.close() } }

    ModalNavigationDrawer(
        drawerState = schublade,
        gesturesEnabled = schublade.isOpen && !schublade.isAnimationRunning,
        modifier = modifier.sidebarWischGeste(schublade, bereichsraum),
        drawerContent = drawerContent,
        content = content,
    )
}

/** Erkennt das waagerechte Auf- und Zuwischen und spielt die Animation in einem Zug ab. */
fun Modifier.sidebarWischGeste(schublade: DrawerState, bereichsraum: CoroutineScope): Modifier =
    pointerInput(schublade) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (schublade.isAnimationRunning) return@awaitEachGesture
            val schliessen = schublade.isOpen
            var weg = Offset.Zero
            var erkannt = false
            while (true) {
                // Vor dem nativen Drawer-Drag übernehmen, damit auch beim Schließen
                // kein Finger-Offset vor die durchgehende Animation geschaltet wird.
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (erkannt) {
                    change.consume()
                    if (!change.pressed) break
                    continue
                }
                if (!change.pressed || change.isConsumed) break
                weg += change.position - change.previousPosition
                if (abs(weg.y) > viewConfiguration.touchSlop && abs(weg.y) >= abs(weg.x)) break
                if (abs(weg.x) > viewConfiguration.touchSlop) {
                    if ((schliessen && weg.x >= 0f) || (!schliessen && weg.x <= 0f)) break
                    if (schublade.isAnimationRunning) break
                    erkannt = true
                    change.consume()
                    bereichsraum.launch {
                        if (schliessen) schublade.close() else schublade.open()
                    }
                } else {
                    // Bereits von einer scrollenden Liste übernommene Gesten nicht stehlen.
                    val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                    if (finalEvent.changes.any { it.id == down.id && it.isConsumed }) break
                }
            }
        }
    }
