package com.quizverse.app

import android.app.Application
import com.quizverse.app.data.database.QuizDatabase
import com.quizverse.app.data.repository.SettingsRepository
import com.quizverse.app.util.HapticManager
import com.quizverse.app.util.SoundManager

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

    /** App-wide settings persisted in SharedPreferences. */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    /** Manages all in-game sound effects via SoundPool. */
    val soundManager: SoundManager by lazy {
        SoundManager(this)
    }

    /** Manages haptic feedback (vibration) for in-game events. */
    val hapticManager: HapticManager by lazy {
        HapticManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Sync SoundManager and HapticManager enabled state with saved settings
        soundManager.enabled = settingsRepository.soundEnabled.value
        hapticManager.enabled = settingsRepository.vibrationEnabled.value
    }

    override fun onTerminate() {
        super.onTerminate()
        soundManager.release()
    }
}
