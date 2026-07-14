package de.frank.entropyreducer.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.gewohnheitSuggestionStore
import de.frank.entropyreducer.data.kiTaskSuggestionStore
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.HabitDao
import de.frank.entropyreducer.data.local.dao.IdeaDao
import de.frank.entropyreducer.data.local.dao.MentalSentenceDao
import de.frank.entropyreducer.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Einmaliger ID-Architektur-Backfill (Frank-Wunsch 2026-06-20). Zwei idempotente Teile:
 *
 * 1. SELF-ROOTING: Altbestand-Eintraege OHNE Herkunft (entropy_entries/habits/mental_sentences — alle
 *    aus der Zeit vor der ID-Architektur) bekommen sich selbst als Wurzel (originId/rootId = eigene
 *    id, originType = eigener Typ). Damit ist KEIN Eintrag mehr "herkunftslos".
 *
 * 2. IDEEN-MARKIERUNG: ALLE aktuell vorhandenen Ideen werden in beide processedIds-Listen
 *    (processed_idea_ids + habit_processed_idea_ids) eingetragen. Damit erzeugt KEINE Alt-Idee mehr
 *    erneut einen Vorschlag — der Doppelvorschlag-Bug (vor dem ID-System angenommene + umbenannte
 *    Aufgabe -> Quell-Idee galt als unverarbeitet -> Vorschlag kam wieder) kann nicht mehr auftreten.
 *    Neue Ideen ab jetzt werden normal verarbeitet.
 *
 * Sicherheits-Doktrin wie die Room-Migratoren ([MentalRoomMigrator] u. a.): Flag-gesteuert (einmalig),
 * fehlertolerant (Fehler werden geloggt, Flag NICHT gesetzt -> Retry beim naechsten Start), additive/
 * idempotente Writes. Aufruf: [EntropyReducerApp.onCreate] via [backfillIfNeeded].
 */
@Singleton
class LineageBackfillMigrator
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val entropyEntryDao: EntropyEntryDao,
    private val habitDao: HabitDao,
    private val mentalSentenceDao: MentalSentenceDao,
    private val ideaDao: IdeaDao,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun backfillIfNeeded() {
        if (prefs.getBoolean(KEY_DONE, false)) return
        applicationScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { runBackfill() }
                    .onFailure { ex ->
                        Diag.w(
                            DiagnosticArea.DATABASE,
                            TAG,
                            "Backfill fehlgeschlagen -> Retry beim naechsten Start",
                            ex,
                        )
                    }
            }
        }
    }

    suspend fun backfillIfNeededNow() = withContext(Dispatchers.IO) {
        if (prefs.getBoolean(KEY_DONE, false)) return@withContext
        runBackfill()
    }

    private suspend fun runBackfill() {
        // 1. Self-Rooting der Altbestand-Endpunkte (idempotent: trifft nur originId IS NULL).
        val tasks = entropyEntryDao.backfillSelfRoot()
        val habits = habitDao.backfillSelfRoot()
        val mentals = mentalSentenceDao.backfillSelfRoot()
        Diag.i(
            DiagnosticArea.DATABASE,
            TAG,
            "CHECKPOINT backfill=self-root entropy_entries=$tasks habits=$habits mental_sentences=$mentals",
        )

        // 2. Alle aktuellen Ideen als verarbeitet markieren (beide processedIds-Listen).
        val ideaIds = ideaDao.getAllIdeasForBackup().map { it.id }.toSet()
        val taskNew = mergeIntoStore(context.kiTaskSuggestionStore, TASK_PROCESSED_KEY, ideaIds)
        val habitNew = mergeIntoStore(context.gewohnheitSuggestionStore, HABIT_PROCESSED_KEY, ideaIds)
        Diag.i(
            DiagnosticArea.DATABASE,
            TAG,
            "CHECKPOINT backfill=processed-ideas ideen=${ideaIds.size} taskNeu=$taskNew habitNeu=$habitNew",
        )

        prefs.edit().putBoolean(KEY_DONE, true).apply()
        Diag.i(DiagnosticArea.DATABASE, TAG, "Backfill abgeschlossen, Flag gesetzt.")
    }

    /** Mergt die neuen IDs in die JSON-Array-Liste des Keys. Gibt die Zahl NEU hinzugefuegter zurueck. */
    private suspend fun mergeIntoStore(
        store: DataStore<Preferences>,
        key: Preferences.Key<String>,
        newIds: Set<String>,
    ): Int {
        var added = 0
        store.edit { prefsIn ->
            val existing = LinkedHashSet<String>()
            prefsIn[key]?.let { raw ->
                runCatching {
                    val arr = JSONArray(raw)
                    for (i in 0 until arr.length()) {
                        arr.optString(i).takeIf { it.isNotBlank() }?.let { existing.add(it) }
                    }
                }
            }
            val before = existing.size
            existing.addAll(newIds)
            added = existing.size - before
            val out = JSONArray()
            existing.forEach { out.put(it) }
            prefsIn[key] = out.toString()
        }
        return added
    }

    companion object {
        private const val TAG = "ERELineageBackfill"
        private const val PREFS_NAME = "entropy_reducer_id_arch_migration"
        private const val KEY_DONE = "id_arch_lineage_backfill_v1_done"
        private val TASK_PROCESSED_KEY = stringPreferencesKey("processed_idea_ids")
        private val HABIT_PROCESSED_KEY = stringPreferencesKey("habit_processed_idea_ids")
    }
}
