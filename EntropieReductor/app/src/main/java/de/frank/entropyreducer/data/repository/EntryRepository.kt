package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Offline-first Repository fuer Entropie-Eintraege — Source of Truth ist Room. */
@Singleton
class EntryRepository @Inject constructor(
    private val dao: EntropyEntryDao,
) {
    fun getActive(): Flow<List<EntropyEntryEntity>> = dao.getActive()
    fun getByBucket(bucket: TimeBucket): Flow<List<EntropyEntryEntity>> = dao.getByTimeBucket(bucket)
    fun getByCategory(cat: EntropyCategory): Flow<List<EntropyEntryEntity>> = dao.getByCategory(cat)
    fun countByStatus(status: EntryStatus): Flow<Int> = dao.countByStatus(status)

    suspend fun get(id: String): EntropyEntryEntity? = dao.getById(id)
    suspend fun upsert(entry: EntropyEntryEntity) = dao.upsert(entry)
    suspend fun update(entry: EntropyEntryEntity) = dao.update(entry)
    suspend fun delete(entry: EntropyEntryEntity) = dao.delete(entry)
    suspend fun deleteAll() = dao.deleteAll()
}
