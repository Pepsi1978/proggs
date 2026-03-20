package com.quizverse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.QuizVerseApp
import com.quizverse.app.data.repository.QuizRepository
import com.quizverse.app.ui.components.AnswerButton
import com.quizverse.app.ui.components.AnswerState
import com.quizverse.app.ui.components.AnimatedTimer
import com.quizverse.app.ui.components.CategoryBackground
import com.quizverse.app.ui.components.QuestionCard
import com.quizverse.app.ui.components.StreakCounter
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.ui.theme.AnimalsEnd
import com.quizverse.app.ui.theme.AnimalsStart
import com.quizverse.app.ui.theme.DortmundEnd
import com.quizverse.app.ui.theme.DortmundStart
import com.quizverse.app.ui.theme.FilmEnd
import com.quizverse.app.ui.theme.FilmStart
import com.quizverse.app.ui.theme.FoodEnd
import com.quizverse.app.ui.theme.FoodStart
import com.quizverse.app.ui.theme.GeoEnd
import com.quizverse.app.ui.theme.GeoStart
import com.quizverse.app.ui.theme.GlassBorder
import com.quizverse.app.ui.theme.GlassWhite
import com.quizverse.app.ui.theme.Gold
import com.quizverse.app.ui.theme.GoldDark
import com.quizverse.app.ui.theme.HerthaEnd
import com.quizverse.app.ui.theme.HerthaStart
import com.quizverse.app.ui.theme.HistoryEnd
import com.quizverse.app.ui.theme.HistoryStart
import com.quizverse.app.ui.theme.LiteratureEnd
import com.quizverse.app.ui.theme.LiteratureStart
import com.quizverse.app.ui.theme.LogicEnd
import com.quizverse.app.ui.theme.LogicStart
import com.quizverse.app.ui.theme.MixedEnd
import com.quizverse.app.ui.theme.MixedStart
import com.quizverse.app.ui.theme.MusicEnd
import com.quizverse.app.ui.theme.MusicStart
import com.quizverse.app.ui.theme.ScienceEnd
import com.quizverse.app.ui.theme.ScienceStart
import com.quizverse.app.ui.theme.SportEnd
import com.quizverse.app.ui.theme.SportStart
import com.quizverse.app.ui.theme.TechEnd
import com.quizverse.app.ui.theme.TechStart
import com.quizverse.app.util.Constants
import com.quizverse.app.viewmodel.QuizViewModel
import com.quizverse.app.viewmodel.QuizViewModelFactory

// Answer option labels
private val ANSWER_LABELS = listOf("A", "B", "C", "D")

// Colors for answer feedback in explanation panel
private val ColorCorrect = Color(0xFF2E7D32)
private val ColorWrong   = Color(0xFFC62828)

// ── Category info helper ──────────────────────────────────────────────────────

