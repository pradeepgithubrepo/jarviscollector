package com.pradeep.jarviscollector.ui.financial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgDeep = Color(0xFF0A0F1E)
private val Surface = Color(0xFF1A1F3A)
private val AccentIndigo = Color(0xFF6366F1)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)
private val GlassBorder = Color.White.copy(alpha = 0.08f)

private val STANDARD_CATEGORIES = listOf(
    "FOOD", "TRANSPORT", "SHOPPING", "BILLS", "HEALTH",
    "ENTERTAINMENT", "INVESTMENT", "SALARY", "TRANSFER",
    "INSURANCE", "EDUCATION", "TRAVEL", "OTHER"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPurgeDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    // Editorial states
    var editableSubcategory by remember { mutableStateOf("") }
    var isSelfTransferChecked by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    // Sync editing states with loaded transaction details
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            editableSubcategory = uiState.subcategory ?: ""
            isSelfTransferChecked = uiState.isSelfTransfer
            selectedCategory = uiState.category
        }
    }

    LaunchedEffect(uiState.operationMessage) {
        val msg = uiState.operationMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearOperationMessage()
        }
    }

    if (showPurgeDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeDialog = false },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Delete Transaction", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "This will permanently delete \"${uiState.displayName}\" from Supabase. " +
                    "Your monthly summaries will be refreshed automatically.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeDialog = false
                        viewModel.purgeTransaction { onBack() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Change Category", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    STANDARD_CATEGORIES.forEach { cat ->
                        val isSelected = cat.equals(selectedCategory.replace(" ", "_").uppercase(), ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentIndigo.copy(0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentIndigo.copy(0.4f) else GlassBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    showCategoryDialog = false
                                    selectedCategory = cat
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                cat.replace("_", " "),
                                color = if (isSelected) AccentIndigo else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Transaction Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize().background(BgDeep)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDeep)
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
                            "Unable to load transaction details.",
                            color = AccentRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hero amount card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = if (uiState.isCredit)
                                            listOf(Color(0xFF064E3B), Color(0xFF065F46))
                                        else
                                            listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
                                    )
                                )
                                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                                .padding(24.dp)
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (uiState.isCredit) AccentGreen.copy(0.15f)
                                                else AccentIndigo.copy(0.15f)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            selectedCategory.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.isCredit) AccentGreen else AccentIndigo,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                    val paymentChannel = uiState.paymentChannel
                                    if (!paymentChannel.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(0.06f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                paymentChannel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextSecondary,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = uiState.displayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (uiState.isCredit) "+" else "-",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (uiState.isCredit) AccentGreen else TextPrimary
                                    )
                                    Text(
                                        text = "₹${String.format("%,.2f", uiState.amount)}",
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (uiState.isCredit) AccentGreen else TextPrimary
                                    )
                                }
                            }
                        }

                        // Editable Audit options card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Surface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    "Audit & Categorization",
                                    fontSize = 12.sp,
                                    color = AccentIndigo,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                // Category selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCategoryDialog = true }
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Category", color = TextSecondary, fontSize = 13.sp)
                                    Text(selectedCategory, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                // Subcategory text input
                                OutlinedTextField(
                                    value = editableSubcategory,
                                    onValueChange = { editableSubcategory = it },
                                    label = { Text("Subcategory", color = TextSecondary) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentIndigo,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Internal transfer switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Internal Transfer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Exclude from spending totals", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = isSelfTransferChecked,
                                        onCheckedChange = { isSelfTransferChecked = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AccentIndigo,
                                            checkedTrackColor = AccentIndigo.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Save Changes button
                                Button(
                                    onClick = {
                                        viewModel.updateDetails(
                                            selectedCategory,
                                            if (editableSubcategory.isBlank()) null else editableSubcategory,
                                            isSelfTransferChecked
                                        )
                                    },
                                    enabled = !uiState.isCategoryChanging && !uiState.isPurging,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (uiState.isCategoryChanging) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary)
                                    } else {
                                        Text("Apply Audit Changes", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Info Details Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Surface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                DetailRow("Date", uiState.date)
                                if (!uiState.transactionId.isNullOrBlank()) {
                                    Divider(color = GlassBorder, thickness = 0.5.dp)
                                    DetailRow("Bank Reference", uiState.transactionId)
                                }
                                if (!uiState.sourceSignalId.isNullOrBlank()) {
                                    Divider(color = GlassBorder, thickness = 0.5.dp)
                                    DetailRow("Signal ID", uiState.sourceSignalId)
                                }
                                if (uiState.createdAt.isNotBlank()) {
                                    Divider(color = GlassBorder, thickness = 0.5.dp)
                                    DetailRow("Recorded", uiState.createdAt.take(10))
                                }
                            }
                        }

                        // Danger delete transaction card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Surface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Danger Zone",
                                    fontSize = 12.sp,
                                    color = AccentRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showPurgeDialog = true },
                                    enabled = !uiState.isPurging && !uiState.isCategoryChanging,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(0.12f), contentColor = AccentRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Purge Transaction", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(0.4f))
        Text(
            text = value ?: "—",
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}
