package de.frank.entropyreducer.data.repository

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Frank-Wunsch 2026-05-09 (Abend): Mutationen triggern Drive-Sync.
 */
@Singleton
class HypothesisMessageRepository @Inject constructor(
    private val dao: HypothesisMessageDao,
    private val syncCoordinator: Lazy<SyncCoordinator>,
) {
    fun observeForHypothesis(hypothesisId: String): Flow<List<HypothesisMessageEntity>> =
        dao.getForHypothesis(hypothesisId)

    suspend fun getForHypothesis(hypothesisId: String): List<HypothesisMessageEntity> =
        dao.getForHypothesisOnce(hypothesisId)

    suspend fun insert(message: HypothesisMessageEntity) {
        dao.insert(message)
        syncCoordinator.get().requestSync("Forscher: Hypothese-Nachricht geaendert")
    }

    suspend fun deleteForHypothesis(hypothesisId: String) {
        dao.deleteForHypothesis(hypothesisId)
        syncCoordinator.get().requestSync("Forscher: Hypothese-Nachricht geaendert")
    }
}
