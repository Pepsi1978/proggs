package com.quizverse.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.quizverse.app.ui.components.XpProgressBar
import com.quizverse.app.ui.theme.Accent
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldDark
import com.quizverse.app.ui.theme.GoldLight
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
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
        label         = "profileFadeIn",
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue   = if (visible) 0f else 60f,
        animationSpec = tween(durationMillis = 450, easing = EaseOutCubic),
        label         = "profileSlideUp",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Profil",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->

        if (state.isLoading) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Gaming Avatar Card with gradient ring + XP bar ---
            GamingAvatarCard(
                level             = state.level,
                xp                = state.xp,
                xpForCurrentLevel = state.xpForCurrentLevel,
                xpForNextLevel    = state.xpForNextLevel,
                levelProgress     = state.levelProgress,
            )

            // --- Statistics section header ---
            Text(
                text       = "Statistiken",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier.padding(top = 4.dp, start = 4.dp),
            )

            val correctRatePct = (state.correctRate * 100).roundToInt()

            // Stats grid — 2 columns
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GamingStatCard(
                    modifier     = Modifier.weight(1f),
                    icon         = Icons.Filled.QuestionMark,
                    iconTint     = Primary,
                    value        = state.totalQuestionsAnswered.toString(),
                    label        = "Fragen",
                    accentColor  = Primary,
                )
                GamingStatCard(
                    modifier     = Modifier.weight(1f),
                    icon         = Icons.Filled.Star,
                    iconTint     = Accent,
                    value        = state.totalCorrectAnswers.toString(),
                    label        = "Richtig",
                    accentColor  = Accent,
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GamingStatCard(
                    modifier     = Modifier.weight(1f),
                    icon         = Icons.Filled.EmojiEvents,
                    iconTint     = Gold,
                    value        = "$correctRatePct%",
                    label        = "Trefferquote",
                    accentColor  = Gold,
                )
                GamingStatCard(
                    modifier     = Modifier.weight(1f),
                    icon         = Icons.Filled.Whatshot,
                    iconTint     = Color(0xFFFF6B35),
                    value        = state.currentStreak.toString(),
                    label        = "Serie",
                    accentColor  = Color(0xFFFF6B35),
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GamingStatCard(
                    modifier     = Modifier.weight(1f),
                    icon         = Icons.Filled.EmojiEvents,
                    iconTint     = GoldDark,
                    value        = state.longestStreak.toString(),
                    label        = "Längste Serie",
                    accentColor  = GoldDark,
                )
                GamingStatCard(
                    modifier     = Modifier.weight(1f),
                    icon         = Icons.Filled.Timer,
                    iconTint     = Secondary,
                    value        = state.totalPlayTimeFormatted,
                    label        = "Spielzeit",
                    accentColor  = Secondary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Gaming Avatar Card — gradient ring around avatar, XP bar with glow
// ---------------------------------------------------------------------------

@Composable
private fun GamingAvatarCard(
    level: Int,
    xp: Int,
    xpForCurrentLevel: Int,
    xpForNextLevel: Int,
    levelProgress: Float,
) {
    // Rotating glow animation for the avatar ring
    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringRotation",
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(24.dp),
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        GlassWhite,
                        Primary.copy(alpha = 0.06f),
                        Gold.copy(alpha = 0.04f),
                    ),
                    start  = Offset(0f, 0f),
                    end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .padding(24.dp),
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avatar with gradient ring
            Box(contentAlignment = Alignment.Center) {
                // Outer glow halo
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = glowPulse * 0.2f))
                        .blur(16.dp),
                )

                // Animated gradient ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(Primary, Gold, Accent, Secondary, Primary),
                            ),
                            shape = CircleShape,
                        ),
                )

                // Avatar placeholder with gradient fill
                Box(
                    modifier = Modifier
                        .size(102.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.30f),
                                    Gold.copy(alpha = 0.15f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Level number in avatar
                    Text(
                        text       = level.toString(),
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Level badge below avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldDark, GoldColor, GoldLight),
                            ),
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Text(
                        text       = "Level $level",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF1A1A1A),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // XP Progress ring
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(96.dp),
            ) {
                // Track ring
                CircularProgressIndicator(
                    progress      = { 1f },
                    modifier      = Modifier.size(96.dp),
                    color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    strokeWidth   = 8.dp,
                    strokeCap     = StrokeCap.Round,
                )
                // Animated fill ring with glow
                val animatedArc by animateFloatAsState(
                    targetValue   = levelProgress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
                    label         = "circularXp",
                )
                CircularProgressIndicator(
                    progress    = { animatedArc },
                    modifier    = Modifier.size(96.dp),
                    color       = Gold,
                    strokeWidth = 8.dp,
                    strokeCap   = StrokeCap.Round,
                    trackColor  = Color.Transparent,
                )
                // XP values in center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "$xp",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Gold,
                    )
                    Text(
                        text  = "XP",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            // Linear XP bar with glow
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                XpProgressBar(
                    currentXp     = xp - xpForCurrentLevel,
                    xpToNextLevel = (xpForNextLevel - xpForCurrentLevel).coerceAtLeast(1),
                    currentLevel  = level,
                    height        = 10.dp,
                    gradientStart = Primary,
                    gradientEnd   = Gold,
                    modifier      = Modifier.fillMaxWidth(),
                )
                Text(
                    text      = "Nächstes Level bei $xpForNextLevel XP",
                    fontSize  = 11.sp,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Gaming Stat Card — icon + glow + Glassmorphism
// ---------------------------------------------------------------------------

@Composable
private fun GamingStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    accentColor: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(18.dp),
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        GlassWhite,
                        accentColor.copy(alpha = 0.06f),
                    ),
                    start  = Offset(0f, 0f),
                    end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .padding(16.dp),
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Icon badge with glow halo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .blur(6.dp),
                )
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.20f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.25f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = iconTint,
                        modifier           = Modifier.size(22.dp),
                    )
                }
            }

            // Value
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center,
                maxLines   = 1,
            )

            // Label
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign  = TextAlign.Center,
                lineHeight = 16.sp,
            )
        }
    }
}

// Local alias for Gold to avoid naming collision with imported Gold
private val GoldColor = Gold
