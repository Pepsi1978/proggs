package com.nems.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nems.app.data.local.entity.SupplementEntryEntity
import kotlinx.coroutines.flow.Flow

data class DailyCompletionStat(
    val date: String,
    val total: Int,
    val takenCount: Int,
)

@Dao
interface SupplementEntryDao {

    @Query("SELECT * FROM supplement_entries WHERE date = :date ORDER BY stackSectionId")
    fun getEntriesForDate(date: String): Flow<List<SupplementEntryEntity>>

    @Query("SELECT * FROM supplement_entries WHERE date = :date AND stackSectionId = :sectionId")
    fun getEntriesForDateAndSection(date: String, sectionId: String): Flow<List<SupplementEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<SupplementEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SupplementEntryEntity)

    @Update
    suspend fun update(entry: SupplementEntryEntity)

    @Query("UPDATE supplement_entries SET taken = :taken, takenTimestamp = :timestamp WHERE id = :id")
    suspend fun updateTakenStatus(id: String, taken: Boolean, timestamp: String?)

    @Query("UPDATE supplement_entries SET taken = 1, takenTimestamp = :timestamp WHERE date = :date AND stackSectionId = :sectionId")
    suspend fun markSectionComplete(date: String, sectionId: String, timestamp: String)

    @Query("UPDATE supplement_entries SET taken = 0, takenTimestamp = NULL WHERE date = :date AND stackSectionId = :sectionId")
    suspend fun markSectionIncomplete(date: String, sectionId: String)

    @Query("SELECT COUNT(*) FROM supplement_entries WHERE date = :date AND stackSectionId = :sectionId AND taken = 0")
    suspend fun countUntakenInSection(date: String, sectionId: String): Int

    @Query("UPDATE supplement_entries SET taken = 1, takenTimestamp = :timestamp WHERE date = :date")
    suspend fun markDayComplete(date: String, timestamp: String)

    @Query("UPDATE supplement_entries SET taken = 0, takenTimestamp = NULL WHERE date = :date")
    suspend fun markDayIncomplete(date: String)

    @Query("SELECT COUNT(*) FROM supplement_entries WHERE date = :date AND taken = 0")
    suspend fun countUntakenForDate(date: String): Int

    @Query("""
        SELECT date, COUNT(*) as total, SUM(CASE WHEN taken = 1 THEN 1 ELSE 0 END) as takenCount
        FROM supplement_entries
        GROUP BY date
    """)
    fun getDailyCompletionStats(): Flow<List<DailyCompletionStat>>

    @Query("""
        SELECT date, COUNT(*) as total, SUM(CASE WHEN taken = 1 THEN 1 ELSE 0 END) as takenCount
        FROM supplement_entries
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
    """)
    fun getCompletionStatsForRange(startDate: String, endDate: String): Flow<List<DailyCompletionStat>>

    @Query("SELECT COUNT(*) FROM supplement_entries WHERE date = :date")
    suspend fun countEntriesForDate(date: String): Int

    @Query("DELETE FROM supplement_entries WHERE date = :date")
    suspend fun deleteEntriesForDate(date: String)

    @Query("SELECT DISTINCT date FROM supplement_entries ORDER BY date DESC")
    fun getAllDates(): Flow<List<String>>

    @Query("""
        SELECT s.name, COUNT(*) - SUM(CASE WHEN e.taken = 1 THEN 1 ELSE 0 END) as missedCount
        FROM supplement_entries e
        JOIN supplements s ON e.supplementId = s.id
        WHERE e.date BETWEEN :startDate AND :endDate
        GROUP BY e.supplementId
        HAVING missedCount > 0
        ORDER BY missedCount DESC
        LIMIT 5
    """)
    fun getMostMissedSupplements(startDate: String, endDate: String): Flow<List<MissedSupplement>>
}

data class MissedSupplement(
    val name: String,
    val missedCount: Int,
)
