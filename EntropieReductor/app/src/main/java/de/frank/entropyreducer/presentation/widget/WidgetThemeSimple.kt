package de.frank.entropyreducer.presentation.widget

import android.content.Context
import android.content.res.Configuration
import de.frank.entropyreducer.data.settings.ThemeMode

/**
 * Int-basierte Palette fuer den klassischen RemoteViews-Widget-Provider.
 *
 * Compose Color (androidx.compose.ui.graphics.Color) kann in RemoteViews
 * nicht direkt verwendet werden — wir brauchen ARGB-Ints. Diese Palette
 * spiegelt 1:1 die WidgetPalette aus WidgetTheme.kt (die fuer Glance war),
 * aber als Int-Werte. Bei Aenderungen muessen beide synchron gehalten werden.
 */
data class SimpleWidgetPalette(
    val bgRoot: Int,
    val surfaceCard: Int,
    val surfaceMuted: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val accent: Int,
    val border: Int,
    val bucketHeute: Int,
    val bucketMorgen: Int,
    val bucketFreiblock: Int,
    val bucketSpaeter: Int,
    val catPhysical: Int,
    val catMental: Int,
    val catTemporal: Int,
    val catEmotional: Int,
    val catHealth: Int,
    val catEnvironment: Int,
    val catOther: Int,
    val prioRed: Int,
    val prioOrange: Int,
    val prioYellow: Int,
    val prioGreen: Int,
    val prioBlue: Int,
    val severityEmpty: Int,
    val cardTintAlpha: Float,
    // Perlen-Farben EXAKT wie die App-Karte (Frank-Wunsch 2026-05-31): KI/manuell-
    // und Prio-Perle nutzen cosmos.glassBg (Hintergrund) + cosmos.textSecondary
    // (Text/Icon). Beide Perlen identisch — nur das Wort unterscheidet.
    val pearlBg: Int,
    val pearlText: Int,
    val isDark: Boolean,
)

val SimpleWidgetDarkPalette = SimpleWidgetPalette(
    bgRoot = 0xFF12100D.toInt(),
    surfaceCard = 0xFF1D1A16.toInt(),
    surfaceMuted = 0xFF26211B.toInt(),
    textPrimary = 0xFFF5F0E8.toInt(),
    textSecondary = 0xFFA89F93.toInt(),
    accent = 0xFFF97316.toInt(),
    border = 0xFF26211B.toInt(),
    bucketHeute = 0xFFEF4444.toInt(),
    bucketMorgen = 0xFFF97316.toInt(),
    bucketFreiblock = 0xFFFACC15.toInt(),
    bucketSpaeter = 0xFF3B82F6.toInt(),
    catPhysical = 0xFFF87171.toInt(),
    catMental = 0xFFA78BFA.toInt(),
    catTemporal = 0xFFFBBF24.toInt(),
    catEmotional = 0xFFF472B6.toInt(),
    catHealth = 0xFF34D399.toInt(),
    catEnvironment = 0xFF22D3EE.toInt(),
    catOther = 0xFF94A3B8.toInt(),
    prioRed = 0xFFEF4444.toInt(),
    prioOrange = 0xFFF97316.toInt(),
    prioYellow = 0xFFFACC15.toInt(),
    prioGreen = 0xFF22C55E.toInt(),
    prioBlue = 0xFF3B82F6.toInt(),
    severityEmpty = 0xFF26211B.toInt(),
    cardTintAlpha = 0.12f,
    pearlBg = 0x14FFFFFF, // GlassDark — weiss alpha 0.08 (App: cosmos.glassBg)
    pearlText = 0xFF94A3B8.toInt(), // TextSecondaryDark (App: cosmos.textSecondary)
    isDark = true,
)

val SimpleWidgetLightPalette = SimpleWidgetPalette(
    bgRoot = 0xFFFAF7F3.toInt(),
    surfaceCard = 0xFFFFFFFF.toInt(),
    surfaceMuted = 0xFFF1ECE5.toInt(),
    textPrimary = 0xFF221C15.toInt(),
    textSecondary = 0xFF6F665B.toInt(),
    accent = 0xFFEA580C.toInt(),
    border = 0xFFE5DED4.toInt(),
    bucketHeute = 0xFFDC2626.toInt(),
    bucketMorgen = 0xFFEA580C.toInt(),
    bucketFreiblock = 0xFFEAB308.toInt(),
    bucketSpaeter = 0xFF2563EB.toInt(),
    catPhysical = 0xFFDC2626.toInt(),
    catMental = 0xFF7C3AED.toInt(),
    catTemporal = 0xFFD97706.toInt(),
    catEmotional = 0xFFDB2777.toInt(),
    catHealth = 0xFF059669.toInt(),
    catEnvironment = 0xFF0891B2.toInt(),
    catOther = 0xFF64748B.toInt(),
    prioRed = 0xFFDC2626.toInt(),
    prioOrange = 0xFFEA580C.toInt(),
    prioYellow = 0xFFEAB308.toInt(),
    prioGreen = 0xFF16A34A.toInt(),
    prioBlue = 0xFF2563EB.toInt(),
    severityEmpty = 0xFFF1ECE5.toInt(),
    cardTintAlpha = 0.18f,
    pearlBg = 0xCCFFFFFF.toInt(), // GlassLight — weiss alpha 0.80 (App: cosmos.glassBg)
    pearlText = 0xFF475569.toInt(), // TextSecondaryLight (App: cosmos.textSecondary)
    isDark = false,
)

fun resolveWidgetPaletteSimple(context: Context, mode: ThemeMode): SimpleWidgetPalette {
    val isSystemDark = (context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val effectiveDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    return if (effectiveDark) SimpleWidgetDarkPalette else SimpleWidgetLightPalette
}
