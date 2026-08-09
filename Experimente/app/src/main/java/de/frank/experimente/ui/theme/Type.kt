@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package de.frank.experimente.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.experimente.R

// Die Schriften des Entwurfs liegen als Datei in `res/font` — eingebettet, nicht
// heruntergeladen. Heruntergeladene kommen verzögert an; bis dahin zeigt die App die
// System-Schrift, und genau in diesem Zustand sieht ein Bildschirm „irgendwie anders" aus,
// ohne dass man den Grund erkennt. Eingebettet sind es zudem dieselben Umrisse, die der
// Browser im Entwurf zeichnet.
//
// Es sind variable Schriften (eine Datei, alle Gewichte). Das Gewicht kommt deshalb über
// die Achse `wght`, nicht über verschiedene Dateien.

private fun gewicht(w: FontWeight) = FontVariation.Settings(FontVariation.weight(w.weight))

/** Überschriften — weiche Serife, warm und ruhig. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_variable, FontWeight.Normal, variationSettings = gewicht(FontWeight.Normal)),
    Font(R.font.fraunces_variable, FontWeight.SemiBold, variationSettings = gewicht(FontWeight.SemiBold)),
)

/** Fließtext — alles Gesprochene und alles, was die KI schreibt. */
val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal, variationSettings = gewicht(FontWeight.Normal)),
    Font(R.font.inter_variable, FontWeight.Medium, variationSettings = gewicht(FontWeight.Medium)),
    Font(R.font.inter_variable, FontWeight.SemiBold, variationSettings = gewicht(FontWeight.SemiBold)),
)

/** Daten und Zahlen — der Laborbuch-Anteil. */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_variable, FontWeight.Normal, variationSettings = gewicht(FontWeight.Normal)),
)

/**
 * Die Typo-Skala aus 02-UI-SPEC.md §3. Größen exakt wie gemessen, keine gerundeten Werte.
 */
@Immutable
data class AppTypography(
    val bildschirmtitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.sp,
    ),
    val abschnittstitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    val kartentitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = 0.sp,
    ),
    val fliesstext: TextStyle = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 25.sp, letterSpacing = 0.sp,
    ),
    val fliesstextKlein: TextStyle = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.sp,
    ),
    val knopf: TextStyle = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp,
    ),
    /** Wird in Großbuchstaben gesetzt — siehe `Zwischenueberschrift`-Composable. */
    val zwischenueberschrift: TextStyle = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.6.sp,
    ),
    val daten: TextStyle = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp,
    ),
    val stufe: TextStyle = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
)

val AppTypo = AppTypography()
