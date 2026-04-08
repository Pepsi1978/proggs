package com.bestjournal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity
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
}
