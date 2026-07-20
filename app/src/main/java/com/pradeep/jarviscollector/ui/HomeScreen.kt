package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.*
import com.pradeep.jarviscollector.ui.dashboard.HomeDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    ownerName: String,
    todos: List<TodoEntity>,
    financialEvents: List<FinancialEventEntity>,
    fyiEvents: List<FyiEventEntity>,
    latestBrief: DailyBriefEntity?,
    recentFacts: List<FactInsightEntity>,
    notifications: List<NotificationEntity> = emptyList(),
    recentActions: List<UserActionEntity> = emptyList(),
    financialInsights: List<FinancialInsightEntity> = emptyList(),
    onNavigateToTodos: () -> Unit,
    onNavigateToFinancial: () -> Unit,
    onNavigateToFyi: () -> Unit,
    onNavigateToDailyBrief: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToSchool: () -> Unit,
    onNavigateToTravel: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToCollectorSettings: () -> Unit,
    onNavigateToFacts: () -> Unit,
    onNavigateToNotificationCenter: () -> Unit,
    onNavigateToActionCenter: () -> Unit,
    onOwnerNameChange: (String) -> Unit,
    onLoadInsights: () -> Unit,
    onCompleteTodo: (String) -> Unit,
    onAddTodoClick: () -> Unit = {},
    onVoiceTodoClick: () -> Unit = {},
    onNavigateToTaskDetail: (String) -> Unit = {},
    onNavigateToFactDetail: (String) -> Unit = {},
    onNavigateToLifecycleEvents: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    modifier: Modifier = Modifier,
    dashboardViewModel: HomeDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val mainScroll = rememberScrollState()
    val quickActionScroll = rememberScrollState()

    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    var vaultPasswordInput by remember { mutableStateOf("") }
    var vaultPasswordError by remember { mutableStateOf<String?>(null) }
    val columnsScroll = rememberScrollState()
    val context = LocalContext.current

    val dashboardState by dashboardViewModel.uiState.collectAsState()

    // ── Morning Brief Popup ─────────────────────────────────────────────────
    var showMorningPopup by remember { mutableStateOf(false) }
    LaunchedEffect(dashboardState.latestBriefPreview) {
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val todayKey = "brief_popup_shown_" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val alreadyShown = prefs.getBoolean(todayKey, false)
        val briefTypeVal = dashboardState.briefType?.uppercase() ?: ""
        val isMorningType = briefTypeVal != "EVENING"
        if (!alreadyShown && dashboardState.latestBriefPreview != null && isMorningType) {
            showMorningPopup = true
        }
    }
    if (showMorningPopup) {
        MorningBriefDialog(
            briefPreview = dashboardState.latestBriefPreview ?: "",
            onOpenBrief = {
                showMorningPopup = false
                val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                val todayKey = "brief_popup_shown_" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                prefs.edit().putBoolean(todayKey, true).apply()
                onNavigateToDailyBrief()
            },
            onDismiss = {
                showMorningPopup = false
                val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                val todayKey = "brief_popup_shown_" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                prefs.edit().putBoolean(todayKey, true).apply()
            }
        )
    }
    // ───────────────────────────────────────────────────────────────────────

    LaunchedEffect(todos, recentFacts, financialInsights) {
        dashboardViewModel.loadDashboardData()
    }

    fun formatCount(count: Int, suffix: String): String {
        return if (dashboardState.isError) "N/A"
        else if (count == -1) "--"
        else "$count $suffix"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(mainScroll)
            .padding(16.dp)
    ) {
        // 1. HEADER SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Jarvis Collector",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Good morning, Pradeep 👋",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Sunday, 30 Jun 2026",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
                Box {
                    IconButton(onClick = onNavigateToNotificationCenter) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(16.dp)
                            .background(Color(0xFFEF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4F46E5), CircleShape)
                        .clickable { onNavigateToCollectorSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Temporary shortcut button for Vault
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                .clickable {
                    vaultPasswordInput = ""
                    vaultPasswordError = null
                    showVaultPasswordDialog = true
                }
                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault",
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🔐 Vault (Secure Family Information)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Secure accounts, investments, insurance policies & assets",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Go",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showVaultPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showVaultPasswordDialog = false },
                containerColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6))
                        Spacer(Modifier.width(8.dp))
                        Text("Vault Security Check", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enter your Vault access password to continue.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        OutlinedTextField(
                            value = vaultPasswordInput,
                            onValueChange = {
                                vaultPasswordInput = it
                                vaultPasswordError = null
                            },
                            label = { Text("Password", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            isError = vaultPasswordError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF8B5CF6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (vaultPasswordError != null) {
                            Text(vaultPasswordError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (vaultPasswordInput == "charanammu") {
                                showVaultPasswordDialog = false
                                onNavigateToVault()
                            } else {
                                vaultPasswordError = "Invalid Password"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Unlock Vault", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVaultPasswordDialog = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Temporary shortcut button for Lifecycle Events
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                .clickable { onNavigateToLifecycleEvents() }
                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Lifecycles",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Lifecycle Events Tracker (NEW)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure recurring insurances, checkups & renewals",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Go",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QUICK ACTIONS SECTION
        Text(
            text = "QUICK ACTIONS",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(quickActionScroll),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                text = "Add Transaction",
                icon = Icons.Default.Add,
                backgroundColor = Color(0xFF4F46E5),
                onClick = onNavigateToFinancial
            )
            QuickActionButton(
                text = "Add To-Do",
                icon = Icons.Default.CheckCircle,
                backgroundColor = Color(0xFF2563EB),
                onClick = onAddTodoClick
            )
            QuickActionButton(
                text = "Add Note",
                icon = Icons.Default.Edit,
                backgroundColor = Color(0xFF0D9488),
                onClick = {}
            )
            QuickActionButton(
                text = "Add FYI",
                icon = Icons.Default.Info,
                backgroundColor = Color(0xFFD97706),
                onClick = onNavigateToFyi
            )
            QuickActionButton(
                text = "Voice To-Do",
                icon = Icons.Default.PlayArrow,
                backgroundColor = Color(0xFF059669),
                onClick = onVoiceTodoClick
            )
            QuickActionButton(
                text = "Lifecycles",
                icon = Icons.Default.DateRange,
                backgroundColor = Color(0xFF8B5CF6),
                onClick = onNavigateToLifecycleEvents
            )
            QuickActionButton(
                text = "More Options",
                icon = Icons.Default.MoreVert,
                backgroundColor = Color(0xFF475569),
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PRIMARY TILE GRID (2x2 Grid Live Data Bindings)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryTile(
                        title = "Tasks",
                        value = formatCount(dashboardState.taskCount, "Open"),
                        subtitle = "Pending",
                        caption = "Tasks Pending",
                        icon = Icons.Default.List,
                        backgroundColor = Color(0xFF312E81),
                        onClick = onNavigateToTodos
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryTile(
                        title = "Facts / FYI",
                        value = formatCount(dashboardState.factCount, "Available"),
                        subtitle = "New Updates",
                        caption = "Facts Collected",
                        icon = Icons.Default.Star,
                        backgroundColor = Color(0xFF1E3A8A),
                        onClick = onNavigateToFacts
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryTile(
                        title = "Financial",
                        value = formatCount(dashboardState.financialCount, "Records"),
                        subtitle = "Room DB Facts",
                        caption = "Records Collected",
                        icon = Icons.Default.ShoppingCart,
                        backgroundColor = Color(0xFF064E3B),
                        onClick = onNavigateToFinancial
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryTile(
                        title = "Alerts",
                        value = formatCount(dashboardState.alertCount, "Pending"),
                        subtitle = "High Priority",
                        caption = "Alerts Action",
                        icon = Icons.Default.Warning,
                        backgroundColor = Color(0xFF78350F),
                        onClick = onNavigateToNotificationCenter
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECONDARY TILES (Health & Family Side-by-Side)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                PrimaryTile(
                    title = "Health",
                    value = "2",
                    subtitle = "Upcoming",
                    caption = "1 Reminder",
                    icon = Icons.Default.Favorite,
                    backgroundColor = Color(0xFF881337),
                    onClick = onNavigateToHealth
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                PrimaryTile(
                    title = "Family",
                    value = "4",
                    subtitle = "Events This Week",
                    caption = "2 Upcoming",
                    icon = Icons.Default.AccountBox,
                    backgroundColor = Color(0xFF115E59),
                    onClick = onNavigateToFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // DAILY BRIEF CARD
        if (dashboardState.latestBriefPreview != null) {
            DailyBriefPreviewCard(
                briefPreview = dashboardState.latestBriefPreview!!,
                generatedAt = dashboardState.briefGeneratedAt,
                briefType = dashboardState.briefType,
                onClick = onNavigateToDailyBrief
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // SUMMARY CARDS (Three Horizontally Scrollable Columns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(columnsScroll),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TODAY'S TASKS CARD
            Box(modifier = Modifier.width(300.dp)) {
                val state = dashboardState
                val mappedTasks = when {
                    state.isError -> null
                    state.todayTasks == null -> null
                    else -> state.todayTasks.map { task ->
                        val color = when (task.priority.uppercase()) {
                            "HIGH" -> Color(0xFFEF4444)
                            "LOW" -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        }
                        SummaryItem(
                            title = task.title,
                            subtitle = task.subtitle,
                            badgeText = task.priority,
                            badgeColor = color,
                            icon = Icons.Default.Check,
                            onClick = { onNavigateToTaskDetail(task.id) }
                        )
                    }
                }

                SummaryCard(
                    title = "TODAY'S TASKS",
                    onViewAllClick = onNavigateToTodos,
                    items = mappedTasks,
                    emptyMessage = "No open tasks",
                    errorMessage = "Unable to load"
                )
            }

            // 2. LATEST FACTS / FYI CARD
            Box(modifier = Modifier.width(300.dp)) {
                val state = dashboardState
                val mappedFacts = when {
                    state.isError -> null
                    state.latestFacts == null -> null
                    else -> state.latestFacts.map { fact ->
                        SummaryItem(
                            title = fact.summary.ifEmpty { fact.title },
                            subtitle = fact.category,
                            badgeText = "",
                            badgeColor = Color.Transparent,
                            bulletColor = Color(0xFF6366F1),
                            onClick = { onNavigateToFactDetail(fact.id) }
                        )
                    }
                }

                SummaryCard(
                    title = "LATEST FACTS / FYI",
                    onViewAllClick = onNavigateToFacts,
                    items = mappedFacts,
                    emptyMessage = "No facts available",
                    errorMessage = "Unable to load"
                )
            }

            // 3. UPCOMING EVENTS CARD
            Box(modifier = Modifier.width(300.dp)) {
                val state = dashboardState
                val mappedEvents = when {
                    state.isError -> null
                    state.upcomingEvents == null -> null
                    else -> state.upcomingEvents.map { event ->
                        SummaryItem(
                            title = event.title,
                            subtitle = event.subtitle,
                            badgeText = "",
                            badgeColor = Color.Transparent,
                            icon = Icons.Default.DateRange,
                            onClick = { onNavigateToTaskDetail(event.id) }
                        )
                    }
                }

                SummaryCard(
                    title = "UPCOMING EVENTS",
                    onViewAllClick = onNavigateToDailyBrief,
                    items = mappedEvents,
                    emptyMessage = "No upcoming events",
                    errorMessage = "Unable to load"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(56.dp)
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PrimaryTile(
    title: String,
    value: String,
    subtitle: String,
    caption: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = caption,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Navigate",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    onViewAllClick: () -> Unit,
    items: List<SummaryItem>?,
    emptyMessage: String,
    errorMessage: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F46E5),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "View all",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.clickable { onViewAllClick() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            when {
                items == null -> {
                    // Loading State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading...",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                items.isEmpty() -> {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyMessage,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable { item.onClick() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.bulletColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .size(8.dp)
                                            .background(item.bulletColor, CircleShape)
                                    )
                                } else if (item.icon != null) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .size(18.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.subtitle.isNotEmpty()) {
                                        Text(
                                            text = item.subtitle,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                
                                if (item.badgeText.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = item.badgeColor.copy(alpha = 0.15f),
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Text(
                                            text = item.badgeText,
                                            color = item.badgeColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SummaryItem(
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val badgeColor: Color,
    val icon: ImageVector? = null,
    val bulletColor: Color? = null,
    val onClick: () -> Unit = {}
)

// ─────────────────────────────────────────────────────────────────────────────
// Daily Brief Preview Card (inserted on Home Dashboard)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DailyBriefPreviewCard(
    briefPreview: String,
    generatedAt: String?,
    briefType: String?,
    onClick: () -> Unit
) {
    val isMorning = briefType?.uppercase() != "EVENING"
    val emoji = if (isMorning) "☀️" else "🌙"
    val accentColor = if (isMorning) Color(0xFFF59E0B) else Color(0xFF8B5CF6)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color(0xFF1E293B)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DAILY BRIEF",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = briefPreview,
                        color = Color(0xFFF1F5F9),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp
                    )
                    if (!generatedAt.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = try {
                                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                                val out = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US)
                                val d = fmt.parse(generatedAt.substring(0, minOf(19, generatedAt.length)))
                                if (d != null) out.format(d) else generatedAt.substring(0, minOf(10, generatedAt.length))
                            } catch (e: Exception) { generatedAt.substring(0, minOf(10, generatedAt.length)) },
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open Brief",
                    tint = accentColor.copy(alpha = 0.70f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Morning Brief Popup Dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MorningBriefDialog(
    briefPreview: String,
    onOpenBrief: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☀️", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Morning Brief",
                    color = Color(0xFFF1F5F9),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Your daily brief is ready.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A)
                ) {
                    Text(
                        text = briefPreview,
                        color = Color(0xFFF1F5F9),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenBrief,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Open Brief", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color(0xFF94A3B8))
            }
        }
    )
}
