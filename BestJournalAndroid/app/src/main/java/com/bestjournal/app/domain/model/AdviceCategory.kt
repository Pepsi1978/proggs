package com.bestjournal.app.domain.model

enum class AdviceCategory(
    val displayName: String,
    val iconName: String,
    val colorHex: String
) {
    SLEEP("Schlaf", "bedtime", "#6C63FF"),
    WORK("Arbeit", "work", "#FF6B6B"),
    FITNESS("Fitness", "fitness_center", "#4ECDC4"),
    NUTRITION("Ernährung", "restaurant", "#FFE66D"),
    MENTAL_HEALTH("Mentale Gesundheit", "psychology", "#A78BFA"),
    RELATIONSHIPS("Beziehungen", "people", "#F472B6"),
    HOME("Zuhause", "home", "#34D399"),
    PERSONAL_GROWTH("Persönliche Entwicklung", "trending_up", "#60A5FA");

    companion object {
        fun fromName(name: String): AdviceCategory? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }
    }
}
