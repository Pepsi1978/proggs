package de.frank.entropyreducer.data.remote.drive

import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.model.ScientistRole
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.serialization.Serializable

/**
 * Backup-Format für Drive (appDataFolder).
 *
 * Frank-Wunsch 2026-05-09 (Abend): Vollstaendiges Backup ALLER Daten — nicht nur
 * Aufgaben (v1), sondern auch Insights, Memories, Hypothesen und Forscher-Sessions.
 * Alles was in Frank's persoenlicher Wissens-Domaene liegt soll bei Reinstall
 * wiederherstellbar sein.
 *
 * version: erlaubt zukuenftige Schema-Evolution.
 *   1 = nur entries (Pre-2026-05-09)
 *   2 = + insights, memories, hypotheses, scientistSessions, scientistMessages,
 *       hypothesisMessages (2026-05-09 Abend)
 *   3 = + biomarkerCardOrder — die Drag&Drop-Reihenfolge der Biomarker-Karten
 *       (Frank-Wunsch 2026-05-10): die Position auf dem Biomarker-Screen wird
 *       jetzt mitgesichert, damit ein Wechsel auf ein neues Handy die exakt
 *       gleiche Anordnung wiederherstellt.
 *   5 = + amazfitWorkouts — Sport-Sessions inkl. Detail-Daten (GPS-Track,
 *       Pulsverlauf, Tempoverlauf, Splits) als Cross-Device-Sicherung.
 *       Frank-Wunsch 2026-05-11: Zepp loescht Detail-Daten serverseitig nach
 *       ~30 Tagen — durch das Backup bleiben sie fuer immer erhalten und
 *       koennen auf dem S23 Ultra ohne API-Call wiederhergestellt werden
 *       (kein Re-Login, Zepp-Handy-App bleibt stabil eingeloggt).
 *
 * Beim Restore werden alle vorherigen Versionen akzeptiert — die nicht-vorhandenen
 * Listen defaulten auf emptyList(), die alten Aufgaben kommen zurueck, der Rest
 * bleibt leer. Beim naechsten Sync nach Restore wird automatisch ein v5-Backup
 * geschrieben das den vollen Stand enthaelt.
 */
@Serializable
data class BackupPayload(
    val version: Int = 5,
    val exportedAt: Long,
    val entries: List<BackupEntry>,
    val insights: List<BackupInsight> = emptyList(),
    val memories: List<BackupMemory> = emptyList(),
    val hypotheses: List<BackupHypothesis> = emptyList(),
    val scientistSessions: List<BackupScientistSession> = emptyList(),
    val scientistMessages: List<BackupScientistMessage> = emptyList(),
    val hypothesisMessages: List<BackupHypothesisMessage> = emptyList(),
    /**
     * Drag&Drop-Reihenfolge der Biomarker-Karten als pipe-separierte Liste der
     * Card-IDs (siehe BiomarkerCardId). Leer = User hat noch nichts verschoben,
     * Standard-Reihenfolge gilt. Beim Restore wird die Liste in den lokalen
     * BiomarkerCardOrderRepository.saveOrder() eingespielt — ungueltige IDs
     * (aus aelteren App-Versionen) werden dort automatisch herausgefiltert.
     */
    val biomarkerCardOrder: List<String> = emptyList(),
    /**
     * Schema v4 (Frank-Wunsch 2026-05-10 abend): Cross-Device-Cache aller
     * Health-Connect-Werte (Gewicht, Koerperfett, Magere Koerpermasse, Wasser,
     * Knochenmasse, Hoehe, BMR). Wird auf dem Sende-Geraet beim refreshWeight
     * gefuellt und beim Restore auf dem Empfaenger-Geraet in die DB geschrieben.
     * Damit hat das neue Geraet sofort den vollen Verlauf, auch wenn Zepp dort
     * nicht rueckwirkend in HC pusht. Default = emptyList damit v3-Backups
     * weiterhin lesbar bleiben.
     */
    val healthConnectValues: List<BackupHealthConnectValue> = emptyList(),
    /**
     * Schema v5 (Frank-Wunsch 2026-05-11): Cross-Device-Sicherung der gesamten
     * Sport-Trainings inklusive der teuren Detail-Streams (GPS-Track, Puls-,
     * Tempo-, Pace-Verlauf, Splits). Zepp-Cloud loescht diese Detail-Daten nach
     * ~30 Tagen serverseitig — durch das Backup haben wir sie fuer immer.
     * Beim Restore auf einem zweiten Geraet entfaellt der API-Call zu Zepp
     * komplett: kein Re-Login, kein Token-Konflikt mit der Zepp-Handy-App.
     * Default = emptyList damit aeltere Backups (v1-v4) weiterhin lesbar bleiben.
     */
    val amazfitWorkouts: List<BackupAmazfitWorkout> = emptyList(),
)

