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
        if (cleanCategory == "shopping") {
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
                    FyiCategoryCard(event = event, tagText = tagText, accentColor = accentColor)
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
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.05f)
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = tagText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(
                            horizontal = 6.dp,
                            vertical = 2.dp
                        )
                    )
                }
                
                if (!event.created_at.isNullOrBlank()) {
                    Text(
                        text = event.created_at,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = event.title ?: "Update",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
        }
    }
}
