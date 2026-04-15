package com.nems.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplements")
data class SupplementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String,
    val capsuleCount: Int,
    val unit: String,
    val solubility: String,
    val frequency: String,
    val defaultStackSectionId: String,
    val diarrhoeaRisk: Boolean = false,
    val isPowder: Boolean = false,
    val alternatesWith: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
)
