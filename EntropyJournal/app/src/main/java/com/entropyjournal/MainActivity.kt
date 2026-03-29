package com.entropyjournal

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.entropyjournal.ui.navigation.AppNavGraph
import com.entropyjournal.ui.theme.EntropyJournalTheme
import com.entropyjournal.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var encryptedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val quickPrefs = getSharedPreferences("entropy_theme_quick", MODE_PRIVATE)

        setContent {
            val followSystem = remember {
                mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false))
            }
            val manualDark = remember {
                mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false))
            }
            val systemDark = isSystemInDarkTheme()

            // Listen to encrypted prefs (settings changes)
            DisposableEffect(Unit) {
                val encListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        Constants.PREF_DARK_THEME -> {
                            manualDark.value = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false)
                        }
                        Constants.PREF_THEME_FOLLOW_SYSTEM -> {
                            followSystem.value = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
                        }
                    }
                }
                encryptedPrefs.registerOnSharedPreferenceChangeListener(encListener)

                // Listen to quick toggle prefs (Journal/Dashboard icon clicks)
                val quickListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "toggle_time") {
                        val newDark = prefs.getBoolean("toggle_dark", false)
                        // Apply to encrypted prefs so settings stay in sync
                        encryptedPrefs.edit()
                            .putBoolean(Constants.PREF_DARK_THEME, newDark)
                            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
                            .apply()
                        manualDark.value = newDark
                        followSystem.value = false
                    }
                }
                quickPrefs.registerOnSharedPreferenceChangeListener(quickListener)

                onDispose {
                    encryptedPrefs.unregisterOnSharedPreferenceChangeListener(encListener)
                    quickPrefs.unregisterOnSharedPreferenceChangeListener(quickListener)
                }
            }

            val isDark = if (followSystem.value) systemDark else manualDark.value

            EntropyJournalTheme(darkTheme = isDark) {
                AppNavGraph()
            }
        }
    }
}
