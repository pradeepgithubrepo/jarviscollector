package com.pradeep.jarviscollector.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object JarvisReminderManager {

    private const val TAG = "JarvisReminderManager"

    private fun setAlarmInOS(context: Context, alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                Log.w(TAG, "Exact alarm permission not granted. Falling back to setAndAllowWhileIdle (inexact).")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Save reminder to local DB + sync to Supabase tasks table
                com.pradeep.jarviscollector.repository.TodoRepository.setReminder(context, reminder)
                
                // 2. Set Alarm in OS AlarmManager
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, JarvisReminderReceiver::class.java).apply {
                    putExtra("reminder_id", reminder.reminder_id)
                    putExtra("title", reminder.title)
                    putExtra("message", reminder.message)
                    putExtra("sound_type", reminder.sound_type)
                    putExtra("action_route", reminder.action_route)
                    putExtra("action_payload", reminder.action_payload)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.reminder_id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                setAlarmInOS(context, alarmManager, reminder.scheduled_timestamp, pendingIntent)
                
                Log.d(TAG, "Successfully scheduled alarm for target: ${reminder.title} at ${reminder.scheduled_timestamp}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm for reminder ID: ${reminder.reminder_id}", e)
            }
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Remove from local DB + clear on Supabase tasks table
                com.pradeep.jarviscollector.repository.TodoRepository.deleteReminder(context, reminderId)

                // 2. Cancel alarm in OS AlarmManager
                cancelAlarmSlot(context, reminderId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel alarm: $reminderId", e)
            }
        }
    }

    // Local-only configuration (used during background pulls from Supabase tasks)
    fun scheduleReminderLocally(context: Context, reminder: ReminderEntity) {
        val db = JarvisDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Save locally to Room
                db.reminderDao().insert(reminder)
                
                // 2. Set Alarm in OS AlarmManager
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, JarvisReminderReceiver::class.java).apply {
                    putExtra("reminder_id", reminder.reminder_id)
                    putExtra("title", reminder.title)
                    putExtra("message", reminder.message)
                    putExtra("sound_type", reminder.sound_type)
                    putExtra("action_route", reminder.action_route)
                    putExtra("action_payload", reminder.action_payload)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.reminder_id.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                setAlarmInOS(context, alarmManager, reminder.scheduled_timestamp, pendingIntent)
                Log.d(TAG, "[Local] Scheduled alarm for: ${reminder.title} at ${reminder.scheduled_timestamp}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule local alarm: ${reminder.reminder_id}", e)
            }
        }
    }

    fun cancelReminderLocally(context: Context, reminderId: String) {
        val db = JarvisDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Remove locally from Room
                db.reminderDao().deleteById(reminderId)

                // 2. Cancel OS alarm
                cancelAlarmSlot(context, reminderId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel local alarm: $reminderId", e)
            }
        }
    }

    private fun cancelAlarmSlot(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, JarvisReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled OS AlarmManager slot: $reminderId")
        }
    }

    // Called on boot completed or app launch to restore schedules
    fun restoreAllActiveAlarms(context: Context) {
        val db = JarvisDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val activeReminders = db.reminderDao().getAll()
                
                for (reminder in activeReminders) {
                    if (reminder.scheduled_timestamp > now) {
                        scheduleReminderLocally(context, reminder)
                    } else {
                        // Past alert, clean up from db
                        db.reminderDao().deleteById(reminder.reminder_id)
                    }
                }
                Log.d(TAG, "Restored ${activeReminders.size} active system alarms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore alarms", e)
            }
        }
    }
}
