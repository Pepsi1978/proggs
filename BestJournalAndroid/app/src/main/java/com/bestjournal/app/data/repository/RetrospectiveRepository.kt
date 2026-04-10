package com.bestjournal.app.data.repository

import com.bestjournal.app.data.local.dao.RetrospectiveDao
import com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity
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

    suspend fun containsTestData(): Boolean = dao.countTestData() > 0

    suspend fun existsForPeriod(type: String, startDate: Long): Boolean =
        dao.existsForPeriod(type, startDate) > 0

    suspend fun getByTypeAndRange(
        type: String,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<RetrospectiveSummaryEntity> = dao.getByTypeAndRange(type, rangeStart, rangeEnd)
}
