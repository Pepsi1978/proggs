package com.entropyjournal.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.entropyjournal.ui.screens.dashboard.DashboardScreen
import com.entropyjournal.ui.screens.entrydetail.EntryDetailScreen
import com.entropyjournal.ui.screens.journal.JournalScreen
import com.entropyjournal.ui.screens.login.LoginScreen
import com.entropyjournal.ui.screens.settings.SettingsScreen
import com.entropyjournal.ui.screens.splash.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("journal", "dashboard", "settings")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo("journal") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                "splash",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                SplashScreen(
                    viewModel = hiltViewModel(),
                    onSplashFinished = { isSignedIn ->
                        val destination = if (isSignedIn) "journal" else "login"
                        navController.navigate(destination) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                "login",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                LoginScreen(
                    viewModel = hiltViewModel(),
                    onLoginSuccess = {
                        navController.navigate("journal") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                "journal",
                enterTransition = { slideInHorizontally { -it / 3 } + fadeIn() },
                exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() }
            ) {
                JournalScreen(
                    viewModel = hiltViewModel(),
                    onEntryClick = { entryId ->
                        navController.navigate("entry_detail/$entryId")
                    }
                )
            }

            composable(
                "entry_detail/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                exitTransition = { slideOutHorizontally { it } + fadeOut() }
            ) {
                EntryDetailScreen(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "dashboard",
                enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                exitTransition = { slideOutHorizontally { it / 3 } + fadeOut() }
            ) {
                DashboardScreen(viewModel = hiltViewModel())
            }

            composable(
                "settings",
                enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                exitTransition = { slideOutHorizontally { it / 3 } + fadeOut() }
            ) {
                SettingsScreen(
                    viewModel = hiltViewModel(),
                    onSignOut = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
