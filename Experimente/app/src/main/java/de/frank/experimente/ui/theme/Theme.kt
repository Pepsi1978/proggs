package de.frank.experimente.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Die drei Möglichkeiten aus F-26. Gespeichert wird der **Modus**, nicht die gerade
 * aufgelöste Farbe — nur so folgt `SYSTEM` (Automatik) auch dem Lauf der Sonne ohne Neustart (A-19).
 * `SYSTEM` bleibt der gespeicherte Name; gemeint ist „Automatisch nach der Sonne“ in Neuenhagen.
 */
enum class Erscheinung(val schluessel: String) {
    HELL("light"),
    DUNKEL("dark"),
    SYSTEM("system");

    /** Die feste Reihenfolge des Schnellschalters auf B-01: Hell → Dunkel → Automatik → Hell. */
    fun naechste(): Erscheinung = when (this) {
        HELL -> DUNKEL
        DUNKEL -> SYSTEM
        SYSTEM -> HELL
    }

    companion object {
        fun aus(schluessel: String?): Erscheinung =
            entries.firstOrNull { it.schluessel == schluessel } ?: SYSTEM
    }
}

@Composable
fun ExperimenteTheme(
    erscheinung: Erscheinung,
    effektstufe: Effektstufe = Effektstufe.VOLL,
    inhalt: @Composable () -> Unit,
) {
    // Automatik folgt der Sonne in Neuenhagen, nicht dem System: jede Minute prüfen, nur solange die App sichtbar ist.
    var sonneDunkel by remember(erscheinung) { mutableStateOf(Sonnenstand.istDunkel()) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lebenszyklus, erscheinung) {
        if (erscheinung != Erscheinung.SYSTEM) return@LaunchedEffect
        lebenszyklus.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                sonneDunkel = Sonnenstand.istDunkel()
                delay(60_000L)
            }
        }
    }
    val dunkel = when (erscheinung) {
        Erscheinung.HELL -> false
        Erscheinung.DUNKEL -> true
        Erscheinung.SYSTEM -> sonneDunkel
    }
    val context = LocalContext.current
    // Die Systemleisten richten sich sonst nach dem System-Modus; eigene Fenster (ModalBottomSheet) lesen ihn über
    // isSystemInDarkTheme, deshalb bekommen sie den Modus der App als Konfiguration.
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
    val farben = if (dunkel) FarbenDunkel else FarbenHell

    // Material 3 wird nur mitversorgt, damit eingebaute Bauteile (Textfeld-Cursor,
    // Auswahlgriffe) nicht aus dem Rahmen fallen. Gezeichnet wird aus `LocalFarben`.
    val schema = remember(dunkel, farben) { if (dunkel) {
        darkColorScheme(
            primary = farben.aktion,
            onPrimary = farben.text,
            background = farben.grund,
            onBackground = farben.text,
            surface = farben.flaeche,
            onSurface = farben.text,
            surfaceVariant = farben.erhoeht,
            onSurfaceVariant = farben.gedaempft,
            outline = farben.rand,
            error = farben.warnung,
        )
    } else {
        lightColorScheme(
            primary = farben.aktion,
            onPrimary = FarbenHell.flaeche,
            background = farben.grund,
            onBackground = farben.text,
            surface = farben.flaeche,
            onSurface = farben.text,
            surfaceVariant = farben.erhoeht,
            onSurfaceVariant = farben.gedaempft,
            outline = farben.rand,
            error = farben.warnung,
        )
    }

    }
    val schriften = remember { Schriften() }
    CompositionLocalProvider(
        LocalFarben provides farben,
        LocalConfiguration provides konfig,
        LocalSchriften provides schriften,
        // A-30: „Bewegung reduzieren" und der Energiesparmodus erzwingen mindestens *Gedämpft*.
        LocalEffektstufe provides geltendeStufe(effektstufe),
    ) {
        MaterialTheme(colorScheme = schema, content = inhalt)
    }
}
