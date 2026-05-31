package com.bestjournal.app.ui.navigation

import android.net.Uri
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
import com.bestjournal.app.ui.screens.consent.ConsentScreen
import com.bestjournal.app.ui.screens.consent.LegalDocument
import com.bestjournal.app.ui.screens.consent.LegalDocumentScreen
import com.bestjournal.app.ui.screens.dashboard.DashboardScreen
import com.bestjournal.app.ui.screens.entrydetail.EntryDetailScreen
import com.bestjournal.app.ui.screens.journal.JournalScreen
import com.bestjournal.app.ui.screens.onboarding.OnboardingScreen
import com.bestjournal.app.ui.screens.paywall.PaywallScreen
import com.bestjournal.app.ui.screens.retrospective.RetrospectiveScreen
import com.bestjournal.app.ui.screens.settings.SettingsScreen
import com.bestjournal.app.ui.screens.splash.SplashDestination
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
                onSplashFinished = { destination ->
                    val route =
                        when (destination) {
                            SplashDestination.Consent -> "consent"
                            SplashDestination.Onboarding -> "onboarding"
                            SplashDestination.Main -> "main"
                        }
                    navController.navigate(route) { popUpTo("splash") { inclusive = true } }
                },
            )
        }

        composable(
            "consent",
            enterTransition = { fadeIn(tween(600)) },
            exitTransition = { fadeOut(tween(400)) },
        ) {
            ConsentScreen(
                viewModel = hiltViewModel(),
                onOpenDocument = { doc ->
                    val route =
                        when (doc) {
                            LegalDocument.Datenschutz -> "legal/datenschutz"
                            LegalDocument.Nutzungsbedingungen -> "legal/nutzungsbedingungen"
                            LegalDocument.Impressum -> "legal/impressum"
                        }
                    navController.navigate(route) { launchSingleTop = true }
                },
                onContinue = {
                    navController.navigate("onboarding") {
                        popUpTo("consent") { inclusive = true }
                    }
                },
            )
        }

        composable(
            "legal/datenschutz",
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) {
            LegalDocumentScreen(
                document = LegalDocument.Datenschutz,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            "legal/nutzungsbedingungen",
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) {
            LegalDocumentScreen(
                document = LegalDocument.Nutzungsbedingungen,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            "legal/impressum",
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { it } + fadeOut() },
        ) {
            LegalDocumentScreen(
                document = LegalDocument.Impressum,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            "onboarding",
            enterTransition = { fadeIn(tween(600)) },
            exitTransition = { fadeOut(tween(400)) },
        ) {
            OnboardingScreen(
                viewModel = hiltViewModel(),
                onFinished = {
                    navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
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
            val retroViewModel:
                com.bestjournal.app.ui.screens.retrospective.RetrospectiveViewModel =
                hiltViewModel()

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        BottomNavBar(
                            currentRoute = mainPages[pagerState.currentPage].route,
                            onItemClick = { item ->
                                val targetPage = mainPages.indexOf(item)
                                if (targetPage >= 0 && targetPage != pagerState.currentPage) {
                                    coroutineScope.launch {
                                        // Fixed-duration tween instead of default spring —
                                        // programmatic scroll no longer tries to fling based
                                        // on distance, which eliminated the frame drop when
                                        // tapping between tabs (distance >= 1 page).
                                        pagerState.animateScrollToPage(
                                            page = targetPage,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing,
                                            ),
                                        )
                                    }
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    HorizontalPager(
                        state = pagerState,
                        // Only preload 1 neighbor page — stops infinite animations on hidden
                        // tabs from burning CPU (Retrospective reviewCta, Settings premiumCta).
                        beyondViewportPageCount = 1,
                        modifier = Modifier.padding(innerPadding),
                    ) { page ->
                        when (page) {
                            0 ->
                                RetrospectiveScreen(
                                    viewModel = retroViewModel,
                                    onNavigateToPaywall = { source ->
                                        navController.navigate("paywall?source=$source") {
                                            launchSingleTop = true
                                        }
                                    },
                                )
                            1 ->
                                DashboardScreen(
                                    viewModel = hiltViewModel(),
                                    onNavigateToPaywall = { source ->
                                        navController.navigate("paywall?source=$source") {
                                            launchSingleTop = true
                                        }
                                    },
                                )
                            2 ->
                                JournalScreen(
                                    viewModel = hiltViewModel(),
                                    onEntryClick = { entryId, searchQuery ->
                                        val encodedQuery = Uri.encode(searchQuery)
                                        navController.navigate(
                                            "entry_detail/$entryId?searchQuery=$encodedQuery"
                                        ) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToPaywall = { source ->
                                        navController.navigate("paywall?source=$source") {
                                            launchSingleTop = true
                                        }
                                    },
                                )
                            3 ->
                                SettingsScreen(
                                    viewModel = hiltViewModel(),
                                    onSignOut = {},
                                    onNavigateToPaywall = { source ->
                                        navController.navigate("paywall?source=$source") {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToLegal = { route ->
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                        }
                                    },
                                )
                        }
                    }
                }
                // Black curtain ON TOP — fades from opaque to invisible
                if (curtainAlpha.value > 0.01f) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .graphicsLayer { alpha = curtainAlpha.value }
                                .background(Color(0xFF131313))
                    )
                }
            } // end enter-animation wrapper
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
                onNavigateToPaywall = { source ->
                    navController.navigate("paywall?source=$source")
                },
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
            PaywallScreen(
                viewModel = hiltViewModel(),
                onDismiss = { navController.popBackStack() },
                onOpenTerms = { navController.navigate("legal/nutzungsbedingungen") },
            )
        }
    }
}
