package com.pradeep.jarviscollector.ui.lifecycle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.LifecycleItemEntity

private val BgDeep = Color(0xFF0A0F1E)
private val Surface = Color(0xFF1A1F3A)
private val SurfaceGlass = Color.White.copy(0.04f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)

private val STANDARD_DOMAINS = listOf(
    "Health", "Insurance", "Vehicle", "Property", "Subscription", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifecycleEventsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LifecycleEventsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog & Form states
    var showFormDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<LifecycleItemEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<LifecycleItemEntity?>(null) }

    // Form inputs
    var domain by remember { mutableStateOf("Health") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var scheduleType by remember { mutableStateOf("ONCE") }
    var daysLater by remember { mutableStateOf("30") }
    var reminderOffsetDays by remember { mutableStateOf("7") }
    var status by remember { mutableStateOf("ACTIVE") }

    // Reset form helper
    val resetForm = { item: LifecycleItemEntity? ->
        editingItem = item
        if (item != null) {
            domain = item.domain ?: "Health"
            title = item.title ?: ""
            description = item.description ?: ""
            scheduleType = item.schedule_type ?: "ONCE"
            daysLater = if (item.schedule_type?.uppercase() == "RECURRING") {
                item.interval_days?.toString() ?: "365"
            } else {
                calculateDaysDifference(item.next_occurrence_date)
            }
            reminderOffsetDays = item.reminder_offset_days?.toString() ?: "7"
            status = item.status ?: "ACTIVE"
        } else {
            domain = "Health"
            title = ""
            description = ""
            scheduleType = "ONCE"
            daysLater = "30"
            reminderOffsetDays = "7"
            status = "ACTIVE"
        }
    }

    // Load message notifications
    LaunchedEffect(uiState.operationMessage) {
        val msg = uiState.operationMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearOperationMessage()
        }
    }

    // Confirm Delete Dialog
    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Event", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to delete \"${item.title}\"? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(item.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Create / Edit overlay Form Dialog
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            containerColor = Surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (editingItem != null) "Edit Lifecycle Event" else "Add Lifecycle Event",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Category Domain Selector buttons
                    Text("Category Domain", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            STANDARD_DOMAINS.take(3).forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (domain == d) AccentIndigo.copy(0.2f) else Color.Transparent)
                                        .border(1.dp, if (domain == d) AccentIndigo else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { domain = d }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(d, color = if (domain == d) AccentIndigo else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            STANDARD_DOMAINS.drop(3).forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (domain == d) AccentIndigo.copy(0.2f) else Color.Transparent)
                                        .border(1.dp, if (domain == d) AccentIndigo else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { domain = d }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(d, color = if (domain == d) AccentIndigo else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Details Description", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Schedule Type selection (ONCE vs RECURRING)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scheduleType = "ONCE" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scheduleType == "ONCE") AccentIndigo else Color.White.copy(0.05f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("One-Time", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { scheduleType = "RECURRING" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scheduleType == "RECURRING") AccentIndigo else Color.White.copy(0.05f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Recurring", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Days later selector
                    OutlinedTextField(
                        value = daysLater,
                        onValueChange = { daysLater = it },
                        label = { Text(if (scheduleType == "RECURRING") "Reoccurs every (days)" else "Occurs in (days from now)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Reminder offset days
                    OutlinedTextField(
                        value = reminderOffsetDays,
                        onValueChange = { reminderOffsetDays = it },
                        label = { Text("Reminder Offset (days before)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Status (ACTIVE / PAUSED) Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Schedule status", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Switch(
                            checked = status == "ACTIVE",
                            onCheckedChange = { status = if (it) "ACTIVE" else "PAUSED" },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentIndigo,
                                checkedTrackColor = AccentIndigo.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = daysLater.toIntOrNull() ?: 30
                        val computedNextDate = calculateNextDate(days)
                        val intervalVal = if (scheduleType == "RECURRING") days else null
                        val reminderOffsetVal = reminderOffsetDays.toIntOrNull()
                        if (title.isNotBlank()) {
                            val item = editingItem
                            if (item != null) {
                                viewModel.updateEvent(
                                    id = item.id,
                                    domain = domain,
                                    title = title,
                                    description = if (description.isBlank()) null else description,
                                    scheduleType = scheduleType,
                                    intervalDays = intervalVal,
                                    nextOccurrenceDate = computedNextDate,
                                    reminderOffsetDays = reminderOffsetVal,
                                    status = status
                                )
                            } else {
                                viewModel.addEvent(
                                    domain = domain,
                                    title = title,
                                    description = if (description.isBlank()) null else description,
                                    scheduleType = scheduleType,
                                    intervalDays = intervalVal,
                                    nextOccurrenceDate = computedNextDate,
                                    reminderOffsetDays = reminderOffsetVal,
                                    status = status
                                )
                            }
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Event", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Lifecycle Events", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncLifecycleEvents() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm(null)
                    showFormDialog = true
                },
                containerColor = AccentIndigo,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize().background(BgDeep)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDeep)
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentIndigo)
                    }
                }
                uiState.isError -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Unable to retrieve lifecycle events.\nCheck database profile configs.",
                            color = AccentRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                uiState.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No lifecycle events scheduled.",
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tapping the + button allows setting reminders for check-ups, insurances or vehicle services.",
                                color = TextSecondary.copy(0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.items) { item ->
                            LifecycleItemRow(
                                item = item,
                                onEdit = {
                                    resetForm(item)
                                    showFormDialog = true
                                },
                                onDelete = { showDeleteConfirmDialog = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifecycleItemRow(
    item: LifecycleItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val domainColor = when (item.domain?.lowercase()) {
        "health" -> Color(0xFF10B981)       // Green
        "insurance" -> Color(0xFF8B5CF6)    // Purple
        "vehicle" -> Color(0xFF3B82F6)      // Blue
        "property" -> Color(0xFFF59E0B)     // Orange
        "subscription" -> Color(0xFF6366F1) // Indigo
        else -> Color(0xFF64748B)           // Slate Grey
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Domain pill + status tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Domain tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(domainColor.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (item.domain ?: "Other").uppercase(),
                        color = domainColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                // Schedule Type & Recurrence Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.schedule_type?.uppercase() == "RECURRING") {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recurring",
                            tint = AccentIndigo,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Every ${item.interval_days ?: 365} days",
                            color = AccentIndigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "One-Time",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Pill
                    val isPaused = item.status?.uppercase() == "PAUSED"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPaused) Color.White.copy(0.08f) else AccentGreen.copy(0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.status ?: "ACTIVE",
                            color = if (isPaused) TextSecondary else AccentGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Body: Title + Details description
            Column {
                Text(
                    text = item.title ?: "Unnamed Event",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(color = GlassBorder, thickness = 0.5.dp)

            // Footer: Next scheduled date + edit/delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Occurrence date info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Next Scheduled:",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                        Text(
                            text = item.next_occurrence_date ?: "Not Set",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action buttons (Edit / Delete)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentRed.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AccentRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun calculateDaysDifference(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "30"
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val targetDate = sdf.parse(dateStr) ?: return "30"
        val today = sdf.parse(sdf.format(java.util.Date())) ?: return "30"
        val diffMs = targetDate.time - today.time
        val diffDays = diffMs / (24 * 60 * 60 * 1000)
        if (diffDays > 0) diffDays.toString() else "0"
    } catch (e: Exception) {
        "30"
    }
}

private fun calculateNextDate(days: Int): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.DAY_OF_YEAR, days)
    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(calendar.time)
}
