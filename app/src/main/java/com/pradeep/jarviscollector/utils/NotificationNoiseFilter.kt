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

    fun normalize(
        title: String,
        message: String
    ): Pair<String, String> {
        val trimmedTitle = title.trim()
        val trimmedMsg = message.trim()

        val colonIndex = trimmedMsg.indexOf(": ")
        if (colonIndex > 0 && !trimmedTitle.contains(": ")) {
            val possibleSender = trimmedMsg.substring(0, colonIndex).trim()
            val possibleMsg = trimmedMsg.substring(colonIndex + 2).trim()

            if (possibleSender.isNotEmpty() && possibleSender.length <= 40 && !possibleSender.contains("\n")) {
                return Pair("$trimmedTitle: $possibleSender", possibleMsg)
            }
        }
        return Pair(trimmedTitle, trimmedMsg)
    }
}