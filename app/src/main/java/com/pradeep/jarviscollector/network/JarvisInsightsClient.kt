package com.pradeep.jarviscollector.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object JarvisInsightsClient {

    private const val SUPABASE_URL =
        "https://tbwnyuampjoamgarwwoo.supabase.co"

    private const val BUCKET_NAME =
        "jarvis-signals"

    private const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRid255dWFtcGpvYW1nYXJ3d29vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE5MzUwOTYsImV4cCI6MjA5NzUxMTA5Nn0.3CdCtROBH2l0wq8GVir9_3rWWZUtD9w2UWsz9caM3cg"

    private val client = OkHttpClient()

    fun downloadInsightJson(
        ownerName: String,
        fileName: String
    ): String? {
        val url =
            "$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$ownerName/insights/$fileName"
        
        Log.d(
            "JarvisInsightsClient",
            "Downloading insight from: $url"
        )

        val request =
            Request.Builder()
                .url(url)
                .addHeader(
                    "apikey",
                    ANON_KEY
                )
                .addHeader(
                    "Authorization",
                    "Bearer $ANON_KEY"
                )
                .get()
                .build()

        return try {
            val response =
                client
                    .newCall(request)
                    .execute()

            if (response.isSuccessful) {
                val body =
                    response.body?.string()

                Log.d(
                    "JarvisInsightsClient",
                    "Downloaded $fileName successfully. Body length: ${body?.length ?: 0}"
                )

                body
            } else {
                Log.w(
                    "JarvisInsightsClient",
                    "Failed to download $fileName: HTTP ${response.code}"
                )

                null
            }
        } catch (
            ex: Exception
        ) {
            Log.e(
                "JarvisInsightsClient",
                "Error downloading $fileName: ${ex.message}",
                ex
            )

            null
        }
    }
}
