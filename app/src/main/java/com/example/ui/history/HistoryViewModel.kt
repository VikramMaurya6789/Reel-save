package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReelSaveApplication
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL,
    DOWNLOADED,
    FAILED
}

data class HistoryUiState(
    val items: List<DownloadItem> = emptyList(),
    val searchQuery: String = "",
    val filter: HistoryFilter = HistoryFilter.ALL
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ReelSaveApplication).container
    private val repository = container.repository

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllReels(),
        _searchQuery,
        _filter
    ) { allItems, query, filter ->
        val filteredByQuery = if (query.isBlank()) {
            allItems
        } else {
            allItems.filter {
                it.url.contains(query, ignoreCase = true) ||
                    (it.username?.contains(query, ignoreCase = true) == true) ||
                    it.filename.contains(query, ignoreCase = true)
            }
        }

        val filteredByStatus = when (filter) {
            HistoryFilter.ALL -> filteredByQuery
            HistoryFilter.DOWNLOADED -> filteredByQuery.filter { it.status == DownloadStatus.COMPLETED }
            HistoryFilter.FAILED -> filteredByQuery.filter { it.status == DownloadStatus.FAILED }
        }

        HistoryUiState(
            items = filteredByStatus,
            searchQuery = query,
            filter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteReel(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
