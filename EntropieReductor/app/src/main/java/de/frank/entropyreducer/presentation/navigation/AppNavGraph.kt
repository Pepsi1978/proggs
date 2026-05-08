package de.frank.entropyreducer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.frank.entropyreducer.presentation.dashboard1.TasksScreen
import de.frank.entropyreducer.presentation.dashboard2.AnalysisScreen
import de.frank.entropyreducer.presentation.dashboard3.ScientistScreen
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerDetailScreen
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerHostScreen
import de.frank.entropyreducer.presentation.experimentcalendar.ExperimentCalendarScreen
import de.frank.entropyreducer.presentation.insights.InsightBoardScreen
import de.frank.entropyreducer.presentation.insights.RepertoireScreen
import de.frank.entropyreducer.presentation.settings.SettingsHomeScreen
import de.frank.entropyreducer.presentation.settings.api.ApiKeysScreen
import de.frank.entropyreducer.presentation.settings.codex.CodexScreen
import de.frank.entropyreducer.presentation.settings.export.ExportScreen
import de.frank.entropyreducer.presentation.settings.memory.MemoryScreen
import de.frank.entropyreducer.presentation.settings.models.ModelsScreen
import de.frank.entropyreducer.presentation.settings.profile.ProfileScreen
import de.frank.entropyreducer.presentation.settings.prompts.PromptsScreen

/**
 * Tab-Switch mit State-Erhaltung (Frank-Wunsch 2026-05-09 Performance):
 * - popUpTo(start) { saveState = true } speichert den State des Tabs den wir verlassen
 * - launchSingleTop verhindert mehrfache Composable-Instanzen des Ziel-Tabs
 * - restoreState = true holt den gespeicherten State des Ziel-Tabs zurueck
 *
 * Effekt: ViewModels, Scroll-Position, Filter, Suchanfragen ueberleben Tab-Switches.
 * Vorher wurde bei jedem Tab-Switch alles neu initialisiert — das hat geruckelt.
 */
private fun NavController.tabSwitch(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Routes.TASKS,
        modifier = modifier,
    ) {
        composable(Routes.TASKS) {
            TasksScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                currentTab = Routes.TASKS,
            )
        }
        composable(Routes.ANALYSIS) {
            AnalysisScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                currentTab = Routes.ANALYSIS,
            )
        }
        composable(Routes.SCIENTIST) {
            ScientistScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                currentTab = Routes.SCIENTIST,
            )
        }
        composable(Routes.BIOMARKER) {
            BiomarkerHostScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                onOpenMetricDetail = { metricKey ->
                    nav.navigate(Routes.biomarkerDetail(metricKey))
                },
            )
        }
        composable(
            route = Routes.BIOMARKER_DETAIL_PATTERN,
            arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
        ) { backStackEntry ->
            val metricKey = backStackEntry.arguments?.getString("metricKey") ?: ""
            BiomarkerDetailScreen(
                metricKey = metricKey,
                onBack = { nav.popBackStack(); Unit },
            )
        }

        // Stage-3-Spezialansichten
        composable(Routes.EXPERIMENT_CALENDAR) {
            ExperimentCalendarScreen(onBack = { nav.popBackStack(); Unit })
        }
        composable(Routes.INSIGHT_BOARD) {
            InsightBoardScreen(onBack = { nav.popBackStack(); Unit })
        }
        composable(Routes.REPERTOIRE) {
            RepertoireScreen(
                onBack = { nav.popBackStack(); Unit },
                onOpenInsight = { nav.navigate(Routes.INSIGHT_BOARD) },
            )
        }

        composable(Routes.SETTINGS_HOME) {
            SettingsHomeScreen(
                onBack = { nav.popBackStack(); Unit },
                onOpen = { route -> nav.navigate(route) },
            )
        }
        composable(Routes.SETTINGS_API) { ApiKeysScreen(onBack = { nav.popBackStack(); Unit }) }
        composable(Routes.SETTINGS_MODELS) { ModelsScreen(onBack = { nav.popBackStack(); Unit }) }
        composable(Routes.SETTINGS_PROFILE) { ProfileScreen(onBack = { nav.popBackStack(); Unit }) }
        composable(Routes.SETTINGS_PROMPTS) { PromptsScreen(onBack = { nav.popBackStack(); Unit }) }
        composable(Routes.SETTINGS_MEMORY) { MemoryScreen(onBack = { nav.popBackStack(); Unit }) }
        composable(Routes.SETTINGS_CODEX) { CodexScreen(onBack = { nav.popBackStack(); Unit }) }
        composable(Routes.SETTINGS_EXPORT) { ExportScreen(onBack = { nav.popBackStack(); Unit }) }
        composable(Routes.SETTINGS_TRIGGERS) {
            de.frank.entropyreducer.presentation.settings.triggers.KiTriggersScreen(
                onBack = { nav.popBackStack(); Unit },
            )
        }
        composable(Routes.SETTINGS_ARCHIVE) {
            de.frank.entropyreducer.presentation.settings.archive.ArchiveScreen(
                onBack = { nav.popBackStack(); Unit },
            )
        }
    }
}
