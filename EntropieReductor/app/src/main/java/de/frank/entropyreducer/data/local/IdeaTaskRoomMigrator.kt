package de.frank.entropyreducer.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.IdeaDao
import de.frank.entropyreducer.data.local.dao.TaskSuggestionDao
import de.frank.entropyreducer.data.local.entities.IdeaEntity
import de.frank.entropyreducer.data.local.entities.IdeaFollowupEntity
import de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity
import de.frank.entropyreducer.data.taskSuggestionsFromJson
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.presentation.ideen.ideenEntriesFromJsonFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ID-Architektur Etappe 2b (Frank-Wunsch 2026-06-19): einmaliger, idempotenter Umzug der
 * Bestandsdaten aus den DataStore-JSON-Listen in die neuen Room-Tabellen.
 *
 * - Ideen (DataStore "ideen_entries") -> Tabelle `ideas` (+ `idea_followups`)
 * - Aufgaben-Vorschlaege (DataStore "ki_task_suggestions") -> Tabelle `task_suggestions`
 *
 * SICHERHEIT (datenkritisch — Frank's echte Daten):
 *  1. Die JSON-Listen werden NUR GELESEN, NIE geloescht/geaendert — sie bleiben als Fallback
 *     liegen (Etappe 2c liest dann aus Room; bis dahin ist der JSON die Quelle der Wahrheit).
 *  2. Idempotent: SharedPrefs-Flag verhindert Mehrfach-Lauf; zusaetzlich wird nur kopiert, wenn
 *     die Room-Tabellen noch LEER sind (Doppelschutz gegen versehentliche Duplikate, z. B. falls
 *     ein Sync sie schon gefuellt hat).
 *  3. Anzahl-Abgleich nach dem Schreiben (Live-Logik-Sonde, kind CHECKPOINT). Stimmt die Zahl
 *     nicht, wird das Flag NICHT gesetzt -> Retry beim naechsten Start (JSON ist ja noch da).
 *  4. Fehler werden geloggt, NIE geworfen — der App-Start darf nie blockiert werden. Das
 *     runCatching-im-applicationScope-Muster ist projektweit etabliert (siehe EntropyReducerApp).
 *  5. Bestandsdaten haben keine Herkunft (originId/originType/rootId = null) — korrekt, sie sind
 *     vor dem Umbau entstanden.
 *
 * Aufruf: [EntropyReducerApp.onCreate] nach Hilt-Init via [migrateIfNeeded].
 */
@Singleton
class IdeaTaskRoomMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ideaDao: IdeaDao,
    private val taskSuggestionDao: TaskSuggestionDao,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun migrateIfNeeded() {
        if (prefs.getBoolean(KEY_DONE, false)) return
        applicationScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { runMigration() }
                    .onFailure { ex ->
                        Diag.w(
                            DiagnosticArea.DATABASE,
                            TAG,
                            "Migration fehlgeschlagen -> Retry beim naechsten Start (JSON unangetastet)",
                            ex,
                        )
                    }
            }
        }
    }

    private suspend fun runMigration() {
        // Doppelschutz: nur kopieren wenn Room noch leer ist.
        val ideaCountBefore = ideaDao.countIdeas()
        val sugCountBefore = taskSuggestionDao.count()
        if (ideaCountBefore > 0 || sugCountBefore > 0) {
            Diag.i(
                DiagnosticArea.DATABASE,
                TAG,
                "Uebersprungen: Room bereits befuellt (ideas=$ideaCountBefore, task_suggestions=$sugCountBefore) -> Flag gesetzt",
            )
            prefs.edit { putBoolean(KEY_DONE, true) }
            return
        }

        // JSON lesen (nur lesen, niemals schreiben). Dedizierte JSON-Quelle — NICHT die Room-basierte
        // ideenEntriesFlow/taskSuggestionsForBackup, sonst laese der Migrator seine eigene Zielquelle.
        val ideas = ideenEntriesFromJsonFlow(context).first()
        val taskSugs = taskSuggestionsFromJson(context).first()

        // Mappen (Bestandsdaten ohne Herkunft).
        val ideaEntities =
            ideas.map { idea ->
                IdeaEntity(
                    id = idea.id,
                    timestampMs = idea.timestampMs,
                    title = idea.title,
                    text = idea.text,
                    summary = idea.summary,
                    improvedText = idea.improvedText,
                    isImproved = idea.isImproved,
                )
            }
        val followupEntities =
            ideas.flatMap { idea ->
                idea.followups.map { f ->
                    IdeaFollowupEntity(
                        id = f.id,
                        ideaId = idea.id,
                        createdAtMs = f.createdAtMs,
                        text = f.text,
                        improvedText = f.improvedText,
                        isImproved = f.isImproved,
                    )
                }
            }
        val nowMs = System.currentTimeMillis()
        val sugEntities =
            taskSugs.mapIndexed { index, s ->
                TaskSuggestionEntity(
                    id = s.id,
                    title = s.title,
                    description = s.description,
                    // JSON hat keinen Zeitstempel — Reihenfolge stabil halten.
                    createdAt = nowMs + index,
                )
            }

        // Schreiben.
        if (ideaEntities.isNotEmpty()) ideaDao.upsertIdeas(ideaEntities)
        if (followupEntities.isNotEmpty()) ideaDao.upsertFollowups(followupEntities)
        if (sugEntities.isNotEmpty()) taskSuggestionDao.upsertAll(sugEntities)

        // Anzahl-Abgleich (Live-Logik-Sonde / CHECKPOINT) — erwartet vs. tatsaechlich.
        val ideaWritten = ideaDao.countIdeas()
        val sugWritten = taskSuggestionDao.count()
        val ideaOk = ideaWritten == ideas.size
        val sugOk = sugWritten == taskSugs.size
        Diag.i(
            DiagnosticArea.DATABASE,
            TAG,
            "CHECKPOINT migration=ideas erwartet=${ideas.size} tatsaechlich=$ideaWritten followups=${followupEntities.size} ok=$ideaOk",
        )
        Diag.i(
            DiagnosticArea.DATABASE,
            TAG,
            "CHECKPOINT migration=task_suggestions erwartet=${taskSugs.size} tatsaechlich=$sugWritten ok=$sugOk",
        )

        if (ideaOk && sugOk) {
            prefs.edit { putBoolean(KEY_DONE, true) }
            Diag.i(
                DiagnosticArea.DATABASE,
                TAG,
                "Migration erfolgreich abgeschlossen. JSON-Listen bleiben als Fallback erhalten.",
            )
        } else {
            Diag.w(
                DiagnosticArea.DATABASE,
                TAG,
                "Anzahl-Mismatch -> Flag NICHT gesetzt, Retry beim naechsten Start. JSON unangetastet.",
            )
        }
    }

    companion object {
        private const val TAG = "EREIdeaTaskMigrator"
        private const val PREFS_NAME = "entropy_reducer_id_arch_migration"
        private const val KEY_DONE = "id_arch_ideas_tasks_v1_done"
    }
}
