package de.frank.stacklabor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.stacklabor.R

/**
 * Inter — als Datei eingebettet, nicht ueber die Google-Fonts-Anbindung geladen.
 *
 * Grund: Eine nachgeladene Schrift kommt verzoegert an; bis dahin zeigt die App die
 * Systemschrift, und genau in diesem Zustand entstehen Screenshots, die „irgendwie anders"
 * aussehen. Die eingebettete Datei ist ausserdem dieselbe, die dem Entwurf zugrunde liegt.
 *
 * `inter_variable.ttf` ist eine variable Schrift und traegt alle benoetigten Schnitte
 * (400 / 500 / 600) in einer Datei.
 */
val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
)

/**
 * Die Schriftskala des Entwurfs. Alle Groessen sind in der Messung gefunden
 * (`stil.fontSize` ist direkt der sp-Wert) — siehe `02-UI-SPEC.md` §3.
 */
object Schrift {
    /** Bildschirmtitel „StackLabor" auf B-01 */
    val titelGross = TextStyle(fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)

    /** Kopfzeile eines Unterbildschirms */
    val kopfzeile = TextStyle(fontFamily = Inter, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)

    /** Stack-Name auf der Karte (B-01) */
    val stackName = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)

    /** Zeile 1 des Mittel-Eintrags, Fliesstext der Auswertung */
    val name = TextStyle(fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)

    /** Fliesstext auf B-07 */
    val fliesstext = TextStyle(fontFamily = Inter, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp)

    /** Grundtext, Zieltext, Listentext */
    val grund = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val grundMittel = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)

    /** Zeile 2 des Eintrags, Zeitpunkte, Metazeilen */
    val klein = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)

    /** Zaehlpunkte, Statusleiste, Ziel-Nummern */
    val winzig = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp)
    val winzigFett = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp)

    /** Abschnitts-Ueberschrift in den Einstellungen */
    val rubrik = TextStyle(
        fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 14.sp, letterSpacing = 1.3.sp,
    )

    /** Der Geraete-Code auf B-11 */
    val code = TextStyle(
        fontFamily = Inter, fontSize = 40.sp, fontWeight = FontWeight.SemiBold,
        lineHeight = 48.sp, letterSpacing = 2.sp,
    )
}

/** Material-3-Typografie, damit eingebaute Bausteine dieselbe Schrift benutzen. */
val StackTypografie = Typography(
    displayLarge = Schrift.titelGross,
    headlineMedium = Schrift.kopfzeile,
    titleLarge = Schrift.stackName,
    titleMedium = Schrift.name,
    bodyLarge = Schrift.fliesstext,
    bodyMedium = Schrift.grund,
    bodySmall = Schrift.klein,
    labelSmall = Schrift.winzig,
)
