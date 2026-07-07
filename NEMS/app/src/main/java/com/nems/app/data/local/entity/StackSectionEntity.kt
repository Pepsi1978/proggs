package com.nems.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stack_sections")
data class StackSectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val timeLabel: String,
    val instruction: String,
)
