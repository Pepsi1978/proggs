package com.bestjournal.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonViolet
import kotlinx.coroutines.delay

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 50
) {
    val currentAmplitude by rememberUpdatedState(amplitude)
    val history = remember {
        mutableStateListOf<Float>().apply { repeat(barCount) { add(0f) } }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(80)
            history.removeAt(0)
            history.add(currentAmplitude)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val maxBarHeight = size.height * 0.9f
        val minBarHeight = 3.dp.toPx()

        for (i in history.indices) {
            val amp = history[i].coerceIn(0f, 1f)
            val boosted = (amp * 3f).coerceAtMost(1f)
            val scaled = kotlin.math.sqrt(boosted.toDouble()).toFloat()
            val barHeight = (maxBarHeight * scaled).coerceAtLeast(minBarHeight)

            val x = i * (barWidth + gap)
            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(NeonCyan, NeonViolet),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
                alpha = 0.5f + scaled * 0.5f
            )
        }
    }
}
