package de.frank.jarvis.bio.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import de.frank.jarvis.ui.theme.Farben
import de.frank.jarvis.ui.theme.LocalFarben

/**
 * Schlanke Kopie der Farb-Schnittstelle aus Entropie Reductor, gespeist aus Jarvis' Farben ([LocalFarben]).
 * So bleiben die übernommenen Biomarker-Bildschirme unverändert, sehen aber aus wie Jarvis. Kein eigener Hell/Dunkel-Umschalter.
 */
data class CosmosThemeExt(
    val isDark: Boolean,
    val glassBg: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentForscher: Color,
    val ok: Color,
    val warn: Color,
    val crit: Color,
    val dialogBg: Color,
    /** Die Jarvis-Farben selbst, für die Glas-Optik der Karten. */
    val farben: Farben,
)

val LocalCosmos = staticCompositionLocalOf<CosmosThemeExt> { error("LocalCosmos fehlt: BioThema { } um den Biomarker-Bereich legen") }

fun cosmosAus(f: Farben): CosmosThemeExt = CosmosThemeExt(
    isDark = f.dunkel,
    glassBg = f.flaeche,
    glassBorder = f.rand,
    textPrimary = f.text,
    textSecondary = f.textLeise,
    accent = f.primaer,
    accentForscher = f.sekundaer,
    ok = f.erfolg,
    warn = if (f.dunkel) CosmosColors.Warning else CosmosColors.WarningLight,
    crit = f.gefahr,
    dialogBg = f.hgUnten,
    farben = f,
)

/** Legt die Biomarker-Farben aus dem aktuellen Jarvis-Thema um [inhalt]. */
@Composable
fun BioThema(inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    val ext = remember(f) { cosmosAus(f) }
    CompositionLocalProvider(LocalCosmos provides ext, content = inhalt)
}

/** Feste, funktionale Farben (Ampeln, Skalen) aus Entropie Reductor; Primär an Jarvis' Blau angeglichen. */
object CosmosColors {
    val BgDarkAccent = Color(0xFF0F1A2C)
    val BgLightAccent = Color(0xFFE6F0FA)
    val AccentPrimary = Color(0xFF4FC8FF)
    val AccentSecondary = Color(0xFFA78BFA)
    val Success = Color(0xFF34C77B)
    val Warning = Color(0xFFFBBF24)
    val WarningLight = Color(0xFFD97706)
    val Critical = Color(0xFFF87171)

    // Offizielle WHOOP-Recovery-Farben (funktional): grün 67–100 %, gelb 34–66 %, rot 0–33 %.
    val WhoopRecoveryGreen = Color(0xFF16EC06)
    val WhoopRecoveryYellow = Color(0xFFFFDE00)
    val WhoopRecoveryRed = Color(0xFFFF0026)
}

/** Offizielle WHOOP-Recovery-Ampel. */
fun whoopRecoveryColor(percent: Double): Color = when {
    percent >= 67.0 -> CosmosColors.WhoopRecoveryGreen
    percent >= 34.0 -> CosmosColors.WhoopRecoveryYellow
    else -> CosmosColors.WhoopRecoveryRed
}
