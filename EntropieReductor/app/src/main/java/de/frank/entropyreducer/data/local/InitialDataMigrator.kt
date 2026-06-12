package de.frank.entropyreducer.data.local

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.di.ApplicationScope
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.MemorySource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Frank-Wunsch 2026-05-09 (Abend): Insights und Memories aus AppDatabase in
 * ScientistDatabase verschoben. Da AppDatabase mit fallbackToDestructiveMigration
 * arbeitet, wuerden bei der Schema-Aenderung 9 → 10 alle alten Insights und
 * Memories geloescht — bevor Frank's Daten in die neue ScientistDatabase wandern
 * koennen.
 *
 * Zwei-Stufen-Verfahren um Race Conditions zu vermeiden:
 *
 *  STUFE 1 — Pre-Hilt:  [readOldDataPreHilt] in EntropyReducerApp.onCreate()
 *                       BEVOR super.onCreate() laeuft. Oeffnet die alte
 *                       entropy_reducer.db direkt mit SQLiteDatabase (raw,
 *                       READ_ONLY) und liest alle Insights + Memories in eine
 *                       in-Memory-Datenstruktur. Schliesst die Connection.
 *                       Diese Stufe braucht KEIN Hilt — laeuft sicher bevor
 *                       Room die alte DB beruehrt.
 *
 *  STUFE 2 — Post-Hilt: [writeRescuedData] nach super.onCreate(), wenn Hilt
 *                       initialisiert ist. Schreibt die in Stufe 1 geretteten
 *                       Daten via ScientistDatabase-DAOs in die neue DB.
 *                       ScientistDatabase wird beim ersten DAO-Zugriff von Room
 *                       geoeffnet, dabei laeuft MIGRATION_1_2 und legt die
 *                       neuen Tabellen `insights` und `memory_entries` an.
 *
 * Idempotent: SharedPrefs-Flag "v10_db_split_done" verhindert mehrfache Ausfuehrung.
 *
 * Fehler werden geloggt aber NICHT geworfen — der App-Start darf nicht blockiert
 * werden. Im Worst-Case (DB-Datei korrupt, Tabellen-Schema unerwartet) gehen die
 * paar Eintraege verloren.
 */
