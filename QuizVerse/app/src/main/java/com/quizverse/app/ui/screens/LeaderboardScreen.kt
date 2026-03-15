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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.data.database.entities.HighScore
import com.quizverse.app.ui.navigation.Screen
import kotlinx.coroutines.delay

// Podium medal colors
private val GoldColor   = Color(0xFFFFD700)
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
    11   -> "Gemischt"
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
    val app = LocalContext.current.applicationContext as QuizVerseApp
    val db  = app.database
    val highScores by db.highScoreDao().getHighScores().collectAsState(initial = emptyList())

    // Delayed entrance animation
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
                        text = "Bestenliste",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor  = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(420)) +
                    slideInVertically(animationSpec = tween(420)) { it / 8 }
        ) {
            if (highScores.isEmpty()) {
                EmptyLeaderboard(
                    paddingValues = paddingValues,
                    onStartQuiz   = { navController.navigate(Screen.Category.route) }
                )
            } else {
                ScoreList(
                    scores        = highScores,
                    paddingValues = paddingValues
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
    onStartQuiz: () -> Unit
) {
    val pulseAnim = rememberInfiniteTransition(label = "trophy_pulse")
    val trophyScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue  = 1.14f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophy_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // Glowing trophy icon
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .scale(trophyScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GoldColor.copy(alpha = 0.28f),
                                GoldColor.copy(alpha = 0.04f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector      = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint             = GoldColor,
                    modifier         = Modifier.size(68.dp)
                )
            }

            Text(
                text  = "Noch keine Ergebnisse",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text  = "Starte dein erstes Quiz und kämpfe um den Spitzenplatz!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onStartQuiz,
                shape   = RoundedCornerShape(16.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector      = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier         = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = "Quiz starten",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Score list
// ---------------------------------------------------------------------------

@Composable
private fun ScoreList(
    scores: List<HighScore>,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(scores) { index, score ->
            val rank      = index + 1
            val animDelay = (index * 60).coerceAtMost(360)

            var itemVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(animDelay.toLong())
                itemVisible = true
            }

            AnimatedVisibility(
                visible = itemVisible,
                enter = fadeIn(tween(360)) +
                        slideInHorizontally(tween(360)) { -48 }
            ) {
                if (rank <= 3) {
                    PodiumScoreCard(rank = rank, score = score)
                } else {
                    RegularScoreCard(rank = rank, score = score)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Podium card (top 3) with gradient accent
// ---------------------------------------------------------------------------

@Composable
private fun PodiumScoreCard(rank: Int, score: HighScore) {
    val (medalColor, gradientColors) = when (rank) {
        1    -> GoldColor   to listOf(Color(0xFFFFFDE7), GoldColor.copy(alpha = 0.13f))
        2    -> SilverColor to listOf(Color(0xFFF5F5F5), SilverColor.copy(alpha = 0.13f))
        else -> BronzeColor to listOf(Color(0xFFFBEFE5), BronzeColor.copy(alpha = 0.13f))
    }

    val medalEmoji = when (rank) { 1 -> "🥇"; 2 -> "🥈"; else -> "🥉" }

    val rankTextColor = when (rank) {
        1    -> Color(0xFF8B6914)
        2    -> Color(0xFF5A5A5A)
        else -> Color(0xFF7B4A1E)
    }

    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors = gradientColors))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Medal circle
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(medalColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = medalEmoji, fontSize = 28.sp)
                }

                // Score details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text       = "${score.score} Pkt.",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 22.sp,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        ModeChip(label = gameModeLabel(score.gameMode))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = categoryName(score.categoryId),
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        DifficultyBadge(difficulty = score.difficulty)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = "${score.correctAnswers}/${score.questionsAnswered} richtig",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text     = "•",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                        Text(
                            text     = formatDate(score.date),
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Rank badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(medalColor.copy(alpha = 0.28f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text       = "#$rank",
                        fontWeight = FontWeight.Black,
                        fontSize   = 16.sp,
                        color      = rankTextColor
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Regular card (rank 4+)
// ---------------------------------------------------------------------------

@Composable
private fun RegularScoreCard(rank: Int, score: HighScore) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "$rank",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = "${score.score} Pkt.",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    ModeChip(label = gameModeLabel(score.gameMode))
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text     = categoryName(score.categoryId),
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    DifficultyBadge(difficulty = score.difficulty)
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text     = "${score.correctAnswers}/${score.questionsAnswered} richtig",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        text     = "•",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                    Text(
                        text     = formatDate(score.date),
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
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
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text       = difficultyName(difficulty),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = color
        )
    }
}

@Composable
private fun ModeChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}
