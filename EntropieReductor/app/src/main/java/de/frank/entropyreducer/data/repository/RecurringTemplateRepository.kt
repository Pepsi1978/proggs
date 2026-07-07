package de.frank.entropyreducer.data.repository

import android.content.Context
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.TombstoneType
import de.frank.entropyreducer.data.local.dao.RecurringTemplateDao
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.markDeleted
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Repository fuer wiederkehrende Aufgaben-Vorlagen (Sprint 2, Frank-Wunsch 2026-05-22).
 *
 * Reine Pass-Through-Schicht auf RecurringTemplateDao — die Logik fuer
 * Faelligkeitsberechnung und Instanzgenerierung lebt in GenerateRecurringInstancesUseCase.
 *
 * Frank-Bugfix 2026-05-22: Jede Mutation triggert sofort einen Drive-Backup-Sync,
 * damit neue/geaenderte Loop-Aufgaben auf dem zweiten Geraet sofort ankommen.
 */
@Singleton
class RecurringTemplateRepository @Inject constructor(
    private val dao: RecurringTemplateDao,
    private val syncCoordinator: Lazy<SyncCoordinator>,
    @ApplicationContext private val appContext: Context,
) {
    fun observeAll(): Flow<List<RecurringTemplateEntity>> = dao.observeAll()

    suspend fun getActive(): List<RecurringTemplateEntity> = dao.getActive()

    suspend fun getById(id: String): RecurringTemplateEntity? = dao.getById(id)

    suspend fun upsert(template: RecurringTemplateEntity) {
        dao.upsert(template)
        syncCoordinator.get().requestSync("Wiederkehrende Aufgaben-Vorlage geaendert")
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
        // Sync-Etappe 1.3: Tombstone, damit die Loeschung beim Restore auf andere Geraete propagiert.
        markDeleted(appContext, TombstoneType.LOOP_TEMPLATE, id)
        syncCoordinator.get().requestSync("Wiederkehrende Aufgaben-Vorlage geaendert")
    }

    /** Sync-Etappe 1.3: Loescht ohne neuen Tombstone/Sync — fuer den Restore (Tombstone existiert schon). */
    suspend fun deleteByIdForRestore(id: String) {
        dao.deleteById(id)
    }

    suspend fun getAllForBackup(): List<RecurringTemplateEntity> = dao.getAllForBackup()
}
