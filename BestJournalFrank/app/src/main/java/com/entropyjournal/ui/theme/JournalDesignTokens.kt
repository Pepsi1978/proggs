package com.entropyjournal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

const val DESIGN_FOUNDATION_VERSION = "v0.21.1 - 18.07.2026 22:21 Uhr"

data class JournalDesignTokens(
    val backgroundBrush: Brush,
    val cardBrush: Brush,
    val accentBrush: Brush,
    val hairlineBrush: Brush,
    val card: Color,
    val cardBorder: Color,
    val chipBackground: Color,
    val chipBorder: Color,
    val navigationBackground: Color,
    val glow: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
)

private fun linear(first: Color, second: Color): Brush =
    Brush.linearGradient(listOf(first, second))

private fun linear(first: Color, second: Color, third: Color, fourth: Color): Brush =
    Brush.linearGradient(listOf(first, second, third, fourth))

private fun linear(
    first: Color,
    second: Color,
    third: Color,
    fourth: Color,
    fifth: Color,
): Brush = Brush.linearGradient(listOf(first, second, third, fourth, fifth))

private fun vertical(first: Color, second: Color): Brush =
    Brush.verticalGradient(listOf(first, second))

private fun goldTokens(isDark: Boolean): JournalDesignTokens =
    if (isDark) {
        JournalDesignTokens(
            backgroundBrush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color(0xFF1E1712), 0.6f to Color(0xFF16110D), 1f to Color(0xFF100C09)),
                center = Offset(540f, 0f),
                radius = 1500f,
            ),
            cardBrush = linear(Color(0xFF221A13), Color(0xFF1A140F)),
            accentBrush = linear(Color(0xFFE8B547), Color(0xFFDF741E)),
            hairlineBrush = linear(Color.Transparent, Color(0xFFE8B547), Color(0xFFDF741E), Color(0xFFE8B547), Color.Transparent),
            card = Color(0xFF221A13),
            cardBorder = Color(0x47E8B547),
            chipBackground = Color(0x17E8B547),
            chipBorder = Color(0x66E8B547),
            navigationBackground = Color(0xE01E1712),
            glow = Color(0x4DE8B547),
            success = Color(0xFF9CBF7E),
            warning = Color(0xFFFFA94D),
            danger = Color(0xFFFF7B6B),
            priorityHigh = Color(0xFFFF6B5E),
            priorityMedium = Color(0xFFFFA94D),
            textPrimary = Color(0xFFEFE7DB),
            textSecondary = Color(0xFFD8CCBA),
            textMuted = Color(0xFF9C8D77),
        )
    } else {
        JournalDesignTokens(
            backgroundBrush = vertical(Color(0xFFFBF5EA), Color(0xFFF6EDDD)),
            cardBrush = Brush.linearGradient(listOf(Color(0xFFFFFDF8), Color(0xFFFFFDF8))),
            accentBrush = linear(Color(0xFFD9A441), Color(0xFFB45309)),
            hairlineBrush = linear(Color.Transparent, Color(0xFFD9A441), Color(0xFFB45309), Color(0xFFD9A441), Color.Transparent),
            card = Color(0xFFFFFDF8),
            cardBorder = Color(0x73D9A441),
            chipBackground = Color(0x24D9A441),
            chipBorder = Color(0x59B45309),
            navigationBackground = Color(0xEBFFFDF8),
            glow = Color(0x33B45309),
            success = Color(0xFF5C8C4A),
            warning = Color(0xFFB45309),
            danger = Color(0xFFC03A2B),
            priorityHigh = Color(0xFFC03A2B),
            priorityMedium = Color(0xFFB45309),
            textPrimary = Color(0xFF33291C),
            textSecondary = Color(0xFF4A3D2A),
            textMuted = Color(0xFF8C7B5F),
        )
    }

private fun themedTokens(
    scheme: ColorScheme,
    backgroundStart: Color,
    backgroundEnd: Color,
    card: Color = scheme.surface,
    border: Color = scheme.outlineVariant,
    glow: Color = scheme.primary.copy(alpha = 0.3f),
    navigation: Color = card.copy(alpha = if (card.alpha < 1f) 0.88f else 0.92f),
): JournalDesignTokens =
    JournalDesignTokens(
        backgroundBrush = vertical(backgroundStart, backgroundEnd),
        cardBrush = Brush.linearGradient(listOf(card, card)),
        accentBrush = linear(scheme.primary, scheme.secondary),
        hairlineBrush = linear(Color.Transparent, scheme.primary, scheme.secondary, Color.Transparent),
        card = card,
        cardBorder = border,
        chipBackground = scheme.primary.copy(alpha = 0.1f),
        chipBorder = scheme.primary.copy(alpha = 0.35f),
        navigationBackground = navigation,
        glow = glow,
        success = scheme.tertiary,
        warning = scheme.secondary,
        danger = scheme.error,
        priorityHigh = scheme.error,
        priorityMedium = scheme.secondary,
        textPrimary = scheme.onBackground,
        textSecondary = scheme.onSurface,
        textMuted = scheme.onSurfaceVariant,
    )

