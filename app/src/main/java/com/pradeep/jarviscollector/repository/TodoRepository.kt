package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TodoRepository {

    private const val TAG = "TodoRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).todoDao()

    suspend fun getTodos(context: Context): List<TodoEntity> {
        return getDao(context).getAll()
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

        val success = JarvisInsightsClient.updateRow("todo_items", "todo_id=eq.$id", payload.toString())
        if (success) {
            Log.d(TAG, "Todo completion synced to Supabase")
            try {
                getDao(context).updateStatus(id, "COMPLETED", timestamp)
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

        val success = JarvisInsightsClient.updateRow("todo_items", "todo_id=eq.$id", payload.toString())
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
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "DISMISSED")
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow("todo_items", "todo_id=eq.$id", payload.toString())
        if (success) {
            Log.d(TAG, "Todo dismissal synced to Supabase")
            try {
                getDao(context).deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting todo locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "todos",
                entityId = id,
                action = "todo_dismiss",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync Todo dismissal to Supabase. Room cache not modified.")
        }
        success
    }
}
