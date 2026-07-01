package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.database.FinancialInsightDao
import com.pradeep.jarviscollector.model.FinancialInsightEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinancialInsightRepository {

    private const val TAG = "FinancialInsightRepo"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context): FinancialInsightDao {
        return JarvisDatabase.getDatabase(context).financialInsightDao()
    }

    fun observeInsights(context: Context): Flow<List<FinancialInsightEntity>> {
        return getDao(context).observeAll()
    }

    fun observeBills(context: Context): Flow<List<FinancialInsightEntity>> {
        return getDao(context).observeBills()
    }

    fun observeSubscriptions(context: Context): Flow<List<FinancialInsightEntity>> {
        return getDao(context).observeSubscriptions()
    }

    fun observeActionsRequired(context: Context): Flow<List<FinancialInsightEntity>> {
        return getDao(context).observePending()
    }

    fun observeUnusualActivity(context: Context): Flow<List<FinancialInsightEntity>> {
        return getDao(context).observeUnusualActivity()
    }

    suspend fun confirmInsight(context: Context, id: String): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("status", "CONFIRMED")
        }

        val success = JarvisInsightsClient.updateRow(
            "financial_facts",
            "id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Financial insight confirmation synced to Supabase")
            getDao(context).updateStatus(id, "CONFIRMED")

            ActionsRepository.logAction(
                context = context,
                entityType = "financial_insights",
                entityId = id,
                action = "FINANCIAL_CONFIRMED",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to confirm financial insight on Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun dismissInsight(context: Context, id: String): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("status", "DISMISSED")
        }

        val success = JarvisInsightsClient.updateRow(
            "financial_facts",
            "id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Financial insight dismissal synced to Supabase")
            getDao(context).updateStatus(id, "DISMISSED")

            ActionsRepository.logAction(
                context = context,
                entityType = "financial_insights",
                entityId = id,
                action = "FINANCIAL_DISMISSED",
                metadata = payload
            )
        } else {
            Log.e(TAG, "Failed to dismiss financial insight on Supabase. Room cache not modified.")
        }
        success
    }

    suspend fun correctInsight(context: Context, id: String, category: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("status", "CORRECTED")
            put("amount", amount)
            // type or description can store category details if the table supports it, or we just log it in user_actions
            put("description", "Corrected category to $category, amount to $amount")
        }

        val success = JarvisInsightsClient.updateRow(
            "financial_facts",
            "id=eq.$id",
            payload.toString()
        )

        if (success) {
            Log.d(TAG, "Financial insight correction synced to Supabase")
            getDao(context).updateStatus(id, "CORRECTED")

            ActionsRepository.logAction(
                context = context,
                entityType = "financial_insights",
                entityId = id,
                action = "FINANCIAL_CORRECTED",
                metadata = JSONObject().apply {
                    put("category", category)
                    put("amount", amount)
                }
            )
        } else {
            Log.e(TAG, "Failed to correct financial insight on Supabase. Room cache not modified.")
        }
        success
    }
}
