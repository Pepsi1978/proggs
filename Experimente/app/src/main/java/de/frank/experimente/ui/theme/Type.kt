package de.frank.experimente.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.experimente.R

/**
 * Die drei Schriften des Entwurfs, als Datei eingebettet.
 *
 * Nicht über `googlefonts` geladen: heruntergeladene Schriften kommen verzögert an, bis
 * dahin zeichnet die App System-Sans — und genau in diesem Zustand entstehen Screenshots,
 * die "irgendwie anders" aussehen, ohne dass man den Grund sieht. Eingebettet sind es
 * dieselben Umrisse, die der Browser im Design zeichnet.
 *
 * Alle drei liegen als variable Schrift vor, deshalb je Gewicht eine `FontVariation`.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variabel(res: Int, gewicht: Int) = Font(
    resId = res,
    weight = FontWeight(gewicht),
    variationSettings = FontVariation.Settings(FontVariation.weight(gewicht)),
)

val Fraunces = FontFamily(
    variabel(R.font.fraunces_variable, 400),
    variabel(R.font.fraunces_variable, 500),
    variabel(R.font.fraunces_variable, 600),
    variabel(R.font.fraunces_variable, 700),
)

val Inter = FontFamily(
    variabel(R.font.inter_variable, 300),
    variabel(R.font.inter_variable, 400),
    variabel(R.font.inter_variable, 500),
    variabel(R.font.inter_variable, 600),
    variabel(R.font.inter_variable, 700),
)

val JetBrainsMono = FontFamily(
    variabel(R.font.jetbrains_mono_variable, 400),
    variabel(R.font.jetbrains_mono_variable, 500),
)

/**
 * Die Typo-Skala aus `02-UI-SPEC.md` §3 — Familie, Größe, Gewicht, Zeilenhöhe und
 * Laufweite exakt wie gemessen. px des Entwurfs sind bei `density: 1` unmittelbar sp.
 */
@Immutable
data class Schriften(
    val bildschirmtitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontSize = 28.sp, fontWeight = FontWeight(600),
        lineHeight = 34.sp, letterSpacing = 0.sp,
    ),
    val abschnittstitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontSize = 22.sp, fontWeight = FontWeight(600),
        lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    val kartentitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontSize = 19.sp, fontWeight = FontWeight(600),
        lineHeight = 25.sp, letterSpacing = 0.sp,
    ),
    val fliesstext: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight(400),
        lineHeight = 25.sp, letterSpacing = 0.sp,
    ),
    val fliesstextKlein: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight(400),
        lineHeight = 21.sp, letterSpacing = 0.sp,
    ),
    val knopf: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight(500),
        lineHeight = 20.sp, letterSpacing = 0.2.sp,
    ),
    val zwischenueberschrift: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight(600),
        lineHeight = 17.sp, letterSpacing = 0.6.sp,
    ),
    val daten: TextStyle = TextStyle(
        fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight(400),
        lineHeight = 18.sp, letterSpacing = 0.sp,
    ),
    val stufe: TextStyle = TextStyle(
        fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight(400),
        lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),

    // --- Weitere Größen, die der Fold-Entwurf setzt --------------------------------------

    /** `font-size:26px; line-height:32px` — Titel von B-08 und B-09. */
    val bildschirmtitelKlein: TextStyle = TextStyle(
        fontFamily = Fraunces, fontSize = 26.sp, fontWeight = FontWeight(600),
        lineHeight = 32.sp, letterSpacing = 0.sp,
    ),

    /** `font-size:20px; line-height:26px` — Titel von B-02 (Experimentname). */
    val gespraechstitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontSize = 20.sp, fontWeight = FontWeight(600),
        lineHeight = 26.sp, letterSpacing = 0.sp,
    ),

    /** `font-size:17px; line-height:23px` — Gruppentitel in B-08. */
    val gruppentitel: TextStyle = TextStyle(
        fontFamily = Fraunces, fontSize = 17.sp, fontWeight = FontWeight(600),
        lineHeight = 23.sp, letterSpacing = 0.sp,
    ),

    /** `font:400 15px/23px Inter` — die Beschreibung auf einer Karte. */
    val kartentext: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight(400),
        lineHeight = 23.sp, letterSpacing = 0.sp,
    ),

    /** `font:400 15px/22px Inter` — eine Zeile der To-Do-Liste. */
    val aufgabenzeile: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight(400),
        lineHeight = 22.sp, letterSpacing = 0.sp,
    ),

    /** `font:400 13px/18px Inter` — die Beschriftung über einem Auswahlfeld. */
    val feldbeschriftung: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight(400),
        lineHeight = 18.sp, letterSpacing = 0.sp,
    ),

    /** `font:400 13px/19px Inter` — der erklärende Satz unter einem Abschnitt. */
    val erklaerung: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight(400),
        lineHeight = 19.sp, letterSpacing = 0.sp,
    ),

    /** `font:500 15px/20px Inter` — die Beschriftung der Knöpfe auf den Karten. */
    val knopfKlein: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight(500),
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),

    /** `font:500 14px/20px Inter` — die Reiter von B-07. */
    val reiter: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight(500),
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),

    /** `font:500 13px/18px Inter` — der Morgen/Abend-Umschalter auf B-01. */
    val umschalter: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight(500),
        lineHeight = 18.sp, letterSpacing = 0.sp,
    ),

    /** `font:400 11px/14px Inter` — die Beschriftung der sechs Leistenfelder. */
    val leiste: TextStyle = TextStyle(
        fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight(400),
        lineHeight = 14.sp, letterSpacing = 0.sp,
    ),

    /** `font:500 15px/20px 'JetBrains Mono'` — die Tempo-Anzeige in B-08. */
    val zahl: TextStyle = TextStyle(
        fontFamily = JetBrainsMono, fontSize = 15.sp, fontWeight = FontWeight(500),
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),

    /** `font:500 10.5px/13px 'JetBrains Mono'` — der Stand im Fortschrittsring. */
    val ringstand: TextStyle = TextStyle(
        fontFamily = JetBrainsMono, fontSize = 10.5.sp, fontWeight = FontWeight(500),
        lineHeight = 13.sp, letterSpacing = 0.sp,
    ),
)

val LocalSchriften = staticCompositionLocalOf { Schriften() }
