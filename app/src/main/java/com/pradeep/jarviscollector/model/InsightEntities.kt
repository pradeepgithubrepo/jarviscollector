package com.pradeep.jarviscollector.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val todo_id: String,
    val title: String?,
    val description: String?,
    val category: String?,           // Added from Supabase `todo_items.category`
    val priority: String?,           // Supports CRITICAL, HIGH, MEDIUM, LOW
    val status: String,              // OPEN, COMPLETED, SNOOZED, DISMISSED
    val due_date: String?,
    val source_signal_id: String?,   // Extracted from Supabase JSON dict `source_reference`
    val source_agent: String?,       // Added from Supabase `todo_items.source_agent`
    val confidence: Double?,         // Added from Supabase `todo_items.confidence`
    val created_at: String?,
    val updated_at: String?,
    val reminder_datetime: String?   // Added to persist Supabase reminder alerts
)



@Entity(tableName = "financial_events")
data class FinancialEventEntity(
    @PrimaryKey val financial_event_id: String,
    val merchant: String?,          // Legacy: raw title sentence from Supabase
    val paid_to: String?,           // Canonical merchant/payee name
    val paid_from: String?,         // Source account
    val transaction_type: String?,  // "debit" or "credit" — authoritative income/expense flag
    val payment_channel: String?,   // "UPI", "NEFT", etc.
    val transaction_id: String?,    // Bank reference number
    val amount: Double?,
    val currency: String?,
    val category: String?,
    val event_timestamp: String?,
    val source_signal_id: String?,
    val created_at: String?,
    val subcategory: String?,
    val is_self_transfer: Boolean?
    // Note: status and updated_at removed — they do not exist on Supabase financial_events
)

@Entity(tableName = "fyi_events")
data class FyiEventEntity(
    @PrimaryKey val fyi_event_id: String,
    val title: String?,
    val summary: String?,
    val category: String?,
    val read_flag: Boolean?,
    val status: String?,
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
    val itemsJson: String,
    val briefType: String?,       // "MORNING" or "EVENING"
    val todoCount: Int?,
    val fyiCount: Int?,
    val factCount: Int?,
    val payloadJson: String?      // JSON metadata blob with sections
)

@Entity(tableName = "facts")
data class FactInsightEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val summary: String?,
    val category: String?,
    val priority: String?,
    val created_at: String?,
    val read_flag: Boolean?,
    val status: String?,
    val source: String?
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val message: String?,
    val type: String?,
    val priority: String?,
    val status: String?,
    val created_at: String?,
    val action_route: String?,
    val action_payload: String?,
    val read_flag: Boolean?
)

@Entity(tableName = "financial_insights")
data class FinancialInsightEntity(
    @PrimaryKey
    val id: String,
    val title: String?,
    val description: String?,
    val type: String?,
    val amount: Double?,
    val dueDate: String?,
    val priority: String?,
    val confidence: String?,
    val status: String?,
    val createdAt: String?
)

@Entity(tableName = "sync_diagnostics")
data class SyncDiagnosticsEntity(
    @PrimaryKey val entityType: String,
    val syncStartedAt: String?,
    val syncCompletedAt: String?,
    val recordsDownloaded: Int,
    val recordsInserted: Int,
    val status: String,
    val errorMessage: String?
)

@Entity(tableName = "local_reminders")
data class ReminderEntity(
    @PrimaryKey val reminder_id: String, // Maps to target entity ID (e.g. todo_id)
    val entity_type: String,              // "TODO", "FINANCIAL", "DAILY_BRIEF"
    val title: String,
    val message: String,
    val scheduled_timestamp: Long,
    val sound_type: String,               // DEFAULT, GENTLE, URGENT, SILENT
    val action_route: String,             // Navigation target path
    val action_payload: String?           // JSON details metadata
)

// Pre-aggregated monthly financial totals from Supabase (jarvis_insights_schemav1)
@Entity(tableName = "monthly_spending_summary", primaryKeys = ["month_key"])
data class MonthlySpendingSummaryEntity(
    val month_key: String,           // e.g. "2026-07"
    val total_income: Double?,
    val total_expense: Double?,
    val total_transfers: Double?,
    val net_cashflow: Double?,
    val transaction_count: Int?
)

// Per-category spend per month from Supabase (jarvis_insights_schemav1)
@Entity(tableName = "monthly_category_spend", primaryKeys = ["month_key", "category"])
data class MonthlyCategorySpendEntity(
    val month_key: String,
    val category: String,
    val amount: Double?,
    val transaction_count: Int?
)

@Entity(tableName = "lifecycle_items")
data class LifecycleItemEntity(
    @PrimaryKey val id: String,
    val domain: String?,
    val title: String?,
    val description: String?,
    val schedule_type: String?,
    val interval_days: Int?,
    val next_occurrence_date: String?,
    val reminder_offset_days: Int?,
    val last_promoted_date: String?,
    val last_todo_id: String?,
    val status: String?,
    val created_at: String?,
    val updated_at: String?
)

@Entity(tableName = "vault_categories")
data class VaultCategoryEntity(
    @PrimaryKey val vault_category_id: String,
    val category_name: String,
    val display_order: Int?,
    val icon: String?,
    val color: String?,
    val is_active: Boolean?,
    val created_at: String?,
    val updated_at: String?
)

@Entity(tableName = "vault_entries")
data class VaultEntryEntity(
    @PrimaryKey val vault_entry_id: String,
    val vault_category_id: String,
    val parent_entry_id: String?,
    val owner: String?,
    val title: String,
    val sub_category: String?,
    val location: String?,
    val access_information: String?,
    val notes: String?,
    val sort_order: Int?,
    val is_active: Boolean?,
    val created_at: String?,
    val updated_at: String?
)


