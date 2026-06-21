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
import com.pradeep.jarviscollector.model.FinancialEventEntity
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    events: List<FinancialEventEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Categorize events by upcoming, recent, and important
    val upcomingEvents =
        events.filter {
            it.status == "upcoming" ||
            it.type.lowercase() == "bill"
        }

    val recentEvents =
        events.filter {
            it.status == "recent" ||
            it.type.lowercase() == "upi"
        }

    val importantEvents =
        events.filter {
            it.status == "important" ||
            it.type.lowercase() == "salary"
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Financial Agent",
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upcoming Payments Section
            if (upcomingEvents.isNotEmpty()) {
                item {
                    FinancialSectionHeader(title = "UPCOMING PAYMENTS")
                }
                items(upcomingEvents) { event ->
                    FinancialCard(event = event)
                }
            }

            // Important Updates / Credits Section
            if (importantEvents.isNotEmpty()) {
                item {
                    FinancialSectionHeader(title = "IMPORTANT CREDITS & SALARY")
                }
                items(importantEvents) { event ->
                    FinancialCard(event = event)
                }
            }

            // Recent UPI Activities Section
            if (recentEvents.isNotEmpty()) {
                item {
                    FinancialSectionHeader(title = "RECENT UPI ACTIVITY")
                }
                items(recentEvents) { event ->
                    FinancialCard(event = event)
                }
            }

            if (events.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        Text(
                            text = "No financial events synced.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
    )
}

@Composable
fun FinancialCard(
    event: FinancialEventEntity,
    modifier: Modifier = Modifier
) {
    val isCredit =
        event.type.lowercase() == "salary" ||
        (event.amount != null && event.amount > 0 && event.type.lowercase() != "bill")
    
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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!event.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!event.dueDate.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Due: ${event.dueDate}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (event.amount != null) {
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = (if (isCredit) "+" else "-") + "$" + String.format("%.2f", abs(event.amount)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
        }
    }
}
