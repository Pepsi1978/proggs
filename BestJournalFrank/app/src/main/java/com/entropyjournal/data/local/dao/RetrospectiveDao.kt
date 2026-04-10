package com.entropyjournal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RetrospectiveDao {

    @Query("SELECT * FROM retrospective_summaries WHERE type = :type ORDER BY startDate DESC")
    fun getByType(type: String): Flow<List<RetrospectiveSummaryEntity>>

    @Query("SELECT * FROM retrospective_summaries ORDER BY startDate DESC")
    fun getAll(): Flow<List<RetrospectiveSummaryEntity>>

    @Query("SELECT * FROM retrospective_summaries WHERE id = :id")
    suspend fun getById(id: Long): RetrospectiveSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: RetrospectiveSummaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summaries: List<RetrospectiveSummaryEntity>)

    @Query("SELECT COUNT(*) FROM retrospective_summaries") suspend fun count(): Int

    @Query("DELETE FROM retrospective_summaries") suspend fun deleteAll()

    @Query("SELECT periodLabel FROM retrospective_summaries WHERE type = 'WEEKLY' LIMIT 1")
    suspend fun getFirstWeeklyLabel(): String?

    @Query(
        "SELECT COUNT(*) FROM retrospective_summaries WHERE type = :type AND startDate = :startDate"
    )
    suspend fun existsForPeriod(type: String, startDate: Long): Int

    @Query(
        "SELECT COUNT(*) FROM retrospective_summaries WHERE title IN ('Ein Anfang voller kleiner Wunder', 'Stürme und stille Siege', 'Farben des Alltags', 'Ein Monat wie ein guter Roman', 'Ein Jahr, das alles verändert hat')"
    )
    suspend fun countTestData(): Int

    @Query(
        "DELETE FROM retrospective_summaries WHERE type = :type AND startDate BETWEEN :rangeStart AND :rangeEnd"
    )
    suspend fun deleteByTypeAndRange(type: String, rangeStart: Long, rangeEnd: Long)

    @Query(
        "SELECT * FROM retrospective_summaries WHERE type = :type AND startDate BETWEEN :rangeStart AND :rangeEnd ORDER BY startDate ASC"
    )
    suspend fun getByTypeAndRange(
        type: String,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<RetrospectiveSummaryEntity>
}
