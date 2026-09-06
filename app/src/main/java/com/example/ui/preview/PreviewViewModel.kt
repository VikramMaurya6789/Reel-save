package com.example.ui.preview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ReelSaveApplication
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.model.ReelFormat
import com.example.domain.model.ReelMetadata
import com.example.domain.model.ResolutionState
import com.example.domain.resolver.UrlValidator
import com.example.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreviewUiState(
    val url: String = "",
    val resolutionState: ResolutionState = ResolutionState.Idle,
    val selectedFormat: ReelFormat? = null,
    val alreadyDownloaded: DownloadItem? = null,
    val activeDownload: DownloadItem? = null,
    val isDownloadComplete: Boolean = false,
    val completedUri: String? = null,
    val errorMessage: String? = null
)

class PreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ReelSaveApplication).container
    private val resolver = container.resolver
    private val repository = container.repository
    private val downloader = container.downloader
    private val preferencesManager = container.preferencesManager

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    init {
        // Observe downloader active downloads map
        viewModelScope.launch {
            downloader.activeDownloadsMap.collect { activeMap ->
                val active = _uiState.value.activeDownload
                if (active != null && activeMap.containsKey(active.id)) {
                    val updated = activeMap[active.id]
                    _uiState.update { it.copy(activeDownload = updated) }
                }
            }
        }
    }

    fun loadReel(rawUrl: String) {
        if (_uiState.value.url == rawUrl && _uiState.value.resolutionState is ResolutionState.Ready) {
            return // Already loaded
        }

        val normalized = UrlValidator.normalizeUrl(rawUrl) ?: rawUrl
        _uiState.update {
            it.copy(
                url = normalized,
                resolutionState = ResolutionState.Resolving,
                errorMessage = null,
                isDownloadComplete = false,
                completedUri = null
            )
        }

        viewModelScope.launch {
            // Check if shortcode was already downloaded
            val shortcode = UrlValidator.extractShortcode(normalized)
            if (shortcode != null) {
                val existing = repository.findCompletedReelByReelId(shortcode)
                if (existing != null) {
                    _uiState.update { it.copy(alreadyDownloaded = existing) }
                }
            }

            val result = resolver.resolveReel(normalized)
            if (result.isSuccess) {
                val metadata = result.getOrThrow()
                val prefQuality = preferencesManager.settings.value.defaultQuality
                val chosenFormat = metadata.formats.find { it.qualityLabel.equals(prefQuality, ignoreCase = true) }
                    ?: metadata.formats.firstOrNull()

                _uiState.update {
                    it.copy(
                        resolutionState = ResolutionState.Ready(metadata),
                        selectedFormat = chosenFormat
                    )
                }

                // Save to history as visited / resolved
                val historyItem = DownloadItem(
                    reelId = metadata.id,
                    url = metadata.originalUrl,
                    filename = FileUtils.generateReelFilename(metadata.username),
                    thumbnailUrl = metadata.thumbnailUrl,
                    username = metadata.username,
                    fileSize = metadata.approxBytes ?: 0L,
                    status = DownloadStatus.IDLE
                )
                repository.insertReel(historyItem)
            } else {
                val exception = result.exceptionOrNull()
                val friendlyMessage = when {
                    !exception?.message.isNullOrBlank() ->
                        exception.message!!
                    exception?.message?.contains("valid", ignoreCase = true) == true ->
                        "Enter a valid Instagram Reel link."
                    exception?.message?.contains("connect", ignoreCase = true) == true ->
                        "Check your internet connection and try again."
                    else ->
                        "The Reel is unavailable or cannot be accessed."
                }
                _uiState.update {
                    it.copy(
                        resolutionState = ResolutionState.Failed(friendlyMessage),
                        errorMessage = friendlyMessage
                    )
                }
            }
        }
    }

    fun selectFormat(format: ReelFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun startDownload() {
        val readyState = _uiState.value.resolutionState as? ResolutionState.Ready ?: return
        val metadata = readyState.metadata
        val format = _uiState.value.selectedFormat ?: metadata.formats.firstOrNull()
        val videoUrl = format?.videoUrl ?: metadata.videoUrl

        val filename = FileUtils.generateReelFilename(metadata.username)
        val initialItem = DownloadItem(
            reelId = metadata.id,
            url = videoUrl,
            filename = filename,
            thumbnailUrl = metadata.thumbnailUrl,
            username = metadata.username,
            fileSize = format?.approxBytes ?: metadata.approxBytes ?: 0L,
            status = DownloadStatus.QUEUED
        )

        viewModelScope.launch {
            val insertedId = repository.insertReel(initialItem)
            val itemWithId = initialItem.copy(id = insertedId)

            _uiState.update {
                it.copy(
                    activeDownload = itemWithId,
                    isDownloadComplete = false
                )
            }

            downloader.startDownload(itemWithId) { savedUri ->
                _uiState.update {
                    it.copy(
                        isDownloadComplete = true,
                        completedUri = savedUri,
                        activeDownload = null,
                        alreadyDownloaded = itemWithId.copy(videoUri = savedUri, status = DownloadStatus.COMPLETED)
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        val active = _uiState.value.activeDownload ?: return
        downloader.cancelDownload(active.id)
        _uiState.update { it.copy(activeDownload = null) }
    }

    fun retry() {
        loadReel(_uiState.value.url)
    }
}
