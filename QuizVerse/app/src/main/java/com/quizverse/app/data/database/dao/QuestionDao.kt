package com.quizverse.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quizverse.app.data.database.entities.Question
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    // --- Read operations ---

    /** Observe all questions belonging to a specific category. */
    @Query("SELECT * FROM questions WHERE categoryId = :categoryId")
    fun getQuestionsByCategory(categoryId: Int): Flow<List<Question>>

    /** Observe questions filtered by both category and difficulty. */
    @Query("SELECT * FROM questions WHERE categoryId = :categoryId AND difficulty = :difficulty")
    fun getQuestionsByCategoryAndDifficulty(categoryId: Int, difficulty: Int): Flow<List<Question>>

    /** Observe a random sample of questions from the entire pool. */
    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :count")
    fun getRandomQuestions(count: Int): Flow<List<Question>>

    /**
     * Return a reproducible random sample filtered by category and difficulty.
     * Used for classic quiz sessions where callers need a one-shot list.
     */
    @Query(
        "SELECT * FROM questions " +
        "WHERE categoryId = :categoryId AND difficulty = :difficulty " +
        "ORDER BY RANDOM() LIMIT :count"
    )
    suspend fun getRandomQuestionsForCategory(
        categoryId: Int,
        count: Int,
        difficulty: Int
    ): List<Question>

    /**
     * Return a reproducible daily selection driven by [seed].
     * SQLite's RANDOM() is seeded per connection, so we derive ordering from
     * a deterministic expression instead: (id * seed) % large_prime.
     * This guarantees every player gets the same questions on the same calendar day.
     */
    @Query(
        "SELECT * FROM questions " +
        "ORDER BY (id * :seed) % 999983 " +
        "LIMIT :count"
    )
    suspend fun getDailyQuestions(count: Int, seed: Long): List<Question>

    /** Observe the total number of questions stored in the database. */
    @Query("SELECT COUNT(*) FROM questions")
    fun getQuestionCount(): Flow<Int>

    /** Observe the number of questions available for a specific category. */
    @Query("SELECT COUNT(*) FROM questions WHERE categoryId = :categoryId")
    fun getQuestionCountByCategory(categoryId: Int): Flow<Int>

    // --- Write operations ---

    /** Bulk-insert questions; existing rows with the same primary key are replaced. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)

    /** Update the answer statistics for a single question after it has been shown. */
    @Query(
        "UPDATE questions " +
        "SET timesAnswered = :timesAnswered, timesCorrect = :timesCorrect " +
        "WHERE id = :id"
    )
    suspend fun updateQuestionStats(id: Int, timesAnswered: Int, timesCorrect: Int)
}
