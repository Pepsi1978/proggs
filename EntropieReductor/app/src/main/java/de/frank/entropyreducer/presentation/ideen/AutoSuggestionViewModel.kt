package de.frank.entropyreducer.presentation.ideen

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.gewohnheitSuggestionStore
import de.frank.entropyreducer.data.kiTaskSuggestionStore
import de.frank.entropyreducer.data.local.dao.HabitSuggestionDao
import de.frank.entropyreducer.data.local.dao.TaskSuggestionDao
import de.frank.entropyreducer.data.local.entities.HabitSuggestionEntity
import de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity
import de.frank.entropyreducer.data.safety.PhoneContentGuard
import de.frank.entropyreducer.domain.usecase.AutoHabitSuggestion
import de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase
import de.frank.entropyreducer.util.runCatchingCancellable
import de.frank.entropyreducer.presentation.tagebuch.TagebuchArea
import de.frank.entropyreducer.presentation.tagebuch.tagebuchEntriesFlow
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray

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
    private val habitSuggestionDao: HabitSuggestionDao,
) : ViewModel() {

    /**
     * Wird nach dem Speichern einer neuen Idee aufgerufen.
     * Ein einziger Gemini-Aufruf generiert Aufgaben UND Gewohnheiten.
     */
    fun triggerSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            generateSuggestions.serializedGeneration {
                runCatchingCancellable {
                    val ideas = ideenEntriesFlow(context).first()
                    if (ideas.isEmpty()) return@serializedGeneration

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

                    // Habits speichern (ab Etappe 3c/3d in Room mit Herkunft)
                    if (result.habits.isNotEmpty()) {
                        storeHabitSuggestions(result.habits)
                    }

                    // Beide processedIds aktualisieren
                    saveProcessedIds(kiStore, TASK_PROCESSED_KEY, updatedProcessedIds)
                    saveProcessedIds(habitStore, HABIT_PROCESSED_KEY, updatedProcessedIds)
                }
            }
        }
    }

    /** Wird nach dem Speichern eines Lerneintrags aufgerufen und speichert beide Vorschlagsarten. */
    fun triggerLearningSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            generateSuggestions.serializedGeneration {
                runCatchingCancellable {
                    val entries = tagebuchEntriesFlow(context, TagebuchArea.LEARNING).first()
                    if (entries.isEmpty()) return@serializedGeneration

                    val taskStore = context.kiTaskSuggestionStore
                    val habitStore = context.gewohnheitSuggestionStore
                    val processedIds =
                        loadProcessedIds(taskStore, LEARNING_TASK_PROCESSED_KEY) +
                            loadProcessedIds(habitStore, LEARNING_HABIT_PROCESSED_KEY)
                    val (result, updatedProcessedIds) =
                        generateSuggestions.generateLearningSuggestions(entries, processedIds).getOrThrow()

                    if (result.tasks.isNotEmpty()) storeKiTaskSuggestions(result.tasks)
                    if (result.habits.isNotEmpty()) storeHabitSuggestions(result.habits)
                    saveProcessedIds(taskStore, LEARNING_TASK_PROCESSED_KEY, updatedProcessedIds)
                    saveProcessedIds(habitStore, LEARNING_HABIT_PROCESSED_KEY, updatedProcessedIds)
                }
            }
        }
    }

    // ---- Private Helper ----

    private val TASK_PROCESSED_KEY =
        androidx.datastore.preferences.core.stringPreferencesKey("processed_idea_ids")
    private val HABIT_PROCESSED_KEY =
        androidx.datastore.preferences.core.stringPreferencesKey("habit_processed_idea_ids")
    private val LEARNING_TASK_PROCESSED_KEY =
        androidx.datastore.preferences.core.stringPreferencesKey("processed_learning_ids")
    private val LEARNING_HABIT_PROCESSED_KEY =
        androidx.datastore.preferences.core.stringPreferencesKey("habit_processed_learning_ids")

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
        val safeSuggestions = newSuggestions.filterNot { s ->
            PhoneContentGuard.isSecondBrainWorkArtifact(s.title, s.description)
        }
        if (safeSuggestions.isEmpty()) return
        val nowMs = System.currentTimeMillis()
        taskSuggestionDao.upsertAll(
            safeSuggestions.mapIndexed { index, s ->
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
        newSuggestions: List<AutoHabitSuggestion>,
    ) {
        // Ab ID-Architektur Etappe 3c/3d in Room (habit_suggestions) mit Herkunft der Quell-Idee.
        val safeSuggestions = newSuggestions.filterNot { s ->
            PhoneContentGuard.isSecondBrainWorkArtifact(null, s.text)
        }
        if (safeSuggestions.isEmpty()) return
        val nowMs = System.currentTimeMillis()
        habitSuggestionDao.upsertAll(
            safeSuggestions.mapIndexed { index, s ->
                HabitSuggestionEntity(
                    id = s.id,
                    text = s.text,
                    createdAt = nowMs + index,
                    originId = s.originId,
                    originType = s.originType,
                    rootId = s.rootId,
                )
            }
        )
    }
}