@Serializable
data class BackupHealthConnectValue(
    val metric: String,
    val timestampMs: Long,
    val value: Double,
)

/**
 * Vollstaendige Snapshot-Repraesentation eines Amazfit/Zepp-Workouts inkl.
 * aller Detail-Streams. Felder 1:1 wie in AmazfitWorkoutEntity — bei Schema-
 * Erweiterungen der Entity muessen die neuen Felder hier ergaenzt werden.
 */
@Serializable
data class BackupAmazfitWorkout(
    val trackId: String,
    val dateKey: String,
    val startMs: Long,
    val endMs: Long,
    val durationSeconds: Long? = null,
    val sportType: Int? = null,
    val sportName: String? = null,
    val distanceMeters: Double? = null,
    val avgPaceSecPerKm: Double? = null,
    val maxPaceSecPerKm: Double? = null,
    val avgSpeedKmh: Double? = null,
    val maxSpeedKmh: Double? = null,
    val calories: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val gpsTrackJson: String? = null,
    val heartRateSeriesJson: String? = null,
    val paceSeriesJson: String? = null,
    val splitsJson: String? = null,
    val altitudeGainMeters: Double? = null,
    val altitudeLossMeters: Double? = null,
    val trainingEffectAerobic: Double? = null,
    val trainingEffectAnaerobic: Double? = null,
    val vo2Max: Double? = null,
    val cadence: Int? = null,
    val strideLengthCm: Int? = null,
    val recoveryTimeHours: Int? = null,
    val skinTempCelsius: Double? = null,
    val swolf: Int? = null,
    val poolLaps: Int? = null,
    val poolLengthMeters: Double? = null,
    val source: String? = null,
    val city: String? = null,
    val paceStreamJson: String? = null,
    val createdAt: Long,
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
    /** Manueller Bucket-Override (Frank-Wunsch 2026-05-09). null = KI-Zuordnung. */
    val manualBucket: String? = null,
    val manualBucketSetAt: Long? = null,
)

@Serializable
data class BackupInsight(
    val id: String,
    val title: String,
    val description: String,
    val targetCategory: String,
    val additionalCategories: List<String> = emptyList(),
    val confidence: Int,
    val successCount: Int,
    val attemptCount: Int,
    val avgBiomarkerImpact: String? = null,
    val avgFeltImpact: Double? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceHypothesisIds: List<String> = emptyList(),
    val manualSource: Boolean = false,
)

@Serializable
data class BackupMemory(
    val id: String,
    val content: String,
    val source: String,
    val isActive: Boolean,
    val confidence: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupHypothesis(
    val id: String,
    val title: String,
    val description: String,
    val rationale: String,
    val createdAt: Long,
    val plannedStartDate: Long,
    val plannedEndDate: Long,
    val actualStartDate: Long? = null,
    val actualEndDate: Long? = null,
    val status: String,
    val outcome: String? = null,
    val outcomeNotes: String? = null,
    val biomarkerBeforeId: String? = null,
    val biomarkerAfterId: String? = null,
    val felltEntropyChange: Int? = null,
    val relatedEntryIds: List<String> = emptyList(),
)

@Serializable
data class BackupScientistSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isArchived: Boolean,
)

@Serializable
data class BackupScientistMessage(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val attachedHypothesisIds: List<String> = emptyList(),
)

@Serializable
data class BackupHypothesisMessage(
    val id: String,
    val hypothesisId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)

// ---------- Entity → Backup ----------

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
    manualBucket = manualBucket?.name,
    manualBucketSetAt = manualBucketSetAt,
)

fun InsightEntity.toBackup(): BackupInsight = BackupInsight(
    id = id,
    title = title,
    description = description,
    targetCategory = targetCategory.name,
    additionalCategories = additionalCategories.map { it.name },
    confidence = confidence,
    successCount = successCount,
    attemptCount = attemptCount,
    avgBiomarkerImpact = avgBiomarkerImpact,
    avgFeltImpact = avgFeltImpact,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sourceHypothesisIds = sourceHypothesisIds,
    manualSource = manualSource,
)

