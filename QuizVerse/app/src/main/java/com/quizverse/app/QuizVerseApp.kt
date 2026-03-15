package com.quizverse.app

import android.app.Application
import com.quizverse.app.data.database.QuizDatabase

/**
 * Application class for QuizVerse.
 *
 * Responsible for app-wide initialization. The Room database is accessed
 * lazily via [QuizDatabase.getDatabase] so the file is only opened the
 * first time it is actually needed, keeping startup time low.
 */
class QuizVerseApp : Application() {

    /**
     * Lazily-initialized singleton Room database instance.
     * Use this property anywhere you need a DAO instead of calling
     * [QuizDatabase.getDatabase] directly.
     */
    val database: QuizDatabase by lazy {
        QuizDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Future: inject logging, analytics, crash reporting here
    }
}
