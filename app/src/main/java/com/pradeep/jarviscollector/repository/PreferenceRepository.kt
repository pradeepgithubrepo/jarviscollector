package com.pradeep.jarviscollector.repository

import android.content.Context
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import com.pradeep.jarviscollector.network.JarvisInsightsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PreferenceRepository {

    private const val TAG = "PreferenceRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDao(context: Context) = JarvisDatabase.getDatabase(context).userPreferenceDao()

    suspend fun getPreferences(context: Context): List<UserPreferenceEntity> {
        return getDao(context).getAll()
    }

    suspend fun getPreference(context: Context, key: String): UserPreferenceEntity? {
        return getDao(context).getByKey(key)
    }

    fun savePreference(context: Context, key: String, value: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = sdf.format(Date())

        val preference = UserPreferenceEntity(
            preference_key = key,
            preference_value = value,
            updated_at = timestamp
        )

        scope.launch {
            // Save to Room cache
            try {
                getDao(context).insert(preference)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting preference locally", e)
            }

            // Sync to Supabase
            val payload = JSONObject().apply {
                put("preference_key", key)
                put("preference_value", value)
                put("updated_at", timestamp)
            }

            val success = JarvisInsightsClient.insertRow("user_preferences", payload.toString())
            if (success) {
                Log.d(TAG, "Preference synced successfully to Supabase")
            }

            // Also log user action
            ActionsRepository.logAction(
                context = context,
                entityType = "user_preferences",
                entityId = key,
                action = "preference_change",
                metadata = JSONObject().apply { put("preference_value", value) }
            )
        }
    }
}
