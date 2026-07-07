package de.frank.entropyreducer.data.repository

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.domain.model.HypothesisStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet Hypothesen / Experimente. Spec §12.3, §14.3.
 *
 * Frank-Wunsch 2026-05-09 (Abend): Jede Mutation triggert einen Drive-Sync,
 * damit Hypothesen ins Backup wandern.
 */
@Singleton
class HypothesisRepository @Inject constructor(
    private val dao: HypothesisDao,
    private val messageDao: HypothesisMessageDao,
    private val syncCoordinator: Lazy<SyncCoordinator>,
) {
    fun observeAll(): Flow<List<HypothesisEntity>> = dao.getAll()
    fun observeByStatus(status: HypothesisStatus): Flow<List<HypothesisEntity>> =
        dao.getByStatus(status)
    fun observeActiveOnDate(date: Long): Flow<List<HypothesisEntity>> = dao.getActiveOnDate(date)

    suspend fun get(id: String): HypothesisEntity? = dao.getById(id)
    suspend fun upsert(h: HypothesisEntity) {
        dao.upsert(h)
        syncCoordinator.get().requestSync("Forscher: Hypothese geaendert")
    }
    suspend fun update(h: HypothesisEntity) {
        dao.update(h)
        syncCoordinator.get().requestSync("Forscher: Hypothese geaendert")
    }

    /**
     * Loescht die Hypothese komplett samt ihrer Inline-Chat-Messages.
     * Die Hypothese verschwindet aus allen Listen, die zugehoerige Konversation auch.
     */
    suspend fun delete(h: HypothesisEntity) {
        messageDao.deleteForHypothesis(h.id)
        dao.delete(h)
        syncCoordinator.get().requestSync("Forscher: Hypothese geaendert")
    }
}
