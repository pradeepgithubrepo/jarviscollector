package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.pradeep.jarviscollector.model.NotificationEvent
import com.pradeep.jarviscollector.model.MobileSignal

@Composable
fun NotificationScreen(

    notifications: List<NotificationEvent>,

    roomSignals: List<MobileSignal>,

    onLoadRoom: () -> Unit,

    onExportJson: () -> Unit,

    exportPath: String

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
                                signal.source
                        )

                        Text(
                            text =
                                signal.sender
                        )

                        Text(
                            text =
                                signal.message
                        )
                    }
                }
            }
        }
    }
}