private data class QuizCategoryInfo(
    val name: String,
    val emoji: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private fun getCategoryInfo(categoryId: Int): QuizCategoryInfo = when (categoryId) {
    1  -> QuizCategoryInfo("Weltgeographie",      "\uD83C\uDF0D", GeoStart,        GeoEnd)
    2  -> QuizCategoryInfo("Wissenschaft",        "\uD83D\uDD2C", ScienceStart,    ScienceEnd)
    3  -> QuizCategoryInfo("Geschichte",          "\uD83D\uDCDC", HistoryStart,    HistoryEnd)
    4  -> QuizCategoryInfo("Film & Fernsehen",    "\uD83C\uDFAC", FilmStart,       FilmEnd)
    5  -> QuizCategoryInfo("Musik",               "\uD83C\uDFB5", MusicStart,      MusicEnd)
    6  -> QuizCategoryInfo("Sport",               "\u26BD",       SportStart,      SportEnd)
    7  -> QuizCategoryInfo("Technologie",         "\uD83D\uDCBB", TechStart,       TechEnd)
    8  -> QuizCategoryInfo("Essen & Trinken",     "\uD83C\uDF73", FoodStart,       FoodEnd)
    9  -> QuizCategoryInfo("Tierwelt",            "\uD83D\uDC3E", AnimalsStart,    AnimalsEnd)
    10 -> QuizCategoryInfo("Literatur",           "\uD83D\uDCDA", LiteratureStart, LiteratureEnd)
    11 -> QuizCategoryInfo("Alle Kategorien",     "\uD83C\uDF1F", MixedStart,      MixedEnd)
    12 -> QuizCategoryInfo("Logik & Denksport",   "\uD83E\uDDE0", LogicStart,      LogicEnd)
    13 -> QuizCategoryInfo("Hertha BSC",          "\uD83D\uDC99", HerthaStart,     HerthaEnd)
    14 -> QuizCategoryInfo("Borussia Dortmund",   "\uD83D\uDC9B", DortmundStart,   DortmundEnd)
    else -> QuizCategoryInfo("Quiz",              "\u2753",       MixedStart,      MixedEnd)
}

/**
 * Main quiz screen. Displays one question at a time with a countdown timer,
 * score tracker, answer buttons and an explanation panel after each answer.
 * Uses premium UI components: AnimatedTimer, QuestionCard (glassmorphism), AnswerButton (3D).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    categoryId: Int,
    difficulty: Int,
    questionCount: Int,
    navController: NavHostController
) {
    // Build ViewModel via factory so it receives the repository
    val app = LocalContext.current.applicationContext as QuizVerseApp
    val repository = QuizRepository(app.database)
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModelFactory(repository))

    val uiState by viewModel.uiState.collectAsState()

    val soundManager = app.soundManager
    val hapticManager = app.hapticManager

    // Load questions once when the screen first appears
    LaunchedEffect(Unit) {
        viewModel.loadQuestions(categoryId, difficulty, questionCount)
    }

    // Timer tick: every second, getting louder + higher + faster
    val maxTime = Constants.TIMER_DURATIONS[difficulty] ?: 40

    LaunchedEffect(uiState.timeRemaining) {
        if (uiState.isAnswered || uiState.currentQuestion == null) return@LaunchedEffect
        val remaining = uiState.timeRemaining
        if (remaining < 1 || remaining >= maxTime) return@LaunchedEffect

        // progress: 0.0 at start (full time) → 1.0 at last second
        val progress = 1.0f - remaining.toFloat() / maxTime.toFloat()

        // Volume: 0.15 at start → 1.0 at end (continuous ramp)
        val volume = (0.15f + 0.85f * progress).coerceIn(0.15f, 1.0f)

        // Pitch: 0.6 (deep) at start → 1.8 (high) at end
        val pitch = (0.6f + 1.2f * progress).coerceIn(0.5f, 2.0f)

        // Play the tick
        soundManager.playCountdownTickPitched(volume, pitch)

        // Extra ticks for speed-up effect in final seconds
        if (remaining <= 10) {
            hapticManager.vibrateWarning()
            kotlinx.coroutines.delay(300)
            soundManager.playCountdownTickPitched(volume, pitch)
            kotlinx.coroutines.delay(300)
            soundManager.playCountdownTickPitched(volume, pitch)
        } else if (remaining <= 20) {
            kotlinx.coroutines.delay(400)
            soundManager.playCountdownTickPitched(volume * 0.8f, pitch)
        }
    }

    // Auto-navigate to ResultScreen when quiz ends
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            navController.navigate(
                Screen.Result.createRoute(
                    score      = uiState.score,
                    total      = uiState.totalQuestions,
                    correct    = uiState.correctAnswers,
                    categoryId = categoryId
                )
            ) {
                popUpTo(Screen.Quiz.route) { inclusive = true }
            }
        }
    }

    val categoryInfo = remember(categoryId) { getCategoryInfo(categoryId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.totalQuestions > 0) {
                        Text(
                            text = "${categoryInfo.emoji} ${categoryInfo.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Text(text = "Quiz")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quiz beenden"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                )
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category-specific animated background
            CategoryBackground(categoryId)

            when {
                // ── Loading ──────────────────────────────────────────────────
                uiState.isLoading -> LoadingContent()

                // ── Error ────────────────────────────────────────────────────
                uiState.errorMessage != null -> ErrorContent(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.loadQuestions(categoryId, difficulty, questionCount) }
                )

                // ── Active Quiz ───────────────────────────────────────────────
                uiState.currentQuestion != null -> ActiveQuizContent(
                    uiState       = uiState,
                    maxTime       = maxTime,
                    categoryInfo  = categoryInfo,
                    difficulty    = difficulty,
                    onAnswerSelected = { index ->
                        viewModel.submitAnswer(index)
                        val question = uiState.currentQuestion
                        if (question != null) {
                            val correct = index == question.correctAnswer
                            if (correct) {
                                soundManager.playCorrect()
                            } else {
                                soundManager.playWrong()
                                hapticManager.vibrateWrong()
                            }
                        }
                    },
                    onNextQuestion = { viewModel.nextQuestion() },
                    onSkipQuestion = { viewModel.skipQuestion() }
                )
            }
        }
    }
}

// ── Loading placeholder ───────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier          = Modifier.fillMaxSize(),
        contentAlignment  = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier  = Modifier.size(56.dp),
                color     = Gold,
                strokeWidth = 4.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text  = "Fragen werden geladen…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Error state ───────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(24.dp)
        ) {
            Text(
                text      = "Fehler",
                style     = MaterialTheme.typography.headlineSmall,
                color     = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(
                    imageVector        = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Erneut versuchen")
            }
        }
    }
}

// ── Main quiz content ─────────────────────────────────────────────────────────

@Composable
private fun ActiveQuizContent(
    uiState: QuizViewModel.QuizUiState,
    maxTime: Int,
    categoryInfo: QuizCategoryInfo,
    difficulty: Int,
    onAnswerSelected: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onSkipQuestion: () -> Unit,
) {
    val question = uiState.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Timer + Score row ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Premium circular timer
            AnimatedTimer(
                remainingSeconds = uiState.timeRemaining,
                totalSeconds     = maxTime,
                size             = 72.dp,
            )

            // Score chip (glass style)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassWhite)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Gold.copy(alpha = 0.5f), GoldDark.copy(alpha = 0.2f)),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text  = "Punkte",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold.copy(alpha = 0.8f),
                    )
                    Text(
                        text       = uiState.score.toString(),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Gold,
                    )
                }
            }

            // Streak counter (premium gold look)
            if (uiState.streak > 0) {
                StreakCounter(
                    streak = uiState.streak,
                )
            }
        }

        // ── Question card (glassmorphism) ──────────────────────────────────
        QuestionCard(
            questionText   = question.questionText,
            questionNumber = uiState.questionIndex + 1,
            totalQuestions = uiState.totalQuestions,
            categoryEmoji  = categoryInfo.emoji,
            categoryName   = categoryInfo.name,
            difficulty     = difficulty,
            gradientStart  = categoryInfo.gradientStart,
            gradientEnd    = categoryInfo.gradientEnd,
            visible        = true,
        )

        // ── Answer buttons (3D premium look) ──────────────────────────────
        val shuffledIndices = remember(question.id) {
            listOf(0, 1, 2, 3).shuffled()
        }
        val answers = listOf(question.answerA, question.answerB, question.answerC, question.answerD)

        shuffledIndices.forEachIndexed { displayPos, originalIndex ->
            val answerState = when {
                !uiState.isAnswered                                      -> AnswerState.Default
                originalIndex == question.correctAnswer                  -> AnswerState.Correct
                originalIndex == uiState.selectedAnswer && !uiState.isCorrect -> AnswerState.Wrong
                uiState.selectedAnswer == originalIndex                  -> AnswerState.Selected
                else                                                     -> AnswerState.Default
            }
            AnswerButton(
                letter  = ANSWER_LABELS[displayPos],
                text    = answers[originalIndex],
                state   = answerState,
                onClick = { onAnswerSelected(originalIndex) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Explanation panel (slides in after answering) ─────────────────
        AnimatedVisibility(
            visible = uiState.showExplanation,
            enter   = expandVertically(animationSpec = tween(400)) + fadeIn(tween(400)),
            exit    = shrinkVertically() + fadeOut()
        ) {
            ExplanationPanel(
                isCorrect   = uiState.isCorrect,
                explanation = question.explanation,
                funFact     = question.funFact
            )
        }

        // ── Action button (Next / Skip) ───────────────────────────────────
        Spacer(Modifier.height(4.dp))
        if (uiState.isAnswered) {
            // Premium gradient "Next" button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Gold, GoldDark),
                        ),
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick  = onNextQuestion,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor   = Color(0xFF1A1A2E),
                    ),
                ) {
                    Text(
                        text       = "Nächste Frage",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            TextButton(
                onClick  = onSkipQuestion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text  = "Überspringen",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Explanation panel ─────────────────────────────────────────────────────────

@Composable
private fun ExplanationPanel(
    isCorrect: Boolean,
    explanation: String,
    funFact: String?
) {
    val bgColor     = if (isCorrect) ColorCorrect.copy(alpha = 0.08f) else ColorWrong.copy(alpha = 0.08f)
    val accentColor = if (isCorrect) ColorCorrect else ColorWrong
    val headerText  = if (isCorrect) "✓ Richtig!" else "✗ Leider falsch"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = bgColor),
        border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = headerText,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = accentColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Fun fact — shown only if present
            if (!funFact.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(accentColor.copy(alpha = 0.2f))
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = "💡 Fun-Fact",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = funFact,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun QuizScreenPreview() {
    QuizScreen(
        categoryId    = 1,
        difficulty    = 2,
        questionCount = 10,
        navController = rememberNavController()
    )
}
