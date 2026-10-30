package de.frank.entropyreducer.presentation.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Automatik folgt der Sonne in Neuenhagen: jede Minute prüfen, nur solange die App sichtbar ist. Bei festem Modus läuft nichts. */
@Composable
fun sonneDunkel(automatik: Boolean): Boolean {
    var dunkel by remember(automatik) { mutableStateOf(Sonnenstand.istDunkel()) }
    val lebenszyklus = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lebenszyklus, automatik) {
        if (!automatik) return@LaunchedEffect
        lebenszyklus.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                dunkel = Sonnenstand.istDunkel()
                delay(60_000L)
            }
        }
    }
    return dunkel
}

/** Konfiguration mit den Nachtbits der App, damit isSystemInDarkTheme() und eigene Fenster wie ModalBottomSheet deren Modus sehen statt den des Systems. */
@Composable
fun appModusKonfiguration(dunkel: Boolean): Configuration {
    val systemKonfig = LocalConfiguration.current
    return remember(systemKonfig, dunkel) {
        Configuration(systemKonfig).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (dunkel) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        }
    }
}
