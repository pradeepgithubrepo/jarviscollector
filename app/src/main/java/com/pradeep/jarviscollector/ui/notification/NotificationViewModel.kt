package com.pradeep.jarviscollector.ui.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.model.NotificationEntity
import com.pradeep.jarviscollector.repository.NotificationCenterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    val notificationsFlow: StateFlow<List<NotificationEntity>> = NotificationCenterRepository.getNotificationsFlow(application)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleReadStatus(id: String, readFlag: Boolean) {
        viewModelScope.launch {
            NotificationCenterRepository.markNotificationRead(getApplication(), id, readFlag)
        }
    }

    fun archiveNotification(id: String) {
        viewModelScope.launch {
            NotificationCenterRepository.archiveNotification(getApplication(), id)
        }
    }
}
