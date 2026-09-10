package de.frank.stacklabor.werftstudio.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import de.frank.stacklabor.werftstudio.ui.model.Appearance
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.model.previewCatalogMedicines
import de.frank.stacklabor.werftstudio.ui.model.previewEvaluationParagraphs
import de.frank.stacklabor.werftstudio.ui.model.previewGoals
import de.frank.stacklabor.werftstudio.ui.model.previewHistory
import de.frank.stacklabor.werftstudio.ui.model.previewMedicines
import de.frank.stacklabor.werftstudio.ui.model.previewQuestions
import de.frank.stacklabor.werftstudio.ui.model.previewStacks
import de.frank.stacklabor.werftstudio.ui.navigation.Origin
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme

private class ScreenRouteProvider : PreviewParameterProvider<StackLaborRoute> {
    override val values = sequenceOf(
        StackLaborRoute.Home,
        StackLaborRoute.StackDetail("stack-1"),
        StackLaborRoute.GoalCatalog(Origin.Home),
        StackLaborRoute.StackGoals("stack-1", Origin.StackDetail),
        StackLaborRoute.MedicineEdit("pea", "stack-1", Origin.StackDetail),
        StackLaborRoute.Breakdown("stack-1", "pea", Origin.StackDetail),
        StackLaborRoute.Evaluation("evaluation-1", Origin.StackDetail),
        StackLaborRoute.Questions("stack-1", Origin.StackDetail),
        StackLaborRoute.AllStacks,
        StackLaborRoute.Settings,
        StackLaborRoute.CodexLogin(Origin.Settings),
        StackLaborRoute.ReorderGoals("stack-1"),
        StackLaborRoute.StackEdit("stack-1", Origin.StackDetail),
        StackLaborRoute.MedicineCatalog("stack-1", Origin.StackDetail),
        StackLaborRoute.History("stack-1", Origin.StackDetail),
    )
}

private val previewCallbacks = StackLaborCallbacks({}, {}, {})
private fun previewState(appearance: Appearance = Appearance.Light) = StackLaborUiState(
    loading = false,
    appearance = appearance,
    stackName = "Morgen-Stack Teil 1",
    stackTime = "08:00 Uhr",
    stackNote = "nüchtern, mit Wasser",
    medicineName = "PEA (Palmitoylethanolamid)",
    evaluationParagraphs = previewEvaluationParagraphs,
    questions = previewQuestions,
    stacks = previewStacks,
    medicines = previewMedicines,
    catalogMedicines = previewCatalogMedicines,
    goals = previewGoals,
    history = previewHistory,
)

@Preview(name = "Hell", widthDp = 297, heightDp = 421, showBackground = true)
@Composable
private fun ScreenLightPreview(@PreviewParameter(ScreenRouteProvider::class) route: StackLaborRoute) {
    val state = previewState(Appearance.Light)
    StackLaborTheme(darkTheme = false, reducedMotion = true) {
        StackLaborScreenHost(route, state, animationsEnabled = false, callbacks = previewCallbacks)
    }
}

@Preview(name = "Dunkel", widthDp = 297, heightDp = 421, showBackground = true)
@Composable
private fun ScreenDarkPreview(@PreviewParameter(ScreenRouteProvider::class) route: StackLaborRoute) {
    val state = previewState(Appearance.Dark)
    StackLaborTheme(darkTheme = true, reducedMotion = true) {
        StackLaborScreenHost(route, state, animationsEnabled = false, callbacks = previewCallbacks)
    }
}

@Preview(name = "Fold innen", widthDp = 440, heightDp = 583, showBackground = true)
@Composable
private fun FoldPreview() {
    val state = previewState()
    StackLaborTheme(darkTheme = false, reducedMotion = true) {
        StackLaborScreenHost(StackLaborRoute.StackDetail("stack-1"), state, false, previewCallbacks)
    }
}
