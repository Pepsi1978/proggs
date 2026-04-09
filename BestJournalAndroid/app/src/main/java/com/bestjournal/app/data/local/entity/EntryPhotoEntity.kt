package com.bestjournal.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_photos",
    foreignKeys =
        [
            ForeignKey(
                entity = JournalEntryEntity::class,
                parentColumns = ["id"],
                childColumns = ["entryId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("entryId")],
)
data class EntryPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val filePath: String,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "0") val isVideo: Boolean = false,
)
