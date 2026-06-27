package de.frank.cortex.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.cortex.data.model.*
import de.frank.cortex.network.ApiClient
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalEntries: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val overview: OverviewResponse? = null,
    val health: HealthResponse? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isConnected: Boolean = false,
    // Stöbern/Suchen
    val searchQuery: String = "",
    val searchResults: List<BrainEntry> = emptyList(),
    val selectedCategory: String? = null,
    val browseResults: List<BrainEntry> = emptyList(),
    val selectedEntry: BrainEntry? = null,
    val isEditing: Boolean = false,
    val editText: String = "",
    val editTitle: String = ""
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        startAutoRefresh()
        // Sofort aktualisieren, sobald das VPN verbunden ist (statt bis zu 20 s aufs Poll-Intervall zu warten).
        viewModelScope.launch {
            WireGuardManager.state.collect { st ->
                if (st == TunnelState.CONNECTED) refreshAll()
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                if (WireGuardManager.state.value == TunnelState.CONNECTED) {
                    refreshAll()
                }
                delay(20_000)
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Parallele Aufrufe
                val countsDeferred = launch { loadCategoryCounts() }
                val overviewDeferred = launch { loadOverview() }
                val healthDeferred = launch { loadHealth() }

                countsDeferred.join()
                overviewDeferred.join()
                healthDeferred.join()

                _uiState.update { it.copy(isLoading = false, error = null, isConnected = true) }
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "refreshAll", "Fehler: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = e.message, isConnected = false) }
            }
        }
    }

    private suspend fun loadCategoryCounts() {
        try {
            val counts = ApiClient.brainApi().categoryCounts()
            // Nur Hauptkategorien: Unterkategorien (a/b/c) werden zum Hauptnamen (a) zusammengefasst
            val mainCounts = mutableMapOf<String, Int>()
            counts.counts.forEach { (name, count) ->
                val main = name.substringBefore("/")
                mainCounts[main] = (mainCounts[main] ?: 0) + count
            }
            _uiState.update { it.copy(totalEntries = counts.total_distinct, categoryCounts = mainCounts) }
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadCategoryCounts", "Fehler: ${e.message}")
        }
    }

    private suspend fun loadOverview() {
        try {
            val overview = ApiClient.dashboardApi().overview()
            _uiState.update { it.copy(overview = overview) }
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadOverview", "Fehler: ${e.message}")
        }
    }

    private suspend fun loadHealth() {
        try {
            val health = ApiClient.brainApi().health()
            _uiState.update { it.copy(health = health) }
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadHealth", "Fehler: ${e.message}")
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            try {
                val response = ApiClient.brainApi().search(
                    SearchRequest(query = query, limit = 20, category = _uiState.value.selectedCategory)
                )
                _uiState.update { it.copy(searchResults = response.items) }
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "search", "Fehler: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        if (category != null) {
            browseCategory(category)
        } else {
            _uiState.update { it.copy(browseResults = emptyList()) }
        }
    }

    private fun browseCategory(category: String) {
        viewModelScope.launch {
            try {
                // byParent liefert alle Einträge der Hauptkategorie + aller Unterkategorien
                val response = ApiClient.brainApi().byParent(category)
                _uiState.update { it.copy(browseResults = response.items) }
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "browseCategory", "Fehler: ${e.message}")
            }
        }
    }

    fun selectEntry(entry: BrainEntry) {
        _uiState.update {
            it.copy(
                selectedEntry = entry,
                editText = entry.text ?: "",
                editTitle = entry.title ?: "",
                isEditing = false
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedEntry = null, isEditing = false) }
    }

    fun toggleEditing() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    fun updateEditText(text: String) {
        _uiState.update { it.copy(editText = text) }
    }

    fun updateEditTitle(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun saveEntry() {
        val entry = _uiState.value.selectedEntry ?: return
        viewModelScope.launch {
            try {
                ApiClient.brainApi().updateEntry(
                    UpdateEntryRequest(
                        doc_id = entry.doc_id,
                        text = _uiState.value.editText,
                        title = _uiState.value.editTitle.ifBlank { null }
                    )
                )
                _uiState.update { it.copy(isEditing = false) }
                CortexLog.info("DashboardVM", "saveEntry", "Eintrag gespeichert: ${entry.doc_id}")
                // Refresh
                if (_uiState.value.selectedCategory != null) browseCategory(_uiState.value.selectedCategory!!)
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "saveEntry", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Speichern fehlgeschlagen: ${e.message}") }
            }
        }
    }

    fun deleteEntry() {
        val entry = _uiState.value.selectedEntry ?: return
        viewModelScope.launch {
            try {
                ApiClient.brainApi().deleteEntry(entry.doc_id)
                _uiState.update { it.copy(selectedEntry = null) }
                CortexLog.info("DashboardVM", "deleteEntry", "Eintrag gelöscht: ${entry.doc_id}")
                refreshAll()
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "deleteEntry", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Löschen fehlgeschlagen: ${e.message}") }
            }
        }
    }
}
