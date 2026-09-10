package de.frank.stacklabor.werftstudio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList

enum class Origin { Home, StackDetail, AllStacks, Settings, GoalCatalog, MedicineCatalog, StackEdit }

sealed interface StackLaborRoute {
    data object Home : StackLaborRoute
    data class StackDetail(val stackId: String) : StackLaborRoute
    data class GoalCatalog(val origin: Origin) : StackLaborRoute
    data class StackGoals(val stackId: String, val origin: Origin) : StackLaborRoute
    data class MedicineEdit(val medicineId: String?, val stackId: String?, val origin: Origin) : StackLaborRoute
    data class Breakdown(val stackId: String, val subjectId: String, val origin: Origin) : StackLaborRoute
    data class Evaluation(val evaluationId: String, val origin: Origin) : StackLaborRoute
    data class Questions(val stackId: String, val origin: Origin) : StackLaborRoute
    data object AllStacks : StackLaborRoute
    data object Settings : StackLaborRoute
    data class CodexLogin(val origin: Origin) : StackLaborRoute
    data class ReorderGoals(val stackId: String) : StackLaborRoute
    data class StackEdit(val stackId: String?, val origin: Origin) : StackLaborRoute
    data class MedicineCatalog(val stackId: String?, val origin: Origin) : StackLaborRoute
    data class History(val stackId: String, val origin: Origin) : StackLaborRoute
}

@Stable
class StackLaborNavigator internal constructor(initialRoutes: List<StackLaborRoute>) {
    private val routes: SnapshotStateList<StackLaborRoute> = mutableStateListOf<StackLaborRoute>().apply {
        addAll(initialRoutes.ifEmpty { listOf(StackLaborRoute.Home) })
    }

    val currentRoute: StackLaborRoute by androidx.compose.runtime.derivedStateOf { routes.last() }
    val canGoBack: Boolean by androidx.compose.runtime.derivedStateOf { routes.size > 1 }

    fun navigate(route: StackLaborRoute) {
        routes += route
    }

    fun back() {
        if (routes.size > 1) routes.removeAt(routes.lastIndex)
    }

    internal fun snapshot(): List<StackLaborRoute> = routes.toList()
}

private val NavigatorSaver = Saver<StackLaborNavigator, ArrayList<String>>(
    save = { navigator -> ArrayList(navigator.snapshot().map(::encodeRoute)) },
    restore = { encoded -> StackLaborNavigator(encoded.map(::decodeRoute)) },
)

@Composable
fun rememberStackLaborNavigator(): StackLaborNavigator =
    rememberSaveable(saver = NavigatorSaver) { StackLaborNavigator(listOf(StackLaborRoute.Home)) }

private fun encodeRoute(route: StackLaborRoute): String = when (route) {
    StackLaborRoute.Home -> "home"
    is StackLaborRoute.StackDetail -> "stack|${route.stackId}"
    is StackLaborRoute.GoalCatalog -> "goalCatalog|${route.origin.name}"
    is StackLaborRoute.StackGoals -> "stackGoals|${route.stackId}|${route.origin.name}"
    is StackLaborRoute.MedicineEdit -> "medicineEdit|${route.medicineId.orEmpty()}|${route.stackId.orEmpty()}|${route.origin.name}"
    is StackLaborRoute.Breakdown -> "breakdown|${route.stackId}|${route.subjectId}|${route.origin.name}"
    is StackLaborRoute.Evaluation -> "evaluation|${route.evaluationId}|${route.origin.name}"
    is StackLaborRoute.Questions -> "questions|${route.stackId}|${route.origin.name}"
    StackLaborRoute.AllStacks -> "allStacks"
    StackLaborRoute.Settings -> "settings"
    is StackLaborRoute.CodexLogin -> "codex|${route.origin.name}"
    is StackLaborRoute.ReorderGoals -> "reorder|${route.stackId}"
    is StackLaborRoute.StackEdit -> "stackEdit|${route.stackId.orEmpty()}|${route.origin.name}"
    is StackLaborRoute.MedicineCatalog -> "medicineCatalog|${route.stackId.orEmpty()}|${route.origin.name}"
    is StackLaborRoute.History -> "history|${route.stackId}|${route.origin.name}"
}

private fun decodeRoute(encoded: String): StackLaborRoute {
    val parts = encoded.split('|')
    fun origin(index: Int) = Origin.valueOf(parts[index])
    return when (parts.first()) {
        "stack" -> StackLaborRoute.StackDetail(parts[1])
        "goalCatalog" -> StackLaborRoute.GoalCatalog(origin(1))
        "stackGoals" -> StackLaborRoute.StackGoals(parts[1], origin(2))
        "medicineEdit" -> StackLaborRoute.MedicineEdit(parts[1].ifEmpty { null }, parts[2].ifEmpty { null }, origin(3))
        "breakdown" -> StackLaborRoute.Breakdown(parts[1], parts[2], origin(3))
        "evaluation" -> StackLaborRoute.Evaluation(parts[1], origin(2))
        "questions" -> StackLaborRoute.Questions(parts[1], origin(2))
        "allStacks" -> StackLaborRoute.AllStacks
        "settings" -> StackLaborRoute.Settings
        "codex" -> StackLaborRoute.CodexLogin(origin(1))
        "reorder" -> StackLaborRoute.ReorderGoals(parts[1])
        "stackEdit" -> StackLaborRoute.StackEdit(parts[1].ifEmpty { null }, origin(2))
        "medicineCatalog" -> StackLaborRoute.MedicineCatalog(parts[1].ifEmpty { null }, origin(2))
        "history" -> StackLaborRoute.History(parts[1], origin(2))
        else -> StackLaborRoute.Home
    }
}
