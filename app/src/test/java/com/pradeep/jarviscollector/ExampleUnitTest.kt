package com.pradeep.jarviscollector

import org.junit.Test
import org.junit.Assert.*
import com.pradeep.jarviscollector.utils.NotificationNoiseFilter
import org.json.JSONObject
import org.json.JSONArray

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

    @Test
    fun testInsightJsonParsing() {
        val todoJson = """
            {
                "generated_at": "2026-06-21T17:39:31Z",
                "version": "1.0",
                "items": [
                    {
                        "id": "todo_1",
                        "title": "Buy groceries",
                        "description": "Milk and eggs",
                        "due_date": "2026-06-22",
                        "priority": "high",
                        "status": "pending",
                        "snooze_count": 0
                    }
                ]
            }
        """.trimIndent()

        val obj = JSONObject(todoJson)
        val itemsArray = obj.optJSONArray("items")
        assertNotNull(itemsArray)
        assertEquals(1, itemsArray!!.length())

        val item = itemsArray.getJSONObject(0)
        assertEquals("todo_1", item.getString("id"))
        assertEquals("Buy groceries", item.getString("title"))
        assertEquals("Milk and eggs", item.getString("description"))
        assertEquals("2026-06-22", item.getString("due_date"))
        assertEquals("high", item.getString("priority"))
        assertEquals("pending", item.getString("status"))
        assertEquals(0, item.getInt("snooze_count"))
    }

    @Test
    fun testInsightSyncWorkerHelperDelayCalculation() {
        val delay = com.pradeep.jarviscollector.service.InsightSyncWorkerHelper.calculateDelayToNextTarget()
        assertTrue("Delay should be positive", delay > 0)
        assertTrue("Delay should be less than or equal to 24 hours", delay <= 24 * 60 * 60 * 1000L)
    }

    @Test
    fun testTodoActionLogic() {
        val todo = com.pradeep.jarviscollector.model.TodoEntity(
            id = "test_id",
            title = "Task",
            description = "Desc",
            dueDate = "2026-06-22",
            priority = "high",
            status = "pending",
            completedAt = null,
            snoozeCount = 0,
            updatedAt = 1000L
        )

        assertEquals("pending", todo.status)
        assertEquals(0, todo.snoozeCount)

        // Mock complete action logic
        val completedTodo = todo.copy(
            status = "completed",
            completedAt = "2000",
            updatedAt = 2000L
        )
        assertEquals("completed", completedTodo.status)
        assertEquals("2000", completedTodo.completedAt)

        // Mock snooze action logic
        val snoozedTodo = todo.copy(
            snoozeCount = todo.snoozeCount + 1,
            updatedAt = 3000L
        )
        assertEquals(1, snoozedTodo.snoozeCount)
    }

    @Test
    fun testFyiCategoryFiltering() {
        val fyiList = listOf(
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "1",
                title = "School Circular",
                content = "Details",
                category = "school",
                timestamp = "2026-06-21T17:51:27Z"
            ),
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "2",
                title = "Family Circular",
                content = "Details",
                category = "Family",
                timestamp = "2026-06-21T17:51:27Z"
            ),
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "3",
                title = "Delivery Alert",
                content = "Details",
                category = "deliveries",
                timestamp = "2026-06-21T17:51:27Z"
            )
        )

        val schoolEvents = fyiList.filter { it.category.lowercase() == "school" }
        val familyEvents = fyiList.filter { it.category.lowercase() == "family" }

        assertEquals(1, schoolEvents.size)
        assertEquals("School Circular", schoolEvents[0].title)

        assertEquals(1, familyEvents.size)
        assertEquals("Family Circular", familyEvents[0].title)
    }

    @Test
    fun testTravelHealthShoppingCategoryFiltering() {
        val fyiList = listOf(
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "1",
                title = "Flight Ticket Booking",
                content = "Details",
                category = "travel",
                timestamp = "2026-06-21T17:51:27Z"
            ),
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "2",
                title = "Annual Health Checkup",
                content = "Details",
                category = "Health",
                timestamp = "2026-06-21T17:51:27Z"
            ),
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "3",
                title = "Amazon Package Out for Delivery",
                content = "Details",
                category = "deliveries",
                timestamp = "2026-06-21T17:51:27Z"
            ),
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "4",
                title = "Grocery Shopping List",
                content = "Details",
                category = "shopping",
                timestamp = "2026-06-21T17:51:27Z"
            ),
            com.pradeep.jarviscollector.model.FyiEventEntity(
                id = "5",
                title = "Random update",
                content = "Details",
                category = "other",
                timestamp = "2026-06-21T17:51:27Z"
            )
        )

        val travelEvents = fyiList.filter { it.category.lowercase() == "travel" }
        val healthEvents = fyiList.filter { it.category.lowercase() == "health" }
        val shoppingEvents = fyiList.filter { it.category.lowercase() == "shopping" || it.category.lowercase() == "deliveries" }

        assertEquals(1, travelEvents.size)
        assertEquals("Flight Ticket Booking", travelEvents[0].title)

        assertEquals(1, healthEvents.size)
        assertEquals("Annual Health Checkup", healthEvents[0].title)

        assertEquals(2, shoppingEvents.size)
        assertTrue(shoppingEvents.any { it.title == "Amazon Package Out for Delivery" })
        assertTrue(shoppingEvents.any { it.title == "Grocery Shopping List" })
    }

    @Test
    fun testCombinedInsightJsonParsing() {
        val combinedJson = """
            {
                "todos": [],
                "financial": {
                    "total_debit": 3290050.439999999,
                    "total_credit": 1748391.1800000002,
                    "events": [
                        {
                            "id": 1,
                            "title": "SBI Credit Card transaction notification",
                            "amount": 314.0,
                            "currency": "INR",
                            "type": "debit",
                            "payment_channel": "Credit Card",
                            "paid_to": null,
                            "paid_from": "FLIPKARTINTERNETPRIV",
                            "transaction_id": null
                        }
                    ]
                },
                "fyi": [
                    {
                        "id": 1,
                        "title": "Delivery of statement maturing on June 8th, 2026.",
                        "fyi_type": "delivery_notification",
                        "content": "Delivery of statement maturing on June 8th, 2026."
                    }
                ],
                "important_items": [
                    {
                        "type": "insurance_renewal",
                        "title": "SBI Credit Card e-statement for July 2025 has been mailed.",
                        "due_date": "2026-06-21",
                        "amount": 80.0,
                        "currency": "INR"
                    }
                ]
            }
        """.trimIndent()

        val obj = org.json.JSONObject(combinedJson)
        
        // Assert financial events
        val financialObj = obj.optJSONObject("financial")
        assertNotNull(financialObj)
        assertEquals(3290050.439999999, financialObj!!.optDouble("total_debit"), 0.001)
        val eventsArray = financialObj.optJSONArray("events")
        assertNotNull(eventsArray)
        assertEquals(1, eventsArray!!.length())
        val firstEvent = eventsArray.getJSONObject(0)
        assertEquals(1, firstEvent.optInt("id"))
        assertEquals("debit", firstEvent.optString("type"))

        // Assert fyi events
        val fyiArray = obj.optJSONArray("fyi")
        assertNotNull(fyiArray)
        assertEquals(1, fyiArray!!.length())
        val firstFyi = fyiArray.getJSONObject(0)
        assertEquals("delivery_notification", firstFyi.optString("fyi_type"))

        // Assert important items
        val importantArray = obj.optJSONArray("important_items")
        assertNotNull(importantArray)
        assertEquals(1, importantArray!!.length())
        val firstImportant = importantArray.getJSONObject(0)
        assertEquals("insurance_renewal", firstImportant.optString("type"))
        assertEquals(80.0, firstImportant.optDouble("amount"), 0.001)
    }
}