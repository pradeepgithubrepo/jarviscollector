package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed class InsightSyncResult {
    data class Success(
        val briefCount: Int,
        val todoCount: Int,
        val financialCount: Int,
        val fyiCount: Int,
        val prefCount: Int
    ) : InsightSyncResult()

    data class Failure(val error: String) : InsightSyncResult()
}

object InsightSyncService {

    private const val TAG = "InsightSyncService"

    suspend fun syncInsights(
        context: Context
    ): InsightSyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting direct Supabase insights schema pull...")

            // 1. Fetch tables from Supabase REST endpoints
            val todosJson = JarvisInsightsClient.fetchTable("todos")
            val financialJson = JarvisInsightsClient.fetchTable("financial_events")
            val fyiJson = JarvisInsightsClient.fetchTable("fyi_events")
            val preferencesJson = JarvisInsightsClient.fetchTable("user_preferences")

            val db = JarvisDatabase.getDatabase(context)

            var todoCount = 0
            var financialCount = 0
            var fyiCount = 0
            var prefCount = 0

            db.withTransaction {
                // 1. Sync Todos
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
                                    source_signal_id = if (obj.isNull("source_signal_id")) null else obj.getString("source_signal_id"),
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
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing todos", e)
                    }
                }

                // 2. Sync Financial Events
                if (financialJson != null) {
                    try {
                        val array = JSONArray(financialJson)
                        val list = mutableListOf<FinancialEventEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                FinancialEventEntity(
                                    financial_event_id = obj.getString("financial_event_id"),
                                    merchant = if (obj.isNull("merchant")) null else obj.getString("merchant"),
                                    amount = if (obj.isNull("amount")) null else obj.getDouble("amount"),
                                    currency = if (obj.isNull("currency")) null else obj.getString("currency"),
                                    category = if (obj.isNull("category")) null else obj.getString("category"),
                                    status = if (obj.isNull("status")) null else obj.getString("status"),
                                    event_timestamp = if (obj.isNull("event_timestamp")) null else obj.getString("event_timestamp"),
                                    source_signal_id = if (obj.isNull("source_signal_id")) null else obj.getString("source_signal_id"),
                                    created_at = if (obj.isNull("created_at")) null else obj.getString("created_at"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        db.financialEventDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.financialEventDao().insertAll(list)
                            financialCount = list.size
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing financial events", e)
                    }
                }

                // 3. Sync FYI Events
                if (fyiJson != null) {
                    try {
                        val array = JSONArray(fyiJson)
                        val list = mutableListOf<FyiEventEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                FyiEventEntity(
                                    fyi_event_id = obj.getString("fyi_event_id"),
                                    title = if (obj.isNull("title")) null else obj.getString("title"),
                                    summary = if (obj.isNull("summary")) null else obj.getString("summary"),
                                    category = if (obj.isNull("category")) null else obj.getString("category"),
                                    read_flag = if (obj.isNull("read_flag")) false else obj.getBoolean("read_flag"),
                                    source_signal_id = if (obj.isNull("source_signal_id")) null else obj.getString("source_signal_id"),
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
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing FYI events", e)
                    }
                }

                // 4. Sync User Preferences
                if (preferencesJson != null) {
                    try {
                        val array = JSONArray(preferencesJson)
                        val list = mutableListOf<UserPreferenceEntity>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                UserPreferenceEntity(
                                    preference_key = obj.getString("preference_key"),
                                    preference_value = if (obj.isNull("preference_value")) null else obj.getString("preference_value"),
                                    updated_at = if (obj.isNull("updated_at")) null else obj.getString("updated_at")
                                )
                            )
                        }
                        db.userPreferenceDao().deleteAll()
                        if (list.isNotEmpty()) {
                            db.userPreferenceDao().insertAll(list)
                            prefCount = list.size
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing user preferences", e)
                    }
                }
            }

            Log.d(
                TAG,
                "Sync complete: todos=$todoCount, financial=$financialCount, fyi=$fyiCount, preferences=$prefCount"
            )

            InsightSyncResult.Success(
                briefCount = 0,
                todoCount = todoCount,
                financialCount = financialCount,
                fyiCount = fyiCount,
                prefCount = prefCount
            )

        } catch (ex: Exception) {
            Log.e(TAG, "Error in syncInsights: ${ex.message}", ex)
            InsightSyncResult.Failure(ex.message ?: "Unknown error")
        }
    }
}
