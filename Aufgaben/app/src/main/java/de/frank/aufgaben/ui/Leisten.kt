package de.frank.aufgaben.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import de.frank.aufgaben.data.Einstellungen

/**
 * Oberer Rand der Bildschirme: sonst die Höhe der Statusleiste, auch wenn sie gerade kurz ausgeblendet ist. Mit
 * „Statusleiste oben ausblenden“ nur noch der Rand der Kamera-Aussparung, damit die Inhalte den frei gewordenen Platz nutzen.
 */
@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.obenLeiste: WindowInsets
    @Composable get() = if (Einstellungen.get(LocalContext.current).statusleisteAus) {
        WindowInsets.displayCutout.only(WindowInsetsSides.Top)
    } else WindowInsets.statusBarsIgnoringVisibility
