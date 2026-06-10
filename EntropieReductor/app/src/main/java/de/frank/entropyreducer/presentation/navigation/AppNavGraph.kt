package de.frank.entropyreducer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.frank.entropyreducer.presentation.amazfit.AmazfitTrainingDetailScreen
import de.frank.entropyreducer.presentation.amazfit.AmazfitTrainingsScreen
import de.frank.entropyreducer.presentation.dashboard1.TasksScreen
import de.frank.entropyreducer.presentation.dashboard2.AnalysisScreen
import de.frank.entropyreducer.presentation.dashboard3.ScientistScreen
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerDetailScreen
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerHostScreen
import de.frank.entropyreducer.presentation.dashboard4.HealthConnectDetailScreen
import de.frank.entropyreducer.presentation.dashboard4.OuraDetailScreen
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
import de.frank.entropyreducer.presentation.tagebuch.TagebuchScreen

/**
 * Tab-Switch mit State-Erhaltung (Frank-Wunsch 2026-05-09 Performance):
 * - popUpTo(start) { saveState = true } speichert den State des Tabs den wir verlassen
 * - launchSingleTop verhindert mehrfache Composable-Instanzen des Ziel-Tabs
 * - restoreState = true holt den gespeicherten State des Ziel-Tabs zurueck
 *
 * Effekt: ViewModels, Scroll-Position, Filter, Suchanfragen ueberleben Tab-Switches. Vorher wurde
 * bei jedem Tab-Switch alles neu initialisiert — das hat geruckelt.
 */
private fun NavController.tabSwitch(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val nav = rememberNavController()

    // Frank-Wunsch 2026-05-17: Beim Erststart der App soll die 5-Tab-Uebersicht
    // sichtbar sein (nicht direkt die Sub-Bar des Aufgaben-Tabs). Der Zustand
    // muss Tab-Wechsel ueberleben, damit ein Tap auf einen Tab im Switcher die
    // Sub-Bar sofort und ohne Flackern zeigt.
    val switcherState = remember { BottomBarSwitcherState() }
    AppNavHost(nav = nav, switcherState = switcherState, modifier = modifier)
}

@Composable
private fun AppNavHost(
    nav: androidx.navigation.NavHostController,
    switcherState: BottomBarSwitcherState,
    modifier: Modifier,
) {
    CompositionLocalProvider(LocalBottomBarSwitcher provides switcherState) {
        AppNavHostInner(nav = nav, modifier = modifier)
    }
}

