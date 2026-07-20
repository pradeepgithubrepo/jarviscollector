package com.pradeep.jarviscollector.ui.financial

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.repository.FinancialRepository
import com.pradeep.jarviscollector.service.InsightSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
    val id: String = "",
    val displayName: String = "",         // paid_to (canonical) or merchant (title)
    val merchant: String = "",            // raw title sentence
    val amount: Double = 0.0,
    val category: String = "",
    val subcategory: String? = null,
    val isSelfTransfer: Boolean = false,
    val date: String = "",
    val isCredit: Boolean = false,        // true = credit, false = debit
    val paymentChannel: String? = null,   // "UPI", "NEFT", etc.
    val transactionId: String? = null,    // Bank reference number
    val sourceSignalId: String? = null,
    val createdAt: String = "",
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val isPurging: Boolean = false,
    val isCategoryChanging: Boolean = false,
    val operationMessage: String? = null  // Shown in snackbar after an operation
)

class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "TransactionDetailVM"
    }

    fun loadTransaction(id: String) {
        viewModelScope.launch {
            try {
                val db = JarvisDatabase.getDatabase(getApplication())
                val event = db.financialEventDao().getAll().find { it.financial_event_id == id }

                if (event != null) {
                    val displayName = when {
                        !event.paid_to.isNullOrBlank() -> event.paid_to
                        !event.merchant.isNullOrBlank() -> event.merchant
                        else -> "Unknown Transaction"
                    }
                    val isCredit = event.transaction_type?.lowercase() == "credit"
                    val rawDate = event.event_timestamp ?: ""
                    val cleanDate = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate

                    _uiState.value = TransactionDetailUiState(
                        id = event.financial_event_id,
                        displayName = displayName,
                        merchant = event.merchant ?: displayName,
                        amount = event.amount ?: 0.0,
                        category = formatCategoryName(event.category ?: "General"),
                        subcategory = event.subcategory,
                        isSelfTransfer = event.is_self_transfer == true,
                        date = cleanDate,
                        isCredit = isCredit,
                        paymentChannel = event.payment_channel,
                        transactionId = event.transaction_id,
                        sourceSignalId = event.source_signal_id,
                        createdAt = event.created_at ?: "",
                        isLoading = false,
                        isError = false
                    )
                } else {
                    _uiState.value = TransactionDetailUiState(isLoading = false, isError = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load transaction details", e)
                _uiState.value = TransactionDetailUiState(isLoading = false, isError = true)
            }
        }
    }

    // True delete: removes from Supabase then Room, then re-syncs
    fun purgeTransaction(onSuccess: () -> Unit) {
        val id = _uiState.value.id
        if (id.isBlank()) return
        _uiState.value = _uiState.value.copy(isPurging = true)
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val success = FinancialRepository.purgeTransaction(context, id)
                if (success) {
                    // Trigger re-sync so monthly summaries refresh
                    InsightSyncService.syncInsights(context)
                    _uiState.value = _uiState.value.copy(
                        isPurging = false,
                        operationMessage = "Transaction permanently deleted."
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPurging = false,
                        operationMessage = "Delete failed. Please try again."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error purging transaction", e)
                _uiState.value = _uiState.value.copy(
                    isPurging = false,
                    operationMessage = "Error: ${e.message}"
                )
            }
        }
    }

    // Update details (category, subcategory, internal transfer flag)
    fun updateDetails(newCategory: String, newSubcategory: String?, isSelfTransfer: Boolean) {
        val id = _uiState.value.id
        if (id.isBlank()) return
        _uiState.value = _uiState.value.copy(isCategoryChanging = true)
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val success = FinancialRepository.updateTransactionDetails(
                    context, id, newCategory, newSubcategory, isSelfTransfer
                )
                if (success) {
                    InsightSyncService.syncInsights(context)
                    _uiState.value = _uiState.value.copy(
                        category = formatCategoryName(newCategory),
                        subcategory = newSubcategory,
                        isSelfTransfer = isSelfTransfer,
                        isCategoryChanging = false,
                        operationMessage = "Transaction details updated."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCategoryChanging = false,
                        operationMessage = "Update failed. Please try again."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating transaction details", e)
                _uiState.value = _uiState.value.copy(
                    isCategoryChanging = false,
                    operationMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun clearOperationMessage() {
        _uiState.value = _uiState.value.copy(operationMessage = null)
    }

    private fun formatCategoryName(raw: String): String {
        return raw.replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
