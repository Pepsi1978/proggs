package de.frank.entropyreducer.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.repository.InsightRepository
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.usecase.RepertoireSortUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightBoardUiState(
    val confirmed: List<InsightEntity> = emptyList(),
    val observation: List<InsightEntity> = emptyList(),
    val discarded: List<InsightEntity> = emptyList(),
    val expandedSection: Section = Section.CONFIRMED,
    val selected: InsightEntity? = null,
)

enum class Section { CONFIRMED, OBSERVATION, DISCARDED }

@HiltViewModel
class InsightBoardViewModel @Inject constructor(
    private val insights: InsightRepository,
) : ViewModel() {

    private val expandedFlow = MutableStateFlow(Section.CONFIRMED)
    private val selectedFlow = MutableStateFlow<InsightEntity?>(null)

    val state: StateFlow<InsightBoardUiState> = combine(
        insights.observeConfirmed(),
        insights.observeInObservation(),
        insights.observeDiscarded(),
        expandedFlow,
        selectedFlow,
    ) { confirmed, observation, discarded, expanded, selected ->
        InsightBoardUiState(
            confirmed = confirmed,
            observation = observation,
            discarded = discarded,
            expandedSection = expanded,
            selected = selected,
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
