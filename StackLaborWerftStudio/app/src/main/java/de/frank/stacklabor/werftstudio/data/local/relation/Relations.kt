package de.frank.stacklabor.werftstudio.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import de.frank.stacklabor.werftstudio.data.local.entity.AlternationsPartnerEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungszelleEntity
import de.frank.stacklabor.werftstudio.data.local.entity.FrageAntwortEntity
import de.frank.stacklabor.werftstudio.data.local.entity.KonkurrenzEntity
import de.frank.stacklabor.werftstudio.data.local.entity.MittelEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragDosisEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragEntity
import de.frank.stacklabor.werftstudio.data.local.entity.WirkstoffkomponenteEntity

data class MittelMitKomponenten(
    @Embedded val mittel: MittelEntity,
    @Relation(parentColumn = "id", entityColumn = "mittelId")
    val komponenten: List<WirkstoffkomponenteEntity>,
)

data class StackEintragMitDosenUndPartnern(
    @Embedded val eintrag: StackEintragEntity,
    @Relation(parentColumn = "id", entityColumn = "stackEintragId")
    val dosen: List<StackEintragDosisEntity>,
    @Relation(parentColumn = "id", entityColumn = "stackEintragId")
    val alternationsPartner: List<AlternationsPartnerEntity>,
)

data class BewertungMitDetailsEntity(
    @Embedded val bewertung: BewertungEntity,
    @Relation(parentColumn = "id", entityColumn = "bewertungId")
    val zellen: List<BewertungszelleEntity>,
    @Relation(parentColumn = "id", entityColumn = "bewertungId")
    val konkurrenzen: List<KonkurrenzEntity>,
    @Relation(parentColumn = "id", entityColumn = "bewertungId")
    val antworten: List<FrageAntwortEntity>,
)
