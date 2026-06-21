package com.pradeep.jarviscollector.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SupabaseUploader {

    private const val SUPABASE_URL =
        "https://tbwnyuampjoamgarwwoo.supabase.co"

    private const val BUCKET_NAME =
        "jarvis-signals"

    private const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRid255dWFtcGpvYW1nYXJ3d29vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE5MzUwOTYsImV4cCI6MjA5NzUxMTA5Nn0.3CdCtROBH2l0wq8GVir9_3rWWZUtD9w2UWsz9caM3cg"


    fun uploadJson(
        fileName: String,
        jsonContent: String
    ): String {

        return try {

            val url =
                "$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$fileName"

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
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .post(
                        jsonContent.toRequestBody(
                            "application/json"
                                .toMediaType()
                        )
                    )
                    .build()

            val response =
                OkHttpClient()
                    .newCall(request)
                    .execute()

            """
HTTP: ${response.code}

BODY:
${response.body?.string()}
            """.trimIndent()

        } catch (
            ex: Exception
        ) {

            """
ERROR:
${ex.javaClass.simpleName}

MESSAGE:
${ex.message}
            """.trimIndent()
        }
    }
}