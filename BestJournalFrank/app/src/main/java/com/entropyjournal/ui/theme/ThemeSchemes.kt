package com.entropyjournal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════════════════
// 5 Klassiker-Themes (Light + Dark) — Farben aus den offiziellen Specs:
// Solarized (Ethan Schoonover, 2011), Dracula (Zeno Rocha, 2013), One Dark/Light
// (Atom 2014), Nord (Arctic Ice Studio, 2016), Gruvbox (morhetz, 2016).
// Jedes Theme: vollstaendiges Material3 ColorScheme mit Background/Surface aus
// dem Original-Theme + Akzent fuer primary/secondary/tertiary.
// ════════════════════════════════════════════════════════════════════════════

// ─── Solarized ──────────────────────────────────────────────────────────────
val SolarizedDarkScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF268BD2), // blue
        onPrimary = Color.White,
        primaryContainer = Color(0xFF073642), // base02
        onPrimaryContainer = Color(0xFF93A1A1),
        secondary = Color(0xFF2AA198), // cyan
        onSecondary = Color(0xFF002B36),
        secondaryContainer = Color(0xFF073642),
        onSecondaryContainer = Color(0xFF93A1A1),
        tertiary = Color(0xFFB58900), // yellow
        onTertiary = Color(0xFF002B36),
        tertiaryContainer = Color(0xFF073642),
        onTertiaryContainer = Color(0xFFEEE8D5),
        error = Color(0xFFDC322F),
        onError = Color.White,
        errorContainer = Color(0xFF3F1010),
        onErrorContainer = Color(0xFFFFCDD2),
        background = Color(0xFF002B36), // base03
        onBackground = Color(0xFF839496), // base0
        surface = Color(0xFF073642), // base02
        onSurface = Color(0xFF93A1A1), // base1
        surfaceVariant = Color(0xFF002B36),
        onSurfaceVariant = Color(0xFF657B83), // base00
        outline = Color(0xFF586E75), // base01
        outlineVariant = Color(0xFF073642),
        surfaceTint = Color(0xFF268BD2),
    )

val SolarizedLightScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF268BD2),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEEE8D5), // base2
        onPrimaryContainer = Color(0xFF073642),
        secondary = Color(0xFF2AA198),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEEE8D5),
        onSecondaryContainer = Color(0xFF073642),
        tertiary = Color(0xFFB58900),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFEEE8D5),
        onTertiaryContainer = Color(0xFF073642),
        error = Color(0xFFDC322F),
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color(0xFFB71C1C),
        background = Color(0xFFFDF6E3), // base3
        onBackground = Color(0xFF657B83), // base00
        surface = Color(0xFFEEE8D5), // base2
        onSurface = Color(0xFF586E75), // base01
        surfaceVariant = Color(0xFFFDF6E3),
        onSurfaceVariant = Color(0xFF93A1A1),
        outline = Color(0xFF93A1A1),
        outlineVariant = Color(0xFFEEE8D5),
        surfaceTint = Color(0xFF268BD2),
    )

// ─── Dracula / Alucard (Light) ──────────────────────────────────────────────
val DraculaDarkScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFBD93F9), // purple
        onPrimary = Color(0xFF282A36),
        primaryContainer = Color(0xFF44475A), // current line
        onPrimaryContainer = Color(0xFFF8F8F2),
        secondary = Color(0xFFFF79C6), // pink
        onSecondary = Color(0xFF282A36),
        secondaryContainer = Color(0xFF44475A),
        onSecondaryContainer = Color(0xFFF8F8F2),
        tertiary = Color(0xFF8BE9FD), // cyan
        onTertiary = Color(0xFF282A36),
        tertiaryContainer = Color(0xFF44475A),
        onTertiaryContainer = Color(0xFFF8F8F2),
        error = Color(0xFFFF5555),
        onError = Color.White,
        errorContainer = Color(0xFF3F1010),
        onErrorContainer = Color(0xFFFFCDD2),
        background = Color(0xFF282A36),
        onBackground = Color(0xFFF8F8F2),
        surface = Color(0xFF343746),
        onSurface = Color(0xFFF8F8F2),
        surfaceVariant = Color(0xFF44475A),
        onSurfaceVariant = Color(0xFF6272A4), // comment
        outline = Color(0xFF6272A4),
        outlineVariant = Color(0xFF44475A),
        surfaceTint = Color(0xFFBD93F9),
    )

val DraculaLightScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF644AC9), // alucard purple
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5DDF8),
        onPrimaryContainer = Color(0xFF1F1F1F),
        secondary = Color(0xFFCB3A87), // alucard pink
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE3F0),
        onSecondaryContainer = Color(0xFF1F1F1F),
        tertiary = Color(0xFF036A96), // alucard cyan
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE0F2FA),
        onTertiaryContainer = Color(0xFF1F1F1F),
        error = Color(0xFFCB3A38),
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color(0xFFB71C1C),
        background = Color(0xFFFFFBEB), // alucard bg
        onBackground = Color(0xFF1F1F1F),
        surface = Color(0xFFFAF5DC),
        onSurface = Color(0xFF1F1F1F),
        surfaceVariant = Color(0xFFF0EBD2),
        onSurfaceVariant = Color(0xFF635D52),
        outline = Color(0xFFA08F6E),
        outlineVariant = Color(0xFFE5DFC3),
        surfaceTint = Color(0xFF644AC9),
    )

// ─── One Dark / One Light (Atom) ────────────────────────────────────────────
val OneDarkScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF61AFEF), // blue
        onPrimary = Color(0xFF282C34),
        primaryContainer = Color(0xFF3E4451),
        onPrimaryContainer = Color(0xFFABB2BF),
        secondary = Color(0xFFC678DD), // purple
        onSecondary = Color(0xFF282C34),
        secondaryContainer = Color(0xFF3E4451),
        onSecondaryContainer = Color(0xFFABB2BF),
        tertiary = Color(0xFF98C379), // green
        onTertiary = Color(0xFF282C34),
        tertiaryContainer = Color(0xFF3E4451),
        onTertiaryContainer = Color(0xFFABB2BF),
        error = Color(0xFFE06C75),
        onError = Color.White,
        errorContainer = Color(0xFF3F1010),
        onErrorContainer = Color(0xFFFFCDD2),
        background = Color(0xFF282C34),
        onBackground = Color(0xFFABB2BF),
        surface = Color(0xFF21252B),
        onSurface = Color(0xFFABB2BF),
        surfaceVariant = Color(0xFF3E4451),
        onSurfaceVariant = Color(0xFF5C6370), // comment
        outline = Color(0xFF5C6370),
        outlineVariant = Color(0xFF3E4451),
        surfaceTint = Color(0xFF61AFEF),
    )

val OneLightScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF4078F2), // blue
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE5EAF5),
        onPrimaryContainer = Color(0xFF383A42),
        secondary = Color(0xFFA626A4), // purple
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF5E5F4),
        onSecondaryContainer = Color(0xFF383A42),
        tertiary = Color(0xFF50A14F), // green
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE7F5E5),
        onTertiaryContainer = Color(0xFF383A42),
        error = Color(0xFFE45649),
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color(0xFFB71C1C),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF383A42),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF383A42),
        surfaceVariant = Color(0xFFEAEAEB),
        onSurfaceVariant = Color(0xFFA0A1A7), // comment
        outline = Color(0xFFA0A1A7),
        outlineVariant = Color(0xFFEAEAEB),
        surfaceTint = Color(0xFF4078F2),
    )

