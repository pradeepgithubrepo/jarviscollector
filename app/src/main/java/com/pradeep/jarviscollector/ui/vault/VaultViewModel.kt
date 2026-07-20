package com.pradeep.jarviscollector.ui.vault

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.database.JarvisDatabase
import com.pradeep.jarviscollector.model.VaultCategoryEntity
import com.pradeep.jarviscollector.model.VaultEntryEntity
import com.pradeep.jarviscollector.repository.VaultRepository
import com.pradeep.jarviscollector.service.InsightSyncService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VaultUiState(
    val categories: List<VaultCategoryEntity> = emptyList(),
    val entriesMap: Map<String, List<VaultEntryEntity>> = emptyMap(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val operationMessage: String? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val db = JarvisDatabase.getDatabase(application)
    private val categoryDao = db.vaultCategoryDao()
    private val entryDao = db.vaultEntryDao()

    companion object {
        private const val TAG = "VaultViewModel"
    }

    init {
        initializeAndObserve()
    }

    private fun initializeAndObserve() {
        viewModelScope.launch {
            try {
                // 1. Seed defaults if DB is empty
                VaultRepository.seedDefaultCategoriesIfEmpty(getApplication())

                // 2. Observe categories flow
                categoryDao.getAllFlow().collect { categoriesList ->
                    _uiState.value = _uiState.value.copy(
                        categories = categoriesList,
                        isLoading = false,
                        isError = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in initializeAndObserve", e)
                _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
            }
        }
    }

    fun getEntriesFlowForCategory(categoryId: String): Flow<List<VaultEntryEntity>> {
        return entryDao.getForCategoryFlow(categoryId)
    }

    fun syncVaultData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                InsightSyncService.syncInsights(getApplication())
                VaultRepository.seedDefaultCategoriesIfEmpty(getApplication())
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun createVaultEntry(
        categoryId: String,
        title: String,
        owner: String?,
        subCategory: String?,
        location: String?,
        accessInformation: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = VaultRepository.createVaultEntry(
                    getApplication(),
                    categoryId, title, owner, subCategory, location, accessInformation, notes
                )
                if (success) {
                    InsightSyncService.syncInsights(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Vault entry saved successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Failed to save entry to remote backend"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating vault entry", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun updateVaultEntry(
        entryId: String,
        categoryId: String,
        title: String,
        owner: String?,
        subCategory: String?,
        location: String?,
        accessInformation: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = VaultRepository.updateVaultEntry(
                    getApplication(),
                    entryId, categoryId, title, owner, subCategory, location, accessInformation, notes
                )
                if (success) {
                    InsightSyncService.syncInsights(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Vault entry updated successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Failed to update entry"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating vault entry", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    operationMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun deleteVaultEntry(entryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = VaultRepository.deleteVaultEntry(getApplication(), entryId)
                if (success) {
                    InsightSyncService.syncInsights(getApplication())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Vault entry deleted"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        operationMessage = "Failed to delete entry"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting vault entry", e)
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
