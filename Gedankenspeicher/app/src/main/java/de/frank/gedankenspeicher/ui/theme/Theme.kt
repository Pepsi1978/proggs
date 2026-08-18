package de.frank.gedankenspeicher.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

/**
 * **Die Erscheinung, die gerade gilt — mit weicher Überblendung (M-10).**
 *
 * Der Wechsel läuft über `animateColorAsState` je Rolle, nicht über einen harten Tausch:
 * `03-MOTION-SPEC.md` M-10 verlangt eine Überblendung von 400 ms, ausdrücklich ohne Wischen,
 * ohne Aufziehen, ohne Kreis, der sich ausbreitet.
 */
@Composable
fun GedankenspeicherTheme(
    erscheinung: Erscheinung,
    inhalt: @Composable () -> Unit,
) {
    val ziel = erscheinung.farben
    val spec = weichSpec<Color>()

    val farben = Farbrollen(
        hintergrund = animateColorAsState(ziel.hintergrund, spec, label = "hintergrund").value,
        hintergrundErhoben = animateColorAsState(ziel.hintergrundErhoben, spec, label = "erhoben").value,
        hintergrundGlas = animateColorAsState(ziel.hintergrundGlas, spec, label = "glas").value,
        rand = animateColorAsState(ziel.rand, spec, label = "rand").value,
        akzent = animateColorAsState(ziel.akzent, spec, label = "akzent").value,
        akzentGedeckt = animateColorAsState(ziel.akzentGedeckt, spec, label = "akzentGedeckt").value,
        textStark = animateColorAsState(ziel.textStark, spec, label = "textStark").value,
        textMittel = animateColorAsState(ziel.textMittel, spec, label = "textMittel").value,
        textSchwach = animateColorAsState(ziel.textSchwach, spec, label = "textSchwach").value,
        fehler = animateColorAsState(ziel.fehler, spec, label = "fehler").value,
        erfolg = animateColorAsState(ziel.erfolg, spec, label = "erfolg").value,
        kiKarte = animateColorAsState(ziel.kiKarte, spec, label = "kiKarte").value,
        kiKarteRand = animateColorAsState(ziel.kiKarteRand, spec, label = "kiKarteRand").value,
        // Nicht mit-animiert: das ist eine Ja/Nein-Angabe für die Statusleisten-Symbole.
        istDunkel = ziel.istDunkel,
    )

    val ansicht = LocalView.current
    if (!ansicht.isInEditMode) {
        SideEffect {
            val fenster = (ansicht.context as android.app.Activity).window
            WindowCompat.getInsetsController(fenster, ansicht).apply {
                isAppearanceLightStatusBars = !ziel.istDunkel
                isAppearanceLightNavigationBars = !ziel.istDunkel
            }
        }
    }

    // Material bekommt dieselben Werte, damit eingebaute Bauteile (Textfeld-Cursor,
    // Auswahlgriffe, Wellenfarbe) nicht aus der Erscheinung fallen.
    val schema = if (ziel.istDunkel) {
        darkColorScheme(
            primary = farben.akzent,
            onPrimary = farben.hintergrund,
            background = farben.hintergrund,
            onBackground = farben.textStark,
            surface = farben.hintergrundErhoben,
            onSurface = farben.textStark,
            surfaceVariant = farben.hintergrundErhoben,
            onSurfaceVariant = farben.textMittel,
            outline = farben.rand,
            error = farben.fehler,
        )
    } else {
        lightColorScheme(
            primary = farben.akzent,
            onPrimary = Color.White,
            background = farben.hintergrund,
            onBackground = farben.textStark,
            surface = farben.hintergrundErhoben,
            onSurface = farben.textStark,
            surfaceVariant = farben.hintergrundErhoben,
            onSurfaceVariant = farben.textMittel,
            outline = farben.rand,
            error = farben.fehler,
        )
    }

    CompositionLocalProvider(
        LocalFarben provides farben,
        LocalSchrift provides Schrift,
    ) {
        MaterialTheme(colorScheme = schema, content = inhalt)
    }
}

/**
 * Der Grundton eines Blattes: der Glaston, aber **deckend**.
 *
 * Compose kann die Fläche hinter einem Bottom Sheet nicht wirklich unscharf zeichnen — das
 * halbdurchsichtige Glas liess deshalb die Knöpfe der Fussleiste ungeblurrt durch das Blatt
 * scheinen, mitten in den Text hinein. Hier wird derselbe Glaston über den Grund gelegt und
 * das Ergebnis deckend gezeichnet: die Farbe bleibt die gemessene, das Durchscheinen ist weg.
 */
val Farbrollen.blattgrund: Color
    get() = hintergrundGlas.compositeOver(hintergrund)

/** Kurzzugriff, damit in den Bildschirmen `Farben.akzent` statt `LocalFarben.current.akzent` steht. */
val Farben: Farbrollen
    @Composable get() = LocalFarben.current

val Schriften: Schriftrollen
    @Composable get() = LocalSchrift.current

/** Die Maße aus `02-UI-SPEC.md` §4, an einer Stelle statt verstreut in den Bildschirmen. */
object Masse {
    val seitenrand = 16.dp
    val kartenAbstand = 12.dp
    val karteInnen = 16.dp
    val kopfleiste = 56.dp
    val fussleiste = 72.dp
    val aufnahmeknopf = 60.dp
    val aura = 80.dp
    val kiKnopf = 48.dp
    val kartenSymbolFlaeche = 36.dp
    val kartenSymbol = 20.dp
    val schubladeSchmal = 280.dp
    val schubladeBreit = 320.dp
    val sitzungszeile = 56.dp
    val tippflaeche = 44.dp
    val karteRadius = 20.dp
    val eingabeRadius = 22.dp
    val gruppeRadius = 16.dp
    val profilRadius = 14.dp
    val blattRadius = 28.dp
    val schubladeRadius = 24.dp
}
