package com.pradeep.jarviscollector.ui.facts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FactInsightEntity

@Composable
fun FactCard(
    fact: FactInsightEntity,
    onToggleRead: () -> Unit,
    onViewEvidence: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isRead = fact.read_flag == true
    val priority = fact.priority?.uppercase() ?: "MEDIUM"
    val priorityColor = when (priority) {
        "HIGH" -> Color(0xFFEF4444)
        "LOW" -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isRead) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.05f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleRead() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$priority PRIORITY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Unread Indicator dot
                if (!isRead) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = fact.title ?: "Important Memory",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
            )

            if (!fact.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = fact.summary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category or Source tag
                Text(
                    text = listOfNotNull(
                        fact.category?.takeIf { it.isNotBlank() },
                        fact.source?.takeIf { it.isNotBlank() }
                    ).joinToString(" • ").uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onViewEvidence != null) {
                        TextButton(
                            onClick = onViewEvidence,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("View Evidence", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (!fact.created_at.isNullOrBlank()) {
                        Text(
                            text = fact.created_at,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
