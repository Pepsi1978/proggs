package de.frank.entropyreducer.data.remote.drive

import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraPersonalInfoEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
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
 * Frank-Wunsch 2026-05-09 (Abend): Vollstaendiges Backup ALLER Daten — nicht nur Aufgaben (v1),
 * sondern auch Insights, Memories, Hypothesen und Forscher-Sessions. Alles was in Frank's
 * persoenlicher Wissens-Domaene liegt soll bei Reinstall wiederherstellbar sein.
 *
 * version: erlaubt zukuenftige Schema-Evolution. 1 = nur entries (Pre-2026-05-09) 2 = + insights,
 * memories, hypotheses, scientistSessions, scientistMessages, hypothesisMessages (2026-05-09 Abend)
 * 3 = + biomarkerCardOrder — die Drag&Drop-Reihenfolge der Biomarker-Karten (Frank-Wunsch
 * 2026-05-10): die Position auf dem Biomarker-Screen wird jetzt mitgesichert, damit ein Wechsel auf
 * ein neues Handy die exakt gleiche Anordnung wiederherstellt. 5 = + amazfitWorkouts —
 * Sport-Sessions inkl. Detail-Daten (GPS-Track, Pulsverlauf, Tempoverlauf, Splits) als
 * Cross-Device-Sicherung. Frank-Wunsch 2026-05-11: Zepp loescht Detail-Daten serverseitig nach ~30
 * Tagen — durch das Backup bleiben sie fuer immer erhalten und koennen auf dem S23 Ultra ohne
 * API-Call wiederhergestellt werden (kein Re-Login, Zepp-Handy-App bleibt stabil eingeloggt).
 *
 * Beim Restore werden alle vorherigen Versionen akzeptiert — die nicht-vorhandenen Listen defaulten
 * auf emptyList(), die alten Aufgaben kommen zurueck, der Rest bleibt leer. Beim naechsten Sync
 * nach Restore wird automatisch ein v5-Backup geschrieben das den vollen Stand enthaelt.
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
     * Drag&Drop-Reihenfolge der Biomarker-Karten als pipe-separierte Liste der Card-IDs (siehe
     * BiomarkerCardId). Leer = User hat noch nichts verschoben, Standard-Reihenfolge gilt. Beim
     * Restore wird die Liste in den lokalen BiomarkerCardOrderRepository.saveOrder() eingespielt —
     * ungueltige IDs (aus aelteren App-Versionen) werden dort automatisch herausgefiltert.
     */
    val biomarkerCardOrder: List<String> = emptyList(),
    /**
     * Schema v4 (Frank-Wunsch 2026-05-10 abend): Cross-Device-Cache aller Health-Connect-Werte
     * (Gewicht, Koerperfett, Magere Koerpermasse, Wasser, Knochenmasse, Hoehe, BMR). Wird auf dem
     * Sende-Geraet beim refreshWeight gefuellt und beim Restore auf dem Empfaenger-Geraet in die DB
     * geschrieben. Damit hat das neue Geraet sofort den vollen Verlauf, auch wenn Zepp dort nicht
     * rueckwirkend in HC pusht. Default = emptyList damit v3-Backups weiterhin lesbar bleiben.
     */
    val healthConnectValues: List<BackupHealthConnectValue> = emptyList(),
    /**
     * Schema v5 (Frank-Wunsch 2026-05-11): Cross-Device-Sicherung der gesamten Sport-Trainings
     * inklusive der teuren Detail-Streams (GPS-Track, Puls-, Tempo-, Pace-Verlauf, Splits).
     * Zepp-Cloud loescht diese Detail-Daten nach ~30 Tagen serverseitig — durch das Backup haben
     * wir sie fuer immer. Beim Restore auf einem zweiten Geraet entfaellt der API-Call zu Zepp
     * komplett: kein Re-Login, kein Token-Konflikt mit der Zepp-Handy-App. Default = emptyList
     * damit aeltere Backups (v1-v4) weiterhin lesbar bleiben.
     */
    val amazfitWorkouts: List<BackupAmazfitWorkout> = emptyList(),
    /**
     * Schema v6 (Frank-Wunsch 2026-05-20): Persoenliches Profil aus den Einstellungen. Wird in
     * jeden KI-Aufruf als Systemkontext mitgeschickt — bei Reinstall ist das ohne Backup verloren.
     */
    val profileText: String = "",
    /**
     * Schema v6 (Frank-Wunsch 2026-05-20): Eintraege aus dem Sub-Bereich "Entropie" (Tagebuch).
     * Inkl. KI-Zusammenfassung und allen Nachtraegen pro Eintrag.
     */
    val tagebuchEntries: List<BackupTagebuchEntry> = emptyList(),
    /**
     * Schema v6 (Frank-Wunsch 2026-05-20): Nachtraege zu Aufgaben-Eintraegen
     * (entropy_entry_followups). Werden ueber entryId mit dem Haupteintrag verknuepft.
     */
    val entropyEntryFollowups: List<BackupEntropyFollowup> = emptyList(),
    /**
     * Schema v7 (Frank-Wunsch 2026-05-20): Eintraege aus dem Sub-Bereich "Thesen" (Aufgaben-Tab
     * Index 2). Spiegelt das gleiche Format wie tagebuchEntries — jeder Eintrag hat Titel, Text,
     * optionale KI-Zusammenfassung und Nachtraege.
     */
    val thesenEntries: List<BackupThesenEntry> = emptyList(),
)

