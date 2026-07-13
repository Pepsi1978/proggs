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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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

internal fun DashboardUiState.withoutEntry(docId: String) = copy(
    selectedEntry = selectedEntry?.takeUnless { it.doc_id == docId },
    isEditing = false,
    browseResults = browseResults.filterNot { it.doc_id == docId },
    searchResults = searchResults.filterNot { it.doc_id == docId },
    error = null
)

/** Geänderten Eintrag ueberall ersetzen (Detail + Browse- UND Suchliste): sonst zeigte die
 *  Suchliste nach Titel-/Text-/Kategorie-Aenderung weiter den alten Stand. */
internal fun DashboardUiState.replaceEntry(updated: BrainEntry) = copy(
    selectedEntry = if (selectedEntry?.doc_id == updated.doc_id) updated else selectedEntry,
    browseResults = browseResults.map { if (it.doc_id == updated.doc_id) updated else it },
    searchResults = searchResults.map { if (it.doc_id == updated.doc_id) updated else it }
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
    private var enterDashboardRefreshJob: Job? = null

    // Laufende Jobs pro Anfrage-Art: neue Anfrage cancelt die alte, sonst ueberschreibt eine
    // langsam eintreffende ALTE Antwort (WireGuard-Mobilfunk-Tunnel!) die aktuelle Ansicht
    // (Suche waehrend des Tippens, Drilldown-Wechsel A->B, parallele Refreshes).
    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var browseJob: Job? = null
    private var subcatJob: Job? = null
    // Kategorie-Mutationen (hinzufuegen/entfernen) laufen SERIELL: zwei schnelle Aenderungen
    // lasen sonst beide den alten Stand und die zweite verwarf die erste (Lost Update).
    private var categoryMutationJob: Job? = null

    fun setScreenActive(active: Boolean) {
        val becameActive = active && !screenActive
        screenActive = active
        if (!active) enterDashboardRefreshJob?.cancel()
        // Beim Wechsel aufs Dashboard zwei Sekunden warten, damit der sichtbare CPU-Wert nicht den
        // kurzen Abruf-Peak selbst zeigt. Der normale 5s-Vitals-Poll bleibt danach unveraendert aktiv.
        if (becameActive && WireGuardManager.state.value == TunnelState.CONNECTED) {
            enterDashboardRefreshJob?.cancel()
            enterDashboardRefreshJob = viewModelScope.launch {
                delay(2_000)
                if (screenActive && WireGuardManager.state.value == TunnelState.CONNECTED) refreshAll()
            }
        }
    }

    init {
        // Zwischengespeicherte Übersicht SOFORT anzeigen (Performance 2026-07-13): Beim Neustart
        // stand oben ewig „0 Einträge", weil totalEntries/Kategorien/Vitals erst nach VPN-Connect +
        // Server-Roundtrip kamen. Der lokale Snapshot füllt die Zahlen sofort; refreshAll ersetzt
        // sie still, sobald der Server antwortet (Stale-while-Revalidate).
        ApiClient.cachedDashboard()?.let { cached ->
            _uiState.update {
                it.copy(
                    totalEntries = cached.totalEntries,
                    categoryCounts = cached.categoryCounts,
                    overview = cached.overview,
                    health = cached.health
                )
            }
        }
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

    private companion object {
        // Eigene Fehlertexte PRO KANAL: jeder Kanal loescht ausschliesslich SEINEN eigenen
        // Fehler — Aktions-Fehler ("Löschen fehlgeschlagen") wurden sonst vom naechsten
        // erfolgreichen 20s-Poll ungelesen weggewischt; umgekehrt blieb ein Such-/Browse-
        // Fehlerbanner stehen, obwohl derselbe Kanal laengst wieder Erfolg hatte.
        const val REFRESH_ERROR = "Server nicht erreichbar"
        const val SEARCH_ERROR = "Suche fehlgeschlagen — Server nicht erreichbar"
        const val BROWSE_ERROR = "Kategorie konnte nicht geladen werden — Server nicht erreichbar"
    }

    // Waehrend ein Refresh laeuft angeforderte weitere Refreshes (z.B. nach deleteEntry) nicht
    // verschlucken, sondern direkt im Anschluss einmal nachholen — sonst zeigte die Uebersicht
    // bis zu 20 s den Stand VOR dem Loeschen.
    private var refreshRerunRequested = false

    fun refreshAll() {
        // In-Flight-Guard: 20s-Poll, VPN-Connect-Collector, Enter-Job und deleteEntry duerfen
        // keine PARALLELEN Voll-Refreshes stapeln (je 4 Requests) — genau das verstopfte den
        // Tunnel frueher ("Fehler Timeout", Vorfall 2026-07-01).
        if (refreshJob?.isActive == true) {
            refreshRerunRequested = true
            return
        }
        refreshJob = viewModelScope.launch {
            do {
                refreshRerunRequested = false
                runRefreshOnce()
            } while (refreshRerunRequested)
        }
    }

    private suspend fun runRefreshOnce() {
        _uiState.update { it.copy(isLoading = true) }
        try {
            // Parallele Aufrufe; jede Lade-Funktion meldet Erfolg/Fehler zurueck, denn sie
            // fangen ihre Exceptions selbst — sonst saehe dieser Block IMMER Erfolg und
            // meldete "verbunden", obwohl der Server komplett down ist.
            val results = coroutineScope {
                listOf(
                    async { loadCategoryCounts() },
                    async { loadCategories() },
                    async { loadOverview() },
                    async { loadHealth() }
                ).awaitAll()
            }

            val anySuccess = results.any { it }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (anySuccess) it.error.takeUnless { msg -> msg == REFRESH_ERROR }
                    else it.error ?: REFRESH_ERROR,
                    isConnected = anySuccess
                )
            }
            // Frische Übersicht für den nächsten Start persistieren (nur nach echtem Erfolg —
            // sonst würde ein Totalausfall den letzten guten Cache überschreiben).
            if (anySuccess) {
                _uiState.value.let { s ->
                    ApiClient.cacheDashboard(
                        DashboardSnapshot(
                            totalEntries = s.totalEntries,
                            categoryCounts = s.categoryCounts,
                            overview = s.overview,
                            health = s.health
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "refreshAll", "Fehler: ${e.message}")
            _uiState.update { it.copy(isLoading = false, error = e.message, isConnected = false) }
        }
    }

    private suspend fun loadCategoryCounts(): Boolean {
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
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadCategoryCounts", "Fehler: ${e.message}")
            return false
        }
    }

    /** Vollstaendige Kategorienliste (inkl. LEERER) fuer den „+ Kategorie"-Picker — aus der Agent-
     *  Registry (/categories/detail), NICHT aus category-counts (das leere Kategorien weglaesst,
     *  weshalb eine gerade angelegte Kategorie vorher NICHT im Picker auftauchte). Gleiche Quelle
     *  wie der Chat-Tab (der die Kategorie deshalb korrekt zeigte). */
    private suspend fun loadCategories(): Boolean {
        try {
            val resp = ApiClient.agentApi().getCategories()
            _uiState.update { it.copy(categories = resp.categories) }
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadCategories", "Fehler: ${e.message}")
            return false
        }
    }

    private suspend fun loadOverview(): Boolean {
        try {
            val overview = ApiClient.dashboardApi().overview()
            _uiState.update { it.copy(overview = overview) }
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadOverview", "Fehler: ${e.message}")
            return false
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

    private suspend fun loadHealth(): Boolean {
        try {
            val health = ApiClient.brainApi().health()
            _uiState.update { it.copy(health = health) }
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CortexLog.error("DashboardVM", "loadHealth", "Fehler: ${e.message}")
            return false
        }
    }

    fun search(query: String) {
        // Alte Suche IMMER canceln: sonst überschreibt eine langsam eintreffende Antwort
        // für „Zie" die bereits angezeigten Ergebnisse für „Ziele" (Race über den Tunnel).
        searchJob?.cancel()
        if (query.isBlank()) {
            // Beim Leeren des Suchfelds auch den EIGENEN Fehlerkanal raeumen: ohne aktive Suche
            // koennte ihn kein Erfolgsfall mehr loeschen — das Banner bliebe dauerhaft stehen.
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    error = it.error.takeUnless { msg -> msg == SEARCH_ERROR }
                )
            }
            return
        }
        searchJob = viewModelScope.launch {
            // Debounce: nicht pro Tastendruck ein POST /search über den schmalen Tunnel feuern —
            // erst wenn 300 ms lang kein weiteres Zeichen kam (jeder neue Aufruf cancelt oben).
            delay(300)
            try {
                val response = ApiClient.brainApi().search(
                    SearchRequest(query = query, limit = 20, category = _uiState.value.selectedCategory)
                )
                // distinctBy: doppelte doc_ids würden die LazyColumn (key = doc_id) crashen.
                // Nur übernehmen, wenn die Query noch aktuell ist (Schutz gegen veraltete Antworten).
                if (_uiState.value.searchQuery == query) {
                    _uiState.update {
                        it.copy(
                            searchResults = response.items.distinctBy { entry -> entry.doc_id },
                            // Erfolg raeumt den EIGENEN Fehler weg (nur den — Aktions-Fehler bleiben).
                            error = it.error.takeUnless { msg -> msg == SEARCH_ERROR }
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "search", "Fehler: ${e.message}")
                // Sichtbar melden: sonst standen unter der neuen Query kommentarlos die
                // (veralteten) Treffer der vorherigen Suche.
                if (_uiState.value.searchQuery == query) {
                    _uiState.update { it.copy(error = SEARCH_ERROR) }
                }
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
            // Kontext verlassen -> eigenen Browse-Fehler mit raeumen (sonst bliebe das
            // Banner ohne aktiven Browse-Kontext dauerhaft stehen).
            _uiState.update {
                it.copy(
                    browseResults = emptyList(),
                    error = it.error.takeUnless { msg -> msg == BROWSE_ERROR }
                )
            }
        }
    }

    fun drillIntoCategory(category: String) {
        val path = _uiState.value.categoryPath + category
        val fullCategory = path.joinToString("/")
        // subcategories SOFORT leeren: sonst blieben die klickbaren Zeilen der ALTEN Ebene bis
        // zur Server-Antwort unter dem neuen Breadcrumb stehen — ein Doppel-Tap baute darueber
        // Phantom-Pfade wie "A/x/x" ("Nichts gefunden." fuer eine nicht existierende Kategorie).
        _uiState.update { it.copy(categoryPath = path, selectedCategory = fullCategory, subcategories = emptyMap()) }
        loadSubcategories(fullCategory)
        browseCategory(fullCategory)
    }

    fun drillBack(toIndex: Int) {
        val path = _uiState.value.categoryPath.take(toIndex)
        _uiState.update { it.copy(categoryPath = path) }
        if (path.isEmpty()) {
            _uiState.update {
                it.copy(
                    subcategories = emptyMap(),
                    selectedCategory = null,
                    browseResults = emptyList(),
                    error = it.error.takeUnless { msg -> msg == BROWSE_ERROR }
                )
            }
        } else {
            val fullCategory = path.joinToString("/")
            // Auch hier subcategories sofort leeren (siehe drillIntoCategory): keine klickbaren
            // Zeilen der alten Ebene unter dem neuen Breadcrumb stehen lassen.
            _uiState.update { it.copy(selectedCategory = fullCategory, subcategories = emptyMap()) }
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
                browseResults = emptyList(),
                error = it.error.takeUnless { msg -> msg == BROWSE_ERROR }
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
        // Alten Job canceln + Ergebnis validieren: sonst überschreibt die langsame Antwort für
        // Ebene A die bereits angezeigten Subkategorien von Ebene B (Drilldown-Race).
        subcatJob?.cancel()
        subcatJob = viewModelScope.launch {
            try {
                val counts = ApiClient.brainApi().categoryCounts()
                val subs = mutableMapOf<String, Int>()
                val prefix = "$parentCategory/"
                counts.counts.forEach { (name, count) ->
                    if (name.startsWith(prefix)) {
                        val remainder = name.removePrefix(prefix)
                        val sub = remainder.substringBefore("/")
                        subs[sub] = (subs[sub] ?: 0) + count
                    }
                }
                if (_uiState.value.categoryPath.joinToString("/") == parentCategory) {
                    _uiState.update { it.copy(subcategories = subs) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "loadSubcategories", "Fehler: ${e.message}")
            }
        }
    }

    private fun browseCategory(category: String) {
        // Alten Browse-Job canceln + Ergebnis gegen die aktuell gewählte Kategorie validieren:
        // sonst zeigt ein langsamer Drilldown in A seine Einträge unter dem Breadcrumb von B.
        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            try {
                // byCategory + byParent sind unabhaengig — PARALLEL statt seriell abfragen:
                // ueber den langsamen WireGuard-Mobilfunk-Tunnel addierte sich vorher die Latenz
                // beider Roundtrips (Drilldown fuehlbar traege). Merge/Ergebnis bleibt identisch.
                // null = Teil-Request fehlgeschlagen (nicht "wirklich leer"): schlagen BEIDE fehl,
                // zeigte die App sonst faelschlich "Nichts gefunden.", als waere die Kategorie leer.
                val exactDeferred = async {
                    try {
                        ApiClient.brainApi().byCategory(category).items
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.warn("DashboardVM", "browseCategory", "byCategory fehlgeschlagen: ${e.message}")
                        null
                    }
                }
                val childDeferred = async {
                    try {
                        ApiClient.brainApi().byParent(category).items
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.warn("DashboardVM", "browseCategory", "byParent fehlgeschlagen: ${e.message}")
                        null
                    }
                }
                val exactItems = exactDeferred.await()
                val childItems = childDeferred.await()
                if (exactItems == null && childItems == null) {
                    if (_uiState.value.selectedCategory == category) {
                        // Auch die Liste leeren: sonst standen die Eintraege der VORHERIGEN
                        // Kategorie dauerhaft unter dem neuen Breadcrumb.
                        _uiState.update { it.copy(error = BROWSE_ERROR, browseResults = emptyList()) }
                    }
                    return@launch
                }
                val merged = (exactItems.orEmpty() + childItems.orEmpty())
                    .distinctBy { it.doc_id }
                    .sortedByDescending { it.updated_at ?: it.created_at ?: "" }
                if (_uiState.value.selectedCategory == category) {
                    _uiState.update {
                        it.copy(
                            browseResults = merged,
                            error = it.error.takeUnless { msg -> msg == BROWSE_ERROR }
                        )
                    }
                }
                CortexLog.info("DashboardVM", "browseCategory", "Kategorie-Unterbaum geladen", mapOf(
                    "category" to category,
                    "exact" to (exactItems?.size ?: -1),
                    "children" to (childItems?.size ?: -1),
                    "merged" to merged.size
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "browseCategory", "Fehler: ${e.message}")
                if (_uiState.value.selectedCategory == category) {
                    _uiState.update { it.copy(browseResults = emptyList()) }
                }
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
        // Beim AUSschalten wie "Abbrechen" behandeln: sonst blieben ungespeicherte Änderungen
        // in editText/editTitle stehen und die Leseansicht zeigte sie als vermeintlich gespeichert.
        if (_uiState.value.isEditing) {
            cancelEditing()
        } else {
            _uiState.update { it.copy(isEditing = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
        // Ziel-Eintrag beim KLICK merken: nach den zwei Server-Roundtrips (Anlegen + Registry)
        // koennte laengst ein anderer Eintrag offen sein — die Kategorie gehoerte sonst ihm.
        val targetDocId = _uiState.value.selectedEntry?.doc_id
        viewModelScope.launch {
            try {
                val resp = ApiClient.agentApi().createCategory(CreateCategoryRequest(name))
                val key = resp.key?.takeIf { it.isNotBlank() } ?: name   // kanonischer Kategorie-String
                loadCategories()   // Picker-Liste frisch aus der Registry (enthaelt jetzt die neue Kategorie)
                // Neue Kategorie SOFORT dem offenen Eintrag zuordnen (wie im Web-Drawer) — genau dafuer
                // legt Frank sie im „+ Kategorie"-Dialog an; addEntryCategory speichert per re-embed.
                if (targetDocId != null && _uiState.value.selectedEntry?.doc_id == targetDocId) {
                    addEntryCategory(key)
                }
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
                _uiState.update { it.replaceEntry(updatedEntry) }
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
        val clean = name.trim()
        if (clean.isEmpty()) return
        val targetDocId = _uiState.value.selectedEntry?.doc_id ?: return
        enqueueEntryMutation(targetDocId) { entry ->
            val cur = currentEntryCats(entry)
            when {
                cur.any { it.equals(clean, ignoreCase = true) } -> null   // schon zugeordnet
                cur.size >= 12 -> {
                    _uiState.update { it.copy(error = "Höchstens 12 Kategorien pro Eintrag") }
                    null
                }
                else -> cur + clean
            }
        }
    }

    /** Eine Kategorie entfernen (mindestens eine bleibt immer). */
    fun removeEntryCategory(name: String) {
        val targetDocId = _uiState.value.selectedEntry?.doc_id ?: return
        enqueueEntryMutation(targetDocId) { entry ->
            val cur = currentEntryCats(entry)
            if (cur.size <= 1) null
            else cur.filterNot { it.equals(name, ignoreCase = true) }.takeIf { it.isNotEmpty() }
        }
    }

    /** Serialisiert Kategorie-Änderungen: Der neue Auftrag wartet auf den vorherigen und liest
     *  den Eintrags-Stand erst DANACH. Vorher lasen zwei schnelle Änderungen beide den alten
     *  Stand — die zweite Server-Antwort verwarf die erste Kategorie (Lost Update).
     *  targetDocId = der Eintrag, der beim KLICK offen war: ist inzwischen ein anderer offen,
     *  wird der Auftrag verworfen — sonst aenderte die Mutation den FALSCHEN Eintrag. */
    private fun enqueueEntryMutation(targetDocId: String, computeNewCats: (BrainEntry) -> List<String>?) {
        val previous = categoryMutationJob
        categoryMutationJob = viewModelScope.launch {
            previous?.join()
            val entry = _uiState.value.selectedEntry ?: return@launch
            if (entry.doc_id != targetDocId) {
                CortexLog.warn("DashboardVM", "entryMutation",
                    "Kategorie-Auftrag verworfen — inzwischen ist ein anderer Eintrag geöffnet")
                return@launch
            }
            val newCats = computeNewCats(entry) ?: return@launch
            try {
                val resp = ApiClient.brainApi().setCategories(SetCategoriesRequest(entry.doc_id, newCats))
                val applied = resp.categories?.takeIf { it.isNotEmpty() } ?: newCats
                val updatedEntry = entry.copy(categories = applied, category = applied.first())
                _uiState.update { it.replaceEntry(updatedEntry) }
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
        // Leerer Text wuerde serverseitig mit 422 abgelehnt (UpdateReq.text min_length 1) und
        // waere ohnehin nie gewollt — klar melden statt Eintrag versehentlich zu leeren.
        if (_uiState.value.editText.isBlank()) {
            _uiState.update { it.copy(error = "Der Eintragstext darf nicht leer sein") }
            return
        }
        // EINMAL beim Klick capturen: Request und lokales Update nutzen dieselben Werte — sonst
        // zeigte die Leseansicht waehrend des Requests WEITERGETIPPTEN (nie gesendeten) Text
        // als gespeichert an.
        val targetDocId = entry.doc_id
        val editedText = _uiState.value.editText
        val editedTitle = _uiState.value.editTitle
        // Ueber dieselbe serielle Queue wie die Kategorie-Mutationen: parallel schrieben beide
        // sonst per replaceEntry ein VERALTETES entry.copy zurueck (Lost Update — der gerade
        // gespeicherte Text sprang nach der Kategorie-Antwort sichtbar auf den alten Stand).
        val previous = categoryMutationJob
        categoryMutationJob = viewModelScope.launch {
            previous?.join()
            val current = _uiState.value.selectedEntry ?: return@launch
            if (current.doc_id != targetDocId) return@launch
            try {
                ApiClient.brainApi().updateEntry(
                    UpdateEntryRequest(
                        doc_id = current.doc_id,
                        text = editedText,
                        title = editedTitle.ifBlank { null }
                    )
                )
                val updatedEntry = current.copy(
                    title = editedTitle.ifBlank { current.title },
                    text = editedText
                )
                _uiState.update { it.replaceEntry(updatedEntry).copy(isEditing = false) }
                CortexLog.info("DashboardVM", "saveEntry", "Eintrag gespeichert: ${current.doc_id}")
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

    private var deleteJob: Job? = null

    fun deleteEntry() {
        val entry = _uiState.value.selectedEntry ?: return
        // In-Flight-Guard: ein Doppel-Tap im Bestaetigungsdialog schickte sonst ein zweites
        // DELETE — der (bewusst idempotente) Server antwortet darauf mit deleted=false und die
        // App meldete faelschlich "Löschen fehlgeschlagen", obwohl das Loeschen erfolgreich war.
        if (deleteJob?.isActive == true) return
        deleteJob = viewModelScope.launch {
            try {
                val response = ApiClient.brainApi().deleteEntry(entry.doc_id)
                if (!response.ok || response.deleted != true) {
                    val detail = response.message ?: "Der Eintrag wurde auf dem Server nicht gefunden."
                    CortexLog.warn("DashboardVM", "deleteEntry", "Löschung nicht bestätigt: $detail", mapOf("doc_id" to entry.doc_id))
                    _uiState.update { it.copy(error = "Löschen fehlgeschlagen: $detail") }
                    return@launch
                }
                _uiState.update { it.withoutEntry(entry.doc_id) }
                CortexLog.info("DashboardVM", "deleteEntry", "Eintrag gelöscht: ${entry.doc_id}")
                refreshAll()
                _uiState.value.selectedCategory?.let(::browseCategory)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("DashboardVM", "deleteEntry", "Fehler: ${e.message}")
                _uiState.update { it.copy(error = "Löschen fehlgeschlagen: ${e.message}") }
            }
        }
    }
}
