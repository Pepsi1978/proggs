package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket

/**
 * Eintrag im Aufgaben-Stream: rohe Notiz + KI-Analyse + Status.
 */
@Entity(tableName = "entropy_entries")
data class EntropyEntryEntity(
    @PrimaryKey val id: String,
    val rawTranscript: String,
    val title: String,
    val description: String,
    val category: EntropyCategory,
    val severity: Int,                       // 1..10
    val priorityScore: Double,               // 0.0..100.0
    val priorityReason: String,
    val status: EntryStatus,
    val timeBucket: TimeBucket,
    val estimatedDurationMinutes: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long?,
    val tags: List<String>,
    val aiNotes: String?,
    val source: EntrySource,
    val biomarkerSnapshotId: String?,
    /**
     * Manuell von Frank gesetzter Bucket (Frank-Wunsch 2026-05-09). Ueberschreibt
     * die KI-Zuordnung. null = KI bestimmt den Bucket. Wenn gesetzt, wird
     * timeBucket im selben Update auf den gleichen Wert geschrieben — manualBucket
     * dient nur als Marker dass die Zuordnung vom User stammt (nicht von der KI).
     */
    val manualBucket: TimeBucket? = null,
    /**
     * Zeitstempel wann manualBucket gesetzt wurde — fuer Tag-Rollover-Logik:
     * MORGEN-Eintraege werden am naechsten Tag automatisch zu HEUTE.
     */
    val manualBucketSetAt: Long? = null,
)
