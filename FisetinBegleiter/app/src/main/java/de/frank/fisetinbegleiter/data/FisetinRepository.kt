package de.frank.fisetinbegleiter.data

import androidx.room.withTransaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class FisetinRepository(private val database: FisetinDatabase) {
    private val dao = database.dao()

    val protocol: Flow<ProtocolTemplateEntity?> = dao.observeProtocol()
    val ingredients: Flow<List<IngredientEntity>> = dao.observeIngredients()
    val stackItems: Flow<List<StackItemEntity>> = dao.observeStackItems()
    val activeCure: Flow<CureWithDays?> = dao.observeActiveCure()
    val history: Flow<List<CureWithDays>> = dao.observeCureHistory()

    suspend fun ensureSeeded() = database.withTransaction {
        if (dao.getProtocol() == null) dao.upsertProtocol(ProtocolTemplateEntity())
        if (dao.ingredientCount() == 0) dao.insertIngredients(SeedData.ingredients)
        if (dao.stackItemCount() == 0) dao.insertStackItems(SeedData.stackItems)
    }

    suspend fun createCure(
        durationDays: Int,
        startDate: LocalDate,
        carrierLiquid: String,
        fatCarrier: String,
        preferredStartMinuteOfDay: Int,
    ): Long = database.withTransaction {
        val cureId = dao.insertCure(
            CureEntity(
                startEpochDay = startDate.toEpochDay(),
                plannedDurationDays = durationDays,
                status = CureStatus.GEPLANT,
                carrierLiquid = carrierLiquid,
                fatCarrier = fatCarrier,
                preferredStartMinuteOfDay = preferredStartMinuteOfDay,
                isTestRun = false,
            ),
        )
        dao.insertCureDays(
            (1..durationDays).map { day ->
                CureDayEntity(
                    cureId = cureId,
                    dayNumber = day,
                    plannedEpochDay = startDate.plusDays((day - 1).toLong()).toEpochDay(),
                )
            },
        )
        cureId
    }

    suspend fun setDrinkTime(cure: CureEntity, day: CureDayEntity, timestamp: Long) = database.withTransaction {
        dao.updateCure(cure.copy(status = CureStatus.AKTIV))
        dao.updateCureDay(day.copy(t0 = timestamp, drinkDoneAt = timestamp))
    }

    suspend fun markMeal(dayId: Long, timestamp: Long) = updateDay(dayId) { it.copy(mealDoneAt = timestamp) }

    suspend fun markSpermidin(dayId: Long, timestamp: Long) = updateDay(dayId) { it.copy(spermidinDoneAt = timestamp) }

    suspend fun markBlockEnded(dayId: Long, timestamp: Long) = updateDay(dayId) { day ->
        day.copy(blockEndedAt = day.blockEndedAt ?: timestamp)
    }

    suspend fun completeDay(cure: CureEntity, day: CureDayEntity, timestamp: Long) = database.withTransaction {
        dao.updateCureDay(day.copy(completedAt = timestamp))
        if (day.dayNumber == cure.plannedDurationDays) {
            dao.updateCure(cure.copy(status = CureStatus.ABGESCHLOSSEN, completedAt = timestamp))
        }
    }

    suspend fun updateNote(cure: CureEntity, note: String) = dao.updateCure(cure.copy(note = note))

    suspend fun deleteAllActiveCures() = dao.deleteAllActiveCures()

    suspend fun saveProtocol(protocol: ProtocolTemplateEntity) = dao.upsertProtocol(protocol)
    suspend fun saveIngredient(ingredient: IngredientEntity) = dao.upsertIngredient(ingredient)
    suspend fun deleteIngredient(ingredient: IngredientEntity) = dao.deleteIngredient(ingredient)
    suspend fun saveStackItem(item: StackItemEntity) = dao.upsertStackItem(item)
    suspend fun deleteStackItem(item: StackItemEntity) = dao.deleteStackItem(item)

    private suspend fun updateDay(dayId: Long, transform: (CureDayEntity) -> CureDayEntity) {
        dao.getCureDay(dayId)?.let { dao.updateCureDay(transform(it)) }
    }

}
