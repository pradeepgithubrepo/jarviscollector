package com.pradeep.jarviscollector.ui.lifecycle

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.LifecycleItemEntity
import com.pradeep.jarviscollector.repository.LifecycleRepository
import com.pradeep.jarviscollector.service.InsightSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LifecycleEventsUiState(
    val items: List<LifecycleItemEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val operationMessage: String? = null
)

class LifecycleEventsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LifecycleEventsUiState())
    val uiState: StateFlow<LifecycleEventsUiState> = _uiState.asStateFlow()

    private val db = JarvisDatabase.getDatabase(application)
    private val dao = db.lifecycleItemDao()

    companion object {
        private const val TAG = "LifecycleEventsVM"
    }

    init {
        observeItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            try {
                dao.getAllFlow().collect { list ->
                    _uiState.value = _uiState.value.copy(
                        items = list,
                        isLoading = false,
                        isError = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting flow of lifecycle items", e)
                _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
            }
        }
    }

    fun syncLifecycleEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                InsightSyncService.syncInsights(getApplication())
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun addEvent(
        domain: String,
        title: String,
        description: String?,
        scheduleType: String,
        intervalDays: Int?,
        nextOccurrenceDate: String,
        reminderOffsetDays: Int?,
        status: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = LifecycleRepository.createLifecycleItem(
                    getApplication(),
                    domain, title, description, scheduleType, intervalDays,
                    nextOccurrenceDate, reminderOffsetDays, status
                )
                if (success) {
                    InsightSyncService.syncInsights(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Event added successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Failed to add event to remote database"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add lifecycle event", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun updateEvent(
        id: String,
        domain: String,
        title: String,
        description: String?,
        scheduleType: String,
        intervalDays: Int?,
        nextOccurrenceDate: String,
        reminderOffsetDays: Int?,
        status: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = LifecycleRepository.updateLifecycleItem(
                    getApplication(),
                    id, domain, title, description, scheduleType, intervalDays,
                    nextOccurrenceDate, reminderOffsetDays, status
                )
                if (success) {
                    InsightSyncService.syncInsights(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Event updated successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Failed to update event details"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update lifecycle event", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = LifecycleRepository.deleteLifecycleItem(getApplication(), id)
                if (success) {
                    InsightSyncService.syncInsights(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Event deleted permanently"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Failed to delete event"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete lifecycle event", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun clearOperationMessage() {
        _uiState.value = _uiState.value.copy(operationMessage = null)
    }
}
