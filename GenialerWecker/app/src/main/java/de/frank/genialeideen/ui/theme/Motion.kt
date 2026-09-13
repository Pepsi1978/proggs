package de.frank.genialeideen.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Alle Bewegungswerte stehen hier — einmal zentral (Baustein N.8). Nirgends im Code
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

    /** Der Atemrhythmus des goldenen Scheins (N.7). */
    const val ATEM_MS = 2500

    /** Auf- und Abschweben von Aktionsknopf und Abzeichen (N.7). */
    const val SCHWEBEN_MS = 4000

    /** Umlauf des wandernden Hintergrundscheins (N.3). */
    const val HINTERGRUND_MS = 24000

    /** Wanderndes Glanzlicht über Karten und Überschriften (N.7). */
    const val GLANZ_MS = 3800

    /** Seitliches Wackeln bei einem Fehler (N.7). */
    const val WACKELN_MS = 320L

    /** Der Theme-Wechsel läuft weich statt hart umzuspringen (N.5). */
    const val THEME_WECHSEL_MS = 420

    /** Schimmer über den Platzhalter-Gerüsten (Baustein L). */
    const val SCHIMMER_MS = 1400

    fun <T> mikro(reduziert: Boolean = false): AnimationSpec<T> = if (reduziert) {
        tween(0)
    } else {
        spring(dampingRatio = 0.55f, stiffness = 900f)
    }

    fun <T> zustand(reduziert: Boolean = false): AnimationSpec<T> = if (reduziert) {
        tween(0)
    } else {
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    fun <T> bildschirm(reduziert: Boolean = false): AnimationSpec<T> = if (reduziert) {
        tween(0)
    } else {
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
    }

    /**
     * Ist im System „Animationen reduzieren" gesetzt, laufen Dauerbewegungen gar nicht erst
     * an und alle Dauern gehen auf nahe null (Baustein N.9).
     */
    fun bewegungReduziert(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
}

val LocalBewegungReduziert = staticCompositionLocalOf { false }
