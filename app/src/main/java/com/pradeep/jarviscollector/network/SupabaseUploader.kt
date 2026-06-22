package com.pradeep.jarviscollector.network

import com.pradeep.jarviscollector.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object SupabaseUploader {

    private val SUPABASE_URL = BuildConfig.SUPABASE_URL

    private const val BUCKET_NAME =
        "jarvis-signals"

    private val ANON_KEY = BuildConfig.SUPABASE_KEY



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