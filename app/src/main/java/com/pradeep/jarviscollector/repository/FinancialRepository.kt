package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinancialRepository {

    private const val TAG = "FinancialRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).financialEventDao()

    suspend fun getFinancialEvents(context: Context): List<FinancialEventEntity> {
        return getDao(context).getAll()
    }

    fun overrideCategory(context: Context, id: String, category: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        scope.launch {
            // Update Room locally (needs a quick direct query update or fetch/modify/insert)
            val db = JarvisDatabase.getDatabase(context)
            try {
                // Since Room doesn't have a direct @Query update category in our DAO yet, let's fetch all and filter/update, or we can just let it sync on pull, but let's do it cleanly by upserting the updated entity if we can.
                // Alternatively, we can let Room get updated when we sync. But to avoid local category delay, let's fetch the existing entity, modify it, and write it back.
                val events = getDao(context).getAll()
                val target = events.find { it.financial_event_id == id }
                if (target != null) {
                    val updated = target.copy(category = category, updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating category locally", e)
            }

            // Sync category override to Supabase
            val payload = JSONObject().apply {
                put("category", category)
                put("updated_at", timestamp)
            }

            val success = JarvisInsightsClient.updateRow(
                "financial_events",
                "financial_event_id=eq.$id",
                payload.toString()
            )
            if (success) {
                Log.d(TAG, "Financial event category override synced to Supabase")
            }

            // Log action
            ActionsRepository.logAction(
                context = context,
                entityType = "financial_events",
                entityId = id,
                action = "financial_category_override",
                metadata = JSONObject().apply { put("category", category) }
            )
        }
    }
}
