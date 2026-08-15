package de.frank.stacklabor.werftstudio.data.repository

import de.frank.stacklabor.werftstudio.data.local.entity.AlternationsPartnerEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungszelleEntity
import de.frank.stacklabor.werftstudio.data.local.entity.EigeneFrageEntity
import de.frank.stacklabor.werftstudio.data.local.entity.FrageAntwortEntity
import de.frank.stacklabor.werftstudio.data.local.entity.KonkurrenzEntity
import de.frank.stacklabor.werftstudio.data.local.entity.MittelEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragDosisEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackSortieransichtEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackZielEntity
import de.frank.stacklabor.werftstudio.data.local.entity.WirkstoffkomponenteEntity
import de.frank.stacklabor.werftstudio.data.local.entity.ZielEntity
import de.frank.stacklabor.werftstudio.data.transfer.AlternationsPartnerTransfer
import de.frank.stacklabor.werftstudio.data.transfer.AntwortTransfer
import de.frank.stacklabor.werftstudio.data.transfer.BewertungTransfer
import de.frank.stacklabor.werftstudio.data.transfer.BewertungszelleTransfer
import de.frank.stacklabor.werftstudio.data.transfer.DosisTransfer
import de.frank.stacklabor.werftstudio.data.transfer.FrageTransfer
import de.frank.stacklabor.werftstudio.data.transfer.KonkurrenzTransfer
import de.frank.stacklabor.werftstudio.data.transfer.MittelTransfer
import de.frank.stacklabor.werftstudio.data.transfer.SortieransichtTransfer
import de.frank.stacklabor.werftstudio.data.transfer.StackEintragTransfer
import de.frank.stacklabor.werftstudio.data.transfer.StackLaborJson
import de.frank.stacklabor.werftstudio.data.transfer.StackTransfer
import de.frank.stacklabor.werftstudio.data.transfer.StackZielTransfer
import de.frank.stacklabor.werftstudio.data.transfer.TransferDokument
import de.frank.stacklabor.werftstudio.data.transfer.WirkstoffkomponenteTransfer
import de.frank.stacklabor.werftstudio.data.transfer.ZielTransfer
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import java.math.BigDecimal

internal data class DatenbankBestand(
    val mittel: List<MittelEntity> = emptyList(),
    val komponenten: List<WirkstoffkomponenteEntity> = emptyList(),
    val stacks: List<StackEntity> = emptyList(),
    val eintraege: List<StackEintragEntity> = emptyList(),
    val dosen: List<StackEintragDosisEntity> = emptyList(),
    val alternationsPartner: List<AlternationsPartnerEntity> = emptyList(),
    val ziele: List<ZielEntity> = emptyList(),
    val stackZiele: List<StackZielEntity> = emptyList(),
    val fragen: List<EigeneFrageEntity> = emptyList(),
    val bewertungen: List<BewertungEntity> = emptyList(),
    val zellen: List<BewertungszelleEntity> = emptyList(),
    val konkurrenzen: List<KonkurrenzEntity> = emptyList(),
    val antworten: List<FrageAntwortEntity> = emptyList(),
    val sortieransichten: List<StackSortieransichtEntity> = emptyList(),
)

