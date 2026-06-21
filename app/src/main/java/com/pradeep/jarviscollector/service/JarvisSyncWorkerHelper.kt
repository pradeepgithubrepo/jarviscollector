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

object JarvisSyncWorkerHelper {

    private const val TAG = "JarvisSyncWorkerHelper"
    private const val WORK_NAME = "JarvisSyncWork"

    fun calculateDelayToNextTarget(): Long {

        val now =
            Calendar.getInstance()

        val targets =
            listOf(
                // 05:55 AM
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 5)
                    set(Calendar.MINUTE, 55)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                },
                // 01:55 PM (13:55)
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 55)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                },
                // 08:55 PM (20:55)
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 55)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            )

        var nextTarget: Calendar? = null

        for (
            target in targets
        ) {

            if (
                target.after(now)
            ) {

                nextTarget = target
                break
            }
        }

        if (
            nextTarget == null
        ) {

            // Next run is tomorrow morning 05:55 AM
            nextTarget =
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 5)
                    set(Calendar.MINUTE, 55)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
        }

        val delay =
            nextTarget.timeInMillis -
                    now.timeInMillis

        Log.d(
            TAG,
            "Next sync target: ${nextTarget.time}. Delay (ms): $delay"
        )

        return delay
    }

    fun initialize(
        context: Context
    ) {

        val delay =
            calculateDelayToNextTarget()

        val syncRequest =
            OneTimeWorkRequestBuilder<JarvisSyncWorker>()
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

        // KEEP ensures we do not clear out/restart already scheduled work on app relaunch
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

        Log.d(
            TAG,
            "Initialized background sync schedule with KEEP policy."
        )
    }

    fun scheduleNextSync(
        context: Context
    ) {

        val delay =
            calculateDelayToNextTarget()

        val syncRequest =
            OneTimeWorkRequestBuilder<JarvisSyncWorker>()
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

        // REPLACE updates the scheduled task with the newly calculated delay
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

        Log.d(
            TAG,
            "Scheduled next background sync target with REPLACE policy."
        )
    }
}
