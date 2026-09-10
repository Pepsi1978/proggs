package de.frank.stacklabor.werftstudio.data.local.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface BestandDao {
    @Query("SELECT COUNT(*) FROM stack")
    suspend fun stackAnzahl(): Int

    @Query("SELECT COUNT(*) FROM stack_eintrag")
    suspend fun eintragAnzahl(): Int

    @Query("SELECT COUNT(*) FROM mittel")
    suspend fun mittelAnzahl(): Int

    @Query("DELETE FROM frage_antwort") suspend fun loescheAlleAntworten()
    @Query("DELETE FROM konkurrenz") suspend fun loescheAlleKonkurrenzen()
    @Query("DELETE FROM bewertungszelle") suspend fun loescheAlleZellen()
    @Query("DELETE FROM bewertung") suspend fun loescheAlleBewertungen()
    @Query("DELETE FROM eigene_frage") suspend fun loescheAlleFragen()
    @Query("DELETE FROM stack_ziel") suspend fun loescheAlleStackZiele()
    @Query("DELETE FROM ziel") suspend fun loescheAlleZiele()
    @Query("DELETE FROM alternations_partner") suspend fun loescheAlleAlternationsPartner()
    @Query("DELETE FROM stack_eintrag_dosis") suspend fun loescheAlleDosen()
    @Query("DELETE FROM stack_eintrag") suspend fun loescheAlleEintraege()
    @Query("DELETE FROM stack_sortieransicht") suspend fun loescheAlleSortieransichten()
    @Query("DELETE FROM stack") suspend fun loescheAlleStacks()
    @Query("DELETE FROM wirkstoffkomponente") suspend fun loescheAlleKomponenten()
    @Query("DELETE FROM mittel") suspend fun loescheAlleMittel()
}
