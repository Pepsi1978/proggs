package com.quizverse.app.data.repository

import com.quizverse.app.data.database.QuizDatabase
import com.quizverse.app.data.database.entities.Achievement
import com.quizverse.app.data.database.entities.Category
import com.quizverse.app.data.database.entities.HighScore
import com.quizverse.app.data.database.entities.Question
import com.quizverse.app.data.database.entities.UserProgress
import com.quizverse.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Single source of truth for all data access in QuizVerse.
 * Wraps the Room DAOs and adds business-logic helpers (XP curve, streak logic, etc.).
 */
class QuizRepository(private val database: QuizDatabase) {

    // ---- Convenience DAO references ----------------------------------------

    private val questionDao   get() = database.questionDao()
    private val progressDao   get() = database.progressDao()
    private val highScoreDao  get() = database.highScoreDao()
    private val achievementDao get() = database.achievementDao()

    // ---- Question access ---------------------------------------------------

    /** Returns a random list of [count] questions for [categoryId] at [difficulty]. */
    suspend fun getRandomQuestions(
        categoryId: Int,
        count: Int,
        difficulty: Int
    ): List<Question> = questionDao.getRandomQuestionsForCategory(categoryId, count, difficulty)

    /** Returns a random list of [count] questions from ALL categories at [difficulty]. */
    suspend fun getRandomQuestionsAllCategories(
        count: Int,
        difficulty: Int
    ): List<Question> = questionDao.getRandomQuestionsAllCategories(count, difficulty)

    /**
     * Returns the daily question set. Uses the current calendar day as seed so every
     * player worldwide sees the same questions on the same day.
     */
    suspend fun getDailyQuestions(count: Int): List<Question> {
        val seed = LocalDate.now().toEpochDay()
        return questionDao.getDailyQuestions(count, seed)
    }

    // ---- High scores -------------------------------------------------------

    /**
     * Persists a completed session as a high score entry.
     * The current ISO-8601 date is recorded automatically.
     */
    suspend fun saveHighScore(
        mode: String,
        categoryId: Int?,
        score: Int,
        answered: Int,
        correct: Int,
        difficulty: Int
    ) {
        val entry = HighScore(
            gameMode         = mode,
            categoryId       = categoryId,
            score            = score,
            date             = LocalDate.now().toString(),
            questionsAnswered = answered,
            correctAnswers   = correct,
            difficulty       = difficulty
        )
        highScoreDao.insert(entry)
    }

    /** Observe all high scores ordered by score descending. */
    fun getHighScores(): Flow<List<HighScore>> = highScoreDao.getHighScores()

    // ---- User progress -----------------------------------------------------

    /** Observe the single UserProgress row. Emits null when the table is empty. */
    fun getProgress(): Flow<UserProgress?> = progressDao.getProgress()

    /**
     * Awards [xpGained] XP to the player and recalculates their level.
     * If no progress row exists yet, a default one is inserted first.
     */
    suspend fun updateXpAndLevel(xpGained: Int) {
        val current = progressDao.getProgress().first()
        if (current == null) {
            val initial = UserProgress(
                totalXp      = xpGained,
                currentLevel = calculateLevel(xpGained)
            )
            progressDao.insertProgress(initial)
        } else {
            val newXp    = current.totalXp + xpGained
            val newLevel = calculateLevel(newXp)
            progressDao.updateXp(newXp, newLevel)
        }
    }

    /**
     * Updates the consecutive-correct-answer streak.
     * When [correct] is false, the streak is reset to 0.
     * Also updates the all-time best streak if surpassed.
     */
    suspend fun updateStreak(correct: Boolean) {
        val current = progressDao.getProgress().first() ?: return
        val newStreak = if (correct) current.currentStreak + 1 else 0
        val newLongest = maxOf(current.longestStreak, newStreak)
        progressDao.updateStreak(newStreak, newLongest)
    }

    /**
     * Increments the cumulative answer counters after each question.
     * Always increments [totalQuestionsAnswered]; only increments
     * [totalCorrectAnswers] when the answer was correct.
     */
    suspend fun incrementAnswerStats(correct: Boolean) {
        val current = progressDao.getProgress().first() ?: return
        val newAnswered = current.totalQuestionsAnswered + 1
        val newCorrect  = current.totalCorrectAnswers + if (correct) 1 else 0
        progressDao.updateStats(newAnswered, newCorrect, current.totalPlayTime)
    }

