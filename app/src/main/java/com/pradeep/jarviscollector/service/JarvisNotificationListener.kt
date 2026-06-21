package com.pradeep.jarviscollector.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

import com.pradeep.jarviscollector.model.NotificationEvent
import com.pradeep.jarviscollector.model.MobileSignal

import com.pradeep.jarviscollector.repository.NotificationRepository
import com.pradeep.jarviscollector.repository.MobileSignalRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.pradeep.jarviscollector.utils.NotificationNoiseFilter

class JarvisNotificationListener :
    NotificationListenerService() {


    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {

        try {

            val packageName =
                sbn.packageName


            if (
                packageName != "com.whatsapp" &&
                packageName != "com.whatsapp.w4b" &&
                packageName != "com.google.android.apps.messaging"
            ) {

                return
            }


            val extras =
                sbn.notification.extras


            val title =
                extras.getString(
                    "android.title",
                    ""
                )


            val message =
                extras
                    .getCharSequence(
                        "android.text"
                    )
                    ?.toString()
                    ?: ""


            if (
                message.isBlank()
                ||
                message.contains(
                    "new messages",
                    ignoreCase = true
                )
                ||
                message.contains(
                    "messages from",
                    ignoreCase = true
                )
            ) {

                return
            }


            val source =
                when (packageName) {

                    "com.whatsapp" ->
                        "whatsapp"

                    "com.whatsapp.w4b" ->
                        "whatsapp_business"

                    else ->
                        "sms"
                }

            if (

                NotificationNoiseFilter
                    .shouldIgnore(

                        title,

                        message

                    )

            ) {

                return
            }
            // ----------------------------------
            // Existing UI Flow
            // ----------------------------------

            NotificationRepository
                .addNotification(

                    NotificationEvent(

                        source = source,

                        title = title,

                        message = message,

                        timestamp =
                            System.currentTimeMillis()
                    )
                )


            // ----------------------------------
            // Persist To Room
            // ----------------------------------

            CoroutineScope(
                Dispatchers.IO
            ).launch {

                MobileSignalRepository
                    .save(

                        context =
                            applicationContext,

                        signal = MobileSignal(

                            deviceId =
                                "pradeep_phone",

                            source =
                                source,

                            sender =
                                title,

                            message =
                                message,

                            timestamp =
                                System.currentTimeMillis()
                        )
                    )
            }

        } catch (
            ex: Exception
        ) {

            ex.printStackTrace()
        }
    }



}