package com.nems.app.ui.theme

import android.content.SharedPreferences
import com.nems.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global theme state — shared between MainActivity and SettingsScreen.
 * Persists the user's theme choice in SharedPreferences.
 */
object ThemeStateHolder {

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(prefs: SharedPreferences) {
        val saved = prefs.getString(Constants.PREF_DARK_THEME, "dark") ?: "dark"
        _themeMode.value = when (saved) {
            "light" -> ThemeMode.LIGHT
            "system" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode, prefs: SharedPreferences) {
        _themeMode.value = mode
        prefs.edit().putString(
            Constants.PREF_DARK_THEME,
            when (mode) {
                ThemeMode.DARK -> "dark"
                ThemeMode.LIGHT -> "light"
                ThemeMode.SYSTEM -> "system"
            },
        ).apply()
    }
}
