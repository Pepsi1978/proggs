package com.nems.app.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// DARK MODE — Spotify-Stil
// Solid colors, minimal contrast, warm copper/gold/sand accents
// ═══════════════════════════════════════════════════════════════

// Background layers
val CosmosBlack = Color(0xFF121212)        // App-Hintergrund
val CosmosDeep = Color(0xFF181818)         // Karten/Surfaces
val CardElevated = Color(0xFF1E1E1E)       // Erhoehte Karten
val CosmosLayer = Color(0xFF282828)        // Hervorgehobene Karten, Dialoge

// Card surfaces (Spotify tonal layering)
val CardSurface = CosmosDeep               // Standard-Karte (#181818)
val CardHighlighted = CosmosLayer          // Selektierte/Hover (#282828)

// Warm accents
val WarmCopper = Color(0xFFC25E00)         // Primaer-Akzent (Buttons, Links, aktive Elemente)
val WarmSand = Color(0xFFE0DCD4)           // Sekundaer-Akzent (Soft-Akzent, sekundaere Buttons)
val WarmGold = Color(0xFF8B6914)           // Tertiaer-Akzent (Gold-Highlights, Badges)
val FeatureAccent = Color(0xFFFF8C00)      // Feature-Akzent (Orange)

// Semantic status colors
val NeonEmerald = Color(0xFF4CAF7D)        // Erfolg / Gruen
val NeonAmber = Color(0xFFFFB300)          // Warnung / Gelb
val NeonRed = Color(0xFFFF5252)            // Fehler / Rot
val NeonCyan = Color(0xFF4ECDC4)           // Info / Cyan

// Text (dark mode)
val TextPrimary = Color(0xFFE6E1E5)        // Haupttext
val TextSecondary = Color(0xFFCAC4D0)      // Untertitel, Beschreibungen
val TextMuted = Color(0xFF938F99)          // Timestamps, Platzhalter, Labels

// Convenience aliases for screens
val StatusGreen = NeonEmerald
val StatusYellow = NeonAmber
val StatusRed = NeonRed
val StatusGray = Color(0xFF48484A)

// Supplement indicators
val RiskYellow = NeonAmber
val PowderGreen = NeonEmerald
val FatSoluble = WarmCopper
val WaterSoluble = NeonCyan

// Section accent colors (warm palette for stack sections)
val SectionMorning1 = WarmCopper           // Morgen-Stack Teil 1
val SectionMorning2 = NeonAmber            // Morgen-Stack Teil 2
val SectionPreSport = NeonEmerald          // Pre-Sport-Stack
val SectionEvening1 = Color(0xFF5B8DEF)    // Abend-Stack Teil 1 (Blue)
val SectionEvening2 = Color(0xFFA78BFA)    // Abend-Stack Teil 2 (Violet)
val SectionEvening3 = NeonCyan             // Abend-Stack Teil 3 (Cyan)

// ═══════════════════════════════════════════════════════════════
// LIGHT MODE
// ═══════════════════════════════════════════════════════════════

val LightBackground = Color(0xFFF8F8FC)         // App-Hintergrund
val LightSurface = Color(0xFFFFFFFF)             // Weisse Karten
val LightSurfaceVariant = Color(0xFFF0F0F5)      // Leicht getoenter Hintergrund
val LightSurfaceContainer = Color(0xFFE8E8F0)    // Staerkerer Toenungs-Container
val LightGlassBackground = Color(0xFFFFFEFC)     // Glas-Effekt-Hintergrund
val LightGlassBorder = Color(0x15000000)         // Sehr subtiler Glasrand (6% Schwarz)

val LightTextPrimary = Color(0xFF1A1A2E)         // Haupttext
val LightTextSecondary = Color(0xFF5A5A70)       // Untertitel
val LightTextMuted = Color(0xFF6E6E86)           // Platzhalter, Labels
