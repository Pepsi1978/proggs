package de.frank.wecker.design

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import kotlin.math.PI
import kotlin.math.sin

/**
 * Morgenruhes Tag im Zeitraffer: eine gemeinsame Uhr für die Schlafszene im Kopf und den Himmel im
 * Hintergrund. Beide lesen dieselbe Prozessuhr — Szene und Himmel zeigen deshalb immer dieselbe
 * Tageszeit, auch im eigenen Fenster des Weckbildschirms.
 *
 * Ablauf (Sekunden im Zyklus von [ZYKLUS]):
 * - 0–6 Tag: Die Sonne steht hoch, die Person ist unterwegs, Schmetterlinge fliegen.
 * - 6–14 Abend: Die Sonne geht langsam unter; ab 11 geht die Tür auf, die Person kommt heim.
 * - 11–29,5 Nacht: Der Mond geht auf und strahlt, eine Fledermaus kreist, die Person schläft.
 * - 26,5–30,5 Morgen: Die Sonne geht auf, die Person wacht auf, streckt sich und geht wieder.
 * - 30,5–46 Tag.
 *
 * Alle Bewegungen, die bei Tag zu sehen sind, laufen mit ganzzahliger Frequenz über den Zyklus —
 * am Übergang von 46 auf 0 springt nichts.
 */
internal object Tageslauf {
    const val ZYKLUS = 46f
    /** Bei reduzierter Bewegung: ein ruhiges Standbild mitten in der Nacht, die Person schläft. */
    const val STANDBILD = 22f

    private val beginn = SystemClock.uptimeMillis()

    fun jetzt(): Float = ((SystemClock.uptimeMillis() - beginn) % (ZYKLUS * 1000f).toLong()) / 1000f

    fun ab(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
    fun weich(x: Float) = x * x * (3f - 2f * x)
    /** 0 → 1 zwischen a und b, hält, 1 → 0 zwischen c und d; außerhalb 0. */
    fun huegel(t: Float, a: Float, b: Float, c: Float, d: Float) =
        if (t < a || t > d) 0f else if (t < c) weich(ab(t, a, b)) else 1f - weich(ab(t, c, d))

    /** Eine Schwingung mit [k] ganzen Perioden je Zyklus, also nahtlos über den Neubeginn. */
    fun welle(t: Float, k: Int, phase: Float = 0f) = sin(2f * PI.toFloat() * (k * t / ZYKLUS) + phase)

    /** Tageslicht: 1 = heller Tag, 0 = Nacht. Langsamer Untergang 6–14, Aufgang 26,5–30,5. */
    fun tag(t: Float): Float = when {
        t < 6f -> 1f
        t < 14f -> 1f - weich(ab(t, 6f, 14f))
        t < 26.5f -> 0f
        t < 30.5f -> weich(ab(t, 26.5f, 30.5f))
        else -> 1f
    }

    /** Wie stark der Mond zu sehen ist: Er geht mit dem Abend auf und verblasst im Morgen. */
    fun mond(t: Float): Float = huegel(t, 11f, 16f, 26f, 29.5f)

    /** Dämmerung — warmes Abend- und Morgenrot, am stärksten, wenn die Sonne am Horizont steht. */
    fun daemmerung(t: Float): Float = huegel(t, 7f, 11.5f, 12.5f, 15f) + huegel(t, 25.5f, 27.5f, 28.5f, 31f)

    /**
     * Wo die Sonne auf ihrem Bogen steht: 0 = Aufgang (unten links), 0,5 = Mittag, 1 = Untergang
     * (unten rechts). Von 26,5 bis 14 des nächsten Zyklus gleichmäßig, also ohne Sprung.
     */
    fun sonnenBogen(t: Float): Float {
        val seitAufgang = if (t >= 26.5f) t - 26.5f else t + ZYKLUS - 26.5f
        return (seitAufgang / (ZYKLUS - 26.5f + 14f)).coerceIn(0f, 1f)
    }

    /** Der Mondbogen von seinem Aufgang (11) bis zum Untergang (29,5). */
    fun mondBogen(t: Float): Float = ab(t, 11f, 29.5f)
}

/**
 * Die laufende Zeit im Tageslauf, rund 30-mal pro Sekunde aufgefrischt — das reicht für die ruhigen
 * Bewegungen und schont den Akku. Gelesen werden soll sie nur im Zeichenblock, dann zeichnet allein
 * die Ebene neu, die sie liest. Steht bei [aktiv] = `false` still.
 */
@Composable
internal fun rememberTageszeit(aktiv: Boolean): State<Float> {
    val zeit = remember { mutableFloatStateOf(if (aktiv) Tageslauf.jetzt() else Tageslauf.STANDBILD) }
    LaunchedEffect(aktiv) {
        if (!aktiv) {
            zeit.floatValue = Tageslauf.STANDBILD
            return@LaunchedEffect
        }
        var letzte = 0L
        while (true) {
            withFrameMillis { t ->
                if (t - letzte >= 32) {
                    zeit.floatValue = Tageslauf.jetzt()
                    letzte = t
                }
            }
        }
    }
    return zeit
}