// ─── Nord ───────────────────────────────────────────────────────────────────
val NordDarkScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF88C0D0), // frost blue
        onPrimary = Color(0xFF2E3440),
        primaryContainer = Color(0xFF3B4252),
        onPrimaryContainer = Color(0xFFD8DEE9),
        secondary = Color(0xFF81A1C1), // frost mid blue
        onSecondary = Color(0xFF2E3440),
        secondaryContainer = Color(0xFF3B4252),
        onSecondaryContainer = Color(0xFFD8DEE9),
        tertiary = Color(0xFFA3BE8C), // aurora green
        onTertiary = Color(0xFF2E3440),
        tertiaryContainer = Color(0xFF3B4252),
        onTertiaryContainer = Color(0xFFD8DEE9),
        error = Color(0xFFBF616A),
        onError = Color.White,
        errorContainer = Color(0xFF3F1010),
        onErrorContainer = Color(0xFFFFCDD2),
        background = Color(0xFF2E3440), // polar night nord0
        onBackground = Color(0xFFD8DEE9), // snow nord4
        surface = Color(0xFF3B4252), // polar night nord1
        onSurface = Color(0xFFE5E9F0), // snow nord5
        surfaceVariant = Color(0xFF434C5E), // polar night nord2
        onSurfaceVariant = Color(0xFF8FBCBB), // frost cyan
        outline = Color(0xFF4C566A), // polar night nord3
        outlineVariant = Color(0xFF3B4252),
        surfaceTint = Color(0xFF88C0D0),
    )

val NordLightScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF5E81AC), // frost dark blue
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8DEE9),
        onPrimaryContainer = Color(0xFF2E3440),
        secondary = Color(0xFF81A1C1),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD8DEE9),
        onSecondaryContainer = Color(0xFF2E3440),
        tertiary = Color(0xFFA3BE8C),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE5E9F0),
        onTertiaryContainer = Color(0xFF2E3440),
        error = Color(0xFFBF616A),
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color(0xFFB71C1C),
        background = Color(0xFFECEFF4), // snow nord6
        onBackground = Color(0xFF2E3440),
        surface = Color(0xFFE5E9F0), // snow nord5
        onSurface = Color(0xFF2E3440),
        surfaceVariant = Color(0xFFD8DEE9), // snow nord4
        onSurfaceVariant = Color(0xFF4C566A),
        outline = Color(0xFF4C566A),
        outlineVariant = Color(0xFFD8DEE9),
        surfaceTint = Color(0xFF5E81AC),
    )

// ─── Gruvbox ────────────────────────────────────────────────────────────────
val GruvboxDarkScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFD79921), // yellow-orange
        onPrimary = Color(0xFF282828),
        primaryContainer = Color(0xFF504945),
        onPrimaryContainer = Color(0xFFEBDBB2),
        secondary = Color(0xFF98971A), // green
        onSecondary = Color(0xFF282828),
        secondaryContainer = Color(0xFF504945),
        onSecondaryContainer = Color(0xFFEBDBB2),
        tertiary = Color(0xFFD65D0E), // orange
        onTertiary = Color(0xFF282828),
        tertiaryContainer = Color(0xFF504945),
        onTertiaryContainer = Color(0xFFEBDBB2),
        error = Color(0xFFCC241D),
        onError = Color.White,
        errorContainer = Color(0xFF3F1010),
        onErrorContainer = Color(0xFFFFCDD2),
        background = Color(0xFF282828), // bg
        onBackground = Color(0xFFEBDBB2), // fg
        surface = Color(0xFF32302F),
        onSurface = Color(0xFFEBDBB2),
        surfaceVariant = Color(0xFF3C3836),
        onSurfaceVariant = Color(0xFFA89984), // gray
        outline = Color(0xFF7C6F64),
        outlineVariant = Color(0xFF504945),
        surfaceTint = Color(0xFFD79921),
    )

val GruvboxLightScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFFB57614), // dark yellow
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEBDBB2),
        onPrimaryContainer = Color(0xFF3C3836),
        secondary = Color(0xFF79740E), // dark green
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEBDBB2),
        onSecondaryContainer = Color(0xFF3C3836),
        tertiary = Color(0xFFAF3A03), // dark orange
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFEBDBB2),
        onTertiaryContainer = Color(0xFF3C3836),
        error = Color(0xFF9D0006),
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color(0xFFB71C1C),
        background = Color(0xFFFBF1C7), // gruvbox light bg
        onBackground = Color(0xFF3C3836), // fg
        surface = Color(0xFFF2E5BC),
        onSurface = Color(0xFF3C3836),
        surfaceVariant = Color(0xFFEBDBB2),
        onSurfaceVariant = Color(0xFF7C6F64),
        outline = Color(0xFFA89984),
        outlineVariant = Color(0xFFEBDBB2),
        surfaceTint = Color(0xFFB57614),
    )

// Cosmos — "Neon Cosmos" 1:1 von ~/proggs/EntropieReductor portiert.
//
// In EntropieReductor sind die Cards/Boxen eigene GlassCard-Composables die ein
// weisses Overlay mit 8% Alpha auf dem dunklen Hintergrund ablegen. BestJournalFrank
// verwendet Material3-Cards — daher setzen wir surface, surfaceVariant und alle
// 5 surfaceContainer-Slots auf die mathematisch berechnete Glas-Mix-Farbe.
// Resultat: Material3-Cards sehen visuell IDENTISCH aus wie EntropieReductor's GlassCards.
//
// surfaceTint wird absichtlich auf surface gesetzt (statt primary), damit Material3
// keine Cyan-Tints auf hoehere Elevations legt — das wuerde den Glas-Look ruinieren.
val CosmosDarkScheme: ColorScheme =
    darkColorScheme(
        primary = ERAccentPrimary,
        onPrimary = ERBgDark,
        secondary = ERAccentSecondary,
        onSecondary = ERBgDark,
        tertiary = ERSuccess,
        background = ERBgDark,
        onBackground = ERTextPrimaryDark,
        surface = ERBgDarkGlass,
        onSurface = ERTextPrimaryDark,
        surfaceVariant = ERBgDarkGlassElevated,
        onSurfaceVariant = ERTextSecondaryDark,
        surfaceContainerLowest = ERBgDark,
        surfaceContainerLow = ERBgDarkGlass,
        surfaceContainer = ERBgDarkGlass,
        surfaceContainerHigh = ERBgDarkGlassElevated,
        surfaceContainerHighest = ERBgDarkGlassElevated,
        error = ERCritical,
        onError = ERTextPrimaryDark,
        surfaceTint = ERBgDarkGlass,
        // outline wird in BottomNavBar.kt fuer inaktive Icons verwendet — daher
        // muss die Farbe deckend sein (nicht alpha-transparent), sonst sind die
        // Buttons unten unleserlich. outlineVariant bleibt transparent fuer den
        // Glas-Border-Schimmer auf Karten.
        outline = ERTextSecondaryDark,
        outlineVariant = ERGlassDarkBorder,
    )

val CosmosLightScheme: ColorScheme =
    lightColorScheme(
        primary = ERAccentPrimary,
        onPrimary = ERBgDark,
        secondary = ERAccentSecondary,
        onSecondary = ERBgDark,
        tertiary = ERSuccess,
        background = ERBgLight,
        onBackground = ERTextPrimaryLight,
        surface = ERBgLightGlass,
        onSurface = ERTextPrimaryLight,
        surfaceVariant = ERBgLightGlassElevated,
        onSurfaceVariant = ERTextSecondaryLight,
        surfaceContainerLowest = ERBgLight,
        surfaceContainerLow = ERBgLightGlass,
        surfaceContainer = ERBgLightGlass,
        surfaceContainerHigh = ERBgLightGlassElevated,
        surfaceContainerHighest = ERBgLightGlassElevated,
        error = ERCritical,
        onError = ERTextPrimaryDark,
        surfaceTint = ERBgLightGlass,
        // outline wird in BottomNavBar.kt fuer inaktive Icons verwendet.
        // Frank-Justierung 2026-05-08: erst war outline #14000000 (transparent → unsichtbar),
        // dann ERTextPrimaryLight #0F172A (zu schwarz), jetzt #374151 (dunkles Grau,
        // gut lesbar auf weiss aber nicht zu hart). outlineVariant bleibt transparent
        // fuer den feinen Card-Border-Schimmer.
        outline = Color(0xFF374151),
        outlineVariant = ERGlassLightBorder,
    )