/** Schema v7: Thesen-Eintrag mit Nachtraegen und KI-Zusammenfassung. */
@Serializable
data class BackupThesenEntry(
    val id: String,
    val timestampMs: Long,
    val title: String,
    val text: String,
    val summary: String? = null,
    val followups: List<BackupThesenFollowup> = emptyList(),
)

/** Schema v7: einzelner Nachtrag in den Thesen. */
@Serializable
data class BackupThesenFollowup(val id: String, val createdAtMs: Long, val text: String)

/** Schema v6: Tagebuch-Eintrag mit Nachtraegen und KI-Zusammenfassung. */
@Serializable
data class BackupTagebuchEntry(
    val id: String,
    val timestampMs: Long,
    val title: String,
    val text: String,
    val summary: String? = null,
    val followups: List<BackupTagebuchFollowup> = emptyList(),
)

/** Schema v6: einzelner Nachtrag im Tagebuch. */
@Serializable
data class BackupTagebuchFollowup(val id: String, val createdAtMs: Long, val text: String)

/** Schema v6: Nachtrag zu einer Aufgabe (entropy_entry_followups). */
@Serializable
data class BackupEntropyFollowup(
    val id: String,
    val entryId: String,
    val rawText: String,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Frank-Wunsch 2026-05-19: Eigene Backup-Datei nur fuer Sport-Trainings. Liegt im
 * Drive-appDataFolder als `entropy_reducer_workouts_v1.json`. Wird beim SyncCoordinator- Upload
 * separat hochgeladen, nachdem das Haupt-Backup erfolgreich war. Beim Restore (z.B. nach `adb
 * uninstall`) wird die Datei automatisch heruntergeladen und in die lokale DB geschrieben — kein
 * Polar-ZIP-Re-Import noetig.
 *
 * Im Gegensatz zur slim-Projektion im Haupt-Backup enthaelt diese Liste ALLE Felder inkl. der
 * teuren Stream-Spalten (gpsTrackJson, heartRateSeriesJson, paceSeriesJson, paceStreamJson,
 * splitsJson). Nach unserer 2-Jahres-Retention ist das ein vertretbarer Datenumfang (~104
 * Trainings * ~50-100 KB = max 15 MB).
 *
 * version: erlaubt Schema-Evolution analog zu BackupPayload. 1 = initiale Version (2026-05-19)
 */
@Serializable
data class WorkoutsBackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val workouts: List<BackupAmazfitWorkout>,
)

@Serializable
data class BackupHealthConnectValue(val metric: String, val timestampMs: Long, val value: Double)

