package de.frank.wecker.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import de.frank.genialeideen.ui.theme.GenialeIdeenTheme
import de.frank.genialeideen.ui.theme.IdeenSchrift
import de.frank.genialeideen.ui.theme.IdeenSchriftBetont
import de.frank.genialeideen.ui.theme.IdeenSchriftFest

/**
 * Setzt Hell/Dunkel, das gewählte Design und dessen Gestalt gemeinsam.
 *
 * Die drei Achsen sind unabhängig: [themeWahl] entscheidet nur über hell oder dunkel, [design] nur
 * über die Farbwelt und die Formen, die Ausrichtung gar nicht. Für [Design.SCHLICHT] liefert
 * [paletteFuer] die bestehenden Paletten unverändert zurück — dieselbe Optik wie bisher.
 */
@Composable
fun WeckerTheme(themeWahl: String, design: Design, ausrichtung: String? = null, content: @Composable () -> Unit) {
    val dunkel = themeWahl == "dark"
    val palette = paletteFuer(design, dunkel)
    CompositionLocalProvider(
        LocalDesignTokens provides tokensFuer(design),
        LocalGestalt provides gestaltFuer(design),
        // Die Materialwerte hängen an Design und Modus und werden einmal hier gesetzt, damit
        // jede Fläche und jedes Bedienelement dieselbe Tiefensprache liest.
        LocalMaterial provides materialFuer(
            design, dunkel,
            primaer = palette.primaer, gedaempft = palette.primaerGedaempft,
            text = palette.textPrimaer, rahmen = palette.rahmen,
        ),
    ) {
        GenialeIdeenTheme(
            themeWahl = themeWahl,
            paletteVorgabe = paletteFuer(design, dunkel),
            // Schlicht behält die Serifenschrift; Morgenruhe und Traumraum setzen die ruhige
            // Grotesk der Entwürfe, Orbit die feste Schrift seiner Instrumententafel.
            titelSchrift = when (design) {
                Design.SCHLICHT -> IdeenSchriftBetont
                Design.MORGENRUHE, Design.TRAUMRAUM -> IdeenSchrift
                Design.ORBIT -> IdeenSchriftFest
            },
            content = {
                // Kein graues Viereck mehr beim Antippen — nirgends in der App. Die Rückmeldung
                // geben die Bedienelemente selbst (Knopf sinkt ein, Knauf wandert, Haken erscheint).
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.LocalIndication provides KeineIndikation) {
                    if (ausrichtung == null) content() else de.frank.wecker.AusrichtungsSperre(ausrichtung, content)
                }
            },
        )
    }
}

/** Eine Rückmeldung, die nichts zeichnet — ersetzt den grauen Wellenschlag von Material. */
private object KeineIndikation : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource): androidx.compose.ui.node.DelegatableNode =
        object : Modifier.Node() {}
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 7
}
