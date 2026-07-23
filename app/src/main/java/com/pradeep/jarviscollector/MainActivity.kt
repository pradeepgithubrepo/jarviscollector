package com.pradeep.jarviscollector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.pradeep.jarviscollector.model.MobileSignal
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.DailyBriefEntity
import com.pradeep.jarviscollector.model.FactInsightEntity
import com.pradeep.jarviscollector.model.NotificationEntity
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import com.pradeep.jarviscollector.model.UserActionEntity
import com.pradeep.jarviscollector.model.FinancialInsightEntity
import com.pradeep.jarviscollector.model.ReminderEntity
import com.pradeep.jarviscollector.repository.FinancialInsightRepository
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.repository.MobileSignalRepository
import com.pradeep.jarviscollector.repository.NotificationRepository
import com.pradeep.jarviscollector.repository.TodoRepository
import com.pradeep.jarviscollector.repository.FinancialRepository
import com.pradeep.jarviscollector.repository.FYIRepository
import com.pradeep.jarviscollector.repository.PreferenceRepository
import com.pradeep.jarviscollector.repository.ActionsRepository
import com.pradeep.jarviscollector.repository.FactRepository
import com.pradeep.jarviscollector.repository.NotificationCenterRepository
import com.pradeep.jarviscollector.ui.NotificationScreen
import com.pradeep.jarviscollector.ui.HomeScreen
import com.pradeep.jarviscollector.ui.TodoScreen
import com.pradeep.jarviscollector.ui.FinancialScreen
import com.pradeep.jarviscollector.ui.FyiScreen
import com.pradeep.jarviscollector.ui.DailyBriefScreen
import com.pradeep.jarviscollector.ui.theme.JarvisTheme
import com.pradeep.jarviscollector.utils.JsonExporter
import com.pradeep.jarviscollector.utils.AppPreferences
import com.pradeep.jarviscollector.repository.SmsRepository
import com.pradeep.jarviscollector.service.JarvisSyncWorkerHelper
import com.pradeep.jarviscollector.service.SyncService
import com.pradeep.jarviscollector.service.SyncResult
import com.pradeep.jarviscollector.navigation.*
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.launch
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set native OS window background & status bar colors to dark theme
        window.statusBarColor = android.graphics.Color.parseColor("#0A0F1E")
        window.navigationBarColor = android.graphics.Color.parseColor("#0A0F1E")

        // Initialize WorkManager background sync schedule
        JarvisSyncWorkerHelper.initialize(applicationContext)

        // Initialize WorkManager background insights sync schedule
        com.pradeep.jarviscollector.service.InsightSyncWorkerHelper.initialize(applicationContext)

        // Initialize WorkManager background todo notification reminders schedule
        com.pradeep.jarviscollector.service.TodoNotificationHelper.initialize(applicationContext)

        // Restore scheduled AlarmManager events
        com.pradeep.jarviscollector.service.JarvisReminderManager.restoreAllActiveAlarms(applicationContext)

        androidx.core.app.ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.POST_NOTIFICATIONS
            ),
            101
        )

        lifecycleScope.launch {
            try {
                SmsRepository.importRecentSmsToRoom(applicationContext)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        setContent {
            var showCreateTodoDialog by remember { mutableStateOf(false) }
            var todoPrefillTitle by remember { mutableStateOf("") }
            var todoPrefillDescription by remember { mutableStateOf("") }
            var todoPrefillPriority by remember { mutableStateOf("MEDIUM") }
            var todoPrefillReminderTime by remember { mutableStateOf<Long?>(null) }
            val context = LocalContext.current

            val todosFlow = remember { TodoRepository.getTodosFlow(applicationContext) }
            val todos by todosFlow.collectAsState(initial = emptyList())
            var financialEvents by remember { mutableStateOf(emptyList<FinancialEventEntity>()) }

            val allInsightsFlow = remember { FinancialInsightRepository.observeInsights(applicationContext) }
            val allInsights by allInsightsFlow.collectAsState(initial = emptyList())

            val reminderDao = remember { JarvisDatabase.getDatabase(applicationContext).reminderDao() }
            val reminders by reminderDao.getAllFlow().collectAsState(initial = emptyList())
            
            val snapshotInsights = remember(allInsights) { allInsights.filter { it.type?.lowercase() == "snapshot" } }

            val actionRequiredInsights = remember(allInsights) { allInsights.filter {
                val t = it.type?.lowercase() ?: ""
                val isAction = t == "action_required" || t == "upcoming_bill" || t == "emi"
                isAction && (it.status?.uppercase() == "PENDING")
            } }
            val subscriptionInsights = remember(allInsights) { allInsights.filter { it.type?.lowercase() == "subscription" } }
            val upcomingBillInsights = remember(allInsights) { allInsights.filter { it.type?.lowercase() == "bill" } }
            val unusualActivityInsights = remember(allInsights) { allInsights.filter { it.type?.lowercase() == "unusual" } }
            var fyiEvents by remember { mutableStateOf(emptyList<FyiEventEntity>()) }
            var latestBrief by remember { mutableStateOf<DailyBriefEntity?>(null) }
            var facts by remember { mutableStateOf(emptyList<FactInsightEntity>()) }
            var notifications by remember { mutableStateOf(emptyList<NotificationEntity>()) }
            var preferences by remember { mutableStateOf(emptyList<UserPreferenceEntity>()) }
            var userActions by remember { mutableStateOf(emptyList<UserActionEntity>()) }
            var roomSignals by remember { mutableStateOf(emptyList<MobileSignal>()) }
            var exportPath by remember { mutableStateOf("") }
            var isSyncing by remember { mutableStateOf(false) }
            var syncResultMessage by remember { mutableStateOf<String?>(null) }
            var ownerName by remember { mutableStateOf(AppPreferences.getOwnerName(applicationContext).ifBlank { "Pradeep" }) }
            var isSyncingInsights by remember { mutableStateOf(false) }
            var insightSyncResultMessage by remember { mutableStateOf<String?>(null) }
            var isBackfilling by remember { mutableStateOf(false) }
            var backfillStep by remember { mutableStateOf<String?>(null) }
            var backfillResultMessage by remember { mutableStateOf<String?>(null) }
            var backfillCompleted by remember {
                mutableStateOf(AppPreferences.isHistoricalBackfillCompleted(applicationContext))
            }

            fun refreshInsights() {
                lifecycleScope.launch {
                    financialEvents = FinancialRepository.getFinancialEvents(applicationContext)
                    fyiEvents = FYIRepository.getFyiEvents(applicationContext)
                    latestBrief = JarvisDatabase.getDatabase(applicationContext).dailyBriefDao().getLatest()
                    facts = FactRepository.getFacts(applicationContext)
                    notifications = NotificationCenterRepository.getNotifications(applicationContext)
                    preferences = PreferenceRepository.getPreferences(applicationContext)
                    userActions = ActionsRepository.getActions(applicationContext)
                }
            }

            // Voice Speech-To-Todo Launcher
            val speechLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
                onResult = { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                        if (!spokenText.isNullOrBlank()) {
                            // Simple Voice Translation parsing logic
                            val calendar = Calendar.getInstance()
                            var hasTime = false
                            
                            // Check for tomorrow vs today
                            if (spokenText.contains("tomorrow", ignoreCase = true)) {
                                calendar.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            
                            // Check for explicit time e.g., "at 5 pm", "at 10 am", "at 3"
                            val timeRegex = "(?i)\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b".toRegex()
                            val match = timeRegex.find(spokenText)
                            if (match != null) {
                                hasTime = true
                                var hour = match.groupValues[1].toInt()
                                val minute = if (match.groupValues[2].isNotEmpty()) match.groupValues[2].toInt() else 0
                                val ampm = match.groupValues[3].lowercase(Locale.US)
                                
                                if (ampm == "pm" && hour < 12) hour += 12
                                if (ampm == "am" && hour == 12) hour = 0
                                
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                            }
                            
                            // If no time is explicitly spoken, default to End of Day (21:00 / 9:00 PM)
                            if (!hasTime) {
                                calendar.set(Calendar.HOUR_OF_DAY, 21) // 9:00 PM (EoD)
                                calendar.set(Calendar.MINUTE, 0)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                            }
                            
                            // Clean up title by removing date/time phrase
                            var cleanTitle = spokenText
                                .replace("(?i)\\btomorrow\\b".toRegex(), "")
                                .replace("(?i)\\btoday\\b".toRegex(), "")
                                .replace(timeRegex, "")
                                .replace("\\s+".toRegex(), " ")
                                .trim()
                                
                            if (cleanTitle.isBlank()) {
                                cleanTitle = "Voice Task"
                            } else {
                                cleanTitle = cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            }
                            
                            val generatedId = UUID.randomUUID().toString()
                            val timestampSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                            val nowStr = timestampSdf.format(Date())
                            
                            val reminderTimestamp = calendar.timeInMillis
                            val rawRemIso = timestampSdf.format(Date(reminderTimestamp))
                            val computedDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(reminderTimestamp))
                            
                            val todo = TodoEntity(
                                todo_id = generatedId,
                                title = cleanTitle,
                                description = "Transcribed voice note: \"$spokenText\"",
                                category = "General",
                                priority = "MEDIUM",
                                status = "OPEN",
                                due_date = computedDueDate,
                                source_signal_id = null,
                                source_agent = "USER",
                                confidence = 1.0,
                                created_at = nowStr,
                                updated_at = nowStr,
                                reminder_datetime = rawRemIso
                            )
                            
                            lifecycleScope.launch {
                                val success = TodoRepository.createTodo(applicationContext, todo)
                                if (success) {
                                    val reminderEntity = ReminderEntity(
                                        reminder_id = generatedId,
                                        entity_type = "TODO",
                                        title = "Reminder: General",
                                        message = cleanTitle,
                                        scheduled_timestamp = reminderTimestamp,
                                        sound_type = "DEFAULT",
                                        action_route = "task_detail/$generatedId",
                                        action_payload = "{\"todo_id\":\"$generatedId\"}"
                                    )
                                    com.pradeep.jarviscollector.service.JarvisReminderManager.scheduleReminder(
                                        applicationContext,
                                        reminderEntity
                                    )
                                }
                                refreshInsights()
                            }
                        }
                    }
                }
            )

            LaunchedEffect(ownerName) {
                refreshInsights()
                lifecycleScope.launch {
                    try {
                        com.pradeep.jarviscollector.service.InsightSyncService.syncInsights(applicationContext)
                        refreshInsights()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Auto-sync failed on launch", e)
                    }
                }
            }

            JarvisTheme {
                val navController = rememberNavController()
                
                LaunchedEffect(navController) {
                    val route = intent.getStringExtra("navigate_route")
                    if (!route.isNullOrBlank()) {
                        try {
                            navController.navigate(route)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to deep link to route: $route", e)
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        val hideBottomBar = currentRoute == Screen.Splash.route
                        if (!hideBottomBar) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    JarvisNavHost(
                        navController = navController,
                        ownerName = ownerName,
                        todos = todos,
                        reminders = reminders,
                        snapshotInsights = snapshotInsights,
                        actionRequiredInsights = actionRequiredInsights,
                        subscriptionInsights = subscriptionInsights,
                        upcomingBillInsights = upcomingBillInsights,
                        unusualActivityInsights = unusualActivityInsights,

                        financialEvents = financialEvents,
                        fyiEvents = fyiEvents,
                        latestBrief = latestBrief,
                        facts = facts,
                        onToggleFactRead = { id, readFlag ->
                            lifecycleScope.launch {
                                FactRepository.markFactRead(applicationContext, id, readFlag)
                                refreshInsights()
                            }
                        },
                        onMarkFyiRead = { id, readFlag ->
                            lifecycleScope.launch {
                                FYIRepository.markFyiRead(applicationContext, id, readFlag)
                                refreshInsights()
                            }
                        },
                        onDismissFyi = { id ->
                            lifecycleScope.launch {
                                FYIRepository.dismissFyi(applicationContext, id)
                                refreshInsights()
                            }
                        },
                        notifications = notifications,
                        onMarkNotificationRead = { id, readFlag ->
                            lifecycleScope.launch {
                                NotificationCenterRepository.markNotificationRead(applicationContext, id, readFlag)
                                refreshInsights()
                            }
                        },
                        onArchiveNotification = { id ->
                            lifecycleScope.launch {
                                NotificationCenterRepository.archiveNotification(applicationContext, id)
                                refreshInsights()
                            }
                        },
                        preferences = preferences,
                        userActions = userActions,
                        onTogglePreference = { key, enabled ->
                            lifecycleScope.launch {
                                PreferenceRepository.savePreference(applicationContext, key, enabled.toString())
                                refreshInsights()
                            }
                        },
                        onConfirmTransaction = { id ->
                            lifecycleScope.launch {
                                FinancialRepository.confirmTransaction(applicationContext, id)
                                refreshInsights()
                            }
                        },
                        onCorrectTransaction = { id, category, amount ->
                            lifecycleScope.launch {
                                FinancialRepository.correctTransaction(applicationContext, id, category, amount)
                                refreshInsights()
                            }
                        },
                        onConfirmInsight = { id ->
                            lifecycleScope.launch {
                                FinancialInsightRepository.confirmInsight(applicationContext, id)
                                refreshInsights()
                            }
                        },
                        onDismissInsight = { id ->
                            lifecycleScope.launch {
                                FinancialInsightRepository.dismissInsight(applicationContext, id)
                                refreshInsights()
                            }
                        },
                        onCorrectInsight = { id, category, amount ->
                            lifecycleScope.launch {
                                FinancialInsightRepository.correctInsight(applicationContext, id, category, amount)
                                refreshInsights()
                            }
                        },
                        roomSignals = roomSignals,
                        exportPath = exportPath,
                        isSyncing = isSyncing,
                        syncResultMessage = syncResultMessage,
                        isSyncingInsights = isSyncingInsights,
                        insightSyncResultMessage = insightSyncResultMessage,
                        isBackfilling = isBackfilling,
                        backfillStep = backfillStep,
                        backfillResultMessage = backfillResultMessage,
                        backfillCompleted = backfillCompleted,
                        onOwnerNameChange = { newName ->
                            ownerName = newName
                            AppPreferences.setOwnerName(applicationContext, newName)
                            refreshInsights()
                        },
                        onLoadInsights = {
                            lifecycleScope.launch {
                                com.pradeep.jarviscollector.service.InsightSyncService.syncInsights(applicationContext)
                                refreshInsights()
                            }
                        },
                        onCompleteTodo = { todoId ->
                            lifecycleScope.launch {
                                TodoRepository.markTodoComplete(applicationContext, todoId)
                                refreshInsights()
                            }
                        },
                        onAddTodoClick = {
                            showCreateTodoDialog = true
                        },
                        onVoiceTodoClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your task...")
                                }
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Speech recognition failed to start", e)
                            }
                        },
                        onConvertFactToTodo = { title, description, priority, reminderTime ->
                            todoPrefillTitle = title
                            todoPrefillDescription = description
                            todoPrefillPriority = priority
                            todoPrefillReminderTime = reminderTime
                            showCreateTodoDialog = true
                        },
                        onSwipeFactSoftDelete = { id ->
                            lifecycleScope.launch {
                                com.pradeep.jarviscollector.repository.FactRepository.deleteFactSoft(applicationContext, id)
                                refreshInsights()
                            }
                        },
                        onSwipeFactHardDelete = { id, callback ->
                            lifecycleScope.launch {
                                val success = com.pradeep.jarviscollector.repository.FactRepository.deleteFactHard(applicationContext, id)
                                if (success) {
                                    refreshInsights()
                                }
                                callback(success)
                            }
                        },
                        onSnoozeTodo = { todoId, durationMinutes ->
                            lifecycleScope.launch {
                                TodoRepository.snoozeTodo(applicationContext, todoId)
                                val todo = todos.find { it.todo_id == todoId }
                                if (todo != null) {
                                    val scheduledTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
                                    val reminderEntity = ReminderEntity(
                                        reminder_id = todoId,
                                        entity_type = "TODO",
                                        title = "Snoozed Task Alert",
                                        message = todo.title ?: "Snoozed task needs attention",
                                        scheduled_timestamp = scheduledTime,
                                        sound_type = "DEFAULT",
                                        action_route = "task_detail/$todoId",
                                        action_payload = "{\"todo_id\":\"$todoId\"}"
                                    )
                                    com.pradeep.jarviscollector.service.JarvisReminderManager.scheduleReminder(
                                        applicationContext,
                                        reminderEntity
                                    )
                                }
                                refreshInsights()
                            }
                        },
                        onSetReminder = { todoId, triggerTime, _, sound ->
                            lifecycleScope.launch {
                                val todo = todos.find { it.todo_id == todoId }
                                if (todo != null) {
                                     val remTitle = when (todo.priority?.uppercase(Locale.US)) {
                                         "CRITICAL", "URGENT" -> "⚠️ Urgent Task Alert"
                                         "HIGH" -> "🔔 High Priority Task"
                                         else -> "Jarvis Task Reminder"
                                     }
                                     val reminderEntity = ReminderEntity(
                                        reminder_id = todoId,
                                        entity_type = "TODO",
                                        title = remTitle,
                                        message = todo.title ?: "Upcoming task deadline",
                                        scheduled_timestamp = triggerTime,
                                        sound_type = sound,
                                        action_route = "task_detail/$todoId",
                                        action_payload = "{\"todo_id\":\"$todoId\"}"
                                    )
                                    com.pradeep.jarviscollector.service.JarvisReminderManager.scheduleReminder(
                                        applicationContext,
                                        reminderEntity
                                    )
                                }
                                refreshInsights()
                            }
                        },
                        onRemoveReminder = { todoId ->
                            lifecycleScope.launch {
                                com.pradeep.jarviscollector.service.JarvisReminderManager.cancelReminder(
                                    applicationContext,
                                    todoId
                                )
                                refreshInsights()
                            }
                        },
                        onDeleteTodo = { todoId ->
                            lifecycleScope.launch {
                                TodoRepository.deleteTodo(applicationContext, todoId)
                                refreshInsights()
                            }
                        },

                        onLoadRoom = {
                            lifecycleScope.launch {
                                roomSignals = MobileSignalRepository.getSignals(applicationContext)
                            }
                        },
                        onExportJson = {
                            lifecycleScope.launch {
                                roomSignals = MobileSignalRepository.getSignals(applicationContext)
                                exportPath = JsonExporter.exportSignals(applicationContext, roomSignals)
                            }
                        },
                        onSyncNow = {
                            lifecycleScope.launch {
                                isSyncing = true
                                syncResultMessage = null
                                try {
                                    val newSmsCount = SmsRepository.importRecentSmsToRoom(applicationContext)
                                    when (val result = SyncService.syncPendingSignals(applicationContext)) {
                                        is SyncResult.Success -> {
                                            syncResultMessage = "Imported $newSmsCount new SMS.\n\nSuccessfully uploaded ${result.count} signals to Supabase!"
                                        }
                                        is SyncResult.NoData -> {
                                            syncResultMessage = "Imported $newSmsCount new SMS.\n\nNo pending signals to upload."
                                        }
                                        is SyncResult.Failure -> {
                                            syncResultMessage = "Imported $newSmsCount new SMS.\n\nSync failed: ${result.error}"
                                        }
                                    }
                                } catch (ex: Exception) {
                                    syncResultMessage = "Error during sync: ${ex.message}"
                                } finally {
                                    isSyncing = false
                                    roomSignals = MobileSignalRepository.getSignals(applicationContext)
                                }
                            }
                        },
                        onDismissSyncResult = { syncResultMessage = null },
                        onDismissInsightSyncResult = { insightSyncResultMessage = null },
                        onSyncInsights = {
                            lifecycleScope.launch {
                                isSyncingInsights = true
                                insightSyncResultMessage = null
                                try {
                                    when (val result = com.pradeep.jarviscollector.service.InsightSyncService.syncInsights(applicationContext)) {
                                        is com.pradeep.jarviscollector.service.InsightSyncResult.Success -> {
                                            insightSyncResultMessage = "Insights synced successfully!\n\n" +
                                                    "Todos: ${result.todoCount}\n" +
                                                    "Financials: ${result.financialCount}\n" +
                                                    "FYIs: ${result.fyiCount}\n" +
                                                    "Preferences: ${result.prefCount}"
                                            refreshInsights()
                                        }
                                        is com.pradeep.jarviscollector.service.InsightSyncResult.Failure -> {
                                            insightSyncResultMessage = "Insights sync failed:\n${result.error}"
                                        }
                                    }
                                } catch (ex: Exception) {
                                    insightSyncResultMessage = "Error: ${ex.message}"
                                } finally {
                                    isSyncingInsights = false
                                }
                            }
                        },
                        onDismissBackfillResult = { backfillResultMessage = null },
                        onRunBackfillAgain = {
                            AppPreferences.setHistoricalBackfillCompleted(applicationContext, false)
                            backfillCompleted = false
                        },
                        onStartBackfill = {
                             lifecycleScope.launch {
                                 isBackfilling = true
                                 try {
                                     backfillStep = "Gathering WhatsApp History..."
                                     val whatsappSignals = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                         MobileSignalRepository.getSignalsBySource(applicationContext, "whatsapp") +
                                         MobileSignalRepository.getSignalsBySource(applicationContext, "whatsapp_business")
                                     }
                                     val whatsappCount = whatsappSignals.size
                                     val whatsappJson = JsonExporter.exportSignalsAsString(whatsappSignals)

                                     backfillStep = "Scraping SMS History (Last 3 Months)..."
                                     val threeMonthsAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000L)
                                     val smsSignals = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                         SmsRepository.readRecentSms(applicationContext, threeMonthsAgo)
                                     }
                                     val smsCount = smsSignals.size

                                     // Save all SMS signals to Room first (if they don't exist)
                                     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                         smsSignals.forEach { signal ->
                                             if (!MobileSignalRepository.exists(applicationContext, signal)) {
                                                 MobileSignalRepository.save(applicationContext, signal)
                                             }
                                         }
                                     }

                                     // Retrieve all SMS signals from Room from the last 3 months
                                     val localSmsSignals = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                         MobileSignalRepository.getSignalsBySource(applicationContext, "sms")
                                             .filter { it.timestamp >= threeMonthsAgo }
                                     }
                                     val smsJson = JsonExporter.exportSignalsAsString(localSmsSignals)

                                     backfillStep = "Uploading Historical Files..."
                                     val timestamp = System.currentTimeMillis()
                                     val whatsappFile = "incoming/${ownerName}_whatsapp_${timestamp}.json"
                                     val smsFile = "incoming/${ownerName}_sms_${timestamp}.json"

                                     val (resWhatsapp, resSms) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                         val w = com.pradeep.jarviscollector.network.SupabaseUploader.uploadJson(whatsappFile, whatsappJson)
                                         val s = com.pradeep.jarviscollector.network.SupabaseUploader.uploadJson(smsFile, smsJson)
                                         Pair(w, s)
                                     }

                                     if ((resWhatsapp.contains("HTTP: 200") || resWhatsapp.contains("HTTP: 201")) &&
                                         (resSms.contains("HTTP: 200") || resSms.contains("HTTP: 201"))) {

                                         // Mark uploaded signals as SYNCED locally
                                         kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                             val whatsappIds = whatsappSignals.map { it.id }
                                             val smsIds = localSmsSignals.map { it.id }
                                             MobileSignalRepository.markSynced(applicationContext, whatsappIds + smsIds)
                                         }

                                         backfillStep = "Historical Backfill Complete"
                                         AppPreferences.setHistoricalBackfillCompleted(applicationContext, true)
                                         backfillCompleted = true
                                         backfillResultMessage = "Successfully uploaded historical dumps to Supabase Storage jarvis-signals bucket!\n\nWhatsApp Records: $whatsappCount\n\nSMS Records: $smsCount"
                                     } else {
                                         backfillResultMessage = "Historical Backfill Failed during upload:\n\nWhatsApp upload result: $resWhatsapp\n\nSMS upload result: $resSms"
                                     }
                                 } catch (ex: Exception) {
                                     backfillResultMessage = "Historical Backfill Failed:\n\n${ex.message}"
                                 } finally {
                                     isBackfilling = false
                                     backfillStep = null
                                 }
                             }
                        },
                        onNavigateToSignalExplorer = { type, id ->
                            navController.navigate(Screen.SignalExplorer.createRoute(type, id))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                // Global Create Todo Dialog
                if (showCreateTodoDialog) {
                    var newTitle by remember { mutableStateOf(todoPrefillTitle) }
                    var newDescription by remember { mutableStateOf(todoPrefillDescription) }
                    var newPriority by remember { mutableStateOf(todoPrefillPriority) }
                    var newReminderTime by remember { mutableStateOf<Long?>(todoPrefillReminderTime) }
                    val context = LocalContext.current

                    fun resetPrefills() {
                        todoPrefillTitle = ""
                        todoPrefillDescription = ""
                        todoPrefillPriority = "MEDIUM"
                        todoPrefillReminderTime = null
                    }

                    AlertDialog(
                        onDismissRequest = { 
                            resetPrefills()
                            showCreateTodoDialog = false 
                        },
                        containerColor = Color(0xFF1E293B),
                        title = { Text("Create New Task", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            androidx.compose.foundation.layout.Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it },
                                    label = { Text("Task Title", color = Color(0xFF94A3B8)) },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedIndicatorColor = Color(0xFF6366F1)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newDescription,
                                    onValueChange = { newDescription = it },
                                    label = { Text("Description", color = Color(0xFF94A3B8)) },
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedIndicatorColor = Color(0xFF6366F1)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )

                                // Priority Selector
                                androidx.compose.foundation.layout.Column {
                                    Text("Priority", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { priority ->
                                            val isSel = newPriority == priority
                                            val activeColor = when (priority) {
                                                "URGENT" -> Color(0xFFEF4444)
                                                "HIGH" -> Color(0xFFF97316)
                                                "MEDIUM" -> Color(0xFFF59E0B)
                                                else -> Color(0xFF10B981)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1.0f)
                                                    .background(
                                                        if (isSel) activeColor else Color(0xFF334155),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { newPriority = priority }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = priority,
                                                    color = if (isSel) Color.White else Color(0xFFCBD5E1),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Reminder Alert Time Selector (Sequential Date + Time Picker)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reminder Alert", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                    Button(
                                        onClick = {
                                            val calendar = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    calendar.set(Calendar.YEAR, year)
                                                    calendar.set(Calendar.MONTH, month)
                                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                                    TimePickerDialog(
                                                        context,
                                                        { _, hour, minute ->
                                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                                            calendar.set(Calendar.MINUTE, minute)
                                                            calendar.set(Calendar.SECOND, 0)
                                                            calendar.set(Calendar.MILLISECOND, 0)
                                                            newReminderTime = calendar.timeInMillis
                                                        },
                                                        calendar.get(Calendar.HOUR_OF_DAY),
                                                        calendar.get(Calendar.MINUTE),
                                                        false
                                                    ).show()
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                    ) {
                                        val btnText = if (newReminderTime != null) {
                                            val sdf = SimpleDateFormat("dd-MMM h:mm a", Locale.US)
                                            sdf.format(Date(newReminderTime!!))
                                        } else {
                                            "Set Alert"
                                        }
                                        Text(btnText, color = Color.White)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newTitle.isNotBlank()) {
                                        val generatedId = UUID.randomUUID().toString()
                                        val timestampSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        }
                                        val nowStr = timestampSdf.format(Date())
                                        
                                        val rawRemIso = newReminderTime?.let { timestampSdf.format(Date(it)) }
                                        val computedDueDate = newReminderTime?.let {
                                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                                        }

                                        val todo = TodoEntity(
                                            todo_id = generatedId,
                                            title = newTitle,
                                            description = newDescription.ifBlank { null },
                                            category = "General",
                                            priority = newPriority,
                                            status = "OPEN",
                                            due_date = computedDueDate,
                                            source_signal_id = null,
                                            source_agent = "USER",
                                            confidence = 1.0,
                                            created_at = nowStr,
                                            updated_at = nowStr,
                                            reminder_datetime = rawRemIso
                                        )

                                        lifecycleScope.launch {
                                            val success = TodoRepository.createTodo(applicationContext, todo)
                                            if (success && newReminderTime != null) {
                                                 val remTitle = when (newPriority.uppercase(Locale.US)) {
                                                     "CRITICAL", "URGENT" -> "⚠️ Urgent Task Alert"
                                                     "HIGH" -> "🔔 High Priority Task"
                                                     else -> "Jarvis Task Reminder"
                                                 }
                                                 val reminderEntity = ReminderEntity(
                                                    reminder_id = generatedId,
                                                    entity_type = "TODO",
                                                    title = remTitle,
                                                    message = newTitle,
                                                    scheduled_timestamp = newReminderTime!!,
                                                    sound_type = "DEFAULT",
                                                    action_route = "task_detail/$generatedId",
                                                    action_payload = "{\"todo_id\":\"$generatedId\"}"
                                                )
                                                // Save locally and PATCH to Supabase tasks table
                                                com.pradeep.jarviscollector.service.JarvisReminderManager.scheduleReminder(
                                                    applicationContext,
                                                    reminderEntity
                                                )
                                            }
                                            refreshInsights()
                                        }
                                        resetPrefills()
                                        showCreateTodoDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Save", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                resetPrefills()
                                showCreateTodoDialog = false 
                            }) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    )
                }
            }
        }
    }
}