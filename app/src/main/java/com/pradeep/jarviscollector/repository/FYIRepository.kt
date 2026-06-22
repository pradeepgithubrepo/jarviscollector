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

    fun markFyiRead(context: Context, id: String, readFlag: Boolean) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        scope.launch {
            // Update Room locally
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.fyi_event_id == id }
                if (target != null) {
                    val updated = target.copy(read_flag = readFlag, updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating read flag locally", e)
            }

            // Sync mark read status to Supabase
            val payload = JSONObject().apply {
                put("read_flag", readFlag)
                put("updated_at", timestamp)
            }

            val success = JarvisInsightsClient.updateRow(
                "fyi_events",
                "fyi_event_id=eq.$id",
                payload.toString()
            )
            if (success) {
                Log.d(TAG, "FYI read status synced to Supabase")
            }

            // Log action
            ActionsRepository.logAction(
                context = context,
                entityType = "fyi_events",
                entityId = id,
                action = if (readFlag) "fyi_mark_read" else "fyi_mark_unread"
            )
        }
    }
}
