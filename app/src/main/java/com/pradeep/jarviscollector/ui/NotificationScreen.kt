package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

import com.pradeep.jarviscollector.model.NotificationEvent
import com.pradeep.jarviscollector.model.MobileSignal

@Composable
fun NotificationScreen(

    notifications: List<NotificationEvent>,

    roomSignals: List<MobileSignal>,

    onLoadRoom: () -> Unit,

    onExportJson: () -> Unit,

    onSyncNow: () -> Unit,

    exportPath: String,

    isSyncing: Boolean,

    syncResultMessage: String?,

    onDismissSyncResult: () -> Unit,

    ownerName: String,

    onOwnerNameChange: (String) -> Unit

) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)

    ) {

        Text(
            text = "Jarvis Collector",
            style =
                MaterialTheme.typography.headlineMedium
        )

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

    if (isSyncing) {
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
                        text = "Syncing in progress...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Reading SMS & uploading to Supabase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
}