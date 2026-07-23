package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.VaultCategoryEntity
import com.pradeep.jarviscollector.model.VaultEntryEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object VaultRepository {

    private const val TAG = "VaultRepository"
    private const val SCHEMA = "jarvis_insights_schemav1"
    private const val FALLBACK_SCHEMA = "jarvis_insights_schema"

    data class DefaultCategorySpec(
        val id: String,
        val name: String,
        val icon: String,
        val color: String
    )

    val DEFAULT_CATEGORIES = listOf(
        DefaultCategorySpec("9af8c8d9-b76e-47d2-94ac-7aeab86efad5", "Bank Accounts", "land-bank", "#2563EB"),
        DefaultCategorySpec("f367e307-f8cb-45c2-91e6-b1d39213c648", "Stocks & Mutual Funds", "chart-trending-up", "#059669"),
        DefaultCategorySpec("f38e18ff-3237-4436-8f21-1da0ed8f9ecf", "Long Term Investments", "vault", "#10B981"),
        DefaultCategorySpec("c6aaf398-0046-4172-b6ca-e5548bc51961", "Insurance", "shield-check", "#D97706"),
        DefaultCategorySpec("314648b5-3b1a-43fb-adf7-eb5062416db4", "Physical Assets", "home", "#7C3AED"),
        DefaultCategorySpec("bae73f29-fd66-4493-a9a0-799f8330e091", "Investments", "chart-bar", "#059669"),
        DefaultCategorySpec("14854c45-4216-4fdd-8366-0c34d17d6c9f", "Properties", "building", "#7C3AED"),
        DefaultCategorySpec("2d2ee013-a7c2-4940-a9f0-2e25ee2239ae", "Vehicles", "car", "#DC2626"),
        DefaultCategorySpec("b6656e92-7d21-45d0-995f-4939b78e7401", "Documents", "file-text", "#4B5563"),
        DefaultCategorySpec("011287a6-4433-40da-8091-cd0ddd6b5618", "Digital Accounts", "key", "#0891B2"),
        DefaultCategorySpec("dd3e2c15-8847-4985-9bcf-d904f065b3a5", "Other", "folder", "#6B7280")
    )

    suspend fun seedDefaultCategoriesIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = JarvisDatabase.getDatabase(context)
            val dao = db.vaultCategoryDao()
            val existing = dao.getAll()
            if (existing.isEmpty()) {
                Log.d(TAG, "No vault categories found. Seeding defaults...")
                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                val entities = mutableListOf<VaultCategoryEntity>()

                DEFAULT_CATEGORIES.forEachIndexed { index, spec ->
                    val payload = JSONObject().apply {
                        put("vault_category_id", spec.id)
                        put("category_name", spec.name)
                        put("display_order", index + 1)
                        put("icon", spec.icon)
                        put("color", spec.color)
                        put("is_active", true)
                        put("created_at", now)
                        put("updated_at", now)
                    }

                    var success = JarvisInsightsClient.insertRow("vault_categories", payload.toString(), SCHEMA)
                    if (!success) {
                        success = JarvisInsightsClient.insertRow("vault_categories", payload.toString(), FALLBACK_SCHEMA)
                    }

                    entities.add(
                        VaultCategoryEntity(
                            vault_category_id = spec.id,
                            category_name = spec.name,
                            display_order = index + 1,
                            icon = spec.icon,
                            color = spec.color,
                            is_active = true,
                            created_at = now,
                            updated_at = now
                        )
                    )
                }

                if (entities.isNotEmpty()) {
                    dao.insertAll(entities)
                    Log.d(TAG, "Successfully seeded ${entities.size} default vault categories")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding default vault categories", e)
        }
    }

    suspend fun createVaultEntry(
        context: Context,
        categoryId: String,
        title: String,
        owner: String?,
        subCategory: String?,
        location: String?,
        accessInformation: String?,
        notes: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val entryId = UUID.randomUUID().toString()
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

            val payload = JSONObject().apply {
                put("vault_entry_id", entryId)
                put("vault_category_id", categoryId)
                put("title", title.trim())
                if (!owner.isNullOrBlank()) put("owner", owner.trim())
                if (!subCategory.isNullOrBlank()) put("sub_category", subCategory.trim())
                if (!location.isNullOrBlank()) put("location", location.trim())
                if (!accessInformation.isNullOrBlank()) put("access_information", accessInformation.trim())
                if (!notes.isNullOrBlank()) put("notes", notes.trim())
                put("is_active", true)
                put("created_at", now)
                put("updated_at", now)
            }

            var success = JarvisInsightsClient.insertRow("vault_entries", payload.toString(), SCHEMA)
            if (!success) {
                success = JarvisInsightsClient.insertRow("vault_entries", payload.toString(), FALLBACK_SCHEMA)
            }

            // Always save locally to Room for immediate UX responsiveness
            val db = JarvisDatabase.getDatabase(context)
            val entity = VaultEntryEntity(
                vault_entry_id = entryId,
                vault_category_id = categoryId,
                parent_entry_id = null,
                owner = owner?.trim(),
                title = title.trim(),
                sub_category = subCategory?.trim(),
                location = location?.trim(),
                access_information = accessInformation?.trim(),
                notes = notes?.trim(),
                sort_order = 0,
                is_active = true,
                created_at = now,
                updated_at = now
            )
            db.vaultEntryDao().insert(entity)

            try {
                ActionsRepository.logAction(
                    context = context,
                    entityType = "VAULT_ENTRY",
                    entityId = entryId,
                    action = "vault_entry_create"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Non-critical error logging action", e)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create vault entry", e)
            false
        }
    }

    suspend fun updateVaultEntry(
        context: Context,
        entryId: String,
        categoryId: String,
        title: String,
        owner: String?,
        subCategory: String?,
        location: String?,
        accessInformation: String?,
        notes: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

            val payload = JSONObject().apply {
                put("vault_category_id", categoryId)
                put("title", title.trim())
                put("owner", owner?.trim() ?: JSONObject.NULL)
                put("sub_category", subCategory?.trim() ?: JSONObject.NULL)
                put("location", location?.trim() ?: JSONObject.NULL)
                put("access_information", accessInformation?.trim() ?: JSONObject.NULL)
                put("notes", notes?.trim() ?: JSONObject.NULL)
                put("updated_at", now)
            }

            val queryParams = "vault_entry_id=eq.$entryId"
            var success = JarvisInsightsClient.updateRow("vault_entries", queryParams, payload.toString(), SCHEMA)
            if (!success) {
                success = JarvisInsightsClient.updateRow("vault_entries", queryParams, payload.toString(), FALLBACK_SCHEMA)
            }

            val db = JarvisDatabase.getDatabase(context)
            val entity = VaultEntryEntity(
                vault_entry_id = entryId,
                vault_category_id = categoryId,
                parent_entry_id = null,
                owner = owner?.trim(),
                title = title.trim(),
                sub_category = subCategory?.trim(),
                location = location?.trim(),
                access_information = accessInformation?.trim(),
                notes = notes?.trim(),
                sort_order = 0,
                is_active = true,
                created_at = now,
                updated_at = now
            )
            db.vaultEntryDao().insert(entity)

            try {
                ActionsRepository.logAction(
                    context = context,
                    entityType = "VAULT_ENTRY",
                    entityId = entryId,
                    action = "vault_entry_update"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Non-critical error logging action", e)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update vault entry", e)
            false
        }
    }

    suspend fun deleteVaultEntry(context: Context, entryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val queryParams = "vault_entry_id=eq.$entryId"
            var success = JarvisInsightsClient.deleteRow("vault_entries", queryParams, SCHEMA)
            if (!success) {
                success = JarvisInsightsClient.deleteRow("vault_entries", queryParams, FALLBACK_SCHEMA)
            }

            val db = JarvisDatabase.getDatabase(context)
            db.vaultEntryDao().deleteById(entryId)

            try {
                ActionsRepository.logAction(
                    context = context,
                    entityType = "VAULT_ENTRY",
                    entityId = entryId,
                    action = "vault_entry_delete"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Non-critical error logging action", e)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete vault entry", e)
            false
        }
    }
}
