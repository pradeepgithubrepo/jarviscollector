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

    val DEFAULT_CATEGORIES = listOf(
        Triple("Bank Accounts", "🏦", "#3B82F6"),
        Triple("Investments", "📈", "#10B981"),
        Triple("Insurance", "🛡", "#8B5CF6"),
        Triple("Properties", "🏠", "#F59E0B"),
        Triple("Vehicles", "🚗", "#EC4899"),
        Triple("Other", "📄", "#64748B")
    )

    suspend fun seedDefaultCategoriesIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = JarvisDatabase.getDatabase(context)
            val dao = db.vaultCategoryDao()
            val existing = dao.getAll()
            if (existing.isEmpty()) {
                Log.d(TAG, "No vault categories found. Seeding defaults to Supabase...")
                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                val entities = mutableListOf<VaultCategoryEntity>()

                DEFAULT_CATEGORIES.forEachIndexed { index, (name, icon, color) ->
                    val id = UUID.randomUUID().toString()
                    val payload = JSONObject().apply {
                        put("vault_category_id", id)
                        put("category_name", name)
                        put("display_order", index + 1)
                        put("icon", icon)
                        put("color", color)
                        put("is_active", true)
                        put("created_at", now)
                        put("updated_at", now)
                    }

                    val success = JarvisInsightsClient.insertRow("vault_categories", payload.toString(), SCHEMA)
                    if (success) {
                        entities.add(
                            VaultCategoryEntity(
                                vault_category_id = id,
                                category_name = name,
                                display_order = index + 1,
                                icon = icon,
                                color = color,
                                is_active = true,
                                created_at = now,
                                updated_at = now
                            )
                        )
                    }
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

            val success = JarvisInsightsClient.insertRow("vault_entries", payload.toString(), SCHEMA)
            if (success) {
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
            }
            success
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
            val success = JarvisInsightsClient.updateRow("vault_entries", queryParams, payload.toString(), SCHEMA)
            if (success) {
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
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update vault entry", e)
            false
        }
    }

    suspend fun deleteVaultEntry(context: Context, entryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val queryParams = "vault_entry_id=eq.$entryId"
            val success = JarvisInsightsClient.deleteRow("vault_entries", queryParams, SCHEMA)
            if (success) {
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
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete vault entry", e)
            false
        }
    }
}
