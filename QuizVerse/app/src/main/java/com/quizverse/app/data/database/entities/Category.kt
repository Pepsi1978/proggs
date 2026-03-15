package com.quizverse.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // Display name of the category (e.g. "Science", "History")
    val name: String,
    // Emoji or icon identifier used in the UI
    val iconName: String,
    // ARGB color as Long for the gradient start (e.g. 0xFF6200EE)
    val gradientStartColor: Long,
    // ARGB color as Long for the gradient end
    val gradientEndColor: Long,
    // Total number of questions available in this category
    val questionCount: Int,
    // Whether the category requires unlocking (e.g. via XP or purchase)
    val isLocked: Boolean
)
