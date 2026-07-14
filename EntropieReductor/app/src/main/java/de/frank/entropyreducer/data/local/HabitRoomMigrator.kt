package de.frank.entropyreducer.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.gewohnheitSuggestionsFromJson
import de.frank.entropyreducer.data.local.dao.HabitDao
import de.frank.entropyreducer.data.local.dao.HabitSuggestionDao
import de.frank.entropyreducer.data.local.entities.HabitEntity
import de.frank.entropyreducer.data.local.entities.HabitSuggestionEntity
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.presentation.mental.gewohnheitenFromJsonFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ID-Architektur Etappe 3b (Frank-Wunsch 2026-06-19): einmaliger, idempotenter Umzug der
 * Bestandsdaten aus den DataStore-JSON-Listen in die neuen Room-Tabellen.
 *
 * - Gewohnheiten (DataStore "gewohnheit_board") -> Tabelle `habits` (position = Listen-Index, damit
 *   die manuelle Reihenfolge erhalten bleibt)
 * - Gewohnheits-Vorschlaege (DataStore "gewohnheit_suggestions") -> Tabelle `habit_suggestions`
 *
 * Gleiche Sicherheits-Doktrin wie [IdeaTaskRoomMigrator]: JSON wird NUR gelesen (nie geloescht,
 * bleibt als Fallback bis 3c/3e Room lesen), idempotent (SharedPrefs-Flag + Room-leer-Check),
 * Anzahl-Abgleich-Sonde, Fehler werden geloggt statt geworfen (App-Start nie blockieren),
 * Bestandsdaten ohne Herkunft (origin/root = null).
 *
 * Aufruf: [EntropyReducerApp.onCreate] nach Hilt-Init via [migrateIfNeeded].
 */
@Singleton
class HabitRoomMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val habitSuggestionDao: HabitSuggestionDao,
    private val database: AppDatabase,
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

    suspend fun migrateIfNeededNow() = withContext(Dispatchers.IO) {
        if (prefs.getBoolean(KEY_DONE, false)) return@withContext
        runMigration()
        check(prefs.getBoolean(KEY_DONE, false)) { "Gewohnheiten-/Vorschlagsmigration nicht vollständig" }
    }

    private suspend fun runMigration() {
        // JSON lesen (nur lesen, niemals schreiben) — dedizierte JSON-Quellen, NICHT die in 3c/3e auf
        // Room umgestellten gewohnheitenFlow/gewohnheitSuggestionsForBackup.
        val gewohnheiten = gewohnheitenFromJsonFlow(context).first()
        val habitSugs = gewohnheitSuggestionsFromJson(context).first()

        // Mappen (Bestandsdaten ohne Herkunft). position = Listen-Index (manuelle Reihenfolge erhalten).
        val habitEntities =
            gewohnheiten.mapIndexed { index, m ->
                HabitEntity(
                    id = m.id,
                    text = m.text,
                    updatedAt = m.updatedAt,
                    position = index,
                )
            }
        val nowMs = System.currentTimeMillis()
        val sugEntities =
            habitSugs.mapIndexed { index, s ->
                HabitSuggestionEntity(
                    id = s.id,
                    text = s.text,
                    // JSON hat keinen Zeitstempel — Reihenfolge stabil halten.
                    createdAt = nowMs + index,
                )
            }

        // Schreiben.
        database.withTransaction {
            if (habitEntities.isNotEmpty()) habitDao.upsertAll(habitEntities)
            if (sugEntities.isNotEmpty()) habitSuggestionDao.upsertAll(sugEntities)
        }

        // Anzahl-Abgleich (Live-Logik-Sonde / CHECKPOINT) — erwartet vs. tatsaechlich.
        val habitWritten = habitDao.count()
        val sugWritten = habitSuggestionDao.count()
        val habitOk = habitWritten >= gewohnheiten.size
        val sugOk = sugWritten >= habitSugs.size
        Diag.i(
            DiagnosticArea.DATABASE,
            TAG,
            "CHECKPOINT migration=habits erwartet=${gewohnheiten.size} tatsaechlich=$habitWritten ok=$habitOk",
        )
        Diag.i(
            DiagnosticArea.DATABASE,
            TAG,
            "CHECKPOINT migration=habit_suggestions erwartet=${habitSugs.size} tatsaechlich=$sugWritten ok=$sugOk",
        )

        if (habitOk && sugOk) {
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
        private const val TAG = "EREHabitMigrator"
        private const val PREFS_NAME = "entropy_reducer_id_arch_migration"
        private const val KEY_DONE = "id_arch_habits_v1_done"
    }
}
