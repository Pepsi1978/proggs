package de.frank.ziele.ui.theme

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

/** Farben im selben Aufbau wie in Geniale Aufgaben und Jarvis, damit die Glas-Bausteine unverändert passen. */
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
) {
    /** Zweite Gruppe (unterstützende Aufgaben): ein ruhigeres, kühleres Gold, damit beide Gruppen auf einen Blick getrennt sind. */
    val leise: Color get() = androidx.compose.ui.graphics.lerp(primaer, textLeise, 0.45f)
}

/**
 * Gold in beiden Modi: Dunkel wie Schmuck auf Anthrazit (warmes Gold, leuchtende Kanten),
 * Hell wie Blattgold auf Elfenbein (tiefes Bronze-Gold, damit Text und Knöpfe genug Kontrast haben).
 */
fun farbenFuer(dunkel: Boolean): Farben = if (dunkel) Farben(
    true, Color(0xFF0E0C09), Color(0xFF17130D), Color(0xFF7A5A16), Color(0xFFB8860B), Color(0xFF2E2314),
    Color(0x14FFFFFF), Color(0x24FFFFFF), Color(0x40E6C25A), Color(0xFF000000),
    Color(0xFFF3ECDD), Color(0xFFB3A68C), Color(0xFF7D735F),
    Color(0xFFE6C25A), Color(0xFFF4D98A), Color(0xFFC9992E), Color(0xFF1A1206), Color(0xFFFF6B5E), Color(0xFF6FCF97), 22.dp,
) else Farben(
    false, Color(0xFFFCF8EF), Color(0xFFF5EDDB), Color(0xFFF0DCA8), Color(0xFFF7E8C4), Color(0xFFFBF2DF),
    Color(0xB3FFFFFF), Color(0xE8FFFFFF), Color(0xFFE8DCC0), Color(0xFF6B5420),
    Color(0xFF1E1810), Color(0xFF6B5F4B), Color(0xFF9E917A),
    Color(0xFF9A7414), Color(0xFFC29A2E), Color(0xFF7A5A0E), Color(0xFFFFFFFF), Color(0xFFD13B30), Color(0xFF2E8B57), 22.dp,
)

val LocalFarben = staticCompositionLocalOf { farbenFuer(true) }
val LocalBewegung = staticCompositionLocalOf { true }

@Composable
fun ZieleTheme(modus: String, inhalt: @Composable () -> Unit) {
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
