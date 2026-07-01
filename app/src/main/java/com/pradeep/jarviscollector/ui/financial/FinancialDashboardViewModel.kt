package com.pradeep.jarviscollector.ui.financial

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.FinancialEventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CategorySummary(
    val category: String,
    val amount: Double
)

data class TransactionSummary(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
    val isIncome: Boolean
)

data class MonthlyTrendItem(
    val month: String,
    val amount: Double
)

data class FinancialDashboardUiState(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val savings: Double = 0.0,
    val recordCount: Int = 0,
    val categoryBreakdown: List<CategorySummary> = emptyList(),
    val recentTransactions: List<TransactionSummary> = emptyList(),
    val monthlyTrend: List<MonthlyTrendItem> = emptyList(),
    val selectedTypeFilter: String = "All",
    val selectedCategoryFilter: String? = null,
    val isLoading: Boolean = true,
    val isError: Boolean = false
)

class FinancialDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FinancialDashboardUiState())
    val uiState: StateFlow<FinancialDashboardUiState> = _uiState.asStateFlow()

    private var rawEvents: List<FinancialEventEntity> = emptyList()

    companion object {
        private const val TAG = "FinancialDashboardVM"
    }

    init {
        loadFinancialData()
        observeLiveUpdates()
    }

    fun setTypeFilter(filter: String) {
        val current = _uiState.value
        _uiState.value = current.copy(selectedTypeFilter = filter)
        applyFiltersAndAggregate()
    }

    fun setCategoryFilter(category: String?) {
        val current = _uiState.value
        _uiState.value = current.copy(selectedCategoryFilter = category)
        applyFiltersAndAggregate()
    }

    fun loadFinancialData() {
        viewModelScope.launch {
            try {
                val db = JarvisDatabase.getDatabase(getApplication())
                rawEvents = db.financialEventDao().getAll()
                applyFiltersAndAggregate()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load financial records from Room", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true
                )
            }
        }
    }

    private fun applyFiltersAndAggregate() {
        val events = rawEvents
        if (events.isEmpty()) {
            _uiState.value = FinancialDashboardUiState(
                isLoading = false,
                isError = false
            )
            return
        }

        val state = _uiState.value
        var incomeSum = 0.0
        var expenseSum = 0.0
        
        val allTransactions = mutableListOf<TransactionSummary>()
        val categoryExpenses = mutableMapOf<String, Double>()
        val monthlyExpenses = mutableMapOf<String, Double>()

        for (event in events) {
            val titleText = event.merchant ?: "Unknown Transaction"
            val amt = event.amount ?: 0.0
            val textLower = titleText.lowercase()
            
            val isIncome = textLower.contains("received") || 
                           textLower.contains("refund") || 
                           textLower.contains("credit") || 
                           textLower.contains("deposit") || 
                           textLower.contains("income") || 
                           textLower.contains("salary")

            if (isIncome) {
                incomeSum += amt
            } else {
                expenseSum += amt
                // Category spend aggregates (always based on debits)
                val cat = event.category?.trim()?.ifEmpty { "Others" } ?: "Others"
                val formattedCat = cat.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                categoryExpenses[formattedCat] = (categoryExpenses[formattedCat] ?: 0.0) + amt

                // Monthly Trend spend aggregates (based on event_timestamp/event_date debits)
                val rawDate = event.event_timestamp ?: ""
                val monthName = getMonthName(rawDate)
                if (monthName != "Unknown") {
                    monthlyExpenses[monthName] = (monthlyExpenses[monthName] ?: 0.0) + amt
                }
            }

            val rawDate = event.event_timestamp ?: ""
            val cleanDate = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate

            allTransactions.add(
                TransactionSummary(
                    id = event.financial_event_id,
                    title = titleText,
                    category = event.category?.replace("_", " ") ?: "General",
                    amount = amt,
                    date = cleanDate,
                    isIncome = isIncome
                )
            )
        }

        // Apply filters to transactions list
        var filteredTransactions = allTransactions.filter { tx ->
            when (state.selectedTypeFilter) {
                "Income" -> tx.isIncome
                "Expense" -> !tx.isIncome
                else -> true
            }
        }

        if (state.selectedCategoryFilter != null) {
            filteredTransactions = filteredTransactions.filter { tx ->
                tx.category.lowercase().trim() == state.selectedCategoryFilter.lowercase().trim()
            }
        }

        // Top 5 spending categories breakdown
        val breakdown = categoryExpenses.map {
            CategorySummary(it.key, it.value)
        }.sortedByDescending { it.amount }.take(5)

        // Monthly trend sorting (last 6 months order)
        val monthOrder = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val trend = monthlyExpenses.map {
            MonthlyTrendItem(it.key, it.value)
        }.sortedWith(compareBy { monthOrder.indexOf(it.month) }).takeLast(6)

        _uiState.value = state.copy(
            totalIncome = incomeSum,
            totalExpenses = expenseSum,
            savings = incomeSum - expenseSum,
            recordCount = events.size,
            categoryBreakdown = breakdown,
            recentTransactions = filteredTransactions.take(10),
            monthlyTrend = trend,
            isLoading = false,
            isError = false
        )
    }

    private fun getMonthName(dateString: String): String {
        if (dateString.length < 7) return "Unknown"
        val monthPart = dateString.substring(5, 7)
        return when (monthPart) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> "Unknown"
        }
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            val db = JarvisDatabase.getDatabase(getApplication())
            db.financialEventDao().getAllFlow().collectLatest {
                loadFinancialData()
            }
        }
    }
}
