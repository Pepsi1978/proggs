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
import com.entropyjournal.util.SunCalculator
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
            val followSystem = remember { mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)) }
            val followSun = remember { mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)) }
            val manualDark = remember { mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false)) }
            val sunDark = remember {
                mutableStateOf(try {
                    val lat = encryptedPrefs.getFloat(Constants.PREF_LATITUDE, 0f).toDouble()
                    val lon = encryptedPrefs.getFloat(Constants.PREF_LONGITUDE, 0f).toDouble()
                    if (lat != 0.0 && lon != 0.0) SunCalculator.isDarkNow(lat, lon) else false
                } catch (_: Exception) { false })
            }
            val systemDark = isSystemInDarkTheme()

            DisposableEffect(Unit) {
                val encListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        Constants.PREF_DARK_THEME -> manualDark.value = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false)
                        Constants.PREF_THEME_FOLLOW_SYSTEM -> followSystem.value = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
                        Constants.PREF_THEME_FOLLOW_SUN -> {
                            followSun.value = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
                            try {
                                val lat = encryptedPrefs.getFloat(Constants.PREF_LATITUDE, 0f).toDouble()
                                val lon = encryptedPrefs.getFloat(Constants.PREF_LONGITUDE, 0f).toDouble()
                                if (lat != 0.0 && lon != 0.0) sunDark.value = SunCalculator.isDarkNow(lat, lon)
                            } catch (_: Exception) {}
                        }
                    }
                }
                encryptedPrefs.registerOnSharedPreferenceChangeListener(encListener)

                val quickListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "toggle_time") {
                        val newDark = prefs.getBoolean("toggle_dark", false)
                        encryptedPrefs.edit()
                            .putBoolean(Constants.PREF_DARK_THEME, newDark)
                            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
                            .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
                            .apply()
                        manualDark.value = newDark
                        followSystem.value = false
                        followSun.value = false
                    }
                }
                quickPrefs.registerOnSharedPreferenceChangeListener(quickListener)

                onDispose {
                    encryptedPrefs.unregisterOnSharedPreferenceChangeListener(encListener)
                    quickPrefs.unregisterOnSharedPreferenceChangeListener(quickListener)
                }
            }

            val isDark = when {
                followSun.value -> sunDark.value
                followSystem.value -> systemDark
                else -> manualDark.value
            }

            EntropyJournalTheme(darkTheme = isDark) {
                AppNavGraph()
            }
        }
    }
}
