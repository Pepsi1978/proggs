package de.frank.experimente.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

/**
 * Die gemessenen Bewegungskurven aus `Specs/Experimente/v2/03-MOTION-SPEC.md`.
 *
 * Jede steht hier als genau die `cubic-bezier`, die im Design gemessen wurde. Eine eingebaute
 * Standardkurve (`FastOutSlowIn` und Verwandte) wäre eine andere Bewegung — sie gilt laut
 * Bau-Auftrag als nicht erfüllt.
 */
object Bewegung {
    /** `cubic-bezier(.2, 0, 0, 1)` — die Hauskurve des Entwurfs (240 / 180 / 120 ms). */
    val ruhig: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** `cubic-bezier(.3, 0, .8, .15)` — Einsinken beim Drücken, 120 ms (M-01, M-11). */
    val einsinken: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** `cubic-bezier(.4, 0, .6, 1)` — Überblenden, 200 ms (M-63). */
    val blenden: Easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

    /** `cubic-bezier(0.42, 0, 0.58, 1)` — CSS `ease-in-out`, das Atmen des Sprechknopfs (M-02). */
    val atmen: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    /** `cubic-bezier(0.25, 0.1, 0.25, 1)` — CSS `ease`, 200 ms. */
    val ease: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** `cubic-bezier(0, 0, 0.58, 1)` — CSS `ease-out` (120 ms; 400 ms bei M-08). */
    val antwort: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    /** `cubic-bezier(0.4, 0, 0.2, 1)` — 240 ms, gestaffeltes Erscheinen (M-04). */
    val weich: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** `linear` — 100 / 200 / 0 ms. */
    val gerade: Easing = LinearEasing

    // Die gemessenen Dauern in ms — nicht gerundet, nicht vereinheitlicht.
    const val SOFORT = 0
    const val SEHR_KURZ = 100
    const val KURZ = 120
    const val MERKEN = 180
    const val MITTEL = 200
    const val LANG = 240
    const val ANTWORT = 400
    const val ATEM = 3200
}
