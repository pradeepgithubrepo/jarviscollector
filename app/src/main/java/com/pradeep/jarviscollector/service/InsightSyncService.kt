package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.DailyBriefEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import com.pradeep.jarviscollector.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

sealed class InsightSyncResult {
    data class Success(
        val briefCount: Int,
        val todoCount: Int,
        val financialCount: Int,
        val fyiCount: Int
    ) : InsightSyncResult()

    data class Failure(val error: String) : InsightSyncResult()
}

object InsightSyncService {

    private const val TAG = "InsightSyncService"

    suspend fun syncInsights(
        context: Context
    ): InsightSyncResult = withContext(Dispatchers.IO) {
        try {
            val ownerName =
                AppPreferences.getOwnerName(context)

            Log.d(
                TAG,
                "Starting insights sync for user: $ownerName"
            )

            // 1. Download payloads
            val briefJson =
                JarvisInsightsClient
                    .downloadInsightJson(
                        ownerName,
                        "daily_brief.json"
                    )

            val todosJson =
                JarvisInsightsClient
                    .downloadInsightJson(
                        ownerName,
                        "todos.json"
                    )

            val financialJson =
                JarvisInsightsClient
                    .downloadInsightJson(
                        ownerName,
                        "financial.json"
                    )

            val fyiJson =
                JarvisInsightsClient
                    .downloadInsightJson(
                        ownerName,
                        "fyi.json"
                    )

            // Verify if any files were downloaded
            if (
                briefJson == null &&
                todosJson == null &&
                financialJson == null &&
                fyiJson == null
            ) {
                return@withContext InsightSyncResult.Failure(
                    "All downloads failed or no insights available on server for $ownerName."
                )
            }

            val db =
                JarvisDatabase.getDatabase(context)

            var briefCount = 0
            var todoCount = 0
            var financialCount = 0
            var fyiCount = 0

            db.withTransaction {
                // Parse daily brief
                if (briefJson != null) {
                    try {
                        val obj = JSONObject(briefJson)
                        val genAt =
                            obj.optString(
                                "generated_at",
                                System.currentTimeMillis().toString()
                            )
                        val version =
                            obj.optString(
                                "version",
                                "1.0"
                            )
                        val items =
                            obj.optJSONArray("items")
                                ?.toString()
                                ?: "[]"

                        val briefEntity =
                            DailyBriefEntity(
                                id = genAt,
                                generatedAt = genAt,
                                version = version,
                                itemsJson = items
                            )

                        db.dailyBriefDao().insert(briefEntity)
                        briefCount = 1
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error processing daily_brief.json",
                            e
                        )
                    }
                }

                // Parse todos
                if (todosJson != null) {
                    try {
                        val obj = JSONObject(todosJson)
                        val itemsArray =
                            obj.optJSONArray("items")
                        val todosList =
                            mutableListOf<TodoEntity>()

                        if (itemsArray != null) {
                            for (i in 0 until itemsArray.length()) {
                                val item =
                                    itemsArray.getJSONObject(i)
                                todosList.add(
                                    TodoEntity(
                                        id =
                                            item.optString(
                                                "id",
                                                UUID.randomUUID().toString()
                                            ),
                                        title =
                                            item.optString(
                                                "title",
                                                ""
                                            ),
                                        description =
                                            if (item.isNull("description")) null else item.optString("description"),
                                        dueDate =
                                            if (item.isNull("due_date")) null else item.optString("due_date"),
                                        priority =
                                            if (item.isNull("priority")) "medium" else item.optString("priority"),
                                        status =
                                            item.optString(
                                                "status",
                                                "pending"
                                            ),
                                        completedAt =
                                            if (item.isNull("completed_at")) null else item.optString("completed_at"),
                                        snoozeCount =
                                            item.optInt(
                                                "snooze_count",
                                                0
                                            ),
                                        updatedAt =
                                            System.currentTimeMillis()
                                    )
                                )
                            }
                        }

                        db.todoDao().deleteAll()
                        if (todosList.isNotEmpty()) {
                            db.todoDao().insertAll(todosList)
                            todoCount = todosList.size
                        }
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error processing todos.json",
                            e
                        )
                    }
                }

                // Parse financial events
                if (financialJson != null) {
                    try {
                        val obj = JSONObject(financialJson)
                        val itemsArray =
                            obj.optJSONArray("items")
                        val financialList =
                            mutableListOf<FinancialEventEntity>()

                        if (itemsArray != null) {
                            for (i in 0 until itemsArray.length()) {
                                val item =
                                    itemsArray.getJSONObject(i)
                                financialList.add(
                                    FinancialEventEntity(
                                        id =
                                            item.optString(
                                                "id",
                                                UUID.randomUUID().toString()
                                            ),
                                        title =
                                            item.optString(
                                                "title",
                                                ""
                                            ),
                                        amount =
                                            if (item.isNull("amount")) null else item.optDouble("amount"),
                                        type =
                                            item.optString(
                                                "type",
                                                ""
                                            ),
                                        dueDate =
                                            if (item.isNull("due_date")) null else item.optString("due_date"),
                                        status =
                                            if (item.isNull("status")) null else item.optString("status"),
                                        description =
                                            if (item.isNull("description")) null else item.optString("description")
                                    )
                                )
                            }
                        }

                        db.financialEventDao().deleteAll()
                        if (financialList.isNotEmpty()) {
                            db.financialEventDao()
                                .insertAll(financialList)
                            financialCount =
                                financialList.size
                        }
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error processing financial.json",
                            e
                        )
                    }
                }

                // Parse FYI events
                if (fyiJson != null) {
                    try {
                        val obj = JSONObject(fyiJson)
                        val itemsArray =
                            obj.optJSONArray("items")
                        val fyiList =
                            mutableListOf<FyiEventEntity>()

                        if (itemsArray != null) {
                            for (i in 0 until itemsArray.length()) {
                                val item =
                                    itemsArray.getJSONObject(i)
                                fyiList.add(
                                    FyiEventEntity(
                                        id =
                                            item.optString(
                                                "id",
                                                UUID.randomUUID().toString()
                                            ),
                                        title =
                                            item.optString(
                                                "title",
                                                ""
                                            ),
                                        content =
                                            item.optString(
                                                "content",
                                                ""
                                            ),
                                        category =
                                            item.optString(
                                                "category",
                                                "other"
                                            ),
                                        timestamp =
                                            item.optString(
                                                "timestamp",
                                                ""
                                            )
                                    )
                                )
                            }
                        }

                        db.fyiEventDao().deleteAll()
                        if (fyiList.isNotEmpty()) {
                            db.fyiEventDao().insertAll(fyiList)
                            fyiCount = fyiList.size
                        }
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error processing fyi.json",
                            e
                        )
                    }
                }
            }

            Log.d(
                TAG,
                "Sync complete: brief=$briefCount, todos=$todoCount, financial=$financialCount, fyi=$fyiCount"
            )

            InsightSyncResult.Success(
                briefCount,
                todoCount,
                financialCount,
                fyiCount
            )

        } catch (
            ex: Exception
        ) {
            Log.e(
                TAG,
                "Error in syncInsights: ${ex.message}",
                ex
            )

            InsightSyncResult.Failure(
                ex.message ?: "Unknown error"
            )
        }
    }
}
