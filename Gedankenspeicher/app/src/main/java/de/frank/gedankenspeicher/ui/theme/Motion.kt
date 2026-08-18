package de.frank.gedankenspeicher.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * **Die sechs Kurven aus `03-MOTION-SPEC.md` §2 — und keine siebte.**
 *
 * Wer eine weitere Dauer braucht, hat die Bewegung falsch eingeordnet. Die Namen hier sind
 * dieselben wie im Spec, damit sich jede Bewegung Zeile für Zeile nachweisen lässt.
 */
object Kurven {
    /** Der Regelfall: Erscheinen, Verschwinden, Wechsel. */
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Rückmeldung auf Bedienung, Symbolwechsel. */
    val kurz: Easing = standard

    /** Blätter und Schublade — mehr Fläche, deshalb länger. */
    val blatt: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Erscheinungswechsel, Farbüberblendungen. */
    val weich: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Der Aufnahmering. */
    val puls: Easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

    /** Wanderndes Leuchten und Schimmer — gleichmäßig, ohne Beschleunigung. */
    val wandern: Easing = LinearEasing

    /** Das Überschwingen des neuen Häkchens (M-12). */
    val ueberschwingen: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
}

/** Die gemessenen Dauern in Millisekunden. */
object Dauern {
    const val STANDARD = 240
    const val KURZ = 120
    const val BLATT = 320
    const val WEICH = 400
    const val PULS = 1600
    const val WANDERN = 2400
}

/**
 * Meldet das System "Bewegung reduzieren" (`ANIMATOR_DURATION_SCALE == 0`), gilt
 * `03-MOTION-SPEC.md` §8: Dauerbewegungen aus, Übergänge nur noch als Überblendung,
 * alle Dauern halbiert.
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

/**
 * Gibt die Dauer zurück, die wirklich laufen soll — halbiert, wenn das System weniger
 * Bewegung will.
 *
 * Halbiert, nicht auf null: eine Bewegung von 0 ms lässt Zustände springen, und gerade beim
 * Wechsel der Erscheinung ist ein harter Sprung unangenehmer als eine kurze Blende. Was
 * ganz verschwindet, sind die **Dauerbewegungen** — die fragen `bewegungReduziert()` selbst ab.
 */
@Composable
@ReadOnlyComposable
fun dauer(gemessen: Int): Int = if (bewegungReduziertJetzt()) gemessen / 2 else gemessen

@Composable
@ReadOnlyComposable
private fun bewegungReduziertJetzt(): Boolean {
    val ctx = LocalContext.current
    return Settings.Global.getFloat(
        ctx.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}

/** Kurzform für die immer wieder gebrauchten Spezifikationen. */
@Composable
fun <T> standardSpec(): FiniteAnimationSpec<T> = tween(dauer(Dauern.STANDARD), easing = Kurven.standard)

@Composable
fun <T> kurzSpec(): FiniteAnimationSpec<T> = tween(dauer(Dauern.KURZ), easing = Kurven.kurz)

@Composable
fun <T> blattSpec(): FiniteAnimationSpec<T> = tween(dauer(Dauern.BLATT), easing = Kurven.blatt)

@Composable
fun <T> weichSpec(): FiniteAnimationSpec<T> = tween(dauer(Dauern.WEICH), easing = Kurven.weich)
