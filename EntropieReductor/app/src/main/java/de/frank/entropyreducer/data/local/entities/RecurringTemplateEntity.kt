package de.frank.entropyreducer.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.TimeBucket

/**
 * Vorlage fuer wiederkehrende Aufgaben (Sprint 2, Frank-Wunsch 2026-05-22).
 *
 * Speichert eine RFC-5545-RRULE plus alle Felder die fuer die Generierung
 * einer normalen EntropyEntryEntity benoetigt werden. Beim App-Start prueft
 * GenerateRecurringInstancesUseCase fuer jede aktive Vorlage, ob seit
 * lastGeneratedAt eine neue Faelligkeit aufgetreten ist, und erzeugt dann
 * eine normale Aufgabe.
 *
 * Architektur-Entscheidung: separate Tabelle (nicht Flag auf entropy_entries),
 * damit Vorlagen klar von tatsaechlichen Aufgaben getrennt bleiben. Die generierten
 * Eintraege haben einen Verweis auf templateId — siehe EntropyEntryEntity.
 */
@Entity(
    tableName = "recurring_templates",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["nextOccurrenceAt"]),
    ],
)
data class RecurringTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val category: EntropyCategory = EntropyCategory.SONSTIGES,
    val priorityScore: Int = 50,
    val severity: Int = 5,
    val estimatedDurationMinutes: Int? = null,
    /**
     * RFC-5545-konforme Recurrence-Rule. Beispiele:
     * - "FREQ=DAILY"
     * - "FREQ=WEEKLY;BYDAY=MO,WE,FR"
     * - "FREQ=MONTHLY;BYMONTHDAY=1"
     * - "FREQ=WEEKLY;INTERVAL=2"
     * Parser: org.dmfs.lib-recur (Apache 2.0).
     */
    val rrule: String,
    /**
     * Lokale Uhrzeit (Minuten seit Mitternacht) zu der die Aufgabe taeglich/woechentlich
     * entstehen soll. Default 480 = 08:00. Verwendet beim Setzen von dueAtMs.
     */
    val timeOfDayMinutes: Int = 480,
    /** Optionales Enddatum (epoch ms). null = unendlich. */
    val untilEpochMs: Long? = null,
    /** Naechstes berechnetes Vorkommen (epoch ms). null = berechnung steht aus. */
    val nextOccurrenceAt: Long? = null,
    /** Wann zuletzt eine Instanz generiert wurde (epoch ms). 0L = noch nie. */
    val lastGeneratedAt: Long = 0L,
    /** Zaehlt erzeugte Instanzen — fuer Debug + UI ("X mal ausgefuehrt"). */
    val occurrenceCount: Int = 0,
    /** Ist die Vorlage aktiv? false = pausiert (erzeugt keine Eintraege). */
    val isActive: Boolean = true,
    /**
     * Frank-Wunsch 2026-05-31: In welchen Zeit-Bucket die generierte Aufgabe kommen
     * soll. null = "KI"/automatisch → HEUTE (bisheriges Verhalten). Frank kann pro
     * Vorlage einen festen Tag (HEUTE/MORGEN/FREIBLOCK/SPAETER) vorgeben.
     */
    val targetBucket: TimeBucket? = null,
    /**
     * Frank-Wunsch 2026-06-01: festes Wiederkehr-Intervall in Tagen. null = "KI entscheidet"
     * (bisheriges Verhalten — taeglich verfuegbar, KI plant frei). Wenn gesetzt (z.B. 5),
     * erscheint die naechste Instanz erst N Tage NACH der letzten Erledigung — eine alle
     * 5 Tage faellige Aufgabe (z.B. Kraftsport) wird also nicht jeden Morgen vorgeschlagen.
     * Wird zusaetzlich in die rrule gespiegelt (FREQ=DAILY;INTERVAL=N) fuer Anzeige + RFC-Pfad.
     */
    val intervalDays: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
)
