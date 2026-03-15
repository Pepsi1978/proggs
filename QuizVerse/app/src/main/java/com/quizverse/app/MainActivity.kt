package com.quizverse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.navigation.QuizVerseNavGraph
import com.quizverse.app.ui.theme.QuizVerseTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Render content edge-to-edge (behind status & navigation bars).
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val navController = rememberNavController()

            QuizVerseTheme(windowSizeClass = windowSizeClass) {
                QuizVerseNavGraph(
                    navController = navController,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}
