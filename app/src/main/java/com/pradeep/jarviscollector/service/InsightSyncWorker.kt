package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class InsightSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {
        Log.d(
            "InsightSyncWorker",
            "Starting scheduled insights background sync task..."
        )

        var isSuccess = false

        try {
            val result =
                InsightSyncService
                    .syncInsights(
                        applicationContext
                    )

            isSuccess =
                result is InsightSyncResult.Success

            if (isSuccess) {
                val successData =
                    result as InsightSyncResult.Success

                Log.d(
                    "InsightSyncWorker",
                    "Insights sync succeeded: brief=${successData.briefCount}, todos=${successData.todoCount}, financial=${successData.financialCount}, fyi=${successData.fyiCount}"
                )
            } else {
                val failData =
                    result as InsightSyncResult.Failure

                Log.e(
                    "InsightSyncWorker",
                    "Insights sync failed: ${failData.error}"
                )
            }

        } catch (
            ex: Exception
        ) {
            Log.e(
                "InsightSyncWorker",
                "Error in insights background sync task: ${ex.message}",
                ex
            )

        } finally {
            // Schedule the next target run for 06:20 AM
            InsightSyncWorkerHelper
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
