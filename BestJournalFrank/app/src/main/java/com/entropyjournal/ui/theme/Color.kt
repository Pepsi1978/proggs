package com.entropyjournal.ui.theme

import androidx.compose.ui.graphics.Color

// Dark mode — forest green + orange palette
val CosmosBlack = Color(0xFF2C3930)       // Background — dark forest green
val CosmosDeep = Color(0xFF3C3D37)        // Surface — warm dark gray
val CosmosLayer = Color(0xFF3F4F44)       // Elevated — muted green-gray
val CosmosSurface = Color(0xFFA27B5C)     // Container — warm earth brown

// Warm accents (dark mode primary palette)
val WarmCopper = Color(0xFFD36B00)        // Primary accent — vibrant orange
val WarmSand = Color(0xFFDCD7C9)          // Secondary accent — soft cream
val WarmGold = Color(0xFFA27B5C)          // Tertiary — earth brown

// Semantic colors (kept for entropy indicators + priority dots)
val NeonEmerald = Color(0xFF4CAF7D)       // Success / low entropy
val NeonAmber = Color(0xFFFFB300)         // Warning / medium entropy
val NeonRed = Color(0xFFFF5252)           // Error / high entropy
val NeonCyan = Color(0xFF4ECDC4)          // Info / low priority dot

// Legacy neon (still used in some components)
val NeonViolet = Color(0xFF7C4DFF)
val NeonMagenta = Color(0xFFFF00E5)

// Summary dashboard palette (cool, informative, non-urgent)
val SummaryBlue = Color(0xFF5B8DEF)          // Primary accent — calm blue
val SummaryIndigo = Color(0xFF6366F1)        // Secondary — deep indigo
val SummaryTeal = Color(0xFF14B8A6)          // Highlight — fresh teal
val SummarySlate = Color(0xFF94A3B8)         // Muted — soft slate

// Glassmorphism (cream-tinted)
val GlassWhite = Color(0x18DCD7C9)        // Cream overlay 9%
val GlassBorder = Color(0x28DCD7C9)       // Cream border 16%
val GlassHighlight = Color(0x0CDCD7C9)    // Cream highlight 5%

// Text (cream tones for dark mode)
val TextPrimary = Color(0xFFDCD7C9)       // Soft cream — high emphasis
val TextSecondary = Color(0xFFADA99A)     // Muted cream — medium emphasis
val TextMuted = Color(0xFF7A7870)         // Warm gray — low emphasis

// Gradient pairs
val GradientCyanToViolet = listOf(WarmCopper, WarmSand)
val GradientVioletToMagenta = listOf(NeonViolet, NeonMagenta)
val GradientEmeraldToCyan = listOf(NeonEmerald, NeonCyan)

// Light mode backgrounds
val LightBackground = Color(0xFFF8F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F5)
val LightSurfaceContainer = Color(0xFFE8E8F0)

// Light mode text
val LightTextPrimary = Color(0xFF1A1A2E)
val LightTextSecondary = Color(0xFF5A5A70)
val LightTextMuted = Color(0xFF9090A8)

// Light mode glass
val LightGlassBorder = Color(0x15000000)
val LightGlassBackground = Color(0xFFFFFEFC)
