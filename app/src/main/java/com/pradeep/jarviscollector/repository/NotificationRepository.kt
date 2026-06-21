package com.pradeep.jarviscollector.repository

import androidx.compose.runtime.mutableStateListOf

import com.pradeep.jarviscollector.model.NotificationEvent

object NotificationRepository {

    val notifications =
        mutableStateListOf<NotificationEvent>()

    fun addNotification(
        event: NotificationEvent
    ) {

        notifications.add(
            0,
            event
        )
    }
}