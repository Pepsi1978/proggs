package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HypothesisMessageRepository @Inject constructor(
    private val dao: HypothesisMessageDao,
) {
    fun observeForHypothesis(hypothesisId: String): Flow<List<HypothesisMessageEntity>> =
        dao.getForHypothesis(hypothesisId)

    suspend fun getForHypothesis(hypothesisId: String): List<HypothesisMessageEntity> =
        dao.getForHypothesisOnce(hypothesisId)

    suspend fun insert(message: HypothesisMessageEntity) = dao.insert(message)

    suspend fun deleteForHypothesis(hypothesisId: String) = dao.deleteForHypothesis(hypothesisId)
}
