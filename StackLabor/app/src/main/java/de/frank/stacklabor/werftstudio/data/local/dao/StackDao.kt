package de.frank.stacklabor.werftstudio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.frank.stacklabor.werftstudio.data.local.entity.AlternationsPartnerEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragDosisEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackSortieransichtEntity
import de.frank.stacklabor.werftstudio.data.local.relation.StackEintragMitDosenUndPartnern
import kotlinx.coroutines.flow.Flow

@Dao
interface StackDao {
    @Query("SELECT * FROM stack ORDER BY sortierung, id")
    fun beobachteStacks(): Flow<List<StackEntity>>

    @Query("SELECT * FROM stack WHERE id = :id")
    suspend fun holeStack(id: String): StackEntity?

    @Query("SELECT * FROM stack ORDER BY sortierung, id")
    suspend fun holeAlleStacks(): List<StackEntity>

    @Upsert
    suspend fun speichereStack(stack: StackEntity)

    @Upsert
    suspend fun speichereStacks(stacks: List<StackEntity>)

    @Delete
    suspend fun loescheStack(stack: StackEntity)

    @Query("UPDATE stack SET gesperrt = :gesperrt WHERE id = :stackId")
    suspend fun setzeGesperrt(stackId: String, gesperrt: Boolean)

    @Query("UPDATE stack SET sortierung = :position WHERE id = :stackId")
    suspend fun setzeStackPosition(stackId: String, position: Int)

    @Transaction
    @Query("SELECT * FROM stack_eintrag WHERE stackId = :stackId ORDER BY reihenfolge, id")
    fun beobachteEintraege(stackId: String): Flow<List<StackEintragMitDosenUndPartnern>>

    @Transaction
    @Query("SELECT * FROM stack_eintrag ORDER BY stackId, reihenfolge, id")
    fun beobachteAlleEintraege(): Flow<List<StackEintragMitDosenUndPartnern>>

    @Transaction
    @Query("SELECT * FROM stack_eintrag WHERE stackId = :stackId ORDER BY reihenfolge, id")
    suspend fun holeEintraege(stackId: String): List<StackEintragMitDosenUndPartnern>

    @Transaction
    @Query("SELECT * FROM stack_eintrag WHERE id = :eintragId")
    suspend fun holeEintrag(eintragId: String): StackEintragMitDosenUndPartnern?

    @Query("SELECT * FROM stack_eintrag WHERE id = :eintragId")
    suspend fun holeEintragEntity(eintragId: String): StackEintragEntity?

    @Query("SELECT * FROM stack_eintrag ORDER BY stackId, reihenfolge, id")
    suspend fun holeAlleEintragEntities(): List<StackEintragEntity>

    @Query("SELECT * FROM stack_eintrag_dosis ORDER BY stackEintragId, variante")
    suspend fun holeAlleDosen(): List<StackEintragDosisEntity>

    @Query("SELECT * FROM alternations_partner ORDER BY stackEintragId, partnerMittelId")
    suspend fun holeAlleAlternationsPartner(): List<AlternationsPartnerEntity>

    @Upsert
    suspend fun speichereEintrag(eintrag: StackEintragEntity)

    @Upsert
    suspend fun speichereEintraege(eintraege: List<StackEintragEntity>)

    @Upsert
    suspend fun speichereDosen(dosen: List<StackEintragDosisEntity>)

    @Upsert
    suspend fun speichereAlternationsPartner(partner: List<AlternationsPartnerEntity>)

    @Query("DELETE FROM stack_eintrag_dosis WHERE stackEintragId = :eintragId")
    suspend fun loescheDosen(eintragId: String)

    @Query("DELETE FROM alternations_partner WHERE stackEintragId = :eintragId")
    suspend fun loescheAlternationsPartner(eintragId: String)

    @Delete
    suspend fun loescheEintrag(eintrag: StackEintragEntity)

    @Query("DELETE FROM stack_eintrag WHERE stackId IN (:stackIds)")
    suspend fun loescheEintraegeDerStacks(stackIds: List<String>)

    @Query("UPDATE stack_eintrag SET aktiv = :aktiv WHERE id = :eintragId")
    suspend fun setzeAktiv(eintragId: String, aktiv: Boolean)

    @Query("UPDATE stack_eintrag SET offenerHinweis = :hinweis WHERE id = :eintragId")
    suspend fun setzeOffenenHinweis(eintragId: String, hinweis: String?)

    @Query("UPDATE stack_eintrag SET reihenfolge = :position WHERE id = :eintragId")
    suspend fun setzeEintragPosition(eintragId: String, position: Int)

    @Query("UPDATE stack_eintrag SET mittelId = :behalteneMittelId WHERE mittelId = :zuErsetzendeMittelId")
    suspend fun haengeMittelUm(zuErsetzendeMittelId: String, behalteneMittelId: String)

    @Upsert
    suspend fun speichereSortieransicht(ansicht: StackSortieransichtEntity)

    @Query("SELECT * FROM stack_sortieransicht ORDER BY stackId")
    suspend fun holeAlleSortieransichten(): List<StackSortieransichtEntity>

    @Query("SELECT * FROM stack_sortieransicht WHERE stackId = :stackId")
    suspend fun holeSortieransicht(stackId: String): StackSortieransichtEntity?

    @Query("SELECT * FROM stack_sortieransicht WHERE stackId = :stackId")
    fun beobachteSortieransicht(stackId: String): Flow<StackSortieransichtEntity?>
}
