package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.KiTriggerDao
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Schreibender und lesender Zugriff auf die KI-Trigger (Spec §16.2).
 * Kapselt DAO-Zugriff sodass UseCases und ViewModels keine Room-Abhaengigkeit
 * direkt importieren müssen.
 */
@Singleton
class KiTriggerRepository @Inject constructor(
    private val dao: KiTriggerDao,
) {
    fun observePendingApproval(): Flow<List<KiTriggerEntity>> = dao.getPendingApproval()
    fun observeActive(): Flow<List<KiTriggerEntity>> = dao.getActive()

    suspend fun upsert(trigger: KiTriggerEntity) = dao.upsert(trigger)
    suspend fun update(trigger: KiTriggerEntity) = dao.update(trigger)
    suspend fun deleteById(id: String) = dao.deleteById(id)

    /** Approval-Flow: Trigger annehmen — aktiviert ihn und setzt approvedAt. */
    suspend fun approve(trigger: KiTriggerEntity, now: Long = System.currentTimeMillis()) {
        dao.update(trigger.copy(isActive = true, approvedAt = now))
    }

    /** Approval-Flow: Trigger ablehnen — entfernt den Eintrag komplett. */
    suspend fun reject(trigger: KiTriggerEntity) = dao.deleteById(trigger.id)

    /** Markiert einen Trigger als gefeuert (zaehlt triggerCount + lastTriggeredAt). */
    suspend fun markFired(trigger: KiTriggerEntity, now: Long = System.currentTimeMillis()) {
        dao.update(
            trigger.copy(
                triggerCount = trigger.triggerCount + 1,
                lastTriggeredAt = now,
            ),
        )
    }
}
