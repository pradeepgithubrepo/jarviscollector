package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.NotificationEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationCenterRepository {

    private const val TAG = "NotificationCenterRepo"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).notificationDao()

    suspend fun getNotifications(context: Context): List<NotificationEntity> {
        return getDao(context).getAll()
    }

    fun getNotificationsFlow(context: Context): Flow<List<NotificationEntity>> {
        return getDao(context).getAllFlow()
    }

    suspend fun markNotificationRead(context: Context, id: String, readFlag: Boolean): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val statusVal = if (readFlag) "READ" else "NEW"
        val payload = JSONObject().apply {
            put("read_flag", readFlag)
            put("status", statusVal)
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow(
            "notifications",
            "id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Notification read status synced to Supabase")
            try {
                getDao(context).updateStatus(id, statusVal, readFlag)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notification locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "notifications",
                entityId = id,
                action = "notification_read",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync notification read status. Room cache not modified.")
        }
        success
    }

    suspend fun archiveNotification(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "ARCHIVED")
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow(
            "notifications",
            "id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Notification archive status synced to Supabase")
            try {
                getDao(context).updateStatus(id, "ARCHIVED", true)
            } catch (e: Exception) {
                Log.e(TAG, "Error archiving notification locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "notifications",
                entityId = id,
                action = "notification_archive",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync notification archive status. Room cache not modified.")
        }
        success
    }
}
