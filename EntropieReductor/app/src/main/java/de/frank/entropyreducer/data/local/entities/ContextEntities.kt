package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.ShiftCode

@Entity(tableName = "calendar_cache")
data class CalendarDayEntity(
    @PrimaryKey val date: String,            // YYYY-MM-DD
    val shiftCode: ShiftCode,
    val rawCalendarText: String,
    val workWindowStart: String?,            // HH:mm
    val workWindowEnd: String?,
    val sleepWindowStart: String?,
    val sleepWindowEnd: String?,
    val availableMinutesEstimate: Int,
    val syncedAt: Long,
)

/** Von der KI vorgeschlagener (und vom Nutzer freigegebener) automatischer Trigger. */
@Entity(tableName = "ki_triggers")
data class KiTriggerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val condition: String,                   // strukturierte Bedingung (JSON oder DSL)
    val proposedAction: String,
    val isActive: Boolean,
    val createdAt: Long,
    val proposedAt: Long,
    val approvedAt: Long?,
    val triggerCount: Int,
    val lastTriggeredAt: Long?,
)
