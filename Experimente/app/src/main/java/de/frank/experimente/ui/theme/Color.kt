package de.frank.experimente.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Die 13 Farbrollen des Entwurfs, je Erscheinung.
 *
 * Quelle: `Specs/Experimente/v2/02-UI-SPEC.md` §2. Die Werte sind gemessen und verbindlich —
 * kein Wert wird gerundet oder durch eine Material-Rolle ersetzt. Material 3 hat weniger
 * Rollen als der Entwurf braucht (Rand weich, Blass, Aktion gedeckt, Erledigt gedeckt),
 * darum eine eigene Palette statt einer verbogenen `ColorScheme`.
 */
@Immutable
data class Farben(
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
    /** `--aufaktion` — die Schrift **auf** der Aktionsfläche. In beiden Erscheinungen gleich. */
    val aufAktion: Color,
    val istDunkel: Boolean,
)

/** 2.1 Dunkel (Standard) — `21dunkelstandard` */
val FarbenDunkel = Farben(
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
    aufAktion = Color(0xFFFFF6F1),
    istDunkel = true,
)

/** 2.2 Hell — `22hell` */
val FarbenHell = Farben(
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
    aufAktion = Color(0xFFFFF6F1),
    istDunkel = false,
)

val LocalFarben = staticCompositionLocalOf { FarbenDunkel }

/**
 * `color-mix(in srgb, A, B p%)` aus dem Entwurf — lineare Mischung im sRGB-Raum.
 * Der Entwurf benutzt sie für Verläufe und Lichtsäume; ohne sie fehlt die Plastizität.
 */
fun mische(a: Color, b: Color, anteilB: Float): Color = Color(
    red = a.red + (b.red - a.red) * anteilB,
    green = a.green + (b.green - a.green) * anteilB,
    blue = a.blue + (b.blue - a.blue) * anteilB,
    alpha = a.alpha + (b.alpha - a.alpha) * anteilB,
)
