package com.pradeep.jarviscollector.ui.financial

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransactionDetailUiState(
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: String = "",
    val isIncome: Boolean = false,
    val source: String = "",
    val isLoading: Boolean = true,
    val isError: Boolean = false
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
                val events = db.financialEventDao().getAll()
                val event = events.find { it.financial_event_id == id }

                if (event != null) {
                    val titleText = event.merchant ?: "Unknown Transaction"
                    val textLower = titleText.lowercase()
                    val isIncome = textLower.contains("received") || 
                                   textLower.contains("refund") || 
                                   textLower.contains("credit") || 
                                   textLower.contains("deposit") || 
                                   textLower.contains("income") || 
                                   textLower.contains("salary")

                    val rawDate = event.event_timestamp ?: ""
                    val cleanDate = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate

                    _uiState.value = TransactionDetailUiState(
                        title = titleText,
                        description = "Financial Ledger Entry: " + (event.category ?: "General Spend"),
                        amount = event.amount ?: 0.0,
                        category = event.category?.replace("_", " ") ?: "General",
                        date = cleanDate,
                        isIncome = isIncome,
                        source = "SMS Signal Capture (ID: ${event.source_signal_id ?: "N/A"})",
                        isLoading = false,
                        isError = false
                    )
                } else {
                    _uiState.value = TransactionDetailUiState(
                        isLoading = false,
                        isError = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load transaction details from Room cache", e)
                _uiState.value = TransactionDetailUiState(
                    isLoading = false,
                    isError = true
                )
            }
        }
    }
}
