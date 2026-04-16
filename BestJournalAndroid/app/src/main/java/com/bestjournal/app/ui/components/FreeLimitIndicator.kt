package com.bestjournal.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestjournal.app.R
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.NeonRed

@Composable
fun FreeLimitIndicator(
    usedCount: Int,
    maxCount: Int,
    featureLabel: String,
    onUpgradeClick: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
    ) {
        val targetProgress = if (maxCount > 0) usedCount.toFloat() / maxCount.toFloat() else 0f
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            label = "progress",
        )
        val percentage = targetProgress * 100f
        val remaining = (maxCount - usedCount).coerceAtLeast(0)

        // Smooth color transition between thresholds
        val progressColor by animateColorAsState(
            targetValue = when {
                percentage >= 80f -> NeonRed
                percentage >= 60f -> NeonAmber
                else -> NeonEmerald
            },
            animationSpec = tween(500),
            label = "color",
        )

        val trackColor = MaterialTheme.colorScheme.surfaceVariant

        // Infinite animations for shimmer and pulse
        val infiniteTransition = rememberInfiniteTransition(label = "indicator")
        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -0.3f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer",
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )

        GlassCard(
            modifier = modifier.fillMaxWidth(),
            glowColor = progressColor,
            glowIntensity = 0.08f,
            cornerRadius = 16.dp,
            contentPadding = 14.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon in colored circle (app design pattern)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(progressColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = progressColor,
                    )
                }

                // Progress section
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Label row with remaining count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = featureLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.limit_remaining, remaining),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = progressColor,
                        )
                    }

                    // Custom gradient progress bar with shimmer sweep
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    ) {
                        val cornerPx = 3.dp.toPx()
                        val barCorner = CornerRadius(cornerPx)

                        // Track background
                        drawRoundRect(
                            color = trackColor,
                            cornerRadius = barCorner,
                        )

                        // Filled progress with gradient
                        if (animatedProgress > 0f) {
                            val filledWidth = size.width * animatedProgress
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        progressColor.copy(alpha = 0.5f),
                                        progressColor,
                                    ),
                                    endX = filledWidth,
                                ),
                                size = Size(filledWidth, size.height),
                                cornerRadius = barCorner,
                            )

                            // Shimmer highlight sweep across filled area
                            val shimmerCenter = shimmerOffset * filledWidth
                            val shimmerHalf = filledWidth * 0.3f
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.2f),
                                        Color.Transparent,
                                    ),
                                    startX = shimmerCenter - shimmerHalf,
                                    endX = shimmerCenter + shimmerHalf,
                                ),
                                size = Size(filledWidth, size.height),
                                cornerRadius = barCorner,
                            )
                        }
                    }

                    // Bottom row: usage text + optional Premium button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.limit_used_of_max, usedCount, maxCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )

                        if (percentage >= 80f) {
                            TextButton(
                                onClick = onUpgradeClick,
                                modifier = Modifier.alpha(pulseAlpha),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 2.dp,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = progressColor,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.label_premium),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = progressColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
