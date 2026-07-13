package de.frank.entropyreducer.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb

/**
 * Kontinuierliche Prioritaets-Farbrampe (Frank-Wunsch 2026-05-31).
 *
 * EINZIGE QUELLE fuer die Prioritaets-Verlaufsfarbe — wird sowohl von der App-Aufgabenkarte
 * (TasksScreen.EntropyEntryCard) als auch vom Home-Screen- Widget
 * (EntropyReducerRemoteViewsService) genutzt. Dadurch sind die Farben GARANTIERT bit-identisch
 * (Poka-Yoke Stufe 3): App und Widget koennen nie auseinanderlaufen, weil beide dieselbe Funktion
 * aufrufen.
 *
 * Der Score wird auf den naechsten 5%-Schritt gerundet (0,5,…,100), sodass JEDER 5%-Schritt eine
 * eigene, klar unterscheidbare Farbe hat. Die fuenf sichtbaren Prioritaetsbereiche behalten dabei
 * jeweils ihre eigene Farbfamilie und variieren nur von hell nach dunkel:
 * - 80..100 : Rot
 * - 60..79  : Orange
 * - 40..59  : Gelb
 * - 20..39  : Gruen
 * - 0..19   : Blau
 */

// Anker-Farben fuer die fuenf Prioritaetsbereiche. Innerhalb eines Bereichs wird zwischen
// "hell" (niedriges Ende) und "dunkel" (hohes Ende) interpoliert, nie in eine Nachbarfarbe.
private val PRIO_LIGHT_RED = Color(0xFFFCA5A5)
private val PRIO_DARK_RED = Color(0xFFB91C1C)
private val PRIO_LIGHT_ORANGE = Color(0xFFFDBA74)
private val PRIO_DARK_ORANGE = Color(0xFFC2410C)
private val PRIO_LIGHT_YELLOW = Color(0xFFFDE047)
private val PRIO_DARK_YELLOW = Color(0xFFCA8A04)
private val PRIO_LIGHT_GREEN = Color(0xFF86EFAC)
private val PRIO_DARK_GREEN = Color(0xFF15803D)
private val PRIO_LIGHT_BLUE = Color(0xFF93C5FD)
private val PRIO_DARK_BLUE = Color(0xFF1D4ED8)

/** Compose-Variante — fuer die App-Karte (Brush/Tint). */
fun priorityRampColor(score: Double): Color {
    val stepped = (Math.round(score.coerceIn(0.0, 100.0) / 5.0) * 5).toDouble()
    fun rangeProgress(start: Double): Float = ((stepped - start) / 20.0).coerceIn(0.0, 1.0).toFloat()
    return when {
        score >= 80.0 -> lerp(PRIO_LIGHT_RED, PRIO_DARK_RED, rangeProgress(80.0))
        score >= 60.0 -> lerp(PRIO_LIGHT_ORANGE, PRIO_DARK_ORANGE, rangeProgress(60.0))
        score >= 40.0 -> lerp(PRIO_LIGHT_YELLOW, PRIO_DARK_YELLOW, rangeProgress(40.0))
        score >= 20.0 -> lerp(PRIO_LIGHT_GREEN, PRIO_DARK_GREEN, rangeProgress(20.0))
        else -> lerp(PRIO_LIGHT_BLUE, PRIO_DARK_BLUE, rangeProgress(0.0))
    }
}

/**
 * ARGB-Int-Variante — fuer das klassische RemoteViews-Widget (kann keine Compose-Color verwenden).
 * Ruft dieselbe Funktion auf wie die App und konvertiert nur das Ergebnis → identische Farbe.
 */
fun priorityRampArgb(score: Double): Int = priorityRampColor(score).toArgb()
