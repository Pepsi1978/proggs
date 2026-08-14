package de.frank.stacklabor.daten.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.stacklabor.daten.entitaeten.EigeneFrageE
import kotlinx.coroutines.flow.Flow

/** Eigene KI-Fragen je Stack (F-11), Bildschirm B-08. */
@Dao
interface FrageDao {

    @Query("SELECT * FROM eigene_frage WHERE stackId = :stackId ORDER BY rowid ASC")
    fun fragenDesStacks(stackId: String): Flow<List<EigeneFrageE>>

    /** Die Fragen, die bei F-12 mitgeschickt werden. */
    @Query("SELECT * FROM eigene_frage WHERE stackId = :stackId ORDER BY rowid ASC")
    suspend fun fragenDesStacksEinmalig(stackId: String): List<EigeneFrageE>

    /** Fuer F-13: alle Fragen aller Stacks. */
    @Query("SELECT * FROM eigene_frage ORDER BY stackId ASC, rowid ASC")
    suspend fun alleFragenEinmalig(): List<EigeneFrageE>

    @Query("SELECT * FROM eigene_frage WHERE id = :frageId")
    suspend fun frageEinmalig(frageId: String): EigeneFrageE?

    @Query("SELECT COUNT(*) FROM eigene_frage WHERE stackId = :stackId")
    fun anzahlFragen(stackId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenFrage(frage: EigeneFrageE)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegenFragen(fragen: List<EigeneFrageE>)

    @Update
    suspend fun aktualisierenFrage(frage: EigeneFrageE)

    @Delete
    suspend fun loeschenFrage(frage: EigeneFrageE)

    @Query("DELETE FROM eigene_frage WHERE id = :frageId")
    suspend fun loeschenFrage(frageId: String)
}
