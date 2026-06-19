package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.PriorityMemoryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import de.frank.entropyreducer.domain.usecase.selectMemoryToUpdate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Repository fuer das Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19).
 * Kapselt das DAO und die Lern-Logik (anlegen/aktualisieren ohne Duplikat).
 */
@Singleton
class PriorityMemoryRepository
@Inject
constructor(
    private val dao: PriorityMemoryDao,
) {
    fun observeAll(): Flow<List<PriorityMemoryEntity>> = dao.getAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun getById(id: String): PriorityMemoryEntity? = dao.getById(id)

    suspend fun getNewest(limit: Int): List<PriorityMemoryEntity> = dao.getNewest(limit)

    suspend fun getAllForBackup(): List<PriorityMemoryEntity> = dao.getAllForBackup()

    /**
     * Legt einen Gedaechtnis-Eintrag an oder aktualisiert einen vorhandenen (Dedup, kein Duplikat).
     * Wird bei jeder manuellen Prio-Setzung gerufen (auch fuer Loop-Aufgaben).
     */
    suspend fun learnFromManualPriority(entry: EntropyEntryEntity, priority: Double, now: Long) {
        val existing = selectMemoryToUpdate(dao.getAllForBackup(), entry.id, entry.title)
        if (existing != null) {
            dao.update(
                existing.copy(
                    title = entry.title,
                    description = entry.description,
                    priority = priority,
                    updatedAt = now,
                    sourceEntryId = entry.id,
                ),
            )
        } else {
            dao.upsert(
                PriorityMemoryEntity(
                    id = UUID.randomUUID().toString(),
                    title = entry.title,
                    description = entry.description,
                    priority = priority,
                    createdAt = now,
                    updatedAt = now,
                    sourceEntryId = entry.id,
                ),
            )
        }
    }

    suspend fun updatePriority(id: String, priority: Double, now: Long) {
        dao.getById(id)?.let { dao.update(it.copy(priority = priority, updatedAt = now)) }
    }

    suspend fun updateContent(id: String, title: String, description: String, now: Long) {
        dao.getById(id)?.let {
            dao.update(it.copy(title = title, description = description, updatedAt = now))
        }
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}
