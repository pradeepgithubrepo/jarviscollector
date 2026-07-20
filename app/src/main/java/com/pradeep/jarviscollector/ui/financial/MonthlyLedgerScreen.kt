package com.pradeep.jarviscollector.ui.financial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.repository.FinancialRepository
import java.util.Locale

private val BackgroundDeep = Color(0xFF070B16)
private val SurfaceGlass = Color(0xFF13192F)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)
private val GlassBorder = Color.White.copy(alpha = 0.06f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyLedgerScreen(
    monthKey: String,
    category: String,
    onBack: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit
) {
    val context = LocalContext.current
    var allTransactions by remember { mutableStateOf<List<FinancialEventEntity>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(monthKey, category) {
        isLoading = true
        allTransactions = if (category == "all") {
            FinancialRepository.getTransactionsByMonthSortedByMaxSpend(context, monthKey)
        } else {
            FinancialRepository.getTransactionsByMonthAndCategory(context, monthKey, category)
        }
        isLoading = false
    }

    // Filter by search query (raw narration or merchant or subcategory)
    val filteredTransactions = remember(allTransactions, searchQuery) {
        if (searchQuery.isBlank()) {
            allTransactions
        } else {
            allTransactions.filter {
                (it.merchant ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.paid_to ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.subcategory ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Sum of listed expenses (excluding transfers & income)
    val totalExpenseSum = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.transaction_type?.uppercase(Locale.US) == "DEBIT" && it.is_self_transfer != true }
            .sumOf { it.amount ?: 0.0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (category == "all") "Monthly Ledger" else "Category: $category",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = monthKey,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDeep)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDeep)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceGlass,
                    unfocusedContainerColor = SurfaceGlass
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Header metric card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (category == "all") "Total Spent (Excl. Transfers)" else "Category Spend",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%,.2f", totalExpenseSum)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${filteredTransactions.size} transactions listed",
                        fontSize = 11.sp,
                        color = AccentIndigo
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentIndigo)
                }
            } else if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTransactions) { tx ->
                        val isDebit = tx.transaction_type?.uppercase(Locale.US) == "DEBIT"
                        val isTransfer = tx.is_self_transfer == true
                        val amountColor = when {
                            isTransfer -> TextSecondary
                            isDebit -> AccentRed
                            else -> AccentGreen
                        }

                        val directionSymbol = when {
                            isTransfer -> "⇄"
                            isDebit -> "-"
                            else -> "+"
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToTransactionDetail(tx.financial_event_id) }
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.paid_to ?: tx.merchant ?: "Unknown Payee",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = tx.event_timestamp ?: "",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = tx.paid_from ?: "",
                                            fontSize = 11.sp,
                                            color = AccentIndigo,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isTransfer) {
                                            Text(
                                                text = "Transfer",
                                                fontSize = 10.sp,
                                                color = TextSecondary,
                                                modifier = Modifier
                                                    .background(GlassBorder, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        tx.subcategory?.let { sub ->
                                            Text(
                                                text = sub,
                                                fontSize = 10.sp,
                                                color = AccentGreen,
                                                modifier = Modifier
                                                    .background(GlassBorder, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "$directionSymbol ₹${String.format("%,.2f", tx.amount ?: 0.0)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = amountColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
