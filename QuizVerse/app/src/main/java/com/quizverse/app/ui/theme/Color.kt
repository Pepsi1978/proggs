package com.quizverse.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Brand colors ────────────────────────────────────────────────────────────
val Primary   = Color(0xFF6C63FF) // Violet
val Secondary = Color(0xFFFF6584) // Coral
val Accent    = Color(0xFF43E97B) // Emerald Green

// ── Category gradient pairs (start → end) ───────────────────────────────────
val GeoStart        = Color(0xFF4FACFE)
val GeoEnd          = Color(0xFF00F2FE)

val ScienceStart    = Color(0xFF6C63FF)
val ScienceEnd      = Color(0xFFA855F7)

val HistoryStart    = Color(0xFFF97316)
val HistoryEnd      = Color(0xFFFBBF24)

val FilmStart       = Color(0xFFEC4899)
val FilmEnd         = Color(0xFFF43F5E)

val MusicStart      = Color(0xFF8B5CF6)
val MusicEnd        = Color(0xFFC084FC)

val SportStart      = Color(0xFF22C55E)
val SportEnd        = Color(0xFF10B981)

val TechStart       = Color(0xFF3B82F6)
val TechEnd         = Color(0xFF6366F1)

val FoodStart       = Color(0xFFF59E0B)
val FoodEnd         = Color(0xFFEF4444)

val AnimalsStart    = Color(0xFF14B8A6)
val AnimalsEnd      = Color(0xFF06B6D4)

val LiteratureStart = Color(0xFF8B5CF6)
val LiteratureEnd   = Color(0xFFEC4899)

val MixedStart      = Color(0xFFF43F5E)
val MixedEnd        = Color(0xFFF97316)

val LogicStart      = Color(0xFF0EA5E9)
val LogicEnd        = Color(0xFF6366F1)

// Bundesliga subcategories
val HerthaStart     = Color(0xFF1A5276)
val HerthaEnd       = Color(0xFF2E86C1)

val DortmundStart   = Color(0xFFFDD835)
val DortmundEnd     = Color(0xFF212121)

// ── Surface / neutral tokens ─────────────────────────────────────────────────
// Light theme surfaces
val SurfaceLight         = Color(0xFFFAF9FF)
val SurfaceVariantLight  = Color(0xFFE7E0EC)
val BackgroundLight      = Color(0xFFFFFBFE)
val OnSurfaceLight       = Color(0xFF1C1B1F)
val OnSurfaceVariantLight= Color(0xFF49454F)
val OutlineLight         = Color(0xFF79747E)

// Dark theme surfaces
val SurfaceDark          = Color(0xFF1C1B1F)
val SurfaceVariantDark   = Color(0xFF49454F)
val BackgroundDark       = Color(0xFF1C1B1F)
val OnSurfaceDark        = Color(0xFFE6E1E5)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val OutlineDark          = Color(0xFF938F99)

// On-brand colors
val OnPrimary   = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)
val OnAccent    = Color(0xFF003319)

// Error
val ErrorColor    = Color(0xFFB3261E)
val OnErrorColor  = Color(0xFFFFFFFF)
val ErrorContainer    = Color(0xFFF9DEDC)
val OnErrorContainer  = Color(0xFF410E0B)

// ── Color schemes ────────────────────────────────────────────────────────────
val LightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = OnPrimary,
    primaryContainer     = Color(0xFFE8E4FF),
    onPrimaryContainer   = Color(0xFF1A0062),
    secondary            = Secondary,
    onSecondary          = OnSecondary,
    secondaryContainer   = Color(0xFFFFD9E1),
    onSecondaryContainer = Color(0xFF3E001D),
    tertiary             = Accent,
    onTertiary           = OnAccent,
    tertiaryContainer    = Color(0xFFB8F5D1),
    onTertiaryContainer  = Color(0xFF002114),
    error                = ErrorColor,
    onError              = OnErrorColor,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    background           = BackgroundLight,
    onBackground         = OnSurfaceLight,
    surface              = SurfaceLight,
    onSurface            = OnSurfaceLight,
    surfaceVariant       = SurfaceVariantLight,
    onSurfaceVariant     = OnSurfaceVariantLight,
    outline              = OutlineLight,
)

val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFFCBBEFF),
    onPrimary            = Color(0xFF33009A),
    primaryContainer     = Color(0xFF4B00D6),
    onPrimaryContainer   = Color(0xFFE8E4FF),
    secondary            = Color(0xFFFFB1C2),
    onSecondary          = Color(0xFF67001F),
    secondaryContainer   = Color(0xFF8E002F),
    onSecondaryContainer = Color(0xFFFFD9E1),
    tertiary             = Color(0xFF4EDA8D),
    onTertiary           = Color(0xFF003820),
    tertiaryContainer    = Color(0xFF005230),
    onTertiaryContainer  = Color(0xFFB8F5D1),
    error                = Color(0xFFF2B8B5),
    onError              = Color(0xFF601410),
    errorContainer       = Color(0xFF8C1D18),
    onErrorContainer     = Color(0xFFF9DEDC),
    background           = BackgroundDark,
    onBackground         = OnSurfaceDark,
    surface              = SurfaceDark,
    onSurface            = OnSurfaceDark,
    surfaceVariant       = SurfaceVariantDark,
    onSurfaceVariant     = OnSurfaceVariantDark,
    outline              = OutlineDark,
)
