package com.quizverse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.quizverse.app.ui.components.CategoryBackground
import com.quizverse.app.ui.navigation.Screen
import com.quizverse.app.util.Constants
import com.quizverse.app.viewmodel.QuizViewModel
import com.quizverse.app.viewmodel.QuizViewModelFactory

// Answer option labels
private val ANSWER_LABELS = listOf("A", "B", "C", "D")

// Colors for answer button feedback
private val ColorCorrect  = Color(0xFF2E7D32)  // green-800
private val ColorWrong    = Color(0xFFC62828)  // red-800
private val ColorNeutral  = Color(0xFF455A64)  // blue-grey-700

/**
 * Main quiz screen. Displays one question at a time with a countdown timer,
 * score tracker, answer buttons and an explanation panel after each answer.
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

    // Timer tick: every second from the start, getting louder + higher + faster
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
        // Every 10s bracket gets faster: last 20s = double tick, last 10s = triple tick
        if (remaining <= 10) {
            // Haptic warning pulse in the last 10 seconds
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
            val correctCount = uiState.questionIndex // questions answered correctly is tracked via score/streak
            navController.navigate(
                Screen.Result.createRoute(
                    score      = uiState.score,
                    total      = uiState.totalQuestions,
                    correct    = correctCount,
                    categoryId = categoryId
                )
            ) {
                // Remove the quiz screen from the back-stack so the user can't go back to it
                popUpTo(Screen.Quiz.route) { inclusive = true }
            }
        }
    }

    // Determine timer progress fraction (0.0 = empty, 1.0 = full)
    val timerFraction = if (maxTime > 0) uiState.timeRemaining.toFloat() / maxTime.toFloat() else 0f

    // Animate the timer bar smoothly
    val animatedTimerFraction by animateFloatAsState(
        targetValue    = timerFraction,
        animationSpec  = tween(durationMillis = 800),
        label          = "timerProgress"
    )

    // Timer bar colour: green → yellow → red as time runs out
    val timerColor by animateColorAsState(
        targetValue = when {
            timerFraction > 0.5f -> Color(0xFF388E3C) // green
            timerFraction > 0.25f -> Color(0xFFF57F17) // amber
            else -> Color(0xFFD32F2F)                 // red
        },
        animationSpec = tween(durationMillis = 500),
        label = "timerColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.totalQuestions > 0) {
                        Text(
                            text = "Frage ${uiState.questionIndex + 1} von ${uiState.totalQuestions}",
                            style = MaterialTheme.typography.titleMedium
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
                    containerColor = MaterialTheme.colorScheme.surface
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
                    uiState             = uiState,
                    animatedTimerFraction = animatedTimerFraction,
                    timerColor          = timerColor,
                    onAnswerSelected    = { index ->
                        viewModel.submitAnswer(index)
                        // Play sound + haptic feedback based on correctness
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
                    onNextQuestion      = { viewModel.nextQuestion() },
                    onSkipQuestion      = { viewModel.skipQuestion() }
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
                color     = MaterialTheme.colorScheme.primary
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
    animatedTimerFraction: Float,
    timerColor: Color,
    onAnswerSelected: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onSkipQuestion: () -> Unit
) {
    val question = uiState.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Timer bar ─────────────────────────────────────────────────────
        TimerBar(
            fraction  = animatedTimerFraction,
            color     = timerColor,
            remaining = uiState.timeRemaining
        )

        // ── Score & streak row ────────────────────────────────────────────
        ScoreRow(score = uiState.score, streak = uiState.streak)

        // ── Question card ─────────────────────────────────────────────────
        QuestionCard(questionText = question.questionText)

        // ── Answer buttons (shuffled order per question) ─────────────────
        // Build a stable shuffled mapping for this question so answers appear
        // in a random order each time, but stay consistent while answering.
        val shuffledIndices = remember(question.id) {
            listOf(0, 1, 2, 3).shuffled()
        }
        val answers = listOf(question.answerA, question.answerB, question.answerC, question.answerD)
        shuffledIndices.forEachIndexed { displayPos, originalIndex ->
            AnswerButton(
                label          = ANSWER_LABELS[displayPos],
                text           = answers[originalIndex],
                isAnswered     = uiState.isAnswered,
                isSelected     = uiState.selectedAnswer == originalIndex,
                isCorrect      = question.correctAnswer == originalIndex,
                onClick        = { onAnswerSelected(originalIndex) }
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
            Button(
                onClick  = onNextQuestion,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text  = "Nächste Frage",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
            }
        } else {
            TextButton(
                onClick  = onSkipQuestion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text  = "Überspringen",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Timer bar ─────────────────────────────────────────────────────────────────

@Composable
private fun TimerBar(fraction: Float, color: Color, remaining: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text  = "Zeit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text       = "${remaining}s",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress      = { fraction },
            modifier      = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color         = color,
            trackColor    = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap     = StrokeCap.Round,
            gapSize       = 0.dp,
            drawStopIndicator = {}
        )
    }
}

// ── Score & streak row ────────────────────────────────────────────────────────

@Composable
private fun ScoreRow(score: Int, streak: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score chip
        Surface(
            shape  = RoundedCornerShape(8.dp),
            color  = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text  = "Punkte",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text       = score.toString(),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Streak chip — only shown when streak > 0, otherwise neutral
        Surface(
            shape    = RoundedCornerShape(8.dp),
            color    = if (streak > 0)
                           MaterialTheme.colorScheme.tertiaryContainer
                       else
                           MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text  = "Serie",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (streak > 0)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text       = if (streak > 0) "🔥 $streak" else "–",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (streak > 0)
                                     MaterialTheme.colorScheme.onTertiaryContainer
                                 else
                                     MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Question card ─────────────────────────────────────────────────────────────

@Composable
private fun QuestionCard(questionText: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text      = questionText,
            style     = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        )
    }
}

// ── Answer button ─────────────────────────────────────────────────────────────

@Composable
private fun AnswerButton(
    label: String,
    text: String,
    isAnswered: Boolean,
    isSelected: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit
) {
    // Decide the visual state
    val containerColor by animateColorAsState(
        targetValue = when {
            !isAnswered                        -> MaterialTheme.colorScheme.surface
            isCorrect                          -> ColorCorrect.copy(alpha = 0.15f)
            isSelected && !isCorrect           -> ColorWrong.copy(alpha = 0.15f)
            else                               -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label         = "answerBg_$label"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !isAnswered                        -> MaterialTheme.colorScheme.outline
            isCorrect                          -> ColorCorrect
            isSelected && !isCorrect           -> ColorWrong
            else                               -> ColorNeutral.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label         = "answerBorder_$label"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            !isAnswered                        -> MaterialTheme.colorScheme.onSurface
            isCorrect                          -> ColorCorrect
            isSelected && !isCorrect           -> ColorWrong
            else                               -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(300),
        label         = "answerText_$label"
    )

    OutlinedButton(
        onClick  = onClick,
        enabled  = !isAnswered,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        border   = BorderStroke(width = 1.5.dp, color = borderColor),
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor         = containerColor,
            disabledContainerColor = containerColor
        )
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label badge (A / B / C / D)
            Surface(
                shape  = RoundedCornerShape(6.dp),
                color  = borderColor.copy(alpha = 0.2f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = borderColor
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text      = text,
                style     = MaterialTheme.typography.bodyMedium,
                color     = textColor,
                modifier  = Modifier.weight(1f)
            )
        }
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
