package de.frank.modellkompass.ui.theme

import android.app.Activity
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Ein Erscheinungsbild in Hell und Dunkel: Orange auf Schwarz, hell in warmem Orange. */
@Immutable
data class Farben(
    val dunkel: Boolean,
    val hgOben: Color,
    val hgUnten: Color,
    val blob1: Color,
    val blob2: Color,
    val blob3: Color,
    val flaeche: Color,
    val flaecheStark: Color,
    val rand: Color,
    val schatten: Color,
    val text: Color,
    val textLeise: Color,
    val textSchwach: Color,
    val primaer: Color,
    val sekundaer: Color,
    val tertiaer: Color,
    val aufPrimaer: Color,
    val gefahr: Color,
    val erfolg: Color,
    val radius: Dp,
)

fun farbenFuer(dunkel: Boolean): Farben = if (dunkel) Farben(
    true, Color(0xFF0A0908), Color(0xFF14100D), Color(0xFFFF7A1A), Color(0xFFB8520F), Color(0xFF3A2414),
    Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0x33FFD9B8), Color(0xFF000000),
    Color(0xFFF7F1EC), Color(0xFFC9B8AA), Color(0xFF8C7B6E),
    Color(0xFFFF8A1F), Color(0xFFFFA64D), Color(0xFFE56A12), Color(0xFF1A0E05), Color(0xFFFF5C4D), Color(0xFFFF9A3D), 22.dp,
) else Farben(
    false, Color(0xFFFFF8F1), Color(0xFFFFEBD8), Color(0xFFFFC08A), Color(0xFFFFD9B5), Color(0xFFFFE9D2),
    Color(0xA8FFFFFF), Color(0xE0FFFFFF), Color(0xD9FFFFFF), Color(0xFF8A3D06),
    Color(0xFF2A1A0E), Color(0xFF6E5343), Color(0xFFA88E7E),
    Color(0xFFEE6A0C), Color(0xFFF6892E), Color(0xFFC9560A), Color(0xFFFFFFFF), Color(0xFFD63B2F), Color(0xFFE0700F), 22.dp,
)

val LocalFarben = staticCompositionLocalOf { farbenFuer(false) }
val LocalBewegung = staticCompositionLocalOf { true }

@Composable
fun ModellKompassTheme(modus: String, inhalt: @Composable () -> Unit) {
    // Automatisch folgt der Sonne in Neuenhagen: jede Minute prüfen, nur solange sichtbar.
    var sonneDunkel by remember(modus) { mutableStateOf(Sonnenstand.istDunkel()) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lebenszyklus, modus) {
        if (modus == "hell" || modus == "dunkel") return@LaunchedEffect
        lebenszyklus.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                sonneDunkel = Sonnenstand.istDunkel()
                delay(60_000L)
            }
        }
    }
    val dunkel = when (modus) { "hell" -> false; "dunkel" -> true; else -> sonneDunkel }
    val farben = remember(dunkel) { farbenFuer(dunkel) }
    val context = LocalContext.current
    // Die Systemleisten richten sich sonst nach dem System-Modus; Symbole wären auf gleichfarbigem Grund unsichtbar.
    // Eigene Fenster wie ModalBottomSheet lesen ihn über isSystemInDarkTheme, deshalb bekommen sie den Modus der App als Konfiguration.
    val systemKonfig = LocalConfiguration.current
    val konfig = remember(systemKonfig, dunkel) {
        Configuration(systemKonfig).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or (if (dunkel) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        }
    }
    SideEffect {
        (context as? Activity)?.window?.let { fenster ->
            WindowCompat.getInsetsController(fenster, fenster.decorView).run {
                isAppearanceLightStatusBars = !dunkel
                isAppearanceLightNavigationBars = !dunkel
            }
        }
    }
    val bewegung = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) > 0f
    }
    val schema = if (dunkel) darkColorScheme(primary = farben.primaer, secondary = farben.sekundaer, surface = farben.hgUnten, background = farben.hgOben)
    else lightColorScheme(primary = farben.primaer, secondary = farben.sekundaer, surface = farben.hgOben, background = farben.hgOben)
    MaterialTheme(colorScheme = schema) {
        CompositionLocalProvider(LocalFarben provides farben, LocalBewegung provides bewegung, LocalConfiguration provides konfig, content = inhalt)
    }
}
