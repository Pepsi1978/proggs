package de.frank.kompass.ui.theme

import android.app.Activity
import android.content.res.Configuration
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Die drei Erscheinungsbild-Modi. Voreinstellung ist `AUTOMATISCH`: hell ab Sonnenaufgang, dunkel ab Ende der bürgerlichen Dämmerung in Neuenhagen. */
enum class ThemeModus(val id: String, val label: String) {
    HELL("hell", "Hell"),
    DUNKEL("dunkel", "Dunkel"),
    AUTOMATISCH("automatisch", "Automatisch"),
    ;

    /** Reihum: hell -> dunkel -> automatisch -> hell. Das ist die Reihenfolge des Kopfleisten-Knopfs. */
    fun naechster(): ThemeModus = when (this) {
        HELL -> DUNKEL
        DUNKEL -> AUTOMATISCH
        AUTOMATISCH -> HELL
    }

    companion object {
        fun fromId(value: String): ThemeModus = entries.firstOrNull { it.id == value } ?: AUTOMATISCH
    }
}

/**
 * Farben, die Material 3 nicht kennt, die die App aber überall braucht.
 *
 * Material 3 hat kein „Rahmen", kein „Eingabefeld" und keine Bedeutungsfarben. Ohne diesen
 * Zusatz müsste jede Stelle die Farbwerte selbst kennen — und die erste vergessene Stelle
 * sieht im anderen Modus falsch aus.
 */
data class KompassFarben(
    val rahmen: Color,
    val eingabefeld: Color,
    val flaecheErhoeht: Color,
    val textGedaempft: Color,
    val goldGedaempft: Color,
    val kupfer: Color,
    val erfolg: Color,
    val warnung: Color,
    val fehler: Color,
    val info: Color,
    val istDunkel: Boolean,
)

private val DunkelZusatz = KompassFarben(
    rahmen = DunkelRahmen,
    eingabefeld = DunkelEingabefeld,
    flaecheErhoeht = DunkelFlaecheErhoeht,
    textGedaempft = DunkelTextGedaempft,
    goldGedaempft = DunkelGoldGedaempft,
    kupfer = DunkelKupfer,
    erfolg = Erfolg,
    warnung = Warnung,
    fehler = Fehler,
    info = Info,
    istDunkel = true,
)

private val HellZusatz = KompassFarben(
    rahmen = HellRahmen,
    eingabefeld = HellEingabefeld,
    flaecheErhoeht = HellFlaecheErhoeht,
    textGedaempft = HellTextGedaempft,
    goldGedaempft = HellGoldGedaempft,
    kupfer = HellKupfer,
    erfolg = Erfolg,
    warnung = Warnung,
    fehler = Fehler,
    info = Info,
    istDunkel = false,
)

val LocalKompassFarben = staticCompositionLocalOf { DunkelZusatz }

private val DunkelSchema = darkColorScheme(
    primary = DunkelGold,
    onPrimary = DunkelAufGold,
    primaryContainer = DunkelGoldGedaempft,
    onPrimaryContainer = DunkelAufGold,
    secondary = DunkelGoldGedaempft,
    onSecondary = DunkelAufGold,
    tertiary = DunkelKupfer,
    onTertiary = Color.White,
    background = DunkelHintergrund,
    onBackground = DunkelText,
    surface = DunkelFlaeche,
    onSurface = DunkelText,
    surfaceVariant = DunkelFlaecheErhoeht,
    onSurfaceVariant = DunkelTextGedaempft,
    outline = DunkelRahmen,
    outlineVariant = DunkelRahmen,
    error = Fehler,
    onError = Color(0xFF1A0000),
)

private val HellSchema = lightColorScheme(
    primary = HellGold,
    onPrimary = HellAufGold,
    primaryContainer = HellGoldGedaempft,
    onPrimaryContainer = HellAufGold,
    secondary = HellGoldGedaempft,
    onSecondary = HellAufGold,
    tertiary = HellKupfer,
    onTertiary = Color.White,
    background = HellHintergrund,
    onBackground = HellText,
    surface = HellFlaeche,
    onSurface = HellText,
    surfaceVariant = HellFlaecheErhoeht,
    onSurfaceVariant = HellTextGedaempft,
    outline = HellRahmen,
    outlineVariant = HellRahmen,
    error = Fehler,
    onError = Color.White,
)

private val KompassTypografie = Typography(
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    // Die Erklärungen sind Fließtext, der am Stück gelesen wird. Eine großzügige Zeilenhöhe
    // macht auf dem schmalen Cover-Display des Fold den Unterschied zwischen les- und mühsam.
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
)

/** Abstände und Radien an einer Stelle, statt an dreißig Stellen einzeln geraten. */
object Mass {
    val rand = 16.dp
    val randSchmal = 12.dp
    val abstand = 12.dp
    val abstandKlein = 8.dp
    val radius = 14.dp
    val radiusKlein = 10.dp

    /** Mindestgröße einer Bedienfläche laut Referenz (Baustein B). */
    val tippflaeche = 48.dp
    val knopfKopfleiste = 40.dp
}

@Composable
fun KompassTheme(
    modus: ThemeModus,
    inhalt: @Composable () -> Unit,
) {
    // Automatisch folgt der Sonne in Neuenhagen (offline, ohne Standortrechte), nicht dem System:
    // jede Minute prüfen, nur solange die App sichtbar ist.
    var sonneDunkel by remember(modus) { mutableStateOf(Sonnenstand.istDunkel()) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lebenszyklus, modus) {
        if (modus != ThemeModus.AUTOMATISCH) return@LaunchedEffect
        lebenszyklus.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                sonneDunkel = Sonnenstand.istDunkel()
                delay(60_000L)
            }
        }
    }
    val istDunkel = when (modus) {
        ThemeModus.HELL -> false
        ThemeModus.DUNKEL -> true
        ThemeModus.AUTOMATISCH -> sonneDunkel
    }
    val schema = if (istDunkel) DunkelSchema else HellSchema
    val zusatz = if (istDunkel) DunkelZusatz else HellZusatz

    val view = LocalView.current
    if (!view.isInEditMode) {
        val fensterFarbe = schema.background.toArgb()
        SideEffect {
            val fenster = (view.context as? Activity)?.window ?: return@SideEffect
            // Status- und Navigationsleiste ziehen mit (Referenz, Baustein A). Ohne das steht
            // im Hellmodus weiße Schrift auf hellem Grund.
            @Suppress("DEPRECATION")
            fenster.statusBarColor = fensterFarbe
            @Suppress("DEPRECATION")
            fenster.navigationBarColor = fensterFarbe
            WindowCompat.getInsetsController(fenster, view).apply {
                isAppearanceLightStatusBars = !istDunkel
                isAppearanceLightNavigationBars = !istDunkel
            }
        }
    }

    // Eigene Fenster wie ModalBottomSheet lesen den Modus über isSystemInDarkTheme, deshalb
    // bekommen sie den Modus der App als Konfiguration.
    val systemKonfig = LocalConfiguration.current
    val konfig = remember(systemKonfig, istDunkel) {
        Configuration(systemKonfig).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (istDunkel) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        }
    }

    CompositionLocalProvider(LocalKompassFarben provides zusatz, LocalConfiguration provides konfig) {
        MaterialTheme(
            colorScheme = schema,
            typography = KompassTypografie,
            content = inhalt,
        )
    }
}
