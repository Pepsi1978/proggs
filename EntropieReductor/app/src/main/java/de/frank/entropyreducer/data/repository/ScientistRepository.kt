package de.frank.entropyreducer.data.repository

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet Wissenschaftler-Sessions und deren Chatverlauf.
 * Spec §12.
 *
 * Frank-Wunsch 2026-05-09 (Abend): Jede Mutation triggert einen Drive-Sync,
 * damit Forscher-Sessions und Messages ins Backup wandern. touchSession
 * triggert KEINEN Sync — das wird beim Lesen aufgerufen und ein Sync pro
 * UI-Tap waere uebertrieben.
 */
@Singleton
class ScientistRepository @Inject constructor(
    private val sessionDao: ScientistSessionDao,
    private val messageDao: ScientistMessageDao,
    private val syncCoordinator: Lazy<SyncCoordinator>,
) {
    fun observeActiveSessions(): Flow<List<ScientistSessionEntity>> = sessionDao.getActive()
    fun observeAllSessions(): Flow<List<ScientistSessionEntity>> = sessionDao.getAll()
    fun observeMessages(sessionId: String): Flow<List<ScientistMessageEntity>> =
        messageDao.getForSession(sessionId)

    suspend fun getSession(id: String): ScientistSessionEntity? = sessionDao.getById(id)

    suspend fun upsertSession(session: ScientistSessionEntity) {
        sessionDao.upsert(session)
        syncCoordinator.get().requestSync("Forscher: Session/Nachricht geaendert")
    }
    suspend fun updateSession(session: ScientistSessionEntity) {
        sessionDao.update(session)
        syncCoordinator.get().requestSync("Forscher: Session/Nachricht geaendert")
    }
    suspend fun archiveSession(session: ScientistSessionEntity) {
        sessionDao.update(session.copy(isArchived = true))
        syncCoordinator.get().requestSync("Forscher: Session/Nachricht geaendert")
    }

    suspend fun insertMessage(message: ScientistMessageEntity) {
        messageDao.insert(message)
        syncCoordinator.get().requestSync("Forscher: Session/Nachricht geaendert")
    }
    suspend fun updateMessage(message: ScientistMessageEntity) {
        messageDao.update(message)
        syncCoordinator.get().requestSync("Forscher: Session/Nachricht geaendert")
    }
    suspend fun deleteMessageById(id: String) {
        messageDao.deleteById(id)
        syncCoordinator.get().requestSync("Forscher: Session/Nachricht geaendert")
    }
    suspend fun getAllMessages(): List<ScientistMessageEntity> = messageDao.getAll()

    suspend fun touchSession(sessionId: String) {
        val s = sessionDao.getById(sessionId) ?: return
        sessionDao.update(s.copy(lastActiveAt = System.currentTimeMillis()))
        // Keine Sync-Anforderung — touchSession ist ein UI-Reaktions-Update,
        // wird oft aufgerufen, fuehrt aber zu keinem inhaltlich neuen Stand.
    }
}
