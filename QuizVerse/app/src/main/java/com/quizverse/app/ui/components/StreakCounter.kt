package com.quizverse.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldDark
import com.quizverse.app.ui.theme.GoldLight

/** Milestones at which the fire emoji grows larger and a pulse triggers. */
private val STREAK_MILESTONES = setOf(5, 10, 20, 50, 100)

/**
 * Displays the current player streak with a fire emoji that grows with the streak,
 * pulses at milestones, and uses Gold accent colours for a premium feel.
 *
 * @param streak   Current streak count.
 * @param modifier Optional layout modifier.
 */
@Composable
fun StreakCounter(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val isMilestone = streak in STREAK_MILESTONES

    // Continuous pulse at milestone streaks
    val infiniteTransition = rememberInfiniteTransition(label = "streakPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isMilestone) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streakEmojiPulse",
    )

    // Emoji grows logarithmically with streak (max ×2.4)
    val emojiSize = (24f + (kotlin.math.ln((streak + 1).toDouble()) * 4f)).coerceIn(24.0, 52.0).toFloat()

    // Number colour: gold accent at all levels, warming with streak count
    val numberColor by animateColorAsState(
        targetValue = when {
            streak >= 50  -> Color(0xFFFF4500)   // deep orange-red
            streak >= 20  -> Color(0xFFFF8C00)   // dark orange
            streak >= 10  -> Gold                 // gold at 10+
            streak >= 5   -> GoldLight            // light gold at 5+
            else          -> Gold                 // always gold accent
        },
        animationSpec = tween(durationMillis = 500),
        label = "streakColor",
    )

    // Gold glow pulse for active streaks
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = if (streak >= 5) 0.3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "streakGlow",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (streak >= 5) Gold.copy(alpha = 0.4f + glowAlpha) else Color.White.copy(alpha = 0.15f),
                        Color.Transparent,
                    ),
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Fire emoji with dynamic scale
            Text(
                text = "🔥",
                fontSize = emojiSize.sp,
                modifier = Modifier.scale(pulseScale),
            )

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = streak.toString(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = numberColor,
                )
                Text(
                    text = "Streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }

            // Milestone celebration badge
            if (isMilestone) {
                Text(
                    text = "🎉",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
