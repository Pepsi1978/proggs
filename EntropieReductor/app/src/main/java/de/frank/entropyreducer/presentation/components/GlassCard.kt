package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Wiederverwendbarer Glas-Container — entspricht dem Design der Referenzbilder.
 * - Dark: weisses Overlay alpha 0.04, weisser Border alpha 0.08
 * - Light: weiss alpha 0.80, schwarzer Border alpha 0.08
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val cosmos = LocalCosmos.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(cosmos.glassBg)
            .border(BorderStroke(1.dp, cosmos.glassBorder), RoundedCornerShape(cornerRadius))
            .padding(contentPadding),
    ) {
        content()
    }
}
