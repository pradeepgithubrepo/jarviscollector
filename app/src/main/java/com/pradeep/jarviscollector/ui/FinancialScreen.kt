package com.pradeep.jarviscollector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.FinancialInsightEntity
import com.pradeep.jarviscollector.ui.financial.FinancialDashboardViewModel
import com.pradeep.jarviscollector.ui.financial.CategorySummary
import com.pradeep.jarviscollector.ui.financial.TransactionSummary

// Premium design tokens
private val BackgroundDeep = Color(0xFF070B16)
private val SurfaceGlass = Color(0xFF13192F)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val AccentAmber = Color(0xFFF59E0B)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)
private val GlassBorder = Color.White.copy(alpha = 0.06f)

// Standard category colors palette for segmented bar & cards
private val CATEGORY_COLORS = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF8B5CF6), // Violet
    Color(0xFF10B981), // Emerald Green
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Rose Red
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEC4899), // Pink
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Orange
    Color(0xFF84CC16)  // Lime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    snapshotInsights: List<FinancialInsightEntity>,
    actionRequired: List<FinancialInsightEntity>,
    subscriptions: List<FinancialInsightEntity>,
    upcomingBills: List<FinancialInsightEntity>,
    unusualActivity: List<FinancialInsightEntity>,
    onConfirmInsight: (String) -> Unit,
    onDismissInsight: (String) -> Unit,
    onCorrectInsight: (String, String, Double) -> Unit,
    onNavigateToSignalExplorer: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinancialDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToTransactionDetail: (String) -> Unit = {},
    onNavigateToMonthlyLedger: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun formatMoney(value: Double): String {
        return "₹${String.format("%,.0f", value)}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Spending Journal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier.fillMaxSize().background(BackgroundDeep)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDeep)
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentIndigo)
                    }
                }
                uiState.isError -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Unable to load financial journal data.",
                            color = AccentRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section 1: Swiper Month Header & Headline Spend
                        item {
                            MonthSpendingHeader(
                                monthName = uiState.displayMonthName,
                                totalExpense = uiState.totalExpenses,
                                momChange = uiState.totalExpenseMoMChange,
                                onPrevMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                formatMoney = ::formatMoney
                            )
                        }

                        // Section 1.5: Action Buttons (View Ledger & Retrigger Recalculation)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToMonthlyLedger(uiState.selectedMonthKey, "all") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlass),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Text("View Ledger", color = TextPrimary, fontWeight = FontWeight.Bold)
                                }

                                var isTriggering by remember { mutableStateOf(false) }
                                Button(
                                    onClick = {
                                        if (!isTriggering) {
                                            isTriggering = true
                                            scope.launch {
                                                val success = com.pradeep.jarviscollector.repository.FinancialRepository.retriggerPipeline(context)
                                                isTriggering = false
                                                if (success) {
                                                    android.widget.Toast.makeText(context, "Aggregate recomputation triggered", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Retrigger failed", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isTriggering) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary)
                                    } else {
                                        Text("Recompute", color = TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Section 2: Segmented Category Share Bar
                        if (uiState.categoryBreakdown.isNotEmpty()) {
                            item {
                                CategoryShareBar(
                                    categories = uiState.categoryBreakdown,
                                    totalExpense = uiState.totalExpenses
                                )
                            }
                        }

                        // Section 3: Spending Insights
                        if (uiState.highestSpendCategory != null || uiState.topSpendingIncreaseCategory != null) {
                            item {
                                SpendingInsightsCard(
                                    highestSpend = uiState.highestSpendCategory,
                                    topIncrease = uiState.topSpendingIncreaseCategory
                                )
                            }
                        }

                        // Section 4: Spending Category List with MoM delta & Till Date comparison
                        if (uiState.categoryBreakdown.isNotEmpty()) {
                            item {
                                SectionTitle("Category Spending Profile")
                            }
                            items(uiState.categoryBreakdown) { cat ->
                                val colorIndex = Math.abs(cat.category.hashCode()) % CATEGORY_COLORS.size
                                val categoryColor = CATEGORY_COLORS[colorIndex]
                                CategorySpendItem(
                                    cat = cat,
                                    color = categoryColor,
                                    formatMoney = ::formatMoney,
                                    onClick = { onNavigateToMonthlyLedger(uiState.selectedMonthKey, cat.category) }
                                )
                            }
                        }

                        // Section 5: Monthly Transactions Ledger
                        item {
                            SectionTitle("Journal Entries")
                        }
                        if (uiState.recentTransactions.isNotEmpty()) {
                            items(uiState.recentTransactions) { tx ->
                                TransactionRow(
                                    tx = tx,
                                    onClick = { onNavigateToTransactionDetail(tx.id) }
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No entries recorded in this month.", color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSpendingHeader(
    monthName: String,
    totalExpense: Double,
    momChange: Double?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    formatMoney: (Double) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal month swiper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = TextPrimary)
            }
            Text(
                text = monthName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Spent Headline
        Text(
            text = "TOTAL SPENT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatMoney(totalExpense),
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        // MoM Change deltas
        if (momChange != null) {
            val isIncrease = momChange > 0.0
            val color = if (isIncrease) AccentRed else AccentGreen
            val indicator = if (isIncrease) "▲" else "▼"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "$indicator ${String.format("%.1f", Math.abs(momChange))}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "vs last month",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CategoryShareBar(
    categories: List<CategorySummary>,
    totalExpense: Double
) {
    if (totalExpense <= 0.0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Horizontal Segmented Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(GlassBorder)
        ) {
            categories.forEach { cat ->
                val weight = (cat.amount / totalExpense).toFloat()
                if (weight > 0.01f) {
                    val colorIndex = Math.abs(cat.category.hashCode()) % CATEGORY_COLORS.size
                    val segmentColor = CATEGORY_COLORS[colorIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .background(segmentColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingInsightsCard(
    highestSpend: String?,
    topIncrease: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceGlass)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "SPENDING INSIGHTS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AccentIndigo,
                letterSpacing = 1.sp
            )

            if (highestSpend != null) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("💡 ", fontSize = 14.sp)
                    Column {
                        Text(
                            text = "Highest Category Spend",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = highestSpend,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (topIncrease != null) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("📈 ", fontSize = 14.sp)
                    Column {
                        Text(
                            text = "Largest MoM Increase",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = topIncrease,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySpendItem(
    cat: CategorySummary,
    color: Color,
    formatMoney: (Double) -> String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceGlass)
            .clickable(onClick = onClick)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator pill
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Category detail label
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cat.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${cat.transactionCount} transactions",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Amounts and delta badge
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatMoney(cat.amount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    // Delta badge
                    if (cat.momChangePercentage != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isIncrease = cat.momChangePercentage > 0.0
                        val deltaColor = if (isIncrease) AccentRed else AccentGreen
                        val arrow = if (isIncrease) "▲" else "▼"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(deltaColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$arrow ${String.format("%.0f", Math.abs(cat.momChangePercentage))}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = deltaColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Till Date: ${formatMoney(cat.cumulativeAmount)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Credit/Debit Arrow Badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (tx.isCredit) AccentGreen.copy(0.08f) else AccentIndigo.copy(0.08f)
                )
                .border(
                    1.dp,
                    if (tx.isCredit) AccentGreen.copy(0.2f) else AccentIndigo.copy(0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (tx.isCredit) "↑" else "↓",
                fontSize = 14.sp,
                color = if (tx.isCredit) AccentGreen else AccentIndigo,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Transaction display details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tx.category, fontSize = 11.sp, color = AccentIndigo)
                if (!tx.paymentChannel.isNullOrBlank()) {
                    Text("·", fontSize = 11.sp, color = TextSecondary)
                    Text(tx.paymentChannel, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Amount & Date
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (tx.isCredit) "+" else "-") + "₹${String.format("%,.0f", tx.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (tx.isCredit) AccentGreen else TextPrimary
            )
            Text(tx.date, fontSize = 10.sp, color = TextSecondary)
        }
    }

    // Divider
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = GlassBorder,
        thickness = 0.5.dp
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        letterSpacing = 1.2.sp
    )
}
