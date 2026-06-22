package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.UserActionEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object ActionsRepository {

    private const val TAG = "ActionsRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logAction(
        context: Context,
        entityType: String,
        entityId: String,
        action: String,
        metadata: JSONObject? = null
    ) {
        val actionId = UUID.randomUUID().toString()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val actionEntity = UserActionEntity(
            action_id = actionId,
            entity_type = entityType,
            entity_id = entityId,
            action = action,
            action_timestamp = timestamp,
            metadata = metadata?.toString()
        )

        scope.launch {
            // Save to local cache first
            try {
                JarvisDatabase.getDatabase(context).userActionDao().insert(actionEntity)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting local user action", e)
            }

            // Sync to Supabase
            val payload = JSONObject().apply {
                put("action_id", actionId)
                put("entity_type", entityType)
                put("entity_id", entityId)
                put("action", action)
                put("action_timestamp", timestamp)
                put("metadata", metadata ?: JSONObject())
            }

            val success = JarvisInsightsClient.insertRow("user_actions", payload.toString())
            if (success) {
                try {
                    JarvisDatabase.getDatabase(context).userActionDao().deleteById(actionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting local user action after sync", e)
                }
            }
        }
    }
}
