package com.pradeep.jarviscollector.service

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TodoNotificationHelper {

    private const val TAG = "TodoNotificationHelper"
    private const val WORK_NAME_MORNING = "JarvisTodoNotificationMorning"
    private const val WORK_NAME_EVENING = "JarvisTodoNotificationEvening"

    fun initialize(context: Context) {
        scheduleMorningCheck(context)
        scheduleEveningCheck(context)
    }

    private fun scheduleMorningCheck(context: Context) {
        val delay = calculateDelayToTarget(7, 0)
        
        val request =
            OneTimeWorkRequestBuilder<TodoNotificationWorker>()
                .setInitialDelay(
                    delay,
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME_MORNING,
                ExistingWorkPolicy.REPLACE,
                request
            )

        Log.d(
            TAG,
            "Scheduled morning todo reminder in $delay ms"
        )
    }

    private fun scheduleEveningCheck(context: Context) {
        val delay = calculateDelayToTarget(18, 0)
        
        val request =
            OneTimeWorkRequestBuilder<TodoNotificationWorker>()
                .setInitialDelay(
                    delay,
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME_EVENING,
                ExistingWorkPolicy.REPLACE,
                request
            )

        Log.d(
            TAG,
            "Scheduled evening todo reminder in $delay ms"
        )
    }

    private fun calculateDelayToTarget(
        hour: Int,
        minute: Int
    ): Long {
        val now =
            Calendar.getInstance()

        val target =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        if (
            target.before(now)
        ) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
