package com.pradeep.jarviscollector.ui.financial

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.model.FinancialInsightEntity
import com.pradeep.jarviscollector.repository.FinancialInsightRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinancialInsightViewModel(private val context: Context) : ViewModel() {

    val allInsights: StateFlow<List<FinancialInsightEntity>> =
        FinancialInsightRepository.observeInsights(context)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialSummary: StateFlow<List<FinancialInsightEntity>> =
        allInsights.map { list ->
            list.filter { it.type?.lowercase() == "snapshot" }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionRequired: StateFlow<List<FinancialInsightEntity>> =
        allInsights.map { list ->
            list.filter {
                val t = it.type?.lowercase() ?: ""
                val isAction = t == "action_required" || t == "upcoming_bill" || t == "emi"
                isAction && (it.status?.uppercase() == "PENDING")
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<FinancialInsightEntity>> =
        allInsights.map { list ->
            list.filter { it.type?.lowercase() == "subscription" }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingBills: StateFlow<List<FinancialInsightEntity>> =
        allInsights.map { list ->
            list.filter { it.type?.lowercase() == "bill" }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unusualActivity: StateFlow<List<FinancialInsightEntity>> =
        allInsights.map { list ->
            list.filter { it.type?.lowercase() == "unusual" }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun confirmInsight(id: String) {
        viewModelScope.launch {
            FinancialInsightRepository.confirmInsight(context, id)
        }
    }

    fun dismissInsight(id: String) {
        viewModelScope.launch {
            FinancialInsightRepository.dismissInsight(context, id)
        }
    }

    fun correctInsight(id: String, category: String, amount: Double) {
        viewModelScope.launch {
            FinancialInsightRepository.correctInsight(context, id, category, amount)
        }
    }
}
