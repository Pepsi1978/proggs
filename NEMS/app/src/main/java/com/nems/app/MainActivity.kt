package com.nems.app

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nems.app.ui.navigation.NmsApp
import com.nems.app.ui.theme.NmsTheme
import com.nems.app.ui.theme.ThemeStateHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var encryptedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ThemeStateHolder.init(encryptedPrefs)

        setContent {
            val themeMode by ThemeStateHolder.themeMode.collectAsStateWithLifecycle()
            NmsTheme(themeMode = themeMode) {
                NmsApp()
            }
        }
    }
}