@Singleton
class InitialDataMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scientistDb: ScientistDatabase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Performance-Fix Loop 1 (2026-05-10): Vorher lief der Schreibvorgang in
     * runBlocking{} auf dem Main-Thread (App.onCreate). Bei 50-200 geretteten
     * Eintraegen + kalter Room-DB blockierte das den Main-Thread 80-300 ms,
     * im Worst-Case ANR-Risiko. Jetzt async ueber ApplicationScope (Dispatchers.IO,
     * SupervisorJob). Das KEY_DONE-Flag wird erst NACH den DAO-Upserts gesetzt —
     * ein Process-Kill mid-write fuehrt also zu einem erneuten Versuch beim
     * naechsten Start (sofern die alte DB-Datei noch existiert).
     */
    fun writeRescuedData(rescued: RescuedData?) {
        if (rescued == null) return
        if (prefs.getBoolean(KEY_DONE, false)) return

        applicationScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (rescued.insights.isNotEmpty() || rescued.memories.isNotEmpty()) {
                        Diag.i(DiagnosticArea.DATABASE, 
                            TAG,
                            "Schreibe ${rescued.insights.size} Insight(s) und " +
                                "${rescued.memories.size} Memory-Eintrag/-Eintraege " +
                                "in ScientistDatabase",
                        )
                        rescued.insights.forEach { scientistDb.insightDao().upsert(it) }
                        rescued.memories.forEach { scientistDb.memoryDao().upsert(it) }
                    }
                    prefs.edit { putBoolean(KEY_DONE, true) }
                }.onFailure { ex ->
                    Diag.w(DiagnosticArea.DATABASE, TAG, "Migration write fehlgeschlagen — wird beim naechsten Start erneut versucht", ex)
                }
            }
        }
    }

    /** Daten die in Stufe 1 aus der alten DB gerettet wurden. */
    data class RescuedData(
        val insights: List<InsightEntity>,
        val memories: List<MemoryEntryEntity>,
    )

    companion object {
        private const val TAG = "EREDataMigrator"
        private const val PREFS_NAME = "entropy_reducer_data_migration"
        private const val KEY_DONE = "v10_db_split_done"

        /**
         * STUFE 1: Liest die alten Insights und Memories direkt aus der entropy_reducer.db
         * VOR Hilt-Init. Wird statisch aufgerufen damit kein Application/Hilt-State
         * gebraucht wird.
         *
         * Returns null wenn:
         *  - Migration schon einmal gelaufen ist (Flag gesetzt)
         *  - Alte DB-Datei nicht existiert (Erstinstall)
         *  - Lesen fehlschlaegt
         */
        fun readOldDataPreHilt(context: Context): RescuedData? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_DONE, false)) return null

            val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
            if (!dbFile.exists()) {
                Diag.d(DiagnosticArea.DATABASE, TAG, "Alte DB-Datei existiert nicht — Erstinstall")
                // Flag setzen, damit der Pre-Hilt-Read nicht bei jedem Start
                // erneut Filesystem checkt. Erstinstall hat nichts zu migrieren.
                prefs.edit { putBoolean(KEY_DONE, true) }
                return null
            }

            return runCatching {
                val rawDb = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                )
                val insights = runCatching { readInsights(rawDb) }.getOrElse { ex ->
                    Diag.w(DiagnosticArea.DATABASE, TAG, "Insights nicht lesbar — Tabelle existiert evtl nicht", ex)
                    emptyList()
                }
                val memories = runCatching { readMemories(rawDb) }.getOrElse { ex ->
                    Diag.w(DiagnosticArea.DATABASE, TAG, "Memories nicht lesbar — Tabelle existiert evtl nicht", ex)
                    emptyList()
                }
                runCatching { rawDb.close() }
                Diag.i(DiagnosticArea.DATABASE, 
                    TAG,
                    "Pre-Hilt-Read: ${insights.size} Insights, ${memories.size} Memories aus alter DB",
                )
                RescuedData(insights, memories)
            }.getOrElse { ex ->
                Diag.w(DiagnosticArea.DATABASE, TAG, "Pre-Hilt-Read fehlgeschlagen", ex)
                null
            }
        }

        /**
         * Liest die `insights`-Tabelle der alten DB. Behandelt SOWOHL v8-Schema
         * (ohne additionalCategories und manualSource) ALS AUCH v9-Schema (mit beiden
         * neuen Spalten). Wenn die Spalten fehlen, werden Defaults gesetzt.
         */
        private fun readInsights(db: SQLiteDatabase): List<InsightEntity> {
            val cursor = db.rawQuery("SELECT * FROM insights", null)
            val out = mutableListOf<InsightEntity>()
            cursor.use { c ->
                val idx = ColumnIndex(c)
                while (c.moveToNext()) {
                    out += InsightEntity(
                        id = c.getString(idx.required("id")),
                        title = c.getString(idx.required("title")),
                        description = c.getString(idx.required("description")),
                        targetCategory = parseCategory(c.getString(idx.required("targetCategory"))),
                        additionalCategories = idx.optional("additionalCategories")
                            ?.let { c.getString(it) }
                            ?.let { parseCategoryList(it) }
                            ?: emptyList(),
                        confidence = c.getInt(idx.required("confidence")),
                        successCount = c.getInt(idx.required("successCount")),
                        attemptCount = c.getInt(idx.required("attemptCount")),
                        avgBiomarkerImpact = idx.optional("avgBiomarkerImpact")
                            ?.let { if (c.isNull(it)) null else c.getString(it) },
                        avgFeltImpact = idx.optional("avgFeltImpact")
                            ?.let { if (c.isNull(it)) null else c.getDouble(it) },
                        createdAt = c.getLong(idx.required("createdAt")),
                        updatedAt = c.getLong(idx.required("updatedAt")),
                        sourceHypothesisIds = idx.optional("sourceHypothesisIds")
                            ?.let { c.getString(it) }
                            ?.let { parseStringList(it) }
                            ?: emptyList(),
                        manualSource = idx.optional("manualSource")
                            ?.let { c.getInt(it) == 1 }
                            ?: false,
                    )
                }
            }
            return out
        }

        private fun readMemories(db: SQLiteDatabase): List<MemoryEntryEntity> {
            val cursor = db.rawQuery("SELECT * FROM memory_entries", null)
            val out = mutableListOf<MemoryEntryEntity>()
            cursor.use { c ->
                val idx = ColumnIndex(c)
                while (c.moveToNext()) {
                    out += MemoryEntryEntity(
                        id = c.getString(idx.required("id")),
                        content = c.getString(idx.required("content")),
                        source = parseMemorySource(c.getString(idx.required("source"))),
                        isActive = c.getInt(idx.required("isActive")) == 1,
                        confidence = c.getInt(idx.required("confidence")),
                        createdAt = c.getLong(idx.required("createdAt")),
                        updatedAt = c.getLong(idx.required("updatedAt")),
                    )
                }
            }
            return out
        }

        private fun parseCategory(s: String?): EntropyCategory =
            runCatching { EntropyCategory.valueOf(s ?: "") }.getOrDefault(EntropyCategory.SONSTIGES)

        private fun parseMemorySource(s: String?): MemorySource =
            runCatching { MemorySource.valueOf(s ?: "") }.getOrDefault(MemorySource.MANUELL)

        private fun parseStringList(json: String): List<String> = runCatching {
            Json.decodeFromString(ListSerializer(String.serializer()), json)
        }.getOrDefault(emptyList())

        private fun parseCategoryList(json: String): List<EntropyCategory> = runCatching {
            Json.decodeFromString(ListSerializer(String.serializer()), json)
                .mapNotNull { runCatching { EntropyCategory.valueOf(it) }.getOrNull() }
        }.getOrDefault(emptyList())

        /**
         * Hilfsklasse die getColumnIndex() fuer required-Spalten und getColumnIndex()
         * fuer optional-Spalten unterscheidet — required wirft bei fehlend, optional
         * gibt null zurueck. Damit funktioniert der Migrator gegen v8-Schema
         * gleichermassen wie v9-Schema.
         */
        private class ColumnIndex(private val cursor: android.database.Cursor) {
            fun required(name: String): Int {
                val idx = cursor.getColumnIndex(name)
                if (idx < 0) error("Spalte '$name' fehlt in alter DB — unerwartetes Schema")
                return idx
            }

            fun optional(name: String): Int? {
                val idx = cursor.getColumnIndex(name)
                return if (idx < 0) null else idx
            }
        }
    }
}
