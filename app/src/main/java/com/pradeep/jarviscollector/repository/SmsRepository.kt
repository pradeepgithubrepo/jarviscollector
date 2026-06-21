package com.pradeep.jarviscollector.repository

import android.content.Context
import android.provider.Telephony
import android.util.Log

import com.pradeep.jarviscollector.model.MobileSignal
import com.pradeep.jarviscollector.utils.AppPreferences

object SmsRepository {

    private const val TAG = "SmsRepository"

    fun readRecentSms(
        context: Context,
        sinceTimestamp: Long = 0L
    ): List<MobileSignal> {

        val signals =
            mutableListOf<MobileSignal>()

        try {
            val selection = "${Telephony.Sms.DATE} > ?"
            val selectionArgs = arrayOf(sinceTimestamp.toString())

            val cursor =
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    "date DESC"
                )

            cursor?.use {
                while (
                    it.moveToNext()
                ) {
                    val sender =
                        it.getString(
                            it.getColumnIndexOrThrow(
                                Telephony.Sms.ADDRESS
                            )
                        ) ?: ""

                    val body =
                        it.getString(
                            it.getColumnIndexOrThrow(
                                Telephony.Sms.BODY
                            )
                        ) ?: ""

                    val date =
                        it.getLong(
                            it.getColumnIndexOrThrow(
                                Telephony.Sms.DATE
                            )
                        )

                    val ownerName =
                        com.pradeep.jarviscollector.utils.AppPreferences
                            .getOwnerName(context)

                    signals.add(
                        MobileSignal(
                            deviceId =
                                "${ownerName}_phone",
                            source =
                                "sms",
                            sender =
                                sender,
                            message =
                                body,
                            timestamp =
                                date
                        )
                    )
                }
            }
        } catch (ex: SecurityException) {
            Log.e(TAG, "SecurityException: READ_SMS permission not granted", ex)
        } catch (ex: Exception) {
            Log.e(TAG, "Error reading SMS inbox: ${ex.message}", ex)
        }

        return signals
    }

    suspend fun importRecentSmsToRoom(
        context: Context
    ): Int {
        val lastImportTime = com.pradeep.jarviscollector.utils.AppPreferences
            .getLastSmsImportTimestamp(context)

        // If first import, look back 24 hours
        val sinceTime = if (lastImportTime == 0L) {
            System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        } else {
            lastImportTime
        }

        val smsSignals = readRecentSms(context, sinceTime)
        var newCount = 0
        var maxTimestamp = lastImportTime

        smsSignals.forEach { signal ->
            if (!MobileSignalRepository.exists(context, signal)) {
                MobileSignalRepository.save(context, signal)
                newCount++
            }
            if (signal.timestamp > maxTimestamp) {
                maxTimestamp = signal.timestamp
            }
        }

        if (maxTimestamp > lastImportTime) {
            com.pradeep.jarviscollector.utils.AppPreferences
                .setLastSmsImportTimestamp(context, maxTimestamp)
        }

        Log.d(TAG, "Imported $newCount new SMS into Room database out of ${smsSignals.size} delta inbox messages.")
        return newCount
    }
}