package de.frank.entropyreducer.presentation.settings.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State des Archiv-Bildschirms (Frank-Wunsch 2026-05-09): zeigt alle archivierten
 * Eintraege gruppiert nach Monat. Sektion-Title wie "Mai 2026" — innerhalb pro
 * Sektion sortiert nach resolvedAt desc.
 */
data class ArchiveUiState(
    val sections: List<ArchiveSection> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
)

data class ArchiveSection(
    val monthLabel: String,
    val entries: List<EntropyEntryEntity>,
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val entries: EntryRepository,
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")

    val state: StateFlow<ArchiveUiState> = combine(
        entries.getArchived(),
        searchQueryFlow,
    ) { list, query ->
        val filtered = if (query.isBlank()) list else list.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                (it.aiNotes?.contains(query, ignoreCase = true) ?: false)
        }
        val sections = filtered
            .groupBy { monthKeyOf(it.resolvedAt ?: it.updatedAt) }
            .toSortedMap(compareByDescending { it })
            .map { (monthKey, entries) ->
                ArchiveSection(
                    monthLabel = monthLabelFromKey(monthKey),
                    entries = entries.sortedByDescending { it.resolvedAt ?: it.updatedAt },
                )
            }
        ArchiveUiState(
            sections = sections,
            totalCount = list.size,
            searchQuery = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState())

    fun setQuery(query: String) {
        searchQueryFlow.value = query
    }

    /**
     * Zurueck ins Aufgabenboard holen — Status auf REDUZIERT zuruecksetzen damit
     * der Eintrag wieder im "Erledigt"-Bereich am Ende der Aufgabenliste auftaucht.
     */
    fun unarchive(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            entries.update(
                entry.copy(
                    status = de.frank.entropyreducer.domain.model.EntryStatus.REDUZIERT,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /**
     * Komplett loeschen — nur fuer Frank wenn er bewusst aufraeumen will. Achtung:
     * geloeschte Archiv-Eintraege sind unwiederbringlich, der Forscher kann nicht
     * mehr daraus lernen.
     */
    fun deletePermanently(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            entries.delete(entry)
        }
    }

    private fun monthKeyOf(timestampMs: Long): String {
        val zoned = java.time.Instant.ofEpochMilli(timestampMs)
            .atZone(java.time.ZoneId.systemDefault())
        return "%04d-%02d".format(zoned.year, zoned.monthValue)
    }

    private fun monthLabelFromKey(key: String): String {
        val parts = key.split("-")
        if (parts.size != 2) return key
        val year = parts[0].toIntOrNull() ?: return key
        val month = parts[1].toIntOrNull() ?: return key
        val monthName = when (month) {
            1 -> "Januar"; 2 -> "Februar"; 3 -> "März"; 4 -> "April"
            5 -> "Mai"; 6 -> "Juni"; 7 -> "Juli"; 8 -> "August"
            9 -> "September"; 10 -> "Oktober"; 11 -> "November"; 12 -> "Dezember"
            else -> "?"
        }
        return "$monthName $year"
    }
}
