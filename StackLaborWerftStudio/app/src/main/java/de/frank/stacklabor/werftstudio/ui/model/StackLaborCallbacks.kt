package de.frank.stacklabor.werftstudio.ui.model

import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute

data class StackLaborCallbacks(
    val onNavigate: (StackLaborRoute) -> Unit,
    val onBack: () -> Unit,
    val onEvent: (StackLaborEvent) -> Unit,
)
