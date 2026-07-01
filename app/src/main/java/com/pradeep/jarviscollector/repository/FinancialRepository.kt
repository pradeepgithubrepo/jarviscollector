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

    suspend fun overrideCategory(context: Context, id: String, category: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

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
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.financial_event_id == id }
                if (target != null) {
                    val updated = target.copy(category = category, updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating category locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "financial_events",
                entityId = id,
                action = "financial_category_override",
                metadata = JSONObject().apply { put("category", category) }
            )
        } else {
            Log.e(TAG, "Failed to sync category override to Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun confirmTransaction(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "CONFIRMED")
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow(
            "financial_events",
            "financial_event_id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Financial transaction confirmation synced to Supabase")
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.financial_event_id == id }
                if (target != null) {
                    val updated = target.copy(status = "CONFIRMED", updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming transaction locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "financial_events",
                entityId = id,
                action = "financial_confirm",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to confirm transaction on Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun correctTransaction(context: Context, id: String, category: String, amount: Double): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val payload = JSONObject().apply {
            put("status", "CORRECTED")
            put("category", category)
            put("amount", amount)
            put("updated_at", timestamp)
        }

        val success = JarvisInsightsClient.updateRow(
            "financial_events",
            "financial_event_id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Financial transaction correction synced to Supabase")
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.financial_event_id == id }
                if (target != null) {
                    val updated = target.copy(status = "CORRECTED", category = category, amount = amount, updated_at = timestamp)
                    getDao(context).insertAll(listOf(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error correcting transaction locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "financial_events",
                entityId = id,
                action = "financial_correct",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to correct transaction on Supabase. Room cache not modified.")
        }
        success
    }
}
