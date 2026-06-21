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

import com.pradeep.jarviscollector.repository.MobileSignalRepository
import com.pradeep.jarviscollector.repository.NotificationRepository
import com.pradeep.jarviscollector.repository.InsightsRepository

import com.pradeep.jarviscollector.ui.NotificationScreen
import com.pradeep.jarviscollector.ui.HomeScreen
import com.pradeep.jarviscollector.ui.TodoScreen
import com.pradeep.jarviscollector.ui.FinancialScreen
import com.pradeep.jarviscollector.ui.FyiScreen
import com.pradeep.jarviscollector.ui.DailyBriefScreen
import com.pradeep.jarviscollector.ui.FamilyScreen
import com.pradeep.jarviscollector.ui.SchoolScreen
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
    object CollectorSettings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        // Initialize WorkManager background sync schedule
        JarvisSyncWorkerHelper
            .initialize(
                applicationContext
            )

        // Initialize WorkManager background insights sync schedule
        com.pradeep.jarviscollector.service.InsightSyncWorkerHelper
            .initialize(
                applicationContext
            )

        // Initialize WorkManager background todo notification reminders schedule
        com.pradeep.jarviscollector.service.TodoNotificationHelper
            .initialize(
                applicationContext
            )

        androidx.core.app.ActivityCompat
            .requestPermissions(

                this,

                arrayOf(
                    android.Manifest.permission.READ_SMS
                ),

                101
            )

        lifecycleScope.launch {

            val smsSignals =
                SmsRepository
                    .readRecentSms(
                        applicationContext
                    )

            android.app.AlertDialog
                .Builder(
                    this@MainActivity
                )
                .setTitle(
                    "SMS Test"
                )
                .setMessage(
                    "Found ${smsSignals.size} SMS"
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .show()
        }

        setContent {

            var currentScreen by remember {
                mutableStateOf<Screen>(Screen.Home)
            }

            var todos by remember {
                mutableStateOf(emptyList<TodoEntity>())
            }

            var financialEvents by remember {
                mutableStateOf(emptyList<FinancialEventEntity>())
            }

            var fyiEvents by remember {
                mutableStateOf(emptyList<FyiEventEntity>())
            }

            var latestBrief by remember {
                mutableStateOf<DailyBriefEntity?>(null)
            }

            var roomSignals by remember {
                mutableStateOf(emptyList<MobileSignal>())
            }

            var exportPath by remember {
                mutableStateOf("")
            }

            var isSyncing by remember {
                mutableStateOf(false)
            }

            var syncResultMessage by remember {
                mutableStateOf<String?>(null)
            }

            var ownerName by remember {
                mutableStateOf(
                    AppPreferences.getOwnerName(applicationContext)
                )
            }

            var isSyncingInsights by remember {
                mutableStateOf(false)
            }

            var insightSyncResultMessage by remember {
                mutableStateOf<String?>(null)
            }

            fun refreshInsights() {
                lifecycleScope.launch {
                    todos = InsightsRepository.getTodos(applicationContext)
                    financialEvents = InsightsRepository.getFinancialEvents(applicationContext)
                    fyiEvents = InsightsRepository.getFyiEvents(applicationContext)
                    latestBrief = InsightsRepository.getLatestDailyBrief(applicationContext)
                }
            }

            LaunchedEffect(ownerName) {
                refreshInsights()
            }

            val familyEvents = fyiEvents.filter { it.category.lowercase() == "family" }
            val schoolEvents = fyiEvents.filter { it.category.lowercase() == "school" }

            JarvisTheme {
                when (currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            ownerName = ownerName,
                            pendingTodoCount = todos.count { it.status == "pending" },
                            unpaidBillCount = financialEvents.count { it.status == "upcoming" || it.type.lowercase() == "bill" },
                            newFyiCount = fyiEvents.size,
                            familyCount = familyEvents.size,
                            schoolCount = schoolEvents.size,
                            briefDate = latestBrief?.generatedAt,
                            onNavigateToTodos = { currentScreen = Screen.Todos },
                            onNavigateToFinancial = { currentScreen = Screen.Financial },
                            onNavigateToFyi = { currentScreen = Screen.Fyi },
                            onNavigateToDailyBrief = { currentScreen = Screen.DailyBrief },
                            onNavigateToFamily = { currentScreen = Screen.Family },
                            onNavigateToSchool = { currentScreen = Screen.School },
                            onNavigateToCollectorSettings = { currentScreen = Screen.CollectorSettings },
                            onOwnerNameChange = { newName ->
                                ownerName = newName
                                AppPreferences.setOwnerName(applicationContext, newName)
                                refreshInsights()
                            }
                        )
                    }
                    is Screen.Todos -> {
                        TodoScreen(
                            todos = todos,
                            onComplete = { todoId ->
                                lifecycleScope.launch {
                                    InsightsRepository.markTodoComplete(applicationContext, todoId)
                                    refreshInsights()
                                }
                            },
                            onSnooze = { todoId ->
                                lifecycleScope.launch {
                                    InsightsRepository.snoozeTodo(applicationContext, todoId)
                                    refreshInsights()
                                }
                            },
                            onDelete = { todoId ->
                                lifecycleScope.launch {
                                    InsightsRepository.deleteTodo(applicationContext, todoId)
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
                                                        "Daily Briefs: ${result.briefCount}\n" +
                                                        "Todos: ${result.todoCount}\n" +
                                                        "Financials: ${result.financialCount}\n" +
                                                        "FYIs: ${result.fyiCount}"
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
                            }
                        )
                    }
                }
            }
        }
    }
}