fun MemoryEntryEntity.toBackup(): BackupMemory = BackupMemory(
    id = id,
    content = content,
    source = source.name,
    isActive = isActive,
    confidence = confidence,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun HypothesisEntity.toBackup(): BackupHypothesis = BackupHypothesis(
    id = id,
    title = title,
    description = description,
    rationale = rationale,
    createdAt = createdAt,
    plannedStartDate = plannedStartDate,
    plannedEndDate = plannedEndDate,
    actualStartDate = actualStartDate,
    actualEndDate = actualEndDate,
    status = status.name,
    outcome = outcome?.name,
    outcomeNotes = outcomeNotes,
    biomarkerBeforeId = biomarkerBeforeId,
    biomarkerAfterId = biomarkerAfterId,
    felltEntropyChange = felltEntropyChange,
    relatedEntryIds = relatedEntryIds,
)

fun ScientistSessionEntity.toBackup(): BackupScientistSession = BackupScientistSession(
    id = id,
    title = title,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    isArchived = isArchived,
)

fun ScientistMessageEntity.toBackup(): BackupScientistMessage = BackupScientistMessage(
    id = id,
    sessionId = sessionId,
    role = role.name,
    content = content,
    createdAt = createdAt,
    attachedHypothesisIds = attachedHypothesisIds,
)

fun HypothesisMessageEntity.toBackup(): BackupHypothesisMessage = BackupHypothesisMessage(
    id = id,
    hypothesisId = hypothesisId,
    role = role.name,
    content = content,
    createdAt = createdAt,
)

fun AmazfitWorkoutEntity.toBackup(): BackupAmazfitWorkout = BackupAmazfitWorkout(
    trackId = trackId,
    dateKey = dateKey,
    startMs = startMs,
    endMs = endMs,
    durationSeconds = durationSeconds,
    sportType = sportType,
    sportName = sportName,
    distanceMeters = distanceMeters,
    avgPaceSecPerKm = avgPaceSecPerKm,
    maxPaceSecPerKm = maxPaceSecPerKm,
    avgSpeedKmh = avgSpeedKmh,
    maxSpeedKmh = maxSpeedKmh,
    calories = calories,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    gpsTrackJson = gpsTrackJson,
    heartRateSeriesJson = heartRateSeriesJson,
    paceSeriesJson = paceSeriesJson,
    splitsJson = splitsJson,
    altitudeGainMeters = altitudeGainMeters,
    altitudeLossMeters = altitudeLossMeters,
    trainingEffectAerobic = trainingEffectAerobic,
    trainingEffectAnaerobic = trainingEffectAnaerobic,
    vo2Max = vo2Max,
    cadence = cadence,
    strideLengthCm = strideLengthCm,
    recoveryTimeHours = recoveryTimeHours,
    skinTempCelsius = skinTempCelsius,
    swolf = swolf,
    poolLaps = poolLaps,
    poolLengthMeters = poolLengthMeters,
    source = source,
    city = city,
    paceStreamJson = paceStreamJson,
    createdAt = createdAt,
)

// ---------- Backup → Entity ----------

/**
 * Mapping fuer Bucket-Strings aus aelteren Backup-Versionen (Frank-Wunsch
 * 2026-05-09). DIESE_WOCHE und DIESEN_MONAT existieren nicht mehr — sie werden
 * auf FREIBLOCK gemappt damit alte Drive-Backups nichts verlieren. Unbekannte
 * Werte fallen auf SPAETER zurueck.
 */
private fun parseBucketCompat(name: String?): TimeBucket {
    if (name == null) return TimeBucket.HEUTE
    return runCatching { TimeBucket.valueOf(name) }.getOrElse {
        when (name) {
            "DIESE_WOCHE", "DIESEN_MONAT" -> TimeBucket.FREIBLOCK
            else -> TimeBucket.SPAETER
        }
    }
}

private fun parseCategoryCompat(name: String): EntropyCategory =
    runCatching { EntropyCategory.valueOf(name) }.getOrDefault(EntropyCategory.SONSTIGES)

fun BackupEntry.toEntity(): EntropyEntryEntity = EntropyEntryEntity(
    id = id,
    rawTranscript = rawTranscript,
    title = title,
    description = description,
    category = parseCategoryCompat(category),
    severity = severity.coerceIn(1, 10),
    priorityScore = priorityScore.coerceIn(0.0, 100.0),
    priorityReason = priorityReason,
    status = runCatching { EntryStatus.valueOf(status) }.getOrDefault(EntryStatus.OFFEN),
    timeBucket = parseBucketCompat(timeBucket),
    estimatedDurationMinutes = estimatedDurationMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    resolvedAt = resolvedAt,
    tags = tags,
    aiNotes = aiNotes,
    source = runCatching { EntrySource.valueOf(source) }
        .getOrDefault(EntrySource.NUTZER_TEXT),
    biomarkerSnapshotId = biomarkerSnapshotId,
    manualBucket = manualBucket?.let { parseBucketCompat(it) },
    manualBucketSetAt = manualBucketSetAt,
)

fun BackupInsight.toEntity(): InsightEntity = InsightEntity(
    id = id,
    title = title,
    description = description,
    targetCategory = parseCategoryCompat(targetCategory),
    additionalCategories = additionalCategories.map { parseCategoryCompat(it) }
        .filter { it != parseCategoryCompat(targetCategory) }
        .distinct(),
    confidence = confidence.coerceIn(0, 100),
    successCount = successCount,
    attemptCount = attemptCount,
    avgBiomarkerImpact = avgBiomarkerImpact,
    avgFeltImpact = avgFeltImpact,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sourceHypothesisIds = sourceHypothesisIds,
    manualSource = manualSource,
)

fun BackupMemory.toEntity(): MemoryEntryEntity = MemoryEntryEntity(
    id = id,
    content = content,
    source = runCatching { MemorySource.valueOf(source) }.getOrDefault(MemorySource.MANUELL),
    isActive = isActive,
    confidence = confidence.coerceIn(0, 100),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BackupHypothesis.toEntity(): HypothesisEntity = HypothesisEntity(
    id = id,
    title = title,
    description = description,
    rationale = rationale,
    createdAt = createdAt,
    plannedStartDate = plannedStartDate,
    plannedEndDate = plannedEndDate,
    actualStartDate = actualStartDate,
    actualEndDate = actualEndDate,
    status = runCatching { HypothesisStatus.valueOf(status) }
        .getOrDefault(HypothesisStatus.VORGESCHLAGEN),
    outcome = outcome?.let { runCatching { HypothesisOutcome.valueOf(it) }.getOrNull() },
    outcomeNotes = outcomeNotes,
    biomarkerBeforeId = biomarkerBeforeId,
    biomarkerAfterId = biomarkerAfterId,
    felltEntropyChange = felltEntropyChange,
    relatedEntryIds = relatedEntryIds,
)

fun BackupScientistSession.toEntity(): ScientistSessionEntity = ScientistSessionEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    isArchived = isArchived,
)

fun BackupScientistMessage.toEntity(): ScientistMessageEntity = ScientistMessageEntity(
    id = id,
    sessionId = sessionId,
    role = runCatching { ScientistRole.valueOf(role) }.getOrDefault(ScientistRole.NUTZER),
    content = content,
    createdAt = createdAt,
    attachedHypothesisIds = attachedHypothesisIds,
)

fun BackupHypothesisMessage.toEntity(): HypothesisMessageEntity = HypothesisMessageEntity(
    id = id,
    hypothesisId = hypothesisId,
    role = runCatching { ScientistRole.valueOf(role) }.getOrDefault(ScientistRole.NUTZER),
    content = content,
    createdAt = createdAt,
)

fun BackupAmazfitWorkout.toEntity(): AmazfitWorkoutEntity = AmazfitWorkoutEntity(
    trackId = trackId,
    dateKey = dateKey,
    startMs = startMs,
    endMs = endMs,
    durationSeconds = durationSeconds,
    sportType = sportType,
    sportName = sportName,
    distanceMeters = distanceMeters,
    avgPaceSecPerKm = avgPaceSecPerKm,
    maxPaceSecPerKm = maxPaceSecPerKm,
    avgSpeedKmh = avgSpeedKmh,
    maxSpeedKmh = maxSpeedKmh,
    calories = calories,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    gpsTrackJson = gpsTrackJson,
    heartRateSeriesJson = heartRateSeriesJson,
    paceSeriesJson = paceSeriesJson,
    splitsJson = splitsJson,
    altitudeGainMeters = altitudeGainMeters,
    altitudeLossMeters = altitudeLossMeters,
    trainingEffectAerobic = trainingEffectAerobic,
    trainingEffectAnaerobic = trainingEffectAnaerobic,
    vo2Max = vo2Max,
    cadence = cadence,
    strideLengthCm = strideLengthCm,
    recoveryTimeHours = recoveryTimeHours,
    skinTempCelsius = skinTempCelsius,
    swolf = swolf,
    poolLaps = poolLaps,
    poolLengthMeters = poolLengthMeters,
    source = source,
    city = city,
    paceStreamJson = paceStreamJson,
    createdAt = createdAt,
)
