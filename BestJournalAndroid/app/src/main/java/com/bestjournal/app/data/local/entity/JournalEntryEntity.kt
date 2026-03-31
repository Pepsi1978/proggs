package com.bestjournal.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rawText: String,
    val improvedText: String?,
    val isImproved: Boolean,
    val displayText: String,
    val audioDurationSeconds: Int,
    val moodTag: String?,
    val entropyScore: Float?,
    val adviceCategoryTags: String?,
    val summary: String? = null,
    val title: String? = null,
    val isSynced: Boolean = false
)
