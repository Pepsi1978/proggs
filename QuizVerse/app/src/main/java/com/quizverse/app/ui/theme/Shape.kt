package com.quizverse.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Shared shape scale used throughout QuizVerse. */
val QuizVerseShapes = Shapes(
    // Small interactive elements: chips, text fields, small cards
    small      = RoundedCornerShape(8.dp),
    // Standard cards and dialogs
    medium     = RoundedCornerShape(16.dp),
    // Category cards, large modals
    large      = RoundedCornerShape(24.dp),
    // Bottom sheets, hero cards
    extraLarge = RoundedCornerShape(32.dp),
)
