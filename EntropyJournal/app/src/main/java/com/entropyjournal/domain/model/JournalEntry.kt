package com.entropyjournal.domain.model

data class JournalEntry(
    val id: Long = 0,
    val timestamp: Long,
    val rawText: String,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
    val displayText: String,
    val audioDurationSeconds: Int,
    val moodTag: String? = null,        // "positiv", "neutral", "belastend"
    val entropyScore: Float? = null,     // 0.0 to 1.0
    val adviceCategoryTags: String? = null, // comma-separated
    val summary: String? = null,
    val title: String? = null,
    val isSynced: Boolean = false
)
