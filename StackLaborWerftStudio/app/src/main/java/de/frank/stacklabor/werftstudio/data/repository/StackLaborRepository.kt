package de.frank.stacklabor.werftstudio.data.repository

import androidx.room.withTransaction
import de.frank.stacklabor.werftstudio.data.local.StackLaborDatabase
import de.frank.stacklabor.werftstudio.data.local.entity.AlternationsPartnerEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungszelleEntity
import de.frank.stacklabor.werftstudio.data.local.entity.EigeneFrageEntity
import de.frank.stacklabor.werftstudio.data.local.entity.FrageAntwortEntity
import de.frank.stacklabor.werftstudio.data.local.entity.KonkurrenzEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragDosisEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackSortieransichtEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackZielEntity
import de.frank.stacklabor.werftstudio.data.transfer.StackLaborJson
import de.frank.stacklabor.werftstudio.domain.AmpelBerechnung
import de.frank.stacklabor.werftstudio.domain.Pruefsumme
import de.frank.stacklabor.werftstudio.domain.PruefsummenDosis
import de.frank.stacklabor.werftstudio.domain.PruefsummenEintrag
import de.frank.stacklabor.werftstudio.domain.PruefsummenFrage
import de.frank.stacklabor.werftstudio.domain.PruefsummenKomponente
import de.frank.stacklabor.werftstudio.domain.PruefsummenMittel
import de.frank.stacklabor.werftstudio.domain.StackPruefsummenInhalt
import de.frank.stacklabor.werftstudio.domain.model.Ampel
import de.frank.stacklabor.werftstudio.domain.model.BewertungMitDetails
import de.frank.stacklabor.werftstudio.domain.model.BewertungsBereich
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.EigeneFrage
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Sortieransicht
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackInhalt
import de.frank.stacklabor.werftstudio.domain.model.StackZiel
import de.frank.stacklabor.werftstudio.domain.model.Ziel
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ImportModus { ERSETZEN, DAZU }

data class AmpelErgebnis(
    val mittelAmpeln: Map<String, Ampel>,
    val zielAmpeln: Map<String, Ampel>,
    val sammelAmpel: Ampel,
)

data class SeedImportErgebnis(
    val stacks: Int,
    val eintraege: Int,
    val mittel: Int,
)

data class StackSortierstatus(
    val ansicht: Sortieransicht = Sortieransicht.EINNAHME,
    val manuelleReihenfolge: Boolean = false,
)

interface StackLaborRepository {
    fun beobachteMittel(): Flow<List<Mittel>>
    fun beobachteStacks(): Flow<List<Stack>>
    fun beobachteEintraege(stackId: String): Flow<List<StackEintrag>>
    fun beobachteAlleEintraege(): Flow<List<StackEintrag>>
    fun beobachteZiele(): Flow<List<Ziel>>
    fun beobachteStackZiele(stackId: String): Flow<List<StackZiel>>
    fun beobachteAlleStackZiele(): Flow<List<StackZiel>>
    fun beobachteFragen(stackId: String): Flow<List<EigeneFrage>>
    fun beobachteNeuesteBewertung(stackId: String): Flow<BewertungMitDetails?>
    fun beobachteNeuesteTagBewertung(): Flow<BewertungMitDetails?>
    fun beobachteHistorie(stackId: String): Flow<List<BewertungMitDetails>>
    fun beobachteSortieransicht(stackId: String): Flow<StackSortierstatus>

    suspend fun speichereMittel(mittel: Mittel)
    suspend fun loescheMittel(mittelId: String, bestaetigt: Boolean = false): Int
    suspend fun fuehreMittelZusammen(behalteneMittelId: String, zuErsetzendeMittelId: String)
    suspend fun speichereStack(stack: Stack)
    suspend fun loescheStack(stackId: String)
    suspend fun speichereEintrag(eintrag: StackEintrag)
    suspend fun loescheEintrag(eintragId: String)
    suspend fun setzeEintragAktiv(eintragId: String, aktiv: Boolean)
    suspend fun setzeOffenenHinweis(eintragId: String, hinweis: String?)
    suspend fun speichereZiel(ziel: Ziel)
    suspend fun loescheZiel(zielId: String)
    suspend fun setzeStackZiel(stackZiel: StackZiel)
    suspend fun entferneStackZiel(stackId: String, zielId: String)
    suspend fun speichereFrage(frage: EigeneFrage)
    suspend fun loescheFrage(frageId: String)
    suspend fun setzeSortieransicht(stackId: String, sortieransicht: Sortieransicht)
    suspend fun sortiereStacks(stackIds: List<String>)
    suspend fun sortiereEintraege(stackId: String, eintragIds: List<String>)
    suspend fun sortiereZiele(stackId: String, zielIds: List<String>)
    suspend fun speichereBewertung(bewertung: BewertungMitDetails)
    suspend fun berechneAmpeln(stackId: String): AmpelErgebnis
    suspend fun berechnePruefsumme(stackId: String, dosisVariante: DosisVariante): String
    suspend fun istBewertungVeraltet(stackId: String, dosisVariante: DosisVariante): Boolean
    suspend fun exportiere(): String
    suspend fun importiere(json: String, modus: ImportModus)
    suspend fun stelleRueckfallsicherungWiederHer()
    suspend fun importiereStartbestand(): SeedImportErgebnis
    suspend fun initialisiereStartbestandWennLeer(): SeedImportErgebnis?
    suspend fun ladeStackInhalt(stackId: String): StackInhalt
}

