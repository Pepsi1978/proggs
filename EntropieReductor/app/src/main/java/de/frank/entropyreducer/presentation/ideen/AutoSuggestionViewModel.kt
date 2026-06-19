package de.frank.entropyreducer.presentation.ideen

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.gewohnheitSuggestionStore
import de.frank.entropyreducer.data.kiTaskSuggestionStore
import de.frank.entropyreducer.data.local.dao.TaskSuggestionDao
import de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity
import de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase
import de.frank.entropyreducer.presentation.mental.Mental
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Agentic Auto-Suggestion: Wird bei JEDER neuen Idee automatisch aufgerufen.
 * Generiert Aufgaben- UND Gewohnheitsvorschläge mit EINEM Gemini-Aufruf.
 *
 * Frank-Wunsch 2026-06-18: Agentisches System — Idee speichern → Vorschläge sofort.
 */
@HiltViewModel
class AutoSuggestionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateSuggestions: GenerateSuggestionsUseCase,
    private val taskSuggestionDao: TaskSuggestionDao,
) : ViewModel() {

    /**
     * Wird nach dem Speichern einer neuen Idee aufgerufen.
     * Ein einziger Gemini-Aufruf generiert Aufgaben UND Gewohnheiten.
     */
    fun triggerSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ideas = ideenEntriesFlow(context).first()
                if (ideas.isEmpty()) return@launch

                // Kombinierter Aufruf: Tasks + Habits in einem
                val kiStore = context.kiTaskSuggestionStore
                val habitStore = context.gewohnheitSuggestionStore

                val taskProcessedIds = loadProcessedIds(kiStore, TASK_PROCESSED_KEY)
                val habitProcessedIds = loadProcessedIds(habitStore, HABIT_PROCESSED_KEY)

                // Ideen die weder in task noch in habit processed sind
                val allProcessedIds = taskProcessedIds + habitProcessedIds
                val (result, updatedProcessedIds) = generateSuggestions
                    .generateSuggestions(ideas, allProcessedIds)
                    .getOrThrow()

                // Tasks speichern (ab Etappe 2c in Room statt im kiStore-JSON)
                if (result.tasks.isNotEmpty()) {
                    storeKiTaskSuggestions(result.tasks)
                }

                // Habits speichern
                if (result.habits.isNotEmpty()) {
                    storeHabitSuggestions(habitStore, result.habits)
                }

                // Beide processedIds aktualisieren
                saveProcessedIds(kiStore, TASK_PROCESSED_KEY, updatedProcessedIds)
                saveProcessedIds(habitStore, HABIT_PROCESSED_KEY, updatedProcessedIds)
            }
        }
    }

    // ---- Private Helper ----

    private val TASK_PROCESSED_KEY =
        androidx.datastore.preferences.core.stringPreferencesKey("processed_idea_ids")
    private val HABIT_PROCESSED_KEY =
        androidx.datastore.preferences.core.stringPreferencesKey("habit_processed_idea_ids")
    private val SUGGESTIONS_KEY =
        androidx.datastore.preferences.core.stringPreferencesKey("suggestions_json")

    private suspend fun loadProcessedIds(
        store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
    ): Set<String> {
        return store.data.first().let { prefs ->
            val raw = prefs[key] ?: return@let emptySet()
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

    private suspend fun saveProcessedIds(
        store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        ids: Set<String>,
    ) {
        store.edit { prefs ->
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            prefs[key] = arr.toString()
        }
    }

    private suspend fun storeKiTaskSuggestions(
        newSuggestions: List<de.frank.entropyreducer.domain.usecase.AutoTaskSuggestion>,
    ) {
        // Ab ID-Architektur Etappe 2c in Room (task_suggestions). Etappe 2d: Herkunft der Quell-Idee
        // (originId/originType/rootId) mitschreiben.
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
    }

    private suspend fun storeHabitSuggestions(
        store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        newSuggestions: List<Mental>,
    ) {
        store.edit { prefs ->
            val raw = prefs[SUGGESTIONS_KEY]
            val existing = parseHabitJson(raw)
            prefs[SUGGESTIONS_KEY] = serializeHabitJson(existing + newSuggestions)
        }
    }

    private fun parseHabitJson(raw: String?): List<Mental> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(Mental(id = id, text = o.optString("text")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeHabitJson(items: List<Mental>): String {
        val arr = JSONArray()
        for (m in items) {
            arr.put(JSONObject().put("id", m.id).put("text", m.text))
        }
        return arr.toString()
    }
}
