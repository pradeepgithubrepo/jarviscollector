package com.pradeep.jarviscollector.ui.facts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.model.FactInsightEntity
import com.pradeep.jarviscollector.repository.FactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FactViewModel(application: Application) : AndroidViewModel(application) {

    val factsFlow: StateFlow<List<FactInsightEntity>> = FactRepository.getFactsFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleReadStatus(id: String, readFlag: Boolean) {
        viewModelScope.launch {
            FactRepository.markFactRead(getApplication(), id, readFlag)
        }
    }
}
