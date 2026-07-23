package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import com.pradeep.jarviscollector.model.DailyBriefEntity
import com.pradeep.jarviscollector.model.FactInsightEntity
import com.pradeep.jarviscollector.model.NotificationEntity
import com.pradeep.jarviscollector.model.UserActionEntity
import com.pradeep.jarviscollector.model.SyncDiagnosticsEntity
import com.pradeep.jarviscollector.model.LifecycleItemEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class InsightSyncResult {
    data class Success(
        val briefCount: Int,
        val todoCount: Int,
        val financialCount: Int,
        val fyiCount: Int,
        val prefCount: Int,
        val factCount: Int = 0,
        val notificationCount: Int = 0,
        val userActionCount: Int = 0,
        val financialInsightCount: Int = 0
    ) : InsightSyncResult()

    data class Failure(val error: String) : InsightSyncResult()
}

object InsightSyncService {

    private const val TAG = "InsightSyncService"

    private suspend fun writeDiagnostics(
        context: Context,
        entityType: String,
        startedAt: String?,
        completedAt: String?,
        downloaded: Int,
        inserted: Int,
        status: String,
        errorMessage: String?
    ) {
        try {
            val db = JarvisDatabase.getDatabase(context)
            db.syncDiagnosticsDao().upsert(
                SyncDiagnosticsEntity(
                    entityType = entityType,
                    syncStartedAt = startedAt,
                    syncCompletedAt = completedAt,
                    recordsDownloaded = downloaded,
                    recordsInserted = inserted,
                    status = status,
                    errorMessage = errorMessage
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write sync diagnostics for $entityType", e)
        }
    }

    private fun parseIsoTimestamp(rawDate: String): Long {
        if (rawDate.isBlank()) return 0L
        try {
            var normalized = rawDate.trim().replace(" ", "T")
            if (normalized.matches(Regex(".+[-+]\\d{2}"))) {
                normalized += ":00"
            }
            if (normalized.matches(Regex(".+[-+]\\d{4}"))) {
                val base = normalized.substring(0, normalized.length - 2)
                val suffix = normalized.substring(normalized.length - 2)
                normalized = "$base:$suffix"
            }
            return java.time.OffsetDateTime.parse(normalized).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.e(TAG, "OffsetDateTime parsing failed for: $rawDate", e)
        }

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(rawDate)
                if (date != null) return date.time
            } catch (e: Exception) {
                // continue
            }
        }
        return 0L
    }

    suspend fun syncInsights(
        context: Context
    ): InsightSyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting direct Supabase insights schema pull...")

            // 1. Fetch tables from Supabase REST endpoints
            val todosJson = JarvisInsightsClient.fetchTable("tasks", "jarvis_insights_schemav1")
            val financialJson = JarvisInsightsClient.fetchTable("financial_transactions", "jarvis_insights_schemav1")
            val fyiJson = JarvisInsightsClient.fetchTable("fyi_events")
            val preferencesJson = JarvisInsightsClient.fetchTable("user_preferences")
            val dailyBriefsJson = JarvisInsightsClient.fetchTable("daily_briefs")
            val factsJson = JarvisInsightsClient.fetchTable("information_items", "jarvis_insights_schemav1")
            val notificationsJson = null // Generated locally from signals / insights
            val userActionsJson = JarvisInsightsClient.fetchTable("user_actions")
            val financialInsightsJson = JarvisInsightsClient.fetchTable("financial_facts")
            val monthlySpendingJson = JarvisInsightsClient.fetchTable("monthly_spending_summary", "jarvis_insights_schemav1")
            val monthlyCategorySpendJson = JarvisInsightsClient.fetchTable("monthly_category_spend", "jarvis_insights_schemav1")
            val lifecycleJson = JarvisInsightsClient.fetchTable("lifecycle_items", "jarvis_insights_schemav1")
            val vaultCategoriesJson = JarvisInsightsClient.fetchTable("vault_categories", "jarvis_insights_schemav1")
                ?: JarvisInsightsClient.fetchTable("vault_categories", "jarvis_insights_schema")
            val vaultEntriesJson = JarvisInsightsClient.fetchTable("vault_entries", "jarvis_insights_schemav1")
                ?: JarvisInsightsClient.fetchTable("vault_entries", "jarvis_insights_schema")


            val db = JarvisDatabase.getDatabase(context)

