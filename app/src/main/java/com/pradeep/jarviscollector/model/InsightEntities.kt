package com.pradeep.jarviscollector.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val priority: String?,
    val status: String,
    val completedAt: String?,
    val snoozeCount: Int,
    val updatedAt: Long
)

@Entity(tableName = "financial_events")
data class FinancialEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double?,
    val type: String,
    val dueDate: String?,
    val status: String?,
    val description: String?
)

@Entity(tableName = "fyi_events")
data class FyiEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val category: String,
    val timestamp: String
)

@Entity(tableName = "daily_briefs")
data class DailyBriefEntity(
    @PrimaryKey val id: String,
    val generatedAt: String,
    val version: String,
    val itemsJson: String
)
