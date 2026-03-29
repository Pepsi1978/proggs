package com.entropyjournal.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.launch

// Page order: Dashboard (0), Journal (1), Settings (2)
private val mainPages = listOf(BottomNavItem.Dashboard, BottomNavItem.Journal, BottomNavItem.Settings)

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute == "main"
    val pagerState = rememberPagerState(initialPage = 1) { mainPages.size } // start on Journal
    val coroutineScope = rememberCoroutineScope()

    // Sync pager → bottom nav (when user swipes)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { /* bottom nav reads pagerState.currentPage directly */ }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = mainPages[pagerState.currentPage].route,
                    onItemClick = { item ->
                        val targetPage = mainPages.indexOf(item)
                        if (targetPage >= 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage)
                            }
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
                        val destination = if (isSignedIn) "main" else "login"
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
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable(
                "main",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> DashboardScreen(viewModel = hiltViewModel())
                        1 -> JournalScreen(
                            viewModel = hiltViewModel(),
                            onEntryClick = { entryId ->
                                navController.navigate("entry_detail/$entryId")
                            }
                        )
                        2 -> SettingsScreen(
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
        }
    }
}
