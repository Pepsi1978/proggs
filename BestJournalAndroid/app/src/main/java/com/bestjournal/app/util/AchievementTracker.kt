package com.bestjournal.app.util

import android.content.Context
import android.content.SharedPreferences
import com.bestjournal.app.R
import javax.inject.Inject
import javax.inject.Singleton

data class Achievement(
    val id: String,
    val titleResId: Int,
    val descResId: Int,
    val iconName: String,
    val unlockedAt: Long? = null,
)

data class AchievementStats(
    val totalEntries: Int = 0,
    val nightEntries: Int = 0,
    val morningEntries: Int = 0,
    val longestEntryWords: Int = 0,
    val currentStreak: Int = 0,
    val photoCount: Int = 0,
    val voiceEntries: Int = 0,
    val dashboardRefreshes: Int = 0,
)

@Singleton
class AchievementTracker @Inject constructor(
    private val prefs: SharedPreferences,
    private val analyticsTracker: AnalyticsTracker,
) {
    companion object {
        private const val KEY_PREFIX = "achievement_unlocked_"

        val ALL_ACHIEVEMENTS = listOf(
            Achievement(
                id = "nachtfalter",
                titleResId = R.string.achievement_nightowl_title,
                descResId = R.string.achievement_nightowl_desc,
                iconName = "Bedtime",
            ),
            Achievement(
                id = "fruehaufsteher",
                titleResId = R.string.achievement_earlybird_title,
                descResId = R.string.achievement_earlybird_desc,
                iconName = "WbSunny",
            ),
            Achievement(
                id = "gestaendnis",
                titleResId = R.string.achievement_confession_title,
                descResId = R.string.achievement_confession_desc,
                iconName = "AutoStories",
            ),
            Achievement(
                id = "marathonschreiber",
                titleResId = R.string.achievement_marathon_title,
                descResId = R.string.achievement_marathon_desc,
                iconName = "EmojiEvents",
            ),
            Achievement(
                id = "wortkunstler",
                titleResId = R.string.achievement_wordartist_title,
                descResId = R.string.achievement_wordartist_desc,
                iconName = "Brush",
            ),
            Achievement(
                id = "bestaendigkeit",
                titleResId = R.string.achievement_consistency_title,
                descResId = R.string.achievement_consistency_desc,
                iconName = "Whatshot",
            ),
            Achievement(
                id = "jahreschronist",
                titleResId = R.string.achievement_chronicler_title,
                descResId = R.string.achievement_chronicler_desc,
                iconName = "CalendarMonth",
            ),
            Achievement(
                id = "fototagebuch",
                titleResId = R.string.achievement_photodiary_title,
                descResId = R.string.achievement_photodiary_desc,
                iconName = "PhotoCamera",
            ),
            Achievement(
                id = "sprachkuenstler",
                titleResId = R.string.achievement_voiceartist_title,
                descResId = R.string.achievement_voiceartist_desc,
                iconName = "Mic",
            ),
            Achievement(
                id = "reflexionsmeister",
                titleResId = R.string.achievement_reflectionmaster_title,
                descResId = R.string.achievement_reflectionmaster_desc,
                iconName = "Psychology",
            ),
        )
    }

    /**
     * Check all achievements against current stats. Returns list of newly unlocked achievement IDs.
     */
    fun checkAchievements(stats: AchievementStats): List<String> {
        val newlyUnlocked = mutableListOf<String>()
        val now = System.currentTimeMillis()

        val checks = mapOf(
            "nachtfalter" to (stats.nightEntries >= 50),
            "fruehaufsteher" to (stats.morningEntries >= 20),
            "gestaendnis" to (stats.longestEntryWords >= 1000),
            "marathonschreiber" to (stats.totalEntries >= 100),
            "wortkunstler" to (stats.totalEntries >= 500),
            "bestaendigkeit" to (stats.currentStreak >= 30),
            "jahreschronist" to (stats.totalEntries >= 365),
            "fototagebuch" to (stats.photoCount >= 50),
            "sprachkuenstler" to (stats.voiceEntries >= 25),
            "reflexionsmeister" to (stats.dashboardRefreshes >= 50),
        )

        for ((id, condition) in checks) {
            if (condition && !isUnlocked(id)) {
                prefs.edit().putLong(KEY_PREFIX + id, now).apply()
                analyticsTracker.trackAchievementUnlocked(id)
                newlyUnlocked.add(id)
            }
        }

        return newlyUnlocked
    }

    fun isUnlocked(id: String): Boolean = prefs.getLong(KEY_PREFIX + id, 0L) > 0L

    fun getUnlockTimestamp(id: String): Long? {
        val ts = prefs.getLong(KEY_PREFIX + id, 0L)
        return if (ts > 0L) ts else null
    }

    fun getAchievementsWithStatus(): List<Achievement> {
        return ALL_ACHIEVEMENTS.map { achievement ->
            achievement.copy(unlockedAt = getUnlockTimestamp(achievement.id))
        }
    }

    fun getUnlockedCount(): Int = ALL_ACHIEVEMENTS.count { isUnlocked(it.id) }

    fun getTitle(context: Context, id: String): String =
        ALL_ACHIEVEMENTS.firstOrNull { it.id == id }
            ?.let { context.getString(it.titleResId) }
            ?: id
}
