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
    // Frank-Wunsch 2026-05-09: Methoden koennen mehrere Kategorien gleichzeitig
    // adressieren — Laufen wirkt z.B. mental + koerperlich + emotional. targetCategory
    // bleibt die primaere (fuer DAO-Filter und Repertoire-Sortierung), additionalCategories
    // listet die weiteren betroffenen Bereiche. Bestaetigte Methoden mit breiter
    // Wirkung erhalten in ProcessEntryUseCase einen staerkeren Prio-Boost (siehe
    // PRIORITY_DOCTRINE — Multi-Cat-Methoden = Wellbeing-First-Hebel).
    val additionalCategories: List<EntropyCategory> = emptyList(),
    val confidence: Int,                     // 0..100
    val successCount: Int,
    val attemptCount: Int,
    val avgBiomarkerImpact: String?,
    val avgFeltImpact: Double?,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceHypothesisIds: List<String>,
    // Frank-Wunsch 2026-05-09: Methoden koennen jetzt manuell hinzugefuegt werden
    // (Mic + KI-Verbesserung). manualSource markiert solche Eintraege, damit sie
    // beim Repertoire-Sort nicht durch Hypothesen-Aggregation ueberschrieben werden.
    val manualSource: Boolean = false,
)
