package com.pradeep.jarviscollector.ui.brief

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pradeep.jarviscollector.model.DailyBriefEntity
import com.pradeep.jarviscollector.repository.DailyBriefRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BriefSectionItem(
    val text: String,
    val index: Int
)

data class BriefPayloadSection(
    val heading: String,
    val bullets: List<String>
)

data class DailyBriefUiState(
    val latestBrief: DailyBriefEntity? = null,
    val latestMorningBrief: DailyBriefEntity? = null,
    val latestEveningBrief: DailyBriefEntity? = null,
    val sections: List<BriefSectionItem> = emptyList(),
    val payloadSections: List<BriefPayloadSection> = emptyList(),
    val generatedAt: String = "",
    val briefType: String = "MORNING",
    val todoCount: Int = 0,
    val fyiCount: Int = 0,
    val factCount: Int = 0,
    val isEmpty: Boolean = false,
    val isLoading: Boolean = true,
    val isError: Boolean = false
)

class DailyBriefViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DailyBriefUiState())
    val uiState: StateFlow<DailyBriefUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "DailyBriefViewModel"
    }

    init {
        loadBrief()
        observeLiveUpdates()
    }

    fun loadBrief() {
        viewModelScope.launch {
            try {
                val latest = DailyBriefRepository.getLatest(getApplication())
                val morning = DailyBriefRepository.getLatestByType(getApplication(), "MORNING")
                val evening = DailyBriefRepository.getLatestByType(getApplication(), "EVENING")

                // Prefer morning brief, fall back to latest
                val primary = morning ?: latest

                if (primary == null) {
                    _uiState.value = DailyBriefUiState(
                        latestMorningBrief = morning,
                        latestEveningBrief = evening,
                        isLoading = false,
                        isEmpty = true
                    )
                    return@launch
                }

                val sections = parseSections(primary.itemsJson)
                val payloadSections = parsePayload(primary.payloadJson)

                _uiState.value = DailyBriefUiState(
                    latestBrief = primary,
                    latestMorningBrief = morning,
                    latestEveningBrief = evening,
                    sections = sections,
                    payloadSections = payloadSections,
                    generatedAt = primary.generatedAt,
                    briefType = primary.briefType ?: "MORNING",
                    todoCount = primary.todoCount ?: 0,
                    fyiCount = primary.fyiCount ?: 0,
                    factCount = primary.factCount ?: 0,
                    isEmpty = sections.isEmpty(),
                    isLoading = false,
                    isError = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load daily brief from Room", e)
                _uiState.value = DailyBriefUiState(isLoading = false, isError = true)
            }
        }
    }

    private fun parseSections(itemsJson: String): List<BriefSectionItem> {
        return try {
            val array = JSONArray(itemsJson)
            (0 until array.length()).map { i ->
                BriefSectionItem(text = array.getString(i), index = i)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse itemsJson: $itemsJson", e)
            emptyList()
        }
    }

    private fun parsePayload(payloadJson: String?): List<BriefPayloadSection> {
        if (payloadJson.isNullOrBlank()) return emptyList()
        return try {
            val obj = JSONObject(payloadJson)
            val result = mutableListOf<BriefPayloadSection>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.get(key)
                val heading = key.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                val bullets = when (value) {
                    is JSONArray -> (0 until value.length()).map { value.getString(it) }
                    is String -> if (value.isNotBlank()) listOf(value) else emptyList()
                    else -> listOf(value.toString())
                }
                if (bullets.isNotEmpty()) {
                    result.add(BriefPayloadSection(heading = heading, bullets = bullets))
                }
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse payloadJson", e)
            emptyList()
        }
    }

    private fun observeLiveUpdates() {
        viewModelScope.launch {
            DailyBriefRepository.getLatestFlow(getApplication()).collectLatest {
                loadBrief()
            }
        }
    }
}
