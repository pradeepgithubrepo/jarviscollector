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
import com.pradeep.jarviscollector.ui.*

@Composable
fun JarvisNavHost(
    navController: NavHostController,
    ownerName: String,
    todos: List<TodoEntity>,
    financialEvents: List<FinancialEventEntity>,
    fyiEvents: List<FyiEventEntity>,
    latestBrief: DailyBriefEntity?,
    roomSignals: List<MobileSignal>,
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
    onSnoozeTodo: (String) -> Unit,
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
    modifier: Modifier = Modifier
) {
    val familyEvents = fyiEvents.filter { it.category?.lowercase() == "family" }
    val schoolEvents = fyiEvents.filter { it.category?.lowercase() == "school" }
    val travelEvents = fyiEvents.filter { it.category?.lowercase() == "travel" }
    val healthEvents = fyiEvents.filter { it.category?.lowercase() == "health" }
    val shoppingEvents = fyiEvents.filter { it.category?.lowercase() == "shopping" || it.category?.lowercase() == "deliveries" }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
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
                onNavigateToTodos = { navController.navigate(Screen.Tasks.route) },
                onNavigateToFinancial = { navController.navigate(Screen.Finance.route) },
                onNavigateToFyi = { navController.navigate(Screen.Fyi.route) },
                onNavigateToDailyBrief = { navController.navigate(Screen.Brief.route) },
                onNavigateToFamily = { navController.navigate(Screen.FyiCategory.createRoute("family")) },
                onNavigateToSchool = { navController.navigate(Screen.FyiCategory.createRoute("school")) },
                onNavigateToTravel = { navController.navigate(Screen.FyiCategory.createRoute("travel")) },
                onNavigateToHealth = { navController.navigate(Screen.FyiCategory.createRoute("health")) },
                onNavigateToShopping = { navController.navigate(Screen.FyiCategory.createRoute("shopping")) },
                onNavigateToCollectorSettings = { navController.navigate(Screen.Profile.route) },
                onOwnerNameChange = onOwnerNameChange,
                onLoadInsights = onLoadInsights
            )
        }

        composable(Screen.Brief.route) {
            DailyBriefScreen(
                brief = latestBrief,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Tasks.route) {
            TodoScreen(
                todos = todos,
                onComplete = onCompleteTodo,
                onSnooze = onSnoozeTodo,
                onDelete = onDeleteTodo,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Fyi.route) {
            FyiScreen(
                events = fyiEvents,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            NotificationScreen(
                notifications = com.pradeep.jarviscollector.repository.NotificationRepository.notifications,
                roomSignals = roomSignals,
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
                onStartBackfill = onStartBackfill
            )
        }

        composable(Screen.Finance.route) {
            FinancialScreen(
                events = financialEvents,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Facts.route) {
            // Facts Screen placeholder for Phase 1
            androidx.compose.material3.Text("Facts Screen - Coming in Phase 3")
        }

        composable(Screen.FyiCategory.route) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")?.lowercase() ?: ""
            FyiCategoryScreen(
                category = category,
                events = fyiEvents,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationDetail.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            // Notification Detail Screen placeholder for Phase 1
            androidx.compose.material3.Text("Notification Detail Screen for ID: $id")
        }
    }
}
