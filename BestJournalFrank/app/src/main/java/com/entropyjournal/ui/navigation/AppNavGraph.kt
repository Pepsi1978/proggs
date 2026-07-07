package com.entropyjournal.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
        composable(
            "splash",
            enterTransition = { fadeIn(tween(600)) },
            exitTransition = { fadeOut(tween(300)) },
        ) {
            SplashScreen(
                viewModel = hiltViewModel(),
                onSplashFinished = {
                    navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                },
            )
        }

        composable(
            "main",
            enterTransition = { fadeIn(tween(600)) },
            exitTransition = { fadeOut(tween(400)) },
        ) {
            // Dark curtain that fades away — content appears from darkness
            val curtainAlpha = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                curtainAlpha.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }

            val pagerState = rememberPagerState(initialPage = initialTab) { mainPages.size }
            val coroutineScope = rememberCoroutineScope()
            val retroViewModel: com.entropyjournal.ui.screens.retrospective.RetrospectiveViewModel =
                hiltViewModel()

            Box(modifier = Modifier.fillMaxSize()) {
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
            // Black curtain ON TOP — fades from opaque to invisible
            if (curtainAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = curtainAlpha.value }
                        .background(Color(0xFF131313)),
                )
            }
            } // end enter-animation wrapper
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
