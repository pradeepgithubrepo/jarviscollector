package com.pradeep.jarviscollector.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pradeep.jarviscollector.MainActivity
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class JarvisReminderReceiver : BroadcastReceiver() {

    private val TAG = "JarvisReminderReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val title = intent.getStringExtra("title") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: "You have a new alert"
        val soundType = intent.getStringExtra("sound_type") ?: "DEFAULT"
        val actionRoute = intent.getStringExtra("action_route") ?: "home"
        val actionPayload = intent.getStringExtra("action_payload") ?: "{}"

        Log.d(TAG, "Triggered reminder alarm: id=$reminderId, title=$title")

        // 1. Write an entry into the local notifications Room table
        val db = JarvisDatabase.getDatabase(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val nowStr = sdf.format(Date())

        val notificationEntity = NotificationEntity(
            id = "reminder-$reminderId-${UUID.randomUUID()}",
            title = title,
            message = message,
            type = "REMINDER",
            priority = if (soundType == "URGENT") "HIGH" else "MEDIUM",
            status = "NEW",
            created_at = nowStr,
            action_route = actionRoute,
            action_payload = actionPayload,
            read_flag = false
        )

        // Run db insert on IO thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.notificationDao().insert(notificationEntity)
                // Also clean up this reminder from local_reminders list as it has fired
                db.reminderDao().deleteById(reminderId)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting notification logs", e)
            }
        }

        // 2. Play Sound alert & Vibration
        triggerVibration(context, soundType)

        // 3. Post System Notification
        postSystemNotification(context, reminderId, title, message, soundType, actionRoute, actionPayload)
    }

    private fun triggerVibration(context: Context, soundType: String) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            when (soundType.uppercase(Locale.US)) {
                "SILENT" -> {
                    // Do nothing
                }
                "GENTLE" -> {
                    val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                }
                "URGENT" -> {
                    // Double pulse
                    val pattern = longArrayOf(0, 300, 100, 300)
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(effect)
                }
                else -> { // DEFAULT
                    val effect = VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    private fun postSystemNotification(
        context: Context,
        id: String,
        title: String,
        message: String,
        soundType: String,
        actionRoute: String,
        actionPayload: String
    ) {
        val channelId = "jarvis_alerts_${soundType.lowercase(Locale.US)}"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Setup notification channel with sound properties
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Jarvis Alerts (${soundType})"
            val importance = if (soundType == "URGENT") {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Generic system notification alerts"
                enableVibration(soundType != "SILENT")
            }

            // Bind sound to channel if not silent
            if (soundType != "SILENT") {
                val soundUri = getSoundUri(soundType)
                if (soundUri != null) {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    channel.setSound(soundUri, attributes)
                }
            } else {
                channel.setSound(null, null)
            }

            manager.createNotificationChannel(channel)
        }

        // Setup tap action deep-link intent
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_route", actionRoute)
            putExtra("navigate_payload", actionPayload)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (soundType == "URGENT") NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        if (soundType != "SILENT") {
            val soundUri = getSoundUri(soundType)
            if (soundUri != null) {
                builder.setSound(soundUri)
            }
        }

        manager.notify(id.hashCode(), builder.build())
    }

    private fun getSoundUri(soundType: String): Uri? {
        return when (soundType.uppercase(Locale.US)) {
            "GENTLE" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "URGENT" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) // Default
        }
    }

    companion object {
        fun triggerTestNotification(context: Context) {
            val receiver = JarvisReminderReceiver()
            val id = "test-notif-${System.currentTimeMillis()}"
            receiver.postSystemNotification(
                context,
                id = id,
                title = "Jarvis Notification Test",
                message = "This is a diagnostic test alert checking the notification channel.",
                soundType = "DEFAULT",
                actionRoute = "home",
                actionPayload = "{}"
            )
            
            val db = JarvisDatabase.getDatabase(context)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val nowStr = sdf.format(Date())
            val notificationEntity = NotificationEntity(
                id = id,
                title = "Jarvis Notification Test",
                message = "This is a diagnostic test alert checking the notification channel.",
                type = "REMINDER",
                priority = "MEDIUM",
                status = "NEW",
                created_at = nowStr,
                action_route = "home",
                action_payload = "{}",
                read_flag = false
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.notificationDao().insert(notificationEntity)
                } catch (e: Exception) {
                    Log.e("JarvisReminderReceiver", "Error inserting test notification log", e)
                }
            }
        }
    }
}