class RoomStackLaborRepository(
    private val database: StackLaborDatabase,
    private val json: StackLaborJson,
    private val startbestandQuelle: StartbestandQuelle,
    private val rueckfallsicherungStore: RueckfallsicherungStore,
    private val jetzt: () -> Long = System::currentTimeMillis,
    private val neueId: () -> String = { UUID.randomUUID().toString() },
) : StackLaborRepository {
    private val katalogDao = database.katalogDao()
    private val stackDao = database.stackDao()
    private val zielDao = database.zielDao()
    private val bewertungDao = database.bewertungDao()
    private val bestandDao = database.bestandDao()

    override fun beobachteMittel(): Flow<List<Mittel>> = katalogDao.beobachteAlle()
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteStacks(): Flow<List<Stack>> = stackDao.beobachteStacks()
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteEintraege(stackId: String): Flow<List<StackEintrag>> = stackDao.beobachteEintraege(stackId)
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteAlleEintraege(): Flow<List<StackEintrag>> = stackDao.beobachteAlleEintraege()
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteZiele(): Flow<List<Ziel>> = zielDao.beobachteZielKatalog()
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteStackZiele(stackId: String): Flow<List<StackZiel>> = zielDao.beobachteStackZiele(stackId)
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteAlleStackZiele(): Flow<List<StackZiel>> = zielDao.beobachteAlleStackZiele()
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteFragen(stackId: String): Flow<List<EigeneFrage>> = zielDao.beobachteFragen(stackId)
        .map { liste -> liste.map { it.toDomain() } }
        .distinctUntilChanged()

    override fun beobachteNeuesteBewertung(stackId: String): Flow<BewertungMitDetails?> =
        bewertungDao.beobachteNeueste(BewertungsBereich.STACK, stackId)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override fun beobachteNeuesteTagBewertung(): Flow<BewertungMitDetails?> =
        bewertungDao.beobachteNeueste(BewertungsBereich.TAG, null)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override fun beobachteHistorie(stackId: String): Flow<List<BewertungMitDetails>> =
        bewertungDao.beobachteHistorie(BewertungsBereich.STACK, stackId, HISTORY_LIMIT)
            .map { liste -> liste.map { it.toDomain() } }
            .distinctUntilChanged()

    override fun beobachteSortieransicht(stackId: String): Flow<StackSortierstatus> =
        stackDao.beobachteSortieransicht(stackId)
            .map { entity ->
                if (entity == null) StackSortierstatus()
                else StackSortierstatus(entity.sortieransicht, entity.manuelleReihenfolge)
            }
            .distinctUntilChanged()

    override suspend fun speichereMittel(mittel: Mittel) {
        validiereMittel(mittel)
        database.withTransaction {
            katalogDao.speichere(mittel.toEntity())
            katalogDao.loescheKomponenten(mittel.id)
            if (mittel.wirkstoffkomponenten.isNotEmpty()) {
                katalogDao.speichereKomponenten(mittel.wirkstoffkomponenten.map { it.toEntity() })
            }
        }
    }

    override suspend fun loescheMittel(mittelId: String, bestaetigt: Boolean): Int {
        val mittel = katalogDao.hole(mittelId)?.mittel ?: return 0
        val verwendungen = katalogDao.verwendungszahl(mittelId)
        check(verwendungen == 0 || bestaetigt) { "Das Mittel wird in $verwendungen Stacks verwendet." }
        katalogDao.loesche(mittel)
        return verwendungen
    }

    override suspend fun fuehreMittelZusammen(behalteneMittelId: String, zuErsetzendeMittelId: String) {
        require(behalteneMittelId != zuErsetzendeMittelId) { "Ein Mittel kann nicht mit sich selbst zusammengefuehrt werden." }
        database.withTransaction {
            checkNotNull(katalogDao.hole(behalteneMittelId)) { "Das Zielmittel fehlt." }
            val quelle = checkNotNull(katalogDao.hole(zuErsetzendeMittelId)) { "Das zu ersetzende Mittel fehlt." }
            val alleEintraege = stackDao.holeAlleEintragEntities()
            val zielNachStack = alleEintraege.filter { it.mittelId == behalteneMittelId }.associateBy { it.stackId }
            alleEintraege.filter { it.mittelId == zuErsetzendeMittelId }.forEach { eintrag ->
                if (zielNachStack.containsKey(eintrag.stackId)) stackDao.loescheEintrag(eintrag)
                else stackDao.speichereEintrag(eintrag.copy(mittelId = behalteneMittelId))
            }

            val zellen = bewertungDao.holeAlleZellen()
            val vorhandeneZellenschluessel = zellen.filter { it.mittelId == behalteneMittelId }
                .mapTo(mutableSetOf()) { it.bewertungId to it.zielId }
            val neueZellen = zellen.filter { it.mittelId == zuErsetzendeMittelId }
                .filter { (it.bewertungId to it.zielId) !in vorhandeneZellenschluessel }
                .map { it.copy(mittelId = behalteneMittelId) }
            if (neueZellen.isNotEmpty()) bewertungDao.speichereZellen(neueZellen)

            val konkurrenzen = bewertungDao.holeAlleKonkurrenzen()
            val vorhandeneKonkurrenzschluessel = konkurrenzen.mapTo(mutableSetOf()) {
                listOf(it.mittelAId, it.mittelBId).sorted().let { paar -> listOf(it.bewertungId, paar[0], paar[1], it.art.name) }
            }
            val neueKonkurrenzen = konkurrenzen.filter {
                it.mittelAId == zuErsetzendeMittelId || it.mittelBId == zuErsetzendeMittelId
            }.mapNotNull { konkurrenz ->
                val a = if (konkurrenz.mittelAId == zuErsetzendeMittelId) behalteneMittelId else konkurrenz.mittelAId
                val b = if (konkurrenz.mittelBId == zuErsetzendeMittelId) behalteneMittelId else konkurrenz.mittelBId
                if (a == b) null else konkurrenz.copy(mittelAId = a, mittelBId = b)
            }.filter { konkurrenz ->
                val paar = listOf(konkurrenz.mittelAId, konkurrenz.mittelBId).sorted()
                vorhandeneKonkurrenzschluessel.add(listOf(konkurrenz.bewertungId, paar[0], paar[1], konkurrenz.art.name))
            }
            if (neueKonkurrenzen.isNotEmpty()) bewertungDao.speichereKonkurrenzen(neueKonkurrenzen)

            val partner = stackDao.holeAlleAlternationsPartner()
            val partnerNeu = partner.filter { it.partnerMittelId == zuErsetzendeMittelId }
                .map { it.copy(partnerMittelId = behalteneMittelId) }
                .distinct()
            if (partnerNeu.isNotEmpty()) stackDao.speichereAlternationsPartner(partnerNeu)
            katalogDao.loesche(quelle.mittel)
        }
    }

    override suspend fun speichereStack(stack: Stack) {
        require(stack.name.isNotBlank() && stack.name.length <= 60) { "Der Stackname muss 1 bis 60 Zeichen haben." }
        require(stack.zeitpunkt.length <= 120 && stack.einnahmeHinweis.length <= 120) { "Stacktexte duerfen hoechstens 120 Zeichen haben." }
        require(stack.sortierung >= 1) { "Die Stacksortierung muss mindestens 1 sein." }
        stackDao.speichereStack(stack.toEntity())
    }

    override suspend fun loescheStack(stackId: String) {
        stackDao.holeStack(stackId)?.let { stackDao.loescheStack(it) }
    }

    override suspend fun speichereEintrag(eintrag: StackEintrag) {
        validiereEintrag(eintrag)
        database.withTransaction {
            checkNotNull(stackDao.holeStack(eintrag.stackId)) { "Der Stack fehlt." }
            checkNotNull(katalogDao.hole(eintrag.mittelId)) { "Das Mittel fehlt." }
            eintrag.alternationsPartnerMittelIds.forEach { checkNotNull(katalogDao.hole(it)) { "Alternationspartner $it fehlt." } }
            stackDao.speichereEintrag(eintrag.toEntity())
            stackDao.loescheDosen(eintrag.id)
            stackDao.speichereDosen(eintrag.dosen.map { it.toEntity() })
            stackDao.loescheAlternationsPartner(eintrag.id)
            if (eintrag.alternationsPartnerMittelIds.isNotEmpty()) {
                stackDao.speichereAlternationsPartner(eintrag.alternationsPartnerEntities())
            }
        }
    }

    override suspend fun loescheEintrag(eintragId: String) {
        database.withTransaction {
            val eintrag = stackDao.holeEintragEntity(eintragId) ?: return@withTransaction
            bewertungDao.loescheZellenFuerStackMittel(eintrag.stackId, eintrag.mittelId)
            stackDao.loescheEintrag(eintrag)
        }
    }

    override suspend fun setzeEintragAktiv(eintragId: String, aktiv: Boolean): Unit = stackDao.setzeAktiv(eintragId, aktiv)
    override suspend fun setzeOffenenHinweis(eintragId: String, hinweis: String?): Unit = stackDao.setzeOffenenHinweis(eintragId, hinweis)

    override suspend fun speichereZiel(ziel: Ziel) {
        require(ziel.text.isNotBlank() && ziel.text.length <= 200) { "Der Zieltext muss 1 bis 200 Zeichen haben." }
        zielDao.speichereZiel(ziel.toEntity())
    }

    override suspend fun loescheZiel(zielId: String) {
        zielDao.holeZiel(zielId)?.let { zielDao.loescheZiel(it) }
    }

    override suspend fun setzeStackZiel(stackZiel: StackZiel) {
        require(stackZiel.rang >= 1) { "Der Zielrang muss mindestens 1 sein." }
        zielDao.speichereStackZiel(stackZiel.toEntity())
    }

    override suspend fun entferneStackZiel(stackId: String, zielId: String): Unit =
        zielDao.loescheStackZiel(stackId, zielId)

    override suspend fun speichereFrage(frage: EigeneFrage) {
        require(frage.text.isNotBlank() && frage.text.length <= 300) { "Der Fragetext muss 1 bis 300 Zeichen haben." }
        zielDao.speichereFrage(frage.toEntity())
    }

    override suspend fun loescheFrage(frageId: String) {
        zielDao.holeFrage(frageId)?.let { zielDao.loescheFrage(it) }
    }

    override suspend fun setzeSortieransicht(stackId: String, sortieransicht: Sortieransicht) {
        val bisher = stackDao.holeSortieransicht(stackId)
        stackDao.speichereSortieransicht(
            bisher?.copy(sortieransicht = sortieransicht)
                ?: StackSortieransichtEntity(stackId, sortieransicht),
        )
    }

    override suspend fun sortiereStacks(stackIds: List<String>) {
        database.withTransaction {
            val vorhanden = stackDao.holeAlleStacks().map { it.id }
            requirePermutation(vorhanden, stackIds, "Stacks")
            stackIds.forEachIndexed { index, id -> stackDao.setzeStackPosition(id, index + 1) }
        }
    }

    override suspend fun sortiereEintraege(stackId: String, eintragIds: List<String>) {
        database.withTransaction {
            val vorhanden = stackDao.holeEintraege(stackId).map { it.eintrag.id }
            requirePermutation(vorhanden, eintragIds, "Stack-Eintraege")
            eintragIds.forEachIndexed { index, id -> stackDao.setzeEintragPosition(id, index + 1) }
            val bisher = stackDao.holeSortieransicht(stackId)
            stackDao.speichereSortieransicht(
                bisher?.copy(manuelleReihenfolge = true)
                    ?: StackSortieransichtEntity(stackId, Sortieransicht.EINNAHME, manuelleReihenfolge = true),
            )
        }
    }

    override suspend fun sortiereZiele(stackId: String, zielIds: List<String>) {
        database.withTransaction {
            val vorhanden = zielDao.holeStackZiele(stackId).map { it.zielId }
            requirePermutation(vorhanden, zielIds, "Stack-Ziele")
            zielIds.forEachIndexed { index, id -> zielDao.setzeZielRang(stackId, id, index + 1) }
        }
    }

    override suspend fun speichereBewertung(bewertung: BewertungMitDetails) {
        validiereBewertung(bewertung)
        database.withTransaction {
            bewertungDao.speichereBewertung(bewertung.bewertung.toEntity())
            bewertungDao.loescheZellen(bewertung.bewertung.id)
            bewertungDao.loescheKonkurrenzen(bewertung.bewertung.id)
            bewertungDao.loescheAntworten(bewertung.bewertung.id)
            if (bewertung.zellen.isNotEmpty()) bewertungDao.speichereZellen(bewertung.zellen.map { it.toEntity() })
            if (bewertung.konkurrenzen.isNotEmpty()) bewertungDao.speichereKonkurrenzen(bewertung.konkurrenzen.map { it.toEntity() })
            if (bewertung.antworten.isNotEmpty()) bewertungDao.speichereAntworten(bewertung.antworten.map { it.toEntity() })
            bewertungDao.begrenzeHistorie(
                bewertung.bewertung.bereich,
                bewertung.bewertung.stackId,
                HISTORY_LIMIT,
            )
        }
    }

    override suspend fun berechneAmpeln(stackId: String): AmpelErgebnis = database.withTransaction {
        val eintraege = stackDao.holeEintraege(stackId).map { it.toDomain() }
        val stackZiele = zielDao.holeStackZiele(stackId).map { it.toDomain() }
        val bewertung = bewertungDao.holeNeueste(BewertungsBereich.STACK, stackId)?.toDomain()
        val zellen = bewertung?.zellen.orEmpty()
        val aktiveMittel = eintraege.filter { it.aktiv }.mapTo(mutableSetOf()) { it.mittelId }
        val mittelAmpeln = eintraege.associate { eintrag ->
            eintrag.mittelId to if (bewertung == null) Ampel.GRAU else AmpelBerechnung.mittelAmpel(
                eintrag.mittelId,
                eintrag.aktiv,
                stackZiele,
                zellen,
            )
        }
        val zielAmpeln = stackZiele.associate { stackZiel ->
            stackZiel.zielId to if (bewertung == null) Ampel.GRAU else AmpelBerechnung.zielAmpel(
                stackZiel.zielId,
                aktiveMittel,
                zellen,
            )
        }
        AmpelErgebnis(
            mittelAmpeln,
            zielAmpeln,
            AmpelBerechnung.sammelAmpel(zielAmpeln.values, bewertung != null),
        )
    }

    override suspend fun berechnePruefsumme(stackId: String, dosisVariante: DosisVariante): String =
        database.withTransaction { pruefsummenInhalt(stackId, dosisVariante).let(Pruefsumme::berechnen) }

    override suspend fun istBewertungVeraltet(stackId: String, dosisVariante: DosisVariante): Boolean =
        database.withTransaction {
            val letzte = bewertungDao.holeNeueste(BewertungsBereich.STACK, stackId)?.bewertung ?: return@withTransaction false
            letzte.pruefsumme != Pruefsumme.berechnen(pruefsummenInhalt(stackId, dosisVariante))
        }

    override suspend fun exportiere(): String = json.kodieren(
        database.withTransaction { leseBestand() }.toTransfer(jetzt()),
    )

    override suspend fun importiere(json: String, modus: ImportModus) {
        val dokument = this.json.dekodieren(json)
        require(dokument.format == StackLaborJson.EXPORT_FORMAT) { "Nur StackLabor-Exporte koennen hier importiert werden." }
        val sicherung = exportiere()
        rueckfallsicherungStore.schreiben(sicherung)
        val importBestand = dokument.toBestand()
        database.withTransaction {
            when (modus) {
                ImportModus.ERSETZEN -> {
                    loescheGesamtenBestand()
                    schreibeBestand(importBestand)
                }
                ImportModus.DAZU -> schreibeBestand(vereinige(importBestand))
            }
            begrenzeAlleHistorien()
        }
    }

    override suspend fun stelleRueckfallsicherungWiederHer() {
        val sicherung = checkNotNull(rueckfallsicherungStore.lesen()) { "Es gibt keine Rueckfallsicherung." }
        val dokument = json.dekodieren(sicherung)
        database.withTransaction {
            loescheGesamtenBestand()
            schreibeBestand(dokument.toBestand())
            begrenzeAlleHistorien()
        }
    }

    override suspend fun importiereStartbestand(): SeedImportErgebnis {
        val dokument = json.dekodieren(startbestandQuelle.lesen())
        require(dokument.format == StackLaborJson.SEED_FORMAT) { "Das Asset ist kein StackLabor-Startbestand." }
        require(dokument.stacks.size == 6 && dokument.eintraege.size == 72 && dokument.mittel.size == 63) {
            "Startbestand muss exakt 6 Stacks, 72 Eintraege und 63 Mittel enthalten."
        }
        val seed = dokument.toBestand()
        rueckfallsicherungStore.schreiben(exportiere())
        database.withTransaction {
            loescheGesamtenBestand()
            schreibeBestand(seed)
        }
        return SeedImportErgebnis(6, 72, 63)
    }

    override suspend fun initialisiereStartbestandWennLeer(): SeedImportErgebnis? {
        if (bestandDao.stackAnzahl() != 0 || bestandDao.mittelAnzahl() != 0) return null
        return importiereStartbestand()
    }

    override suspend fun ladeStackInhalt(stackId: String): StackInhalt = database.withTransaction {
        val stack = checkNotNull(stackDao.holeStack(stackId)) { "Stack $stackId fehlt." }.toDomain()
        val eintraege = stackDao.holeEintraege(stackId).map { it.toDomain() }
        val mittelNachId = katalogDao.holeAlle().associateBy { it.mittel.id }
        StackInhalt(
            stack = stack,
            mittel = eintraege.map { eintrag ->
                checkNotNull(mittelNachId[eintrag.mittelId]) { "Mittel ${eintrag.mittelId} fehlt." }.toDomain()
            },
            eintraege = eintraege,
            ziele = zielDao.holeStackZiele(stackId).map { it.toDomain() },
            fragen = zielDao.holeFragen(stackId).map { it.toDomain() },
        )
    }

    private suspend fun pruefsummenInhalt(stackId: String, dosisVariante: DosisVariante): StackPruefsummenInhalt {
        val stack = checkNotNull(stackDao.holeStack(stackId)) { "Stack $stackId fehlt." }
        val mittel = katalogDao.holeAlle().associateBy { it.mittel.id }
        val eintraege = stackDao.holeEintraege(stackId).map { relation ->
            val eintrag = relation.toDomain()
            val katalogMittel = checkNotNull(mittel[eintrag.mittelId]) { "Mittel ${eintrag.mittelId} fehlt." }
            PruefsummenEintrag(
                id = eintrag.id,
                mittel = PruefsummenMittel(
                    id = katalogMittel.mittel.id,
                    loeslichkeit = katalogMittel.mittel.loeslichkeit,
                    darreichungsform = katalogMittel.mittel.darreichungsform,
                    komponenten = katalogMittel.komponenten.map {
                        PruefsummenKomponente(it.id, it.name, it.menge, it.einheit)
                    },
                ),
                frequenzTyp = eintrag.frequenzTyp,
                alleNTage = eintrag.alleNTage,
                gruppeId = eintrag.gruppeId,
                zusatztext = eintrag.zusatztext,
                dosen = eintrag.dosen.map {
                    PruefsummenDosis(it.variante, it.stueckzahl, it.mengeJeStueck, it.einheit, it.einheitText)
                },
                alternationsPartnerMittelIds = eintrag.alternationsPartnerMittelIds,
            )
        }
        return StackPruefsummenInhalt(
            zeitpunkt = stack.zeitpunkt,
            einnahmeHinweis = stack.einnahmeHinweis,
            dosisVariante = dosisVariante,
            eintraege = eintraege,
            aktiveZielIds = zielDao.holeStackZiele(stackId).map { it.zielId },
            fragen = zielDao.holeFragen(stackId).map { PruefsummenFrage(it.id, it.text) },
        )
    }

    private suspend fun leseBestand(): DatenbankBestand = DatenbankBestand(
        mittel = katalogDao.holeAlleEntities(),
        komponenten = katalogDao.holeAlleKomponenten(),
        stacks = stackDao.holeAlleStacks(),
        eintraege = stackDao.holeAlleEintragEntities(),
        dosen = stackDao.holeAlleDosen(),
        alternationsPartner = stackDao.holeAlleAlternationsPartner(),
        ziele = zielDao.holeAlleZiele(),
        stackZiele = zielDao.holeAlleStackZiele(),
        fragen = zielDao.holeAlleFragen(),
        bewertungen = bewertungDao.holeAlleBewertungen(),
        zellen = bewertungDao.holeAlleZellen(),
        konkurrenzen = bewertungDao.holeAlleKonkurrenzen(),
        antworten = bewertungDao.holeAlleAntworten(),
        sortieransichten = stackDao.holeAlleSortieransichten(),
    )

    private suspend fun schreibeBestand(bestand: DatenbankBestand) {
        if (bestand.mittel.isNotEmpty()) katalogDao.speichereAlle(bestand.mittel)
        if (bestand.komponenten.isNotEmpty()) katalogDao.speichereKomponenten(bestand.komponenten)
        if (bestand.stacks.isNotEmpty()) stackDao.speichereStacks(bestand.stacks)
        if (bestand.eintraege.isNotEmpty()) stackDao.speichereEintraege(bestand.eintraege)
        if (bestand.dosen.isNotEmpty()) stackDao.speichereDosen(bestand.dosen)
        if (bestand.alternationsPartner.isNotEmpty()) stackDao.speichereAlternationsPartner(bestand.alternationsPartner)
        if (bestand.ziele.isNotEmpty()) zielDao.speichereZiele(bestand.ziele)
        if (bestand.stackZiele.isNotEmpty()) zielDao.speichereStackZiele(bestand.stackZiele)
        if (bestand.fragen.isNotEmpty()) zielDao.speichereFragen(bestand.fragen)
        if (bestand.bewertungen.isNotEmpty()) bewertungDao.speichereBewertungen(bestand.bewertungen)
        if (bestand.zellen.isNotEmpty()) bewertungDao.speichereZellen(bestand.zellen)
        if (bestand.konkurrenzen.isNotEmpty()) bewertungDao.speichereKonkurrenzen(bestand.konkurrenzen)
        if (bestand.antworten.isNotEmpty()) bewertungDao.speichereAntworten(bestand.antworten)
        bestand.sortieransichten.forEach { stackDao.speichereSortieransicht(it) }
    }

    private suspend fun loescheGesamtenBestand() {
        bestandDao.loescheAlleAntworten()
        bestandDao.loescheAlleKonkurrenzen()
        bestandDao.loescheAlleZellen()
        bestandDao.loescheAlleBewertungen()
        bestandDao.loescheAlleFragen()
        bestandDao.loescheAlleStackZiele()
        bestandDao.loescheAlleZiele()
        bestandDao.loescheAlleAlternationsPartner()
        bestandDao.loescheAlleDosen()
        bestandDao.loescheAlleEintraege()
        bestandDao.loescheAlleSortieransichten()
        bestandDao.loescheAlleStacks()
        bestandDao.loescheAlleKomponenten()
        bestandDao.loescheAlleMittel()
    }

    private suspend fun vereinige(import: DatenbankBestand): DatenbankBestand {
        val vorhanden = leseBestand()
        val mittelIds = vorhanden.mittel.mapTo(mutableSetOf()) { it.id }
        val komponentenIds = vorhanden.komponenten.mapTo(mutableSetOf()) { it.id }
        val stackIds = vorhanden.stacks.mapTo(mutableSetOf()) { it.id }
        val eintragIds = vorhanden.eintraege.mapTo(mutableSetOf()) { it.id }
        val zielIds = vorhanden.ziele.mapTo(mutableSetOf()) { it.id }
        val frageIds = vorhanden.fragen.mapTo(mutableSetOf()) { it.id }
        val bewertungIds = vorhanden.bewertungen.mapTo(mutableSetOf()) { it.id }

        val neueMittel = import.mittel.filter { mittelIds.add(it.id) }
        val neueKomponenten = import.komponenten.mapNotNull { komponente ->
            if (komponente.mittelId !in mittelIds) return@mapNotNull null
            val id = freieId(komponente.id, komponentenIds)
            komponente.copy(id = id)
        }

        val stackMap = import.stacks.associate { stack -> stack.id to freieId(stack.id, stackIds) }
        val ersteNeueStackPosition = (vorhanden.stacks.maxOfOrNull { it.sortierung } ?: 0) + 1
        val neueStacks = import.stacks.sortedWith(compareBy({ it.sortierung }, { it.id })).mapIndexed { index, stack ->
            stack.copy(id = stackMap.getValue(stack.id), sortierung = ersteNeueStackPosition + index)
        }
        val eintragMap = import.eintraege.associate { eintrag -> eintrag.id to freieId(eintrag.id, eintragIds) }
        val neueEintraege = import.eintraege.map {
            it.copy(id = eintragMap.getValue(it.id), stackId = stackMap.getValue(it.stackId))
        }
        val neueDosen = import.dosen.map { it.copy(stackEintragId = eintragMap.getValue(it.stackEintragId)) }
        val neuePartner = import.alternationsPartner.map { it.copy(stackEintragId = eintragMap.getValue(it.stackEintragId)) }

        val zielNachText = vorhanden.ziele.associateBy { it.text }
        val zielMap = import.ziele.associate { ziel ->
            ziel.id to (zielNachText[ziel.text]?.id ?: freieId(ziel.id, zielIds))
        }
        val neueZiele = import.ziele.filter { zielNachText[it.text] == null }.map { it.copy(id = zielMap.getValue(it.id)) }
        val neueStackZiele = import.stackZiele.map {
            it.copy(stackId = stackMap.getValue(it.stackId), zielId = zielMap.getValue(it.zielId))
        }

        val frageMap = import.fragen.associate { frage -> frage.id to freieId(frage.id, frageIds) }
        val neueFragen = import.fragen.map {
            it.copy(id = frageMap.getValue(it.id), stackId = stackMap.getValue(it.stackId))
        }
        val bewertungMap = import.bewertungen.associate { bewertung ->
            bewertung.id to freieId(bewertung.id, bewertungIds)
        }
        val neueBewertungen = import.bewertungen.map { bewertung ->
            bewertung.copy(
                id = bewertungMap.getValue(bewertung.id),
                stackId = bewertung.stackId?.let(stackMap::getValue),
            )
        }
        val neueZellen = import.zellen.map {
            it.copy(bewertungId = bewertungMap.getValue(it.bewertungId), zielId = zielMap.getValue(it.zielId))
        }
        val neueKonkurrenzen = import.konkurrenzen.map {
            it.copy(bewertungId = bewertungMap.getValue(it.bewertungId))
        }
        val neueAntworten = import.antworten.map {
            it.copy(bewertungId = bewertungMap.getValue(it.bewertungId), frageId = frageMap.getValue(it.frageId))
        }
        val neueSortieransichten = import.sortieransichten.map {
            it.copy(stackId = stackMap.getValue(it.stackId))
        }
        return DatenbankBestand(
            mittel = neueMittel,
            komponenten = neueKomponenten,
            stacks = neueStacks,
            eintraege = neueEintraege,
            dosen = neueDosen,
            alternationsPartner = neuePartner,
            ziele = neueZiele,
            stackZiele = neueStackZiele,
            fragen = neueFragen,
            bewertungen = neueBewertungen,
            zellen = neueZellen,
            konkurrenzen = neueKonkurrenzen,
            antworten = neueAntworten,
            sortieransichten = neueSortieransichten,
        )
    }

    private suspend fun begrenzeAlleHistorien() {
        bewertungDao.holeAlleBewertungen()
            .map { it.bereich to it.stackId }
            .distinct()
            .forEach { (bereich, stackId) -> bewertungDao.begrenzeHistorie(bereich, stackId, HISTORY_LIMIT) }
    }

    private fun freieId(vorschlag: String, belegt: MutableSet<String>): String {
        if (belegt.add(vorschlag)) return vorschlag
        var id: String
        do id = neueId() while (!belegt.add(id))
        return id
    }

    private fun validiereMittel(mittel: Mittel) {
        require(mittel.id.isNotBlank()) { "Mittel-ID fehlt." }
        require(mittel.name.isNotBlank() && mittel.name.length <= 80) { "Der Mittelname muss 1 bis 80 Zeichen haben." }
        require(mittel.hersteller.length <= 80 && mittel.beistoffe.length <= 200) { "Mittel-Stammdaten sind zu lang." }
        require(mittel.wirkstoffkomponenten.all { it.mittelId == mittel.id && it.name.isNotBlank() }) { "Ungueltige Wirkstoffkomponente." }
        require(mittel.wirkstoffkomponenten.all { it.menge == null || it.menge.signum() > 0 }) { "Wirkstoffmengen muessen positiv sein." }
    }

    private fun validiereEintrag(eintrag: StackEintrag) {
        require(eintrag.id.isNotBlank() && eintrag.stackId.isNotBlank() && eintrag.mittelId.isNotBlank()) { "Eintrags-IDs fehlen." }
        require(eintrag.reihenfolge >= 1) { "Die Eintragsposition muss mindestens 1 sein." }
        require(eintrag.zusatztext.length <= 300) { "Der Zusatztext darf hoechstens 300 Zeichen haben." }
        require(eintrag.frequenzTyp.name != "ALLE_N_TAGE" || (eintrag.alleNTage ?: 0) >= 2) { "Das Frequenzintervall muss mindestens 2 Tage sein." }
        require(eintrag.dosen.count { it.variante == DosisVariante.FREI } == 1) { "Genau eine freie Dosis ist erforderlich." }
        require(eintrag.dosen.distinctBy { it.variante }.size == eintrag.dosen.size) { "Dosisvarianten duerfen nicht doppelt sein." }
        require(eintrag.dosen.all { it.stackEintragId == eintrag.id && it.stueckzahl.signum() > 0 }) { "Ungueltige Stueckzahl." }
        require(eintrag.dosen.all { (it.mengeJeStueck == null) == (it.einheit == null) }) { "Menge und Einheit muessen gemeinsam gesetzt sein." }
        require(eintrag.dosen.all { it.mengeJeStueck == null || it.mengeJeStueck.signum() > 0 }) { "Dosen muessen positiv sein." }
        require(eintrag.mittelId !in eintrag.alternationsPartnerMittelIds) { "Ein Mittel kann nicht mit sich selbst alternieren." }
    }

    private fun validiereBewertung(details: BewertungMitDetails) {
        val bewertung = details.bewertung
        require((bewertung.bereich == BewertungsBereich.STACK) == (bewertung.stackId != null)) { "Bewertungsbereich und Stack passen nicht." }
        require(details.zellen.all { it.bewertungId == bewertung.id && it.staerke in 1..3 && it.grund.length <= 140 }) { "Ungueltige Bewertungszelle." }
        require(details.konkurrenzen.all { it.bewertungId == bewertung.id && it.mittelAId != it.mittelBId && it.schwere in 1..3 }) { "Ungueltige Konkurrenz." }
        require(details.antworten.all { it.bewertungId == bewertung.id }) { "Ungueltige Frage-Antwort." }
    }

    private fun requirePermutation(vorhanden: List<String>, neu: List<String>, bezeichnung: String) {
        require(neu.size == neu.toSet().size && vorhanden.toSet() == neu.toSet()) {
            "$bezeichnung muessen vollstaendig und ohne Duplikate sortiert werden."
        }
    }

    companion object {
        const val HISTORY_LIMIT: Int = 5
    }
}
