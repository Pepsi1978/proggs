package com.entropyjournal.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.ui.theme.LocalJournalDesignTokens
import com.entropyjournal.util.DateTimeFormatter

@Composable
fun RecordingOverlay(
    amplitude: Float,
    durationSeconds: Int,
    transcriptionModel: String = "",
    modifier: Modifier = Modifier
) {
    val designTokens = LocalJournalDesignTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 112.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(designTokens.navigationBackground)
            .border(
                1.dp,
                designTokens.chipBorder,
                RoundedCornerShape(26.dp),
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = DateTimeFormatter.formatDuration(durationSeconds),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        GoldenWaveform(amplitude = amplitude)
        Spacer(modifier = Modifier.height(10.dp))
        if (transcriptionModel.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonRed)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = transcriptionModel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = "Tippe auf Stopp zum Beenden",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GoldenWaveform(amplitude: Float) {
    val designTokens = LocalJournalDesignTokens.current
    val transition = rememberInfiniteTransition(label = "gold_wave")
    val phase by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "gold_wave_phase",
        )
    Canvas(modifier = Modifier.width(61.dp).height(40.dp)) {
        val barWidth = 5.dp.toPx()
        val gap = 3.dp.toPx()
        repeat(8) { index ->
            val oscillation =
                ((kotlin.math.sin((phase + index * 0.9f).toDouble()) + 1.0) / 2.0).toFloat()
            val signal = (amplitude * 2.5f).coerceIn(0.15f, 1f)
            val barHeight = size.height * (0.28f + 0.72f * oscillation * signal)
            val x = index * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            drawRoundRect(
                brush = designTokens.accentBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
