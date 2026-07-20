package com.pradeep.jarviscollector.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.TodoEntity
import com.pradeep.jarviscollector.model.ReminderEntity
import com.pradeep.jarviscollector.repository.TodoRepository
import com.pradeep.jarviscollector.service.JarvisReminderManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class TodoUiFilter {
    object All : TodoUiFilter()
    object Today : TodoUiFilter()
    object Overdue : TodoUiFilter()
    object Completed : TodoUiFilter()
}

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    
    // UI Filter State
    private val _selectedFilter = MutableStateFlow<TodoUiFilter>(TodoUiFilter.All)
    val selectedFilter: StateFlow<TodoUiFilter> = _selectedFilter.asStateFlow()

    // Flow of Todos from Room
    val todosFlow: StateFlow<List<TodoEntity>> = db.todoDao().getAllFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow of active reminders from Room
    val remindersFlow: StateFlow<List<ReminderEntity>> = db.reminderDao().getAllFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setFilter(filter: TodoUiFilter) {
        _selectedFilter.value = filter
    }

    // Business actions (Remote-First)
    fun completeTodo(todoId: String) {
        viewModelScope.launch {
            // Cancel active reminder first
            JarvisReminderManager.cancelReminder(getApplication(), todoId)
            TodoRepository.markTodoComplete(getApplication(), todoId)
        }
    }

    fun snoozeTodo(todoId: String, durationMinutes: Int) {
        viewModelScope.launch {
            // 1. Snooze locally & remotely
            TodoRepository.snoozeTodo(getApplication(), todoId)
            
            // 2. Set alarm for snoozed interval
            val todo = db.todoDao().getAll().find { it.todo_id == todoId } ?: return@launch
            val scheduledTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
            
            val reminder = ReminderEntity(
                reminder_id = todoId,
                entity_type = "TODO",
                title = "Snoozed Task Alert",
                message = todo.title ?: "Snoozed task needs attention",
                scheduled_timestamp = scheduledTime,
                sound_type = "DEFAULT",
                action_route = "task_detail/$todoId",
                action_payload = "{\"todo_id\":\"$todoId\"}"
            )
            
            JarvisReminderManager.scheduleReminder(getApplication(), reminder)
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            // Cancel active alarm
            JarvisReminderManager.cancelReminder(getApplication(), todoId)
            TodoRepository.deleteTodo(getApplication(), todoId)
        }
    }

    fun setCustomReminder(todoId: String, dateInMillis: Long, timeInMinutes: Int, soundType: String) {
        viewModelScope.launch {
            val todo = db.todoDao().getAll().find { it.todo_id == todoId } ?: return@launch
            
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateInMillis
                set(Calendar.HOUR_OF_DAY, timeInMinutes / 60)
                set(Calendar.MINUTE, timeInMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val scheduledTime = calendar.timeInMillis
            
            val reminder = ReminderEntity(
                reminder_id = todoId,
                entity_type = "TODO",
                title = "Reminder: ${todo.category ?: "Task"}",
                message = todo.title ?: "Upcoming task deadline",
                scheduled_timestamp = scheduledTime,
                sound_type = soundType,
                action_route = "task_detail/$todoId",
                action_payload = "{\"todo_id\":\"$todoId\"}"
            )

            JarvisReminderManager.scheduleReminder(getApplication(), reminder)
        }
    }

    fun removeReminder(todoId: String) {
        viewModelScope.launch {
            JarvisReminderManager.cancelReminder(getApplication(), todoId)
        }
    }
}
