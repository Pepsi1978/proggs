package de.frank.entropyreducer.presentation.dashboard1

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.kiTaskSuggestionStore
import de.frank.entropyreducer.data.local.dao.TaskSuggestionDao
import de.frank.entropyreducer.data.local.entities.OriginType
import de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.usecase.AutoTaskSuggestion
import de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
import de.frank.entropyreducer.presentation.ideen.ideenEntriesFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

data class KiTaskSuggestion(
    val id: String,
    val title: String,
    val description: String,
) {
    companion object {
        fun create(title: String, description: String): KiTaskSuggestion =
            KiTaskSuggestion(
                id = java.util.UUID.randomUUID().toString(),
                title = title.take(60),
                description = description.take(500),
            )
    }
}

enum class KiTaskSuggestState { IDLE, LOADING }

private val KEY_PROCESSED_IDEAS = stringPreferencesKey("processed_idea_ids")

@HiltViewModel
class KiTaskSuggestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateSuggestions: GenerateSuggestionsUseCase,
    private val process: ProcessEntryUseCase,
    private val taskSuggestionDao: TaskSuggestionDao,
) : ViewModel() {

    // DataStore weiterhin NUR fuer processed_idea_ids (wird erst in Etappe 2d durch die ID-Kette
    // abgeloest). Die Vorschlaege selbst liegen ab Etappe 2c in Room (task_suggestions).
    private val store = context.kiTaskSuggestionStore

    private val _state = MutableStateFlow(KiTaskSuggestState.IDLE)
    val state: StateFlow<KiTaskSuggestState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _acceptingId = MutableStateFlow<String?>(null)
    val acceptingId: StateFlow<String?> = _acceptingId.asStateFlow()

    // Ab ID-Architektur Etappe 2c aus Room (task_suggestions) statt DataStore-JSON.
    val suggestions: StateFlow<List<KiTaskSuggestion>> =
        taskSuggestionDao.getAll()
            .map { rows ->
                rows.map {
                    KiTaskSuggestion(id = it.id, title = it.title, description = it.description)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Sofort-Heal beim Oeffnen des Reiters (Frank-Wunsch 2026-06-20): verwaiste/verbrauchte
        // Vorschlaege sofort wegraeumen, ohne auf den naechsten Drive-Restore zu warten.
        viewModelScope.launch {
            runCatching { de.frank.entropyreducer.data.healOrphanedSuggestions(context) }
        }
    }

    fun dismissError() { _error.value = null }

    fun generateSuggestions() {
        if (_state.value == KiTaskSuggestState.LOADING) return
        viewModelScope.launch {
            _state.value = KiTaskSuggestState.LOADING
            _error.value = null
            runCatching {
                val ideas = ideenEntriesFlow(context).first()
                val processedIds = loadProcessedIdeaIds()
                val (result, updatedProcessedIds) = generateSuggestions
                    .generateSuggestions(ideas, processedIds)
                    .getOrThrow()
                if (result.tasks.isNotEmpty()) {
                    storeSuggestions(result.tasks)
                }
                saveProcessedIdeaIds(updatedProcessedIds)
                if (result.tasks.isEmpty() && ideas.isNotEmpty()) {
                    _error.value = "Alle Ideen wurden bereits als Vorschlag verarbeitet."
                }
            }.onFailure { ex ->
                _error.value = ex.message ?: "Vorschlag-Generierung fehlgeschlagen"
            }
            _state.value = KiTaskSuggestState.IDLE
        }
    }

    fun acceptSuggestion(suggestion: KiTaskSuggestion) {
        viewModelScope.launch {
            _acceptingId.value = suggestion.id
            val text = "${suggestion.title}. ${suggestion.description}"
            // ID-Architektur Etappe 2d: Herkunft Vorschlag -> Aufgabe durchreichen, damit die Kette
            // Idee -> Vorschlag -> Aufgabe luckenlos ist. rootId erbt die urspruengliche Idee.
            val sug = taskSuggestionDao.getById(suggestion.id)
            process(
                text,
                EntrySource.NUTZER_TEXT,
                originId = sug?.id,
                originType = sug?.let { OriginType.TASK_SUGGESTION },
                rootId = sug?.rootId ?: sug?.id,
            )
                .onSuccess { entry ->
                    // Direktive #3 robust (Frank-Wunsch 2026-06-20): Annehmen ist das staerkste
                    // "diese Idee ist erledigt"-Signal. Die Quell-Idee dauerhaft als verarbeitet
                    // markieren — zweite Schutzschicht neben dem Endpunkt-Ketten-Dedup (countByRootId),
                    // greift auch nachdem die angenommene Aufgabe spaeter geloescht wurde.
                    val ideaId = sug?.rootId ?: sug?.originId
                    if (ideaId != null) {
                        saveProcessedIdeaIds(loadProcessedIdeaIds() + ideaId)
                    }
                    // Accept-Live-Sonde (Frank-Wunsch 2026-06-20): bestaetigt den Herkunfts-Uebergang
                    // Vorschlag -> Aufgabe live (erwartet vs. tatsaechlich). ok=false faellt im Log auf.
                    val expectedRoot = sug?.rootId ?: sug?.id
                    Diag.i(
                        DiagnosticArea.AGENTIC,
                        "AcceptLineage",
                        "CHECKPOINT Annehmen Aufgabe '${entry.title.take(28)}': erwartet rootId=${expectedRoot ?: "-"} " +
                            "tatsaechlich originId=${entry.originId ?: "NULL!"} originType=${entry.originType ?: "-"} " +
                            "rootId=${entry.rootId ?: "NULL!"} ok=${entry.rootId != null && entry.rootId == expectedRoot}",
                    )
                    removeSuggestion(suggestion.id)
                }
                .onFailure { ex ->
                    _error.value = ex.message ?: "Aufgabe konnte nicht uebernommen werden"
                }
            _acceptingId.value = null
        }
    }

    fun deleteSuggestion(id: String) {
        removeSuggestion(id)
    }

    private fun removeSuggestion(id: String) {
        viewModelScope.launch {
            taskSuggestionDao.deleteById(id)
            // Frank-Wunsch 2026-06-20: Loeschung propagieren (Tombstone) — sonst behaelt ein 2. Geraet
            // seine Kopie und der angenommene/verworfene Vorschlag taucht beim Restore wieder auf.
            de.frank.entropyreducer.data.markDeleted(
                context, de.frank.entropyreducer.data.TombstoneType.TASK_SUGGESTION, id)
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Aufgabenvorschlag: entfernt")
        }
    }

    private fun storeSuggestions(newSuggestions: List<AutoTaskSuggestion>) {
        viewModelScope.launch {
            // ID-Architektur Etappe 2d: Herkunft (originId/originType/rootId) der Quell-Idee mitschreiben.
            val nowMs = System.currentTimeMillis()
            taskSuggestionDao.upsertAll(
                newSuggestions.mapIndexed { index, s ->
                    TaskSuggestionEntity(
                        id = s.id,
                        title = s.title,
                        description = s.description,
                        createdAt = nowMs + index,
                        originId = s.originId,
                        originType = s.originType,
                        rootId = s.rootId,
                    )
                }
            )
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Aufgabenvorschlag: generiert")
        }
    }

    private suspend fun loadProcessedIdeaIds(): Set<String> {
        return store.data.first().let { prefs ->
            val raw = prefs[KEY_PROCESSED_IDEAS] ?: return@let emptySet()
            runCatching {
                val arr = JSONArray(raw)
                buildSet(arr.length()) {
                    for (i in 0 until arr.length()) {
                        arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
            }.getOrDefault(emptySet())
        }
    }

    private suspend fun saveProcessedIdeaIds(ids: Set<String>) {
        store.edit { prefs ->
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            prefs[KEY_PROCESSED_IDEAS] = arr.toString()
        }
    }

    fun resetProcessedIdeas() {
        viewModelScope.launch {
            store.edit { prefs ->
                prefs[KEY_PROCESSED_IDEAS] = "[]"
            }
            _error.value = null
        }
    }
}
