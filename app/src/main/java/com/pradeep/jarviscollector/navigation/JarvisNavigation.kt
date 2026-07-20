package com.pradeep.jarviscollector.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pradeep.jarviscollector.model.DailyBriefEntity
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.FyiEventEntity
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.MobileSignal
import com.pradeep.jarviscollector.model.FactInsightEntity
import com.pradeep.jarviscollector.model.NotificationEntity
import com.pradeep.jarviscollector.model.FinancialInsightEntity
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import com.pradeep.jarviscollector.model.UserActionEntity
import com.pradeep.jarviscollector.model.ReminderEntity
import com.pradeep.jarviscollector.ui.*
import com.pradeep.jarviscollector.ui.splash.SplashScreen
import com.pradeep.jarviscollector.ui.name_selection.NameSelectionScreen
import com.pradeep.jarviscollector.utils.AppPreferences
import com.pradeep.jarviscollector.ui.facts.FactsScreen
import com.pradeep.jarviscollector.ui.facts.FactDetailScreen
import com.pradeep.jarviscollector.ui.notification.NotificationCenterScreen
import com.pradeep.jarviscollector.ui.actioncenter.ActionCenterScreen
import com.pradeep.jarviscollector.ui.todo.TaskDetailScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text


@Composable
fun JarvisNavHost(
    navController: NavHostController,
    ownerName: String,
    todos: List<TodoEntity>,
    reminders: List<ReminderEntity>,
    snapshotInsights: List<FinancialInsightEntity>,
    actionRequiredInsights: List<FinancialInsightEntity>,
    subscriptionInsights: List<FinancialInsightEntity>,
    upcomingBillInsights: List<FinancialInsightEntity>,
    unusualActivityInsights: List<FinancialInsightEntity>,
    financialEvents: List<FinancialEventEntity>,
    fyiEvents: List<FyiEventEntity>,
    latestBrief: DailyBriefEntity?,
    roomSignals: List<MobileSignal>,
    facts: List<FactInsightEntity>,
    notifications: List<NotificationEntity>,
    preferences: List<UserPreferenceEntity>,
    userActions: List<UserActionEntity>,
    exportPath: String,
    isSyncing: Boolean,
    syncResultMessage: String?,
    isSyncingInsights: Boolean,
    insightSyncResultMessage: String?,
    isBackfilling: Boolean,
    backfillStep: String?,
    backfillResultMessage: String?,
    backfillCompleted: Boolean,
    onOwnerNameChange: (String) -> Unit,
    onLoadInsights: () -> Unit,
    onCompleteTodo: (String) -> Unit,
    onAddTodoClick: () -> Unit,
    onVoiceTodoClick: () -> Unit,
    onConvertFactToTodo: (String, String, String, Long?) -> Unit,
    onSwipeFactSoftDelete: (String) -> Unit,
    onSwipeFactHardDelete: (String, (Boolean) -> Unit) -> Unit,
    onToggleFactRead: (String, Boolean) -> Unit,
    onMarkFyiRead: (String, Boolean) -> Unit,
    onDismissFyi: (String) -> Unit,
    onMarkNotificationRead: (String, Boolean) -> Unit,
    onArchiveNotification: (String) -> Unit,
    onTogglePreference: (String, Boolean) -> Unit,
    onConfirmTransaction: (String) -> Unit,
    onCorrectTransaction: (String, String, Double) -> Unit,
    onConfirmInsight: (String) -> Unit,
    onDismissInsight: (String) -> Unit,
    onCorrectInsight: (String, String, Double) -> Unit,
    onSnoozeTodo: (String, Int) -> Unit,
    onSetReminder: (String, Long, Int, String) -> Unit,
    onRemoveReminder: (String) -> Unit,
    onDeleteTodo: (String) -> Unit,
    onLoadRoom: () -> Unit,
    onExportJson: () -> Unit,
    onSyncNow: () -> Unit,
    onDismissSyncResult: () -> Unit,
    onDismissInsightSyncResult: () -> Unit,
    onSyncInsights: () -> Unit,
    onDismissBackfillResult: () -> Unit,
    onRunBackfillAgain: () -> Unit,
    onStartBackfill: () -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val familyEvents = fyiEvents.filter { it.category?.lowercase() == "family" }
    val schoolEvents = fyiEvents.filter { it.category?.lowercase() == "school" }
    val travelEvents = fyiEvents.filter { it.category?.lowercase() == "travel" }
    val healthEvents = fyiEvents.filter { it.category?.lowercase() == "health" }
    val shoppingEvents = fyiEvents.filter { it.category?.lowercase() == "shopping" || it.category?.lowercase() == "deliveries" }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // Splash screen composable
        composable(Screen.Splash.route) {
            SplashScreen(
                ownerName = ownerName,
                onNavigateToHome = {
                    if (ownerName.isBlank()) {
                        navController.navigate(Screen.NameSelection.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        // Name selection screen composable
        composable(Screen.NameSelection.route) {
            NameSelectionScreen(navController = navController, onOwnerNameChange = onOwnerNameChange)
        }

        composable(Screen.Home.route) {
            HomeScreen(
                ownerName = ownerName,
                todos = todos,
                financialEvents = financialEvents,
                fyiEvents = fyiEvents,
                latestBrief = latestBrief,
                recentFacts = facts.filter { it.read_flag != true },
                notifications = notifications,
                recentActions = userActions,
                financialInsights = snapshotInsights + actionRequiredInsights + subscriptionInsights + upcomingBillInsights + unusualActivityInsights,
                onNavigateToTodos = {
                    try {
                        navController.navigate(Screen.Tasks.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: tasks", e)
                    }
                },
                onNavigateToFinancial = {
                    try {
                        navController.navigate(Screen.Finance.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: finance", e)
                    }
                },
                onNavigateToFyi = {
                    try {
                        navController.navigate(Screen.Fyi.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: fyi", e)
                    }
                },
                onNavigateToDailyBrief = {
                    try {
                        navController.navigate(Screen.Brief.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: brief", e)
                    }
                },
                onNavigateToFamily = {
                    try {
                        navController.navigate(Screen.FyiCategory.createRoute("family"))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: family", e)
                    }
                },
                onNavigateToSchool = {
                    try {
                        navController.navigate(Screen.FyiCategory.createRoute("school"))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: school", e)
                    }
                },
                onNavigateToTravel = {
                    try {
                        navController.navigate(Screen.FyiCategory.createRoute("travel"))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: travel", e)
                    }
                },
                onNavigateToHealth = {
                    try {
                        navController.navigate(Screen.FyiCategory.createRoute("health"))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: health", e)
                    }
                },
                onNavigateToShopping = {
                    try {
                        navController.navigate(Screen.FyiCategory.createRoute("shopping"))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: shopping", e)
                    }
                },
                onNavigateToCollectorSettings = {
                    try {
                        navController.navigate(Screen.Profile.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: profile", e)
                    }
                },
                onNavigateToFacts = {
                    try {
                        navController.navigate(Screen.Facts.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: facts", e)
                    }
                },
                onNavigateToNotificationCenter = {
                    try {
                        navController.navigate(Screen.NotificationCenter.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: notification_center", e)
                    }
                },
                onNavigateToActionCenter = {
                    try {
                        navController.navigate(Screen.ActionCenter.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: action_center", e)
                    }
                },
                onOwnerNameChange = onOwnerNameChange,
                onLoadInsights = onLoadInsights,
                onCompleteTodo = onCompleteTodo,
                onAddTodoClick = onAddTodoClick,
                onVoiceTodoClick = onVoiceTodoClick,
                onNavigateToTaskDetail = { id ->
                    try {
                        navController.navigate(Screen.TaskDetail.createRoute(id))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: task_detail", e)
                    }
                },
                onNavigateToFactDetail = { id ->
                    try {
                        navController.navigate(Screen.FactDetail.createRoute(id))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: fact_detail", e)
                    }
                },
                onNavigateToLifecycleEvents = {
                    try {
                        navController.navigate(Screen.LifecycleEvents.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: lifecycle_events", e)
                    }
                },
                onNavigateToVault = {
                    try {
                        navController.navigate(Screen.VaultCategories.route)
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: vault", e)
                    }
                }
            )
        }

        composable(Screen.Brief.route) {
            DailyBriefScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VaultCategories.route) {
            com.pradeep.jarviscollector.ui.vault.VaultCategoriesScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { catId, catName ->
                    try {
                        navController.navigate(Screen.VaultEntries.createRoute(catId, catName))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: vault_entries", e)
                    }
                }
            )
        }

        composable(Screen.VaultEntries.route) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: "Vault Entries"
            com.pradeep.jarviscollector.ui.vault.VaultEntriesScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Tasks.route) {
            TodoScreen(
                todos = todos,
                reminders = reminders,
                onComplete = onCompleteTodo,
                onSnooze = onSnoozeTodo,
                onSetReminder = onSetReminder,
                onRemoveReminder = onRemoveReminder,
                onDelete = onDeleteTodo,
                onAddTodoClick = onAddTodoClick,
                onVoiceTodoClick = onVoiceTodoClick,
                onNavigateToTaskDetail = { id ->
                    navController.navigate(Screen.TaskDetail.createRoute(id))
                },
                onNavigateToSignalExplorer = onNavigateToSignalExplorer,
                onBack = { navController.popBackStack() }
            )
        }


        composable(Screen.Fyi.route) {
            FyiScreen(
                events = fyiEvents,
                onMarkRead = onMarkFyiRead,
                onDismiss = onDismissFyi,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            NotificationScreen(
                notifications = com.pradeep.jarviscollector.repository.NotificationRepository.notifications,
                roomSignals = roomSignals,
                preferences = preferences,
                onTogglePreference = onTogglePreference,
                onLoadRoom = onLoadRoom,
                onExportJson = onExportJson,
                onSyncNow = onSyncNow,
                exportPath = exportPath,
                isSyncing = isSyncing,
                syncResultMessage = syncResultMessage,
                onDismissSyncResult = onDismissSyncResult,
                ownerName = ownerName,
                onOwnerNameChange = onOwnerNameChange,
                isSyncingInsights = isSyncingInsights,
                insightSyncResultMessage = insightSyncResultMessage,
                onDismissInsightSyncResult = onDismissInsightSyncResult,
                onSyncInsights = onSyncInsights,
                isBackfilling = isBackfilling,
                backfillStep = backfillStep,
                backfillResultMessage = backfillResultMessage,
                backfillCompleted = backfillCompleted,
                onDismissBackfillResult = onDismissBackfillResult,
                onRunAgain = onRunBackfillAgain,
                onStartBackfill = onStartBackfill,
                onNavigateToDebugPipeline = { navController.navigate(Screen.DebugPipeline.route) }
            )
        }

        composable(Screen.Finance.route) {
            FinancialScreen(
                snapshotInsights = snapshotInsights,
                actionRequired = actionRequiredInsights,
                subscriptions = subscriptionInsights,
                upcomingBills = upcomingBillInsights,
                unusualActivity = unusualActivityInsights,
                onConfirmInsight = onConfirmInsight,
                onDismissInsight = onDismissInsight,
                onCorrectInsight = onCorrectInsight,
                onNavigateToSignalExplorer = onNavigateToSignalExplorer,
                onBack = { navController.popBackStack() },
                onNavigateToTransactionDetail = { id ->
                    try {
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Screen unavailable: transaction_detail", e)
                    }
                },
                onNavigateToMonthlyLedger = { month, cat ->
                    navController.navigate(Screen.MonthlyLedger.createRoute(month, cat))
                }
            )
        }

        composable(Screen.MonthlyLedger.route) { backStackEntry ->
            val monthKey = backStackEntry.arguments?.getString("monthKey") ?: ""
            val category = backStackEntry.arguments?.getString("category") ?: "all"
            com.pradeep.jarviscollector.ui.financial.MonthlyLedgerScreen(
                monthKey = monthKey,
                category = category,
                onBack = { navController.popBackStack() },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                }
            )
        }

        composable(Screen.LifecycleEvents.route) {
            com.pradeep.jarviscollector.ui.lifecycle.LifecycleEventsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Facts.route) {
            FactsScreen(
                facts = facts,
                onNavigateToFactDetail = { id ->
                    navController.navigate(Screen.FactDetail.createRoute(id))
                },
                onToggleRead = onToggleFactRead,
                onNavigateToSignalExplorer = onNavigateToSignalExplorer,
                onSwipeSoftDelete = onSwipeFactSoftDelete,
                onSwipeHardDelete = onSwipeFactHardDelete,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationCenter.route) {
            NotificationCenterScreen(
                notifications = notifications,
                onMarkRead = onMarkNotificationRead,
                onArchive = onArchiveNotification,
                onNavigateToRoute = { route ->
                    val screenRoute = when (route.lowercase()) {
                        "facts" -> Screen.Facts.route
                        "tasks" -> Screen.Tasks.route
                        "brief" -> Screen.Brief.route
                        "fyi" -> Screen.Fyi.route
                        "finance" -> Screen.Finance.route
                        "profile" -> Screen.Profile.route
                        else -> null
                    }
                    if (screenRoute != null) {
                        navController.navigate(screenRoute)
                    }
                },
                onNavigateToSignalExplorer = onNavigateToSignalExplorer,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ActionCenter.route) {
            ActionCenterScreen(
                actions = userActions,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FyiCategory.route) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")?.lowercase() ?: ""
            FyiCategoryScreen(
                category = category,
                events = fyiEvents,
                onMarkRead = onMarkFyiRead,
                onDismiss = onDismissFyi,
                onNavigateToSignalExplorer = onNavigateToSignalExplorer,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationDetail.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            // Notification Detail Screen placeholder for Phase 1
            androidx.compose.material3.Text("Notification Detail Screen for ID: $id")
        }

        composable(Screen.SignalExplorer.route) { backStackEntry ->
            val entityType = backStackEntry.arguments?.getString("entityType") ?: ""
            val entityId = backStackEntry.arguments?.getString("entityId") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            
            val traceState = remember(entityType, entityId) {
                mutableStateOf<com.pradeep.jarviscollector.repository.SignalTrace?>(null)
            }
            
            LaunchedEffect(entityType, entityId) {
                traceState.value = com.pradeep.jarviscollector.repository.SignalExplorerRepository.getTraceForEntity(
                    context, entityType, entityId
                )
            }
            
            traceState.value?.let { trace ->
                com.pradeep.jarviscollector.ui.signalexplorer.SignalExplorerScreen(
                    trace = trace,
                    onBack = { navController.popBackStack() }
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        composable(Screen.DebugPipeline.route) {
            com.pradeep.jarviscollector.ui.debug.DebugDataPipelineScreen(
                todos = todos,
                facts = facts,
                notifications = notifications,
                financialInsights = snapshotInsights + actionRequiredInsights + subscriptionInsights + upcomingBillInsights + unusualActivityInsights,
                fyiEvents = fyiEvents,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            TaskDetailScreen(
                todoId = id,
                todos = todos,
                reminders = reminders,
                onComplete = onCompleteTodo,
                onSnooze = onSnoozeTodo,
                onSetReminder = onSetReminder,
                onRemoveReminder = onRemoveReminder,
                onDelete = onDeleteTodo,
                onNavigateToSignalExplorer = onNavigateToSignalExplorer,
                onBack = { navController.popBackStack() }
            )
        }


        composable(
            route = Screen.FactDetail.route,
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            FactDetailScreen(
                factId = id,
                facts = facts,
                onConvertTodo = onConvertFactToTodo,
                onDismissFact = { factId ->
                    onDismissInsight(factId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            com.pradeep.jarviscollector.ui.financial.TransactionDetailScreen(
                transactionId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
