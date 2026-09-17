package de.frank.wecker

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.frank.genialeideen.ui.BewegterHintergrund
import kotlinx.coroutines.delay

/** True only while the screen is in front and resumed; animations and tickers run only then. */
@Composable
fun rememberResumed(): Boolean {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    return state.isAtLeast(Lifecycle.State.RESUMED)
}

/** Current time, refreshed every [periodMs] while resumed and immediately on return. */
@Composable
fun rememberNow(periodMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { now = System.currentTimeMillis(); delay(periodMs) }
        }
    }
    return now
}

/** The shared moving background, but only while resumed; otherwise the static page background remains. */
@Composable
fun SichtbarerHintergrund() {
    if (rememberResumed()) BewegterHintergrund()
}
