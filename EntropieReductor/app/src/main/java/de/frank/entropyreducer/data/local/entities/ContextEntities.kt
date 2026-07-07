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

/**
 * Einzelnes Google-Calendar-Event (Stufe 4 Erweiterung).
 *
 * Wird ZUSAETZLICH zum Schichtcode aus dem Sync-Worker befuellt — d.h. der
 * ExperimentCalendarScreen kann pro Tag sowohl die Schicht (als großer Marker)
 * als auch alle anderen Termine (Arzttermine, Geburtstage, Erinnerungen, ...)
 * anzeigen. Für die KI bedeutet das: sie kann auf "morgen ist Urlaub" oder
 * "Freitag steht ein Arzttermin an" reagieren.
 *
 * Gespeichert wird die Event-ID aus Google damit ein Re-Sync sauberes Upsert
 * macht. Bei jedem Sync werden ALTE Events des Sync-Fensters zuerst geloescht,
 * dann die neuen geschrieben — so bleiben geloeschte Google-Events nicht haengen.
 */
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,              // Google-Event-ID
    val date: String,                        // YYYY-MM-DD (Start-Datum)
    val summary: String,
    val description: String?,
    val location: String?,
    val startMs: Long,                       // 0 wenn allDay
    val endMs: Long,                         // 0 wenn allDay
    val allDay: Boolean,
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
