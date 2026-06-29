package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.pradeep.jarviscollector.network.SupabaseUploader
import com.pradeep.jarviscollector.repository.MobileSignalRepository
import com.pradeep.jarviscollector.utils.JsonExporter

sealed class SyncResult {
    data class Success(val count: Int) : SyncResult()
    data class Failure(val error: String) : SyncResult()
    object NoData : SyncResult()
}

object SyncService {

    private const val TAG = "SyncService"

    suspend fun syncPendingSignals(
        context: Context
    ): SyncResult = withContext(Dispatchers.IO) {

        try {
            val pendingSignals =
                MobileSignalRepository
                    .getPendingSignals(
                        context
                    )

            if (
                pendingSignals.isEmpty()
            ) {

                Log.d(
                    TAG,
                    "No pending signals to sync."
                )

                return@withContext SyncResult.NoData
            }

            val jsonString =
                JsonExporter
                    .exportSignalsAsString(
                        pendingSignals
                    )

            val ownerName =
                com.pradeep.jarviscollector.utils.AppPreferences
                    .getOwnerName(context)

            val fileName =
                "incoming/${ownerName}_${System.currentTimeMillis()}.json"

            val uploadResult =
                SupabaseUploader
                    .uploadJson(
                        fileName,
                        jsonString
                    )

            Log.d(
                TAG,
                "Upload response: $uploadResult"
            )

            if (
                uploadResult.contains(
                    "HTTP: 200"
                ) ||
                uploadResult.contains(
                    "HTTP: 201"
                )
            ) {

                val ids =
                    pendingSignals.map {
                        it.id
                    }

                MobileSignalRepository
                    .markSynced(
                        context,
                        ids
                    )

                Log.d(
                    TAG,
                    "Synced ${ids.size} signals."
                )

                return@withContext SyncResult.Success(ids.size)

            } else {

                Log.e(
                    TAG,
                    "Upload failed: $uploadResult"
                )

                return@withContext SyncResult.Failure("Upload failed: $uploadResult")
            }

        } catch (
            ex: Exception
        ) {

            Log.e(
                TAG,
                "Error syncing: ${ex.message}",
                ex
            )

            return@withContext SyncResult.Failure(ex.message ?: "Unknown error")
        }
    }
}
