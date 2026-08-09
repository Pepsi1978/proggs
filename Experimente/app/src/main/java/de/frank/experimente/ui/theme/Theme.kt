package de.frank.experimente.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Die drei Möglichkeiten aus F-26. Vorbelegung: [DUNKEL]. */
enum class Erscheinung(val id: String) {
    HELL("light"),
    DUNKEL("dark"),
    SYSTEM("system");

    /** Reihenfolge des Schnellschalters auf B-01: Hell → Dunkel → System → Hell. */
    fun naechste(): Erscheinung = when (this) {
        HELL -> DUNKEL
        DUNKEL -> SYSTEM
        SYSTEM -> HELL
    }

    companion object {
        fun ausId(wert: String?): Erscheinung =
            entries.firstOrNull { it.id == wert } ?: DUNKEL
    }
}

@Composable
fun ExperimenteTheme(
    erscheinung: Erscheinung,
    content: @Composable () -> Unit,
) {
    val dunkel = when (erscheinung) {
        Erscheinung.DUNKEL -> true
        Erscheinung.HELL -> false
        Erscheinung.SYSTEM -> isSystemInDarkTheme()
    }
    val farben = if (dunkel) DunkelFarben else HellFarben

    // Material 3 braucht ein ColorScheme für seine eigenen Bauteile (Dialoge, Ripple-Basis).
    // Die App selbst arbeitet mit LocalAppColors — dort stehen die Rollen, die M3 nicht kennt.
    val m3 = if (dunkel) {
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
            onPrimary = farben.flaeche,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val fenster = (view.context as Activity).window
            fenster.statusBarColor = farben.grund.value.toInt()
            fenster.navigationBarColor = farben.flaeche.value.toInt()
            WindowCompat.getInsetsController(fenster, view).apply {
                isAppearanceLightStatusBars = !dunkel
                isAppearanceLightNavigationBars = !dunkel
            }
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides farben,
        LocalReduzierteBewegung provides istBewegungReduziert(LocalContext.current),
    ) {
        MaterialTheme(colorScheme = m3, content = content)
    }
}