fun journalDesignTokens(
    appTheme: AppTheme,
    isDark: Boolean,
    scheme: ColorScheme,
): JournalDesignTokens =
    when (appTheme) {
        AppTheme.GoldenThread -> goldTokens(isDark)
        AppTheme.PolarLight -> if (isDark) {
            themedTokens(scheme, Color(0xFF0C1B2A), Color(0xFF081018), Color(0x0FFFFFFF), Color(0x4D4CC9F0), navigation = Color(0xB30C1B2A))
        } else {
            themedTokens(scheme, Color(0xFFF2F8FC), Color(0xFFE3EFF8), Color(0xD9FFFFFF), Color(0x330077B6), navigation = Color(0xE6FFFFFF))
        }
        AppTheme.Nebula -> if (isDark) {
            themedTokens(scheme, Color(0xFF1C1030), Color(0xFF140A24), Color(0x0FFFFFFF), Color(0x59C084FC), navigation = Color(0xBF1C1030))
        } else {
            themedTokens(scheme, Color(0xFFFAF5FF), Color(0xFFF6ECFB), Color(0xE6FFFFFF), Color(0x339333EA), navigation = Color(0xEBFFFFFF))
        }
        AppTheme.EmeraldForest -> if (isDark) {
            themedTokens(scheme, Color(0xFF0E1F17), Color(0xFF08120E), Color(0x0FFFFFFF), Color(0x5234D399), navigation = Color(0xBF0E1F17))
        } else {
            themedTokens(scheme, Color(0xFFF3F8F4), Color(0xFFE9F2E9), Color(0xE6FFFFFF), Color(0x33047857), navigation = Color(0xEBFFFFFF))
        }
        AppTheme.SunEmber -> if (isDark) {
            themedTokens(scheme, Color(0xFF231410), Color(0xFF170E08), Color(0x0FFFFFFF), Color(0x59FF8C42), navigation = Color(0xC7231410))
        } else {
            themedTokens(scheme, Color(0xFFFDF6EE), Color(0xFFFAEBDD), Color(0xEBFFFFFF), Color(0x38C2410C), navigation = Color(0xF0FFFFFF))
        }
        AppTheme.Profile -> if (isDark) {
            themedTokens(scheme, Color(0xFF0F1A20), Color(0xFF101825), Color(0x14FFFFFF), scheme.primary.copy(alpha = 0.4f))
        } else {
            themedTokens(scheme, scheme.background, scheme.surfaceVariant)
        }
        AppTheme.Neutral -> if (isDark) {
            themedTokens(scheme, Color(0xFF141210), Color(0xFF0F0E0C), Color(0xFF1E1A16), Color(0x66C25E00))
        } else {
            themedTokens(scheme, scheme.background, scheme.surfaceVariant)
        }
        AppTheme.Solarized -> if (isDark) {
            themedTokens(scheme, Color(0xFF003545), Color(0xFF00212B), Color(0xFF0A3B47), Color(0x732AA198))
        } else {
            themedTokens(scheme, scheme.background, scheme.surfaceVariant)
        }
        AppTheme.Dracula -> if (isDark) {
            themedTokens(scheme, Color(0xFF2B2D3E), Color(0xFF1F2130), Color(0xFF31344A), Color(0x73BD93F9))
        } else {
            themedTokens(scheme, scheme.background, scheme.surfaceVariant)
        }
        AppTheme.OneDark -> if (isDark) {
            themedTokens(scheme, Color(0xFF2B3039), Color(0xFF1E222A), Color(0xFF2E3440), Color(0x6661AFEF))
        } else {
            themedTokens(scheme, scheme.background, scheme.surfaceVariant)
        }
        AppTheme.Nord -> if (isDark) {
            themedTokens(scheme, Color(0xFF323A4A), Color(0xFF272E3B), Color(0xFF3D4658), Color(0x7388C0D0))
        } else {
            themedTokens(scheme, scheme.background, scheme.surfaceVariant)
        }
        AppTheme.Gruvbox -> if (isDark) {
            themedTokens(scheme, Color(0xFF2C2622), Color(0xFF211D1A), Color(0xFF363029), Color(0x73D79921))
        } else {
            themedTokens(scheme, scheme.background, scheme.surfaceVariant)
        }
        AppTheme.Cosmos -> if (isDark) {
            themedTokens(scheme, ERBgDarkMid, ERBgDark, Color(0x14FFFFFF), Color(0x7322D3EE))
        } else {
            themedTokens(scheme, ERBgLight, ERBgLightMid, Color(0xCCFFFFFF), ERGlassLightBorder)
        }
        AppTheme.Aurora -> if (isDark) {
            themedTokens(scheme, Color(0xFF173035), Color(0xFF432234), Color(0x17FFFFFF), Color(0x73B5A8E8))
        } else {
            themedTokens(scheme, AuroraLightGradientStart, AuroraLightGradientEnd, Color.White, AuroraOutlineLight)
        }
    }

val LocalJournalDesignTokens = staticCompositionLocalOf { goldTokens(isDark = true) }
