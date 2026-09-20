package de.frank.wecker.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Das gewählte Erscheinungsbild. Es ist unabhängig von Hell/Dunkel und von der Ausrichtung:
 * jedes Design hat beide Modi. [SCHLICHT] ist die Vorgabe und bildet die bisherige Gold-/Glasoptik
 * unverändert ab — Bestandsnutzer sehen ohne eigene Wahl genau das, was sie kennen.
 */
enum class Design(val id: String, val anzeige: String, val beschreibung: String) {
    SCHLICHT("schlicht", "Schlicht", "Das gewohnte Gold auf Glas."),
    MORGENRUHE("morgenruhe", "Morgenruhe", "Ruhiger Tagesablauf an einer Achse, Leinen und Tinte."),
    TRAUMRAUM("traumraum", "Traumraum", "Weiche Kuppel und runde Flächen, warmes Schwarz und Glut-Orange."),
    ORBIT("orbit", "Orbit", "Instrumententafel mit Ring und festen Zahlen, Eisblau und Limette.");

    companion object {
        fun von(id: String?): Design = entries.firstOrNull { it.id == id } ?: SCHLICHT
    }
}

/**
 * Form- und Schriftmerkmale eines Designs. Farben liegen weiterhin in der GoldPalette, damit jeder
 * vorhandene Baustein ohne Änderung mitzieht; hier stehen nur die Merkmale, die es dort nicht gibt.
 */
@Immutable
data class DesignTokens(
    val design: Design,
    /** Eckenradius der großen Flächen: von fast kantig (Orbit) bis stark gerundet (Traumraum). */
    val karteRadius: Dp,
    /** Eckenradius kleiner Bedienelemente wie Chips und stiller Knöpfe. */
    val chipRadius: Dp,
    /** Eckenradius der Alarmtasten. */
    val tasteRadius: Dp,
    /** Ob Flächen den plastischen Gold-Look mit Schein und Verlauf tragen. */
    val plastisch: Boolean,
)

val LocalDesignTokens = staticCompositionLocalOf { tokensFuer(Design.SCHLICHT) }

/**
 * Die zeichnenden Teile eines Designs. Der geteilte Code berechnet Zustand und Rückrufe genau
 * einmal und übergibt sie hier; die Gestalten sind reine Darsteller ohne Zugriff auf das ViewModel.
 * So bleiben alle Funktionen, Eingaben und Abläufe in jedem Design vollständig erhalten.
 */
interface WeckerGestalt {
    /** Hintergrund hinter allen Bildschirmen. */
    @Composable fun Hintergrund(modifier: Modifier)

    /** Umschließende Fläche für Karten und Abschnitte — trägt Editor und Einstellungen. */
    @Composable fun Flaeche(modifier: Modifier, erhoeht: Boolean, inhalt: @Composable () -> Unit)

    /** Das Motiv des Designs neben dem Text; Schlicht zeigt keines. */
    @Composable fun Motiv(modifier: Modifier)

    /** Ob die Weckerliste ihre Kopfkarte mit dem Restzeitring zeigt (Orbit ersetzt sie durch sein Modul). */
    val zeigtRestzeitRing: Boolean

    /** Rahmenfarbe für Trennlinien innerhalb der Flächen. */
    @Composable fun trennfarbe(): Color
}

val LocalGestalt = staticCompositionLocalOf<WeckerGestalt> { SchlichtGestalt }

fun gestaltFuer(design: Design): WeckerGestalt = when (design) {
    Design.SCHLICHT -> SchlichtGestalt
    Design.MORGENRUHE -> MorgenruheGestalt
    Design.TRAUMRAUM -> TraumraumGestalt
    Design.ORBIT -> OrbitGestalt
}

fun tokensFuer(design: Design): DesignTokens = when (design) {
    // Genau die bisherigen Werte: 20 dp Karten, 12 dp Knöpfe, plastisch, keine eigene Zahlenschrift.
    Design.SCHLICHT -> DesignTokens(design, 20.dp, 12.dp, 16.dp, plastisch = true)
    Design.MORGENRUHE -> DesignTokens(design, 22.dp, 999.dp, 22.dp, plastisch = false)
    Design.TRAUMRAUM -> DesignTokens(design, 32.dp, 999.dp, 999.dp, plastisch = false)
    Design.ORBIT -> DesignTokens(design, 6.dp, 4.dp, 5.dp, plastisch = false)
}
