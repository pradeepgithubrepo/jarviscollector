package com.pradeep.jarviscollector.utils

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "jarvis_collector_prefs"
    private const val KEY_OWNER_NAME = "owner_name"
    private const val KEY_LAST_SMS_IMPORT_TIMESTAMP = "last_sms_import_timestamp"

    fun getOwnerName(
        context: Context
    ): String {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        return prefs
            .getString(
                KEY_OWNER_NAME,
                "pradeep"
            )
            ?: "pradeep"
    }

    fun setOwnerName(
        context: Context,
        name: String
    ) {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        prefs
            .edit()
            .putString(
                KEY_OWNER_NAME,
                name.lowercase().trim()
            )
            .apply()
    }

    fun getLastSmsImportTimestamp(
        context: Context
    ): Long {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        return prefs.getLong(KEY_LAST_SMS_IMPORT_TIMESTAMP, 0L)
    }

    fun setLastSmsImportTimestamp(
        context: Context,
        timestamp: Long
    ) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        prefs.edit()
            .putLong(KEY_LAST_SMS_IMPORT_TIMESTAMP, timestamp)
            .apply()
    }
}
