package com.quizverse.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Visual state of an answer button. */
enum class AnswerState { Default, Selected, Correct, Wrong }

/**
 * A premium answer option button with 3D gradient look, bevel shading, and rich visual feedback.
 *
 * Features:
 * - Letter badge (A/B/C/D) as 3D circle with gradient
 * - Animated gradient background (bevel: top-left highlight, bottom-right shadow)
 * - Green glow pulse animation on correct answers
 * - Red shake + glow animation on wrong answers
 * - Scale-up on press via [MutableInteractionSource]
 *
 * @param letter   One of "A", "B", "C", "D".
 * @param text     The answer text.
 * @param state    Current [AnswerState] driving colour and animation.
 * @param onClick  Invoked when the button is tapped (only active in [AnswerState.Default]).
 * @param modifier Optional layout modifier.
 */
@Composable
fun AnswerButton(
    letter: String,
    text: String,
    state: AnswerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ── Gradient backgrounds (3D bevel look) ──────────────────────────────
    val gradientDefault = Brush.linearGradient(
        colors = listOf(Color(0xFF2A3550), Color(0xFF1A2038)),
    )
    val gradientSelected = Brush.linearGradient(
        colors = listOf(Color(0xFF4A5280), Color(0xFF2A3060)),
    )
    val gradientCorrect = Brush.linearGradient(
        colors = listOf(Color(0xFF1E6B3A), Color(0xFF104A26)),
    )
    val gradientWrong = Brush.linearGradient(
        colors = listOf(Color(0xFF8B1A1A), Color(0xFF5A0A0A)),
    )

    val backgroundGradient = when (state) {
        AnswerState.Default  -> gradientDefault
        AnswerState.Selected -> gradientSelected
        AnswerState.Correct  -> gradientCorrect
        AnswerState.Wrong    -> gradientWrong
    }

    // ── Border colors ──────────────────────────────────────────────────────
    val borderDefault   = Color(0xFF3A4565)
    val borderSelected  = Color(0xFF6366F1)
    val borderCorrect   = Color(0xFF4ADE80)
    val borderWrong     = Color(0xFFF87171)

    val targetBorder by animateColorAsState(
        targetValue = when (state) {
            AnswerState.Default  -> borderDefault
            AnswerState.Selected -> borderSelected
            AnswerState.Correct  -> borderCorrect
            AnswerState.Wrong    -> borderWrong
        },
        animationSpec = tween(durationMillis = 300),
        label = "answerBorder",
    )

    // ── Glow effect for correct/wrong states ──────────────────────────────
    val glowAnim = rememberInfiniteTransition(label = "glowPulse")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0f,
        targetValue = when (state) {
            AnswerState.Correct -> 0.6f
            AnswerState.Wrong   -> 0.5f
            else                -> 0f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val glowColor = when (state) {
        AnswerState.Correct -> Color(0xFF4ADE80)
        AnswerState.Wrong   -> Color(0xFFF87171)
        else                -> Color.Transparent
    }

    // ── Shake animation (wrong answer) ────────────────────────────────────
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == AnswerState.Wrong) {
            repeat(5) {
                shakeOffset.animateTo(
                    targetValue = if (it % 2 == 0) 12f else -12f,
                    animationSpec = tween(durationMillis = 65, easing = LinearEasing),
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(durationMillis = 65))
        }
    }

    // ── Press scale animation ──────────────────────────────────────────────
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "answerScale",
    )

    // ── Status icon ────────────────────────────────────────────────────────
    val statusIcon = when (state) {
        AnswerState.Correct  -> "✓"
        AnswerState.Wrong    -> "✗"
        AnswerState.Selected -> "→"
        AnswerState.Default  -> ""
    }

    // Badge gradient colors (3D circle)
    val badgeGradient = when (state) {
        AnswerState.Default  -> Brush.radialGradient(
            colors = listOf(Color(0xFF4A5580), Color(0xFF2A3455)),
        )
        AnswerState.Selected -> Brush.radialGradient(
            colors = listOf(Color(0xFF8080F0), Color(0xFF4040C0)),
        )
        AnswerState.Correct  -> Brush.radialGradient(
            colors = listOf(Color(0xFF60EE90), Color(0xFF20A040)),
        )
        AnswerState.Wrong    -> Brush.radialGradient(
            colors = listOf(Color(0xFFFF8888), Color(0xFFCC2222)),
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer { translationX = shakeOffset.value }
            .scale(scale),
    ) {
        // Glow background layer (behind the button)
        if (state == AnswerState.Correct || state == AnswerState.Wrong) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(glowColor.copy(alpha = glowAlpha * 0.3f)),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundGradient)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            targetBorder.copy(alpha = 0.9f),
                            targetBorder.copy(alpha = 0.4f),
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                // Top-left bevel highlight
                .border(
                    width = 0.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.12f)),
                    enabled = state == AnswerState.Default,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 3D letter badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeGradient)
                    .border(
                        width = 1.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            ) {
                Text(
                    text = if (statusIcon.isNotEmpty()) statusIcon else letter,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
            }

            // Answer text
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = if (state != AnswerState.Default) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )

            // Right-side glow dot for feedback states
            if (state == AnswerState.Correct || state == AnswerState.Wrong) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = 0.8f + glowAlpha * 0.2f)),
                )
            }
        }
    }
}
