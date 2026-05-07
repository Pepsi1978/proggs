package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet Wissenschaftler-Sessions und deren Chatverlauf.
 * Spec §12.
 */
@Singleton
class ScientistRepository @Inject constructor(
    private val sessionDao: ScientistSessionDao,
    private val messageDao: ScientistMessageDao,
) {
    fun observeActiveSessions(): Flow<List<ScientistSessionEntity>> = sessionDao.getActive()
    fun observeAllSessions(): Flow<List<ScientistSessionEntity>> = sessionDao.getAll()
    fun observeMessages(sessionId: String): Flow<List<ScientistMessageEntity>> =
        messageDao.getForSession(sessionId)

    suspend fun getSession(id: String): ScientistSessionEntity? = sessionDao.getById(id)

    suspend fun upsertSession(session: ScientistSessionEntity) = sessionDao.upsert(session)
    suspend fun updateSession(session: ScientistSessionEntity) = sessionDao.update(session)
    suspend fun archiveSession(session: ScientistSessionEntity) =
        sessionDao.update(session.copy(isArchived = true))

    suspend fun insertMessage(message: ScientistMessageEntity) = messageDao.insert(message)

    suspend fun touchSession(sessionId: String) {
        val s = sessionDao.getById(sessionId) ?: return
        sessionDao.update(s.copy(lastActiveAt = System.currentTimeMillis()))
    }
}
