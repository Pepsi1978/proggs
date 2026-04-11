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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.entropyjournal.ui.screens.dashboard.DashboardScreen
import com.entropyjournal.ui.screens.entrydetail.EntryDetailScreen
import com.entropyjournal.ui.screens.journal.JournalScreen
import com.entropyjournal.ui.screens.retrospective.RetrospectiveScreen
import com.entropyjournal.ui.screens.settings.SettingsScreen
import com.entropyjournal.ui.screens.splash.SplashScreen
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
                onSplashFinished = {
                    navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                },
            )
        }

        composable("main", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            val pagerState = rememberPagerState(initialPage = initialTab) { mainPages.size }
            val coroutineScope = rememberCoroutineScope()
            val retroViewModel: com.entropyjournal.ui.screens.retrospective.RetrospectiveViewModel =
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
                        1 -> DashboardScreen(viewModel = hiltViewModel())
                        2 ->
                            JournalScreen(
                                viewModel = hiltViewModel(),
                                onEntryClick = { entryId, query ->
                                    navController.navigate("entry_detail/$entryId?q=$query")
                                },
                            )
                        3 -> SettingsScreen(viewModel = hiltViewModel(), onSignOut = {})
                    }
                }
            }
        }

        composable(
            "entry_detail/{entryId}?q={searchQuery}",
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
            EntryDetailScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                searchQuery = backStackEntry.arguments?.getString("searchQuery") ?: "",
            )
        }
    }
}
