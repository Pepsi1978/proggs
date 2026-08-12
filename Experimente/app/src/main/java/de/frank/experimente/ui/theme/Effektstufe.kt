package de.frank.experimente.ui.theme

import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * **F-41 — die Effekt-Stärke.** Drei Stufen, sofort wirksam, ohne Neustart.
 *
 * Der Entwurf trägt sie als `data-effekte` am Wurzelelement:
 * - `Voll` — alle Effekte aus UI-Spec §7 sind aktiv.
 * - `Gedämpft` — `[data-dauerbewegung]{animation:none; opacity:.45}`: Dauerbewegungen,
 *   Partikel und die Kipp-Parallaxe stehen still, bleiben aber sichtbar. Übergänge,
 *   Weichzeichnen und Verläufe bleiben.
 * - `Aus` — `*{animation:none; transition:none}` und `[data-dauerbewegung]{display:none}`:
 *   nur einfache Überblendungen, keine Verläufe in Bewegung, keine Partikel.
 *
 * **Kein Effekt trägt Information allein.** Auf *Aus* bleibt jede Funktion vollständig
 * bedienbar — was ein Leuchten sagt, sagt daneben auch ein Wort oder eine Form.
 */
enum class Effektstufe(val schluessel: String, val anzeige: String) {
    VOLL("voll", "Voll"),
    GEDAEMPFT("gedaempft", "Gedämpft"),
    AUS("aus", "Aus");

    /** Laufen Dauerbewegungen (Lichtgrund, wandernder Rand, Atmen, Schimmer)? */
    val dauerbewegung: Boolean get() = this == VOLL

    /** Sind Dauerbewegungs-Elemente überhaupt sichtbar? Auf *Aus* verschwinden sie. */
    val dauerbewegungSichtbar: Boolean get() = this != AUS

    /** Die Deckkraft stillstehender Dauerbewegungen auf *Gedämpft* (`opacity:.45`). */
    val dauerbewegungDeckkraft: Float get() = if (this == GEDAEMPFT) 0.45f else 1f

    /** Partikel (E-15 Funken, E-16 Lichtblüte) laufen nur auf ausdrückliche Handlung. */
    val partikel: Boolean get() = this == VOLL

    /** Kipp-Parallaxe (E-08) und Scroll-Parallaxe (E-09). */
    val parallaxe: Boolean get() = this == VOLL

    /** Weichzeichnen (E-03 Glasflächen, E-12 Bildschirmwechsel) und Verläufe. */
    val weichzeichnen: Boolean get() = this != AUS

    /** Federphysik (E-07); auf *Aus* bleibt ein einfacher Übergang. */
    val federphysik: Boolean get() = this != AUS

    /** Gestaffeltes Erscheinen (E-10) und der Schein (E-05). */
    val schein: Boolean get() = this != AUS

    companion object {
        fun aus(schluessel: String?): Effektstufe =
            entries.firstOrNull { it.schluessel == schluessel } ?: VOLL
    }
}

val LocalEffektstufe = staticCompositionLocalOf { Effektstufe.VOLL }

/**
 * Die tatsächlich geltende Stufe.
 *
 * **A-30:** Meldet das System „Bewegung reduzieren“ oder ist der Energiesparmodus an, gilt
 * mindestens *Gedämpft* — auch wenn *Voll* eingestellt ist. Frank kann so nicht versehentlich
 * gegen seine eigene Systemeinstellung laufen.
 */
@Composable
fun geltendeStufe(gewaehlt: Effektstufe): Effektstufe {
    val ctx = LocalContext.current
    val sparmodus = androidx.compose.runtime.remember(ctx) {
        ctx.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    val gebremst = bewegungReduziert() || sparmodus
    return if (gebremst && gewaehlt == Effektstufe.VOLL) Effektstufe.GEDAEMPFT else gewaehlt
}

/**
 * Die gemessene Dauer — oder 0 ms, wenn die Stufe *Aus* jede Bewegung abstellt
 * (`*{transition:none}`) bzw. das System keine Bewegung will.
 */
@Composable
fun dauer(gemessen: Int, stufe: Effektstufe): Int =
    if (stufe == Effektstufe.AUS) 0 else dauer(gemessen)
