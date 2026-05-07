package de.frank.entropyreducer.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Mehrfarbiger Status-Balken "Zustand jetzt" gemaess Spec §4.1 und Referenzbildern.
 * Gradient: 0-25% Rot, 25-50% Gelb, 50-75% Hellgruen, 75-100% Dunkelgruen.
 */
@Composable
fun StatusBar(
    modifier: Modifier = Modifier,
    percent: Int,
    label: String = "Zustand jetzt",
    onClick: (() -> Unit)? = null,
) {
    val animated by animateFloatAsState(
        targetValue = (percent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 600),
        label = "statusBar",
    )
    val cosmos = LocalCosmos.current

    val gradient = Brush.horizontalGradient(
        0f to CosmosColors.StatusRed,
        0.33f to CosmosColors.StatusYellow,
        0.66f to CosmosColors.StatusLightGreen,
        1f to CosmosColors.StatusGreen,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textSecondary,
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(cosmos.glassBg),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(50))
                    .background(gradient),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$percent %",
            color = CosmosColors.AccentPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

/** Variante als Ring fuer Recovery-Score (Dashboard 4). */
@Composable
fun RingPlaceholder(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(cosmos.glassBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$score",
            color = CosmosColors.Success,
            style = MaterialTheme.typography.displayLarge,
        )
    }
}
