package com.bestjournal.app.domain.model

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

data class Advice(
    val title: String,
    val description: String,
    val priority: AdvicePriority,
    val connection: String,          // cross-category link description
    val derivation: List<DerivationEntry> = emptyList()
)

data class DerivationEntry(
    val date: String,
    val summary: String
)

data class TopAction(
    val title: String,
    val description: String
)

enum class AdvicePriority(val label: String) {
    HIGH("hoch"),
    MEDIUM("mittel"),
    LOW("niedrig")
}
