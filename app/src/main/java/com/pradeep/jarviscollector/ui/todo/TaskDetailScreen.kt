package com.pradeep.jarviscollector.ui.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.ReminderEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    todoId: String,
    todos: List<TodoEntity>,
    reminders: List<ReminderEntity>,
    onComplete: (String) -> Unit,
    onSnooze: (String, Int) -> Unit,
    onSetReminder: (String, Long, Int, String) -> Unit,
    onRemoveReminder: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val todo = todos.find { it.todo_id == todoId }
    val reminder = reminders.find { it.reminder_id == todoId }

    if (todo == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text("Task not found", color = Color.White)
        }
        return
    }

    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val priorityColor = when (todo.priority?.uppercase(Locale.US)) {
        "CRITICAL" -> Color(0xFFEF4444)
        "HIGH" -> Color(0xFFF97316)
        "MEDIUM" -> Color(0xFFF59E0B)
        else -> Color(0xFF9CA3AF) // LOW / DEFAULT
    }

    // Date formatter helper
    val formattedReceivedDate = remember(todo.created_at) {
        val rawDate = todo.created_at
        if (rawDate.isNullOrBlank()) {
            "Unknown"
        } else {
            try {
                // Handle formats: "2026-06-29T09:07:03.528226+00:00" or "2026-06-29 09:07:03"
                val cleaned = if (rawDate.contains(".")) rawDate.split(".")[0] else rawDate
                val cleaned2 = if (cleaned.contains("+")) cleaned.split("+")[0] else cleaned
                
                val sdfInputT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val sdfInputSpace = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val sdfInputDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                
                val date = try {
                    sdfInputT.parse(cleaned2)
                } catch (e: Exception) {
                    try {
                        sdfInputSpace.parse(cleaned2)
                    } catch (e2: Exception) {
                        sdfInputDate.parse(cleaned2)
                    }
                }
                
                if (date != null) {
                    SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(date)
                } else {
                    rawDate.split("T")[0]
                }
            } catch (e: Exception) {
                rawDate.split("T")[0]
            }
        }
    }

    // Function to trigger Calendar + Time Pickers sequentially
    fun triggerReminderPickers() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Trigger TimePickerDialog next
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        // Save the reminder with default alarm sound type
                        onSetReminder(todo.todo_id, calendar.timeInMillis, 0, "DEFAULT")
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false // 12-hour format with AM/PM
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Detail", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (todo.source_signal_id != null) {
                        IconButton(onClick = { onNavigateToSignalExplorer("todos", todo.todo_id) }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Signal Trace", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        bottomBar = {
            Surface(
                color = Color(0xFF1E293B),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Complete Action
                    if (todo.status.uppercase(Locale.US) != "COMPLETED") {
                        IconButtonWithText(
                            icon = Icons.Default.CheckCircle,
                            text = "Complete",
                            color = Color(0xFF10B981),
                            onClick = {
                                onComplete(todo.todo_id)
                                onBack()
                            }
                        )
                    }
                    
                    // Snooze Action
                    IconButtonWithText(
                        icon = Icons.Default.Refresh,
                        text = "Snooze",
                        color = Color(0xFF3B82F6),
                        onClick = { showSnoozeDialog = true }
                    )

                    // Simplified Reminder Action (DatePicker flow)
                    IconButtonWithText(
                        icon = Icons.Default.Notifications,
                        text = "Reminder",
                        color = if (reminder != null) Color(0xFFF59E0B) else Color.White,
                        onClick = { triggerReminderPickers() }
                    )

                    // Delete Action
                    IconButtonWithText(
                        icon = Icons.Default.Delete,
                        text = "Delete",
                        color = Color(0xFFEF4444),
                        onClick = { showDeleteConfirm = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Header Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(priorityColor, shape = RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = todo.title ?: "Untitled Task",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = todo.category ?: "General",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = priorityColor.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = todo.priority ?: "LOW",
                                    fontSize = 11.sp,
                                    color = priorityColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Task Metadata Card (Simplified per user request)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Metadata", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    MetadataRow(label = "Status", value = todo.status)
                    MetadataRow(label = "Due Date", value = todo.due_date ?: "No deadline set")
                    
                    if (reminder != null) {
                        val sdf = SimpleDateFormat("dd-MMM-yyyy h:mm a", Locale.US)
                        val remStr = sdf.format(Date(reminder.scheduled_timestamp))
                        MetadataRow(label = "Reminder", value = remStr)
                    } else {
                        MetadataRow(label = "Reminder", value = "No reminder set")
                    }

                    MetadataRow(label = "Received On", value = formattedReceivedDate)
                }
            }

            // Notes / Description Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Description & Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = todo.description ?: "No description provided.",
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Snooze Dialog
    if (showSnoozeDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Snooze Task", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val intervals = listOf(
                        15 to "15 Minutes",
                        30 to "30 Minutes",
                        60 to "1 Hour",
                        120 to "2 Hours",
                        1440 to "1 Day"
                    )
                    intervals.forEach { (mins, label) ->
                        Button(
                            onClick = {
                                onSnooze(todo.todo_id, mins)
                                showSnoozeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozeDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // 2. Delete Confirm Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Delete Task", color = Color.White) },
            text = { Text("Are you sure you want to permanently delete this task? This action will cancel any active alarms.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(todo.todo_id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun IconButtonWithText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = text, tint = color, modifier = Modifier.size(28.dp))
        }
        Text(text = text, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}
