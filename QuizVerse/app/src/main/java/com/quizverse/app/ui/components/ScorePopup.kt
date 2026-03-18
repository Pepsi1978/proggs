package com.quizverse.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizverse.app.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Floating score label (e.g. "+100") that animates upward and fades out with a gold glow.
 *
 * The score text is rendered with a layered shadow + glow effect to achieve a
 * premium gold-glow appearance. Place at the desired anchor position and control
 * visibility through [visible]. Pass [onAnimationEnd] to hide the popup after
 * the animation completes.
 *
 * @param score          Score delta to display (e.g. "+100", "+250 ⚡").
 * @param visible        Whether the popup is currently showing.
 * @param color          Text colour; defaults to premium Gold.
 * @param onAnimationEnd Called when the exit animation finishes.
 * @param modifier       Optional layout modifier.
 */
@Composable
fun ScorePopup(
    score: String,
    visible: Boolean,
    color: Color = Gold,
    onAnimationEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    // Y-offset: animates from 0 → -140 px (floats upward)
    val offsetY = remember { Animatable(0f) }
    // Alpha: stays at 1 for the first 400 ms, then fades to 0
    val alpha = remember { Animatable(1f) }
    // Scale: brief pop-in at the start
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(visible) {
        // Pop in
        launch {
            scale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 100),
            )
        }
        // Float upward
        launch {
            offsetY.animateTo(
                targetValue = -140f,
                animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            )
        }
        // Fade out after a short hold
        launch {
            delay(380)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 550, easing = LinearEasing),
            )
        }
        delay(950)
        onAnimationEnd()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset { IntOffset(x = 0, y = offsetY.value.toInt()) }
            .alpha(alpha.value),
    ) {
        // Gold glow halo (blurred behind text)
        Text(
            text = score,
            color = color.copy(alpha = 0.4f),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            modifier = Modifier.offset(x = 0.dp, y = 2.dp),
        )
        // Dark shadow layer for depth
        Text(
            text = score,
            color = Color.Black.copy(alpha = 0.45f),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            modifier = Modifier.offset(x = 1.5.dp, y = 2.dp),
        )
        // Main gold text
        Text(
            text = score,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
        )
    }
}
