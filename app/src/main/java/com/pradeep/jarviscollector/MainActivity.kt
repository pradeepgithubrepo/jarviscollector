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
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.repository.MobileSignalRepository
import com.pradeep.jarviscollector.repository.NotificationRepository
import com.pradeep.jarviscollector.repository.TodoRepository
import com.pradeep.jarviscollector.repository.FinancialRepository
import com.pradeep.jarviscollector.repository.FYIRepository
import com.pradeep.jarviscollector.repository.PreferenceRepository
import com.pradeep.jarviscollector.repository.ActionsRepository
import com.pradeep.jarviscollector.ui.NotificationScreen
import com.pradeep.jarviscollector.ui.HomeScreen
import com.pradeep.jarviscollector.ui.TodoScreen
import com.pradeep.jarviscollector.ui.FinancialScreen
import com.pradeep.jarviscollector.ui.FyiScreen
import com.pradeep.jarviscollector.ui.DailyBriefScreen
import com.pradeep.jarviscollector.ui.FamilyScreen
import com.pradeep.jarviscollector.ui.SchoolScreen
import com.pradeep.jarviscollector.ui.TravelScreen
import com.pradeep.jarviscollector.ui.HealthScreen
import com.pradeep.jarviscollector.ui.ShoppingScreen
import com.pradeep.jarviscollector.ui.theme.JarvisTheme
import com.pradeep.jarviscollector.utils.JsonExporter
import com.pradeep.jarviscollector.utils.AppPreferences
import com.pradeep.jarviscollector.repository.SmsRepository
import com.pradeep.jarviscollector.service.JarvisSyncWorkerHelper
import com.pradeep.jarviscollector.service.SyncService
import com.pradeep.jarviscollector.service.SyncResult
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object Todos : Screen()
    object Financial : Screen()
    object Fyi : Screen()
    object DailyBrief : Screen()
    object Family : Screen()
    object School : Screen()
    object Travel : Screen()
    object Health : Screen()
    object Shopping : Screen()
    object CollectorSettings : Screen()
}

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
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            var todos by remember { mutableStateOf(emptyList<TodoEntity>()) }
            var financialEvents by remember { mutableStateOf(emptyList<FinancialEventEntity>()) }
            var fyiEvents by remember { mutableStateOf(emptyList<FyiEventEntity>()) }
            var latestBrief by remember { mutableStateOf<DailyBriefEntity?>(null) }
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
                }
            }

            LaunchedEffect(ownerName) {
                refreshInsights()
            }

            val familyEvents = fyiEvents.filter { it.category?.lowercase() == "family" }
            val schoolEvents = fyiEvents.filter { it.category?.lowercase() == "school" }
            val travelEvents = fyiEvents.filter { it.category?.lowercase() == "travel" }
            val healthEvents = fyiEvents.filter { it.category?.lowercase() == "health" }
            val shoppingEvents = fyiEvents.filter { it.category?.lowercase() == "shopping" || it.category?.lowercase() == "deliveries" }

            JarvisTheme {
                when (currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            ownerName = ownerName,
                            pendingTodoCount = todos.count { it.status == "OPEN" || it.status == "SNOOZED" },
                            unpaidBillCount = financialEvents.count { it.status?.lowercase() == "upcoming" || it.category?.lowercase() == "bill" },
                            newFyiCount = fyiEvents.size,
                            familyCount = familyEvents.size,
                            schoolCount = schoolEvents.size,
                            travelCount = travelEvents.size,
                            healthCount = healthEvents.size,
                            shoppingCount = shoppingEvents.size,
                            briefDate = latestBrief?.generatedAt,
                            onNavigateToTodos = { currentScreen = Screen.Todos },
                            onNavigateToFinancial = { currentScreen = Screen.Financial },
                            onNavigateToFyi = { currentScreen = Screen.Fyi },
                            onNavigateToDailyBrief = { currentScreen = Screen.DailyBrief },
                            onNavigateToFamily = { currentScreen = Screen.Family },
                            onNavigateToSchool = { currentScreen = Screen.School },
                            onNavigateToTravel = { currentScreen = Screen.Travel },
                            onNavigateToHealth = { currentScreen = Screen.Health },
                            onNavigateToShopping = { currentScreen = Screen.Shopping },
                            onNavigateToCollectorSettings = { currentScreen = Screen.CollectorSettings },
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
                            }
                        )
                    }
                    is Screen.Todos -> {
                        TodoScreen(
                            todos = todos,
                            onComplete = { todoId ->
                                lifecycleScope.launch {
                                    TodoRepository.markTodoComplete(applicationContext, todoId)
                                    refreshInsights()
                                }
                            },
                            onSnooze = { todoId ->
                                lifecycleScope.launch {
                                    TodoRepository.snoozeTodo(applicationContext, todoId)
                                    refreshInsights()
                                }
                            },
                            onDelete = { todoId ->
                                lifecycleScope.launch {
                                    TodoRepository.deleteTodo(applicationContext, todoId)
                                    refreshInsights()
                                }
                            },
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.Financial -> {
                        FinancialScreen(
                            events = financialEvents,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.Fyi -> {
                        FyiScreen(
                            events = fyiEvents,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.DailyBrief -> {
                        DailyBriefScreen(
                            brief = latestBrief,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.Family -> {
                        FamilyScreen(
                            events = familyEvents,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.School -> {
                        SchoolScreen(
                            events = schoolEvents,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.Travel -> {
                        TravelScreen(
                            events = travelEvents,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.Health -> {
                        HealthScreen(
                            events = healthEvents,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.Shopping -> {
                        ShoppingScreen(
                            events = shoppingEvents,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.CollectorSettings -> {
                        NotificationScreen(
                            notifications = NotificationRepository.notifications,
                            roomSignals = roomSignals,
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
                            exportPath = exportPath,
                            isSyncing = isSyncing,
                            syncResultMessage = syncResultMessage,
                            onDismissSyncResult = { syncResultMessage = null },
                            ownerName = ownerName,
                            onOwnerNameChange = { newName ->
                                ownerName = newName
                                AppPreferences.setOwnerName(applicationContext, newName)
                                refreshInsights()
                            },
                            isSyncingInsights = isSyncingInsights,
                            insightSyncResultMessage = insightSyncResultMessage,
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
                            isBackfilling = isBackfilling,
                            backfillStep = backfillStep,
                            backfillResultMessage = backfillResultMessage,
                            backfillCompleted = backfillCompleted,
                            onDismissBackfillResult = { backfillResultMessage = null },
                            onRunAgain = {
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
                             }
                        )
                    }
                }
            }
        }
    }
}