internal fun DatenbankBestand.toTransfer(exportiertAm: Long): TransferDokument = TransferDokument(
    format = StackLaborJson.EXPORT_FORMAT,
    schemaVersion = StackLaborJson.AKTUELLE_SCHEMA_VERSION,
    exportiertAmEpochMillis = exportiertAm,
    mittel = mittel.map {
        MittelTransfer(it.id, it.name, it.loeslichkeit.name, it.darreichungsform.name, it.hersteller, it.durchfallrisiko, it.beistoffe, it.darreichungsformText, it.loeslichkeitKiErmittelt)
    },
    wirkstoffkomponenten = komponenten.map { WirkstoffkomponenteTransfer(it.id, it.mittelId, it.name, it.menge?.toPlainString(), it.einheit?.name) },
    stacks = stacks.map { StackTransfer(it.id, it.name, it.zeitpunkt, it.einnahmeHinweis, it.sortierung) },
    eintraege = eintraege.map {
        StackEintragTransfer(it.id, it.stackId, it.mittelId, it.frequenzTyp.name, it.alleNTage, it.aktiv, it.reihenfolge, it.gruppeId, it.zusatztext, it.offenerHinweis, it.frequenzText)
    },
    dosen = dosen.map { DosisTransfer(it.stackEintragId, it.variante.name, it.stueckzahl.toPlainString(), it.mengeJeStueck?.toPlainString(), it.einheit?.name, it.einheitText) },
    alternationsPartner = alternationsPartner.map { AlternationsPartnerTransfer(it.stackEintragId, it.partnerMittelId) },
    ziele = ziele.map { ZielTransfer(it.id, it.text) },
    stackZiele = stackZiele.map { StackZielTransfer(it.stackId, it.zielId, it.rang) },
    fragen = fragen.map { FrageTransfer(it.id, it.stackId, it.text) },
    bewertungen = bewertungen.map { BewertungTransfer(it.id, it.bereich.name, it.stackId, it.zeitpunktEpochMillis, it.modell, it.denkstufe, it.pruefsumme, it.gesamt, it.hinweise) },
    bewertungszellen = zellen.map { BewertungszelleTransfer(it.bewertungId, it.mittelId, it.zielId, it.wirkung.name, it.staerke, it.grund) },
    konkurrenzen = konkurrenzen.map { KonkurrenzTransfer(it.bewertungId, it.mittelAId, it.mittelBId, it.art.name, it.schwere, it.grund) },
    antworten = antworten.map { AntwortTransfer(it.bewertungId, it.frageId, it.text) },
    sortieransichten = sortieransichten.map { SortieransichtTransfer(it.stackId, it.sortieransicht.name, it.manuelleReihenfolge) },
)

internal fun TransferDokument.toBestand(): DatenbankBestand = DatenbankBestand(
    mittel = mittel.map {
        MittelEntity(it.id, it.name, enumValue(it.loeslichkeit), enumValue(it.darreichungsform), it.hersteller, it.durchfallrisiko, it.beistoffe, it.darreichungsformText, it.loeslichkeitKiErmittelt)
    },
    komponenten = wirkstoffkomponenten.map {
        WirkstoffkomponenteEntity(it.id, it.mittelId, it.name, it.menge?.let(::BigDecimal), it.einheit?.let { name -> enumValue<Einheit>(name) })
    },
    stacks = stacks.map { StackEntity(it.id, it.name, it.zeitpunkt, it.einnahmeHinweis, it.sortierung) },
    eintraege = eintraege.map {
        StackEintragEntity(it.id, it.stackId, it.mittelId, enumValue(it.frequenzTyp), it.alleNTage, it.aktiv, it.reihenfolge, it.gruppeId, it.zusatztext, it.offenerHinweis, it.frequenzText)
    },
    dosen = dosen.map {
        StackEintragDosisEntity(it.stackEintragId, enumValue(it.variante), BigDecimal(it.stueckzahl), it.mengeJeStueck?.let(::BigDecimal), it.einheit?.let { name -> enumValue<Einheit>(name) }, it.einheitText)
    },
    alternationsPartner = alternationsPartner.map { AlternationsPartnerEntity(it.stackEintragId, it.partnerMittelId) },
    ziele = ziele.map { ZielEntity(it.id, it.text) },
    stackZiele = stackZiele.map { StackZielEntity(it.stackId, it.zielId, it.rang) },
    fragen = fragen.map { EigeneFrageEntity(it.id, it.stackId, it.text) },
    bewertungen = bewertungen.map {
        BewertungEntity(it.id, enumValue(it.bereich), it.stackId, it.zeitpunktEpochMillis, it.modell, it.denkstufe, it.pruefsumme, it.gesamt, it.hinweise)
    },
    zellen = bewertungszellen.map {
        BewertungszelleEntity(it.bewertungId, it.mittelId, it.zielId, enumValue(it.wirkung), it.staerke, it.grund)
    },
    konkurrenzen = konkurrenzen.map {
        KonkurrenzEntity(it.bewertungId, it.mittelAId, it.mittelBId, enumValue(it.art), it.schwere, it.grund)
    },
    antworten = antworten.map { FrageAntwortEntity(it.bewertungId, it.frageId, it.text) },
    sortieransichten = sortieransichten.map { StackSortieransichtEntity(it.stackId, enumValue(it.ansicht), it.manuelleReihenfolge) },
)

private inline fun <reified T : Enum<T>> enumValue(name: String): T = enumValueOf(name)
