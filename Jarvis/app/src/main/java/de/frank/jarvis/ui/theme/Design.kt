package de.frank.jarvis.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
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

/** Farben im selben Aufbau wie in Geniale Aufgaben, damit die Glas-Bausteine unverändert passen. */
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

/** Jarvis leuchtet in einer Farbfamilie: kühles Blau bis Cyan. Rot nur für Störungen, Grün nur für „online“. */
fun farbenFuer(dunkel: Boolean): Farben = if (dunkel) Farben(
    true, Color(0xFF04070F), Color(0xFF0A1424), Color(0xFF1E6FD6), Color(0xFF12B5D6), Color(0xFF0F2250),
    Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0x38BFE6FF), Color(0xFF000000),
    Color(0xFFEAF4FC), Color(0xFFA9BED6), Color(0xFF6D819C),
    Color(0xFF4FC8FF), Color(0xFF8ADFFF), Color(0xFF2E96E6), Color(0xFF021018), Color(0xFFFF6B6B), Color(0xFF4FE0A0), 20.dp,
) else Farben(
    false, Color(0xFFF2F7FD), Color(0xFFE2EEFA), Color(0xFFBFDDF8), Color(0xFFCFEFF8), Color(0xFFE6F1FC),
    Color(0xA8FFFFFF), Color(0xE0FFFFFF), Color(0xD9FFFFFF), Color(0xFF14508C),
    Color(0xFF0F1D30), Color(0xFF475A75), Color(0xFF8595AB),
    Color(0xFF0B7FD0), Color(0xFF2E9BE6), Color(0xFF0A68B0), Color(0xFFFFFFFF), Color(0xFFD9363E), Color(0xFF14935C), 20.dp,
)

/** Die Designs zum Umschalten: Blau (Jarvis) und Gold (wie Geniale Ideen), jeweils hell und dunkel. */
val DESIGNS = listOf("blau" to "Blau", "gold" to "Gold wie Geniale Ideen")

/** Gold wie Geniale Ideen: Anthrazit mit Gold bzw. Creme mit Bronze. */
fun goldFarbenFuer(dunkel: Boolean): Farben = if (dunkel) Farben(
    true, Color(0xFF121212), Color(0xFF181818), Color(0xFF6B4F14), Color(0xFF8A5A10), Color(0xFF2C2418),
    Color(0x14FFFFFF), Color(0x24FFFFFF), Color(0x40E3B341), Color(0xFF000000),
    Color(0xFFEDE7DA), Color(0xFFA79C86), Color(0xFF7A705E),
    Color(0xFFE3B341), Color(0xFFF0CC6A), Color(0xFFC9922B), Color(0xFF1A1408), Color(0xFFFF5252), Color(0xFF4CAF7D), 20.dp,
) else Farben(
    false, Color(0xFFFAF7F0), Color(0xFFF4EFE3), Color(0xFFF1E1B8), Color(0xFFF6E7C8), Color(0xFFFBF3E2),
    Color(0xB0FFFFFF), Color(0xE6FFFFFF), Color(0xFFE6DFCF), Color(0xFF6B5420),
    Color(0xFF1B1710), Color(0xFF6B6151), Color(0xFF9A8F7C),
    Color(0xFF8B6914), Color(0xFFA9812A), Color(0xFF6F5410), Color(0xFFFFFFFF), Color(0xFFD9363E), Color(0xFF2E8B57), 20.dp,
)

val LocalFarben = staticCompositionLocalOf { farbenFuer(true) }
val LocalBewegung = staticCompositionLocalOf { true }

@Composable
fun JarvisTheme(modus: String, design: String = "blau", inhalt: @Composable () -> Unit) {
    val system = isSystemInDarkTheme()
    val dunkel = when (modus) { "hell" -> false; "dunkel" -> true; else -> system }
    val farben = remember(dunkel, design) { if (design == "gold") goldFarbenFuer(dunkel) else farbenFuer(dunkel) }
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
