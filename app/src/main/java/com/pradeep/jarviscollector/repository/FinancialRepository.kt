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

object FinancialRepository {

    private const val TAG = "FinancialRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).financialEventDao()

    suspend fun getFinancialEvents(context: Context): List<FinancialEventEntity> {
        return getDao(context).getAll()
    }

    suspend fun getTransactionsByMonth(context: Context, monthKey: String): List<FinancialEventEntity> {
        return getDao(context).getByMonth(monthKey)
    }

    suspend fun getTransactionsByMonthAndCategory(context: Context, monthKey: String, category: String): List<FinancialEventEntity> {
        return getDao(context).getByMonthAndCategory(monthKey, category)
    }

    suspend fun getTransactionsByMonthSortedByMaxSpend(context: Context, monthKey: String): List<FinancialEventEntity> {
        return getDao(context).getByMonthSortedByMaxSpend(monthKey)
    }

    suspend fun updateTransactionDetails(
        context: Context,
        id: String,
        category: String,
        subcategory: String?,
        isSelfTransfer: Boolean
    ): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("category", category)
            put("subcategory", subcategory ?: JSONObject.NULL)
            put("is_self_transfer", isSelfTransfer)
            put("is_override", true)
        }

        val success = JarvisInsightsClient.updateRow(
            "financial_transactions",
            "transaction_id=eq.$id",
            payload.toString(),
            "jarvis_insights_schemav1"
        )

        if (success) {
            Log.d(TAG, "Financial transaction details synced to Supabase: $id")
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.financial_event_id == id }
                if (target != null) {
                    getDao(context).insertAll(listOf(target.copy(
                        category = category,
                        subcategory = subcategory,
                        is_self_transfer = isSelfTransfer
                    )))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating transaction locally", e)
            }

            ActionsRepository.logAction(
                context = context,
                entityType = "financial_transactions",
                entityId = id,
                action = "financial_transaction_update",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to sync transaction details to Supabase.")
        }
        success
    }

    // True hard delete — removes transaction from Supabase and local Room
    suspend fun purgeTransaction(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val success = JarvisInsightsClient.deleteRow(
            "financial_transactions",
            "transaction_id=eq.$id",
            "jarvis_insights_schemav1"
        )
        if (success) {
            Log.d(TAG, "Transaction purged from Supabase: $id")
            try {
                getDao(context).deleteById(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting transaction locally after purge", e)
            }
            ActionsRepository.logAction(
                context = context,
                entityType = "financial_transactions",
                entityId = id,
                action = "financial_purge",
                metadata = JSONObject().apply { put("id", id) }
            )
        } else {
            Log.e(TAG, "Failed to purge transaction from Supabase. Local data unchanged.")
        }
        success
    }

    // Retrigger the aggregates pipeline by inserting a Manual execution request
    suspend fun retriggerPipeline(context: Context): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("pipeline_name", "financial_agent")
            put("phase", "qualification")
            put("trigger_type", "MANUAL")
            put("status", "PENDING")
        }

        val success = JarvisInsightsClient.insertRow(
            "pipeline_runs",
            payload.toString(),
            "jarvis_insights_schemav1"
        )

        if (success) {
            Log.d(TAG, "Aggregate recalculation retrigger successful")
            ActionsRepository.logAction(
                context = context,
                entityType = "pipelines",
                entityId = "financial_agent",
                action = "pipeline_retrigger",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to retrigger pipeline execution on Supabase")
        }
        success
    }

    suspend fun confirmTransaction(context: Context, id: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("category", "CONFIRMED")
        }
        val success = JarvisInsightsClient.updateRow(
            "financial_transactions",
            "transaction_id=eq.$id",
            payload.toString(),
            "jarvis_insights_schemav1"
        )
        if (success) {
            Log.d(TAG, "Financial transaction confirmation synced to Supabase")
            ActionsRepository.logAction(
                context = context,
                entityType = "financial_transactions",
                entityId = id,
                action = "financial_confirm",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to confirm transaction on Supabase.")
        }
        success
    }

    suspend fun correctTransaction(context: Context, id: String, category: String, amount: Double): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("category", category)
            put("amount", amount)
        }
        val success = JarvisInsightsClient.updateRow(
            "financial_transactions",
            "transaction_id=eq.$id",
            payload.toString(),
            "jarvis_insights_schemav1"
        )
        if (success) {
            Log.d(TAG, "Financial transaction correction synced to Supabase")
            try {
                val events = getDao(context).getAll()
                val target = events.find { it.financial_event_id == id }
                if (target != null) {
                    getDao(context).insertAll(listOf(target.copy(category = category, amount = amount)))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error correcting transaction locally", e)
            }
            ActionsRepository.logAction(
                context = context,
                entityType = "financial_transactions",
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
