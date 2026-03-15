package com.quizverse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quizverse.app.ui.theme.QuizVerseTheme

/** Top-level navigation destinations. */
object Routes {
    const val HOME = "home"
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Render content edge-to-edge (behind status & navigation bars).
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            QuizVerseTheme(windowSizeClass = windowSizeClass) {
                QuizVerseNavGraph()
            }
        }
    }
}

/** Root navigation graph for QuizVerse. */
@Composable
private fun QuizVerseNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomePlaceholderScreen()
        }
    }
}

/** Temporary placeholder shown until the real home screen is implemented. */
@Composable
private fun HomePlaceholderScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier        = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "QuizVerse",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
