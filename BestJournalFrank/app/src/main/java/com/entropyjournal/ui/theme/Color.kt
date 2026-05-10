package com.entropyjournal.ui.theme

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
val WarmCopper = Color(0xFFC25E00)        // Primary accent — warm copper (WCAG AA on white)
val WarmSand = Color(0xFFE0DCD4)          // Secondary accent — soft cream
val WarmGold = Color(0xFF8B6914)          // Tertiary — warm gold

// Semantic colors (kept for entropy indicators + priority dots)
val NeonEmerald = Color(0xFF4CAF7D)       // Success / low entropy
val NeonAmber = Color(0xFFFFB300)         // Warning / medium entropy
val NeonRed = Color(0xFFFF5252)           // Error / high entropy
val NeonCyan = Color(0xFF4ECDC4)          // Info / low priority dot
val FeatureAccentOrange = Color(0xFFFF8C00) // TTS speaker + share buttons — highlights special features

// @Deprecated: Use semantic colors (NeonEmerald, NeonAmber, NeonRed) instead
val NeonViolet = Color(0xFF7C4DFF)
val NeonMagenta = Color(0xFFFF00E5)

// Dashboard category palette — each category has 4 semantic color roles
data class DashboardPalette(
    val primary: Color,       // Main accent — glow, headings, buttons
    val secondary: Color,     // Gradient partner, HIGH priority
    val accent: Color,        // Highlight — LOW priority, tertiary elements
    val muted: Color,         // Subdued — subtitle text, legend dots
)

val SummaryPalette = DashboardPalette(
    primary = Color(0xFF5B8DEF),     // Blue
    secondary = Color(0xFF6366F1),   // Indigo
    accent = Color(0xFF14B8A6),      // Teal
    muted = Color(0xFF94A3B8),       // Slate
)

val InsightPalette = DashboardPalette(
    primary = Color(0xFFA78BFA),     // Violet
    secondary = Color(0xFFF472B6),   // Rose
    accent = Color(0xFFFBBF24),      // Warm
    muted = Color(0xFFC084FC),       // Mauve
)

val GoalPalette = DashboardPalette(
    primary = Color(0xFF10B981),     // Emerald
    secondary = Color(0xFF38BDF8),   // Sky
    accent = Color(0xFFF59E0B),      // Gold
    muted = Color(0xFFFB7185),       // Coral
)

val CustomPalette = DashboardPalette(
    primary = Color(0xFFE8A838),     // Amber
    secondary = Color(0xFFD4A574),   // Sand
    accent = Color(0xFF8FAE8B),      // Sage
    muted = Color(0xFFA09890),       // Stone
)

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
val LightTextMuted = Color(0xFF6E6E86)

// Light mode glass
val LightGlassBorder = Color(0x15000000)
val LightGlassBackground = Color(0xFFFFFEFC)

// ═══════════════════════════════════════════════════════════════
// EntropieReductor "Neon Cosmos" Palette — 1:1 von ~/proggs/EntropieReductor
// Dunkles Galaxy-Blau mit Cyan-Akzenten + Off-White Light mit
// dezenter Galaxy-Andeutung. Frank-Wunsch 2026-05-08: als auswaehlbares
// Theme im Themes Manager der BestJournalFrank App.
// ═══════════════════════════════════════════════════════════════

// Hintergruende — Dark
val ERBgDark = Color(0xFF0A0E1A)
val ERBgDarkMid = Color(0xFF0F1729)
val ERBgDarkAccent = Color(0xFF1A1F38)

// Hintergruende — Light (Off-White mit dezenter Galaxy-Andeutung)
val ERBgLight = Color(0xFFF5F7FB)
val ERBgLightMid = Color(0xFFE8EDFA)
val ERBgLightAccent = Color(0xFFDCE3F5)

// Glas-Flaechen (fuer Boxen/Karten)
val ERGlassDark = Color(0x14FFFFFF)             // weisses Overlay alpha 0.08
val ERGlassDarkBorder = Color(0x29FFFFFF)       // weisses Overlay alpha 0.16
val ERGlassLight = Color(0xCCFFFFFF)            // weiss alpha 0.80
val ERGlassLightBorder = Color(0x14000000)      // schwarz alpha 0.08

// Akzente (gleich in beiden Modi)
val ERAccentPrimary = Color(0xFF22D3EE)         // Cyan
val ERAccentSecondary = Color(0xFFA78BFA)       // Violett
val ERSuccess = Color(0xFF34D399)               // Mintgruen
val ERWarning = Color(0xFFFBBF24)               // Bernstein
val ERCritical = Color(0xFFF87171)              // Korallrot

