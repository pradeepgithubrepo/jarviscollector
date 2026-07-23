package com.pradeep.jarviscollector.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.ReminderEntity
import java.text.SimpleDateFormat
import java.util.*

enum class TodoFilter {
    ALL, TODAY, OVERDUE, COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    todos: List<TodoEntity>,
    reminders: List<ReminderEntity>,
    onComplete: (String) -> Unit,
    onSnooze: (String, Int) -> Unit,
    onSetReminder: (String, Long, Int, String) -> Unit,
    onRemoveReminder: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onAddTodoClick: () -> Unit,
    onVoiceTodoClick: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(TodoFilter.ALL) }
    var showSnoozeDialogForId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Load active tasks
    val filteredList = remember(todos, activeFilter) {
        val list = todos.filter { it.status.uppercase(Locale.US) != "DISMISSED" && it.status.uppercase(Locale.US) != "COMPLETED" }
        when (activeFilter) {
            TodoFilter.ALL -> list
            TodoFilter.TODAY -> list.filter {
                it.due_date == todayStr
            }
            TodoFilter.OVERDUE -> list.filter {
                val due = it.due_date
                due != null && due < todayStr
            }
            TodoFilter.COMPLETED -> todos.filter { it.status.uppercase(Locale.US) == "COMPLETED" && it.status.uppercase(Locale.US) != "DISMISSED" }
        }
    }

    val priorityWeights = remember {
        mapOf("CRITICAL" to 4, "URGENT" to 4, "HIGH" to 3, "MEDIUM" to 2, "LOW" to 1)
    }

    fun sortTasks(tasks: List<TodoEntity>): List<TodoEntity> {
        return tasks.sortedWith(
            compareByDescending<TodoEntity> { priorityWeights[it.priority?.uppercase(Locale.US)] ?: 0 }
                .thenBy { it.due_date ?: "9999-12-31" }
        )
    }

    // Partition tasks
    val userAddedTasks = remember(filteredList, activeFilter) {
        if (activeFilter == TodoFilter.COMPLETED) emptyList()
        else sortTasks(filteredList.filter { it.source_agent?.uppercase(Locale.US) == "USER" })
    }

    val systemAddedTasks = remember(filteredList, activeFilter) {
        if (activeFilter == TodoFilter.COMPLETED) emptyList()
        else sortTasks(filteredList.filter { it.source_agent?.uppercase(Locale.US) != "USER" })
    }

    val completedTasksSorted = remember(filteredList, activeFilter) {
        if (activeFilter == TodoFilter.COMPLETED) {
            sortTasks(filteredList)
        } else {
            emptyList()
        }
    }

    // Sequentially trigger pickers for setting task alarm reminder
    fun triggerReminderPickers(todoId: String) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        onSetReminder(todoId, calendar.timeInMillis, 0, "DEFAULT")
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
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
                title = { Text("ToDos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent, // Gradient drawn on wrapper below
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Task FAB
                SmallFloatingActionButton(
                    onClick = onVoiceTodoClick,
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Voice Task", modifier = Modifier.size(18.dp))
                }

                // Text Add Task FAB
                FloatingActionButton(
                    onClick = onAddTodoClick,
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF070B14))
                )
            )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TodoFilter.values().forEach { filter ->
                    val isSelected = activeFilter == filter
                    val label = when (filter) {
                        TodoFilter.ALL -> "Total"
                        TodoFilter.TODAY -> "Today"
                        TodoFilter.OVERDUE -> "Overdue"
                        TodoFilter.COMPLETED -> "Completed"
                    }
                    val count = when (filter) {
                        TodoFilter.ALL -> todos.count { it.status.uppercase(Locale.US) != "DISMISSED" && it.status.uppercase(Locale.US) != "COMPLETED" }
                        TodoFilter.TODAY -> todos.count { it.due_date == todayStr && it.status.uppercase(Locale.US) != "COMPLETED" && it.status.uppercase(Locale.US) != "DISMISSED" }
                        TodoFilter.OVERDUE -> todos.count { val due = it.due_date; due != null && due < todayStr && it.status.uppercase(Locale.US) != "COMPLETED" && it.status.uppercase(Locale.US) != "DISMISSED" }
                        TodoFilter.COMPLETED -> todos.count { it.status.uppercase(Locale.US) == "COMPLETED" && it.status.uppercase(Locale.US) != "DISMISSED" }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { activeFilter = filter },
                        label = { Text("$label ($count)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E293B),
                            selectedContainerColor = Color(0xFF6366F1),
                            labelColor = Color(0xFF94A3B8),
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.05f),
                            selectedBorderColor = Color(0xFF6366F1),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks found",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.0f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeFilter == TodoFilter.COMPLETED) {
                        item {
                            Button(
                                onClick = onClearCompleted,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Text("🗑️ Clear All Completed Tasks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        items(completedTasksSorted, key = { it.todo_id }) { todo ->
                            val taskReminder = reminders.find { it.reminder_id == todo.todo_id }
                            TodoV2Card(
                                todo = todo,
                                reminder = taskReminder,
                                onClick = { onNavigateToTaskDetail(todo.todo_id) },
                                onComplete = { onComplete(todo.todo_id) },
                                onSnooze = { showSnoozeDialogForId = todo.todo_id },
                                onReminder = { triggerReminderPickers(todo.todo_id) },
                                onDelete = { onDelete(todo.todo_id) }
                            )
                        }
                    } else {
                        // User manual added tasks (separate section at top)
                        if (userAddedTasks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "📍 PERSONAL TASKS (USER ADDED)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(userAddedTasks, key = { it.todo_id }) { todo ->
                                val taskReminder = reminders.find { it.reminder_id == todo.todo_id }
                                TodoV2Card(
                                    todo = todo,
                                    reminder = taskReminder,
                                    onClick = { onNavigateToTaskDetail(todo.todo_id) },
                                    onComplete = { onComplete(todo.todo_id) },
                                    onSnooze = { showSnoozeDialogForId = todo.todo_id },
                                    onReminder = { triggerReminderPickers(todo.todo_id) },
                                    onDelete = { onDelete(todo.todo_id) }
                                )
                            }
                        }

                        // Agent/Signal added tasks
                        if (systemAddedTasks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "⚡ AGENT / SIGNAL TASKS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }

                            items(systemAddedTasks, key = { it.todo_id }) { todo ->
                                val taskReminder = reminders.find { it.reminder_id == todo.todo_id }
                                TodoV2Card(
                                    todo = todo,
                                    reminder = taskReminder,
                                    onClick = { onNavigateToTaskDetail(todo.todo_id) },
                                    onComplete = { onComplete(todo.todo_id) },
                                    onSnooze = { showSnoozeDialogForId = todo.todo_id },
                                    onReminder = { triggerReminderPickers(todo.todo_id) },
                                    onDelete = { onDelete(todo.todo_id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Snooze Alarm Dialog ---
    if (showSnoozeDialogForId != null) {
        val id = showSnoozeDialogForId!!
        AlertDialog(
            onDismissRequest = { showSnoozeDialogForId = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Snooze Alarm", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        15 to "15 minutes",
                        30 to "30 minutes",
                        60 to "1 hour",
                        120 to "2 hours"
                    ).forEach { (mins, label) ->
                        Button(
                            onClick = {
                                onSnooze(id, mins)
                                showSnoozeDialogForId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozeDialogForId = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun TodoV2Card(
    todo: TodoEntity,
    reminder: ReminderEntity?,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = todo.status.uppercase(Locale.US) == "COMPLETED"

    val priorityColor = when (todo.priority?.uppercase(Locale.US)) {
        "CRITICAL", "URGENT" -> Color(0xFFEF4444)
        "HIGH" -> Color(0xFFF97316)
        "MEDIUM" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981) // LOW
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left priority status indicator line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(priorityColor)
            )

            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = todo.title ?: "Untitled Task",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFF64748B) else Color.White,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )

                    // Priority Badge
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = todo.priority ?: "LOW",
                            color = priorityColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Due Date Info
                        if (todo.due_date != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Due Date",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Due: ${todo.due_date}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Active Alarm info
                        if (reminder != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Active Alarm",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val sdf = SimpleDateFormat("dd-MMM h:mm a", Locale.US)
                                val timeStr = sdf.format(Date(reminder.scheduled_timestamp))
                                Text(
                                    text = "Alarm: $timeStr",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Card options menu trigger
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Actions", tint = Color(0xFF94A3B8))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            if (!isCompleted) {
                                DropdownMenuItem(
                                    text = { Text("Complete Task", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981)) },
                                    onClick = {
                                        onComplete()
                                        showMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Snooze Alarm", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                onClick = {
                                    onSnooze()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set Reminder", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                onClick = {
                                    onReminder()
                                    showMenu = false
                                }
                            )
                            Divider(color = Color(0xFF334155))
                            DropdownMenuItem(
                                text = { Text("Delete Task", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
