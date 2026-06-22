package com.pradeep.jarviscollector.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pradeep.jarviscollector.repository.TodoRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodoNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("TodoNotificationWorker", "Running todo notification check...")
        
        try {
            val todos = TodoRepository.getPendingTodos(applicationContext)
            if (todos.isEmpty()) {
                Log.d("TodoNotificationWorker", "No pending todos found.")
                return Result.success()
            }

            val todayStr =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date())
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowStr =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(calendar.time)

            var notificationId = 2000

            for (todo in todos) {
                val dueDate = todo.due_date
                if (dueDate == todayStr) {
                    sendNotification(
                        id = notificationId++,
                        title = "Task Due Today",
                        content = todo.title ?: "Untitled Task",
                        priority = NotificationCompat.PRIORITY_HIGH
                    )
                } else if (dueDate == tomorrowStr) {
                    sendNotification(
                        id = notificationId++,
                        title = "Task Due Tomorrow",
                        content = todo.title ?: "Untitled Task",
                        priority = NotificationCompat.PRIORITY_DEFAULT
                    )
                }
            }
        } catch (ex: Exception) {
            Log.e(
                "TodoNotificationWorker",
                "Error sending notifications: ${ex.message}",
                ex
            )
        } finally {
            TodoNotificationHelper.initialize(applicationContext)
        }
        
        return Result.success()
    }

    private fun sendNotification(
        id: Int,
        title: String,
        content: String,
        priority: Int
    ) {
        val channelId = "todo_reminders"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Todo Reminders"
            val desc = "Notifications for tasks due today or tomorrow"
            val importance = if (priority == NotificationCompat.PRIORITY_HIGH) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_DEFAULT
            }
            val channel =
                NotificationChannel(channelId, name, importance).apply {
                    description = desc
                }
            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(priority)
            .setAutoCancel(true)

        try {
            val manager =
                NotificationManagerCompat.from(applicationContext)
            
            manager.notify(id, builder.build())
            
            Log.d(
                "TodoNotificationWorker",
                "Posted notification: $title - $content"
            )
        } catch (e: SecurityException) {
            Log.w(
                "TodoNotificationWorker",
                "Permission missing for notification posting",
                e
            )
        }
    }
}
