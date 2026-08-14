package de.frank.stacklabor.werftstudio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import de.frank.stacklabor.werftstudio.data.local.entity.EigeneFrageEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackZielEntity
import de.frank.stacklabor.werftstudio.data.local.entity.ZielEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZielDao {
    @Query("SELECT * FROM ziel ORDER BY text COLLATE NOCASE, id")
    fun beobachteZielKatalog(): Flow<List<ZielEntity>>

    @Query("SELECT * FROM ziel ORDER BY id")
    suspend fun holeAlleZiele(): List<ZielEntity>

    @Query("SELECT * FROM ziel WHERE id = :id")
    suspend fun holeZiel(id: String): ZielEntity?

    @Query("SELECT * FROM stack_ziel WHERE stackId = :stackId ORDER BY rang, zielId")
    fun beobachteStackZiele(stackId: String): Flow<List<StackZielEntity>>

    @Query("SELECT * FROM stack_ziel WHERE stackId = :stackId ORDER BY rang, zielId")
    suspend fun holeStackZiele(stackId: String): List<StackZielEntity>

    @Query("SELECT * FROM stack_ziel ORDER BY stackId, rang, zielId")
    suspend fun holeAlleStackZiele(): List<StackZielEntity>

    @Query("SELECT * FROM stack_ziel ORDER BY stackId, rang, zielId")
    fun beobachteAlleStackZiele(): Flow<List<StackZielEntity>>

    @Upsert
    suspend fun speichereZiel(ziel: ZielEntity)

    @Upsert
    suspend fun speichereZiele(ziele: List<ZielEntity>)

    @Delete
    suspend fun loescheZiel(ziel: ZielEntity)

    @Upsert
    suspend fun speichereStackZiel(stackZiel: StackZielEntity)

    @Upsert
    suspend fun speichereStackZiele(stackZiele: List<StackZielEntity>)

    @Query("DELETE FROM stack_ziel WHERE stackId = :stackId AND zielId = :zielId")
    suspend fun loescheStackZiel(stackId: String, zielId: String)

    @Query("UPDATE stack_ziel SET rang = :rang WHERE stackId = :stackId AND zielId = :zielId")
    suspend fun setzeZielRang(stackId: String, zielId: String, rang: Int)

    @Query("SELECT * FROM eigene_frage WHERE stackId = :stackId ORDER BY id")
    fun beobachteFragen(stackId: String): Flow<List<EigeneFrageEntity>>

    @Query("SELECT * FROM eigene_frage WHERE stackId = :stackId ORDER BY id")
    suspend fun holeFragen(stackId: String): List<EigeneFrageEntity>

    @Query("SELECT * FROM eigene_frage WHERE id = :id")
    suspend fun holeFrage(id: String): EigeneFrageEntity?

    @Query("SELECT * FROM eigene_frage ORDER BY stackId, id")
    suspend fun holeAlleFragen(): List<EigeneFrageEntity>

    @Upsert
    suspend fun speichereFrage(frage: EigeneFrageEntity)

    @Upsert
    suspend fun speichereFragen(fragen: List<EigeneFrageEntity>)

    @Delete
    suspend fun loescheFrage(frage: EigeneFrageEntity)
}
