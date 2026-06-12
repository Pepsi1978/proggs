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
 * eigene, klar unterscheidbare Farbe hat. Drei Farb- familien ueber je ~33,3 % des Bereichs (kein
 * Blau):
 * - 66,7..100 : Rot — hellrot (66,7) → dunkelrot (100)
 * - 33,3..66,7: Gelb — hellgelb (33,3) → dunkelorange (66,7)
 * - 0..33,3 : Gruen — hellgruen (0) → dunkelgruen (33,3)
 */

// Anker-Farben fuer die drei Farbfamilien. Innerhalb jeder Familie wird zwischen
// "hell" (niedriges Ende) und "dunkel" (hohes Ende) interpoliert.
private val PRIO_DARK_RED = Color(0xFF7F1D1D) // 100 % — dunkelrot
private val PRIO_LIGHT_RED = Color(0xFFF87171) // ~67 % — hellrot
private val PRIO_DARK_ORANGE = Color(0xFFEA580C) // ~67 % — dunkelorange/-gelb
private val PRIO_LIGHT_YELLOW = Color(0xFFFDE047) // ~33 % — hellgelb
private val PRIO_DARK_GREEN = Color(0xFF15803D) // ~33 % — dunkelgruen
private val PRIO_LIGHT_GREEN = Color(0xFF86EFAC) // 0 % — hellgruen

/** Compose-Variante — fuer die App-Karte (Brush/Tint). */
fun priorityRampColor(score: Double): Color {
    val stepped = (Math.round(score.coerceIn(0.0, 100.0) / 5.0) * 5).toDouble()
    val third = 100.0 / 3.0
    return when {
        stepped >= 2 * third -> {
            val t = ((stepped - 2 * third) / (100.0 - 2 * third)).coerceIn(0.0, 1.0)
            lerp(PRIO_LIGHT_RED, PRIO_DARK_RED, t.toFloat())
        }
        stepped >= third -> {
            val t = ((stepped - third) / third).coerceIn(0.0, 1.0)
            lerp(PRIO_LIGHT_YELLOW, PRIO_DARK_ORANGE, t.toFloat())
        }
        else -> {
            val t = (stepped / third).coerceIn(0.0, 1.0)
            lerp(PRIO_LIGHT_GREEN, PRIO_DARK_GREEN, t.toFloat())
        }
    }
}

/**
 * ARGB-Int-Variante — fuer das klassische RemoteViews-Widget (kann keine Compose-Color verwenden).
 * Ruft dieselbe Funktion auf wie die App und konvertiert nur das Ergebnis → identische Farbe.
 */
fun priorityRampArgb(score: Double): Int = priorityRampColor(score).toArgb()
