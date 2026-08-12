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

    // --- M-76 bis M-95: die Bewegungen des Monitors und der Effekte -----------------------
    // Alle aus `03-MOTION-SPEC.md` §2. Die Kurve ist verbindlich; eine eingebaute
    // Standardkurve an ihrer Stelle gilt als nicht erfüllt.

    /** M-76 — der Lichtgrund wandert. 24000 ms, `cubic-bezier(0.42,0,0.58,1)`, endlos wechselnd. */
    const val LICHTGRUND = 24_000

    /** M-77 — die Glasleiste verdichtet sich beim Scrollen. 200 ms, Hauskurve. */
    const val GLASLEISTE = 200

    /** M-79 — der Plus-Knopf atmet. 3200 ms, `cubic-bezier(0.42,0,0.58,1)`, endlos wechselnd. */
    const val PLUSATEM = 3200

    /** M-80 — die Anlegefläche fährt herein. 320 ms, Hauskurve. */
    const val ANLEGEN = 320

    /** M-81 — eine neue Karte fliegt ein und funkelt. 480 ms, `spring(0.6, 320)`. */
    const val EINFLUG = 480

    /** M-82 — der übernommene Vorschlag fliegt zum Monitor-Feld. 520 ms, Hauskurve. */
    const val ZUMMONITOR = 520

    /** M-83 — die Karte wandert nach „Läuft“. 400 ms, `spring(0.7, 260)`. */
    const val NACHLAEUFT = 400

    /** M-84 — Funken beim Start. 1200 ms, linear. */
    const val FUNKEN = 1_200

    /** M-85 — die Karte hebt sich beim Ziehen ab. 160 ms, Hauskurve. */
    const val ABHEBEN = 160

    /** M-86 — die Karte klappt auf. 280 ms, `spring(0.8, 300)`. */
    const val KLAPPEN = 280

    /** M-87 — der Fortschrittsring füllt sich. 600 ms, Hauskurve. */
    const val RING = 600

    /** M-88 — eine Zahl zählt hoch. 400 ms, Hauskurve. */
    const val ZAEHLEN = 400

    /** M-89 — der Schimmer über dem Skelett. 1400 ms, linear, endlos. */
    const val SCHIMMER = 1_400

    /** M-90 — der Lichtsaum wandert um die Laufkarte. 6000 ms, linear, endlos. */
    const val LICHTSAUM = 6_000

    /** M-91 — Bildschirmwechsel mit Weichzeichnen. 260 ms, Hauskurve. */
    const val WECHSEL = 260

    /** M-93 — die Lichtblüte beim Abschließen. 1600 ms, Hauskurve. */
    const val BLUETE = 1_600

    /** M-94 — das Leistenfeld leuchtet auf. 240 ms, Hauskurve, wechselnd. */
    const val LEUCHTEN = 240

    /** M-95 — gestaffeltes Erscheinen: 240 ms je Karte, 60 ms Versatz. */
    const val STAFFEL_VERSATZ = 60

    // Die gemessenen Federn. `spring(Dämpfung, Steifigkeit)` aus dem Motion-Spec.

    /** M-78 — die Karte kippt zur Neigung des Geräts: `spring(0.75, 200)`. */
    const val KIPP_DAEMPFUNG = 0.75f
    const val KIPP_STEIFE = 200f

    /** M-81 — Einflug: `spring(0.6, 320)`. */
    const val EINFLUG_DAEMPFUNG = 0.6f
    const val EINFLUG_STEIFE = 320f

    /** M-83 — nach „Läuft“: `spring(0.7, 260)`. */
    const val WANDERN_DAEMPFUNG = 0.7f
    const val WANDERN_STEIFE = 260f

    /** M-86 — Aufklappen: `spring(0.8, 300)`. */
    const val KLAPP_DAEMPFUNG = 0.8f
    const val KLAPP_STEIFE = 300f

    /** E-07 — Federphysik unter jedem Druck: `spring(0.55, 380)`. */
    const val DRUCK_DAEMPFUNG = 0.55f
    const val DRUCK_STEIFE = 380f
}
