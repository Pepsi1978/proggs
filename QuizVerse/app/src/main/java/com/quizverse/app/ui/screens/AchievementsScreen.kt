package com.quizverse.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import kotlinx.coroutines.delay

// ── Brand colors ──────────────────────────────────────────────────────────────
private val Primary      = Color(0xFF6C63FF)
private val Accent       = Color(0xFF43E97B)
private val BronzeColor  = Color(0xFFCD7F32)
private val SilverColor  = Color(0xFFC0C0C0)
private val GoldColor    = Color(0xFFFFD700)

private val BackgroundDark    = Color(0xFF12121F)
private val SurfaceCard       = Color(0xFF1E1E2E)
private val SurfaceCardLocked = Color(0xFF181826)
private val TextPrimary       = Color(0xFFEAEAFF)
private val TextSecondary     = Color(0xFF8888BB)

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
            .background(BackgroundDark)
    ) {
        // Subtle top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Primary.copy(alpha = 0.18f), Color.Transparent)
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
                tint               = TextPrimary
            )
        }

        Text(
            text       = "Erfolge",
            color      = TextPrimary,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.weight(1f)
        )

        if (totalCount > 0) {
            Surface(
                shape    = RoundedCornerShape(20.dp),
                color    = Primary.copy(alpha = 0.22f),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text       = "$unlockedCount",
                        color      = Primary,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "/", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text       = "$totalCount",
                        color      = TextSecondary,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Filter chips ──────────────────────────────────────────────────────────────

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
                    selectedContainerColor = Primary,
                    selectedLabelColor     = Color.White,
                    containerColor         = SurfaceCard,
                    labelColor             = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = isSelected,
                    selectedBorderColor = Primary.copy(alpha = 0.5f),
                    borderColor         = Color.White.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(20.dp)
            )
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

// ── Achievement card ──────────────────────────────────────────────────────────

@Composable
private fun AchievementCard(achievement: Achievement) {
    val tierColor = when (achievement.tier) {
        1    -> BronzeColor
        2    -> SilverColor
        3    -> GoldColor
        else -> TextSecondary
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

    val cardColor    = if (achievement.isUnlocked) SurfaceCard else SurfaceCardLocked
    val contentAlpha = if (achievement.isUnlocked) 1f else 0.55f

    Surface(
        shape          = RoundedCornerShape(16.dp),
        color          = cardColor,
        modifier       = Modifier.fillMaxWidth(),
        shadowElevation = if (achievement.isUnlocked) 4.dp else 1.dp,
        tonalElevation  = 0.dp
    ) {
        Box {
            // Left tier accent stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        if (achievement.isUnlocked) tierColor
                        else tierColor.copy(alpha = 0.3f)
                    )
            )

            // Subtle top gradient on unlocked cards
            if (achievement.isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    tierColor.copy(alpha = 0.07f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (achievement.isUnlocked)
                                tierColor.copy(alpha = 0.15f)
                            else
                                Color.White.copy(alpha = 0.04f)
                        )
                        .alpha(contentAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = achievement.iconName, fontSize = 26.sp)
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
                            color      = TextPrimary.copy(alpha = contentAlpha),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            modifier   = Modifier.weight(1f, fill = false)
                        )

                        if (tierLabel.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = tierColor.copy(
                                    alpha = if (achievement.isUnlocked) 0.20f else 0.08f
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
                        color      = TextSecondary.copy(alpha = contentAlpha),
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
                            color = TextSecondary.copy(alpha = contentAlpha),
                            fontSize = 11.sp
                        )
                        Text(
                            text       = "${(progress * 100).toInt()}%",
                            color      = if (achievement.isUnlocked) Accent
                                         else TextSecondary.copy(alpha = contentAlpha),
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
                        color      = if (achievement.isUnlocked) Accent
                                     else tierColor.copy(alpha = 0.4f),
                        trackColor = Color.White.copy(alpha = 0.07f),
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

                // Unlocked / locked icon
                if (achievement.isUnlocked) {
                    Icon(
                        imageVector        = Icons.Default.CheckCircle,
                        contentDescription = "Freigeschaltet",
                        tint               = Accent,
                        modifier           = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = "Gesperrt",
                        tint               = TextSecondary.copy(alpha = 0.4f),
                        modifier           = Modifier.size(20.dp)
                    )
                }
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
                color     = TextSecondary,
                fontSize  = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
