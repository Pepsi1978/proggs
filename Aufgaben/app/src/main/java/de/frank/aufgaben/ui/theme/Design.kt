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
import androidx.compose.ui.graphics.compositeOver
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
    /** Ausgegraute Fassung (erledigte Aufgaben): auch [prio] liefert dann Grau. */
    val ausgegraut: Boolean = false,
) {
    /**
     * Fassung für erledigte Aufgaben: Fläche, alle Akzente, Prioritäten und Text neutral grau, mit lesbarem Kontrast
     * (Text etwa 4,5 : 1). Gilt für jedes Design, hell und dunkel; wer sie nutzt, färbt alles darin automatisch grau.
     */
    fun erledigt(): Farben {
        val grau = if (dunkel) Color(0xFF8A8A90) else Color(0xFF8E8E94)
        val flaecheGrau = if (dunkel) Color(0xE6404046) else Color(0xFFD3D3D7)
        return copy(
            flaeche = flaecheGrau, flaecheStark = flaecheGrau,
            text = if (dunkel) Color(0xFFB4B4BA) else Color(0xFF55555B),
            textLeise = if (dunkel) Color(0xFF96969C) else Color(0xFF6E6E74),
            textSchwach = grau, primaer = grau, sekundaer = grau, tertiaer = grau, gefahr = grau, erfolg = grau,
            ausgegraut = true,
        )
    }

    /**
     * Deckende Fläche unter dem Glas (Bereiche Heute und Morgen). Hell: deckendes Weiß. Dunkel: Die Glasfarbe ist ein
     * fast durchsichtiges Weiß und wäre deckend gemacht reines Weiß, deshalb über dem oberen Hintergrund verrechnet,
     * auf dem die Bereiche liegen.
     */
    val flaecheDeckend: Color get() = if (dunkel) flaeche.compositeOver(hgOben) else flaeche.copy(alpha = 1f)

    /** Prioritäten als klare Ampel: Hoch rot, Mittel gelb, Gering grün; Später bleibt neutral. */
    fun prio(p: Prioritaet): Color = if (ausgegraut) textSchwach else when (p) {
        Prioritaet.HOCH -> if (dunkel) Color(0xFFFF5449) else Color(0xFFE02B20)
        Prioritaet.MITTEL -> if (dunkel) Color(0xFFFFD233) else Color(0xFFE8B000)
        Prioritaet.GERING -> if (dunkel) Color(0xFF4FD866) else Color(0xFF239B3C)
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
