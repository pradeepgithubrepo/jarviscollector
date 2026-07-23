package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.MobileSignal
import com.pradeep.jarviscollector.model.NotificationEvent
import com.pradeep.jarviscollector.model.UserPreferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BgDeep = Color(0xFF0A0F1E)
private val SurfaceCard = Color(0xFF131A2D)
private val SurfaceGlass = Color.White.copy(alpha = 0.04f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentEmerald = Color(0xFF10B981)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)

@Composable
fun NotificationScreen(
    notifications: List<NotificationEvent>,
    roomSignals: List<MobileSignal>,
    preferences: List<UserPreferenceEntity>,
    onTogglePreference: (String, Boolean) -> Unit,
    onLoadRoom: () -> Unit,
    onExportJson: () -> Unit,
    onSyncNow: () -> Unit,
    exportPath: String,
    isSyncing: Boolean,
    syncResultMessage: String?,
    onDismissSyncResult: () -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    isSyncingInsights: Boolean,
    insightSyncResultMessage: String?,
    onDismissInsightSyncResult: () -> Unit,
    onSyncInsights: () -> Unit,
    isBackfilling: Boolean,
    backfillStep: String?,
    backfillResultMessage: String?,
    backfillCompleted: Boolean,
    onStartBackfill: () -> Unit,
    onRunAgain: () -> Unit,
    onDismissBackfillResult: () -> Unit,
    onNavigateToDebugPipeline: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val mainScroll = rememberScrollState()

    // ── Hidden Developer Mode State ───────────────────────────────────────────
    var versionTapCount by remember { mutableIntStateOf(0) }
    var showDeveloperDialog by remember { mutableStateOf(false) }

    // Room DAO counts for Developer Console
    var factsCount by remember { mutableStateOf(-1) }
    var todosCount by remember { mutableStateOf(-1) }
    var fyiCount by remember { mutableStateOf(-1) }
    var notifsCount by remember { mutableStateOf(-1) }
    var finCount by remember { mutableStateOf(-1) }
    var familyCount by remember { mutableStateOf(-1) }

    LaunchedEffect(showDeveloperDialog) {
        if (showDeveloperDialog) {
            withContext(Dispatchers.IO) {
                try {
                    val db = JarvisDatabase.getDatabase(context)
                    factsCount = db.factInsightDao().getAll().size
                    todosCount = db.todoDao().getAll().size
                    val fyiList = db.fyiEventDao().getAll()
                    fyiCount = fyiList.size
                    familyCount = fyiList.count { it.category?.lowercase() == "family" }
                    notifsCount = db.notificationDao().getAll().size
                    finCount = db.financialInsightDao().getAll().size
                } catch (e: Exception) {
                    android.util.Log.e("NotificationScreen", "Direct DAO counts fail", e)
                }
            }
        }
    }

    // Active Owner Name formatting
    val currentOwnerClean = ownerName.ifBlank { "pradeep" }.lowercase()
    val familyMembers = listOf("pradeep", "shobana")

    // ── MAIN PROFILE CONTENT SCROLL ───────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(mainScroll)
            .padding(16.dp)
    ) {
        // Top Title Header
        Text(
            text = "Profile & Settings",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure your identity, preferences & application security",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── SECTION 1: USER PROFILE & FAMILY ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AccentViolet, AccentIndigo)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentOwnerClean.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = currentOwnerClean.replaceFirstChar { it.uppercase() },
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Primary User • Active Profile",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Divider(color = GlassBorder, thickness = 1.dp)

                // Family Member Selector Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ACTIVE FAMILY MEMBER",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        familyMembers.forEach { member ->
                            val isSelected = currentOwnerClean == member
                            val memberCapitalized = member.replaceFirstChar { it.uppercase() }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) AccentIndigo.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentIndigo else GlassBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onOwnerNameChange(member) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(AccentEmerald)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = memberCapitalized,
                                        color = if (isSelected) AccentIndigo else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── SECTION 2: NOTIFICATIONS ──────────────────────────────────────────
        Text(
            text = "NOTIFICATION PREFERENCES",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                val notificationRows = listOf(
                    "todo_notifications_enabled" to Pair("ToDo Notifications", "Reminders for task deadlines & schedule alerts"),
                    "financial_notifications_enabled" to Pair("Financial Notifications", "Spending alerts, bill due dates & unusual transactions"),
                    "brief_notifications_enabled" to Pair("Daily Brief Notifications", "Morning & evening intelligent summary popups"),
                    "lifecycle_notifications_enabled" to Pair("Lifecycle Event Reminders", "Health check-up, insurance & vehicle renewals")
                )

                notificationRows.forEachIndexed { index, (key, info) ->
                    val (label, description) = info
                    val prefEntity = preferences.find { it.preference_key == key }
                    val isChecked = prefEntity?.preference_value == "true" || prefEntity == null

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = isChecked,
                            onCheckedChange = { enabled -> onTogglePreference(key, enabled) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentIndigo,
                                checkedTrackColor = AccentIndigo.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (index < notificationRows.lastIndex) {
                        Divider(color = GlassBorder, thickness = 0.5.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── SECTION 3: APPLICATION & SECURITY ────────────────────────────────
        Text(
            text = "APPLICATION & SECURITY",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Appearance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Appearance",
                            tint = AccentViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Appearance Theme", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Dark Theme (System Default)", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentViolet.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Dark",
                            color = AccentViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Divider(color = GlassBorder, thickness = 0.5.dp)

                // Vault Security
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault Security",
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Vault Password Protection", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Secured family records & assets", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Configured",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Divider(color = GlassBorder, thickness = 0.5.dp)

                // Supabase Sync Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Status",
                            tint = AccentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cloud Database Sync", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Supabase jarvis_insights_schemav1", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Synchronized",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── SECTION 4: ABOUT FOOTER & HIDDEN DEVELOPER GESTURE ───────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    versionTapCount++
                    if (versionTapCount >= 5) {
                        versionTapCount = 0
                        showDeveloperDialog = true
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Jarvis",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Personal Intelligence Platform",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Version 1.0.0 (Build 104)",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ── HIDDEN DEVELOPER & DIAGNOSTIC CONSOLE DIALOG ─────────────────────────
    if (showDeveloperDialog) {
        Dialog(
            onDismissRequest = { showDeveloperDialog = false },
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
                            Text("🛠️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Developer & Diagnostics Console",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showDeveloperDialog = false },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Open Debug Pipeline Button
                    Button(
                        onClick = {
                            showDeveloperDialog = false
                            onNavigateToDebugPipeline?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("OPEN DEBUG DATA PIPELINE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Room DAO Counts Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ROOM DAO COUNTS (Direct)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFEF4444))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Facts: $factsCount", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Todos: $todosCount", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("FYI: $fyiCount", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Notifications: $notifsCount", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Financial: $finCount", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Family: $familyCount", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Controls (Load Room, Export JSON, Sync Now, Sync Insights)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onLoadRoom,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Load From Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onExportJson,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onSyncNow,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onSyncInsights,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sync Insights", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                com.pradeep.jarviscollector.service.JarvisReminderReceiver.triggerTestNotification(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("📢 Trigger Test Notification", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Historical Backfill Section
                    Text("Historical Backfill", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (backfillCompleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = {}, enabled = false, shape = RoundedCornerShape(10.dp)) {
                                Text("Completed", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = onRunAgain, shape = RoundedCornerShape(10.dp)) {
                                Text("Run Again", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(onClick = onStartBackfill, shape = RoundedCornerShape(10.dp)) {
                            Text("Start Backfill", fontSize = 12.sp)
                        }
                    }

                    if (exportPath.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Exported To: $exportPath", color = AccentEmerald, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Room Signals List
                    Text("Room Signals (${roomSignals.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    roomSignals.take(10).forEach { signal ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCard)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Source: ${signal.source} | Sender: ${signal.sender}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Message: ${signal.message}", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog Feedback Overlays ──────────────────────────────────────────────
    if (syncResultMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissSyncResult,
            containerColor = SurfaceCard,
            title = { Text("Sync Finished", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(syncResultMessage, color = TextSecondary) },
            confirmButton = {
                Button(onClick = onDismissSyncResult, colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)) {
                    Text("OK")
                }
            }
        )
    }

    if (isSyncingInsights) {
        Dialog(onDismissRequest = {}) {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentViolet)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Syncing insights...", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (insightSyncResultMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissInsightSyncResult,
            containerColor = SurfaceCard,
            title = { Text("Insights Sync Finished", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(insightSyncResultMessage, color = TextSecondary) },
            confirmButton = {
                Button(onClick = onDismissInsightSyncResult, colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)) {
                    Text("OK")
                }
            }
        )
    }

    if (isBackfilling) {
        Dialog(onDismissRequest = {}) {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentIndigo)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Historical Backfill...", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(backfillStep ?: "Processing...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }

    if (backfillResultMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissBackfillResult,
            containerColor = SurfaceCard,
            title = { Text("Historical Backfill Completed", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(backfillResultMessage, color = TextSecondary) },
            confirmButton = {
                Button(onClick = onDismissBackfillResult, colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)) {
                    Text("OK")
                }
            }
        )
    }
}