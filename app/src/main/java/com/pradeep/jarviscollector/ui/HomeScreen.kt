package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun HomeScreen(
    ownerName: String,
    pendingTodoCount: Int,
    unpaidBillCount: Int,
    newFyiCount: Int,
    familyCount: Int,
    schoolCount: Int,
    travelCount: Int,
    healthCount: Int,
    shoppingCount: Int,
    briefDate: String?,
    onNavigateToTodos: () -> Unit,
    onNavigateToFinancial: () -> Unit,
    onNavigateToFyi: () -> Unit,
    onNavigateToDailyBrief: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToSchool: () -> Unit,
    onNavigateToTravel: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToCollectorSettings: () -> Unit,
    onOwnerNameChange: (String) -> Unit,
    onLoadInsights: () -> Unit, // New callback for manual load
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val formattedOwner = ownerName.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Premium personalized greeting header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Good Morning,",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = formattedOwner,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }

            // User Selection Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (ownerName == "pradeep") MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.height(32.dp)
                ) {
                    TextButton(
                        onClick = { onOwnerNameChange("pradeep") },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "P",
                            fontWeight = FontWeight.Bold,
                            color = if (ownerName == "pradeep") Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (ownerName == "shobana") MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.height(32.dp)
                ) {
                    TextButton(
                        onClick = { onOwnerNameChange("shobana") },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "S",
                            fontWeight = FontWeight.Bold,
                            color = if (ownerName == "shobana") Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Summary banner card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "TODAY'S SUMMARY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val summaryText = buildString {
                    append("Hello $formattedOwner. ")
                    if (pendingTodoCount > 0) {
                        append("You have $pendingTodoCount pending tasks to complete. ")
                    } else {
                        append("All todos are cleared for today! ")
                    }
                    if (unpaidBillCount > 0) {
                        append("There are $unpaidBillCount important bills or financial actions. ")
                    }
                    if (newFyiCount > 0) {
                        append("You have $newFyiCount new circulars and updates.")
                    }
                }
                
                Text(
                    text = summaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SYSTEM AGENTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { onLoadInsights() }) {
            Text("Load Insights")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 1: TODOS & FINANCIAL
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            AgentTile(
                agent = AgentInfo(
                    id = "todos",
                    name = "Todos",
                    statusText = if (pendingTodoCount > 0) "$pendingTodoCount Pending" else "No Pending Tasks",
                    badgeCount = pendingTodoCount,
                    accentColor = Color(0xFF8B5CF6) // Violet
                ),
                onClick = onNavigateToTodos,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            AgentTile(
                agent = AgentInfo(
                    id = "financial",
                    name = "Financial",
                    statusText = if (unpaidBillCount > 0) "$unpaidBillCount Actions" else "No Due Payments",
                    badgeCount = unpaidBillCount,
                    accentColor = Color(0xFF10B981) // Teal
                ),
                onClick = onNavigateToFinancial,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 2: FYI & DAILY BRIEF
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            AgentTile(
                agent = AgentInfo(
                    id = "fyi",
                    name = "Fyi",
                    statusText = if (newFyiCount > 0) "$newFyiCount Updates" else "No New Updates",
                    badgeCount = newFyiCount,
                    accentColor = Color(0xFF06B6D4) // Cyan
                ),
                onClick = onNavigateToFyi,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            AgentTile(
                agent = AgentInfo(
                    id = "daily_brief",
                    name = "Daily Brief",
                    statusText = if (briefDate != null) "Latest generated" else "No Briefs Synced",
                    accentColor = Color(0xFF3B82F6) // Blue
                ),
                onClick = onNavigateToDailyBrief,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 3: FAMILY & SCHOOL
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            AgentTile(
                agent = AgentInfo(
                    id = "family",
                    name = "Family",
                    statusText = if (familyCount > 0) "$familyCount Updates" else "No Family Updates",
                    badgeCount = familyCount,
                    accentColor = Color(0xFFEC4899) // Pink
                ),
                onClick = onNavigateToFamily,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            AgentTile(
                agent = AgentInfo(
                    id = "school",
                    name = "School",
                    statusText = if (schoolCount > 0) "$schoolCount Circulars" else "No School Circulars",
                    badgeCount = schoolCount,
                    accentColor = Color(0xFF3B82F6) // Blue
                ),
                onClick = onNavigateToSchool,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 4: TRAVEL & HEALTH
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            AgentTile(
                agent = AgentInfo(
                    id = "travel",
                    name = "Travel",
                    statusText = if (travelCount > 0) "$travelCount Logs" else "No Travel Logs",
                    badgeCount = travelCount,
                    accentColor = Color(0xFF0D9488) // Teal-Green accent
                ),
                onClick = onNavigateToTravel,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            AgentTile(
                agent = AgentInfo(
                    id = "health",
                    name = "Health",
                    statusText = if (healthCount > 0) "$healthCount Alerts" else "No Health Alerts",
                    badgeCount = healthCount,
                    accentColor = Color(0xFFEF4444) // Red accent
                ),
                onClick = onNavigateToHealth,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 5: SHOPPING & PLACEHOLDER (Locked)
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            AgentTile(
                agent = AgentInfo(
                    id = "shopping",
                    name = "Shopping",
                    statusText = if (shoppingCount > 0) "$shoppingCount Alerts" else "No Shopping Alerts",
                    badgeCount = shoppingCount,
                    accentColor = Color(0xFFF59E0B) // Amber accent
                ),
                onClick = onNavigateToShopping,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            AgentTile(
                agent = AgentInfo(
                    id = "leisure",
                    name = "Leisure",
                    statusText = "Locked",
                    accentColor = Color(0xFF6B7280),
                    isAvailable = false
                ),
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Banner for Collector Settings / Sync Panel
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .clickable { onNavigateToCollectorSettings() }
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COLLECTOR STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444), // Crimson
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SMS Ingestion & Signals Sync",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Manage background collection tasks",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Text(
                        text = "MANAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}
