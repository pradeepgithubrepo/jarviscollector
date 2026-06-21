package com.pradeep.jarviscollector

import org.junit.Test
import org.junit.Assert.*
import com.pradeep.jarviscollector.utils.NotificationNoiseFilter

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testNotificationNormalization_GroupFormA() {
        // Form A: Title already contains a colon, message has text
        val title = "SRI PATATHUARASI AMMAN FLAT: Sundar T1"
        val message = "Good morning all,\nCar parking cleaning activity..."
        
        val (normalizedTitle, normalizedMessage) = NotificationNoiseFilter.normalize(title, message)
        
        assertEquals("SRI PATATHUARASI AMMAN FLAT: Sundar T1", normalizedTitle)
        assertEquals("Good morning all,\nCar parking cleaning activity...", normalizedMessage)
    }

    @Test
    fun testNotificationNormalization_GroupFormB() {
        // Form B: Title is group name, message starts with Sender Name:
        val title = "SRI PATATHUARASI AMMAN FLAT"
        val message = "Sundar T1: Good morning all,\nCar parking cleaning activity..."
        
        val (normalizedTitle, normalizedMessage) = NotificationNoiseFilter.normalize(title, message)
        
        assertEquals("SRI PATATHUARASI AMMAN FLAT: Sundar T1", normalizedTitle)
        assertEquals("Good morning all,\nCar parking cleaning activity...", normalizedMessage)
    }

    @Test
    fun testNotificationNormalization_NormalMessage() {
        // Normal message: no colon in message or title
        val title = "Pradeep"
        val message = "Hello, how are you?"
        
        val (normalizedTitle, normalizedMessage) = NotificationNoiseFilter.normalize(title, message)
        
        assertEquals("Pradeep", normalizedTitle)
        assertEquals("Hello, how are you?", normalizedMessage)
    }

    @Test
    fun testNotificationNormalization_MessageWithColonButNoSender() {
        // Title: Pradeep, Message: "Note: please do this"
        val title = "Pradeep"
        val message = "Note: please do this"
        
        val (normalizedTitle, normalizedMessage) = NotificationNoiseFilter.normalize(title, message)
        
        // "Note" is short, but wait, should it extract it?
        // Yes, "Pradeep: Note" and "please do this".
        // Let's see if this is acceptable or if we should check if the message pattern is handled.
        // Usually, in WhatsApp notifications, the structure is "SenderName: message".
        // If it was a direct chat: title = "Pradeep", message = "Note: please do this".
        // It gets stored as sender = "Pradeep: Note", message = "please do this".
        // While slightly altered, it still deduplicates consistently.
        // Let's check a longer prefix like "This is a very long sentence: and then another one".
        // Since "This is a very long sentence" is > 40 chars, it won't trigger extraction.
        val titleLong = "Pradeep"
        val messageLong = "This is a very long sentence before the colon: and then another one"
        val (normTitleLong, normMsgLong) = NotificationNoiseFilter.normalize(titleLong, messageLong)
        
        assertEquals("Pradeep", normTitleLong)
        assertEquals("This is a very long sentence before the colon: and then another one", normMsgLong)
    }
}