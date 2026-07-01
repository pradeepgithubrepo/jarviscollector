package com.pradeep.jarviscollector.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.NotificationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    notifications: List<NotificationEntity>,
    onMarkRead: (String, Boolean) -> Unit,
    onArchive: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterType by remember { mutableStateOf("ALL") } // ALL, UNREAD, ARCHIVED

    val filteredList = notifications.filter {
        val statusVal = it.status?.uppercase() ?: "NEW"
        val isArchived = statusVal == "ARCHIVED"
        when (filterType) {
            "UNREAD" -> !isArchived && it.read_flag != true
            "ARCHIVED" -> isArchived
            else -> !isArchived // ALL (excludes archived by default to keep center clean)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Notification Center",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == "ALL",
                onClick = { filterType = "ALL" },
                label = { Text("Active") }
            )
            FilterChip(
                selected = filterType == "UNREAD",
                onClick = { filterType = "UNREAD" },
                label = { Text("Unread") }
            )
            FilterChip(
                selected = filterType == "ARCHIVED",
                onClick = { filterType = "ARCHIVED" },
                label = { Text("Archived") }
            )
        }

        if (filteredList.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Text(
                    text = when (filterType) {
                        "UNREAD" -> "No unread alerts."
                        "ARCHIVED" -> "No archived alerts."
                        else -> "Notification Center is clear!"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { alert -> alert.id }) { alert ->
                    NotificationCard(
                        notification = alert,
                        onMarkRead = { onMarkRead(alert.id, true) },
                        onArchive = { onArchive(alert.id) },
                        onTap = {
                            if (!alert.action_route.isNullOrBlank()) {
                                onMarkRead(alert.id, true)
                                onNavigateToRoute(alert.action_route)
                            } else {
                                onNavigateToSignalExplorer("notification", alert.id)
                            }
                        }
                    )
                }
            }
        }
    }
}
