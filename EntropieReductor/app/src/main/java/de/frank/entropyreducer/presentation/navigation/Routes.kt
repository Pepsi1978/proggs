package de.frank.entropyreducer.presentation.navigation

/** Alle Top-Level- und Sub-Routes als String-Konstanten. */
object Routes {
    // Bottom-Nav
    const val TASKS = "tasks"
    const val ANALYSIS = "analysis"
    const val SCIENTIST = "scientist"
    const val BIOMARKER = "biomarker"

    // Stage-3-Spezialansichten (siehe Spec §14)
    const val EXPERIMENT_CALENDAR = "experiment_calendar"
    const val INSIGHT_BOARD = "insight_board"
    const val REPERTOIRE = "repertoire"

    // Biomarker-Detail-Screen pro Metrik (Frank-Wunsch 2026-05-08).
    // Pfad: biomarker/detail/{metricKey}
    const val BIOMARKER_DETAIL_PATTERN = "biomarker/detail/{metricKey}"
    fun biomarkerDetail(metricKey: String): String = "biomarker/detail/$metricKey"

    // Settings-Stack
    const val SETTINGS_HOME = "settings"
    const val SETTINGS_API = "settings/api_keys"
    const val SETTINGS_MODELS = "settings/models"
    const val SETTINGS_PROFILE = "settings/profile"
    const val SETTINGS_PROMPTS = "settings/prompts"
    const val SETTINGS_MEMORY = "settings/memory"
    const val SETTINGS_CODEX = "settings/codex"
    const val SETTINGS_EXPORT = "settings/export"
    const val SETTINGS_TRIGGERS = "settings/triggers"
}
