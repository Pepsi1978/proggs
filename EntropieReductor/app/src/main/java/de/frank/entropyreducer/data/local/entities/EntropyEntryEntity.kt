package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket

/**
 * Eintrag im Aufgaben-Stream: rohe Notiz + KI-Analyse + Status.
 *
 * Indizes (Frank-Wunsch 2026-05-09 Performance): die Haupt-Queries filtern auf
 * status, timeBucket, category, resolvedAt und sortieren nach priorityScore.
 * Ohne Index waeren das Full-Table-Scans — bei 100+ Eintraegen merklich.
 *
 * @Immutable (PERFORMANCE 2026-05-09): garantiert Compose dass alle Felder
 * effektiv unveraenderlich sind. Ohne diese Annotation behandelt Compose
 * `tags: List<String>` als unstable (List ist eine Schnittstelle, kein
 * konkret immutable Typ), was die ganze Entity unstable macht und
 * EntropyEntryCard nicht-skippable. Bei jeder LazyColumn-Recomposition
 * wuerden alle sichtbaren Karten neu komponiert — Hauptursache fuer Jank
 * beim Scrollen im Aufgaben-Bereich.
 */
@Immutable
@Entity(
    tableName = "entropy_entries",
    indices = [
        Index("status"),
        Index("timeBucket"),
        Index("category"),
        Index("resolvedAt"),
        Index("priorityScore"),
        Index(value = ["status", "timeBucket"]), // getActive + getByTimeBucket
        Index(value = ["status", "resolvedAt"]), // getRecentlyResolved + getResolvedBefore
    ],
)
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