            var todoCount = 0
            var financialCount = 0
            var fyiCount = 0
            var prefCount = 0
            var briefCount = 0
            var factCount = 0
            var notificationCount = 0
            var userActionCount = 0
            var financialInsightCount = 0

            db.withTransaction {
                // 1. Sync Todos
                val startTodos = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "TODOS", startTodos, null, 0, 0, "PENDING", null)
                if (todosJson != null) {
                    try {
                        val array = JSONArray(todosJson)
                        val list = mutableListOf<TodoEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            
                            // Map fields from jarvis_insights_schemav1.tasks
                            val rawDue = if (obj.isNull("due_datetime")) null else obj.getString("due_datetime")
                            val cleanDueDate = if (rawDue != null && rawDue.contains("T")) rawDue.split("T")[0] else rawDue

                            list.add(
                                TodoEntity(
                                    todo_id = obj.getString("id"),
                                    title = if (obj.isNull("title")) null else obj.getString("title"),
                                    description = if (obj.isNull("description")) null else obj.getString("description"),
                                    category = if (obj.isNull("source_type")) "General" else obj.getString("source_type"),
                                    priority = if (obj.isNull("priority")) "MEDIUM" else obj.getString("priority"),
                                    status = obj.optString("status", "OPEN"),
                                    due_date = cleanDueDate,
                                    source_signal_id = if (obj.isNull("route_id")) null else obj.getString("route_id"),
                                    source_agent = if (obj.isNull("created_by")) "JARVIS" else obj.getString("created_by"),
                                    confidence = 1.0,
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at"),
                                    reminder_datetime = if (obj.isNull("reminder_datetime")) null else obj.getString("reminder_datetime")
                                )
                            )
                        }
                        db.todoDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.todoDao().insertAll(list)
                            todoCount = list.size
                            
                            // Reconcile and restore exact alarms from reminder_datetime fields
                            try {
                                val reminderDao = db.reminderDao()
                                val now = System.currentTimeMillis()
                                for (todo in list) {
                                    val rawRem = todo.reminder_datetime
                                    val localRem = reminderDao.getById(todo.todo_id)
                                    if (!rawRem.isNullOrBlank()) {
                                        val targetTime = parseIsoTimestamp(rawRem)
                                        if (targetTime > now) {
                                            val remTitle = when (todo.priority?.uppercase(Locale.US)) {
                                                "CRITICAL", "URGENT" -> "⚠️ Urgent Task Alert"
                                                "HIGH" -> "🔔 High Priority Task"
                                                else -> "Jarvis Task Reminder"
                                            }
                                            val newRem = com.pradeep.jarviscollector.model.ReminderEntity(
                                                reminder_id = todo.todo_id,
                                                entity_type = "TODO",
                                                title = remTitle,
                                                message = todo.title ?: "Upcoming task deadline",
                                                scheduled_timestamp = targetTime,
                                                sound_type = "DEFAULT",
                                                action_route = "task_detail/${todo.todo_id}",
                                                action_payload = "{\"todo_id\":\"${todo.todo_id}\"}"
                                            )
                                            JarvisReminderManager.scheduleReminderLocally(context, newRem)
                                        } else {
                                            if (localRem != null) {
                                                JarvisReminderManager.cancelReminderLocally(context, todo.todo_id)
                                            }
                                        }
                                    } else {
                                        if (localRem != null) {
                                            JarvisReminderManager.cancelReminderLocally(context, todo.todo_id)
                                        }
                                    }
                                }
                            } catch (reException: Exception) {
                                Log.e(TAG, "Failed to reconcile reminders from synced tasks", reException)
                            }
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "TODOS", startTodos, complete, array.length(), todoCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing todos", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "TODOS", startTodos, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "TODOS", startTodos, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 2. Sync Financial Events (from jarvis_insights_schemav1 with authoritative fields)
                val startFin = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "FINANCIAL_EVENTS", startFin, null, 0, 0, "PENDING", null)
                if (financialJson != null) {
                    try {
                        val array = JSONArray(financialJson)
                        val list = mutableListOf<FinancialEventEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                FinancialEventEntity(
                                    financial_event_id = obj.optString("transaction_id", ""),
                                    merchant = if (obj.isNull("raw_narration")) null else obj.getString("raw_narration"),
                                    paid_to = if (obj.isNull("merchant")) null else obj.getString("merchant"),
                                    paid_from = if (obj.isNull("source_account")) null else obj.getString("source_account"),
                                    transaction_type = if (obj.isNull("direction")) null else obj.getString("direction"),
                                    payment_channel = if (obj.isNull("transaction_type")) null else obj.getString("transaction_type"),
                                    transaction_id = if (obj.isNull("reference_number")) null else obj.getString("reference_number"),
                                    amount = if (obj.isNull("amount")) null else obj.getDouble("amount"),
                                    currency = if (obj.isNull("currency")) null else obj.getString("currency"),
                                    category = if (obj.isNull("category")) null else obj.getString("category"),
                                    event_timestamp = if (obj.isNull("event_date")) null else obj.getString("event_date"),
                                    source_signal_id = if (obj.isNull("signal_route_id")) null else obj.getString("signal_route_id"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    subcategory = if (obj.isNull("subcategory")) null else obj.getString("subcategory"),
                                    is_self_transfer = if (obj.isNull("is_self_transfer")) null else obj.getBoolean("is_self_transfer")
                                )
                            )
                        }
                        db.financialEventDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.financialEventDao().insertAll(list)
                            financialCount = list.size
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FINANCIAL_EVENTS", startFin, complete, array.length(), financialCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing financial events", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FINANCIAL_EVENTS", startFin, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "FINANCIAL_EVENTS", startFin, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 3. Sync FYI Events
                val startFyi = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "FYI", startFyi, null, 0, 0, "PENDING", null)
                if (fyiJson != null) {
                    try {
                        val array = JSONArray(fyiJson)
                        val list = mutableListOf<FyiEventEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                FyiEventEntity(
                                    fyi_event_id = obj.optString("event_id", obj.optString("fyi_event_id", "")),
                                    title = if (obj.isNull("title")) null else obj.getString("title"),
                                    summary = if (obj.isNull("summary")) (if (obj.isNull("description")) null else obj.getString("description")) else obj.getString("summary"),
                                    category = if (obj.isNull("category")) null else obj.getString("category"),
                                    read_flag = if (obj.isNull("read_flag")) false else obj.getBoolean("read_flag"),
                                    status = if (obj.isNull("status")) "NEW" else obj.getString("status"),
                                    source_signal_id = if (obj.isNull("source_signal_id")) (if (obj.isNull("source")) null else obj.getString("source")) else obj.getString("source_signal_id"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        db.fyiEventDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.fyiEventDao().insertAll(list)
                            fyiCount = list.size
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FYI", startFyi, complete, array.length(), fyiCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing FYI events", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FYI", startFyi, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "FYI", startFyi, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 4. Sync Preferences
                val startPref = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "PREFERENCES", startPref, null, 0, 0, "PENDING", null)
                if (preferencesJson != null) {
                    try {
                        val array = JSONArray(preferencesJson)
                        val list = mutableListOf<UserPreferenceEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                UserPreferenceEntity(
                                    preference_key = obj.getString("preference_key"),
                                    preference_value = obj.optString("preference_value", ""),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        db.userPreferenceDao().deleteAll()
                        if (list.isNotEmpty()) {
                            for (pref in list) {
                                db.userPreferenceDao().insert(pref)
                            }
                            prefCount = list.size
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "PREFERENCES", startPref, complete, array.length(), prefCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing user preferences", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "PREFERENCES", startPref, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "PREFERENCES", startPref, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 5. Sync Daily Briefs
                val startBrief = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "BRIEF", startBrief, null, 0, 0, "PENDING", null)
                if (dailyBriefsJson != null) {
                    try {
                        val array = JSONArray(dailyBriefsJson)
                        val list = mutableListOf<DailyBriefEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                DailyBriefEntity(
                                    id = obj.optString("brief_id", obj.optString("id", "")),
                                    generatedAt = obj.optString("generated_at", obj.optString("generatedAt", "")),
                                    version = obj.optString("version", "1.0"),
                                    itemsJson = obj.optString("content", obj.optString("items_json", obj.optString("itemsJson", "[]"))),
                                    briefType = if (obj.isNull("brief_type")) null else obj.optString("brief_type"),
                                    todoCount = if (obj.isNull("todo_count")) null else obj.optInt("todo_count"),
                                    fyiCount = if (obj.isNull("fyi_count")) null else obj.optInt("fyi_count"),
                                    factCount = if (obj.isNull("fact_count")) null else obj.optInt("fact_count"),
                                    payloadJson = if (obj.isNull("payload_json")) null else obj.optString("payload_json")
                                )
                            )
                        }
                        db.dailyBriefDao().deleteAll()
                        if (list.isNotEmpty()) {
                            for (brief in list) {
                                db.dailyBriefDao().insert(brief)
                            }
                            briefCount = list.size
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "BRIEF", startBrief, complete, array.length(), briefCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing daily brief", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "BRIEF", startBrief, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "BRIEF", startBrief, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 6. Sync Facts
                val startFacts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "FACTS", startFacts, null, 0, 0, "PENDING", null)
                if (factsJson != null) {
                    try {
                        val array = JSONArray(factsJson)
                        val list = mutableListOf<FactInsightEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val rawPayloadObj = obj.optJSONObject("raw_payload")
                            val rawPayloadStr = rawPayloadObj?.toString() ?: obj.optString("raw_payload", "")

                            list.add(
                                FactInsightEntity(
                                    id = obj.getString("id"),
                                    title = obj.optString("title", "Signal"),
                                    summary = obj.optString("summary", ""),
                                    category = obj.optString("category", "GENERAL"),
                                    priority = obj.optString("importance_level", "EPHEMERAL"),
                                    created_at = obj.optString("created_at", ""),
                                    read_flag = false, // Locally managed or default unread
                                    status = obj.optString("processing_path", "RULE_BASED"),
                                    source = rawPayloadStr
                                )
                            )
                        }
                        db.factInsightDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.factInsightDao().insertAll(list)
                            factCount = list.size
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FACTS", startFacts, complete, array.length(), factCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing facts", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FACTS", startFacts, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "FACTS", startFacts, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 7. Sync Notifications
                val startNotif = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "NOTIFICATIONS", startNotif, null, 0, 0, "PENDING", null)
                // Generated locally, successfully sync with 0 records
                val completeNotif = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "NOTIFICATIONS", startNotif, completeNotif, 0, 0, "SUCCESS", "Local-only / generated from signals")

                // 8. Sync User Actions
                val startActions = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "USER_ACTIONS", startActions, null, 0, 0, "PENDING", null)
                if (userActionsJson != null) {
                    try {
                        val array = JSONArray(userActionsJson)
                        val list = mutableListOf<UserActionEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                UserActionEntity(
                                    action_id = obj.getString("action_id"),
                                    entity_type = if (obj.isNull("entity_type")) null else obj.getString("entity_type"),
                                    entity_id = if (obj.isNull("entity_id")) null else obj.getString("entity_id"),
                                    action = if (obj.isNull("action")) null else obj.getString("action"),
                                    action_timestamp = if (obj.isNull("action_timestamp")) null else obj.getString("action_timestamp"),
                                    metadata = if (obj.isNull("metadata")) null else obj.get("metadata").toString()
                                )
                            )
                        }
                        db.userActionDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.userActionDao().insertAll(list)
                            userActionCount = list.size
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "USER_ACTIONS", startActions, complete, array.length(), userActionCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing user actions", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "USER_ACTIONS", startActions, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "USER_ACTIONS", startActions, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 9. Sync Financial Insights
                val startFinIns = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "FINANCIAL", startFinIns, null, 0, 0, "PENDING", null)
                if (financialInsightsJson != null) {
                    try {
                        val array = JSONArray(financialInsightsJson)
                        val list = mutableListOf<com.pradeep.jarviscollector.model.FinancialInsightEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                com.pradeep.jarviscollector.model.FinancialInsightEntity(
                                    id = obj.getString("id"),
                                    title = if (obj.isNull("merchant_canonical")) (if (obj.isNull("fact_type")) "Financial Alert" else obj.getString("fact_type")) else obj.getString("merchant_canonical"),
                                    description = "Category: " + (if (obj.isNull("category")) "General" else obj.getString("category")),
                                    type = if (obj.isNull("fact_type")) "unusual" else obj.getString("fact_type"),
                                    amount = if (obj.isNull("amount")) 0.0 else obj.getDouble("amount"),
                                    dueDate = if (obj.isNull("event_date")) null else obj.getString("event_date"),
                                    priority = "MEDIUM",
                                    confidence = if (obj.isNull("classification_confidence")) "HIGH" else obj.getString("classification_confidence"),
                                    status = "PENDING",
                                    createdAt = if (obj.isNull("created_at")) null else obj.getString("created_at")
                                )
                            )
                        }
                        db.financialInsightDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.financialInsightDao().upsertAll(list)
                            financialInsightCount = list.size

                            val highPriorityFinancialInsights = list.filter { it.priority?.uppercase() == "HIGH" && it.status?.uppercase() == "PENDING" }
                            if (highPriorityFinancialInsights.isNotEmpty()) {
                                val newNotifications = highPriorityFinancialInsights.map { insight ->
                                    NotificationEntity(
                                        id = "fin-notif-${insight.id}",
                                        title = "Urgent Financial Action: ${insight.title}",
                                        message = insight.description ?: "High-priority financial alert requires your review.",
                                        type = "FINANCIAL",
                                        priority = "HIGH",
                                        status = "NEW",
                                        created_at = insight.createdAt ?: java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
                                        action_route = "finance",
                                        action_payload = "{\"insight_id\":\"${insight.id}\"}",
                                        read_flag = false
                                    )
                                }
                                db.notificationDao().insertAll(newNotifications)
                            }
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FINANCIAL", startFinIns, complete, array.length(), financialInsightCount, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing financial insights", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "FINANCIAL", startFinIns, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "FINANCIAL", startFinIns, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 9. Sync Monthly Spending Summary
                if (monthlySpendingJson != null) {
                    try {
                        val array = JSONArray(monthlySpendingJson)
                        val list = mutableListOf<com.pradeep.jarviscollector.model.MonthlySpendingSummaryEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                com.pradeep.jarviscollector.model.MonthlySpendingSummaryEntity(
                                    month_key = obj.optString("month_key", ""),
                                    total_income = if (obj.isNull("total_income")) null else obj.getDouble("total_income"),
                                    total_expense = if (obj.isNull("total_expense")) null else obj.getDouble("total_expense"),
                                    total_transfers = if (obj.isNull("total_transfers")) null else obj.getDouble("total_transfers"),
                                    net_cashflow = if (obj.isNull("net_cashflow")) null else obj.getDouble("net_cashflow"),
                                    transaction_count = if (obj.isNull("transaction_count")) null else obj.getInt("transaction_count")
                                )
                            )
                        }
                        db.monthlySpendingSummaryDao().deleteAll()
                        if (list.isNotEmpty()) db.monthlySpendingSummaryDao().insertAll(list)
                        Log.d(TAG, "Synced ${list.size} monthly spending summary rows")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing monthly spending summary", e)
                    }
                }

                // 10. Sync Monthly Category Spend
                if (monthlyCategorySpendJson != null) {
                    try {
                        val array = JSONArray(monthlyCategorySpendJson)
                        val list = mutableListOf<com.pradeep.jarviscollector.model.MonthlyCategorySpendEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                com.pradeep.jarviscollector.model.MonthlyCategorySpendEntity(
                                    month_key = obj.optString("month_key", ""),
                                    category = obj.optString("category", "Other"),
                                    amount = if (obj.isNull("amount")) null else obj.getDouble("amount"),
                                    transaction_count = if (obj.isNull("transaction_count")) null else obj.getInt("transaction_count")
                                )
                            )
                        }
                        db.monthlyCategorySpendDao().deleteAll()
                        if (list.isNotEmpty()) db.monthlyCategorySpendDao().insertAll(list)
                        Log.d(TAG, "Synced ${list.size} monthly category spend rows")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing monthly category spend", e)
                    }
                }

                // 11. Sync Lifecycle Items
                val startLifecycle = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                writeDiagnostics(context, "LIFECYCLE_ITEMS", startLifecycle, null, 0, 0, "PENDING", null)
                if (lifecycleJson != null) {
                    try {
                        val array = JSONArray(lifecycleJson)
                        val list = mutableListOf<LifecycleItemEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                LifecycleItemEntity(
                                    id = obj.getString("id"),
                                    domain = if (obj.isNull("domain")) null else obj.getString("domain"),
                                    title = if (obj.isNull("title")) null else obj.getString("title"),
                                    description = if (obj.isNull("description")) null else obj.getString("description"),
                                    schedule_type = if (obj.isNull("schedule_type")) null else obj.getString("schedule_type"),
                                    interval_days = if (obj.isNull("interval_days")) null else obj.getInt("interval_days"),
                                    next_occurrence_date = if (obj.isNull("next_occurrence_date")) null else obj.getString("next_occurrence_date"),
                                    reminder_offset_days = if (obj.isNull("reminder_offset_days")) null else obj.getInt("reminder_offset_days"),
                                    last_promoted_date = if (obj.isNull("last_promoted_date")) null else obj.getString("last_promoted_date"),
                                    last_todo_id = if (obj.isNull("last_todo_id")) null else obj.getString("last_todo_id"),
                                    status = if (obj.isNull("status")) null else obj.getString("status"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        db.lifecycleItemDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.lifecycleItemDao().insertAll(list)
                        }
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "LIFECYCLE_ITEMS", startLifecycle, complete, array.length(), list.size, "SUCCESS", null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing lifecycle items", e)
                        val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                        writeDiagnostics(context, "LIFECYCLE_ITEMS", startLifecycle, complete, 0, 0, "FAILURE", e.message ?: e.toString())
                    }
                } else {
                    val complete = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    writeDiagnostics(context, "LIFECYCLE_ITEMS", startLifecycle, complete, 0, 0, "FAILURE", "Supabase returned null/error payload")
                }

                // 12. Sync Vault Categories
                if (vaultCategoriesJson != null) {
                    try {
                        val array = JSONArray(vaultCategoriesJson)
                        val list = mutableListOf<com.pradeep.jarviscollector.model.VaultCategoryEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                com.pradeep.jarviscollector.model.VaultCategoryEntity(
                                    vault_category_id = obj.getString("vault_category_id"),
                                    category_name = obj.getString("category_name"),
                                    display_order = if (obj.isNull("display_order")) null else obj.getInt("display_order"),
                                    icon = if (obj.isNull("icon")) null else obj.getString("icon"),
                                    color = if (obj.isNull("color")) null else obj.getString("color"),
                                    is_active = if (obj.isNull("is_active")) null else obj.getBoolean("is_active"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        if (list.isNotEmpty()) {
                            db.vaultCategoryDao().deleteAll()
                            db.vaultCategoryDao().insertAll(list)
                        }
                        Log.d(TAG, "Synced ${list.size} vault category rows")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing vault categories", e)
                    }
                }
                com.pradeep.jarviscollector.repository.VaultRepository.seedDefaultCategoriesIfEmpty(context)

                // 13. Sync Vault Entries
                if (vaultEntriesJson != null) {
                    try {
                        val array = JSONArray(vaultEntriesJson)
                        val list = mutableListOf<com.pradeep.jarviscollector.model.VaultEntryEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                com.pradeep.jarviscollector.model.VaultEntryEntity(
                                    vault_entry_id = obj.getString("vault_entry_id"),
                                    vault_category_id = obj.getString("vault_category_id"),
                                    parent_entry_id = if (obj.isNull("parent_entry_id")) null else obj.getString("parent_entry_id"),
                                    owner = if (obj.isNull("owner")) null else obj.getString("owner"),
                                    title = obj.getString("title"),
                                    sub_category = if (obj.isNull("sub_category")) null else obj.getString("sub_category"),
                                    location = if (obj.isNull("location")) null else obj.getString("location"),
                                    access_information = if (obj.isNull("access_information")) null else obj.getString("access_information"),
                                    notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                                    sort_order = if (obj.isNull("sort_order")) null else obj.getInt("sort_order"),
                                    is_active = if (obj.isNull("is_active")) null else obj.getBoolean("is_active"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        db.vaultEntryDao().deleteAll()
                        if (list.isNotEmpty()) db.vaultEntryDao().insertAll(list)
                        Log.d(TAG, "Synced ${list.size} vault entry rows")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing vault entries", e)
                    }
                }
            }

            Log.d(
                TAG,
                "Sync complete: briefs=$briefCount, todos=$todoCount, financial=$financialCount, fyi=$fyiCount, preferences=$prefCount, facts=$factCount, notifications=$notificationCount, userActions=$userActionCount, finInsights=$financialInsightCount"
            )

            InsightSyncResult.Success(
                briefCount = briefCount,
                todoCount = todoCount,
                financialCount = financialCount,
                fyiCount = fyiCount,
                prefCount = prefCount,
                factCount = factCount,
                notificationCount = notificationCount,
                userActionCount = userActionCount,
                financialInsightCount = financialInsightCount
            )

        } catch (ex: Exception) {
            Log.e(TAG, "Error in syncInsights: ${ex.message}", ex)
            InsightSyncResult.Failure(ex.message ?: "Unknown error")
        }
    }
}
