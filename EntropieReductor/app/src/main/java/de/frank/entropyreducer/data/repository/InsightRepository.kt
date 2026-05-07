package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.InsightDao
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Insight-Board + Mein-Repertoire. Spec §14.1, §14.2, §16.6.
 */
@Singleton
class InsightRepository @Inject constructor(
    private val dao: InsightDao,
) {
    fun observeAll(): Flow<List<InsightEntity>> = dao.getByConfidenceDesc()
    fun observeByCategory(c: EntropyCategory): Flow<List<InsightEntity>> = dao.getByCategory(c)
    fun observeConfirmed(): Flow<List<InsightEntity>> = dao.getConfirmed()
    fun observeInObservation(): Flow<List<InsightEntity>> = dao.getInObservation()
    fun observeDiscarded(): Flow<List<InsightEntity>> = dao.getDiscarded()

    suspend fun upsert(i: InsightEntity) = dao.upsert(i)
    suspend fun update(i: InsightEntity) = dao.update(i)
    suspend fun delete(i: InsightEntity) = dao.delete(i)
}
