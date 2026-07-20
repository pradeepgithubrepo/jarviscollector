package com.pradeep.jarviscollector.ui.financial

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.FinancialEventEntity
import com.pradeep.jarviscollector.model.MonthlySpendingSummaryEntity
import com.pradeep.jarviscollector.model.MonthlyCategorySpendEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategorySummary(
    val category: String,
    val amount: Double,
    val cumulativeAmount: Double,
    val momChangePercentage: Double?, // e.g., 18.2 for +18.2%, -5.0 for -5%, null if no previous month data
    val transactionCount: Int = 0
)

data class TransactionSummary(
    val id: String,
    val displayName: String,
    val category: String,
    val amount: Double,
    val date: String,
    val isCredit: Boolean,
    val paymentChannel: String?
)

data class FinancialDashboardUiState(
    val selectedMonthKey: String = "", // "YYYY-MM"
    val displayMonthName: String = "",  // e.g. "July 2026"
    
    // Authoritative totals for the selected month
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    
    // Trend insights for selected month vs previous month
    val totalExpenseMoMChange: Double? = null,      // e.g. -15.4%
    val topSpendingIncreaseCategory: String? = null, // e.g. "Food (+₹4,500)"
    val highestSpendCategory: String? = null,        // e.g. "Investment (₹25,000)"

    // Category breakdown with comparative spending metrics
    val categoryBreakdown: List<CategorySummary> = emptyList(),

    // Ledger for the selected month
    val recentTransactions: List<TransactionSummary> = emptyList(),
    val recordCount: Int = 0,

    val isLoading: Boolean = true,
    val isError: Boolean = false
)

class FinancialDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedMonthKey = MutableStateFlow(getCurrentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _uiState = MutableStateFlow(FinancialDashboardUiState())
    val uiState: StateFlow<FinancialDashboardUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "FinancialDashboardVM"
    }

    init {
        observeAllFinancialFlows()
    }

    private fun getCurrentMonthKey(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    fun previousMonth() {
        val current = _selectedMonthKey.value
        val prev = getPreviousMonthKey(current)
        if (prev.isNotBlank()) {
            _selectedMonthKey.value = prev
        }
    }

    fun nextMonth() {
        val current = _selectedMonthKey.value
        val next = getNextMonthKey(current)
        if (next.isNotBlank()) {
            _selectedMonthKey.value = next
        }
    }

    private fun getPreviousMonthKey(currentKey: String): String {
        try {
            val parts = currentKey.split("-")
            if (parts.size == 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                return if (month == 1) {
                    "${year - 1}-12"
                } else {
                    val prevMonth = month - 1
                    val prevMonthStr = if (prevMonth < 10) "0$prevMonth" else "$prevMonth"
                    "$year-$prevMonthStr"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating previous month for $currentKey", e)
        }
        return ""
    }

    private fun getNextMonthKey(currentKey: String): String {
        try {
            val parts = currentKey.split("-")
            if (parts.size == 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                return if (month == 12) {
                    "${year + 1}-01"
                } else {
                    val nextMonth = month + 1
                    val nextMonthStr = if (nextMonth < 10) "0$nextMonth" else "$nextMonth"
                    "$year-$nextMonthStr"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating next month for $currentKey", e)
        }
        return ""
    }

    private fun formatDisplayMonth(monthKey: String): String {
        try {
            val parts = monthKey.split("-")
            if (parts.size == 2) {
                val year = parts[0]
                val monthName = when (parts[1]) {
                    "01" -> "January"; "02" -> "February"; "03" -> "March"
                    "04" -> "April"; "05" -> "May"; "06" -> "June"
                    "07" -> "July"; "08" -> "August"; "09" -> "September"
                    "10" -> "October"; "11" -> "November"; "12" -> "December"
                    else -> parts[1]
                }
                return "$monthName $year"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting display month name for $monthKey", e)
        }
        return monthKey
    }

    private fun observeAllFinancialFlows() {
        viewModelScope.launch {
            try {
                val db = JarvisDatabase.getDatabase(getApplication())
                combine(
                    _selectedMonthKey,
                    db.monthlySpendingSummaryDao().getAllFlow(),
                    db.monthlyCategorySpendDao().getAllFlow(),
                    db.financialEventDao().getAllFlow()
                ) { monthKey, summaries, categorySpend, events ->
                    buildUiState(monthKey, summaries, categorySpend, events)
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing financial flows", e)
                _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
            }
        }
    }

    private fun buildUiState(
        monthKey: String,
        summaries: List<MonthlySpendingSummaryEntity>,
        categorySpend: List<MonthlyCategorySpendEntity>,
        events: List<FinancialEventEntity>
    ): FinancialDashboardUiState {

        val displayMonth = formatDisplayMonth(monthKey)
        val prevMonthKey = getPreviousMonthKey(monthKey)

        // 1. Get spending summaries for selected & previous month
        val currentSummary = summaries.find { it.month_key == monthKey }
        val prevSummary = summaries.find { it.month_key == prevMonthKey }

        val totalExpenses = currentSummary?.total_expense ?: 0.0
        val prevExpenses = prevSummary?.total_expense ?: 0.0

        val totalIncome = currentSummary?.total_income ?: 0.0

        val totalExpenseMoMChange = if (prevExpenses > 0.0) {
            ((totalExpenses - prevExpenses) / prevExpenses) * 100.0
        } else {
            null
        }

        // 2. Build category breakdown with MoM deltas & cumulative sums
        val currentCategories = categorySpend.filter { it.month_key == monthKey }
        val prevCategories = categorySpend.filter { it.month_key == prevMonthKey }

        // Find highest spend category this month
        val highestSpend = currentCategories.maxByOrNull { it.amount ?: 0.0 }
        val highestSpendCategory = highestSpend?.let {
            "${formatCategoryName(it.category)} (₹${String.format("%,.0f", it.amount ?: 0.0)})"
        }

        // Find largest absolute spend increase vs last month
        var maxIncrease = 0.0
        var topIncreaseCategoryName: String? = null
        
        val breakdown = currentCategories.map { currentCat ->
            val prevCat = prevCategories.find { it.category == currentCat.category }
            val currentAmount = currentCat.amount ?: 0.0
            val prevAmount = prevCat?.amount ?: 0.0

            val diff = currentAmount - prevAmount
            if (diff > maxIncrease) {
                maxIncrease = diff
                topIncreaseCategoryName = "${formatCategoryName(currentCat.category)} (+₹${String.format("%,.0f", diff)})"
            }

            val delta = if (prevAmount > 0.0) {
                ((currentAmount - prevAmount) / prevAmount) * 100.0
            } else {
                null
            }

            // Calculate "Till Date" cumulative amount across all data <= current month
            val cumulativeAmount = categorySpend
                .filter { it.category == currentCat.category && it.month_key <= monthKey }
                .sumOf { it.amount ?: 0.0 }

            CategorySummary(
                category = formatCategoryName(currentCat.category),
                amount = currentAmount,
                cumulativeAmount = cumulativeAmount,
                momChangePercentage = delta,
                transactionCount = currentCat.transaction_count ?: 0
            )
        }.sortedByDescending { it.amount }

        // 3. Filter monthly transactions (excluding internal transfers)
        val monthlyTx = events.filter { event ->
            val rawDate = event.event_timestamp ?: ""
            rawDate.startsWith(monthKey) && event.is_self_transfer != true
        }.map { event ->
            val displayName = when {
                !event.paid_to.isNullOrBlank() -> event.paid_to
                !event.merchant.isNullOrBlank() -> event.merchant
                else -> "Unknown Transaction"
            }
            val isCredit = event.transaction_type?.lowercase() == "credit"
            val rawDate = event.event_timestamp ?: ""
            val cleanDate = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate

            TransactionSummary(
                id = event.financial_event_id,
                displayName = displayName,
                category = formatCategoryName(event.category ?: "General"),
                amount = event.amount ?: 0.0,
                date = cleanDate,
                isCredit = isCredit,
                paymentChannel = event.payment_channel
            )
        }

        return FinancialDashboardUiState(
            selectedMonthKey = monthKey,
            displayMonthName = displayMonth,
            totalExpenses = totalExpenses,
            totalIncome = totalIncome,
            totalExpenseMoMChange = totalExpenseMoMChange,
            topSpendingIncreaseCategory = topIncreaseCategoryName,
            highestSpendCategory = highestSpendCategory,
            categoryBreakdown = breakdown,
            recentTransactions = monthlyTx,
            recordCount = monthlyTx.size,
            isLoading = false,
            isError = false
        )
    }

    private fun formatCategoryName(raw: String): String {
        return raw.replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
