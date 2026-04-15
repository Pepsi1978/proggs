package com.nems.app.ui.daydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nems.app.data.local.entity.StackSectionEntity
import com.nems.app.data.local.entity.SupplementEntity
import com.nems.app.data.local.entity.SupplementEntryEntity
import com.nems.app.data.repository.SupplementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SectionWithEntries(
    val section: StackSectionEntity,
    val entries: List<EntryWithSupplement>,
    val takenCount: Int,
    val totalCount: Int,
)

data class EntryWithSupplement(
    val entry: SupplementEntryEntity,
    val supplement: SupplementEntity?,
)

sealed interface DayDetailUiState {
    data object Loading : DayDetailUiState
    data class Success(
        val date: LocalDate,
        val sections: List<SectionWithEntries>,
        val overallProgress: Float,
        val isDienstTag: Boolean,
    ) : DayDetailUiState
}

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SupplementRepository,
) : ViewModel() {

    private val dateString: String = savedStateHandle["date"] ?: LocalDate.now().toString()
    val date: LocalDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)

    private val _isDienstTag = MutableStateFlow(false)
    val isDienstTag: StateFlow<Boolean> = _isDienstTag.asStateFlow()

    val uiState: StateFlow<DayDetailUiState> = combine(
        repository.getEntriesForDate(date),
        repository.getAllSections(),
        repository.getAllActiveSuplements(),
        _isDienstTag,
    ) { entries, sections, supplements, dienst ->
        val supplementMap = supplements.associateBy { it.id }
        val sectionEntries = sections.map { section ->
            val sectionItems = entries
                .filter { it.stackSectionId == section.id }
                .map { entry ->
                    EntryWithSupplement(entry, supplementMap[entry.supplementId])
                }
            SectionWithEntries(
                section = section,
                entries = sectionItems,
                takenCount = sectionItems.count { it.entry.taken },
                totalCount = sectionItems.size,
            )
        }.filter { it.entries.isNotEmpty() }

        val totalEntries = sectionEntries.sumOf { it.totalCount }
        val totalTaken = sectionEntries.sumOf { it.takenCount }
        val progress = if (totalEntries > 0) totalTaken.toFloat() / totalEntries else 0f

        DayDetailUiState.Success(
            date = date,
            sections = sectionEntries,
            overallProgress = progress,
            isDienstTag = dienst,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayDetailUiState.Loading)

    init {
        // Generate entries for this date if they don't exist yet.
        // Uses first() to collect both flows once without creating a persistent subscription.
        viewModelScope.launch {
            val supplements = repository.getAllActiveSuplements().first()
            val sections = repository.getAllSections().first()
            repository.generateDailyEntries(date, supplements, sections, _isDienstTag.value)
        }
    }

    fun toggleEntry(entryId: String, taken: Boolean) {
        viewModelScope.launch {
            repository.toggleEntry(entryId, taken)
        }
    }

    fun toggleSectionComplete(sectionId: String) {
        viewModelScope.launch {
            repository.toggleSectionComplete(date, sectionId)
        }
    }

    fun toggleDayComplete() {
        viewModelScope.launch {
            repository.toggleDayComplete(date)
        }
    }

    fun toggleDienstTag() {
        val newValue = !_isDienstTag.value
        _isDienstTag.value = newValue
        // generateDailyEntries short-circuits via count guard when entries already exist,
        // so we skip the two costly first() calls here — entries are always present by
        // the time the user can toggle Dienst (init block already generated them).
        viewModelScope.launch {
            repository.generateDailyEntries(date, emptyList(), emptyList(), newValue)
        }
    }
}
