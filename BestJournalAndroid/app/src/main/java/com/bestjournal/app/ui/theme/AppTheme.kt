package com.bestjournal.app.ui.theme

/**
 * Named app themes available in the Settings → Erscheinungsbild dropdown.
 *
 * - [Neutral]: Static color scheme (the original WarmDarkScheme + LightColorScheme). Profile
 *   selection has no effect on the look.
 * - [Profile]: Color scheme is derived from the currently selected dashboard profile (Teal / Copper
 *   / Violet / Emerald / Amber). Switching profile re-tints the whole app.
 *
 * The actual ColorScheme construction lives in [Theme.kt] — this enum is just a tag so
 * persistence + UI can refer to it.
 */
enum class AppTheme(val storageKey: String) {
    Neutral("neutral"),
    Profile("profile");

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.storageKey == key } ?: Neutral
    }
}
