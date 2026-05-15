package com.entropyjournal.ui.screens.dashboard

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.dao.JournalEntryDao
import com.entropyjournal.data.prefs.CustomAnalysesStore
import com.entropyjournal.data.repository.AdviceRepository
import com.entropyjournal.domain.usecase.AnalyzeEntropyUseCase
import com.entropyjournal.domain.usecase.GenerateAdviceUseCase
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isAutoUpdate: Boolean = false,
    val selectedCategoryIndex: Int = 0,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val currentScenario: Int = 1,
    val isScenarioSwitch: Boolean = false,
    val isDeleteUpdate: Boolean = false,
    val customHeaderTop5: String = "",
    val customHeaderAnalyse: String = "",
    val customHeaderErgebnisse: String = "",
    // B3 — true wenn das aktive Custom-Profil keinen Fokustext hat. UI zeigt
    // dann eine Hinweis-Karte statt des Dashboards, damit der Benutzer den
    // Schritt nicht uebersieht.
    val emptyCustomPromptWarning: Boolean = false,
    // C1 — sichtbarer Profil-Header (Profilname + Kurzfokus). Greift fuer alle
    // Profile (eingebaute und custom). Wird im Dashboard oben angezeigt.
    val activeProfileLabel: String = "",
    val activeProfileFocus: String = "",
    // A2 — fokus_kern und fokus_zitate aus der KI-Antwort fuer Custom-Profile.
    val customFokusKern: String = "",
    val customFokusZitateJson: String = "",
)

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    private val generateAdviceUseCase: GenerateAdviceUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val adviceRepository: AdviceRepository,
    private val journalEntryDao: JournalEntryDao,
    private val encryptedPrefs: SharedPreferences,
) : ViewModel() {

    val adviceBlocks =
        generateAdviceUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    private var manualRefreshActive = false

    // CRITICAL: these two properties must be declared BEFORE the init block.
    // viewModelScope.launch uses Dispatchers.Main.immediate (coroutines ≥ 1.6), so the
    // polling coroutine runs INLINE on the current thread until its first suspension
    // point (delay(500)). If these properties were declared after init, they would
    // still be 0L during that first inline pass — and any Pref that had ever been
    // touched in the past (e.g. the user toggled 'Längere Version' once) would look
    // strictly greater than 0L and spuriously trigger clearDashboard + refreshDashboard
    // on every cold start. Declaring them here guarantees the initial Pref read.
    private var lastCustomPromptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
    private var lastVerboseChangedAt =
        encryptedPrefs.getLong(Constants.PREF_VERBOSE_DASHBOARD_CHANGED_AT, 0L)

    init {
        // PREF_DASHBOARD_UPDATING / PREF_DASHBOARD_UPDATE_IS_DELETE are runtime-only
        // flags used for cross-ViewModel communication during a live analysis. If the
        // app was killed mid-analysis (OOM, swipe-away, crash), the finally-blocks that
        // reset them never ran, leaving stale `true` values in persistent prefs. The
        // polling loop below would then render the "Dashboard wird aktualisiert" state
        // on every cold start even though nothing is actually running. Clear them here.
        encryptedPrefs
            .edit()
            .putBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
            .putBoolean(Constants.PREF_DASHBOARD_UPDATE_IS_DELETE, false)
            .apply()

        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        _uiState.value =
            _uiState.value.copy(
                currentScenario = scenario,
                customHeaderTop5 = encryptedPrefs.getString("custom_header_top5", "") ?: "",
                customHeaderAnalyse = encryptedPrefs.getString("custom_header_analyse", "") ?: "",
                customHeaderErgebnisse =
                    encryptedPrefs.getString("custom_header_ergebnisse", "") ?: "",
                // Bug-Fix 2026-05-15: customFokusKern nur bei Custom-Profil aus Prefs lesen,
                // sonst stehen die Daten vom letzten Custom-Profil-Refresh fuer alle anderen
                // Profile in der UI sichtbar.
                customFokusKern =
                    if (scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX)
                        encryptedPrefs.getString("custom_fokus_kern", "") ?: ""
                    else "",
                customFokusZitateJson =
                    if (scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX)
                        encryptedPrefs.getString("custom_fokus_zitate_json", "") ?: ""
                    else "",
                activeProfileLabel = computeProfileLabel(scenario),
                activeProfileFocus = computeProfileFocus(scenario),
                emptyCustomPromptWarning = isEmptyCustomProfile(scenario),
            )
        // Poll for background dashboard updates triggered by JournalViewModel
        viewModelScope.launch {
            while (true) {
                val updating = encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
                if (updating != _uiState.value.isLoading && !manualRefreshActive) {
                    val isDelete =
                        encryptedPrefs.getBoolean(Constants.PREF_DASHBOARD_UPDATE_IS_DELETE, false)
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = updating,
                            isAutoUpdate = updating,
                            isDeleteUpdate = isDelete,
                        )
                }
                val currentScenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                if (currentScenario != _uiState.value.currentScenario) {
                    val emptyWarn = isEmptyCustomProfile(currentScenario)
                    // Bug-Fix 2026-05-15: customFokusKern/Zitate beim Wechsel zuruecksetzen,
                    // damit nicht die Werte vom alten Profil sichtbar bleiben.
                    _uiState.value =
                        _uiState.value.copy(
                            currentScenario = currentScenario,
                            isScenarioSwitch = !emptyWarn,
                            activeProfileLabel = computeProfileLabel(currentScenario),
                            activeProfileFocus = computeProfileFocus(currentScenario),
                            emptyCustomPromptWarning = emptyWarn,
                            customFokusKern = "",
                            customFokusZitateJson = "",
                        )
                    adviceRepository.clearDashboard()
                    if (currentScenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                        val customPrompt =
                            CustomAnalysesStore.activePromptOrEmpty(
                                encryptedPrefs,
                                currentScenario,
                            )
                        if (customPrompt.isNotBlank()) {
                            refreshDashboard()
                        }
                        // Bei leerem Custom-Prompt: KEIN refresh, aber emptyCustomPromptWarning
                        // ist oben bereits gesetzt — UI zeigt Hinweis-Karte.
                    } else {
                        refreshDashboard()
                    }
                }
                val promptSavedAt = encryptedPrefs.getLong("custom_prompt_saved_at", 0L)
                if (promptSavedAt > lastCustomPromptSavedAt && promptSavedAt > 0L) {
                    lastCustomPromptSavedAt = promptSavedAt
                    val active = _uiState.value.currentScenario
                    if (active >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                        val customPrompt =
                            CustomAnalysesStore.activePromptOrEmpty(encryptedPrefs, active)
                        adviceRepository.clearDashboard()
                        if (customPrompt.isNotBlank()) {
                            _uiState.value =
                                _uiState.value.copy(
                                    isScenarioSwitch = true,
                                    activeProfileLabel = computeProfileLabel(active),
                                    activeProfileFocus = computeProfileFocus(active),
                                    emptyCustomPromptWarning = false,
                                )
                            refreshDashboard()
                        } else {
                            // Custom-Prompt wurde geleert — Warn-Card statt stillem Skip
                            _uiState.value =
                                _uiState.value.copy(
                                    activeProfileLabel = computeProfileLabel(active),
                                    activeProfileFocus = "",
                                    emptyCustomPromptWarning = true,
                                )
                        }
                    }
                }
                // Re-run analysis whenever the "Längere Version" switch flips so
                // the user sees the new length immediately without leaving settings.
                val verboseChangedAt =
                    encryptedPrefs.getLong(Constants.PREF_VERBOSE_DASHBOARD_CHANGED_AT, 0L)
                if (verboseChangedAt > lastVerboseChangedAt && verboseChangedAt > 0L) {
                    lastVerboseChangedAt = verboseChangedAt
                    adviceRepository.clearDashboard()
                    _uiState.value = _uiState.value.copy(isScenarioSwitch = true)
                    refreshDashboard()
                }
                kotlinx.coroutines.delay(500)
            }
        }

        // Auto-generate dashboard only on first load after an ACTUAL Google restore.
        // Previously this block ran on every cold start, so if blockCount happened to
        // be 0 (empty dashboard DB for any reason) it would fire an AI refresh even
        // though the user had not added any new entries. We now only trigger the
        // refresh when PREF_RESTORE_PENDING was actually true when the VM started —
        // i.e. the user really came back from a Drive restore.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hadRestore =
                    encryptedPrefs.getBoolean(Constants.PREF_RESTORE_PENDING, false)
                awaitRestoreComplete()
                if (!hadRestore) return@launch

                val blockCount = adviceRepository.getBlockCount()
                val entryCount = journalEntryDao.getEntryCount()
                if (blockCount == 0 && entryCount > 0) {
                    Log.d(
                        "DashboardVM",
                        "Dashboard empty after real restore, generating from $entryCount entries",
                    )
                    refreshDashboard()
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Auto-generate after restore failed: ${e.message}", e)
            }
        }
    }

    /** Waits for the Google Drive restore flag to clear (max 10 minutes). */
    private suspend fun awaitRestoreComplete() {
        val maxWaitMs = 10L * 60 * 1000
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < maxWaitMs) {
            val restorePending =
                encryptedPrefs.getBoolean(Constants.PREF_RESTORE_PENDING, false)
            if (!restorePending) break
            Log.d("DashboardVM", "Restore in progress, waiting...")
            kotlinx.coroutines.delay(3000)
        }
    }

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            manualRefreshActive = true
            _uiState.value =
                _uiState.value.copy(isLoading = true, isAutoUpdate = false, errorMessage = null)
            analyzeEntropyUseCase(freshAnalysis = true)
                .onSuccess {
                    encryptedPrefs
                        .edit()
                        .putLong(
                            "dashboard_last_updated_${_uiState.value.currentScenario}",
                            System.currentTimeMillis(),
                        )
                        .apply()
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            canUndo = adviceRepository.canUndo,
                            selectedCategoryIndex = 0,
                            isScenarioSwitch = false,
                            customHeaderTop5 =
                                encryptedPrefs.getString("custom_header_top5", "") ?: "",
                            customHeaderAnalyse =
                                encryptedPrefs.getString("custom_header_analyse", "") ?: "",
                            customHeaderErgebnisse =
                                encryptedPrefs.getString("custom_header_ergebnisse", "") ?: "",
                            customFokusKern =
                                encryptedPrefs.getString("custom_fokus_kern", "") ?: "",
                            customFokusZitateJson =
                                encryptedPrefs.getString("custom_fokus_zitate_json", "") ?: "",
                        )
                    // Auto-hide undo button after 5 seconds
                    if (adviceRepository.canUndo) {
                        kotlinx.coroutines.delay(5_000)
                        _uiState.value = _uiState.value.copy(canUndo = false)
                    }
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            isScenarioSwitch = false,
                            errorMessage = error.message ?: "Analyse fehlgeschlagen",
                        )
                }
            manualRefreshActive = false
        }
    }

    fun getCustomPrompt(): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return CustomAnalysesStore.activePromptOrEmpty(encryptedPrefs, scenario)
    }

    fun undoDashboard() {
        viewModelScope.launch {
            val success = adviceRepository.undoLastRefresh()
            _uiState.value = _uiState.value.copy(canUndo = false, selectedCategoryIndex = 0)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // C1 — Helper: lesbare Bezeichnung des aktiven Profils
    private fun computeProfileLabel(scenario: Int): String {
        return when {
            scenario == 0 -> "Zusammenfassung"
            scenario == 1 -> "Entropie"
            scenario == 2 -> "Selbsterkenntnis"
            scenario == 3 -> "Persönliche Ziele"
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX ->
                CustomAnalysesStore.activeNameOrDefault(encryptedPrefs, scenario)
            else -> "Profil"
        }
    }

    // C1 — Helper: kurzer Fokus-Text fuer den Header (max 200 Zeichen).
    // Bei eingebauten Profilen: kurze Beschreibung. Bei Custom: erste 200 Zeichen
    // des Profil-Prompts.
    private fun computeProfileFocus(scenario: Int): String {
        return when {
            scenario == 0 -> "Erzählende Zusammenfassung der wichtigsten Ereignisse und Gefühle."
            scenario == 1 -> "Klassische Entropie-Analyse mit Stress- und Belastungsmustern."
            scenario == 2 -> "Tiefgründige Selbsterkenntnis: Denkmuster und verborgene Stärken."
            scenario == 3 -> "Motivierender Ziel-Begleiter mit Fortschritts-Sicht."
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX -> {
                val prompt = CustomAnalysesStore.activePromptOrEmpty(encryptedPrefs, scenario)
                if (prompt.isBlank()) ""
                else if (prompt.length > 200) prompt.take(197) + "..."
                else prompt
            }
            else -> ""
        }
    }

    // B3 — Helper: ist das aktive Profil ein Custom-Profil mit leerem Prompt?
    private fun isEmptyCustomProfile(scenario: Int): Boolean {
        if (scenario < Constants.FIRST_CUSTOM_SCENARIO_INDEX) return false
        return CustomAnalysesStore.activePromptOrEmpty(encryptedPrefs, scenario).isBlank()
    }

    fun getLastUpdatedText(): String? {
        val ts =
            encryptedPrefs.getLong("dashboard_last_updated_${_uiState.value.currentScenario}", 0L)
        if (ts == 0L) return null
        val sdf = java.text.SimpleDateFormat("dd.MM. 'um' HH:mm", java.util.Locale.GERMAN)
        return "Letzte Aktualisierung am ${sdf.format(java.util.Date(ts))}"
    }
}
