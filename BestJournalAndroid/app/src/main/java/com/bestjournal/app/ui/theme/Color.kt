package com.bestjournal.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// Dark mode — Option A: Spotify-Stil (Minimalist Dark)
// Inspired by Spotify, Twitter/X, Material 3 tonal surfaces
// Principle: solid colors, minimal contrast, NO borders/gradients
// ═══════════════════════════════════════════════════════════════
val CosmosBlack = Color(0xFF121212)       // Background — pure dark (Spotify standard)
val CosmosDeep = Color(0xFF181818)        // Surface / Card — barely lighter (+6)
val CosmosLayer = Color(0xFF282828)       // Elevated surface — hover/dialog (+16)
val CosmosSurface = Color(0xFF8B6914)     // Container accent — warm gold-brown

// Warm accents (dark mode primary palette — unchanged)
val WarmCopper = Color(0xFFD36B00)        // Primary accent — vibrant orange
val WarmSand = Color(0xFFE0DCD4)          // Secondary accent — soft cream
val WarmGold = Color(0xFF8B6914)          // Tertiary — warm gold

// Semantic colors (kept for entropy indicators + priority dots)
val NeonEmerald = Color(0xFF4CAF7D)       // Success / low entropy
val NeonAmber = Color(0xFFFFB300)         // Warning / medium entropy
val NeonRed = Color(0xFFFF5252)           // Error / high entropy
val NeonCyan = Color(0xFF4ECDC4)          // Info / low priority dot

// Legacy neon (still used in some components)
val NeonViolet = Color(0xFF7C4DFF)
val NeonMagenta = Color(0xFFFF00E5)

// Summary dashboard palette (cool, informative, non-urgent)
val SummaryBlue = Color(0xFF5B8DEF)
val SummaryIndigo = Color(0xFF6366F1)
val SummaryTeal = Color(0xFF14B8A6)
val SummarySlate = Color(0xFF94A3B8)

// Self-insight dashboard palette (warm, introspective, personal)
val InsightViolet = Color(0xFFA78BFA)
val InsightRose = Color(0xFFF472B6)
val InsightWarm = Color(0xFFFBBF24)
val InsightMauve = Color(0xFFC084FC)

// Goals dashboard palette (motivating, progress-oriented)
val GoalEmerald = Color(0xFF10B981)
val GoalGold = Color(0xFFF59E0B)
val GoalSky = Color(0xFF38BDF8)
val GoalCoral = Color(0xFFFB7185)

// Custom/Individual analysis palette (neutral, warm, adaptable)
val CustomAmber = Color(0xFFE8A838)          // Primary — warm amber
val CustomSand = Color(0xFFD4A574)           // Secondary — earthy sand
val CustomSage = Color(0xFF8FAE8B)           // Highlight — soft sage green
val CustomStone = Color(0xFFA09890)          // Muted — warm stone

// Dark mode card surfaces (tonal layering — Spotify principle)
val CardSurface = Color(0xFF181818)       // Standard card — barely visible over background
val CardElevated = Color(0xFF1E1E1E)      // Slightly elevated card
val CardHighlighted = Color(0xFF282828)   // Highlighted / selected / hover

// Glassmorphism — kept for light mode only, dark mode uses solid colors
val GlassWhite = Color(0x18DCD7C9)
val GlassBorder = Color(0x28DCD7C9)
val GlassHighlight = Color(0x0CDCD7C9)

// Text (high contrast on dark background)
val TextPrimary = Color(0xFFE6E1E5)       // M3 onSurface — primary text
val TextSecondary = Color(0xFFCAC4D0)     // M3 onSurfaceVariant — secondary text
val TextMuted = Color(0xFF938F99)         // M3 outline — muted text

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
