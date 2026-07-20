package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.FactInsightEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FactRepository {

    private const val TAG = "FactRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).factInsightDao()

    suspend fun getFacts(context: Context): List<FactInsightEntity> {
        return getDao(context).getAll()
    }

    fun getFactsFlow(context: Context): Flow<List<FactInsightEntity>> {
        return getDao(context).getAllFlow()
    }

    suspend fun markFactRead(context: Context, id: String, readFlag: Boolean): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val statusVal = if (readFlag) "ACKNOWLEDGED" else "NEW"
        val payload = JSONObject().apply {
            put("status", statusVal)
            put("updated_at", timestamp)
        }

        // 1. Call Supabase first
        val success = JarvisInsightsClient.updateRow(
            "facts",
            "fact_id=eq.$id",
            payload.toString()
        ) || JarvisInsightsClient.updateRow(
            "facts",
            "id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Fact read status synced to Supabase")
            // 2. Update Room locally on success
            try {
                getDao(context).updateReadStatus(id, readFlag, statusVal)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating read flag locally", e)
            }

            // 3. Log action
            ActionsRepository.logAction(
                context = context,
                entityType = "facts",
                entityId = id,
                action = "fact_read",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync Fact read status to Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun deleteFactSoft(context: Context, id: String) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            getDao(context).deleteById(id)
            Log.d(TAG, "Fact soft-deleted locally: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error soft-deleting fact locally", e)
        }
    }

    suspend fun deleteFactHard(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // 1. Call Supabase delete
        val success = JarvisInsightsClient.deleteRow(
            "information_items",
            "id=eq.$id",
            "jarvis_insights_schemav1"
        )

        if (success) {
            // 2. Delete locally on success
            try {
                getDao(context).deleteById(id)
                Log.d(TAG, "Fact hard-deleted from Supabase and locally: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error hard-deleting fact locally", e)
            }
        } else {
            Log.e(TAG, "Failed to hard-delete from Supabase. Room cache not modified.")
        }
        success
    }
}
