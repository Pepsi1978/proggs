package de.frank.entropyreducer.data.remote.drive

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.serialization.Serializable

/**
 * Backup-Format für Drive (appDataFolder).
 * version: erlaubt zukuenftige Schema-Evolution.
 * exportedAt: Zeitstempel des Exports — Restore prueft, ob das Drive-Backup
 *             juenger ist als der lokale Stand.
 * entries: alle EntropyEntries (auch ARCHIVIERTE) — vollstaendiger Snapshot.
 */
@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val entries: List<BackupEntry>,
)

@Serializable
data class BackupEntry(
    val id: String,
    val rawTranscript: String,
    val title: String,
    val description: String,
    val category: String,
    val severity: Int,
    val priorityScore: Double,
    val priorityReason: String,
    val status: String,
    val timeBucket: String,
    val estimatedDurationMinutes: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long? = null,
    val tags: List<String> = emptyList(),
    val aiNotes: String? = null,
    val source: String,
    val biomarkerSnapshotId: String? = null,
)

fun EntropyEntryEntity.toBackup(): BackupEntry = BackupEntry(
    id = id,
    rawTranscript = rawTranscript,
    title = title,
    description = description,
    category = category.name,
    severity = severity,
    priorityScore = priorityScore,
    priorityReason = priorityReason,
    status = status.name,
    timeBucket = timeBucket.name,
    estimatedDurationMinutes = estimatedDurationMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    resolvedAt = resolvedAt,
    tags = tags,
    aiNotes = aiNotes,
    source = source.name,
    biomarkerSnapshotId = biomarkerSnapshotId,
)

fun BackupEntry.toEntity(): EntropyEntryEntity = EntropyEntryEntity(
    id = id,
    rawTranscript = rawTranscript,
    title = title,
    description = description,
    category = runCatching { EntropyCategory.valueOf(category) }
        .getOrDefault(EntropyCategory.SONSTIGES),
    severity = severity.coerceIn(1, 10),
    priorityScore = priorityScore.coerceIn(0.0, 100.0),
    priorityReason = priorityReason,
    status = runCatching { EntryStatus.valueOf(status) }.getOrDefault(EntryStatus.OFFEN),
    timeBucket = runCatching { TimeBucket.valueOf(timeBucket) }
        .getOrDefault(TimeBucket.HEUTE),
    estimatedDurationMinutes = estimatedDurationMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    resolvedAt = resolvedAt,
    tags = tags,
    aiNotes = aiNotes,
    source = runCatching { EntrySource.valueOf(source) }
        .getOrDefault(EntrySource.NUTZER_TEXT),
    biomarkerSnapshotId = biomarkerSnapshotId,
)
