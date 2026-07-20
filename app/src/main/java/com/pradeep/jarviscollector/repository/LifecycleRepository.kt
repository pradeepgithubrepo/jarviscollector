package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.LifecycleItemEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

object LifecycleRepository {

    private const val TAG = "LifecycleRepository"

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).lifecycleItemDao()

    suspend fun getLifecycleItems(context: Context): List<LifecycleItemEntity> {
        return getDao(context).getAll()
    }

    // CRUD: Add a new Lifecycle Event
    suspend fun createLifecycleItem(
        context: Context,
        domain: String,
        title: String,
        description: String?,
        scheduleType: String,
        intervalDays: Int?,
        nextOccurrenceDate: String,
        reminderOffsetDays: Int?,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val payload = JSONObject().apply {
            put("id", newId)
            put("domain", domain)
            put("title", title)
            put("description", description ?: JSONObject.NULL)
            put("schedule_type", scheduleType)
            put("interval_days", intervalDays ?: JSONObject.NULL)
            put("next_occurrence_date", nextOccurrenceDate)
            put("reminder_offset_days", reminderOffsetDays ?: JSONObject.NULL)
            put("status", status)
        }

        val success = JarvisInsightsClient.insertRow(
            "lifecycle_items",
            payload.toString(),
            "jarvis_insights_schemav1"
        )

        if (success) {
            Log.d(TAG, "Lifecycle item created on Supabase: $newId")
            try {
                getDao(context).insert(
                    LifecycleItemEntity(
                        id = newId,
                        domain = domain,
                        title = title,
                        description = description,
                        schedule_type = scheduleType,
                        interval_days = intervalDays,
                        next_occurrence_date = nextOccurrenceDate,
                        reminder_offset_days = reminderOffsetDays,
                        last_promoted_date = null,
                        last_todo_id = null,
                        status = status,
                        created_at = null,
                        updated_at = null
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting lifecycle item locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "lifecycle_items",
                entityId = newId,
                action = "lifecycle_create",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to create lifecycle item on Supabase")
        }
        success
    }

    // CRUD: Edit an existing Lifecycle Event
    suspend fun updateLifecycleItem(
        context: Context,
        id: String,
        domain: String,
        title: String,
        description: String?,
        scheduleType: String,
        intervalDays: Int?,
        nextOccurrenceDate: String,
        reminderOffsetDays: Int?,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("domain", domain)
            put("title", title)
            put("description", description ?: JSONObject.NULL)
            put("schedule_type", scheduleType)
            put("interval_days", intervalDays ?: JSONObject.NULL)
            put("next_occurrence_date", nextOccurrenceDate)
            put("reminder_offset_days", reminderOffsetDays ?: JSONObject.NULL)
            put("status", status)
        }

        val success = JarvisInsightsClient.updateRow(
            "lifecycle_items",
            "id=eq.$id",
            payload.toString(),
            "jarvis_insights_schemav1"
        )

        if (success) {
            Log.d(TAG, "Lifecycle item updated on Supabase: $id")
            try {
                val existing = getDao(context).getAll().find { it.id == id }
                if (existing != null) {
                    getDao(context).insert(
                        existing.copy(
                            domain = domain,
                            title = title,
                            description = description,
                            schedule_type = scheduleType,
                            interval_days = intervalDays,
                            next_occurrence_date = nextOccurrenceDate,
                            reminder_offset_days = reminderOffsetDays,
                            status = status
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating lifecycle item locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "lifecycle_items",
                entityId = id,
                action = "lifecycle_update",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to update lifecycle item on Supabase")
        }
        success
    }

    // CRUD: Delete a Lifecycle Event
    suspend fun deleteLifecycleItem(context: Context, id: String): Boolean = withContext(Dispatchers.IO) {
        val success = JarvisInsightsClient.deleteRow(
            "lifecycle_items",
            "id=eq.$id",
            "jarvis_insights_schemav1"
        )

        if (success) {
            Log.d(TAG, "Lifecycle item deleted from Supabase: $id")
            try {
                getDao(context).deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting lifecycle item locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "lifecycle_items",
                entityId = id,
                action = "lifecycle_delete",
                metadata = JSONObject().apply { put("id", id) }
            )
        } else {
            Log.e(TAG, "Failed to delete lifecycle item from Supabase")
        }
        success
    }
}
