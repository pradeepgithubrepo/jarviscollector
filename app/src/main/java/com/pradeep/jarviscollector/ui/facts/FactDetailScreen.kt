package com.pradeep.jarviscollector.ui.facts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FactInsightEntity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactDetailScreen(
    factId: String,
    facts: List<FactInsightEntity>,
    onConvertTodo: (String, String, String, Long?) -> Unit,
    onDismissFact: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fact = facts.find { it.id == factId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signal Diagnostics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF070B14))
                )
            )
    ) { innerPadding ->
        if (fact == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Signal not found.", color = Color(0xFF94A3B8), fontSize = 16.sp)
            }
        } else {
            // 1. JSON parsing of the raw payload
            val payloadJson = fact.source ?: ""
            var senderName = "SYSTEM"
            var rawMessage = fact.summary ?: ""
            var requiresAction = false
            var fyiCandidate = false
            var memoryCandidate = false
            var financialCandidate = false
            var formattedJson = "{}"

            try {
                if (payloadJson.isNotBlank() && payloadJson.trim().startsWith("{")) {
                    val root = JSONObject(payloadJson)
                    senderName = root.optString("sender", "SYSTEM")
                    rawMessage = root.optString("raw_message", fact.summary ?: "")
                    formattedJson = root.toString(2)
                    
                    val contract = root.optJSONObject("contract")
                    if (contract != null) {
                        requiresAction = contract.optBoolean("requires_action", false)
                        fyiCandidate = contract.optBoolean("fyi_candidate", false)
                        memoryCandidate = contract.optBoolean("memory_candidate", false)
                        financialCandidate = contract.optBoolean("financial_candidate", false)
                    }
                }
            } catch (e: Exception) {
                // Formatting fallback
                formattedJson = payloadJson
            }

            val formattedDate = rememberFormattedDate(fact.created_at)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Details Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SENDER: $senderName".uppercase(Locale.US),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8),
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.06f)
                            ) {
                                Text(
                                    text = (fact.status ?: "RULE_BASED").uppercase(Locale.US),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = rawMessage,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 22.sp
                        )

                        Text(
                            text = "Processed At: $formattedDate",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // AI Classification Checks Checklist
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AI INTENT CLASSIFICATIONS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        ClassificationRow(label = "Task Candidate (Requires Action)", checked = requiresAction, accentColor = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.height(6.dp))
                        ClassificationRow(label = "FYI Announcement Candidate", checked = fyiCandidate, accentColor = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(6.dp))
                        ClassificationRow(label = "Financial Transaction Candidate", checked = financialCandidate, accentColor = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(6.dp))
                        ClassificationRow(label = "Memory/Recall Candidate", checked = memoryCandidate, accentColor = Color(0xFFF59E0B))
                    }
                }

                // Monospace JSON Visualizer
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "RAW PAYLOAD CONTRACT (JSON)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp)
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = formattedJson,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF34D399),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Action Buttons Row (Convert / Dismiss)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Button(
                        onClick = {
                            val cleanTitle = if (fact.title?.contains("expires", ignoreCase = true) == true ||
                                fact.title?.contains("due", ignoreCase = true) == true) {
                                fact.title
                            } else {
                                "Action required: $rawMessage"
                            }

                            onConvertTodo(
                                cleanTitle,
                                "Ref: Signal Route id: ${fact.id}\nSource Sender: $senderName\nMessage: $rawMessage",
                                "MEDIUM",
                                null
                            )
                        },
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Convert to ToDo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { onDismissFact(fact.id) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Dismiss Signal", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ClassificationRow(
    label: String,
    checked: Boolean,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFCBD5E1)
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (checked) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
        ) {
            Text(
                text = if (checked) "YES" else "NO",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (checked) accentColor else Color(0xFF475569),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun rememberFormattedDate(rawDate: String?): String {
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
