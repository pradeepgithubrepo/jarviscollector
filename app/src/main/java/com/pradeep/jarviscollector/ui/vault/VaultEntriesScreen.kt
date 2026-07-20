package com.pradeep.jarviscollector.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradeep.jarviscollector.model.VaultEntryEntity

private val BgDeep = Color(0xFF0A0F1E)
private val Surface = Color(0xFF1A1F3A)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val AccentViolet = Color(0xFF8B5CF6)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEntriesScreen(
    categoryId: String,
    categoryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val entries by viewModel.getEntriesFlowForCategory(categoryId).collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog & Form state
    var showFormDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<VaultEntryEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<VaultEntryEntity?>(null) }

    // Form inputs
    var title by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var subCategory by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var accessInfo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val resetForm = { item: VaultEntryEntity? ->
        editingEntry = item
        if (item != null) {
            title = item.title
            owner = item.owner ?: ""
            subCategory = item.sub_category ?: ""
            location = item.location ?: ""
            accessInfo = item.access_information ?: ""
            notes = item.notes ?: ""
        } else {
            title = ""
            owner = ""
            subCategory = ""
            location = ""
            accessInfo = ""
            notes = ""
        }
    }

    LaunchedEffect(uiState.operationMessage) {
        val msg = uiState.operationMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearOperationMessage()
        }
    }

    // Confirm Delete Dialog
    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Vault Record", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to delete \"${entry.title}\"? This action will remove it permanently.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVaultEntry(entry.vault_entry_id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Add / Edit Dialog
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            containerColor = Surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = if (editingEntry != null) "Edit Vault Record" else "Add Vault Record",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *", color = TextSecondary) },
                        placeholder = { Text("e.g. HDFC Salary Account / Policy No") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = owner,
                        onValueChange = { owner = it },
                        label = { Text("Owner", color = TextSecondary) },
                        placeholder = { Text("e.g. Pradeep / Family") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = { subCategory = it },
                        label = { Text("Sub Category", color = TextSecondary) },
                        placeholder = { Text("e.g. Savings / Term Insurance") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Platform", color = TextSecondary) },
                        placeholder = { Text("e.g. HDFC Indiranagar / Zerodha") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accessInfo,
                        onValueChange = { accessInfo = it },
                        label = { Text("Access / Account / Policy Info", color = TextSecondary) },
                        placeholder = { Text("e.g. Customer ID: 987654") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes", color = TextSecondary) },
                        placeholder = { Text("e.g. Nominee details or branch info") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val item = editingEntry
                            if (item != null) {
                                viewModel.updateVaultEntry(
                                    entryId = item.vault_entry_id,
                                    categoryId = categoryId,
                                    title = title,
                                    owner = owner,
                                    subCategory = subCategory,
                                    location = location,
                                    accessInformation = accessInfo,
                                    notes = notes
                                )
                            } else {
                                viewModel.createVaultEntry(
                                    categoryId = categoryId,
                                    title = title,
                                    owner = owner,
                                    subCategory = subCategory,
                                    location = location,
                                    accessInformation = accessInfo,
                                    notes = notes
                                )
                            }
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    resetForm(null)
                    showFormDialog = true
                },
                containerColor = AccentViolet,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Vault Record")
            }
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
                entries.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No records in $categoryName.",
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button below to add your first record.",
                                color = TextSecondary.copy(0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries) { entry ->
                            VaultEntryCard(
                                entry = entry,
                                onEdit = {
                                    resetForm(entry)
                                    showFormDialog = true
                                },
                                onDelete = { showDeleteDialog = entry }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultEntryCard(
    entry: VaultEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Title + Owner badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!entry.owner.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentViolet.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = entry.owner.uppercase(),
                            color = AccentViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sub category pill if present
            if (!entry.sub_category.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.sub_category,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Fields: Location & Access Information & Notes
            if (!entry.location.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Location / Platform: ", color = TextSecondary, fontSize = 12.sp)
                    Text(entry.location, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!entry.access_information.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Access / Info: ", color = TextSecondary, fontSize = 12.sp)
                    Text(entry.access_information, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!entry.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Notes: ${entry.notes}",
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            Divider(color = GlassBorder, thickness = 0.5.dp)

            // Actions row: Edit & Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentRed.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AccentRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
