package com.entropyjournal

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        setContent {
            val isDarkTheme = remember {
                mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, true))
            }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == Constants.PREF_DARK_THEME) {
                        isDarkTheme.value = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, true)
                    }
                }
                encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            EntropyJournalTheme(darkTheme = isDarkTheme.value) {
                AppNavGraph()
            }
        }
    }
}
