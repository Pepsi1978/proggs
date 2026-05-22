package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.RecurringTemplateDao
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository fuer wiederkehrende Aufgaben-Vorlagen (Sprint 2, Frank-Wunsch 2026-05-22).
 *
 * Reine Pass-Through-Schicht auf RecurringTemplateDao — die Logik fuer
 * Faelligkeitsberechnung und Instanzgenerierung lebt in GenerateRecurringInstancesUseCase.
 */
@Singleton
class RecurringTemplateRepository @Inject constructor(
    private val dao: RecurringTemplateDao,
) {
    fun observeAll(): Flow<List<RecurringTemplateEntity>> = dao.observeAll()

    suspend fun getActive(): List<RecurringTemplateEntity> = dao.getActive()

    suspend fun getById(id: String): RecurringTemplateEntity? = dao.getById(id)

    suspend fun upsert(template: RecurringTemplateEntity) = dao.upsert(template)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun getAllForBackup(): List<RecurringTemplateEntity> = dao.getAllForBackup()
}
