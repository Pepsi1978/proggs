package com.entropyjournal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "retrospective_summaries")
data class RetrospectiveSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // WEEKLY, MONTHLY, YEARLY
    val periodLabel: String, // e.g. "KW 14 · 2026", "März 2026", "2025"
    val startDate: Long, // epoch millis
    val endDate: Long, // epoch millis
    val title: String,
    val summaryText: String,
    val periodIndex: Int, // week-of-month (1-5), month (1-12), or year
    val createdAt: Long = System.currentTimeMillis(),
)
