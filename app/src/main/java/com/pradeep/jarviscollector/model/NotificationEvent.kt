package com.pradeep.jarviscollector.model

data class NotificationEvent(

    val source: String,

    val title: String,

    val message: String,

    val timestamp: Long
)