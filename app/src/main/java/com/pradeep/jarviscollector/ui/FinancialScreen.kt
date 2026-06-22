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
    val upcomingEvents = events.filter {
        val cat = it.category?.lowercase() ?: ""
        val stat = it.status?.lowercase() ?: ""
        stat == "upcoming" || cat == "bill"
    }

    val recentEvents = events.filter {
        val cat = it.category?.lowercase() ?: ""
        val stat = it.status?.lowercase() ?: ""
        stat == "recent" || cat == "upi" || (stat != "upcoming" && cat != "bill" && stat != "important" && cat != "salary")
    }

    val importantEvents = events.filter {
        val cat = it.category?.lowercase() ?: ""
        val stat = it.status?.lowercase() ?: ""
        stat == "important" || cat == "salary"
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
            if (upcomingEvents.isNotEmpty()) {
                item {
                    FinancialSectionHeader(title = "UPCOMING PAYMENTS")
                }
                items(upcomingEvents) { event ->
                    FinancialCard(event = event)
                }
            }

            if (importantEvents.isNotEmpty()) {
                item {
                    FinancialSectionHeader(title = "IMPORTANT CREDITS & SALARY")
                }
                items(importantEvents) { event ->
                    FinancialCard(event = event)
                }
            }

            if (recentEvents.isNotEmpty()) {
                item {
                    FinancialSectionHeader(title = "RECENT ACTIVITY")
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
    val cat = event.category?.lowercase() ?: ""
    val isCredit = cat == "salary" || (event.amount != null && event.amount > 0 && cat != "bill")
    
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
                    text = event.merchant ?: event.category?.uppercase() ?: "Unknown Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val details = mutableListOf<String>()
                if (!event.category.isNullOrBlank()) details.add("Category: ${event.category}")
                if (!event.status.isNullOrBlank()) details.add("Status: ${event.status}")
                if (details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.joinToString(" | "),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!event.event_timestamp.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Date: ${event.event_timestamp}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (event.amount != null) {
                Spacer(modifier = Modifier.width(12.dp))
                val currencySymbol = if (event.currency?.uppercase() == "USD") "$" else "₹"
                Text(
                    text = (if (isCredit) "+" else "-") + currencySymbol + String.format("%.2f", abs(event.amount)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
        }
    }
}
