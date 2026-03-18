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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.data.database.entities.HighScore
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldDark
import com.quizverse.app.ui.theme.GoldLight
import kotlinx.coroutines.delay

// Podium medal colors
private val GoldColor   = Gold
private val SilverColor = Color(0xFFC0C0C0)
private val BronzeColor = Color(0xFFCD7F32)

private fun categoryName(id: Int?): String = when (id) {
    1    -> "Weltgeographie"
    2    -> "Wissenschaft"
    3    -> "Geschichte"
    4    -> "Film"
    5    -> "Musik"
    6    -> "Sport"
    7    -> "Technologie"
    8    -> "Essen"
    9    -> "Tierwelt"
    10   -> "Literatur"
    11   -> "Alle Kategorien"
    12   -> "Logik"
    13   -> "Hertha BSC"
    14   -> "BVB"
    else -> "Unbekannt"
}

private fun difficultyName(level: Int): String = when (level) {
    1    -> "Leicht"
    2    -> "Mittel"
    3    -> "Schwer"
    4    -> "Experte"
    5    -> "Meister"
    else -> "Unbekannt"
}

private fun difficultyColor(level: Int): Color = when (level) {
    1    -> Color(0xFF4CAF50)
    2    -> Color(0xFF8BC34A)
    3    -> Color(0xFFFFC107)
    4    -> Color(0xFFFF9800)
    5    -> Color(0xFFF44336)
    else -> Color(0xFF9E9E9E)
}

private fun gameModeLabel(mode: String): String = when (mode) {
    "classic"  -> "Klassisch"
    "blitz"    -> "Blitz"
    "survival" -> "Überleben"
    "daily"    -> "Täglich"
    else       -> mode
}

private fun formatDate(iso: String): String =
    try { if (iso.length >= 10) iso.substring(0, 10) else iso }
    catch (_: Exception) { iso }

