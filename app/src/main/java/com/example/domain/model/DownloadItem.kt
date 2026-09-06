package com.example.domain.model

enum class DownloadStatus {
    IDLE,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadItem(
    val id: Long = 0,
    val reelId: String,
    val url: String,
    val filename: String,
    val thumbnailUrl: String? = null,
    val username: String? = null,
    val videoUri: String? = null,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val downloadSpeed: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)

sealed class ResolutionState {
    data object Idle : ResolutionState()
    data object Validating : ResolutionState()
    data object Resolving : ResolutionState()
    data class Ready(val metadata: ReelMetadata) : ResolutionState()
    data class Failed(val message: String, val canRetry: Boolean = true) : ResolutionState()
}
