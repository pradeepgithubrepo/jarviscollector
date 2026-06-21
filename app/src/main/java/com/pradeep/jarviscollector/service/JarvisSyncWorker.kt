package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.pradeep.jarviscollector.repository.SmsRepository

class JarvisSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        Log.d(
            "JarvisSyncWorker",
            "Starting scheduled sync background task..."
        )

        var isSuccess = false

        try {
            // 1. Import SMS inbox
            SmsRepository
                .importRecentSmsToRoom(
                    applicationContext
                )

            // 2. Upload pending signals & mark synced
            val result =
                SyncService
                    .syncPendingSignals(
                        applicationContext
                    )

            isSuccess = result is SyncResult.Success || result is SyncResult.NoData

        } catch (
            ex: Exception
        ) {

            Log.e(
                "JarvisSyncWorker",
                "Error in sync task: ${ex.message}",
                ex
            )

        } finally {

            // Schedule the next sync target run
            JarvisSyncWorkerHelper
                .scheduleNextSync(
                    applicationContext
                )
        }

        return if (
            isSuccess
        ) {

            Result.success()

        } else {

            Result.retry()
        }
    }
}
