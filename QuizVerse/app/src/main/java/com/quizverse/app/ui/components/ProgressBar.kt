package com.quizverse.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Horizontal XP progress bar with animated fill, gradient fill colour,
 * glow effect, animated shimmer on the fill, and level/XP labels.
 *
 * @param currentXp       Player's XP within the current level.
 * @param xpToNextLevel   Total XP needed to reach the next level.
 * @param currentLevel    The player's current level number shown on the left.
 * @param height          Bar height.
 * @param gradientStart   Start colour of the progress gradient.
 * @param gradientEnd     End colour of the progress gradient.
 * @param modifier        Optional layout modifier.
 */
@Composable
fun XpProgressBar(
    currentXp: Int,
    xpToNextLevel: Int,
    currentLevel: Int,
    height: Dp = 14.dp,
    gradientStart: Color = Color(0xFF6C63FF),
    gradientEnd: Color = Color(0xFFA855F7),
    modifier: Modifier = Modifier,
) {
    val progress = (currentXp.toFloat() / xpToNextLevel.coerceAtLeast(1)).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "xpProgress",
    )

    // Shimmer animation: moves a highlight from left to right
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    // Glow pulse animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Level $currentLevel",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = gradientStart,
            )
            Text(
                text = "$currentXp / $xpToNextLevel XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(contentAlignment = Alignment.Center) {
            // Glow layer behind the bar (blurred)
            if (animatedProgress > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(height + 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    gradientStart.copy(alpha = glowAlpha * 0.5f),
                                    gradientEnd.copy(alpha = glowAlpha * 0.5f),
                                ),
                            ),
                        )
                        .blur(8.dp)
                        .align(Alignment.CenterStart),
                )
            }

            // Bar track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f)),
            ) {
                // Filled portion with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(gradientStart, gradientEnd),
                            ),
                        ),
                ) {
                    // Shimmer highlight overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        (shimmerOffset - 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                                        shimmerOffset.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.45f),
                                        (shimmerOffset + 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                                        1f to Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                }

                // Top shine overlay (static)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight(0.40f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .align(Alignment.TopStart),
                )
            }
        }
    }
}
