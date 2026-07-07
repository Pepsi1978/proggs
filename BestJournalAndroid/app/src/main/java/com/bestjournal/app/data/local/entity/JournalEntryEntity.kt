package com.bestjournal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries", indices = [Index("timestamp"), Index("isSynced")])
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rawText: String,
    val improvedText: String?,
    val isImproved: Boolean,
    val displayText: String,
    val audioDurationSeconds: Int,
    val moodTag: String?,
    val adviceCategoryTags: String?,
    val summary: String? = null,
    val title: String? = null,
    val followUpText: String? = null,
    val isSynced: Boolean = false,
)
