package com.bestjournal.app.ui.navigation

import android.net.Uri
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bestjournal.app.ui.screens.dashboard.DashboardScreen
import com.bestjournal.app.ui.screens.entrydetail.EntryDetailScreen
import com.bestjournal.app.ui.screens.journal.JournalScreen
import com.bestjournal.app.ui.screens.onboarding.OnboardingScreen
import com.bestjournal.app.ui.screens.paywall.PaywallScreen
import com.bestjournal.app.ui.screens.retrospective.RetrospectiveScreen
import com.bestjournal.app.ui.screens.settings.SettingsScreen
import com.bestjournal.app.ui.screens.splash.SplashScreen
import kotlinx.coroutines.launch

// Page order: Retrospective (0), Dashboard (1), Journal (2), Settings (3)
private val mainPages =
    listOf(
        BottomNavItem.Retrospective,
        BottomNavItem.Dashboard,
        BottomNavItem.Journal,
        BottomNavItem.Settings,
    )

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController(), initialTab: Int = 2) {
    // NavHost WITHOUT Scaffold — splash and login get full screen, no bottom bar
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            SplashScreen(
                viewModel = hiltViewModel(),
                onSplashFinished = { isOnboardingDone ->
                    val destination = if (isOnboardingDone) "main" else "onboarding"
                    navController.navigate(destination) { popUpTo("splash") { inclusive = true } }
                },
            )
        }

        composable("onboarding", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            OnboardingScreen(
                viewModel = hiltViewModel(),
                onFinished = {
                    navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                },
            )
        }

        composable("main", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            // Scaffold with bottom bar ONLY wraps the main content —
            // splash and login screens are completely isolated
            val pagerState = rememberPagerState(initialPage = initialTab) { mainPages.size }
            val coroutineScope = rememberCoroutineScope()
            val retroViewModel:
                com.bestjournal.app.ui.screens.retrospective.RetrospectiveViewModel =
                hiltViewModel()

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    BottomNavBar(
                        currentRoute = mainPages[pagerState.currentPage].route,
                        onItemClick = { item ->
                            val targetPage = mainPages.indexOf(item)
                            if (targetPage >= 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 3,
                    modifier = Modifier.padding(innerPadding),
                ) { page ->
                    when (page) {
                        0 -> RetrospectiveScreen(viewModel = retroViewModel)
                        1 ->
                            DashboardScreen(
                                viewModel = hiltViewModel(),
                                onNavigateToPaywall = { source ->
                                    navController.navigate("paywall?source=$source")
                                },
                            )
                        2 ->
                            JournalScreen(
                                viewModel = hiltViewModel(),
                                onEntryClick = { entryId, searchQuery ->
                                    val encodedQuery = Uri.encode(searchQuery)
                                    navController.navigate(
                                        "entry_detail/$entryId?searchQuery=$encodedQuery"
                                    )
                                },
                                onNavigateToPaywall = { source ->
                                    navController.navigate("paywall?source=$source")
                                },
                            )
                        3 ->
                            SettingsScreen(
                                viewModel = hiltViewModel(),
                                onSignOut = {},
                                onNavigateToPaywall = { source ->
                                    navController.navigate("paywall?source=$source")
                                },
                                // Profile change only updates Dashboard, not retrospectives
                            )
                    }
                }
            }
        }

        composable(
            "entry_detail/{entryId}?searchQuery={searchQuery}",
            arguments =
                listOf(
                    navArgument("entryId") { type = NavType.LongType },
                    navArgument("searchQuery") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) { backStackEntry ->
            val searchQuery = backStackEntry.arguments?.getString("searchQuery") ?: ""
            EntryDetailScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                searchQuery = searchQuery,
            )
        }

        composable(
            "paywall?source={source}",
            arguments =
                listOf(
                    navArgument("source") {
                        type = NavType.StringType
                        defaultValue = "limit_reached"
                    }
                ),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) {
            PaywallScreen(viewModel = hiltViewModel(), onDismiss = { navController.popBackStack() })
        }
    }
}
