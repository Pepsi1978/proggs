package com.bestjournal.app.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
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
                title = "Nachtfalter",
                description = "50 Einträge nach 22 Uhr geschrieben",
                iconName = "Bedtime",
            ),
            Achievement(
                id = "fruehaufsteher",
                title = "Frühaufsteher",
                description = "20 Einträge vor 7 Uhr geschrieben",
                iconName = "WbSunny",
            ),
            Achievement(
                id = "gestaendnis",
                title = "Geständnis",
                description = "Einen Eintrag mit über 1.000 Wörtern verfasst",
                iconName = "AutoStories",
            ),
            Achievement(
                id = "marathonschreiber",
                title = "Marathonschreiber",
                description = "100 Tagebucheinträge geschrieben",
                iconName = "EmojiEvents",
            ),
            Achievement(
                id = "wortkunstler",
                title = "Wortkünstler",
                description = "500 Tagebucheinträge geschrieben",
                iconName = "Brush",
            ),
            Achievement(
                id = "bestaendigkeit",
                title = "Beständigkeit",
                description = "30-Tage-Streak erreicht",
                iconName = "Whatshot",
            ),
            Achievement(
                id = "jahreschronist",
                title = "Jahreschronist",
                description = "365 Einträge geschrieben",
                iconName = "CalendarMonth",
            ),
            Achievement(
                id = "fototagebuch",
                title = "Fototagebuch",
                description = "50 Fotos an Einträge angehängt",
                iconName = "PhotoCamera",
            ),
            Achievement(
                id = "sprachkuenstler",
                title = "Sprachkünstler",
                description = "25 Spracheinträge erstellt",
                iconName = "Mic",
            ),
            Achievement(
                id = "reflexionsmeister",
                title = "Reflexionsmeister",
                description = "50 Dashboard-Analysen durchgeführt",
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

    fun getTitle(id: String): String =
        ALL_ACHIEVEMENTS.firstOrNull { it.id == id }?.title ?: id
}
