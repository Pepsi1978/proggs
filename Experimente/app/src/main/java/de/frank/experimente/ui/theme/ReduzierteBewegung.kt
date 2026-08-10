package de.frank.experimente.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Der Entwurf trägt einen `@media (prefers-reduced-motion: reduce)`-Block: dort laufen die
 * Bewegungen auf 0 ms. Auf Android ist das Gegenstück `ANIMATOR_DURATION_SCALE == 0`.
 *
 * Punkt 3 des Abnahme-Tors verlangt dieses Verhalten ausdrücklich — auch die Zustände, die
 * erst beim Drücken oder Aufnehmen entstehen.
 */
@Composable
fun bewegungReduziert(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) {
        Settings.Global.getFloat(
            ctx.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/** Gibt die gemessene Dauer zurück — oder 0 ms, wenn das System keine Bewegung will. */
@Composable
fun dauer(gemessen: Int): Int = if (bewegungReduziert()) 0 else gemessen
