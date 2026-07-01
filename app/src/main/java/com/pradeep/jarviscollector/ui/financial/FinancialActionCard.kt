package com.pradeep.jarviscollector.ui.financial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FinancialInsightEntity
import kotlin.math.abs

@Composable
fun FinancialActionCard(
    insight: FinancialInsightEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onCorrect: () -> Unit,
    onViewEvidence: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priority = insight.priority?.uppercase() ?: "MEDIUM"
    val priorityColor = when (priority) {
        "HIGH" -> Color(0xFFEF4444)
        "LOW" -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                TextButton(
                    onClick = onViewEvidence,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View Evidence", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = insight.title ?: "Financial Action Alert",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!insight.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (insight.amount != null) {
                        Text(
                            text = "₹${String.format("%.2f", abs(insight.amount))}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                    if (!insight.dueDate.isNullOrBlank()) {
                        Text(
                            text = "Due: ${insight.dueDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onCorrect) {
                        Text("Correct")
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
