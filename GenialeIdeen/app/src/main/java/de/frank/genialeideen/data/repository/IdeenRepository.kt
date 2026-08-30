package de.frank.genialeideen.data.repository

import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.data.local.Kategorieart
import de.frank.genialeideen.data.local.NachrichtEntity
import de.frank.genialeideen.data.local.SuchanfrageEntity
import de.frank.genialeideen.observability.IdeenLog
import java.text.Normalizer
import kotlinx.coroutines.flow.Flow

class IdeenRepository(private val datenbank: GenialeIdeenDatabase) {
    private val ideenDao = datenbank.ideenDao()
    private val nachrichtenDao = datenbank.nachrichtenDao()
    private val suchverlaufDao = datenbank.suchverlaufDao()
    private val kategorienDao = datenbank.kategorienDao()

    fun alleIdeen(): Flow<List<IdeeEntity>> = ideenDao.alle()

    fun beobachteIdee(id: Long): Flow<IdeeEntity?> = ideenDao.beobachte(id)

    fun nachrichten(ideeId: Long): Flow<List<NachrichtEntity>> = nachrichtenDao.fuerIdee(ideeId)

    fun letzteSuchanfragen(): Flow<List<SuchanfrageEntity>> = suchverlaufDao.letzte()

    fun alleKategorien(): Flow<List<KategorieEntity>> = kategorienDao.alle()

    /** Ausschließlich eine Nutzeraktion darf diesen manuellen Anlagepfad aufrufen. */
    suspend fun legeKategorieAn(rohName: String, art: Kategorieart): Long? {
        val name = rohName.trim().take(60)
        if (name.isBlank()) return null
        kategorienDao.nachNameUndArt(name, art)?.let { return it.id }
        val reihenfolge = kategorienDao.anzahl(art)
        val id = kategorienDao.einfuegen(
            KategorieEntity(name = name, reihenfolge = reihenfolge, art = art),
        )
        return if (id > 0) id else kategorienDao.nachNameUndArt(name, art)?.id
    }

    suspend fun benenneKategorieUm(id: Long, rohName: String): Boolean {
        val name = rohName.trim().take(60)
        if (name.isBlank()) return false
        val kategorie = kategorienDao.nachId(id) ?: return false
        val doppelt = kategorienDao.nachNameUndArt(name, kategorie.art)
        if (doppelt != null && doppelt.id != id) return false
        kategorienDao.benenneUm(id, name)
        return true
    }

    suspend fun setzeKategorie(ideeId: Long, kategorieId: Long?) =
        ideenDao.setzeKategorie(ideeId, kategorieId)

    suspend fun loescheKategorie(id: Long) = kategorienDao.loescheMitZuordnungen(id)

    suspend fun lege(
        titel: String,
        text: String,
        aufnahmePfad: String? = null,
        originalText: String? = null,
        kategorieId: Long? = null,
    ): Long {
        val oben = ideenDao.naechsteReihenfolgeOben(IdeenStatus.OFFEN.name)
        val id = ideenDao.einfuegen(
            IdeeEntity(
                titel = titel,
                text = text,
                reihenfolge = oben,
                aufnahmePfad = aufnahmePfad,
                originalText = originalText,
                kategorieId = kategorieId,
            ),
        )
        IdeenLog.info("Ideen", "lege", "Neue Idee angelegt", mapOf("id" to id, "chars" to text.length))
        return id
    }

    suspend fun aendere(idee: IdeeEntity, titel: String, text: String) {
        ideenDao.aktualisieren(
            idee.copy(titel = titel, text = text, geaendertAm = System.currentTimeMillis()),
        )
    }

    suspend fun setzeStatus(idee: IdeeEntity, status: IdeenStatus) {
        val oben = ideenDao.naechsteReihenfolgeOben(status.name)
        ideenDao.aktualisieren(
            idee.copy(
                status = status.name,
                reihenfolge = oben,
                geaendertAm = System.currentTimeMillis(),
                umgesetztAm = if (status == IdeenStatus.UMGESETZT) System.currentTimeMillis() else null,
            ),
        )
        IdeenLog.info("Ideen", "setzeStatus", "Status gewechselt", mapOf("id" to idee.id, "status" to status.name))
    }

    suspend fun loesche(idee: IdeeEntity) {
        idee.aufnahmePfad?.let { pfad -> runCatching { java.io.File(pfad).delete() } }
        ideenDao.loeschen(idee)
    }

    suspend fun schreibeReihenfolge(ids: List<Long>) = ideenDao.schreibeReihenfolge(ids)

    suspend fun lade(id: Long): IdeeEntity? = ideenDao.lade(id)

    suspend fun ergaenzeNachricht(
        ideeId: Long,
        rolle: String,
        text: String,
        unvollstaendig: Boolean = false,
    ): Long = nachrichtenDao.einfuegen(
        NachrichtEntity(ideeId = ideeId, rolle = rolle, text = text, unvollstaendig = unvollstaendig),
    )

    suspend fun aktualisiereNachricht(nachricht: NachrichtEntity) = nachrichtenDao.aktualisieren(nachricht)

    suspend fun loescheNachricht(id: Long) = nachrichtenDao.loesche(id)

    suspend fun loescheNachrichten(ids: List<Long>) = nachrichtenDao.loescheMehrere(ids)

    suspend fun loescheKonversation(ideeId: Long) = nachrichtenDao.loescheFuerIdee(ideeId)

    suspend fun nachrichtenEinmal(ideeId: Long): List<NachrichtEntity> =
        nachrichtenDao.fuerIdeeEinmal(ideeId)

    /**
     * Sucht über Titel und Text. Umlaute und Ersatzschreibung finden dasselbe: Die Anfrage wird
     * in beiden Schreibweisen als ODER-Ausdruck an FTS4 gegeben (Baustein K).
     */
    suspend fun suche(rohAnfrage: String): List<IdeeEntity> {
        val anfrage = rohAnfrage.trim()
        if (anfrage.length < 2) return emptyList()
        val varianten = linkedSetOf(anfrage.lowercase(), entumlaute(anfrage), umlaute(anfrage))
            .filter { it.isNotBlank() }
        val ausdruck = varianten.joinToString(" OR ") { variante ->
            variante.split(Regex("""\s+""")).filter(String::isNotBlank).joinToString(" ") { wort ->
                "${wort.replace("\"", "")}*"
            }
        }
        return runCatching { ideenDao.suche(ausdruck) }
            .onFailure { IdeenLog.warn("Suche", "suche", "FTS-Ausdruck abgelehnt", mapOf("laenge" to anfrage.length)) }
            .getOrDefault(emptyList())
    }

    suspend fun merkeSuchanfrage(anfrage: String) {
        if (anfrage.trim().length < 2) return
        suchverlaufDao.merken(SuchanfrageEntity(anfrage.trim()))
    }

    suspend fun leereSuchverlauf() = suchverlaufDao.leeren()

    private fun entumlaute(text: String): String = text.lowercase()
        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        .let { Normalizer.normalize(it, Normalizer.Form.NFC) }

    private fun umlaute(text: String): String = text.lowercase()
        .replace("ae", "ä").replace("oe", "ö").replace("ue", "ü").replace("ss", "ß")
}
