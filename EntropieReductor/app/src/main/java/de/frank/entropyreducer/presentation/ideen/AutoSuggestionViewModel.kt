package de.frank.entropyreducer.presentation.ideen

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.gewohnheitSuggestionStore
import de.frank.entropyreducer.data.kiTaskSuggestionStore
import de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase
import de.frank.entropyreducer.presentation.dashboard1.KiTaskSuggestion
import de.frank.entropyreducer.presentation.mental.Mental
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Agentic Auto-Suggestion: Wird bei JEDER neuen Idee automatisch aufgerufen.
 * Generiert Aufgaben- UND Gewohnheitsvorschläge ohne Button-Druck.
 *
 * Frank-Wunsch 2026-06-18: Agentisches System — Idee speichern → Vorschläge sofort.
 */
@HiltViewModel
class AutoSuggestionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateSuggestions: GenerateSuggestionsUseCase,
) : ViewModel() {

    /**
     * Wird nach dem Speichern einer neuen Idee aufgerufen.
     * Generiert im Hintergrund both Aufgaben- als auch Gewohnheitsvorschläge.
     * Nur Ideen die noch nicht verarbeitet wurden, werden an Gemini geschickt.
     */
    fun triggerSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ideas = ideenEntriesFlow(context).first()
                if (ideas.isEmpty()) return@launch

                // Task-Vorschläge
                val kiStore = context.kiTaskSuggestionStore
                val taskProcessedIds = loadProcessedIds(kiStore, TASK_PROCESSED_KEY)
                val (newTasks, updatedTaskProcessedIds) = generateSuggestions
                    .generateTaskSuggestions(ideas, taskProcessedIds)
                    .getOrThrow()
                if (newTasks.isNotEmpty()) {
                    storeKiTaskSuggestions(kiStore, newTasks)
                }
                saveProcessedIds(kiStore, TASK_PROCESSED_KEY, updatedTaskProcessedIds)

                // Gewohnheitsvorschläge
                val habitStore = context.gewohnheitSuggestionStore
                val habitProcessedIds = loadProcessedIds(habitStore, HABIT_PROCESSED_KEY)
                val (newHabits, updatedHabitProcessedIds) = generateSuggestions
                    .generateHabitSuggestions(ideas, habitProcessedIds)
                    .getOrThrow()
                if (newHabits.isNotEmpty()) {
                    storeHabitSuggestions(habitStore, newHabits)
                }
                saveProcessedIds(habitStore, HABIT_PROCESSED_KEY, updatedHabitProcessedIds)
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
        store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        newSuggestions: List<de.frank.entropyreducer.domain.usecase.AutoTaskSuggestion>,
    ) {
        store.edit { prefs ->
            val raw = prefs[SUGGESTIONS_KEY]
            val existing = parseKiTaskJson(raw)
            val mapped = newSuggestions.map {
                KiTaskSuggestion(id = it.id, title = it.title, description = it.description)
            }
            prefs[SUGGESTIONS_KEY] = serializeKiTaskJson(existing + mapped)
        }
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

    private fun parseKiTaskJson(raw: String?): List<KiTaskSuggestion> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(KiTaskSuggestion(id = id, title = o.optString("title"), description = o.optString("description")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeKiTaskJson(items: List<KiTaskSuggestion>): String {
        val arr = JSONArray()
        for (item in items) {
            arr.put(JSONObject().put("id", item.id).put("title", item.title).put("description", item.description))
        }
        return arr.toString()
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
