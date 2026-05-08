package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.ScientistRole

@Entity(tableName = "scientist_sessions")
data class ScientistSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isArchived: Boolean,
)

@Entity(tableName = "scientist_messages")
data class ScientistMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: ScientistRole,
    val content: String,
    val createdAt: Long,
    val attachedHypothesisIds: List<String>,
)

@Entity(tableName = "hypothesis_messages")
data class HypothesisMessageEntity(
    @PrimaryKey val id: String,
    val hypothesisId: String,
    val role: ScientistRole,
    val content: String,
    val createdAt: Long,
)

@Entity(tableName = "hypotheses")
data class HypothesisEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val rationale: String,
    val createdAt: Long,
    val plannedStartDate: Long,
    val plannedEndDate: Long,
    val actualStartDate: Long?,
    val actualEndDate: Long?,
    val status: HypothesisStatus,
    val outcome: HypothesisOutcome?,
    val outcomeNotes: String?,
    val biomarkerBeforeId: String?,
    val biomarkerAfterId: String?,
    val felltEntropyChange: Int?,
    val relatedEntryIds: List<String>,
)

@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val targetCategory: EntropyCategory,
    val confidence: Int,                     // 0..100
    val successCount: Int,
    val attemptCount: Int,
    val avgBiomarkerImpact: String?,
    val avgFeltImpact: Double?,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceHypothesisIds: List<String>,
)
