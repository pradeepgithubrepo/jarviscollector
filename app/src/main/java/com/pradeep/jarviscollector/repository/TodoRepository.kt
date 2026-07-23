package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.ReminderEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TodoRepository {

    private const val TAG = "TodoRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).todoDao()
    private fun getReminderDao(context: Context) = JarvisDatabase.getDatabase(context).reminderDao()

    suspend fun getTodos(context: Context): List<TodoEntity> {
        return getDao(context).getAll()
    }

    suspend fun createTodo(context: Context, todo: TodoEntity): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("id", todo.todo_id)
            put("title", todo.title)
            put("description", todo.description)
            put("priority", todo.priority ?: "MEDIUM")
            put("status", "OPEN")
            put("due_datetime", todo.due_date?.let { "${it}T12:00:00.000Z" })
            put("notification_profile", "STANDARD")
            put("source_type", "USER_TEXT")
            put("route_id", JSONObject.NULL)
            put("created_by", "USER")
            put("assigned_to", "Pradeep")
            put("created_at", timestamp)
            put("updated_at", timestamp)
            put("completed_at", JSONObject.NULL)
            put("reminder_datetime", todo.reminder_datetime?.let { it } ?: JSONObject.NULL)
        }

        val success = JarvisInsightsClient.insertRow("tasks", payload.toString(), "jarvis_insights_schemav1")
        if (success) {
            getDao(context).insert(todo)
        }
        success
    }

    fun getTodosFlow(context: Context): Flow<List<TodoEntity>> {
        return getDao(context).getAllFlow()
    }

    suspend fun getPendingTodos(context: Context): List<TodoEntity> {
        return getDao(context).getPending()
    }

    suspend fun getCompletedTodos(context: Context): List<TodoEntity> {
        return getDao(context).getCompleted()
    }

    suspend fun markTodoComplete(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "COMPLETED")
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow("tasks", "id=eq.$id", payload.toString(), "jarvis_insights_schemav1")
        if (success) {
            Log.d(TAG, "Todo completion synced to Supabase")
            try {
                getDao(context).updateStatus(id, "COMPLETED", timestamp)
                // When complete, remove any pending local reminders
                getReminderDao(context).deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating status locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "todos",
                entityId = id,
                action = "todo_complete",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync Todo completion to Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun snoozeTodo(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "SNOOZED")
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow("tasks", "id=eq.$id", payload.toString(), "jarvis_insights_schemav1")
        if (success) {
            Log.d(TAG, "Todo snooze synced to Supabase")
            try {
                getDao(context).snoozeTodo(id, timestamp)
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing todo locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "todos",
                entityId = id,
                action = "todo_snooze",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync Todo snooze to Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun deleteTodo(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val success = JarvisInsightsClient.deleteRow("tasks", "id=eq.$id", "jarvis_insights_schemav1")
        if (success) {
            Log.d(TAG, "Todo deleted successfully from Supabase")
            try {
                getDao(context).deleteById(id)
                // Clear any reminder associated with this todo
                getReminderDao(context).deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting todo locally", e)
            }

            val logPayload = JSONObject().apply {
                put("action", "delete")
                put("todo_id", id)
            }
            ActionsRepository.logAction(
                context = context,
                entityType = "todos",
                entityId = id,
                action = "todo_dismiss",
                metadata = logPayload
            )
        } else {
            Log.e(TAG, "Failed to delete Todo from Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun clearCompletedTodos(context: Context): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val completed = getDao(context).getCompleted()
        var allSuccess = true
        for (todo in completed) {
            val success = deleteTodo(context, todo.todo_id)
            if (!success) {
                allSuccess = false
            }
        }
        allSuccess
    }

    // --- REMINDER MANAGEMENT METHODS ---

    suspend fun getReminder(context: Context, todoId: String): ReminderEntity? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        getReminderDao(context).getById(todoId)
    }

    fun getReminderFlow(context: Context, todoId: String): Flow<ReminderEntity?> {
        return getReminderDao(context).getByIdFlow(todoId)
    }

    suspend fun setReminder(context: Context, reminder: ReminderEntity) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // 1. Save locally
        getReminderDao(context).insert(reminder)

        // 2. Sync to Supabase
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val isoTimestamp = sdf.format(Date(reminder.scheduled_timestamp))
        val payload = JSONObject().apply {
            put("reminder_datetime", isoTimestamp)
        }
        val success = JarvisInsightsClient.updateRow("tasks", "id=eq.${reminder.reminder_id}", payload.toString(), "jarvis_insights_schemav1")
        if (success) {
            Log.d(TAG, "Successfully synced reminder_datetime to Supabase tasks")
        }
    }

    suspend fun deleteReminder(context: Context, todoId: String) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // 1. Remove locally
        getReminderDao(context).deleteById(todoId)

        // 2. Sync to Supabase (set reminder_datetime to null)
        val payload = JSONObject().apply {
            put("reminder_datetime", JSONObject.NULL)
        }
        val success = JarvisInsightsClient.updateRow("tasks", "id=eq.$todoId", payload.toString(), "jarvis_insights_schemav1")
        if (success) {
            Log.d(TAG, "Successfully cleared reminder_datetime on Supabase tasks")
        }
    }
}

