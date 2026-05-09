package de.frank.entropyreducer.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.repository.InsightRepository
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.usecase.PolishMethodTextUseCase
import de.frank.entropyreducer.domain.usecase.RepertoireSortUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class InsightBoardUiState(
    val confirmed: List<InsightEntity> = emptyList(),
    val observation: List<InsightEntity> = emptyList(),
    val discarded: List<InsightEntity> = emptyList(),
    val expandedSection: Section = Section.CONFIRMED,
    val selected: InsightEntity? = null,
    val addDialog: AddMethodState = AddMethodState.Closed,
)

enum class Section { CONFIRMED, OBSERVATION, DISCARDED }

/**
 * State der manuellen Methoden-Erfassung. Frank-Wunsch 2026-05-09:
 *
 *  Closed:    Sheet zu.
 *  Polishing: Whisper-Transkript ist da, KI verbessert gerade — Spinner anzeigen.
 *  Ready:     KI-Politur abgeschlossen ODER Fallback. Felder editierbar, Frank
 *             kann Title/Description/Categories anpassen, dann Speichern.
 *  Saving:    upsert läuft — Sheet schließt sich gleich von selbst.
 */
sealed class AddMethodState {
    object Closed : AddMethodState()
    object Polishing : AddMethodState()
    data class Ready(
        val title: String,
        val description: String,
        val primaryCategory: EntropyCategory,
        val additionalCategories: List<EntropyCategory>,
        val usedAi: Boolean,
    ) : AddMethodState()
    object Saving : AddMethodState()
}

