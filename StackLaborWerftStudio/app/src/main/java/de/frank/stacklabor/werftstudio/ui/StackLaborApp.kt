package de.frank.stacklabor.werftstudio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.frank.stacklabor.werftstudio.ui.model.Appearance
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.navigation.Origin
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborNavigator
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute
import de.frank.stacklabor.werftstudio.ui.navigation.rememberStackLaborNavigator
import de.frank.stacklabor.werftstudio.ui.screens.AllStacksScreen
import de.frank.stacklabor.werftstudio.ui.screens.BreakdownSheet
import de.frank.stacklabor.werftstudio.ui.screens.CodexLoginScreen
import de.frank.stacklabor.werftstudio.ui.screens.EvaluationScreen
import de.frank.stacklabor.werftstudio.ui.screens.GoalCatalogScreen
import de.frank.stacklabor.werftstudio.ui.screens.HistorySheet
import de.frank.stacklabor.werftstudio.ui.screens.HomeScreen
import de.frank.stacklabor.werftstudio.ui.screens.MedicineCatalogScreen
import de.frank.stacklabor.werftstudio.ui.screens.MedicineEditSheet
import de.frank.stacklabor.werftstudio.ui.screens.QuestionsSheet
import de.frank.stacklabor.werftstudio.ui.screens.ReorderGoalsScreen
import de.frank.stacklabor.werftstudio.ui.screens.SettingsScreen
import de.frank.stacklabor.werftstudio.ui.screens.StackDetailScreen
import de.frank.stacklabor.werftstudio.ui.screens.StackEditSheet
import de.frank.stacklabor.werftstudio.ui.screens.StackGoalsSheet
import de.frank.stacklabor.werftstudio.ui.screens.AppLockScreen
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme
import de.frank.stacklabor.werftstudio.ui.theme.rememberSystemAnimationsEnabled

@Composable
fun StackLaborApp(
    state: StackLaborUiState,
    onEvent: (StackLaborEvent) -> Unit,
    navigator: StackLaborNavigator = rememberStackLaborNavigator(),
) {
    val systemAnimationsEnabled = rememberSystemAnimationsEnabled().value
    val animationsEnabled = systemAnimationsEnabled && !state.reducedMotion
    val callbacks = StackLaborCallbacks(
        onNavigate = navigator::navigate,
        onBack = navigator::back,
        onEvent = onEvent,
    )
    BackHandler(enabled = navigator.canGoBack) {
        navigator.back()
    }
    LaunchedEffect(navigator.currentRoute) {
        onEvent(StackLaborEvent.RouteChanged(navigator.currentRoute))
    }
    StackLaborTheme(
        darkTheme = state.appearance == Appearance.Dark,
        reducedMotion = !animationsEnabled,
    ) {
        if (state.appLocked) {
            // Solange der Fingerabdruck fehlt, bleibt der Inhalt der App unsichtbar.
            AppLockScreen(callbacks)
        } else if (animationsEnabled) {
            AnimatedContent(
                targetState = navigator.currentRoute,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "screenNavigation",
            ) { route ->
                StackLaborScreenHost(route, state, animationsEnabled, callbacks)
            }
        } else {
            StackLaborScreenHost(navigator.currentRoute, state, false, callbacks)
        }
    }
}

@Composable
fun StackLaborScreenHost(
    route: StackLaborRoute,
    state: StackLaborUiState,
    animationsEnabled: Boolean,
    callbacks: StackLaborCallbacks,
) {
    when (route) {
        StackLaborRoute.Home -> HomeScreen(state, animationsEnabled, callbacks)
        is StackLaborRoute.StackDetail -> StackDetailScreen(route.stackId, state, animationsEnabled, callbacks)
        is StackLaborRoute.GoalCatalog -> GoalCatalogScreen(state, animationsEnabled, callbacks)
        is StackLaborRoute.StackGoals -> StackGoalsSheet(route.stackId, state, callbacks) {
            OriginUnderlay(route.origin, route.stackId, state, animationsEnabled, callbacks)
        }
        is StackLaborRoute.MedicineEdit -> MedicineEditSheet(route.medicineId, route.stackId, state, callbacks) {
            OriginUnderlay(route.origin, route.stackId, state, animationsEnabled, callbacks)
        }
        is StackLaborRoute.Breakdown -> BreakdownSheet(route.stackId, route.subjectId, state, callbacks) {
            OriginUnderlay(route.origin, route.stackId, state, animationsEnabled, callbacks)
        }
        is StackLaborRoute.Evaluation -> EvaluationScreen(state, callbacks)
        is StackLaborRoute.Questions -> QuestionsSheet(route.stackId, state, callbacks) {
            OriginUnderlay(route.origin, route.stackId, state, animationsEnabled, callbacks)
        }
        StackLaborRoute.AllStacks -> AllStacksScreen(state, callbacks)
        StackLaborRoute.Settings -> SettingsScreen(state, callbacks)
        is StackLaborRoute.CodexLogin -> CodexLoginScreen(state, callbacks)
        is StackLaborRoute.ReorderGoals -> ReorderGoalsScreen(route.stackId, state, callbacks)
        is StackLaborRoute.StackEdit -> StackEditSheet(route.stackId, state, callbacks) {
            OriginUnderlay(route.origin, route.stackId, state, animationsEnabled, callbacks)
        }
        is StackLaborRoute.MedicineCatalog -> MedicineCatalogScreen(route.stackId, state, animationsEnabled, callbacks)
        is StackLaborRoute.History -> HistorySheet(route.stackId, state, callbacks) {
            OriginUnderlay(route.origin, route.stackId, state, animationsEnabled, callbacks)
        }
    }
}

@Composable
private fun OriginUnderlay(
    origin: Origin,
    stackId: String?,
    state: StackLaborUiState,
    animationsEnabled: Boolean,
    callbacks: StackLaborCallbacks,
) {
    when (origin) {
        Origin.Home -> HomeScreen(state, animationsEnabled, callbacks)
        Origin.StackDetail -> state.stacks.firstOrNull()?.let { StackDetailScreen(stackId ?: it.id, state, animationsEnabled, callbacks) }
            ?: HomeScreen(state, animationsEnabled, callbacks)
        Origin.AllStacks -> AllStacksScreen(state, callbacks)
        Origin.Settings -> SettingsScreen(state, callbacks)
        Origin.GoalCatalog -> GoalCatalogScreen(state, animationsEnabled, callbacks)
        Origin.MedicineCatalog -> MedicineCatalogScreen(stackId, state, animationsEnabled, callbacks)
        Origin.StackEdit -> state.stacks.firstOrNull()?.let { StackDetailScreen(stackId ?: it.id, state, animationsEnabled, callbacks) }
            ?: HomeScreen(state, animationsEnabled, callbacks)
    }
}
