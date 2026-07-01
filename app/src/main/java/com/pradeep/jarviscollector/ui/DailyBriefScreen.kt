package com.pradeep.jarviscollector.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pradeep.jarviscollector.ui.brief.BriefPayloadSection
import com.pradeep.jarviscollector.ui.brief.BriefSectionItem
import com.pradeep.jarviscollector.ui.brief.DailyBriefUiState
import com.pradeep.jarviscollector.ui.brief.DailyBriefViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens — matches Home Dashboard dark slate palette
// ─────────────────────────────────────────────────────────────────────────────
private val BriefBg        = Color(0xFF0F172A)
private val BriefSurface   = Color(0xFF1E293B)
private val BriefSurfaceAlt= Color(0xFF263548)
private val BriefAccent    = Color(0xFF3B82F6)
private val BriefGold      = Color(0xFFF59E0B)
private val BriefPurple    = Color(0xFF8B5CF6)
private val BriefGreen     = Color(0xFF10B981)
private val BriefTextPrimary   = Color(0xFFF1F5F9)
private val BriefTextSecondary = Color(0xFF94A3B8)
private val BriefDivider       = Color(0xFF334155)

// ─────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBriefScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyBriefViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            BriefTopBar(
                briefType = uiState.briefType,
                generatedAt = uiState.generatedAt,
                onBack = onBack
            )
        },
        containerColor = BriefBg
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BriefBg)
        ) {
            when {
                uiState.isLoading -> BriefLoadingState()
                uiState.isError   -> BriefErrorState(onRetry = { viewModel.loadBrief() })
                uiState.isEmpty   -> BriefEmptyState()
                else              -> BriefContent(uiState = uiState)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top App Bar
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefTopBar(
    briefType: String,
    generatedAt: String,
    onBack: () -> Unit
) {
    val isMorning = briefType.uppercase() != "EVENING"
    val icon   = if (isMorning) "☀️" else "🌙"
    val title  = if (isMorning) "Morning Brief" else "Evening Brief"
    val accent = if (isMorning) BriefGold else BriefPurple

    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$icon $title",
                        color = BriefTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.20f)
                    ) {
                        Text(
                            text = briefType.uppercase(),
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (generatedAt.isNotBlank()) {
                    Text(
                        text = "Generated ${formatGeneratedAt(generatedAt)}",
                        color = BriefTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = BriefTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BriefBg
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BriefContent(uiState: DailyBriefUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Summary Metrics Row ──────────────────────────────────────────────
        if (uiState.todoCount > 0 || uiState.fyiCount > 0 || uiState.factCount > 0) {
            item {
                BriefMetricsRow(
                    todoCount  = uiState.todoCount,
                    fyiCount   = uiState.fyiCount,
                    factCount  = uiState.factCount
                )
            }
        }

        // ── Divider ──────────────────────────────────────────────────────────
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = BriefDivider, thickness = 1.dp)
                Text(
                    text = "  BRIEF  ",
                    color = BriefTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Divider(modifier = Modifier.weight(1f), color = BriefDivider, thickness = 1.dp)
            }
        }

        // ── Content Sections ─────────────────────────────────────────────────
        itemsIndexed(uiState.sections) { _, item ->
            BriefContentCard(item = item)
        }

        // ── Payload Sections ─────────────────────────────────────────────────
        if (uiState.payloadSections.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Divider(modifier = Modifier.weight(1f), color = BriefDivider, thickness = 1.dp)
                    Text(
                        text = "  DETAILS  ",
                        color = BriefTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Divider(modifier = Modifier.weight(1f), color = BriefDivider, thickness = 1.dp)
                }
            }
            itemsIndexed(uiState.payloadSections) { _, section ->
                BriefPayloadSectionCard(section = section)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Metrics Row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BriefMetricsRow(todoCount: Int, fyiCount: Int, factCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BriefSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BriefMetricChip(
                label = "Tasks",
                count = todoCount,
                color = BriefAccent,
                icon = Icons.Default.List
            )
            VerticalDivider(color = BriefDivider)
            BriefMetricChip(
                label = "FYI",
                count = fyiCount,
                color = BriefGreen,
                icon = Icons.Default.Info
            )
            VerticalDivider(color = BriefDivider)
            BriefMetricChip(
                label = "Facts",
                count = factCount,
                color = BriefPurple,
                icon = Icons.Default.Star
            )
        }
    }
}

@Composable
private fun VerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(1.dp)
            .background(color)
    )
}

@Composable
private fun BriefMetricChip(label: String, count: Int, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = count.toString(),
            color = BriefTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = BriefTextSecondary,
            fontSize = 11.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Brief Content Card (one per item in the content array)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BriefContentCard(item: BriefSectionItem) {
    val indexColor = when (item.index % 4) {
        0    -> BriefAccent
        1    -> BriefGold
        2    -> BriefGreen
        else -> BriefPurple
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BriefSurface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Index bubble
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(indexColor.copy(alpha = 0.20f))
                    .border(1.dp, indexColor.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (item.index + 1).toString(),
                    color = indexColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.text,
                color = BriefTextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Payload Section Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BriefPayloadSectionCard(section: BriefPayloadSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BriefSurfaceAlt
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = section.heading,
                color = BriefAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            section.bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(BriefTextSecondary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bullet,
                        color = BriefTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// States: Loading / Empty / Error
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BriefLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BriefAccent)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Loading Brief…", color = BriefTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BriefEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No Brief Available Yet",
                color = BriefTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Your Daily Brief will appear here\nonce the AI pipeline generates one.",
                color = BriefTextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BriefErrorState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Unable to Load Brief",
                color = BriefTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", color = BriefAccent)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilities
// ─────────────────────────────────────────────────────────────────────────────
private fun formatGeneratedAt(raw: String): String {
    return try {
        val inputFmt  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val outputFmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
        val date = inputFmt.parse(raw.substring(0, minOf(19, raw.length)))
        if (date != null) outputFmt.format(date) else raw.substring(0, minOf(10, raw.length))
    } catch (e: Exception) {
        raw.substring(0, minOf(10, raw.length))
    }
}
