package com.bestjournal.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bestjournal.app.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.util.Achievement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Trophy colors inspired by PlayStation
private val WarmGold = Color(0xFF8B6914)
private val BrightGold = Color(0xFFD4A017)
private val LightGold = Color(0xFFFFD700)
private val SoftGold = Color(0xFFF5DEB3)
private val LockedGray = Color(0xFF5A5A5A)
private val LockedBgDark = Color(0xFF2A2A2E)
private val LockedBgLight = Color(0xFFE8E8EC)
// Dark mode unlocked row: warm tone that harmonizes with the app's forest-green palette
private val UnlockedBgDark = Color(0xFF33382A)
private val UnlockedBgLight = Color(0xFFFFF8E7)

@Composable
fun AchievementsSection(
    achievements: List<Achievement>,
    onSectionViewed: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var hasTrackedView by remember { mutableStateOf(false) }
    val unlockedCount = achievements.count { it.unlockedAt != null }
    val totalCount = achievements.size
    val isDark = LocalIsDarkTheme.current

    val progressAnimatable = remember { Animatable(0f) }
    val targetProgress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f

    LaunchedEffect(expanded) {
        if (expanded) {
            if (!hasTrackedView) {
                onSectionViewed()
                hasTrackedView = true
            }
            // Reset to 0 first so re-expand shows the animation again
            progressAnimatable.snapTo(0f)
            progressAnimatable.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        }
    }

    GlassCard {
        Column {
            // Header row — title treated as "visually centered" regardless of
            // the trophy icon on the left (same pattern as Feedback, Sicherheit,
            // Premium etc.). Invisible counterbalance spacer on the right equals
            // icon-circle (36) + spacer (10) = 46 dp.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Left trophy
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.achievements_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(R.string.achievements_counter, unlockedCount, totalCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    // Right trophy (mirrors the left one for symmetry)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                // Chevron pinned to the right edge
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.dp),
                )
            }

            // Animated progress bar + achievement list
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar with gold gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isDark) Color(0xFF3A3A3E) else Color(0xFFE0E0E4)
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progressAnimatable.value)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(WarmGold, BrightGold, LightGold)
                                    )
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Achievement list
                    achievements.forEachIndexed { index, achievement ->
                        AchievementRow(
                            achievement = achievement,
                            isDark = isDark,
                            animationDelay = index * 80,
                        )
                        if (index < achievements.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    isDark: Boolean,
    animationDelay: Int,
) {
    val isUnlocked = achievement.unlockedAt != null

    // Staggered entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "row_alpha",
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "row_scale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isUnlocked) {
                    if (isDark) UnlockedBgDark else UnlockedBgLight
                } else {
                    if (isDark) LockedBgDark else LockedBgLight
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon circle
        AchievementIcon(
            achievement = achievement,
            isUnlocked = isUnlocked,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isUnlocked) stringResource(achievement.titleResId) else "???",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (isUnlocked) {
                    if (isDark) SoftGold else WarmGold
                } else {
                    LockedGray
                },
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isUnlocked) stringResource(achievement.descResId) else stringResource(R.string.achievement_secret),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontStyle = if (!isUnlocked) FontStyle.Italic else FontStyle.Normal,
                ),
                color = if (isUnlocked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    LockedGray.copy(alpha = 0.7f)
                },
            )
            if (isUnlocked && achievement.unlockedAt != null) {
                Spacer(modifier = Modifier.height(2.dp))
                val dateStr = remember(achievement.unlockedAt) {
                    SimpleDateFormat(
                        android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMyyyy"),
                        Locale.getDefault()
                    ).format(Date(achievement.unlockedAt))
                }
                Text(
                    text = stringResource(R.string.achievements_unlocked_at, dateStr),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = BrightGold.copy(alpha = 0.7f),
                )
            }
        }

        // Lock icon for locked achievements
        if (!isUnlocked) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = LockedGray.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AchievementIcon(
    achievement: Achievement,
    isUnlocked: Boolean,
) {
    val icon = if (isUnlocked) {
        getIconForName(achievement.iconName)
    } else {
        Icons.Rounded.HelpOutline
    }

    if (isUnlocked) {
        // Shimmer animation only for unlocked achievements (saves CPU on locked ones)
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmerAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "shimmer_alpha",
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrightGold.copy(alpha = 0.25f * shimmerAlpha),
                            WarmGold.copy(alpha = 0.1f),
                        )
                    )
                ),
        ) {
            Canvas(modifier = Modifier.size(44.dp)) {
                drawCircle(
                    color = BrightGold.copy(alpha = 0.3f * shimmerAlpha),
                    radius = size.minDimension / 2,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
                )
            }
            Icon(
                icon,
                contentDescription = stringResource(achievement.titleResId),
                tint = LightGold.copy(alpha = shimmerAlpha),
                modifier = Modifier.size(24.dp),
            )
        }
    } else {
        // Static icon for locked achievements — no animation overhead
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LockedGray.copy(alpha = 0.15f),
                            Color.Transparent,
                        )
                    )
                ),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = LockedGray.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun getIconForName(name: String): ImageVector = when (name) {
    "Bedtime" -> Icons.Rounded.Bedtime
    "WbSunny" -> Icons.Rounded.WbSunny
    "AutoStories" -> Icons.Rounded.AutoStories
    "EmojiEvents" -> Icons.Rounded.EmojiEvents
    "Brush" -> Icons.Rounded.Brush
    "Whatshot" -> Icons.Rounded.Whatshot
    "CalendarMonth" -> Icons.Rounded.CalendarMonth
    "PhotoCamera" -> Icons.Rounded.PhotoCamera
    "Mic" -> Icons.Rounded.Mic
    "Psychology" -> Icons.Rounded.Psychology
    else -> Icons.Rounded.EmojiEvents
}
