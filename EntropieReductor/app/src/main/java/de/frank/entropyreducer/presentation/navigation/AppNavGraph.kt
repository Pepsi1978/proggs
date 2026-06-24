package de.frank.entropyreducer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.frank.entropyreducer.presentation.amazfit.AmazfitTrainingDetailScreen
import de.frank.entropyreducer.presentation.amazfit.AmazfitTrainingsScreen
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.components.LocalBelowSubTabRow
import de.frank.entropyreducer.presentation.dashboard1.TasksScreen
import de.frank.entropyreducer.presentation.dashboard2.AnalysisScreen
import de.frank.entropyreducer.presentation.dashboard3.ScientistScreen
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerDetailScreen
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerHostScreen
import de.frank.entropyreducer.presentation.dashboard4.HealthConnectDetailScreen
import de.frank.entropyreducer.presentation.dashboard4.OuraDetailScreen
import de.frank.entropyreducer.presentation.experimentcalendar.ExperimentCalendarScreen
import de.frank.entropyreducer.presentation.ideen.IdeenScreen
import de.frank.entropyreducer.presentation.insights.InsightBoardScreen
import de.frank.entropyreducer.presentation.insights.RepertoireScreen
import de.frank.entropyreducer.presentation.mental.GewohnheitBoardScreen
import de.frank.entropyreducer.presentation.mental.MentalBoardScreen
import de.frank.entropyreducer.presentation.settings.SettingsHomeScreen
import de.frank.entropyreducer.presentation.settings.api.ApiKeysScreen
import de.frank.entropyreducer.presentation.settings.codex.CodexScreen
import de.frank.entropyreducer.presentation.settings.export.ExportScreen
import de.frank.entropyreducer.presentation.settings.memory.MemoryScreen
import de.frank.entropyreducer.presentation.settings.models.ModelsScreen
import de.frank.entropyreducer.presentation.settings.profile.ProfileScreen
import de.frank.entropyreducer.presentation.settings.prompts.PromptsScreen
import de.frank.entropyreducer.presentation.tagebuch.TagebuchScreen
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlinx.coroutines.launch

/**
 * Tab-Switch OHNE State-Erhaltung (Frank-Wunsch 2026-06-24).
 *
 * Vorher wurde saveState/restoreState = true verwendet, damit beim Tab-Wechsel der
 * alte Tab-State (inkl. Sub-Pager-Position) erhalten blieb. Frank moechte aber, dass
 * beim Wechsel zwischen Hauptreitern IMMER der erste Sub-Tab (Page 0 = Hauptreiter-Name)
 * sofort angezeigt wird — ohne sichtbares Zurueckspringen von der alten Sub-Position.
 * Mit saveState/restoreState = true stellte der Pager die alte Page wieder her und
 * erst ON_RESUME scrollte sichtbar auf 0 (der Sprung war zu sehen).
 *
 * Jetzt: saveState = false + restoreState = false → der Host-Composable wird neu
 * komponiert und rememberPagerState(initialPage = 0) startet sofort bei Page 0.
 * Kein Sprung, kein Delay. Scroll-Positionen in Sub-Screens gehen bei Tab-Wechsel
 * verloren (bewusst akzeptiert — "immer Standard").
 */
private fun NavController.tabSwitch(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = false }
        launchSingleTop = false
        restoreState = false
    }
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    AppNavHost(nav = nav, modifier = modifier)
}

@Composable
private fun AppNavHost(
    nav: androidx.navigation.NavHostController,
    modifier: Modifier,
) {
    AppNavHostInner(nav = nav, modifier = modifier)
}

