package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.coroutines.flow.Flow

@Dao
interface EntropyEntryDao {

    @Query("SELECT * FROM entropy_entries WHERE id = :id")
    suspend fun getById(id: String): EntropyEntryEntity?

    /**
     * Backfill (Frank-Wunsch 2026-06-20): Altbestand-Aufgaben ohne Herkunft selbst-verwurzeln
     * (originId/rootId = eigene id, originType=TASK). Idempotent — trifft nur Zeilen ohne Herkunft.
     */
    @Query(
        "UPDATE entropy_entries SET originId = id, originType = 'TASK', rootId = id " +
            "WHERE originId IS NULL OR originId = ''"
    )
    suspend fun backfillSelfRoot(): Int

    @Query(
        "SELECT * FROM entropy_entries WHERE status != :archived ORDER BY priorityScore DESC, createdAt DESC"
    )
    fun getActive(archived: EntryStatus = EntryStatus.ARCHIVIERT): Flow<List<EntropyEntryEntity>>

    @Query(
        "SELECT * FROM entropy_entries WHERE timeBucket = :bucket AND status != :archived ORDER BY priorityScore DESC"
    )
    fun getByTimeBucket(
        bucket: TimeBucket,
        archived: EntryStatus = EntryStatus.ARCHIVIERT,
    ): Flow<List<EntropyEntryEntity>>

    @Query(
        "SELECT * FROM entropy_entries WHERE category = :cat AND status != :archived ORDER BY priorityScore DESC"
    )
    fun getByCategory(
        cat: EntropyCategory,
        archived: EntryStatus = EntryStatus.ARCHIVIERT,
    ): Flow<List<EntropyEntryEntity>>

    @Query(
        "SELECT * FROM entropy_entries WHERE status = :resolved AND resolvedAt >= :sinceMillis ORDER BY resolvedAt DESC"
    )
    fun getRecentlyResolved(
        resolved: EntryStatus = EntryStatus.REDUZIERT,
        sinceMillis: Long,
    ): Flow<List<EntropyEntryEntity>>

    @Query(
        "SELECT * FROM entropy_entries WHERE status = :archived ORDER BY resolvedAt DESC, updatedAt DESC"
    )
    fun getArchived(archived: EntryStatus = EntryStatus.ARCHIVIERT): Flow<List<EntropyEntryEntity>>

    @Query("SELECT * FROM entropy_entries WHERE status = :resolved AND resolvedAt < :beforeMillis")
    suspend fun getResolvedBefore(
        resolved: EntryStatus = EntryStatus.REDUZIERT,
        beforeMillis: Long,
    ): List<EntropyEntryEntity>

    @Query("SELECT * FROM entropy_entries WHERE status != :archived ORDER BY priorityScore DESC")
    fun getByPriorityDesc(
        archived: EntryStatus = EntryStatus.ARCHIVIERT
    ): Flow<List<EntropyEntryEntity>>

    @Query("SELECT COUNT(*) FROM entropy_entries WHERE status = :status")
    fun countByStatus(status: EntryStatus): Flow<Int>

    /**
     * Ketten-Dedup ueber die ANGENOMMENEN Endpunkte (Frank-Wunsch 2026-06-20, Direktive #3 robust):
     * zaehlt Aufgaben, deren Wurzel diese Idee ist (rootId = Quell-Idee). So bleibt eine Idee
     * dedupliziert, auch nachdem ihr Vorschlag angenommen (und damit geloescht) wurde — die Kette
     * Idee -> Vorschlag -> Aufgabe bricht nicht. Cross-device robust, weil rootId ab Backup-Schema v19
     * mitgesichert wird.
     */
    @Query("SELECT COUNT(*) FROM entropy_entries WHERE rootId = :rootId")
    suspend fun countByRootId(rootId: String): Int

    /**
     * Frank-Wunsch 2026-05-19: ALLE Eintraege fuer das Drive-Backup — auch archivierte. getActive()
     * filtert ARCHIVIERT raus damit der Tasks-Screen sauber bleibt, aber das Backup MUSS auch das
     * Archiv (Bereich "Entropie") enthalten, sonst gehen archivierte Eintraege bei Reinstall
     * verloren.
     */
    @Query("SELECT * FROM entropy_entries ORDER BY createdAt ASC")
    suspend fun getAllForBackup(): List<EntropyEntryEntity>

    /**
     * Frank-Wunsch 2026-05-22 (dritte Iteration): Letzte N Aufgaben mit
     * MANUELL gesetzter Dauer — Trainingsdaten fuer die KI. Wenn Frank
     * fuer "Wohnung putzen" 60 min einstellt, lernt die KI fuer aehnliche
     * Aufgaben automatisch eine vergleichbare Schaetzung.
     */
    @Query(
        "SELECT * FROM entropy_entries WHERE durationManuallySet = 1 " +
            "AND estimatedDurationMinutes IS NOT NULL " +
            "ORDER BY updatedAt DESC LIMIT :limit"
    )
    suspend fun getRecentManualDurationSamples(limit: Int): List<EntropyEntryEntity>

    // Bugfix 2026-07-03 (Room-Almanach K1): @Upsert statt @Insert(REPLACE) — REPLACE ist
    // DELETE+INSERT und feuert den CASCADE der entropy_entry_followups: jedes Update eines
    // bestehenden Eintrags (auch via Drive-Sync-LWW) loeschte still alle seine Nachtraege.
    @Upsert suspend fun upsert(entry: EntropyEntryEntity)

    @Update suspend fun update(entry: EntropyEntryEntity)

    @Delete suspend fun delete(entry: EntropyEntryEntity)

    @Query("DELETE FROM entropy_entries WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM entropy_entries") suspend fun deleteAll()
}