    // ---- Categories --------------------------------------------------------

    /**
     * Observe all categories.
     * TODO: replace stub with categoryDao.getAllCategories() once CategoryDao is added to QuizDatabase.
     * Currently returns an empty Flow to avoid a compile error; wire up the real DAO later.
     */
    fun getAllCategories(): Flow<List<Category>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    // ---- Achievements ------------------------------------------------------

    /** Observe the full achievement list. */
    fun getAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    /**
     * Compares current [UserProgress] against each achievement's [requiredValue] and
     * unlocks any achievement whose threshold has been crossed but is not yet unlocked.
     */
    suspend fun checkAndUnlockAchievements() {
        val progress     = progressDao.getProgress().first() ?: return
        val achievements = achievementDao.getAllAchievements().first()
        val today        = LocalDate.now().toString()

        for (achievement in achievements) {
            if (achievement.isUnlocked) continue

            // Map achievement id prefix to the relevant progress value
            val currentValue: Int = when {
                achievement.id.startsWith("quiz_")      -> progress.totalQuestionsAnswered
                achievement.id.startsWith("first_")     -> progress.totalQuestionsAnswered
                achievement.id.startsWith("correct_")   -> progress.totalCorrectAnswers
                achievement.id.startsWith("streak_")    -> progress.longestStreak
                achievement.id.startsWith("level_")     -> progress.currentLevel
                achievement.id.startsWith("daily_")     -> progress.dailyChallengeStreak
                achievement.id.startsWith("xp_")        -> progress.totalXp
                achievement.id.startsWith("speed_")     -> progress.totalCorrectAnswers // approximation
                achievement.id.startsWith("hard_")      -> progress.totalCorrectAnswers
                achievement.id.startsWith("master_")    -> progress.totalCorrectAnswers
                achievement.id.startsWith("perfect_")   -> progress.totalCorrectAnswers
                achievement.id.startsWith("marathon_")  -> progress.totalQuestionsAnswered
                achievement.id.startsWith("playtime_")  -> (progress.totalPlayTime / 60_000).toInt() // ms to minutes
                achievement.id.startsWith("cat_")       -> progress.totalQuestionsAnswered // per-category tracking would need separate counters
                achievement.id.startsWith("night_")     -> 0 // checked at runtime, not via progress
                achievement.id.startsWith("early_")     -> 0 // checked at runtime, not via progress
                achievement.id.startsWith("no_hint")    -> progress.totalQuestionsAnswered
                achievement.id.startsWith("comeback")   -> progress.longestStreak
                achievement.id.startsWith("quick_")     -> progress.totalQuestionsAnswered
                else                                     -> 0
            }

            // Always update the progress counter
            achievementDao.updateProgress(achievement.id, currentValue)

            // Unlock if threshold met
            if (currentValue >= achievement.requiredValue) {
                achievementDao.unlockAchievement(achievement.id, today)
            }
        }
    }

    // ---- Daily challenge ---------------------------------------------------

    /**
     * Records a daily challenge completion.
     * Updates the last-played date and increments the daily-challenge streak.
     *
     * @param date    ISO-8601 date string for today (e.g. "2026-03-15").
     * @param streak  The new daily-challenge streak value after completion.
     */
    suspend fun saveDailyChallengeCompletion(date: String, streak: Int) {
        progressDao.updateDailyChallenge(streak = streak, date = date)
    }

    // ---- XP / Level helpers ------------------------------------------------

    /**
     * Calculates the player level from cumulative [totalXp].
     *
     * Formula uses an exponential curve:
     *   level = floor( sqrt( totalXp / 50 ) ) + 1
     *
     * This produces:
     *   Level  1 →   0 XP
     *   Level  2 →  50 XP
     *   Level  5 → 800 XP
     *   Level 10 → 4 500 XP
     */
    fun calculateLevel(totalXp: Int): Int =
        floor(sqrt(totalXp / 50.0)).toInt() + 1

    /**
     * Returns the total XP required to reach [level].
     * Inverse of [calculateLevel]:  xp = (level - 1)^2 * 50
     */
    fun xpForLevel(level: Int): Int =
        ((level - 1).toDouble().pow(2.0) * 50.0).toInt()
}
