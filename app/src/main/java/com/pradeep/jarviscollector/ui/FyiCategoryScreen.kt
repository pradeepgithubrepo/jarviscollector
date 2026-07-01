package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FyiEventEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FyiCategoryScreen(
    category: String,
    events: List<FyiEventEntity>,
    onMarkRead: (String, Boolean) -> Unit,
    onDismiss: (String) -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanCategory = category.trim().lowercase()
    val (title, tagText, accentColor) = when (cleanCategory) {
        "family" -> Triple("Family Agent", "FAMILY UPDATE", Color(0xFFEC4899))
        "school" -> Triple("School Agent", "SCHOOL UPDATE", Color(0xFF3B82F6))
        "travel" -> Triple("Travel Agent", "TRAVEL UPDATE", Color(0xFF06B6D4))
        "health" -> Triple("Health Agent", "HEALTH UPDATE", Color(0xFFEF4444))
        else -> Triple("Shopping Agent", "SHOPPING UPDATE", Color(0xFFF59E0B))
    }

    val filteredEvents = events.filter {
        val eventCat = it.category?.trim()?.lowercase() ?: ""
        val active = it.status != "DISMISSED" && it.status != "ARCHIVED"
        active && if (cleanCategory == "shopping") {
            eventCat == "shopping" || eventCat == "deliveries"
        } else {
            eventCat == cleanCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (filteredEvents.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Text(
                    text = "No $cleanCategory updates found.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredEvents, key = { it.fyi_event_id }) { event ->
                    FyiCategoryCard(
                        event = event,
                        tagText = tagText,
                        accentColor = accentColor,
                        onMarkRead = onMarkRead,
                        onDismiss = onDismiss,
                        onViewEvidence = { onNavigateToSignalExplorer("fyi", event.fyi_event_id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FyiCategoryCard(
    event: FyiEventEntity,
    tagText: String,
    accentColor: Color,
    onMarkRead: (String, Boolean) -> Unit,
    onDismiss: (String) -> Unit,
    onViewEvidence: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isRead = event.read_flag == true

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isRead) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.05f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "READ",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                if (!event.created_at.isNullOrBlank()) {
                    Text(
                        text = event.created_at,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = event.title ?: "Update",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
            )
            
            if (!event.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = event.summary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onViewEvidence != null) {
                    TextButton(
                        onClick = onViewEvidence,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("View Evidence", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (!isRead) {
                    TextButton(
                        onClick = { onMarkRead(event.fyi_event_id, true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Mark Read", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(
                    onClick = { onDismiss(event.fyi_event_id) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Dismiss", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
