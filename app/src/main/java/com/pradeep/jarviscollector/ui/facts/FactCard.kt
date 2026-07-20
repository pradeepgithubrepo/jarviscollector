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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FactCard(
    fact: FactInsightEntity,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    onViewEvidence: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 1. Safe JSON parsing of raw_payload in source field
    val payloadJson = fact.source ?: ""
    var senderName = "SYSTEM"
    var requiresAction = false
    var fyiCandidate = false
    var memoryCandidate = false
    var financialCandidate = false

    try {
        if (payloadJson.isNotBlank() && payloadJson.trim().startsWith("{")) {
            val root = JSONObject(payloadJson)
            senderName = root.optString("sender", "SYSTEM").uppercase(Locale.US)
            val contract = root.optJSONObject("contract")
            if (contract != null) {
                requiresAction = contract.optBoolean("requires_action", false)
                fyiCandidate = contract.optBoolean("fyi_candidate", false)
                memoryCandidate = contract.optBoolean("memory_candidate", false)
                financialCandidate = contract.optBoolean("financial_candidate", false)
            }
        }
    } catch (e: Exception) {
        // Fallback
    }

    val isRead = fact.read_flag == true
    val formattedDate = rememberFormattedDateInCard(fact.created_at)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "FROM: $senderName",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Processing Path badge (stored in status)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Text(
                        text = (fact.status ?: "RULE_BASED").uppercase(Locale.US),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text (summary)
            Text(
                text = fact.summary ?: "Empty signal text",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 20.sp
            )

            // AI Candidates Badges Section
            val hasCandidates = requiresAction || fyiCandidate || memoryCandidate || financialCandidate
            if (hasCandidates) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (requiresAction) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.12f)
                        ) {
                            Text(
                                "TASK CANDIDATE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (fyiCandidate) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.12f)
                        ) {
                            Text(
                                "FYI CANDIDATE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (financialCandidate) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f)
                        ) {
                            Text(
                                "FINANCE CANDIDATE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (memoryCandidate) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f)
                        ) {
                            Text(
                                "MEMORY CANDIDATE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Received: $formattedDate",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )

                if (onViewEvidence != null) {
                    TextButton(
                        onClick = { onViewEvidence() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Trace Details", fontSize = 11.sp, color = Color(0xFF818CF8))
                    }
                }
            }
        }
    }
}

@Composable
fun rememberFormattedDateInCard(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return "Unknown Date"
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSZ", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )
        var parsedDate: Date? = null
        for (f in formats) {
            try {
                parsedDate = f.parse(rawDate)
                if (parsedDate != null) break
            } catch (e: Exception) {}
        }
        if (parsedDate != null) {
            SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US).format(parsedDate)
        } else {
            rawDate
        }
    } catch (e: Exception) {
        rawDate
    }
}
