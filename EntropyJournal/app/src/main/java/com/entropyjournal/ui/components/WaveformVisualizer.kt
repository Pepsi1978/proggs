package com.entropyjournal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonViolet
import kotlinx.coroutines.delay

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 40
) {
    val currentAmplitude by rememberUpdatedState(amplitude)
    val history = remember { FloatArray(barCount) }
    val version = remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Push new amplitude sample every 80ms — scrolls bars left
    LaunchedEffect(Unit) {
        while (true) {
            delay(80)
            System.arraycopy(history, 1, history, 0, history.size - 1)
            history[history.size - 1] = currentAmplitude
            version.intValue++
        }
    }

    // Read version to trigger recomposition
    @Suppress("UNUSED_VARIABLE")
    val tick = version.intValue

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val maxBarHeight = size.height * 0.9f
        val minBarHeight = 3.dp.toPx()

        for (i in 0 until barCount) {
            val amp = history[i].coerceIn(0f, 1f)
            // Scale amplitude for better visibility (boost low values)
            val scaled = kotlin.math.sqrt(amp.toDouble()).toFloat()
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
