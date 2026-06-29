package com.pradeep.jarviscollector.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Brief : Screen("brief", "Brief", Icons.Default.Star)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.List)
    object Fyi : Screen("fyi", "FYI", Icons.Default.Info)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Facts : Screen("facts")
    object Finance : Screen("finance")
    object FyiCategory : Screen("fyi_category/{category}") {
        fun createRoute(category: String) = "fyi_category/$category"
    }
    object NotificationDetail : Screen("notification/{id}") {
        fun createRoute(id: String) = "notification/$id"
    }
}
