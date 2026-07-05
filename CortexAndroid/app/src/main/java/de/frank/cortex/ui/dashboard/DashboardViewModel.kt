package de.frank.cortex.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.frank.cortex.data.model.*
import de.frank.cortex.network.ApiClient
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
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
    val editTitle: String = "",
    val editCategory: String? = null,
    val categories: List<CategoryInfo> = emptyList(),
    // Drilldown
    val categoryPath: List<String> = emptyList(),
    val subcategories: Map<String, Int> = emptyMap()
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Nur pollen/aktualisieren, wenn der Dashboard-Screen wirklich sichtbar ist. Sonst lief der
    // 20s-Poll (und der VPN-Connect-Trigger) im Hintergrund WEITER — auch waehrend man im Chat auf
    // die Agent-Antwort wartet. Ueber den schmalen WireGuard-Mobilfunk-Tunnel verstopften diese
    // parallelen Polls die Leitung (Connect-Timeouts -> sichtbare "Fehler Timeout"-Toasts) und
    // konkurrierten mit der Chat-Anfrage um Bandbreite. Der Screen meldet seine Sichtbarkeit via
    // setScreenActive (Vorfall 2026-07-01: Performance-Debug Internet-Antwort).
    @Volatile private var screenActive = false

    fun setScreenActive(active: Boolean) {
        val becameActive = active && !screenActive
        screenActive = active
        // Beim Sichtbarwerden SOFORT laden statt bis zu 20 s auf den naechsten Poll-Tick zu warten
        // (Frank-Bug 2026-07-02: Dashboard zeigte ~1 Minute lang "0 Gesamteintraege"). Ist das VPN
        // noch nicht verbunden, uebernimmt der bestehende VPN-Connect-Trigger im init-Block.
        if (becameActive && WireGuardManager.state.value == TunnelState.CONNECTED) {
            refreshAll()
        }
    }

    init {
        startAutoRefresh()
        // Sofort aktualisieren, sobald das VPN verbunden ist (statt bis zu 20 s aufs Poll-Intervall zu
        // warten) — aber nur wenn das Dashboard gerade sichtbar ist (nicht im Hintergrund/Chat).
        viewModelScope.launch {
            WireGuardManager.state.collect { st ->
                if (st == TunnelState.CONNECTED && screenActive) refreshAll()
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                if (screenActive && WireGuardManager.state.value == TunnelState.CONNECTED) {
                    refreshAll()
                }
                delay(20_000)
            }
        }
        viewModelScope.launch {
            while (true) {
                if (screenActive && WireGuardManager.state.value == TunnelState.CONNECTED) {
                    loadVitals()
                }
                delay(5_000)
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Parallele Aufrufe
                val countsDeferred = launch { loadCategoryCounts() }
                val catsDeferred = launch { loadCategories() }
                val overviewDeferred = launch { loadOverview() }
                val healthDeferred = launch { loadHealth() }

                countsDeferred.join()
                catsDeferred.join()
                overviewDeferred.join()
                healthDeferred.join()

                _uiState.update { it.copy(isLoading = false, error = null, isConnected = true) }
            } catch (e: CancellationException) {
                throw e
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
            // NUR die Zaehler fuer die Uebersicht (Balken). Die Kategorien-LISTE fuer den Picker
            // kommt aus loadCategories() (Registry, inkl. leerer) — category-counts laesst leere weg.
            _uiState.update {
                it.copy(
                    totalEntries = counts.total_distinct,
                    categoryCounts = mainCounts
                )
            }
            CortexLog.info("DashboardVM", "loadCategoryCounts", "Gesamtzahl aus Brain-API übernommen", mapOf(
                "total_distinct" to counts.total_distinct,
                "categories" to counts.counts.size
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadCategoryCounts", "Fehler: ${e.message}")
        }
    }

    /** Vollstaendige Kategorienliste (inkl. LEERER) fuer den „+ Kategorie"-Picker — aus der Agent-
     *  Registry (/categories/detail), NICHT aus category-counts (das leere Kategorien weglaesst,
     *  weshalb eine gerade angelegte Kategorie vorher NICHT im Picker auftauchte). Gleiche Quelle
     *  wie der Chat-Tab (der die Kategorie deshalb korrekt zeigte). */
    private suspend fun loadCategories() {
        try {
            val resp = ApiClient.agentApi().getCategories()
            _uiState.update { it.copy(categories = resp.categories) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadCategories", "Fehler: ${e.message}")
        }
    }

    private suspend fun loadOverview() {
        try {
            val overview = ApiClient.dashboardApi().overview()
            _uiState.update { it.copy(overview = overview) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadOverview", "Fehler: ${e.message}")
        }
    }

    private suspend fun loadVitals() {
        try {
            val vitals = ApiClient.dashboardApi().vitals()
            _uiState.update { state ->
                val current = state.overview
                state.copy(
                    overview = if (current != null) {
                        current.copy(agent = vitals.agent ?: current.agent, server = vitals.server ?: current.server)
                    } else {
                        OverviewResponse(
                            total = state.totalEntries,
                            brain = state.health?.let { BrainOverview(it.status, it.version, it.points) },
                            agent = vitals.agent,
                            server = vitals.server
                        )
                    },
                    isConnected = true
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadVitals", "Fehler: ${e.message}")
        }
    }

    private suspend fun loadHealth() {
        try {
            val health = ApiClient.brainApi().health()
            _uiState.update { it.copy(health = health) }
        } catch (e: CancellationException) {
            throw e
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
            } catch (e: CancellationException) {
                throw e
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

    fun drillIntoCategory(category: String) {
        val path = _uiState.value.categoryPath + category
        val fullCategory = path.joinToString("/")
        _uiState.update { it.copy(categoryPath = path, selectedCategory = fullCategory) }
        loadSubcategories(fullCategory)
        browseCategory(fullCategory)
    }

    fun drillBack(toIndex: Int) {
        val path = _uiState.value.categoryPath.take(toIndex)
        _uiState.update { it.copy(categoryPath = path) }
        if (path.isEmpty()) {
            _uiState.update { it.copy(subcategories = emptyMap(), selectedCategory = null, browseResults = emptyList()) }
        } else {
            val fullCategory = path.joinToString("/")
            _uiState.update { it.copy(selectedCategory = fullCategory) }
            loadSubcategories(fullCategory)
            browseCategory(fullCategory)
        }
    }

    fun drillToRoot() {
        _uiState.update {
            it.copy(
                categoryPath = emptyList(),
                subcategories = emptyMap(),
                selectedCategory = null,
                browseResults = emptyList()
            )
        }
    }

    fun handleBackNavigation() {
        val state = _uiState.value
        when {
            state.selectedEntry != null -> clearSelection()
            state.categoryPath.size > 1 -> drillBack(state.categoryPath.lastIndex)
            state.categoryPath.isNotEmpty() -> drillToRoot()
            state.selectedCategory != null -> selectCategory(null)
        }
    }

    private fun loadSubcategories(parentCategory: String) {
        viewModelScope.launch {
            try {
                val counts = ApiClient.brainApi().categoryCounts()
                val subs = mutableMapOf<String, Int>()
                val prefix = "$parentCategory/"
                counts.counts.forEach { (name, count) ->
                    if (name.startsWith(prefix)) {
                        val remainder = name.removePrefix(prefix)
                        val sub = remainder.substringBefore("/")
                        val fullSub = "$parentCategory/$sub"
                        subs[sub] = (subs[sub] ?: 0) + count
                    }
                }
                _uiState.update { it.copy(subcategories = subs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "loadSubcategories", "Fehler: ${e.message}")
            }
        }
    }

    private fun browseCategory(category: String) {
        viewModelScope.launch {
            try {
                // byCategory + byParent sind unabhaengig — PARALLEL statt seriell abfragen:
                // ueber den langsamen WireGuard-Mobilfunk-Tunnel addierte sich vorher die Latenz
                // beider Roundtrips (Drilldown fuehlbar traege). Merge/Ergebnis bleibt identisch.
                val exactDeferred = async {
                    try {
                        ApiClient.brainApi().byCategory(category).items
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.warn("DashboardVM", "browseCategory", "byCategory fehlgeschlagen: ${e.message}")
                        emptyList()
                    }
                }
                val childDeferred = async {
                    try {
                        ApiClient.brainApi().byParent(category).items
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.warn("DashboardVM", "browseCategory", "byParent fehlgeschlagen: ${e.message}")
                        emptyList()
                    }
                }
                val exactItems = exactDeferred.await()
                val childItems = childDeferred.await()
                val merged = (exactItems + childItems)
                    .distinctBy { it.doc_id }
                    .sortedByDescending { it.updated_at ?: it.created_at ?: "" }
                _uiState.update { it.copy(browseResults = merged) }
                CortexLog.info("DashboardVM", "browseCategory", "Kategorie-Unterbaum geladen", mapOf(
                    "category" to category,
                    "exact" to exactItems.size,
                    "children" to childItems.size,
                    "merged" to merged.size
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "browseCategory", "Fehler: ${e.message}")
                _uiState.update { it.copy(browseResults = emptyList()) }
            }
        }
    }

    fun selectEntry(entry: BrainEntry) {
        _uiState.update {
            it.copy(
                selectedEntry = entry,
                editText = entry.text ?: "",
                editTitle = entry.title ?: "",
                editCategory = entry.category,
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

    fun cancelEditing() {
        val entry = _uiState.value.selectedEntry ?: return
        _uiState.update {
            it.copy(
                isEditing = false,
                editTitle = entry.title ?: "",
                editText = entry.text ?: "",
                editCategory = entry.category
            )
        }
    }

    fun updateEditText(text: String) {
        _uiState.update { it.copy(editText = text) }
    }

    fun updateEditTitle(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun updateEditCategory(category: String?) {
        _uiState.update { it.copy(editCategory = category) }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.agentApi().createCategory(CreateCategoryRequest(name))
                val key = resp.key?.takeIf { it.isNotBlank() } ?: name   // kanonischer Kategorie-String
                loadCategories()   // Picker-Liste frisch aus der Registry (enthaelt jetzt die neue Kategorie)
                // Neue Kategorie SOFORT dem offenen Eintrag zuordnen (wie im Web-Drawer) — genau dafuer
                // legt Frank sie im „+ Kategorie"-Dialog an; addEntryCategory speichert per re-embed.
                if (_uiState.value.selectedEntry != null) addEntryCategory(key)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "createCategory", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Kategorie konnte nicht erstellt werden: ${e.message}") }
            }
        }
    }

    fun saveEntryCategory() {
        val entry = _uiState.value.selectedEntry ?: return
        val category = _uiState.value.editCategory ?: return
        viewModelScope.launch {
            try {
                ApiClient.brainApi().changeCategory(ChangeCategoryRequest(entry.doc_id, category))
                val updatedEntry = entry.copy(category = category)
                _uiState.update { it.copy(selectedEntry = updatedEntry) }
                CortexLog.info("DashboardVM", "saveEntryCategory", "Kategorie gespeichert", mapOf("doc_id" to entry.doc_id, "category" to category))
                loadCategoryCounts()
                if (_uiState.value.selectedCategory != null) browseCategory(_uiState.value.selectedCategory!!)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "saveEntryCategory", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Kategorie speichern fehlgeschlagen: ${e.message}") }
            }
        }
    }

    // ── Multi-Category: mehrere Kategorien pro Eintrag (Frank-Wunsch 2026-07-02) ──────────────────
    /** Aktuelle Kategorien des offenen Eintrags (Fallback: die eine primaere). */
    private fun currentEntryCats(entry: BrainEntry): List<String> =
        entry.categories?.takeIf { it.isNotEmpty() } ?: listOfNotNull(entry.category)

    /** Eine WEITERE Kategorie hinzufuegen (sofort speichern; brain bettet den Eintrag mit allen neu ein). */
    fun addEntryCategory(name: String) {
        val entry = _uiState.value.selectedEntry ?: return
        val clean = name.trim()
        if (clean.isEmpty()) return
        val cur = currentEntryCats(entry)
        if (cur.any { it.equals(clean, ignoreCase = true) }) return   // schon zugeordnet
        if (cur.size >= 12) {
            _uiState.update { it.copy(error = "Höchstens 12 Kategorien pro Eintrag") }
            return
        }
        setEntryCategories(entry, cur + clean)
    }

    /** Eine Kategorie entfernen (mindestens eine bleibt immer). */
    fun removeEntryCategory(name: String) {
        val entry = _uiState.value.selectedEntry ?: return
        val cur = currentEntryCats(entry)
        if (cur.size <= 1) return
        setEntryCategories(entry, cur.filterNot { it.equals(name, ignoreCase = true) })
    }

    private fun setEntryCategories(entry: BrainEntry, newCats: List<String>) {
        if (newCats.isEmpty()) return
        viewModelScope.launch {
            try {
                val resp = ApiClient.brainApi().setCategories(SetCategoriesRequest(entry.doc_id, newCats))
                val applied = resp.categories?.takeIf { it.isNotEmpty() } ?: newCats
                val updatedEntry = entry.copy(categories = applied, category = applied.first())
                _uiState.update { it.copy(selectedEntry = updatedEntry) }
                CortexLog.info("DashboardVM", "setEntryCategories", "Kategorien gesetzt", mapOf(
                    "doc_id" to entry.doc_id, "categories" to applied.joinToString(",")
                ))
                loadCategoryCounts()
                if (_uiState.value.selectedCategory != null) browseCategory(_uiState.value.selectedCategory!!)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "setEntryCategories", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Kategorien speichern fehlgeschlagen: ${e.message}") }
            }
        }
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
                val updatedEntry = entry.copy(
                    title = _uiState.value.editTitle.ifBlank { entry.title },
                    text = _uiState.value.editText
                )
                _uiState.update { it.copy(isEditing = false, selectedEntry = updatedEntry) }
                CortexLog.info("DashboardVM", "saveEntry", "Eintrag gespeichert: ${entry.doc_id}")
                // Refresh
                if (_uiState.value.selectedCategory != null) browseCategory(_uiState.value.selectedCategory!!)
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "deleteEntry", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Löschen fehlgeschlagen: ${e.message}") }
            }
        }
    }
}
