package de.frank.entropyreducer.presentation.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import de.frank.entropyreducer.data.settings.ThemeMode

/**
 * Theme-Paletten fuer das Home-Screen-Widget (Frank-Wunsch 2026-05-11).
 *
 * Frank moechte ein eigenes Hell-/Dunkel-Theme fuer das Widget, das
 * UNABHAENGIG vom App-Theme einstellbar ist (manuell in Widget-Settings).
 *
 * Die Farben sind 1:1 gespiegelt von theme/Color.kt (CosmosColors,
 * EntropyCategory.color, priorityColor, bucketCardTint). Glance kann die
 * Compose-Theme-Tokens (LocalCosmos, MaterialTheme) nicht direkt nutzen —
 * deshalb hier eine statische Spiegelung. Aenderungen in der App muessen
 * hier mitgezogen werden.
 */
data class WidgetPalette(
    // Hintergrund-Hierarchie
    val bgRoot: Color,
    val surfaceCard: Color,
    val surfaceMuted: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    // Akzent + Bucket
    val accent: Color,
    val border: Color,
    val bucketHeute: Color,
    val bucketMorgen: Color,
    val bucketFreiblock: Color,
    val bucketSpaeter: Color,
    // Kategorien
    val catPhysical: Color,
    val catMental: Color,
    val catTemporal: Color,
    val catEmotional: Color,
    val catHealth: Color,
    val catEnvironment: Color,
    val catOther: Color,
    // Prioritaet (5 Stufen)
    val prioRed: Color,
    val prioOrange: Color,
    val prioYellow: Color,
    val prioGreen: Color,
    val prioBlue: Color,
    // Severity-Bar
    val severityFull: Color,
    val severityEmpty: Color,
    // Card-Tint-Alpha (heller braucht andere Mischung als dunkler)
    val cardTintAlpha: Float,
    // Marker fuer Karten-Gradient-Drawable-Auswahl
    val isDark: Boolean,
)

/**
 * Dunkle Palette — Standard. Spiegelt CosmosColors (theme/Color.kt) im
 * Dark-Mode (effectiveDark = true im Compose-Theme).
 */
val WidgetDarkPalette = WidgetPalette(
    bgRoot = Color(0xFF12100D),
    surfaceCard = Color(0xFF1D1A16),
    surfaceMuted = Color(0xFF26211B),
    textPrimary = Color(0xFFF5F0E8),
    textSecondary = Color(0xFFA89F93),
    accent = Color(0xFFF97316),
    border = Color(0xFF26211B),
    bucketHeute = Color(0xFFEF4444),
    bucketMorgen = Color(0xFFF97316),
    bucketFreiblock = Color(0xFFFACC15),
    bucketSpaeter = Color(0xFF3B82F6),
    catPhysical = Color(0xFFF87171),
    catMental = Color(0xFFA78BFA),
    catTemporal = Color(0xFFFBBF24),
    catEmotional = Color(0xFFF472B6),
    catHealth = Color(0xFF34D399),
    catEnvironment = Color(0xFF22D3EE),
    catOther = Color(0xFF94A3B8),
    prioRed = Color(0xFFEF4444),
    prioOrange = Color(0xFFF97316),
    prioYellow = Color(0xFFFACC15),
    prioGreen = Color(0xFF22C55E),
    prioBlue = Color(0xFF3B82F6),
    severityFull = Color(0xFFFBBF24),
    severityEmpty = Color(0xFF26211B),
    cardTintAlpha = 0.12f,
    isDark = true,
)

/**
 * Helle Palette — heller Hintergrund + dunkler Text, gleiche Akzente.
 * Spiegelt das App-Theme im Light-Mode (effectiveDark = false). Card-Tint
 * etwas saettiger als im Dark-Mode damit die Bucket-Faerbung sichtbar bleibt.
 */
val WidgetLightPalette = WidgetPalette(
    bgRoot = Color(0xFFFAF7F3),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF1ECE5),
    textPrimary = Color(0xFF221C15),
    textSecondary = Color(0xFF6F665B),
    accent = Color(0xFFEA580C),
    border = Color(0xFFE5DED4),
    bucketHeute = Color(0xFFDC2626),
    bucketMorgen = Color(0xFFEA580C),
    bucketFreiblock = Color(0xFFEAB308),
    bucketSpaeter = Color(0xFF2563EB),
    catPhysical = Color(0xFFDC2626),
    catMental = Color(0xFF7C3AED),
    catTemporal = Color(0xFFD97706),
    catEmotional = Color(0xFFDB2777),
    catHealth = Color(0xFF059669),
    catEnvironment = Color(0xFF0891B2),
    catOther = Color(0xFF64748B),
    prioRed = Color(0xFFDC2626),
    prioOrange = Color(0xFFEA580C),
    prioYellow = Color(0xFFEAB308),
    prioGreen = Color(0xFF16A34A),
    prioBlue = Color(0xFF2563EB),
    severityFull = Color(0xFFD97706),
    severityEmpty = Color(0xFFF1ECE5),
    cardTintAlpha = 0.18f,
    isDark = false,
)

/**
 * Loest die effektive Palette unter Beruecksichtigung von Widget-Theme-Mode
 * und Geraete-Konfiguration.
 *
 *  - LIGHT  → immer hell, egal was das Geraet sagt
 *  - DARK   → immer dunkel, egal was das Geraet sagt
 *  - SYSTEM → folgt der aktuellen Geraete-Konfiguration (Configuration.uiMode)
 */
fun resolveWidgetPalette(context: Context, mode: ThemeMode): WidgetPalette {
    val isSystemDark = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val effectiveDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    return if (effectiveDark) WidgetDarkPalette else WidgetLightPalette
}
