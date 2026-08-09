package de.frank.experimente.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

/**
 * Die benannten Kurven und Dauern aus 03-MOTION-SPEC.md §2.
 *
 * Diese Namen werden überall sonst nur noch referenziert, nie neu erfunden. Jede Kurve ist
 * die exakte `cubic-bezier` aus dem Spec — **keine** eingebaute Standardkurve wie
 * `FastOutSlowIn` als Ersatz.
 */
object Bewegung {

    /** Standard: Erscheinen, Hereinschieben. `cubic-bezier(0.2, 0, 0, 1)` */
    const val RUHIG_MS = 240
    val ruhig: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Druckrückmeldung, Verschwinden. `cubic-bezier(0.3, 0, 0.8, 0.15)` */
    const val KNAPP_MS = 120
    val knapp: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Große Flächen, die Auswertung. `cubic-bezier(0.4, 0, 0.2, 1)` */
    const val WEICH_MS = 400
    val weich: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Bildschirmwechsel über die untere Leiste. `cubic-bezier(0.4, 0, 0.6, 1)` */
    const val BLENDEN_MS = 200
    val blenden: Easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

    /** Karten, die weggehen. `cubic-bezier(0.4, 0, 1, 1)` */
    const val HINAUS_MS = 140
    val hinaus: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Ein Haken zeichnet sich. `cubic-bezier(0.2, 0, 0, 1)` */
    const val HAKEN_MS = 180
    val haken: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * Eine KI-Antwort erscheint (M-08) — auf B-02 die Gesprächsblase, auf B-03 die Auswertung.
     * Im Design gemessen als `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`, 400 ms.
     */
    const val ANTWORT_MS = 400
    val antwort: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    /**
     * Laufende Aufnahme — die einzige Dauerbewegung der App.
     * Im Design gemessen als `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`,
     * 3200 ms, endlos, `alternate`.
     */
    const val ATMEN_MS = 3200
    val atmen: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    /** Wartezustand der KI — wandernder Streifen, `linear`. */
    const val WANDERN_MS = 1800
    val wandern: Easing = LinearEasing

    /** Versatz zwischen zwei gestaffelt erscheinenden Vorschlagskarten (M-04). */
    const val STAFFEL_MS = 40

    /** Erst ab dieser Wartezeit zeigt sich ein Ladezustand (§7). */
    const val WARTE_SCHWELLE_MS = 400L
}