// ---------------------------------------------------------------------------
//  Main Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavHostController) {
    val app        = LocalContext.current.applicationContext as QuizVerseApp
    val db         = app.database
    val highScores by db.highScoreDao().getHighScores().collectAsState(initial = emptyList())

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        visible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Bestenliste",
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

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(animationSpec = tween(420)) +
                      slideInVertically(animationSpec = tween(420)) { it / 8 },
        ) {
            if (highScores.isEmpty()) {
                EmptyLeaderboard(
                    paddingValues = paddingValues,
                    onStartQuiz   = { navController.navigate(Screen.Category.route) },
                )
            } else {
                ScoreList(
                    scores        = highScores,
                    paddingValues = paddingValues,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyLeaderboard(
    paddingValues: PaddingValues,
    onStartQuiz: () -> Unit,
) {
    val pulseAnim = rememberInfiniteTransition(label = "trophy_pulse")
    val trophyScale by pulseAnim.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.18f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trophy_scale",
    )
    val glowAlpha by pulseAnim.animateFloat(
        initialValue  = 0.2f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    Box(
        modifier           = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment   = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier            = Modifier.padding(horizontal = 40.dp),
        ) {
            // Glowing trophy with glow halo
            Box(contentAlignment = Alignment.Center) {
                // Outer glow halo
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(GoldColor.copy(alpha = glowAlpha * 0.3f))
                        .blur(18.dp),
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(trophyScale)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(GoldColor, GoldLight, GoldDark, GoldColor),
                            ),
                            shape = CircleShape,
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GoldColor.copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint               = GoldColor,
                        modifier           = Modifier.size(64.dp),
                    )
                }
            }

            Text(
                text       = "Noch keine Ergebnisse",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text      = "Starte dein erstes Quiz und kämpfe um den Spitzenplatz!",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gradient CTA button
            Box(modifier = Modifier.fillMaxWidth(0.7f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .offset(y = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldColor.copy(alpha = 0.3f))
                        .blur(6.dp),
                )
                Button(
                    onClick        = onStartQuiz,
                    shape          = RoundedCornerShape(16.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier       = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GoldDark, GoldColor, GoldLight),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint               = Color(0xFF1A1A1A),
                                modifier           = Modifier.size(20.dp),
                            )
                            Text(
                                text       = "Quiz starten",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp,
                                color      = Color(0xFF1A1A1A),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Score list with real podium for top 3
// ---------------------------------------------------------------------------

@Composable
private fun ScoreList(
    scores: List<HighScore>,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier        = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Real podium section for top 3 (show all at once in a podium layout)
        if (scores.size >= 3) {
            item {
                PodiumSection(
                    first  = scores[0],
                    second = scores[1],
                    third  = scores[2],
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            // Less than 3 scores: show as regular podium cards
            itemsIndexed(scores.take(3)) { index, score ->
                val rank      = index + 1
                val animDelay = (index * 80).coerceAtMost(240)
                var itemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(animDelay.toLong())
                    itemVisible = true
                }
                AnimatedVisibility(
                    visible = itemVisible,
                    enter   = fadeIn(tween(360)) + slideInHorizontally(tween(360)) { -48 },
                ) {
                    PodiumScoreCard(rank = rank, score = score)
                }
            }
        }

        // Remaining ranks 4+
        val startIndex = if (scores.size >= 3) 3 else scores.size
        itemsIndexed(scores.drop(startIndex)) { index, score ->
            val rank      = startIndex + index + 1
            val animDelay = (index * 60).coerceAtMost(360)

            var itemVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(animDelay.toLong())
                itemVisible = true
            }

            AnimatedVisibility(
                visible = itemVisible,
                enter   = fadeIn(tween(360)) + slideInHorizontally(tween(360)) { -48 },
            ) {
                RegularScoreCard(rank = rank, score = score)
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Real Podium Section — 1st place higher than 2nd/3rd
// ---------------------------------------------------------------------------

@Composable
private fun PodiumSection(
    first: HighScore,
    second: HighScore,
    third: HighScore,
) {
    // Entrance animations per column
    val anim1 = remember { Animatable(0f) }
    val anim2 = remember { Animatable(0f) }
    val anim3 = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(80)
        anim2.animateTo(1f, tween(450, easing = EaseOutBack))
        anim3.animateTo(1f, tween(450, easing = EaseOutBack))
        anim1.animateTo(1f, tween(550, easing = EaseOutBack))
    }

    // Podium glow animation for 1st place
    val infiniteTransition = rememberInfiniteTransition(label = "podiumGlow")
    val goldGlow by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "goldGlow",
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Medal avatars row: 2nd | 1st (higher) | 3rd
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.Bottom,
        ) {
            // 2nd place
            PodiumAvatar(
                rank      = 2,
                score     = second,
                color     = SilverColor,
                glow      = 0f,
                modifier  = Modifier
                    .weight(1f)
                    .alpha(anim2.value)
                    .scale(anim2.value.coerceAtLeast(0.1f)),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 1st place (taller/more prominent)
            PodiumAvatar(
                rank      = 1,
                score     = first,
                color     = GoldColor,
                glow      = goldGlow,
                isFirst   = true,
                modifier  = Modifier
                    .weight(1f)
                    .alpha(anim1.value)
                    .scale(anim1.value.coerceAtLeast(0.1f)),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 3rd place
            PodiumAvatar(
                rank      = 3,
                score     = third,
                color     = BronzeColor,
                glow      = 0f,
                modifier  = Modifier
                    .weight(1f)
                    .alpha(anim3.value)
                    .scale(anim3.value.coerceAtLeast(0.1f)),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Podium platform bars
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.Bottom,
        ) {
            // 2nd podium bar
            PodiumBar(height = 56.dp, color = SilverColor, label = "2", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            // 1st podium bar (tallest)
            PodiumBar(height = 80.dp, color = GoldColor, label = "1", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            // 3rd podium bar
            PodiumBar(height = 44.dp, color = BronzeColor, label = "3", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumAvatar(
    rank: Int,
    score: HighScore,
    color: Color,
    glow: Float,
    isFirst: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val medalEmoji = when (rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }
    val avatarSize = if (isFirst) 72.dp else 56.dp

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Medal emoji badge
        Text(text = medalEmoji, fontSize = if (isFirst) 28.sp else 22.sp)

        // Avatar circle with glow
        Box(contentAlignment = Alignment.Center) {
            if (isFirst && glow > 0f) {
                Box(
                    modifier = Modifier
                        .size(avatarSize + 20.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = glow * 0.3f))
                        .blur(12.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(
                        width = if (isFirst) 2.5.dp else 2.dp,
                        brush = Brush.sweepGradient(
                            if (isFirst) listOf(GoldDark, GoldColor, GoldLight, GoldDark)
                            else listOf(color, color.copy(alpha = 0.6f), color),
                        ),
                        shape = CircleShape,
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = rank.toString(),
                    fontSize = if (isFirst) 24.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    color    = color,
                )
            }
        }

        // Score value
        Text(
            text       = "${score.score}",
            style      = if (isFirst) {
                TextStyle(
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush      = Brush.horizontalGradient(
                        listOf(GoldDark, GoldColor, GoldLight),
                    ),
                )
            } else {
                TextStyle(
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
            },
        )

        // Category label
        Text(
            text     = categoryName(score.categoryId),
            fontSize = 10.sp,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PodiumBar(
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        color.copy(alpha = 0.5f),
                    ),
                ),
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Black,
            color      = Color.White.copy(alpha = 0.85f),
            modifier   = Modifier.padding(top = 6.dp),
        )
    }
}

// ---------------------------------------------------------------------------
//  Podium card (top 3, used when < 3 total scores)
// ---------------------------------------------------------------------------

@Composable
private fun PodiumScoreCard(rank: Int, score: HighScore) {
    val (medalColor, gradientColors) = when (rank) {
        1    -> GoldColor   to listOf(GoldColor.copy(alpha = 0.18f), GoldLight.copy(alpha = 0.06f))
        2    -> SilverColor to listOf(SilverColor.copy(alpha = 0.15f), SilverColor.copy(alpha = 0.04f))
        else -> BronzeColor to listOf(BronzeColor.copy(alpha = 0.15f), BronzeColor.copy(alpha = 0.04f))
    }

    val medalEmoji    = when (rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }
    val rankTextColor = when (rank) {
        1    -> GoldDark
        2    -> Color(0xFF5A5A5A)
        else -> Color(0xFF7B4A1E)
    }

    // Glow animation for 1st place
    val infiniteTransition = rememberInfiniteTransition(label = "cardGlow$rank")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue  = if (rank == 1) 0.4f else 0.2f,
        targetValue   = if (rank == 1) 0.9f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "borderAlpha$rank",
    )

    Card(
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (rank == 1) 6.dp else 3.dp),
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = if (rank == 1) 1.5.dp else 1.dp,
                    color = medalColor.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(20.dp),
                )
                .background(Brush.horizontalGradient(colors = gradientColors)),
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Medal circle
                Box(
                    modifier         = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(medalColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = medalEmoji, fontSize = 30.sp)
                }

                // Score details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (rank == 1) {
                            Text(
                                text  = "${score.score} Pkt.",
                                style = TextStyle(
                                    fontSize   = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    brush      = Brush.horizontalGradient(
                                        listOf(GoldDark, GoldColor, GoldLight),
                                    ),
                                ),
                            )
                        } else {
                            Text(
                                text       = "${score.score} Pkt.",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 22.sp,
                                color      = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        ModeChip(label = gameModeLabel(score.gameMode))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text     = categoryName(score.categoryId),
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        DifficultyBadge(difficulty = score.difficulty)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text     = "${score.correctAnswers}/${score.questionsAnswered} richtig",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            text     = "•",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        )
                        Text(
                            text     = formatDate(score.date),
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }

                // Rank badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(medalColor.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text       = "#$rank",
                        fontWeight = FontWeight.Black,
                        fontSize   = 16.sp,
                        color      = rankTextColor,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Regular card (rank 4+) with alternating backgrounds
// ---------------------------------------------------------------------------

@Composable
private fun RegularScoreCard(rank: Int, score: HighScore) {
    // Alternating row background
    val isEven = rank % 2 == 0
    val bgColor = if (isEven) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }

    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 0.5.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(16.dp),
                )
                .background(bgColor),
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rank circle
                Box(
                    modifier         = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "$rank",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text       = "${score.score} Pkt.",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 17.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                        )
                        ModeChip(label = gameModeLabel(score.gameMode))
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text     = categoryName(score.categoryId),
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        DifficultyBadge(difficulty = score.difficulty)
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text     = "${score.correctAnswers}/${score.questionsAnswered} richtig",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                        Text(
                            text     = "•",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        )
                        Text(
                            text     = formatDate(score.date),
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Small reusable chips
// ---------------------------------------------------------------------------

@Composable
private fun DifficultyBadge(difficulty: Int) {
    val color = difficultyColor(difficulty)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text       = difficultyName(difficulty),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = color,
        )
    }
}

@Composable
private fun ModeChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.primary,
        )
    }
}
