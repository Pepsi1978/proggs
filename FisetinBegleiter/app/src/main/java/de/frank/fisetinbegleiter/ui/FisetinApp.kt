package de.frank.fisetinbegleiter.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class MainScreen(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Heute", Icons.Outlined.Home),
    TIMELINE("timeline", "Ablauf", Icons.Outlined.Checklist),
    STACK("stack", "Stack", Icons.Outlined.Inventory2),
    HISTORY("history", "Verlauf", Icons.Outlined.History),
    MORE("more", "Sys", Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FisetinApp(
    externalDestination: String?,
    onDestinationConsumed: () -> Unit,
    onExport: (String) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    showFirstStartNotice: Boolean,
    onNoticeConfirmed: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(MainScreen.TODAY) }
    var noticeVisible by remember(showFirstStartNotice) { mutableStateOf(showFirstStartNotice) }

    LaunchedEffect(externalDestination) {
        externalDestination?.let { destination ->
            screen = MainScreen.entries.firstOrNull { it.route == destination } ?: MainScreen.TODAY
            onDestinationConsumed()
        }
    }

    if (noticeVisible) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Wichtiger Hinweis") },
            text = {
                Text(
                    "Diese App bildet nur dein persönliches, selbst festgelegtes Protokoll ab. " +
                        "Sie ist keine medizinische Beratung. Bei Unsicherheiten oder Wechselwirkungen ärztlichen Rat einholen.\n\n" +
                        "Venlafaxin wird durch die Kur nicht verändert und nie ausgesetzt.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    noticeVisible = false
                    onNoticeConfirmed()
                }) { Text("Verstanden") }
            },
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp
        Row(Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail {
                    MainScreen.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = destination == screen,
                            onClick = { screen = destination },
                            icon = { Icon(destination.icon, destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when (screen) {
                                    MainScreen.TODAY -> "Fisetin-Begleiter"
                                    else -> screen.label
                                },
                            )
                        },
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            MainScreen.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = destination == screen,
                                    onClick = { screen = destination },
                                    icon = { Icon(destination.icon, destination.label) },
                                    label = { Text(destination.label) },
                                )
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                val modifier = Modifier.fillMaxSize().padding(padding)
                when (screen) {
                    MainScreen.TODAY -> TodayScreen(
                        state = state,
                        onCreateCure = viewModel::createCure,
                        onDrinkNow = viewModel::drinkNow,
                        onCancelCure = viewModel::cancelAllActiveCures,
                        onGoTimeline = { screen = MainScreen.TIMELINE },
                        onGoStack = { screen = MainScreen.STACK },
                        onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                        exactAlarmsAvailable = viewModel.canScheduleExactAlarms(),
                        modifier = modifier,
                    )
                    MainScreen.TIMELINE -> TimelineScreen(
                        state = state,
                        onMealDone = viewModel::markMeal,
                        onSpermidinDone = viewModel::markSpermidin,
                        onCompleteDay = viewModel::completeDay,
                        modifier = modifier,
                    )
                    MainScreen.STACK -> StackScreen(state, modifier)
                    MainScreen.HISTORY -> HistoryScreen(
                        state = state,
                        onUpdateNote = viewModel::updateNote,
                        onCancelCure = viewModel::cancelAllActiveCures,
                        onExport = { onExport(viewModel.exportText()) },
                        modifier = modifier,
                    )
                    MainScreen.MORE -> MoreScreen(
                        state = state,
                        onSaveProtocol = viewModel::saveProtocol,
                        onSaveIngredient = viewModel::saveIngredient,
                        onDeleteIngredient = viewModel::deleteIngredient,
                        onOpenBatterySettings = onOpenBatterySettings,
                        modifier = modifier,
                    )
                }
            }
        }
    }
}
