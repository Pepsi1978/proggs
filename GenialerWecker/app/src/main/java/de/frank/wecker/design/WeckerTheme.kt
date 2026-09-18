package de.frank.wecker.design

import androidx.compose.runtime.Composable
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
fun WeckerTheme(themeWahl: String, design: Design, content: @Composable () -> Unit) {
    val dunkel = themeWahl == "dark"
    CompositionLocalProvider(
        LocalDesignTokens provides tokensFuer(design),
        LocalGestalt provides gestaltFuer(design),
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
            content = content,
        )
    }
}
