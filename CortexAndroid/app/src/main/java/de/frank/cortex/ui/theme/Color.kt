package de.frank.cortex.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Dark Theme =====
val DarkBg = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF141A23)
val DarkRaised = Color(0xFF1A2230)
val DarkBorder = Color(0xFF222C3A)
val DarkBorderSoft = Color(0xFF1B2430)
val DarkText = Color(0xFFE8EEF6)
val DarkMuted = Color(0xFF8A98AC)
val DarkFaint = Color(0xFF5B687C)
val DarkField = Color(0xFF0F141C)
val DarkFieldBorder = Color(0xFF26303F)
val DarkChip = Color(0x0DFFFFFF) // rgba(255,255,255,.05)
val DarkChipBorder = Color(0xFF27313F)
val DarkUserBubble = Color(0x337B7BF5) // rgba(123,123,245,.20)
val DarkUserBorder = Color(0x577B7BF5) // rgba(123,123,245,.34)

// ===== Light Theme =====
val LightBg = Color(0xFFF4F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightRaised = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE4E8F1)
val LightBorderSoft = Color(0xFFEDF0F6)
val LightText = Color(0xFF1A2230)
val LightMuted = Color(0xFF5C6B82)
val LightFaint = Color(0xFF9AA7BC)
val LightField = Color(0xFFF7F9FD)
val LightFieldBorder = Color(0xFFE1E6F0)
val LightChip = Color(0xFFF2F4FA)
val LightChipBorder = Color(0xFFE4E8F1)
val LightUserBubble = Color(0x1E6363E6) // rgba(99,99,230,.12)
val LightUserBorder = Color(0x426363E6) // rgba(99,99,230,.26)

// ===== Akzente (shared, exact from design) =====
val Iris = Color(0xFF7B7BF5)
val IrisLight = Color(0xFF6363E6)
val Mint = Color(0xFF4FD1B0)
val MintLight = Color(0xFF1FA98A)
val Amber = Color(0xFFF2B65A)
val AmberLight = Color(0xFFD99327)
val Rose = Color(0xFFF2698E)
val RoseLight = Color(0xFFE14C72)
val Orange = Color(0xFFF97316)
val OrangeLight = Color(0xFFF2710C)

// ===== Kategorie-Palette (13, exakt aus Design) =====
val CategoryColors = listOf(
    Color(0xFF7B7BF5), // Iris
    Color(0xFF4FD1B0), // Mint
    Color(0xFFF2B65A), // Amber
    Color(0xFFF2698E), // Rose
    Color(0xFF5AB0F2), // Sky
    Color(0xFFB68CF5), // Lavender
    Color(0xFF3FD0D6), // Teal
    Color(0xFF9BD05A), // Lime
    Color(0xFFF08A5A), // Tangerine
    Color(0xFF5AD0A0), // Emerald
    Color(0xFFF25AC0), // Pink
    Color(0xFF5A7BF2), // Blue
    Color(0xFFD0C45A)  // Gold
)

fun categoryColor(name: String): Color {
    // floorMod statt Negieren: -Int.MIN_VALUE bleibt negativ (Int-Overflow) und haette einen
    // negativen Index (Crash) ergeben. floorMod liefert fuer JEDEN Hash garantiert 0..size-1.
    val index = Math.floorMod(name.hashCode(), CategoryColors.size)
    return CategoryColors[index]
}
