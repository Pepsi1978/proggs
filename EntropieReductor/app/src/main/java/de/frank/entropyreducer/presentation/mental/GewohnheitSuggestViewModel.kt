package de.frank.entropyreducer.presentation.mental

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.gewohnheitSuggestionStore
import de.frank.entropyreducer.data.local.dao.HabitSuggestionDao
import de.frank.entropyreducer.data.local.entities.HabitSuggestionEntity
import de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase
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

private val HABIT_PROCESSED_KEY = stringPreferencesKey("habit_processed_idea_ids")

enum class SuggestState { IDLE, LOADING }

@HiltViewModel
class GewohnheitSuggestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateSuggestions: GenerateSuggestionsUseCase,
    private val habitSuggestionDao: HabitSuggestionDao,
) : ViewModel() {

    // DataStore weiterhin NUR fuer habit_processed_idea_ids (wird erst mit der Dedup-Ablösung
    // abgeloest). Die Vorschlaege selbst liegen ab Etappe 3c in Room (habit_suggestions).
    private val store = context.gewohnheitSuggestionStore

    private val _state = MutableStateFlow(SuggestState.IDLE)
    val state: StateFlow<SuggestState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Ab ID-Architektur Etappe 3c aus Room (habit_suggestions) statt DataStore-JSON.
    val suggestions: StateFlow<List<Mental>> =
        habitSuggestionDao.getAll()
            .map { rows -> rows.map { Mental(id = it.id, text = it.text) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissError() { _error.value = null }

    fun generateSuggestions() {
        if (_state.value == SuggestState.LOADING) return
        viewModelScope.launch {
            _state.value = SuggestState.LOADING
            _error.value = null
            runCatching {
                val ideas = ideenEntriesFlow(context).first()
                val processedIds = loadProcessedIds()
                val (result, updatedProcessedIds) = generateSuggestions
                    .generateSuggestions(ideas, processedIds)
                    .getOrThrow()
                storeSuggestions(result.habits)
                saveProcessedIds(updatedProcessedIds)
                if (result.habits.isEmpty() && ideas.isNotEmpty()) {
                    _error.value = "Alle Ideen wurden bereits als Vorschläge verarbeitet."
                } else if (result.habits.isEmpty()) {
                    _error.value = "Keine Ideen vorhanden — zuerst Ideen eingeben."
                }
            }.onFailure { ex ->
                _error.value = ex.message ?: "Vorschlag-Generierung fehlgeschlagen"
            }
            _state.value = SuggestState.IDLE
        }
    }

    fun acceptSuggestion(id: String) {
        viewModelScope.launch {
            // Entfernt nur den Vorschlag aus Room. Das Anlegen der Gewohnheit (inkl. Herkunft, 3d)
            // passiert separat ueber addGewohnheit / Drag-Promotion im GewohnheitBoardScreen.
            habitSuggestionDao.deleteById(id)
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: angenommen")
        }
    }

    fun deleteSuggestion(id: String) {
        viewModelScope.launch {
            habitSuggestionDao.deleteById(id)
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: verworfen")
        }
    }

    fun addSuggestion(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val m = Mental.create(clean)
            habitSuggestionDao.upsert(
                HabitSuggestionEntity(id = m.id, text = m.text, createdAt = System.currentTimeMillis())
            )
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: hinzugefuegt")
        }
    }

    private fun storeSuggestions(newSuggestions: List<Mental>) {
        viewModelScope.launch {
            // Herkunft (originId/originType/rootId) wird erst in Etappe 3d gesetzt (Idee -> Vorschlag).
            val nowMs = System.currentTimeMillis()
            habitSuggestionDao.upsertAll(
                newSuggestions.mapIndexed { index, m ->
                    HabitSuggestionEntity(id = m.id, text = m.text, createdAt = nowMs + index)
                }
            )
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: generiert")
        }
    }

    fun resetProcessedIdeas() {
        viewModelScope.launch {
            store.edit { prefs ->
                prefs[HABIT_PROCESSED_KEY] = "[]"
            }
            _error.value = null
        }
    }

    private suspend fun loadProcessedIds(): Set<String> {
        return store.data.first().let { prefs ->
            val raw = prefs[HABIT_PROCESSED_KEY] ?: return@let emptySet()
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

    private suspend fun saveProcessedIds(ids: Set<String>) {
        store.edit { prefs ->
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            prefs[HABIT_PROCESSED_KEY] = arr.toString()
        }
    }
}
