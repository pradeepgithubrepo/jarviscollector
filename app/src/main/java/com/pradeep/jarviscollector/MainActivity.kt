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
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize WorkManager background sync schedule
        JarvisSyncWorkerHelper.initialize(applicationContext)

        // Initialize WorkManager background insights sync schedule
        com.pradeep.jarviscollector.service.InsightSyncWorkerHelper.initialize(applicationContext)

        // Initialize WorkManager background todo notification reminders schedule
        com.pradeep.jarviscollector.service.TodoNotificationHelper.initialize(applicationContext)

        androidx.core.app.ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_SMS),
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
            var todos by remember { mutableStateOf(emptyList<TodoEntity>()) }
            var financialEvents by remember { mutableStateOf(emptyList<FinancialEventEntity>()) }

            val allInsightsFlow = remember { FinancialInsightRepository.observeInsights(applicationContext) }
            val allInsights by allInsightsFlow.collectAsState(initial = emptyList())
            
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
            var ownerName by remember { mutableStateOf(AppPreferences.getOwnerName(applicationContext)) }
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
                    todos = TodoRepository.getTodos(applicationContext)
                    financialEvents = FinancialRepository.getFinancialEvents(applicationContext)
                    fyiEvents = FYIRepository.getFyiEvents(applicationContext)
                    latestBrief = JarvisDatabase.getDatabase(applicationContext).dailyBriefDao().getLatest()
                    facts = FactRepository.getFacts(applicationContext)
                    notifications = NotificationCenterRepository.getNotifications(applicationContext)
                    preferences = PreferenceRepository.getPreferences(applicationContext)
                    userActions = ActionsRepository.getActions(applicationContext)
                }
            }

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
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    JarvisNavHost(
                        navController = navController,
                        ownerName = ownerName,
                        todos = todos,
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
                        onSnoozeTodo = { todoId ->
                            lifecycleScope.launch {
                                TodoRepository.snoozeTodo(applicationContext, todoId)
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
            }
        }
    }
}