package com.entropyjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
 * Bekommt einen Lucide-Icon-Key (siehe [LucideIconPool]) und zeigt das passende
 * Symbol mit Tag/Nacht-Farbpalette an. Der Kreis hat einen subtilen Rand-Glow
 * und einen weichen radialen Hintergrund-Verlauf.
 *
 * @param iconKey Schluessel aus [LucideIconPool] (z.B. "sun", "mountain")
 * @param size Aussendurchmesser des Kreises (Standard 36dp)
 */
@Composable
fun CircleIconBadge(
    iconKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val isDark = LocalIsDarkTheme.current
    val icon = LucideIconPool.get(iconKey)

    // Tag/Nacht-Farbpaletten — passend zum Frank-Theme
    val borderColor = if (isDark) Color(0xFF00B0D4) else Color(0xFF006B7A)
    val fillStart = if (isDark) Color(0xFF0D2A33) else Color(0xFFE3F4F7)
    val fillEnd = if (isDark) Color(0xFF062028) else Color(0xFFD1ECF1)
    val iconTint = if (isDark) Color(0xFFB3E5FC) else Color(0xFF006B7A)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(fillStart, fillEnd),
                )
            )
            .border(
                width = 1.5.dp,
                color = borderColor.copy(alpha = if (isDark) 0.85f else 0.55f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
