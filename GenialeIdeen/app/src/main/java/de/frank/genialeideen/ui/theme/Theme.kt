package de.frank.genialeideen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.genialeideen.R

val LocalGold = staticCompositionLocalOf { DunkleGoldPalette }

/** Eine eigene Schriftfamilie statt der Systemschrift (Baustein N.2). */
val IdeenSchrift = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
)

val IdeenSchriftBetont = FontFamily(
    Font(R.font.newsreader, FontWeight.SemiBold),
)

private fun typografie(skalierung: Float) = Typography().let { standard ->
    Typography(
        displayLarge = standard.displayLarge.skaliert(skalierung, IdeenSchriftBetont),
        displayMedium = standard.displayMedium.skaliert(skalierung, IdeenSchriftBetont),
        displaySmall = standard.displaySmall.skaliert(skalierung, IdeenSchriftBetont),
        headlineLarge = standard.headlineLarge.skaliert(skalierung, IdeenSchriftBetont),
        headlineMedium = standard.headlineMedium.skaliert(skalierung, IdeenSchriftBetont),
        headlineSmall = standard.headlineSmall.skaliert(skalierung, IdeenSchriftBetont),
        titleLarge = standard.titleLarge.skaliert(skalierung, IdeenSchriftBetont),
        titleMedium = standard.titleMedium.skaliert(skalierung, IdeenSchrift),
        titleSmall = standard.titleSmall.skaliert(skalierung, IdeenSchrift),
        bodyLarge = standard.bodyLarge.skaliert(skalierung, IdeenSchrift),
        bodyMedium = standard.bodyMedium.skaliert(skalierung, IdeenSchrift),
        bodySmall = standard.bodySmall.skaliert(skalierung, IdeenSchrift),
        labelLarge = standard.labelLarge.skaliert(skalierung, IdeenSchrift),
        labelMedium = standard.labelMedium.skaliert(skalierung, IdeenSchrift),
        labelSmall = standard.labelSmall.skaliert(skalierung, IdeenSchrift),
    )
}

private fun TextStyle.skaliert(faktor: Float, familie: FontFamily): TextStyle =
    copy(fontFamily = familie, fontSize = (fontSize.value * faktor).sp)

/**
 * @param themeWahl `light`, `dark` oder `system`.
 */
@Composable
fun GenialeIdeenTheme(
    themeWahl: String,
    schriftSkalierung: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dunkel = when (themeWahl) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (dunkel) DunkleGoldPalette else HelleGoldPalette
    val context = LocalContext.current
    val schema = if (dunkel) {
        darkColorScheme(
            primary = palette.primaer,
            onPrimary = palette.aufPrimaer,
            secondary = palette.primaerGedaempft,
            onSecondary = palette.aufPrimaer,
            tertiary = palette.akzentWarm,
            background = palette.hintergrund,
            onBackground = palette.textPrimaer,
            surface = palette.flaeche,
            onSurface = palette.textPrimaer,
            surfaceVariant = palette.flaecheErhoeht,
            onSurfaceVariant = palette.textGedaempft,
            outline = palette.rahmen,
            error = Semantisch.fehler,
        )
    } else {
        lightColorScheme(
            primary = palette.primaer,
            onPrimary = palette.aufPrimaer,
            secondary = palette.primaerGedaempft,
            onSecondary = palette.aufPrimaer,
            tertiary = palette.akzentWarm,
            background = palette.hintergrund,
            onBackground = palette.textPrimaer,
            surface = palette.flaeche,
            onSurface = palette.textPrimaer,
            surfaceVariant = palette.flaecheErhoeht,
            onSurfaceVariant = palette.textGedaempft,
            outline = palette.rahmen,
            error = Semantisch.fehler,
        )
    }
    CompositionLocalProvider(
        LocalGold provides palette,
        LocalBewegungReduziert provides Motion.bewegungReduziert(context),
    ) {
        MaterialTheme(
            colorScheme = schema,
            typography = typografie(schriftSkalierung),
            content = content,
        )
    }
}
