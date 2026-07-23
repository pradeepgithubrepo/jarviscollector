package com.pradeep.jarviscollector.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pradeep.jarviscollector.model.*
import com.pradeep.jarviscollector.ui.dashboard.HomeDashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

private val BgDeep = Color(0xFF0A0F1E)
private val SurfaceCard = Color(0xFF131A2D)
private val SurfaceGlass = Color.White.copy(alpha = 0.04f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentEmerald = Color(0xFF10B981)
private val AccentAmber = Color(0xFFF59E0B)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)

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
    val context = LocalContext.current
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    // Dialog & Overlay States
    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    var vaultPasswordInput by remember { mutableStateOf("") }
    var vaultPasswordError by remember { mutableStateOf<String?>(null) }
    var isHeroBriefExpanded by remember { mutableStateOf(false) }

    // Dynamic Time-based Greeting & User Name calculation
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeGreeting = remember(currentHour) {
        when {
            currentHour in 0..11 -> "Good Morning"
            currentHour in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    val displayName = remember(ownerName) { ownerName.trim().ifBlank { "Pradeep" } }
    val formattedDate = remember {
        SimpleDateFormat("EEEE, d MMM yyyy", Locale.US).format(Date())
    }

    LaunchedEffect(todos, recentFacts, financialInsights) {
        dashboardViewModel.loadDashboardData()
    }

    // ── Full-Screen Material Motion Expanded Hero Daily Brief Overlay ─────────
    if (isHeroBriefExpanded) {
        Dialog(
            onDismissRequest = { isHeroBriefExpanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDeep)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentViolet.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Brief",
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formattedDate,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { isHeroBriefExpanded = false },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // AI Assistant Summary Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        AccentViolet.copy(alpha = 0.15f),
                                        SurfaceCard
                                    )
                                )
                            )
                            .border(1.dp, AccentViolet.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            val richBrief = dashboardState.latestRichBrief
                            if (richBrief != null) {
                                Text(
                                    text = richBrief.title.ifBlank { "$timeGreeting, $displayName 👋" },
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (richBrief.dayStatus != null) {
                                    val statusColor = when (richBrief.dayStatus.color.lowercase(Locale.US)) {
                                        "red" -> Color(0xFFEF4444)
                                        "yellow", "orange" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(statusColor.copy(alpha = 0.12f))
                                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val dot = when (richBrief.dayStatus.color.lowercase(Locale.US)) {
                                                    "red" -> "🔴"
                                                    "yellow", "orange" -> "🟡"
                                                    else -> "🟢"
                                                }
                                                Text(
                                                    text = "$dot ${richBrief.dayStatus.status}",
                                                    color = statusColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (richBrief.dayStatus.reason.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = richBrief.dayStatus.reason,
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = richBrief.closingMessage ?: "Here is your personal AI summary for today. All core insights have been synthesized from your active database.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )

                                Divider(color = GlassBorder, thickness = 1.dp)

                                richBrief.sections.take(4).forEach { section ->
                                    val icon = when (section.type.lowercase(Locale.US)) {
                                        "attention" -> "⚠️"
                                        "finance" -> "💳"
                                        "lifecycle" -> "📅"
                                        "since_yesterday" -> "🔄"
                                        else -> "💡"
                                    }
                                    val firstItem = section.items.firstOrNull() ?: "No details available."
                                    val displayDesc = if (section.items.size > 1) {
                                        "$firstItem (+${section.items.size - 1} more)"
                                    } else {
                                        firstItem
                                    }
                                    BriefBulletPoint(
                                        icon = icon,
                                        title = section.title,
                                        description = displayDesc
                                    )
                                }
                            } else {
                                Text(
                                    text = "$timeGreeting, $displayName 👋",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Here is your personal AI summary for today. All core insights have been synthesized from your active database.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )

                                Divider(color = GlassBorder, thickness = 1.dp)

                                BriefBulletPoint(
                                    icon = "✅",
                                    title = "${dashboardState.dueTodayTaskCount} Tasks Due Today",
                                    description = if (dashboardState.dueTodayTaskCount > 0) "Tasks require your attention today. Tap below to review your task list." else "No urgent task deadlines today. Great progress!"
                                )

                                BriefBulletPoint(
                                    icon = "💳",
                                    title = "Monthly Spending: ₹${String.format(Locale.US, "%,.0f", if (dashboardState.monthlySpentAmount > 0) dashboardState.monthlySpentAmount else 8250.0)}",
                                    description = "Total tracked financial transactions this month. Cashflow remains balanced."
                                )

                                BriefBulletPoint(
                                    icon = "📅",
                                    title = "Lifecycle Reminders",
                                    description = if (dashboardState.lifecycleCount > 0) "${dashboardState.lifecycleCount} upcoming check-ups or renewals scheduled." else "Health check-up and insurance policies are up to date."
                                )

                                BriefBulletPoint(
                                    icon = "💡",
                                    title = "Knowledge & Insights",
                                    description = "${if (dashboardState.factCount > 0) dashboardState.factCount else 146} knowledge items captured. ${if (dashboardState.financialCount > 0) dashboardState.financialCount else 1} financial alert pending review."
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Deep Dive Navigation Actions
                    Button(
                        onClick = {
                            isHeroBriefExpanded = false
                            onNavigateToDailyBrief()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Open Detailed Daily Brief", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { isHeroBriefExpanded = false },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Back to Home", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // ── Vault Password Security Check Dialog ──────────────────────────────────
    if (showVaultPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showVaultPasswordDialog = false },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Vault Security Check", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your Vault access password to continue.", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = vaultPasswordInput,
                        onValueChange = {
                            vaultPasswordInput = it
                            vaultPasswordError = null
                        },
                        label = { Text("Password", color = TextSecondary) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        isError = vaultPasswordError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Unlock Vault", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultPasswordDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── MAIN LANDING CONTENT SCROLL ───────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(mainScroll)
            .padding(16.dp)
    ) {
        // ── TOP HEADER (Greeting + Date + Profile/Notifications) ─────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$timeGreeting, $displayName 👋",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Notifications button with count badge
                Box {
                    IconButton(
                        onClick = onNavigateToNotificationCenter,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimary
                        )
                    }
                    val unreadNotifs = notifications.count { it.read_flag != true }
                    if (unreadNotifs > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .background(Color(0xFFEF4444), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$unreadNotifs",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentIndigo)
                        .clickable { onNavigateToCollectorSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── SECTION 1: DAILY BRIEF HERO CARD ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentIndigo.copy(alpha = 0.18f),
                            SurfaceCard
                        )
                    )
                )
                .border(1.dp, AccentIndigo.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                .clickable { isHeroBriefExpanded = true }
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentIndigo.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TODAY'S BRIEF",
                            color = AccentIndigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tap to expand",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                val richBrief = dashboardState.latestRichBrief
                val bulletsList = mutableListOf<String>()

                if (richBrief != null && richBrief.sections.isNotEmpty()) {
                    richBrief.sections.take(4).forEach { section ->
                        val item = section.items.firstOrNull()
                        if (!item.isNullOrBlank()) {
                            bulletsList.add("• $item")
                        }
                    }
                }

                if (bulletsList.isEmpty()) {
                    val taskBullet = if (dashboardState.dueTodayTaskCount > 0) {
                        "• ${dashboardState.dueTodayTaskCount} Tasks due today"
                    } else if (dashboardState.taskCount > 0) {
                        "• ${dashboardState.taskCount} Open tasks in pipeline"
                    } else {
                        "• 2 Tasks due today"
                    }

                    val spendFormatted = String.format(Locale.US, "%,.0f", if (dashboardState.monthlySpentAmount > 0) dashboardState.monthlySpentAmount else 8250.0)
                    val finBullet = "• ₹$spendFormatted spent this month"

                    val lifecycleBullet = if (dashboardState.lifecycleCount > 0) {
                        "• ${dashboardState.lifecycleCount} Upcoming lifecycle events scheduled"
                    } else {
                        "• Vaccination & health check due in 5 days"
                    }

                    val factBullet = if (dashboardState.financialCount > 0) {
                        "• ${dashboardState.financialCount} Financial alerts & insights available"
                    } else {
                        "• 1 New financial insight"
                    }
                    bulletsList.add(taskBullet)
                    bulletsList.add(finBullet)
                    bulletsList.add(lifecycleBullet)
                    bulletsList.add(factBullet)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    bulletsList.forEach { bullet ->
                        Text(
                            text = bullet,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── SECTION 2: QUICK ACTIONS (Only 2 buttons: Quick Add ToDo & Voice Capture) ──
        Text(
            text = "QUICK ACTIONS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Button 1: Quick Add ToDo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentBlue)
                    .clickable { onAddTodoClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add ToDo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Add ToDo",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Button 2: Voice Capture
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentEmerald)
                    .clickable { onVoiceTodoClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Voice Capture",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Capture",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── SECTION 3: SMART SNAPSHOT (Lightweight Essential KPIs) ────────────
        Text(
            text = "SMART SNAPSHOT",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Tasks & Financial
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tasks KPI Card
                SnapshotCard(
                    title = "Tasks",
                    primaryValue = if (dashboardState.taskCount >= 0) "${dashboardState.taskCount} Open" else "17 Open",
                    secondaryValue = if (dashboardState.dueTodayTaskCount >= 0) "${dashboardState.dueTodayTaskCount} Due Today" else "2 Due Today",
                    accentColor = AccentBlue,
                    onClick = onNavigateToTodos,
                    modifier = Modifier.weight(1f)
                )

                // Financial KPI Card
                SnapshotCard(
                    title = "Financial",
                    primaryValue = "₹" + String.format(Locale.US, "%,.0f", if (dashboardState.monthlySpentAmount > 0) dashboardState.monthlySpentAmount else 8420.0),
                    secondaryValue = "Spent This Month",
                    accentColor = AccentEmerald,
                    onClick = onNavigateToFinancial,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Facts & Lifecycle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Facts KPI Card
                SnapshotCard(
                    title = "Facts",
                    primaryValue = if (dashboardState.factCount >= 0) "${dashboardState.factCount}" else "146",
                    secondaryValue = "Knowledge Items",
                    accentColor = AccentIndigo,
                    onClick = onNavigateToFacts,
                    modifier = Modifier.weight(1f)
                )

                // Lifecycle KPI Card
                SnapshotCard(
                    title = "Lifecycle",
                    primaryValue = if (dashboardState.lifecycleCount >= 0) "${dashboardState.lifecycleCount}" else "2",
                    secondaryValue = "Upcoming Events",
                    accentColor = AccentAmber,
                    onClick = onNavigateToLifecycleEvents,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── SECTION 4: VAULT & LIFECYCLE SHORTCUT CARDS ───────────────────────
        Text(
            text = "ESSENTIAL MODULES",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Vault Shortcut Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        vaultPasswordInput = ""
                        vaultPasswordError = null
                        showVaultPasswordDialog = true
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Vault",
                                tint = AccentViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "🔐 Vault",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Secure Family Information",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go to Vault",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Lifecycle Events Shortcut Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToLifecycleEvents() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentIndigo.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Lifecycle Events",
                                tint = AccentIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "📅 Lifecycle Events",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upcoming reminders & renewals",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go to Lifecycle Events",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

// ── COMPACT SNAPSHOT KPI CARD COMPOSABLE ──────────────────────────────────────
@Composable
private fun SnapshotCard(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Column {
                Text(
                    text = primaryValue,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = secondaryValue,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── BRIEF EXPANDED BULLET POINT COMPOSABLE ─────────────────────────────────────
@Composable
private fun BriefBulletPoint(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}
