package com.nems.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "supplement_entries",
    indices = [
        Index("date"),
        Index("supplementId"),
        Index("stackSectionId"),
        Index(value = ["date", "supplementId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SupplementEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplementId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StackSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["stackSectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SupplementEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val supplementId: String,
    val stackSectionId: String,
    val taken: Boolean = false,
    val takenTimestamp: String? = null,
    val notes: String? = null,
)
