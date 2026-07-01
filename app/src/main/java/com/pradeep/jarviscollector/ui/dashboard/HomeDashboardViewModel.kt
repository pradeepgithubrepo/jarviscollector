package com.pradeep.jarviscollector.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.repository.DailyBriefRepository
import com.pradeep.jarviscollector.repository.FactRepository
import com.pradeep.jarviscollector.repository.TodoRepository
import org.json.JSONArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class TaskSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val priority: String,
    val category: String
)

data class FactSummary(
    val id: String,
    val title: String,
    val summary: String,
    val category: String
)

data class EventSummary(
    val id: String,
    val title: String,
    val subtitle: String
)

data class HomeDashboardUiState(
    val taskCount: Int = -1,
    val factCount: Int = -1,
    val financialCount: Int = -1,
    val alertCount: Int = -1,
    val todayTasks: List<TaskSummary>? = null,       // null represents loading state
    val latestFacts: List<FactSummary>? = null,
    val upcomingEvents: List<EventSummary>? = null,
    val latestBriefPreview: String? = null,          // First line of latest brief
    val briefGeneratedAt: String? = null,
    val briefType: String? = null,
    val isError: Boolean = false
)

class HomeDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState: StateFlow<HomeDashboardUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "HomeDashboardViewModel"
    }

    init {
        loadDashboardData()
        observeLiveUpdates()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val db = JarvisDatabase.getDatabase(getApplication())
                
                // 1. Fetch Todos & Today Tasks & Upcoming Events
                val todos = TodoRepository.getTodos(getApplication())
                val openTodos = todos.filter { it.status != "COMPLETED" }
                val openTaskCount = openTodos.size

                val priorityWeights = mapOf("HIGH" to 3, "MEDIUM" to 2, "LOW" to 1)
                
                // Sort by priority desc, due date asc
                val sortedTasksForToday = openTodos.sortedWith(
                    compareByDescending<com.pradeep.jarviscollector.model.TodoEntity> {
                        priorityWeights[it.priority?.uppercase()] ?: 0
                    }.thenBy {
                        it.due_date ?: ""
                    }
                ).take(3)

                val mappedTodayTasks = sortedTasksForToday.map {
                    TaskSummary(
                        id = it.todo_id,
                        title = it.title ?: "Untitled Task",
                        subtitle = it.due_date ?: "No due date",
                        priority = it.priority ?: "MEDIUM",
                        category = "TASK"
                    )
                }

                // 2. Fetch Facts & Latest Facts
                val facts = FactRepository.getFacts(getApplication())
                val totalFactCount = facts.size
                
                val sortedFacts = facts.sortedByDescending { it.created_at ?: "" }.take(3)
                val mappedFacts = sortedFacts.map {
                    FactSummary(
                        id = it.id,
                        title = it.title ?: "Important fact",
                        summary = it.summary ?: "",
                        category = it.category ?: "General"
                    )
                }

                // 3. Fetch Financial Count
                val finFactsCount = db.financialInsightDao().getAll().size

                // 4. Alerts: Count SNOOZED or HIGH priority open tasks
                val alertsCount = openTodos.count { it.status == "SNOOZED" || it.priority?.uppercase() == "HIGH" }

                // 5. Upcoming Events: tasks with non-null due dates sorted by nearest due date
                val upcomingTasks = todos
                    .filter { !it.due_date.isNullOrBlank() && it.status != "COMPLETED" }
                    .sortedBy { it.due_date }
                    .take(3)
                val mappedEvents = upcomingTasks.map {
                    EventSummary(
                        id = it.todo_id,
                        title = it.title ?: "Event",
                        subtitle = it.due_date ?: ""
                    )
                }

                // 6. Daily Brief preview
                val latestBrief = db.dailyBriefDao().getLatest()
                val briefPreview = latestBrief?.let {
                    try { JSONArray(it.itemsJson).optString(0).takeIf { s -> s.isNotBlank() } } catch (e: Exception) { null }
                }

                _uiState.value = HomeDashboardUiState(
                    taskCount = openTaskCount,
                    factCount = totalFactCount,
                    financialCount = finFactsCount,
                    alertCount = alertsCount,
                    todayTasks = mappedTodayTasks,
                    latestFacts = mappedFacts,
                    upcomingEvents = mappedEvents,
                    latestBriefPreview = briefPreview,
                    briefGeneratedAt = latestBrief?.generatedAt,
                    briefType = latestBrief?.briefType,
                    isError = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dashboard summary data", e)
                _uiState.value = HomeDashboardUiState(
                    isError = true,
                    todayTasks = emptyList(),
                    latestFacts = emptyList(),
                    upcomingEvents = emptyList()
                )
            }
        }
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            val db = JarvisDatabase.getDatabase(getApplication())
            
            launch {
                db.dailyBriefDao().getLatestFlow().collectLatest {
                    loadDashboardData()
                }
            }

            launch {
                db.factInsightDao().getAllFlow().collectLatest {
                    loadDashboardData()
                }
            }
            
            launch {
                db.financialInsightDao().observeAll().collectLatest {
                    loadDashboardData()
                }
            }
            
            // Collect live updates on todos to refresh dashboard summaries instantly
            launch {
                db.todoDao().getAllFlow().collectLatest {
                    loadDashboardData()
                }
            }
        }
    }
}