@HiltViewModel
class InsightBoardViewModel @Inject constructor(
    private val insights: InsightRepository,
    private val polish: PolishMethodTextUseCase,
) : ViewModel() {

    private val expandedFlow = MutableStateFlow(Section.CONFIRMED)
    private val selectedFlow = MutableStateFlow<InsightEntity?>(null)
    private val addDialogFlow = MutableStateFlow<AddMethodState>(AddMethodState.Closed)

    val state: StateFlow<InsightBoardUiState> = combine(
        insights.observeConfirmed(),
        insights.observeInObservation(),
        insights.observeDiscarded(),
        combine(expandedFlow, selectedFlow, addDialogFlow) { e, s, a -> Triple(e, s, a) },
    ) { confirmed, observation, discarded, triple ->
        InsightBoardUiState(
            confirmed = confirmed,
            observation = observation,
            discarded = discarded,
            expandedSection = triple.first,
            selected = triple.second,
            addDialog = triple.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightBoardUiState())

    fun toggleSection(s: Section) {
        expandedFlow.value = if (expandedFlow.value == s) Section.CONFIRMED else s
    }

    fun open(insight: InsightEntity) {
        selectedFlow.value = insight
    }

    fun close() {
        selectedFlow.value = null
    }

    fun adjustConfidence(insight: InsightEntity, newConfidence: Int) {
        viewModelScope.launch {
            val updated = insight.copy(
                confidence = newConfidence.coerceIn(0, 100),
                updatedAt = System.currentTimeMillis(),
            )
            insights.update(updated)
            selectedFlow.value = updated
        }
    }

    fun setCategory(insight: InsightEntity, category: EntropyCategory) {
        viewModelScope.launch {
            val updated = insight.copy(
                targetCategory = category,
                additionalCategories = insight.additionalCategories.filter { it != category },
                updatedAt = System.currentTimeMillis(),
            )
            insights.update(updated)
            selectedFlow.value = updated
        }
    }

    /**
     * Toggle für eine Zusatz-Kategorie im Detail-Sheet. Wenn die Kategorie schon
     * primary ist, wird sie ignoriert (kann nicht gleichzeitig primary + additional
     * sein). Sonst wird sie in additionalCategories hinzugefügt oder entfernt.
     */
    fun toggleAdditionalCategory(insight: InsightEntity, category: EntropyCategory) {
        if (category == insight.targetCategory) return
        viewModelScope.launch {
            val current = insight.additionalCategories
            val next = if (current.contains(category)) {
                current.filter { it != category }
            } else {
                current + category
            }
            val updated = insight.copy(
                additionalCategories = next,
                updatedAt = System.currentTimeMillis(),
            )
            insights.update(updated)
            selectedFlow.value = updated
        }
    }

    fun setTitleAndDescription(insight: InsightEntity, title: String, description: String) {
        viewModelScope.launch {
            val updated = insight.copy(
                title = title.trim(),
                description = description.trim(),
                updatedAt = System.currentTimeMillis(),
            )
            insights.update(updated)
            selectedFlow.value = updated
        }
    }

    fun delete(insight: InsightEntity) {
        viewModelScope.launch {
            insights.delete(insight)
            close()
        }
    }

    // ---------- Manuelles Hinzufuegen via Mic + KI-Politur (Frank-Wunsch 2026-05-09) ----------

    fun openAddDialog() {
        // Sheet startet im Polishing-State NICHT — erst auf Mic-Tap. Aber wir
        // brauchen einen "Open"-State, in dem das Sheet sichtbar ist und der
        // Mic-Button bereit ist. Wir nutzen Ready mit leeren Werten dafür —
        // Frank tippt den Mic-Button, das Transkript kommt zurueck, dann erst
        // wechseln wir auf Polishing.
        addDialogFlow.value = AddMethodState.Ready(
            title = "",
            description = "",
            primaryCategory = EntropyCategory.MENTAL,
            additionalCategories = emptyList(),
            usedAi = false,
        )
    }

    fun closeAddDialog() {
        addDialogFlow.value = AddMethodState.Closed
    }

    /** Wird vom WhisperMicButton aufgerufen sobald die Transkription fertig ist. */
    fun onTranscriptReady(rawText: String) {
        if (rawText.isBlank()) return
        addDialogFlow.value = AddMethodState.Polishing
        viewModelScope.launch {
            val polished = polish(rawText)
            addDialogFlow.value = AddMethodState.Ready(
                title = polished.title,
                description = polished.description,
                primaryCategory = polished.primaryCategory,
                additionalCategories = polished.additionalCategories,
                usedAi = polished.usedAi,
            )
        }
    }

    fun editAddTitle(value: String) {
        val s = addDialogFlow.value as? AddMethodState.Ready ?: return
        addDialogFlow.value = s.copy(title = value)
    }

    fun editAddDescription(value: String) {
        val s = addDialogFlow.value as? AddMethodState.Ready ?: return
        addDialogFlow.value = s.copy(description = value)
    }

    fun setAddPrimaryCategory(c: EntropyCategory) {
        val s = addDialogFlow.value as? AddMethodState.Ready ?: return
        // Wenn die neue primary zuvor in additionalCategories war, raus damit —
        // primary und additional duerfen sich nicht ueberschneiden.
        addDialogFlow.value = s.copy(
            primaryCategory = c,
            additionalCategories = s.additionalCategories.filter { it != c },
        )
    }

    fun toggleAddAdditional(c: EntropyCategory) {
        val s = addDialogFlow.value as? AddMethodState.Ready ?: return
        if (c == s.primaryCategory) return
        val next = if (s.additionalCategories.contains(c)) {
            s.additionalCategories.filter { it != c }
        } else {
            s.additionalCategories + c
        }
        addDialogFlow.value = s.copy(additionalCategories = next)
    }

    fun saveManualMethod() {
        val s = addDialogFlow.value as? AddMethodState.Ready ?: return
        if (s.title.isBlank() && s.description.isBlank()) return
        addDialogFlow.value = AddMethodState.Saving
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // confidence 80 + attemptCount 3 + successCount 3 → DAO-Filter
            // getConfirmed() greift (confidence >= 70 AND attemptCount >= 3).
            // Damit landet die Methode SOFORT bei "Bestätigte Methoden", wie
            // von Frank gewuenscht.
            val entity = InsightEntity(
                id = UUID.randomUUID().toString(),
                title = s.title.ifBlank { s.description.take(60) }.trim(),
                description = s.description.trim(),
                targetCategory = s.primaryCategory,
                additionalCategories = s.additionalCategories,
                confidence = 80,
                successCount = 3,
                attemptCount = 3,
                avgBiomarkerImpact = null,
                avgFeltImpact = null,
                createdAt = now,
                updatedAt = now,
                sourceHypothesisIds = emptyList(),
                manualSource = true,
            )
            insights.upsert(entity)
            addDialogFlow.value = AddMethodState.Closed
        }
    }
}

@HiltViewModel
class RepertoireViewModel @Inject constructor(
    insights: InsightRepository,
    sort: RepertoireSortUseCase,
) : ViewModel() {

    val state: StateFlow<List<InsightEntity>> = insights.observeAll()
        .map { sort(it, minConfidence = 50) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
