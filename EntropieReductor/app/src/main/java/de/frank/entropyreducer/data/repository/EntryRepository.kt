package de.frank.entropyreducer.data.repository

import dagger.Lazy
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first Repository für Entropie-Eintraege — Source of Truth ist Room. Nach jeder Mutation
 * feuert ein Drive-Sync-Trigger, sofern Backup aktiviert ist. Lazy-Inject vermeidet Hilt-Zyklen
 * (SyncCoordinator -> EntryRepository -> SyncCoordinator).
 */
@Singleton
class EntryRepository
@Inject
constructor(private val dao: EntropyEntryDao, private val coordinatorLazy: Lazy<SyncCoordinator>) {
    fun getActive(): Flow<List<EntropyEntryEntity>> = dao.getActive()

    fun getByBucket(bucket: TimeBucket): Flow<List<EntropyEntryEntity>> =
        dao.getByTimeBucket(bucket)

    fun getByCategory(cat: EntropyCategory): Flow<List<EntropyEntryEntity>> = dao.getByCategory(cat)

    fun countByStatus(status: EntryStatus): Flow<Int> = dao.countByStatus(status)

    fun getRecentlyResolved(sinceMillis: Long): Flow<List<EntropyEntryEntity>> =
        dao.getRecentlyResolved(sinceMillis = sinceMillis)

    /** Archivierte Eintraege fuer den Settings-Archiv-Bereich (Frank-Wunsch 2026-05-09). */
    fun getArchived(): Flow<List<EntropyEntryEntity>> = dao.getArchived()

    /**
     * Frank-Wunsch 2026-05-19: ALLE Eintraege fuer Drive-Backup — auch archivierte (Bereich
     * "Entropie"). Wird nur vom SyncCoordinator beim Upload aufgerufen.
     */
    suspend fun getAllForBackup(): List<EntropyEntryEntity> = dao.getAllForBackup()

    /**
     * Alle REDUZIERT-Eintraege deren resolvedAt vor [beforeMillis] liegt — Kandidaten fuer
     * Auto-Archivierung.
     */
    suspend fun getResolvedBefore(beforeMillis: Long): List<EntropyEntryEntity> =
        dao.getResolvedBefore(beforeMillis = beforeMillis)

    suspend fun get(id: String): EntropyEntryEntity? = dao.getById(id)

    suspend fun upsert(entry: EntropyEntryEntity) {
        dao.upsert(entry)
        coordinatorLazy.get().requestSync()
    }

    suspend fun update(entry: EntropyEntryEntity) {
        dao.update(entry)
        coordinatorLazy.get().requestSync()
    }

    suspend fun delete(entry: EntropyEntryEntity) {
        dao.delete(entry)
        coordinatorLazy.get().requestSync()
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        coordinatorLazy.get().requestSync()
    }
}
