package com.entropyjournal.data.repository

import com.entropyjournal.data.local.dao.RetrospectiveDao
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RetrospectiveRepository @Inject constructor(private val dao: RetrospectiveDao) {
    fun getWeeklySummaries(): Flow<List<RetrospectiveSummaryEntity>> = dao.getByType("WEEKLY")

    fun getMonthlySummaries(): Flow<List<RetrospectiveSummaryEntity>> = dao.getByType("MONTHLY")

    fun getYearlySummaries(): Flow<List<RetrospectiveSummaryEntity>> = dao.getByType("YEARLY")

    suspend fun getById(id: Long): RetrospectiveSummaryEntity? = dao.getById(id)

    suspend fun insert(summary: RetrospectiveSummaryEntity): Long = dao.insert(summary)

    suspend fun insertAll(summaries: List<RetrospectiveSummaryEntity>) = dao.insertAll(summaries)

    suspend fun count(): Int = dao.count()

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun getFirstWeeklyLabel(): String? = dao.getFirstWeeklyLabel()
}
