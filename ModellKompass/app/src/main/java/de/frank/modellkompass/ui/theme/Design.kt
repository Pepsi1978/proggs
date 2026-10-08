package de.frank.modellkompass.ui.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
fun ModellKompassTheme(dunkel: Boolean, inhalt: @Composable () -> Unit) {
    val farben = remember(dunkel) { farbenFuer(dunkel) }
    val context = LocalContext.current
    val bewegung = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) > 0f
    }
    val schema = if (dunkel) darkColorScheme(primary = farben.primaer, secondary = farben.sekundaer, surface = farben.hgUnten, background = farben.hgOben)
    else lightColorScheme(primary = farben.primaer, secondary = farben.sekundaer, surface = farben.hgOben, background = farben.hgOben)
    MaterialTheme(colorScheme = schema) {
        CompositionLocalProvider(LocalFarben provides farben, LocalBewegung provides bewegung, content = inhalt)
    }
}
