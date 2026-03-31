package com.bestjournal.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "advice_blocks")
data class AdviceBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val entropyLevel: Float,
    val categorySummary: String,
    val adviceJson: String,
    val overallAnalysis: String,
    val lastUpdated: Long,
    val basedOnEntryCount: Int
)
