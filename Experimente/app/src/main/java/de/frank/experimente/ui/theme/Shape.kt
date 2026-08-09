package de.frank.experimente.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Formen aus 02-UI-SPEC.md §5 — gemessen im Design als 14 px / 20 px / 24 px / 9999 px.
 * Die Design-Bezugsgröße ist 412 × 915 dp bei density 1, px sind damit unmittelbar dp.
 *
 * Keine Schatten, keine Verläufe — Trennung nur über Fläche und 1 dp Rand.
 */
@Immutable
data class AppShapes(
    val karte: RoundedCornerShape = RoundedCornerShape(20.dp),
    val eingabefeld: RoundedCornerShape = RoundedCornerShape(14.dp),
    val knopf: RoundedCornerShape = RoundedCornerShape(14.dp),
    val dialog: RoundedCornerShape = RoundedCornerShape(24.dp),
    val vollrund: RoundedCornerShape = RoundedCornerShape(percent = 50),
    /** Gesprächsblase „Ich" — abgeflachte Ecke unten rechts, gemessen `20px 20px 6px 20px`. */
    val blaseIch: RoundedCornerShape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
    /** Gesprächsblase „KI" — abgeflachte Ecke unten links, gemessen `20px 20px 20px 6px`. */
    val blaseKi: RoundedCornerShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
)

val AppForm = AppShapes()
