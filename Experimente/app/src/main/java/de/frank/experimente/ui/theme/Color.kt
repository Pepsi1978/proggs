package de.frank.experimente.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Die 13 Farbrollen aus 02-UI-SPEC.md §2.
 *
 * Beide Erscheinungen sind gleichrangig und vollständig. Die Werte stammen aus dem
 * gemessenen Design (`WERFT-DESIGN/design-tokens.json`) und sind dort identisch mit der
 * Absicht aus v1 — nichts gerundet, nichts vereinheitlicht.
 *
 * Material 3 kennt diese Rollen nicht (Grund/Fläche/Erhöht/Rand/Rand weich/Blass/
 * Aktion gedeckt/Erledigt/Erledigt gedeckt), deshalb eine eigene Palette statt die
 * Design-Semantik in `ColorScheme` zu verbiegen.
 */
@Immutable
data class AppColors(
    val grund: Color,
    val flaeche: Color,
    val erhoeht: Color,
    val rand: Color,
    val randWeich: Color,
    val text: Color,
    val gedaempft: Color,
    val blass: Color,
    val aktion: Color,
    val aktionGedeckt: Color,
    val erledigt: Color,
    val erledigtGedeckt: Color,
    val warnung: Color,
    val istDunkel: Boolean,
)

/** Erscheinung `21dunkelstandard` (dark) — die Standard-Erscheinung. */
val DunkelFarben = AppColors(
    grund = Color(0xFF151210),
    flaeche = Color(0xFF201B17),
    erhoeht = Color(0xFF2A231D),
    rand = Color(0xFF38302A),
    randWeich = Color(0xFF2C251F),
    text = Color(0xFFF4EEE7),
    gedaempft = Color(0xFFA99C8F),
    blass = Color(0xFF6E635A),
    aktion = Color(0xFFC4623C),
    aktionGedeckt = Color(0xFF3A231A),
    erledigt = Color(0xFF6F8F6A),
    erledigtGedeckt = Color(0xFF22301F),
    warnung = Color(0xFFD8A03C),
    istDunkel = true,
)

/** Erscheinung `22hell` (light). */
val HellFarben = AppColors(
    grund = Color(0xFFF8F4EE),
    flaeche = Color(0xFFFFFFFF),
    erhoeht = Color(0xFFFFFFFF),
    rand = Color(0xFFE6DCD0),
    randWeich = Color(0xFFEFE8DF),
    text = Color(0xFF1E1915),
    gedaempft = Color(0xFF6C6157),
    blass = Color(0xFF9C9186),
    aktion = Color(0xFFB0522E),
    aktionGedeckt = Color(0xFFF6E6DD),
    erledigt = Color(0xFF5A7A55),
    erledigtGedeckt = Color(0xFFE6EFE3),
    warnung = Color(0xFF9A6A12),
    istDunkel = false,
)

val LocalAppColors = staticCompositionLocalOf { DunkelFarben }
