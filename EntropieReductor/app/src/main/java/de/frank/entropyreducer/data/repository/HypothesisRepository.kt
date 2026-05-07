package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.domain.model.HypothesisStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet Hypothesen / Experimente. Spec §12.3, §14.3.
 */
@Singleton
class HypothesisRepository @Inject constructor(
    private val dao: HypothesisDao,
) {
    fun observeAll(): Flow<List<HypothesisEntity>> = dao.getAll()
    fun observeByStatus(status: HypothesisStatus): Flow<List<HypothesisEntity>> =
        dao.getByStatus(status)
    fun observeActiveOnDate(date: Long): Flow<List<HypothesisEntity>> = dao.getActiveOnDate(date)

    suspend fun get(id: String): HypothesisEntity? = dao.getById(id)
    suspend fun upsert(h: HypothesisEntity) = dao.upsert(h)
    suspend fun update(h: HypothesisEntity) = dao.update(h)
    suspend fun delete(h: HypothesisEntity) = dao.delete(h)
}
