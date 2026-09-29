package de.frank.aufgaben.ui.theme

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
import de.frank.aufgaben.data.Prioritaet

/** Die vier Erscheinungsbilder. Jedes hat Hell und Dunkel, eigene Szene und eigenen Hintergrund. */
enum class Design(val id: String, val anzeige: String, val beschreibung: String, val emoji: String) {
    ORANGE("orange", "Orange", "Orange auf Schwarz, hell in warmem Orange – Feierabend-Werkstatt mit Katze.", "🟠"),
    AURORA("aurora", "Aurora", "Ruhiges Violett – Ideen an der Tafel skizzieren.", "🟣"),
    GARTEN("garten", "Garten", "Sanftes Grün – gießen, wachsen lassen, ernten.", "🟢"),
    KOSMOS("kosmos", "Kosmos", "Tiefes Blau – Checkliste, Countdown, Start.", "🔵");

    companion object {
        fun von(id: String?): Design = if (id == "glut") ORANGE else entries.firstOrNull { it.id == id } ?: ORANGE
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
    /** Prioritäten als Abstufungen der Designfarbe statt als Regenbogen. */
    fun prio(p: Prioritaet): Color = when (p) {
        Prioritaet.HOCH -> primaer
        Prioritaet.MITTEL -> androidx.compose.ui.graphics.lerp(primaer, textLeise, 0.38f)
        Prioritaet.GERING -> androidx.compose.ui.graphics.lerp(primaer, textSchwach, 0.7f)
        Prioritaet.SPAETER -> textSchwach
    }
}

val LocalFarben = staticCompositionLocalOf { farbenFuer(Design.ORANGE, false) }
val LocalBewegung = staticCompositionLocalOf { true }

@Composable
fun AufgabenTheme(designId: String, modus: String, inhalt: @Composable () -> Unit) {
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
