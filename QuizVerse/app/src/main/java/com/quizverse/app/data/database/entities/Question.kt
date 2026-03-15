package com.quizverse.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val questionText: String,
    val answerA: String,
    val answerB: String,
    val answerC: String,
    val answerD: String,
    val correctAnswer: Int, // 0=A, 1=B, 2=C, 3=D
    val explanation: String,
    val difficulty: Int, // 1=Easy, 2=Medium, 3=Hard, 4=Expert, 5=Master
    val funFact: String? = null,
    val timesAnswered: Int = 0,
    val timesCorrect: Int = 0
)
