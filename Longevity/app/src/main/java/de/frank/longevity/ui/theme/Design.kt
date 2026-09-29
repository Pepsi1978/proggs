package de.frank.longevity.ui.theme

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

/** Die vier Erscheinungsbilder. Jedes hat Hell und Dunkel, eigene Szene und eigenen Hintergrund. */
enum class Design(val id: String, val anzeige: String, val beschreibung: String, val emoji: String) {
    MORGENROT("morgenrot", "Morgenröte", "Die Sonne geht unter, du schläfst, die Sonne geht auf – der Rhythmus eines langen Lebens.", "🌅"),
    LEBENSBAUM("lebensbaum", "Lebensbaum", "Ein Baum wächst Ring um Ring – jedes gesunde Jahr zählt.", "🌳"),
    ATEM("atem", "Atem", "Ruhige Wellen, ein Atemkreis und ein gelassener Herzschlag.", "🌊"),
    HELIX("helix", "Helix", "Leuchtende DNA-Doppelhelix im Labor der Zukunft.", "🧬");

    companion object {
        fun von(id: String?): Design = entries.firstOrNull { it.id == id } ?: MORGENROT
    }
}

@Immutable
data class Farben(
    val design: Design,
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
}

fun farbenFuer(design: Design, dunkel: Boolean): Farben = when (design) {
    Design.ATEM -> if (dunkel) Farben(
        design, true, Color(0xFF031216), Color(0xFF06283A), Color(0xFF1C8FB5), Color(0xFF2BD1B0), Color(0xFF5B7CFF),
        Color(0x1CFFFFFF), Color(0x30FFFFFF), Color(0x40BFF6FF), Color(0xFF000000),
        Color(0xFFE8FBFF), Color(0xFFA8D5DF), Color(0xFF6E9AA6),
        Color(0xFF4FD6E8), Color(0xFF7CF2C8), Color(0xFF8FA8FF), Color(0xFF02161C), Color(0xFFFF6B7D), Color(0xFF4FE3A5), 28.dp,
    ) else Farben(
        design, false, Color(0xFFEAF8FB), Color(0xFFE9F1FF), Color(0xFF9FE3F0), Color(0xFFA8F0DA), Color(0xFFC3CEFF),
        Color(0xA6FFFFFF), Color(0xDEFFFFFF), Color(0xD9FFFFFF), Color(0xFF0E5A6E),
        Color(0xFF0B2A33), Color(0xFF45666F), Color(0xFF84A1A9),
        Color(0xFF0B8EAD), Color(0xFF14A889), Color(0xFF4F63E0), Color(0xFFFFFFFF), Color(0xFFE5364D), Color(0xFF14A06A), 28.dp,
    )
    Design.LEBENSBAUM -> if (dunkel) Farben(
        design, true, Color(0xFF07140F), Color(0xFF0F2A1E), Color(0xFF2E9D6A), Color(0xFFE8B04B), Color(0xFF3FA7D6),
        Color(0x1CFFFFFF), Color(0x30FFFFFF), Color(0x3DFFFFFF), Color(0xFF000000),
        Color(0xFFECF8F0), Color(0xFFB3D4C0), Color(0xFF7FA38C),
        Color(0xFF5FD39A), Color(0xFFF4B860), Color(0xFF6CC3F0), Color(0xFF06170F), Color(0xFFFF7A7A), Color(0xFF5FD39A), 30.dp,
    ) else Farben(
        design, false, Color(0xFFEEF8F0), Color(0xFFFFF6E3), Color(0xFFA8E6C4), Color(0xFFFFD99A), Color(0xFFB9E3F7),
        Color(0xA6FFFFFF), Color(0xDEFFFFFF), Color(0xD9FFFFFF), Color(0xFF1F5E40),
        Color(0xFF15301F), Color(0xFF4F6E5A), Color(0xFF8AA595),
        Color(0xFF238B5B), Color(0xFFE8912D), Color(0xFF2B8FC4), Color(0xFFFFFFFF), Color(0xFFE5484D), Color(0xFF238B5B), 30.dp,
    )
    Design.MORGENROT -> if (dunkel) Farben(
        design, true, Color(0xFF160B10), Color(0xFF2B1224), Color(0xFFFF6A3D), Color(0xFF9B3FD1), Color(0xFFFFB347),
        Color(0x1EFFFFFF), Color(0x32FFFFFF), Color(0x3DFFE2D0), Color(0xFF000000),
        Color(0xFFFFF1EA), Color(0xFFE3BFB4), Color(0xFFAC8A84),
        Color(0xFFFF8A5C), Color(0xFFC77DFF), Color(0xFFFFC857), Color(0xFF200A0A), Color(0xFFFF6B6B), Color(0xFF7BD389), 22.dp,
    ) else Farben(
        design, false, Color(0xFFFFF3EA), Color(0xFFFFE0E6), Color(0xFFFFB38F), Color(0xFFE3B5FF), Color(0xFFFFE08A),
        Color(0xA3FFFFFF), Color(0xDBFFFFFF), Color(0xD9FFFFFF), Color(0xFF8C3A1F),
        Color(0xFF3A1A12), Color(0xFF7A5046), Color(0xFFAF8A80),
        Color(0xFFE8552B), Color(0xFF8E3FC7), Color(0xFFE0A100), Color(0xFFFFFFFF), Color(0xFFD7263D), Color(0xFF2E9E5B), 22.dp,
    )
    Design.HELIX -> if (dunkel) Farben(
        design, true, Color(0xFF03060F), Color(0xFF0A1330), Color(0xFF2F6BFF), Color(0xFF00C2D1), Color(0xFF8E5CFF),
        Color(0x1AFFFFFF), Color(0x2EFFFFFF), Color(0x40A9D4FF), Color(0xFF000000),
        Color(0xFFEAF3FF), Color(0xFFA9BCDD), Color(0xFF6F82A6),
        Color(0xFF62A8FF), Color(0xFFC6FF4A), Color(0xFF3FE0EE), Color(0xFF020814), Color(0xFFFF6B7D), Color(0xFFC6FF4A), 14.dp,
    ) else Farben(
        design, false, Color(0xFFEAF2FF), Color(0xFFF1ECFF), Color(0xFFA9C8FF), Color(0xFF9EF0F5), Color(0xFFD2BFFF),
        Color(0xA8FFFFFF), Color(0xE0FFFFFF), Color(0xD9FFFFFF), Color(0xFF1B3A8C),
        Color(0xFF0E1A3A), Color(0xFF4A5A7E), Color(0xFF8A97B5),
        Color(0xFF2F6BFF), Color(0xFF00A3B4), Color(0xFF7A4DFF), Color(0xFFFFFFFF), Color(0xFFE5364D), Color(0xFF1FA35C), 14.dp,
    )
}

val LocalFarben = staticCompositionLocalOf { farbenFuer(Design.MORGENROT, false) }
val LocalBewegung = staticCompositionLocalOf { true }

@Composable
fun LongevityTheme(designId: String, modus: String, inhalt: @Composable () -> Unit) {
    val system = isSystemInDarkTheme()
    val dunkel = when (modus) { "hell" -> false; "dunkel" -> true; else -> system }
    val farben = remember(designId, dunkel) { farbenFuer(Design.von(designId), dunkel) }
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
