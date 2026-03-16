package com.quizverse.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizverse.app.data.database.entities.Question
import com.quizverse.app.data.repository.QuizRepository
import com.quizverse.app.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds all UI state and business logic for an active quiz session.
 * Handles classic, blitz, and survival game modes.
 */
class QuizViewModel(private val repository: QuizRepository) : ViewModel() {

    // ---- UI state ----------------------------------------------------------

    data class QuizUiState(
        val currentQuestion: Question? = null,
        val questionIndex: Int = 0,
        val totalQuestions: Int = 0,
        val score: Int = 0,
        /** Consecutive correct answers in this session */
        val streak: Int = 0,
        /** Seconds remaining on the countdown timer */
        val timeRemaining: Int = 0,
        /** Whether the player has already submitted an answer for the current question */
        val isAnswered: Boolean = false,
        /** Index of the answer the player tapped (0–3), or -1 if none yet */
        val selectedAnswer: Int = -1,
        val isCorrect: Boolean = false,
        val showExplanation: Boolean = false,
        /** Remaining lives for survival mode (-1 = not applicable) */
        val lives: Int = -1,
        val isLoading: Boolean = false,
        val isFinished: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    // ---- Internal state ----------------------------------------------------

    private var questions: List<Question> = emptyList()
    private var timerJob: Job? = null
    private var currentDifficulty: Int = 1
    private var isSurvivalMode: Boolean = false

    // ---- Public API --------------------------------------------------------

    /**
     * Loads questions and starts the first timer.
     * @param categoryId  Category filter; use -1 for mixed categories.
     * @param difficulty  Difficulty level 1–5.
     * @param count       Number of questions to load.
     * @param survival    Whether to apply survival rules (limited lives, no skip).
     */
    fun loadQuestions(
        categoryId: Int,
        difficulty: Int,
        count: Int,
        survival: Boolean = false
    ) {
        currentDifficulty = difficulty
        isSurvivalMode = survival

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                questions = if (categoryId == 11) {
                    // "Alle Kategorien" — pull from the entire question pool
                    repository.getRandomQuestionsAllCategories(count, difficulty)
                } else {
                    repository.getRandomQuestions(categoryId, count, difficulty)
                }

                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Keine Fragen verfügbar.")
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading      = false,
                        totalQuestions = questions.size,
                        questionIndex  = 0,
                        score          = 0,
                        streak         = 0,
                        isFinished     = false,
                        lives          = if (survival) Constants.SURVIVAL_STARTING_LIVES else -1,
                        currentQuestion = questions[0]
                    )
                }
                startTimer()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    /**
     * Records the player's answer and updates score / streak / lives.
     * @param answerIndex  The tapped answer index (0 = A, 1 = B, 2 = C, 3 = D).
     */
    fun submitAnswer(answerIndex: Int) {
        val state = _uiState.value
        if (state.isAnswered || state.currentQuestion == null) return

        timerJob?.cancel()

        val question  = state.currentQuestion
        val correct   = answerIndex == question.correctAnswer
        val newScore  = state.score + if (correct) calculateScore(state) else 0
        val newStreak = if (correct) state.streak + 1 else 0
        val newLives  = if (isSurvivalMode && !correct) {
            (state.lives - 1).coerceAtLeast(0)
        } else {
            state.lives
        }

        viewModelScope.launch {
            repository.updateStreak(correct)
            if (correct) {
                val xpGained = Constants.XP_BASE * currentDifficulty
                repository.updateXpAndLevel(xpGained)
            }
        }

        _uiState.update {
            it.copy(
                isAnswered      = true,
                selectedAnswer  = answerIndex,
                isCorrect       = correct,
                showExplanation = true,
                score           = newScore,
                streak          = newStreak,
                lives           = newLives
            )
        }
    }

    /** Advances to the next question or finishes the quiz. */
    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.questionIndex + 1

        // Survival: end early if out of lives
        if (isSurvivalMode && state.lives == 0) {
            finishQuiz()
            return
        }

        if (nextIndex >= questions.size) {
            finishQuiz()
            return
        }

        _uiState.update {
            it.copy(
                questionIndex   = nextIndex,
                currentQuestion = questions[nextIndex],
                isAnswered      = false,
                selectedAnswer  = -1,
                isCorrect       = false,
                showExplanation = false,
                timeRemaining   = Constants.TIMER_DURATIONS[currentDifficulty] ?: 20
            )
        }
        startTimer()
    }

    /**
     * Skips the current question without counting it as wrong.
     * Not available in survival mode.
     */
    fun skipQuestion() {
        if (isSurvivalMode) return
        timerJob?.cancel()
        nextQuestion()
    }

    // ---- Private helpers ---------------------------------------------------

    private fun startTimer() {
        timerJob?.cancel()
        val duration = Constants.TIMER_DURATIONS[currentDifficulty] ?: 20
        _uiState.update { it.copy(timeRemaining = duration) }

        timerJob = viewModelScope.launch {
            for (remaining in duration - 1 downTo 0) {
                delay(1_000L)
                _uiState.update { it.copy(timeRemaining = remaining) }
            }
            // Timer ran out — auto-submit as wrong
            if (!_uiState.value.isAnswered) {
                submitAnswer(-1)
            }
        }
    }

    /**
     * Calculates the score for a correct answer using:
     *   base points × difficulty multiplier × time bonus × streak bonus
     */
    private fun calculateScore(state: QuizUiState): Int {
        val base        = 100.0
        val diffMult    = Constants.DIFFICULTY_MULTIPLIERS[currentDifficulty] ?: 1.0
        val maxTime     = (Constants.TIMER_DURATIONS[currentDifficulty] ?: 20).toDouble()
        val timeFraction = (state.timeRemaining / maxTime).coerceIn(0.0, 1.0)
        // Time bonus: 0–50 extra points
        val timeBonus   = timeFraction * 50.0
        // Streak bonus: +5 per consecutive correct, capped at +50
        val streakBonus = (state.streak * 5.0).coerceAtMost(50.0)

        return ((base + timeBonus + streakBonus) * diffMult).toInt()
    }

    private fun finishQuiz() {
        timerJob?.cancel()
        val state = _uiState.value

        viewModelScope.launch {
            repository.checkAndUnlockAchievements()
        }

        _uiState.update { it.copy(isFinished = true) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
