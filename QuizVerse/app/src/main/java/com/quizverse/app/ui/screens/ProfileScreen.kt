package com.quizverse.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.quizverse.app.ui.theme.Accent
import com.quizverse.app.ui.theme.Primary
import com.quizverse.app.ui.theme.Secondary
import com.quizverse.app.viewmodel.ProfileViewModel
import com.quizverse.app.viewmodel.QuizViewModelFactory
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel(factory = QuizViewModelFactory.create())
    val state by viewModel.uiState.collectAsState()

    // Entrance animation: fade in + slide up
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
        label = "profileFadeIn"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 60f,
        animationSpec = tween(durationMillis = 450, easing = EaseOutCubic),
        label = "profileSlideUp"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .alpha(animatedAlpha)
                .offset(y = animatedOffsetY.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Level Badge with circular XP progress ---
            LevelBadgeCard(
                level = state.level,
                xp = state.xp,
                xpForCurrentLevel = state.xpForCurrentLevel,
                xpForNextLevel = state.xpForNextLevel,
                levelProgress = state.levelProgress
            )

            // --- Section header ---
            Text(
                text = "Statistiken",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            val correctRatePct = (state.correctRate * 100).roundToInt()

            // Stats Row 1: Fragen beantwortet | Richtige Antworten
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "❓",
                    value = state.totalQuestionsAnswered.toString(),
                    label = "Fragen beantwortet",
                    gradientColors = listOf(
                        Primary.copy(alpha = 0.18f),
                        Primary.copy(alpha = 0.04f)
                    )
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "✅",
                    value = state.totalCorrectAnswers.toString(),
                    label = "Richtige Antworten",
                    gradientColors = listOf(
                        Secondary.copy(alpha = 0.18f),
                        Secondary.copy(alpha = 0.04f)
                    )
                )
            }

            // Stats Row 2: Trefferquote | Aktuelle Serie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🎯",
                    value = "$correctRatePct%",
                    label = "Trefferquote",
                    gradientColors = listOf(
                        Accent.copy(alpha = 0.18f),
                        Accent.copy(alpha = 0.04f)
                    )
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🔥",
                    value = state.currentStreak.toString(),
                    label = "Aktuelle Serie",
                    gradientColors = listOf(
                        Color(0xFFFF6B35).copy(alpha = 0.18f),
                        Color(0xFFFF6B35).copy(alpha = 0.04f)
                    )
                )
            }

            // Stats Row 3: Längste Serie | Spielzeit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🏆",
                    value = state.longestStreak.toString(),
                    label = "Längste Serie",
                    gradientColors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.18f),
                        Color(0xFFFFD700).copy(alpha = 0.04f)
                    )
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "⏱️",
                    value = state.totalPlayTimeFormatted,
                    label = "Spielzeit",
                    gradientColors = listOf(
                        Primary.copy(alpha = 0.12f),
                        Secondary.copy(alpha = 0.06f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Level Badge Card
// ---------------------------------------------------------------------------

@Composable
private fun LevelBadgeCard(
    level: Int,
    xp: Int,
    xpForCurrentLevel: Int,
    xpForNextLevel: Int,
    levelProgress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.20f),
                            Secondary.copy(alpha = 0.12f),
                            Accent.copy(alpha = 0.06f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Dein Level",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Medium
                )

                // Circular progress ring around level number
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(124.dp)
                ) {
                    // Track ring
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(124.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    // Animated fill ring
                    val animatedArc by animateFloatAsState(
                        targetValue = levelProgress.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
                        label = "circularXp"
                    )
                    CircularProgressIndicator(
                        progress = { animatedArc },
                        modifier = Modifier.size(124.dp),
                        color = Primary,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = Color.Transparent
                    )
                    // Level number + star
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⭐", fontSize = 20.sp)
                        Text(
                            text = level.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // XP label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$xp XP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        text = "Nächstes Level bei $xpForNextLevel XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                        textAlign = TextAlign.Center
                    )
                }

                // Linear XP bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val animatedLinear by animateFloatAsState(
                        targetValue = levelProgress.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
                        label = "linearXp"
                    )
                    LinearProgressIndicator(
                        progress = { animatedLinear },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                        strokeCap = StrokeCap.Round
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$xpForCurrentLevel XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                        )
                        Text(
                            text = "$xpForNextLevel XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stat Card
// ---------------------------------------------------------------------------

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String,
    gradientColors: List<Color>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Emoji badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
