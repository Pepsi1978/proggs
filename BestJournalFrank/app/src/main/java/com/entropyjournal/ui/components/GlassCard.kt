package com.entropyjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import com.entropyjournal.ui.theme.LocalJournalDesignTokens

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    glowIntensity: Float = 0.15f,
    cornerRadius: Dp = 18.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val tokens = LocalJournalDesignTokens.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 14.dp else 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.32f else 0.08f),
                spotColor = glowColor.copy(alpha = glowIntensity.coerceIn(0f, 0.35f)),
            )
            .clip(shape)
            .background(tokens.cardBrush)
            .border(1.dp, tokens.cardBorder, shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
