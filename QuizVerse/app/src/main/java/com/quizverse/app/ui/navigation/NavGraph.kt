package com.quizverse.app.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.quizverse.app.ui.screens.AchievementsScreen
import com.quizverse.app.ui.screens.CategoryScreen
import com.quizverse.app.ui.screens.DailyChallengeScreen
import com.quizverse.app.ui.screens.DifficultyScreen
import com.quizverse.app.ui.screens.HomeScreen
import com.quizverse.app.ui.screens.LeaderboardScreen
import com.quizverse.app.ui.screens.ProfileScreen
import com.quizverse.app.ui.screens.QuizScreen
import com.quizverse.app.ui.screens.ResultScreen
import com.quizverse.app.ui.screens.SettingsScreen
import com.quizverse.app.ui.screens.ShopScreen

// Sealed class defining all navigation routes in the app
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Category : Screen("category")
    data object Leaderboard : Screen("leaderboard")
    data object Profile : Screen("profile")
    data object Achievements : Screen("achievements")
    data object DailyChallenge : Screen("daily_challenge")
    data object Settings : Screen("settings")
    data object Shop : Screen("shop")

    // Screen with a single categoryId argument
    data object Difficulty : Screen("difficulty/{categoryId}") {
        fun createRoute(categoryId: Int) = "difficulty/$categoryId"
    }

    // Screen with categoryId, difficulty level and question count arguments
    data object Quiz : Screen("quiz/{categoryId}/{difficulty}/{questionCount}") {
        fun createRoute(categoryId: Int, difficulty: Int, questionCount: Int) =
            "quiz/$categoryId/$difficulty/$questionCount"
    }

    // Result screen carrying score, total, correct count and categoryId
    data object Result : Screen("result/{score}/{total}/{correct}/{categoryId}") {
        fun createRoute(score: Int, total: Int, correct: Int, categoryId: Int) =
            "result/$score/$total/$correct/$categoryId"
    }
}

// Main navigation host that wires all screens together
@Composable
fun QuizVerseNavGraph(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(route = Screen.Category.route) {
            CategoryScreen(navController = navController)
        }

        composable(route = Screen.Leaderboard.route) {
            LeaderboardScreen(navController = navController)
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(route = Screen.Achievements.route) {
            AchievementsScreen(navController = navController)
        }

        composable(route = Screen.DailyChallenge.route) {
            DailyChallengeScreen(navController = navController)
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(route = Screen.Shop.route) {
            ShopScreen(navController = navController)
        }

        // Difficulty screen – receives categoryId as an Int argument
        composable(
            route = Screen.Difficulty.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
            DifficultyScreen(categoryId = categoryId, navController = navController)
        }

        // Quiz screen – receives categoryId, difficulty and questionCount as Int arguments
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.IntType },
                navArgument("difficulty") { type = NavType.IntType },
                navArgument("questionCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
            val difficulty = backStackEntry.arguments?.getInt("difficulty") ?: 1
            val questionCount = backStackEntry.arguments?.getInt("questionCount") ?: 10
            QuizScreen(
                categoryId = categoryId,
                difficulty = difficulty,
                questionCount = questionCount,
                navController = navController
            )
        }

        // Result screen – receives score, total, correct and categoryId as Int arguments
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType },
                navArgument("correct") { type = NavType.IntType },
                navArgument("categoryId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val total = backStackEntry.arguments?.getInt("total") ?: 0
            val correct = backStackEntry.arguments?.getInt("correct") ?: 0
            val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
            ResultScreen(
                score = score,
                total = total,
                correct = correct,
                categoryId = categoryId,
                navController = navController
            )
        }
    }
}
