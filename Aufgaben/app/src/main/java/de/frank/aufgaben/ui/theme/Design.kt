package de.frank.aufgaben.ui.theme

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
import de.frank.aufgaben.data.Prioritaet
import kotlinx.coroutines.delay

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
    val farben = remember(designId, dunkel) { farbenFuer(Design.von(designId), dunkel) }
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
