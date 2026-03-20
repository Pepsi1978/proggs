package com.quizverse.app.util

/**
 * Application-wide constants for QuizVerse.
 * Centralises all magic numbers, string keys, and lookup tables.
 */
object Constants {

    // --- Timer durations in seconds per difficulty level ---
    val TIMER_DURATIONS: Map<Int, Int> = mapOf(
        1 to 60,
        2 to 40,
        3 to 30,
        4 to 20,
        5 to 16
    )

    // --- Score multipliers per difficulty level ---
    val DIFFICULTY_MULTIPLIERS: Map<Int, Double> = mapOf(
        1 to 1.0,
        2 to 1.5,
        3 to 2.0,
        4 to 3.0,
        5 to 5.0
    )

    // --- Human-readable German difficulty names ---
    val DIFFICULTY_NAMES: Map<Int, String> = mapOf(
        1 to "Leicht",
        2 to "Mittel",
        3 to "Schwer",
        4 to "Experte",
        5 to "Meister"
    )

    // --- Minimum player level required to unlock each difficulty ---
    val DIFFICULTY_UNLOCK_LEVELS: Map<Int, Int> = mapOf(
        1 to 0,
        2 to 5,
        3 to 15,
        4 to 30,
        5 to 50
    )

    // --- Minimum player level required to unlock each game mode ---
    val GAME_MODE_UNLOCK_LEVELS: Map<String, Int> = mapOf(
        "classic"   to 0,
        "survival"  to 8,
        "time_race" to 12,
        "duel"      to 3,
        "marathon"  to 20,
        "daily"     to 0
    )

    // --- Base XP awarded per correct answer (before multipliers) ---
    const val XP_BASE: Int = 10

    // --- Number of questions per daily challenge ---
    const val DAILY_QUESTION_COUNT: Int = 15

    // --- Default number of questions for a standard quiz session ---
    const val DEFAULT_QUESTION_COUNT: Int = 10

    // --- How many quiz rounds between interstitial ads ---
    const val INTERSTITIAL_ROUND_INTERVAL: Int = 5

    // --- Starting lives for survival mode ---
    const val SURVIVAL_STARTING_LIVES: Int = 3

    // --- German level title strings mapped to inclusive level ranges (stored as Pair<min, max>) ---
    // Key format: "min-max", value: title
    val LEVEL_TITLES: Map<IntRange, String> = mapOf(
        (1..4)   to "Neuling",
        (5..9)   to "Lehrling",
        (10..14) to "Auszubildender",
        (15..19) to "Kenner",
        (20..24) to "Fortgeschrittener",
        (25..29) to "Experte",
        (30..39) to "Meister",
        (40..49) to "Großmeister",
        (50..59) to "Champion",
        (60..74) to "Legende",
        (75..89) to "Mythos",
        (90..99) to "Titan",
        (100..Int.MAX_VALUE) to "Quiz-Gott"
    )

    /**
     * Returns the German title for the given player level.
     * Falls back to "Neuling" if no range matches.
     */
    fun titleForLevel(level: Int): String =
        LEVEL_TITLES.entries.firstOrNull { level in it.key }?.value ?: "Neuling"

    // --- Category IDs mapped to display name and emoji ---
    data class CategoryInfo(val name: String, val emoji: String)

    val CATEGORIES: Map<Int, CategoryInfo> = mapOf(
        1  to CategoryInfo("Weltgeographie",       "🌍"),
        2  to CategoryInfo("Wissenschaft & Natur", "🔬"),
        3  to CategoryInfo("Geschichte",           "📜"),
        4  to CategoryInfo("Film & Fernsehen",     "🎬"),
        5  to CategoryInfo("Musik",                "🎵"),
        6  to CategoryInfo("Sport",                "⚽"),
        7  to CategoryInfo("Technologie",          "💻"),
        8  to CategoryInfo("Essen & Trinken",      "🍳"),
        9  to CategoryInfo("Tierwelt",             "🐾"),
        10 to CategoryInfo("Sprache & Literatur",  "📚"),
        11 to CategoryInfo("Alle Kategorien",      "🌟"),
        12 to CategoryInfo("Logik & Denksport",    "🧠"),
        13 to CategoryInfo("Hertha BSC",           "⚽"),
        14 to CategoryInfo("Borussia Dortmund",    "⚽"),
        15 to CategoryInfo("Fußball",              "⚽"),
        16 to CategoryInfo("Gesundheit & Medizin", "🏥"),
        17 to CategoryInfo("Politik & Gesellschaft","🏛️")
    )
}
