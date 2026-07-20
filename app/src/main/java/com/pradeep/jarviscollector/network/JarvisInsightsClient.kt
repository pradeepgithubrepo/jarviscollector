package com.pradeep.jarviscollector.network

import android.util.Log
import com.pradeep.jarviscollector.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object JarvisInsightsClient {

    private const val TAG = "JarvisInsightsClient"

    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val API_KEY = BuildConfig.SUPABASE_SECRET_KEY

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
      * Fetch all records from a Supabase table.
      */
    fun fetchTable(tableName: String, schemaProfile: String = "jarvis_insights_schema"): String? {
        val url = if (tableName.contains("?")) {
            "$SUPABASE_URL/rest/v1/$tableName&select=*"
        } else {
            "$SUPABASE_URL/rest/v1/$tableName?select=*"
        }
        Log.d(TAG, "GET request to: $url (Schema: $schemaProfile)")

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Accept-Profile", schemaProfile)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful) {
                val rowCount = try {
                    org.json.JSONArray(body).length()
                } catch (e: Exception) {
                    1
                }
                QueryInstrumentation.log("GET", url, rowCount, true)
                body
            } else {
                QueryInstrumentation.log("GET", url, 0, false, "Code ${response.code}: $body")
                Log.e(TAG, "Failed to GET $tableName: Code ${response.code}, Body: $body")
                null
            }
        } catch (ex: Exception) {
            QueryInstrumentation.log("GET", url, 0, false, ex.message ?: ex.toString())
            Log.e(TAG, "Exception during GET $tableName", ex)
            null
        }
    }

    /**
      * Insert/Upsert a record in a Supabase table.
      */
    fun insertRow(tableName: String, jsonPayload: String, schemaProfile: String = "jarvis_insights_schema"): Boolean {
        val url = "$SUPABASE_URL/rest/v1/$tableName"
        Log.d(TAG, "POST request to: $url, Payload: $jsonPayload (Schema: $schemaProfile)")

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Profile", schemaProfile)
            .addHeader("Accept-Profile", schemaProfile)
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(jsonPayload.toRequestBody(jsonMediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                QueryInstrumentation.log("POST", url, 1, true)
                Log.d(TAG, "Insert successful into $tableName")
                true
            } else {
                val body = response.body?.string()
                QueryInstrumentation.log("POST", url, 0, false, "Code ${response.code}: $body")
                Log.e(TAG, "Failed to POST to $tableName: Code ${response.code}, Body: $body")
                false
            }
        } catch (ex: Exception) {
            QueryInstrumentation.log("POST", url, 0, false, ex.message ?: ex.toString())
            Log.e(TAG, "Exception during POST to $tableName", ex)
            false
        }
    }

    /**
      * Update/Patch a record in a Supabase table.
      * Example queryParam: "todo_id=eq.123-456"
      */
    fun updateRow(tableName: String, queryParam: String, jsonPayload: String, schemaProfile: String = "jarvis_insights_schema"): Boolean {
        val url = "$SUPABASE_URL/rest/v1/$tableName?$queryParam"
        Log.d(TAG, "PATCH request to: $url, Payload: $jsonPayload (Schema: $schemaProfile)")

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Profile", schemaProfile)
            .addHeader("Accept-Profile", schemaProfile)
            .patch(jsonPayload.toRequestBody(jsonMediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                QueryInstrumentation.log("PATCH", url, 1, true)
                Log.d(TAG, "Update successful for $tableName with query $queryParam")
                true
            } else {
                val body = response.body?.string()
                QueryInstrumentation.log("PATCH", url, 0, false, "Code ${response.code}: $body")
                Log.e(TAG, "Failed to PATCH $tableName: Code ${response.code}, Body: $body")
                false
            }
        } catch (ex: Exception) {
            QueryInstrumentation.log("PATCH", url, 0, false, ex.message ?: ex.toString())
            Log.e(TAG, "Exception during PATCH to $tableName", ex)
            false
        }
    }

    /**
      * Delete a record in a Supabase table.
      */
    fun deleteRow(tableName: String, queryParam: String, schemaProfile: String = "jarvis_insights_schema"): Boolean {
        val url = "$SUPABASE_URL/rest/v1/$tableName?$queryParam"
        Log.d(TAG, "DELETE request to: $url (Schema: $schemaProfile)")

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Profile", schemaProfile)
            .addHeader("Accept-Profile", schemaProfile)
            .delete()
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                QueryInstrumentation.log("DELETE", url, 1, true)
                Log.d(TAG, "Delete successful from $tableName with query $queryParam")
                true
            } else {
                val body = response.body?.string()
                QueryInstrumentation.log("DELETE", url, 0, false, "Code ${response.code}: $body")
                Log.e(TAG, "Failed to DELETE from $tableName: Code ${response.code}, Body: $body")
                false
            }
        } catch (ex: Exception) {
            QueryInstrumentation.log("DELETE", url, 0, false, ex.message ?: ex.toString())
            Log.e(TAG, "Exception during DELETE from $tableName", ex)
            false
        }
    }
}

