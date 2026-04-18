package com.bestjournal.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Indices accelerate the two most frequent DAO queries:
//   WHERE type = :type ORDER BY startDate DESC
//   WHERE type = :type AND startDate BETWEEN :a AND :b
@Entity(tableName = "retrospective_summaries", indices = [Index(value = ["type", "startDate"])])
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
