package com.quizverse.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.quizverse.app.R

/**
 * Manages all in-game sound effects via Android's [SoundPool].
 * SoundPool is used for short, low-latency clips (correct, wrong, tick, etc.).
 */
class SoundManager(context: Context) {

    /** Controls whether any sounds are played. Toggle from settings. */
    var enabled: Boolean = true

    private val soundPool: SoundPool
    private var soundCorrect: Int = 0
    private var soundWrong: Int = 0
    private var soundLevelUp: Int = 0
    private var soundTick: Int = 0
    private var soundAchievement: Int = 0
    private var soundClick: Int = 0
    private var soundTimerWarning: Int = 0
    private var soundCountdownTick: Int = 0
    private var soundStreak: Int = 0
    private var soundStadium: Int = 0
    private var soundTimeUp: Int = 0
    private var soundQuizComplete: Int = 0

    // Track currently playing stadium stream so we can stop it
    private var stadiumStreamId: Int = 0

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributes)
            .build()

        // Load all sound resources
        soundCorrect = loadSafe(context, R.raw.sound_correct)
        soundWrong = loadSafe(context, R.raw.sound_wrong)
        soundLevelUp = loadSafe(context, R.raw.sound_level_up)
        soundTick = loadSafe(context, R.raw.sound_tick)
        soundAchievement = loadSafe(context, R.raw.sound_achievement)
        soundClick = loadSafe(context, R.raw.sound_click)
        soundTimerWarning = loadSafe(context, R.raw.sound_timer_warning)
        soundCountdownTick = loadSafe(context, R.raw.sound_countdown_tick)
        soundStreak = loadSafe(context, R.raw.sound_streak)
        soundStadium = loadSafe(context, R.raw.sound_stadium_ambience)
        soundTimeUp = loadSafe(context, R.raw.sound_time_up)
        soundQuizComplete = loadSafe(context, R.raw.sound_quiz_complete)
    }

    // ---- Public methods ----------------------------------------------------

    /** Played when the player selects a correct answer. */
    fun playCorrect() = play(soundCorrect)

    /** Played when the player selects a wrong answer or time runs out. */
    fun playWrong() = play(soundWrong)

    /** Played when the player levels up. */
    fun playLevelUp() = play(soundLevelUp)

    /** Played every second of the countdown timer. */
    fun playTick() = play(soundTick, volume = 0.4f)

    /** Played when an achievement is unlocked. */
    fun playAchievement() = play(soundAchievement)

    /** Played on button taps and navigation actions. */
    fun playClick() = play(soundClick, volume = 0.6f)

    /** Played as countdown tick with adjustable volume (gets louder as time runs out). */
    fun playCountdownTick(volume: Float = 0.5f) = play(soundCountdownTick, volume = volume.coerceIn(0.1f, 1.0f))

    /** Played when a correct-answer streak is achieved. */
    fun playStreak() = play(soundStreak, volume = 0.7f)

    /** Played when time runs out. */
    fun playTimeUp() = play(soundTimeUp)

    /** Played when a quiz is completed (victory jingle). */
    fun playQuizComplete() = play(soundQuizComplete)

    /** Start looping stadium ambience in the background. */
    fun startStadiumAmbience(volume: Float = 0.25f) {
        if (!enabled || soundStadium == 0) return
        // Loop = -1 means infinite loop
        stadiumStreamId = soundPool.play(soundStadium, volume, volume, 1, -1, 1.0f)
    }

    /** Stop the stadium ambience loop. */
    fun stopStadiumAmbience() {
        if (stadiumStreamId != 0) {
            soundPool.stop(stadiumStreamId)
            stadiumStreamId = 0
        }
    }

    /** Releases all SoundPool resources. Call from the Application's onTerminate. */
    fun release() {
        stopStadiumAmbience()
        soundPool.release()
    }

    // ---- Private helpers --------------------------------------------------

    private fun play(soundId: Int, volume: Float = 1.0f) {
        if (!enabled || soundId == 0) return
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    private fun loadSafe(context: Context, resId: Int): Int {
        return try {
            soundPool.load(context, resId, 1)
        } catch (e: Exception) {
            0
        }
    }
}
