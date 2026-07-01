package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight

import com.pradeep.jarviscollector.model.NotificationEvent
import com.pradeep.jarviscollector.model.MobileSignal
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import com.pradeep.jarviscollector.database.JarvisDatabase
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color



@Composable
fun NotificationScreen(
    notifications: List<NotificationEvent>,
    roomSignals: List<MobileSignal>,
    preferences: List<UserPreferenceEntity>,
    onTogglePreference: (String, Boolean) -> Unit,
    onLoadRoom: () -> Unit,
    onExportJson: () -> Unit,
    onSyncNow: () -> Unit,
    exportPath: String,
    isSyncing: Boolean,
    syncResultMessage: String?,
    onDismissSyncResult: () -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    isSyncingInsights: Boolean,
    insightSyncResultMessage: String?,
    onDismissInsightSyncResult: () -> Unit,
    onSyncInsights: () -> Unit,
    isBackfilling: Boolean,
    backfillStep: String?,
    backfillResultMessage: String?,
    backfillCompleted: Boolean,
    onStartBackfill: () -> Unit,
    onRunAgain: () -> Unit,
    onDismissBackfillResult: () -> Unit,
    onNavigateToDebugPipeline: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var factsCount by remember { mutableStateOf(-1) }
    var todosCount by remember { mutableStateOf(-1) }
    var fyiCount by remember { mutableStateOf(-1) }
    var notifsCount by remember { mutableStateOf(-1) }
    var finCount by remember { mutableStateOf(-1) }
    var familyCount by remember { mutableStateOf(-1) }

    LaunchedEffect(ownerName, isSyncing) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = JarvisDatabase.getDatabase(context)
                factsCount = db.factInsightDao().getAll().size
                todosCount = db.todoDao().getAll().size
                val fyiList = db.fyiEventDao().getAll()
                fyiCount = fyiList.size
                familyCount = fyiList.count { it.category?.lowercase() == "family" }
                notifsCount = db.notificationDao().getAll().size
                finCount = db.financialInsightDao().getAll().size
            } catch (e: Exception) {
                android.util.Log.e("NotificationScreen", "Direct DAO counts fail", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jarvis Collector",
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = { onNavigateToDebugPipeline?.invoke() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("OPEN DEBUG PIPELINE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("ROOM DAO COUNTS (Direct)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Facts: $factsCount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Todos: $todosCount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("FYI: $fyiCount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Notifications: $notifsCount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Financial: $finCount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Family: $familyCount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active User: ",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Button(
                onClick = { onOwnerNameChange("pradeep") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ownerName == "pradeep") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (ownerName == "pradeep") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Pradeep")
            }

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Button(
                onClick = { onOwnerNameChange("shobana") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ownerName == "shobana") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (ownerName == "shobana") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Shobana")
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                "Live Notifications: ${notifications.size}"
        )

        Text(
            text =
                "Room Signals: ${roomSignals.size}"
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text = "Notification Preferences",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val prefKeys = listOf(
                    "todo_notifications_enabled" to "Todo Notifications",
                    "fact_notifications_enabled" to "Fact Notifications",
                    "fyi_notifications_enabled" to "FYI Notifications",
                    "brief_notifications_enabled" to "Brief Notifications",
                    "financial_notifications_enabled" to "Financial Notifications"
                )

                prefKeys.forEachIndexed { index, (key, label) ->
                    val prefEntity = preferences.find { it.preference_key == key }
                    val isChecked = prefEntity?.preference_value == "true"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { enabled -> onTogglePreference(key, enabled) }
                        )
                    }
                    if (index < prefKeys.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Row {

            Button(
                onClick = onLoadRoom
            ) {

                Text(
                    "Load From Room"
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Button(
                onClick = onExportJson
            ) {

                Text(
                    "Export JSON"
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Button(
                onClick = onSyncNow
            ) {

                Text(
                    "Sync Now"
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Button(
                onClick = onSyncInsights
            ) {

                Text(
                    "Sync Insights"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text = "Historical Backfill",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        if (backfillCompleted) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {},
                    enabled = false
                ) {
                    Text("Historical Backfill (Completed)")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onRunAgain
                ) {
                    Text("Run Again")
                }
            }
        } else {
            Button(
                onClick = onStartBackfill
            ) {
                Text("Historical Backfill")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onNavigateToDebugPipeline?.invoke() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Debug Data Pipeline", color = MaterialTheme.colorScheme.onTertiary)
        }

        if (
            exportPath.isNotBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "Exported To:"
            )

            Text(
                text =
                    exportPath
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        LazyColumn {

            items(roomSignals) {

                    signal ->

                Card(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 8.dp
                            )

                ) {

                    Column(

                        modifier =
                            Modifier
                                .padding(12.dp)

                    ) {

                        Text(
                            text =
                                "Source: ${signal.source}"
                        )

                        Text(
                            text =
                                "Sender: ${signal.sender}"
                        )

                        Text(
                            text =
                                "Message: ${signal.message}"
                        )

                        Text(
                            text =
                                "Device: ${signal.deviceId}"
                        )

                        Text(
                            text =
                                "Status: ${signal.syncStatus}"
                        )
                    }
                }
            }
        }
    }

    if (syncResultMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissSyncResult,
            title = {
                Text(text = "Sync Finished")
            },
            text = {
                Text(text = syncResultMessage)
            },
            confirmButton = {
                Button(
                    onClick = onDismissSyncResult
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (isSyncingInsights) {
        Dialog(
            onDismissRequest = {}
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Syncing insights...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Downloading from Supabase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (insightSyncResultMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissInsightSyncResult,
            title = {
                Text(text = "Insights Sync Finished")
            },
            text = {
                Text(text = insightSyncResultMessage)
            },
            confirmButton = {
                Button(
                    onClick = onDismissInsightSyncResult
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (isBackfilling) {
        Dialog(
            onDismissRequest = {}
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Historical Backfill",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = backfillStep ?: "Starting...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (backfillResultMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissBackfillResult,
            title = {
                Text(text = "Historical Backfill Completed")
            },
            text = {
                Text(text = backfillResultMessage)
            },
            confirmButton = {
                Button(
                    onClick = onDismissBackfillResult
                ) {
                    Text("OK")
                }
            }
        )
    }
}