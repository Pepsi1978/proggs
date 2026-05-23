package de.frank.entropyreducer.data.diagnostics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticLogDao {

    @Insert
    suspend fun insert(entry: DiagnosticLogEntry): Long

    /** Alle Eintraege, neueste zuerst. Fuer den Diagnose-Screen (Flow → Live-Update). */
    @Query("SELECT * FROM diagnostic_log ORDER BY timestampMs DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<DiagnosticLogEntry>>

    /** Gefiltert nach einem Bereich, neueste zuerst. */
    @Query(
        "SELECT * FROM diagnostic_log WHERE area = :area ORDER BY timestampMs DESC, id DESC LIMIT :limit"
    )
    fun observeByArea(area: String, limit: Int = 500): Flow<List<DiagnosticLogEntry>>

    /** Snapshot fuer den Export (keine Flow). */
    @Query("SELECT * FROM diagnostic_log ORDER BY timestampMs DESC, id DESC LIMIT :limit")
    suspend fun snapshot(limit: Int = 2000): List<DiagnosticLogEntry>

    /** Letzter Eintrag eines Bereichs — fuer "zuletzt OK"-Anzeige. */
    @Query(
        "SELECT * FROM diagnostic_log WHERE area = :area ORDER BY timestampMs DESC, id DESC LIMIT 1"
    )
    suspend fun latestForArea(area: String): DiagnosticLogEntry?

    @Query("DELETE FROM diagnostic_log WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    /**
     * Hartes Sicherheitsnetz: behaelt nur die [maxCount] neuesten Eintraege. Verhindert dass
     * die Log-DB bei einem fehlerhaften Dauer-Loop unbegrenzt waechst (Poka-Yoke).
     */
    @Query(
        "DELETE FROM diagnostic_log WHERE id NOT IN " +
            "(SELECT id FROM diagnostic_log ORDER BY timestampMs DESC, id DESC LIMIT :maxCount)"
    )
    suspend fun trimToMaxCount(maxCount: Int): Int

    @Query("DELETE FROM diagnostic_log")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM diagnostic_log")
    suspend fun count(): Int
}
