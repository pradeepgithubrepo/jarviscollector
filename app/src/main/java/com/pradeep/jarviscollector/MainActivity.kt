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

import com.pradeep.jarviscollector.network.SupabaseUploader
import com.pradeep.jarviscollector.repository.SmsRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import androidx.core.app.ActivityCompat
class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
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

                exportPath =
                    exportPath
            )
        }
    }
}