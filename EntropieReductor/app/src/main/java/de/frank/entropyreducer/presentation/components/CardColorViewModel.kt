package de.frank.entropyreducer.presentation.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.repository.CardColorRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Leichter ViewModel nur fuer die per-Karte gespeicherten Farb-Indizes.
 * Wird in den verschiedenen Detail-Screens (Oura, Health Connect, Amazfit,
 * BiomarkerDetail) per Hilt injiziert, damit alle die gleiche
 * `CardColorRepository`-Quelle teilen.
 *
 * Frank-Wunsch 2026-05-18 (Folgeauftrag): Farbpalette soll auf jedem
 * Pattern verfuegbar sein — der bisherige Hauptmangel war dass BiomarkerVM
 * (mit allen Whoop-/Oura-Flows) nur im Biomarker-Screen verfuegbar ist.
 * Dieser kleine VM braucht keine Sync-/Health-Connect-Dependencies und
 * kann ueberall billig injiziert werden.
 */
@HiltViewModel
class CardColorViewModel @Inject constructor(
    private val repo: CardColorRepository,
) : ViewModel() {

    val cardColors: StateFlow<Map<String, Int>> =
        repo.cardColors.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap(),
        )

    fun setCardColor(cardId: String, colorIndex: Int) {
        viewModelScope.launch { repo.setCardColor(cardId, colorIndex) }
    }

    fun setCardColors(cardIds: List<String>, colorIndex: Int) {
        viewModelScope.launch {
            cardIds.forEach { id -> repo.setCardColor(id, colorIndex) }
        }
    }
}

/**
 * Composable-Helper: liefert die aktuell gespeicherten Farben + Setter zurueck.
 * Vereinfacht die Nutzung in den Detail-Screens auf zwei Zeilen.
 */
@Composable
fun rememberCardColors(): CardColorAccess {
    val vm: CardColorViewModel = hiltViewModel()
    val colors by vm.cardColors.collectAsStateWithLifecycle()
    return CardColorAccess(
        colors = colors,
        setColor = { id, idx -> vm.setCardColor(id, idx) },
        setColors = { ids, idx -> vm.setCardColors(ids, idx) },
    )
}

data class CardColorAccess(
    val colors: Map<String, Int>,
    val setColor: (String, Int) -> Unit,
    val setColors: (List<String>, Int) -> Unit,
)