@Composable
private fun AppNavHostInner(nav: androidx.navigation.NavHostController, modifier: Modifier) {
    val cosmos = LocalCosmos.current

    // Mic-State fuer die BottomBar
    var micState by remember { mutableStateOf(MicState.IDLE) }
    var micActionsOpen by remember { mutableStateOf(false) }

    // Aktueller Tab fuer die BottomBar
    var currentTab by remember { mutableStateOf(Routes.TASKS) }

    val widgetLink by
        de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus.events.collectAsStateWithLifecycle()
    LaunchedEffect(widgetLink) {
        val link = widgetLink ?: return@LaunchedEffect
        when (link.action) {
            de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_SETTINGS -> {
                nav.navigate(Routes.SETTINGS_WIDGET)
                de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus.clear()
            }
            de.frank.entropyreducer.presentation.widget.WidgetIntents.ACTION_OPEN -> {
                nav.tabSwitch(Routes.TASKS)
                de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus.clear()
            }
            else -> Unit
        }
    }

    // Aktuellen Tab aus der Navigation ableiten
    LaunchedEffect(nav.currentBackStackEntry) {
        nav.currentBackStackEntry?.destination?.route?.let { route ->
            when (route) {
                Routes.TASKS, Routes.ANALYSIS, Routes.SCIENTIST, Routes.BIOMARKER -> {
                    currentTab = route
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = Routes.TASKS, modifier = Modifier.fillMaxSize()) {
            // ============================================================
            // AUFGABEN-HOST mit SubTabRow + HorizontalPager
            // ============================================================
            composable(Routes.TASKS) {
                val tabs = remember { subTabsFor(Routes.TASKS) }
                val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
                val coroutineScope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize()) {
                    SubTabRow(
                        tabs = tabs,
                        selectedIndex = pagerState.currentPage,
                        tabColor = cosmos.accentTasksSub,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CompositionLocalProvider(LocalBelowSubTabRow provides true) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            beyondViewportPageCount = tabs.size - 1,
                        ) { page ->
                            when (page) {
                                0 -> TasksScreen(
                                    onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    currentTab = Routes.TASKS,
                                    onOpenSubArea = { parent, index -> nav.navigate(Routes.subRouteFor(parent, index)) },
                                    onOpenEntryDetail = { entryId -> nav.navigate(Routes.entropyEntryDetail(entryId)) },
                                    onOpenLoopDetail = { templateId -> nav.navigate(Routes.loopTemplateDetail(templateId)) },
                                    showBottomBar = false,
                                )
                                1 -> GewohnheitBoardScreen(
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    showBottomBar = false,
                                )
                                2 -> MentalBoardScreen(
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    showBottomBar = false,
                                )
                                3 -> IdeenScreen(
                                    onBack = { nav.popBackStack() },
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    onOpenEntry = { entryId -> nav.navigate(Routes.ideeEntryDetail(entryId)) },
                                    showBottomBar = false,
                                )
                            }
                        }
                    }
                }
            }

            // ============================================================
            // ANALYSE-HOST mit SubTabRow + HorizontalPager
            // ============================================================
            composable(Routes.ANALYSIS) {
                val tabs = remember { subTabsFor(Routes.ANALYSIS) }
                val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
                val coroutineScope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize()) {
                    SubTabRow(
                        tabs = tabs,
                        selectedIndex = pagerState.currentPage,
                        tabColor = cosmos.accentAnalyse,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CompositionLocalProvider(LocalBelowSubTabRow provides true) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            beyondViewportPageCount = tabs.size - 1,
                        ) { page ->
                            when (page) {
                                0 -> AnalysisScreen(
                                    onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    currentTab = Routes.ANALYSIS,
                                    onOpenSubArea = { parent, index -> nav.navigate(Routes.subRouteFor(parent, index)) },
                                    showBottomBar = false,
                                )
                                else -> SubAreaScreen(
                                    parentTab = Routes.ANALYSIS,
                                    subIndex = page + 1,
                                    onBack = { nav.popBackStack() },
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    showBottomBar = false,
                                )
                            }
                        }
                    }
                }
            }

            // ============================================================
            // FORSCHER-HOST mit SubTabRow + HorizontalPager
            // ============================================================
            composable(Routes.SCIENTIST) {
                val tabs = remember { subTabsFor(Routes.SCIENTIST) }
                val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
                val coroutineScope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize()) {
                    SubTabRow(
                        tabs = tabs,
                        selectedIndex = pagerState.currentPage,
                        tabColor = cosmos.accentForscher,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CompositionLocalProvider(LocalBelowSubTabRow provides true) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            beyondViewportPageCount = tabs.size - 1,
                        ) { page ->
                            // Frank-Wunsch 2026-06-24: Hauptreiter "Forscher" steht ganz
                            // vorne (page 0 = ScientistScreen). Danach Entropie (Tagebuch),
                            // Thesen und Journal in ihrer logischen Reihenfolge.
                            when (page) {
                                0 -> ScientistScreen(
                                    onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    currentTab = Routes.SCIENTIST,
                                    onOpenSubArea = { parent, index -> nav.navigate(Routes.subRouteFor(parent, index)) },
                                    showBottomBar = false,
                                )
                                1 -> TagebuchScreen(
                                    onBack = { nav.popBackStack() },
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    onOpenEntry = { entryId -> nav.navigate(Routes.tagebuchEntryDetail(entryId)) },
                                    showBottomBar = false,
                                )
                                2 -> de.frank.entropyreducer.presentation.thesen.ThesenScreen(
                                    onBack = { nav.popBackStack() },
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    onOpenEntry = { entryId -> nav.navigate(Routes.thesenEntryDetail(entryId)) },
                                    showBottomBar = false,
                                )
                                3 -> de.frank.entropyreducer.presentation.journal.JournalScreen(
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    onOpenEntry = { sourceId -> nav.navigate(Routes.journalEntryDetail(sourceId)) },
                                    showBottomBar = false,
                                )
                            }
                        }
                    }
                }
            }

            // ============================================================
            // BIOMARKER-HOST mit SubTabRow + HorizontalPager
            // ============================================================
            composable(Routes.BIOMARKER) {
                val tabs = remember { subTabsFor(Routes.BIOMARKER) }
                val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
                val coroutineScope = rememberCoroutineScope()

                Column(modifier = Modifier.fillMaxSize()) {
                    SubTabRow(
                        tabs = tabs,
                        selectedIndex = pagerState.currentPage,
                        tabColor = cosmos.accentBio,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    CompositionLocalProvider(LocalBelowSubTabRow provides true) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            beyondViewportPageCount = tabs.size - 1,
                        ) { page ->
                            // Frank-Wunsch 2026-06-24: Hauptreiter "Biomarker" steht ganz
                            // vorne (page 0 = BiomarkerHostScreen). Danach die drei SubArea-
                            // Platzhalter (Schlafdetails, Trainings-Uebersicht, Bestleistungen)
                            // in ihrer logischen Reihenfolge mit subIndex 1..3.
                            when (page) {
                                0 -> BiomarkerHostScreen(
                                    onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    onOpenMetricDetail = { metricKey -> nav.navigate(Routes.biomarkerDetail(metricKey)) },
                                    onOpenTrainingDetail = { trackId -> nav.navigate(Routes.amazfitTrainingDetail(trackId)) },
                                    onOpenAllTrainings = { nav.navigate(Routes.AMAZFIT_TRAININGS) },
                                    onOpenOuraDetail = { metricKey -> nav.navigate(Routes.ouraDetail(metricKey)) },
                                    onOpenHealthConnectDetail = { metricKey -> nav.navigate(Routes.healthConnectDetail(metricKey)) },
                                    onOpenSubArea = { parent, index -> nav.navigate(Routes.subRouteFor(parent, index)) },
                                    showBottomBar = false,
                                )
                                1, 2, 3 -> SubAreaScreen(
                                    parentTab = Routes.BIOMARKER,
                                    subIndex = page,
                                    onBack = { nav.popBackStack() },
                                    onSwitchSub = { p, i -> nav.navigate(Routes.subRouteFor(p, i)) },
                                    onSwitchTab = { route -> nav.tabSwitch(route) },
                                    showBottomBar = false,
                                )
                            }
                        }
                    }
                }
            }

            // ============================================================
            // DETAIL-SCREENS (bleiben unverändert)
            // ============================================================
            composable(
                route = Routes.LOOP_TEMPLATE_DETAIL_PATTERN,
                arguments = listOf(navArgument("templateId") { type = NavType.StringType }),
            ) { backStackEntry ->
                de.frank.entropyreducer.presentation.recurring.LoopTemplateDetailScreen(
                    templateId = backStackEntry.arguments?.getString("templateId").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = Routes.ENTROPY_ENTRY_DETAIL_PATTERN,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) {
                de.frank.entropyreducer.presentation.dashboard1.detail.EntryDetailScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.TAGEBUCH_ENTRY_DETAIL_PATTERN,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) {
                de.frank.entropyreducer.presentation.tagebuch.TagebuchEntryDetailScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.IDEE_ENTRY_DETAIL_PATTERN,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) {
                de.frank.entropyreducer.presentation.ideen.IdeenEntryDetailScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.JOURNAL_ENTRY_DETAIL_PATTERN,
                arguments = listOf(navArgument("sourceId") { type = NavType.StringType }),
            ) {
                de.frank.entropyreducer.presentation.journal.JournalEntryDetailScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.THESEN_ENTRY_DETAIL_PATTERN,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
            ) {
                de.frank.entropyreducer.presentation.thesen.ThesenEntryDetailScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.AMAZFIT_TRAININGS) {
                AmazfitTrainingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDetail = { trackId -> nav.navigate(Routes.amazfitTrainingDetail(trackId)) },
                )
            }
            composable(
                route = Routes.AMAZFIT_TRAINING_DETAIL_PATTERN,
                arguments = listOf(navArgument("trackId") { type = NavType.StringType }),
            ) {
                AmazfitTrainingDetailScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = Routes.BIOMARKER_DETAIL_PATTERN,
                arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
            ) { backStackEntry ->
                val metricKey = backStackEntry.arguments?.getString("metricKey") ?: ""
                BiomarkerDetailScreen(
                    metricKey = metricKey,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = Routes.OURA_DETAIL_PATTERN,
                arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
            ) { backStackEntry ->
                val metricKey = backStackEntry.arguments?.getString("metricKey") ?: ""
                OuraDetailScreen(
                    metricKey = metricKey,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = Routes.HC_DETAIL_PATTERN,
                arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
            ) { backStackEntry ->
                val metricKey = backStackEntry.arguments?.getString("metricKey") ?: ""
                HealthConnectDetailScreen(
                    metricKey = metricKey,
                    onBack = { nav.popBackStack() },
                )
            }

            // Stage-3-Spezialansichten
            composable(Routes.EXPERIMENT_CALENDAR) {
                ExperimentCalendarScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.INSIGHT_BOARD) {
                InsightBoardScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.REPERTOIRE) {
                RepertoireScreen(
                    onBack = { nav.popBackStack() },
                    onOpenInsight = { nav.navigate(Routes.INSIGHT_BOARD) },
                )
            }

            composable(Routes.SETTINGS_HOME) {
                SettingsHomeScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { route -> nav.navigate(route) },
                )
            }
            composable(Routes.SETTINGS_API) {
                ApiKeysScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_MODELS) {
                ModelsScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_PROFILE) {
                ProfileScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_MEMORY) {
                MemoryScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_PRIORITY_MEMORY) {
                de.frank.entropyreducer.presentation.settings.prioritymemory.PriorityMemoryScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDetail = { id -> nav.navigate(Routes.priorityMemoryDetail(id)) },
                )
            }
            composable(
                route = Routes.PRIORITY_MEMORY_DETAIL_PATTERN,
                arguments = listOf(navArgument("memoryId") { type = NavType.StringType }),
            ) {
                de.frank.entropyreducer.presentation.settings.prioritymemory.PriorityMemoryDetailScreen(
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.SETTINGS_EXPORT) {
                ExportScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_ARCHIVE) {
                de.frank.entropyreducer.presentation.settings.archive.ArchiveScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_WIDGET) {
                de.frank.entropyreducer.presentation.settings.WidgetSettingsScreen(
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_DIAGNOSTICS) {
                de.frank.entropyreducer.presentation.settings.diagnostics.DiagnosticLogScreen(
                    onBack = { nav.popBackStack() }
                )
            }
        }

        // Fixierte BottomBar unten
        CosmosBottomBar(
            currentTab = currentTab,
            micState = micState,
            onTabSelected = { route ->
                currentTab = route
                nav.tabSwitch(route)
            },
            onMicClick = { micActionsOpen = !micActionsOpen },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )
    }
}
