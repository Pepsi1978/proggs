package com.quizverse.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // Identifier for the game mode (e.g. "classic", "blitz", "survival", "daily")
    val gameMode: String,
    // Category the score belongs to; null means all-category/mixed run
    val categoryId: Int? = null,
    // Final score achieved in this session
    val score: Int,
    // ISO-8601 date string when the score was achieved
    val date: String,
    // How many questions were presented during this session
    val questionsAnswered: Int,
    // How many of those questions were answered correctly
    val correctAnswers: Int,
    // Difficulty level used for this session (1=Easy … 5=Master)
    val difficulty: Int
)
