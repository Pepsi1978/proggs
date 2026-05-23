package de.frank.entropyreducer.presentation.settings.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogDao
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogEntry
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel fuer den Diagnose-Screen (Frank-Wunsch 2026-05-23). Beobachtet die Diagnose-Log-DB
 * live und erlaubt Filtern nach Bereich, Loeschen und Export.
 */
@HiltViewModel
class DiagnosticLogViewModel
@Inject
constructor(
    private val dao: DiagnosticLogDao,
) : ViewModel() {

    private val _selectedArea = MutableStateFlow<DiagnosticArea?>(null)
    val selectedArea: StateFlow<DiagnosticArea?> = _selectedArea.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<DiagnosticLogEntry>> =
        _selectedArea
            .flatMapLatest { area ->
                if (area == null) dao.observeRecent(LIST_LIMIT)
                else dao.observeByArea(area.name, LIST_LIMIT)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(area: DiagnosticArea?) {
        _selectedArea.value = area
    }

    fun clearLog() {
        viewModelScope.launch { runCatching { dao.clearAll() } }
    }

    /**
     * Baut einen lesbaren Text-Export aller aktuellen Eintraege (neueste zuerst). Wird vom Screen
     * an einen ACTION_SEND-Intent uebergeben, damit Frank das Protokoll z.B. per Mail teilen kann.
     */
    suspend fun buildExportText(): String {
        val snapshot = runCatching { dao.snapshot(EXPORT_LIMIT) }.getOrDefault(emptyList())
        if (snapshot.isEmpty()) return "Entropie Reductor — Diagnose-Protokoll: keine Eintraege."
        val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.GERMANY)
        return buildString {
            append("Entropie Reductor — Diagnose-Protokoll\n")
            append("Exportiert: ${df.format(java.util.Date())}\n")
            append("Eintraege: ${snapshot.size}\n")
            append("============================================\n\n")
            snapshot.forEach { e ->
                val area = DiagnosticArea.fromNameOrNull(e.area)?.displayName ?: e.area
                append("[${df.format(java.util.Date(e.timestampMs))}] ${e.level} · $area\n")
                append(e.message).append('\n')
                e.details?.takeIf { it.isNotBlank() }?.let { append(it).append('\n') }
                append("--------------------------------------------\n")
            }
        }
    }

    private companion object {
        const val LIST_LIMIT = 500
        const val EXPORT_LIMIT = 2000
    }
}
