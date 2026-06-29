package com.pradeep.jarviscollector.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val todo_id: String,
    val title: String?,
    val description: String?,
    val priority: String?,
    val status: String,
    val due_date: String?,
    val source_signal_id: String?,
    val created_at: String?,
    val updated_at: String?
)

@Entity(tableName = "financial_events")
data class FinancialEventEntity(
    @PrimaryKey val financial_event_id: String,
    val merchant: String?,
    val amount: Double?,
    val currency: String?,
    val category: String?,
    val status: String?,
    val event_timestamp: String?,
    val source_signal_id: String?,
    val created_at: String?,
    val updated_at: String?
)

@Entity(tableName = "fyi_events")
data class FyiEventEntity(
    @PrimaryKey val fyi_event_id: String,
    val title: String?,
    val summary: String?,
    val category: String?,
    val read_flag: Boolean?,
    val source_signal_id: String?,
    val created_at: String?,
    val updated_at: String?
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val preference_key: String,
    val preference_value: String?,
    val updated_at: String?
)

@Entity(tableName = "user_actions")
data class UserActionEntity(
    @PrimaryKey val action_id: String,
    val entity_type: String?,
    val entity_id: String?,
    val action: String?,
    val action_timestamp: String?,
    val metadata: String? // JSON string
)



@Entity(tableName = "daily_briefs")
data class DailyBriefEntity(
    @PrimaryKey val id: String,
    val generatedAt: String,
    val version: String,
    val itemsJson: String
)
