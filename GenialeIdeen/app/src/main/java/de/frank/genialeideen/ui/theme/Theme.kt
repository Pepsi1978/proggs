package de.frank.genialeideen.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.frank.genialeideen.R
import kotlinx.coroutines.delay

val LocalGold = staticCompositionLocalOf { HelleGoldPalette }

/** Eine eigene Schriftfamilie statt der Systemschrift (Baustein N.6). */
val IdeenSchrift = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
)

val IdeenSchriftBetont = FontFamily(
    Font(R.font.newsreader, FontWeight.SemiBold),
)

/**
 * Die Schrift für die Ideen-Überschriften: Inter, nicht die Serifenschrift. Serifen werden
 * künstlich fett schnell matschig, die Grotesk bleibt auch bei Black klar lesbar. Sie ist als
 * Normal registriert — nur dann zeichnet Android bei hohen Gewichten überhaupt fett.
 */
val IdeenSchriftDick = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
)

/** Festbreitenschrift für den Anmeldecode (Baustein O.1). */
val IdeenSchriftFest = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Medium),
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
        titleMedium = standard.titleMedium.skaliert(skalierung, IdeenSchriftBetont),
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
 * @param themeWahl `light`, `dark` oder `auto`. Bei `auto` folgt die App der Sonne in
 *   Neuenhagen ([Sonnenstand]), nicht dem Android-System.
 *
 * Der Wechsel läuft weich: Jede Farbe wandert in den neuen Wert, statt hart umzuspringen
 * (Baustein N.5).
 */
@Composable
fun GenialeIdeenTheme(
    themeWahl: String,
    schriftSkalierung: Float = 1f,
    content: @Composable () -> Unit,
) {
    // Automatisch folgt der Sonne: jede Minute prüfen, nur solange sichtbar.
    var sonneDunkel by remember(themeWahl) { mutableStateOf(Sonnenstand.istDunkel()) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lebenszyklus, themeWahl) {
        if (themeWahl == "light" || themeWahl == "dark") return@LaunchedEffect
        lebenszyklus.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                sonneDunkel = Sonnenstand.istDunkel()
                delay(60_000L)
            }
        }
    }
    val dunkel = when (themeWahl) {
        "dark" -> true
        "auto" -> sonneDunkel
        else -> false
    }
    val ziel = if (dunkel) DunkleGoldPalette else HelleGoldPalette
    // Eigene Fenster wie ModalBottomSheet lesen den Modus über isSystemInDarkTheme, deshalb bekommen sie den Modus der App als Konfiguration.
    val systemKonfig = LocalConfiguration.current
    val konfig = remember(systemKonfig, dunkel) {
        Configuration(systemKonfig).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or (if (dunkel) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        }
    }
    val context = LocalContext.current
    // Die Systemleisten richten sich sonst nach dem System-Modus; Symbole wären auf gleichfarbigem Grund unsichtbar.
    SideEffect {
        (context as? Activity)?.window?.let { fenster ->
            WindowCompat.getInsetsController(fenster, fenster.decorView).run {
                isAppearanceLightStatusBars = !dunkel
                isAppearanceLightNavigationBars = !dunkel
            }
        }
    }
    val reduziert = Motion.bewegungReduziert(context)
    val dauer = if (reduziert) 0 else Motion.THEME_WECHSEL_MS

    @Composable
    fun weich(farbe: Color, name: String) =
        animateColorAsState(farbe, tween(dauer), label = name).value

    val palette = GoldPalette(
        hintergrund = weich(ziel.hintergrund, "hintergrund"),
        flaeche = weich(ziel.flaeche, "flaeche"),
        flaecheErhoeht = weich(ziel.flaecheErhoeht, "flaecheErhoeht"),
        primaer = weich(ziel.primaer, "primaer"),
        primaerGedaempft = weich(ziel.primaerGedaempft, "primaerGedaempft"),
        aufPrimaer = weich(ziel.aufPrimaer, "aufPrimaer"),
        akzentWarm = weich(ziel.akzentWarm, "akzentWarm"),
        textPrimaer = weich(ziel.textPrimaer, "textPrimaer"),
        textGedaempft = weich(ziel.textGedaempft, "textGedaempft"),
        rahmen = weich(ziel.rahmen, "rahmen"),
        eingabefeld = weich(ziel.eingabefeld, "eingabefeld"),
        istDunkel = ziel.istDunkel,
    )

    val schema = remember(dunkel, palette) { if (dunkel) {
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
    }
    val schrift = remember(schriftSkalierung) { typografie(schriftSkalierung) }
    CompositionLocalProvider(
        LocalGold provides palette,
        LocalBewegungReduziert provides reduziert,
        LocalConfiguration provides konfig,
    ) {
        MaterialTheme(
            colorScheme = schema,
            typography = schrift,
            content = content,
        )
    }
}
