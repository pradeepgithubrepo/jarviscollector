package com.pradeep.jarviscollector.ui.facts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FactInsightEntity
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactsScreen(
    facts: List<FactInsightEntity>,
    onNavigateToFactDetail: (String) -> Unit,
    onToggleRead: (String, Boolean) -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    onSwipeSoftDelete: (String) -> Unit,
    onSwipeHardDelete: (String, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember(facts) {
        listOf("ALL") + facts.mapNotNull { it.category?.uppercase(Locale.US) }.distinct().sorted()
    }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredFacts = remember(facts, selectedCategory) {
        val list = if (selectedCategory == "ALL") {
            facts
        } else {
            facts.filter { it.category?.uppercase(Locale.US) == selectedCategory }
        }
        list.sortedByDescending { it.created_at }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Signal Feed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${facts.size} signals processed",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF070B14))
                )
            )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(alpha = 0.05f),
                            selectedBorderColor = Color(0xFF6366F1),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
            }

            if (filteredFacts.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(
                        text = "No signals found in this category.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFacts, key = { it.id }) { fact ->
                        SwipeableCard(
                            onSwipeRight = {
                                onSwipeSoftDelete(fact.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Signal dismissed locally")
                                }
                            },
                            onSwipeLeft = {
                                onSwipeHardDelete(fact.id) { success ->
                                    scope.launch {
                                        if (success) {
                                            snackbarHostState.showSnackbar("Signal deleted from server")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to delete signal from server")
                                        }
                                    }
                                }
                            }
                        ) {
                            FactCard(
                                fact = fact,
                                onClick = { onNavigateToFactDetail(fact.id) },
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
    }
}

@Composable
fun SwipeableCard(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset = animateFloatAsState(targetValue = offsetX)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 200f) {
                            onSwipeRight()
                        } else if (offsetX < -200f) {
                            onSwipeLeft()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        if (offsetX != 0f) {
            val color = if (offsetX > 0) Color(0xFF10B981) else Color(0xFFEF4444)
            val alignment = if (offsetX > 0) Alignment.CenterStart else Alignment.CenterEnd
            val labelText = if (offsetX > 0) "DISMISS LOCALLY" else "DELETE FROM SERVER"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Text(
                    text = labelText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.value.toInt(), 0) }
                .background(Color.Transparent)
        ) {
            content()
        }
    }
}
