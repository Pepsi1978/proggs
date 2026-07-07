package com.bestjournal.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AdviceBlock(
    val id: Long = 0,
    val categoryName: String,
    val categoryIcon: String,        // Material icon name (e.g. "bedtime")
    val categoryColor: String,       // Hex color (e.g. "#6C63FF")
    val entropyLevel: Float,         // 0.0 to 1.0
    val categorySummary: String,
    val advices: List<Advice>,
    val overallAnalysis: String,
    val topActions: List<TopAction> = emptyList(),
    val lastUpdated: Long,
    val basedOnEntryCount: Int
)

@Immutable
data class Advice(
    val title: String,
    val description: String,
    val priority: AdvicePriority,
    val connection: String,          // cross-category link description
    val derivation: List<DerivationEntry> = emptyList()
)

@Immutable
data class DerivationEntry(
    val date: String,
    val summary: String
)

@Immutable
data class TopAction(
    val title: String,
    val description: String,
    val detailedDescription: String = ""
)

enum class AdvicePriority(val label: String) {
    HIGH("hoch"),
    MEDIUM("mittel"),
    LOW("niedrig")
}
