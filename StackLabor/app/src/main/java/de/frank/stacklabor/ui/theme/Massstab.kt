package de.frank.stacklabor.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * **Der Maßstab des Entwurfs.**
 *
 * Der Entwurf ist für eine Bildschirmbreite von **297 dp** gebaut (zugeklappt) bzw. **440 dp**
 * (aufgeklappt) — so steht es in `02-UI-SPEC.md`, und alle gemessenen Werte beziehen sich
 * darauf: Karte 273 dp breit, Eintrag 56 dp hoch, Name 15 sp.
 *
 * Das Gerät meldet aber etwas anderes: Das Cover-Display hat 1248 px bei Dichte 2,625,
 * also **475 dp** Breite — mehr als anderthalbmal so viel. Würden die Werte des Entwurfs
 * unverändert als dp verwendet, entstünde dasselbe Layout in falscher Größe: flache Karten,
 * zu kleine Schrift, zu weite Abstände. Es sähe aus wie eine andere App.
 *
 * Deshalb rechnet StackLabor in der Dichte des Entwurfs: Die Dichte wird so gesetzt, dass
 * die Bildschirmbreite **genau** der Entwurfsbreite entspricht. Damit kommt jeder gemessene
 * Wert eins zu eins auf dem Gerät an — 273 dp Karte sind dann wirklich die Karte aus dem Bild.
 *
 * Die Schriftskalierung des Systems bleibt dabei unangetastet (Frank hat sie auf 90 %
 * gestellt); nur der Längenmaßstab wird angeglichen.
 */
object Massstab {
    /** Breite, für die der Entwurf zugeklappt gebaut ist. */
    const val BREITE_ZUGEKLAPPT = 297f

    /** Breite, für die das zweispaltige Layout gebaut ist. */
    const val BREITE_AUFGEKLAPPT = 440f

    /** Ab dieser gemeldeten Breite gilt das Gerät als aufgeklappt. */
    const val SCHWELLE_AUFGEKLAPPT = 600
}

/**
 * Legt den Maßstab des Entwurfs über den Inhalt.
 *
 * Alles darin rechnet in denselben dp wie die Messung in `Specs/StackLabor/v2/messung/`.
 */
@Composable
fun ImMassstabDesEntwurfs(inhalt: @Composable () -> Unit) {
    val konfiguration = LocalConfiguration.current
    val dichte = LocalDensity.current

    val zielBreite =
        if (konfiguration.screenWidthDp >= Massstab.SCHWELLE_AUFGEKLAPPT) Massstab.BREITE_AUFGEKLAPPT
        else Massstab.BREITE_ZUGEKLAPPT

    // Die tatsächliche Pixelbreite des Fensters, aus der gemeldeten dp-Breite zurückgerechnet.
    val breiteInPixeln = konfiguration.screenWidthDp * dichte.density
    val neueDichte = breiteInPixeln / zielBreite

    CompositionLocalProvider(
        LocalDensity provides Density(density = neueDichte, fontScale = dichte.fontScale),
        content = inhalt,
    )
}
