package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FYIRepository {

    private const val TAG = "FYIRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).fyiEventDao()

    suspend fun getFyiEvents(context: Context): List<FyiEventEntity> {
        return getDao(context).getAll()
    }

    suspend fun markFyiRead(context: Context, id: String, readFlag: Boolean): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val statusVal = if (readFlag) "READ" else "NEW"
        val payload = JSONObject().apply {
            put("read_flag", readFlag)
            put("status", statusVal)
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow(
            "fyi_events",
            "fyi_event_id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "FYI read status synced to Supabase")
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.fyi_event_id == id }
                if (target != null) {
                    val updated = target.copy(read_flag = readFlag, status = statusVal, updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating read flag locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "fyi_events",
                entityId = id,
                action = if (readFlag) "fyi_mark_read" else "fyi_mark_unread",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync FYI read status to Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun dismissFyi(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "DISMISSED")
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow(
            "fyi_events",
            "fyi_event_id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "FYI dismissal synced to Supabase")
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.fyi_event_id == id }
                if (target != null) {
                    val updated = target.copy(status = "DISMISSED", updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing FYI locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "fyi_events",
                entityId = id,
                action = "fyi_dismiss",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync FYI dismissal to Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun archiveFyi(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "ARCHIVED")
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow(
            "fyi_events",
            "fyi_event_id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "FYI archive synced to Supabase")
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.fyi_event_id == id }
                if (target != null) {
                    val updated = target.copy(status = "ARCHIVED", updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error archiving FYI locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "fyi_events",
                entityId = id,
                action = "fyi_archive",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync FYI archive to Supabase. Room cache not modified.")
        }
        success
    }
}
