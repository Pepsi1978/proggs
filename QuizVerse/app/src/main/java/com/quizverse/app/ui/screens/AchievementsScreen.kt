package com.quizverse.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.data.database.entities.Achievement
import com.quizverse.app.ui.theme.Accent
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldDark
import com.quizverse.app.ui.theme.GoldLight
import com.quizverse.app.ui.theme.Primary
import kotlinx.coroutines.delay

// ── Tier colors ───────────────────────────────────────────────────────────────
private val BronzeColor  = Color(0xFFCD7F32)
private val BronzeGlow   = Color(0xFFE8A050)
private val SilverColor  = Color(0xFFC0C0C0)
private val SilverGlow   = Color(0xFFD8D8E8)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavHostController) {
    val app          = LocalContext.current.applicationContext as QuizVerseApp
    val db           = app.database
    val achievements by db.achievementDao().getAllAchievements().collectAsState(initial = emptyList())

    // 0 = Alle  |  1 = Freigeschaltet  |  2 = Gesperrt
    var selectedFilter by remember { mutableStateOf(0) }

    val filteredAchievements = remember(achievements, selectedFilter) {
        when (selectedFilter) {
            1    -> achievements.filter { it.isUnlocked }
            2    -> achievements.filter { !it.isUnlocked }
            else -> achievements
        }
    }

    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }
    val totalCount    = achievements.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle premium top gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.22f),
                            Gold.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AchievementsTopBar(
                unlockedCount = unlockedCount,
                totalCount    = totalCount,
                onBack        = { navController.popBackStack() }
            )

            FilterChipRow(
                selectedIndex    = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredAchievements.isEmpty()) {
                EmptyState(filterIndex = selectedFilter)
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = filteredAchievements,
                        key   = { _, a -> a.id }
                    ) { index, achievement ->
                        StaggeredAchievementCard(achievement = achievement, index = index)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun AchievementsTopBar(
    unlockedCount: Int,
    totalCount:    Int,
    onBack:        () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector        = Icons.Default.ArrowBack,
                contentDescription = "Zurück",
                tint               = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text       = "Erfolge",
            color      = MaterialTheme.colorScheme.onBackground,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.weight(1f)
        )

        if (totalCount > 0) {
            // Gold-tinted badge for achievement count
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Gold.copy(alpha = 0.18f), Primary.copy(alpha = 0.18f))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Gold.copy(alpha = 0.4f), Primary.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(end = 8.dp)
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text       = "$unlockedCount",
                        color      = Gold,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "/",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text       = "$totalCount",
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Filter chips with gradient ─────────────────────────────────────────────────

@Composable
private fun FilterChipRow(
    selectedIndex:    Int,
    onFilterSelected: (Int) -> Unit
) {
    val filters = listOf("Alle", "Freigeschaltet", "Gesperrt")

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEachIndexed { index, label ->
            val isSelected = selectedIndex == index
            // Gradient-styled filter chip using Box for selected state
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(
                            colors = listOf(Primary, Primary.copy(alpha = 0.8f))
                        ) else Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Primary.copy(alpha = 0.5f)
                                else GlassBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                FilterChip(
                    selected = isSelected,
                    onClick  = { onFilterSelected(index) },
                    label    = {
                        Text(
                            text       = label,
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Transparent,
                        selectedLabelColor     = Color.White,
                        containerColor         = Color.Transparent,
                        labelColor             = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = isSelected,
                        selectedBorderColor = Color.Transparent,
                        borderColor         = Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

// ── Staggered entrance animation ──────────────────────────────────────────────

@Composable
private fun StaggeredAchievementCard(achievement: Achievement, index: Int) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(achievement.id) {
        delay((index * 60L).coerceAtMost(400L))
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(animationSpec = tween(350)) +
                  slideInVertically(
                      animationSpec    = tween(350, easing = EaseOutCubic),
                      initialOffsetY   = { it / 4 }
                  )
    ) {
        AchievementCard(achievement = achievement)
    }
}

// ── Achievement card with 3D/Glassmorphism effect ─────────────────────────────

@Composable
private fun AchievementCard(achievement: Achievement) {
    val tierColor = when (achievement.tier) {
        1    -> BronzeColor
        2    -> SilverColor
        3    -> Gold
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val tierGlow = when (achievement.tier) {
        1    -> BronzeGlow
        2    -> SilverGlow
        3    -> GoldLight
        else -> Color.Transparent
    }

    val tierLabel = when (achievement.tier) {
        1    -> "Bronze"
        2    -> "Silber"
        3    -> "Gold"
        else -> ""
    }

    val progress = if (achievement.requiredValue > 0) {
        (achievement.currentValue.toFloat() / achievement.requiredValue.toFloat()).coerceIn(0f, 1f)
    } else {
        if (achievement.isUnlocked) 1f else 0f
    }

    val contentAlpha = if (achievement.isUnlocked) 1f else 0.55f

    // Card with glassmorphism effect
    val cardBackground = if (achievement.isUnlocked)
        Brush.linearGradient(
            colors = listOf(
                tierColor.copy(alpha = 0.08f),
                GlassWhite,
                MaterialTheme.colorScheme.surface
            )
        )
    else
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(
                width = 1.dp,
                brush = if (achievement.isUnlocked) Brush.linearGradient(
                    colors = listOf(tierColor.copy(alpha = 0.45f), tierGlow.copy(alpha = 0.2f))
                ) else Brush.linearGradient(
                    colors = listOf(GlassBorder, GlassBorder)
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Tier glow effect for unlocked cards
        if (achievement.isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                tierGlow.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Left tier accent stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (achievement.isUnlocked)
                            listOf(tierGlow, tierColor)
                        else
                            listOf(tierColor.copy(alpha = 0.25f), tierColor.copy(alpha = 0.1f))
                    )
                )
        )

        // Blur overlay for locked achievements
        if (!achievement.isUnlocked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            )
        }

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon with tier glow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked)
                            Brush.radialGradient(
                                colors = listOf(
                                    tierGlow.copy(alpha = 0.22f),
                                    tierColor.copy(alpha = 0.08f)
                                )
                            )
                        else
                            Brush.radialGradient(
                                colors = listOf(
                                    GlassWhite,
                                    Color.Transparent
                                )
                            )
                    )
                    .alpha(contentAlpha),
                contentAlignment = Alignment.Center
            ) {
                if (!achievement.isUnlocked) {
                    // Blurred icon under lock
                    Text(
                        text = achievement.iconName,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .alpha(0.3f)
                            .blur(3.dp)
                    )
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = "Gesperrt",
                        tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier           = Modifier.size(20.dp)
                    )
                } else {
                    Text(text = achievement.iconName, fontSize = 26.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text + progress
            Column(modifier = Modifier.weight(1f)) {
                // Title row
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = achievement.nameKey,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f, fill = false)
                    )

                    if (tierLabel.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    tierColor.copy(
                                        alpha = if (achievement.isUnlocked) 0.22f else 0.08f
                                    )
                                )
                        ) {
                            Text(
                                text       = tierLabel,
                                color      = tierColor.copy(alpha = contentAlpha),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text       = achievement.descriptionKey,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    fontSize   = 12.sp,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "${achievement.currentValue} / ${achievement.requiredValue}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        fontSize = 11.sp
                    )
                    Text(
                        text       = "${(progress * 100).toInt()}%",
                        color      = if (achievement.isUnlocked) Accent
                                     else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val animatedProgress by animateFloatAsState(
                    targetValue  = progress,
                    animationSpec = tween(800, easing = EaseOutCubic),
                    label        = "progress_${achievement.id}"
                )

                LinearProgressIndicator(
                    progress  = { animatedProgress },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color      = if (achievement.isUnlocked) tierColor
                                 else tierColor.copy(alpha = 0.4f),
                    trackColor = GlassWhite,
                    strokeCap  = StrokeCap.Round
                )

                // Unlock date
                if (achievement.isUnlocked && achievement.unlockedDate != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text       = "Freigeschaltet: ${achievement.unlockedDate}",
                        color      = Accent.copy(alpha = 0.75f),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Unlocked check icon (lock icon is now inside the emoji box)
            if (achievement.isUnlocked) {
                Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = "Freigeschaltet",
                    tint               = tierColor,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(filterIndex: Int) {
    val (emoji, message) = when (filterIndex) {
        1    -> "🎯" to "Noch keine Erfolge freigeschaltet.\nSpiele mehr Quizze, um Erfolge zu verdienen!"
        2    -> "🏆" to "Alle Erfolge sind freigeschaltet!\nGroßartige Leistung!"
        else -> "📋" to "Keine Erfolge vorhanden."
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = emoji, fontSize = 52.sp)
            Text(
                text      = message,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize  = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
