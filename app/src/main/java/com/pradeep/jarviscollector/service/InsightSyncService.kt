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

    suspend fun syncInsights(
        context: Context
    ): InsightSyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting direct Supabase insights schema pull...")

            // 1. Fetch tables from Supabase REST endpoints
            val todosJson = JarvisInsightsClient.fetchTable("todo_items")
            val financialJson = JarvisInsightsClient.fetchTable("financial_events")
            val fyiJson = JarvisInsightsClient.fetchTable("fyi_events")
            val preferencesJson = JarvisInsightsClient.fetchTable("user_preferences")
            val dailyBriefsJson = JarvisInsightsClient.fetchTable("daily_briefs")
            val factsJson = JarvisInsightsClient.fetchTable("facts")
            val notificationsJson = null // Generated locally from signals / insights
            val userActionsJson = JarvisInsightsClient.fetchTable("user_actions")
            val financialInsightsJson = JarvisInsightsClient.fetchTable("financial_facts")

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
                            list.add(
                                TodoEntity(
                                    todo_id = obj.getString("todo_id"),
                                    title = if (obj.isNull("title")) null else obj.getString("title"),
                                    description = if (obj.isNull("description")) null else obj.getString("description"),
                                    priority = if (obj.isNull("priority")) null else obj.getString("priority"),
                                    status = obj.optString("status", "OPEN"),
                                    due_date = if (obj.isNull("due_date")) null else obj.getString("due_date"),
                                    source_signal_id = if (obj.isNull("source_signal_id")) (if (obj.isNull("source_reference")) null else obj.getString("source_reference")) else obj.getString("source_signal_id"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        db.todoDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.todoDao().insertAll(list)
                            todoCount = list.size
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

                // 2. Sync Financial Events
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
                                    financial_event_id = obj.optString("id", obj.optString("financial_event_id", "")),
                                    merchant = if (obj.isNull("title")) (if (obj.isNull("paid_to")) (if (obj.isNull("merchant")) null else obj.getString("merchant")) else obj.getString("paid_to")) else obj.getString("title"),
                                    amount = if (obj.isNull("amount")) null else obj.getDouble("amount"),
                                    currency = if (obj.isNull("currency")) null else obj.getString("currency"),
                                    category = if (obj.isNull("category")) null else obj.getString("category"),
                                    status = if (obj.isNull("status")) "PENDING" else obj.getString("status"),
                                    event_timestamp = if (obj.isNull("event_date")) (if (obj.isNull("event_timestamp")) null else obj.getString("event_timestamp")) else obj.getString("event_date"),
                                    source_signal_id = if (obj.isNull("source_signal_id")) null else obj.getString("source_signal_id"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("created_at")) null else obj.getString("created_at")
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
                            list.add(
                                FactInsightEntity(
                                    id = obj.optString("fact_id", obj.optString("id", "")),
                                    title = obj.optString("fact_type", obj.optString("title", "")),
                                    summary = obj.optString("fact_value", obj.optString("summary", "")),
                                    category = obj.optString("category", "General"),
                                    priority = obj.optString("priority", "MEDIUM"),
                                    created_at = obj.optString("created_at", ""),
                                    read_flag = obj.optBoolean("read_flag", false) || (obj.optString("status", "").uppercase() == "ACKNOWLEDGED"),
                                    status = obj.optString("status", "NEW"),
                                    source = obj.optString("source_agent", obj.optString("source", ""))
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
