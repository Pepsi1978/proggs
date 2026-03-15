package com.quizverse.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // Cumulative XP earned by the player
    val totalXp: Int = 0,
    // Current level derived from totalXp
    val currentLevel: Int = 1,
    // Number of consecutive days the player has played
    val currentStreak: Int = 0,
    // All-time best consecutive-day streak
    val longestStreak: Int = 0,
    // Total number of questions answered across all game modes
    val totalQuestionsAnswered: Int = 0,
    // Total number of correctly answered questions
    val totalCorrectAnswers: Int = 0,
    // Accumulated in-game play time in milliseconds
    val totalPlayTime: Long = 0L,
    // ISO-8601 date string of the most recent play session (e.g. "2026-03-15")
    val lastPlayedDate: String = "",
    // Consecutive days the daily challenge was completed
    val dailyChallengeStreak: Int = 0,
    // ISO-8601 date string of the last completed daily challenge
    val lastDailyChallengeDate: String = ""
)
