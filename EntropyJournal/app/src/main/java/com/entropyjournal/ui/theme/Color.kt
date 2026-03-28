package com.entropyjournal.ui.theme

import androidx.compose.ui.graphics.Color

// Dark mode — warm brown-gray palette
val CosmosBlack = Color(0xFF2D2424)       // Background — warm dark brown
val CosmosDeep = Color(0xFF363333)        // Surface — cards, nav bar
val CosmosLayer = Color(0xFF393E46)       // Elevated — dialogs, inputs
val CosmosSurface = Color(0xFF5C3D2E)     // Container — chips, tags

// Warm accents (dark mode primary palette)
val WarmCopper = Color(0xFFB85C38)        // Primary accent — buttons, links
val WarmSand = Color(0xFFE0C097)          // Secondary accent — highlights
val WarmGold = Color(0xFFD4A76A)          // Tertiary — subtle warmth

// Semantic colors (kept for entropy indicators + priority dots)
val NeonEmerald = Color(0xFF4CAF7D)       // Success / low entropy
val NeonAmber = Color(0xFFFFB300)         // Warning / medium entropy
val NeonRed = Color(0xFFFF5252)           // Error / high entropy
val NeonCyan = Color(0xFF4ECDC4)          // Info / low priority dot

// Legacy neon (still used in some components)
val NeonViolet = Color(0xFF7C4DFF)
val NeonMagenta = Color(0xFFFF00E5)

// Glassmorphism (warm-tinted)
val GlassWhite = Color(0x18E0C097)        // Warm sand overlay 9%
val GlassBorder = Color(0x28E0C097)       // Warm sand border 16%
val GlassHighlight = Color(0x0CE0C097)    // Warm sand highlight 5%

// Text (warm tones for dark mode)
val TextPrimary = Color(0xFFEDE5DD)       // Warm white — high emphasis
val TextSecondary = Color(0xFFA8A098)     // Warm gray — medium emphasis
val TextMuted = Color(0xFF7A7068)         // Warm muted — low emphasis

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