@Composable
private fun AppNavHostInner(nav: androidx.navigation.NavHostController, modifier: Modifier) {

    // Frank-Wunsch 2026-05-11: Settings-Icon im Widget oeffnet direkt den
    // Widget-Settings-Screen. ACTION_FOCUS / ACTION_RESCHEDULE werden in
    // TasksScreen behandelt — hier nur ACTION_SETTINGS + ACTION_OPEN.
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
                // Sicherstellen dass Tasks-Tab vorne ist (Header-Tap)
                nav.tabSwitch(Routes.TASKS)
                de.frank.entropyreducer.presentation.widget.WidgetDeepLinkBus.clear()
            }
            // ACTION_FOCUS / ACTION_RESCHEDULE → TasksScreen-LaunchedEffect kuemmert sich
            else -> Unit
        }
    }

    NavHost(navController = nav, startDestination = Routes.TASKS, modifier = modifier) {
        // Sub-Area-Navigation (Frank-Wunsch 2026-05-17): Klick auf ein Sub-Icon
        // im Sub-Mode der Bottom-Bar navigiert zum jeweiligen weissen Platzhalter.
        val onOpenSubArea: (String, Int) -> Unit = { parent, index ->
            nav.navigate(Routes.subRouteFor(parent, index))
        }

        composable(Routes.TASKS) {
            TasksScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                currentTab = Routes.TASKS,
                onOpenSubArea = onOpenSubArea,
                onOpenEntryDetail = { entryId -> nav.navigate(Routes.entropyEntryDetail(entryId)) },
                onOpenLoopDetail = { templateId ->
                    nav.navigate(Routes.loopTemplateDetail(templateId))
                },
            )
        }
        composable(
            route = Routes.LOOP_TEMPLATE_DETAIL_PATTERN,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType }),
        ) { backStackEntry ->
            de.frank.entropyreducer.presentation.recurring.LoopTemplateDetailScreen(
                templateId = backStackEntry.arguments?.getString("templateId").orEmpty(),
                onBack = {
                    nav.popBackStack()
                    Unit
                },
            )
        }
        composable(
            route = Routes.ENTROPY_ENTRY_DETAIL_PATTERN,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
        ) {
            de.frank.entropyreducer.presentation.dashboard1.detail.EntryDetailScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(
            route = Routes.TAGEBUCH_ENTRY_DETAIL_PATTERN,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
        ) {
            de.frank.entropyreducer.presentation.tagebuch.TagebuchEntryDetailScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(
            route = Routes.JOURNAL_ENTRY_DETAIL_PATTERN,
            arguments = listOf(navArgument("sourceId") { type = NavType.StringType }),
        ) {
            de.frank.entropyreducer.presentation.journal.JournalEntryDetailScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(
            route = Routes.THESEN_ENTRY_DETAIL_PATTERN,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
        ) {
            de.frank.entropyreducer.presentation.thesen.ThesenEntryDetailScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.ANALYSIS) {
            AnalysisScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                currentTab = Routes.ANALYSIS,
                onOpenSubArea = onOpenSubArea,
            )
        }
        composable(Routes.SCIENTIST) {
            ScientistScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                currentTab = Routes.SCIENTIST,
                onOpenSubArea = onOpenSubArea,
            )
        }
        composable(Routes.BIOMARKER) {
            BiomarkerHostScreen(
                onOpenSettings = { nav.navigate(Routes.SETTINGS_HOME) },
                onSwitchTab = { route -> nav.tabSwitch(route) },
                onOpenMetricDetail = { metricKey ->
                    nav.navigate(Routes.biomarkerDetail(metricKey))
                },
                onOpenTrainingDetail = { trackId ->
                    nav.navigate(Routes.amazfitTrainingDetail(trackId))
                },
                onOpenAllTrainings = { nav.navigate(Routes.AMAZFIT_TRAININGS) },
                onOpenOuraDetail = { metricKey -> nav.navigate(Routes.ouraDetail(metricKey)) },
                onOpenHealthConnectDetail = { metricKey ->
                    nav.navigate(Routes.healthConnectDetail(metricKey))
                },
                onOpenSubArea = onOpenSubArea,
            )
        }

        // Sub-Bereich-Platzhalter (4 Tabs × 3 Indizes = 12 Targets, hier ueber 4
        // Pattern-Routen mit {index}-Argument).
        val subAreaComposables =
            listOf(
                Routes.TASKS_SUB_PATTERN to Routes.TASKS,
                Routes.ANALYSIS_SUB_PATTERN to Routes.ANALYSIS,
                Routes.SCIENTIST_SUB_PATTERN to Routes.SCIENTIST,
                Routes.BIOMARKER_SUB_PATTERN to Routes.BIOMARKER,
            )
        subAreaComposables.forEach { (pattern, parent) ->
            composable(
                route = pattern,
                arguments = listOf(navArgument("index") { type = NavType.IntType }),
            ) { backStackEntry ->
                val index = backStackEntry.arguments?.getInt("index") ?: 1
                // Bugfix 2026-05-19: Klick auf das Parent-Tab-Icon vom Sub-Screen
                // aus muss VERLAESSLICH den Parent-Screen zeigen — nicht den Sub
                // wieder einblenden. Vorher: tabSwitch mit restoreState=true hat
                // bei popUpTo den gespeicherten Sub-State faelschlich wieder
                // angewendet, sodass die Entropie-Liste sichtbar blieb obwohl
                // der Benutzer auf "Aufgaben" geklickt hat. Jetzt: wenn das
                // Tab-Ziel der eigene Parent ist, einfach popBackStack zum
                // Parent — sonst regulaerer Tab-Wechsel mit State-Erhaltung.
                val onSwitchTabFromSub: (String) -> Unit = { route ->
                    if (route == parent) {
                        nav.popBackStack(parent, inclusive = false)
                    } else {
                        nav.tabSwitch(route)
                    }
                }
                // Frank-Wunsch 2026-06-10: "Journal" (read-only Spiegel der BestJournal-
                // Frank-Eintraege, eigener Screen) ist vom Aufgaben- in den Forscher-
                // Bereich umgezogen — Forscher-Slot 1 = Tagebuch ("Entropie"),
                // Forscher-Slot 2 = Thesen, Forscher-Slot 3 = Journal. Aufgaben-Slot 1
                // und Slot 3 sind leere Platzhalter (else-Zweig unten via SubAreaScreen).
                val isJournal = parent == Routes.SCIENTIST && index == 3
                // Frank-Wunsch 2026-06-09: Aufgaben-Slot 2 = Mentalboard.
                val isMental = parent == Routes.TASKS && index == 2
                val isTagebuch = parent == Routes.SCIENTIST && index == 1
                val isThesen = parent == Routes.SCIENTIST && index == 2
                if (isJournal) {
                    de.frank.entropyreducer.presentation.journal.JournalScreen(
                        onSwitchSub = { p, i ->
                            nav.navigate(Routes.subRouteFor(p, i)) {
                                popUpTo(pattern) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSwitchTab = onSwitchTabFromSub,
                        onOpenEntry = { sourceId ->
                            nav.navigate(Routes.journalEntryDetail(sourceId))
                        },
                    )
                } else if (isTagebuch) {
                    TagebuchScreen(
                        onBack = {
                            nav.popBackStack()
                            Unit
                        },
                        onSwitchSub = { p, i ->
                            nav.navigate(Routes.subRouteFor(p, i)) {
                                popUpTo(pattern) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSwitchTab = onSwitchTabFromSub,
                        onOpenEntry = { entryId ->
                            nav.navigate(Routes.tagebuchEntryDetail(entryId))
                        },
                    )
                } else if (isThesen) {
                    de.frank.entropyreducer.presentation.thesen.ThesenScreen(
                        onBack = {
                            nav.popBackStack()
                            Unit
                        },
                        onSwitchSub = { p, i ->
                            nav.navigate(Routes.subRouteFor(p, i)) {
                                popUpTo(pattern) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSwitchTab = onSwitchTabFromSub,
                        onOpenEntry = { entryId -> nav.navigate(Routes.thesenEntryDetail(entryId)) },
                    )
                } else if (isMental) {
                    de.frank.entropyreducer.presentation.mental.MentalBoardScreen(
                        onSwitchSub = { p, i ->
                            nav.navigate(Routes.subRouteFor(p, i)) {
                                popUpTo(pattern) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSwitchTab = onSwitchTabFromSub,
                    )
                } else {
                    SubAreaScreen(
                        parentTab = parent,
                        subIndex = index,
                        onBack = {
                            nav.popBackStack()
                            Unit
                        },
                        onSwitchSub = { p, i ->
                            nav.navigate(Routes.subRouteFor(p, i)) {
                                // Sub-zu-Sub-Wechsel ersetzt den aktuellen Sub-Eintrag
                                // im Back-Stack, damit "Zurueck" zuverlaessig zum Parent
                                // fuehrt — nicht durch eine Kette von Sub-Bereichen.
                                popUpTo(pattern) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSwitchTab = onSwitchTabFromSub,
                    )
                }
            }
        }
        composable(Routes.AMAZFIT_TRAININGS) {
            AmazfitTrainingsScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                },
                onOpenDetail = { trackId -> nav.navigate(Routes.amazfitTrainingDetail(trackId)) },
            )
        }
        composable(
            route = Routes.AMAZFIT_TRAINING_DETAIL_PATTERN,
            arguments = listOf(navArgument("trackId") { type = NavType.StringType }),
        ) {
            AmazfitTrainingDetailScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(
            route = Routes.BIOMARKER_DETAIL_PATTERN,
            arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
        ) { backStackEntry ->
            val metricKey = backStackEntry.arguments?.getString("metricKey") ?: ""
            BiomarkerDetailScreen(
                metricKey = metricKey,
                onBack = {
                    nav.popBackStack()
                    Unit
                },
            )
        }
        composable(
            route = Routes.OURA_DETAIL_PATTERN,
            arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
        ) { backStackEntry ->
            val metricKey = backStackEntry.arguments?.getString("metricKey") ?: ""
            OuraDetailScreen(
                metricKey = metricKey,
                onBack = {
                    nav.popBackStack()
                    Unit
                },
            )
        }
        composable(
            route = Routes.HC_DETAIL_PATTERN,
            arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
        ) { backStackEntry ->
            val metricKey = backStackEntry.arguments?.getString("metricKey") ?: ""
            HealthConnectDetailScreen(
                metricKey = metricKey,
                onBack = {
                    nav.popBackStack()
                    Unit
                },
            )
        }

        // Stage-3-Spezialansichten
        composable(Routes.EXPERIMENT_CALENDAR) {
            ExperimentCalendarScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.INSIGHT_BOARD) {
            InsightBoardScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.REPERTOIRE) {
            RepertoireScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                },
                onOpenInsight = { nav.navigate(Routes.INSIGHT_BOARD) },
            )
        }

        composable(Routes.SETTINGS_HOME) {
            SettingsHomeScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                },
                onOpen = { route -> nav.navigate(route) },
            )
        }
        composable(Routes.SETTINGS_API) {
            ApiKeysScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_MODELS) {
            ModelsScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_PROFILE) {
            ProfileScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_PROMPTS) {
            PromptsScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_MEMORY) {
            MemoryScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_CODEX) {
            CodexScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_EXPORT) {
            ExportScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_TRIGGERS) {
            de.frank.entropyreducer.presentation.settings.triggers.KiTriggersScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_ARCHIVE) {
            de.frank.entropyreducer.presentation.settings.archive.ArchiveScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_WIDGET) {
            de.frank.entropyreducer.presentation.settings.WidgetSettingsScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
        composable(Routes.SETTINGS_DIAGNOSTICS) {
            de.frank.entropyreducer.presentation.settings.diagnostics.DiagnosticLogScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
    }
}