// Text
val ERTextPrimaryDark = Color(0xFFF8FAFC)
val ERTextSecondaryDark = Color(0xFF94A3B8)
val ERTextPrimaryLight = Color(0xFF0F172A)
val ERTextSecondaryLight = Color(0xFF475569)

// Glas-Mix-Farben — entstehen wenn der GlassDark/Light-Overlay
// auf BgDark/BgLight liegt. EntropieReductor zeichnet alle Cards mit eigenem
// GlassCard-Composable, hier in BestJournalFrank werden die Material3-Defaults
// genutzt, also setzen wir die surface-Slots direkt auf die berechneten Werte.
// Mathematik: rgb_out = rgb_overlay * alpha + rgb_bg * (1 - alpha)
//
// Dark: GlassDark = white@0.078 ueber BgDark #0A0E1A → #1D212C (graublau)
// Light: GlassLight = white@0.80 ueber BgLight #F5F7FB → #FDFDFE (fast weiss)
val ERBgDarkGlass = Color(0xFF1D212C)             // Standard-Surface (Cards)
val ERBgDarkGlassElevated = Color(0xFF272B36)     // Elevated-Surface (Hover/Dialog)
val ERBgLightGlass = Color(0xFFFDFDFE)            // Standard-Surface Light
val ERBgLightGlassElevated = Color(0xFFEEF2F9)    // Elevated-Surface Light

// ─── Aurora ─────────────────────────────────────────────────────────────────
// Pastel-Aquarell-Theme: weicher 3-Stop-Diagonalgradient als Vollbild-Hintergrund,
// weisse Glasmorphismus-Cards, gedaempftes Smaragdgruen als Hauptakzent.
// Inspiriert von den Referenzbildern (Frank 2026-05-10).

// Light-Gradient-Stops (Top-Left → Mid → Bottom-Right)
val AuroraLightGradientStart = Color(0xFFD8EDDF)   // blasses Mintgruen
val AuroraLightGradientMid = Color(0xFFE2D6F0)     // weiches Lavendel
val AuroraLightGradientEnd = Color(0xFFF2DDE8)     // gedaempftes Rosa

// Dark-Gradient-Stops (gleiche Stimmung, Nachthimmel-Toene)
val AuroraDarkGradientStart = Color(0xFF1A2B2F)    // Tiefblaugruen
val AuroraDarkGradientMid = Color(0xFF2A1F3B)      // gedaempftes Indigo
val AuroraDarkGradientEnd = Color(0xFF3B1F2E)      // dunkles Bordeaux

// Akzente (gemeinsam fuer Light + Dark, leicht variierte Helligkeit)
val AuroraEmeraldLight = Color(0xFF1F8E5A)         // Primary Light — Smaragd
val AuroraEmeraldDark = Color(0xFF7DD3A4)          // Primary Dark — helleres Mint
val AuroraLavenderLight = Color(0xFF7A6BB8)        // Secondary Light — Lavendel-Lila
val AuroraLavenderDark = Color(0xFFB5A8E8)         // Secondary Dark — weicher Lavendel
val AuroraHoneyLight = Color(0xFFE8B547)           // Tertiary Light — Honiggelb (Timeline-Knoten)
val AuroraHoneyDark = Color(0xFFF0C76A)            // Tertiary Dark — warmes Honig

// Cards + Text Light
val AuroraCardLight = Color(0xFFFFFFFF)            // weiss deckend (Cards heben sich vom Gradient ab)
val AuroraCardElevatedLight = Color(0xFFF8F9FB)    // minimal grau-weisser Schimmer
val AuroraTextPrimaryLight = Color(0xFF1F2733)     // tiefes Anthrazit
val AuroraTextSecondaryLight = Color(0xFF5C6470)   // warmes Grau
val AuroraOutlineLight = Color(0xFFD8DBE2)         // sanfter Kartenrand

// Cards + Text Dark
val AuroraCardDark = Color(0xFF2C2435)             // glasiges dunkles Lila
val AuroraCardElevatedDark = Color(0xFF382E45)     // erhoehte Variante
val AuroraTextPrimaryDark = Color(0xFFE8EBF2)      // sanftes Creme
val AuroraTextSecondaryDark = Color(0xFFA8AEB8)    // gedaempftes Beige-Grau
val AuroraOutlineDark = Color(0xFF4A3F58)          // dezenter Lila-Rand