// ─── Aurora ─────────────────────────────────────────────────────────────────
// Pastel-Aquarell-Theme — Hintergrund kommt aus einem Vollbild-Brush in Theme.kt,
// background/surface verhalten sich aehnlich wie Cosmos (deckend-weiss bzw. dunkel,
// aber der Brush ueberlagert sie als Box-Background).
val AuroraLightScheme: ColorScheme =
    lightColorScheme(
        primary = AuroraEmeraldLight,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFCDEEDD),       // hellgruener Container
        onPrimaryContainer = Color(0xFF0F4029),
        secondary = AuroraLavenderLight,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE6E0F5),     // hellvioletter Container
        onSecondaryContainer = Color(0xFF2D2455),
        tertiary = AuroraHoneyLight,
        onTertiary = Color(0xFF3D2A00),
        tertiaryContainer = Color(0xFFFAEBC8),
        onTertiaryContainer = Color(0xFF4A3300),
        error = Color(0xFFD96A6A),
        onError = Color.White,
        errorContainer = Color(0xFFFFE3E3),
        onErrorContainer = Color(0xFF7A1F1F),
        // background wird vom Aurora-Brush in Theme.kt komplett ueberdeckt — wir
        // setzen aber trotzdem die Mid-Stop-Farbe, damit beim allerersten Frame
        // (bevor der Brush rendert) kein Flash entsteht.
        background = AuroraLightGradientMid,
        onBackground = AuroraTextPrimaryLight,
        surface = AuroraCardLight,
        onSurface = AuroraTextPrimaryLight,
        surfaceVariant = AuroraCardElevatedLight,
        onSurfaceVariant = AuroraTextSecondaryLight,
        surfaceContainerLowest = AuroraCardLight,
        surfaceContainerLow = AuroraCardLight,
        surfaceContainer = AuroraCardLight,
        surfaceContainerHigh = AuroraCardElevatedLight,
        surfaceContainerHighest = AuroraCardElevatedLight,
        outline = AuroraOutlineLight,
        outlineVariant = Color(0xFFEAECEF),
        surfaceTint = AuroraCardLight,
    )

val AuroraDarkScheme: ColorScheme =
    darkColorScheme(
        primary = AuroraEmeraldDark,
        onPrimary = Color(0xFF0F1F18),
        primaryContainer = Color(0xFF1F4D38),
        onPrimaryContainer = Color(0xFFCDEEDD),
        secondary = AuroraLavenderDark,
        onSecondary = Color(0xFF1B1429),
        secondaryContainer = Color(0xFF3D3157),
        onSecondaryContainer = Color(0xFFE6E0F5),
        tertiary = AuroraHoneyDark,
        onTertiary = Color(0xFF2C1F00),
        tertiaryContainer = Color(0xFF54421A),
        onTertiaryContainer = Color(0xFFFAEBC8),
        error = Color(0xFFE89898),
        onError = Color(0xFF2F0F0F),
        errorContainer = Color(0xFF5A2424),
        onErrorContainer = Color(0xFFFFE3E3),
        background = AuroraDarkGradientMid,
        onBackground = AuroraTextPrimaryDark,
        surface = AuroraCardDark,
        onSurface = AuroraTextPrimaryDark,
        surfaceVariant = AuroraCardElevatedDark,
        onSurfaceVariant = AuroraTextSecondaryDark,
        surfaceContainerLowest = AuroraCardDark,
        surfaceContainerLow = AuroraCardDark,
        surfaceContainer = AuroraCardDark,
        surfaceContainerHigh = AuroraCardElevatedDark,
        surfaceContainerHighest = AuroraCardElevatedDark,
        outline = AuroraOutlineDark,
        outlineVariant = Color(0x33B5A8E8),
        surfaceTint = AuroraCardDark,
    )
