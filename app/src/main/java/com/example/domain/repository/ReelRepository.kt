package com.example.domain.repository

import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

interface ReelRepository {
    fun getAllReels(): Flow<List<DownloadItem>>
    fun getCompletedDownloads(): Flow<List<DownloadItem>>
    fun getActiveDownloads(): Flow<List<DownloadItem>>
    fun getReelById(id: Long): Flow<DownloadItem?>
    suspend fun findCompletedReelByReelId(reelId: String): DownloadItem?
    suspend fun findLatestReelByReelId(reelId: String): DownloadItem?
    fun searchReels(query: String): Flow<List<DownloadItem>>
    suspend fun insertReel(item: DownloadItem): Long
    suspend fun updateReel(item: DownloadItem)
    suspend fun updateProgress(id: Long, progress: Float, downloadedBytes: Long, status: DownloadStatus)
    suspend fun markCompleted(id: Long, videoUri: String, fileSize: Long)
    suspend fun markFailed(id: Long, errorMessage: String)
    suspend fun deleteReel(id: Long)
    suspend fun clearAllHistory()
}
