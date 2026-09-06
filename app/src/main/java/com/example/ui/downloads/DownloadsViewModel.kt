package com.example.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReelSaveApplication
import com.example.domain.downloader.MediaStoreHelper
import com.example.domain.model.DownloadItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val selectedTab: Int = 0, // 0 = Active, 1 = Completed
    val activeDownloads: List<DownloadItem> = emptyList(),
    val completedDownloads: List<DownloadItem> = emptyList(),
    val deleteItemCandidate: DownloadItem? = null
)

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ReelSaveApplication).container
    private val repository = container.repository
    private val downloader = container.downloader

    private val _selectedTab = MutableStateFlow(0)
    private val _deleteCandidate = MutableStateFlow<DownloadItem?>(null)

    val uiState: StateFlow<DownloadsUiState> = combine(
        _selectedTab,
        repository.getActiveDownloads(),
        repository.getCompletedDownloads(),
        downloader.activeDownloadsMap,
        _deleteCandidate
    ) { tab, repoActive, completed, runningMap, deleteCandidate ->
        // Merge active downloads from repository with live progress from runningMap
        val mergedActive = repoActive.map { item ->
            runningMap[item.id] ?: item
        }
        DownloadsUiState(
            selectedTab = tab,
            activeDownloads = mergedActive,
            completedDownloads = completed,
            deleteItemCandidate = deleteCandidate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsUiState()
    )

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun cancelDownload(id: Long) {
        downloader.cancelDownload(id)
    }

    fun promptDelete(item: DownloadItem) {
        _deleteCandidate.value = item
    }

    fun dismissDeleteDialog() {
        _deleteCandidate.value = null
    }

    fun confirmDelete() {
        val item = _deleteCandidate.value ?: return
        viewModelScope.launch {
            MediaStoreHelper.deleteVideo(getApplication(), item.videoUri)
            repository.deleteReel(item.id)
            _deleteCandidate.value = null
        }
    }
}
