package de.frank.experimente.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import de.frank.experimente.R

// OFFEN: Die Schriften sollen laut Bau-Regel als Datei in `res/font` liegen, nicht
// heruntergeladen werden — heruntergeladene kommen verzögert an, und bis dahin zeigt die
// App die System-Schrift. Der Versuch, sie über die Google-Fonts-CSS zu beziehen, lieferte
// unbrauchbare Dateien (Kopf `b88a0000` statt `00010000`), die Android nicht laden kann.
// Bis gültige TTF vorliegen, bleibt der Anbieter-Weg — er stürzt wenigstens nicht ab.

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/** Überschriften — weiche Serife, warm und ruhig. */
val Fraunces = FontFamily(
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = provider, weight = FontWeight.SemiBold),
)

/** Fließtext — alles Gesprochene und alles, was die KI schreibt. */
val Inter = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
)

/** Daten und Zahlen — der Laborbuch-Anteil. */
val JetBrainsMono = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Normal),
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