/**
 * Vollstaendige Snapshot-Repraesentation eines Amazfit/Zepp-Workouts inkl. aller Detail-Streams.
 * Felder 1:1 wie in AmazfitWorkoutEntity — bei Schema- Erweiterungen der Entity muessen die neuen
 * Felder hier ergaenzt werden.
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

fun EntropyEntryEntity.toBackup(): BackupEntry =
    BackupEntry(
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

fun InsightEntity.toBackup(): BackupInsight =
    BackupInsight(
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

fun MemoryEntryEntity.toBackup(): BackupMemory =
    BackupMemory(
        id = id,
        content = content,
        source = source.name,
        isActive = isActive,
        confidence = confidence,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun HypothesisEntity.toBackup(): BackupHypothesis =
    BackupHypothesis(
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

fun ScientistSessionEntity.toBackup(): BackupScientistSession =
    BackupScientistSession(
        id = id,
        title = title,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
        isArchived = isArchived,
    )

fun ScientistMessageEntity.toBackup(): BackupScientistMessage =
    BackupScientistMessage(
        id = id,
        sessionId = sessionId,
        role = role.name,
        content = content,
        createdAt = createdAt,
        attachedHypothesisIds = attachedHypothesisIds,
    )

fun HypothesisMessageEntity.toBackup(): BackupHypothesisMessage =
    BackupHypothesisMessage(
        id = id,
        hypothesisId = hypothesisId,
        role = role.name,
        content = content,
        createdAt = createdAt,
    )

/**
 * Mapping aus der schlanken Backup-Projektion. Streams sind hier per SELECT bereits ausgeschlossen
 * — kein RAM-Druck.
 */
fun de.frank.entropyreducer.data.local.dao.AmazfitWorkoutBackupRow.toBackup():
    BackupAmazfitWorkout =
    BackupAmazfitWorkout(
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
        gpsTrackJson = null,
        heartRateSeriesJson = null,
        paceSeriesJson = null,
        splitsJson = null,
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
        paceStreamJson = null,
        createdAt = createdAt,
    )

/**
 * Frank-Wunsch 2026-05-19: Vollstaendige Backup-Repraesentation INKLUSIVE aller Stream-Felder
 * (gpsTrackJson, heartRateSeriesJson, paceSeriesJson, paceStreamJson, splitsJson). Wird vom
 * separaten Workouts-Backup-File (`entropy_reducer_workouts_v1.json`) genutzt — das Haupt-Backup
 * bleibt schlank dank der 2-Jahres-Retention (~104 Trainings statt 956).
 *
 * Frueher (vor 2026-05-19) wurden die Streams pauschal auf null gesetzt um OOM bei 956+
 * Polar-Bulk-Trainings zu vermeiden. Mit der jetzigen Retention ist das Volumen unter ~15 MB
 * insgesamt — kein OOM-Risiko mehr.
 */
