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
import de.frank.stacklabor.werftstudio.data.local.entity.StackZielEntity
import de.frank.stacklabor.werftstudio.data.local.entity.WirkstoffkomponenteEntity
import de.frank.stacklabor.werftstudio.data.local.entity.ZielEntity
import de.frank.stacklabor.werftstudio.data.local.relation.BewertungMitDetailsEntity
import de.frank.stacklabor.werftstudio.data.local.relation.MittelMitKomponenten
import de.frank.stacklabor.werftstudio.data.local.relation.StackEintragMitDosenUndPartnern
import de.frank.stacklabor.werftstudio.domain.model.Bewertung
import de.frank.stacklabor.werftstudio.domain.model.BewertungMitDetails
import de.frank.stacklabor.werftstudio.domain.model.Bewertungszelle
import de.frank.stacklabor.werftstudio.domain.model.EigeneFrage
import de.frank.stacklabor.werftstudio.domain.model.FrageAntwort
import de.frank.stacklabor.werftstudio.domain.model.Konkurrenz
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackEintragDosis
import de.frank.stacklabor.werftstudio.domain.model.StackZiel
import de.frank.stacklabor.werftstudio.domain.model.Wirkstoffkomponente
import de.frank.stacklabor.werftstudio.domain.model.Ziel

internal fun MittelMitKomponenten.toDomain(): Mittel = Mittel(
    id = mittel.id,
    name = mittel.name,
    loeslichkeit = mittel.loeslichkeit,
    darreichungsform = mittel.darreichungsform,
    darreichungsformText = mittel.darreichungsformText,
    hersteller = mittel.hersteller,
    durchfallrisiko = mittel.durchfallrisiko,
    beistoffe = mittel.beistoffe,
    wirkstoffkomponenten = komponenten.map(WirkstoffkomponenteEntity::toDomain),
    loeslichkeitKiErmittelt = mittel.loeslichkeitKiErmittelt,
)

internal fun Mittel.toEntity(): MittelEntity = MittelEntity(
    id = id,
    name = name,
    loeslichkeit = loeslichkeit,
    darreichungsform = darreichungsform,
    darreichungsformText = darreichungsformText,
    hersteller = hersteller,
    durchfallrisiko = durchfallrisiko,
    beistoffe = beistoffe,
    loeslichkeitKiErmittelt = loeslichkeitKiErmittelt,
)

internal fun WirkstoffkomponenteEntity.toDomain(): Wirkstoffkomponente = Wirkstoffkomponente(
    id = id,
    mittelId = mittelId,
    name = name,
    menge = menge,
    einheit = einheit,
)

internal fun Wirkstoffkomponente.toEntity(): WirkstoffkomponenteEntity = WirkstoffkomponenteEntity(
    id = id,
    mittelId = mittelId,
    name = name,
    menge = menge,
    einheit = einheit,
)

internal fun StackEntity.toDomain(): Stack = Stack(id, name, zeitpunkt, einnahmeHinweis, sortierung, gesperrt)
internal fun Stack.toEntity(): StackEntity = StackEntity(id, name, zeitpunkt, einnahmeHinweis, sortierung, gesperrt)

internal fun StackEintragMitDosenUndPartnern.toDomain(): StackEintrag = StackEintrag(
    id = eintrag.id,
    stackId = eintrag.stackId,
    mittelId = eintrag.mittelId,
    frequenzTyp = eintrag.frequenzTyp,
    alleNTage = eintrag.alleNTage,
    frequenzText = eintrag.frequenzText,
    aktiv = eintrag.aktiv,
    reihenfolge = eintrag.reihenfolge,
    gruppeId = eintrag.gruppeId,
    zusatztext = eintrag.zusatztext,
    offenerHinweis = eintrag.offenerHinweis,
    dosen = dosen.map(StackEintragDosisEntity::toDomain),
    alternationsPartnerMittelIds = alternationsPartner.map { it.partnerMittelId },
)

internal fun StackEintrag.toEntity(): StackEintragEntity = StackEintragEntity(
    id = id,
    stackId = stackId,
    mittelId = mittelId,
    frequenzTyp = frequenzTyp,
    alleNTage = alleNTage,
    frequenzText = frequenzText,
    aktiv = aktiv,
    reihenfolge = reihenfolge,
    gruppeId = gruppeId,
    zusatztext = zusatztext,
    offenerHinweis = offenerHinweis,
)

internal fun StackEintragDosisEntity.toDomain(): StackEintragDosis = StackEintragDosis(
    stackEintragId, variante, stueckzahl, mengeJeStueck, einheit, einheitText,
)

internal fun StackEintragDosis.toEntity(): StackEintragDosisEntity = StackEintragDosisEntity(
    stackEintragId, variante, stueckzahl, mengeJeStueck, einheit, einheitText,
)

internal fun StackEintrag.alternationsPartnerEntities(): List<AlternationsPartnerEntity> =
    alternationsPartnerMittelIds.map { AlternationsPartnerEntity(id, it) }

internal fun ZielEntity.toDomain(): Ziel = Ziel(id, text)
internal fun Ziel.toEntity(): ZielEntity = ZielEntity(id, text)
internal fun StackZielEntity.toDomain(): StackZiel = StackZiel(stackId, zielId, rang)
internal fun StackZiel.toEntity(): StackZielEntity = StackZielEntity(stackId, zielId, rang)
internal fun EigeneFrageEntity.toDomain(): EigeneFrage = EigeneFrage(id, stackId, text)
internal fun EigeneFrage.toEntity(): EigeneFrageEntity = EigeneFrageEntity(id, stackId, text)

internal fun BewertungEntity.toDomain(): Bewertung = Bewertung(
    id, bereich, stackId, zeitpunktEpochMillis, modell, denkstufe, pruefsumme, gesamt, hinweise,
)

internal fun Bewertung.toEntity(): BewertungEntity = BewertungEntity(
    id, bereich, stackId, zeitpunktEpochMillis, modell, denkstufe, pruefsumme, gesamt, hinweise,
)

internal fun BewertungszelleEntity.toDomain(): Bewertungszelle = Bewertungszelle(
    bewertungId, mittelId, zielId, wirkung, staerke, grund,
)

internal fun Bewertungszelle.toEntity(): BewertungszelleEntity = BewertungszelleEntity(
    bewertungId, mittelId, zielId, wirkung, staerke, grund,
)

internal fun KonkurrenzEntity.toDomain(): Konkurrenz = Konkurrenz(
    bewertungId, mittelAId, mittelBId, art, schwere, grund,
)

internal fun Konkurrenz.toEntity(): KonkurrenzEntity = KonkurrenzEntity(
    bewertungId, mittelAId, mittelBId, art, schwere, grund,
)

internal fun FrageAntwortEntity.toDomain(): FrageAntwort = FrageAntwort(bewertungId, frageId, text)
internal fun FrageAntwort.toEntity(): FrageAntwortEntity = FrageAntwortEntity(bewertungId, frageId, text)

internal fun BewertungMitDetailsEntity.toDomain(): BewertungMitDetails = BewertungMitDetails(
    bewertung = bewertung.toDomain(),
    zellen = zellen.map(BewertungszelleEntity::toDomain),
    konkurrenzen = konkurrenzen.map(KonkurrenzEntity::toDomain),
    antworten = antworten.map(FrageAntwortEntity::toDomain),
)
