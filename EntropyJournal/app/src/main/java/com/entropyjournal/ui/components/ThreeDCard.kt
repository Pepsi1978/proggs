package com.entropyjournal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.entropyjournal.ui.theme.NeonCyan

@Composable
fun ThreeDCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }

    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "rotationY"
    )

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        rotationY = ((change.position.x - centerX) / centerX * 8f)
                            .coerceIn(-8f, 8f)
                        rotationX = (-(change.position.y - centerY) / centerY * 8f)
                            .coerceIn(-8f, 8f)
                    },
                    onDragEnd = {
                        rotationX = 0f
                        rotationY = 0f
                    }
                )
            }
            .graphicsLayer {
                this.rotationX = animatedRotationX
                this.rotationY = animatedRotationY
                cameraDistance = 12f * density
            }
    ) {
        GlassCard(glowColor = glowColor) { content() }
    }
}
