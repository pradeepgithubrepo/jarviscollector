package com.pradeep.jarviscollector.network

import androidx.compose.runtime.mutableStateListOf

object QueryInstrumentation {
    data class QueryRecord(
        val timestamp: Long,
        val method: String,
        val url: String,
        val rowCount: Int,
        val success: Boolean,
        val error: String?
    )

    val records = mutableStateListOf<QueryRecord>()

    fun log(method: String, url: String, rowCount: Int, success: Boolean, error: String? = null) {
        records.add(
            QueryRecord(
                timestamp = System.currentTimeMillis(),
                method = method,
                url = url,
                rowCount = rowCount,
                success = success,
                error = error
            )
        )
        // Keep only last 50 queries to prevent memory leaks
        if (records.size > 50) {
            records.removeAt(0)
        }
    }
}
