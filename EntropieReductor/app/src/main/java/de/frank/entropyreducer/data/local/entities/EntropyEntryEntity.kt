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
    /**
     * Frank-Wunsch 2026-05-22 (zweite Iteration): Marker ob estimatedDurationMinutes
     * vom Benutzer manuell gesetzt wurde. true = manuell, false = KI-Schaetzung.
     * Die KI darf den Wert beim Rescore nur ueberschreiben wenn dieser Marker false
     * ist — manuelle Setzungen haben immer Vorrang.
     */
    val durationManuallySet: Boolean = false,
    /**
     * Frank-Wunsch 2026-05-22 (dritte Iteration): Frist (Deadline) der Aufgabe als
     * epoch ms. null = keine Frist. Wird vom Benutzer ueber einen Datepicker gesetzt
     * und fliesst in die Prio-Berechnung ein — kurze Restzeit erhoeht die Prio
     * progressiv (siehe ProcessEntryUseCase). Bei < 24h Restzeit wird die Prio
     * auf mindestens 95 angehoben (Frank's Vorgabe: "wenn nur noch ein Tag, fast 100%").
     */
    val dueAtMs: Long? = null,
    /**
     * Frank-Wunsch 2026-05-31: manuell per Schieberegler gesetzte Prioritaet
     * (0..100, in 5er-Schritten). null = KI bestimmt die Prioritaet (priorityScore).
     * Wenn gesetzt, hat dieser Wert IMMER Vorrang vor der KI-Prioritaet — auch bei
     * spaeteren Rescores. Die Aufgaben-Kachel faerbt sich nach der effektiven
     * Prioritaet (manualPriorityScore ?: priorityScore).
     */
    val manualPriorityScore: Double? = null,
    /**
     * Frank-Wunsch 2026-06-19: Marker ob manualPriorityScore vom Benutzer AN DIESER
     * INSTANZ manuell gesetzt wurde (Schieberegler im Aufgaben-Reiter). null = NICHT
     * vom Nutzer gesetzt (Loop/Template-Prio oder KI). Wenn gesetzt (!= null), darf die
     * Loop-Pflege (GenerateRecurringInstancesUseCase) die Prio NICHT mehr mit der
     * Template-Prio ueberschreiben — manuell schlaegt KI/Template (analog manualBucketSetAt
     * beim Bucket). Eine Setzung im Loop-Template-Screen setzt diesen Marker bewusst auf
     * null zurueck (dann ist die Prio wieder Template-getrieben).
     */
    val manualPriorityScoreSetAt: Long? = null,
    /**
     * ID-Architektur (Frank-Wunsch 2026-06-19, Etappe 1): Herkunfts-Kette.
     * - originId = direkter Vorgaenger (z. B. der Aufgaben-Vorschlag, aus dem diese Aufgabe entstand)
     * - originType = Art des Vorgaengers (IDEA / TASK_SUGGESTION / HABIT_SUGGESTION / ...)
     * - rootId = Ur-Eintrag der Kette (die urspruengliche Idee)
     * Alle null = Ursprung ODER Bestandsdatum vor dem Umbau. Werden ab Etappe 2 an den
     * Uebergaengen befuellt (siehe docs/specs/2026-06-19-id-architektur-design.md).
     */
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)
