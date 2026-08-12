package de.frank.experimente.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Die drei Möglichkeiten aus F-26. Gespeichert wird der **Modus**, nicht die gerade
 * aufgelöste Farbe — nur so folgt `SYSTEM` auch späteren Systemwechseln ohne Neustart (A-19).
 */
enum class Erscheinung(val schluessel: String) {
    HELL("light"),
    DUNKEL("dark"),
    SYSTEM("system");

    /** Die feste Reihenfolge des Schnellschalters auf B-01: Hell → Dunkel → System → Hell. */
    fun naechste(): Erscheinung = when (this) {
        HELL -> DUNKEL
        DUNKEL -> SYSTEM
        SYSTEM -> HELL
    }

    companion object {
        fun aus(schluessel: String?): Erscheinung =
            entries.firstOrNull { it.schluessel == schluessel } ?: DUNKEL
    }
}

@Composable
fun ExperimenteTheme(
    erscheinung: Erscheinung,
    effektstufe: Effektstufe = Effektstufe.VOLL,
    inhalt: @Composable () -> Unit,
) {
    // `isSystemInDarkTheme()` liest die Systemdarstellung bei jeder Neuzeichnung neu —
    // damit greift ein Wechsel im System sofort, solange SYSTEM aktiv ist.
    val dunkel = when (erscheinung) {
        Erscheinung.HELL -> false
        Erscheinung.DUNKEL -> true
        Erscheinung.SYSTEM -> isSystemInDarkTheme()
    }
    val farben = if (dunkel) FarbenDunkel else FarbenHell

    // Material 3 wird nur mitversorgt, damit eingebaute Bauteile (Textfeld-Cursor,
    // Auswahlgriffe) nicht aus dem Rahmen fallen. Gezeichnet wird aus `LocalFarben`.
    val schema = if (dunkel) {
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

    CompositionLocalProvider(
        LocalFarben provides farben,
        LocalSchriften provides Schriften(),
        // A-30: „Bewegung reduzieren" und der Energiesparmodus erzwingen mindestens *Gedämpft*.
        LocalEffektstufe provides geltendeStufe(effektstufe),
    ) {
        MaterialTheme(colorScheme = schema, content = inhalt)
    }
}
