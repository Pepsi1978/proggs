package com.bestjournal.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_follow_ups",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["entryId", "createdAt"], unique = true),
    ],
)
data class EntryFollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val rawText: String = text,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
)
