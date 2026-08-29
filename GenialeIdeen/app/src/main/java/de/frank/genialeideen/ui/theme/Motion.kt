package de.frank.genialeideen.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Alle Bewegungswerte stehen hier — einmal zentral (Baustein N.3). Nirgends im Code
 * stehen handgetippte Dauern.
 */
object Motion {
    /** Mikro-Rückmeldung: Druck, Haken, Umschalter. */
    const val MIKRO_MS = 130

    /** Zustandswechsel innerhalb eines Bildschirms. */
    const val ZUSTAND_MS = 280

    /** Bildschirmwechsel und geteilte Elemente. */
    const val BILDSCHIRM_MS = 400

    /** Versatz je Listenelement beim gestaffelten Einblenden. */
    const val STAFFEL_MS = 50

    /** Wie weit ein Listenelement beim Einblenden hochgleitet. */
    const val STAFFEL_HUB_DP = 12

    fun <T> mikro(reduziert: Boolean = false): AnimationSpec<T> = if (reduziert) {
        tween(0)
    } else {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
    }

    fun <T> zustand(reduziert: Boolean = false): AnimationSpec<T> = if (reduziert) {
        tween(0)
    } else {
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    fun <T> bildschirm(reduziert: Boolean = false): AnimationSpec<T> = if (reduziert) {
        tween(0)
    } else {
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    }

    /**
     * Ist im System „Animationen reduzieren" gesetzt, laufen Dauerbewegungen gar nicht erst
     * an und alle Dauern gehen auf nahe null (Baustein N.4).
     */
    fun bewegungReduziert(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
}

val LocalBewegungReduziert = staticCompositionLocalOf { false }
