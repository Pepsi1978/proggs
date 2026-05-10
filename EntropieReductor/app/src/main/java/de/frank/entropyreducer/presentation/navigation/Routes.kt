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

    // Oura-Ring-Detail-Screen pro Metrik (Frank-Wunsch 2026-05-10).
    // Pfad: biomarker/oura/{metricKey} mit metricKey aus
    // OuraMetricKey.READINESS / SLEEP_SCORE / ACTIVITY / RESILIENCE.
    const val OURA_DETAIL_PATTERN = "biomarker/oura/{metricKey}"
    fun ouraDetail(metricKey: String): String = "biomarker/oura/$metricKey"

    // Health-Connect-Detail-Screen pro Body-Composition-Metrik (Frank-Wunsch 2026-05-10).
    // Pfad: biomarker/healthconnect/{metricKey} mit metricKey aus
    // HealthConnectMetricKey.WEIGHT / BODY_FAT / LEAN_BODY_MASS / BODY_WATER / BONE_MASS.
    const val HC_DETAIL_PATTERN = "biomarker/healthconnect/{metricKey}"
    fun healthConnectDetail(metricKey: String): String = "biomarker/healthconnect/$metricKey"

    // Amazfit T-Rex 3 Sport-Bereich (Frank-Wunsch 2026-05-09).
    // Pfade unter Biomarker erreichbar via "Alle anzeigen" oder Klick auf Workout-Zeile.
    const val AMAZFIT_TRAININGS = "amazfit/trainings"
    const val AMAZFIT_TRAINING_DETAIL_PATTERN = "amazfit/trainings/{trackId}"
    fun amazfitTrainingDetail(trackId: String): String = "amazfit/trainings/$trackId"

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
    const val SETTINGS_ARCHIVE = "settings/archive"
}
