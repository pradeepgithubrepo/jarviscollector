package com.pradeep.jarviscollector.utils

object NotificationNoiseFilter {

    private val ignoredPatterns = listOf(

        "downloading document",

        ".apk",

        "app-debug.apk",

        "Backup in progress",

        "this message was deleted",

        "you deleted this message",

        "new messages",

        "messages from"
    )

    fun shouldIgnore(

        sender: String,

        message: String

    ): Boolean {

        val content =
            "$sender $message"
                .lowercase()

        return ignoredPatterns.any {

                pattern ->

            content.contains(
                pattern.lowercase()
            )
        }
    }
}