package de.frank.stacklabor.werftstudio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import de.frank.stacklabor.werftstudio.data.local.entity.MittelEntity
import de.frank.stacklabor.werftstudio.data.local.entity.WirkstoffkomponenteEntity
import de.frank.stacklabor.werftstudio.data.local.relation.MittelMitKomponenten
import kotlinx.coroutines.flow.Flow

@Dao
interface KatalogDao {
    @Transaction
    @Query("SELECT * FROM mittel ORDER BY name COLLATE NOCASE, id")
    fun beobachteAlle(): Flow<List<MittelMitKomponenten>>

    @Transaction
    @Query("SELECT * FROM mittel WHERE id = :id")
    suspend fun hole(id: String): MittelMitKomponenten?

    @Transaction
    @Query("SELECT * FROM mittel ORDER BY id")
    suspend fun holeAlle(): List<MittelMitKomponenten>

    @Query("SELECT * FROM mittel ORDER BY id")
    suspend fun holeAlleEntities(): List<MittelEntity>

    @Query("SELECT * FROM wirkstoffkomponente ORDER BY mittelId, id")
    suspend fun holeAlleKomponenten(): List<WirkstoffkomponenteEntity>

    @Query("SELECT * FROM mittel WHERE name LIKE '%' || :suchtext || '%' COLLATE NOCASE ORDER BY name COLLATE NOCASE")
    fun suche(suchtext: String): Flow<List<MittelEntity>>

    @Upsert
    suspend fun speichere(mittel: MittelEntity)

    @Upsert
    suspend fun speichereAlle(mittel: List<MittelEntity>)

    @Upsert
    suspend fun speichereKomponenten(komponenten: List<WirkstoffkomponenteEntity>)

    @Query("DELETE FROM wirkstoffkomponente WHERE mittelId = :mittelId")
    suspend fun loescheKomponenten(mittelId: String)

    @Delete
    suspend fun loesche(mittel: MittelEntity)

    @Query("SELECT COUNT(*) FROM stack_eintrag WHERE mittelId = :mittelId")
    suspend fun verwendungszahl(mittelId: String): Int
}
