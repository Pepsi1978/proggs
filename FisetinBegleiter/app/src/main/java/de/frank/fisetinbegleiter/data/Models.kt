package de.frank.fisetinbegleiter.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class CureStatus {
    GEPLANT,
    AKTIV,
    ABGESCHLOSSEN,
}

enum class StackCategory {
    HARTE_SPERRE,
    GRENZFALL,
    UNPROBLEMATISCH,
}

enum class IngredientPhase {
    DRINK,
    SPERMIDIN,
}

@Entity(tableName = "protocol_templates")
data class ProtocolTemplateEntity(
    @PrimaryKey val id: Int = 1,
    val standardDurationDays: Int = 3,
    val antioxidantBlockMinutes: Int = 240,
    val mealDeadlineMinutes: Int = 30,
    val mealWarningMinutes: Int = 25,
    val spermidinStartMinutes: Int = 180,
    val spermidinReminderMinutes: Int = 225,
    val preferredStartMinuteOfDay: Int = 480,
    val remindersEnabled: Boolean = true,
)

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phase: IngredientPhase,
    val name: String,
    val amount: String,
    val note: String,
    val optional: Boolean = false,
    val sortOrder: Int,
)

@Entity(tableName = "stack_items")
data class StackItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val usualDose: String,
    val category: StackCategory,
    val isMedication: Boolean = false,
    val note: String = "",
    val sortOrder: Int,
)

@Entity(tableName = "cures")
data class CureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochDay: Long,
    val plannedDurationDays: Int,
    val status: CureStatus,
    val carrierLiquid: String,
    val fatCarrier: String,
    val preferredStartMinuteOfDay: Int,
    val note: String = "",
    val completedAt: Long? = null,
)

@Entity(
    tableName = "cure_days",
    foreignKeys = [
        ForeignKey(
            entity = CureEntity::class,
            parentColumns = ["id"],
            childColumns = ["cureId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cureId")],
)
data class CureDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cureId: Long,
    val dayNumber: Int,
    val plannedEpochDay: Long,
    val t0: Long? = null,
    val drinkDoneAt: Long? = null,
    val mealDoneAt: Long? = null,
    val spermidinDoneAt: Long? = null,
    val blockEndedAt: Long? = null,
    val completedAt: Long? = null,
)

data class CureWithDays(
    @androidx.room.Embedded val cure: CureEntity,
    @Relation(parentColumn = "id", entityColumn = "cureId")
    val days: List<CureDayEntity>,
)

enum class AppCureState {
    KEINE_KUR,
    KUR_GEPLANT,
    TAG_LAEUFT_VOR_DRINK,
    SPERRFENSTER_AKTIV,
    SPERRFENSTER_VORBEI,
    TAG_ABGESCHLOSSEN,
    KUR_ABGESCHLOSSEN,
}
