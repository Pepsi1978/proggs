package de.frank.entropyreducer.data.repository

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.domain.model.MemorySource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Frank-Wunsch 2026-05-09 (Abend): Jede Mutation triggert einen Drive-Sync, damit Memories ins
 * Backup wandern. SyncCoordinator als Lazy fuer Zyklus-Vermeidung.
 */
@Singleton
class MemoryRepository
@Inject
constructor(private val dao: MemoryDao, private val syncCoordinator: Lazy<SyncCoordinator>) {
    fun getAll(): Flow<List<MemoryEntryEntity>> = dao.getAll()

    fun getActive(): Flow<List<MemoryEntryEntity>> = dao.getActive()

    fun getProposals(): Flow<List<MemoryEntryEntity>> = dao.getBySource(MemorySource.KI_VORSCHLAG)

    suspend fun upsert(memory: MemoryEntryEntity) {
        dao.upsert(memory)
        syncCoordinator.get().requestSync("Forscher: Memory geaendert")
    }

    suspend fun delete(memory: MemoryEntryEntity) {
        dao.delete(memory)
        syncCoordinator.get().requestSync("Forscher: Memory geaendert")
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        syncCoordinator.get().requestSync("Forscher: Memory geaendert")
    }

    suspend fun deleteBySource(src: MemorySource) {
        dao.deleteBySource(src)
        syncCoordinator.get().requestSync("Forscher: Memory geaendert")
    }
}
