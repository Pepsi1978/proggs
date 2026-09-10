package de.frank.stacklabor.werftstudio.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import de.frank.stacklabor.werftstudio.domain.model.BewertungsBereich
import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.KonkurrenzArt
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Sortieransicht
import de.frank.stacklabor.werftstudio.domain.model.Wirkung
import java.math.BigDecimal

@Entity(tableName = "mittel")
data class MittelEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val loeslichkeit: Loeslichkeit,
    val darreichungsform: Darreichungsform,
    val hersteller: String,
    val durchfallrisiko: Boolean,
    val beistoffe: String,
    val darreichungsformText: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val loeslichkeitKiErmittelt: Boolean = false,
)

@Entity(
    tableName = "wirkstoffkomponente",
    foreignKeys = [
        ForeignKey(
            entity = MittelEntity::class,
            parentColumns = ["id"],
            childColumns = ["mittelId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mittelId")],
)
data class WirkstoffkomponenteEntity(
    @androidx.room.PrimaryKey val id: String,
    val mittelId: String,
    val name: String,
    val menge: BigDecimal?,
    val einheit: Einheit?,
)

@Entity(tableName = "stack")
data class StackEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val zeitpunkt: String,
    val einnahmeHinweis: String,
    val sortierung: Int,
    @androidx.room.ColumnInfo(defaultValue = "0") val gesperrt: Boolean = false,
)

@Entity(
    tableName = "stack_eintrag",
    foreignKeys = [
        ForeignKey(
            entity = StackEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MittelEntity::class,
            parentColumns = ["id"],
            childColumns = ["mittelId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("stackId"),
        Index("mittelId"),
        Index(value = ["stackId", "mittelId"], unique = true),
        Index(value = ["stackId", "reihenfolge"]),
        Index("gruppeId"),
    ],
)
data class StackEintragEntity(
    @androidx.room.PrimaryKey val id: String,
    val stackId: String,
    val mittelId: String,
    val frequenzTyp: FrequenzTyp,
    val alleNTage: Int?,
    val aktiv: Boolean,
    val reihenfolge: Int,
    val gruppeId: String?,
    val zusatztext: String,
    val offenerHinweis: String?,
    val frequenzText: String? = null,
)

@Entity(
    tableName = "stack_eintrag_dosis",
    primaryKeys = ["stackEintragId", "variante"],
    foreignKeys = [
        ForeignKey(
            entity = StackEintragEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackEintragId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stackEintragId")],
)
data class StackEintragDosisEntity(
    val stackEintragId: String,
    val variante: DosisVariante,
    val stueckzahl: BigDecimal,
    val mengeJeStueck: BigDecimal?,
    val einheit: Einheit?,
    val einheitText: String? = null,
)

@Entity(
    tableName = "alternations_partner",
    primaryKeys = ["stackEintragId", "partnerMittelId"],
    foreignKeys = [
        ForeignKey(
            entity = StackEintragEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackEintragId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MittelEntity::class,
            parentColumns = ["id"],
            childColumns = ["partnerMittelId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stackEintragId"), Index("partnerMittelId")],
)
data class AlternationsPartnerEntity(
    val stackEintragId: String,
    val partnerMittelId: String,
)

@Entity(tableName = "ziel", indices = [Index(value = ["text"], unique = true)])
data class ZielEntity(
    @androidx.room.PrimaryKey val id: String,
    val text: String,
)

@Entity(
    tableName = "stack_ziel",
    primaryKeys = ["stackId", "zielId"],
    foreignKeys = [
        ForeignKey(
            entity = StackEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ZielEntity::class,
            parentColumns = ["id"],
            childColumns = ["zielId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stackId"), Index("zielId"), Index(value = ["stackId", "rang"])],
)
data class StackZielEntity(
    val stackId: String,
    val zielId: String,
    val rang: Int,
)

@Entity(
    tableName = "eigene_frage",
    foreignKeys = [
        ForeignKey(
            entity = StackEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stackId")],
)
data class EigeneFrageEntity(
    @androidx.room.PrimaryKey val id: String,
    val stackId: String,
    val text: String,
)

@Entity(
    tableName = "bewertung",
    foreignKeys = [
        ForeignKey(
            entity = StackEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stackId"), Index(value = ["bereich", "stackId", "zeitpunktEpochMillis"])],
)
data class BewertungEntity(
    @androidx.room.PrimaryKey val id: String,
    val bereich: BewertungsBereich,
    val stackId: String?,
    val zeitpunktEpochMillis: Long,
    val modell: String,
    val denkstufe: String,
    val pruefsumme: String,
    val gesamt: String,
    val hinweise: List<String>,
)

@Entity(
    tableName = "bewertungszelle",
    primaryKeys = ["bewertungId", "mittelId", "zielId"],
    foreignKeys = [
        ForeignKey(
            entity = BewertungEntity::class,
            parentColumns = ["id"],
            childColumns = ["bewertungId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MittelEntity::class,
            parentColumns = ["id"],
            childColumns = ["mittelId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ZielEntity::class,
            parentColumns = ["id"],
            childColumns = ["zielId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bewertungId"), Index("mittelId"), Index("zielId")],
)
data class BewertungszelleEntity(
    val bewertungId: String,
    val mittelId: String,
    val zielId: String,
    val wirkung: Wirkung,
    val staerke: Int,
    val grund: String,
)

@Entity(
    tableName = "konkurrenz",
    primaryKeys = ["bewertungId", "mittelAId", "mittelBId", "art"],
    foreignKeys = [
        ForeignKey(
            entity = BewertungEntity::class,
            parentColumns = ["id"],
            childColumns = ["bewertungId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MittelEntity::class,
            parentColumns = ["id"],
            childColumns = ["mittelAId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MittelEntity::class,
            parentColumns = ["id"],
            childColumns = ["mittelBId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bewertungId"), Index("mittelAId"), Index("mittelBId")],
)
data class KonkurrenzEntity(
    val bewertungId: String,
    val mittelAId: String,
    val mittelBId: String,
    val art: KonkurrenzArt,
    val schwere: Int,
    val grund: String,
)

@Entity(
    tableName = "frage_antwort",
    primaryKeys = ["bewertungId", "frageId"],
    foreignKeys = [
        ForeignKey(
            entity = BewertungEntity::class,
            parentColumns = ["id"],
            childColumns = ["bewertungId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EigeneFrageEntity::class,
            parentColumns = ["id"],
            childColumns = ["frageId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bewertungId"), Index("frageId")],
)
data class FrageAntwortEntity(
    val bewertungId: String,
    val frageId: String,
    val text: String,
)

@Entity(
    tableName = "stack_sortieransicht",
    foreignKeys = [
        ForeignKey(
            entity = StackEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class StackSortieransichtEntity(
    @androidx.room.PrimaryKey val stackId: String,
    val sortieransicht: Sortieransicht,
    @androidx.room.ColumnInfo(defaultValue = "0") val manuelleReihenfolge: Boolean = false,
)