fun AmazfitWorkoutEntity.toBackup(): BackupAmazfitWorkout =
    BackupAmazfitWorkout(
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
 * Mapping fuer Bucket-Strings aus aelteren Backup-Versionen (Frank-Wunsch 2026-05-09). DIESE_WOCHE
 * und DIESEN_MONAT existieren nicht mehr — sie werden auf FREIBLOCK gemappt damit alte
 * Drive-Backups nichts verlieren. Unbekannte Werte fallen auf SPAETER zurueck.
 */
private fun parseBucketCompat(name: String?): TimeBucket {
    if (name == null) return TimeBucket.HEUTE
    return runCatching { TimeBucket.valueOf(name) }
        .getOrElse {
            when (name) {
                "DIESE_WOCHE",
                "DIESEN_MONAT" -> TimeBucket.FREIBLOCK
                else -> TimeBucket.SPAETER
            }
        }
}

private fun parseCategoryCompat(name: String): EntropyCategory =
    runCatching { EntropyCategory.valueOf(name) }.getOrDefault(EntropyCategory.SONSTIGES)

fun BackupEntry.toEntity(): EntropyEntryEntity =
    EntropyEntryEntity(
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
        source = runCatching { EntrySource.valueOf(source) }.getOrDefault(EntrySource.NUTZER_TEXT),
        biomarkerSnapshotId = biomarkerSnapshotId,
        manualBucket = manualBucket?.let { parseBucketCompat(it) },
        manualBucketSetAt = manualBucketSetAt,
    )

fun BackupInsight.toEntity(): InsightEntity =
    InsightEntity(
        id = id,
        title = title,
        description = description,
        targetCategory = parseCategoryCompat(targetCategory),
        additionalCategories =
            additionalCategories
                .map { parseCategoryCompat(it) }
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

fun BackupMemory.toEntity(): MemoryEntryEntity =
    MemoryEntryEntity(
        id = id,
        content = content,
        source = runCatching { MemorySource.valueOf(source) }.getOrDefault(MemorySource.MANUELL),
        isActive = isActive,
        confidence = confidence.coerceIn(0, 100),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BackupHypothesis.toEntity(): HypothesisEntity =
    HypothesisEntity(
        id = id,
        title = title,
        description = description,
        rationale = rationale,
        createdAt = createdAt,
        plannedStartDate = plannedStartDate,
        plannedEndDate = plannedEndDate,
        actualStartDate = actualStartDate,
        actualEndDate = actualEndDate,
        status =
            runCatching { HypothesisStatus.valueOf(status) }
                .getOrDefault(HypothesisStatus.VORGESCHLAGEN),
        outcome = outcome?.let { runCatching { HypothesisOutcome.valueOf(it) }.getOrNull() },
        outcomeNotes = outcomeNotes,
        biomarkerBeforeId = biomarkerBeforeId,
        biomarkerAfterId = biomarkerAfterId,
        felltEntropyChange = felltEntropyChange,
        relatedEntryIds = relatedEntryIds,
    )

fun BackupScientistSession.toEntity(): ScientistSessionEntity =
    ScientistSessionEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
        isArchived = isArchived,
    )

fun BackupScientistMessage.toEntity(): ScientistMessageEntity =
    ScientistMessageEntity(
        id = id,
        sessionId = sessionId,
        role = runCatching { ScientistRole.valueOf(role) }.getOrDefault(ScientistRole.NUTZER),
        content = content,
        createdAt = createdAt,
        attachedHypothesisIds = attachedHypothesisIds,
    )

fun BackupHypothesisMessage.toEntity(): HypothesisMessageEntity =
    HypothesisMessageEntity(
        id = id,
        hypothesisId = hypothesisId,
        role = runCatching { ScientistRole.valueOf(role) }.getOrDefault(ScientistRole.NUTZER),
        content = content,
        createdAt = createdAt,
    )

fun BackupAmazfitWorkout.toEntity(): AmazfitWorkoutEntity =
    AmazfitWorkoutEntity(
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

// =========================================================================
// Health-Backup (Frank-Wunsch 2026-05-19): separate Drive-Datei
// `entropy_reducer_health_v1.json` fuer Whoop- und Oura-Daten. Verhindert
// Datenverlust nach Reinstall — Whoop liefert nur 90 Tage rueckwirkend,
// Oura nur 6 Monate. Volumen ca. 1-3 MB fuer 2 Jahre (keine GPS-Streams,
// nur Daily-Summary plus 5-Min-Schlafphasen-Strings).
// =========================================================================

/**
 * Komplettes Health-Backup mit Whoop (Daily Recovery + Workouts) und Oura (6 Tabellen). Wird als
 * eigene Drive-Datei `entropy_reducer_health_v1.json` im appDataFolder gespeichert, parallel zum
 * Haupt- und Workouts-Backup.
 *
 * version: 1 = initiale Version (Frank-Wunsch 2026-05-19).
 */
@Serializable
data class HealthBackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val whoopSnapshots: List<BackupBiomarkerSnapshot> = emptyList(),
    val whoopWorkouts: List<BackupWhoopWorkout> = emptyList(),
    val ouraReadiness: List<BackupOuraReadiness> = emptyList(),
    val ouraDailySleep: List<BackupOuraDailySleep> = emptyList(),
    val ouraActivity: List<BackupOuraActivity> = emptyList(),
    val ouraResilience: List<BackupOuraResilience> = emptyList(),
    val ouraSleepDetail: List<BackupOuraSleepDetail> = emptyList(),
    val ouraPersonalInfo: BackupOuraPersonalInfo? = null,
)

// ---------- Whoop Datenklassen ----------

/** 1:1 Spiegel der BiomarkerSnapshotEntity (Whoop Daily Recovery). */
@Serializable
data class BackupBiomarkerSnapshot(
    val id: String,
    val capturedAt: Long,
    val recoveryScore: Int? = null,
    val hrvMs: Double? = null,
    val restingHeartRate: Int? = null,
    val sleepPerformance: Int? = null,
    val sleepTotalMinutes: Int? = null,
    val sleepRemMinutes: Int? = null,
    val sleepDeepMinutes: Int? = null,
    val sleepLightMinutes: Int? = null,
    val sleepAwakeMinutes: Int? = null,
    val sleepDisturbances: Int? = null,
    val dayStrain: Double? = null,
    val dayKilojoules: Double? = null,
    val createdAt: Long,
    val respiratoryRate: Double? = null,
    val sleepConsistencyPercent: Int? = null,
    val sleepEfficiencyPercent: Int? = null,
    val sleepNeedMinutes: Int? = null,
    val sleepDebtMinutes: Int? = null,
    val spo2Percent: Double? = null,
    val skinTempCelsius: Double? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val sleepCycleCount: Int? = null,
)

/** 1:1 Spiegel der WhoopWorkoutEntity. */
@Serializable
data class BackupWhoopWorkout(
    val id: String,
    val dateKey: String,
    val startMs: Long,
    val endMs: Long,
    val sportId: Int? = null,
    val sportName: String? = null,
    val strain: Double? = null,
    val kilojoule: Double? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val percentRecorded: Double? = null,
    val distanceMeter: Double? = null,
    val altitudeGainMeter: Double? = null,
    val zoneZeroMilli: Long? = null,
    val zoneOneMilli: Long? = null,
    val zoneTwoMilli: Long? = null,
    val zoneThreeMilli: Long? = null,
    val zoneFourMilli: Long? = null,
    val zoneFiveMilli: Long? = null,
    val createdAt: Long,
)

// ---------- Oura Datenklassen ----------

@Serializable
data class BackupOuraReadiness(
    val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val temperatureDeviation: Double? = null,
    val temperatureTrendDeviation: Double? = null,
    val activityBalance: Int? = null,
    val bodyTemperature: Int? = null,
    val hrvBalance: Int? = null,
    val previousDayActivity: Int? = null,
    val previousNight: Int? = null,
    val recoveryIndex: Int? = null,
    val restingHeartRate: Int? = null,
    val sleepBalance: Int? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraDailySleep(
    val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val deepSleepScore: Int? = null,
    val efficiencyScore: Int? = null,
    val latencyScore: Int? = null,
    val remSleepScore: Int? = null,
    val restfulnessScore: Int? = null,
    val timingScore: Int? = null,
    val totalSleepScore: Int? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraActivity(
    val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val activeCalories: Int? = null,
    val totalCalories: Int? = null,
    val targetCalories: Int? = null,
    val steps: Int? = null,
    val walkingDistanceMeters: Int? = null,
    val highActivitySeconds: Int? = null,
    val mediumActivitySeconds: Int? = null,
    val lowActivitySeconds: Int? = null,
    val nonWearSeconds: Int? = null,
    val restingSeconds: Int? = null,
    val sedentarySeconds: Int? = null,
    val inactivityAlerts: Int? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraResilience(
    val day: String,
    val capturedAt: Long,
    val level: String? = null,
    val sleepRecovery: Double? = null,
    val daytimeRecovery: Double? = null,
    val stress: Double? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraSleepDetail(
    val id: String,
    val day: String,
    val capturedAt: Long,
    val bedtimeStart: String? = null,
    val bedtimeEnd: String? = null,
    val type: String? = null,
    val totalSleepSeconds: Int? = null,
    val timeInBedSeconds: Int? = null,
    val awakeSeconds: Int? = null,
    val lightSeconds: Int? = null,
    val deepSeconds: Int? = null,
    val remSeconds: Int? = null,
    val efficiency: Int? = null,
    val latencySeconds: Int? = null,
    val restlessPeriods: Int? = null,
    val averageBreath: Double? = null,
    val averageHeartRate: Double? = null,
    val averageHrv: Int? = null,
    val lowestHeartRate: Int? = null,
    val sleepPhase5Min: String? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraPersonalInfo(
    val id: Long = 1L,
    val ouraUserId: String? = null,
    val age: Int? = null,
    val weightKg: Double? = null,
    val heightMeters: Double? = null,
    val biologicalSex: String? = null,
    val email: String? = null,
    val capturedAt: Long,
)

// ---------- Entity → Backup Mappings ----------

fun BiomarkerSnapshotEntity.toBackup(): BackupBiomarkerSnapshot =
    BackupBiomarkerSnapshot(
        id = id,
        capturedAt = capturedAt,
        recoveryScore = recoveryScore,
        hrvMs = hrvMs,
        restingHeartRate = restingHeartRate,
        sleepPerformance = sleepPerformance,
        sleepTotalMinutes = sleepTotalMinutes,
        sleepRemMinutes = sleepRemMinutes,
        sleepDeepMinutes = sleepDeepMinutes,
        sleepLightMinutes = sleepLightMinutes,
        sleepAwakeMinutes = sleepAwakeMinutes,
        sleepDisturbances = sleepDisturbances,
        dayStrain = dayStrain,
        dayKilojoules = dayKilojoules,
        createdAt = createdAt,
        respiratoryRate = respiratoryRate,
        sleepConsistencyPercent = sleepConsistencyPercent,
        sleepEfficiencyPercent = sleepEfficiencyPercent,
        sleepNeedMinutes = sleepNeedMinutes,
        sleepDebtMinutes = sleepDebtMinutes,
        spo2Percent = spo2Percent,
        skinTempCelsius = skinTempCelsius,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        sleepCycleCount = sleepCycleCount,
    )

fun WhoopWorkoutEntity.toBackup(): BackupWhoopWorkout =
    BackupWhoopWorkout(
        id = id,
        dateKey = dateKey,
        startMs = startMs,
        endMs = endMs,
        sportId = sportId,
        sportName = sportName,
        strain = strain,
        kilojoule = kilojoule,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        percentRecorded = percentRecorded,
        distanceMeter = distanceMeter,
        altitudeGainMeter = altitudeGainMeter,
        zoneZeroMilli = zoneZeroMilli,
        zoneOneMilli = zoneOneMilli,
        zoneTwoMilli = zoneTwoMilli,
        zoneThreeMilli = zoneThreeMilli,
        zoneFourMilli = zoneFourMilli,
        zoneFiveMilli = zoneFiveMilli,
        createdAt = createdAt,
    )

fun OuraReadinessEntity.toBackup(): BackupOuraReadiness =
    BackupOuraReadiness(
        day = day,
        capturedAt = capturedAt,
        score = score,
        temperatureDeviation = temperatureDeviation,
        temperatureTrendDeviation = temperatureTrendDeviation,
        activityBalance = activityBalance,
        bodyTemperature = bodyTemperature,
        hrvBalance = hrvBalance,
        previousDayActivity = previousDayActivity,
        previousNight = previousNight,
        recoveryIndex = recoveryIndex,
        restingHeartRate = restingHeartRate,
        sleepBalance = sleepBalance,
        createdAt = createdAt,
    )

fun OuraDailySleepEntity.toBackup(): BackupOuraDailySleep =
    BackupOuraDailySleep(
        day = day,
        capturedAt = capturedAt,
        score = score,
        deepSleepScore = deepSleepScore,
        efficiencyScore = efficiencyScore,
        latencyScore = latencyScore,
        remSleepScore = remSleepScore,
        restfulnessScore = restfulnessScore,
        timingScore = timingScore,
        totalSleepScore = totalSleepScore,
        createdAt = createdAt,
    )

fun OuraActivityEntity.toBackup(): BackupOuraActivity =
    BackupOuraActivity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        activeCalories = activeCalories,
        totalCalories = totalCalories,
        targetCalories = targetCalories,
        steps = steps,
        walkingDistanceMeters = walkingDistanceMeters,
        highActivitySeconds = highActivitySeconds,
        mediumActivitySeconds = mediumActivitySeconds,
        lowActivitySeconds = lowActivitySeconds,
        nonWearSeconds = nonWearSeconds,
        restingSeconds = restingSeconds,
        sedentarySeconds = sedentarySeconds,
        inactivityAlerts = inactivityAlerts,
        createdAt = createdAt,
    )

fun OuraResilienceEntity.toBackup(): BackupOuraResilience =
    BackupOuraResilience(
        day = day,
        capturedAt = capturedAt,
        level = level,
        sleepRecovery = sleepRecovery,
        daytimeRecovery = daytimeRecovery,
        stress = stress,
        createdAt = createdAt,
    )

fun OuraSleepDetailEntity.toBackup(): BackupOuraSleepDetail =
    BackupOuraSleepDetail(
        id = id,
        day = day,
        capturedAt = capturedAt,
        bedtimeStart = bedtimeStart,
        bedtimeEnd = bedtimeEnd,
        type = type,
        totalSleepSeconds = totalSleepSeconds,
        timeInBedSeconds = timeInBedSeconds,
        awakeSeconds = awakeSeconds,
        lightSeconds = lightSeconds,
        deepSeconds = deepSeconds,
        remSeconds = remSeconds,
        efficiency = efficiency,
        latencySeconds = latencySeconds,
        restlessPeriods = restlessPeriods,
        averageBreath = averageBreath,
        averageHeartRate = averageHeartRate,
        averageHrv = averageHrv,
        lowestHeartRate = lowestHeartRate,
        sleepPhase5Min = sleepPhase5Min,
        createdAt = createdAt,
    )

fun OuraPersonalInfoEntity.toBackup(): BackupOuraPersonalInfo =
    BackupOuraPersonalInfo(
        id = id,
        ouraUserId = ouraUserId,
        age = age,
        weightKg = weightKg,
        heightMeters = heightMeters,
        biologicalSex = biologicalSex,
        email = email,
        capturedAt = capturedAt,
    )

// ---------- Backup → Entity Mappings ----------

fun BackupBiomarkerSnapshot.toEntity(): BiomarkerSnapshotEntity =
    BiomarkerSnapshotEntity(
        id = id,
        capturedAt = capturedAt,
        recoveryScore = recoveryScore,
        hrvMs = hrvMs,
        restingHeartRate = restingHeartRate,
        sleepPerformance = sleepPerformance,
        sleepTotalMinutes = sleepTotalMinutes,
        sleepRemMinutes = sleepRemMinutes,
        sleepDeepMinutes = sleepDeepMinutes,
        sleepLightMinutes = sleepLightMinutes,
        sleepAwakeMinutes = sleepAwakeMinutes,
        sleepDisturbances = sleepDisturbances,
        dayStrain = dayStrain,
        dayKilojoules = dayKilojoules,
        createdAt = createdAt,
        respiratoryRate = respiratoryRate,
        sleepConsistencyPercent = sleepConsistencyPercent,
        sleepEfficiencyPercent = sleepEfficiencyPercent,
        sleepNeedMinutes = sleepNeedMinutes,
        sleepDebtMinutes = sleepDebtMinutes,
        spo2Percent = spo2Percent,
        skinTempCelsius = skinTempCelsius,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        sleepCycleCount = sleepCycleCount,
    )

fun BackupWhoopWorkout.toEntity(): WhoopWorkoutEntity =
    WhoopWorkoutEntity(
        id = id,
        dateKey = dateKey,
        startMs = startMs,
        endMs = endMs,
        sportId = sportId,
        sportName = sportName,
        strain = strain,
        kilojoule = kilojoule,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        percentRecorded = percentRecorded,
        distanceMeter = distanceMeter,
        altitudeGainMeter = altitudeGainMeter,
        zoneZeroMilli = zoneZeroMilli,
        zoneOneMilli = zoneOneMilli,
        zoneTwoMilli = zoneTwoMilli,
        zoneThreeMilli = zoneThreeMilli,
        zoneFourMilli = zoneFourMilli,
        zoneFiveMilli = zoneFiveMilli,
        createdAt = createdAt,
    )

fun BackupOuraReadiness.toEntity(): OuraReadinessEntity =
    OuraReadinessEntity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        temperatureDeviation = temperatureDeviation,
        temperatureTrendDeviation = temperatureTrendDeviation,
        activityBalance = activityBalance,
        bodyTemperature = bodyTemperature,
        hrvBalance = hrvBalance,
        previousDayActivity = previousDayActivity,
        previousNight = previousNight,
        recoveryIndex = recoveryIndex,
        restingHeartRate = restingHeartRate,
        sleepBalance = sleepBalance,
        createdAt = createdAt,
    )

fun BackupOuraDailySleep.toEntity(): OuraDailySleepEntity =
    OuraDailySleepEntity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        deepSleepScore = deepSleepScore,
        efficiencyScore = efficiencyScore,
        latencyScore = latencyScore,
        remSleepScore = remSleepScore,
        restfulnessScore = restfulnessScore,
        timingScore = timingScore,
        totalSleepScore = totalSleepScore,
        createdAt = createdAt,
    )

fun BackupOuraActivity.toEntity(): OuraActivityEntity =
    OuraActivityEntity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        activeCalories = activeCalories,
        totalCalories = totalCalories,
        targetCalories = targetCalories,
        steps = steps,
        walkingDistanceMeters = walkingDistanceMeters,
        highActivitySeconds = highActivitySeconds,
        mediumActivitySeconds = mediumActivitySeconds,
        lowActivitySeconds = lowActivitySeconds,
        nonWearSeconds = nonWearSeconds,
        restingSeconds = restingSeconds,
        sedentarySeconds = sedentarySeconds,
        inactivityAlerts = inactivityAlerts,
        createdAt = createdAt,
    )

fun BackupOuraResilience.toEntity(): OuraResilienceEntity =
    OuraResilienceEntity(
        day = day,
        capturedAt = capturedAt,
        level = level,
        sleepRecovery = sleepRecovery,
        daytimeRecovery = daytimeRecovery,
        stress = stress,
        createdAt = createdAt,
    )

fun BackupOuraSleepDetail.toEntity(): OuraSleepDetailEntity =
    OuraSleepDetailEntity(
        id = id,
        day = day,
        capturedAt = capturedAt,
        bedtimeStart = bedtimeStart,
        bedtimeEnd = bedtimeEnd,
        type = type,
        totalSleepSeconds = totalSleepSeconds,
        timeInBedSeconds = timeInBedSeconds,
        awakeSeconds = awakeSeconds,
        lightSeconds = lightSeconds,
        deepSeconds = deepSeconds,
        remSeconds = remSeconds,
        efficiency = efficiency,
        latencySeconds = latencySeconds,
        restlessPeriods = restlessPeriods,
        averageBreath = averageBreath,
        averageHeartRate = averageHeartRate,
        averageHrv = averageHrv,
        lowestHeartRate = lowestHeartRate,
        sleepPhase5Min = sleepPhase5Min,
        createdAt = createdAt,
    )

fun BackupOuraPersonalInfo.toEntity(): OuraPersonalInfoEntity =
    OuraPersonalInfoEntity(
        id = id,
        ouraUserId = ouraUserId,
        age = age,
        weightKg = weightKg,
        heightMeters = heightMeters,
        biologicalSex = biologicalSex,
        email = email,
        capturedAt = capturedAt,
    )

// =========================================================================
// Schema v6 (Frank-Wunsch 2026-05-20): Tagebuch + Profil + Entropie-Followups
// =========================================================================

fun de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity.toBackup():
    BackupEntropyFollowup =
    BackupEntropyFollowup(
        id = id,
        entryId = entryId,
        rawText = rawText,
        improvedText = improvedText,
        isImproved = isImproved,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BackupEntropyFollowup.toEntity():
    de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity =
    de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity(
        id = id,
        entryId = entryId,
        rawText = rawText,
        improvedText = improvedText,
        isImproved = isImproved,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
