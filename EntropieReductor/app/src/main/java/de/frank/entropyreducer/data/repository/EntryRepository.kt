package de.frank.entropyreducer.data.repository

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.TombstoneType
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.markDeleted
import de.frank.entropyreducer.data.markManyDeleted
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.safety.PhoneContentGuard
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
constructor(
    private val dao: EntropyEntryDao,
    private val coordinatorLazy: Lazy<SyncCoordinator>,
    @ApplicationContext private val appContext: Context,
) {
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

    /**
     * Frank-Wunsch 2026-05-22 (dritte Iteration): KI-Trainingsdaten fuer
     * Dauer-Schaetzung. Liefert die letzten N Aufgaben mit MANUELL gesetzter
     * Dauer als Few-Shot-Beispiele.
     */
    suspend fun getRecentManualDurationSamples(limit: Int = 8): List<EntropyEntryEntity> =
        dao.getRecentManualDurationSamples(limit)

    suspend fun upsert(entry: EntropyEntryEntity) {
        if (
            PhoneContentGuard.isSecondBrainWorkArtifact(
                entry.title,
                entry.rawTranscript + "\n" + entry.description,
            )
        ) {
            return
        }
        dao.upsert(entry)
        coordinatorLazy.get().requestSync("Aufgabe/Entropie-Eintrag: hinzugefuegt/geaendert")
    }

    suspend fun update(entry: EntropyEntryEntity) {
        if (
            PhoneContentGuard.isSecondBrainWorkArtifact(
                entry.title,
                entry.rawTranscript + "\n" + entry.description,
            )
        ) {
            return
        }
        dao.update(entry)
        coordinatorLazy.get().requestSync("Aufgabe/Entropie-Eintrag: editiert")
    }

    suspend fun delete(entry: EntropyEntryEntity) {
        dao.delete(entry)
        // Sync-Etappe 1.2: Tombstone, damit die Loeschung beim Restore auch auf andere Geraete
        // propagiert (sonst wuerde der additive Restore die Aufgabe dort "auferstehen" lassen).
        markDeleted(appContext, TombstoneType.AUFGABE, entry.id)
        coordinatorLazy.get().requestSync("Aufgabe/Entropie-Eintrag: geloescht")
    }

    suspend fun deleteAll() {
        // IDs VOR dem Loeschen einsammeln, damit fuer jede ein Tombstone gesetzt werden kann.
        val ids = dao.getAllForBackup().map { it.id }
        dao.deleteAll()
        markManyDeleted(appContext, TombstoneType.AUFGABE, ids)
        coordinatorLazy.get().requestSync("Aufgabe/Entropie-Eintrag: alle geloescht")
    }

    /**
     * Sync-Etappe 1.2: Loescht eine Aufgabe OHNE neuen Tombstone und OHNE Sync-Trigger — fuer den
     * Restore, der eine per Tombstone als geloescht markierte Aufgabe lokal entfernt. Der Tombstone
     * existiert dort bereits; erneutes Markieren/Triggern waere ueberfluessig.
     */
    suspend fun deleteByIdForRestore(id: String) {
        val e = dao.getById(id) ?: return
        dao.delete(e)
    }

    /**
     * Sync-Etappe 1.5: Loest einen Drive-Backup-Sync aus fuer Aufgaben-bezogene Mutationen, die
     * NICHT ueber dieses Repository laufen (z.B. Nachtraege ueber den Followup-DAO oder das
     * Abhaken ueber das Home-Widget). Kein eigener DB-Schreibvorgang — nur der Trigger.
     */
    fun triggerBackup(reason: String) {
        coordinatorLazy.get().requestSync(reason)
    }
}
