package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object InsightSyncWorkerHelper {

    private const val TAG = "InsightSyncWorkerHelper"
    private const val WORK_NAME = "JarvisInsightSyncWork"

    fun calculateDelayToNextTarget(): Long {
        val now =
            Calendar.getInstance()

        val target =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 20)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        if (
            target.before(now)
        ) {
            // Target has passed for today, schedule for tomorrow
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay =
            target.timeInMillis -
                    now.timeInMillis

        Log.d(
            TAG,
            "Next insights sync target: ${target.time}. Delay (ms): $delay"
        )

        return delay
    }

    fun initialize(
        context: Context
    ) {
        val delay =
            calculateDelayToNextTarget()

        val syncRequest =
            OneTimeWorkRequestBuilder<InsightSyncWorker>()
                .setInitialDelay(
                    delay,
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            NetworkType.CONNECTED
                        )
                        .build()
                )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

        Log.d(
            TAG,
            "Initialized background insights sync schedule with KEEP policy."
        )
    }

    fun scheduleNextSync(
        context: Context
    ) {
        val delay =
            calculateDelayToNextTarget()

        val syncRequest =
            OneTimeWorkRequestBuilder<InsightSyncWorker>()
                .setInitialDelay(
                    delay,
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            NetworkType.CONNECTED
                        )
                        .build()
                )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

        Log.d(
            TAG,
            "Scheduled next background insights sync target with REPLACE policy."
        )
    }
}
