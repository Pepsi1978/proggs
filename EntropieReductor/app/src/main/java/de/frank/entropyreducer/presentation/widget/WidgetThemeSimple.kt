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
    val isDark: Boolean,
)

val SimpleWidgetDarkPalette = SimpleWidgetPalette(
    bgRoot = 0xFF0E0B24.toInt(),
    surfaceCard = 0xFF1A1438.toInt(),
    surfaceMuted = 0xFF2A1F44.toInt(),
    textPrimary = 0xFFEAE6FF.toInt(),
    textSecondary = 0xFFA499D9.toInt(),
    accent = 0xFFB388FF.toInt(),
    border = 0xFF2A1F44.toInt(),
    bucketHeute = 0xFFF472B6.toInt(),
    bucketMorgen = 0xFFFBBF24.toInt(),
    bucketFreiblock = 0xFF22C55E.toInt(),
    bucketSpaeter = 0xFF60A5FA.toInt(),
    catPhysical = 0xFFF87171.toInt(),
    catMental = 0xFFA78BFA.toInt(),
    catTemporal = 0xFFFBBF24.toInt(),
    catEmotional = 0xFFF472B6.toInt(),
    catHealth = 0xFF34D399.toInt(),
    catEnvironment = 0xFF22D3EE.toInt(),
    catOther = 0xFF94A3B8.toInt(),
    prioRed = 0xFFEF4444.toInt(),
    prioOrange = 0xFFF97316.toInt(),
    prioYellow = 0xFFEAB308.toInt(),
    prioGreen = 0xFF22C55E.toInt(),
    prioBlue = 0xFF3B82F6.toInt(),
    severityEmpty = 0xFF2A1F44.toInt(),
    cardTintAlpha = 0.12f,
    isDark = true,
)

val SimpleWidgetLightPalette = SimpleWidgetPalette(
    bgRoot = 0xFFFAF7FF.toInt(),
    surfaceCard = 0xFFFFFFFF.toInt(),
    surfaceMuted = 0xFFEBE6F8.toInt(),
    textPrimary = 0xFF1E1A3A.toInt(),
    textSecondary = 0xFF6B6385.toInt(),
    accent = 0xFF7C3AED.toInt(),
    border = 0xFFD9D3EC.toInt(),
    bucketHeute = 0xFFDB2777.toInt(),
    bucketMorgen = 0xFFD97706.toInt(),
    bucketFreiblock = 0xFF16A34A.toInt(),
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
    prioYellow = 0xFFCA8A04.toInt(),
    prioGreen = 0xFF16A34A.toInt(),
    prioBlue = 0xFF2563EB.toInt(),
    severityEmpty = 0xFFEBE6F8.toInt(),
    cardTintAlpha = 0.18f,
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
