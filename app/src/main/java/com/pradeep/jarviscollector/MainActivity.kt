package com.pradeep.jarviscollector

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.runtime.*

import androidx.lifecycle.lifecycleScope

import com.pradeep.jarviscollector.model.MobileSignal

import com.pradeep.jarviscollector.repository.MobileSignalRepository
import com.pradeep.jarviscollector.repository.NotificationRepository

import com.pradeep.jarviscollector.ui.NotificationScreen

import com.pradeep.jarviscollector.utils.JsonExporter
import com.pradeep.jarviscollector.utils.AppPreferences

import com.pradeep.jarviscollector.repository.SmsRepository
import com.pradeep.jarviscollector.service.JarvisSyncWorkerHelper
import com.pradeep.jarviscollector.service.SyncService
import com.pradeep.jarviscollector.service.SyncResult

import kotlinx.coroutines.launch

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

            var roomSignals by remember {

                mutableStateOf(
                    emptyList<MobileSignal>()
                )
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

            NotificationScreen(

                notifications =
                    NotificationRepository.notifications,

                roomSignals =
                    roomSignals,

                onLoadRoom = {

                    lifecycleScope.launch {

                        roomSignals =
                            MobileSignalRepository
                                .getSignals(
                                    applicationContext
                                )
                    }
                },

                onExportJson = {

                    lifecycleScope.launch {

                        roomSignals =
                            MobileSignalRepository
                                .getSignals(
                                    applicationContext
                                )

                        exportPath =
                            JsonExporter
                                .exportSignals(
                                    applicationContext,
                                    roomSignals
                                )
                    }
                },

                onSyncNow = {

                    lifecycleScope.launch {

                        isSyncing = true
                        syncResultMessage = null

                        try {
                            // 1. Ingest local SMS messages
                            val newSmsCount =
                                SmsRepository
                                    .importRecentSmsToRoom(
                                        applicationContext
                                    )

                            // 2. Perform database to Supabase sync
                            when (
                                val result =
                                    SyncService
                                        .syncPendingSignals(
                                            applicationContext
                                        )
                            ) {

                                is SyncResult.Success -> {

                                    syncResultMessage =
                                        "Imported $newSmsCount new SMS.\n\nSuccessfully uploaded ${result.count} signals to Supabase!"
                                }

                                is SyncResult.NoData -> {

                                    syncResultMessage =
                                        "Imported $newSmsCount new SMS.\n\nNo pending signals to upload."
                                }

                                is SyncResult.Failure -> {

                                    syncResultMessage =
                                        "Imported $newSmsCount new SMS.\n\nSync failed: ${result.error}"
                                }
                            }

                        } catch (
                            ex: Exception
                        ) {

                            syncResultMessage =
                                "Error during sync: ${ex.message}"

                        } finally {

                            isSyncing = false

                            // Refresh room signals list in UI to update status
                            roomSignals =
                                MobileSignalRepository
                                    .getSignals(
                                        applicationContext
                                    )
                        }
                    }
                },

                exportPath =
                    exportPath,

                isSyncing =
                    isSyncing,

                syncResultMessage =
                    syncResultMessage,

                onDismissSyncResult = {

                    syncResultMessage = null
                },

                ownerName =
                    ownerName,

                onOwnerNameChange = { newName ->
                    ownerName = newName
                    AppPreferences.setOwnerName(applicationContext, newName)
                }
            )
        }
    }
}