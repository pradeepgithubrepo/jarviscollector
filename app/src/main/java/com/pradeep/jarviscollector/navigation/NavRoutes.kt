package com.pradeep.jarviscollector.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Brief : Screen("brief", "Brief", Icons.Default.Star)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.List)
    object Fyi : Screen("fyi", "FYI", Icons.Default.Info)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Facts : Screen("facts", "Facts", Icons.Default.Star)
    object Finance : Screen("finance", "Financial", Icons.Default.ShoppingCart)
    object FyiCategory : Screen("fyi_category/{category}") {
        fun createRoute(category: String) = "fyi_category/$category"
    }
    object NotificationDetail : Screen("notification/{id}") {
        fun createRoute(id: String) = "notification/$id"
    }
    object NotificationCenter : Screen("notification_center")
    object ActionCenter : Screen("action_center")
    object SignalExplorer : Screen("signal_explorer/{entityType}/{entityId}") {
        fun createRoute(entityType: String, entityId: String) = "signal_explorer/$entityType/$entityId"
    }
    object DebugPipeline : Screen("debug_pipeline")
    object TaskDetail : Screen("task_detail/{id}", "Task Detail") {
        fun createRoute(id: String) = "task_detail/$id"
    }
    object FactDetail : Screen("fact_detail/{id}", "Fact Detail") {
        fun createRoute(id: String) = "fact_detail/$id"
    }
    object TransactionDetail : Screen("transaction_detail/{id}", "Transaction Detail") {
        fun createRoute(id: String) = "transaction_detail/$id"
    }
}
