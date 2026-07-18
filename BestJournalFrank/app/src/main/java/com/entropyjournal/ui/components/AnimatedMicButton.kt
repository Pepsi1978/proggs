package com.entropyjournal.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.LocalJournalDesignTokens

@Composable
fun AnimatedMicButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val designTokens = LocalJournalDesignTokens.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val transition = rememberInfiniteTransition(label = "goldenMic")
    val breathScale by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec =
                infiniteRepeatable(
                    tween(1500, easing = EaseInOutSine),
                    RepeatMode.Reverse,
                ),
            label = "micBreathe",
        )
    val ringRotation by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
            label = "micRing",
        )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(76.dp).scale(if (isRecording) 1f else breathScale),
    ) {
        if (isRecording) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    brush =
                        Brush.sweepGradient(
                            listOf(
                                primary,
                                secondary,
                                tertiary,
                                primary,
                            )
                        ),
                    startAngle = ringRotation,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }

        Box(
            modifier =
                Modifier.size(64.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) {
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
                            )
                        } else {
                            designTokens.accentBrush
                        }
                    )
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = if (isRecording) "Aufnahme stoppen" else "Aufnahme starten",
                tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
