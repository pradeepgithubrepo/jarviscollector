package com.pradeep.jarviscollector.ui.debug

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.*
import com.pradeep.jarviscollector.network.QueryInstrumentation
import com.pradeep.jarviscollector.service.InsightSyncService
import com.pradeep.jarviscollector.service.InsightSyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDataPipelineScreen(
    todos: List<TodoEntity>,
    facts: List<FactInsightEntity>,
    notifications: List<NotificationEntity>,
    financialInsights: List<FinancialInsightEntity>,
    fyiEvents: List<FyiEventEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Database counts loaded directly from Room
    var roomTodoCount by remember { mutableStateOf(-1) }
    var roomFactCount by remember { mutableStateOf(-1) }
    var roomNotifCount by remember { mutableStateOf(-1) }
    var roomFinCount by remember { mutableStateOf(-1) }
    var roomFyiCount by remember { mutableStateOf(-1) }
    var roomFamilyCount by remember { mutableStateOf(-1) }

    // Direct Room query bypass results
    var directFactsResult by remember { mutableStateOf<List<FactInsightEntity>>(emptyList()) }

    // Direct Room records explorer
    var explorerTodos by remember { mutableStateOf<List<TodoEntity>>(emptyList()) }
    var explorerFacts by remember { mutableStateOf<List<FactInsightEntity>>(emptyList()) }
    var explorerNotifs by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    var explorerFins by remember { mutableStateOf<List<FinancialInsightEntity>>(emptyList()) }
    var explorerFamily by remember { mutableStateOf<List<FyiEventEntity>>(emptyList()) }

    // First JSON Record Inspection States
    var inspectFactsJson by remember { mutableStateOf("Loading...") }
    var inspectTodosJson by remember { mutableStateOf("Loading...") }
    var inspectFyiJson by remember { mutableStateOf("Loading...") }
    var inspectBriefsJson by remember { mutableStateOf("Loading...") }
    var inspectFinEventsJson by remember { mutableStateOf("Loading...") }
    var inspectFinFactsJson by remember { mutableStateOf("Loading...") }
    var inspectPrefsJson by remember { mutableStateOf("Loading...") }

    // Sync Diagnostics Logs
    var diagnosticsLogs by remember { mutableStateOf<List<SyncDiagnosticsEntity>>(emptyList()) }

    // Manual sync execution status
    var isSyncing by remember { mutableStateOf(false) }
    var syncResultText by remember { mutableStateOf<String?>(null) }

    // Supabase reachability (check if any query was successful)
    val supabaseReachable = remember(QueryInstrumentation.records.size) {
        QueryInstrumentation.records.any { it.success }
    }

    fun loadRawJSONInspect() {
        scope.launch(Dispatchers.IO) {
            fun fetchFirstRow(t: String): String {
                val raw = com.pradeep.jarviscollector.network.JarvisInsightsClient.fetchTable(t)
                return try {
                    if (raw == null) "Failed / Empty"
                    else {
                        val arr = JSONArray(raw)
                        if (arr.length() > 0) arr.getJSONObject(0).toString(2)
                        else "Empty array []"
                    }
                } catch (e: Exception) {
                    "Error parsing: ${e.message}\nRaw: $raw"
                }
            }

            inspectFactsJson = fetchFirstRow("facts")
            inspectTodosJson = fetchFirstRow("todo_items")
            inspectFyiJson = fetchFirstRow("fyi_events")
            inspectBriefsJson = fetchFirstRow("daily_briefs")
            inspectFinEventsJson = fetchFirstRow("financial_events")
            inspectFinFactsJson = fetchFirstRow("financial_facts")
            inspectPrefsJson = fetchFirstRow("user_preferences")
        }
    }

    fun loadDirectDatabaseMetrics() {
        scope.launch(Dispatchers.IO) {
            val db = JarvisDatabase.getDatabase(context)
            
            val directTodos = db.todoDao().getAll()
            val directFacts = db.factInsightDao().getAll()
            val directNotifs = db.notificationDao().getAll()
            val directFyi = db.fyiEventDao().getAll()
            val directFins = db.financialInsightDao().getAll()
            
            roomTodoCount = directTodos.size
            roomFactCount = directFacts.size
            roomNotifCount = directNotifs.size
            roomFyiCount = directFyi.size
            roomFamilyCount = directFyi.count { it.category?.lowercase() == "family" }
            roomFinCount = directFins.size
            
            // Set Direct Compose OK check
            directFactsResult = directFacts.take(5)

            // Set Explorer list values
            explorerTodos = directTodos.take(5)
            explorerFacts = directFacts.take(5)
            explorerNotifs = directNotifs.take(5)
            explorerFins = directFins.take(5)
            explorerFamily = directFyi.filter { it.category?.lowercase() == "family" }.take(5)

            // Diagnostics Logs
            diagnosticsLogs = db.syncDiagnosticsDao().getAll()
        }
    }

    LaunchedEffect(Unit) {
        loadDirectDatabaseMetrics()
        loadRawJSONInspect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Debug Data Pipeline", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {
                    loadDirectDatabaseMetrics()
                    loadRawJSONInspect()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Counts")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection Health Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PIPELINE HEALTH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Supabase Reachable")
                            Text(
                                text = if (supabaseReachable) "YES" else "NO (No Success Logs Yet)",
                                color = if (supabaseReachable) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Manual Sync Button
            item {
                Button(
                    onClick = {
                        isSyncing = true
                        syncResultText = "Syncing..."
                        scope.launch {
                            val res = InsightSyncService.syncInsights(context)
                            isSyncing = false
                            syncResultText = when (res) {
                                is InsightSyncResult.Success -> "SUCCESS: Downloaded ${res.todoCount} todos, ${res.factCount} facts."
                                is InsightSyncResult.Failure -> "FAILED: ${res.error}"
                            }
                            loadDirectDatabaseMetrics()
                            loadRawJSONInspect()
                        }
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSyncing) "Running Sync..." else "Run Sync Now")
                }
                syncResultText?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // 3. First Record JSON Inspection (Evidence Gathering)
            item {
                Text("FIRST RECORD INSPECTION", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val inspectItems = listOf(
                            "facts" to inspectFactsJson,
                            "todo_items" to inspectTodosJson,
                            "fyi_events" to inspectFyiJson,
                            "daily_briefs" to inspectBriefsJson,
                            "financial_events" to inspectFinEventsJson,
                            "financial_facts" to inspectFinFactsJson,
                            "user_preferences" to inspectPrefsJson
                        )
                        inspectItems.forEach { (title, json) ->
                            Text("Endpoint: $title", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = json,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Diagnostics Sync Logs
            item {
                Text("DOWNLOADED VS INSERTED COUNTS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (diagnosticsLogs.isEmpty()) {
                item { Text("No sync diagnostics recorded yet. Run a sync.", fontSize = 12.sp) }
            } else {
                items(diagnosticsLogs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(log.entityType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Status: ${log.status}", fontSize = 12.sp, color = if (log.status == "SUCCESS") Color(0xFF10B981) else Color(0xFFEF4444))
                            Text("Downloaded: ${log.recordsDownloaded} | Inserted: ${log.recordsInserted}", fontSize = 12.sp)
                            if (log.errorMessage != null) {
                                Text("Error: ${log.errorMessage}", fontSize = 11.sp, color = Color(0xFFEF4444), fontFamily = FontFamily.Monospace)
                            }
                            Text("Started: ${log.syncStartedAt ?: "N/A"} | Finished: ${log.syncCompletedAt ?: "N/A"}", fontSize = 10.sp)
                        }
                    }
                }
            }

            // 5. Memory Query Network Log Tracker (Telemetry)
            item {
                Text("QUERY TELEMETRY (Supabase Queries)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            val queryHistory = QueryInstrumentation.records.toList()
            if (queryHistory.isEmpty()) {
                item { Text("No queries captured yet.", fontSize = 12.sp) }
            } else {
                items(queryHistory.reversed()) { q ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Request: [${q.method}] ${q.url}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Rows Returned: ${q.rowCount}", fontSize = 11.sp)
                            Text("Status: ${if (q.success) "SUCCESS" else "FAILURE"}", fontSize = 11.sp, color = if (q.success) Color(0xFF10B981) else Color(0xFFEF4444))
                            if (q.error != null) {
                                Text("Failure Reason: ${q.error}", fontSize = 10.sp, color = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }

            // 6. Room vs ViewModel Counts
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DATA COUNTS PIPELINE COMPARE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entity", fontWeight = FontWeight.Bold)
                            Text("Room DB (DAO)", fontWeight = FontWeight.Bold)
                            Text("ViewModel (Flow)", fontWeight = FontWeight.Bold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Facts")
                            Text("$roomFactCount")
                            Text("${facts.size}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Todos")
                            Text("$roomTodoCount")
                            Text("${todos.size}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FYI")
                            Text("$roomFyiCount")
                            Text("${fyiEvents.size}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Notifications")
                            Text("$roomNotifCount")
                            Text("${notifications.size}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Financial Insights")
                            Text("$roomFinCount")
                            Text("${financialInsights.size}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Family FYI")
                            Text("$roomFamilyCount")
                            Text("-")
                        }
                    }
                }
            }

            // 7. Direct Facts Bypass Card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DIRECT COMPOSABLE QUERY (Facts Test)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (directFactsResult.isEmpty()) {
                            Text("Direct DAO call returned 0 facts.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            directFactsResult.forEach { f ->
                                Text("• ${f.title}: ${f.summary}", fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // 8. Database Explorer Card
            item {
                Text("RAW DATABASE EXPLORER", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top 5 Raw Todos:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        if (explorerTodos.isEmpty()) Text("Empty", fontSize = 11.sp)
                        explorerTodos.forEach {
                            Text("ID: ${it.todo_id} | Title: ${it.title} | Status: ${it.status}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Top 5 Raw FYIs:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        if (fyiEvents.isEmpty()) Text("Empty", fontSize = 11.sp)
                        fyiEvents.take(5).forEach {
                            Text("ID: ${it.fyi_event_id} | Title: ${it.title} | Read: ${it.read_flag}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Top 5 Raw Notifications:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        if (explorerNotifs.isEmpty()) Text("Empty", fontSize = 11.sp)
                        explorerNotifs.forEach {
                            Text("ID: ${it.id} | Title: ${it.title} | Priority: ${it.priority}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Top 5 Raw Financial Insights:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        if (explorerFins.isEmpty()) Text("Empty", fontSize = 11.sp)
                        explorerFins.forEach {
                            Text("ID: ${it.id} | Title: ${it.title} | Type: ${it.type} | Amt: ${it.amount}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Top 5 Raw Family FYIs:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        if (explorerFamily.isEmpty()) Text("Empty", fontSize = 11.sp)
                        explorerFamily.forEach {
                            Text("ID: ${it.fyi_event_id} | Title: ${it.title} | Category: ${it.category}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
