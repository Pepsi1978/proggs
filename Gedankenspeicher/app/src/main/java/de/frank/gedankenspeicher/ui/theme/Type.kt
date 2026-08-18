package de.frank.gedankenspeicher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.gedankenspeicher.R

/**
 * **Inter, als Datei eingebettet** (`02-UI-SPEC.md` §3).
 *
 * Nicht über `googlefonts` geladen: heruntergeladene Schriften kommen verzögert an, bis
 * dahin zeichnet die App System-Sans — und in genau diesem Zustand entstehen Screenshots,
 * die "irgendwie anders" aussehen, ohne dass man den Grund sieht.
 *
 * Inter liegt als variable Schrift vor, deshalb je Gewicht eine `FontVariation`.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variabel(gewicht: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(gewicht),
    variationSettings = FontVariation.Settings(FontVariation.weight(gewicht)),
)

val Inter = FontFamily(
    variabel(400),
    variabel(500),
    variabel(600),
    variabel(700),
)

/**
 * Die Skala aus `02-UI-SPEC.md` §3. Die Namen sind die Rollen des Specs, nicht die
 * Material-Namen — so lässt sich beim Nachbauen Zeile für Zeile abgleichen.
 */
@Immutable
data class Schriftrollen(
    val bildschirmtitel: TextStyle,
    val kartenUeberschrift: TextStyle,
    val notiztext: TextStyle,
    val kiAntworttext: TextStyle,
    val zeitstempel: TextStyle,
    val knopf: TextStyle,
    val sitzungsname: TextStyle,
    val eingabefeld: TextStyle,
    val geraetecode: TextStyle,
    val einstellung: TextStyle,
    val einstellungErklaerung: TextStyle,
)

val Schrift = Schriftrollen(
    bildschirmtitel = TextStyle(
        fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight(600),
        lineHeight = 28.sp, letterSpacing = (-0.2).sp,
    ),
    kartenUeberschrift = TextStyle(
        fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight(600),
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    notiztext = TextStyle(
        fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight(400),
        lineHeight = 25.sp, letterSpacing = 0.sp,
    ),
    kiAntworttext = TextStyle(
        fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight(400),
        lineHeight = 26.sp, letterSpacing = 0.sp,
    ),
    zeitstempel = TextStyle(
        fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight(500),
        lineHeight = 16.sp, letterSpacing = 0.3.sp,
    ),
    knopf = TextStyle(
        fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight(600),
        lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    sitzungsname = TextStyle(
        fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight(500),
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    eingabefeld = TextStyle(
        fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight(400),
        lineHeight = 24.sp, letterSpacing = 0.sp,
    ),
    geraetecode = TextStyle(
        fontFamily = Inter, fontSize = 34.sp, fontWeight = FontWeight(700),
        lineHeight = 40.sp, letterSpacing = 4.sp,
    ),
    einstellung = TextStyle(
        fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight(500),
        lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    einstellungErklaerung = TextStyle(
        fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight(400),
        lineHeight = 18.sp, letterSpacing = 0.sp,
    ),
)

val LocalSchrift = staticCompositionLocalOf { Schrift }
