package com.entropyjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.LocalIsDarkTheme

/**
 * Kreisfoermiges Symbol-Badge fuer die Timeline-Schiene.
 * Bekommt einen Lucide-Icon-Key (siehe [LucideIconPool]) und nutzt die im Pool
 * hinterlegten Tag/Nacht-Farben des jeweiligen Symbols. So bekommt jedes Symbol
 * seine eigene thematische Farbe (Sonne gelb, Wasser blau, Familie pink usw.).
 *
 * @param iconKey Schluessel aus [LucideIconPool] (z.B. "sun", "glass_water")
 * @param size Aussendurchmesser des Kreises (Standard 36dp)
 */
@Composable
fun CircleIconBadge(
    iconKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val isDark = LocalIsDarkTheme.current
    val entry = LucideIconPool.get(iconKey)
    val icon = entry?.icon ?: LucideIconPool.getIcon(LucideIconPool.fallbackKey)

    val baseTint = if (entry != null) {
        if (isDark) entry.tintDark else entry.tintLight
    } else {
        if (isDark) Color(0xFF80DEEA) else Color(0xFF006064)
    }

    // Deckender Hintergrund — App-Background, damit die Timeline-Linie nicht durchschimmert.
    // Darueber ein dezenter Tint-Wash, der dem Badge die Kategorie-Farbe gibt.
    val solidBackground = MaterialTheme.colorScheme.background
    val washColor = if (isDark) baseTint.copy(alpha = 0.22f) else baseTint.copy(alpha = 0.14f)
    val borderColor = baseTint.copy(alpha = if (isDark) 0.85f else 0.55f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(solidBackground)            // Schicht 1: deckender App-Hintergrund
            .background(washColor)                  // Schicht 2: kategoriespezifischer Wash
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = baseTint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
