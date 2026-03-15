package com.quizverse.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    // Stable string key used to identify the achievement (e.g. "first_win", "streak_7")
    @PrimaryKey val id: String,
    // Localisation key for the achievement name
    val nameKey: String,
    // Localisation key for the achievement description
    val descriptionKey: String,
    // Emoji or icon identifier used in the UI
    val iconName: String,
    // Target value that must be reached to unlock this achievement
    val requiredValue: Int,
    // Player's current progress toward requiredValue
    val currentValue: Int = 0,
    // Whether the achievement has been fully unlocked
    val isUnlocked: Boolean = false,
    // ISO-8601 date string of when the achievement was unlocked; null if not yet unlocked
    val unlockedDate: String? = null,
    // Tier of the achievement: 1=Bronze, 2=Silver, 3=Gold
    val tier: Int
)
