package com.pradeep.jarviscollector.ui.facts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FactInsightEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactsScreen(
    facts: List<FactInsightEntity>,
    onToggleRead: (String, Boolean) -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOnlyUnread by remember { mutableStateOf(false) }

    val filteredFacts = if (showOnlyUnread) {
        facts.filter { it.read_flag != true }
    } else {
        facts
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Durable Memory",
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
            actions = {
                FilterChip(
                    selected = showOnlyUnread,
                    onClick = { showOnlyUnread = !showOnlyUnread },
                    label = { Text("Unread Only", fontSize = 12.sp) },
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (filteredFacts.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Text(
                    text = if (showOnlyUnread) "No unread memories." else "No memories recorded yet.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFacts, key = { it.id }) { fact ->
                    FactCard(
                        fact = fact,
                        onToggleRead = {
                            val newReadStatus = fact.read_flag != true
                            onToggleRead(fact.id, newReadStatus)
                        },
                        onViewEvidence = {
                            onNavigateToSignalExplorer("fact", fact.id)
                        }
                    )
                }
            }
        }
    }
}
