package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.MemoryDao
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.domain.model.MemorySource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao,
) {
    fun getAll(): Flow<List<MemoryEntryEntity>> = dao.getAll()
    fun getActive(): Flow<List<MemoryEntryEntity>> = dao.getActive()
    fun getProposals(): Flow<List<MemoryEntryEntity>> = dao.getBySource(MemorySource.KI_VORSCHLAG)

    suspend fun upsert(memory: MemoryEntryEntity) = dao.upsert(memory)
    suspend fun delete(memory: MemoryEntryEntity) = dao.delete(memory)
    suspend fun deleteAll() = dao.deleteAll()
}
